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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.CallbackContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.ModifyAction;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest(EntityServiceImpl.class)
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*",
    "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*",
            "org.slf4j.*",  "com.sun.org.apache.xerces.internal.impl.dv.dtd.*" })
public class EntityServiceTest extends CMSMongoTest {

    protected static final String        LAST_MODIFIED_TIME  = "lastModifiedTime";
    protected static final String        MANIFEST            = "Manifest";
    protected static final String        SERVICE_INSTANCE    = "ServiceInstance";
    protected static final String        APPLICATION_SERVICE = "ApplicationService";
    protected static final String        COMPUTE             = "Compute";
    protected static RuntimeDataLoader   raptorLoader        = null;
    protected static final String        RAPTOR_REPO         = "raptor-paas";
    protected static final String        RAPTOR_DATA_FILE    = "raptorTopology.json";

    protected static RuntimeDataLoader   deployLoader        = null;
    protected static final String        DEPLOY_REPO         = "software-deployment";
    protected static final String        DEPLOY_DATA_FILE    = "softwareDeploymentRuntime.json";

    protected static RuntimeDataLoader   stratusLoader       = null;
    protected static RuntimeDataLoader   cmsdbLoader         = null;
    protected static final String        STRATUS_REPO        = "stratus-ci";
    protected static final String        CMSDB_REPO          = "cmsdb";
    protected static final String        STRATUS_DATA_FILE   = "stratusRuntime.json";
    protected static final String        CMSDB_DATA_FILE     = "cmsdbRuntime.json";
    protected static final String        NETWORK_ADDRESS     = "NetworkAddress";

    protected static MetadataDataLoader  metaLoader          = null;

    protected static IRepositoryService  repoService         = null;
    protected static IBranchService      branchService       = null;
    protected static IEntityService      entityService       = null;
    protected static IMetadataService    raptorMetaService   = null;
    protected static IMetadataService    deployMetaService   = null;
    protected static IPersistenceService persistenceService  = null;

    protected static IMetadataService    stratusMetaService  = null;
    protected static IMetadataService    cmsdbMetaService    = null;

    protected static final String        SOURCE_IP           = "127.0.0.1";
    protected static EntityContext       context;
    protected static MetadataContext     metaContext;

    protected static MongoDataSource     dataSource;

    @BeforeClass
    public static void setUp(){
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource bootStrapDs = new MongoDataSource(connectionString);
        config = new CMSDBConfig(bootStrapDs);
        dataSource = new MongoDataSource(connectionString, config);
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

        //create a testing branch
        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);

        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        cmsdbMetaService = repoService.getRepository(CMSDB_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = newEntityContext();

        metaContext = new MetadataContext();
        metaContext.setSourceIp(SOURCE_IP);
        metaContext.setSubject("unitTestUser");
        
        // check indexes are loaded
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : deployMetas) {
            checkIndexesLoaded(deployMetaService, meta);
        }
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
    
    protected JsonEntity newServiceInstance(String branchId){
        String metaType = SERVICE_INSTANCE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test");
        return newEntity;
    }

    protected JsonEntity newCompute(String branchId) {
        String metaType = COMPUTE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Compute Instance for Entity-Branch Test");
        return newEntity;
    }

    protected JsonEntity newApplicationService(String branchId){
        String metaType = APPLICATION_SERVICE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Application Service Instance for Entity-Branch Test");
        return newEntity;
    }
    
    protected JsonEntity newManifest(String branchId){
        MetaClass manifestCls = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("Manifest");
        JsonEntity manifestEntity = new JsonEntity(manifestCls);
        manifestEntity.setBranchId(branchId);
        manifestEntity.addFieldValue("name", "Dummy Manifest for Entity-Branch Test" + new Random().nextInt());
        manifestEntity.addFieldValue("lastModifiedTime", new Date());
                
        MetaClass versionCls = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("ManifestVersion");
        JsonEntity versionEntity = new JsonEntity(versionCls);
        versionEntity.setBranchId(branchId);
        versionEntity.addFieldValue("name", "Dummy Version for Entity-Branch Test" + new Random().nextInt());
        versionEntity.addFieldValue("createdTime", new Date());
        versionEntity.addFieldValue("description", "Dummy Version description");
        
        manifestEntity.addFieldValue("versions", versionEntity);
        return manifestEntity;
    }
    
    @Test
    public void create(){
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, newId);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        
        try {
            newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
            newInst.setId(newId);
            entityService.create(newInst, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.ENTITY_ALREADY_EXIST, e.getErrorEnum());
        }
    }
    
    @Test
    public void batchCreateEmptyEntities(){
        List<String> ids = entityService.batchCreate(null, context, new ArrayList<String>());
        Assert.assertTrue(ids.isEmpty());
        
        ids = entityService.batchCreate(Collections.<IEntity> emptyList(), context, new ArrayList<String>());
        Assert.assertTrue(ids.isEmpty());
    }
    
    @Test
    public void batchCreate(){
        IEntity service = newServiceInstance(IBranch.DEFAULT_BRANCH);
        service.setId("EntityServiceTest-batchCreate-ServiceInstance");
        
        IEntity application = newApplicationService(IBranch.DEFAULT_BRANCH);
        application.setId("EntityServiceTest-batchCreate-ApplicationService");
        
        List<String> ids = entityService.batchCreate(Arrays.asList(application, service), context, new ArrayList<String>());
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, APPLICATION_SERVICE, ids.get(0));
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, ids.get(1));
        entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
    }
    
    @Test
    public void batchCreateException(){
        IEntity si1 = newServiceInstance(IBranch.DEFAULT_BRANCH);
        si1.setId("si-1");
        
        IEntity app1 = newApplicationService(IBranch.DEFAULT_BRANCH);
        app1.setId("as-1");
        app1.addFieldValue("name", "as-1");
        
        IEntity app2 = newApplicationService(IBranch.DEFAULT_BRANCH);
        app2.setId("as-2");
        app2.addFieldValue("name", "as-2");
        
        IEntity app3 = newApplicationService(IBranch.DEFAULT_BRANCH);
        app3.setId("as-2");
        app3.addFieldValue("name", "as-3");
        
        try {
            entityService.batchCreate(Arrays.asList(si1, app1, app2, app3), context, new ArrayList<String>());
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.ENTITY_ALREADY_EXIST, e.getErrorEnum());
            Assert.assertTrue(e.getMessage().contains(
                            "batch create failure: error code is 1016 and error message is entity as-2 already exists in branch main. The following entities have been created: [si-1, as-1, as-2]"));
        }
    }
    
    @Test
    public void batchCreateEmbedException(){
        String metaType = "Team";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        IEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.setId("test-team");
        newEntity.addFieldValue("name", "test-team");
        
        try {
            entityService.batchCreate(Arrays.asList(newEntity), context, new ArrayList<String>());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_EMBED_ID_PATH, e.getErrorEnum());
            Assert.assertTrue(e.getMessage().contains("batch create failure: error code is 10022 and error message is Invalid embed ID: test-team, path: null. The following entities have been created: []"));
        }
    }
    
    @Test
    public void batchCreateInner(){
        IEntity room = newEntity(raptorMetaService, "room21", "Room", IBranch.DEFAULT_BRANCH);
        room.setId("room21");
        
        IEntity door = newEntity(raptorMetaService, "door211", "Door", IBranch.DEFAULT_BRANCH);
        door.setId("door211");
        String path = "Room!room21!path";
        door.setHostEntity(path);
        
        IEntity lock1 = newEntity(raptorMetaService, "lock2111", "Lock", IBranch.DEFAULT_BRANCH);
        lock1.setId("lock2111");
        String lockHost = "Door!door211!lock";
        lock1.setHostEntity(lockHost);
        
        IEntity lock2 = newEntity(raptorMetaService, "lock2111", "Lock", IBranch.DEFAULT_BRANCH);
        lock2.setId("lock2112");
        lock2.setHostEntity(lockHost);
        
        entityService.batchCreate(Arrays.asList(room, door, lock1, lock2), context, new ArrayList<String>());
        
        IEntity qRoom = buildQueryEntity(room);
        IEntity getRoom = entityService.get(qRoom, context);
        Assert.assertNotNull(getRoom);
        IEntity pathEntity = (IEntity)getRoom.getFieldValues("path").get(0);
        Assert.assertEquals("door211", pathEntity.getId());
        Assert.assertEquals("Door", pathEntity.getType());
        
        IEntity qDoor = buildQueryEntity(door);
        IEntity getDoor = entityService.get(qDoor, context);
        Assert.assertNotNull(getDoor);
        IEntity lockEntity1 = (IEntity)getDoor.getFieldValues("lock").get(0);
        Assert.assertEquals("lock2111", lockEntity1.getId());
        Assert.assertEquals("Lock", lockEntity1.getType());
        IEntity lockEntity2 = (IEntity)getDoor.getFieldValues("lock").get(1);
        Assert.assertEquals("lock2112", lockEntity2.getId());
        Assert.assertEquals("Lock", lockEntity2.getType());     
        
        IEntity qLock1 = buildQueryEntity(lock1);
        IEntity getLock1 = entityService.get(qLock1, context);
        Assert.assertNotNull(getLock1);

        IEntity qLock2 = buildQueryEntity(lock2);
        IEntity getLock2 = entityService.get(qLock2, context);
        Assert.assertNotNull(getLock2);
        
        //cascading delete
        entityService.delete(qRoom, context);
        Assert.assertNull(entityService.get(qRoom, context));
        Assert.assertNull(entityService.get(qDoor, context));
        Assert.assertNull(entityService.get(qLock1, context));
        Assert.assertNull(entityService.get(qLock2, context));
    }

    @Test
    public void batchCreateDifferentRepo() {
        try {
            IEntity service = newServiceInstance(IBranch.DEFAULT_BRANCH);
            service.setId("EntityServiceTest-batchCreateDifferentRepo-ServiceInstance");

            IEntity manifest = newManifest(IBranch.DEFAULT_BRANCH);
            manifest.setId("EntityServiceTest-batchCreateDifferentRepo-ServiceInstance-2");
            entityService.batchCreate(Arrays.asList(service, manifest), context, new ArrayList<String>());
            Assert.fail();
        } catch (CmsDalException e) {
            // expected
        	Assert.assertEquals(DalErrCodeEnum.REPOSITORYNAME_INCONSISTENCY, e.getErrorEnum());
        }
    }

    @Test
    public void batchCreateDifferentBranch() {
        try {
            IEntity service = newServiceInstance(IBranch.DEFAULT_BRANCH);
            service.setId("EntityServiceTest-batchCreateDifferentBranch-ServiceInstance");
            IEntity application = newApplicationService("subic");
            entityService.batchCreate(Arrays.asList(service, application), context, new ArrayList<String>());
        } catch (CmsDalException e) {
            // expected
        	Assert.assertEquals(DalErrCodeEnum.BRANCHID_INCONSISTENCY, e.getErrorEnum());
        }
    }

    @Test
    public void batchUpdate() {
        //create 
        IEntity service = newServiceInstance(IBranch.DEFAULT_BRANCH);
        service.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test" +  + System.currentTimeMillis());
        service.setId("EntityServiceTest-batchUpdate-ServiceInstance" + System.currentTimeMillis());
        IEntity application = newApplicationService(IBranch.DEFAULT_BRANCH);
        application.addFieldValue("name", "Dummy Application Service Instance for Entity-Branch Test" +  + System.currentTimeMillis());
        application.setId("EntityServiceTest-batchUpdate-ApplicationService" + System.currentTimeMillis());
        List<String> ids = entityService.batchCreate(Arrays.asList(application, service), context, new ArrayList<String>());

        //update field values
        List<IEntity> updateEntities = new ArrayList<IEntity>();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, APPLICATION_SERVICE, ids.get(0));
        IEntity entityGet = entityService.get(qEntity, context);
        String newArchTier = "new-arch-tider";
        entityGet.addFieldValue("archTier", newArchTier);
        updateEntities.add(entityGet);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, ids.get(1));
        entityGet = entityService.get(qEntity, context);
        String newPort = "9191";
        entityGet.addFieldValue("port", newPort);//port field has no default value
        updateEntities.add(entityGet);

        entityService.batchModify(updateEntities, context, new ArrayList<String>());
        
        //check 
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, APPLICATION_SERVICE, ids.get(0));
        entityGet = entityService.get(qEntity, context);
        Assert.assertTrue(entityGet.getFieldValues("archTier") != null);
        Assert.assertTrue(entityGet.getFieldValues("archTier").size() > 0);
        Assert.assertEquals(newArchTier, entityGet.getFieldValues("archTier").get(0));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, ids.get(1));
        entityGet = entityService.get(qEntity, context);
        Assert.assertTrue(entityGet.getFieldValues("port") != null);
        Assert.assertTrue(entityGet.getFieldValues("port").size() > 0);
        Assert.assertEquals(newPort, entityGet.getFieldValues("port").get(0));
    }

