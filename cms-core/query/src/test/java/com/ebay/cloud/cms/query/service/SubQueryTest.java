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

package com.ebay.cloud.cms.query.service;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.QueryExecuteException;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.util.TestUtils;

public class SubQueryTest extends MongoBaseTest {

    @Test
    public void testSub01Nested() {
        String sub = "ApplicationService[@name =~\"srp.*\" " +
                		"and " +
                		"(@services =& ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" " +
                		                                "and " +
                		                                "@runsOn =& Compute[@name=\"compute-00002\"]]" +
                		") " +
                	  "]";

        IQueryResult result = queryService.query(sub, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 1);
    }
    
    @Test
    public void testSub01NestedNot() {
        String sub = "ApplicationService[@name =~\"srp.*\" " +
                        "and not " +
                        " @services =& ServiceInstance" +
                      "]";

        IQueryResult result = queryService.query(sub, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testSub02Multi() {
        String sub = "ApplicationService[@name =~\"srp.*\"" +
        		" and " +
            		"(@services =& ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" " +
            		"                                 and @runsOn =& Compute[@name=\"compute-00002\"]])" +
        		" and " +
            		"@updateStrategies =& UpdateStrategy[@name=\"1-100\"]" +
        		"]";

        IQueryResult result = queryService.query(sub, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 1);
    }
    
    @Test
    public void testSub02MultiNot() {
        String sub = "ApplicationService[@name =~\"srp.*\"" +
                " and " +
                    "(@services =& ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" " +
                    "                                 and @runsOn =& Compute[@name=\"compute-00002\"]])" +
                " and " +
                    "@updateStrategies =& UpdateStrategy[@name=\"1-30-100\"]" + // incorrect update strategy name
                "]";

        IQueryResult result = queryService.query(sub, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 0);
    }

    @Test
    public void testSub03() {
        String sub = "ServiceInstance[@name=~\"srp-app.*\" and @runsOn =& Compute[@name=~\"compute-0000.*\"]]";

        IQueryResult result = queryService.query(sub, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(8, result.getEntities().size());

        // the service instance that have no compute attached
        String notIn1 = "4fbb314fc681caf13e283a7a";
        // the service instance that have compute name as compute-00010 (not match the regex).
        String notIn2 = "4fbb314fc681caf13e283a8c";
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ServiceInstance", entity.getType());
            Assert.assertFalse(entity.getId().equals(notIn1));
            Assert.assertFalse(entity.getId().equals(notIn2));
        }
    }
    
    /**
     * CMS-2935
     */
    @Test
    public void testSub_CMS2935() {
        String query = "ServiceInstance[@runsOn =& ServiceInstance[@name=~\"srp-app.*\"].runsOn[@name=\"compute-00002\"]{@_oid}]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ServiceInstance", entity.getType());
        }
    }
    
