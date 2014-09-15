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

import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.codehaus.jackson.node.ObjectNode;
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
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class InnerEntityServiceTest {

    protected static final String        AGENT               = "Agent";
    protected static final String        FQDN                = "FQDN";
    protected static final String        NODE_SERVER         = "NodeServer";
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
    
    protected static final String        CMSDB_REPO          = "cmsdb";
    protected static final String        CMSDB_DATA_FILE     = "cmsdbRuntime.json";
    protected static RuntimeDataLoader   cmsdbLoader       = null;
    
    protected static MetadataDataLoader  metaLoader          = null;

    protected static IRepositoryService  repoService         = null;
    protected static IBranchService      branchService       = null;
    protected static IEntityService      entityService       = null;
    protected static IMetadataService    raptorMetaService   = null;
    protected static IMetadataService    deployMetaService   = null;
    protected static IMetadataService    cmsdbMetaService    = null;
    protected static IPersistenceService persistenceService  = null;

    protected static IMetadataService    stratusMetaService  = null;

    protected static final String        SOURCE_IP           = "127.0.0.1";
    protected static EntityContext       context;

    protected static MongoDataSource     dataSource;
    protected static CMSDBConfig         config;

    @BeforeClass
    public static void setUp() {
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

        // create a testing branch
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);
        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        cmsdbMetaService = repoService.getRepository(CMSDB_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = newEntityContext();
        cmsdbPersistcontext = newPersistentContext(cmsdbMetaService);
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

    private static int seq = 1;
    private static Random rd = new Random(System.currentTimeMillis());
    private static PersistenceContext cmsdbPersistcontext;

    @SuppressWarnings("unused")
    private String generateName(String base) {
        return base + ":" + System.currentTimeMillis() + ":" + rd.nextGaussian();
    }

    private IEntity createNodeServer(int seq) {
        IEntity nodeServer = newNodeServer(seq);
        String newId = entityService.create(nodeServer, context);
        nodeServer.setId(newId);
        return entityService.get(nodeServer, context);
    }

    private JsonEntity newNodeServer(int seq) {
        JsonEntity nodeServer = new JsonEntity(cmsdbMetaService.getMetaClass(NODE_SERVER));
        nodeServer.setBranchId(IBranch.DEFAULT_BRANCH);
        nodeServer.addFieldValue("adminStatus", "NORMAL");
        nodeServer.addFieldValue("adminNotes", "nothing to do.");
        nodeServer.addFieldValue("assetStatus", "build");
        nodeServer.addFieldValue("resourceId", "10.0.0." + seq);
        nodeServer.addFieldValue("nodeType", "vmm");
        // network is not strong reference, create a danlging network addr for creation
        JsonEntity networkAddr = new JsonEntity(cmsdbMetaService.getMetaClass(NETWORK_ADDRESS));
        networkAddr.setId("networkAddress-faked-id");
        networkAddr.setBranchId(IBranch.DEFAULT_BRANCH);
        networkAddr.addFieldValue("resourceId", "10.0.0." + seq);
        nodeServer.addFieldValue("networkAddress", networkAddr);
        // fqdn is not strong reference, create a danlging fqdn for creation
        JsonEntity fqdn = new JsonEntity(cmsdbMetaService.getMetaClass(FQDN));
        fqdn.setId("fqdn-faked-id");
        fqdn.setBranchId(IBranch.DEFAULT_BRANCH);
        fqdn.addFieldValue("resourceId", "www.ebay.com" + seq);
        nodeServer.addFieldValue("hostName", fqdn);
        return nodeServer;
    }

    private IEntity createAgent(IEntity nodeServer, String type, String port) {
        IEntity agent = newAgent(nodeServer, type, port);
        context.setPath("NodeServer!" + nodeServer.getId() + "!agents");
        String agentId = entityService.create(agent, context);
        agent.setId(agentId);
        return entityService.get(agent, context);
    }

    private JsonEntity newAgent(IEntity nodeServer, String type, String port) {
        JsonEntity agent = new JsonEntity(cmsdbMetaService.getMetaClass(AGENT));
        agent.setBranchId(IBranch.DEFAULT_BRANCH);
        agent.addFieldValue("type", type);
        agent.addFieldValue("resourceId", nodeServer.getId() + type + ":" + port);
        agent.addFieldValue("port", port);
        agent.setHostEntity("NodeServer!" + nodeServer.getId() + "!agents");
        return agent;
    }

    @Test
    @SuppressWarnings("unused")
    public void testDeleteInnerRelationships() {
        IEntity nodeServer = createNodeServer(seq++);
        IEntity cronusAgent = createAgent(nodeServer, "cronus", "8001");
        IEntity tivoliAgent = createAgent(nodeServer, "tivoli", "8003");
        // delete all agent through parent
        IEntity deleteNodeSever = newNodeServer(seq++);
        deleteNodeSever.setId(nodeServer.getId());
        try {
            entityService.deleteField(nodeServer, "agents", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }
    }

    @Test
    public void testDeleteInnerRelationships_withInvalidInput() {
        IEntity nodeServer = createNodeServer(seq++);
        IEntity cronusAgent = createAgent(nodeServer, "cronus", "8001");
        IEntity tivoliAgent = createAgent(nodeServer, "tivoli", "8003");
        // ganglia agent is a NEW agent, not created actually
        IEntity gangliaAgent = newAgent(nodeServer, "ganglia-agent", "8652");

        // delete agents with mixed content : some valid with some invalid
        IEntity deleteNodeSever = newNodeServer(seq++);
        deleteNodeSever.setId(nodeServer.getId());
        deleteNodeSever.addFieldValue("agents", cronusAgent);
        deleteNodeSever.addFieldValue("agents", gangliaAgent);
        try {
            entityService.deleteField(nodeServer, "agents", context);
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
        }

        // assert data not deleted
        Assert.assertNotNull(entityService.get(cronusAgent, context));
        Assert.assertNotNull(entityService.get(tivoliAgent, context));
        IEntity queryNodeServer = buildQueryEntity(nodeServer); 
        cmsdbPersistcontext.setFetchFieldProperties(true);
        IEntity getNodeServer = persistenceService.get(queryNodeServer, cmsdbPersistcontext);
        Assert.assertTrue(getNodeServer.hasField("agents"));
        Assert.assertEquals(2, getNodeServer.getFieldValues("agents").size());
        Assert.assertEquals(2, getNodeServer.getFieldProperty("agents", FieldProperty.LENGTH.getName()));
    }

    private IEntity buildQueryEntity(IEntity givenEntity) {
        IEntity query = new JsonEntity(givenEntity.getMetaClass());
        query.setBranchId(IBranch.DEFAULT_BRANCH);
        // query entity only care about the id
        query.setId(givenEntity.getId());
        return query;
    }

    @Test
    public void deleteDangleInner() {
        // make up a dangling inner agent, using persistent service directly
        IEntity nodeServer = createNodeServer(seq++);
        IEntity agent = createAgent(nodeServer, "cronus", "8001");
        IEntity bsonAgent = persistenceService.get(buildQueryEntity(agent), cmsdbPersistcontext);
        // remove mongo _id and _oid to generat a new one
        ((ObjectNode) bsonAgent.getNode()).remove("_id");
        bsonAgent.removeField(InternalFieldEnum.ID.getName());
        bsonAgent.addFieldValue("resourceId", agent.getFieldValues("resourceId").get(0) + "-dangling");
        persistenceService.create(bsonAgent, cmsdbPersistcontext);

        IEntity getAgent = entityService.get(bsonAgent, context);
        Assert.assertNotNull(getAgent.getId());
        // assert this is a new agent 
        Assert.assertFalse(agent.getId().equals(getAgent.getId()));
        // assert this agent _hostentity is set, but not really in the node server
        Assert.assertNotNull(getAgent.getHostEntity());
        IEntity getNodeServer = entityService.get(nodeServer, context);
        Assert.assertTrue(getAgent.getHostEntity().contains(getNodeServer.getId()));
        
        // now try to delete this agent through entity API
        entityService.delete(getAgent, context);
        Assert.assertNull(entityService.get(getAgent, context));
    }
    
    
//    @Test
//    public void testCreateInnerEntity_multiple() {
//        IEntity nodeServer = createNodeServer(seq++);
//        IEntity cronusAgent = createAgent(nodeServer, "cronus", "8001");
//        IEntity tivoliAgent = createAgent(nodeServer, "tivoli", "8003");
//        // ganglia agent is a NEW agent, not created actually
//        IEntity gangliaAgent = newAgent(nodeServer, "ganglia-agent", "8652");
//
//        // delete agents with mixed content : some valid with some invalid
//        IEntity deleteNodeSever = newNodeServer(seq++);
//        deleteNodeSever.setId(nodeServer.getId());
//        deleteNodeSever.addFieldValue("agents", cronusAgent);
//        deleteNodeSever.addFieldValue("agents", gangliaAgent);
//        try {
//            entityService.deleteField(nodeServer, "agents", context);
//            Assert.fail();
//        } catch (CmsEntMgrException e) {
//            // expected
//            Assert.assertEquals(e.getErrorEnum(), EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE);
//        }
//
//        // assert data not deleted
//        Assert.assertNotNull(entityService.get(cronusAgent, context));
//        Assert.assertNotNull(entityService.get(tivoliAgent, context));
//        IEntity queryNodeServer = buildQueryEntity(nodeServer); 
//        IEntity getNodeServer = persistenceService.get(queryNodeServer, cmsdbPersistcontext);
//        Assert.assertTrue(getNodeServer.hasField("agents"));
//        Assert.assertEquals(2, getNodeServer.getFieldValues("agents").size());
//    }


}
