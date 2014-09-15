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

package com.ebay.cloud.cms.query.translator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.EmbedActionContainer;
import com.ebay.cloud.cms.query.executor.InnerJoinAction;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.parser.QueryParser;

/**
 * User: Rene Xu
 * Email: rene.xu@ebay.com
 * Date: 5/14/12 5:19 PM
 */
public class QueryTranslatorTest extends MongoBaseTest {
    
    @Test
    public void testTranslator() throws Exception {
        String query = "Environment.applications";
        QueryExecPlan plan = parseAndPlan(getTranslator(raptorContext), raptorParser, query);
        Assert.assertNotNull(plan);
        System.out.println(plan);
        SearchAction action = (SearchAction)plan.getRootAction();
        Assert.assertNotNull(action);
        Assert.assertNotNull(action.getParseNode());
        Assert.assertEquals("Environment", action.getParseNode().getMetaClass().getName());


        InnerJoinAction joinAction = (InnerJoinAction)action.getChildrenActions().get(0);
        Assert.assertNotNull(joinAction);
        
        action = (SearchAction)joinAction.getChildrenActions().get(0);
        Assert.assertEquals("ApplicationService", action.getParseNode().getMetaClass().getName());
        Assert.assertEquals("applications", action.getParseNode().getMetaReference().getName());
        Assert.assertTrue(action.getChildrenActions().isEmpty());
    }

    private QueryExecPlan parseAndPlan(QueryTranslator translator, QueryParser parser, String query) {
        QueryExecPlan plan = translator.translate(parser.parse(query));
        return plan;
    }

    @Test
    public void testEmbedded() throws Exception {
        String query = "Manifest.versions";
        QueryExecPlan plan = parseAndPlan(getTranslator(deployContext), deployParser, query);
        Assert.assertNotNull(plan);
        System.out.println(plan);
        
        AbstractAction action1 = plan.getRootAction();
        Assert.assertTrue(action1 instanceof SearchAction);
        
        AbstractAction action2 = action1.getChildrenActions().get(0);
        Assert.assertTrue(action2 instanceof InnerJoinAction);
        
        SearchAction action3 = (SearchAction)action2.getChildrenActions().get(0);
        Assert.assertEquals("versions", action3.getParseNode().getMetaReference().getName());
        Assert.assertTrue(action3.isEmbedSearch());
        
        EmbedActionContainer embedContainer = ((SearchAction)action1).getEmbedActionContainer();
        Set<AbstractAction> containedActionSet = embedContainer.getContainedActions();
        Assert.assertTrue(containedActionSet.contains(action1));
        Assert.assertTrue(containedActionSet.contains(action2));
        Assert.assertTrue(containedActionSet.contains(action3));
    }

    @Test
    public void testEmbedded2() throws Exception {
        String query = "Manifest.versions.approvals";
        deployContext.setExplain(true);
        QueryExecPlan plan = parseAndPlan(getTranslator(deployContext), deployParser, query);
        
        Assert.assertNotNull(plan);
        
        AbstractAction action1 = plan.getRootAction();
        Assert.assertTrue(action1 instanceof SearchAction);
        
        AbstractAction action2 = action1.getChildrenActions().get(0);
        Assert.assertTrue(action2 instanceof InnerJoinAction);
        
        SearchAction action3 = (SearchAction)action2.getChildrenActions().get(0);
        Assert.assertTrue(action3.isEmbedSearch());
        Assert.assertEquals("versions", action3.getParseNode().getMetaReference().getName());
        
        AbstractAction action4 = action3.getChildrenActions().get(0);
        Assert.assertTrue(action4 instanceof InnerJoinAction);
        
        SearchAction action5 = (SearchAction)action4.getChildrenActions().get(0);
        Assert.assertTrue(action5.isEmbedSearch());
        Assert.assertEquals("approvals", action5.getParseNode().getMetaReference().getName());
        
        Assert.assertEquals(0, action5.getChildrenActions().size());        
    }
    
