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

package com.ebay.cloud.cms.entmgr.entity.impl;

import java.util.ArrayList;
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
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author liasu
 * 
 */
@SuppressWarnings("unused")
public class FieldEntityServiceTest extends CMSMongoTest {

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

    private static int seq = 0;
    private static MetadataContext metaContext = null; 

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

        metaContext = new MetadataContext();
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(metaContext);
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

    private JsonEntity newServiceInstance(String branchId) {
        String metaType = SERVICE_INSTANCE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);     
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test");
        return newEntity;
    }
    
    private JsonEntity newEnvironment(String branchId) {
        String metaType = ENVIRONMENT;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);     
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Environment");
        return newEntity;
    }
    
    private IEntity createEnvironment() {
        JsonEntity jEntity = newEnvironment(MAIN_BRANCH);
        String id = entityService.create(jEntity, context);
        IEntity qEntity = buildQueryEntity(jEntity.getRepositoryName(), jEntity.getBranchId(), jEntity.getType(),id);
        return entityService.get(qEntity, context);
    }

    private IEntity createServiceInstance() {
        JsonEntity jEntity = newServiceInstance(MAIN_BRANCH);
        String id = entityService.create(jEntity, context);
        IEntity qEntity = buildQueryEntity(jEntity.getRepositoryName(), jEntity.getBranchId(), jEntity.getType(),
                id);
        return entityService.get(qEntity, context);
    }

