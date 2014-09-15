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

package com.ebay.cloud.cms.service.resources;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.MockUriInfo;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.service.resources.impl.EntityResource;
import com.ebay.cloud.cms.service.resources.impl.MetadataResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;

public class MetadataVersionTest extends CMSResourceTest {

    private static final String     META_VERSION_TEST1_JSON          = "/meta-version-test1.json";
    private static final String     META_VERSION_TEST2_JSON          = "/meta-version-test2.json";

    private static final String     META_VERSION_TEST_CHILD_1_JSON   = "/meta-version-test-child1.json";
    private static final String     META_VERSION_TEST_CHILD_2_JSON   = "/meta-version-test-child2.json";

    
    private static final String     META_VERSION_TEST1_ENTITY        = "/MetaVersionTest1.json";
    private static final String     META_VERSION_TEST2_ENTITY        = "/MetaVersionTest2.json";
    private static final String     META_VERSION_TEST3_ENTITY        = "/MetaVersionTest3.json";
    
    private static final String     META_VERSION_TEST_CHILD1_ENTITY  = "/MetaVersionTestChild1.json";
    private static final String     META_VERSION_TEST_CHILD2_ENTITY  = "/MetaVersionTestChild2.json";
    private static final String     META_VERSION_TEST_CHILD3_ENTITY  = "/MetaVersionTestChild3.json";
    private static final String     META_VERSION_TEST_CHILD4_ENTITY  = "/MetaVersionTestChild4.json";

    private static final String     META_NAME                        = "MetaVersionTest";
    private static final String     META_CHILD_NAME                  = "MetaVersionTestChild";
    
    private static MetadataResource metaResource;
    
    private static EntityResource   entityResource;
    
    private static MockUriInfo      uriInfo;

    private static final String     RAPTOR_PAAS            = RAPTOR_REPO;

    @BeforeClass
    public static void setupResource() {
        metaResource = new MetadataResource();
        entityResource = new EntityResource();
        
        uriInfo = new MockUriInfo();
        uriInfo.getQueryParameters().put(CMSResourceUtils.REQ_PARAM_COMPONENT, Arrays.asList("unitTestUser"));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testMetadataVersion() {
        // 1. create meta class
        String meta = loadJson(META_VERSION_TEST1_JSON);
        CMSResponse resp = metaResource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        
        meta = loadJson(META_VERSION_TEST_CHILD_1_JSON);
        resp = metaResource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        
        // 2. create entity
        String entityStr = loadJson(META_VERSION_TEST1_ENTITY);
        uriInfo.map.add("comment", "create comment 1");
        Response response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        entityStr = loadJson(META_VERSION_TEST_CHILD1_ENTITY);
        uriInfo.map.add("comment", "create child comment 1");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        // 3. update meta class
        meta = loadJson(META_VERSION_TEST2_JSON);
        
        resp = metaResource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_NAME, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        
        // 4. create entity
        entityStr = loadJson(META_VERSION_TEST2_ENTITY);
        uriInfo.map.add("comment", "create comment 2");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        entityStr = loadJson(META_VERSION_TEST_CHILD2_ENTITY);
        uriInfo.map.add("comment", "create comment 2");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        // 5. update index
        resp = metaResource.createMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_NAME,
                "{\"indexName\": \"ut_testIndex\", \"keyList\": [\"parentId\", \"rootId\"], \"indexOptions\": [\"unique\"]}",
                new MockHttpServletRequest());
        assertOk(resp);
        
        resp = metaResource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_NAME, false, null, new MockHttpServletRequest());
        assertOk(resp);
        MetaClass newMeta = (MetaClass)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        Assert.assertEquals(2, newMeta.getVersion());
        
        // 6. update child meta
        meta = loadJson(META_VERSION_TEST_CHILD_2_JSON);
        
