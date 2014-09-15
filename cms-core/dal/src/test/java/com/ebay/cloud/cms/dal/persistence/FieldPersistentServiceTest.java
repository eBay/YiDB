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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.dal.common.DeploymentEntityGenerator;
import com.ebay.cloud.cms.dal.common.RaptorEntityGenerator;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.utils.TestUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBObject;

/**
 * @author liasu
 * 
 */
@SuppressWarnings("static-access")
public class FieldPersistentServiceTest extends CMSMongoTest {

    private static final String EMPLOYEE = "Employee";
    private static final String           SOFTWARE_DEPLOYMENT = "software-deployment";
    private static final String           RAPTOR_REPO         = "raptor-paas";
    private static final String           BRANCH_TEST         = "test";
    private static final String           PROPERTIES          = "properties";
    private static final String           MANIFEST_REF        = "manifestRef";
    private static final String           SERVICE_INSTANCE    = RaptorEntityGenerator.TypeEnum.ServiceInstance.name();
    private static final String           APPLICATION_SERVICE = RaptorEntityGenerator.TypeEnum.ApplicationService.name();
    private static final String           COMPANY             = "company";

    private static PersistenceServiceImpl persistenceService  = null;
    private static IRepositoryService     repoService         = null;

    private static IMetadataService       raptorMetaService   = null;
    private static IMetadataService       deployMetaService   = null;

    private static PersistenceContext     raptorContext       = null;
    private static PersistenceContext     deployContext       = null;

    private static MetadataDataLoader     metaLoader          = null;

