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

import java.util.List;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.util.TestUtils;

/**
 * Sepearated test class that focus on embed related cases.
 * 
 * @author liasu
 * 
 */
public class EmbedQueryServiceTest extends MongoBaseTest {

    @Test
    public void integerNot() {
        IQueryResult result = null;
        // case 1
        String query = "NodeServer.capacities[@resourceId=\"1001.7.2.57:slot\" and not @total != -2]";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(0, result.getEntities().size());

        // case 2
        String notQuery = "NodeServer.capacities[@resourceId=\"1001.7.2.57:slot\" and not @total != 2 ]";
        result = queryService.query(notQuery, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    /**
     * CMS-4623
     */
    @Test
    public void integerEq() {
        IQueryResult result = null;
        raptorContext.setAllowFullTableScan(true);
        String query = "AllowFullTableScanParentTest.embed[@number = 1]";
        result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    protected static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        context.setModifier("unitTestUser");
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        context.setDbConfig(dbConfig);
        return context;
    }

    @Test
    public void decimalNegative() {
        String decimalQuery = null;
        IQueryResult result = null;
        MetaClass exprClass = raptorMetaService.getMetaClass("ExpressionTest");
        JsonEntity exprEntity = new JsonEntity(exprClass);
        exprEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        exprEntity.addFieldValue("name", "expression-test");
        exprEntity.addFieldValue("i", 3);
        exprEntity.addFieldValue("i1", 3);
        exprEntity.addFieldValue("d1", 1.0);
        exprEntity.addFieldValue("d", 3.0);
        EntityContext context = newEntityContext();
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        entityService.create(exprEntity, context);

        raptorContext.setAllowFullTableScan(true);
        // case 3
        decimalQuery = "ExpressionTest[@d > 2.1 ]";
        result = queryService.query(decimalQuery, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        decimalQuery = "ExpressionTest[not @d > 2.1 ]";
        result = queryService.query(decimalQuery, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());

        // case 4
        decimalQuery = "ExpressionTest[@d1 > -2.1 ]";
        result = queryService.query(decimalQuery, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        decimalQuery = "ExpressionTest[not @d1 < -2.1 ]";
        result = queryService.query(decimalQuery, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testQuery_filter() {
        raptorContext.setAllowFullTableScan(true);
        String queryString = "Dep.team[@name=\"dev-team-020\"]{@label, @isLargeTeam}";

        IQueryResult result = queryService.query(queryString, raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertNotNull(entities);
        IEntity entity = entities.get(0);
        Assert.assertEquals(4, entity.getFieldNames().size());
        Assert.assertFalse(entity.getFieldNames().contains("name"));
    }

    @Test
    public void testQuery_embedSet() {
        raptorContext.setAllowFullTableScan(false);
        String query = "AllowFullTableScanParentTest.(embed && embed2)";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(
                    QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(),
                    qoe.getErrorCode());
        }
    }

    @Test
    public void testQuery_embedSet2() {
        raptorContext.setAllowFullTableScan(false);
        String query = "AllowFullTableScanParentTest.(embed || embed2)";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(
                    QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(),
                    qoe.getErrorCode());
        }
    }

    /**
     * CMS-3721
     */
    @Test
    public void testQuery_embedSet3() {
        raptorContext.setAllowFullTableScan(true);
        String query = "AllowFullTableScanParentTest.(embed && embed2)";
        IQueryResult result = queryService.query(query, raptorContext);
    }

    /**
     * CMS-3721
     */
    @Test
    public void testQuery_embedSet4() {
        raptorContext.setAllowFullTableScan(true);
        String query = "AllowFullTableScanParentTest.(embed || embed2)";
        IQueryResult result = queryService.query(query, raptorContext);
    }

    /**
     * CMS-3767
     */
    @Test
    public void testQuery_embedSet5() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "LBService.(capacities && runsOn.nodeServer!AssetServer.networkControllers.port.networkPorts!Vlan.subnets)";
        IQueryResult result = queryService.query(query, cmsdbContext);
    }

    /**
     * CMS-4108
     */
    @Test
    public void embedSet_hint() {
        String query = "AssetServer{@resourceId}.(configuredTo[@resourceId=~\"P1G2\"] && networkControllers.port)";
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(cmsdbContext.getDbConfig());
        context.setAllowFullTableScan(true);
        context.getCursor().setHint(1);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void embedSet_nohint() {
        String query = "AssetServer{*}.(configuredTo[@resourceId=~\"^P1G2\"/s] && networkControllers.port)";
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(cmsdbContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = null;

        // no hint
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());

        // hint = 0
        context.getCursor().setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        
        // hint = 1
        context.getCursor().setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("SkuConfiguration", getFirstExecutedMetaClass(result));

        // hint = 2
        context.getCursor().setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 3
        context.getCursor().setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("NetworkPort", getFirstExecutedMetaClass(result));
    }

    /**
     * CMS-4108
     */
    @Test
    public void embedHint() {
        String query = "AssetServer[@_creator=\"CloudMgr\" or @resourceOwner=\"CloudMgr\"].networkControllers{@resourceId}.port{@resourceId}";
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(cmsdbContext.getDbConfig());
        context.setAllowFullTableScan(true);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = null;

        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 0
        context.getCursor().setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 1
        context.getCursor().setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 2
        context.getCursor().setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("NetworkPort", getFirstExecutedMetaClass(result));

        // hint = 3
        context.getCursor().setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));
    }

    String getFirstExecutedMetaClass(IQueryResult result) {
        return result.getExplanations().get(0).getJsonExplanation()
                .get("criteria").get("$and").get(0).get("_t").getTextValue();
    }

    @Test
    public void embedSetHint_criteriaOnNonEmbed() {
        String query = "AssetServer.networkControllers.port.networkPorts!Vlan.subnets[@_oid=\"51afafe9e4b01696e74867fc\"]{@resourceId}";
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(cmsdbContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        IQueryResult result = null;

        // no hint
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("Subnet", getFirstExecutedMetaClass(result));

        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("NetworkPort", getFirstExecutedMetaClass(result));

        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("Vlan", getFirstExecutedMetaClass(result));

        // hint = 4
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertEquals(2, result.getEntities().size());
        Assert.assertEquals("Subnet", getFirstExecutedMetaClass(result));
    }

    /**
     * A.(embed_b.ref1 && ref2.embed_c) with hint=3
     */
    @Test
    public void testEmbedSet_tail() {
        String query = "Manifest{*}."
                + "versions{*}."
                + "("
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + "&& "
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageVersion Bundle.*\"]{*}"
                + ")";
        QueryContext context = new QueryContext(DEPLOY_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        context.setExplain(true);
        IQueryResult result = null;
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));

        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));
        
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));
        
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("DeployClassOfService", getFirstExecutedMetaClass(result));
        
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Package", getFirstExecutedMetaClass1(result));
        
