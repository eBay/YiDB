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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.Repository.AccessType;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.MockUriInfo;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.UriModeInfo;
import com.ebay.cloud.cms.service.resources.impl.MetadataResource;
import com.ebay.cloud.cms.service.resources.impl.QueryResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MetadataResourceTest extends CMSResourceTest {

    private static final String     META_TEST1_JSON        = "/meta-test1.json";
    private static final String     META_TEST2_JSON        = "/meta-test2.json";
    private static final String     META_TEST3_JSON        = "/meta-test3.json";

    private static final String     META_TEST1_UPDATE_JSON = "/meta-test1-update.json";

    private static final String     TEST_REPO1             = "test-repo1";

    private static final String     APPLICATION_SERVICE    = "ApplicationService";

    private static IMetadataResource resource;

    private static final String     RAPTOR_PAAS            = RAPTOR_REPO;

    @BeforeClass
    public static void setupResource() {
        resource = new MetadataResource();
        
        String meta = loadJson(META_TEST3_JSON);
        CMSResponse response = resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(response);
    }

    @Test
    public void testGetRepositorys() {
        CMSResponse response = resource.getRepositories(nullMockUri, CMSPriority.NEUTRAL.toString(), mockHttpRequest);

        assertOkAndNotNullResult(response);
    }

    @Test
    public void testGetRepository() {
        CMSResponse response = resource.getRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, mockHttpRequest);

        assertOkAndNotNullResult(response);
    }

    @Test(expected = NotFoundException.class)
    public void testGetRepository01() {
        resource.getRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), "raptor1", mockHttpRequest);
    }

    @Test
    public void testGetMetaClasses() {
        CMSResponse response = resource.getMetaClasses(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS,
                CMSQueryMode.URI.toString(), mockHttpRequest);

        assertOkAndNotNullResult(response);
    }

    @Test
    public void testGetMetaClasses01() {
        try {
            resource.getMetaClasses(nullMockUri, CMSPriority.NEUTRAL.toString(), "repo1", CMSQueryMode.URI.toString(), mockHttpRequest);
        } catch (NotFoundException e) {
            // expected
        }

        try {
            resource.getMetaClasses(nullMockUri, "normal1", RAPTOR_REPO, "url", mockHttpRequest);
        } catch (BadParamException e) {
            // expected
        }
    }

    @Test
    public void testGetMetadata() {
        CMSResponse response = resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, APPLICATION_SERVICE,
                false, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);
    }

    @Test
    public void testGetMetadata01() {
        try {
            resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), "repo1", APPLICATION_SERVICE, false,
                    CMSQueryMode.URI.toString(), new MockHttpServletRequest());
            Assert.fail();
        } catch (NotFoundException e) {
            // expected
        }
    }
    
    @Test
    public void testGetMetadata02() {
        CMSResponse resp = resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), STRATUS_REPO, APPLICATION_SERVICE,
                false, CMSQueryMode.SHOWALL.toString().toLowerCase(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        JsonNode fieldsNode= ((JsonNode)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0)).get("fields");
        Set<String> jsonFieldNames = new HashSet<String>();
        Iterator<String> it = fieldsNode.getFieldNames();
        while(it.hasNext()) {
            jsonFieldNames.add(it.next());
        }
        
        MetaClass meta = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, APPLICATION_SERVICE);
        Collection<String> allMetaFieldNames = meta.getFieldNames();
        
        for (String metaName : jsonFieldNames) {
            if (!meta.getFieldByName(metaName).isInternal()) {
                Assert.assertTrue(metaName, allMetaFieldNames.contains(metaName));
            }
        }
    }
    
    @Test
    public void testGetMetadata03() {
        QueryResource queryResource = new QueryResource();
        UriInfo ui = new UriModeInfo();
        String queryString = "ApplicationService.applicationService!ServiceInstance";
        queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(),
                STRATUS_REPO, "main", queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest());
        
        CMSResponse resp = resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), STRATUS_REPO, APPLICATION_SERVICE,
                false, CMSQueryMode.SHOWALL.toString().toLowerCase(), new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        JsonNode fieldsNode= ((JsonNode)((List<?>)resp.get(CMSResponse.RESULT_KEY)).get(0)).get("fields");
        Set<String> jsonFieldNames = new HashSet<String>();
        Iterator<String> it = fieldsNode.getFieldNames();
        while(it.hasNext()) {
            jsonFieldNames.add(it.next());
        }
        
        MetaClass meta = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, APPLICATION_SERVICE);
        for (String metaName : jsonFieldNames) {
            Assert.assertTrue(!meta.getFieldByName(metaName).isVirtual());
        }
    }
    
    @Test
    public void testGetMetadata04() {
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-CMS-METAVERSION", "abcde");
        
        try {
            resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), STRATUS_REPO, APPLICATION_SERVICE,
                false, CMSQueryMode.SHOWALL.toString().toLowerCase(), request);
            Assert.fail();
        } catch (BadParamException e) {

        }
    }

    @Test
    public void testGetMetadataHistory() {
        CMSResponse response = resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, "MetaTest3",
                true, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOkAndNullResult(response);
    }
    
    @Test
    public void testGetMetadataHistory01DateStart() {
        UriInfo uriInfo = new MockUriInfo();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();

        parameters.add("start", Long.toString(new Date().getTime()));
        CMSResponse response = resource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, "MetaTest3",
                true, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOk(response);
        List<?> results = (List<?>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testGetMetadataHistory02DateEnd() {
        UriInfo uriInfo = new MockUriInfo();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        parameters.add("end", Long.toString(new Date().getTime() - 1000L * 3600L));// set end as 3600 seconds(one hour) before this case, where should be not log found. 
        CMSResponse response = resource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, "MetaTest3",
                true, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOk(response);
        List<?> results = (List<?>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertTrue(results.isEmpty());
    }
    
    @Test
    public void testGetMetadataHistory03Limit() {
        UriInfo uriInfo = new MockUriInfo();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        parameters.add("limit", "1");
        CMSResponse response = resource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, "MetaTest3",
                true, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOk(response);
        List<?> results = (List<?>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(0, results.size());
    }
    
    @Test
    public void testGetMetadataHistory04Skip() {
        UriInfo uriInfo = new MockUriInfo();
        MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
        parameters.add("skip", "1");
        CMSResponse response = resource.getMetadata(uriInfo, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, "MetaTest3",
                true, CMSQueryMode.URI.toString(), new MockHttpServletRequest());

        assertOk(response);
        List<?> results = (List<?>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertTrue(results.isEmpty());
    }
    
    @Test
    public void testGetMetadataHierarchy() {
        String noParentMetaName = "Base";
        Map<String, List<String>> hierarchy = __getMetaHierarchy(CMSDB_REPO, noParentMetaName);
        Assert.assertTrue(hierarchy.containsKey("ancestors"));
        Assert.assertTrue(hierarchy.containsKey("descendants"));
        Assert.assertEquals(0, hierarchy.get("ancestors").size());
        Assert.assertTrue(hierarchy.get("descendants").size() > 0);
    }
    
    @Test
    public void testGetMetadataHierarchy2() {
        String noChildMetaName = "ApplicationService";
        Map<String, List<String>> hierarchy = __getMetaHierarchy(CMSDB_REPO, noChildMetaName);
        Assert.assertTrue(hierarchy.containsKey("ancestors"));
        Assert.assertTrue(hierarchy.containsKey("descendants"));
        Assert.assertEquals(2, hierarchy.get("ancestors").size());
        Assert.assertEquals(0, hierarchy.get("descendants").size());
    }

    @Test
    public void testGetMetadataHierarchy3() {
        String noParentChlidMetaName = "ApplicationService";
        Map<String, List<String>> hierarchy = __getMetaHierarchy(RAPTOR_REPO, noParentChlidMetaName);
        Assert.assertTrue(hierarchy.containsKey("ancestors"));
        Assert.assertTrue(hierarchy.containsKey("descendants"));
        Assert.assertEquals(0, hierarchy.get("ancestors").size());
        Assert.assertEquals(0, hierarchy.get("descendants").size());
    }

    @Test(expected = NotFoundException.class)
    public void testGetMetadataHierarchy4() {
        String notFoundMetaName = "notFound";
        __getMetaHierarchy(RAPTOR_REPO, notFoundMetaName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> __getMetaHierarchy(String repoName, String noParentChlidMetaName) {
        final UriInfo uriInfo = new MockUriInfo();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final String priority = CMSPriority.NEUTRAL.toString();
        CMSResponse resp = null;
        resp = resource.getMetadataHierarchy(uriInfo, priority, repoName, noParentChlidMetaName, request);
        assertOkAndNotNullResult(resp);
        Collection<?> result = null;
        result = (Collection<?>) resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, result.size());
        Iterator<?> it = result.iterator();
        Map<String, List<String>> hierarchy = (Map<String, List<String>>) it.next();
        return hierarchy;
    }

    @Test
    public void testCreateRepository() {
        Repository repository = new Repository();
        repository.setAccessType(AccessType.Private);
        repository.setRepositoryName(TEST_REPO1);
        repository.setRepositoryAdmin("unittest");
        CMSResponse response = resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
        
        Repository getRepo = server.getRepository(CMSPriority.NEUTRAL, TEST_REPO1);
        Assert.assertEquals(repository.getAccessType(), getRepo.getAccessType());
        Assert.assertEquals("unittest", getRepo.getRepositoryAdmin());

        assertOkAndNotNullResult(response);

        response = resource.getRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), TEST_REPO1, mockHttpRequest);
        assertOkAndNotNullResult(response);

        // case 1: duplicate repository
        try {
            resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
        } catch (CMSServerException e) {
            // expected
            e.printStackTrace();
        }
    }

    @Test(expected = BadParamException.class)
    public void testCreateRepository01() {
        Repository repository = new Repository();
        repository.setAccessType(AccessType.Private);
        resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
    }
    
    @Test(expected = BadParamException.class)
    public void testCreateRepository02() {
        Repository repository = new Repository();
        repository.setAccessType(AccessType.Private);
        StringBuffer sb = new StringBuffer();
        for(int i=0; i <= CMSConsts.MAX_LENGTH_OF_REPO_NAME; i++) {
        	sb.append("a");
        }
        String repositoryName = sb.toString();
        repository.setRepositoryName(repositoryName);
        resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
    }
    
    @Test(expected = BadParamException.class)
    public void testCreateRepository03() {
        Repository repository = new Repository();
        repository.setAccessType(AccessType.Private);
        Random randomGenerator = new Random();
        char[] invalidChars = CMSConsts.INVALID_REPOSITORY_NAME_CHARACTERS;
        String repositoryName = String.valueOf(invalidChars[randomGenerator.nextInt(invalidChars.length)]);
        repository.setRepositoryName(repositoryName);
        resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
    }
    
    @Test(expected = BadParamException.class)
    public void testCreateRepository04() {
        Repository repository = new Repository();
        repository.setAccessType(AccessType.Private);
        String repositoryName = " ";
        repository.setRepositoryName(repositoryName);
        resource.createRepository(nullMockUri, CMSPriority.NEUTRAL.toString(), repository, mockHttpRequest);
    }

    @Test
    public void testUpdateRepo() {
        String oldAdmin = "unittest";
        String repositoryName = "test-update-repo";
        AccessType oldType = AccessType.Private;
        Repository repository = new Repository();
        repository.setAccessType(oldType);
        repository.setRepositoryName(repositoryName);
        repository.setRepositoryAdmin(oldAdmin);
        resource.createRepository(nullMockUri, nullPriority, repository, mockHttpRequest);
        
        Repository getRepo = server.getRepository(CMSPriority.NEUTRAL, repositoryName);
        Assert.assertEquals(oldType, getRepo.getAccessType());
        Assert.assertEquals(oldAdmin, getRepo.getRepositoryAdmin());

        String newAdmin = "newAdmin";
        AccessType newType = AccessType.Public;
        repository.setRepositoryAdmin(newAdmin);
        repository.setAccessType(newType);
        CMSResponse resp = resource.updateRepository(nullMockUri, nullPriority, repositoryName, repository, mockHttpRequest);
        assertOk(resp);
        resp = resource.getRepository(nullMockUri, nullPriority, repositoryName, mockHttpRequest);
        assertOkAndNotNullResult(resp);
        getRepo = server.getRepository(CMSPriority.NEUTRAL, repositoryName);
        Assert.assertEquals(newType, getRepo.getAccessType());
        Assert.assertEquals(newAdmin, getRepo.getRepositoryAdmin());
    }

    @Test
    public void testUpdateRepo_validation() {
        Repository repo = new Repository("raptor-dds");
        try {
            resource.updateRepository(nullMockUri, nullPriority, RAPTOR_PAAS, repo, mockHttpRequest);
            Assert.fail();
        } catch (BadParamException bpe) {
            // expected
        }
//        repo.setRepositoryName(RAPTOR_PAAS);
//        repo.setAccessType(AccessType.Private);
//        try {
//            resource.updateRepository(nullMockUri, nullPriority, RAPTOR_PAAS, repo, mockHttpRequest);
//            Assert.fail();
//        } catch (BadParamException bpe) {
//            // expected
//        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMetaClass() throws JsonGenerationException, JsonMappingException, IOException {
        String meta = loadJson(META_TEST1_JSON);
        CMSResponse response = resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        ObjectMapper mapper = new ObjectMapper();
        for (Object obj : result) {
            if (obj instanceof MetaClass) {
                MetaClass clz = (MetaClass) obj;
                String metaclz = mapper.writeValueAsString(clz);
                Assert.assertTrue(clz.getIndexes().size() > 1);
                System.out.println(metaclz);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMetaClassWithRepo() throws JsonGenerationException, JsonMappingException, IOException {
        String meta = loadJson(META_TEST2_JSON);
        CMSResponse response = resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        ObjectMapper mapper = new ObjectMapper();
        for (Object obj : result) {
            if (obj instanceof MetaClass) {
                MetaClass clz = (MetaClass) obj;
                String metaclz = mapper.writeValueAsString(clz);
                Assert.assertTrue(clz.getIndexes().size() > 1);
                System.out.println(metaclz);
            }
        }
    }

    @Test
    public void testGetMetadataIndex() throws Exception {
        CMSResponse response = resource.getMetadata(new MockUriInfo(), CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, APPLICATION_SERVICE,
                false, CMSQueryMode.URI.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(response);
    }

    @Test
    public void testCreateMetadataIndex() throws Exception {
        DB raptorDb = getDataSource().getMongoInstance().getDB(RAPTOR_PAAS);
        DBCollection coll = raptorDb.getCollection("main_ApplicationService");

        CMSResponse resp = resource.getMetadata(new MockUriInfo(), null, RAPTOR_PAAS, APPLICATION_SERVICE, false, null,
                new MockHttpServletRequest());
        MetaClass oldMeta = (MetaClass) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
        List<DBObject> oldIndexObjects = coll.getIndexInfo();
        Set<String> oldIndexInDb = new HashSet<String>();
        for (DBObject dbo : oldIndexObjects) {
            oldIndexInDb.add(dbo.get("name").toString());
        }
        // assert the index consistency
        for (IndexInfo ii : oldMeta.getOptions().getIndexes()) {
            Assert.assertTrue(oldIndexInDb.contains(ii.getIndexName()));
        }

        CMSResponse response = resource
                .createMetadataIndex(nullMockUri, 
                        CMSPriority.NEUTRAL.toString(),
                        RAPTOR_PAAS,
                        APPLICATION_SERVICE,
                        "{\"indexName\": \"ut_appTierIndex\", \"keyList\": [\"archTier\", \"name\"], \"indexOptions\": [\"unique\"]}",
                        new MockHttpServletRequest());
        // assert create index operation ok
        assertOk(response);
        
        // more assertion, the old indexes are there, also the new indexes
        CMSResponse newResp = resource.getMetadata(new MockUriInfo(), null, RAPTOR_PAAS, APPLICATION_SERVICE, false, null,
                new MockHttpServletRequest());
        assertOk(newResp);
        MetaClass newMeta = (MetaClass)((List<?>)newResp.get(CMSResponse.RESULT_KEY)).get(0);
        List<DBObject> indexObjects = coll.getIndexInfo();
        Set<String> newIndexInDb = new HashSet<String>();
        for (DBObject dbo :indexObjects) {
            newIndexInDb.add(dbo.get("name").toString());
        }

        // old indexes must be kept
        for (IndexInfo ii : oldMeta.getOptions().getIndexes()) {
            Assert.assertTrue(newIndexInDb.contains(ii.getIndexName()));
        }
        // new index are added
        for (IndexInfo ii : newMeta.getOptions().getIndexes()) {
            Assert.assertTrue(newIndexInDb.contains(ii.getIndexName()));
        }
    }

    @Test
    public void testDeleteMetadataIndex() throws Exception {
        CMSResponse response = resource.deleteMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS,
                APPLICATION_SERVICE, "appTierIndex", new MockHttpServletRequest());
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
        
        CMSResponse resp = resource.getMetadata(new MockUriInfo(), null, RAPTOR_PAAS, APPLICATION_SERVICE, false, null,
                new MockHttpServletRequest());
        MetaClass metadata = (MetaClass) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
        Assert.assertNull(metadata.getOptions().getIndexByName("appTierIndex"));
    }

    // **Dependent on testCreateMetaClass()
    @Test
    public void testUpdateMetadataIndexExists() throws JsonParseException, JsonMappingException, IOException {
        String meta = loadJson(META_TEST1_UPDATE_JSON);
        ObjectMapper mapper = new ObjectMapper();
        MetaClass m = mapper.readValue(meta, MetaClass.class);
        IndexInfo index = new IndexInfo();
        index.setIndexName("meta_test1_indexName");// an exsitng index create in
                                                   // testCreateMetaClass case;
        index.addKeyField("branchStatus");
        m.addIndex(index);

        CMSResponse response = resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, m.getName(), meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);
    }

    // **Dependent on testCreateMetaClass()
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateMetadata() throws JsonParseException, JsonMappingException, IOException {
        String meta = loadJson(META_TEST1_UPDATE_JSON);
        ObjectMapper mapper = new ObjectMapper();
        MetaClass m = mapper.readValue(meta, MetaClass.class);

        CMSResponse response = resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, m.getName(), meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        for (Object obj : result) {
            if (obj instanceof MetaClass) {
                MetaClass clz = (MetaClass) obj;
                String metaclz = mapper.writeValueAsString(clz);
                System.out.println(metaclz);
            }
        }
    }

    @Test
    public void testUpdateMetadata01() throws JsonParseException, JsonMappingException, IOException {
        String metatype = "ApplicationService";

        Mongo mongo = getDataSource().getMongoInstance();
        DB db = mongo.getDB("raptor-paas");
        DBCollection coll = db.getCollection("main_ApplicationService");

        CMSResponse resp = resource.getMetadata(new MockUriInfo(), null, RAPTOR_PAAS, metatype, false, CMSQueryMode.NORMAL.toString(),
                new MockHttpServletRequest());
        MetaClass oldMeta = (MetaClass) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
        // update metadata should not delete indexes. So,
        // assert old indexes on ServiceInstance are still there, and new
        // indexes are added
        List<DBObject> indexObjects = coll.getIndexInfo();
        Set<String> indexNamesInDB = new HashSet<String>();
        for (DBObject obj : indexObjects) {
            indexNamesInDB.add(obj.get("name").toString());
        }
        // assert old indexes are kept
        for (IndexInfo ii : oldMeta.getOptions().getIndexes()) {
            Assert.assertTrue(" index " + ii.getIndexName() + " not found", indexNamesInDB.contains(ii.getIndexName()));
        }

//        MetaClass metaClass = new MetaClass();
//        metaClass.setName(metatype);
//        IndexInfo newIndex = new IndexInfo();
//        newIndex.setIndexName("newIndex_fromMetaUpdate");
//        newIndex.addKeyField("name");
//        newIndex.addKeyField("archTier");
//        newIndex.addKeyField("healthStatus");
//        newIndex.addOption(IndexOptionEnum.unique);
//        metaClass.getOptions().addIndex(newIndex);
        
        String meta = loadJson("/update_metadata01.txt");
        MetaClass metaClass = new ObjectConverter<MetaClass>().fromJson(meta, MetaClass.class);

        CMSResponse response = resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS,
                metaClass.getName(), meta, CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
        assertOkAndNotNullResult(response);


        System.out.println(indexNamesInDB);

        //refetch the index
        indexObjects = coll.getIndexInfo();
        indexNamesInDB.clear();
        for (DBObject obj : indexObjects) {
            indexNamesInDB.add(obj.get("name").toString());
        }
        
        // assert old indexes are kept
        for (IndexInfo ii : oldMeta.getOptions().getIndexes()) {
            Assert.assertTrue(" index " + ii.getIndexName() + " not found", indexNamesInDB.contains(ii.getIndexName()));
        }

        // assert new indexes are also there
        for (IndexInfo ii : metaClass.getOptions().getIndexes()) {
            Assert.assertTrue(" index " + ii.getIndexName() + " not found", indexNamesInDB.contains(ii.getIndexName()));
        }
    }

    /**
     * CMS-3507
     */
    @Test
    public void testUpdateMetaClass02() {
        // 1. update rack field
        String rackFieldJson = loadJson("/RackFieldOnRow.json");
        String rowJson = loadJson("/Row.json");
        CMSResponse resp = resource.updateMetadata(nullMockUri, nullPriority, CMSDB_REPO, "Row", "racks",
                rackFieldJson, nullMode, mockHttpRequest);
        assertOkAndNotNullResult(resp);
        // 2. update rack field
        resp = resource.updateMetadata(nullMockUri, nullPriority, CMSDB_REPO, "Row", rowJson, nullMode,
                mockHttpRequest);
        assertOkAndNotNullResult(resp);
    }

    @Test
    public void testDeleteMetaField() {
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
        server.getDBConfig().updateConfig(configs);
        
        CMSResponse resp = resource.deleteMetaField(nullMockUri, nullPriority, RAPTOR_PAAS, SERVICE_INSTANCE, "https",
                new MockHttpServletRequest());
        assertOk(resp);
        MetaClass mc = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, SERVICE_INSTANCE);
        Assert.assertNotNull(mc);
        Assert.assertTrue(mc.getFieldByName("https") == null);
    }

    @Test
    public void testDeleteMetaFieldNotAllowed() {
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, false);
        server.getDBConfig().updateConfig(configs);

        try {
            resource.deleteMetaField(nullMockUri, nullPriority, RAPTOR_PAAS, SERVICE_INSTANCE, "https", new MockHttpServletRequest());
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }
    }

    @Test
    public void testDeleteMetadataNotAllowed() {
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, false);
        server.getDBConfig().updateConfig(configs);

        try {
            resource.deleteMetadata(nullMockUri, nullPriority, RAPTOR_PAAS, SERVICE_INSTANCE, new MockHttpServletRequest());
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }
    }    
    

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteMetaclass() throws JsonGenerationException, JsonMappingException, IOException {
        Map<String, Object> configs = new HashMap<String, Object>();
        configs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
        server.getDBConfig().updateConfig(configs);
        
        String meta = loadJson(META_TEST1_JSON);
        CMSResponse response = resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_PAAS, meta,
                CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());

        assertOkAndNotNullResult(response);
        
        String metaClassName = "meta-test1";
        
        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        List<String> metaClassNames = new ArrayList<String>();
        for (Object obj : result) {
            if (obj instanceof MetaClass) {
                MetaClass clz = (MetaClass) obj;
                metaClassNames.add(clz.getName());
            }
        }
        
        Assert.assertTrue(metaClassNames.contains(metaClassName));
        
        CMSResponse resp = resource.deleteMetadata(nullMockUri, nullPriority, RAPTOR_PAAS, metaClassName, mockHttpRequest);
        assertOk(resp);
        try {
            resource.getMetadata(nullMockUri, nullPriority, RAPTOR_PAAS, metaClassName, false, nullMode,
                    mockHttpRequest);
            Assert.fail();
        } catch (NotFoundException e) {
            // expecteds
        }
    }

}
