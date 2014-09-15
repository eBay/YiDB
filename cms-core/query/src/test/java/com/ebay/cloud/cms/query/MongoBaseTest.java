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

package com.ebay.cloud.cms.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.query.parser.QueryParser;
import com.ebay.cloud.cms.query.service.IQueryService;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.impl.QueryServiceImpl;
import com.ebay.cloud.cms.query.translator.QueryTranslator;
import com.ebay.cloud.cms.query.util.TestUtils;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * User: Rene Xu
 * Email: rene.xu@ebay.com
 * Date: 5/21/12 4:36 PM
 */
public abstract class MongoBaseTest extends CMSMongoTest {

    protected static IQueryService queryService = null;
    protected static ISearchService searchService = null;
    protected static IRepositoryService repositoryService = null;
    protected static IBranchService branchService = null;
    protected static IEntityService entityService = null;
    
    protected static IMetadataService raptorMetaService = null;
    protected static IMetadataService deployMetaService = null;
    protected static IMetadataService stratusMetaService = null;
    protected static IMetadataService cmsdbMetaService = null;
    
    protected static QueryParser raptorParser = null;
    protected static QueryParser deployParser = null;
    protected static QueryParser stratusParser = null;
    protected static QueryParser cmsdbParser = null;

    protected static QueryContext raptorContext = null;
    protected static QueryContext deployContext = null;
    protected static QueryContext stratusContext = null;
    protected static QueryContext cmsdbContext = null;

    protected static final String RAPTOR_REPO = "raptor-paas";
    private static final String RAPTOR_TEST_DATA = "raptorTopology.json";

    protected static final String DEPLOY_REPO = "software-deployment";
    private static final String DEPLOY_TEST_DATA = "softwareDeploymentRuntime.json";

    protected static final String STRATUS_REPO = "stratus-ci";
    protected static final String STRATUS_TEST_DATA = "stratusRuntime.json";
    
    protected static final String CMSDB_REPO = "cmsdb";
    protected static final String CMSDB_TEST_DATA = "cmsdbRuntime.json";
    
    protected static String RAPTOR_MAIN_BRANCH_ID;
    protected static String SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID;
    protected static String STRATUS_MAIN_BRANCH_ID;
    protected static String CMSDB_MAIN_BRANCH_ID;
    
    protected static CMSDBConfig dbConfig = null;
    protected static Integer SYS_LIMIT_DOCUMENTS_MONGO_QUERY = null;
    private static MetadataContext metaContext;
    
    @Before
    public void setup() {
        raptorContext.clearMetadataServices();
        deployContext.clearMetadataServices();
        stratusContext.clearMetadataServices();
        cmsdbContext.clearMetadataServices();
    }
    
    /**
     * Create a query context for unit test which set CMSDBConfig object already
     * The CMSDBConfig object contains system limitation setting for query
     * @param repositoryName
     * @param branchName
     * @return
     */
    protected static QueryContext newQueryContext(String repositoryName, String branchName) {
    	QueryContext qc = new QueryContext(repositoryName, branchName);
    	qc.setDbConfig(dbConfig);
    	qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
    	qc.setRepositoryService(repositoryService);
    	return qc;
    }
    
    protected static QueryContext newQueryContext(QueryContext qContext) {
    	QueryContext qc = new QueryContext(qContext);
    	qc.setDbConfig(dbConfig);
    	qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
    	return qc;
    }
    
