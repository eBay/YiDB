/*
Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


/**
 * 
 */
/* 
Copyright 2012 eBay Software Foundation 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/ 

package com.ebay.cloud.cms.dal.persistence;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.common.RaptorEntityGenerator;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.utils.TestUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.sequence.MongoSequence;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/**
 * @author liasu
 * 
 */
public class EntityExpressionTest extends CMSMongoTest {

    private static IPersistenceService persistenceService = null;
    private static IRepositoryService  repoService        = null;
    private static IMetadataService    metaService        = null;

    private static PersistenceContext  context            = null;

    private static final String        RAPTOR_REPO        = "raptor-paas";
    private static final String        BRANCH_TEST        = "test";
    private static MetadataDataLoader  metaLoader         = null;
    private static MongoSequence       sequence           = null;

    private static MetaClass           applicationMetadata;
    private static MetaClass           serviceMetadata;

    @BeforeClass
    public static void setUp() {
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource dataSource = new MongoDataSource(connectionString);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        persistenceService = new PersistenceServiceImpl(dataSource);
        context = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);
        context.setRegistration(TestUtils.getTestDalImplemantation(dataSource));

        sequence = new MongoSequence(dataSource.getMongoInstance(), CMSConsts.SYS_DB, CMSConsts.SEQUENCE_COLL,
                CMSConsts.NEXT_FIELD_NAME_SEQ);

        applicationMetadata = initApplicationServiceExpressionField();
        applicationMetadata = initApplicationServiceValidationField();
        