//    @Test
//    public void batchModifyForceUpdate() {
//        IEntity service = newServiceInstance(IBranch.DEFAULT_BRANCH);
//        service.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test" +  + System.currentTimeMillis());
//        service.setId("EntityServiceTest-batchUpdate-ServiceInstance" + System.currentTimeMillis());
//        IEntity application = newApplicationService(IBranch.DEFAULT_BRANCH);
//        application.addFieldValue("name", "Dummy Application Service Instance for Entity-Branch Test" +  + System.currentTimeMillis());
//        application.setId("EntityServiceTest-batchUpdate-ApplicationService" + System.currentTimeMillis());
//        List<String> ids = entityService.batchCreate(Arrays.asList(application, service), context, new ArrayList<String>());
//
//        List<IEntity> updateEntities = new ArrayList<IEntity>();
//        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, APPLICATION_SERVICE, ids.get(0));
//        IEntity entityGet = entityService.get(qEntity, context);
//        updateEntities.add(entityGet);
//
//        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, ids.get(1));
//        entityGet = entityService.get(qEntity, context);
//        updateEntities.add(entityGet);
//
//        entityService.batchModify(updateEntities, context, new ArrayList<String>());
//        entityGet = entityService.get(qEntity, context);
//        Assert.assertEquals(0, entityGet.getVersion());
//
//        EntityContext newContext = newEntityContext();
//        newContext.setForceUpdate(true);
//        newContext.setFetchFieldProperty(false);
//        entityService.batchModify(updateEntities, newContext, new ArrayList<String>());
//        entityGet = entityService.get(qEntity, newContext);
//        Assert.assertEquals(1, entityGet.getVersion());
//    }
    
    @Test
    public void batchModifyEmpty() {
        entityService.batchModify(null, context, new ArrayList<String>());
        entityService.batchModify(Collections.<IEntity> emptyList(), context, new ArrayList<String>());
    }

    @Test
    public void testCreateWithDateDefault() {
        MetaClass manifestMeta = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass(MANIFEST);
        MetaAttribute timeField = (MetaAttribute) manifestMeta.getFieldByName(LAST_MODIFIED_TIME);
        Assert.assertEquals("$NOW", timeField.getDefaultValue());
        
        IEntity jsonEntity = newManifest(IBranch.DEFAULT_BRANCH);
        entityService.create(jsonEntity, context);
    }
    
    @Test
    public void delete() {
        EntityContext context = newEntityContext();

        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, newId);
        entityService.delete(qEntity, context);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet == null);
    }

    @Test
    public void delete_noMandatoryCheck() {
        EntityContext context = newEntityContext();
        MetaClass escalationMeta = cmsdbMetaService.getMetaClass("Escalation");
        JsonEntity escalationJson = new JsonEntity(escalationMeta);
        escalationJson.addFieldValue("resourceId", "escalation-resource-Id");
        escalationJson.setBranchId(IBranch.DEFAULT_BRANCH);
        escalationJson.addFieldValue("contact", "DL-TEAM-CMS-ALL@gmail.com");
        escalationJson.addFieldValue("contactNumber", "28913746");
        escalationJson.addFieldValue("escalation", "bin@gmail.com");
        escalationJson.addFieldValue("escalationNumber", "1234567890");
        escalationJson.addFieldValue("primaryHours", "7*24");
        escalationJson.addFieldValue("offHours", "0");
        escalationJson.addFieldValue("offHoursContact", "hofan");
        escalationJson.addFieldValue("offHoursContactNumber", "3216549870");
        escalationJson.addFieldValue("offHoursEscalation", "xgeng");
        escalationJson.addFieldValue("offHoursEscalationNumber", "4326784529");

        String newId = entityService.create(escalationJson, context);

        escalationJson.setId(newId);

        JsonEntity queryEscalation = new JsonEntity(escalationMeta);
        queryEscalation.setBranchId(IBranch.DEFAULT_BRANCH);
        queryEscalation.setId(newId);
        entityService.delete(queryEscalation, context);
    }

    @Test
    public void deleteCheckHistory() {
        EntityContext context = newEntityContext();

        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); // created on
                                                               // main
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, newId);
        entityService.delete(qEntity, context);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet == null);
    }

    @Test
    public void modify(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        JsonEntity modifyInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String modifiedName = "Modified Name Test";
        modifyInst.addFieldValue("name", modifiedName);
        modifyInst.setId(newId);

        IEntity qEntity = buildQueryEntity(modifyInst);
        entityService.modify(qEntity, modifyInst, context);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertEquals(modifiedName, entityGet.getFieldValues("name").get(0));
        
        entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertTrue(modifiedName.equals(entityGet.getFieldValues("name").get(0)));
    }
    
    @Test
    public void modify_nochange() {
        EntityContext context = newEntityContext();
        
        IEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        newInst = entityService.get(newInst, context);
        final int oldVersion = newInst.getVersion();
        
        IEntity compute = newCompute(IBranch.DEFAULT_BRANCH);
        String modifiedName = "Modified Name Test";
        compute.addFieldValue("name", modifiedName);
        compute.setId(newId);
        compute.addFieldValue("https", true);
        compute.addFieldValue("manifestDiff", false);
        compute.addFieldValue("location", "PHX");
        entityService.create(compute, context);
        // modify to add one
        newInst.addFieldValue("runsOn", compute);
        entityService.modify(newInst, newInst, context);
        IEntity getServ = entityService.get(newInst, context);
        final int newVersion = getServ.getVersion();
        assertEquals(oldVersion + 1, newVersion);
        
        {
            // modify again
            compute = entityService.get(compute, context);
            compute.addFieldValue("name", "new-name");
            compute.addFieldValue("https", false);
            compute.addFieldValue("manifestDiff", true);
            IEntity entity = new JsonEntity(raptorMetaService.getMetaClass(SERVICE_INSTANCE));
            entity.setBranchId(IBranch.DEFAULT_BRANCH);
            entity.setId(newId);
            entity.addFieldValue("runsOn", compute);
            Assert.assertFalse(context.hasRequestTrackingCode());
            entityService.modify(entity, entity, context);
            Assert.assertTrue(context.hasRequestTrackingCode());
            Assert.assertEquals(202, context.getRequestTrackingCode().getErrorCode());
        }
    }
    
    @Test
    public void modifyWithOut_ForceUpdate(){
        EntityContext context = newEntityContext();
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        newInst.setId(newId);
        IEntity qEntity = buildQueryEntity(newInst);
        
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        int version = entityGet.getVersion();
        
        entityService.modify(qEntity, newInst, context);

        entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertEquals(version, entityGet.getVersion());
    }
    
    @Test
    public void modifyWithWrongVersion(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        JsonEntity modifyInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String modifiedName = "Modified Name Test With Wrong Version";
        modifyInst.addFieldValue("name", modifiedName);
        modifyInst.setVersion(100);
        modifyInst.setId(newId);

        IEntity qEntity = buildQueryEntity(modifyInst);
        try {
            entityService.modify(qEntity, modifyInst, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.VERSION_CONFLICT, e.getErrorEnum());
        }
    }

    @Test
    public void modifySetReferenceNull() {
        // create a service instance with a referencing compute
        EntityContext context = newEntityContext();
        JsonEntity newCompute = newCompute(IBranch.DEFAULT_BRANCH);
        JsonEntity newService = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newComputeId = entityService.create(newCompute, context);
        newCompute.setId(newComputeId);

        newService.addFieldValue("runsOn", newCompute);
        String newServiceId = entityService.create(newService, context);

        // get, modify
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE,
                newServiceId);
        JsonEntity getService = (JsonEntity) entityService.get(qEntity, context);
        Assert.assertTrue(!getService.getFieldValues("runsOn").isEmpty());
        //
        try {
            getService.addFieldValue("runsOn", null);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected - not allow null as reference value
        }
