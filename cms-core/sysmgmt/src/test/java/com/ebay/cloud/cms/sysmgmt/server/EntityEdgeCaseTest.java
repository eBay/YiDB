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

package com.ebay.cloud.cms.sysmgmt.server;

import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class EntityEdgeCaseTest extends CMSMongoTest {

    private static final String SOURCE_IP = "127.0.0.1";
    private static final CMSPriority   P                           = CMSPriority.NEUTRAL;
    private static final String        CMSDB                       = "cmsdb";
    private static final String        CMSDB_RUNTIME               = "cmsdbRuntime.json";
    private static final String        STRATUS_RUNTIME             = "stratusRuntime.json";
    private static final String        SOFTWARE_DEPLOYMENT_RUNTIME = "softwareDeploymentRuntime.json";
    private static final String        SOFTWARE_DEPLOYMENT         = "software-deployment";
    private static final String        SERVICE_INSTANCE            = "ServiceInstance";
    private static final String        STRATUS_REPO                = "stratus-ci";
    private static final String        APPLICATION_SERVICE         = "ApplicationService";
    private static final String        BRANCH_MAIN                 = "main";
    private static final String        RAPTOR_PAAS                 = "raptor-paas";
    private static final String        RAPTOR_TEST_DATA            = "raptorTopology.json";
    private static CMSServer           server;
    private static QueryContext        raptorQueryContext;
    private static MetadataContext     metaContext;
    private static EntityContext CTX;
    private static CMSDBConfig config;
    @BeforeClass
    public static void setup() {
        MongoDataSource dataSource = new MongoDataSource(getConnectionString());
        config = new CMSDBConfig(dataSource);
        MetadataDataLoader.getInstance(dataSource).loadTestDataFromResource();
        MetadataDataLoader.getInstance(dataSource).loadCMSDBMetaDataFromResource();
        
        IRepositoryService repositoryService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        RuntimeDataLoader.getDataLoader(dataSource, repositoryService, RAPTOR_PAAS).load(RAPTOR_TEST_DATA);
        RuntimeDataLoader.getDataLoader(dataSource, repositoryService, SOFTWARE_DEPLOYMENT).load(SOFTWARE_DEPLOYMENT_RUNTIME);
        RuntimeDataLoader.getDataLoader(dataSource, repositoryService, STRATUS_REPO).load(STRATUS_RUNTIME);
        RuntimeDataLoader.getDataLoader(dataSource, repositoryService, CMSDB).load(CMSDB_RUNTIME);
        
        server = CMSServer.getCMSServer();
        server.start();

        CTX = newEntityContext();

        raptorQueryContext = new QueryContext(RAPTOR_PAAS, BRANCH_MAIN);
        raptorQueryContext.setSourceIP(SOURCE_IP);

        metaContext = new MetadataContext();
        metaContext.setSourceIp(SOURCE_IP);
        metaContext.setSubject("unitTestUser");
    }

    private static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        context.setDbConfig(config);
        return context;
    }

    @Test
    public void test_modifyArray_SetAsEmpty() {
        final String fieldName = "manifestRef";
        JsonEntity servEntity = createJsonEntity(SERVICE_INSTANCE, "4fbb314fc681caf13e283a7a");
        ObjectNode servNode = servEntity.getNode();
        // add empty array
        servNode.put(fieldName, JsonNodeFactory.instance.arrayNode());
        // assert before modify
        IEntity getServ = server.get(P, servEntity, Collections.<String, List<String>> emptyMap(), CTX);
        Assert.assertTrue(getServ.hasField(fieldName));
        Assert.assertTrue(getServ.getFieldValues(fieldName).size() > 0);
        // modify
        server.modify(P, servEntity, servEntity, CTX);
        // now assert
        getServ = server.get(P, servEntity, Collections.<String, List<String>> emptyMap(), CTX);
        Assert.assertTrue(getServ.hasField(fieldName));
        Assert.assertTrue(getServ.getFieldValues(fieldName).size() == 0);
    }

    @Test
    public void test_modifyLong_SetNull() {
        final String fieldName = "numServices";
        JsonEntity servEntity = createJsonEntity("Environment", "4fbb314fc681caf13e283a78");
        ObjectNode servNode = servEntity.getNode();
        // add empty array
        servNode.put(fieldName, JsonNodeFactory.instance.nullNode());
        // assert before modify
        IEntity getServ = server.get(P, servEntity, Collections.<String, List<String>> emptyMap(), CTX);
        Assert.assertFalse(getServ.hasField(fieldName));
        // modify
        server.modify(P, servEntity, servEntity, CTX);
        // now assert
        getServ = server.get(P, servEntity, Collections.<String, List<String>> emptyMap(), CTX);
        Assert.assertFalse(getServ.hasField(fieldName));
    }

    @Test
    public void test_modifyReferenceArray_SetNull() {
        final String fieldName = "services";
        JsonEntity appEntity = createJsonEntity(APPLICATION_SERVICE, "4fbb314fc681caf13e283a76");
        appEntity.getNode().put(fieldName, JsonNodeFactory.instance.nullNode());
        EntityContext ctx = newEntityContext();
        // assert before modify
        IEntity getApp = server.get(P, appEntity, Collections.<String, List<String>> emptyMap(), ctx);
        Assert.assertTrue(getApp.hasField(fieldName));
        Assert.assertTrue(getApp.getFieldValues(fieldName).size() > 0);
        // modify
        server.modify(P, appEntity, appEntity, ctx);
        getApp = server.get(P, appEntity, Collections.<String, List<String>> emptyMap(), CTX);
        Assert.assertTrue(getApp.hasField(fieldName));
        Assert.assertTrue(getApp.getFieldValues(fieldName).size() == 0);
    }

    private JsonEntity createJsonEntity(String meta, String oid) {
        MetaClass appMetaClass = server.getMetaClass(P, RAPTOR_PAAS, meta);
        JsonEntity servEntity = new JsonEntity(appMetaClass);
        servEntity.setId(oid);
        servEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        return servEntity;
    }

}
