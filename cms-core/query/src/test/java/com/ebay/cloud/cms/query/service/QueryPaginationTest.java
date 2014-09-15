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

import junit.framework.Assert;

import org.apache.commons.lang.ArrayUtils;
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
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.util.TestUtils;

public class QueryPaginationTest  extends MongoBaseTest {

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
        QueryContext.setDefaultPaginationMode(PaginationMode.SKIP_BASED);
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
        tempContext.setSkips(new int[] {0, 0, 0});
        tempContext.setLimits(new int[]{0, 0, 2});
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService.services.runsOn", tempContext);
        List<IEntity> computers = (List<IEntity>) result.getEntities();
        Assert.assertTrue(tempContext.getDbTimeCost() < 100);
        Assert.assertEquals(2, computers.size());
        Assert.assertEquals("compute-00002", computers.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00003", computers.get(1).getFieldValues("name").get(0));
    }
    
    @Test
    public void testCompute02() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setLimits(new int[] {0, 0, 3});
        tempContext.setSkips(new int[] {0, 0, 2});
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService.services.runsOn", tempContext);
        List<IEntity> computers = (List<IEntity>) result.getEntities();
        Assert.assertEquals(3, computers.size());
        Assert.assertEquals("compute-00004", computers.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00005", computers.get(1).getFieldValues("name").get(0));
        Assert.assertEquals("compute-00006", computers.get(2).getFieldValues("name").get(0));
    }
    
    @Test
    public void testService01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setSkips(new int[] {0, 0, 0});
        tempContext.setLimits(new int[] {0, 2, 0});
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();

