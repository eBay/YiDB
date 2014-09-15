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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
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
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author shuachen
 *
 * 2014-4-23
 */
public class EntityComparatorTest extends CMSMongoTest {

	private static final EntityFieldTargetMerger targetFieldPushMerger = new EntityFieldTargetMerger(false);
    private static final EntityFieldTargetMerger targetFieldPullMerger = new EntityFieldTargetMerger(true);
    private static final EntityFieldDeltaMerger deltaFieldPushMerger = new EntityFieldDeltaMerger();


    private static final String ENVIRONMENT = "Environment";
    private static final String NODE_SERVER = "NodeServer";
    private static final String        LAST_MODIFIED_TIME  = "lastModifiedTime";
    private static final String        MANIFEST            = "Manifest";
    private static final String        SERVICE_INSTANCE    = "ServiceInstance";
    private static final String        APPLICATION_SERVICE = "ApplicationService";
    private static final String        COMPUTE             = "Compute";

    private static RuntimeDataLoader   raptorLoader        = null;
    private static final String        RAPTOR_REPO         = "raptor-paas";
    private static final String        RAPTOR_DATA_FILE    = "raptorTopology.json";

    private static RuntimeDataLoader   deployLoader        = null;
    private static final String        DEPLOY_REPO         = "software-deployment";
    private static final String        DEPLOY_DATA_FILE    = "softwareDeploymentRuntime.json";

    private static RuntimeDataLoader   stratusLoader       = null;
    private static final String        STRATUS_REPO        = "stratus-ci";
    private static final String        STRATUS_DATA_FILE   = "stratusRuntime.json";

    protected static RuntimeDataLoader cmsdbLoader         = null;
    protected static final String      CMSDB_REPO          = "cmsdb";
    protected static final String      CMSDB_DATA_FILE     = "cmsdbRuntime.json";


    private static MetadataDataLoader  metaLoader          = null;

    private static IRepositoryService  repoService         = null;
    private static IBranchService      branchService       = null;
    private static IEntityService      entityService       = null;
    private static IMetadataService    raptorMetaService   = null;
    private static IMetadataService    deployMetaService   = null;
    private static IMetadataService    stratusMetaService  = null;
    private static IMetadataService    cmsdbMetaService    = null;
    private static IPersistenceService persistenceService  = null;


    private static final String        SOURCE_IP           = "127.0.0.1";
    private static final String        MAIN_BRANCH         = IBranch.DEFAULT_BRANCH;
    private static EntityContext       context;

    private static MongoDataSource     dataSource;
    protected static CMSDBConfig         config;

    @BeforeClass
    public static void setUp() {
        ServiceFactory.clearServiceCaches();
        
        String connectionString = CMSMongoTest.getConnectionString();
        dataSource = new MongoDataSource(connectionString);
        config = new CMSDBConfig(dataSource);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        metaLoader.loadCMSDBMetaDataFromResource();
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

        context = newEntityContext();

        // check indexes are loaded
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(new MetadataContext());
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(new MetadataContext());
        for (MetaClass meta : deployMetas) {
            checkIndexesLoaded(deployMetaService, meta);
        }
    }

