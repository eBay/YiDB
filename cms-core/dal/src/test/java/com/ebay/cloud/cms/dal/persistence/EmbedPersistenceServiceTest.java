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

package com.ebay.cloud.cms.dal.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.dal.common.DeploymentEntityGenerator;
import com.ebay.cloud.cms.dal.common.DummyEntity;
import com.ebay.cloud.cms.dal.common.TestDataLoader;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.DalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.utils.TestUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

@SuppressWarnings("static-access")
public class EmbedPersistenceServiceTest extends CMSMongoTest{

	private static IPersistenceService persistenceService = null;
	private static IRepositoryService repoService = null;
	private static IMetadataService deployMetaService = null;
	
	private static PersistenceContext manifestContext = null;
	
	private static final String DEPLOYMENT= "software-deployment";
	private static final String BRANCH_TEST = "test";
	
	
	private static MetadataDataLoader metaLoader = null;
	private static String testManifestId = null;
	private static TestDataLoader dataLoader = null;
	
	private static final String baseName = "EmbedTest";
    private static final EntityIDHelper entityIDHelper = EntityIDHelper.getInstance();
	

	@BeforeClass
	public static void setUp(){
		
		String connectionString = CMSMongoTest.getConnectionString();
		MongoDataSource dataSource = new MongoDataSource(connectionString);
		metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadTestDataFromResource();
		repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		deployMetaService = repoService.getRepository(DEPLOYMENT).getMetadataService();
		persistenceService = new PersistenceServiceImpl(dataSource);
		
        manifestContext = new PersistenceContext(deployMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST + "_base");
        manifestContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));

		dataLoader = TestDataLoader.getDataLoader(dataSource, DEPLOYMENT);
		
		List<BsonEntity> dummyList = new ArrayList<BsonEntity>();
		BsonEntity manifestEntity = DeploymentEntityGenerator.generateManifest(baseName, BRANCH_TEST,dummyList);
		dataLoader.load(dummyList, DBCollectionPolicy.SplitByMetadata);
		
		testManifestId = manifestEntity.getId();
		
		verifyTestData();
	}
	
	private static void verifyTestData() {
		List<BsonEntity> dummyList = new ArrayList<BsonEntity>();
		BsonEntity createManifestEntity = DeploymentEntityGenerator.generateManifest(baseName, BRANCH_TEST,dummyList);
		for (BsonEntity be : dummyList) {
			persistenceService.create(be, manifestContext);
		}
		String createdManifestId = createManifestEntity.getId();
		
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
        BsonEntity manifestEntity = (BsonEntity) persistenceService.get(queryEntity, manifestContext);

		@SuppressWarnings("unchecked")
		List<BsonEntity> versionList = (List<BsonEntity>)manifestEntity.getFieldValues("versions");
		assertTrue(versionList.size()>0);
		
		BsonEntity manifestVersion = versionList.get(0);
		
		String  embedId = manifestVersion.getId();
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getEntity != null);
		
		String getId = getEntity.getId();
		assertEquals(embedId, getId);
		
		String name = (String) getEntity.getFieldValues("name").get(0);
		String expectedName = "Dummy ManifestVersion " + baseName + "-0001";
		assertEquals(expectedName,name);
		assertEquals(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), getEntity.getType());
		assertEquals(0, getEntity.getVersion()); //make sure version has been back filled into entity
		assertEquals(DEPLOYMENT, getEntity.getRepositoryName());
		assertEquals(BRANCH_TEST, getEntity.getBranchId());
		assertEquals(StatusEnum.ACTIVE, getEntity.getStatus());
	}
	
    @Test(expected = CmsDalException.class)
	public void test01CreatEmbedWithVersion() {
        // test create
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion
                .name(), BRANCH_TEST, "New Dummy Version");

        MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        MetaField versionField = manifestClass.getFieldByName("versions");
        versionInst.setEmbedPath(entityIDHelper.generateEmbedPath(
                DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId, versionField));
        versionInst.setVersion(100);
        persistenceService.create(versionInst, manifestContext);
    }
	
	@Test
	public void test01ModifyAndCheckEmbedFieldVersion(){
		List<BsonEntity> dummyList = new ArrayList<BsonEntity>();
		BsonEntity createManifestEntity = DeploymentEntityGenerator.generateManifest(baseName, BRANCH_TEST,dummyList);
		for (BsonEntity be : dummyList) {
			persistenceService.create(be, manifestContext);
		}
		String createdManifestId = createManifestEntity.getId();

		BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		BsonEntity manifestEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);

		@SuppressWarnings("unchecked")
		List<BsonEntity> versionList = (List<BsonEntity>)manifestEntity.getFieldValues("versions");
		int manifestVersionCount = versionList.size();
		assertTrue(manifestVersionCount>0);
		int rootVersion = manifestEntity.getVersion();
		assertEquals(0,rootVersion);
		
		//retrieve the embed document manifest version
		BsonEntity manifestVersion = versionList.get(0);
		String  embedId = manifestVersion.getId();
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		BsonEntity getEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(getEntity != null);		
		String getId = getEntity.getId();
		assertEquals(embedId, getId);
		int embedVersion = getEntity.getVersion();
		assertEquals(rootVersion, embedVersion); //embed document should have same version as root document
		
		final String nameField =  "name";
		String name = (String) getEntity.getFieldValues(nameField).get(0);
		String expectedName = "Dummy ManifestVersion " + baseName + "-0001";
		assertEquals(expectedName,name);
		//let's modify the name filed of 1st embed manifest version
		String modifiedName = "Dummy ManifestVersion " + baseName + "-0001-modify";
		getEntity.addFieldValue(nameField, modifiedName);
		//!!
		getEntity.setVersion(0); //
		persistenceService.modify(getEntity, manifestContext);

		//check both root and embed document's version should match
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		BsonEntity rootEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(rootEntity!=null);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		BsonEntity embedEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(embedEntity!=null);
		int preVersion = rootVersion;
		rootVersion  = rootEntity.getVersion();
		embedVersion = embedEntity.getVersion();
		assertEquals(preVersion+1,rootVersion);
		assertEquals(rootVersion, embedVersion);
		
		//one more time
		//remove version field
		MetaClass embedMetaClass = embedEntity.getMetaClass();
		Collection<String> filedNames = embedMetaClass.getFieldNames();
		assertTrue(filedNames.size()>0);
		embedEntity.removeField(InternalFieldEnum.VERSION.getName());
		persistenceService.modify(embedEntity, manifestContext);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		rootEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(rootEntity!=null);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		embedEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(embedEntity!=null);
		preVersion = rootVersion;
		rootVersion  = rootEntity.getVersion();
		embedVersion = embedEntity.getVersion();
		assertEquals(preVersion+1,rootVersion);
		assertEquals(rootVersion, embedVersion);
	}

	private BsonEntity createEmbedVersion() {
		//test create
		DummyEntity helper = new DummyEntity(deployMetaService);
		BsonEntity versionInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
				BRANCH_TEST, "New Dummy Version");
		
		MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
		MetaField versionField = manifestClass.getFieldByName("versions");
		versionInst.setEmbedPath(entityIDHelper.generateEmbedPath(DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId, versionField));		

		versionInst.removeField(InternalFieldEnum.VERSION.getName());

		String newVersionId = persistenceService.create(versionInst, manifestContext);
		BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), newVersionId);
        BsonEntity getVersionEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		return getVersionEntity;
	}
    
    @Test
    public void testCreateWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");

        BsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        BsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest.addFieldValue("versions", versionInst1);
        manifest.addFieldValue("versions", versionInst2);
        
        try {
            persistenceService.create(manifest, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.DUPLICATE_REFERENCE, e.getErrorEnum());
        }
    }
    
    @Test
    public void testCreateWithoutMandatoryFieldInEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.removeField("createdTime");
        versionInst1.setId("mandatoryId");
        
        BsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest");
        manifest.addFieldValue("versions", versionInst1);
        
        try {
            persistenceService.create(manifest, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, e.getErrorEnum());
        }
    }
    
    @Test
    public void testCreateWithInvalidEmbedId() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("Manifest!manifest-1!versions!mandatoryId");
        
        BsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest");
        manifest.addFieldValue("versions", versionInst1);
        try {
        	persistenceService.create(manifest, manifestContext);
        	Assert.fail();
        } catch (CmsDalException e) {
        	Assert.assertEquals(DalErrCodeEnum.INVALID_EMBED_ID, e.getErrorEnum());
        }
    }
    
    @Test
    public void test02CreateAndGet() {
	    BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity getManifest =(BsonEntity) persistenceService.get(queryEntity, manifestContext);
        List<?> oldVersions = getManifest.getFieldValues("versions");
        Integer oldVersionLength = 0;
        if (!oldVersions.isEmpty()) {
            oldVersionLength = getManifest.getFieldLength("versions");
            Assert.assertEquals(oldVersionLength, getManifest.getFieldLength("versions"));
        }
	    
		IEntity getVersionEntity = createEmbedVersion();
		assertTrue(getVersionEntity!=null);
		assertEquals(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(),getVersionEntity.getType());
		assertTrue(getVersionEntity.hasField(InternalFieldEnum.VERSION.getName()));
		
		getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        Integer newVersionLength = 0;
        newVersionLength = getManifest.getFieldLength("versions");
        Assert.assertEquals(newVersionLength, getManifest.getFieldLength("versions"));
        Assert.assertEquals(1, newVersionLength - oldVersionLength);
    }

    @Test
    public void verifyCreatedEmbed() {
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity manifestEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        Assert.assertNotNull(manifestEntity.getFieldValues("versions"));
        Assert.assertTrue(manifestEntity.getFieldValues("versions").size() > 0);
        Assert.assertTrue(((BsonEntity) manifestEntity.getFieldValues("versions").get(0)).getId().contains(
                testManifestId));
    }

    /**
     * Modify root entity to add embed entity should also have the embed entity id encoded
     */
    @Test
    @SuppressWarnings("unchecked")
    public void modifyToAddEmbedEntity() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity newVersionEntity = helper.newEntityWithDummyValues(
                DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), BRANCH_TEST,
                "New Dummy Version Modify Embed");
        newVersionEntity.setId("/customized/new/add/embed/id");
        String customId = newVersionEntity.getId();

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity manifestEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        manifestEntity.addFieldValue("versions", newVersionEntity);

        persistenceService.modify(manifestEntity, manifestContext);

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity getManifest = (BsonEntity) persistenceService.get(queryEntity, manifestContext);
        Assert.assertTrue(getManifest.getVersion() > manifestEntity.getVersion());
        System.out.println(getManifest.getVersion() > manifestEntity.getVersion());
        List<BsonEntity> versionEntities = (List<BsonEntity>) getManifest.getFieldValues("versions");
        Assert.assertNotNull(versionEntities);
        Assert.assertTrue(versionEntities.size() > 0);
        Assert.assertTrue(versionEntities.size() == manifestEntity.getFieldValues("versions").size());
        Assert.assertEquals(versionEntities.get(0).getId(), ((List<BsonEntity>)manifestEntity.getFieldValues("versions")).get(0).getId());
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().startsWith(manifestEntity.getMetaClass().getName()));
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().contains(customId));
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().contains(getManifest.getId()));
    }
	
	@Test
	public void test03ModifyEmbed(){
		BsonEntity newVersionEntity = createEmbedVersion();
		String newId = newVersionEntity.getId();
		int currentVersion = newVersionEntity.getVersion();

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), newId);
		String modifiedName = "Modified Version 1";
		queryEntity.addFieldValue("name", modifiedName);
		
        persistenceService.modify(queryEntity, manifestContext);
        
        IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
		assertEquals(currentVersion+1, getEntity.getVersion());
		assertEquals(modifiedName, getEntity.getFieldValues("name").get(0));
		
	}
	
	@Test (expected=CmsDalException.class)
    public void test04ModifyEmbedVersionWrong() {
        BsonEntity newVersionEntity = createEmbedVersion();
        int currentVersion = newVersionEntity.getVersion();
        String modifiedName = "Modified Version 1";

        newVersionEntity.addFieldValue("name", modifiedName);
        newVersionEntity.setVersion(currentVersion + 1);
        persistenceService.modify(newVersionEntity, manifestContext);
    }
	
    @Test
    public void testModifyWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");
        
        BsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        BsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        BsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest2.setId(id);
        manifest2.addFieldValue("versions", versionInst1);
        manifest2.addFieldValue("versions", versionInst2);
        
        try {
            persistenceService.modify(manifest2, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.DUPLICATE_REFERENCE, e.getErrorEnum());
        }
    }
    
    @Test
    public void testModifyeWithoutMandatoryFieldInEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("mandatoryId");
        
        BsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest");
        manifest.addFieldValue("versions", versionInst1);
        persistenceService.create(manifest, manifestContext);
        
        versionInst1.removeField("createdTime");
        
        try {
            persistenceService.modify(manifest, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, e.getErrorEnum());
        }
    }
    
    @Test
    public void testModifyFieldWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version-1");
        versionInst1.setId("dupId");
        
        BsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest-1");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        BsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version-2");
        versionInst2.setId("dupId");
        
        BsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest-2");
        manifest2.setId(id);
        manifest2.addFieldValue("versions", versionInst1);
        manifest2.addFieldValue("versions", versionInst2);
        
        try {
            persistenceService.modifyField(manifest2, "versions", manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.DUPLICATE_REFERENCE, e.getErrorEnum());
        }
    }
	
	@Test(expected = CmsDalException.class)
    public void test04ReplaceEmbedVersionWrong() {
        BsonEntity newVersionEntity = createEmbedVersion();
        int currentVersion = newVersionEntity.getVersion();
        String modifiedName = "Modified Version 1";

        newVersionEntity.addFieldValue("name", modifiedName);
        newVersionEntity.setVersion(currentVersion + 1);
        persistenceService.replace(newVersionEntity, manifestContext);
    }
	
	@Test
	public void test05ReplaceEmbed(){
		BsonEntity newVersionEntity = createEmbedVersion();
		String branchId = newVersionEntity.getBranchId();
		String versionId = newVersionEntity.getId();
		int currentVersion = newVersionEntity.getVersion();
		String replaceName = "Replace Dummy Manifest Version";
		newVersionEntity.addFieldValue("name", replaceName);
		DummyEntity helper = new DummyEntity(deployMetaService);
		int packageCount = 3;
		int packageIndex = 0;
		for(packageIndex = 0; packageIndex < packageCount; packageIndex++){
			String packageName = "Replace Dummy Package " + packageIndex;
			IEntity packageEntity = helper.newEntity("Package",BRANCH_TEST, packageName);
			packageEntity.setId(ObjectId.get().toString());
			//FIXME: what if package entity is embed document, who's responsible to populate the correct
			//embed document id
			newVersionEntity.addFieldValue("packages", packageEntity);
		}
		
        persistenceService.replace(newVersionEntity, manifestContext);
        BsonEntity queryEntity = buildQueryEntity(branchId, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), versionId);
        IEntity getVersionEntity = persistenceService.get(queryEntity, manifestContext);
		assertEquals(currentVersion+1, getVersionEntity.getVersion());
		assertEquals(replaceName, getVersionEntity.getFieldValues("name").get(0));
		
		@SuppressWarnings("unchecked")
		List<IEntity> packageRefs = (List<IEntity>)getVersionEntity.getFieldValues("packages");
		assertEquals(packageCount, packageRefs.size());
		for(packageIndex = 0; packageIndex < packageCount; packageIndex++){
			IEntity packageRef = packageRefs.get(packageIndex);
			assertFalse(packageRef.hasField("name"));
		}
	}
	
    @Test
    public void testReplaceWithoutMandatoryFieldInEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("mandatoryId");
        
        BsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest");
        manifest.addFieldValue("versions", versionInst1);
        
        persistenceService.create(manifest, manifestContext);
        
        versionInst1.removeField("createdTime");
        manifest.addFieldValue("versions", versionInst1);
        
        try {
            persistenceService.replace(manifest, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, e.getErrorEnum());
        }
    }
	
    @Test
    public void testReplaceWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        BsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");
        
        BsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        BsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        BsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest2.setId(id);
        manifest2.addFieldValue("versions", versionInst1);
        manifest2.addFieldValue("versions", versionInst2);
        
        try {
            persistenceService.replace(manifest2, manifestContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.DUPLICATE_REFERENCE, e.getErrorEnum());
        }
    }
	
	@Test
    public void test06DeleteEmbed() {
        BsonEntity newVersionEntity = createEmbedVersion();
        // assert the _length is set correctly
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        Integer oldVersionLength = 0;
        oldVersionLength = getManifest.getFieldLength("versions");
        Assert.assertEquals(oldVersionLength, getManifest.getFieldLength("versions"));

        String branchId = newVersionEntity.getBranchId();
        String newId = newVersionEntity.getId();
        String type = newVersionEntity.getType();
        persistenceService.delete(newVersionEntity, manifestContext);
        queryEntity = buildQueryEntity(branchId, type, newId);
        IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
        assertTrue(getEntity == null);

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        List<?> newVersions = getManifest.getFieldValues("versions");
        int newVersionLength = getManifest.getFieldLength("versions");
        Assert.assertEquals(newVersionLength, newVersions.size());
        // assert the _length in underlying database is update correctly
        Assert.assertEquals(1, oldVersionLength - newVersionLength);
    }
	
	private BsonEntity createEmbedApproval(){
	    BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
		IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
		int originalVersion = manifestEntity.getVersion();
		
		DummyEntity helper = new DummyEntity(deployMetaService);
		BsonEntity approvalInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Approval.name(),
				BRANCH_TEST, "New Dummy Approval");
		
		@SuppressWarnings("unchecked")
		List<BsonEntity> versionList = (List<BsonEntity>)manifestEntity.getFieldValues("versions");
		assertTrue(versionList.size()>0);
		
		BsonEntity manifestVersion = versionList.get(0);
		String  manifestVersionId = manifestVersion.getId();
		
		MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        MetaField approvalField = manifestClass.getFieldByName("approvals");
		approvalInst.setEmbedPath(entityIDHelper.generateEmbedPath(DeploymentEntityGenerator.TypeEnum.Manifest.name(), manifestVersionId, approvalField));
		approvalInst.removeField(InternalFieldEnum.VERSION.getName());
		String newApprovalId = persistenceService.create(approvalInst, manifestContext);
		
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newApprovalId);
	    BsonEntity getApprovalEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
	    assertEquals(originalVersion + 1, getApprovalEntity.getVersion());
	    assertEquals(newApprovalId, getApprovalEntity.getId());
	    return getApprovalEntity;
	}
	
	@Test
	public void test07CreateAndGetWithNestedArray(){
		IEntity getApprovalEntity = createEmbedApproval();
		assertTrue(getApprovalEntity!=null);
		assertEquals(getApprovalEntity.getType(),DeploymentEntityGenerator.TypeEnum.Approval.name());
	}
	
	@Test
	public void test08ModifyEmbedWithNestedArray(){
		IEntity approvalInst = createEmbedApproval();
		String newId = approvalInst.getId();
		int originalVersion = approvalInst.getVersion();
		MetaClass approvalCls = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Approval.name());
		BsonEntity modifyInst = new BsonEntity(approvalCls);
		modifyInst.setId(newId);
		modifyInst.setBranchId(BRANCH_TEST);
		
		String modifiedName = "Modified Dummy Approval";
		modifyInst.addFieldValue("name", modifiedName);

		persistenceService.modify(modifyInst, manifestContext);
		
		BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newId);
		IEntity getModifiedInst = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getModifiedInst!=null);
		assertEquals(newId, getModifiedInst.getId());
		assertEquals(DeploymentEntityGenerator.TypeEnum.Approval.name(), getModifiedInst.getType());
		assertEquals(originalVersion+1, getModifiedInst.getVersion());
		assertEquals(modifiedName, getModifiedInst.getFieldValues("name").get(0));
		assertEquals(StatusEnum.ACTIVE, getModifiedInst.getStatus());
	
	}
	
	@Test (expected = CmsDalException.class)
	public void test09ModifyEmbedWithNestedArrayWrongVersion(){
		IEntity approvalInst = createEmbedApproval();
		String newId = approvalInst.getId();
		int originalVersion = approvalInst.getVersion();
		MetaClass approvalCls = deployMetaService.getMetaClass("Approval");
		BsonEntity modifyInst = new BsonEntity(approvalCls);
		modifyInst.setId(newId);
		modifyInst.setBranchId(BRANCH_TEST);
		modifyInst.setVersion(originalVersion + 1);
		
		String modifiedName = "Modified Dummy Approval";
		modifyInst.addFieldValue("name", modifiedName);
		
		persistenceService.modify(modifyInst, manifestContext);
	}
	
	@Test
	public void test10ReplaceEmbedWithNestedArray(){
		BsonEntity approvalInst = createEmbedApproval();
		String newId = approvalInst.getId();
		int originalVersion = approvalInst.getVersion();
		
		String replacedName = "Modified Replaced Approval";
		approvalInst.addFieldValue("name", replacedName);
		
		persistenceService.replace(approvalInst, manifestContext);
		
		BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newId);
		BsonEntity getModifiedInst = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(getModifiedInst!=null);
		assertEquals(newId, getModifiedInst.getId());
		assertEquals("Approval", getModifiedInst.getType());
		assertEquals(originalVersion+1, getModifiedInst.getVersion());
		assertEquals(replacedName, getModifiedInst.getFieldValues("name").get(0));
		assertEquals(StatusEnum.ACTIVE, getModifiedInst.getStatus());
	}
	
	@Test
	public void test11DeleteEmbedWithNestedArray(){
		BsonEntity approvalInst = createEmbedApproval();
		String embedId = approvalInst.getId();
        persistenceService.delete(approvalInst, manifestContext);
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		BsonEntity getEntity = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(getEntity==null);
	}
	
	@Test
    public void test12GetParentAlreadyDeletedEmbed() {
		BsonEntity approvalInst = createEmbedApproval();
		String embedId = approvalInst.getId();
		
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		BsonEntity getApprovalInst = (BsonEntity) persistenceService.get(queryEntity,manifestContext);
		assertTrue(getApprovalInst != null);
		
		String parentId = entityIDHelper.getParentId(embedId);
		MetaClass metaVersion = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
		BsonEntity manifestVersion = new BsonEntity(metaVersion);
        manifestVersion.setBranchId(BRANCH_TEST);
        manifestVersion.setId(parentId);
        //now delete parent document
        persistenceService.delete(manifestVersion, manifestContext);
        
        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		getApprovalInst = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
		assertTrue(getApprovalInst == null);
	}
	

	@Test
    public void test13DeleteAlreadyDeletedEmbed() {
        BsonEntity approvalInst = createEmbedApproval();
        persistenceService.delete(approvalInst, manifestContext);
        persistenceService.delete(approvalInst, manifestContext);
	}

    @Test(expected = CmsDalException.class)
    public void test14DeleteParentAlreadyDeletedEmbed() {
        BsonEntity approvalInst = createEmbedApproval();
        String embedId = approvalInst.getId();
        String parentId = entityIDHelper.getParentId(embedId);

        MetaClass metaVersion = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        BsonEntity manifestVersion = new BsonEntity(metaVersion);
        manifestVersion.setBranchId(BRANCH_TEST);
        manifestVersion.setId(parentId);

        persistenceService.delete(manifestVersion, manifestContext);
        persistenceService.delete(approvalInst, manifestContext);
    }
	
    @Test
    public void test15CreateAndFilterGetEmbed() {
        createEmbedVersion();
        createEmbedApproval();

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity manifestEntity = (BsonEntity) persistenceService.get(queryEntity, manifestContext);
        BsonEntity versionEntity = ((BsonEntity) manifestEntity.getFieldValues("versions").get(0));
        Assert.assertTrue(versionEntity.getFieldValues("approvals").size() > 0);
        Assert.assertNotNull(versionEntity.getLastModified());

        String versioId = versionEntity.getId();

        MetaClass metadata = versionEntity.getMetaClass();
        PersistenceContext pc = new PersistenceContext(deployMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST + "_base");
        pc.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
        pc.addQueryField(InternalFieldEnum.ID.getDbName());
        pc.addQueryField(metadata.getFieldByName("approvals").getDbName());

        queryEntity = buildQueryEntity(BRANCH_TEST, metadata.getName(), versioId);
        IEntity filterEntiy = persistenceService.get(queryEntity, pc);
        Assert.assertNotNull(filterEntiy);
        Assert.assertNotNull(filterEntiy.getId());
        // type is always returned
        Assert.assertNotNull(filterEntiy.getType());
        Assert.assertNull(filterEntiy.getLastModified());
        Assert.assertTrue(filterEntiy.getFieldValues("approvals").size() == versionEntity.getFieldValues("approvals")
                .size());
        Assert.assertNull(filterEntiy.getLastModified());
    }

    @Test
    public void test16DeleteMandatoryField() {
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity manifestEntity = (BsonEntity) persistenceService.get(queryEntity, manifestContext);
        BsonEntity entity = new BsonEntity(manifestEntity.getMetaClass());
        entity.setBranchId(manifestEntity.getBranchId());
        entity.setId(manifestEntity.getId());
        try {
            persistenceService.deleteField(entity, "lastModifiedTime", manifestContext);
            Assert.fail();
        } catch (IllegalArgumentException illegalException) {
            // expected
            Assert.assertTrue(illegalException.getMessage().contains("mandatory field"));
        }
    }

    @Test
    public void test17EmbedCreateLength() {
        BsonEntity approvalInst = createEmbedApproval();
        String embedId = approvalInst.getId();
        String parentId = entityIDHelper.getParentId(embedId);

        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), parentId);
        BsonEntity getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        
        Integer len = getManifest.getFieldLength("approvals");
        List<?> approvals = getManifest.getFieldValues("approvals");
        Assert.assertEquals(len.intValue(), approvals.size());
        
        createEmbedApproval();

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), parentId);
        getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        
        len = getManifest.getFieldLength("approvals");
        approvals = getManifest.getFieldValues("approvals");
        Assert.assertEquals(len.intValue(), approvals.size());
    }
    
    private static BsonEntity buildQueryEntity(String branchname, String metadata, String oid) {
        MetaClass meta = deployMetaService.getMetaClass(metadata);
        BsonEntity queryEntity = new BsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

    @Test
    public void testContextAdditionalFilter() {
        BsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        BsonEntity getManifest =(BsonEntity) persistenceService.get(queryEntity, manifestContext);
        List<?> oldVersions = getManifest.getFieldValues("versions");
        Integer oldVersionLength = 0;
        if (!oldVersions.isEmpty()) {
            oldVersionLength = getManifest.getFieldLength("versions");
            Assert.assertEquals(oldVersionLength, getManifest.getFieldLength("versions"));
        }
        
        IEntity getVersionEntity = createEmbedVersion();
        assertTrue(getVersionEntity!=null);
        assertEquals(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(),getVersionEntity.getType());
        assertTrue(getVersionEntity.hasField(InternalFieldEnum.VERSION.getName()));
        
        getManifest = (BsonEntity)persistenceService.get(queryEntity, manifestContext);
        Integer newVersionLength = 0;
        newVersionLength = getManifest.getFieldLength("versions");
        Assert.assertEquals(newVersionLength, getManifest.getFieldLength("versions"));
        Assert.assertEquals(1, newVersionLength - oldVersionLength);

        // test get with read filter : root get
        PersistenceContext context = new PersistenceContext(manifestContext);
        Map<String, List<SearchCriteria>> additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        MetaClass metaClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        MetaField nameField = metaClass.getFieldByName("name");
        ISearchField searchField = new SelectionField(nameField, DalSearchStrategy.getInstance());
        SearchCriteria criteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.NE, getManifest
                .getFieldValues("name").get(0));
        additionalCriteria.put(DeploymentEntityGenerator.TypeEnum.Manifest.name(), Arrays.asList(criteria));

        context.setAdditionalCriteria(additionalCriteria);
        
        BsonEntity getEntity2 = (BsonEntity)persistenceService.get(queryEntity, context);
        Assert.assertNull(getEntity2);
        
        // test get with root filter : embed get

    }
}