        context.setHint(5);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Package", getFirstExecutedMetaClass1(result));
    }

    /**
     * A.(embed_b.ref1 && ref2.embed_c) with hint=3
     */
    @Test
    public void testEmbedSet_tail02() {
        String query = "Manifest{*}."
                + "versions{*}."
                + "("
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageVersion Bundle.*\"]{*}"
                + "&& "
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + ")";
        QueryContext context = new QueryContext(DEPLOY_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));
        context.setExplain(true);
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));

        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));
        
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Package", getFirstExecutedMetaClass1(result));
        
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Package", getFirstExecutedMetaClass1(result));
        
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Manifest", getFirstExecutedMetaClass1(result));
        
        context.setHint(5);
        result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("DeployClassOfService", getFirstExecutedMetaClass(result));
    }

    /*
     * A.(b && (b || b)) Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testRefEmbed00() {
        cmsdbContext.setAllowFullTableScan(true);
        cmsdbContext.setExplain(true);

        String query = "NodeServer{@resourceId}.(networkAddress[@_oid=\"51f9766f171b7e36601ac23c\"]{@resourceId} && capacities{@available})";

        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());

        getExecutedMetaClass(result, cmsdbContext);

        List<IEntity> capacities = (List<IEntity>) result.getEntities().get(0)
                .getFieldValues("capacities");
        Assert.assertEquals(1, capacities.size());
        for (IEntity capa : capacities) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(capa.getFieldNames().size() > 2);
            Assert.assertEquals("ResourceCapacity", capa.getType());
        }
    }

    private void getExecutedMetaClass(IQueryResult result, QueryContext cmsdbContext) {
        MetaClass mc = cmsdbMetaService.getMetaClass("NodeServer");
        MetaField field = mc.getFieldByName("networkAddress");
        ArrayNode andNodes = (ArrayNode) result.getExplanations().get(1)
                .getJsonExplanation().get("criteria").get("$and");
        boolean hasId = false;
        // FIXME: flatten will have different explanation
        if (cmsdbContext.getRegistration().registrationId.equals("hierarchy")) {
	        JsonNode node = andNodes.get(1).get("$and").get(0).get("$and").get(1);
	        hasId |= node.has(field.getValueDbName() + "._i");
	        Assert.assertTrue(hasId);
        }
    }

    private String getFirstExecutedMetaClass1(IQueryResult result) {
        return result.getExplanations().get(0).getJsonExplanation()
                .get("criteria").get("$and").get(0).get("$and").get(0)
                .get("_t").getTextValue();
    }

    /*
     * A.b.(c && d)
     */
    @Test
    public void testRefEmbed01() {
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer{@resourceId}.managementServer.(networkAddress[@_oid=\"51f97481171b7e36601aa781\"]{@resourceId} && capacities{@available})";

        // no hint
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(5, result.getEntities().size());
        Assert.assertEquals("NetworkAddress", getFirstExecutedMetaClass(result));

        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(5, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(5, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));

        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(5, result.getEntities().size());
        Assert.assertEquals("NetworkAddress", getFirstExecutedMetaClass(result));

        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(5, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));
    }

    /*
     * A.b.(c || d)
     */
    @Test
    public void testRefEmbed02() {
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(true);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer.managementServer{@_oid}.(networkAddress{@resourceId} || capacities[@_oid=\"NodeServer!51f97481171b7e36601aa785!capacities!5216e063171bd2d7a58c67d5\"]{@available})";

        IQueryResult result = null;
        // no hint
        result = queryService.query(query, context);
        Assert.assertEquals(6, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(6, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));

        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(6, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));

        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(6, result.getEntities().size());
        Assert.assertEquals("NetworkAddress", getFirstExecutedMetaClass(result));

        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(6, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));
    }

    @Test
    public void testRefEmbed03() {
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer.(networkControllers[@resourceId=\"005056B603F8\"]{@resourceOwner} && managementServer.(networkAddress && capacities{@available}))";

        // no hint
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));

        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("NetworkAddress", getFirstExecutedMetaClass(result));

        // hint = 4
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertEquals(3, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));
    }

    @Test
    public void testRefEmbed04() {
        QueryContext context = new QueryContext(CMSDB_REPO,
                IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(true);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer.(networkControllers[@resourceId=\"005056B603F8\"]{@resourceOwner} || managementServer.(networkAddress || capacities{@available}))";

        // no hint
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));

        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));

        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("NetworkAddress", getFirstExecutedMetaClass(result));

        // hint = 4
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertEquals(18, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass1(result));
    }
    
    /*
     * A.b.(c && d)
     * CMS-4226
     */
    @Test
    public void testRefEmbed05() {
        QueryContext context = new QueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer[@resourceId=\"Uuid-564D07D6-7C82-9CEC-7028-364219411198\"]{@resourceId}.managementServer.capacities{@available}";

        context.getCursor().setLimits(new int[] { 1, 1, 1 });
        IQueryResult result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass(result));
        Assert.assertEquals("NodeServer",
                result.getExplanations().get(1).getJsonExplanation().get("criteria").get("$and").get(0).get("_t").getTextValue());
        Assert.assertTrue(result.getExplanations().get(1).getJsonExplanation().get("criteria").get("$and").get(1)
                .has("_i"));
    }

    @Test
    public void testRefEmbed06() {
        QueryContext context = new QueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        context.setDbConfig(deployContext.getDbConfig());
        context.setAllowFullTableScan(false);
        context.setExplain(true);
        context.setRegistration(TestUtils.getDefaultDalImplementation(dataSource));

        String query = "AssetServer{@resourceId}.( networkControllers.port.networkPorts!Vlan[@resourceId=\"lvs2-ra00400:01\"] && nodeServer.capacities{@resourceId, @total})";

        // no hint
        IQueryResult result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Vlan", getFirstExecutedMetaClass(result));
        
        // hint = 0
        context.setHint(0);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));
        
        // hint = 1
        context.setHint(1);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("AssetServer", getFirstExecutedMetaClass1(result));
        
        // hint = 2
        context.setHint(2);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("NetworkPort", getFirstExecutedMetaClass(result));
        
        // hint = 3
        context.setHint(3);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("Vlan", getFirstExecutedMetaClass(result));
        
        // hint = 4
        context.setHint(4);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass(result));
        
        // hint = 5
        context.setHint(5);
        result = queryService.query(query, context);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("NodeServer", getFirstExecutedMetaClass(result));
    }
}