    protected static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setDbConfig(config);
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        return context;
    }
    
    protected static PersistenceContext newPersistentContext(IMetadataService metaService) {
        PersistenceContext pContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.PRIMARY, IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }

    private static void checkIndexesLoaded(IMetadataService metaService, MetaClass metaClass) {
        if (metaClass.getName().equals(HistoryMetaClass.NAME) || metaClass.getName().equals(BranchMetaClass.TYPE_NAME)) {
            return;
        }

        Map<String, DBObject> indexObjects = getCollectionIndexMap(metaService, metaClass);
        for (IndexInfo ii : metaClass.getIndexes()) {
            Assert.assertTrue(indexObjects.containsKey(ii.getIndexName()));
        }
    }

    private static Map<String, DBObject> getCollectionIndexMap(IMetadataService metaService, MetaClass metaClass) {
        PersistenceContext pc = newPersistentContext(metaService);
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
    
	@Test
	public void testProcessAttribute() {
		MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
		String found = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"label\": \"lbmsstag1-app\", \"preManifestRef\": [\"lbms_master_old_wsdl_staging-0.1.20130710214948\", \"lbms_master_old_wsdl_staging-0.1.20130710214949\"]}";
		String source= "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"label\": \"lbmsstag2-app\", \"preManifestRef\": [\"lbms_master_old_wsdl_staging-0.1.20130710214949\"], \"nugget\":null}";
		IEntity foundEntity = new JsonEntity(metaClass, found);
		IEntity sourceEntity = new JsonEntity(metaClass, source);
		EntityComparator comparator = new EntityComparator(sourceEntity);
		MetaField metaField = metaClass.getFieldByName("name");
		comparator.processAttribute(foundEntity, metaField);
		Assert.assertFalse(comparator.getDiffResult());
		
		metaField = metaClass.getFieldByName("label");
		comparator.processAttribute(foundEntity, metaField);
		Assert.assertTrue(comparator.getDiffResult());
		
		metaField = metaClass.getFieldByName("preManifestRef");
		comparator.processAttribute(foundEntity, metaField);
		Assert.assertTrue(comparator.getDiffResult());
		
		metaField = metaClass.getFieldByName("nugget");
		comparator.processAttribute(foundEntity, metaField);
		Assert.assertTrue(comparator.getDiffResult());
	}
	
	@Test
	public void testProcessReference() {
		MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
		String found = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"services\": [{\"_oid\":\"4fbb314fc681caf13e283a6a\"}, {\"_oid\":\"4fbb314fc681caf13e283a6c\"}]}";
		String source= "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"services\": [{\"_oid\":\"4fbb314fc681caf13e283a6a\"}, {\"_oid\":\"4fbb314fc681caf13e283a6c\"}]}";
		IEntity foundEntity = new JsonEntity(metaClass, found);
		IEntity sourceEntity = new JsonEntity(metaClass, source);
		EntityComparator comparator = new EntityComparator(sourceEntity);
		MetaRelationship metaRelationship = (MetaRelationship)metaClass.getFieldByName("services");
		comparator.processReference(foundEntity, metaRelationship);
		Assert.assertFalse(comparator.getDiffResult());
		
		found = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"services\": [{\"_oid\":\"4fbb314fc681caf13e283a6a\"}, {\"_oid\":\"4fbb314fc681caf13e283a6e\"}]}";
		foundEntity = new JsonEntity(metaClass, found);
		comparator.processReference(foundEntity, metaRelationship);
		Assert.assertTrue(comparator.getDiffResult());
		
		found = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"services\": [{\"_oid\":\"4fbb314fc681caf13e283a6a\"}, {\"_oid\":\"4fbb314fc681caf13e283a6b\"}, {\"_oid\":\"4fbb314fc681caf13e283a6c\"}]}";
		foundEntity = new JsonEntity(metaClass, found);
		comparator.processReference(foundEntity, metaRelationship);
		Assert.assertTrue(comparator.getDiffResult());
	}
	
	@Test
	public void testProcessReference_embed() {
		MetaClass metaClass = raptorMetaService.getMetaClass("AllowFullTableScanParentTest");
		String found = "{\"_oid\": \"4fbb314fc681caf13e244438\",\"_type\": \"AllowFullTableScanParentTest\",\"name\": \"AllowFullTableScanParentTest-embed-integer-test2\",\"embed\": {\"_oid\": \"AllowFullTableScanParentTest!4fbb314fc681caf13e244438!embed!embed11\",\"_type\": \"EmbeddedTest\",\"name\": \"EmbeddedTest-name\",\"label\": \"stratus\",\"number\": 2}}";
		String source= "{\"_oid\": \"4fbb314fc681caf13e244438\",\"_type\": \"AllowFullTableScanParentTest\",\"name\": \"AllowFullTableScanParentTest-embed-integer-test2\",\"embed\": {\"_oid\": \"AllowFullTableScanParentTest!4fbb314fc681caf13e244438!embed!embed11\",\"_type\": \"EmbeddedTest\",\"name\": \"EmbeddedTest-name\",\"label\": \"stratus\",\"number\": 2}}";
		IEntity foundEntity = new JsonEntity(metaClass, found);
		IEntity sourceEntity = new JsonEntity(metaClass, source);
		EntityComparator comparator = new EntityComparator(sourceEntity);
		MetaRelationship metaRelationship = (MetaRelationship)metaClass.getFieldByName("embed");
		comparator.processReference(foundEntity, metaRelationship);
		Assert.assertFalse(comparator.getDiffResult());
		
		found = "{\"_oid\": \"4fbb314fc681caf13e244438\",\"_type\": \"AllowFullTableScanParentTest\",\"name\": \"AllowFullTableScanParentTest-embed-integer-test2\",\"embed\": {\"_oid\": \"AllowFullTableScanParentTest!4fbb314fc681caf13e244438!embed!embed12\",\"_type\": \"EmbeddedTest\",\"name\": \"EmbeddedTest-name\",\"label\": \"stratus\",\"number\": 2}}";
		foundEntity = new JsonEntity(metaClass, found);
		comparator.processReference(foundEntity, metaRelationship);
		Assert.assertTrue(comparator.getDiffResult());
	}
}