        resp = metaResource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_CHILD_NAME, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        
        resp = metaResource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_CHILD_NAME, false, null, new MockHttpServletRequest());
        assertOk(resp);
        newMeta = (MetaClass)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        Assert.assertEquals(3, newMeta.getVersion());
        
        // 7. create child entity with version = 3 && parentVersion = 2
        entityStr = loadJson(META_VERSION_TEST_CHILD3_ENTITY);
        uriInfo.map.add("comment", "create child comment 3");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        
        // 6. delete meta field 'isMain'
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
        server.getDBConfig().updateConfig(configs);
        
        resp = metaResource.deleteMetaField(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_NAME, "isMain",
                new MockHttpServletRequest());
        assertOk(resp);
        
        resp = metaResource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_NAME, false, null, new MockHttpServletRequest());
        assertOk(resp);
        newMeta = (MetaClass)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        Assert.assertEquals(3, newMeta.getVersion());
        
        resp = metaResource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, META_CHILD_NAME, false, null, new MockHttpServletRequest());
        assertOk(resp);
        newMeta = (MetaClass)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        Assert.assertEquals(4, newMeta.getVersion());
        
        
        // 7. create entity
        entityStr = loadJson(META_VERSION_TEST3_ENTITY);
        uriInfo.map.add("comment", "create comment 3");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp);
        
        entityStr = loadJson(META_VERSION_TEST_CHILD4_ENTITY);
        uriInfo.map.add("comment", "create child comment 4");
        response = entityResource.createEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME,
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        resp = (CMSResponse) response.getEntity();
        assertOkAndNotNullResult(resp); 
        
        
        // 8. get root entities on various versions
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-CMS-METAVERSION", "0");
        
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME, "MetaVersionTest-1", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
       
        ObjectNode entity1 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        JsonNode parentIdNode= entity1.get("parentId");
        Assert.assertNotNull(parentIdNode);
        JsonNode rootIdNode= entity1.get("rootId");
        Assert.assertNotNull(rootIdNode);
        JsonNode isMainNode= entity1.get("isMain");
        Assert.assertNotNull(isMainNode);
        JsonNode nameNode = entity1.get("name");
        Assert.assertNull(nameNode);
        JsonNode metaVersion= entity1.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(0, metaVersion.asInt());

        
        request.addHeader("X-CMS-METAVERSION", "1");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME, "MetaVersionTest-2", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        ObjectNode entity2 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity2.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity2.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity2.get("isMain");
        Assert.assertNotNull(isMainNode);
        nameNode = entity2.get("name");
        Assert.assertNull(nameNode);
        metaVersion= entity2.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(1, metaVersion.asInt());

        
        request.addHeader("X-CMS-METAVERSION", "3");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME, "MetaVersionTest-3", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        ObjectNode entity3 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity3.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity3.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity3.get("isMain");
        Assert.assertNull(isMainNode);
        nameNode = entity3.get("name");
        Assert.assertNotNull(nameNode);
        metaVersion= entity3.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(3, metaVersion.asInt());
        
        // get root entity with wrong meta version
        request.addHeader("X-CMS-METAVERSION", "0");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_NAME, "MetaVersionTest-2", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        ObjectNode entity4 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity4.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity4.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity4.get("isMain");
        Assert.assertNotNull(isMainNode);
        nameNode = entity4.get("name");
        Assert.assertNull(nameNode);
        metaVersion= entity4.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(1, metaVersion.asInt());
        
        // 9. get child entities on various versions
        request.addHeader("X-CMS-METAVERSION", "0");
        
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-1", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
       
        entity1 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity1.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity1.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity1.get("isMain");
        Assert.assertNotNull(isMainNode);
        nameNode = entity1.get("name");
        Assert.assertNull(nameNode);
        JsonNode nameNode1 = entity1.get("name1");
        Assert.assertNotNull(nameNode1);
        metaVersion= entity1.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(0, metaVersion.asInt());

        
        request.addHeader("X-CMS-METAVERSION", "1");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-2", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        entity2 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity2.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity2.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity2.get("isMain");
        Assert.assertNotNull(isMainNode);
        nameNode = entity2.get("name");
        Assert.assertNotNull(nameNode);
        nameNode1 = entity2.get("name1");
        Assert.assertNotNull(nameNode1);
        JsonNode nameNode2 = entity3.get("name2");
        Assert.assertNull(nameNode2);
        metaVersion= entity2.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(1, metaVersion.asInt());

        
        request.addHeader("X-CMS-METAVERSION", "3");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-3", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        entity3 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity3.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity3.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity3.get("isMain");
        Assert.assertNotNull(isMainNode);
        nameNode = entity3.get("name");
        Assert.assertNotNull(nameNode);
        nameNode1 = entity3.get("name1");
        Assert.assertNotNull(nameNode1);
        nameNode2 = entity3.get("name2");
        Assert.assertNotNull(nameNode2);
        metaVersion= entity3.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(3, metaVersion.asInt());
        
        
        request.addHeader("X-CMS-METAVERSION", "4");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-4", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        entity4 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity4.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity4.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity4.get("isMain");
        Assert.assertNull(isMainNode);
        nameNode = entity4.get("name");
        Assert.assertNotNull(nameNode);
        nameNode1 = entity4.get("name1");
        Assert.assertNotNull(nameNode1);
        nameNode2 = entity4.get("name2");
        Assert.assertNotNull(nameNode2);
        metaVersion= entity4.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(4, metaVersion.asInt());
        
        // 10. update entity to latest metaversion
        request.addHeader("X-CMS-METAVERSION", "3");
        response = entityResource.modifyEntityField(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-3", "name", 
                CMSPriority.NEUTRAL.toString(), null, "false", "\"newName\"", nullMode, request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        request.addHeader("X-CMS-METAVERSION", "4");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-3", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        entity3 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        parentIdNode= entity3.get("parentId");
        Assert.assertNotNull(parentIdNode);
        rootIdNode= entity3.get("rootId");
        Assert.assertNotNull(rootIdNode);
        isMainNode= entity3.get("isMain");
        Assert.assertNull(isMainNode);
        nameNode = entity3.get("name");
        Assert.assertNotNull("newName", nameNode.asText());
        nameNode1 = entity3.get("name1");
        Assert.assertNotNull(nameNode1);
        nameNode2 = entity3.get("name2");
        Assert.assertNotNull(nameNode2);
        metaVersion= entity3.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(4, metaVersion.asInt());
        
        // 11. entity deleteField to latest metaversion
//        request.addHeader("X-CMS-METAVERSION", "1");
        response = entityResource.deleteEntityField(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-2", "name", 
                CMSPriority.NEUTRAL.toString(), null, "", nullMode, request);
        resp = (CMSResponse) response.getEntity();
        
        request.addHeader("X-CMS-METAVERSION", "-1");
        response = entityResource.getEntity(uriInfo, RAPTOR_PAAS, "main", META_CHILD_NAME, "MetaVersionTestChild-2", CMSPriority.NEUTRAL.toString(),
                null, CMSQueryMode.URI.toString(), request);
        resp = (CMSResponse) response.getEntity();
        assertOk(resp);
        
        entity3 = (ObjectNode)((LinkedList<ObjectNode>)resp.get(CMSResponse.RESULT_KEY)).get(0);
        metaVersion= entity3.get("_metaversion");
        Assert.assertNotNull(metaVersion);
        Assert.assertEquals(4, metaVersion.asInt());
    }

}
