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

package com.ebay.cloud.cms.dal.persistence.flatten;

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.common.flatten.RaptorEntityGenerator;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.sequence.MongoSequence;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/**
 * @author liasu
 * 
 */
public class EmbedEntityExpressionTest extends CMSMongoTest {

    private static IPersistenceService persistenceService = null;
    private static IRepositoryService  repoService        = null;
    private static IMetadataService    metaService        = null;

    private static PersistenceContext  context            = null;

    private static final String        RAPTOR_REPO        = "raptor-paas";
    private static final String        BRANCH_TEST        = "test";
    private static MetadataDataLoader  metaLoader         = null;
    private static MongoSequence       sequence           = null;

    private static MetaClass           depMetadata;
    private static MetaClass           teamMetadata;
    private static MetaClass           personMetadata;
    private static MetaClass           serviceMetadata;

    @BeforeClass
    public static void setUp() {
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource dataSource = new MongoDataSource(connectionString);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        persistenceService = new NewPersistenceServiceImpl(dataSource);
        context = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);

        sequence = new MongoSequence(dataSource.getMongoInstance(), CMSConsts.SYS_DB, CMSConsts.SEQUENCE_COLL,
                CMSConsts.NEXT_FIELD_NAME_SEQ);

        depMetadata     = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.Dep.name());
        teamMetadata    = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.Team.name());
        personMetadata  = initPersonMeta();
        
        serviceMetadata = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
    }

    private static MetaClass initPersonMeta() {
        MetaClass meta = metaService.getMetaClass(RaptorEntityGenerator.TypeEnum.Person.name());
        MetaAttribute attr = new MetaAttribute(false);
        attr.setName("oldPerson");
        attr.setDataType(DataTypeEnum.STRING);
        attr.setValidation("$age > 50");
        attr.setDbName(sequence.getNext());
        meta.addField(attr);
        
        return meta;
    }

    @Test
    public void createWithExpression() {
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "25");
        team11.addFieldValue("person", person111);
        
        NewBsonEntity team12    = newExpressEntity(teamMetadata, "team12");
        NewBsonEntity person121 = newPersonEntity(personMetadata, "person121", "28");
        NewBsonEntity person122 = newPersonEntity(personMetadata, "person122", "52");
        team12.addFieldValue("person", person121);
        team12.addFieldValue("person", person122);
        
        dep1.addFieldValue("team", team11);
        dep1.addFieldValue("team", team12);
        
        String createId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);        
    }

    @Test
    public void createWithValidation() {
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "25");
        person111.addFieldValue("oldPerson", "yes");
        team11.addFieldValue("person", person111);
        dep1.addFieldValue("team", team11);
        
        try {
            persistenceService.create(dep1, context);
            Assert.fail();        
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
        
        person111.addFieldValue("age", "52");
        String createId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);        
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void moidfyWithExpression() {
        NewBsonEntity dep2      = newExpressEntity(depMetadata, "dep2");
        NewBsonEntity team21    = newExpressEntity(teamMetadata, "team21");
        NewBsonEntity person211 = newPersonEntity(personMetadata, "person211", "25");
        team21.addFieldValue("person", person211);
        dep2.addFieldValue("team", team21);
        
        String createId = persistenceService.create(dep2, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);        
        IEntity getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);
        
        String newName = "dep2-update-1";
        getDep.addFieldValue("name", newName);
        
        List<IEntity> teams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity team = teams.get(0);
        List<IEntity> persons = (List<IEntity>)team.getFieldValues("person");
        IEntity person = persons.get(0);
        person.addFieldValue("age", "52");
        NewBsonEntity person212 = newPersonEntity(personMetadata, "person212", "28");
        persons.add(person212);
        team.setFieldValues("person", persons);
        getDep.setFieldValues("team", teams);

        persistenceService.modify(getDep, context);
        getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void moidfyWithExpression01() {
        MetaAttribute hometown = new MetaAttribute(false);
        hometown.setName("hometown");
        hometown.setDataType(DataTypeEnum.STRING);
        hometown.setDbName(sequence.getNext());
        personMetadata.addField(hometown);
        MetaAttribute attribute = new MetaAttribute(false);
        attribute.setName("newCalName");
        attribute.setDataType(DataTypeEnum.STRING);
        attribute.setExpression("$name + $hometown");
        attribute.setDbName(sequence.getNext());
        personMetadata.addField(attribute);
        
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "25");
        team11.addFieldValue("person", person111);
        dep1.addFieldValue("team", team11);

        String createId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        // here the modify payload dones't have the name
        List<IEntity> teams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity team = teams.get(0);
        IEntity person = ((List<IEntity>)team.getFieldValues("person")).get(0);
        person.addFieldValue("hometown", "Shanghai");
        getDep.setFieldValues("team", teams);

        persistenceService.modify(getDep, context);
        getDep = persistenceService.get(queryEntity, context);
        
        List<IEntity> getTeams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity getTeam = getTeams.get(0);
        IEntity getPerson = ((List<IEntity>)getTeam.getFieldValues("person")).get(0);

        Assert.assertEquals(getPerson.getFieldValues("name").get(0).toString()
                + getPerson.getFieldValues("hometown").get(0).toString(), getPerson.getFieldValues("newCalName").get(0).toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void moidfyWithValidation() {
        NewBsonEntity dep2      = newExpressEntity(depMetadata, "dep2");
        NewBsonEntity team21    = newExpressEntity(teamMetadata, "team21");
        NewBsonEntity person211 = newPersonEntity(personMetadata, "person211", "52");
        person211.addFieldValue("oldPerson", "yes");
        team21.addFieldValue("person", person211);
        dep2.addFieldValue("team", team21);
        
        String createId = persistenceService.create(dep2, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);        
        IEntity getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);
        
        String newName = "dep2-update-1";
        getDep.addFieldValue("name", newName);
        
        List<IEntity> teams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity team = teams.get(0);
        IEntity person = ((List<IEntity>)team.getFieldValues("person")).get(0);
        person.addFieldValue("age", "25");
        getDep.setFieldValues("team", teams);

        try {
            persistenceService.modify(getDep, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void replaceWithExpression() {
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "25");
        team11.addFieldValue("person", person111);
        dep1.addFieldValue("team", team11);

        String createId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        
        getDep.addFieldValue("name", "newDep1");
        List<IEntity> teams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity team = teams.get(0);
        team.removeField("person");
        NewBsonEntity person112 = newPersonEntity(personMetadata, "person112", "60");
        team.addFieldValue("person", person112);
        getDep.setFieldValues("team", teams);
        
        persistenceService.replace(getDep, context);
        
        getDep = persistenceService.get(queryEntity, context);
        verifyDep(getDep);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void replaceWithValidation() {
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "52");
        person111.addFieldValue("oldPerson", "yes");
        team11.addFieldValue("person", person111);
        dep1.addFieldValue("team", team11);

        String createId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), createId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        
        getDep.addFieldValue("name", "newDep1");
        List<IEntity> teams = (List<IEntity>) getDep.getFieldValues("team");
        IEntity team = teams.get(0);
        team.removeField("person");
        NewBsonEntity person112 = newPersonEntity(personMetadata, "person112", "25");
        person112.addFieldValue("oldPerson", "yes");
        team.addFieldValue("person", person112);
        getDep.setFieldValues("team", teams);
        
        try {
            persistenceService.replace(getDep, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VALIDATION_FAILED, e.getErrorEnum());
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void referenceInExpression() {
    	MetaRelationship attribute = new MetaRelationship();
        attribute.setName("services");
        attribute.setDataType(DataTypeEnum.RELATIONSHIP);
        attribute.setRefDataType(serviceMetadata.getName());
        attribute.setDbName(sequence.getNext());
        attribute.setCardinality(CardinalityEnum.Many);
        personMetadata.addField(attribute);

        MetaAttribute refDiffAttribute = new MetaAttribute(false);
        refDiffAttribute.setName("refDiff");
        refDiffAttribute.setDataType(DataTypeEnum.BOOLEAN);
        refDiffAttribute.setDbName(sequence.getNext());
        refDiffAttribute.setExpression("if ($services != null && $services.length >=2) {$services[0].getId() != $services[1].getId()}");
        personMetadata.addField(refDiffAttribute);
    	
        NewBsonEntity dep1      = newExpressEntity(depMetadata, "dep1");
        NewBsonEntity team11    = newExpressEntity(teamMetadata, "team11");
        NewBsonEntity person111 = newPersonEntity(personMetadata, "person111", "25");
        NewBsonEntity service1 = new NewBsonEntity(serviceMetadata);
        service1.setId("faked-id-1");// just use a fake id here
        NewBsonEntity service2 = new NewBsonEntity(serviceMetadata);
        service2.setId("faked-id-2");// just use a fake id here
        person111.addFieldValue("services", service1);
        person111.addFieldValue("services", service2);
        team11.addFieldValue("person", person111);
        dep1.addFieldValue("team", team11);

        String newId = persistenceService.create(dep1, context);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.Dep.name(), newId);
        IEntity getDep = persistenceService.get(queryEntity, context);
        
        verifyDep(getDep);
        
        IEntity team = ((List<IEntity>)getDep.getFieldValues("team")).get(0);
        IEntity person = ((List<IEntity>)team.getFieldValues("person")).get(0);
        Assert.assertTrue((Boolean) person.getFieldValues("refDiff").get(0));
    }
    
    private NewBsonEntity newExpressEntity(MetaClass metaClass, String name) {
        NewBsonEntity entity = new NewBsonEntity(metaClass);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", generateRandomName(name));
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }

    private NewBsonEntity newPersonEntity(MetaClass metaClass, String name, String age) {
        NewBsonEntity entity = new NewBsonEntity(metaClass);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", name);
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");

        entity.addFieldValue("age", age);
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }
    
    @SuppressWarnings("unchecked")
    private void verifyDep(IEntity getDep) {
    	Assert.assertNotNull(getDep.getFieldValues("CalName"));
        Assert.assertEquals(getDep.getFieldValues("name").get(0), getDep.getFieldValues("CalName").get(0));
        
        List<IEntity> teams = (List<IEntity>)getDep.getFieldValues("team");
        for (IEntity team : teams) {
        	int teamSize = team.getFieldValues("person").size() ;
        	Boolean isLargeTeam = (Boolean)team.getFieldValues("isLargeTeam").get(0);
        	Assert.assertEquals(isLargeTeam.booleanValue(), teamSize > 1);
        	
        	List<IEntity> persons = (List<IEntity>)team.getFieldValues("person");
        	for (IEntity person : persons) {
        		String age = (String) person.getFieldValues("age").get(0);
        		Boolean isOld = (Boolean)person.getFieldValues("isOld").get(0);
        		Assert.assertEquals(Integer.valueOf(age) > 50, isOld.booleanValue());
        	}
        }
    }
    
    private NewBsonEntity buildQueryEntity(String branchname, String metadata, String oid) {
        MetaClass meta = metaService.getMetaClass(metadata);
        NewBsonEntity queryEntity = new NewBsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

}