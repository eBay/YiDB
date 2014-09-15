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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.QueryCursor;
import com.ebay.cloud.cms.query.util.TestUtils;
import com.ebay.cloud.cms.utils.CollectionUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class QueryPaginationByIdTest  extends MongoBaseTest {

    private static final String RESOURCE_ID = "resourceId";
    private static final String APPLICATION_SERVICE = "ApplicationService";
    private static final String SERVICE_INSTANCE = "ServiceInstance";
    private static IPersistenceService persistenceService = null;
    private static IRepositoryService  repoService        = null;
    private static IMetadataService raptorMetaService;
    private static PaginationMode oldDefaultPaginationMode = null;

    @BeforeClass
    public static void setUpBeforeClass() {
        persistenceService = DalServiceFactory.getPersistenceService(getDataSource(), TestUtils.getTestDalImplemantation(dataSource));
        repoService = RepositoryServiceFactory.createRepositoryService(getDataSource(), "localCMSServer");
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        
        oldDefaultPaginationMode = QueryContext.getDefaultPaginationMode();
        QueryContext.setDefaultPaginationMode(PaginationMode.ID_BASED);

        raptorContext.setPaginationMode(QueryContext.getDefaultPaginationMode());
        deployContext.setPaginationMode(QueryContext.getDefaultPaginationMode());
        stratusContext.setPaginationMode(QueryContext.getDefaultPaginationMode());
        cmsdbContext.setPaginationMode(QueryContext.getDefaultPaginationMode());
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        QueryContext.setDefaultPaginationMode(oldDefaultPaginationMode);
    }

    @Test
    public void testCompute01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        QueryCursor cursor = new QueryCursor();
        cursor.setJoinCursorValues(null);
        cursor.setLimits(new int[]{0, 0, 2});
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        tempContext.setCursor(cursor);
        
        IQueryResult result = queryService.query("ApplicationService.services.runsOn", tempContext);
        List<IEntity> computers = (List<IEntity>) result.getEntities();
        Assert.assertTrue(tempContext.getDbTimeCost() < 100);
        Assert.assertEquals(2, computers.size());
        Assert.assertEquals("compute-00002", computers.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00003", computers.get(1).getFieldValues("name").get(0));
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertTrue(result.getNextCursor().isJoinCursor());
    }
    
    @Test
    public void testCompute02() {
        IQueryResult result = null;
        String oid = null;
        {
            String queryString0 = "Compute[@name=\"compute-00003\"]";
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setPaginationMode(PaginationMode.ID_BASED);
            result = queryService.query(queryString0, context);
            oid = result.getEntities().get(0).getId();
            System.out.println(result.getEntities().get(0).getId());
        }

        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        QueryCursor cursor = new QueryCursor();
        cursor.setLimits(new int[] {0, 0, 3});
        cursor.setJoinCursorValues(Arrays.asList("", "", oid));
        tempContext.setCursor(cursor);
        
        String queryString = "ApplicationService.services.runsOn";
        result = queryService.query(queryString, tempContext);
        List<IEntity> computers = (List<IEntity>) result.getEntities();
        Assert.assertEquals(3, computers.size());
        Assert.assertEquals("compute-00004", computers.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00005", computers.get(1).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00006", computers.get(2).getFieldValues("name").get(0));
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertTrue(result.getNextCursor().isJoinCursor());
    }

    @Test
    public void testService01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        QueryCursor cursor = new QueryCursor();
        cursor.setJoinCursorValues(Arrays.asList("", "", ""));
        cursor.setLimits(new int[] { 0, 2, 0 });
        tempContext.setCursor(cursor);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);

        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();

        Assert.assertEquals(1, services.size());
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertNotNull(result.getNextCursor());
        int[] nextLimits = result.getNextCursor().getLimits();
        Assert.assertEquals(3, nextLimits.length);
        Assert.assertEquals(2, nextLimits[1]);
        Assert.assertEquals(1000, nextLimits[2]);
        Assert.assertNull(result.getNextCursor().getSkips());
    }

    @Test
    public void testService02() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        QueryCursor cursor = tempContext.getCursor();
        cursor.setJoinCursorValues(Arrays.<String> asList(null, null, null, null));
        cursor.setLimits(new int[] { 0, 12, 0 });

        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();

        Assert.assertEquals(10, services.size());
        Assert.assertEquals("srp-app:Raptor-00002", services.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00003", services.get(1).getFieldValues("name").get(0));
        Assert.assertFalse(result.hasMoreResults());
    }
    
    @Test
    public void testService03() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        tempContext.setAllowFullTableScan(true);
        
        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();
        
        Assert.assertEquals(10, services.size());
        Assert.assertEquals("srp-app:Raptor-00002", services.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00003", services.get(1).getFieldValues("name").get(0));
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertNull(result.getNextCursor());
    }
    
    @Test
    public void testService04() {
        String oid = null;
        {
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            IQueryResult result = queryService.query("ServiceInstance[@name=\"srp-app:Raptor-00004\"]", context);
            oid = result.getEntities().get(0).getId();
        }
        
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        tempContext.setAllowFullTableScan(true);
        QueryCursor cursor = tempContext.getCursor();
        cursor.setLimits(new int[] {0, 3, 0});
        cursor.setJoinCursorValues(Arrays.asList(null, oid, ""));
        
        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();
        Assert.assertEquals(3, services.size());
        Assert.assertEquals("srp-app:Raptor-00005", services.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00006", services.get(1).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00007", services.get(2).getFieldValues("name").get(0));
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertTrue(result.getNextCursor().isJoinCursor());
    }
    
    @Test
    public void testApplication01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setPaginationMode(PaginationMode.ID_BASED);
        tempContext.setAllowFullTableScan(true);
        QueryCursor cursor = tempContext.getQueryCursor();
        cursor.setLimits(new int[] { 2, 0, 0 });
        
        IQueryResult result = queryService.query("ApplicationService{@name}.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> applications = (List<IEntity>) result.getEntities();
        Assert.assertEquals(2, applications.size());
        Assert.assertEquals("srp-app:Raptor", applications.get(0).getFieldValues("name").get(0));
    }

    @SuppressWarnings("unused")
    private static IEntity createServiceInstance(String baseName) {
        IEntity entity1 = newServiceInstance(baseName);
        String branchId = entity1.getBranchId();
        PersistenceContext persistenceContext = createRaptorPersistentContext();
        String newId = persistenceService.create(entity1, persistenceContext);
        String metaType = entity1.getType();
        entity1.setId(newId);
        IEntity saveEntity = persistenceService.get(entity1, persistenceContext);
        return saveEntity;
    }

    private static IEntity newServiceInstance(String baseName) {
        String metaType = SERVICE_INSTANCE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        BsonEntity newEntity = new BsonEntity(instCls);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("name", generateRandomName(baseName));
        return newEntity;
    }
    
    private static IEntity newApplicationService(String baseName) {
        String metaType = APPLICATION_SERVICE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("name", generateRandomName(baseName));
        return newEntity;
    }
    
    @Test
    public void testQueryIterSkip01() {
        String oid = null;
        {
            String query = "ApplicationService{@_oid}.services[@name=~\"srp.*\"].runsOn";
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query(query, context);
            oid = result.getEntities().get(0).getId();
        }
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        raptorContext.removeSortOn();
        raptorContext.getQueryCursor().setJoinCursorValues(Arrays.asList(oid, null, null));
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip02() {
        raptorContext.removeSortOn();
        String oid = null;
        String oid_11 = null;
        {
            String query = "ServiceInstance[@name=~\"srp.*\"]{@_oid}";
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query(query, context);
            oid = result.getEntities().get(6).getId();
            oid_11 = result.getEntities().get(10).getId();
        }
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setSkips(null);
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        raptorContext.getQueryCursor().setHint(-1);
        raptorContext.getQueryCursor().setJoinCursorValues(Arrays.asList(null, oid, null));
        // first round : 
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.getQueryCursor().setLimits(new int[]{0, 0});
        IQueryResult result0 = queryService.query(query, raptorContext);
        Assert.assertFalse(result0.hasMoreResults());
        Assert.assertEquals(3, result0.getEntities().size());

        // second round : add limit
        raptorContext.getQueryCursor().setJoinCursorValues(Arrays.asList(null, oid));
        raptorContext.setLimits(new int[]{0, 2});
        result0 = queryService.query(query, raptorContext);
        Assert.assertTrue(result0.hasMoreResults());
        Assert.assertEquals(2, result0.getEntities().size());

        // third round : increase join oid limit based on the suggestion
        int nextHint = result0.getNextCursor().getHint();
        Assert.assertEquals(1, nextHint);
        raptorContext.setCursor(result0.getNextCursor());
        result0 = queryService.query(query, raptorContext);
        Assert.assertFalse(result0.hasMoreResults());
        Assert.assertEquals(1, result0.getEntities().size());

        // fourth round : increase skip/limits to bigger than the available counts
        raptorContext.getCursor().setJoinCursorValues(Arrays.asList(null, oid_11, ""));
        raptorContext.getCursor().setLimits(new int[] {0, 0, 0});
        raptorContext.getCursor().setHint(1);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertNull(result.getNextCursor());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip03_hint() {
        raptorContext.removeSortOn();
        String oid = null;
        {
            String query = "ApplicationService.services[@name=~\"srp.*\"]{@_oid}";
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query(query, context);
            oid = result.getEntities().get(9).getId();
        }
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        raptorContext.getCursor().setLimits(new int[] { 0, 0 });
        raptorContext.getCursor().setJoinCursorValues(Arrays.asList(null, oid));
        raptorContext.setHint(0);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip03_hint2() {
        raptorContext.removeSortOn();
        String oid = null;
        {
            String query = "ServiceInstance[@name=~\"srp.*\"]{@_oid}";
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query(query, context);
            oid = result.getEntities().get(10).getId();
        }
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        raptorContext.getCursor().setLimits(new int[] { 0, 0, 0 });
        raptorContext.getCursor().setJoinCursorValues(Arrays.asList(null, oid));
        // compare to 03_hint : we have default sort order on _oid, so the hint doesn't affect the result
        raptorContext.setHint(1);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip05_embed() {
        String oid = null;
        String secondVersionId = null;
        {
            QueryContext context = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query("Manifest{*}.versions{*}", context);
            oid = result.getEntities().get(0).getId();
            secondVersionId = ((IEntity)result.getEntities().get(0).getFieldValues("versions").get(1)).getId();
        }
        // oid: 4fbdaccec681643199735a5b
        // secondVersionId:
        // Manifest!4fbdaccec681643199735a5b!versions!4fbdaccec681643199735a5e
        String query = "Manifest{*}.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.getCursor().setJoinCursorValues(Arrays.asList(oid, secondVersionId));
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(0, result.getEntities().size());
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertNull(result.getNextCursor());
    }
    
    @Test
    public void testQueryIterSkip05_embed2() {
        String secondVersionId = null;
        {
            QueryContext context = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query("Manifest{*}.versions{*}", context);
            secondVersionId = ((IEntity)result.getEntities().get(0).getFieldValues("versions").get(1)).getId();
        }
        String query = "Manifest{*}.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.getCursor().setJoinCursorValues(Arrays.asList(null, secondVersionId));
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        // compare to _embed1, the skip on the second array doesn't take affect as Manifest->Version is embed
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertFalse(result.hasMoreResults());
    }
    
    @Test
    public void testQueryIte_reverse() {
        String query = "VPool[exists @environment]{*}.parentCluster!Compute[@fqdns=~\".*.com\"]";
        QueryContext qc = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.getCursor().setLimits(new int[] { 1, 2 });
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertNotNull(result.getNextCursor().getJoinCursorValues());
        Assert.assertNull(result.getNextCursor().getSkips());

        int[] nLimit = result.getNextCursor().getLimits();
        int hint = result.getNextCursor().getHint();
        Assert.assertEquals(0, hint);
        Assert.assertEquals(2, nLimit.length);
        Assert.assertEquals(1, nLimit[0]);
        Assert.assertEquals(2, nLimit[1]);

        int count = result.getEntities().size();
        System.out.println("fetch size:  " + count);
        int iterateCount = 1;
        while (result.hasMoreResults()) {
            iterateCount++;
            System.out.println("iterate round: " + iterateCount + ", next skip _oids: "
                    + ArrayUtils.toString(result.getNextCursor().getJoinCursorValues()) + ",next limits: "
                    + ArrayUtils.toString(result.getNextCursor().getLimits()));
            qc.setCursor(result.getNextCursor());

            result = queryService.query(query, qc);
            System.out.println("fetch size:  " + result.getEntities().size());
            count += result.getEntities().size();
        }
        Assert.assertEquals(11, iterateCount);

        QueryContext qc1 = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc1.setAllowFullTableScan(true);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc1.setSourceIP("127.0.0.1");
        IQueryResult result1 = queryService.query(query, qc1);
        Assert.assertFalse(result1.hasMoreResults());
        Assert.assertNull(result.getNextCursor());
        Assert.assertTrue(count >= result1.getEntities().size());
    }

    @Test
    public void testQueryIte_join() {
        String query = "VPool[exists @environment]{*}.computes[@fqdns=~\".*.com\"]";
        QueryContext qc = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.setLimits(new int[] { 1, 6 });
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.hasMoreResults());

        int[] nLimit = result.getNextCursor().getLimits();
        int hint = result.getNextCursor().getHint();
        Assert.assertEquals(0, hint);
        List<String> nextCursorValues = result.getNextCursor().getJoinCursorValues();
        Assert.assertNotNull(nextCursorValues);
        Assert.assertEquals(2, nextCursorValues.size());
        Assert.assertEquals(2, nLimit.length);
        Assert.assertEquals(1, nLimit[0]);
        Assert.assertEquals(6, nLimit[1]);

        // continuing query using the next cursor
        List<String> fetchVPoolIds = new ArrayList<String>();
        int count = result.getEntities().size();
        System.out.println("fetch size:  " + count);
        // add to fetched ids
        for (IEntity entity : result.getEntities()) {
            fetchVPoolIds.add(entity.getId());
        }
        
        int iterateCount = 1;
        while (result.hasMoreResults()) {
            iterateCount++;
            System.out.println("iterate round: " + iterateCount + ", next skip _oid s: "
                    + ArrayUtils.toString(result.getNextCursor().getJoinCursorValues()) + ",next limits: "
                    + ArrayUtils.toString(result.getNextCursor().getLimits()));
            qc.setCursor(result.getNextCursor());

            result = queryService.query(query, qc);
            System.out.println("fetch size:  " + result.getEntities().size());
            count += result.getEntities().size();
            for (IEntity entity : result.getEntities()) {
                Assert.assertFalse(fetchVPoolIds.contains(entity.getId()));
                fetchVPoolIds.add(entity.getId());
            }
        }
        Assert.assertEquals(10, iterateCount);

        // assert the iterated query results with the no-limit results.
        List<String> fetchVPoolIds2 = new ArrayList<String>();
        QueryContext qc1 = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc1.setPaginationMode(PaginationMode.ID_BASED);
        qc1.setAllowFullTableScan(true);
        qc1.setSourceIP("127.0.0.1");
        IQueryResult result1 = queryService.query(query, qc1);
        Assert.assertFalse(result1.hasMoreResults());
        Assert.assertTrue(2 >= result1.getEntities().size());
        for (IEntity entity : result1.getEntities()) {
            fetchVPoolIds2.add(entity.getId());
        }
        Assert.assertEquals(fetchVPoolIds, fetchVPoolIds2);
    }

    @Test
    public void testCount_withCursor() {
        QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        context.setPaginationMode(PaginationMode.SKIP_BASED);
        context.setCountOnly(true);
        context.setAllowFullTableScan(true);
        long noSkipCount = queryService.query(SERVICE_INSTANCE, context).getCount();
        
        context.setSkips(new int[] {3});
        long withSkipCount = queryService.query(SERVICE_INSTANCE, context).getCount();
        Assert.assertEquals(noSkipCount, withSkipCount);
        
        context.setPaginationMode(PaginationMode.ID_BASED);
        long noCursorCount = queryService.query(SERVICE_INSTANCE, context).getCount();
        Assert.assertEquals(noSkipCount, noCursorCount);
        
        JsonEntity cursor = new JsonEntity(raptorMetaService.getMetaClass(SERVICE_INSTANCE));
        // make the cursor bigger than some valid entity id
        cursor.setId("4fbb314fc681caf13e283a7a");
        context.getCursor().setSingleCursorValue(cursor);
        long withCursorCount = queryService.query(SERVICE_INSTANCE, context).getCount();
        Assert.assertEquals(noSkipCount, withCursorCount);
    }

    /**
     * Count doesn't support join
     */
    @Ignore
    @Test
    public void testQueryIter_count() {
        EntityContext raptorEntityContext = new EntityContext();
        raptorEntityContext.setSourceIp("127.0.0.1");
        for (int i = 0; i < 26; i++) {
            IEntity app = newServiceInstance("service_query_itr_count_");
            entityService.create(app, raptorEntityContext);
        }

        for (int i = 0; i < 16; i++) {
            IEntity app = newApplicationService("app_service_query_itr_count_");
            entityService.create(app, raptorEntityContext);
        }

        String query = "ServiceInstance.appService";
        QueryContext qc = newQueryContext(raptorContext);
        qc.setCountOnly(true);
        qc.setHint(1);
        qc.setAllowFullTableScan(true);
        qc.setLimits(new int[] { 12, 6 });
        IQueryResult result = queryService.query(query, qc);
        Assert.assertFalse(result.hasMoreResults());
    }

    @Ignore
    /**
     * Count doesn't support join
     * Case 2 : hint = 0
     */
    @Test
    public void testQueryIter_count2() {
        EntityContext raptorEntityContext = new EntityContext();
        raptorEntityContext.setSourceIp("127.0.0.1");
        for (int i = 0; i < 26; i++) {
            IEntity app = newServiceInstance("service_query_itr_count_");
            entityService.create(app, raptorEntityContext);
        }

        for (int i = 0; i < 16; i++) {
            IEntity app = newApplicationService("app_service_query_itr_count_");
            entityService.create(app, raptorEntityContext);
        }

        String query = "ServiceInstance.appService";
        QueryContext qc = newQueryContext(raptorContext);
        qc.setCountOnly(true);
        qc.setHint(0);
        qc.setAllowFullTableScan(true);
        qc.setLimits(new int[] { 12, 6 });
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.hasMoreResults());
        while (result.hasMoreResults()) {
            qc.setSkips(result.getNextCursor().getSkips());
            // keep limit always as 12, 6
//            qc.setLimits(result.getNextLimits());
            result = queryService.query(query, qc);
            System.out.println("fetch size:  " + result.getEntities().size());
        }
    }

    /**
     * CMS-3031
     */
    @Test
    public void testQueryExplanation_notControll() {
        String query = SERVICE_INSTANCE;
        QueryContext qc = newQueryContext(RAPTOR_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.setExplain(false);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNull(queryResult.getExplanations());
    }

    /**
     * CMS-3031
     */
    @Test
    public void testQueryExplanation_notControll2() {
        String query = "ServiceInstance[@runsOn =& Compute]";
        QueryContext qc = newQueryContext(RAPTOR_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.setExplain(false);
        
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNull(queryResult.getExplanations());
    }

    @Test
    public void testQueryExplanation_subquery() {
        String query = "ServiceInstance[@runsOn =& Compute]";
        QueryContext qc = newQueryContext(RAPTOR_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNotNull(queryResult.getExplanations());
        List<IQueryExplanation> explans = queryResult.getExplanations();
        Assert.assertEquals(2, explans.size());
        ObjectNode objectNode = (ObjectNode)explans.get(0).getJsonExplanation();
        Assert.assertEquals("Compute", objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue());
    }

    @Test
    public void testQueryIterLimit01() {
        String query = "ApplicationService{*}.services";
        QueryContext qc = newQueryContext(RAPTOR_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.getCursor().setLimits(new int[] { 0, 5 });
        int hint = 1;
        qc.getCursor().setHint(hint);
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertTrue(result.getNextCursor().isJoinCursor());
        List<String> joinedCursorValues = result.getNextCursor().getJoinCursorValues();
        int[] nextLimits = result.getNextCursor().getLimits();
        int nextHint = result.getNextCursor().getHint();
        Assert.assertEquals(hint, nextHint);
        Assert.assertEquals(2, joinedCursorValues.size());
        Assert.assertEquals(2, nextLimits.length);
        Assert.assertEquals(5, nextLimits[1]);

        qc.setCursor(result.getNextCursor());
        result = queryService.query(query, qc);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertNotNull(result.getNextCursor());
    }

    /**
     * limit for embed version
     * 
     * hint for embed
     */
    @Test
    public void testQueryIterLimit02_embed() {
        String query = "Manifest.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.getCursor().setLimits(new int[] { 1, 0 });
        int hint = 1;
        qc.getCursor().setHint(hint);
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertNull(result.getNextCursor());
    }

    @Test
    public void testQueryIterLimit03_embed() {
        String query = "Manifest.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.ID_BASED);
        qc.setAllowFullTableScan(true);
        qc.getCursor().setLimits(new int[] { 0 });
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertFalse(result.hasMoreResults());
    }

    @Test
    public void testQueryIter00() {
        raptorContext.removeSortOn();
        String room = "Room";
        MetaClass metaClass = raptorMetaService.getMetaClass(room);
        PersistenceContext persistenceContext = createRaptorPersistentContext();
        BsonEntity entity = new BsonEntity(metaClass);
        final int CREATE_COUNT = 101;
        for (int i = 0; i < CREATE_COUNT; i++) {
            entity.removeField(InternalFieldEnum.ID.getName());
            entity.getNode().remove("_id");
            persistenceService.create(entity, persistenceContext);
        }
        long TOTAL_COUNT = 0;
        {
            QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
            context.setPaginationMode(PaginationMode.ID_BASED);
            context.setCountOnly(true);
            IQueryResult result = queryService.query(room, context);
            TOTAL_COUNT = result.getCount();
        }

        String queryStr = "Room";
        // clear skip/limit
        raptorContext.getCursor().setHint(-1);
        raptorContext.getCursor().setSkips(null);
        raptorContext.getCursor().setJoinCursorValues(null);
        raptorContext.getCursor().setSingleCursorValue(null);
        raptorContext.getCursor().setLimits(new int[] { CREATE_COUNT / 2 });
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        IQueryResult result = queryService.query(queryStr, raptorContext);
        Assert.assertNull(result.getNextCursor().getJoinCursorValues());
        IEntity cursorValue = result.getNextCursor().getSingleCursorValue();
        int[] nextLimits = result.getNextCursor().getLimits();
        Assert.assertEquals(CREATE_COUNT / 2, nextLimits[0]);
        int fetchCount = 0;
        int count = 1;
        fetchCount = fetchCount + result.getEntities().size();
        while (result.hasMoreResults()) {
            Assert.assertFalse(result.getNextCursor().isJoinCursor());
            cursorValue = result.getNextCursor().getSingleCursorValue();
            Assert.assertNotNull(cursorValue);
            nextLimits = result.getNextCursor().getLimits();
            Assert.assertEquals(CREATE_COUNT / 2, nextLimits[0]);
            raptorContext.setCursor(result.getNextCursor());
            result = queryService.query(queryStr, raptorContext);
            fetchCount = fetchCount + result.getEntities().size();
            count++;
        }
        Assert.assertEquals(3, count);
        Assert.assertEquals(TOTAL_COUNT, fetchCount);
    }

    private static PersistenceContext newPersistenceContext(IMetadataService metaService) {
        PersistenceContext pContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(dbConfig);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }
    
    private static PersistenceContext createRaptorPersistentContext() {
        return newPersistenceContext(raptorMetaService);
    }

    private static PersistenceContext createCmsdbPersistentContext() {
        return newPersistenceContext(cmsdbMetaService);
    }

    @Test
    public void testQuery_sortOnIterate() throws Exception {
        for (int i = 0; i < 2; i++) {
            testQuery_sortOnNonIndex();
        }
    }

    @Test
    public void testQuery_sortOnNonIndex() throws Exception {
        final String metaName = "Manifest";
        final String RELEASE_TYPE = "releaseType";
        final String SOFTWARE_VERSION = "softwareVersion";
        long TOTAL_COUNT = 0;
        final int ENTITY_COUNT = 100;
        final Set<String> allIds = new HashSet<String>();
        // prepare data
        {
            MetaClass metaClass = cmsdbMetaService.getMetaClass(metaName);
            PersistenceContext persistenceContext = createCmsdbPersistentContext();
            BsonEntity entity = new BsonEntity(metaClass);
            for (int i = 0; i < ENTITY_COUNT; i++) {
                entity.removeField(InternalFieldEnum.ID.getName());
                entity.getNode().remove("_id");
                // indexed unique field
                entity.addFieldValue(RESOURCE_ID, generateRandomName("sortOn-Non-Index") + "!!!" + i);
                // non-indexed same value field
                if (i % 2 == 0) {
                    entity.addFieldValue(RELEASE_TYPE, "product-release");
                } else if (i % 3 == 0) {
                    // simluate the null case
                    MetaField field = metaClass.getFieldByName(RELEASE_TYPE);
                    DBObject dbObject = new BasicDBObject();
                    dbObject.put(MetaField.VALUE_KEY, null);
                    entity.getNode().put(field.getDbName(), dbObject);
                } else {
                    // not set value for other case
                }
                // non-indexed different value field with duplicate
                entity.addFieldValue(SOFTWARE_VERSION, "1.0." + i % 100);
                persistenceService.create(entity, persistenceContext);
            }

            QueryContext context = newQueryContext(CMSDB_REPO, CMSDB_MAIN_BRANCH_ID);
            context.setPaginationMode(PaginationMode.ID_BASED);
            context.setCountOnly(false);
            context.setAllowFullTableScan(true);
            IQueryResult result = queryService.query(metaName, context);
            TOTAL_COUNT = result.getEntities().size();
            for (IEntity e : result.getEntities()) {
                allIds.add(e.getId());
            }
        }

        String query = metaName + "{@_oid}";
        QueryContext queryContext = newQueryContext(CMSDB_REPO, CMSDB_MAIN_BRANCH_ID);
        queryContext.setPaginationMode(PaginationMode.ID_BASED);
        queryContext.setAllowFullTableScan(true);
        queryContext.setExplain(true);

        // case 0 : no - pagination
        {
            queryAndIterate(TOTAL_COUNT, query, queryContext, allIds, 100);
        }

        // case 0.0 : explicted paged on _oid
        {
            iteratorWithSort(Arrays.asList(InternalFieldEnum.ID.getName()), TOTAL_COUNT, query, queryContext, allIds, 1);
            iteratorWithSort(Arrays.asList(InternalFieldEnum.ID.getName()), TOTAL_COUNT, query, queryContext, allIds, 2);
            iteratorWithSort(Arrays.asList(InternalFieldEnum.ID.getName()), TOTAL_COUNT, query, queryContext, allIds, 3);
            iteratorWithSort(Arrays.asList(InternalFieldEnum.ID.getName()), TOTAL_COUNT, query, queryContext, allIds, 5);
        }

        // case 1 : pagination on indexed field
        {
            iteratorWithSort(Arrays.asList(RESOURCE_ID), TOTAL_COUNT, query, queryContext, allIds, 1);
            iteratorWithSort(Arrays.asList(RESOURCE_ID), TOTAL_COUNT, query, queryContext, allIds, 2);
            iteratorWithSort(Arrays.asList(RESOURCE_ID), TOTAL_COUNT, query, queryContext, allIds, 3);
            iteratorWithSort(Arrays.asList(RESOURCE_ID), TOTAL_COUNT, query, queryContext, allIds, 5);
        }

        // case 2 : pagination on non-indexed field with some different value
        {
            iteratorWithSort(Arrays.asList(SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 1);
            iteratorWithSort(Arrays.asList(SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 2);
            iteratorWithSort(Arrays.asList(SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 3);
            iteratorWithSort(Arrays.asList(SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 5);
        }

        // case 3 : pagination on non-indexed field with same value
        {
            iteratorWithSort(Arrays.asList(RELEASE_TYPE), TOTAL_COUNT, query, queryContext, allIds, 1);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE), TOTAL_COUNT, query, queryContext, allIds, 2);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE), TOTAL_COUNT, query, queryContext, allIds, 3);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE), TOTAL_COUNT, query, queryContext, allIds, 5);
        }

        // case 4 : multiple sort on
        {
            iteratorWithSort(Arrays.asList(RELEASE_TYPE, SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 1);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE, SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 2);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE, SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 3);
            iteratorWithSort(Arrays.asList(RELEASE_TYPE, SOFTWARE_VERSION), TOTAL_COUNT, query, queryContext, allIds, 5);
        }
    }

    private void iteratorWithSort(final List<String> sortOnFields, long TOTAL_COUNT, String query, QueryContext queryContext, Set<String> allIds, int step)
            throws Exception {
        IQueryResult result;
        int fetchCount = 0;
        int queryIterationCount = 0;

        Set<String> fetchedIds = new HashSet<String>();
        queryContext.getCursor().setLimits(null);
        queryContext.getCursor().setSingleCursorValue(null);
        queryContext.getCursor().removeSortOn();
        queryContext.getCursor().setLimits(new int[] {step});
        for (String sortOnField : sortOnFields) {
            queryContext.getCursor().addSortOn(sortOnField);
        }
        result = queryService.query(query, queryContext);
        queryIterationCount++;
        List<String> repeatedIds = new ArrayList<String>();
        do {
            for (IEntity e : result.getEntities()) {
                if (fetchedIds.contains(e.getId())) {
                    repeatedIds.add(e.getId());
                } else {
                    fetchedIds.add(e.getId());
                }
                fetchCount++;
            }
            if (result.hasMoreResults()) {
                Assert.assertNotNull(result.getNextCursor());
                Assert.assertNotNull(result.getNextCursor().getSortOn());
                Assert.assertFalse(result.getNextCursor().isJoinCursor());
                Assert.assertNotNull(result.getNextCursor().getSingleCursorValue());
                for (String sortField : sortOnFields) {
                    Assert.assertTrue(result.getNextCursor().getSortOn().contains(sortField));
                }
                result.getNextCursor().setLimits(new int[] { step });
                queryContext.setCursor(result.getNextCursor());
                result = queryService.query(query, queryContext);
                queryIterationCount++;
            } else {
                break;
            }
        } while (true);
        System.out.println(" iterate count : " + queryIterationCount);
        Set<String> missedIds = CollectionUtils.diffSet(allIds, fetchedIds);
        if (TOTAL_COUNT != fetchCount) {
            StringBuilder sb = new StringBuilder();
            sb.append(" Repeated entity ids: ").append(new ObjectMapper().writeValueAsString(repeatedIds));
            sb.append(" Missed entity ids: ").append(new ObjectMapper().writeValueAsString(missedIds));
            Assert.fail(sb.toString());
        }
        Assert.assertEquals(" Missed entity ids: " + new ObjectMapper().writeValueAsString(missedIds), 0, missedIds.size());
        Assert.assertEquals(TOTAL_COUNT, fetchCount);
        Assert.assertEquals(0, repeatedIds.size());
        Assert.assertEquals(0, missedIds.size());
        Assert.assertEquals(TOTAL_COUNT, fetchedIds.size());
//        Assert.assertEquals(2, queryIterationCount);
        fetchedIds.clear();
        fetchCount = 0;
        queryIterationCount = 0;
    }

    private void queryAndIterate(long TOTAL_COUNT, String query, QueryContext queryContext, Set<String> allIds, int step) throws Exception {
        queryContext.getCursor().setLimits(null);
        queryContext.getCursor().setSingleCursorValue(null);

        Set<String> fetchedIds = new HashSet<String>();
        IQueryResult result;
        int fetchCount = 0;
        queryContext.getCursor().setLimits(new int[] {step});
        result = queryService.query(query, queryContext);
        List<String> repeatedIds = new ArrayList<String>();
        do {
            for (IEntity e : result.getEntities()) {
                if (fetchedIds.contains(e.getId())) {
                    repeatedIds.add(e.getId());
                } else {
                    fetchedIds.add(e.getId());
                }
                fetchCount++;
            }
            if (result.hasMoreResults()) {
                Assert.assertNotNull(result.getNextCursor());
                Assert.assertNull(result.getNextCursor().getSortOn());
                Assert.assertNull(result.getNextCursor().getJoinCursorValues());
                Assert.assertFalse(result.getNextCursor().isJoinCursor());
                queryContext.setCursor(result.getNextCursor());
                result = queryService.query(query, queryContext);
            } else {
                break;
            }
        } while (true);

        Set<String> missedIds = CollectionUtils.diffSet(allIds, fetchedIds);
        Assert.assertEquals("Missed Entity ids : " + missedIds, 0, missedIds.size());
        System.out.println(" repeated entity ids: " + new ObjectMapper().writeValueAsString(repeatedIds));
        if (TOTAL_COUNT != fetchCount) {
            Assert.fail(" repeated entity ids: " + new ObjectMapper().writeValueAsString(repeatedIds));
        }
        Assert.assertEquals(TOTAL_COUNT, fetchCount);
        Assert.assertEquals(0, repeatedIds.size());
        Assert.assertEquals(TOTAL_COUNT, fetchedIds.size());
//        Assert.assertEquals(2, queryIterationCount);
        fetchedIds.clear();
        fetchCount = 0;
    }

    @Test
    public void testSortOn_fieldProperty() {
        String query = SERVICE_INSTANCE + "{@_oid, @manifestRef.$_length}";
        // create service instance data
        raptorContext.getCursor().setSkips(null);
        raptorContext.getCursor().setLimits(null);
        raptorContext.setPaginationMode(PaginationMode.ID_BASED);
        raptorContext.getCursor().addSortOn("manifestRef._length");
        raptorContext.getCursor().setJoinCursorValues(null);
        raptorContext.getCursor().setSingleCursorValue(null);
        raptorContext.getCursor().setLimits(new int[] { 1 });
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        IEntity oldEntity = result.getEntities().get(0);
        Integer oldLength = (Integer)oldEntity.getFieldProperty("manifestRef", "_length");

        raptorContext.setCursor(result.getNextCursor());
        result = queryService.query(query, raptorContext);
        Assert.assertTrue(result.hasMoreResults());
        int nullToNotNull = 0;
        for (IEntity e : result.getEntities()) {
            Integer curLength = (Integer) e.getFieldProperty("manifestRef", "_length");
            if (curLength == null) {
                Assert.assertNull(oldLength);
            } else if (oldLength == null) {
                nullToNotNull++;
            } else {
                Assert.assertTrue(curLength >= oldLength);
            }
            // swap
            oldLength = curLength;
        }
    }


}
