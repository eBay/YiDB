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

package com.ebay.cloud.cms.query.optimizer;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.InnerJoinAction;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.parser.QueryParser;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.translator.QueryTranslator;

/**
 * query optimizer test 
 * 
 * @author xjiang
 *
 */
public class QueryOptimizerTest extends MongoBaseTest {

    @Test
    public void testCostAnalysis01() throws Exception {
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);

        String query = "Environment.applications[@label = \"srp-app\"].services[@name = \"srp-app:Raptor-00001\"].runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        optimizer.optimize(plan);
        SearchAction action = (SearchAction) plan.getRootAction();
        action.getSearchQuery().getMetadata().getName().equals("ServiceInstance");       
    }

    private QueryExecPlan parseAndPlan(String query) {
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));
        return plan;
    }

    @Test
    public void testCostAnalysis02() throws Exception {
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);

        String query = "Environment.applications[@label = \"srp-app\"].services[@name = \"srp-app:Raptor-00001\" and @activeManifestDiff = true].runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        optimizer.optimize(plan);
        SearchAction action = (SearchAction) plan.getRootAction();
        action.getSearchQuery().getMetadata().getName().equals("ServiceInstance");       
    }
    
    @Test
    public void testCostAnalysis03() throws Exception {
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);

        String query = "Environment.applications[@label = \"srp-app\"].services[@name = \"srp-app:Raptor-00001\" or @activeManifestDiff = true].runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        optimizer.optimize(plan);
        SearchAction action = (SearchAction) plan.getRootAction();;
        action.getSearchQuery().getMetadata().getName().equals("ApplicationService");       
    }
    
    @Test
    public void testCostAnalysis04() throws Exception {
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);

        String query = "Environment.applications[@label != \"srp-app\"].services[@name != \"srp-app:Raptor-00001\"].runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        optimizer.optimize(plan);
        SearchAction action = (SearchAction) plan.getRootAction();;
        action.getSearchQuery().getMetadata().getName().equals("ApplicationService");       
    }
    
    @Test
    public void testCostAnalysis05() throws Exception {
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);

        String query = "Environment.applications[@label != \"srp-app\"].services[@name in (\"srp-app:Raptor-00001\")].runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        optimizer.optimize(plan);
        SearchAction action = (SearchAction) plan.getRootAction();;
        action.getSearchQuery().getMetadata().getName().equals("ServiceInstance");       
    }

    @Test
    public void testReference() throws Exception {

        String query = "Environment.applications[@label = \"srp-app\"].services.runsOn";
        QueryExecPlan plan = parseAndPlan(query);
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);
        optimizer.optimize(plan);
        Assert.assertNotNull(plan);

   
        AbstractAction action = plan.getRootAction();
        Assert.assertEquals(7, plan.getActionCount());
        
        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
    }

    @Test
    public void testHint() throws Exception {

        String query = "Environment.applications.services.runsOn";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setHint(2);
        QueryTranslator translator = new QueryTranslator(tempContext);

        QueryExecPlan plan = translator.translate(raptorParser.parse(query));
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);
        optimizer.optimize(plan);

        AbstractAction action = plan.getRootAction();
        Assert.assertEquals(7, plan.getActionCount());

        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof InnerJoinAction);
        action = action.getChildrenActions().get(0);
        Assert.assertTrue(action instanceof SearchAction);
    }

    @Test
    public void testJoinAction() {
        String query = "ApplicationService.services.runsOn";
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));
        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);
        optimizer.optimize(plan);

        Assert.assertTrue(plan.getActionCount() == 5);
    }
    
    @Test(expected=QueryOptimizeException.class)
    public void testFullTableScan() {
        cmsdbContext.setAllowFullTableScan(false);
        String query = "Rack[@assets.$_length > 120]{@_oid, @assets.$_length}.assets[@assetStatus = \"IN SERVICE\"].asset!AssetServer{@resourceId,@type}";
        QueryExecPlan plan = getTranslator(cmsdbContext).translate(cmsdbParser.parse(query));
        QueryOptimizer optimizer = new QueryOptimizer(cmsdbContext);
        optimizer.optimize(plan);
    }
    
    @Test
    public void testMetaClassFullTableScan() {
        MetaClass meta = repositoryService.getRepository(cmsdbContext.getRepositoryName()).getMetadataService().getMetaClass("Rack");
        meta.setAllowFullTableScan(true);

        QueryContext context = new QueryContext(cmsdbContext);
        context.setAllowFullTableScan(false);
        context.setRepositoryService(repositoryService);

        QueryParser cmsdbParser = new QueryParser(context);
        
        String query = "Rack";
        QueryExecPlan plan = getTranslator(context).translate(cmsdbParser.parse(query));
        QueryOptimizer optimizer = new QueryOptimizer(context);
        optimizer.optimize(plan);

        meta.setAllowFullTableScan(false);
    }

}
