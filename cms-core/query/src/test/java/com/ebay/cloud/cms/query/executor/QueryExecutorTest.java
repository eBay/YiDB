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

package com.ebay.cloud.cms.query.executor;



import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizer;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.impl.QueryServiceImpl;

/**
 * query executor test
 * 
 * @author xjiang
 *
 */
public class QueryExecutorTest extends MongoBaseTest {

    @Test
    public void test01() {       
        String query = "Environment{*}.applications[@label = \"srp-app\"]{*}.services[@name = \"srp-app:Raptor-00004\"]{*}.runsOn{*}";
        QueryExecPlan plan = getTranslator(raptorContext).translate(raptorParser.parse(query));

        QueryOptimizer optimizer = new QueryOptimizer(raptorContext);
        optimizer.optimize(plan);
        
        QueryExecutor executor = new QueryExecutor(raptorContext);
        executor.execute(plan);
        QueryServiceImpl.populateResult(plan, raptorContext);

        IQueryResult result = plan.getQueryResult();
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void test02() {
        String query = "Manifest{*}.versions[@name=\"Dummy ManifestVersion Bundle-0-0001\"]{*}.packages{*}.versions{*}";
        QueryExecPlan plan = getTranslator(deployContext).translate(deployParser.parse(query));
        
        QueryOptimizer optimizer = new QueryOptimizer(deployContext);
        optimizer.optimize(plan);
        
        QueryExecutor executor = new QueryExecutor(deployContext);
        executor.execute(plan);
        QueryServiceImpl.populateResult(plan, deployContext);
        
        IQueryResult result = plan.getQueryResult();
        Assert.assertEquals(1, result.getEntities().size());
    }
        
}
