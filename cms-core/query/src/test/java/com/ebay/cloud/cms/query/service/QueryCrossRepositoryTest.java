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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.parser.QueryParseException;

public class QueryCrossRepositoryTest extends MongoBaseTest {


    @Test
    @SuppressWarnings("unchecked")
    public void testCrossRepo00() {
        QueryContext qContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        qContext.setDbConfig(dbConfig);
        String query0 = "RefApplicationService{*}.services[@name=~\"srp-app:Raptor*\"]";
        IQueryResult result0 = queryService.query(query0, qContext);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(1, result0.getEntities().size());
        for (IEntity entity : result0.getEntities()){
            Assert.assertEquals(DEPLOY_REPO, entity.getRepositoryName());
            for (IEntity serviceEntity : (List<IEntity>)entity.getFieldValues("services")) {
                Assert.assertEquals(RAPTOR_REPO, serviceEntity.getRepositoryName());
            }
        }

        String query1= "RefApplicationService.services[@name=~\"srp-app:Raptor*\"]{*}";
        IQueryResult result1 = queryService.query(query1, qContext);
        Assert.assertNotNull(result1.getEntities());
        Assert.assertEquals(3, result1.getEntities().size());
        for (IEntity entity : result1.getEntities()) {
            Assert.assertEquals(RAPTOR_REPO, entity.getRepositoryName());
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testCrossRepo01ProjectRefRepoOnly() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setDbConfig(dbConfig);
        String query0 = "RefApplicationService{*}.raptorApplicationService[@name=~\"^srp-app:Raptor*\"/s].services{*}";
        IQueryResult result0 = queryService.query(query0, qContext);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(1, result0.getEntities().size());
        for (IEntity entity : result0.getEntities()) {
            Assert.assertEquals(STRATUS_REPO, entity.getRepositoryName());
            for (IEntity serviceEntity : (List<IEntity>) entity.getFieldValues("raptorApplicationService")) {
                Assert.assertEquals(RAPTOR_REPO, serviceEntity.getRepositoryName());
            }
        }
    }

    @Test
    public void testCrossRepo02ProjectReferingRepo() {
    	QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        String query0 = "RefApplicationService.sdRefApplicationService[@name=~\"SoftwareDeployment-ApplicationService001\"]";
        IQueryResult result0 = queryService.query(query0, qContext);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(1, result0.getEntities().size());
        for (IEntity entity : result0.getEntities()) {
            Assert.assertEquals(DEPLOY_REPO, entity.getRepositoryName());
        }
    }

    @Test
    public void testCrossRepo03MultiRepo() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        String query0 = "RefApplicationService.sdRefApplicationService[@name=~\"SoftwareDeployment-ApplicationService001\"].services[@name=~\"srp-app:Raptor*\"]{*}";
        IQueryResult result0 = queryService.query(query0, qContext);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(3, result0.getEntities().size());
        for (IEntity entity : result0.getEntities()) {
            Assert.assertEquals(RAPTOR_REPO, entity.getRepositoryName());
        }
    }
    
    @Test
    public void testCrossRepo04TypeCast() {
        QueryContext qContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        String query0 = "PoolCluster[@name=~\"SoftwareDeployment-PoolCluster\"]{*}.<VPool, VCluster>pools[@healthState=\"UNKNOWN\"]{@_type, @label}";
        IQueryResult result0 = queryService.query(query0, qContext);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(2, result0.getEntities().size());
        for (IEntity entity : result0.getEntities()) {
            Assert.assertEquals(DEPLOY_REPO, entity.getRepositoryName());
        }
    }

    @Test
    public void tsetCrossRepo05Typecast() {
        QueryContext qContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        String query1 = "PoolCluster[@name=~\"SoftwareDeployment-PoolCluster\"].<VPool, VCluster>pools[@healthState=\"UNKNOWN\"]{@_type, @label}";
        IQueryResult result1 = queryService.query(query1, qContext);
        Assert.assertNotNull(result1.getEntities());
        Assert.assertEquals(8, result1.getEntities().size());
        for (IEntity entity : result1.getEntities()) {
            Assert.assertEquals(STRATUS_REPO, entity.getRepositoryName());
        }
    }

    // case 2 : aggregation on type cast FIXME: CMS doesn't merge the aggregation result in current implementation
    @Test
    public void testCrossRepo06TypeCastAggregation() {
        QueryContext qContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        String query2 = "PoolCluster[@name=~\"SoftwareDeployment-PoolCluster\"].<VPool, VCluster>pools<@healthState>[@healthState=\"UNKNOWN\"]{ @healthState }";
        IQueryResult result2 = queryService.query(query2, qContext);
        Assert.assertNotNull(result2.getEntities());
        Assert.assertEquals(2, result2.getEntities().size());
        for (IEntity entity : result2.getEntities()) {
            Assert.assertEquals(STRATUS_REPO, entity.getRepositoryName());
            // TODO: following assertion should be compleletement of QueryAggregationTest.testAggregationOnTypeCastRoot()
//            Assert.assertTrue(entity.hasField("healthState"));
            System.out.println(entity);
        }
    }

    /**
     * A.b.c.d query - A/B in software-deployment, while c.d in stratus-ci.
     */
    @Test
    public void testCrossRepo07ABCD() {
        QueryContext qContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        String query = "ServiceCluster.poolClusters[@_oid=\"4fbd4ec123456123456ddd\"].pools[@_oid=\"CLgo6gjcth\"].computes";
        IQueryResult result = queryService.query(query, qContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals(STRATUS_REPO, entity.getRepositoryName());
            System.out.println(entity);
        }
    }
    
    /**
     * testCrossRepositoryRightAfterReverse - This is not supportted.
     * 
     */
    @Test
    public void testCrossRepositoryRightAfterReverse() {
        // case 1 Normal - cross-repo reference -> Meta <-reverse - Meta
        {
            String query = "PoolCluster[@_oid=\"4fbd4ec123456123456ddd\"].pools[@_oid=\"CLgo6gjcth\"].parentCluster!Compute";
            QueryContext qc = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
            IQueryResult result = queryService.query(query, qc);
            Assert.assertEquals(0, result.getEntities().size());
        }

        // case 2 : 
        {
            String query = "PoolCluster[@_oid=\"4fbd4ec123456123456ddd\"].pools[@_oid=\"CLgo6gjcth\"].computes.computes!VPool";
            QueryContext qc = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
            IQueryResult result = queryService.query(query, qc);
            Assert.assertEquals(1, result.getEntities().size());
        }

        // case 3 : use cross-repository as reverse reference is not supportted
        // In this case, stratus-ci also has an PoolCluster. So the reversed cluster is wrong.
        {
            try {
                String query = "VCluster[@_oid=\"CLgo6gjcth\"].pools!PoolCluster[@_oid=\"4fbd4ec123456123456ddd\"]";
                QueryContext qc = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
                queryService.query(query, qc);
                Assert.fail();
            } catch (QueryParseException qpe) {
                Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), qpe.getErrorCode());
            }
        }
    }

}