//    private IEntity createServiceInstanceWithManifestRef() {
//        String manfiestRef = "manifestRef";
//        JsonEntity entity = newServiceInstance(MAIN_BRANCH);
//        entity.addFieldValue(manfiestRef, "manifest-1.0.0");
//        entity.addFieldValue(manfiestRef, "manifest-1.0.1");
//        entity.addFieldValue(manfiestRef, "manifest-1.0.2");
//        
//        String id = entityService.create(entity, context);
//        IEntity getEntity = entityService.get(RAPTOR_REPO, MAIN_BRANCH, SERVICE_INSTANCE, id, context);
//        return getEntity;
//    }
    
    private IEntity createServiceInstanceWithManifestRef() {
        String manifestRef = "manifestRef";
        JsonEntity entity = newServiceInstance(MAIN_BRANCH);
        entity.addFieldValue(manifestRef, "manifest-1.0.0");
        entity.addFieldValue(manifestRef, "manifest-1.0.1");
        entity.addFieldValue(manifestRef, "manifest-1.0.2");
        
        String actmanifest = "activeManifestRef";
        entity.addFieldValue(actmanifest, "manifest-1.0.0");
        entity.addFieldValue(actmanifest, "manifest-1.0.1");
        entity.addFieldValue(actmanifest, "manifest-1.0.2");
        
        String id = entityService.create(entity, context);
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, SERVICE_INSTANCE, id);
        IEntity getEntity = entityService.get(qEntity, context);
        return getEntity;
    }

    private IEntity createServiceInstanceWithProperty() {
        String properties= "properties";
        JsonEntity entity = newServiceInstance(MAIN_BRANCH);
        entity.addFieldValue(properties, "{\"f1\" : 1 , \"f2\": [\"embed array\", 3]}");
        String id = entityService.create(entity, context);
        IEntity qEntity =buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, SERVICE_INSTANCE, id);
        IEntity getEntity = entityService.get(qEntity, context);
        return getEntity;
    }
    
    @Test
    public void test01ModifyArrayField() {
        String manifestRef = "manifestRef";
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        
        MetaClass instCls = metaService.getMetaClass(SERVICE_INSTANCE);

        MetaAttribute equalsTo = new MetaAttribute(false);
        String fieldName = "equalsTo";
		equalsTo.setName(fieldName);
        equalsTo.setDataType(DataTypeEnum.BOOLEAN);
        equalsTo.setDbName(fieldName);
        String expr = "[x for each (x in $manifestRef) if ($activeManifestRef.indexOf(x)==-1)].length==0 "
                + "&& [x for each (x in $activeManifestRef) if ($manifestRef.indexOf(x)==-1)].length==0";
        equalsTo.setExpression(expr);
        instCls.addField(equalsTo);
        
        IEntity getEntity = createServiceInstanceWithManifestRef();
        Assert.assertNotNull(getEntity.getFieldValues(manifestRef));
        Assert.assertEquals(3, getEntity.getFieldValues(manifestRef).size());
        Assert.assertTrue((Boolean)getEntity.getFieldValues(fieldName).get(0));

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(getEntity.getBranchId());
        modifyEntity.setId(getEntity.getId());
        modifyEntity.addFieldValue(manifestRef, "new manifest");
        entityService.modifyField(modifyEntity, modifyEntity, manifestRef, context);
        
        IEntity qEntity = buildQueryEntity(getEntity);
        IEntity newGet = entityService.get(qEntity, context);
        Assert.assertNotNull(newGet.getFieldValues(manifestRef));
        Assert.assertEquals(1, newGet.getFieldValues(manifestRef).size() - getEntity.getFieldValues(manifestRef).size());
        Assert.assertFalse((Boolean)newGet.getFieldValues(fieldName).get(0));
    }
    
    @Test
    public void test01ModifyArrayFieldWithDupContents() {
        String manfiestRef = "manifestRef";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);
        MetaField mf = instCls.getFieldByName(manfiestRef);

        IEntity getEntity = (IEntity)createServiceInstanceWithManifestRef();
        Assert.assertNotNull(getEntity.getFieldValues(manfiestRef));
        Assert.assertEquals(3, getEntity.getFieldValues(manfiestRef).size());
        Assert.assertEquals(3, getEntity.getFieldProperty(manfiestRef, FieldProperty.LENGTH.getName()));

        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(getEntity);
        IEntity bEntity = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(3, bEntity.getFieldProperty(manfiestRef, FieldProperty.LENGTH.getName()));

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(getEntity.getBranchId());
        modifyEntity.setId(getEntity.getId());
        modifyEntity.addFieldValue(manfiestRef, "new manifest");
        for (Object o : getEntity.getFieldValues(manfiestRef)) {
            modifyEntity.addFieldValue(manfiestRef, o);
        }
        entityService.modifyField(modifyEntity, modifyEntity, manfiestRef, context);
        IEntity newGet = persistenceService.get(queryEntity, pContext);

        Assert.assertEquals(1, newGet.getFieldValues(manfiestRef).size() - getEntity.getFieldValues(manfiestRef).size());
        Assert.assertEquals(4, newGet.getFieldProperty(manfiestRef, FieldProperty.LENGTH.getName()));
        
        IEntity getSI = entityService.get(queryEntity, context);
        Assert.assertEquals(4, getSI.getFieldValues(manfiestRef).size());
        Assert.assertEquals(4, getSI.getFieldProperty(manfiestRef, FieldProperty.LENGTH.getName()));
    }
    
    static int appCount = 0;
    
    private JsonEntity newApplicationService(String branchId){
        String metaType = APPLICATION_SERVICE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Application Service Instance for Entity-Branch Test:" + (appCount++));
        return newEntity;
    }

    private JsonEntity newLBService(String branchId){
        String metaType = "LBService";
        MetaClass instCls = repoService.getRepository(CMSDB_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("floatType", "netscalar-shared-primary");
        newEntity.addFieldValue("resourceId", "Dummy NodeServer for Entity-Branch Test:" + (appCount++));
        return newEntity;
    }
    
    private JsonEntity newNetworkAddress(String branchId){
        String metaType = "NetworkAddress";
        MetaClass instCls = repoService.getRepository(CMSDB_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("address", "127.0.0.1");
        newEntity.addFieldValue("ipVersion", "IPv4");
        newEntity.addFieldValue("resourceId", "Dummy NodeServer for Entity-Branch Test:" + (appCount++));
        return newEntity;
    }
    
    private JsonEntity newCapacity(String branchId){
        String metaType = "ResourceCapacity";
        MetaClass instCls = repoService.getRepository(CMSDB_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("resourceId", "placeholder");
        newEntity.addFieldValue("total", 10);
        newEntity.addFieldValue("used", 1);
        newEntity.addFieldValue("reserved", 2);
        newEntity.addFieldValue("unit", "unit");
        newEntity.addFieldValue("type", "bandwidth");
        return newEntity;
    }

    @Test
    public void test02ModifyReferenceArray() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        appService.addFieldValue("services", service1);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        JsonEntity modifyEntity = new JsonEntity(metaClass);
        modifyEntity.setBranchId(appService.getBranchId());
        modifyEntity.setId(appId);
        // add a service through modify field
        IEntity service2 = createServiceInstance();
        modifyEntity.addFieldValue("services", service2);

        entityService.modifyField(modifyEntity, modifyEntity, "services", context);

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity entity = entityService.get(qEntity, context);
        Assert.assertNotNull(entity.getFieldValues("services").size());
        Assert.assertEquals(2, entity.getFieldValues("services").size());

        // modify again, should not change anything
        JsonEntity modifyEntity1 = new JsonEntity(metaClass);
        modifyEntity1.setBranchId(appService.getBranchId());
        modifyEntity1.setId(appId);
        // add a service through modify field
        modifyEntity.addFieldValue("services", service2);
        
        entityService.modifyField(modifyEntity1, modifyEntity1, "services", context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity getEntity1 = entityService.get(qEntity, context);
        Assert.assertNotNull(getEntity1.getFieldValues("services").size());
        Assert.assertEquals(2, getEntity1.getFieldValues("services").size());
    }

    /**
     * CMS-4763:: EntityComparator: ReadBeforeWrite should ignore updates that have no change.
     */
    @Test
    public void test02ModifyReferenceArray_withUselsessFields_expression() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        appService.addFieldValue("services", service1);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        JsonEntity modifyEntity = new JsonEntity(metaClass);
        modifyEntity.setBranchId(appService.getBranchId());
        modifyEntity.setId(appId);

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity entity = entityService.get(qEntity, context);
        Assert.assertNotNull(entity.getFieldValues("services").size());
        Assert.assertEquals(1, entity.getFieldValues("services").size());
        int oldVersion = entity.getVersion();

        // modify, should not change anything
        JsonEntity modifyEntity1 = new JsonEntity(metaClass);
        modifyEntity1.setBranchId(appService.getBranchId());
        modifyEntity1.setId(appId);
        // add a service through modify field
        service1.addFieldValue("name", "services-name");
        modifyEntity1.addFieldValue("services", service1);

        entityService.modifyField(modifyEntity1, modifyEntity1, "services", context);

        qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity getEntity1 = entityService.get(qEntity, context);
        Assert.assertNotNull(getEntity1.getFieldValues("services").size());
        Assert.assertEquals(1, getEntity1.getFieldValues("services").size());
        Assert.assertEquals(oldVersion, getEntity1.getVersion());
    }
    
    @Test
    public void test02ModifyReferenceArray_withUselsessFields_noexpression() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        appService.addFieldValue("services", service1);
        entityService.create(appService, context);
        IEntity getApp = entityService.get(appService, context);
        
        // create environment;
        IEntity environment = createEnvironment();
        int oldVersion = environment.getVersion();

        // modify to add one app
        MetaClass metaClass = raptorMetaService.getMetaClass(ENVIRONMENT);
        JsonEntity modifyEntity1 = new JsonEntity(metaClass);
        modifyEntity1.setBranchId(environment.getBranchId());
        modifyEntity1.setId(environment.getId());
        // add a service through modify field
        final String applications = "applications";
        modifyEntity1.addFieldValue(applications, getApp);

        entityService.modifyField(modifyEntity1, modifyEntity1, applications, context);

        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, environment.getType(), environment.getId());
        int newVersion = oldVersion; // init
        {
            IEntity getEntity1 = entityService.get(qEntity, context);
            Assert.assertNotNull(getEntity1.getFieldValues(applications).size());
            Assert.assertEquals(1, getEntity1.getFieldValues(applications).size());
            newVersion = getEntity1.getVersion();
            Assert.assertEquals(oldVersion + 1, newVersion);
        }
        {
            // modify again with the same one
            modifyEntity1 = new JsonEntity(metaClass);
            modifyEntity1.setBranchId(environment.getBranchId());
            modifyEntity1.setId(environment.getId());
            getApp.addFieldValue("name", "change some name");
            modifyEntity1.addFieldValue(applications, getApp);
            entityService.modifyField(modifyEntity1, modifyEntity1, applications, context);

            IEntity getEntity2 = entityService.get(qEntity, context);
            Assert.assertEquals(newVersion, getEntity2.getVersion());
            Assert.assertEquals(1, getEntity2.getFieldValues(applications).size());
        }
    }

    @Test
    public void test03ModifyReferenceArray_typeCast() {
        IEntity group = createResourceGroup();
        IEntity vCluster = createVCluster();
        Assert.assertNotNull(group.getId());
        Assert.assertNotNull(vCluster.getId());
        group.addFieldValue("children", vCluster);
        // should pass dangling check
        entityService.modifyField(group, group, "children", context);
        IEntity getEntity = entityService.get(group, context);
        Assert.assertEquals(1, getEntity.getFieldValues("children").size());

        // delete would got ref-integrity check
        try {
            entityService.delete(vCluster, context);
            Assert.fail();
        } catch (CmsEntMgrException eme) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY.getErrorCode(), eme.getErrorCode());
        }
    }

    @Test
    public void test04Create_strongWithTypeCast() {
        IEntity group = newResourceGroup_withVCluster();
        try {
            entityService.create(group, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void test04Create_strongWithValidTypeCast() {
        IEntity group = newResourceGroup();
        IEntity vcluster = createVCluster();
        group.addFieldValue("children", vcluster);
        String id = entityService.create(group, context);
        group.setId(id);
        IEntity getGroup = entityService.get(group, context);
        Assert.assertNotNull(getGroup);

        // delete would got ref-integrity check
        try {
            entityService.delete(vcluster, context);
            Assert.fail();
        } catch (CmsEntMgrException eme) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY.getErrorCode(), eme.getErrorCode());
        }
    }

    private IEntity newResourceGroup_withVCluster() {
        IEntity newEntity = newResourceGroup();

        JsonEntity nodeServerEntity = new JsonEntity(stratusMetaService.getMetaClass("VCluster"));
        nodeServerEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        nodeServerEntity.setId("generate-fqdnEntity-id");

        newEntity.addFieldValue("children", nodeServerEntity);
        return newEntity;
    }

    private IEntity createResourceGroup() {
        JsonEntity newEntity = newResourceGroup();
        String id = entityService.create(newEntity, context);
        IEntity qEntity = buildQueryEntity(newEntity.getRepositoryName(), newEntity.getBranchId(), newEntity.getType(),
                id);
        return entityService.get(qEntity, context);
    }

    private JsonEntity newResourceGroup() {
        String metaType = "ResourceGroup";
        MetaClass instCls = stratusMetaService.getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("resourceId", "PHX-GROUPO-001");
        return newEntity;
    }
    
    private IEntity createVCluster() {
        String metaType = "VCluster";
        MetaClass instCls = stratusMetaService.getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("resourceId", "vcluster-oid");
        String id = entityService.create(newEntity, context);
        newEntity.setId(id);
        return entityService.get(newEntity, context);
    }

    private IEntity createNodeServer() {
        String metaType = NODE_SERVER;
        MetaClass instCls = cmsdbMetaService.getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        JsonEntity fqdnEntity = new JsonEntity(cmsdbMetaService.getMetaClass("FQDN"));
        fqdnEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        fqdnEntity.setId("generate-fqdnEntity-id");
        newEntity.addFieldValue("hostName", fqdnEntity);
        newEntity.addFieldValue("nodeType", "vmm");
        newEntity.addFieldValue("adminStatus", "NORMAL");
        newEntity.addFieldValue("adminNotes", "test-admin");
        newEntity.addFieldValue("assetStatus", "prep");
        newEntity.addFieldValue("resourceId", "PHX-GROUPO-001");
        JsonEntity naEntity = new JsonEntity(cmsdbMetaService.getMetaClass("NetworkAddress"));
        naEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        naEntity.setId("generate-naeitty-id");
        newEntity.addFieldValue("networkAddress", naEntity);
        String id = entityService.create(newEntity, context);
        IEntity qEntity = buildQueryEntity(newEntity.getRepositoryName(), newEntity.getBranchId(), newEntity.getType(),
                id);
        return entityService.get(qEntity, context);
    }
    
    @Test
    public void testModifyOneReferenceWithoutId() {
        IEntity service1 = createServiceInstance();
        IEntity appService = newApplicationService(service1.getBranchId());
        String appId = entityService.create(appService, context);

        appService.setId(appId);
        String fieldName = "appService";
        service1.addFieldValue(fieldName, appService);
        entityService.modifyField(service1, service1, fieldName, context);
        
        service1 = buildQueryEntity(service1);
        appService = buildQueryEntity(appService);
        appService.removeField("_oid");
        service1.addFieldValue(fieldName, appService);
        try {
        	entityService.modifyField(service1, service1, fieldName, context);
            Assert.fail();
        } catch (CmsDalException e) {
        	Assert.assertEquals(DalErrCodeEnum.MISS_REFID.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void test01ModifyReferenceArrayWithDupContents() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        String fieldName = "services";
        appService.addFieldValue(fieldName, service1);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        MetaField field = metaClass.getFieldByName(fieldName);
        
        JsonEntity modifyEntity = new JsonEntity(metaClass);
        modifyEntity.setBranchId(appService.getBranchId());
        modifyEntity.setId(appId);
        // add two services through modify field, the first is the duplicated-existing one
        IEntity service2 = createServiceInstance();
        modifyEntity.addFieldValue(fieldName, service1);
        modifyEntity.addFieldValue(fieldName, service2);

        entityService.modifyField(modifyEntity, modifyEntity, fieldName, context);

        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        
        IEntity entity = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(2, entity.getFieldValues(fieldName).size());
        Assert.assertEquals(2, entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));

        IEntity getEntity = entityService.get(queryEntity, context);
        Assert.assertEquals(2, getEntity.getFieldValues(fieldName).size());
        Assert.assertEquals(2, getEntity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));

        // modify again, should not change anything
        JsonEntity modifyEntity1 = new JsonEntity(metaClass);
        modifyEntity1.setBranchId(appService.getBranchId());
        modifyEntity1.setId(appId);
        // add a service through modify field
        modifyEntity.addFieldValue(fieldName, service2);
        
        entityService.modifyField(modifyEntity1, modifyEntity1, fieldName, context);

        IEntity entity2 = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(2, entity2.getFieldValues(fieldName).size());
        Assert.assertEquals(2, entity2.getFieldProperty(field.getName(), FieldProperty.LENGTH.getName()));
        
        IEntity getEntity2 = entityService.get(queryEntity, context);
        Assert.assertEquals(2, getEntity2.getFieldValues(fieldName).size());
        Assert.assertEquals(2, getEntity2.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));
    }

    @Test
    public void test03ModifyEmebedArray() {
        IEntity createdServiceInstance = createStratusServiceInstanceWithAccessPoint(stratusMetaService, seq++);
        Assert.assertNotNull(createdServiceInstance);
        Assert.assertTrue(createdServiceInstance.getFieldValues("serviceAccessPoints").size() == 2);
        
        IEntity stratusServiceInst = newStratusServiceInstance(stratusMetaService, seq++);
        stratusServiceInst.setId(createdServiceInstance.getId());
        IEntity newAccessPoint = newServiceAccessPoint(stratusMetaService, seq++);
        stratusServiceInst.addFieldValue("serviceAccessPoints", newAccessPoint);
        
        try {
            entityService.modifyField(stratusServiceInst, stratusServiceInst, "serviceAccessPoints", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
        }
           
        PersistenceContext pContext = newPersistentContext(stratusMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity bEntity = persistenceService.get(stratusServiceInst, pContext);
        Assert.assertTrue(bEntity.getFieldValues("serviceAccessPoints").size() == 2);
        Assert.assertEquals(2, bEntity.getFieldProperty("serviceAccessPoints", FieldProperty.LENGTH.getName()));
    }
    
    protected IEntity createStratusServiceInstanceWithAccessPoint(IMetadataService metaService, int seq) {
        IEntity stratusServiceInst = newStratusServiceInstance(metaService, seq++);
        IEntity accessPoint1 = newServiceAccessPoint(metaService, seq++);
        IEntity accessPoint2 = newServiceAccessPoint(metaService, seq++);
        stratusServiceInst.addFieldValue("serviceAccessPoints", accessPoint1);
        stratusServiceInst.addFieldValue("serviceAccessPoints", accessPoint2);

        String instId = entityService.create(stratusServiceInst, context);
        PersistenceContext pContext = newPersistentContext(metaService);
        IEntity queryEntity = buildQueryEntity(STRATUS_REPO, stratusServiceInst.getBranchId(), stratusServiceInst.getType(), instId);
        IEntity getInst = persistenceService.get(queryEntity, pContext);
        return getInst;
    }
    
    protected static IEntity newStratusServiceInstance(IMetadataService metaService, int seq) {
        MetaClass metaClass = metaService.getMetaClass("ServiceInstance");
        IEntity newEntity = new JsonEntity(metaClass);
        newEntity.addFieldValue("description", "ServiceInstance-" + seq);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        return newEntity;
    }

    protected IEntity newServiceAccessPoint(IMetadataService metaService, int seq) {
        MetaClass meta = metaService.getMetaClass("ServiceAccessPoint");
        IEntity accessPoint = new JsonEntity(meta);
        accessPoint.addFieldValue("label", generateRandomName("accessPoint") + "-" + seq);
        accessPoint.addFieldValue("port", 80);
        accessPoint.addFieldValue("protocol", "tcp");
        return accessPoint;
    }

    @Test
    public void test04ModifyJsonField() {
        String properties = "properties";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);

        IEntity getEntity = createServiceInstanceWithProperty();
        Assert.assertNotNull(getEntity.getFieldValues(properties));

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(getEntity.getBranchId());
        modifyEntity.setId(getEntity.getId());
        modifyEntity.addFieldValue(properties, "{\"f1\" : 3 , \"f2\": null, \"f3\": [\"array\"], \"f4\": {}}");
        entityService.modifyField(modifyEntity, modifyEntity, properties, context);

        IEntity qEntity = buildQueryEntity(getEntity);
        IEntity newGet = entityService.get(qEntity, context);
        Assert.assertNotNull(newGet.getFieldValues(properties));
        BasicDBObject property = (BasicDBObject) newGet.getFieldValues(properties).get(0);
        Assert.assertEquals(3, property.get("f1"));
        Assert.assertEquals(null, property.get("null"));
        Assert.assertTrue(property.get("f3") instanceof List);
        Assert.assertNotNull(property.get("f4"));
    }

    @Test
    public void test02CASModifyField() {
        JsonEntity appService = newApplicationService(MAIN_BRANCH);
        String appId = entityService.create(appService, context);

        String name = (String)appService.getFieldValues("name").get(0);
        appService.addFieldValue("name", "newAppName");
        appService.setId(appId);
        IEntity qEntity = buildQueryEntity(appService);
        entityService.casModifyField(qEntity, appService, "name", name, context);
        
        IEntity getApp = entityService.get(qEntity, context);
        String newName = (String)appService.getFieldValues("name").get(0);
        Assert.assertEquals("newAppName", newName);
    }
    
    @Test
    public void test02CASModifyField_embed_withExpression() {
        EntityContext context = newEntityContext();
        IEntity networkAddress = newNetworkAddress(MAIN_BRANCH);
        entityService.create(networkAddress, context);
        //
        JsonEntity lbService = newLBService(MAIN_BRANCH);
        lbService.addFieldValue("networkAddress", networkAddress);
        String lbServiceId = entityService.create(lbService, context);
        JsonEntity capacity = newCapacity(MAIN_BRANCH);
        context.setPath("LBService!" + lbService.getId() + "!capacities");
        String capacityId = entityService.create(capacity, context);

        final String reserved = "reserved";
        context.setPath(null);
        IEntity getCap = entityService.get(capacity, context);
        final long oldAvail = (Long) getCap.getFieldValues("available").get(0);
        final long oldTotal = (Long) getCap.getFieldValues("total").get(0);
        final long oldUsed = (Long) getCap.getFieldValues("used").get(0);
        final long oldReserved = (Long)getCap.getFieldValues(reserved).get(0);
        Assert.assertEquals(oldAvail, oldTotal - oldUsed - oldReserved);
        // case 1 cas modify field
        {
            getCap.addFieldValue(reserved, oldReserved + 2);
            entityService.casModifyField(capacity, getCap, reserved, oldReserved, context);

            IEntity getCap2 = entityService.get(capacity, context);
            final long newAvail = (Long) getCap2.getFieldValues("available").get(0);
            final long newTotal = (Long) getCap2.getFieldValues("total").get(0);
            final long newUsed = (Long) getCap2.getFieldValues("used").get(0);
            final long newReserved = (Long) getCap2.getFieldValues(reserved).get(0);
            Assert.assertEquals(oldReserved + 2, newReserved);
            Assert.assertEquals(oldTotal, newTotal);
            Assert.assertEquals(oldUsed, newUsed);
            Assert.assertEquals(newAvail, newTotal - newUsed - newReserved);
        }
        // case 2 : modify field
        {
            getCap.removeField("_version");
            getCap.addFieldValue(reserved, oldReserved + 3);
            entityService.modifyField(capacity, getCap, reserved, context);

            IEntity getCap2 = entityService.get(capacity, context);
            final long newAvail = (Long) getCap2.getFieldValues("available").get(0);
            final long newTotal = (Long) getCap2.getFieldValues("total").get(0);
            final long newUsed = (Long) getCap2.getFieldValues("used").get(0);
            final long newReserved = (Long) getCap2.getFieldValues(reserved).get(0);
            Assert.assertEquals(oldReserved + 3, newReserved);
            Assert.assertEquals(oldTotal, newTotal);
            Assert.assertEquals(oldUsed, newUsed);
            Assert.assertEquals(newAvail, newTotal - newUsed - newReserved);
        }
    }
    
    @Test
    public void test02CASModifyFieldWithInvalidValue() {
        JsonEntity appService = newApplicationService(MAIN_BRANCH);
        List<String> manifestRefs = new ArrayList<String>();
        manifestRefs.add("ref1");
        manifestRefs.add("ref2");
        appService.setFieldValues("preManifestRef", manifestRefs);

        String appId = entityService.create(appService, context);
        appService.setId(appId);
        IEntity qEntity = buildQueryEntity(appService);
        try {
            entityService.casModifyField(qEntity, appService, "preManifestRef", "ref1", context);
            Assert.fail();
        } catch(CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED, e.getErrorEnum());
        }
        
        appService.addFieldValue("name", "newAppName");
        appService.setId(appId);
        try {
            entityService.casModifyField(qEntity, appService, "name", "newAppName", context);
            Assert.fail();
        } catch(CmsEntMgrException e) {
            Assert.assertEquals(EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED, e.getErrorEnum());
        }
    }
    
    @Test
    public void test05DeleteArrayField() {
        String manifestRef = "manifestRef";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);

        IEntity oldGet = createServiceInstanceWithManifestRef();
        Assert.assertNotNull(oldGet.getFieldValues(manifestRef));
        Assert.assertEquals(3, oldGet.getFieldValues(manifestRef).size());

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(oldGet.getBranchId());
        modifyEntity.setId(oldGet.getId());
        // remove the first value
        modifyEntity.addFieldValue(manifestRef, oldGet.getFieldValues(manifestRef).get(0));
        entityService.pullField(modifyEntity, modifyEntity, manifestRef, context);

        IEntity qEntity = buildQueryEntity(oldGet);
        IEntity newGet = entityService.get(qEntity, context);
        Assert.assertEquals(2,  newGet.getFieldProperty(manifestRef, FieldProperty.LENGTH.getName()));
        Assert.assertEquals(1, oldGet.getFieldValues(manifestRef).size() - newGet.getFieldValues(manifestRef).size());
    }
    
    @Test
    public void test05DeleteArrayWithNotExisting() {
        String manifestRef = "manifestRef";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);

        IEntity oldGet = createServiceInstanceWithManifestRef();
        Assert.assertNotNull(oldGet.getFieldValues(manifestRef));
        Assert.assertEquals(3, oldGet.getFieldValues(manifestRef).size());
        
        IEntity queryEntity = buildQueryEntity(oldGet);
        IEntity bGet = entityService.get(queryEntity, context);
        Assert.assertEquals(3, bGet.getFieldProperty(manifestRef, FieldProperty.LENGTH.getName()));        

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(oldGet.getBranchId());
        modifyEntity.setId(oldGet.getId());
        // remove the first value
        modifyEntity.addFieldValue(manifestRef, "not-existing-read");

        entityService.pullField(modifyEntity, modifyEntity, manifestRef, context);

        IEntity newGet = entityService.get(queryEntity, context);
        Assert.assertNotNull(newGet.getFieldValues(manifestRef));
        Assert.assertEquals(0, oldGet.getFieldValues(manifestRef).size() - newGet.getFieldValues(manifestRef).size());
        Assert.assertEquals(3, newGet.getFieldProperty(manifestRef, FieldProperty.LENGTH.getName()));
    }

    @Test
    public void test06DeleteReferenceArray() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        String fieldName = "services";
        IEntity service2 = createServiceInstance();
        appService.addFieldValue(fieldName, service1);
        appService.addFieldValue(fieldName, service2);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        MetaField field = metaClass.getFieldByName(fieldName);
        
        JsonEntity deleteEntity = new JsonEntity(metaClass);
        deleteEntity.setBranchId(appService.getBranchId());
        deleteEntity.setId(appId);
        deleteEntity.addFieldValue(fieldName, service2);

        entityService.pullField(deleteEntity, deleteEntity, fieldName, context);
        
        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity entity = entityService.get(queryEntity, context);
        
        Assert.assertEquals(1, entity.getFieldValues(fieldName).size());
        Assert.assertEquals(1, entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));

        // delete again with not-matched item, should not change anything
        IEntity service3 = createServiceInstance();
        
        JsonEntity deleteEntity1 = new JsonEntity(metaClass);
        deleteEntity1.setBranchId(appService.getBranchId());
        deleteEntity1.setId(appId);
        // add a service not in application reference
        deleteEntity1.addFieldValue(fieldName, service3);
        
        entityService.pullField(deleteEntity1, deleteEntity1, fieldName, context);
        
        IEntity getEntity1 = entityService.get(queryEntity, context);
        Assert.assertEquals(1, getEntity1.getFieldValues(fieldName).size());
        Assert.assertEquals(1, getEntity1.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));
    }
    
    @Test
    public void test06DeleteReferenceArrayPartionlMatch() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        String fieldName = "services";
        IEntity service2 = createServiceInstance();
        appService.addFieldValue(fieldName, service1);
        appService.addFieldValue(fieldName, service2);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        IEntity service3 = createServiceInstance();
        JsonEntity deleteEntity = new JsonEntity(metaClass);
        deleteEntity.setBranchId(appService.getBranchId());
        deleteEntity.setId(appId);
        deleteEntity.addFieldValue(fieldName, service2);
        deleteEntity.addFieldValue(fieldName, service3);

        entityService.pullField(deleteEntity, deleteEntity, fieldName, context);

        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity entity = persistenceService.get(queryEntity, pContext);
        
        Assert.assertEquals(1, entity.getFieldValues(fieldName).size());
        MetaField field = metaClass.getFieldByName(fieldName);
        int length1 = (Integer) entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName());
        Assert.assertEquals(1,  length1);
    }
    
    @Test
    public void test06DeleteReferenceArrayAll() {
        IEntity service1 = createServiceInstance();
        JsonEntity appService = newApplicationService(service1.getBranchId());
        IEntity service2 = createServiceInstance();
        String fieldName = "services";
        appService.addFieldValue(fieldName, service1);
        appService.addFieldValue(fieldName, service2);
        String appId = entityService.create(appService, context);

        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        JsonEntity deleteEntity = new JsonEntity(metaClass);
        deleteEntity.setBranchId(appService.getBranchId());
        deleteEntity.setId(appId);
        deleteEntity.addFieldValue(fieldName, service1);
        deleteEntity.addFieldValue(fieldName, service2);

        entityService.pullField(deleteEntity, deleteEntity, fieldName, context);

        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService.getType(), appId);
        IEntity entity = persistenceService.get(queryEntity, pContext);
        
        Assert.assertEquals(0, entity.getFieldValues(fieldName).size());
        MetaField field = metaClass.getFieldByName(fieldName);
        int length1 = (Integer) entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName());
        Assert.assertEquals(0,  length1);
    }

    @Test
    public void test06DeleteReferenceArrayWithEmptyList() {
        String fieldName = "services";
        JsonEntity appService1 = newApplicationService(MAIN_BRANCH);
        appService1.setFieldValues(fieldName, new ArrayList<IEntity>());
        String appId = entityService.create(appService1, context);

        IEntity service1 = createServiceInstance();
        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        JsonEntity deleteEntity = new JsonEntity(metaClass);
        deleteEntity.setBranchId(MAIN_BRANCH);
        deleteEntity.setId(appId);
        deleteEntity.addFieldValue(fieldName, service1);

        entityService.pullField(deleteEntity, deleteEntity, fieldName, context);

        PersistenceContext pContext = newPersistentContext(raptorMetaService);
        pContext.setFetchFieldProperties(true);
        IEntity queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService1.getType(), appId);
        IEntity entity = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(0, entity.getFieldValues(fieldName).size());
        Assert.assertEquals(0, entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));
        
        JsonEntity appService2 = newApplicationService(MAIN_BRANCH);
        appService2.addFieldValue(fieldName, service1);
        appId = entityService.create(appService2, context);

        deleteEntity = new JsonEntity(metaClass);
        deleteEntity.setBranchId(MAIN_BRANCH);
        deleteEntity.setId(appId);
        deleteEntity.setFieldValues(fieldName, new ArrayList<IEntity>());

        entityService.pullField(deleteEntity, deleteEntity, fieldName, context);
        queryEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, appService2.getType(), appId);
        entity = persistenceService.get(queryEntity, pContext);
        Assert.assertEquals(1, entity.getFieldValues(fieldName).size());
        Assert.assertEquals(1, entity.getFieldProperty(fieldName, FieldProperty.LENGTH.getName()));
    }

    //
    // CMS-3082
    //
    @Test
    public void test07DeleteEmbedArray() {
        MetaClass depCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Dep");
        MetaClass teamCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Team");
        MetaClass personCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Person");
        // team1 with two dev
        JsonEntity person1 = new JsonEntity(personCls);
        person1.addFieldValue("name", "dev-01");
        JsonEntity person2 = new JsonEntity(personCls);
        person2.addFieldValue("name", "dev-02");
        JsonEntity team1 = new JsonEntity(teamCls);
        team1.addFieldValue("name", "stratus-dev-team");
        team1.addFieldValue("person", person1);
        team1.addFieldValue("person", person2);
        // team2 with two qa
        JsonEntity person3 = new JsonEntity(personCls);
        person3.addFieldValue("name", "qa-01");
        JsonEntity person4 = new JsonEntity(personCls);
        person4.addFieldValue("name", "qa-02");
        JsonEntity team2 = new JsonEntity(teamCls);
        team2.addFieldValue("name", "stratus-qa-team");
        // two team are belong to same dep
        JsonEntity dep = new JsonEntity(depCls);
        dep.setBranchId(MAIN_BRANCH);
        dep.addFieldValue("label", "cloud");
        dep.addFieldValue("team", team1);
        dep.addFieldValue("team", team2);
        // create the entities
        String depId = entityService.create(dep, context);
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, "Dep", depId);
        IEntity getDep = entityService.get(qEntity, context);
        String team1Id = ((IEntity) getDep.getFieldValues("team").get(0)).getId();
        // try to delete the field
        JsonEntity deleteEntity = new JsonEntity(depCls);
        deleteEntity.setId(depId);
        deleteEntity.setBranchId(MAIN_BRANCH);
        JsonEntity deleteTeam = new JsonEntity(teamCls);
        deleteTeam.setId(team1Id);
        deleteTeam.setBranchId(MAIN_BRANCH);
        deleteEntity.addFieldValue("team", deleteTeam);
        entityService.pullField(deleteEntity, deleteEntity, "team", context);
        // now assert
        getDep = entityService.get(qEntity, context);
        List<?> teams = getDep.getFieldValues("team");
        Assert.assertEquals(1, teams.size());
    }
        
    @Test
    public void test08DeleteField() {
        String manfiestRef = "manifestRef";
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        
        MetaClass instCls = metaService.getMetaClass(SERVICE_INSTANCE);

        MetaAttribute newName = new MetaAttribute(false);
        String fieldName = "newName";
        newName.setName(fieldName);
        newName.setDataType(DataTypeEnum.STRING);
        newName.setDbName(fieldName);
        String expr = "$name + \" case\"";
        newName.setExpression(expr);
        instCls.addField(newName);
        
        IEntity getEntity = createServiceInstanceWithManifestRef();
        Assert.assertNotNull(getEntity.getFieldValues(manfiestRef));
        Assert.assertEquals(3, getEntity.getFieldValues(manfiestRef).size());
        Assert.assertEquals((String)getEntity.getFieldValues(fieldName).get(0), "Dummy Service Instance for Entity-Branch Test case");

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(getEntity.getBranchId());
        modifyEntity.setId(getEntity.getId());
        
        context.setModifier("unitTestUser-1");
        context.setUserId("unitTestUserID-1");
        context.setComment("unitTestComments-1");        
        
        entityService.deleteField(modifyEntity, "name", context);
        
        context.setModifier("unitTestUser");
        context.setUserId("unitTestUserID");
        context.setComment("unitTestComments");   
        
        IEntity newGet = entityService.get(getEntity, context);
        String val = (String)newGet.getFieldValues(fieldName).get(0);
        Assert.assertEquals(val, "null case");
        
        String modifier = (String)newGet.getFieldValues(InternalFieldEnum.MODIFIER.getName()).get(0);
        String userid = (String)newGet.getFieldValues(InternalFieldEnum.USER.getName()).get(0);
        String comment = (String)newGet.getFieldValues(InternalFieldEnum.COMMENT.getName()).get(0);
        Assert.assertEquals("unitTestUser-1", modifier);
        Assert.assertEquals("unitTestUserID-1", userid);
        Assert.assertEquals("unitTestComments-1", comment);
    }

    @Test
    public void modifyFieldNoDiff() {
        String manfiestRef = "manifestRef";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(SERVICE_INSTANCE);

        IEntity oldGet = createServiceInstanceWithManifestRef();
        Assert.assertNotNull(oldGet.getFieldValues(manfiestRef));
        Assert.assertEquals(3, oldGet.getFieldValues(manfiestRef).size());

        JsonEntity modifyEntity = new JsonEntity(instCls);
        modifyEntity.setBranchId(oldGet.getBranchId());
        modifyEntity.setId(oldGet.getId());
        // modify the field with the same content in database, the action should be ignored 
        modifyEntity.setFieldValues(manfiestRef, oldGet.getFieldValues(manfiestRef));

        entityService.modifyField(modifyEntity, modifyEntity, manfiestRef, context);

        IEntity qEntity = buildQueryEntity(oldGet);
        IEntity newGet = entityService.get(qEntity, context);
        Assert.assertEquals(oldGet.getVersion(), newGet.getVersion());
        Assert.assertNotNull(newGet.getFieldValues(manfiestRef));
        Assert.assertEquals(oldGet.getFieldValues(manfiestRef).size(), newGet.getFieldValues(manfiestRef).size());
    }

    @Test
    public void deleteFieldNotExisitng() {
        IEntity serviceInstance = createServiceInstance();
        // https not set when created, so the delete field would be ignored
        entityService.deleteField(serviceInstance, "port", context);
        IEntity qEntity = buildQueryEntity(serviceInstance);
        IEntity get = entityService.get(qEntity, context);
        Assert.assertEquals(serviceInstance.getVersion(), get.getVersion());
        Assert.assertFalse(get.hasField("port"));
    }
    
    @Test
    public void deleteEmbedFieldNotExisitng() {
        MetaClass depCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Dep");
        MetaClass teamCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Team");
        MetaClass personCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Person");
        // team1 with two dev
        JsonEntity person1 = new JsonEntity(personCls);
        person1.addFieldValue("name", "dev-01");
        JsonEntity person2 = new JsonEntity(personCls);
        person2.addFieldValue("name", "dev-02");
        JsonEntity team1 = new JsonEntity(teamCls);
        team1.addFieldValue("name", "stratus-dev-team");
        team1.addFieldValue("person", person1);
        team1.addFieldValue("person", person2);

        // two team are belong to same dep
        JsonEntity dep = new JsonEntity(depCls);
        dep.setBranchId(MAIN_BRANCH);
        dep.addFieldValue("label", "cloud");
        dep.addFieldValue("team", team1);

        // create the entities
        String depId = entityService.create(dep, context);
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, MAIN_BRANCH, "Dep", depId);
        IEntity getDep = entityService.get(qEntity, context);
        String team1Id = ((IEntity) getDep.getFieldValues("team").get(0)).getId();
        // try to delete the field
        JsonEntity deleteEntity = new JsonEntity(depCls);
        deleteEntity.setId(depId);
        deleteEntity.setBranchId(MAIN_BRANCH);
        JsonEntity deleteTeam = new JsonEntity(teamCls);
        deleteTeam.setId(team1Id + "2");
        deleteTeam.setBranchId(MAIN_BRANCH);
        deleteEntity.addFieldValue("team", deleteTeam);
        entityService.deleteField(deleteEntity, "team", context);
    }

    private IEntity buildQueryEntity(IEntity entity) {
        return buildQueryEntity(entity.getRepositoryName(), entity.getBranchId(), entity.getType(), entity.getId());
    }

    private IEntity buildQueryEntity(String reponame, String branchname, String metadata, String oid) {
        MetaClass meta = repoService.getRepository(reponame).getMetadataService().getMetaClass(metadata);
        IEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

}