//        entityService.modify(getService, context);
//        // verify
//        getService = (JsonEntity) entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE,
//                newServiceId, context);
//        Assert.assertTrue(getService.hasField("runsOn"));
//        Assert.assertTrue(getService.getFieldValues("runsOn").size() == 1);
//        Assert.assertTrue(getService.getFieldValues("runsOn").get(0) == null);
    }

    protected static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        context.setDbConfig(config);
        return context;
    }
    
    @Test
    public void modifyNoExistingEntity(){
        EntityContext context = newEntityContext();

        JsonEntity modifyInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String modifiedName = "Modified Name Test";
        modifyInst.addFieldValue("name", modifiedName);
        
        String noExistingId = ObjectId.get().toString();
        modifyInst.setId(noExistingId);
        
        boolean hasException = false;
        try{
            IEntity qEntity = buildQueryEntity(modifyInst);
            entityService.modify(qEntity, modifyInst, context);
        }catch(CmsDalException ex){
            hasException = true;
            assertEquals(DalErrCodeEnum.ENTITY_NOT_FOUND.getErrorCode(), ex.getErrorCode());
        }
        assertTrue(hasException);
    }

    @Test
    public void replace_userSetIdNotExisting() {
        EntityContext context = newEntityContext();
        JsonEntity replaceInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        replaceInst.setId("new-id-replace-serviceisntnace");
        String replaceName = "Replaced Name Test";
        replaceInst.addFieldValue("name", replaceName);
        replaceInst.setStatus(StatusEnum.ACTIVE);
        IEntity qEntity = buildQueryEntity(replaceInst);
        entityService.replace(qEntity, replaceInst, context);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertEquals(replaceName, entityGet.getFieldValues("name").get(0));
    }
    
    @Test
    public void replace(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        JsonEntity replaceInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String replaceName = "Replaced Name Test";
        replaceInst.addFieldValue("name", replaceName);
        replaceInst.setId(newId);
        replaceInst.setStatus(StatusEnum.ACTIVE);
        IEntity qEntity = buildQueryEntity(replaceInst);
        entityService.replace(qEntity, replaceInst, context);
        
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertEquals(replaceName, entityGet.getFieldValues("name").get(0));
    }

    @Test
    public void createEmbed(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newManifest(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qMani = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "Manifest", newId);
        IEntity entityGet = entityService.get(qMani, context);
        assertNotNull(entityGet);
        
        int oldV = entityGet.getVersion();
        
        //create single manifestversion
        MetaClass versionClass = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("ManifestVersion");
        JsonEntity versionE1 = new JsonEntity(versionClass);
        versionE1.setBranchId(IBranch.DEFAULT_BRANCH);
        versionE1.addFieldValue("name", "version Entity - create embed 1");
        versionE1.addFieldValue("createdTime", new Date());
        versionE1.setId("Manifest!" + entityGet.getId() + "!versions!" + "versionObj1");
        context.setPath("Manifest!" + entityGet.getId() + "!versions");
        String versionId1 = entityService.create(versionE1, context);
        
        IEntity rootE = entityService.get(qMani, context);
        Assert.assertNotNull(rootE);
        Assert.assertEquals(oldV+1, rootE.getVersion());
        Assert.assertEquals(2, rootE.getFieldValues("versions").size());
        Assert.assertEquals(2, rootE.getFieldProperty("versions", FieldProperty.LENGTH.getName()));
        
        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "ManifestVersion", versionId1);
        IEntity embedE1 = entityService.get(qEntity, context);
        Assert.assertNotNull(embedE1);

        //create single manifestversion by embed path
        JsonEntity versionE2 = new JsonEntity(versionClass);
        versionE2.setBranchId(IBranch.DEFAULT_BRANCH);
        versionE2.addFieldValue("name", "version Entity - create embed 2");
        versionE2.addFieldValue("createdTime", new Date());
        String versionId2 = entityService.create(versionE2, context);

        rootE = entityService.get(qMani, context);
        Assert.assertNotNull(rootE);
        Assert.assertEquals(oldV+2, rootE.getVersion());
        Assert.assertEquals(3, rootE.getFieldValues("versions").size());
        Assert.assertEquals(3, rootE.getFieldProperty("versions", FieldProperty.LENGTH.getName()));
        
        qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "ManifestVersion", versionId2);
        IEntity embedE2 = entityService.get(qEntity, context);
        Assert.assertNotNull(embedE2);

        // assert the manifest version collection is not created - use search service to search
        SearchProjection sp = new SearchProjection();
        PersistenceContext persistenceContext = newPersistentContext(deployMetaService);
        sp.addField(ProjectionField.STAR);
        MetaClass manifestVersionCls = deployMetaService.getMetaClass("ManifestVersion");
        SearchQuery query = new SearchQuery(manifestVersionCls, null, sp, persistenceContext.getRegistration().searchStrategy);
        SearchServiceImpl service = new SearchServiceImpl(dataSource);
        SearchResult result = service.search(query, new SearchOption(), persistenceContext);

        IEntity getVersionEntity = (IEntity) entityGet.getFieldValues("versions").get(0);
        for (IEntity searchVersion : result.getResultSet()) {
            Assert.assertFalse(getVersionEntity.getId().contains(searchVersion.getId()));
        }
    }
    
    @Test
    public void createEmbedWithId(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newManifest(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qMani = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "Manifest", newId);
        IEntity entityGet = entityService.get(qMani, context);
        assertNotNull(entityGet);
        
        //create single manifestversion
        MetaClass versionClass = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("ManifestVersion");
        JsonEntity versionE1 = new JsonEntity(versionClass);
        versionE1.setBranchId(IBranch.DEFAULT_BRANCH);
        versionE1.addFieldValue("name", "version Entity - create embed 1");
        versionE1.addFieldValue("createdTime", new Date());
        versionE1.setId("versionObj1");
        context.setPath("Manifest!" + entityGet.getId() + "!versions");
        
        String versionId1 = entityService.create(versionE1, context);
        
        IEntity rootE = entityService.get(qMani, context);
        Assert.assertNotNull(rootE);
        Assert.assertEquals(2, rootE.getFieldValues("versions").size());
        
        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "ManifestVersion", versionId1);
        IEntity embedE1 = entityService.get(qEntity, context);
        Assert.assertNotNull(embedE1);
    }
    
    @Test
    public void createStandaloneEmbed() {
        MetaClass noUseCls = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("NoUse");
        JsonEntity noUseE = new JsonEntity(noUseCls);
        noUseE.setBranchId(IBranch.DEFAULT_BRANCH);
        noUseE.addFieldValue("name", "noUse Entity - create embed 1");
        context.setPath(null);
        try {
            entityService.create(noUseE, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.STANDALONE_EMBED, e.getErrorEnum());
        }
    }
    
    @Test
    public void deleteEmbed(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newManifest(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "Manifest", newId);
        IEntity entityGet = entityService.get(qEntity, context);
        assertNotNull(entityGet);
        
        entityService.delete(qEntity, context);
        
        entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet == null);
    }
    
    @Test
    public void modifyEmdbed(){
        EntityContext context = newEntityContext();
        context.setFetchFieldProperty(false);
        JsonEntity newInst = newManifest(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "Manifest", newId);
        IEntity entityGet = entityService.get(qEntity, context);
        assertNotNull(entityGet);
        
        String modeifyName = "Dummy Manifest for Modify " + new Random().nextInt();
        entityGet.addFieldValue("name", modeifyName);
        IEntity dEntity = buildQueryEntity(entityGet);
        entityService.modify(dEntity, entityGet, context);

        entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        assertEquals(entityGet.getFieldValues("name").get(0), modeifyName);
    }
    
    @Test
    public void replaceEmbed(){
        EntityContext context = newEntityContext();
        
        JsonEntity newInst = newManifest(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        
        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "Manifest", newId);
        IEntity rootE = entityService.get(qEntity, context);
        assertNotNull(rootE);
        
        IEntity getVersion = (IEntity) rootE.getFieldValues("versions").get(0);
        
        //create new object to clean version
        MetaClass versionCls = repoService.getRepository(DEPLOY_REPO).getMetadataService().getMetaClass("ManifestVersion");
        JsonEntity versionE = new JsonEntity(versionCls);
        versionE.setBranchId(IBranch.DEFAULT_BRANCH);
        String modeifyName = "replaceEmbed version";
        versionE.addFieldValue("name", modeifyName);
        versionE.addFieldValue("createdTime", new Date());
        versionE.setId(getVersion.getId());
        
        IEntity rEntity = buildQueryEntity(versionE);
        entityService.replace(rEntity, versionE, context);
        
        IEntity rootE1 = entityService.get(qEntity, context);
        assertEquals(rootE.getVersion() + 1, rootE1.getVersion());

        qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "ManifestVersion", versionE.getId());
        IEntity versionE1 = entityService.get(qEntity, context);
        assertNotNull(versionE1);
        assertEquals(versionE1.getFieldValues("name").get(0), modeifyName);
        List<?> desc = versionE1.getFieldValues("description");
        assertTrue(desc.isEmpty());
    }
    
    @Test
    public void testJsonEntity01(){
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);
        JsonEntity entity = new JsonEntity(instCls, newInst.getNode().toString());
        Assert.assertNotNull(entity);
    }
    
    @Test
    public void testJsonEntity02(){
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);
        JsonEntity entity = new JsonEntity(instCls, newInst.getNode().toString());
        Assert.assertNotNull(entity);
    }
    
    @Test (expected=CmsDalException.class)
    public void testJsonEntity03(){
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);
        new JsonEntity(instCls, "{a:b}");
    }
    
    @Test (expected=CmsDalException.class)
    public void testJsonEntity04(){
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);
        new JsonEntity(instCls, "{a:b}");
    }
    
    @Test (expected=CmsEntMgrException.class)
    public void testEmptyBranch01(){
        EntityContext context = newEntityContext();        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);      
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, "ErrorBranch", SERVICE_INSTANCE, newId);
        entityService.get(qEntity, context);
    }
    
    @Test (expected=CmsEntMgrException.class)
    public void testEmptyBranch02(){
        EntityContext context = newEntityContext();        
        JsonEntity newEntity = newServiceInstance(IBranch.DEFAULT_BRANCH);
        newEntity.setBranchId("ErrorBranch");
        entityService.create(newEntity, context);        
    }
    
    @Test (expected=CmsEntMgrException.class)
    public void testEmptyBranch03(){
        EntityContext context = newEntityContext();        
        JsonEntity newEntity = newServiceInstance(IBranch.DEFAULT_BRANCH);
        newEntity.setId(ObjectId.get().toString());
        newEntity.setBranchId("ErrorBranch");
        IEntity qEntity = buildQueryEntity(newEntity);
        entityService.replace(qEntity, newEntity, context);        
    }

    @Test
    public void filterGet() {
        EntityContext filterContext = newEntityContext();
        filterContext.addQueryField(InternalFieldEnum.ID.getName());
        filterContext.addQueryField("versions");

        IEntity qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, MANIFEST,
                "4fbdaccec681643199735a5b");
        IEntity filterEntity = entityService.get(qEntity, filterContext);
        Assert.assertNotNull(filterEntity);
        Assert.assertNotNull(filterEntity.getId());
        Assert.assertNotNull(filterEntity.getType());
        Assert.assertTrue(filterEntity.getFieldValues("versions").size() > 0);
        Assert.assertNull(filterEntity.getLastModified());

        // case 2:
        filterContext.addQueryField("packages"); // non existing field of
                                                 // Manifest
        try {
            entityService.get(qEntity, filterContext);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.FIELD_NOT_FOUND, e.getErrorEnum());
        }
    }
    
    protected static IEntity newApplicationService(int seq){
        MetaClass metaClass = raptorMetaService.getMetaClass("ApplicationService");
        JsonEntity newEntity = new JsonEntity(metaClass);
        newEntity.addFieldValue("name", "ApplcationService-" + seq);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        return newEntity;
    }
    
    protected static IEntity newServiceInstance(IMetadataService metaService, int seq){
        MetaClass metaClass = metaService.getMetaClass("ServiceInstance");
        JsonEntity newEntity = new JsonEntity(metaClass);
        newEntity.addFieldValue("name", "ServiceInstance-" + seq);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);      
        return newEntity;
    }
    
    protected static IEntity createServiceInstance(IMetadataService metaService, int seq){
        IEntity entity1 = newServiceInstance(metaService, seq);
        String branchId = entity1.getBranchId();
        PersistenceContext persistenceContext = newPersistentContext(metaService);
        String newId = persistenceService.create(entity1, persistenceContext);
        String metaType = entity1.getType();

        IEntity queryEntity = buildQueryEntity(metaService, branchId, newId, metaType);
        IEntity saveEntity = persistenceService.get(queryEntity, persistenceContext);
        return saveEntity;
    }
    
    @Test
    public void testStrongReferenceCreateBad(){
        try {
            IEntity service1 = createServiceInstance(raptorMetaService, 1);
            IEntity service2 = createServiceInstance(raptorMetaService, 2);
            
            IEntity qEntity = buildQueryEntity(RAPTOR_REPO, service2.getBranchId(), service2.getType(), service2.getId());
            entityService.delete(qEntity, context);
            // create application service with service instance
            IEntity application1 = newApplicationService(1);
            application1.addFieldValue("services", service1);
            application1.addFieldValue("services", service2);
            entityService.create(application1, context);
            Assert.assertFalse(true);
        }
        catch (CmsEntMgrException e) {
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
        }
    }
    
    @Test
    public void testStrongReferenceDeleteFrom(){
        try {
            // create service instance
            IEntity service1 = createServiceInstance(raptorMetaService, 1);
            String branchId = service1.getBranchId();
            String oid = service1.getId();
            String type = service1.getType();
            // create application service with service instance
            IEntity application1 = newApplicationService(2);
            application1.addFieldValue("services", service1);
			entityService.create(application1, context);
            // delete service 
            IEntity qEntity = buildQueryEntity(RAPTOR_REPO, branchId, type, oid);
            entityService.delete(qEntity, context);
            Assert.assertFalse(true);
        }
        catch (CmsEntMgrException e) {
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
        }
    }
        
    //ApplicationService->ServiceInstance (strong)
    //should be able to delete ApplicationService
    @Test
    public void testStrongReferenceDeleteTo(){
        try {
            // create service instance
            IEntity service1 = createServiceInstance(raptorMetaService, 1);
            // create application service with service instance
            IEntity application1 = newApplicationService(3);
            application1.addFieldValue("services", service1);
            entityService.create(application1, context);  
            String branchId = application1.getBranchId();
//          String oid = application1.getId();
//          String type = application1.getType();
            // delete service 
            IEntity qEntity = buildQueryEntity(RAPTOR_REPO, branchId, service1.getType(), service1.getId());
            entityService.delete(qEntity, context);
            Assert.assertFalse(true);
        }
        catch (CmsEntMgrException e) {
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
        }
    }

    @Test
    public void testUpdateJsonNullEnumField() {
        // case 0 : pass a json entity into entity service with an enum field as
        // null to update the entity
        IEntity service1 = createStratusServiceInstance(stratusMetaService, 101);

        String branchId = service1.getBranchId();
        String oid = service1.getId();
        String type = service1.getType();
        MetaClass metadata = stratusMetaService.getMetaClass(type);

        JsonEntity updateJson = new JsonEntity(metadata);
        updateJson.setBranchId(branchId);
        updateJson.setId(oid);
        String opState = "opState";

        updateJson.addFieldValue(opState, null);
        
        updateJson.addFieldValue(opState, "NORMAL");
    }

    protected static IEntity newStratusServiceInstance(IMetadataService metaService, int seq) {
        MetaClass metaClass = metaService.getMetaClass("ServiceInstance");
        IEntity newEntity = new JsonEntity(metaClass);
        newEntity.addFieldValue("description", "ServiceInstance-" + seq);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        return newEntity;
    }

    protected static IEntity createStratusServiceInstance(IMetadataService metaService, int seq) {
        IEntity entity1 = newStratusServiceInstance(metaService, seq);
        String branchId = entity1.getBranchId();
        PersistenceContext persistenceContext = newPersistentContext(metaService);
        String newId = persistenceService.create(entity1, persistenceContext);
        String metaType = entity1.getType();
        IEntity queryEntity = buildQueryEntity(metaService, branchId, newId, metaType);
        IEntity saveEntity = persistenceService.get(queryEntity, persistenceContext);
        return saveEntity;
    }
    
    @Test
    public void testCreateCrossRepository() {
        IMetadataService deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        MetaClass metadata = deployMetaService.getMetaClass("RefApplicationService");
        JsonEntity jsonEntity = new JsonEntity(metadata);
        jsonEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        jsonEntity.addFieldValue("name", generateRandomName("crossRepository"));

        // get a entity from RAPTOR-PAAS repository, and set to the creating ref
        // entity
        EntityContext filterContext = newEntityContext();
        filterContext.addQueryField(InternalFieldEnum.ID.getName());
        //entityService.setReturnBsonEntity(false);
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE,
                "4fbb314fc681caf13e283a7a");
        IEntity filterEntity = entityService.get(qEntity, filterContext);
        Assert.assertNotNull(filterEntity);
        jsonEntity.addFieldValue("services", filterEntity);

        String refId = entityService.create(jsonEntity, newEntityContext());
        Assert.assertNotNull(refId);

        // assertion
        qEntity = buildQueryEntity(DEPLOY_REPO, IBranch.DEFAULT_BRANCH, "RefApplicationService", refId);
        IEntity getEntity = entityService.get(qEntity, newEntityContext());
        Assert.assertNotNull(getEntity);
        Assert.assertNotNull(getEntity.getFieldValues("services"));
        Assert.assertNotNull(getEntity.getFieldValues("services").get(0));
        Assert.assertTrue(getEntity.getFieldValues("services").get(0) instanceof IEntity);
        Assert.assertEquals(filterEntity.getId(), ((IEntity) getEntity.getFieldValues("services").get(0)).getId());
    }

    /**
     * Cases from cms 2287 - modify ServiceInstance to append an embed ServiceAccessPoint with given id. The embed entity
     * will be appended but without _oid!
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testAppendToEmbedArrayWithoutId() {
        IEntity serviceInst = createStratusServiceInstanceWithAccessPoint(stratusMetaService, seq++);
        List<IEntity> accessPoints = (List<IEntity>) serviceInst.getFieldValues("serviceAccessPoints");
        int apNum = 0;
        for (IEntity p : accessPoints) {
            Assert.assertTrue(p.getId().trim().length() > 0);
            ++apNum;
        }

        MetaClass meta = stratusMetaService.getMetaClass("ServiceInstance");
        IEntity entity = new JsonEntity(meta);
        entity.setId(serviceInst.getId());
        entity.setBranchId(serviceInst.getBranchId());
        for (IEntity p : accessPoints) {
            entity.addFieldValue("serviceAccessPoints", p);
        }

        IEntity apWithOutId = newServiceAccessPoint(stratusMetaService, seq++);
        entity.addFieldValue("serviceAccessPoints", apWithOutId);
        IEntity qEntity = buildQueryEntity(entity);
        entityService.modify(qEntity, entity, context);

        qEntity = buildQueryEntity(serviceInst.getRepositoryName(), serviceInst.getBranchId(),
                serviceInst.getType(), serviceInst.getId());
        IEntity getInst = entityService.get(qEntity, context);
        List<JsonEntity> aps = (List<JsonEntity>) getInst.getFieldValues("serviceAccessPoints");
        Assert.assertTrue(aps.size() == apNum);
        for (IEntity e : aps) {
            Assert.assertTrue(e.hasField(InternalFieldEnum.ID.getName()));
            Assert.assertTrue(e.getId().trim().length() > 0);
        }
    }

    /**
     * Cases from cms 2287 - modify ServiceInstance by giving payload doesn't work!
     * <pre>
     * {
     *      "serviceAccessPoints": []
     * }
     * </pre>
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testSetEmbedArrayAsEmpty() {
        IEntity serviceInst = createStratusServiceInstanceWithAccessPoint(stratusMetaService, seq++);
        List<IEntity> accessPoints = (List<IEntity>) serviceInst.getFieldValues("serviceAccessPoints");
        for (IEntity p : accessPoints) {
            Assert.assertTrue(p.getId().trim().length() > 0);
        }

        MetaClass meta = stratusMetaService.getMetaClass("ServiceInstance");
        IEntity entity = new JsonEntity(meta);
        entity.setId(serviceInst.getId());
        entity.setBranchId(serviceInst.getBranchId());
        entity.setFieldValues("serviceAccessPoints", new ArrayList());
        IEntity qEntity = buildQueryEntity(entity);
        entityService.modify(qEntity, entity, context);

        PersistenceContext pContext = newPersistentContext(stratusMetaService);        
        
        IEntity queryEntity = buildQueryEntity(stratusMetaService, serviceInst.getBranchId(), serviceInst.getId(), serviceInst.getType());
        IEntity getInst = persistenceService.get(queryEntity, pContext);
        Assert.assertTrue(getInst.hasField("serviceAccessPoints"));
        Assert.assertTrue(getInst.getFieldValues("serviceAccessPoints").size() > 0);

        // get again by retrieve json entity
        qEntity = buildQueryEntity(serviceInst.getRepositoryName(), serviceInst.getBranchId(), serviceInst.getType(),
                serviceInst.getId());
        IEntity getInst2 = entityService.get(qEntity, context);
        Assert.assertTrue(getInst2.hasField("serviceAccessPoints"));
        Assert.assertTrue(getInst2.getFieldValues("serviceAccessPoints").size() > 0);
    }
    

    @Test
    public void testDeleteEmbedObject(){
        EntityContext context = newEntityContext();
        IEntity qDep = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep006");
        IEntity depGet = entityService.get(qDep, context);
        int version = depGet.getVersion();
        Assert.assertEquals(2, depGet.getFieldProperty("team", FieldProperty.LENGTH.getName()));
        
        IEntity qTeam = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep006!team!team610");
        IEntity entityGet01 = entityService.get(qTeam, context);
        Assert.assertNotNull(entityGet01);

        entityService.delete(qTeam, context);
        Assert.assertNull(entityService.get(qTeam, context));
        
        // check version and field length
        depGet = entityService.get(qDep, context);
        Assert.assertEquals(version + 1, depGet.getVersion());
        Assert.assertEquals(1, depGet.getFieldProperty("team", FieldProperty.LENGTH.getName()));
    }
    
    @Test
    public void testDeleteObjectWithEmbedObject(){
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet01);
        try {
            entityService.delete(qEntity, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertTrue(e.getMessage().indexOf("leader01") > 0);
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
        }
        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet02);
    }
    
    @Test
    public void testDeleteWithStrongReference(){
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Leader", "leader02");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet01);

        entityService.delete(qEntity, context);

        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertNull(entityGet02);
    }
    
    @Test
    public void testReplaceWithStrongReference(){
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet01);
        int teamNum = entityGet01.getFieldValues("team").size();
        entityGet01.addFieldValue("label", "cwc");

        qEntity = buildQueryEntity(entityGet01);
        entityService.replace(qEntity, entityGet01, context);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet03 = entityService.get(qEntity, context);
        List<?> list = entityGet03.getFieldValues("team");
        Assert.assertEquals(teamNum, list.size());
    }
    
    @Test
    public void testModifyWithStrongReference() {
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet01 = entityService.get(qEntity, context);
        List<?> list = entityGet01.getFieldValues("team");
        int oldTeamSize = list.size(); // original list size. NOTE the list might be changed after the modify call.
        Assert.assertNotNull(entityGet01);
        entityGet01.addFieldValue("label", "c-modify-c");
        entityService.modify(qEntity, entityGet01, context);
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertEquals(entityGet02.getFieldValues("label").get(0).toString(),"c-modify-c");
        
        IEntity newDep = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        newDep.setId("dep000");
        newDep.addFieldValue("label", "csc");
        MetaClass teamMeta = raptorMetaService.getMetaClass("Team");
        IEntity team1 = new JsonEntity(teamMeta);
        team1.setId("Dep!dep000!team!team020");
        newDep.addFieldValue("team", team1);
        newDep.setBranchId(IBranch.DEFAULT_BRANCH);

        qEntity = buildQueryEntity(newDep);
        entityService.modify(qEntity, newDep, context);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet03 = entityService.get(qEntity, context);
        list = entityGet03.getFieldValues("team");
        int newTeamSize = list.size();
        Assert.assertEquals(0, newTeamSize - oldTeamSize);
        
        Assert.assertEquals("csc", entityGet03.getFieldValues("label").get(0));
    }
    
    @Test
    public void testBatchModifyWithStrongReference(){
        EntityContext context = newEntityContext();
        IEntity newDep = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        newDep.setId("dep000");
        newDep.addFieldValue("label", "csc");
        MetaClass teamMeta = raptorMetaService.getMetaClass("Team");
        IEntity team1 = new JsonEntity(teamMeta);
        team1.setId("Dep!dep000!team!team010");
        team1.setFieldValues("name", Arrays.asList("new name 1"));
        newDep.addFieldValue("team", team1);
        
        IEntity team2 = new JsonEntity(teamMeta);
        team2.setId("Dep!dep000!team!team020");
        team2.setFieldValues("name", Arrays.asList("new name 2"));
        newDep.addFieldValue("team", team2);
        
        IEntity team3 = new JsonEntity(teamMeta);
        team3.setId("Dep!dep000!team!team030");
        newDep.addFieldValue("team", team3);
        
        newDep.setBranchId(IBranch.DEFAULT_BRANCH);
        List<IEntity> ens = new ArrayList<IEntity>();
        ens.add(newDep);

        entityService.batchModify(ens, context, new ArrayList<String>());

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet03 = entityService.get(qEntity, context);
        List<?> list = entityGet03.getFieldValues("team");
        Assert.assertEquals(2, list.size());
        IEntity newTeam1 = (IEntity)list.get(0);
        String name1 = (String)newTeam1.getFieldValues("name").get(0);
        Assert.assertEquals("dev-team-010", name1);
        IEntity newTeam2 = (IEntity)list.get(1);
        String name2 = (String)newTeam2.getFieldValues("name").get(0);
        Assert.assertEquals("dev-team-020", name2);
    }
    
    @Test
    public void testModifyFieldWithStrongReference() {
        EntityContext context = newEntityContext();
        
        JsonEntity newDep = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        newDep.setId("dep000");
        newDep.setBranchId(IBranch.DEFAULT_BRANCH);
        MetaClass teamMeta = raptorMetaService.getMetaClass("Team");
        JsonEntity team1 = new JsonEntity(teamMeta);
        team1.setId("team-new-id3");
        newDep.addFieldValue("team", team1);

        try {
            entityService.modifyField(newDep, newDep, "team", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
        }
    }
    
    @Test
    public void testDeleteFieldWithStrongReference() {
        EntityContext context = newEntityContext();
        JsonEntity newDep = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        newDep.setId("dep000");
        newDep.setBranchId(IBranch.DEFAULT_BRANCH);

        entityService.deleteField(newDep, "team", context);

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity entityGet03 = entityService.get(qEntity, context);
        List<?> list = entityGet03.getFieldValues("team");
        Assert.assertEquals(0, list.size());
    }
    
    @Test
    public void testCreateWithInactive() {
        IMetadataService metaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        MetaClass manifestMeta = metaService.getMetaClass(MANIFEST);
        MetaAttribute timeField = (MetaAttribute) manifestMeta.getFieldByName(LAST_MODIFIED_TIME);
        Assert.assertEquals("$NOW", timeField.getDefaultValue());
        
        IEntity jsonEntity = newManifest(IBranch.DEFAULT_BRANCH);
        String manifestId = entityService.create(jsonEntity, context);
        
        PersistenceContext pContext = newPersistentContext(metaService);

        IEntity queryEntity = buildQueryEntity(metaService, IBranch.DEFAULT_BRANCH, manifestId, MANIFEST);
        persistenceService.markDeleted(queryEntity, pContext);
        
        IEntity getManifest= entityService.get(queryEntity, context);
        Assert.assertNull(getManifest);
        
        jsonEntity.setId(manifestId);
        String newId = entityService.create(jsonEntity, context);
        getManifest= entityService.get(queryEntity, context);
        Assert.assertNotNull(getManifest);
        Assert.assertEquals(manifestId, newId);
    }

    protected static PersistenceContext newPersistentContext(IMetadataService metaService) {
        PersistenceContext pContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }

    @Test
    public void testModifyWithInactive() {
        IMetadataService metaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        MetaClass manifestMeta = metaService.getMetaClass(MANIFEST);
        MetaAttribute timeField = (MetaAttribute) manifestMeta.getFieldByName(LAST_MODIFIED_TIME);
        Assert.assertEquals("$NOW", timeField.getDefaultValue());
        
        IEntity jsonEntity = newManifest(IBranch.DEFAULT_BRANCH);
        String manifestId = entityService.create(jsonEntity, context);
        
        PersistenceContext pContext = newPersistentContext(metaService);

        IEntity queryEntity = buildQueryEntity(metaService, IBranch.DEFAULT_BRANCH, manifestId, MANIFEST);
        persistenceService.markDeleted(queryEntity, pContext);

        jsonEntity.addFieldValue("name", " newName");
        try {
            entityService.modify(queryEntity, jsonEntity, context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.ENTITY_NOT_ACTIVE, e.getErrorEnum());
        }
    }
    
    @Test
    public void testCreateEntityWithInnerField() {
        IEntity door = newEntity(raptorMetaService, "door10", "Door", IBranch.DEFAULT_BRANCH);
        IEntity room = newEntity(raptorMetaService, "room10", "Room", IBranch.DEFAULT_BRANCH);
        room.addFieldValue("path", door);
        
        try {
            entityService.create(room, context);
            Assert.fail();
        } catch(CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
        }
    }
    
    @Test
    public void testCreateInnerEntityInEmbed() {
        IEntity parentTest = newEntity(raptorMetaService, "parentTest1", "AllowFullTableScanParentTest", IBranch.DEFAULT_BRANCH);
        parentTest.setId("parentTest1");

        IEntity embeddedTest = newEntity(raptorMetaService, "embeddedTest1", "EmbeddedTest", IBranch.DEFAULT_BRANCH);
        embeddedTest.setId("embeddedTest1");
        
        parentTest.addFieldValue("embed", embeddedTest);
        entityService.create(parentTest, context);
        
        IEntity lock = newEntity(raptorMetaService, "lock1", "Lock", IBranch.DEFAULT_BRANCH);
        String lockHost = "AllowFullTableScanParentTest!parentTest1!embed!embeddedTest1!locks";
        context.setPath(lockHost);
        String lockId = entityService.create(lock, context);
        
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", lockId);
        IEntity queryLock = entityService.get(queryEntity, context);
        Assert.assertNotNull(queryLock);
        
        entityService.delete(parentTest, context);
        queryLock = entityService.get(queryEntity, context);
        Assert.assertNull(queryLock);
    }
    
    @Test
    public void testDeleteInnerEntityInEmbed() {
        IEntity parentTest = newEntity(raptorMetaService, "parentTest2", "AllowFullTableScanParentTest", IBranch.DEFAULT_BRANCH);
        parentTest.setId("parentTest2");

        IEntity embeddedTest = newEntity(raptorMetaService, "embeddedTest2", "EmbeddedTest", IBranch.DEFAULT_BRANCH);
        embeddedTest.setId("embeddedTest2");
        
        parentTest.addFieldValue("embed", embeddedTest);
        entityService.create(parentTest, context);
        
        IEntity lock = newEntity(raptorMetaService, "lock2", "Lock", IBranch.DEFAULT_BRANCH);
        String lockHost = "AllowFullTableScanParentTest!parentTest2!embed!embeddedTest2!locks";
        context.setPath(lockHost);
        String lockId = entityService.create(lock, context);
        
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", lockId);
        IEntity queryLock = entityService.get(queryEntity, context);

        queryEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "EmbeddedTest", "AllowFullTableScanParentTest!parentTest2!embed!embeddedTest2");
        Assert.assertNotNull(queryLock);
        try {
            entityService.deleteField(queryEntity, "locks", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
        }
        
        entityService.delete(queryLock, context);
        queryLock = entityService.get(queryLock, context);
        Assert.assertNull(queryLock);
    }

    @Test
    public void testDeleteInnerEntityByDeleteFieldEmbed() {
        IEntity parentTest = newEntity(raptorMetaService, "parentTest3", "AllowFullTableScanParentTest",
                IBranch.DEFAULT_BRANCH);
        parentTest.setId("parentTest3");
        IEntity embeddedTest = newEntity(raptorMetaService, "embeddedTest3", "EmbeddedTest", IBranch.DEFAULT_BRANCH);
        embeddedTest.setId("embeddedTest3");

        parentTest.addFieldValue("embed", embeddedTest);
        entityService.create(parentTest, context);

        IEntity lock = newEntity(raptorMetaService, "lock3", "Lock", IBranch.DEFAULT_BRANCH);
        String lockHost = "AllowFullTableScanParentTest!parentTest3!embed!embeddedTest3!locks";
        context.setPath(lockHost);
        String lockId = entityService.create(lock, context);

        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", lockId);
        IEntity queryLock = entityService.get(queryEntity, context);
        Assert.assertNotNull(queryLock);

        queryEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "AllowFullTableScanParentTest",
                "parentTest3");
        entityService.deleteField(queryEntity, "embed", context);

        queryLock = entityService.get(queryLock, context);
        Assert.assertNull(queryLock);
    }    
    
    @Test
    @SuppressWarnings("unchecked")
    public void testCreateInnerEntity() throws InterruptedException {
        IEntity door21 = newEntity(raptorMetaService, "door21", "Door", IBranch.DEFAULT_BRANCH);
        String path = "Room!room02!path";
        context.setPath(path);
        String doorId = entityService.create(door21, context);
        IEntity queryDoor = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, doorId, "Door");
        IEntity getDoor = entityService.get(queryDoor, context);
        Date originModifiedTime = getDoor.getLastModified();

        IEntity roomEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, "room02", "Room");
        IEntity getRoom = entityService.get(roomEntity, context);
        Assert.assertEquals(1, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        
        IEntity door22 = newEntity(raptorMetaService, "door22", "Door", IBranch.DEFAULT_BRANCH);
        entityService.create(door22, context);
        
        IEntity lock1 = newEntity(raptorMetaService, "lock211", "Lock", IBranch.DEFAULT_BRANCH);
        String lockHost = "Door!" + doorId + "!lock";
        context.setPath(lockHost);
        String lockId1 = entityService.create(lock1, context);
        getDoor = entityService.get(queryDoor, context);
        Date newModifiedTime = getDoor.getLastModified();
        Assert.assertTrue(newModifiedTime.after(originModifiedTime));
        
        IEntity lock2 = newEntity(raptorMetaService, "lock212", "Lock", IBranch.DEFAULT_BRANCH);
        String lockId2 = entityService.create(lock2, context);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room02");
        IEntity room = entityService.get(qEntity, context);
        IEntity getPath = (IEntity)room.getFieldValues("path").get(0);
        Assert.assertEquals(getPath.getId(), doorId);
        // check field length
        IEntity bEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, "room02", "Room");
        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity getInst = persistenceService.get(bEntity, pContext);
        Assert.assertEquals(2, getInst.getFieldProperty("path", FieldProperty.LENGTH.getName()));

        queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", doorId);
        getDoor = entityService.get(queryDoor, context);
        Assert.assertNotNull(getDoor);
        List<String> hosts = (List<String>) getDoor.getFieldValues(InternalFieldEnum.HOSTENTITY.getName());
        Assert.assertEquals(1, hosts.size());
        Assert.assertEquals(path, hosts.get(0));
        IEntity getLock1 = (IEntity)getDoor.getFieldValues("lock").get(0);
        Assert.assertEquals(getLock1.getId(), lockId1);
        IEntity getLock2 = (IEntity)getDoor.getFieldValues("lock").get(1);
        Assert.assertEquals(getLock2.getId(), lockId2);
        // check field length
        IEntity queryEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, doorId, "Door");
        IEntity getBsonDoor = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(2, getBsonDoor.getFieldProperty("lock", FieldProperty.LENGTH.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", lockId1);
        getLock1 = entityService.get(qEntity, context);
        hosts = (List<String>) getLock1.getFieldValues(InternalFieldEnum.HOSTENTITY.getName());
        Assert.assertEquals(1, hosts.size());
        Assert.assertEquals(lockHost, hosts.get(0));
        
        getLock2 = entityService.get(qEntity, context);
        hosts = (List<String>) getLock2.getFieldValues(InternalFieldEnum.HOSTENTITY.getName());
        Assert.assertEquals(1, hosts.size());
        Assert.assertEquals(lockHost, hosts.get(0));
        
        IEntity lock3 = newEntity(raptorMetaService, "lock213", "Lock", IBranch.DEFAULT_BRANCH);
        entityService.create(lock3, context);
        // check field length
        queryEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, doorId, "Door");
        getBsonDoor = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(3, getBsonDoor.getFieldProperty("lock", FieldProperty.LENGTH.getName()));
    }
    
    @Test
    public void testCreateEntity_nullId() {
        IEntity room = newEntity(raptorMetaService, "room-id-null", "Room", IBranch.DEFAULT_BRANCH);
        room.setId(null);
        // even we explicitly set a null id, the creation should not fail.
        entityService.create(room, context);
    }
    
    @Test
    public void testCreateMultiInnerEntityOnCardinalityOne() {
        IEntity lock1 = newEntity(raptorMetaService, "lock111", "Lock", IBranch.DEFAULT_BRANCH);
        String lockHost = "Door!door11!rearlock";
        context.setPath(lockHost);
        entityService.create(lock1, context);
        
        IEntity lock2 = newEntity(raptorMetaService, "lock112", "Lock", IBranch.DEFAULT_BRANCH);
        context.setPath(lockHost);
        try {
            entityService.create(lock2, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INNER_RELATIONSHIP_EXISTED, e.getErrorEnum());
        }
    }
    
    @Test
    public void testCreateInnerEntityWithoutPath() {
        IEntity door = newEntity(raptorMetaService, "door22", "Door", IBranch.DEFAULT_BRANCH);
        context.setPath(null);
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH);
        }
    }
    
    @Test
    public void testCreateInnerEntityWithInvalidPath() {
        IEntity door = newEntity(raptorMetaService, "door22", "Door", IBranch.DEFAULT_BRANCH);

        door.setHostEntity("");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }

        door.setHostEntity("room!room00!path");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }
        
        door.setHostEntity("Room!room00!name");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }
        
        door.setHostEntity("Room!room00!door");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }
        
        door.setHostEntity("Room!room00!path");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.ENTITY_NOT_FOUND, e.getErrorEnum());
        }
        
        door.setHostEntity("Lock!room02!path");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }
        
        door.setHostEntity("Room!room02!door");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, e.getErrorEnum());
        }
        
        door.setHostEntity("Room!room01!nnnn");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH);
        }
        
        door.setHostEntity("Room!room01");
        try {
            entityService.create(door, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH);
        }
    }
    
    @Test
    public void testCreateInnerEntityWithId() {
        IEntity door = newEntity(raptorMetaService, "door31", "Door", IBranch.DEFAULT_BRANCH);
        String id = "door31";
        door.setId(id);
        door.setHostEntity("Room!room03!path");

        String doorId = entityService.create(door, context);
        Assert.assertEquals(id, doorId);
        
        IEntity lock = newEntity(raptorMetaService, "lock311", "Lock", IBranch.DEFAULT_BRANCH);
        String lockId = "lock311";
        lock.setId(lockId);
        lock.setHostEntity("Door!door31!lock");

        String newlockId = entityService.create(lock, context);
        Assert.assertEquals(lockId, newlockId);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room03");
        IEntity room = entityService.get(qEntity, context);
        IEntity getPath = (IEntity)room.getFieldValues("path").get(0);
        Assert.assertEquals(getPath.getType(), "Door");
        Assert.assertEquals(getPath.getId(), doorId);

        IEntity queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", doorId);
        IEntity getDoor = entityService.get(queryDoor, context);
        Assert.assertNotNull(getDoor);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteInnerEntity() {
        // check field length
        IEntity queryEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, "room04", "Room");
        IEntity getRoom = entityService.get(queryEntity, context);
        Assert.assertEquals(2, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        
        // delete inner
        IEntity queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door41");
        entityService.delete(queryDoor, context);
        
        getRoom = entityService.get(queryEntity, context);
        Assert.assertEquals(1, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        
        IEntity queryLock = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", "lock411");
        IEntity lock1 = entityService.get(queryLock, context);
        Assert.assertNull(lock1);
        
        queryLock = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", "lock412");
        IEntity lock2 = entityService.get(queryLock, context);
        Assert.assertNull(lock2);
        
        IEntity door = entityService.get(queryDoor, context);
        Assert.assertNull(door);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room04");
        IEntity room = entityService.get(qEntity, context);
        Assert.assertNotNull(room);
        
        List<IEntity> pathList = (List<IEntity>)room.getFieldValues("path");
        Assert.assertEquals(1, pathList.size());
        IEntity getDoor = pathList.get(0);
        Assert.assertEquals("door42", getDoor.getId());
        
        // delete inner
        IEntity queryDoor42 = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door42");
        entityService.delete(queryDoor42, context);
        
        getRoom = entityService.get(queryEntity, context);
        Assert.assertEquals(0, getRoom.getFieldValues("path").size());
        Assert.assertEquals(0, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        
        // create inner
        IEntity door43 = newEntity(raptorMetaService, "door43", "Door", IBranch.DEFAULT_BRANCH);
        String path = "Room!room04!path";
        context.setPath(path);
        String doorId = entityService.create(door43, context);
        
        getRoom = entityService.get(queryEntity, context);
        Assert.assertEquals(1, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        getDoor = (IEntity)getRoom.getFieldValues("path").get(0);
        Assert.assertEquals(doorId, getDoor.getId());
    }
    
    @Test
    public void testDeleteNonExistingInnerEntity() {
        IEntity queryRoom = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room04");
        JsonEntity door = new JsonEntity(raptorMetaService.getMetaClass("Door"));
        door.setId("door45");
        queryRoom.addFieldValue("path", door);
        try {
            entityService.deleteField(queryRoom, "path", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }
    }
    
    @Test
    public void testDeleteHostEntityWithInnerEntity() {
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room05");
        entityService.delete(qEntity, context);
        IEntity room = entityService.get(qEntity, context);
        Assert.assertNull(room);
        
        String id = "door51";
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", id);
        IEntity door1 = entityService.get(qEntity, context);
        Assert.assertNull(door1);

        id = "door52";
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", id);
        IEntity door2 = entityService.get(qEntity, context);
        Assert.assertNull(door2);
        
        id = "lock511";
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", id);
        IEntity lock1 = entityService.get(qEntity, context);
        Assert.assertNull(lock1);
        
        id = "lock512";
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", id);
        IEntity lock2 = entityService.get(qEntity, context);
        Assert.assertNull(lock2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteInnerRelationship() {
        IEntity queryRoom = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room06");
        // check field length
        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(raptorMetaService, IBranch.DEFAULT_BRANCH, "room06", "Room");
        IEntity getRoom = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(2, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));

        JsonEntity door61 = new JsonEntity(raptorMetaService.getMetaClass("Door"));
        door61.setId("door61");
        queryRoom.addFieldValue("path", door61);
        try {
            entityService.deleteField(queryRoom, "path", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }
        // data should not be touched
        IEntity queryLock = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", "lock611");
        IEntity lock1 = entityService.get(queryLock, context);
        Assert.assertNotNull(lock1);
        // data should not be touched
        IEntity queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door61");
        IEntity door = entityService.get(queryDoor, context);
        Assert.assertNotNull(door);
        // data should not be touched
        IEntity room06 = entityService.get(queryRoom, context);
        List<IEntity> doors = (List<IEntity>)room06.getFieldValues("path");
        Assert.assertEquals(2, doors.size());
        Assert.assertEquals("door61", doors.get(0).getId());
        // check field length - data should not be touched
        getRoom = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(2, getRoom.getFieldProperty("path", FieldProperty.LENGTH.getName()));
        
        queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door62");
        JsonEntity lock621 = new JsonEntity(raptorMetaService.getMetaClass("Lock"));
        lock621.setId("lock621");
        queryDoor.addFieldValue("lock", lock621);
        try {
            entityService.deleteField(queryDoor, "lock", context);
            Assert.fail();
        } catch (CmsEntMgrException ceme) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE, ceme.getErrorEnum());
        }
        // data should not be touched
        queryLock = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Lock", "lock621");
        IEntity lock = entityService.get(queryLock, context);
        Assert.assertNotNull(lock);
        // data should not be touched
        queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door62");
        door = entityService.get(queryDoor, context);
        List<IEntity> locks = (List<IEntity>)door.getFieldValues("lock");
        Assert.assertEquals(1, locks.size());
    }

    @Test
    public void testModifyInnerRelationship() {
        IEntity queryRoom = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room01");

        JsonEntity door12 = new JsonEntity(raptorMetaService.getMetaClass("Door"));
        door12.setId("door12");
        queryRoom.addFieldValue("path", door12);
        try {
            entityService.modify(queryRoom, queryRoom, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }
    }
    
    @Test
    public void testModifyFieldInnerRelationship() {
        IEntity queryRoom = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room01");

        JsonEntity door12 = new JsonEntity(raptorMetaService.getMetaClass("Door"));
        door12.setId("door12");
        queryRoom.addFieldValue("path", door12);
        try {
            entityService.modifyField(queryRoom, queryRoom, "path", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }
    }
    
    @Test
    public void testModifyInner() {
        int maxIndexedArraySize = 3;
        RepositoryOption option = new RepositoryOption();
        option.setMaxIndexedArraySize(maxIndexedArraySize);
        Repository repo = repoService.getRepository(RAPTOR_REPO);
        RepositoryOption oldOption = repo.getOptions();
        repo.setOptions(option);
        
        EntityContext context = newEntityContext();
        IEntity door11 = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door11");
        door11.setId("door11");
        entityService.modify(door11, door11, context);
        repo.setOptions(oldOption);
    }
    
    @Test
    public void testModifyHostEntityByModifyField() {
        IEntity queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door11");
        queryDoor.addFieldValue("name", "newdoor-11");
        queryDoor.addFieldValue("_hostentity", "invalid");
        try {
            entityService.modifyField(queryDoor, queryDoor, "_hostentity", context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Can not update internal field _hostentity!", e.getMessage());
        }
    }
    
    @Test
    public void testModifyHostEntityByModify() {
        IEntity queryDoor = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Door", "door11");
        queryDoor.addFieldValue("name", "newdoor11");
        queryDoor.addFieldValue("_hostentity", "invalid");

        entityService.modify(queryDoor, queryDoor, context);

        IEntity getDoor = entityService.get(queryDoor, context);
        String name = (String)getDoor.getFieldValues("name").get(0);
        Assert.assertEquals("newdoor11", name);
        String host = (String)getDoor.getFieldValues("_hostentity").get(0);
        Assert.assertEquals("Room!room01!path", host);
    }
    
    @Test
    public void testReplaceWithInnerRelationship() {
        IEntity queryRoom = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Room", "room01");

        JsonEntity door12 = new JsonEntity(raptorMetaService.getMetaClass("Door"));
        door12.setId("door12");
        queryRoom.addFieldValue("path", door12);
        try {
            entityService.replace(queryRoom, queryRoom, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.META_CONTAINS_INNER_RELATIONSHIP);
        }
    }
    
    @Test
    public void testDeleteFieldWithExpression() {
        JsonEntity entity = newEnvironmentEntity(raptorMetaService);
        entity.addFieldValue("numService",  Integer.valueOf(10));
        entity.addFieldValue("numServices", Integer.valueOf(2));

        String createId = entityService.create(entity, context);
        entity.setId(createId);

        IEntity getApp = entityService.get(entity, context);
        Long val = (Long)getApp.getFieldValues("total").get(0);
        Assert.assertEquals(Long.valueOf(12), val);

        getApp.removeField("numServices");
        entityService.deleteField(getApp, "numServices", context);

        getApp = entityService.get(entity, context);
        Long newVal = (Long)getApp.getFieldValues("total").get(0);
        Assert.assertEquals(Long.valueOf(10), newVal);
    }

    @Test
    public void testDeleteExpressionField() {
        JsonEntity entity = newEnvironmentEntity(raptorMetaService);
        entity.addFieldValue("numService", Integer.valueOf(10));
        entity.addFieldValue("numServices", Integer.valueOf(2));
        entity.addFieldValue("total", Integer.valueOf(1));

        String createId = entityService.create(entity, context);
        entity.setId(createId);

        IEntity getApp = entityService.get(entity, context);
        
        Long val = (Long)getApp.getFieldValues("total").get(0);
        Assert.assertEquals(Long.valueOf(12), val);
        
        try {
            entityService.deleteField(entity, "total", context);
            Assert.fail();
        } catch(CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.CANNOT_DELETE_EXPRESSION_FIELD, e.getErrorEnum());
        }
    }
    
    private static class ACLCheckCallback implements IEntityOperationCallback {
        @Override
        public boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context) {
            if (op != Operation.CREATE) {
                Assert.assertNotNull(existingEntity);
                Assert.assertFalse(newEntity.getFieldValues("team").isEmpty());
                Assert.assertEquals("Dep", existingEntity.getMetaClass().getName());
                Assert.assertEquals(Operation.MODIFY, op);
            }
            Assert.assertEquals("Dep", newEntity.getMetaClass().getName());

            return true;
        }
    }
    
    @Test
    public void testEntityOperationCallback() {
        entityService.setCallback(new ACLCheckCallback());
        context.setPath(null);
        IEntity dep = newEntity(raptorMetaService, "dep21", "Dep", IBranch.DEFAULT_BRANCH);
        String createId = entityService.create(dep, context);
        dep.setId(createId);
        
        IEntity queryDep = buildQueryEntity(dep);

        IEntity team1 = newEntity(raptorMetaService, "team21", "Team", IBranch.DEFAULT_BRANCH);
        dep.addFieldValue("team", team1);
        try {
            entityService.modifyField(queryDep, dep, "team", context);
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
        }

        team1.setId("Dep!" + createId + "!team!team21");
        entityService.create(team1, context);
        
        IEntity team2 = newEntity(raptorMetaService, "team22", "Team", IBranch.DEFAULT_BRANCH);
        IEntity newDep = entityService.get(dep, context);
        newDep.addFieldValue("team", team2);
        entityService.modify(queryDep, newDep, context);
        
        IEntity person1 = newEntity(raptorMetaService, "person21", "Person", IBranch.DEFAULT_BRANCH);
        person1.setId(team1.getId() + "!person!person21");
        entityService.create(person1, context);
        
        IEntity queryTeam = buildQueryEntity(team1);
        
        IEntity person2 = newEntity(raptorMetaService, "person22", "Person", IBranch.DEFAULT_BRANCH);
        IEntity newTeam1 = entityService.get(queryTeam, context);
        newTeam1.addFieldValue("person", person2);
        entityService.modify(queryTeam, newTeam1, context);
        
        IEntity person3 = newEntity(raptorMetaService, "person23", "Person", IBranch.DEFAULT_BRANCH);
        IEntity newTeam2 = entityService.get(queryTeam, context);
        newTeam2.addFieldValue("person", person3);
        entityService.replace(queryTeam, newTeam2, context);
        
        entityService.deleteField(queryTeam, "person", context);
        
        entityService.setCallback(null);
    }

    private JsonEntity newEnvironmentEntity(IMetadataService metaService) {
        MetaClass meta = metaService.getMetaClass("Environment");
        JsonEntity env = new JsonEntity(meta);
        env.setBranchId(IBranch.DEFAULT_BRANCH);
        return env;
    }

    protected static int seq = 0;
    private static CMSDBConfig config;
    
    protected IEntity newEntity(IMetadataService metaService, String name, String type, String branchId) {
        MetaClass meta = metaService.getMetaClass(type);
        IEntity entity = new JsonEntity(meta);
        entity.addFieldValue("name", name);
        entity.setBranchId(branchId);
        return entity;
    }

    protected IEntity newServiceAccessPoint(IMetadataService metaService, int seq) {
        MetaClass meta = metaService.getMetaClass("ServiceAccessPoint");
        IEntity accessPoint = new JsonEntity(meta);
        accessPoint.addFieldValue("label", generateRandomName("accessPoint") + "-" + seq);
        accessPoint.addFieldValue("port", 80);
        accessPoint.addFieldValue("protocol", "tcp");
        return accessPoint;
    }

    protected IEntity createStratusServiceInstanceWithAccessPoint(IMetadataService metaService, int seq) {
        IEntity stratusServiceInst = newStratusServiceInstance(metaService, seq++);
        IEntity accessPoint1 = newServiceAccessPoint(metaService, seq++);
        IEntity accessPoint2 = newServiceAccessPoint(metaService, seq++);
        stratusServiceInst.addFieldValue("serviceAccessPoints", accessPoint1);
        stratusServiceInst.addFieldValue("serviceAccessPoints", accessPoint2);

        String instId = entityService.create(stratusServiceInst, context);
        PersistenceContext pContext = newPersistentContext(metaService);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        IEntity queryEntity = buildQueryEntity(metaService, stratusServiceInst.getBranchId(), instId, stratusServiceInst.getType());
        IEntity getInst = persistenceService.get(queryEntity, pContext);
        return getInst;
    }
    
    private JsonEntity buildQueryEntity(IEntity entity) {
        return buildQueryEntity(entity.getRepositoryName(), entity.getBranchId(), entity.getType(), entity.getId());
    }
    
    private JsonEntity buildQueryEntity(String reponame, String branchname, String metadata, String oid) {
        MetaClass meta = repoService.getRepository(reponame).getMetadataService().getMetaClass(metadata);
        JsonEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

    private static IEntity buildQueryEntity(IMetadataService metaService, String branchname, String oid, String metadata) {
        MetaClass meta = metaService.getMetaClass(metadata);
        JsonEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }
    
    @Test
    public void testHashedIndex() {
        MetaClass meta = new MetaClass();
        meta.setName("HashedClass");
        meta.setRepository(RAPTOR_REPO);
        MetaAttribute strAttr = new MetaAttribute();
        strAttr.setName("stringField");
        strAttr.setDataType(DataTypeEnum.STRING);
        meta.addField(strAttr);

        MetaAttribute intAttr = new MetaAttribute();
        intAttr.setName("intField");
        intAttr.setDataType(DataTypeEnum.INTEGER);
        meta.addField(intAttr);

        IndexInfo ii = new IndexInfo("hashIdx");
        ii.addKeyField(intAttr.getName());
        ii.addKeyField(strAttr.getName());
        ii.addOption(IndexOptionEnum.hashed);
        ii.addOption(IndexOptionEnum.unique);
        meta.addIndex(ii);

        try {
            raptorMetaService.createMetaClass(meta, new MetadataContext());
            Assert.fail();
        } catch (IllegalIndexException iie) {
            // expected
            iie.getMessage().contains("uniqueness");
        }
        try {
            ii.removeOption(IndexOptionEnum.unique);
            raptorMetaService.createMetaClass(meta, new MetadataContext());
            Assert.fail();
        } catch (IllegalIndexException iie) {
            // expected
            iie.getMessage().contains("single");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnumMetaChange_ok() {
        entityService.setCallback(null);
        // newly create meta class with enum; create an entity; then change the
        // enum value list; try to get/query the given entity
        MetaClass newMeta = new MetaClass();
        newMeta.setName("EnumModelTest");
        newMeta.setRepository(raptorMetaService.getRepository().getRepositoryName());

        MetaAttribute enumAttr = new MetaAttribute();
        enumAttr.setName("enumAttr");
        enumAttr.setDataType(DataTypeEnum.ENUM);
        enumAttr.addEnumValue("datamodel");

        newMeta.addField(enumAttr);
        // create meta
        raptorMetaService.createMetaClass(newMeta, new MetadataContext());
        MetaClass getMeta = raptorMetaService.getMetaClass(newMeta.getName());
        // create entity
        JsonEntity newEntity = new JsonEntity(getMeta);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue(enumAttr.getName(), "datamodel");
        String newId = entityService.create(newEntity, context);
        newEntity.setId(newId);
        Assert.assertNotNull(entityService.get(newEntity, context));
        // now change enum meta
        MetaAttribute newEnumAttr = new MetaAttribute();
        newEnumAttr.setName("enumAttr");
        newEnumAttr.setDataType(DataTypeEnum.ENUM);
        newEnumAttr.addEnumValue("model");
        MetaClass updateMeta = new MetaClass();
        updateMeta.setName("EnumModelTest");
        updateMeta.setRepository(raptorMetaService.getRepository().getRepositoryName());
        updateMeta.addField(newEnumAttr);
        raptorMetaService.updateMetaField(updateMeta, newEnumAttr.getName(), metaContext);

        // try to get the entity with old data
        IEntity getEntity = entityService.get(newEntity, context);
        Assert.assertNotNull(getEntity);
        List<String> getEnumValues = (List<String>) getEntity.getFieldValues(enumAttr.getName());
        Assert.assertNotNull(getEnumValues);
        Assert.assertEquals(1, getEnumValues.size());
    }

    public void testCreateWithOverride() {
        MetaClass empMeta = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Employee");
        MetaClass workerMeta = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Worker");

        JsonEntity empEntity = new JsonEntity(empMeta);
        empEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        empEntity.addFieldValue("name", "emp1");

        String empId = entityService.create(empEntity, context);

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Employee", empId);
        IEntity getEmp = entityService.get(qEntity, context);
        List<?> ageList = getEmp.getFieldValues("age");
        Assert.assertEquals(0, ageList.size());
        List<?> addrList = getEmp.getFieldValues("address");
        Assert.assertEquals("", addrList.get(0));

        empEntity.addFieldValue("title", "staff");
        try {
            entityService.create(empEntity, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {

        }

        JsonEntity workerEntity = new JsonEntity(workerMeta);
        workerEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        workerEntity.addFieldValue("name", "worker1");
        workerEntity.addFieldValue("title", "staff");

        try {
            entityService.create(workerEntity, context);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, e.getErrorEnum());
        }

        workerEntity.addFieldValue("address", "dummy address");
        String workerId = entityService.create(workerEntity, context);
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Worker", workerId);
        IEntity getWorker = entityService.get(qEntity, context);

        ageList = getWorker.getFieldValues("age");
        Assert.assertEquals(28, ageList.get(0));
        addrList = getWorker.getFieldValues("address");
        Assert.assertEquals("dummy address", addrList.get(0));
    }
    
    @Test(expected = CmsDalException.class)
    @Ignore
    public void testCheckDocumentSize() {
    	Long maxDocumentSize = 10L;
    	RepositoryOption option = new RepositoryOption();
    	option.setMaxDocumentSize(maxDocumentSize);
    	
    	Repository repo = repoService.getRepository(RAPTOR_REPO);
    	RepositoryOption oldOption = repo.getOptions();
    	repo.setOptions(option);
    	
    	IEntityService spy = PowerMockito.spy(entityService);
		try {
			PowerMockito.doReturn(repo).when(spy, "getRepository", Mockito.any(PersistenceContext.class));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
    	
    	IEntity room = newEntity(raptorMetaService, "room-testCheckDocumentSize", "Room", IBranch.DEFAULT_BRANCH);
    	spy.create(room, context);
    	
    	// reset repositoryOption
    	repo.setOptions(oldOption);
    }
    
//    @Test(expected = CmsDalException.class)
//    public void testCheckRepositorySize() {
//    	Long maxRepositorySize = 10L;
//    	RepositoryOption option = new RepositoryOption();
//    	option.setMaxRepositorySize(maxRepositorySize);
//    	
//    	Repository repo = repoService.getRepository(RAPTOR_REPO);
//    	RepositoryOption oldOption = repo.getOptions();
//    	repo.setOptions(option);
//    	
//    	IEntityService spy = PowerMockito.spy(entityService);
//		try {
//			PowerMockito.doReturn(repo).when(spy, "getRepository", Mockito.any(PersistenceContext.class));
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//		
//		Map<String, Object> databaseSizeMap = new HashMap<String, Object>();
//		databaseSizeMap.put(RAPTOR_REPO, maxRepositorySize);
//		
//		EntityContext ec = Mockito.mock(EntityContext.class);
//		Mockito.when(ec.getDatabaseSizeMap()).thenReturn(databaseSizeMap);
//		
//    	IEntity room = newEntity(raptorMetaService, "room-testCheckRepositorySize", "Room", IBranch.DEFAULT_BRANCH);
//    	spy.create(room, ec);
//    	
//    	// reset repositoryOption
//    	repo.setOptions(oldOption);
//    }
    
//    @Test(expected = CmsDalException.class)
//    public void testCheckIndexedArraySize() {
//    	MetaClass mClass = new MetaClass();
//    	String metaClassName = "TestIndexedArraySize";
//		mClass.setName(metaClassName);
//		mClass.setRepository(RAPTOR_REPO);
//		
//		MetaAttribute a1 = new MetaAttribute();
//		String fieldName = "name";
//		a1.setName(fieldName);
//		a1.setDataType(DataTypeEnum.STRING);
//		mClass.addField(a1);
//		
//		MetaAttribute a2 = new MetaAttribute();
//		String arrayFieldName = "arrayFieldName";
//		a2.setName(arrayFieldName);
//		a2.setCardinality(CardinalityEnum.Many);
//		mClass.addField(a2);
//		
//		IndexInfo index = new IndexInfo("arrayFieldNameIndex");
//		index.addKeyField(arrayFieldName);
//		mClass.getOptions().addIndex(index);
//		
//		raptorMetaService.createMetaClass(mClass, new MetadataContext());
//    	
//    	int maxIndexedArraySize = 3;
//    	RepositoryOption option = new RepositoryOption();
//    	option.setMaxIndexedArraySize(maxIndexedArraySize);
//    	
//    	Repository repo = repoService.getRepository(RAPTOR_REPO);
//    	RepositoryOption oldOption = repo.getOptions();
//    	repo.setOptions(option);
//    	
//    	IEntityService spy = PowerMockito.spy(entityService);
//		try {
//			PowerMockito.doReturn(repo).when(spy, "getRepository", Mockito.any(PersistenceContext.class));
//		} catch (Exception e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//    	
//    	IEntity entity = newEntity(raptorMetaService, "testCheckIndexedArraySize", metaClassName, IBranch.DEFAULT_BRANCH);
//    	
//    	String[] fieldValues = {"junior", "staff", "senior", "principal"};
//    	entity.setFieldValues(arrayFieldName, Arrays.asList(fieldValues));
//    	spy.create(entity, context);
//    	
//    	// reset repositoryOption
//    	repo.setOptions(oldOption);
//    }

    /**
     * CMS-4107
     */
    @Test
    public void mulitple_thread_create_get() {
        final IMetadataService metaService = cmsdbMetaService;
        final IEntityService entityService = EntityServiceTest.entityService;

        final int THREAD_COUNT = 10;
        final int THREAD_LOOP = 10;
        final CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        final MetadataContext metaContext = new MetadataContext(true, true);
        class Run implements Runnable {
            int count = THREAD_LOOP;

            @Override
            public void run() {
                while (count-- > 0) {
                    System.out.println("start one round of create and delete!");
                    MetaClass asMeta = metaService.getMetaClass("AssetServer");
                    JsonEntity assetServer = new JsonEntity(asMeta);
                    assetServer.setBranchId(IBranch.DEFAULT_BRANCH);
                    assetServer.addFieldValue("type", "vm");
                    assetServer.addFieldValue("isAllocated", false);
                    assetServer.addFieldValue("resourceId", "resource-asset-server" + Thread.currentThread().getId());
                    String id = entityService.create(assetServer, context);

                    // refresh the metadatas
                    metaService.getMetaClasses(metaContext);
                    MetaClass newAsMeta = metaService.getMetaClass("AssetServer");
                    Assert.assertTrue(asMeta != newAsMeta);
                    assetServer = new JsonEntity(newAsMeta);
                    assetServer.setBranchId(IBranch.DEFAULT_BRANCH);
                    assetServer.setId(id);
                    entityService.delete(assetServer, context);
                }
                System.out.println("Thread end :" + Thread.currentThread().getId());
                latch.countDown();
            }
        }
        
        ThreadPoolExecutor service = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            service.execute(new Run());
        }
        try {
            Thread.sleep(3000);
            service.shutdown();
            service.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            List<Runnable> notRun = (List<Runnable>) service.shutdownNow();
            int totalNotRunCount = 0;
            for (Runnable r : notRun) {
                totalNotRunCount += ((Run) r).count;
            }
            System.out.println(String.format("Pending running count : %d of total %d", totalNotRunCount, THREAD_COUNT
                    * THREAD_LOOP));
        }
    }

    @Test
    public void testPushFieldToEmptyList() {
        int maxIndexedArraySize = 3;
        RepositoryOption option = new RepositoryOption();
        option.setMaxIndexedArraySize(maxIndexedArraySize);
        Repository repo = repoService.getRepository(RAPTOR_REPO);
        RepositoryOption oldOption = repo.getOptions();
        repo.setOptions(option);
        
        String fieldName = "preManifestRef";
        EntityContext context = newEntityContext();
        
        JsonEntity newApp = new JsonEntity(raptorMetaService.getMetaClass("ApplicationService"));
        newApp.setId("ApplicationService001");
        newApp.setBranchId(IBranch.DEFAULT_BRANCH);
        entityService.create(newApp, context);
        
        newApp.addFieldValue(fieldName, "test1");
        context.setModifyAction(ModifyAction.PUSHFIELD);
        entityService.modifyField(newApp, newApp, fieldName, context);
        
        repo.setOptions(oldOption);
    }
    
    @Test
    public void testCheckSystemLimitByAddInnerToExistingHostEntity() {
    	int maxIndexedArraySize = 3;
        RepositoryOption option = new RepositoryOption();
        option.setMaxIndexedArraySize(maxIndexedArraySize);
        Repository repo = repoService.getRepository(RAPTOR_REPO);
        RepositoryOption oldOption = repo.getOptions();
        repo.setOptions(option);
        
        IEntity room = newEntity(raptorMetaService, "room51", "Room", IBranch.DEFAULT_BRANCH);
        room.setId("room51");
        
        String roomOid = entityService.create(room, context);
        Assert.assertEquals("room51", roomOid);
        
        IEntity door = newEntity(raptorMetaService, "door511", "Door", IBranch.DEFAULT_BRANCH);
        door.setId("door511");
        String path = "Room!room51!path";
        context.setPath(path);
        
        String doorOid = entityService.create(door, context);
        Assert.assertEquals("door511", doorOid);
        
        repo.setOptions(oldOption);
    }
    
 }