    @Test
    public void testEmbedded3() throws Exception {
        String query = "Manifest.versions.packages";
        deployContext.setExplain(true);
        QueryExecPlan plan = parseAndPlan(getTranslator(deployContext), deployParser, query);
        
        Assert.assertNotNull(plan);
        
        AbstractAction action1 = plan.getRootAction();
        Assert.assertTrue(action1 instanceof SearchAction);
        
        AbstractAction action2 = action1.getChildrenActions().get(0);
        Assert.assertTrue(action2 instanceof InnerJoinAction);
        
        SearchAction action3 = (SearchAction)action2.getChildrenActions().get(0);
        Assert.assertTrue(action3.isEmbedSearch());
        Assert.assertEquals("versions", action3.getParseNode().getMetaReference().getName());
        
        AbstractAction action4 = action3.getChildrenActions().get(0);
        Assert.assertTrue(action4 instanceof InnerJoinAction);
        
        SearchAction action5 = (SearchAction)action4.getChildrenActions().get(0);        
        Assert.assertEquals("packages", action5.getParseNode().getMetaReference().getName());
        Assert.assertEquals(0, action5.getChildrenActions().size());
    }

    @Test
    public void testFailure() throws Exception {
        String query = null;
        try {
            query = "A.b";
            getTranslator(deployContext).translate(deployParser.parse(query));
            Assert.assertFalse(true);
        } catch (Exception qte) {

        }

        try {
            query = "Manifest.ddd";
            getTranslator(deployContext).translate(deployParser.parse(query));
            Assert.assertFalse(true);
        } catch (Exception qte) {

        }
    }

    @Test
    public void testTypeCastValidation() {
        try {
            String query = "<ServiceInstance, ApplicationService>Compute";
            parseAndPlan(getTranslator(raptorContext), raptorParser, query);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.TYPE_CAST_NOT_SUBMETA.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void testSetQueryPlanUnion() {
        String query = "ApplicationService.(services[@name=~\"srp-app.*\"] || updateStrategies{*})";
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));
        // TODO : assert the structure
    }
    
    @Test
    public void testSubQuery() {
        String query = "ApplicationService[@name =~\"srp.*\" " +
                "and " +
                "(@services =& ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" " +
                                                "and " +
                                                "@runsOn =& Compute[@name=\"compute-00002\"]]" +
                ") " +
              "]";
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));

        // assert sub query
        Assert.assertFalse(plan.getSubPlans().isEmpty());
        Assert.assertEquals(1, plan.getSubPlans().size());
        // assert nested sub query
        Assert.assertFalse(plan.getSubPlans().isEmpty());
        Assert.assertEquals(1, plan.getSubPlans().size());
    }
    
    @Test
    public void testSubQueryProjectValidation() {
        String query = "ApplicationService[@name =~\"srp.*\" " +
                "and " +
                "(@services =& ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" " +
                                                "and " +
                                                "@runsOn =& Compute[@name=\"compute-00002\"]]{@_oid, @name}" +
                ") " +
              "]";
        try {
            getTranslator(raptorContext).translate(raptorParser.parse(query));
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.IILEGAL_PROJECTION.getErrorCode(), e.getErrorCode());
        }
    }
    
//    @Test
//    public void testNestedRefQuery01(){
//        String query = "Group{*}.customer{*}";
//        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));
//        // TODO : assert structure
//    }
//    
//    @Test
//    public void testNestedRefQuery02(){
//        String query = "Group{*}.customer[@_oid=\"Dep!dep000!team!team020!person!person022\"]{*}";
//        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));
//        // TODO : assert structure
//    }

    @Test
    public void testReverseQuery() {
        String queryStr = "Environment{*}.applications{*}.appService!ServiceInstance[@name=~\"srp-app:Raptor.*\"].runsOn[exists @name]";
        
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(queryStr));
        // TODO : assert structure
    }

    @Test(expected = QueryOptimizeException.class)
    public void testTranslateExceedMaxJoins() {
    	Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
    	Map<String, Object> clonedCurrentConfig = new HashMap<String, Object>(currentConfig);
    	Map<String, Object> modConfig = new HashMap<String, Object>();
        modConfig.put(CMSDBConfig.SYS_LIMIT_JOINED_COLLECTIONS, 5);
        dbConfig.updateConfig(modConfig);

        String query = "ResourceGroup.environment.environment!ApplicationService.serviceInstances.runsOn.nodeServer.ntpServers.agents.networkAddress";
        try{
        	parseAndPlan(getTranslator(cmsdbContext), cmsdbParser, query);
        	Assert.fail();
        }catch(IllegalArgumentException r) {
        	throw r;
        }finally {
            dbConfig.updateConfig(clonedCurrentConfig);
        }
        
    }
}