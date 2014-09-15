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
package com.ebay.cloud.cms.typsafe.service;

import java.util.Arrays;
import java.util.Random;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.ConsistencyTypeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.Repository;
import com.ebay.cloud.cms.typsafe.restful.Constants;
import com.ebay.cloud.cms.web.RunTestServer;

/**
 * @author liasu
 *
 */
public class MetadataServiceTest {

    private static final String CMSDB = "cmsdb";
    private static final String RAPTOR_PAAS = "raptor-paas";
    private static final String SOFTWARE_DEPLOYMENT = "software-deployment";
    private static final String STRATUS_CI = "stratus-ci";
    private static final String LOCAL_ENDPOINT = "http://localhost:9000/cms";
    private static final String RESOURCE_ID = "resourceId";
    private static final String NODE_SERVER = "NodeServer";
    private static final String RESOURCE_CAPCACITY = "ResourceCapacity";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static CMSClientService raptorService;
    private static CMSClientService sdService;
    private static CMSClientService stratusService;
    private static CMSClientService cmsdbService;
    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Random random = new Random(System.currentTimeMillis());
    private static CMSClientConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        RunTestServer.startServer(new String[] { "-initData", "9000" });

        config = new CMSClientConfig(LOCAL_ENDPOINT, RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE
                + ".raptor_paas");
        raptorService = CMSClientService.getClientService(config);

        sdService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, SOFTWARE_DEPLOYMENT,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".software_deployment"));

        stratusService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, STRATUS_CI,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".stratus_ci"));

        cmsdbService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, CMSDB,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".cmsdb"));

        Assert.assertNotNull(config.getPriority());
        config.setPriority(null);
        Assert.assertNull(config.getPriority());

        Assert.assertNotNull(config.getConsistentPolicy());
        config.setConsistentPolicy(null);
        Assert.assertNull(config.getConsistentPolicy());

        Assert.assertNotNull(config.getTimeOut());
        config.setTimeOut(100);
        Assert.assertNotNull(config.getTimeOut());

        Assert.assertNull(config.getAuthorization());
        config.setAuthorization("user:ci-test");
        Assert.assertNotNull(config.getAuthorization());
    }

    @AfterClass
    public static void teardown() throws Exception {
        RunTestServer.stopServer();
    }

    private String generateRandomName(String baseName) {
        return baseName + System.currentTimeMillis() + random.nextDouble();
    }

    //
    @Test
    public void testCreateRepo() {
        MetadataService service = new MetadataService(raptorService);
        CMSClientContext context = new CMSClientContext();
        Repository repo = new Repository();
        final String repositoryName = "new-repo";
        repo.setRepositoryName(repositoryName);
        service.createRepository(repo, context);
        Repository getRepo = service.getRepository(repo.getRepositoryName(), context);
        Assert.assertNotNull(getRepo);

        MetaClass meta1 = new MetaClass();
        meta1.setName("Ip");
        meta1.setDescription("");
        meta1.setRepository(repositoryName);

        MetaClass meta = new MetaClass();
        meta.setName("Node");
        meta.setDescription("");
        meta.setRepository(repositoryName);

        MetaAttribute attr = new MetaAttribute();
        attr.setName("name");
        attr.setMandatory(true);
        attr.setDataType(DataTypeEnum.STRING);
        attr.setCardinality(CardinalityEnum.One);

        MetaRelationship mr = new MetaRelationship();
        mr.setName("ips");
        mr.setCardinality(CardinalityEnum.Many);
        mr.setDataType(DataTypeEnum.RELATIONSHIP);
        mr.setConsistencyType(ConsistencyTypeEnum.Normal);
        mr.setRelationType(RelationTypeEnum.Reference);
        mr.setRefDataType("Ip");

        meta.addField(attr);
        meta.addField(mr);

        CMSClientConfig config = new CMSClientConfig(LOCAL_ENDPOINT, repositoryName, IBranch.DEFAULT_BRANCH,
                Constants.META_PACKAGE + ".new_repo");
        CMSClientService clientService = CMSClientService.getClientService(config);
        MetadataService newMetaService = new MetadataService(clientService);

        newMetaService.createMetadatas(Arrays.asList(meta1, meta), context);
        // assert
        MetaClass ipMeta = newMetaService.getMetadata("Ip", context);
        Assert.assertNotNull(ipMeta);

        MetaClass nodeMeta = newMetaService.getMetadata("Node", context);
        Assert.assertNotNull(nodeMeta);
    }
}
