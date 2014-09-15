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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.metadata.QueryMetaClass;
import com.ebay.cloud.cms.query.parser.QueryParseException;

/**
 * @author liasu
 *
 */
public class ReverseQueryTest extends MongoBaseTest {

    /**
     * A.c!C
     */
    @Test
    public void testReverse01() {
    	raptorContext.setAllowFullTableScan(true);
        String queryStr = "ApplicationService.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"]{@_oid}";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(10, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ServiceInstance", entity.getType());
        }
    }

    /**
     * A.b.c!C
     */
    @Test
    public void testReverse02() {
    	raptorContext.setAllowFullTableScan(true);
        String queryStr = "Environment.applications{*}.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"]";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ApplicationService", entity.getType());
            // not only _oid and _type
            Assert.assertTrue(entity.getFieldNames().size() > 2);
        }
    }

    /**
     * A.b.( c!C && d)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testReverse03() {
        String queryStr = "Environment{*}.applications{*}." +
        		"( appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"].runsOn[exists @name] " +
        		" && updateStrategies[@name=\"1-100\"])";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("Environment", entity.getType());

            List<IEntity> apps = (List<IEntity>) entity.getFieldValues("applications");
            Assert.assertNotNull(apps);
            Assert.assertFalse(apps.isEmpty());
            Assert.assertTrue(apps.size() == 1);
            for (IEntity app : apps) {
                Assert.assertEquals("ApplicationService", app.getType());
                // not only _oid and _type
                Assert.assertTrue(app.getFieldNames().size() > 2);
            }
        }
    }
    
    /**
     * A.b.c!C.d.e.f
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testReverse04() {
        String queryStr = "Environment{*}.applications{*}.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"].runsOn[exists @name]";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("Environment", entity.getType());
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            
            List<IEntity> apps = (List<IEntity>) entity.getFieldValues("applications");
            Assert.assertNotNull(apps);
            Assert.assertFalse(apps.isEmpty());
            Assert.assertTrue(apps.size() == 1);
            for (IEntity app : apps) {
                Assert.assertEquals("ApplicationService", app.getType());
                // not only _oid and _type
                Assert.assertTrue(app.getFieldNames().size() > 2);
            }
        }
    }
    
    /**
     * A.b.c!C.d!D
     */
    @Test
    public void testReverse05() {
        QueryContext context = new QueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        String queryStr = "Environment.applications.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"].services!ApplicationService{*}";
        IQueryResult result = queryService.query(queryStr, context);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(10, result.getEntities().size());
        QueryMetaClass siQmc = createServiceInstaceQueryMeta();
        QueryMetaClass appServiceQmc = createAppServiceQueryMeta(siQmc);
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ApplicationService", entity.getType());
            // not only _oid and _type
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            Assert.assertTrue(((String)entity.getFieldValues("name").get(0)).contains("srp-app:Raptor"));
            JsonEntity jsonApp = new JsonEntity(appServiceQmc, ((JsonEntity)entity).getNode());
            Assert.assertTrue(jsonApp.getFieldValues("appService!ServiceInstance").isEmpty());
        }
    }

    @Test
    public void testReverse06() {
        raptorContext.setAllowFullTableScan(true);
        String queryStr = "Environment.applications{*}.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"]{*}";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());

        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("ApplicationService", entity.getType());
            // not only _oid and _type
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            @SuppressWarnings("unchecked")
            List<IEntity> siEntities = (List<IEntity>) entity.getFieldValues("appService!ServiceInstance");
            Assert.assertFalse(siEntities.isEmpty());
            for(IEntity si : siEntities) {
            	Assert.assertEquals("ServiceInstance", si.getType());
                Assert.assertTrue(si.getFieldNames().size() > 2);
                Assert.assertTrue(si.getFieldNames().contains("appService"));
            }
        }
    }

    @Test
    public void testReverse_badReference() {
        String query = "ServiceInstance{@_oid}.updateStrategies!ApplicationService[@name=~\"^srp-app:Raptor.*\"]";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException qpe) {
            // expected
            System.out.println(qpe.getMessage());
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), qpe.getErrorCode());
        }
    }

    @Test
    public void testReverse_badReference2() {
        String query = "ServiceInstance{@_oid}.(services!ApplicationService.updateStrategies  && updateStrategies!ApplicationService[@name=~\"^srp-app:Raptor.*\"])";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException qpe) {
            // expected
            System.out.println(qpe.getMessage());
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), qpe.getErrorCode());
        }
    }
    
    @Test
    public void testReverse_badReference3() {
        String query = "LOJApplicationService.dependOn!LOJApplicationService{*}";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException qpe) {
            // expected
            System.out.println(qpe.getMessage());
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), qpe.getErrorCode());
        }
    }

    @Test
    public void testReverse_CMS3482() {
        String query = "ServiceInstance{@_oid}.services!ApplicationService[@name=~\"^srp-app:Raptor.*\"].applications!Environment[@name=\"XXXXXX\"]";
        raptorContext.setHint(-1);
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());
    }

