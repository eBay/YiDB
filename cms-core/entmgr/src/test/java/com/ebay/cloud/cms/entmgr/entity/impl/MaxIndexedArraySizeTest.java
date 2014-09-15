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

package com.ebay.cloud.cms.entmgr.entity.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@Ignore
public class MaxIndexedArraySizeTest extends CMSMongoTest {

    protected static final String LAST_MODIFIED_TIME = "lastModifiedTime";
    protected static final String MANIFEST = "Manifest";
    protected static final String SERVICE_INSTANCE = "ServiceInstance";
    protected static final String APPLICATION_SERVICE = "ApplicationService";
    protected static final String COMPUTE = "Compute";
    protected static RuntimeDataLoader raptorLoader = null;
    protected static final String RAPTOR_REPO = "raptor-paas";
    protected static final String RAPTOR_DATA_FILE = "raptorTopology.json";

    protected static RuntimeDataLoader deployLoader = null;
    protected static final String DEPLOY_REPO = "software-deployment";
    protected static final String DEPLOY_DATA_FILE = "softwareDeploymentRuntime.json";

    protected static RuntimeDataLoader stratusLoader = null;
    protected static RuntimeDataLoader cmsdbLoader = null;
    protected static final String STRATUS_REPO = "stratus-ci";
    protected static final String CMSDB_REPO = "cmsdb";
    protected static final String STRATUS_DATA_FILE = "stratusRuntime.json";
    protected static final String CMSDB_DATA_FILE = "cmsdbRuntime.json";
    protected static final String MAX_INDEXED_ARRAY_SIZE_DATA_FILE = "MaxIndexedArraySize.json";
    protected static final String NETWORK_ADDRESS = "NetworkAddress";

    protected static MetadataDataLoader metaLoader = null;

    protected static IRepositoryService repoService = null;
    protected static IBranchService branchService = null;
    protected static IEntityService entityService = null;
    protected static IMetadataService raptorMetaService = null;
    protected static IMetadataService deployMetaService = null;
    protected static IPersistenceService persistenceService = null;

    protected static IMetadataService stratusMetaService = null;
    protected static IMetadataService cmsdbMetaService = null;

    protected static final String SOURCE_IP = "127.0.0.1";
    protected static EntityContext context;
    protected static MetadataContext metaContext;

    protected static MongoDataSource dataSource;

    @BeforeClass
    public static void setUp() {
    	
    	ServiceFactory.clearServiceCaches();
    	
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource bootStrapDs = new MongoDataSource(connectionString);
        CMSDBConfig dbConfig = new CMSDBConfig(bootStrapDs);
        dataSource = new MongoDataSource(connectionString, dbConfig);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        metaLoader.loadCMSDBMetaDataFromResource();
        metaLoader.loadMaxIndexedArraySizeMetaDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        raptorLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, RAPTOR_REPO);
        raptorLoader.load(RAPTOR_DATA_FILE);

        deployLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, DEPLOY_REPO);
        deployLoader.load(DEPLOY_DATA_FILE);

        stratusLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, STRATUS_REPO);
        stratusLoader.load(STRATUS_DATA_FILE);

        cmsdbLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, CMSDB_REPO);
        cmsdbLoader.load(CMSDB_DATA_FILE);
        
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

        // create a testing branch
        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);

        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        cmsdbMetaService = repoService.getRepository(CMSDB_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setDbConfig(dbConfig);

        metaContext = new MetadataContext();
        metaContext.setSourceIp(SOURCE_IP);
        metaContext.setSubject("unitTestUser");
        metaContext.setDbConfig(dbConfig);

        // check indexes are loaded
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : deployMetas) {
            checkIndexesLoaded(deployMetaService, meta);
        }

        Repository repository = new Repository();
        repository.setRepositoryName(RAPTOR_REPO);
        RepositoryOption repositoryOption = new RepositoryOption();
        repositoryOption.setMaxIndexedArraySize(3);
        repository.setOptions(repositoryOption);
        repoService.updateRepository(repository);
    }
    
    @AfterClass
	public static void tear() {
    	cleanTestData();
    	
		Repository repository = repoService.getRepository(RAPTOR_REPO);
		repository.getOptions().setMaxIndexedArraySize(null);
		repoService.updateRepository(repository);
		
		ServiceFactory.clearServiceCaches();
	}
    
    private static void cleanTestData() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		String metaClassName2 = "EmbeddedTest4Index";
		String metaClassName3 = "InnerTest";
		String metaClassName4 = "QueryWithSortOnTest";
		String metaClassName5 = "Resource";
		String metaClassName6 = "Base";
		
		Map<String, Object> currentConfig = config.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		config.updateConfig(newConfig);

		try {
			raptorMetaService.deleteMetaClass(metaClassName1, metaContext);
			raptorMetaService.deleteMetaClass(metaClassName2, metaContext);
			raptorMetaService.deleteMetaClass(metaClassName3, metaContext);
			raptorMetaService.deleteMetaClass(metaClassName4, metaContext);
			raptorMetaService.deleteMetaClass(metaClassName5, metaContext);
			raptorMetaService.deleteMetaClass(metaClassName6, metaContext);
		} catch (Exception e) {
			Assert.fail();
		}
		
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, false);
		config.updateConfig(newConfig);
	}
    
    protected static void checkIndexesLoaded(IMetadataService metaService, MetaClass metaClass) {
		if (metaClass.getName().equals(HistoryMetaClass.NAME) || metaClass.getName().equals(BranchMetaClass.TYPE_NAME)) {
			return;
		}

		Map<String, DBObject> indexObjects = getCollectionIndexMap(metaService, metaClass);
		for (IndexInfo ii : metaClass.getIndexes()) {
			Assert.assertTrue(indexObjects.containsKey(ii.getIndexName()));
		}
		if (metaClass.getName().equals("Manifest")) {
			boolean findEmbedIndex = false;
			for (String dbIndexName : indexObjects.keySet()) {
				if (dbIndexName.contains("manifestVersionNameIndex")) {
					findEmbedIndex = true;
				}
			}
			Assert.assertTrue(findEmbedIndex);
		}
	}

	protected static Map<String, DBObject> getCollectionIndexMap(IMetadataService metaService, MetaClass metaClass) {
		PersistenceContext pc = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
				ConsistentPolicy.PRIMARY, IBranch.DEFAULT_BRANCH);
		pc.setMongoDataSource(dataSource);
		DBCollection collection = pc.getDBCollection(metaClass);
		List<DBObject> indexInfo = collection.getIndexInfo();
		Assert.assertNotNull(indexInfo);
		Assert.assertTrue(indexInfo.size() > 0);

		Map<String, DBObject> indexMap = new HashMap<String, DBObject>();
		for (DBObject indexObject : indexInfo) {
			String name = (String) indexObject.get("name");
			indexMap.put(name, indexObject);
		}
		return indexMap;
	}

	private void createEmbedOrInnerEntity(String dataFilePath, String contextPath) {
		Map<String, List<JsonNode>> rawData = RuntimeDataLoader.loadRawData(dataFilePath);
		context.setPath(contextPath);
		for (Entry<String, List<JsonNode>> entry : rawData.entrySet()) {
			MetaClass metaClass = raptorMetaService.getMetaClass(entry.getKey());
			for (JsonNode instNode : entry.getValue()) {
				IEntity embedEntity = new JsonEntity(metaClass, (ObjectNode) instNode);
				embedEntity.setBranchId(IBranch.DEFAULT_BRANCH);
				entityService.create(embedEntity, context);
			}
		}
	}

	// 3.2.1
	@Test
	public void testCreateParentEntity_manyString_success() {
		raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyString_success.json");
		MetaClass metaClass = raptorMetaService.getMetaClass("SystemLimitationOnDocTest");
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId("SystemLimitationOnDocTestManyStringSuccess");
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
	}

	// 3.2.2
	@Test
	public void testCreateParentEntity_manyString_fail() {
		try {
			raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyString_fail.json");
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield manyString of metaclass SystemLimitationOnDocTest! Max is 3, Actual is 4",
					e.getMessage());
		}
	}

	// 3.2.3
	@Test
	public void testCreateParentEntity_manyReference_success() {
		raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyReference_success.json");
		MetaClass metaClass = raptorMetaService.getMetaClass("SystemLimitationOnDocTest");
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId("SystemLimitationOnDocTestManyReferenceSuccess");
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
	}

	// 3.2.4
	@Test
	public void testCreateParentEntity_manyReference_fail() {
		try {
			raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyReference_fail.json");
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield manyReference of metaclass SystemLimitationOnDocTest! Max is 3, Actual is 4",
					e.getMessage());
		}
	}

	// 3.2.5
	@Test
	public void testCreateParentEntity_manyEmbed_success() {
		raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyEmbed_success.json");
		MetaClass metaClass = raptorMetaService.getMetaClass("SystemLimitationOnDocTest");
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId("SystemLimitationOnDocTestManyEmbedSuccess");
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
	}

	// 3.2.6
	@Test
	public void testCreateParentEntity_manyEmbed_fail() {
		// thrown.expect(CmsDalException.class);
		// thrown.expectMessage("Exceed max indexed array size on metafield manyEmbed of metaclass SystemLimitationOnDocTest! Max is 3, Actual is 4");
		try {
			raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyEmbed_fail.json");
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield manyEmbed of metaclass SystemLimitationOnDocTest! Max is 3, Actual is 4",
					e.getMessage());
		}
	}

	// 3.2.7
	@Test
	public void testCreateParentEntity_manyEmbed_manyString_fail() {
		// thrown.expect(CmsDalException.class);
		// thrown.expectMessage("Exceed max indexed array size on metafield name of metaclass EmbeddedTest4Index! Max is 3, Actual is 4");
		try {
			raptorLoader.load("./maxIndexedArraySize/createParentEntity_manyEmbed_manyString_fail.json");
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield name of metaclass EmbeddedTest4Index! Max is 3, Actual is 4",
					e.getMessage());
		}
	}

	// 3.3
	@Test
	public void testCreateEmbedEntity() {

		raptorLoader.load("./maxIndexedArraySize/createEmbedEntity_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestEmbed";
		String fieldName = "manyEmbed";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(2, foundEntity.getFieldValues(fieldName).size());

		String contextPath = metaClassName + "!" + oid + "!" + fieldName;

		createEmbedOrInnerEntity("./maxIndexedArraySize/createEmbedEntity_addOneEmbed_success.json", contextPath);

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/createEmbedEntity_addAnotherEmbed_fail.json", contextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals("Exceed max indexed array size on metafield " + fieldName + " of metaclass "
					+ metaClassName + "! Max is 3, Actual is 4", e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		Assert.assertEquals(3, foundEntity.getFieldValues("manyEmbed").size());
	}

	// 3.4
	
	@Test
	public void testCreateInnerEntity() {
		raptorLoader.load("./maxIndexedArraySize/createInnerEntity_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestInner";
		String fieldName = "manyInner";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(0, foundEntity.getFieldValues("manyInner").size());

		String contextPath = metaClassName + "!" + oid + "!" + fieldName;

		createEmbedOrInnerEntity("./maxIndexedArraySize/createInnerEntity_success.json", contextPath);

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/createInnerEntity_fail.json", contextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield manyInner of metaclass SystemLimitationOnDocTest! Max is 3, Actual is 4",
					e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		Assert.assertEquals(3, foundEntity.getFieldValues("manyInner").size());
	}

	// 3.5
	@SuppressWarnings("unchecked")
    @Test
	public void testCreateEmbededInnerEntity() {
		raptorLoader.load("./maxIndexedArraySize/createEmbededInnerEntity_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestEmbededInner";
		String fieldName = "manyEmbed";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(3, foundEntity.getFieldValues(fieldName).size());

		String embedContextPath = metaClassName + "!" + oid + "!" + fieldName;

		String embedClassName = "EmbeddedTest4Index";
		String embedOid = "EmbeddedTest4IndexEmbededInnerOID1";
		String embededInnerFieldName = "embedManyInner";
		String innerContextPath = embedContextPath + "!" + embedOid + "!" + embededInnerFieldName;

		createEmbedOrInnerEntity("./maxIndexedArraySize/createEmbededInnerEntity_success.json", innerContextPath);

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/createEmbededInnerEntity_fail.json", innerContextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals("Exceed max indexed array size on metafield " + embededInnerFieldName
					+ " of metaclass " + embedClassName + "! Max is 3, Actual is 4", e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		List<IEntity> embedEntities = (List<IEntity>) foundEntity.getFieldValues(fieldName);
		boolean found = false;
		for (IEntity embedEntity : embedEntities) {
			if (embedEntity.getId().endsWith(embedOid)) {
				found = true;
				Assert.assertEquals(3, embedEntity.getFieldValues(embededInnerFieldName).size());
				break;
			}
		}
		if (!found) {
			Assert.fail("Can not find target embed entity!");
		}
	}

	// 3.6
	@Test
	public void testCreateEmbededEntity_manyString() {
		raptorLoader.load("./maxIndexedArraySize/createEmbedEntity_manyString_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestEmbedManyString";
		String fieldName = "manyEmbed";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(2, foundEntity.getFieldValues(fieldName).size());

		String contextPath = metaClassName + "!" + oid + "!" + fieldName;

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/createEmbedEntity_manyString_fail.json", contextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals(
					"Exceed max indexed array size on metafield name of metaclass EmbeddedTest4Index! Max is 3, Actual is 4",
					e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		Assert.assertEquals(2, foundEntity.getFieldValues(fieldName).size());
	}

	// 3.7
	@Test
	public void testBatchCreateEmbedEntity() {
		raptorLoader.load("./maxIndexedArraySize/batchCreateEmbedEntity_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestBatchEmbedOID";
		String fieldName = "manyEmbed";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(2, foundEntity.getFieldValues(fieldName).size());

		String contextPath = metaClassName + "!" + oid + "!" + fieldName;

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/batchCreateEmbedEntity_fail.json", contextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals("Exceed max indexed array size on metafield " + fieldName + " of metaclass "
					+ metaClassName + "! Max is 3, Actual is 4", e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		Assert.assertEquals(3, foundEntity.getFieldValues(fieldName).size());
	}

	// 3.8
	@Test
	public void testBatchCreateInnerEntity() {
		raptorLoader.load("./maxIndexedArraySize/batchCreateInnerEntity_setup.json");

		String metaClassName = "SystemLimitationOnDocTest";
		String oid = "SystemLimitationOnDocTestBatchInnerOID";
		String fieldName = "manyInner";

		MetaClass metaClass = raptorMetaService.getMetaClass(metaClassName);
		IEntity queryEntity = new JsonEntity(metaClass);
		queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		queryEntity.setId(oid);
		IEntity foundEntity = entityService.get(queryEntity, context);
		Assert.assertNotNull(foundEntity);
		Assert.assertEquals(0, foundEntity.getFieldValues(fieldName).size());

		String contextPath = metaClassName + "!" + oid + "!" + fieldName;

		createEmbedOrInnerEntity("./maxIndexedArraySize/batchCreateInnerEntity_success.json", contextPath);

		try {
			createEmbedOrInnerEntity("./maxIndexedArraySize/batchCreateInnerEntity_fail.json", contextPath);
			Assert.fail();
		} catch (CmsDalException e) {
			Assert.assertEquals("Exceed max indexed array size on metafield " + fieldName + " of metaclass "
					+ metaClassName + "! Max is 3, Actual is 4", e.getMessage());
		}

		foundEntity = entityService.get(queryEntity, context);
		Assert.assertEquals(3, foundEntity.getFieldValues(fieldName).size());
	}

}