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

import com.ebay.cloud.cms.dal.common.flatten.DeploymentEntityGenerator;
import com.ebay.cloud.cms.dal.common.flatten.DummyEntity;
import com.ebay.cloud.cms.dal.common.flatten.TestDataLoader;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewCollectionFinder;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

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
	private static DBCollectionPolicy poilcy = DBCollectionPolicy.SplitByMetadata;
	
	private static final String baseName = "EmbedTest";
	

	@BeforeClass
	public static void setUp(){
		
		String connectionString = CMSMongoTest.getConnectionString();
		MongoDataSource dataSource = new MongoDataSource(connectionString);
		metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadTestDataFromResource();
		repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		deployMetaService = repoService.getRepository(DEPLOYMENT).getMetadataService();
		persistenceService = new NewPersistenceServiceImpl(dataSource);
		
        manifestContext = new PersistenceContext(deployMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST + "_base");
        manifestContext.setCollectionFinder(new NewCollectionFinder());

		dataLoader = TestDataLoader.getDataLoader(dataSource, DEPLOYMENT);
		
		List<NewBsonEntity> dummyList = new ArrayList<NewBsonEntity>();
		NewBsonEntity manifestEntity = DeploymentEntityGenerator.generateManifest(poilcy, dataLoader, baseName, BRANCH_TEST,dummyList);
		
		testManifestId = manifestEntity.getId();
		
		verifyTestData();
	}
	
	private static void verifyTestData() {
		String createdManifestId = testManifestId;
		
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
        IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);

		@SuppressWarnings("unchecked")
		List<NewBsonEntity> versionList = (List<NewBsonEntity>)manifestEntity.getFieldValues("versions");
		assertTrue(versionList.size()>0);
		
		NewBsonEntity manifestVersion = versionList.get(0);
		
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
        NewBsonEntity versionInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion
                .name(), BRANCH_TEST, "New Dummy Version");

        MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        MetaField versionField = manifestClass.getFieldByName("versions");
        versionInst.setEmbedPath(FlattenEntityIDHelper.generateEmbedPath(
                DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId, versionField));
        versionInst.setVersion(100);
        persistenceService.create(versionInst, manifestContext);
    }
	
	@Test
	public void test01ModifyAndCheckEmbedFieldVersion(){
		String createdManifestId = testManifestId;

		NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);

		@SuppressWarnings("unchecked")
		List<NewBsonEntity> versionList = (List<NewBsonEntity>)manifestEntity.getFieldValues("versions");
		int manifestVersionCount = versionList.size();
		assertTrue(manifestVersionCount>0);
		int rootVersion = manifestEntity.getVersion();
		assertTrue(rootVersion >=0);
		
		//retrieve the embed document manifest version
		NewBsonEntity manifestVersion = versionList.get(0);
		String  embedId = manifestVersion.getId();
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getEntity != null);		
		String getId = getEntity.getId();
		assertEquals(embedId, getId);
		int embedVersion = getEntity.getVersion();
		assertEquals(rootVersion, embedVersion); //embed document should have same version as root document
		
		//http://jirap.corp.ebay.com/browse/CMS-1809
		final String nameField =  "name";
		String name = (String) getEntity.getFieldValues(nameField).get(0);
		//let's modify the name filed of 1st embed manifest version
		String modifiedName = "Dummy ManifestVersion " + baseName + "-0001-modify";
		assertFalse(modifiedName.equals(name));
		getEntity.addFieldValue(nameField, modifiedName);
		//!!
		getEntity.setVersion(rootVersion); //
		persistenceService.modify(getEntity, manifestContext);

		//check both root and embed document's version should match
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		IEntity rootEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(rootEntity!=null);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		IEntity embedEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(embedEntity!=null);
		assertEquals(modifiedName, embedEntity.getFieldValues(nameField).get(0));
		int preVersion = rootVersion;
		rootVersion  = rootEntity.getVersion();
		embedVersion = embedEntity.getVersion();
		assertEquals(preVersion+1,rootVersion);
		assertEquals(rootVersion, embedVersion);
		
		//one more time
		//http://jirap.corp.ebay.com/browse/CMS-1808
		//remove version field
		MetaClass embedMetaClass = embedEntity.getMetaClass();
		Collection<String> filedNames = embedMetaClass.getFieldNames();
		assertTrue(filedNames.size()>0);
		embedEntity.removeField(InternalFieldEnum.VERSION.getName());
		persistenceService.modify(embedEntity, manifestContext);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), createdManifestId);
		rootEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(rootEntity!=null);
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), embedId);
		embedEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(embedEntity!=null);
		preVersion = rootVersion;
		rootVersion  = rootEntity.getVersion();
		embedVersion = embedEntity.getVersion();
		assertEquals(preVersion+1,rootVersion);
		assertEquals(rootVersion, embedVersion);
	}

	private IEntity createEmbedVersion() {
		//test create
		DummyEntity helper = new DummyEntity(deployMetaService);
		NewBsonEntity versionInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
				BRANCH_TEST, "New Dummy Version");
		
		MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
		MetaField versionField = manifestClass.getFieldByName("versions");
        versionInst.setEmbedPath(FlattenEntityIDHelper.generateEmbedPath(
                DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId, versionField));		

		versionInst.removeField(InternalFieldEnum.VERSION.getName());

		String newVersionId = persistenceService.create(versionInst, manifestContext);
		NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), newVersionId);
        IEntity getVersionEntity = persistenceService.get(queryEntity, manifestContext);
		return getVersionEntity;
	}
    
    @Test
    public void testCreateWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");

        NewBsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        NewBsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.removeField("createdTime");
        versionInst1.setId("mandatoryId");
        
        NewBsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("Manifest!manifest-1!versions!mandatoryId");
        
        NewBsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
	    NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity getManifest = persistenceService.get(queryEntity, manifestContext);
        List<?> oldVersions = getManifest.getFieldValues("versions");
        Integer oldVersionLength = 0;
        if (!oldVersions.isEmpty()) {
            oldVersionLength = oldVersions.size();
            Assert.assertEquals(oldVersionLength, getManifest.getFieldProperty("versions", FieldProperty.LENGTH.getName()));
        }
	    
		IEntity getVersionEntity = createEmbedVersion();
		assertTrue(getVersionEntity!=null);
		assertEquals(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(),getVersionEntity.getType());
		assertTrue(getVersionEntity.hasField(InternalFieldEnum.VERSION.getName()));
		
		getManifest = persistenceService.get(queryEntity, manifestContext);
        Integer newVersionLength = 0;
        newVersionLength = getManifest.getFieldValues("versions").size();
        Assert.assertEquals(newVersionLength, getManifest.getFieldProperty("versions", FieldProperty.LENGTH.getName()));
        Assert.assertEquals(1, newVersionLength - oldVersionLength);
    }

    @Test
    public void verifyCreatedEmbed() {
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
        Assert.assertNotNull(manifestEntity.getFieldValues("versions"));
        Assert.assertTrue(manifestEntity.getFieldValues("versions").size() > 0);
        Assert.assertTrue(((NewBsonEntity) manifestEntity.getFieldValues("versions").get(0)).getId().contains(
                testManifestId));
    }

    /**
     * Modify root entity to add embed entity should also have the embed entity id encoded
     */
    @Test
    @SuppressWarnings("unchecked")
    public void modifyToAddEmbedEntity() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        NewBsonEntity newVersionEntity = helper.newEntityWithDummyValues(
                DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), BRANCH_TEST,
                "New Dummy Version Modify Embed");
        newVersionEntity.setId("/customized/new/add/embed/id");
        String customId = newVersionEntity.getId();

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
        manifestEntity.addFieldValue("versions", newVersionEntity);

        persistenceService.modify(manifestEntity, manifestContext);

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity getManifest = persistenceService.get(queryEntity, manifestContext);
        Assert.assertTrue(getManifest.getVersion() > manifestEntity.getVersion());
        System.out.println(getManifest.getVersion() > manifestEntity.getVersion());
        List<NewBsonEntity> versionEntities = (List<NewBsonEntity>) getManifest.getFieldValues("versions");
        Assert.assertNotNull(versionEntities);
        Assert.assertTrue(versionEntities.size() > 0);
        Assert.assertTrue(versionEntities.size() == manifestEntity.getFieldValues("versions").size());
        Assert.assertEquals(versionEntities.get(0).getId(), ((List<NewBsonEntity>)manifestEntity.getFieldValues("versions")).get(0).getId());
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().startsWith(manifestEntity.getMetaClass().getName()));
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().contains(customId));
        Assert.assertTrue(versionEntities.get(versionEntities.size() - 1).getId().contains(getManifest.getId()));
    }
	
	@Test
	public void test03ModifyEmbed(){
		IEntity newVersionEntity = createEmbedVersion();
		String newId = newVersionEntity.getId();
		int currentVersion = newVersionEntity.getVersion();

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), newId);
		String modifiedName = "Modified Version 1";
		queryEntity.addFieldValue("name", modifiedName);
		
        persistenceService.modify(queryEntity, manifestContext);
        
        IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
		assertEquals(currentVersion+1, getEntity.getVersion());
		assertEquals(modifiedName, getEntity.getFieldValues("name").get(0));
		
	}
	
	@Test (expected=CmsDalException.class)
    public void test04ModifyEmbedVersionWrong() {
        IEntity newVersionEntity = createEmbedVersion();
        int currentVersion = newVersionEntity.getVersion();
        String modifiedName = "Modified Version 1";

        newVersionEntity.addFieldValue("name", modifiedName);
        newVersionEntity.setVersion(currentVersion + 1);
        persistenceService.modify(newVersionEntity, manifestContext);
    }
	
    @Test
    public void testModifyWithDuplicatedEmbed() {
        DummyEntity helper = new DummyEntity(deployMetaService);
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");
        
        NewBsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        NewBsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        NewBsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("mandatoryId");
        
        NewBsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version-1");
        versionInst1.setId("dupId");
        
        NewBsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Manifest-1");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        NewBsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version-2");
        versionInst2.setId("dupId");
        
        NewBsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        IEntity newVersionEntity = createEmbedVersion();
        int currentVersion = newVersionEntity.getVersion();
        String modifiedName = "Modified Version 1";

        newVersionEntity.addFieldValue("name", modifiedName);
        newVersionEntity.setVersion(currentVersion + 1);
        persistenceService.replace(newVersionEntity, manifestContext);
    }
	
	@Test
	public void test05ReplaceEmbed(){
		IEntity newVersionEntity = createEmbedVersion();
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
        NewBsonEntity queryEntity = buildQueryEntity(branchId, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), versionId);
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("mandatoryId");
        
        NewBsonEntity manifest = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        NewBsonEntity versionInst1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst1.setId("dupId");
        
        NewBsonEntity manifest1 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
                BRANCH_TEST, "New Dummy Version");
        manifest1.addFieldValue("versions", versionInst1);
        
        String id = persistenceService.create(manifest1, manifestContext);

        NewBsonEntity versionInst2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), 
                BRANCH_TEST, "New Dummy Version");
        versionInst2.setId("dupId");
        
        NewBsonEntity manifest2 = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Manifest.name(), 
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
        IEntity newVersionEntity = createEmbedVersion();
        // assert the _length is set correctly
        IEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity getManifest = persistenceService.get(queryEntity, manifestContext);
        Integer oldVersionLength = 0;
        oldVersionLength = getManifest.getFieldValues("versions").size();
        Assert.assertEquals(oldVersionLength, getManifest.getFieldProperty("versions", FieldProperty.LENGTH.getName()));

        String branchId = newVersionEntity.getBranchId();
        String newId = newVersionEntity.getId();
        String type = newVersionEntity.getType();
        persistenceService.delete(newVersionEntity, manifestContext);
        queryEntity = buildQueryEntity(branchId, type, newId);
        IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
        assertTrue(getEntity == null);

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        getManifest = persistenceService.get(queryEntity, manifestContext);
        List<?> newVersions = getManifest.getFieldValues("versions");
        int newVersionLength = (Integer) getManifest.getFieldProperty("versions", FieldProperty.LENGTH.getName());
        Assert.assertEquals(newVersionLength, newVersions.size());
        // assert the _length in underlying database is update correctly
        Assert.assertEquals(1, oldVersionLength - newVersionLength);
    }
	
	private IEntity createEmbedApproval(){
	    NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
		IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
		int originalVersion = manifestEntity.getVersion();
		
		DummyEntity helper = new DummyEntity(deployMetaService);
		NewBsonEntity approvalInst = helper.newEntityWithDummyValues(DeploymentEntityGenerator.TypeEnum.Approval.name(),
				BRANCH_TEST, "New Dummy Approval");
		
		@SuppressWarnings("unchecked")
		List<NewBsonEntity> versionList = (List<NewBsonEntity>)manifestEntity.getFieldValues("versions");
		assertTrue(versionList.size()>0);
		
		NewBsonEntity manifestVersion = versionList.get(0);
		String  manifestVersionId = manifestVersion.getId();
		
		MetaClass manifestClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        MetaField approvalField = manifestClass.getFieldByName("approvals");
        approvalInst.setEmbedPath(FlattenEntityIDHelper.generateEmbedPath(
                DeploymentEntityGenerator.TypeEnum.Manifest.name(), manifestVersionId, approvalField));
		approvalInst.removeField(InternalFieldEnum.VERSION.getName());
		String newApprovalId = persistenceService.create(approvalInst, manifestContext);
		
		queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newApprovalId);
	    IEntity getApprovalEntity = persistenceService.get(queryEntity, manifestContext);
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
		NewBsonEntity modifyInst = new NewBsonEntity(approvalCls);
		modifyInst.setId(newId);
		modifyInst.setBranchId(BRANCH_TEST);
		
		String modifiedName = "Modified Dummy Approval";
		modifyInst.addFieldValue("name", modifiedName);

		persistenceService.modify(modifyInst, manifestContext);
		
		NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newId);
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
		NewBsonEntity modifyInst = new NewBsonEntity(approvalCls);
		modifyInst.setId(newId);
		modifyInst.setBranchId(BRANCH_TEST);
		modifyInst.setVersion(originalVersion + 1);
		
		String modifiedName = "Modified Dummy Approval";
		modifyInst.addFieldValue("name", modifiedName);
		
		persistenceService.modify(modifyInst, manifestContext);
	}
	
	@Test
	public void test10ReplaceEmbedWithNestedArray(){
		IEntity approvalInst = createEmbedApproval();
		String newId = approvalInst.getId();
		int originalVersion = approvalInst.getVersion();
		
		String replacedName = "Modified Replaced Approval";
		approvalInst.addFieldValue("name", replacedName);
		
		persistenceService.replace(approvalInst, manifestContext);
		
		NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), newId);
		IEntity getModifiedInst = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getModifiedInst!=null);
		assertEquals(newId, getModifiedInst.getId());
		assertEquals("Approval", getModifiedInst.getType());
		assertEquals(originalVersion+1, getModifiedInst.getVersion());
		assertEquals(replacedName, getModifiedInst.getFieldValues("name").get(0));
		assertEquals(StatusEnum.ACTIVE, getModifiedInst.getStatus());
	}
	
	@Test
	public void test11DeleteEmbedWithNestedArray(){
		IEntity approvalInst = createEmbedApproval();
		String embedId = approvalInst.getId();
        persistenceService.delete(approvalInst, manifestContext);
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		IEntity getEntity = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getEntity==null);
	}
	
	@Test
    public void test12GetParentAlreadyDeletedEmbed() {
		IEntity approvalInst = createEmbedApproval();
		String embedId = approvalInst.getId();
		
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		IEntity getApprovalInst = persistenceService.get(queryEntity,manifestContext);
		assertTrue(getApprovalInst != null);
		
		String parentId = FlattenEntityIDHelper.getParentId(embedId);
		MetaClass metaVersion = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
		NewBsonEntity manifestVersion = new NewBsonEntity(metaVersion);
        manifestVersion.setBranchId(BRANCH_TEST);
        manifestVersion.setId(parentId);
        //now delete parent document
        persistenceService.delete(manifestVersion, manifestContext);
        
        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Approval.name(), embedId);
		getApprovalInst = persistenceService.get(queryEntity, manifestContext);
		assertTrue(getApprovalInst == null);
	}
	

	@Test
    public void test13DeleteAlreadyDeletedEmbed() {
        IEntity approvalInst = createEmbedApproval();
        persistenceService.delete(approvalInst, manifestContext);
        persistenceService.delete(approvalInst, manifestContext);
	}

    @Test(expected = CmsDalException.class)
    public void test14DeleteParentAlreadyDeletedEmbed() {
        IEntity approvalInst = createEmbedApproval();
        String embedId = approvalInst.getId();
        String parentId = FlattenEntityIDHelper.getParentId(embedId);

        MetaClass metaVersion = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.ManifestVersion.name());
        NewBsonEntity manifestVersion = new NewBsonEntity(metaVersion);
        manifestVersion.setBranchId(BRANCH_TEST);
        manifestVersion.setId(parentId);

        persistenceService.delete(manifestVersion, manifestContext);
        persistenceService.delete(approvalInst, manifestContext);
    }
	
    @Test
    public void test15CreateAndFilterGetEmbed() {
        createEmbedVersion();
        createEmbedApproval();

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
        NewBsonEntity versionEntity = ((NewBsonEntity) manifestEntity.getFieldValues("versions").get(0));
        Assert.assertTrue(versionEntity.getFieldValues("approvals").size() > 0);
        Assert.assertNotNull(versionEntity.getLastModified());

        String versioId = versionEntity.getId();

        MetaClass versionMetadata = versionEntity.getMetaClass();
        PersistenceContext pc = new PersistenceContext(deployMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST + "_base");
        pc.addQueryField(InternalFieldEnum.ID.getDbName());
        pc.addQueryField(versionMetadata.getFieldByName("approvals").getFlattenValueDbName());

        queryEntity = buildQueryEntity(BRANCH_TEST, versionMetadata.getName(), versioId);
        IEntity filterEntiy = persistenceService.get(queryEntity, pc);
        Assert.assertNotNull(filterEntiy);
        Assert.assertNotNull(filterEntiy.getId());
        // type is always returned
        Assert.assertNotNull(filterEntiy.getType());
        Assert.assertNull(filterEntiy.getLastModified());
        Assert.assertEquals(filterEntiy.getFieldValues("approvals").size(), versionEntity.getFieldValues("approvals")
                .size());
        Assert.assertNull(filterEntiy.getLastModified());
    }

    @Test
    public void test16DeleteMandatoryField() {
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        IEntity manifestEntity = persistenceService.get(queryEntity, manifestContext);
        NewBsonEntity entity = new NewBsonEntity(manifestEntity.getMetaClass());
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
        IEntity approvalInst = createEmbedApproval();
        String embedId = approvalInst.getId();
        String parentId = FlattenEntityIDHelper.getParentId(embedId);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST,
                DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), parentId);
        IEntity getManifest = persistenceService.get(queryEntity, manifestContext);

        Integer len = (Integer) getManifest.getFieldProperty("approvals", FieldProperty.LENGTH.getName());
        List<?> approvals = getManifest.getFieldValues("approvals");
        Assert.assertEquals(len.intValue(), approvals.size());

        createEmbedApproval();

        queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.ManifestVersion.name(), parentId);
        getManifest = persistenceService.get(queryEntity, manifestContext);

        len = (Integer) getManifest.getFieldProperty("approvals", FieldProperty.LENGTH.getName());
        approvals = getManifest.getFieldValues("approvals");
        Assert.assertEquals(len.intValue(), approvals.size());
    }
    
    private static NewBsonEntity buildQueryEntity(String branchname, String metadata, String oid) {
        MetaClass meta = deployMetaService.getMetaClass(metadata);
        NewBsonEntity queryEntity = new NewBsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }
    
    @Test
    public void testContextAdditionalFilter() {
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, DeploymentEntityGenerator.TypeEnum.Manifest.name(), testManifestId);
        NewBsonEntity getManifest =(NewBsonEntity) persistenceService.get(queryEntity, manifestContext);
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
        
        getManifest = (NewBsonEntity)persistenceService.get(queryEntity, manifestContext);
        Integer newVersionLength = 0;
        newVersionLength = getManifest.getFieldLength("versions");
        Assert.assertEquals(newVersionLength, getManifest.getFieldLength("versions"));
        Assert.assertEquals(1, newVersionLength - oldVersionLength);

        // test get with read filter : root get
        PersistenceContext context = new PersistenceContext(manifestContext);
        Map<String, List<SearchCriteria>> additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        MetaClass metaClass = deployMetaService.getMetaClass(DeploymentEntityGenerator.TypeEnum.Manifest.name());
        MetaField nameField = metaClass.getFieldByName("name");
        ISearchField searchField = new SelectionField(nameField, NewDalSearchStrategy.getInstance());
        SearchCriteria criteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.NE, getManifest
                .getFieldValues("name").get(0));
        additionalCriteria.put(DeploymentEntityGenerator.TypeEnum.Manifest.name(), Arrays.asList(criteria));

        context.setAdditionalCriteria(additionalCriteria);

        NewBsonEntity getEntity2 = (NewBsonEntity)persistenceService.get(queryEntity, context);
        Assert.assertNull(getEntity2);
        
        // test get with root filter : embed get

    }
}