//    @Test
//    public void testReverseHint() {
//        // TODO
//    }
//
//    @Test
//    public void testReverseSkip() {
//        // TODO
//    }
//
//    @Test
//    public void testReverseLimit() {
//        // TODO
//    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReverseProjection() {
        String queryStr = "Environment{*}.applications{*}.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"/s]{*}.services!ApplicationService{*}";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        // create QueryMetaClass for result visiting
        QueryMetaClass siQmc = createServiceInstaceQueryMeta();
        QueryMetaClass appServiceQmc = createAppServiceQueryMeta(siQmc);
        
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue("Environment".equals(entity.getType()));
            // not only _oid and _type
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            
            List<IEntity> apps = (List<IEntity>) entity.getFieldValues("applications");
            Assert.assertFalse(apps.isEmpty());
            for(IEntity app : apps) {
            	Assert.assertEquals("ApplicationService", app.getType());
                Assert.assertTrue(app.getFieldNames().size() > 2);
                
                JsonEntity jsonApp = new JsonEntity(appServiceQmc, ((JsonEntity)app).getNode());
            	List<IEntity> siEntities = (List<IEntity>) jsonApp.getFieldValues("appService!ServiceInstance");
            	for(IEntity si : siEntities) {
                	Assert.assertEquals("ServiceInstance", si.getType());
                    Assert.assertTrue(si.getFieldNames().size() > 2);

                    JsonEntity jsonServ = new JsonEntity(siQmc, ((JsonEntity)si).getNode());
                	List<IEntity> asEntities = (List<IEntity>) jsonServ.getFieldValues("services!ApplicationService");
                	for(IEntity as : asEntities) {
                    	Assert.assertEquals("ApplicationService", as.getType());
                        Assert.assertTrue(as.getFieldNames().size() > 2);
                	}
            	}
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReverseProjection1() {
        String queryStr = "Environment{*}.applications{*}.appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"/s]{@port}";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        // create QueryMetaClass for result visiting
        QueryMetaClass siQmc = createServiceInstaceQueryMeta();
        QueryMetaClass appServiceQmc = createAppServiceQueryMeta(siQmc);
        
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue("Environment".equals(entity.getType()));
            // not only _oid and _type
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            
            List<IEntity> apps = (List<IEntity>) entity.getFieldValues("applications");
            Assert.assertFalse(apps.isEmpty());
            for(IEntity app : apps) {
                Assert.assertEquals("ApplicationService", app.getType());
                Assert.assertTrue(app.getFieldNames().size() > 2);
                
                JsonEntity jsonApp = new JsonEntity(appServiceQmc, ((JsonEntity)app).getNode());
                List<IEntity> siEntities = (List<IEntity>) jsonApp.getFieldValues("appService!ServiceInstance");
                for(IEntity si : siEntities) {
                    Assert.assertEquals("ServiceInstance", si.getType());
                    Assert.assertTrue(si.getFieldNames().size() == 3);
                }
            }
        }
    }
    private QueryMetaClass createAppServiceQueryMeta(QueryMetaClass siQmc) {
        QueryMetaClass appServiceQmc = QueryMetaClass.newInstance(newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID), raptorMetaService.getMetaClass("ApplicationService"));
        appServiceQmc.addReverseField(siQmc, (MetaRelationship)siQmc.getFieldByName("appService"));
        return appServiceQmc;
    }

    private QueryMetaClass createServiceInstaceQueryMeta() {
        QueryMetaClass siQmc = QueryMetaClass.newInstance(newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID), raptorMetaService.getMetaClass("ServiceInstance"));
        MetaClass appMeta = raptorMetaService.getMetaClass("ApplicationService");
        siQmc.addReverseField(appMeta, (MetaRelationship) appMeta.getFieldByName("services"));
        return siQmc;
    }