    @Test
    public void testSub_CMS2935_02() {
        String query = "Environment[@applications =& ServiceInstance.(appService{*} && runsOn[@_oid=\"4fbb314fc681caf13e283a7b\"])]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 1);

        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("Environment", entity.getType());
        }
    }
    
    /**
     * CMS-2935
     */
    @Test
    public void testSub_CMS2935_03() {
        String query = "ServiceInstance[@_oid =& ServiceInstance[@name=~\"srp-app.*\"]{@_oid}.runsOn[@name=\"compute-00002\"]]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ServiceInstance", entity.getType());
        }
    }
    

    @Test
    public void testSub04WithSetUnion() {
        String query = "Environment[@applications =& ApplicationService{*}.(services[@name=~\"srp-app.*\"] || updateStrategies[@name=\"1-100\"])]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 1);

        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("Environment", entity.getType());
        }
    }

    @Test
    public void testSub04WithSetUnionNot() {
        String query = "Environment[not @applications =& ApplicationService{*}.(services[@name=~\"srp-app.*\"] || updateStrategies[@name=\"1-100\"])]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 0);
    }
    
    @Test
    public void testSub04WithSetUnion2() {
        // query string that has update strategy with non-existing name
        String query = "Environment[@applications =& ApplicationService{*}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-30-100\"])]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 0);
    }

    @Test
    public void testSub04_CMS3477_1() {
        String query = "ApplicationService[@services =& ApplicationService[@name=~\"^srp-app:Raptor.*\"/s].(services[@_oid=~\"XXXXXX\"]{@_oid} || updateStrategies[@name=~\"1-.*\"])]{*}";
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testSub04_CMS3477_2() {
        String query = "ApplicationService[@services =& ServiceInstance{@_oid}.(services!ApplicationService[@name=~\"^srp-app:Raptor.*\"/s] &&(appService[@name=\"XXXXXX\"] || runsOn[@name=\"compute-00002\"])) and not exists @updateStrategies]{*}";
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testSub05NonRef() {
        // sub query on non-
        String query = "Environment[@_status =& ApplicationService{@_status}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]";
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 1);
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("Environment", entity.getType());
        }
    }

    @Test
    public void testSub05NonRefOid() {
        String query = "Environment[@_status =& ApplicationService{@_oid}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]";
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 0);
    }

    @Test
    public void testSub05NonRefType() {
        String query = "Environment[@_status =& ApplicationService{@_type}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]";
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertTrue(result.getEntities().size() == 0);
    }

    @Test
    public void testSub05NonRef3_TooMuchProjection() {
        // sub query on non-
        String query = "Environment[@_status =& ApplicationService{*}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]";
        try {
            raptorContext.setAllowFullTableScan(true);
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryExecuteException qee) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.IILEGAL_PROJECTION.getErrorCode(), qee.getErrorCode());
        }
    }

    @Test(expected=QueryParseException.class)
    public void testSub05NonRef3_TooMuchProjection1() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment[@_status =& ApplicationService{@_status}.services{*}]";
        queryService.query(query, raptorContext);
    }

    @Test(expected=QueryParseException.class)
    public void testSub05NonRef3_TooMuchProjection2() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment[@_status =& ApplicationService{@_status}+.services{@_status}]";
        queryService.query(query, raptorContext);
    }

    /**
     * When projection is a reference.
     */
    @Test
    public void testSub_CMS3430() {
        String query = "Environment.applications[@services=& ApplicationService{@services}]";
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * Hint issue
     */
    @Test
    public void testSub_CMS3430_2() {
        String query = "Asset[@_oid =& Rack{@assets}]";
        cmsdbContext.setAllowFullTableScan(true);
        cmsdbContext.setHint(1);// ## hint handling
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(7, result.getEntities().size());
    }

    @Test
    public void testSub_CMS3403_3() {
        String query = "Environment.applications[@services=& ApplicationService{@services}]";
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setHint(1);// ## sub query hint handling
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testSub05NonRef_fieldProperty() {
        MetaClass metaClass = raptorMetaService.getMetaClass("ApplicationService");
        IEntity newApp = new JsonEntity(metaClass);
        newApp.setBranchId(IBranch.DEFAULT_BRANCH);
        newApp.addFieldValue("name", generateRandomName("cms3430-test"));
        
        EntityContext context = newEntityContext();
        String id = entityService.create(newApp, context);
        newApp.setId(id);
        
        String query = "Environment.applications[@services.$_length =& ApplicationService{@services.$_length}]";
        raptorContext.setHint(-1);
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        entityService.delete(newApp, context);
    }

    protected static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        context.setModifier("unitTestUser");
        context.setDbConfig(dbConfig);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        return context;
    }

    @Test
    public void testSub05NonRef_aggregationFunc() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment.applications[@services.$_length =& ApplicationService<@_type>{$count()}]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testSub05NonRef_aggregationField() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        context.clearMetadataServices();
        String query = "Environment.applications[@_type =& ApplicationService<@_type>{$max(@_type)}]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testSub05NonRef_aggregationField_01() {
        QueryContext context = new QueryContext(raptorContext);
        context.clearMetadataServices();
        context.setAllowFullTableScan(true);
        String query = "ServiceInstance[@port =& ServiceInstance<@healthStatus>[@healthStatus=\"unknown\"]{$max(@port)}]";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testSubQuery_selfAggregate_groupField() {
        String query = "Vlan[@resourceId =& Vlan<@resourceId>{@resourceId}]";
        QueryContext context = new QueryContext(cmsdbContext);
        context.clearMetadataServices();
        context.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testSub06Projection_endReference() {
        // sub query on non-
        String query = "Environment[@_status =& ApplicationService{*}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryExecuteException qee) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.IILEGAL_PROJECTION.getErrorCode(), qee.getErrorCode());
        }
    }

    @Test
    public void testSubQuery_oid() {
        String query = "Environment[@_oid =& Environment[@_status =& ApplicationService{@_status}.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100\"])]{@_oid} ]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertTrue(result.getEntities().size() == 1);
    }

    @Test
    public void testSubQuery_danglingCheck() {
        String query = "ApplicationService[not @services =& ServiceInstance{@_oid}]{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testSubQuery_danglingCheck02() {
        String query = "ServiceInstance[ exists @runsOn and not @runsOn =& Compute{@_oid} ]{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testSubQuery_danglingCheck03() {
        String query = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
    }

    @Test
    public void testSubQuery_maxFetch() {
        String query = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        QueryContext qc = newQueryContext(raptorContext);
        qc.setMaxFetch(1);
        try {
            queryService.query(query, qc);
            Assert.fail();
        } catch (QueryExecuteException qe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.INCOMPLETE_JOIN_QUERY.getErrorCode(), qe.getErrorCode());
        }
    }

    @Test
    public void testSubQuery_countonly() {
        String query = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        QueryContext qc = newQueryContext(raptorContext);
        qc.setCountOnly(true);
        qc.setMaxFetch(100);
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(2, result.getCount());
    }
    
    @Test
    public void testSubQuery_countonlyMoreSub() {
        String query = "ServiceInstance[ not @runsOn =& Compute{@_oid}.runsOn!ServiceInstance ]{*}";
        QueryContext qc = newQueryContext(raptorContext);
        qc.setCountOnly(true);
        qc.setMaxFetch(100);
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(2, result.getCount());
    }

    @Test
    public void testSubQuery_embed() {
        String query = "Dep.team[@_oid =& Dep[@_oid=~\"dep00[01]\"].team{@_oid}.person[@_oid=\"Dep!dep000!team!team010!person!person012\"] and @label=\"stratus\"]{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    // TODO
    // Sub query tests with aggregation
    // 1. group as target field
    // 2. group as projected field
    // 3. aggregation as target field
    // 4. aggregation as projected field
    
    // TODO
    // sub query with set query projection
    // 

}
