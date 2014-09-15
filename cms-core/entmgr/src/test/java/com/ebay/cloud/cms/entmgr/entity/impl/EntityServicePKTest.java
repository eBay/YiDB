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

import static org.junit.Assert.assertTrue;

import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Ignore;
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
import com.ebay.cloud.cms.entmgr.entity.CallbackContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/**
 * This case should be deprecated. Since PK feature is moved out.
 *
 */
public class EntityServicePKTest extends CMSMongoTest {

    private static final String _802_ADDRESS = "802Address";
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
    protected static final String        STRATUS_REPO        = "stratus-ci";
    protected static final String        STRATUS_DATA_FILE   = "stratusRuntime.json";
    protected static final String        NETWORK_ADDRESS     = "NetworkAddress";

    protected static MetadataDataLoader  metaLoader          = null;

    protected static IRepositoryService  repoService         = null;
    protected static IBranchService      branchService       = null;
    protected static IEntityService      entityService       = null;
    protected static IMetadataService    raptorMetaService   = null;
    protected static IMetadataService    deployMetaService   = null;
    protected static IPersistenceService persistenceService  = null;

    protected static IMetadataService    stratusMetaService  = null;

    protected static final String        SOURCE_IP           = "127.0.0.1";
    protected static EntityContext       context;
    private static int                   seq                 = 0;
    private static MetaClass             networkMeta         = null;

    protected static MongoDataSource     dataSource;
    protected static CMSDBConfig         config;

    @BeforeClass
    public static void setUp() {
        String connectionString = CMSMongoTest.getConnectionString();
        dataSource = new MongoDataSource(connectionString);
        config = new CMSDBConfig(dataSource);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        raptorLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, RAPTOR_REPO);
        raptorLoader.load(RAPTOR_DATA_FILE);

        deployLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, DEPLOY_REPO);
        deployLoader.load(DEPLOY_DATA_FILE);

        stratusLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, STRATUS_REPO);
        stratusLoader.load(STRATUS_DATA_FILE);

        // create a testing branch
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);

        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = newEntityContext();
        
        networkMeta = stratusMetaService.getMetaClass(NETWORK_ADDRESS);
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
                ConsistentPolicy.safePolicy(), IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }


    @Test
    public void modifyWithSameValue() {
        EntityContext context = newEntityContext();

        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);

        String newId = entityService.create(newInst, context);
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE,
                newId);
        IEntity entity1 = entityService.get(qEntity, context);

        newInst.setId(newId);
        entityService.modify(newInst, newInst, context);

        IEntity entity2 = entityService.get(qEntity, context);
        Assert.assertTrue(entity1.getVersion() == entity2.getVersion());
    }

    // TODO
    @Test
    public void modifyWithDiffReference() {

    }

    @Test
    public void modifyWithDiffOnlyInEmbed() {

    }

    @Test
    public void batchModifyWithNoDiff() {

    }

    @Test
    public void batchModifyWithPartDiff() {

    }

    public static class FailedCallback implements IEntityOperationCallback {

        @Override
        public boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context) {
            return false;
        }
    }

    @Test
    public void callbackTest() {
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context);
        entityService.setCallback(new FailedCallback());

        try {
            JsonEntity newInst2 = newServiceInstance(IBranch.DEFAULT_BRANCH);
            entityService.create(newInst2, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        // modify is checked.
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId);
        IEntity entityGet = entityService.get(qEntity, context);
        assertTrue(entityGet != null);
        try {
            entityService.modify(entityGet, entityGet, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        // replace
        try {
            qEntity = buildQueryEntity(entityGet);
            entityService.replace(qEntity, entityGet, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        // delete
        try {
            qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, SERVICE_INSTANCE, newId);
            entityService.delete(qEntity, context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        // delete field
        try {
            entityService.deleteField(entityGet, "name", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        // modify field
        try {
            entityService.modifyField(entityGet, entityGet, "name", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, e.getErrorEnum());
        }

        entityService.setCallback(null);
    }

    protected JsonEntity newServiceInstance(String branchId) {
        String metaType = SERVICE_INSTANCE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test");
        return newEntity;
    }
    
    //
    // PK test cases
    //
    //
    @Test
    public void replace_noid_withPk() {
        EntityContext context = newEntityContext();
        JsonEntity replaceInst = newNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        String replaceName = "b.ebay.com";
        replaceInst.addFieldValue("hostname", replaceName);
        replaceInst.setStatus(StatusEnum.ACTIVE);
        try {
            IEntity qEntity = buildQueryEntity(replaceInst);
            entityService.replace(qEntity, replaceInst, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    protected EntityContext newPKEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        //context.setUsePk(true);
        context.setDbConfig(config);
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return context;
    }

//    @Test
//    public void get_noid_withpk() {
//        EntityContext context = newPKEntityContext();
//        MetaClass metaClass = stratusMetaService.getMetaClass(NETWORK_ADDRESS);
//        JsonEntity inst = newNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
//        String id = entityService.create(inst, context);
//        IEntity queryEntity = new JsonEntity(metaClass);
//        queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
//        List<String> pks = metaClass.getPrimaryKeys();
//        for (String pk : pks) {
//            queryEntity.setFieldValues(pk, inst.getFieldValues(pk));
//        }
//        IEntity entity = entityService.get(queryEntity, context);
//        Assert.assertNotNull(entity);
//        Assert.assertEquals(id, entity.getId());
//    }

    @Test
    public void get_noid_withoutpk() {
        EntityContext context = newPKEntityContext();
        MetaClass metaClass = stratusMetaService.getMetaClass(NETWORK_ADDRESS);
        JsonEntity inst = newNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        String id = entityService.create(inst, context);
        Assert.assertNotNull(id);
        IEntity queryEntity = new JsonEntity(metaClass);
        queryEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        try {
            entityService.get(queryEntity, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Ignore
    @Test
    public void modify_noid() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        
        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        qe.addFieldValue(_802_ADDRESS, je.getFieldValues(_802_ADDRESS).get(0));
        entityService.modify(qe, entity, context);
        
        IEntity getJe = entityService.get(entity, context);
        Assert.assertNotNull(getJe);
        Assert.assertEquals(entity.getFieldValues(_802_ADDRESS).get(0), getJe.getFieldValues(_802_ADDRESS).get(0));
    }
    
    @Ignore
    @Test
    public void modify_noid_nopk() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        
        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        try {
            entityService.modify(qe, entity, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        // use new value to query, should find nothing
        IEntity getJe = entityService.get(entity, context);
        Assert.assertNull(getJe);
    }

    @Ignore
    @Test
    public void replace_noid() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        
        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        qe.addFieldValue(_802_ADDRESS, je.getFieldValues(_802_ADDRESS).get(0));
        entityService.replace(qe, entity, context);
        
        IEntity getJe = entityService.get(entity, context);
        Assert.assertNotNull(getJe);
        Assert.assertEquals(entity.getFieldValues(_802_ADDRESS).get(0), getJe.getFieldValues(_802_ADDRESS).get(0));
    }
    
    @Ignore
    @Test
    public void replace_noid_nopk() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);

        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        try {
            entityService.replace(qe, entity, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        // use new value to query, should find nothing
        IEntity getJe = entityService.get(entity, context);
        Assert.assertNull(getJe);
    }
    
    @Ignore
    @Test
    public void delete_noid() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        qe.addFieldValue(_802_ADDRESS, je.getFieldValues(_802_ADDRESS).get(0));
        entityService.delete(qe, context);

        IEntity getJe = entityService.get(je, context);
        Assert.assertNull(getJe);
        IEntity qEntity = buildQueryEntity(je.getRepositoryName(), je.getBranchId(), je.getType(), je.getId());
        getJe = entityService.get(qEntity, context);
        Assert.assertNull(getJe);
    }
    
    @Test
    public void delete_noid_nopk() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        try {
            entityService.delete(qe, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        IEntity getJe = entityService.get(je, context);
        Assert.assertNotNull(getJe);
        IEntity qEntity = buildQueryEntity(je.getRepositoryName(), je.getBranchId(), je.getType(), je.getId());
        getJe = entityService.get(qEntity, context);
        Assert.assertNotNull(getJe);
    }
    
    @Ignore
    @Test
    public void modifyfield_noid() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        
        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        qe.addFieldValue(_802_ADDRESS, je.getFieldValues(_802_ADDRESS).get(0));
        entityService.modifyField(qe, entity, _802_ADDRESS, context);
        
        IEntity getJe = entityService.get(entity, context);
        Assert.assertNotNull(getJe);
        Assert.assertEquals(entity.getFieldValues(_802_ADDRESS).get(0), getJe.getFieldValues(_802_ADDRESS).get(0));
    }
    
    @Test
    public void modifyfield_noid_nopk() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        
        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        try {
            entityService.modifyField(qe, entity, _802_ADDRESS, context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        IEntity getJe = entityService.get(je, context);
        Assert.assertNotNull(getJe);
        Assert.assertEquals(je.getFieldValues(_802_ADDRESS).get(0), getJe.getFieldValues(_802_ADDRESS).get(0));
    }
    
    @Ignore
    @Test
    public void deletefield_noid() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        Assert.assertTrue(je.hasField("hostname"));

        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());
        entity.addFieldValue(_802_ADDRESS, "new-oa:db:-test:" + seq++);

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        qe.addFieldValue(_802_ADDRESS, je.getFieldValues(_802_ADDRESS).get(0));
        entityService.deleteField(qe, "hostname", context);
        
        IEntity getJe = entityService.get(je, context);
        Assert.assertNotNull(getJe);
        Assert.assertFalse(getJe.hasField("hostname"));
    }
    
    @Test
    public void deletefield_noid_nopk() {
        JsonEntity je = createNetworkAddress(IBranch.DEFAULT_BRANCH, seq++);
        Assert.assertTrue(je.hasField("hostname"));

        IEntity entity = new JsonEntity(networkMeta);
        entity.setBranchId(je.getBranchId());

        EntityContext context = newPKEntityContext();
        IEntity qe = new JsonEntity(networkMeta);
        qe.setBranchId(je.getBranchId());
        try {
            entityService.deleteField(qe, "hostname", context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        
        IEntity getJe = entityService.get(je, context);
        Assert.assertNotNull(getJe);
        Assert.assertTrue(getJe.hasField("hostname"));
    }
    
    protected JsonEntity createNetworkAddress(String branchId, int seq) {
        JsonEntity je = newNetworkAddress(branchId, seq);
        String id = entityService.create(je, context);
        je.setId(id);
        return je;
    }

    protected JsonEntity newNetworkAddress(String branchId, int seq){
        String metaType = NETWORK_ADDRESS;
        MetaClass instCls = repoService.getRepository(STRATUS_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("ipaddress", "1011.249.64.99");
        newEntity.addFieldValue("hostname", "a.ebay.com");
        newEntity.addFieldValue("zone", "corp");
        newEntity.addFieldValue(_802_ADDRESS, "00:e0:ce:af:" + seq);
        return newEntity;
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
}