//
//    /**
//     * @see testReverseWithEmbedReference
//     */
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testReverseWithEmbedReference01() {
//        String queryStr = "Group.customer[@name=\"dev-02\"]{*}.person!Team.team!Dep{*}";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() > 0);
//        for (IEntity person : result.getEntities()) {
//            Assert.assertEquals("Person", person.getType());
//            Assert.assertTrue(person.getFieldNames().size() > 2);
//            List<IEntity> teams= (List<IEntity>) person.getFieldValues("person!Team");
//            for (IEntity team : teams) {
//            	Assert.assertEquals("Team", team.getType());
//            	//Assert.assertTrue(team.getFieldNames().size() > 2);
//            	List<IEntity> deps= (List<IEntity>) team.getFieldValues("team!Dep");
//            	for (IEntity dep : deps) {
//                	Assert.assertEquals("Dep", dep.getType());
//                	Assert.assertTrue(dep.getFieldNames().size() > 2);
//            	}
//            }
//
//        }
//    }
//
//    @Test
//    public void testReverseWithEmbedReference02() {
//        String queryStr = "Dep.team.person[@name=\"dev-02\"].customer!Group";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() == 2);
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Group", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testReverseWithEmbedReference03() {
//        String queryStr = "Dep.team.team!Dep";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertEquals(15, result.getEntities().size());
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Dep", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//            
//            List<IEntity> teams = (List<IEntity>)entity.getFieldValues("team");
//            Assert.assertTrue(teams.size() > 0);
//            for (IEntity team : teams) {
//                Assert.assertEquals("Team", team.getType());
//                Assert.assertTrue(team.getFieldNames().size() > 2);
//                
//                List<IEntity> persons = (List<IEntity>) team.getFieldValues("person");
//                for (IEntity person : persons) {
//                	Assert.assertEquals("Person", person.getType());
//                	Assert.assertTrue(person.getFieldNames().size() > 2);
//                }
//            }
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference04() {
//    	String queryStr = "Dep.team.team!Dep.team.person";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertEquals(43, result.getEntities().size());
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Person", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference05() {
//    	String queryStr = "Dep.team[@_oid=\"Dep!dep002!team!team210\"].person";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() == 1);
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Person", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference06() {
//    	String queryStr = "Dep.team{*}.team!Dep[@_oid=\"dep004\"].team.person";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() == 1);
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Team", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference07() {
//    	String queryStr = "Dep.team.team!Dep[@_oid=\"dep004\"].team{*}.person";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() == 1);
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Team", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference08() {
//    	String queryStr = "Group[@_oid=~\"group0[12]\"].customer[@name=~\"dev-0[24]\"].person!Team[@_oid=~\"Dep!dep.*!team!team0[14]0\"].team!Dep[@_oid=\"dep000\"]{*}";
//    	raptorContext.setHint(1);
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertTrue(result.getEntities().size() == 2);
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Dep", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//    
//    @Test
//    public void testReverseWithEmbedReference09() {
//        String queryStr = "Dep{*}.team.team!Dep";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertEquals(10, result.getEntities().size());
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Dep", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//        }
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testReverseWithEmbedReference10() {
//        String queryStr = "Dep.team{*}.team!Dep{*}";
//        IQueryResult result = queryService.query(queryStr, raptorContext);
//        Assert.assertNotNull(result.getEntities());
//        Assert.assertEquals(15, result.getEntities().size());
//        for (IEntity entity : result.getEntities()) {
//            Assert.assertEquals("Team", entity.getType());
//            Assert.assertTrue(entity.getFieldNames().size() > 2);
//            
//            List<IEntity> deps = (List<IEntity>) entity.getFieldValues("team!Dep");
//            Assert.assertTrue(deps.size() > 0);
//            for (IEntity dep : deps) {
//                Assert.assertEquals("Dep", dep.getType());
//                Assert.assertTrue(dep.getFieldNames().size() > 2);
//            }
//        }
//    }
    
    /**
     * FIXME:  @seeAlso testReverseWithSubAnSetQuery
     */
    @Test
    public void testReverseWithSetQuery() {
    }

    /**
     * the join action doesn't filter out the VPool
     */
    @Test
    public void testReverseWithSubQuery00() {
        String queryStr = "VPool[@_oid =& <VCluster, VPool>Resource[exists @environment]{@_oid}]{*}.parentCluster!Compute[@fqdns=\"slc4-0003.ebay.com\"]";
        IQueryResult result = queryService.query(queryStr, stratusContext);
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("VPool", entity.getType());
        }
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testReverseWithSubAnSetQuery() {
        String queryStr = "Environment.applications." +
                "( appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"]{*}.services!ApplicationService[@services =& " +
                "               ServiceInstance[@_oid=\"4fbb314fc681caf13e283a7c\" and @runsOn =& Compute[@name=\"compute-00002\"]]] " +
                " && updateStrategies[@name=\"1-100\"]{*})";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertTrue(result.getEntities().size() > 0);
        
        int serviceCount = 0;
        int strategyCount = 0;
        int others = 0;
        for (IEntity entity : result.getEntities()) {
            if ("ServiceInstance".equals(entity.getType())) {
                serviceCount++;
            }
            else if ("UpdateStrategy".equals(entity.getType())) {
                strategyCount++;
            }
            else {
            	others++;
            }
        }
        Assert.assertTrue(serviceCount == 10);
        Assert.assertTrue(strategyCount == 1);
        Assert.assertTrue(others == 0);
    }

    /**
     * A.b.(c!C{*}.d!D{*} && e{*})
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testReverseWithSetProjection() {
        String queryStr = "Environment.applications." +
                "( appService!ServiceInstance[@name=~\"^srp-app:Raptor.*\"]{*}.services!ApplicationService{*} " +
                " && updateStrategies[@name=\"1-100\"]{*})";
        IQueryResult result = queryService.query(queryStr, raptorContext);
        int serviceCount = 0;
        int strategyCount = 0;
        int appCount = 0;
        int others = 0;
        for (IEntity entity : result.getEntities()) {            
            if ("ServiceInstance".equals(entity.getType())) {
                serviceCount++;
            	List<IEntity> apps = (List<IEntity>)entity.getFieldValues("services!ApplicationService");
            	for(IEntity app : apps) {
					Assert.assertEquals("ApplicationService", app.getType());
					appCount++;
                }
            	
            }
            else if ("UpdateStrategy".equals(entity.getType())) {
                strategyCount++;
            }
            else {
            	others++;
            }
        }
        Assert.assertTrue(serviceCount > 0);
        Assert.assertTrue(strategyCount > 0);
        Assert.assertTrue(appCount > 0);
        Assert.assertTrue(others == 0);
	}

	@Test
	public void testReverseWithEmbedReference01() {
		String queryStr = "Dep.team.team!Dep";
		try {
			queryService.query(queryStr, raptorContext);
			Assert.fail();
		} catch (QueryParseException ex) {
			Assert.assertEquals(QueryErrCodeEnum.REVERSE_QUERY_ON_EMBED_NOT_SUPPORT.getErrorCode(), ex.getErrorCode());
		}
	}

	@Test
	public void testReverseWithEmbedReference02() {
		String queryStr = "Dep.team.(person && team!Dep)";
		try {
			queryService.query(queryStr, raptorContext);
			Assert.fail();
		} catch (QueryParseException ex) {
			Assert.assertEquals(QueryErrCodeEnum.REVERSE_QUERY_ON_EMBED_NOT_SUPPORT.getErrorCode(), ex.getErrorCode());
		}
    }

    private void query_reverse() {
        String query = "AssetServer[ @_oid=\"51f977a4171b7e36601ad3f0\"].nodeServer.nodeServer!AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"]{@resourceId}";
        QueryContext qc = newQueryContext(CMSDB_REPO, CMSDB_MAIN_BRANCH_ID);
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.getEntities().size() > 0);
    }

    @Test
    public void test_reverse_meta_refresh() throws InterruptedException {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                query_reverse();
            }
        };
        testReverse_meta_refresh(run);
    }

    private void testReverse_meta_refresh(final Runnable run) throws InterruptedException {
        final int THREAD_SIZE = 10;
        final int ITERATION_SIZE = 100;
        ExecutorService service = Executors.newFixedThreadPool(THREAD_SIZE);
        final MetadataContext context = new MetadataContext();
        context.setRefreshMetadata(true);
        final Callable<Integer> call = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                cmsdbMetaService.getMetaClasses(context);
                cmsdbMetaService.getMetaClasses(context);
                run.run();
                // refresh the metaclasses
                cmsdbMetaService.getMetaClasses(context);
                cmsdbMetaService.getMetaClasses(context);
                cmsdbMetaService.getMetaClasses(context);
                return 0;
            }
        };
        final List<String> errors = new ArrayList<String>();
        final CountDownLatch latch = new CountDownLatch(10);
        final AtomicInteger errorcCount = new AtomicInteger();
        final AtomicInteger succCount = new AtomicInteger();
        for (int i = 0; i < THREAD_SIZE; i++) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < ITERATION_SIZE; j++) {
                        try {
                            call.call();
                            succCount.incrementAndGet();
                        } catch (Throwable t) {
                            errorcCount.incrementAndGet();
                            t.printStackTrace();
                            try {
                                StringWriter sw = new StringWriter();
                                PrintWriter pw = new PrintWriter(sw);
                                t.printStackTrace(pw);
                                pw.flush();
                                errors.add(sw.toString());
                                pw.close();
                            } catch (Exception e) {
                                System.out.println("write log failed, ignore this one");
                            }
                        }
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
        for (String e : errors) {
            System.out.println(e);
        }
        Assert.assertEquals(0, errorcCount.get());
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void test_reverse_cross_repository() throws InterruptedException {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                queryReverseCrossRepository();
            }
        };
        testReverse_meta_refresh(run);
    }

    @Test
    public void queryReverseCrossRepository() {
        String query = "PoolCluster[@_oid=\"4fbd4ec123456123456ddd\"].pools[@_oid=\"CLgo6gjcth\"].computes.computes!VPool";
        QueryContext qc = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(1, result.getEntities().size());
    }

}
