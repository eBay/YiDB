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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.service.resources.impl.QueryResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class QueryResourceTest extends CMSResourceTest {

    private static final String QUERY_STRING = "ApplicationService[@archTier=\"app\"]";
    private static final String BRANCH_MAIN  = "main";
    private static final String RAPTOR_PAAS  = "raptor-paas";
    private QueryResource       queryResource;

    @Before
    public void setupResource() {
        queryResource = new QueryResource();
    }

    public static final class NormalModeInfo extends MockUriInfo {
        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            MultivaluedMap<String, String> map = super.getQueryParameters();
            map.add("mode", "normal");
            return map;
        }
    }

    public static final class UriModeInfo extends MockUriInfo {
        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            MultivaluedMap<String, String> map = super.getQueryParameters();
            map.add(QueryResource.QueryParameterEnum.explain.toString(), "true");
            map.add("mode", "uri");
            map.add("uselsesParmater", "true");
            map.add("maxFetch", "1500");
            return map;
        }
    }

    public static class MockUriInfo implements UriInfo {

        MultivaluedMap<String, String> map = new MultivaluedMapImpl();

        public MockUriInfo() {
            map.add(QueryResource.QueryParameterEnum.explain.toString(), "true");
            map.add(QueryResource.QueryParameterEnum.allowFullTableScan.toString(), "true");
        }

        @Override
        public UriBuilder getRequestUriBuilder() {
            return null;
        }

        @Override
        public URI getRequestUri() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            return map;
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            return null;
        }

        @Override
        public List<PathSegment> getPathSegments() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            return null;
        }

        @Override
        public String getPath(boolean decode) {
            return null;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            return null;
        }

        @Override
        public List<String> getMatchedURIs() {
            return null;
        }

        @Override
        public List<Object> getMatchedResources() {
            return null;
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            return null;
        }

        @Override
        public URI getBaseUri() {
            return null;
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            return null;
        }

        @Override
        public URI getAbsolutePath() {
            return null;
        }
    }

    @Test
    public void testQuery() {
        UriInfo ui = new MockUriInfo();

        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, QUERY_STRING, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
    }

    @Test
    public void testQueryModeUri() {
        UriInfo ui = new UriModeInfo();

        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, QUERY_STRING, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
    }

    @Test
    public void testQueryUselessQueryParameter() {
        UriInfo ui = new UriModeInfo();

        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, QUERY_STRING, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAggregaionActiveAPIQuery() {
        UriInfo ui = new UriModeInfo();

        String queryString = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"].runsOn[@name=\"compute-00010\"]";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        // assert retrieved fields
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        JsonNode entity = results.get(0);
        Assert.assertNotNull(entity.get("https"));
        Assert.assertFalse(entity.get("https").isNull());
        Assert.assertNotNull(entity.get("activeManifestDiff"));
        Assert.assertFalse(entity.get("activeManifestDiff").isNull());
        Assert.assertNotNull(entity.get("$max_port"));
        Assert.assertFalse(entity.get("$max_port").isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldPropertyActiveAPIQuery() {
        UriInfo ui = new UriModeInfo();

        String queryString = "ApplicationService[@services.$_length > 1]{@services.$_length, @services.$_lastmodified}";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        // assert retrieved fields
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(2, results.size());
        JsonNode entity = results.get(0);
        System.out.println(entity);

        Assert.assertNotNull(entity.get("services._length"));
        Assert.assertFalse(entity.get("services._length").isNull());
        
        // services is not projected
        Assert.assertFalse(entity.has("services"));

        Assert.assertNotNull(entity.get("services._lastmodified"));
        Assert.assertFalse(entity.get("services._lastmodified").isNull());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testRelationWithEmptyList() {
        UriInfo ui = new UriModeInfo();
        
        String queryString = "Dep[@_oid=\"dep011\"]";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();

        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        JsonNode entity = results.get(0);
        
        Assert.assertTrue(entity.has("team"));
        
        queryString = "Dep[@_oid=\"dep011\"]{@team.$_length}";
        response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();

        results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        entity = results.get(0);
        
        Assert.assertFalse(entity.has("team"));
        Assert.assertNotNull(entity.get("team._length"));
        Assert.assertFalse(entity.get("team._length").isNull());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testFieldPropertyWithEmptyList() {
        UriInfo ui = new UriModeInfo();
        
        String queryString = "Room[@_oid=\"room07\"]{@window}";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        // assert retrieved fields
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        JsonNode entity = results.get(0);

        Assert.assertTrue(entity.has("window"));
        Assert.assertEquals(0, entity.get("window").size());

        
        queryString = "Room[@_oid=\"room07\"]{@window.$_length, @window.$_lastmodified}";
        response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        // assert retrieved fields
        results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        entity = results.get(0);

        Assert.assertNotNull(entity.get("window._length"));
        Assert.assertFalse(entity.get("window._length").isNull());
        
        // services is not projected
        Assert.assertFalse(entity.has("window"));

        Assert.assertNotNull(entity.get("window._lastmodified"));
        Assert.assertFalse(entity.get("window._lastmodified").isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFieldPropertyNormalQuery() {
        UriInfo ui = new NormalModeInfo();

        String queryString = "ApplicationService[@updateStrategies.$_length > 1]{@updateStrategies.$_length}";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        // assert retrieved fields
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(2, results.size());
        JsonNode entity = results.get(0);

        Assert.assertNotNull(entity.get("updateStrategies._length"));
        Assert.assertTrue(!entity.get("updateStrategies._length").isNull());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAggregationServiceInstanceStat() throws Exception {
        UriInfo ui = new NormalModeInfo();

        String queryString = "ServiceInstance<@healthStatus>{ @healthStatus, $count() }";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(response));

        // assert retrieved fields
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(3, results.size());
        JsonNode entity = results.get(0);
        Assert.assertNotNull(entity.get("healthStatus"));
        Assert.assertTrue(!entity.get("healthStatus").isNull());
        Assert.assertNotNull(entity.get("$count"));
        Assert.assertTrue(!entity.get("$count").isNull());
    }

    @Test
    public void testQuery01() throws Exception {
        UriInfo ui = new NormalModeInfo();
        ui.getQueryParameters().add("sortOn", "healthStatus");
        ui.getQueryParameters().add("sortOrder", "asc");
        ui.getQueryParameters().add("limit", "100");
        ui.getQueryParameters().add("skip", "1");
        ui.getQueryParameters().add("hint", "0");

        String queryString = "ServiceInstance";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
    }

    @Test
    public void testQuery02CountOnly() throws Exception {
        UriInfo ui = new NormalModeInfo();

        String queryString = "ServiceInstance";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.COUNT.name(),
                new MockHttpServletRequest()).getEntity();
        assertOk(response);
    }
    
    @Test
    public void testQueryWithReadFilter() throws Exception {
        String queryString = "ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7a\"]";

        // get without read filter
        CMSResponse resp = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, new NormalModeInfo(),
                CMSQueryMode.NORMAL.name(), new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(resp);

        // with filter
        HttpServletRequest request = getReadFilter();
        resp = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(),
                ConsistentPolicy.safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, new NormalModeInfo(),
                CMSQueryMode.NORMAL.name(), request).getEntity();
        assertOkAndNullResult(resp);
    }

    private HttpServletRequest getReadFilter() {
        HttpServletRequest request = new MockHttpServletRequest();
        Map<String, List<SearchCriteria>> additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        MetaField nameField = metaClass.getFieldByName("name");
        ISearchField searchField = new SelectionField(nameField,
                RegistrationUtils.getDefaultDalImplementation(dataSource).searchStrategy);
        // ## see raptorTopology.json for this value
        SearchCriteria criteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.NE, "srp-app:Raptor-00001");
        LogicalSearchCriteria lsc = new LogicalSearchCriteria(LogicOperatorEnum.AND);
        lsc.addChild(criteria);
        
        additionalCriteria.put(SERVICE_INSTANCE, Arrays.<SearchCriteria>asList(lsc));
        request.setAttribute(CMSResourceUtils.REQ_READ_FILTER, additionalCriteria);
        
        return request;
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testQuery03ReverseQuery() throws Exception {
        UriInfo ui = new NormalModeInfo();

        String queryString = "ApplicationService[@name=\"srp-app:Raptor\"]{*}.appService!ServiceInstance[@name=~\"srp-app:Raptor-0000[1-8]\"]{*}";
        CMSResponse response = (CMSResponse)queryResource.query(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
        
        List<JsonNode> results = (List<JsonNode>) response.get("result");
        Assert.assertEquals(1, results.size());
        for (JsonNode appService : results) {
        	JsonNode siEntities = appService.get("appService!ServiceInstance");
        	for (JsonNode si : siEntities) {
        		Iterator<String> fields = si.getFieldNames();
        		int fieldsNo = 0;
        		while (fields.hasNext()) {
        			fields.next();
        			++fieldsNo;
        		}
            	Assert.assertTrue(fieldsNo > 2);
        	}
        }
    }

    @Test
    public void testPostQuery() {
        String queryString = "ApplicationService[@label=\"srp-app\"]{*}.services{*}.runsOn";
        CMSResponse response = (CMSResponse)queryResource.queryEntity(nullMockUri, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, queryString, new MockUriInfo(), CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
    }

    @Test
    public void testQuerySkipLimit() {
        QueryContext qContext = newQueryContext(cmsdbContext);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.add("skip", ",,1,,"); // tailing , is ignored
        params.add("limit", ",0,,3,");
        QueryResource.parseQueryParameters(params, qContext);
        int[] skips = qContext.getSkips();
        int[] limits = qContext.getLimits();
        Assert.assertEquals(5, skips.length);
        Assert.assertEquals(5, limits.length);
        Assert.assertEquals(0, qContext.getSkip(1));
        Assert.assertEquals(1, qContext.getSkip(2));
        Assert.assertEquals(Integer.MAX_VALUE, qContext.getLimit(0));
        Assert.assertEquals(0, qContext.getLimit(1));
        Assert.assertEquals(3, qContext.getLimit(3));
    }

    @Test
    public void testPagination() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("skip", StringUtils.join(new String[] {"0", "7", "0"}, ","));
        qParameter.add("limit", StringUtils.join(new String[] {"0", "2", "0"}, ","));
        qParameter.add(CMSResourceUtils.REQ_PAGINATION_MODE, PaginationMode.SKIP_BASED.name());
        
        CMSResponse response = (CMSResponse)queryResource.queryEntity(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
        System.out.println(response);
        Boolean hasMore = (Boolean)response.get("hasmore");
        Assert.assertNotNull(hasMore);
        Assert.assertTrue(hasMore); // assert has more true
        JsonNode pageNode = (JsonNode) response.get("pagination");
        Assert.assertNotNull(pageNode);
        ArrayNode skipNode = (ArrayNode)pageNode.get("skip");
        Assert.assertNotNull(skipNode);
        JsonNode hintNode = (JsonNode) pageNode.get("hint");
        Assert.assertNotNull(hintNode);
        List<Integer> skips = new ArrayList<Integer>();
        Iterator<JsonNode> it = skipNode.getElements();
        while (it.hasNext()) {
            skips.add(it.next().asInt());
        }
        Assert.assertEquals(3, skips.size());
        List<Integer> limits = new ArrayList<Integer>();
        ArrayNode limitNode = (ArrayNode)pageNode.get("limit");
        Assert.assertNotNull(limitNode);
        Iterator<JsonNode> it2 = limitNode.getElements();
        while (it2.hasNext()) {
            limits.add(it2.next().asInt());
        }
        Assert.assertEquals(3, limits.size());
        
        UriInfo ui2 = new MockUriInfo();
        MultivaluedMap<String, String> qParameter2 = ui2.getQueryParameters();
        qParameter2.add("skip", StringUtils.join(skips.toArray(), ","));
        qParameter2.add("limit", StringUtils.join(limits.toArray(), ","));
        qParameter2.add(CMSResourceUtils.REQ_PAGINATION_MODE, PaginationMode.SKIP_BASED.name());
        response = (CMSResponse)queryResource.queryEntity(ui2, CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy()
                .getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui2, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
        System.out.println(response);
        hasMore = (Boolean)response.get("hasmore");
        Assert.assertNotNull(hasMore);
        Assert.assertFalse(hasMore); // assert has more false
        pageNode = (JsonNode) response.get("pagination");
        Assert.assertNull(pageNode);
    }
    
    @Test
    public void testPagination_singleQuery() {
        String query = "ServiceInstance[@name=~\"srp.*\"]";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("skip", StringUtils.join(new String[] { "7" }));
        qParameter.add("limit", StringUtils.join(new String[] { "2" }));
        qParameter.add("maxFetch", "10000");
        qParameter.add("sortOn", "_oid");
        qParameter.add("sortOrder", "desc");
        qParameter.add("paginationMode", "SKIP_BASED");

        CMSResponse response = (CMSResponse)queryResource.queryEntity(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(response);
        Boolean hasMore = (Boolean) response.get("hasmore");
        Assert.assertNotNull(hasMore);
        Assert.assertTrue(hasMore); // assert has more true
        JsonNode pageNode = (JsonNode) response.get("pagination");
        Assert.assertNotNull(pageNode);
        ArrayNode skipNode = (ArrayNode) pageNode.get("skip");
        Assert.assertNotNull(skipNode);
        JsonNode hintNode = (JsonNode) pageNode.get("hint");
        Assert.assertNotNull(hintNode);
        JsonNode sortOnNode = (JsonNode) pageNode.get("sortOn");
        Assert.assertNotNull(sortOnNode);
        JsonNode sortOrderNode = (JsonNode) pageNode.get("sortOrder");
        Assert.assertNotNull(sortOrderNode);
        JsonNode maxFetchNode = (JsonNode) pageNode.get("maxFetch");
        Assert.assertNotNull(maxFetchNode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAggregate_no_null_value() {
        String query = "NetworkAddress<@_user>{@_user, $count()}";
        UriInfo ui = new MockUriInfo();
        CMSResponse response = (CMSResponse)queryResource.queryEntity(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                .safePolicy().getName(), CMSDB_REPO, BRANCH_MAIN, query, ui, CMSQueryMode.URI.name(),
                new MockHttpServletRequest()).getEntity();
        List<ObjectNode> results = (List<ObjectNode>) response.get(CMSResponse.RESULT_KEY);
        boolean noUser = false;
        boolean realUser = false;
        for (ObjectNode node : results) {
            System.out.println(node.toString());
            if (node.has("_user") ) {
                String user = node.get("_user").asText();
                if ("null".equals(user)) {
                    Assert.fail();
                } else {
                    realUser = true;
                    Assert.assertEquals(1, node.get("$count").asInt());
                }
            } else {
                noUser = true;
                Assert.assertEquals(6, node.get("$count").asInt());
            }
        }
        Assert.assertTrue(noUser);
        Assert.assertTrue(realUser);
    }

    @Test
    public void testQueryNextUrl_single() {
        String query = "ServiceInstance[@name=~\"srp.*\"]";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("skip", StringUtils.join(new String[] { "7" }));
        qParameter.add("limit", StringUtils.join(new String[] { "2" }));
        qParameter.add("maxFetch", "10000");
        qParameter.add("sortOn", "_oid");
        qParameter.add("sortOrder", "desc");
        queryNextUrl(query, ui);
    }

    private void queryNextUrl(String query, UriInfo ui) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        request.setMethod("POST");
        CMSResponse response = (CMSResponse)queryResource.queryEntity(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                request).getEntity();
        assertOkAndNotNullResult(response);
        Boolean hasMore = (Boolean) response.get("hasmore");
        Assert.assertNotNull(hasMore);
        Assert.assertTrue(hasMore); // assert has more true
        JsonNode pageNode = (JsonNode) response.get("pagination");
        Assert.assertNotNull(pageNode);

        JsonNode nextNode = (JsonNode) response.get("next");
        Assert.assertEquals("POST", nextNode.get("method").asText());
        System.out.println(nextNode.get("url").asText());
        Assert.assertNotNull(nextNode.get("body"));
        System.out.println(nextNode.get("body").asText());
        
        // edge case test
        {
            // empty cursor
            qParameter.putSingle("cursor", "");
            response = (CMSResponse)queryResource.queryEntity(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                    .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                    request).getEntity();
            assertOkAndNotNullResult(response);
            hasMore = (Boolean) response.get("hasmore");
            Assert.assertNotNull(hasMore);

            nextNode = (JsonNode) response.get("next");
            Assert.assertEquals("POST", nextNode.get("method").asText());
            System.out.println(nextNode.get("url").asText());
            Assert.assertNotNull(nextNode.get("body"));
            System.out.println(nextNode.get("body").asText());
        }
        // get
        {
            request.setMethod("GET");
            response = (CMSResponse)queryResource.query(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                    .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                    request).getEntity();
            nextNode = (JsonNode) response.get("next");
            Assert.assertEquals("GET", nextNode.get("method").asText());
            System.out.println(nextNode.get("url").asText());
            Assert.assertNull(nextNode.get("body"));
        }
    }

    @Test
    public void testQueryNextUrl_join() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("skip", StringUtils.join(new String[] {"0", "7", "0"}, ","));
        qParameter.add("limit", StringUtils.join(new String[] {"0", "2", "0"}, ","));
        queryNextUrl(query, ui);
    }
    

    @Test
    public void testQueryNextUrl_single_id_based() {
        String query = "ServiceInstance[@name=~\"srp.*\"]";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("paginationMode", "ID_BASED");
        qParameter.add("limit", StringUtils.join(new String[] { "2" }));
        qParameter.add("maxFetch", "10000");
        qParameter.add("sortOn", "_oid");
        qParameter.add("sortOrder", "desc");
        queryNextUrl(query, ui);
    }
    
    @Test
    public void testQueryNextUrl_join_id_based() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("paginationMode", "ID_BASED");
        qParameter.add("limit", StringUtils.join(new String[] {"0", "2", "0"}, ","));
        queryNextUrl(query, ui);
    }
    
    @Test
    public void testQueryNextUrl_join_id_based_get_() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        UriInfo ui = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui.getQueryParameters();
        qParameter.add("paginationMode", "ID_BASED");
        qParameter.add("limit", StringUtils.join(new String[] {"0", "2", "0"}, ","));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        CMSResponse response = (CMSResponse)queryResource.query(ui, CMSPriority.NEUTRAL.toString(), ConsistentPolicy
                .safePolicy().getName(), RAPTOR_PAAS, BRANCH_MAIN, query, ui, CMSQueryMode.NORMAL.name(),
                request).getEntity();
        assertOkAndNotNullResult(response);
        
        // get next url assertion
        Boolean hasMore = (Boolean) response.get("hasmore");
        Assert.assertNotNull(hasMore);
        Assert.assertTrue(hasMore); // assert has more true
        JsonNode pageNode = (JsonNode) response.get("pagination");
        Assert.assertNotNull(pageNode);

        JsonNode nextNode = (JsonNode) response.get("next");
        Assert.assertEquals("GET", nextNode.get("method").asText());
        System.out.println(nextNode.get("url").asText());
        Assert.assertNull(nextNode.get("body"));
    }

}