        serviceMetadata = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
    }

    @Test
    public void createWithExpression() {
        BsonEntity entity = newExpressEntity(applicationMetadata, "expression-create");

        String createId = persistenceService.create(entity, context);
        
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity)persistenceService.get(queryEntity, context);

        Assert.assertNotNull(getApp.getFieldValues("name"));
        Assert.assertEquals(entity.getFieldValues("name").get(0), getApp.getFieldValues("name").get(0));

        Assert.assertNotNull(getApp.getFieldValues("CalName"));
        Assert.assertEquals(getApp.getFieldValues("name").get(0), getApp.getFieldValues("CalName").get(0));
    }

    @Test
    public void moidfyWithExpression() {
        String oldName = "expression-update";
        BsonEntity entity = newExpressEntity(applicationMetadata, oldName);

        String createId = persistenceService.create(entity, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity) persistenceService.get(queryEntity, context);
        String newName = "expression-update-1";
        getApp.addFieldValue("name", newName);

        persistenceService.modify(getApp, context);
        getApp = (BsonEntity)persistenceService.get(queryEntity, context);
        Assert.assertFalse(oldName.equals(getApp.getFieldValues("name").get(0)));
        Assert.assertTrue(newName.equals(getApp.getFieldValues("name").get(0)));

        Assert.assertEquals(newName, getApp.getFieldValues("CalName").get(0));
    }
    
    @Test
    public void moidfyWithExpression01() {
        MetaAttribute attribute = new MetaAttribute(false);
        attribute.setName("newCalName");
        attribute.setDataType(DataTypeEnum.STRING);
        attribute.setExpression("$name + $archTier");
        attribute.setDbName(sequence.getNext());
        applicationMetadata.addField(attribute);
        
        String oldName = "expression-update";
        BsonEntity entity = newExpressEntity(applicationMetadata, oldName);

        String createId = persistenceService.create(entity, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity)persistenceService.get(queryEntity, context);
        // here the modify payload dones't have the name
        getApp.addFieldValue("archTier", "archTierValue");

        persistenceService.modify(getApp, context);
        getApp = (BsonEntity)persistenceService.get(queryEntity, context);

        // cal name should not be null, and should be updated even the payload
        // is only partial of the involved field
        Assert.assertEquals(getApp.getFieldValues("name").get(0).toString()
                + getApp.getFieldValues("archTier").get(0).toString(), getApp.getFieldValues("newCalName").get(0));
    }

    @Test
    public void replaceWithExpression() {
        String oldName = "expression-replace";
        BsonEntity entity = newExpressEntity(applicationMetadata, oldName);

        String createId = persistenceService.create(entity, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity)persistenceService.get(queryEntity, context);
        String newName = "expression-replace-1";
        getApp.addFieldValue("name", newName);

        persistenceService.modify(getApp, context);
        queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        getApp = (BsonEntity)persistenceService.get(queryEntity, context);
        Assert.assertFalse(oldName.equals(getApp.getFieldValues("name").get(0)));
        Assert.assertTrue(newName.equals(getApp.getFieldValues("name").get(0)));

        Assert.assertEquals(newName, getApp.getFieldValues("CalName").get(0));
    }

    @Test
    public void createWithValidation() {
        BsonEntity entity = newValidationEntity(applicationMetadata, "validation-create");
        entity.addFieldValue("ValName", "123456789");
        
        String createId = persistenceService.create(entity, context);
        
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity)persistenceService.get(queryEntity, context);

        Assert.assertNotNull(getApp.getFieldValues("ValName"));
        Assert.assertEquals(entity.getFieldValues("ValName").get(0), getApp.getFieldValues("ValName").get(0));
        Assert.assertNotNull(getApp.getFieldValues("ValExprName"));
        Assert.assertEquals(getApp.getFieldValues("name").get(0), getApp.getFieldValues("ValExprName").get(0));
        
        BsonEntity invalidEntity = newValidationEntity(applicationMetadata, "invld");
        try {
            persistenceService.create(invalidEntity, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
        
        invalidEntity = newValidationEntity(applicationMetadata, "invld-creation");
        invalidEntity.addFieldValue("invalidValidationName", "invld");
        try {
            persistenceService.create(invalidEntity, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
    }

    @Test
    public void moidfyWithValidation() {
        BsonEntity entity = newValidationEntity(applicationMetadata, "validation-modify");
        entity.addFieldValue("ValName", "123456789");
        
        String createId = persistenceService.create(entity, context);
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity getApp = (BsonEntity)persistenceService.get(queryEntity, context);
        getApp.addFieldValue("name", "invld");
        try {
            persistenceService.modify(getApp, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
        
        BsonEntity newGetApp = (BsonEntity) persistenceService.get(queryEntity, context);
        newGetApp.addFieldValue("invalidValidationName", "invld");
        try {
            persistenceService.modify(newGetApp, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
    }

    @Test
    public void replaceWithValidation() {
        BsonEntity entity = newValidationEntity(applicationMetadata, "validation-replace");
        entity.addFieldValue("ValName", "123456789");
        
        String createId = persistenceService.create(entity, context);
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), createId);
        BsonEntity invalidEntity = (BsonEntity)persistenceService.get(queryEntity, context);
        invalidEntity.addFieldValue("name", "invld");
        try {
            persistenceService.replace(invalidEntity, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
        
        invalidEntity = (BsonEntity) persistenceService.get(queryEntity, context);
        invalidEntity.addFieldValue("invalidValidationName", "invld");
        try {
            persistenceService.replace(invalidEntity, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
    }
    
    @Test
    public void manifestDiffUsingExpression() {
        BsonEntity newApp = newExpressEntity(applicationMetadata, "manifestDiffUsingExpression");
        newApp.addFieldValue("activeManifestCur", "manifest-1.0");
        newApp.addFieldValue("activeManifestRef", "manifest-2.0");
        newApp.addFieldValue("activeManifestRef", "manifest-1.0");

        String newId = persistenceService.create(newApp, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), newId);
        BsonEntity getApp = (BsonEntity) persistenceService.get(queryEntity, context);
        Assert.assertNotNull(getApp);
        Assert.assertNotNull(getApp.getFieldValues("activeManifestDiff").get(0));
        Assert.assertTrue((Boolean) getApp.getFieldValues("activeManifestDiff").get(0));
    }

    @Test
    public void referenceInExpression() {
        BsonEntity newApp = newExpressEntity(applicationMetadata, "referenceInExpression");
        BsonEntity service1 = new BsonEntity(serviceMetadata);
        service1.setId("faked-id-1");// just use a fake id here
        
        BsonEntity service2 = new BsonEntity(serviceMetadata);
        service2.setId("faked-id-2");// just use a fake id here

        newApp.addFieldValue("services", service1);
        newApp.addFieldValue("services", service2);
        String newId = persistenceService.create(newApp, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), newId);
        BsonEntity getApp = (BsonEntity) persistenceService.get(queryEntity, context);
        Assert.assertNotNull(getApp);
        Assert.assertNotNull(getApp.getFieldValues("refDiff").get(0));
        Assert.assertTrue((Boolean) getApp.getFieldValues("refDiff").get(0));
    }
    
    @Ignore
    @Test
    public void fieldPropertyExpression() {
        BsonEntity newApp = newExpressEntity(applicationMetadata, "fieldPropertyExpression");
        String newId = persistenceService.create(newApp, context);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), newId);
        BsonEntity getApp = (BsonEntity) persistenceService.get(queryEntity, context);
        Assert.assertNotNull(getApp);
        Assert.assertNotNull(getApp.getFieldValues("nameLastModifedTime").get(0));
        Assert.assertTrue(getApp.getFieldTimestamp("name").equals(getApp.getFieldValues("nameLastModifedTime").get(0)));
    }

    private BsonEntity newExpressEntity(MetaClass metaClass, String name) {
        BsonEntity entity = new BsonEntity(metaClass);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", generateRandomName(name));
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }

    private BsonEntity newValidationEntity(MetaClass metaClass, String name) {
        BsonEntity entity = new BsonEntity(metaClass);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", name);
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }
    
    private static MetaClass initApplicationServiceExpressionField() {
        MetaClass metaClass = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ApplicationService.name());
        MetaAttribute attribute = new MetaAttribute(false);
        attribute.setName("CalName");
        attribute.setDataType(DataTypeEnum.STRING);
        attribute.setExpression("$name");
        attribute.setDbName(sequence.getNext());
        metaClass.addField(attribute);
        
        // sample for diff between reference
        MetaAttribute refDiffAttribute = new MetaAttribute(false);
        refDiffAttribute.setName("refDiff");
        refDiffAttribute.setDataType(DataTypeEnum.BOOLEAN);
        refDiffAttribute.setDbName(sequence.getNext());
        refDiffAttribute.setExpression("if ($services != null && $services.length >=2) {$services[0].getId() != $services[1].getId()} else false;");
        metaClass.addField(refDiffAttribute);
        
        // sample field for using field property. 
        // FIXME: currently not support field properties. One solution is to add a reserved variable called self
//        MetaAttribute fieldPropertyCheckAttribute = new MetaAttribute(false);
//        fieldPropertyCheckAttribute.setName("nameLastModifedTime");
//        fieldPropertyCheckAttribute.setDataType(DataTypeEnum.DATE);
//        fieldPropertyCheckAttribute.setDbName(sequence.getNext());
//        fieldPropertyCheckAttribute.setExpression("$self.getFieldTimestamp(\"name\")");// TODO
//        metaClass.addField(fieldPropertyCheckAttribute);
        return metaClass;
    }
    
    private static MetaClass initApplicationServiceValidationField() {
        MetaClass metaClass = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ApplicationService.name());
        MetaAttribute attribute1 = new MetaAttribute(false);
        attribute1.setName("ValName");
        attribute1.setDataType(DataTypeEnum.STRING);
        attribute1.setValidation("$ValName.length > 6");
        attribute1.setDbName(sequence.getNext());
        metaClass.addField(attribute1);
        
        MetaAttribute attribute2 = new MetaAttribute(false);
        attribute2.setName("ValExprName");
        attribute2.setDataType(DataTypeEnum.STRING);
        attribute2.setExpression("$name");
        attribute2.setValidation("$ValExprName.length > 10");
        attribute2.setDbName(sequence.getNext());
        metaClass.addField(attribute2);
        
        MetaAttribute attribute3 = new MetaAttribute(false);
        attribute3.setName("invalidValidationName");
        attribute3.setDataType(DataTypeEnum.STRING);
        attribute3.setValidation("$invalidValidationName.length");
        attribute3.setDbName(sequence.getNext());
        metaClass.addField(attribute3);
        
        return metaClass;
    }
    
    private BsonEntity buildQueryEntity(String branchname, String metadata, String oid) {
        MetaClass meta = metaService.getMetaClass(metadata);
        BsonEntity queryEntity = new BsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

}