    @BeforeClass
    public static void setupData() {
        dataSource = new MongoDataSource(getConnectionString());
        dbConfig = new CMSDBConfig(dataSource);
        MetadataDataLoader.getInstance(dataSource).loadTestDataFromResource();                       
        MetadataDataLoader.getInstance(dataSource).loadCMSDBMetaDataFromResource();                       
        repositoryService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");                
        List<IEntity> raptorEntites = RuntimeDataLoader.getDataLoader(dataSource, repositoryService, RAPTOR_REPO).load(RAPTOR_TEST_DATA);
        List<IEntity> sdEntites = RuntimeDataLoader.getDataLoader(dataSource, repositoryService, DEPLOY_REPO).load(DEPLOY_TEST_DATA);
        List<IEntity> stratusEntites = RuntimeDataLoader.getDataLoader(dataSource, repositoryService, STRATUS_REPO).load(STRATUS_TEST_DATA);
        List<IEntity> cmsdbEntites = RuntimeDataLoader.getDataLoader(dataSource, repositoryService, CMSDB_REPO).load(CMSDB_TEST_DATA);
        
        RAPTOR_MAIN_BRANCH_ID = raptorEntites.get(0).getBranchId();
        SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID = sdEntites.get(0).getBranchId();
        STRATUS_MAIN_BRANCH_ID = stratusEntites.get(0).getBranchId();
        CMSDB_MAIN_BRANCH_ID = cmsdbEntites.get(0).getBranchId();

        QueryContext.setDefaultSmallTableThreshold(0);
        
        searchService = new SearchServiceImpl(dataSource);
        branchService = ServiceFactory.getBranchService(dataSource, TestUtils.getTestDalImplemantation(dataSource));
        queryService = new QueryServiceImpl(repositoryService, branchService, searchService);
        entityService = ServiceFactory.getEntityService(dataSource, repositoryService, TestUtils.getTestDalImplemantation(dataSource));

        raptorMetaService = repositoryService.getRepository(RAPTOR_REPO).getMetadataService();
        raptorContext = new QueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setRepositoryService(repositoryService);
        raptorContext.setSearchService(searchService);
        raptorContext.setBranchService(branchService);
        raptorContext.setShowDisplayMeta(true);
        raptorContext.setDbConfig(dbConfig);
        raptorContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        raptorParser = new QueryParser(raptorContext);
        
        deployMetaService = repositoryService.getRepository(DEPLOY_REPO).getMetadataService();
        deployContext = new QueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        deployContext.setAllowFullTableScan(true);
        deployContext.setRepositoryService(repositoryService);
        deployContext.setSearchService(searchService);
        deployContext.setBranchService(branchService);
        deployContext.setShowDisplayMeta(true);
        deployContext.setDbConfig(dbConfig);
        deployContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        deployParser = new QueryParser(deployContext);

        stratusMetaService = repositoryService.getRepository(STRATUS_REPO).getMetadataService();
        stratusContext = new QueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        stratusContext.setAllowFullTableScan(true);
        stratusContext.setRepositoryService(repositoryService);
        stratusContext.setSearchService(searchService);
        stratusContext.setBranchService(branchService);
        stratusContext.setShowDisplayMeta(true);
        stratusContext.setDbConfig(dbConfig);
        stratusContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        stratusParser = new QueryParser(stratusContext);
        
        cmsdbMetaService = repositoryService.getRepository(CMSDB_REPO).getMetadataService();
        cmsdbContext = new QueryContext(CMSDB_REPO, CMSDB_MAIN_BRANCH_ID);
        cmsdbContext.setAllowFullTableScan(true);
        cmsdbContext.setRepositoryService(repositoryService);
        cmsdbContext.setSearchService(searchService);
        cmsdbContext.setBranchService(branchService);
        cmsdbContext.setShowDisplayMeta(true);
        cmsdbContext.setDbConfig(dbConfig);
        cmsdbContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        cmsdbParser = new QueryParser(cmsdbContext);
        // check indexes are created
        
        
        metaContext = new MetadataContext(true, true);
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : deployMetas) {
            checkIndexesLoaded(deployMetaService, meta);
        }
        
        SYS_LIMIT_DOCUMENTS_MONGO_QUERY = (Integer)dbConfig.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY);
    }
    
    private static void checkIndexesLoaded(IMetadataService metaService, MetaClass metaClass) {
        if (metaClass.getName().equals(HistoryMetaClass.NAME) || metaClass.getName().equals(BranchMetaClass.TYPE_NAME)) {
            return;
        }

        Map<String, DBObject> indexObjects = getCollectionIndexMap(metaService, metaClass);
        for (IndexInfo ii : metaClass.getIndexes()) {
            Assert.assertTrue(" index " + ii.getIndexName() + " not found! ", indexObjects.containsKey(ii.getIndexName()));
        }
    }
    
    private static Map<String, DBObject> getCollectionIndexMap(IMetadataService metaService, MetaClass metaClass) {
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

    @AfterClass
    public static void clearData() {
        System.out.println("");
    }

    protected QueryTranslator getTranslator(QueryContext context) {
        return new QueryTranslator(context);
    }

    protected JsonEntity buildQueryEntity(String reponame, String branchname, String metadata, String oid) {
        MetaClass meta = repositoryService.getRepository(reponame).getMetadataService().getMetaClass(metadata);
        JsonEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }
    
}
