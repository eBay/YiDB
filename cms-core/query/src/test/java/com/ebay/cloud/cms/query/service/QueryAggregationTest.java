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

package com.ebay.cloud.cms.query.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.metadata.AggregateMetaAttribute;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.util.TestUtils;

/**
 * @author liasu
 * 
 */
public class QueryAggregationTest extends MongoBaseTest {

    @Test
    public void test00SimpleAggr() {
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setExplain(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        }
        
        Assert.assertEquals(1, queryResult.getExplanations().size());
    }

    @Test
    public void test01SimpleAggr() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>[ $max(@port) > \"123\"] { @https, @activeManifestDiff, $max(@port), $min(@port), $sum(@_pversion), $avg(@_version), $count() }";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$min_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$min_port").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$sum__pversion"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$sum__pversion").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$avg__version"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$avg__version").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$count"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$count").isNull());
        }
    }

    @Test
    public void test02PrefixJoinProject() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]{ @https, $max(@port) }";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
        }
    }
    
    @Test
    public void test02PrefixJoinProject_noresult() {
    	QueryContext context = new QueryContext(raptorContext);
        String query = "ApplicationService[@_oid=\"abc\"].services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]{ @https, $max(@port) }";
        IQueryResult queryResult = queryService.query(query, context);

        Assert.assertEquals(0, queryResult.getEntities().size());
    }
    
    /**
     * Projection must be in the group one or not specified(default on the
     * aggregation field)
     */
    @Test
    public void test03PrefixJoinNoProject() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        }
    }

    /**
     * Could only have one group in one query
     */
    @Test
    public void test04MultiAggr() {
        try {
            String query = "ApplicationService<@_oid>.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.MULTI_AGGR_FORBID.getErrorCode(), e.getErrorCode());
        }
    }

    /**
     * Projection must be in the group one or not specified(default on the
     * aggregation field)
     */
    @Test
    public void test05ProjectOnNonAggr() {
        try {
            String query = "ApplicationService{*}.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.PROJECT_NON_AGGR.getErrorCode(), e.getErrorCode());
        }
    }

    /**
     * Project fields must in the group list
     */
    @Test
    public void test06IllegalProject() {
        try {
            String query = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"] {@_oid}";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.IILEGAL_PROJECTION.getErrorCode(), e.getErrorCode());
        }
    }

    /**
     * Aggregation field only referenced in project
     */
    @Test
    public void test07OnlyProjectAggr() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>{$max(@port)}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
    }

    @Test
    public void test08DuplicateProjectAggr() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, $max(@port)}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
    }

    @Test
    public void test08MultipleFunOnSameField() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https>[@https=false]{ @https, $max(@port), $min(@port), $count()}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("8081", ((JsonEntity) result.getEntities().get(0)).getNode().get("$max_port")
                .getTextValue());
        Assert.assertEquals("8080", ((JsonEntity) result.getEntities().get(0)).getNode().get("$min_port")
                .getTextValue());
    }

    @Test
    public void test09CountWithParam() {
        try {
            String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, $max(@port), $count(@https)}";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void test10IllegalSumAvgCount() {
        try {
            String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, @activeManifestDiff, $max(@port), $min(@port), $sum(@port)}";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
            System.out.println(e.getMessage());
        }

        try {
            String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, @activeManifestDiff, $max(@port), $min(@port), $avg(@port)}";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
            System.out.println(e.getMessage());
        }

        try {
            String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, @activeManifestDiff, $max(@port), $min(@port), $count(@port)}";
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void test10SuffixSearch() {
        String query = "ServiceInstance<@https, @activeManifestDiff>[@https=false]{ @https, @activeManifestDiff, $max(@port), $min(@port), $count()}.runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void test10SuffixSearch1() {
        String query = "ServiceInstance<@https>[@https=false]{ @https, $max(@port), $min(@port), $count()}.runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        System.out.println(result.getEntities().get(0));
    }

    @Test
    public void test10SuffixSearch2() {
        String query = "ServiceInstance<@https>[@https=false]{ @https, $max(@port), $min(@port), $count()}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        System.out.println(result.getEntities().get(0));
    }

    @Test
    public void test10FilterOnGroup() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>[@https=false]{ @https, @activeManifestDiff}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        System.out.println(result.getEntities().get(0));
    }
    
    @Test
    public void test10FilterOnCount() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>[$count() < 3]{ @https, @activeManifestDiff, $max(@port)}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        System.out.println(result.getEntities().get(0));
    }

    @Test
    public void test11SuffixSearchNoProjection() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(false);
        String query = "ServiceInstance<@https, @activeManifestDiff>.runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
    }

    @Test
    public void test12MultiSearchNoProjection() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(false);
        String query = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"].runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        Assert.assertNull(((JsonNode) entity.getNode()).get("_oid"));
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
    }

    @Test
    public void test12MultiSearchNoProjection1() {
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setLimits(new int[] { 10 });
        String query = "ServiceInstance[@https=true]<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        System.out.println(result.getEntities().get(0));
        raptorContext.setLimits(null);
    }

    @Test
    public void test13AggrOnList() {
    	raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService<@activeManifestRef>";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestRef"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestRef").isNull());
        }
    }
    
    @Test
    public void test13AggrOnList_1() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@manifestRef>{@manifestRef, $count()}";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(3, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(entity);
            List<?> refs = entity.getFieldValues("manifestRef");
            Assert.assertNotNull(refs);
            Assert.assertEquals(1, refs.size());
            JsonNode node = (JsonNode) entity.getNode();
            if ("manifest-1.0".equals(refs.get(0))) {
                Assert.assertEquals(12, node.get("$count").asInt());
            } else if ("manifest-2.0".equals(refs.get(0))) {
                Assert.assertEquals(11, node.get("$count").asInt());
            } else if ("manifest-3.0".equals(refs.get(0))) {
                Assert.assertEquals(2, node.get("$count").asInt());
            }
        }
    }

    @Test
    public void test14AggrOnReference() {
        try {
            String query = "ApplicationService<@services>";
            queryService.query(query, raptorContext);
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
        }
    }
    
    @Test
    public void test16AggrOnJson() {
        try {
            String query = "ServiceInstance<@properties>";
            queryService.query(query, raptorContext);
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void test15GroupService() {
    	raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@healthStatus>{ @healthStatus, $count() }";
        IQueryResult result = queryService.query(query, raptorContext);
        
        Assert.assertEquals(3, result.getEntities().size());// up, down, known
        for (IEntity entity : result.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("healthStatus"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("healthStatus").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$count"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$count").isNull());
        }
    }

    @Test
    public void testAggregation() {
        Repository repo = repositoryService.getRepository(RAPTOR_REPO);
        MetaClass serviceInstance = repo.getMetadataService().getMetaClass("ServiceInstance");
        MetaField metaField = serviceInstance.getFieldByName("name");
        MetaField metaFieldPort = serviceInstance.getFieldByName("port");

        AggregateMetaAttribute amf1 = new AggregateMetaAttribute(new AggregationField(AggFuncEnum.MIN, new SelectionField(
                metaFieldPort, TestUtils.getDefaultDalImplementation(dataSource).searchStrategy)));
        AggregateMetaAttribute amf2 = new AggregateMetaAttribute(new AggregationField(AggFuncEnum.MAX, new SelectionField(
                metaFieldPort, TestUtils.getDefaultDalImplementation(dataSource).searchStrategy)));

        AggregateMetaAttribute amf3 = new AggregateMetaAttribute(new AggregationField(AggFuncEnum.COUNT, null));
        AggregateMetaAttribute amf4 = new AggregateMetaAttribute(new GroupField(metaField, TestUtils.getDefaultDalImplementation(dataSource).searchStrategy));

        Assert.assertTrue(amf1.equals(amf1));
        Assert.assertFalse(amf1.equals(null));
        Assert.assertFalse(amf1.equals(amf2));
        Assert.assertFalse(amf1.equals(amf3));
        Assert.assertFalse(amf3.equals(amf1));
        Assert.assertFalse(amf3.equals(amf4));
        Assert.assertFalse(amf2.equals(amf4));

        Assert.assertFalse(amf1.equals(new SelectionField(metaFieldPort, TestUtils.getDefaultDalImplementation(dataSource).searchStrategy)));
    }

    @Test
    public void test16PrefixQuery() {
    	raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.services[@https=true]<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        }
    }

    @Test
    public void test17PrefixQueryWithFilter() {
    	raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService[@name=~\"srp-app.*\"].services[@https=true]<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        }
    }
    
    @Test
    public void test18PrefixQueryWithFilter() {
    	QueryContext context = new QueryContext(raptorContext);
    	context.setAllowFullTableScan(true);
        String query = "ApplicationService[@name=~\"srp-app-invalid-name.*\"].services[@https=true]<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, context);

        Assert.assertEquals(0, queryResult.getEntities().size());
    }
    
    @Test
    public void test19QueryWithSortOn01() {
        String query = "ServiceInstance<@healthStatus>{ @healthStatus, $count() }";
        QueryContext qc = newQueryContext(raptorContext.getRepositoryName(), raptorContext.getBranchName());
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        qc.addSortOn("_oid");
        qc.setDbConfig(dbConfig);
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        qc.setLimits(new int[]{1});

        IQueryResult result = queryService.query(query, qc);
        for (IQueryExplanation exp : result.getExplanations()) {
            System.out.println(exp.getJsonExplanation());
        }
        Assert.assertEquals(3, result.getEntities().size());// up, down, known
        for (IEntity entity : result.getEntities()) {
            System.out.println(entity);
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("healthStatus"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("healthStatus").isNull());
            Assert.assertNotNull(((JsonNode) entity.getNode()).get("$count"));
            Assert.assertTrue(!((JsonNode) entity.getNode()).get("$count").isNull());
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test19QueryWithSortOn02() {
        String query = "ServiceInstance<@healthStatus>{ @healthStatus, $count() }";
        QueryContext qc = newQueryContext(raptorContext.getRepositoryName(), raptorContext.getBranchName());
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        qc.addSortOn("healthStatus");
        qc.setDbConfig(dbConfig);
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        qc.setLimits(new int[]{1});

        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(3, result.getEntities().size());// down, unknown, up
        
        IEntity entity = result.getEntities().get(0);
        List<String> values = (List<String>)entity.getFieldValues("healthStatus");
        Assert.assertEquals("down", values.get(0));
        
        entity = result.getEntities().get(1);
        values = (List<String>)entity.getFieldValues("healthStatus");
        Assert.assertEquals("unknown", values.get(0));
        
        entity = result.getEntities().get(2);
        values = (List<String>)entity.getFieldValues("healthStatus");
        Assert.assertEquals("up", values.get(0));
    }

    /**
     * //FIXME
     * Case 0: A.b<>.c
     */
    @Ignore
    @Test
    public void test20AggregationWithEmbed() {
        String query = "Manifest.versions[@_lastmodified > date(110)]<@name>[@name=\"Dummy ManifestVersion Bundle-0-0001\"]{@name, $count()}.approvals";
        IQueryResult queryResult = queryService.query(query, deployContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertEquals("ManifestVersion", entity.getMetaClass().getName());
            // 
        }
    }
    /**
     * //FIXME
     * Case 1: A<>.b.c
     */
    @Ignore
    @Test
    public void test20AggregationWithEmbed_01() {
        String query = "Manifest<@name>.versions[@_lastmodified > date(110) and @name=\"Dummy ManifestVersion Bundle-0-0001\"].approvals";
        IQueryResult queryResult = queryService.query(query, deployContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertEquals("ManifestVersion", entity.getMetaClass().getName());
            // 
        }
    }
    /**
     * //FIXME
     * Case 2: A.b.c<>
     */
    @Test
    @Ignore
    public void test20AggregationWithEmbed_02() {
        String query = "Manifest.versions[@_lastmodified > date(110) and @name=\"Dummy ManifestVersion Bundle-0-0001\"].approvals<@name>{@name, $count()}";
        IQueryResult queryResult = queryService.query(query, deployContext);

        Assert.assertEquals(1, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertEquals("ManifestVersion", entity.getMetaClass().getName());
            // 
        }
    }

    @Test
    public void test21AggregationInternalField() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance[@https=true]<@_oid>[$max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertEquals("ServiceInstance", entity.getMetaClass().getName());
        }
    }
    
    @Test
    public void test22AggregationInternalFieldWithJoin() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.services[@https=true and @port > \"123\"]<@_oid>";
        IQueryResult queryResult = queryService.query(query, raptorContext);

        Assert.assertEquals(2, queryResult.getEntities().size());
        for (IEntity entity : queryResult.getEntities()) {
            Assert.assertNotNull(entity);
            Assert.assertEquals("ServiceInstance", entity.getMetaClass().getName());
            System.out.println(entity.getId());
        }
    }

    @Test
    public void test23PrefixPostfix_join() {
        QueryContext context = new QueryContext(raptorContext);
        String query = "Environment.applications[@archTier=\"app\"].services<@https, @activeManifestDiff>[ $max(@port) > \"123\"].runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("https"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("https").isNull());
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("$max_port"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("$max_port").isNull());
        Assert.assertNotNull(((JsonNode) entity.getNode()).get("activeManifestDiff"));
        Assert.assertTrue(!((JsonNode) entity.getNode()).get("activeManifestDiff").isNull());
    }

    @Test
    public void test23PrefixPostfix_join_noresult() {
    	QueryContext context = new QueryContext(raptorContext);
        String query = "Environment[@name=\"EnvRaptor\"].applications[@archTier=\"app-invalid\"].services<@https, @activeManifestDiff>[ $max(@port) > \"123\"].runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void test23PrefixPostfix_join_noresult2() {
    	QueryContext context = new QueryContext(raptorContext);
        String query = "Environment[@name in (\"EnvRaptor-invalid\")].applications[@archTier=\"app\"].services<@https, @activeManifestDiff>[ $max(@port) > \"123\"].runsOn[@name=\"compute-00010\"]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void test23PrefixPostfix_join_noresult3() {
    	QueryContext context = new QueryContext(raptorContext);
        String query = "Environment[@name in (\"EnvRaptor-invalid\")].applications[@archTier=\"app\"].services.runsOn[@name=\"compute-00010\"]<@name>";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    @Ignore
    @Test
    public void testAggregationOnTypeCastRoot() {
        // TODO
        String query = "<NodeServer, NetworkAddress>Resource<@resourceId>";
        cmsdbContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertNotNull(result);
        System.out.println(result.getEntities());
        for (IEntity entity : result.getEntities()) {
            Assert.assertFalse(entity.getFieldValues("resourceId").isEmpty());
        }
    }

    @Test
    public void testAggregationQuery_noFullTableScan() {
        String query = "Rack[@_oid=\"5199c9450cf2359a4ea29a62\"].assets<@resourceId>.asset!AssetServer";
        cmsdbContext.setAllowFullTableScan(false);
        cmsdbContext.setExplain(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(6, result.getEntities().size());
        List<IQueryExplanation> queryExplanation = result.getExplanations();
        JsonNode explainNode = queryExplanation.get(2).getJsonExplanation();
        Assert.assertEquals("AssetServer", explainNode.get("criteria").get("$and").get(0).get("_t").getTextValue());
        JsonNode refereInNode = explainNode.get("criteria").get("$and").get(2);
        Assert.assertNotNull(refereInNode);
        // FIXME: flatten will have different explanation
        if (cmsdbContext.getRegistration().registrationId.equals("hierarchy")) {
	        String dbValueName = cmsdbMetaService.getMetaClass("AssetServer").getFieldByName("asset").getValueDbName()
	                + "._i";
	        Assert.assertTrue(((ObjectNode) refereInNode).has(dbValueName));
        }
    }

    @Test
    public void testAggregation_EmptyResult() {
        String query = "ServiceInstance[@_oid=\"\"]<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        raptorContext.setAllowFullTableScan(false);
        IQueryResult queryResult = queryService.query(query, raptorContext);
        Assert.assertEquals(0, queryResult.getEntities().size());
    }

    /**
     * Aggregation with has_more would have more test cases.
     * 
     * CMS-4227
     */
    @Test
    public void testAggregation_hasmore() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.getCursor().setLimits(new int[] { 2 });
        String query = "ServiceInstance<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
        IQueryResult queryResult = queryService.query(query, context);
        Assert.assertFalse(queryResult.hasMoreResults());
        Assert.assertEquals(2, queryResult.getEntities().size());
    }
    
    @Test
    public void testAggregation_onLastQuery() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setHint(1);
        String query = "ApplicationService.services.runsOn<@_oid>{@_oid, $count()}";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(10, result.getEntities().size());
    }
    

    @Test
    public void testAggregation_onLastQueryWithSkip() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);

        String query = "ApplicationService.services.runsOn<@assetStatus>{@assetStatus, $count()}";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        
        context.setAllowFullTableScan(true);
        context.setSkips(new int[]{0, 0, 1});
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
    }
    
    @Test
    public void testAggregation_exceedSysLimit() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setSkips(null);
        context.setLimits(new int[] {3});

        Map<String, Object> currConfig = new HashMap<String, Object>(dbConfig.getCurrentConfiguration());
        
        Integer originSysLimit = (Integer) currConfig.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY);
        currConfig.put(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY, Integer.valueOf(3));
        dbConfig.updateConfig(currConfig);

        try {
            String query = "Compute<@_oid>{@_oid, $count()}";
            IQueryResult result = queryService.query(query, context);
            Assert.assertFalse(result.hasMoreResults());
        } finally {
            currConfig.put(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY, originSysLimit);
            dbConfig.updateConfig(currConfig);
        }
    }

    
    @Test
    public void testAggregationAndPagination01() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setHint(0);
        context.setSkips(null);
        context.setLimits(new int[] {2, 2, 2});
        context.setPaginationMode(PaginationMode.SKIP_BASED);
        
        String query = "ApplicationService<@architecture>{@architecture, $count()}.(updateStrategies && services)";
        Map<String, Integer> countMap1 = queryPagination(query, "architecture", context);
        
        context.setHint(1);
        context.setSkips(null);
        context.setLimits(new int[] {2, 2, 2});
        Map<String, Integer> countMap2 = queryPagination(query, "architecture", context);
        
        context.setHint(2);
        context.setSkips(null);
        context.setLimits(new int[] {2, 2, 2});
        Map<String, Integer> countMap3 = queryPagination(query, "architecture", context);
        
        for (String hs : countMap1.keySet()) {
            Assert.assertEquals(countMap1.get(hs).intValue(), countMap2.get(hs).intValue());
            Assert.assertEquals(countMap1.get(hs).intValue(), countMap3.get(hs).intValue());
        }
 
    }
    
    @Test
    public void testAggregationAndPagination02() {
        queryWithHint(0);
        queryWithHint(1);
        queryWithHint(2);
    }
    
    private void queryWithHint(int i) {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setSkips(null);
        context.setHint(i);
        context.setPaginationMode(PaginationMode.SKIP_BASED);
        
        String query = "ApplicationService<@architecture>{@architecture, $count()}.(updateStrategies && services)";
        Map<String, Integer> countMap = queryPagination(query, "architecture", context);
        
        String query1 = "ApplicationService<@architecture>{@architecture, $count()}.(updateStrategies && services[@healthStatus=\"up\"])";
        Map<String, Integer> countMap1 = queryPagination(query1, "architecture", context);
        
        String query2 = "ApplicationService<@architecture>{@architecture, $count()}.(updateStrategies && services[@healthStatus=\"down\"])";
        Map<String, Integer> countMap2 = queryPagination(query2, "architecture", context);
        
        String query3 = "ApplicationService<@architecture>{@architecture, $count()}.(updateStrategies && services[@healthStatus=\"unknown\"])";
        Map<String, Integer> countMap3 = queryPagination(query3, "architecture", context);
        
        for (String hs : countMap.keySet()) {
            int count = 0;
            if (countMap1.containsKey(hs)) {
                count += countMap1.get(hs).intValue();
            }
            if (countMap2.containsKey(hs)) {
                count += countMap2.get(hs).intValue();
            }
            if (countMap3.containsKey(hs)) {
                count += countMap3.get(hs).intValue();
            }
            Assert.assertEquals(countMap.get(hs).intValue(), count);
        }
        
    }

    @Test
    public void testAggregationAndPagination03() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setSkips(null);
        context.setPaginationMode(PaginationMode.SKIP_BASED);

        String query = "ServiceInstance<@healthStatus>{@healthStatus, $count()}.(appService && runsOn)";
        IQueryResult result = queryService.query(query, context);
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for (IEntity entity : result.getEntities()) {
            String hs = (String)entity.getFieldValues("healthStatus").get(0);
            Integer count = (Integer)entity.getFieldValues("$count").get(0);
            countMap.put(hs, count);
        }
        
        context.setSkips(null);
        context.setHint(0);
        context.setLimits(new int[] {2, 2, 2});
        String query1 = "ServiceInstance<@healthStatus>{@healthStatus, $count()}.(appService && runsOn[@assetStatus=\"normal\"])";
        Map<String, Integer> countMap1 = queryPagination(query1, "healthStatus", context);

        context.setSkips(null);
        context.setHint(0);
        context.setLimits(new int[] {2, 2, 2});
        String query2 = "ServiceInstance<@healthStatus>{@healthStatus, $count()}.(appService && runsOn[@assetStatus=\"maintenance\"])";
        Map<String, Integer> countMap2 = queryPagination(query2, "healthStatus", context);

        context.setSkips(null);
        context.setHint(0);
        context.setLimits(new int[] {2, 2, 2});
        String query3 = "ServiceInstance<@healthStatus>{@healthStatus, $count()}.(appService && runsOn[@assetStatus=\"deprecated\"])";
        Map<String, Integer> countMap3 = queryPagination(query3, "healthStatus", context);

        for (String hs : countMap.keySet()) {
            int count = 0;
            if (countMap1.containsKey(hs)) {
                count += countMap1.get(hs).intValue();
            }
            if (countMap2.containsKey(hs)) {
                count += countMap2.get(hs).intValue();
            }
            if (countMap3.containsKey(hs)) {
                count += countMap3.get(hs).intValue();
            }
            Assert.assertEquals(countMap.get(hs).intValue(), count);
        }
    }
    
    @Test
    public void testAggrWithCountMode() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.setSkips(null);
        context.setCountOnly(true);
        
    	try {
    		String query = "ServiceInstance<@https, @activeManifestDiff>[ $max(@port) > \"123\"]";
    		queryService.query(query, context);
    		Assert.fail();
    	} catch (QueryParseException e) {
    		Assert.assertEquals(QueryErrCodeEnum.AGG_COUNT_NOT_SUPPORT.getErrorCode(), e.getErrorCode());
    	}
    }
    
    private Map<String, Integer> queryPagination(String query, String key, QueryContext context) {
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        IQueryResult result = queryService.query(query, context);
        while (result.hasMoreResults()) {
            for (IEntity entity : result.getEntities()) {
                String hs = (String)entity.getFieldValues(key).get(0);
                Integer newCount = (Integer)entity.getFieldValues("$count").get(0);
                
                Integer count = countMap.get(hs);
                if (count != null) {
                    newCount = Integer.valueOf(count.intValue() + newCount.intValue());
                }
                countMap.put(hs, newCount);
            }
            
            context.setSkips(result.getNextCursor().getSkips());
            context.setLimits(result.getNextCursor().getLimits());
            result = queryService.query(query, context);
        }
        
        return countMap;
    }

    @Test
    public void test25Aggregate_no_null_value01() {
        QueryContext context = new QueryContext(cmsdbContext);
        context.setAllowFullTableScan(true);
        context.setSkips(null);
        String query = "NetworkAddress<@_user>{@_user, $count()}";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(false, result.hasMoreResults());
        Assert.assertEquals(2, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            System.out.println(entity.toString());
            if (!entity.hasField("_user")) {
                Assert.assertEquals(6, entity.getFieldValues("$count").get(0));
            } else {
                String user = (String) entity.getFieldValues("_user").get(0);
                Assert.assertNotNull(user);
                Assert.assertEquals(1, entity.getFieldValues("$count").get(0));
            }
        }
    }

}