    @BeforeClass
    public static void setUp() {
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource dataSource = new MongoDataSource(connectionString);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(SOFTWARE_DEPLOYMENT).getMetadataService();

        persistenceService = new PersistenceServiceImpl(dataSource);

        raptorContext = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);
        raptorContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
        deployContext = new PersistenceContext(deployMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);
        deployContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
    }

    private static BsonEntity createServiceInstance(int seq) {
        BsonEntity entity1 = newServiceInstance(seq);
        String newId = persistenceService.create(entity1, raptorContext);
        BsonEntity queryEntity = buildQueryEntity(entity1.getBranchId(), entity1.getMetaClass(), newId);
        BsonEntity saveEntity = (BsonEntity)persistenceService.get(queryEntity, raptorContext);
        return saveEntity;
    }

    private static BsonEntity newServiceInstance(int seq) {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        BsonEntity newEntity = new BsonEntity(metaClass);
        newEntity.addFieldValue("name", "ServiceInstance-" + seq);
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }

    private static BsonEntity newApplicationService(int seq) {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ApplicationService.name());
        BsonEntity newEntity = new BsonEntity(metaClass);
        newEntity.addFieldValue("name", "ApplcationService-" + seq);
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }
    
    @Test
    public void test00DeleteNormalField_withcontent() {
        BsonEntity entity = createServiceInstance(00);
        BsonEntity deleteEntity = new BsonEntity(entity.getMetaClass());
        deleteEntity.setBranchId(entity.getBranchId());
        deleteEntity.setId(entity.getId());
        deleteEntity.addFieldValue("name", "name-content");
        persistenceService.deleteField(deleteEntity, "name", raptorContext);

        BsonEntity getEntity = (BsonEntity)persistenceService.get(entity, raptorContext);
        Assert.assertFalse(getEntity.getFieldNames().contains("name"));
        
        Assert.assertEquals(1, getEntity.getVersion() - entity.getVersion());
    }

    @Test
    public void test01DeleteNormalField() {
        BsonEntity entity = createServiceInstance(01);
        BsonEntity deleteEntity = new BsonEntity(entity.getMetaClass());
        deleteEntity.setBranchId(entity.getBranchId());
        deleteEntity.setId(entity.getId());
        persistenceService.deleteField(deleteEntity, "name", raptorContext);

        BsonEntity getEntity = (BsonEntity)persistenceService.get(entity, raptorContext);
        Assert.assertFalse(getEntity.getFieldNames().contains("name"));
        
        Assert.assertEquals(1, getEntity.getVersion() - entity.getVersion());
    }

    @Ignore
    @Test
    public void test03DeleteArrayFieldWithNotMatchContent() {
        BsonEntity serviceInstance = newServiceInstance(3);
        serviceInstance.addFieldValue(MANIFEST_REF, "manifest-1.0");
        serviceInstance.addFieldValue(MANIFEST_REF, "manifest-2.0");
        serviceInstance.addFieldValue(MANIFEST_REF, "manifest-3.0");
        String newId = persistenceService.create(serviceInstance, raptorContext);
        BsonEntity queryEntity = buildQueryEntity(serviceInstance.getBranchId(), serviceInstance.getMetaClass(), newId);
        persistenceService.get(queryEntity, raptorContext);

        BsonEntity deleteEntity = new BsonEntity(serviceInstance.getMetaClass());
        deleteEntity.setId(newId);
        deleteEntity.setBranchId(serviceInstance.getBranchId());
        deleteEntity.addFieldValue(MANIFEST_REF, "manifest-4.0");
        persistenceService.deleteField(deleteEntity, MANIFEST_REF, raptorContext);

        BsonEntity getEntity = (BsonEntity)persistenceService.get(queryEntity, raptorContext);
        Assert.assertNotNull(getEntity.getFieldValues(MANIFEST_REF));
        Assert.assertTrue(getEntity.getFieldValues(MANIFEST_REF).size() == 3);
        // NOTE: here the result of 2 is incorrect, but dal won't check
        // whether the given payload are matched in
        // the db. It's entmgr's responsibility to do the query/matching
        Assert.assertTrue(getEntity.getFieldLength(MANIFEST_REF).equals(2));
        Assert.assertEquals(1, getEntity.getVersion());
    }

    @Test
    public void test05DeleteValidation() {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        try {
            persistenceService.deleteField(new BsonEntity(metaClass), null, raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            persistenceService.deleteField(new BsonEntity(metaClass), "", raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            persistenceService.deleteField(new BsonEntity(metaClass), InternalFieldEnum.ID.getName(), raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            persistenceService.deleteField(new BsonEntity(metaClass), "invalid field", raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            BsonEntity entity = new BsonEntity(metaClass);
            entity.addFieldValue("name", "newName");
            persistenceService.deleteField(entity, "name", raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private BsonEntity newPackage(int seq) {
        MetaClass metaClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Package.name());
        BsonEntity newEntity = new BsonEntity(metaClass);
        newEntity.addFieldValue("name", "Packge-" + seq);
        newEntity.addFieldValue("lastModifiedTime", new Date());
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }

    private BsonEntity newPackageVersion(int seq) {
        MetaClass metaClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.PackageVersion.name());
        BsonEntity newEntity = new BsonEntity(metaClass);
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.addFieldValue("name", "PackgeVersion-" + seq);
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }

    private BsonEntity createPackageWithVersion(int seq) {
        BsonEntity containPackage = newPackage(seq);
        String branchId = containPackage.getBranchId();

        BsonEntity packVersion1 = newPackageVersion(seq++);
        containPackage.addFieldValue("versions", packVersion1);

        BsonEntity packVersion2 = newPackageVersion(seq++);
        containPackage.addFieldValue("versions", packVersion2);

        String newId = persistenceService.create(containPackage, deployContext);
        BsonEntity queryEntity = buildQueryEntity(branchId, containPackage.getMetaClass(), newId);
        BsonEntity saveEntity = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        return saveEntity;
    }

    /**
     * This case for root field delete command apply on the embedded field
     */
    @Test
    public void test07DeleteWholeEmbeddedArray() {
        // delete embed field array cases: unset the whole embed field
        // Package --embed-> PackageVersion(Many)
        BsonEntity createdPackage = createPackageWithVersion(seq++);
        Assert.assertNotNull(createdPackage.getFieldValues("versions"));
        Assert.assertEquals(2, createdPackage.getFieldValues("versions").size());

        MetaClass metaClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Package.name());
        BsonEntity deleteEntity = new BsonEntity(metaClass);
        deleteEntity.setId(createdPackage.getId());
        deleteEntity.setBranchId(createdPackage.getBranchId());

        persistenceService.deleteField(deleteEntity, "versions", deployContext);

        // assertion
        BsonEntity queryEntity = buildQueryEntity(createdPackage.getBranchId(), createdPackage.getMetaClass(), createdPackage.getId());
        BsonEntity getEntity = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        Assert.assertFalse(getEntity.hasField("versions"));
        Assert.assertEquals(1, getEntity.getVersion());
    }

    private BsonEntity newManifestWithNoUse(int seq) {
        MetaClass manifestMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        BsonEntity manifest = new BsonEntity(manifestMeta);
        manifest.addFieldValue("name", "manifest-feild-test-" + seq);
        manifest.addFieldValue("noUses", newNoUse(seq));
        manifest.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        manifest.setBranchId(BRANCH_TEST);
        return manifest;
    }

    private BsonEntity newNoUse(int seq) {
        MetaClass noUseMeta = deployMetaService.getMetaClass("NoUse");
        BsonEntity noUse = new BsonEntity(noUseMeta);
        noUse.addFieldValue("name", "no-use-field-test-" + seq);
        noUse.addFieldValue("noUseHistory", "no-use-hisotry-001");
        noUse.addFieldValue("noUseHistory", "no-use-hisotry-002");
        noUse.addFieldValue("noUseHistory", "no-use-hisotry-003");
        noUse.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        noUse.setBranchId(BRANCH_TEST);
        return noUse;
    }

    private BsonEntity createManifestWithNoUse(int seq) {
        BsonEntity newManifest = newManifestWithNoUse(seq);
        String newId = persistenceService.create(newManifest, deployContext);
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, newManifest.getMetaClass(), newId);
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        return getManifest;
    }

    private BsonEntity newApproval(int seq) {
        MetaClass approvalMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Approval.name());
        BsonEntity approval = new BsonEntity(approvalMeta);
        approval.addFieldValue("name", "approval-field-test-" + seq);
        approval.addFieldValue("manifestCreatedTime", new Date());
        approval.addFieldValue("createdTime", new Date());
        approval.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return approval;
    }

    private BsonEntity newManifestVersionWithApproval(int seq) {
        MetaClass manifestVersionMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        BsonEntity manifestVersion = new BsonEntity(manifestVersionMeta);
        manifestVersion.addFieldValue("name", "manifest-version-field-test-" + seq);
        manifestVersion.addFieldValue("createdTime", new Date());
        manifestVersion.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");

        BsonEntity approval0 = newApproval(seq++);
        BsonEntity approval1 = newApproval(seq++);
        manifestVersion.addFieldValue("approvals", approval0);
        manifestVersion.addFieldValue("approvals", approval1);
        return manifestVersion;
    }

    private BsonEntity newManifestWithVersion(int seq) {
        BsonEntity entity = newManifestWithNoUse(seq);

        BsonEntity manifestVersion0 = newManifestVersionWithApproval(seq++);
        BsonEntity manifestVersion1 = newManifestVersionWithApproval(seq++);

        entity.addFieldValue("versions", manifestVersion0);
        entity.addFieldValue("versions", manifestVersion1);
        return entity;
    }
    
    private BsonEntity createManifestWithVersion() {
        BsonEntity manifest = newManifestWithVersion(seq++);
        String manifestId = persistenceService.create(manifest, deployContext);
        MetaClass manifestMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, manifestMeta, manifestId);
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        return getManifest;
    }

    /**
     * Delete a non-existing field is like a linux <code>touch</code> command:
     * don't change the user content. But udpate the version and timestamp
     * @throws InterruptedException 
     */
    @Test
    public void test10DeleteNotExisting() throws InterruptedException {
        BsonEntity service1 = createServiceInstance(07);

        BsonEntity get0 =(BsonEntity) persistenceService.get(service1, raptorContext);
        int version0 = get0.getVersion();
        Assert.assertEquals(0, version0);
        Date lastmodified0 = get0.getLastModified();
        Assert.assertNotNull(lastmodified0);

        BsonEntity deleteEntity = new BsonEntity(raptorMetaService.getMetaClass(SERVICE_INSTANCE));
        deleteEntity.setBranchId(service1.getBranchId());
        deleteEntity.setId(service1.getId());
        Thread.sleep(1000);
        persistenceService.deleteField(deleteEntity, "https", raptorContext);

        BsonEntity get1 = (BsonEntity)persistenceService.get(service1, raptorContext);
        Assert.assertEquals(1, get1.getVersion() - version0);
        Assert.assertNotNull(get1.getLastModified());
        Assert.assertEquals(true, get1.getLastModified().after(lastmodified0));
        Assert.assertEquals(1, get1.getVersion());
    }

    /**
     * TODO: NOT SUPPORT RIGHT NOW. delete a property inside a JSON field. Not supported now.
     */
    @Test
    @Ignore
    public void test11DeleteJson() {
    }

    private static int seq = 8;

    @Test
    public void test01ModifyNormalField() throws InterruptedException {
        BsonEntity serviceInstance = createServiceInstance(seq++);
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        modifyEntity.addFieldValue("name", "tryAnewName");

        Date oldLastModified = serviceInstance.getFieldTimestamp("name");
        Thread.sleep(1000);
        persistenceService.modifyField(modifyEntity, "name", raptorContext);
        BsonEntity getEntity = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);

        Assert.assertEquals(modifyEntity.getFieldValues("name").get(0), getEntity.getFieldValues("name").get(0));
        Date newLastModified = getEntity.getFieldTimestamp("name");

        Assert.assertTrue(newLastModified.after(oldLastModified));
        Assert.assertEquals(1, getEntity.getVersion());
    }

    @Test
    public void test02ModifyArrayField() {
        BsonEntity serviceInstance = newServiceInstance(seq++);
        String fieldName = MANIFEST_REF;
        serviceInstance.addFieldValue(fieldName, "manifest-1.0");
        serviceInstance.addFieldValue(fieldName, "manifest-2.0");
        serviceInstance.addFieldValue(fieldName, "manifest-3.0");

        String newId = persistenceService.create(serviceInstance, raptorContext);
        BsonEntity queryEntity = buildQueryEntity(serviceInstance.getBranchId(), serviceInstance.getMetaClass(), newId);
        BsonEntity oldGet = (BsonEntity)persistenceService.get(queryEntity, raptorContext);
        List<?> oldManifests = oldGet.getFieldValues(fieldName);
        Assert.assertNotNull(oldManifests);
        Assert.assertEquals(3, oldManifests.size());

        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        String newManifestValue = "manifest-4.0";
        modifyEntity.addFieldValue(fieldName, newManifestValue);

        persistenceService.modifyField(modifyEntity, fieldName, raptorContext);

        queryEntity = buildQueryEntity(serviceInstance.getBranchId(), serviceInstance.getMetaClass(), newId);
        BsonEntity newGet = (BsonEntity)persistenceService.get(queryEntity, raptorContext);
        List<?> newManifests = newGet.getFieldValues(fieldName);
        Assert.assertNotNull(newManifests);
        Assert.assertEquals(1, newManifests.size() - oldManifests.size());
        Assert.assertTrue(newManifests.contains(newManifestValue));
        Assert.assertEquals(1, newGet.getVersion());
        
        
        BsonEntity modifyEntity01 = new BsonEntity(metaClass);
        modifyEntity01.setBranchId(serviceInstance.getBranchId());
        modifyEntity01.setId(serviceInstance.getId());
        String newManifestValue01 = "manifest-4.0";
        modifyEntity01.addFieldValue(fieldName, newManifestValue01);

        persistenceService.modifyField(modifyEntity01, fieldName, raptorContext);
        
        queryEntity = buildQueryEntity(serviceInstance.getBranchId(), serviceInstance.getMetaClass(), newId);
        BsonEntity newGet01 = (BsonEntity)persistenceService.get(queryEntity, raptorContext);
        List<?> newManifests01 = newGet01.getFieldValues(fieldName);
        Assert.assertNotNull(newManifests01);
        Assert.assertEquals(0, newManifests01.size() - newManifests01.size());
        Assert.assertTrue(newManifests01.contains(newManifestValue01));
        Assert.assertEquals(2, newGet01.getVersion());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test03ModifyReferenceArrayField() {
        BsonEntity serv1 = createServiceInstance(seq++);
        BsonEntity serv2 = createServiceInstance(seq++);
        BsonEntity appEntity = newApplicationService(seq++);
        appEntity.addFieldValue("services", serv1);
        appEntity.addFieldValue("services", serv2);

        persistenceService.create(appEntity, raptorContext);

        BsonEntity serv3 = createServiceInstance(seq++);
        BsonEntity modifyEntity = new BsonEntity(raptorMetaService.getMetaClass(APPLICATION_SERVICE));
        modifyEntity.setBranchId(appEntity.getBranchId());
        modifyEntity.setId(appEntity.getId());
        modifyEntity.addFieldValue("services", serv3);
        persistenceService.modifyField(modifyEntity, "services", raptorContext);

        // assert
        BsonEntity getApp = (BsonEntity)persistenceService.get(appEntity, raptorContext);
        Assert.assertNotNull(getApp.getFieldValues("services"));
        Assert.assertEquals(3, getApp.getFieldValues("services").size());
        Assert.assertEquals(1, getApp.getVersion());
        Set<String> ids = new HashSet<String>();
        ids.add(serv1.getId());
        ids.add(serv2.getId());
        ids.add(serv3.getId());
        for (BsonEntity entity : (List<BsonEntity>) getApp.getFieldValues("services")) {
            Assert.assertTrue(ids.contains(entity.getId()));
        }
        
        BsonEntity modifyEntity01 = new BsonEntity(raptorMetaService.getMetaClass(APPLICATION_SERVICE));
        modifyEntity01.setBranchId(appEntity.getBranchId());
        modifyEntity01.setId(appEntity.getId());
        modifyEntity01.addFieldValue("services", serv3);
        persistenceService.modifyField(modifyEntity01, "services", raptorContext);

        // assert
        BsonEntity getApp01 = (BsonEntity)persistenceService.get(appEntity, raptorContext);
        Assert.assertNotNull(getApp01.getFieldValues("services"));
        Assert.assertEquals(3, getApp01.getFieldValues("services").size());
    }

    @Test
    public void test04appendToEmbedArrayField() {
        BsonEntity packEntity = createPackageWithVersion(seq++);
        int oldLength = packEntity.getFieldLength("versions");

        MetaClass packageMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Package.name());
        BsonEntity modifyEntity = new BsonEntity(packageMeta);
        modifyEntity.setBranchId(packEntity.getBranchId());
        modifyEntity.setId(packEntity.getId());
        BsonEntity newVersion = newPackageVersion(seq++);
//        modifyEntity.setFieldValues("versions", packEntity.getFieldValues("versions"));
        modifyEntity.addFieldValue("versions", newVersion);

        persistenceService.modifyField(modifyEntity, "versions", deployContext);

        // assertion
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, modifyEntity.getMetaClass(), packEntity.getId());
        BsonEntity getPack = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        Assert.assertNotNull(getPack.getFieldValues("versions"));
        Assert.assertEquals(1, getPack.getFieldValues("versions").size() - oldLength);
        Assert.assertEquals(1, getPack.getFieldLength("versions") - oldLength);
        Assert.assertEquals(1, getPack.getVersion());
    }

    @Test
    public void test05appendArrayFieldInEmbed() {
        BsonEntity manifest = createManifestWithNoUse(seq++);
        int oldVersion = manifest.getVersion();

        BsonEntity noUse = (BsonEntity) manifest.getFieldValues("noUses").get(0);
        
        MetaClass noUseMeta = deployMetaService.getMetaClass("NoUse");
        BsonEntity modifyEntity = new BsonEntity(noUseMeta);
        modifyEntity.setId(noUse.getId());
        modifyEntity.setBranchId(noUse.getBranchId());
//        modifyEntity.setFieldValues("noUseHistory", noUse.getFieldValues("noUseHistory"));
        modifyEntity.addFieldValue("noUseHistory", "no-use-hisotry-004");
        modifyEntity.addFieldValue("noUseHistory", "no-use-hisotry-005");
        persistenceService.modifyField(modifyEntity, "noUseHistory", deployContext);
        
        BsonEntity getMani = (BsonEntity)persistenceService.get(manifest, deployContext);
        Assert.assertEquals(1, getMani.getVersion() - oldVersion);
        BsonEntity getNoUse = (BsonEntity)getMani.getFieldValues("noUses").get(0);
        Assert.assertEquals(5, getNoUse.getFieldValues("noUseHistory").size());
        Assert.assertEquals(5, getNoUse.getFieldLength("noUseHistory").intValue());
        Assert.assertEquals(1, getMani.getVersion());
    }

    @Test
    public void test06appendNestArrayFieldInEmbed() {
        // Manifest --embed(Many)-> ManfiestVersion --embed(Many)-> Approval
        BsonEntity manifest = createManifestWithVersion();
        BsonEntity manifestVersion = (BsonEntity)manifest.getFieldValues("versions").get(1);

        MetaClass manifetVersionMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        BsonEntity deleteManifestVersion = new BsonEntity(manifetVersionMeta);
        deleteManifestVersion.setId(manifestVersion.getId());
        deleteManifestVersion.setBranchId(manifestVersion.getBranchId());
        BsonEntity approval = newApproval(seq++);
//        deleteManifestVersion.setFieldValues("approvals", manifestVersion.getFieldValues("approvals"));
        deleteManifestVersion.addFieldValue("approvals", approval);

        persistenceService.modifyField(deleteManifestVersion, "approvals", deployContext);

        // assertion
        BsonEntity getManifest = (BsonEntity)persistenceService.get(manifest, deployContext);
        Assert.assertEquals(2, getManifest.getFieldValues("versions").size());
        Assert.assertEquals(1, getManifest.getVersion());
        BsonEntity getVersion = (BsonEntity) getManifest.getFieldValues("versions").get(1);
        Assert.assertEquals(manifestVersion.getId(), getVersion.getId());
        Assert.assertEquals(3, getVersion.getFieldValues("approvals").size());
        Assert.assertEquals(3, getVersion.getFieldLength("approvals").intValue());
    }

    @Test
    public void test07appendJsonFieldInEmbed() {
        //Manifest -embedOne -> NoUse -json-> noUseProperties
        BsonEntity manifest = newManifestWithNoUse(seq++);
        BsonEntity noUse = (BsonEntity) manifest.getFieldValues("noUses").get(0);
        noUse.addFieldValue("noUseProperties", "{\"abc\": 111}");
        String manifestId = persistenceService.create(manifest, deployContext);
        BsonEntity queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity oldManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        BsonEntity oldNoUse = (BsonEntity) oldManifest.getFieldValues("noUses").get(0);

        MetaClass noUseMeta = deployMetaService.getMetaClass("NoUse");
        BsonEntity addNoUse = new BsonEntity(noUseMeta);
        addNoUse.setId(oldNoUse.getId());
        addNoUse.setBranchId(oldNoUse.getBranchId());
        addNoUse.addFieldValue("noUseProperties", "{\"def\": \"321\"}");
        persistenceService.modifyField(addNoUse, "noUseProperties", deployContext);

        // assertion
        queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity newManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        Assert.assertEquals(1, newManifest.getVersion());
        BsonEntity newNoUse = (BsonEntity) newManifest.getFieldValues("noUses").get(0);
        DBObject property = (DBObject) newNoUse.getFieldValues("noUseProperties").get(0);
        property.containsField("abc");
        Assert.assertEquals(111, property.get("abc"));
        property.containsField("def");
        Assert.assertEquals("321", property.get("def"));
    }

    @Test
    public void test08AppendEmptyContent() {
        BsonEntity createdEntity = createServiceInstanceManifest();
        Assert.assertNotNull(createdEntity.getFieldValues(MANIFEST_REF));
        Assert.assertEquals(3, createdEntity.getFieldValues(MANIFEST_REF).size());

        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(createdEntity.getBranchId());
        modifyEntity.setId(createdEntity.getId());
        // NOT set field value
        // modifyEntity.addFieldValue("manifestRef", "");

        persistenceService.modifyField(modifyEntity, MANIFEST_REF, raptorContext);

        BsonEntity get = (BsonEntity)persistenceService.get(createdEntity, raptorContext);
        Assert.assertEquals(1, get.getVersion());
        Assert.assertNotNull(get.getFieldValues(MANIFEST_REF));
        Assert.assertEquals(3, get.getFieldValues(MANIFEST_REF).size());
    }

    private BsonEntity createServiceInstanceManifest() {
        BsonEntity serviceInstance = newServiceInstance(seq++);
        String fieldName = MANIFEST_REF;
        serviceInstance.addFieldValue(fieldName, "manifest-1.0");
        serviceInstance.addFieldValue(fieldName, "manifest-2.0");
        serviceInstance.addFieldValue(fieldName, "manifest-3.0");
        persistenceService.create(serviceInstance, raptorContext);
        BsonEntity get = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        return get;
    }

    @Test
    public void test09AppendValidation() {
        BsonEntity serviceInstance = createServiceInstance(seq++);
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        modifyEntity.addFieldValue("name", "tryAnewName");

        persistenceService.modifyField(modifyEntity, "name", raptorContext);
        BsonEntity get = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        Assert.assertEquals(1, get.getVersion());
    }

    @Test
    public void test10AppendJsonField() {
        BsonEntity serviceInstance = createServiceInstance(seq++);

        // construct modify entity
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());

        modifyEntity.addFieldValue(PROPERTIES, "{\"ab\": 5}");
        persistenceService.modifyField(modifyEntity, PROPERTIES, raptorContext);

        BsonEntity oldGet = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        DBObject oldProp = (DBObject) oldGet.getFieldValues(PROPERTIES).get(0);
        Assert.assertEquals(5, oldProp.get("ab"));

        // try to update again
        modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());

        modifyEntity.addFieldValue(PROPERTIES, "{\"ab\": 10, \"c\": [\"new string in c's property\"]}");
        persistenceService.modifyField(modifyEntity, PROPERTIES, raptorContext);

        BsonEntity newGet = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        Assert.assertEquals(2, newGet.getVersion());
        DBObject newProp = (DBObject) newGet.getFieldValues(PROPERTIES).get(0);
        Assert.assertEquals(10, newProp.get("ab"));
        Assert.assertNotNull(newProp.get("c"));
    }

    @Test
    public void test11AppendJsonWithEmptyContent() {
        BsonEntity serviceInstance = createServiceInstance(seq++);

        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        modifyEntity.addFieldValue(PROPERTIES, "{\"ab\": 5}");
        persistenceService.modifyField(modifyEntity, PROPERTIES, raptorContext);

        BsonEntity oldGet = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        DBObject oldProp = (DBObject) oldGet.getFieldValues(PROPERTIES).get(0);
        Assert.assertEquals(5, oldProp.get("ab"));

        // try to update again, this time we don't set values
        modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        // NOT set values
        // modifyEntity.addFieldValue(PROPERTIES,
        // "{\"ab\": 10, \"c\": [\"new string in c's property\"]}");
        persistenceService.modifyField(modifyEntity, PROPERTIES, raptorContext);

        BsonEntity newGet = (BsonEntity)persistenceService.get(serviceInstance, raptorContext);
        Assert.assertEquals(2, newGet.getVersion());
        DBObject newProp = (DBObject) newGet.getFieldValues(PROPERTIES).get(0);
        Assert.assertEquals(5, newProp.get("ab"));
        Assert.assertNull(newProp.get("c"));
    }
    
    @Test
    public void test12ModifyNormalEmbedField() throws InterruptedException {
        // Manifest -embedOne -> NoUse -json-> noUseProperties
        BsonEntity manifest = newManifestWithNoUse(seq++);
        BsonEntity noUse = (BsonEntity) manifest.getFieldValues("noUses").get(0);
        noUse.addFieldValue("name", generateRandomName("ManifestName12-"));
        String manifestId = persistenceService.create(manifest, deployContext);
        BsonEntity queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity oldManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        BsonEntity oldNoUse = (BsonEntity) oldManifest.getFieldValues("noUses").get(0);
        Date oldDate = oldNoUse.getFieldTimestamp("name");

        MetaClass noUseMeta = deployMetaService.getMetaClass("NoUse");
        BsonEntity addNoUse = new BsonEntity(noUseMeta);
        addNoUse.setId(oldNoUse.getId());
        addNoUse.setBranchId(oldNoUse.getBranchId());
        addNoUse.addFieldValue("name", generateRandomName("noUserName-"));
        Thread.sleep(1000);
        persistenceService.modifyField(addNoUse, "name", deployContext);
        Date modifyDate = addNoUse.getFieldTimestamp("name");

        // assertion
        queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity newManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        Assert.assertEquals(1, newManifest.getVersion());
        BsonEntity newNoUse = (BsonEntity) newManifest.getFieldValues("noUses").get(0);
        String name = newNoUse.getFieldValues("name").get(0).toString();
        Assert.assertEquals(addNoUse.getFieldValues("name").get(0).toString(), name);
        Date newDate = newNoUse.getFieldTimestamp("name");
        
        Assert.assertTrue(newDate.after(oldDate));
        Assert.assertTrue(newDate.equals(modifyDate));
    }
    
    @Test
    public void test13ModifyInternalField() {
        BsonEntity serviceInstance = createServiceInstance(seq++);
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(serviceInstance.getBranchId());
        modifyEntity.setId(serviceInstance.getId());
        modifyEntity.addFieldValue("_createtime", new Date());
        try {
            persistenceService.modifyField(modifyEntity, "_createtime", raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

    }
    
    @Test
    public void test14deleteEmbedFieldCardinalityOne() {
        // Manifest -embedOne -> NoUse -json-> noUseProperties
        BsonEntity manifest = newManifestWithNoUse(seq++);
        BsonEntity noUse = (BsonEntity) manifest.getFieldValues("noUses").get(0);
        noUse.addFieldValue("name", "testname");
        String manifestId = persistenceService.create(manifest, deployContext);
        
        BsonEntity queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity oldManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        BsonEntity oldNoUse = (BsonEntity) oldManifest.getFieldValues("noUses").get(0);
        String oldName = (String)oldNoUse.getFieldValues("name").get(0);
        Assert.assertEquals("testname", oldName);
        
        queryEntity = buildQueryEntity(oldNoUse.getBranchId(), oldNoUse.getMetaClass(), oldNoUse.getId());
        persistenceService.deleteField(queryEntity, "name", deployContext);
        
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        List<?> names = getManifest.getFieldValues("name");
        Assert.assertTrue(names.isEmpty());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void test15EmbedCreateCardinalityOne() {
        // Manifest -embedOne -> NoUse -json-> noUseProperties
        MetaClass manifestMeta = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        BsonEntity manifest = new BsonEntity(manifestMeta);
        manifest.setBranchId(BRANCH_TEST);
        manifest.addFieldValue("name", "manifest-feild-test");
        String manifestId = persistenceService.create(manifest, deployContext);
        
        BsonEntity queryEntity = buildQueryEntity(manifest.getBranchId(), manifest.getMetaClass(), manifestId);
        BsonEntity oldManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        List<?> oldNoUses = oldManifest.getFieldValues("noUses");
        Assert.assertTrue(oldNoUses.isEmpty());

        BsonEntity noUse = newNoUse(seq++);
        noUse.setEmbedPath(EntityIDHelper.getInstance().generateEmbedPath(
                DeploymentEntityGenerator.TypeEnum.Manifest.name(), manifestId, manifestMeta.getFieldByName("noUses")));
        
        String noUseId = persistenceService.create(noUse, deployContext);
        
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, deployContext);
        List<BsonEntity> noUses = (List<BsonEntity>) getManifest.getFieldValues("noUses");
        Assert.assertEquals(1, noUses.size());
        Assert.assertEquals(noUseId, noUses.get(0).getId());
    }
    
    private BsonEntity newEmployeeEntity(int seq) {
        MetaClass meta = raptorMetaService.getMetaClass(EMPLOYEE);
        MetaAttribute companyField = (MetaAttribute) meta.getFieldByName(COMPANY);
        Assert.assertTrue(companyField.isConstant());
        BsonEntity entity = new BsonEntity(meta);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", "employee-test-" + seq);
        entity.addFieldValue(COMPANY, "ebay-cloud");
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }

    @Test
    public void test13ModifyConstantField() {
        BsonEntity employee = newEmployeeEntity(seq++);
        String newId = persistenceService.create(employee, raptorContext);

        MetaClass metaClass = raptorMetaService.getMetaClass(EMPLOYEE);
        BsonEntity modifyEntity = new BsonEntity(metaClass);
        modifyEntity.setBranchId(employee.getBranchId());
        modifyEntity.setId(newId);
        modifyEntity.addFieldValue(COMPANY, "new company");
        try {
            persistenceService.modifyField(modifyEntity, COMPANY, raptorContext);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private static BsonEntity buildQueryEntity(String branchname, MetaClass meta, String oid) {
        BsonEntity queryEntity = new BsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

}