        Assert.assertEquals(1, services.size());
        Assert.assertTrue(result.hasMoreResults());
    }
    
    @Test
    public void testService02() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setLimits(new int[] {0, 12, 0});
        tempContext.setSkips(new int[] {0, 0, 0});
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
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
        tempContext.setAllowFullTableScan(true);
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();
               
        Assert.assertEquals(10, services.size());
        Assert.assertEquals("srp-app:Raptor-00002", services.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00003", services.get(1).getFieldValues("name").get(0));
        Assert.assertFalse(result.hasMoreResults());
    }
    
    @Test
    public void testService04() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setLimits(new int[] {0, 3, 0});
        tempContext.setSkips(new int[] {0, 4, 0});
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> services = (List<IEntity>) result.getEntities();
        Assert.assertEquals(3, services.size());
        Assert.assertEquals("srp-app:Raptor-00005", services.get(0).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00006", services.get(1).getFieldValues("name").get(0));
        Assert.assertEquals("srp-app:Raptor-00007", services.get(2).getFieldValues("name").get(0));
    }
    
    @Test
    public void testApplication01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        tempContext.setLimits(new int[] { 2, 0, 0 });
        tempContext.setSkips(new int[] { 0, 0, 0 });
        tempContext.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query("ApplicationService{@name}.services{@name}.runsOn{@name}", tempContext);
        List<IEntity> applications = (List<IEntity>) result.getEntities();
        Assert.assertEquals(2, applications.size());
        Assert.assertEquals("srp-app:Raptor", applications.get(0).getFieldValues("name").get(0));
    }

    @SuppressWarnings("unused")
    private static IEntity createServiceInstance(String baseName) {
        IEntity entity1 = newServiceInstance(baseName);
        String branchId = entity1.getBranchId();
        PersistenceContext persistenceContext = createPersistentContext();
        String newId = persistenceService.create(entity1, persistenceContext);
        String metaType = entity1.getType();
        entity1.setId(newId);
        IEntity saveEntity = persistenceService.get(entity1, persistenceContext);
        return saveEntity;
    }

    private static IEntity newServiceInstance(String baseName) {
        String metaType = SERVICE_INSTANCE;
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setCreator("unitTestUser");
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("name", generateRandomName(baseName));
        return newEntity;
    }
    
    private static JsonEntity newApplicationService(String baseName) {
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
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        QueryContext context = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        context.setAllowFullTableScan(true);
        context.setSkips(new int[] { 1, 0, 0 });
        IQueryResult result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip02() {
        // first round : skip = 7
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setSkips(new int[]{0, 7});
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setLimits(new int[]{0, 0});
        IQueryResult result0 = queryService.query(query, raptorContext);
        Assert.assertFalse(result0.hasMoreResults());
        Assert.assertEquals(3, result0.getEntities().size());

        // second round : add limit
        raptorContext.setSkips(new int[]{0, 7});
        raptorContext.setLimits(new int[]{0, 2});
        result0 = queryService.query(query, raptorContext);
        Assert.assertTrue(result0.hasMoreResults());
        Assert.assertEquals(2, result0.getEntities().size());

        // third round : increase skip/limit based on the suggestion
        int[] nextSkips = result0.getNextCursor().getSkips();
        int[] nextLimits = result0.getNextCursor().getLimits();
        int nextHint = result0.getNextCursor().getHint();
        Assert.assertEquals(3, result0.getNextCursor().getSkips().length);
        Assert.assertEquals(9, result0.getNextCursor().getSkips()[1]);
        Assert.assertEquals(1, nextHint);
        raptorContext.setSkips(nextSkips);
        raptorContext.setLimits(nextLimits);
        raptorContext.setHint(nextHint);
        result0 = queryService.query(query, raptorContext);
        Assert.assertFalse(result0.hasMoreResults());
        Assert.assertEquals(1, result0.getEntities().size());

        // fourth round : increase skip/limits to bigger than the available counts
        raptorContext.setSkips(new int[] {0, 11, 0});
        raptorContext.setLimits(new int[] {0, 0, 0});
        raptorContext.setHint(1);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip03_hint() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setLimits(new int[]{0, 0});
        raptorContext.setSkips(new int[] {0, 10});
        raptorContext.setHint(0);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip03_hint2() {
        String query = "ApplicationService.services[@name=~\"srp.*\"].runsOn";
        raptorContext.setLimits(new int[]{0, 0, 0});
        raptorContext.setSkips(new int[] {0, 11, 0});
        // compare to 03_hint : we have default sort order on _oid, so the hint doesn't affect the result 
        raptorContext.setHint(1);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testQueryIterSkip05_embed() {
        String query = "Manifest{*}.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setSkips(new int[] {1, 0});
        qc.setSourceIP("127.0.0.1");
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(0, result.getEntities().size());
        Assert.assertFalse(result.hasMoreResults());
    }
    
    @Test
    public void testQueryIterSkip05_embed2() {
        String query = "Manifest{*}.versions{*}";
        QueryContext qc = newQueryContext(DEPLOY_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setSkips(new int[] {0, 1});
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
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
        qc.setAllowFullTableScan(true);
        qc.setLimits(new int[] { 1, 2 });
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.hasMoreResults());
        int[] nLimit = result.getNextCursor().getLimits();
        int[] nSkips = result.getNextCursor().getSkips();
        int hint = result.getNextCursor().getHint();
        Assert.assertEquals(0, hint);
        Assert.assertEquals(0, nSkips[0]);
        Assert.assertEquals(2, nSkips[1]);
        Assert.assertEquals(2, nLimit.length);
        Assert.assertEquals(1, nLimit[0]);
        Assert.assertEquals(2, nLimit[1]);

        int count = result.getEntities().size();
        System.out.println("fetch size:  " + count);
        int iterateCount = 1;
        while (result.hasMoreResults()) {
            iterateCount++;
            System.out.println("iterate round: " + iterateCount + ", next skips: "
                    + ArrayUtils.toString(result.getNextCursor().getSkips()) + ",next limits: "
                    + ArrayUtils.toString(result.getNextCursor().getLimits()));
            qc.setSkips(result.getNextCursor().getSkips());
            qc.setLimits(result.getNextCursor().getLimits());

            result = queryService.query(query, qc);
            System.out.println("fetch size:  " + result.getEntities().size());
            count += result.getEntities().size();
        }
        Assert.assertEquals(11, iterateCount);

        QueryContext qc1 = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc1.setAllowFullTableScan(true);
        qc1.setSourceIP("127.0.0.1");
        IQueryResult result1 = queryService.query(query, qc1);
        Assert.assertFalse(result1.hasMoreResults());
        Assert.assertTrue(count >= result1.getEntities().size());
    }

    @Test
    public void testQueryIte_join() {
        String query ="VPool[exists @environment]{*}.computes[@fqdns=~\".*.com\"]";
        QueryContext qc = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        qc.setLimits(new int[] {1, 6});
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertTrue(result.hasMoreResults());
        int[] nLimit = result.getNextCursor().getLimits();
        int[] nSkips = result.getNextCursor().getSkips();
        int hint = result.getNextCursor().getHint();
        Assert.assertEquals(0, hint);
        Assert.assertEquals(1, nSkips[0]);
        Assert.assertEquals(0, nSkips[1]);
        Assert.assertEquals(2, nLimit.length);
        Assert.assertEquals(1, nLimit[0]);
        Assert.assertEquals(6, nLimit[1]);
        
        int count = result.getEntities().size();
        System.out.println("fetch size:  " + count);
        int iterateCount = 1;
        while (result.hasMoreResults()) {
            iterateCount++;
            System.out.println("iterate round: " + iterateCount + ", next skips: "
                    + ArrayUtils.toString(result.getNextCursor().getSkips()) + ",next limits: "
                    + ArrayUtils.toString(result.getNextCursor().getLimits()));
            qc.setSkips(result.getNextCursor().getSkips());
            qc.setLimits(result.getNextCursor().getLimits());

            result = queryService.query(query, qc);
            System.out.println("fetch size:  " + result.getEntities().size());
            count += result.getEntities().size();
        }
        Assert.assertEquals(10, iterateCount);

        QueryContext qc1 = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc1.setAllowFullTableScan(true);
        qc1.setSourceIP("127.0.0.1");
        IQueryResult result1 = queryService.query(query, qc1);
        Assert.assertFalse(result1.hasMoreResults());
        Assert.assertTrue(2 >= result1.getEntities().size());
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
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
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
        qc.setAllowFullTableScan(true);
        qc.setExplain(false);
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNull(queryResult.getExplanations());
    }

    @Test
    public void testQueryExplanation_subquery() {
        String query = "ServiceInstance[@runsOn =& Compute]";
        QueryContext qc = newQueryContext(RAPTOR_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
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
        qc.setSkips(new int[] { 0, 0 });
        qc.setLimits(new int[] { 0, 5 });
        qc.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        int hint = 1;
        qc.setHint(hint);
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertTrue(result.hasMoreResults());
        int[] nextSkips = result.getNextCursor().getSkips();
        int[] nextLimits = result.getNextCursor().getLimits();
        int nextHint = result.getNextCursor().getHint();
        Assert.assertEquals(hint, nextHint);
        Assert.assertEquals(2, nextSkips.length);
        Assert.assertEquals(2, nextLimits.length);
        Assert.assertEquals(5, nextSkips[1]);
        Assert.assertEquals(5, nextLimits[1]);
        
        qc.setSkips(nextSkips);
        qc.setLimits(nextLimits);
        qc.setHint(nextHint);
        result = queryService.query(query, qc);
        Assert.assertEquals(1, result.getEntities().size());
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
        qc.setAllowFullTableScan(true);
        qc.setSkips(new int[] { 0, 0 });
        qc.setLimits(new int[] { 1, 0 });
        int hint = 1;
        qc.setHint(hint);
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
        qc.setAllowFullTableScan(true);
        qc.setSkips(new int[] { 0, 0 });
        qc.setLimits(new int[] { 0 });
        qc.setSourceIP("127.0.0.1");
        IQueryResult result = queryService.query(query, qc);
        Assert.assertFalse(result.hasMoreResults());
    }

    @Test
    public void testQueryIter00() {
        String room = "Room";
        MetaClass metaClass = raptorMetaService.getMetaClass(room);
        PersistenceContext persistenceContext = createPersistentContext();
        BsonEntity entity = new BsonEntity(metaClass);
        final int TOTAL_COUNT = 101;
        for (int i = 0; i < TOTAL_COUNT; i++) {
            entity.removeField(InternalFieldEnum.ID.getName());
            entity.getNode().remove("_id");
            persistenceService.create(entity, persistenceContext);
        }
        String queryStr = "Room";
        // clear skip/limit
        raptorContext.setHint(-1);
        raptorContext.setSkips(null);
        raptorContext.setLimits(new int[] { TOTAL_COUNT / 2 });
        IQueryResult result = queryService.query(queryStr, raptorContext);
        int[] nextSkips = result.getNextCursor().getSkips();
        int[] nextLimits = result.getNextCursor().getLimits();
        Assert.assertEquals(1, nextSkips.length);
        Assert.assertEquals(TOTAL_COUNT / 2, nextSkips[0]);
        Assert.assertEquals(TOTAL_COUNT / 2, nextLimits[0]);
        int lastSkip = 0;
        int lastLimit = result.getEntities().size();
        int count = 1;
        while (result.hasMoreResults()) {
            nextSkips = result.getNextCursor().getSkips();
            nextLimits = result.getNextCursor().getLimits();
            Assert.assertEquals(1, nextSkips.length);
            Assert.assertEquals(nextSkips[0], lastSkip + lastLimit);
            Assert.assertEquals(TOTAL_COUNT / 2, nextLimits[0]);
            lastSkip = nextSkips[0];
            lastLimit = nextLimits[0];
            raptorContext.setSkips(result.getNextCursor().getSkips());
            raptorContext.setLimits(result.getNextCursor().getLimits());
            raptorContext.setHint(result.getNextCursor().getHint());
            result = queryService.query(queryStr, raptorContext);
            count++;
        }
        Assert.assertEquals(3, count);
    }

    private static PersistenceContext createPersistentContext() {
        PersistenceContext context = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), IBranch.DEFAULT_BRANCH);
        context.setDbConfig(dbConfig);
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return context;
    }
    
    /**
     * CMS-3924, no big volume of data for big skip limit test.
     */
    @Test
    public void testQueryOnIndexField() {
        cmsdbContext.setPaginationMode(PaginationMode.SKIP_BASED);
        cmsdbContext.addSortOn("resourceId");
        cmsdbContext.setSkips(new int[] { 200000 });
        IQueryResult result = queryService.query("AssetServer", cmsdbContext);
        Assert.assertNotNull(result);
    }
    
    /**
     * CMS-4192
     */
    @Test
    public void testQueryWithHintOnEmbed01() {
        QueryContext qc = newQueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        qc.setPaginationMode(PaginationMode.SKIP_BASED);
        qc.setHint(2);
        qc.setSkips(new int[] {0, 0, 0, 0});
        qc.setLimits(new int[] { 5, 5, 5, 5 });
        qc.setAllowFullTableScan(true);
        
        IQueryResult result = queryService.query("AssetServer.(configuredTo[@resourceId=~\"P1G2\"] && networkControllers.port)", qc);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.getNextCursor().getHint());
        int[] skips = result.getNextCursor().getSkips();
        Assert.assertEquals(5, skips[0]);
        Assert.assertEquals(0, skips[1]);
        Assert.assertEquals(0, skips[2]);
        Assert.assertEquals(0, skips[3]);
    }
    
    /**
     * CMS-4193
     */
    @Test
    public void testQueryWithHintOnEmbed02() {
        raptorContext.setPaginationMode(PaginationMode.SKIP_BASED);
        raptorContext.setHint(1);
        raptorContext.setSkips(new int[] {0, 0, 0});
        raptorContext.setLimits(new int[] { 1, 1, 1 });
        IQueryResult result = queryService.query("Dep.team.person", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getNextCursor().getHint());
        int[] skips = result.getNextCursor().getSkips();
        Assert.assertEquals(1, skips[0]);
        Assert.assertEquals(0, skips[1]);
        Assert.assertEquals(0, skips[2]);
    }

}
