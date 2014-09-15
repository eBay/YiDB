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

package com.ebay.cloud.cms.query.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchResult.QueryExplanation;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.util.TestUtils;

/**
 * User: Rene Xu Email: rene.xu@ebay.com Date: 5/21/12 5:07 PM
 */
public class QueryServiceTest extends MongoBaseTest {

    @Test(expected = QueryParseException.class)
    public void testException() {
        IQueryResult result = queryService.query("Rene", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNull(result.getEntities());
        System.out.println(result.toString());
    }

    @Test
    public void testQuery00() {
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query("Environment[@_oid=\"4fbb314fc681caf13e283a78\"]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testQuery01() {
        raptorContext.setExplain(true);
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Environment", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        for (IEntity e : entities) {
            Assert.assertEquals("Environment", e.getType());
            System.out.println(e.getFieldValues("name").get(0));
        }

        List<IQueryExplanation> explanations = result.getExplanations();
        Assert.assertNotNull(explanations);
        Assert.assertNotNull(explanations.get(0));
        raptorContext.setExplain(false);
    }

    @Test
    public void testQuery02() {
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query("Environment[@name=\"EnvRaptor\"]{*}", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity env = entities.get(0);
        System.out.println(env.getFieldNames());
        //"_user": null => now CMS JsonEntity will ignore this null value field
        Assert.assertEquals(17, env.getFieldNames().size());
    }

    @Test
    public void testQuery03() {
        IQueryResult result = queryService.query(
                "ApplicationService[@name=\"srp-app:Raptor\"]{@name, @activeManifestDiff, @updateStrategies}",
                raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity env = entities.get(0);
        Collection<String> fieldNames = env.getFieldNames();

        // jianxu1: 2012/12/14, after remove type as response, field names count
        // should be 4 now
        // liasu: 2013/5/28, CMS 2645: make _oid, _type as implicitly required
        // for each projection
        Assert.assertEquals(5, fieldNames.size());
        // Assert.assertEquals(4, fieldNames.size());
    }

    @Test
    public void testQuery04RegexEscape() {
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query(
                "ApplicationService[@name=~\"^srp-app:Raptor\\*\"/s]{@name, @activeManifestDiff, @updateStrategies}",
                raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testQuery04RegexEscape_fulltablescan() {
        raptorContext.setAllowFullTableScan(false);
        try {
            queryService.query(
                    "ApplicationService[@name=~\"^srp-app:Raptor\\*\"/i]{@name, @activeManifestDiff, @updateStrategies}",
                    raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
        try {
            queryService.query(
                    "ApplicationService[@name=~\"srp-app:Raptor\\*\"/s]{@name, @activeManifestDiff, @updateStrategies}",
                    raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
    }

    @Test
    public void testPluralNameQuery() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Environments", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        for (IEntity e : entities) {
            Assert.assertEquals("Environment", e.getType());
            System.out.println(e.getFieldValues("name").get(0));
        }
    }

    @Test
    public void testAttrExist() {
        IQueryResult result = queryService.query("ApplicationService[exists @name]", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.getEntities().size());
    }

    @Test
    public void testAttrNotExist() {
        IQueryResult result = queryService.query("ApplicationService[not exists @name]", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testJoinQuery() {
    	raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Environment.applications{@label}.services", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity app = entities.get(0);
        Assert.assertNotNull(app);
        Assert.assertEquals("ApplicationService", app.getType());
        List<?> values = app.getFieldValues("label");
        Assert.assertEquals(1, values.size());
        System.out.println("label=" + values.get(0));
        Assert.assertEquals("srp-app", values.get(0));
    }

    @Test
    public void testJoinQuery2() {
        IQueryResult result = queryService.query("Environment.applications[@label=\"srp-app\"]", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity app = entities.get(0);
        System.out.println(app.getFieldValues("name").get(0));
        Assert.assertNotNull(app);
        System.out.println(app.getFieldValues("name").get(0));
        Assert.assertEquals("ApplicationService", app.getType());
    }

    @Test
    public void testJoinQuery3() {
        IQueryResult result = queryService.query("Environment.applications[@label=\"srp-app2\"]", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getEntities().isEmpty());
    }

    @Test
    public void testJoinQuery4() {
        IQueryResult result = queryService.query("ServiceInstance.runsOn", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(10, entities.size());
    }

    @Test
    public void testJoinQuery5() {
        IQueryResult result = queryService.query("ServiceInstance[@name=\"srp-app:Raptor-00002\"].runsOn",
                raptorContext);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.getEntities().isEmpty());
    }

    @Test
    public void testJoinQuery6() {
        IQueryResult result = queryService.query("ServiceInstance[@name=\"srp-app:Raptor-00001\"].runsOn",
                raptorContext);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getEntities().isEmpty());
    }

    @Test(expected = QueryParseException.class)
    public void testJoinQuery7() {
        // abc doesnt exist
        IQueryResult result = queryService.query("ServiceInstance[@name=\"srp-app:Raptor-00001\"].abc", raptorContext);
        Assert.assertNotNull(result);
    }

    @Test
    public void testJoinQuery8() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService
                .query("ApplicationService[@label=\"srp-app\"]{*}.services{*}", raptorContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(2, appList.size());
    }

    @Test
    public void testJoinQuery9() {
        IQueryResult result = queryService.query("ApplicationService[@label=\"srp-app\"].services{*}", raptorContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(12, appList.size());
    }

    @Test
    public void testJoinQuery10() {
    	raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("ApplicationService[@label=\"srp-app\"]{*}.services", raptorContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(2, appList.size());
    }

    /**
     * cases from paas integration. Join query will have a join action that
     * filter out all service instance doesn't match the filter search.
     */
    @Test
    public void testJoinQuery11FilterOnReference() {
        IQueryResult result = queryService.query(
                "ApplicationService{@_oid, @services}.services[@_oid=\"4fbb314fc681caf13e283a7a\"]", raptorContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(1, appList.size());
        Assert.assertEquals(1, appList.get(0).getFieldValues("services").size());
    }

    /**
     * cases from paas integration. To get services under application service
     * that has some specific ref service id
     */
    @Test
    public void tsetJoinQuery12FilterOnReference1() {
        IQueryResult result = queryService.query(
                "ApplicationService[@services=\"4fbb314fc681caf13e283a7a\"]{@_oid, @services}", raptorContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(1, appList.size());
        Assert.assertEquals(10, appList.get(0).getFieldValues("services").size());
    }

    /**
     * cases from CMS-2279. Cross repo would have duplicated fields
     */
    @Test
    public void testJoinQuery13FilterOnCrossReference() {
        IQueryResult result = queryService.query(
                "RefApplicationService{*}.raptorApplicationService[@name=~\"^srp-app:Raptor*\"].services{*}",
                stratusContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(1, appList.size());
        Assert.assertEquals(1, appList.get(0).getFieldValues("raptorApplicationService").size());
    }

    /**
     * Embed would have duplicated fields should not have duplication
     */
    @Test
    public void testJoinQuery14FilterOnEmbedReference() {
        IQueryResult result = queryService.query(
                "Manifest{@versions}.versions.approvals[@name=\"Dummy Approval Bundle-0-0002\"]", deployContext);
        List<IEntity> appList = result.getEntities();
        Assert.assertEquals(1, appList.size());
        Assert.assertEquals(1, appList.get(0).getFieldValues("versions").size());
    }

    @Test
    public void testEmbeddedQuery() {
        deployContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Manifest", deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity manifest = entities.get(0);
        Assert.assertNotNull(manifest);
        List<?> versions = manifest.getFieldValues("versions");
        Assert.assertEquals(2, versions.size());
        IEntity version = (IEntity) versions.get(0);
        Assert.assertFalse(version.getFieldValues(InternalFieldEnum.TYPE.getName()).isEmpty());
        Assert.assertFalse(version.getFieldValues("name").isEmpty());
    }

    @Test
    public void testEmbeddedQuery2() {
        deployContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Manifest.versions", deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(2, versions.size());
        for (IEntity v : versions) {
            Assert.assertNotNull(v);
            System.out.println(v.getFieldValues("name").get(0));
        }
    }

    @Test
    public void testEmbeddedQuery3() {
        IQueryResult result = queryService.query("Manifest.versions[@name=\"Dummy ManifestVersion Bundle-0-0001\"]",
                deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
        for (IEntity v : versions) {
            Assert.assertNotNull(v);
            System.out.println(v.getFieldValues("name").get(0));
            Assert.assertEquals("Dummy ManifestVersion Bundle-0-0001", v.getFieldValues("name").get(0));
        }
    }

    @Test
    public void testEmbeddedQuery4() {
        IQueryResult result = queryService
                .query("Manifest.versions[@name=\"Dummy ManifestVersion Bundle-0-0002\"]{*}.packages[@name=\"Dummy Package Bundle-0-0002\"]{*}",
                        deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
    }

    @Ignore
    // TODO: CMS-3695
    @Test
    public void testEmbedQuery_notExists() {
        String query = "Manifest.versions[not exists @description]";
        deployContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    // Manifest{*}.approvals[@applicationId="125"].classOfService[@_oid="QA"]?allowFullTableScan=true&limit=100&skip=0
    @Test
    public void testEmbedJoinQuery() {
        deployContext.setAllowFullTableScan(true);
        String joinQuery = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"].versions{*}.approvals[@appId=\"Dummy appId--1790614450\"].classOfService[@name=\"QA\"]";
        deployContext.setHint(3);
        IQueryResult result = queryService.query(joinQuery, deployContext);
        assertNotNull(result);
        List<IEntity> versions = result.getEntities();
        assertTrue(versions.size() > 0);
        for (IEntity entity : versions) {
            String type = entity.getType();
            assertEquals("ManifestVersion", type);
        }
    }

    // /**
    // * CMS-3509 : need more deep references out from packages
    // * A-embed->B->ref->C-ref->D
    // * Manifest -> ManifestApproval -> PolicyGroup -> ClassOfService
    // */
    // @Test
    // public void testEmbedJoinQuery_removal() {
    // cmsdbContext.setAllowFullTableScan(false);
    // String query =
    // "Manifest[@resourceId=\"Manifest-Test\"].approvals{*}.policyGroup.compatibleCos[@_oid=\"XXX-not-valid\"]";
    // IQueryResult result = queryService.query(query, cmsdbContext);
    // Assert.assertEquals(0, result.getEntities().size());
    // }

    @Test
    public void testMixedQuery() {
    	raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("ApplicationService.services.appService", raptorContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> services = result.getEntities();
        Assert.assertNotNull(services);
        Assert.assertEquals(10, services.size());
        for (IEntity app : services) {
            Assert.assertNotNull(app);
            System.out.println(app.getFieldValues("name").get(0));
            Assert.assertEquals("ApplicationService", app.getType());
        }
    }

    @Test
    public void testEEQuery() {
        deployContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Manifest.versions.approvals{@name}", deployContext);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());

        List<IEntity> packages = result.getEntities();
        Assert.assertEquals(2, packages.size());
    }

    @Test
    public void testERefQuery() {
        deployContext.setAllowFullTableScan(true);
        deployContext.setHint(-1);
        IQueryResult result = queryService.query("Manifest.versions.packages", deployContext);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());

        List<IEntity> packages = result.getEntities();
        Assert.assertEquals(6, packages.size());
        for (IEntity v : packages) {
            System.out.println(v);
            Assert.assertEquals("Package", v.getType());
        }
    }

    @Test
    public void testProjection() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("ServiceInstance{@port}.runsOn{@label}", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(10, entities.size());
        IEntity serviceInstance = entities.get(0);
        Assert.assertNotNull(serviceInstance.getId());
        List<?> portVal = serviceInstance.getFieldValues("port");
        Assert.assertFalse(portVal.isEmpty());
        List<?> httpsVal = serviceInstance.getFieldValues("https");
        Assert.assertTrue(httpsVal.isEmpty());

        List<?> runsOnVal = serviceInstance.getFieldValues("runsOn");
        IEntity compute = (IEntity) runsOnVal.get(0);
        Assert.assertNotNull(compute.getId());
        Assert.assertNotNull(compute.getType());
        List<?> labelVal = compute.getFieldValues("label");
        Assert.assertFalse(labelVal.isEmpty());
        List<?> locationVal = compute.getFieldValues("location");
        Assert.assertTrue(locationVal.isEmpty());
    }

    @Test
    public void testProjection02() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("ServiceInstance{@port}.runsOn", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(10, entities.size());
        IEntity serviceInstance = entities.get(0);
        Assert.assertNotNull(serviceInstance.getId());
        List<?> portVal = serviceInstance.getFieldValues("port");
        Assert.assertFalse(portVal.isEmpty());
        List<?> httpsVal = serviceInstance.getFieldValues("https");
        Assert.assertTrue(httpsVal.isEmpty());

        List<?> runsOnVal = serviceInstance.getFieldValues("runsOn");
        IEntity compute = (IEntity) runsOnVal.get(0);
        // only _oid and _type
        Assert.assertTrue(compute.getFieldNames().size() == 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProjection03() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Environment{*}.applications.updateStrategies{*}", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity environment = entities.get(0);
        Assert.assertNotNull(environment.getId());
        Assert.assertNotNull("Environment", environment.getType());
        Assert.assertTrue(environment.getFieldNames().size() > 2);

        List<IEntity> apps = (List<IEntity>) environment.getFieldValues("applications");
        Assert.assertEquals(1, apps.size());
        IEntity app = (IEntity) apps.get(0);
        Assert.assertEquals("ApplicationService", app.getType());

        List<IEntity> strategies = (List<IEntity>) app.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() == 2);
        for (IEntity strategy : strategies) {
            Assert.assertEquals("UpdateStrategy", strategy.getType());
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProjection04() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("Environment{*}.applications{*}.updateStrategies", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
        IEntity environment = entities.get(0);
        Assert.assertNotNull(environment.getId());
        Assert.assertNotNull("Environment", environment.getType());
        Assert.assertTrue(environment.getFieldNames().size() > 2);

        List<IEntity> apps = (List<IEntity>) environment.getFieldValues("applications");
        Assert.assertEquals(1, apps.size());
        IEntity app = (IEntity) apps.get(0);
        Assert.assertEquals("ApplicationService", app.getType());
        Assert.assertTrue(app.getFieldNames().size() > 2);

        List<IEntity> strategies = (List<IEntity>) app.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() == 2);
        for (IEntity strategy : strategies) {
            Assert.assertEquals("UpdateStrategy", strategy.getType());
            Assert.assertTrue(strategy.getFieldNames().size() == 2);
        }
    }

    /**
     * CMS-3698
     */
    @Test
    public void testProjection05() {
        String query = null;
        IQueryResult result = null;
        ArrayNode display = null;
        raptorContext.setShowDisplayMeta(true);

        ObjectNode appDisplay = null;
        ObjectNode servDisplay = null;
        // case 0
        query = "ApplicationService[@name=~\"srp-app.*\"]{@name}.services{@name}";
        result = queryService.query(query, raptorContext);
        // assertion
        Assert.assertNotNull(result.getDisplayMeta());
        display = result.getDisplayMeta();
        Assert.assertEquals(1, display.size());
        appDisplay = (ObjectNode) display.get(0);
        Assert.assertNotNull(appDisplay);
        Assert.assertEquals(4, appDisplay.get("fields").size());
        servDisplay = (ObjectNode) appDisplay.get("fields").get("services").get("refDataType").get("ServiceInstance");
        Assert.assertEquals(3, servDisplay.get("fields").size());

        // case 1
        query = "ApplicationService[@name=~\"srp-app.*\"]{*}.services{@name}";
        result = queryService.query(query, raptorContext);
        Assert.assertNotNull(result.getDisplayMeta());
        // assertion
        display = result.getDisplayMeta();
        Assert.assertEquals(1, display.size());
        appDisplay = (ObjectNode) display.get(0);
        Assert.assertNotNull(appDisplay);
        Assert.assertEquals(33, appDisplay.get("fields").size());
        servDisplay = (ObjectNode) appDisplay.get("fields").get("services").get("refDataType").get("ServiceInstance");
        Assert.assertEquals(3, servDisplay.get("fields").size());

        // case 2
        query = "ResourceGroup.<VCluster, Compute>children[exists @healthState]{*}";
        QueryContext qc = newQueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        qc.setShowDisplayMeta(true);
        result = queryService.query(query, qc);
        Assert.assertNotNull(result.getDisplayMeta());
        Assert.assertEquals(3, result.getDisplayMeta().size());

        ObjectNode resourceDisplay = null;
        MetaClass resourceMeta = cmsdbMetaService.getMetaClass("Resource");
        resourceDisplay = (ObjectNode) result.getDisplayMeta().get(0).get("fields");
        Assert.assertEquals(resourceMeta.getFieldNames().size(), resourceDisplay.size());

        ObjectNode vclusterDisplay = null;
        vclusterDisplay = (ObjectNode) result.getDisplayMeta().get(1).get("fields");
        MetaClass vclusterMeta = cmsdbMetaService.getMetaClass("VCluster");
        Assert.assertEquals(vclusterMeta.getFieldNames().size(), vclusterDisplay.size());

        ObjectNode computeDisplay = null;
        computeDisplay = (ObjectNode) result.getDisplayMeta().get(2).get("fields");
        MetaClass computeMeta = cmsdbMetaService.getMetaClass("Compute");
        Assert.assertEquals(computeMeta.getFieldNames().size(), computeDisplay.size());

        // case 3
        query = "ResourceGroup{@_oid}.<VCluster, Compute>children[exists @healthState]{*}";
        result = queryService.query(query, qc);
        Assert.assertNotNull(result.getDisplayMeta());
        Assert.assertEquals(1, result.getDisplayMeta().size());
        ObjectNode childRel = (ObjectNode) result.getDisplayMeta().get(0).get("fields").get("children")
                .get("refDataType");
        Assert.assertEquals(3, childRel.size());
        vclusterDisplay = (ObjectNode) childRel.get("VCluster").get("fields");
        Assert.assertEquals(vclusterMeta.getFieldNames().size(), vclusterDisplay.size());
        computeDisplay = (ObjectNode) childRel.get("Compute").get("fields");
        Assert.assertEquals(computeMeta.getFieldNames().size(), computeDisplay.size());

        // case 4
        query = "<VCluster, Compute>Resource[exists @healthState]{*}";
        result = queryService.query(query, qc);
        Assert.assertNotNull(result.getDisplayMeta());
        Assert.assertEquals(3, result.getDisplayMeta().size());
        resourceDisplay = (ObjectNode) result.getDisplayMeta().get(0).get("fields");
        Assert.assertEquals(resourceMeta.getFieldNames().size(), resourceDisplay.size());
        vclusterDisplay = (ObjectNode) result.getDisplayMeta().get(1).get("fields");
        Assert.assertEquals(vclusterMeta.getFieldNames().size(), vclusterDisplay.size());
        computeDisplay = (ObjectNode) result.getDisplayMeta().get(2).get("fields");
        Assert.assertEquals(computeMeta.getFieldNames().size(), computeDisplay.size());
    }

    @Test
    public void testProject06() {
        // case 0 : aggregation
        String query = "NetworkAddress<@healthState>{@healthState, $max(@address), $count()}";
        cmsdbContext.setAllowFullTableScan(true);
        cmsdbContext.setShowDisplayMeta(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        ArrayNode display = result.getDisplayMeta();
        ObjectNode na = (ObjectNode) display.get(0).get("fields");
        Assert.assertEquals(5, na.size());
        Assert.assertTrue(na.has("healthState"));
        Assert.assertTrue(na.has("$count"));
        Assert.assertTrue(na.has("$max_address"));
    }

    /**
     * Set query
     */
    @Test
    public void testProject07() {
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setShowDisplayMeta(true);
        String query = "ApplicationService.(services{@_oid}&&updateStrategies{@_type})";
        IQueryResult result = queryService.query(query, raptorContext);
        ArrayNode display = result.getDisplayMeta();
        Assert.assertEquals(2, display.size());
        ObjectNode servDisplay = (ObjectNode) display.get(0).get("fields");
        Assert.assertEquals(2, servDisplay.size());
        ObjectNode usDisplay = (ObjectNode) display.get(1).get("fields");
        Assert.assertEquals(2, usDisplay.size());
    }

    @Test
    public void testProject08() {
        String query = "Rack{@location}.assets.asset!AssetServer{@resourceId,@faultDomain}";
        cmsdbContext.setAllowFullTableScan(true);
        cmsdbContext.setShowDisplayMeta(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertNotNull(result.getDisplayMeta());
        Assert.assertEquals(1, result.getDisplayMeta().size());
        // reverse
        ObjectNode rackDisplay = (ObjectNode) result.getDisplayMeta().get(0).get("fields");
        Assert.assertEquals(4, rackDisplay.size());
        ObjectNode assetDisplay = (ObjectNode) rackDisplay.get("assets").get("refDataType").get("Asset").get("fields");
        Assert.assertEquals(3, assetDisplay.size());
        ObjectNode assetServerDisplay = (ObjectNode) assetDisplay.get("asset!AssetServer").get("refDataType")
                .get("AssetServer").get("fields");
        Assert.assertEquals(4, assetServerDisplay.size());
    }

    @Test
    public void testProjection09() {
        String query = "ApplicationService[@resourceId=~\"srp-app.*\"]{*}.accessPoints{*}";
        cmsdbContext.setShowDisplayMeta(true);
        cmsdbContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertNotNull(result.getDisplayMeta());
        ObjectNode apDisplay = (ObjectNode) result.getDisplayMeta().get(0).get("fields").get("accessPoints")
                .get("refDataType").get("AccessPoint").get("fields");
        MetaClass meta = cmsdbMetaService.getMetaClass("AccessPoint");
        Assert.assertEquals(meta.getFieldNames().size(), apDisplay.size());
    }

    @Test
    public void testBooleanValue() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("ServiceInstance[@activeManifestDiff = true]", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(12, entities.size());
    }

    @Test
    public void testDateValue() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService
                .query("ServiceInstance[@_lastmodified > date(1337667919439)]", raptorContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertTrue(entities.size() > 0);
        Date d = new Date(1337667919439l);
        for (IEntity entity : entities) {
            Assert.assertTrue(entity.getLastModified().after(d));
        }
    }

    @Test
    public void testNotGEOperator() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(
                "ServiceInstance[((@activeManifestDiff = true) and not (@_lastmodified >= date(1337667919439)))]",
                raptorContext);
        Date d = new Date(1337667919439l);
        for (IEntity entity : result.getEntities()) {
            Assert.assertFalse(entity.getLastModified().after(d));
        }
    }

    @Test
    public void testNotOperator() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(
                "ServiceInstance[((@activeManifestDiff = true) and (@_lastmodified > date(1337667919439)))]",
                raptorContext);
        Date d = new Date(1337667919439l);
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getLastModified().after(d));
        }
        result = queryService.query(
                "ServiceInstance[not ((@activeManifestDiff = true) and (@_lastmodified > date(1337667919439)))]",
                raptorContext);

        for (IEntity entity : result.getEntities()) {
            Assert.assertFalse(((Boolean) entity.getFieldValues("activeManifestDiff").get(0))
                    && entity.getLastModified().after(d));
        }
    }

    @Test
    public void testReferenceField() {
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query("ServiceInstance[@appService = \"4fbb314fc681caf13e283a76\"]",
                raptorContext);
        Assert.assertEquals(result.getEntities().size(), 10);
        result = queryService.query("ServiceInstance[@runsOn = \"4fbb314fc681caf13e283a7b\"]", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 1);
    }

    @Test
    public void testInherit() {
        // prepare meta
        String repoName = "inheritRepo";
        Repository repo = null;
        try {
            repo = repositoryService.getRepository(repoName);
        } catch (Exception e) {
            //
        }
        if (repo == null) {
            repo = repositoryService.createRepository(new Repository(repoName));

            Branch branch = new Branch();
            branch.setRepositoryName(repo.getRepositoryName());
            branch.setMainBranch(true);
            branch.setId(IBranch.DEFAULT_BRANCH);
            EntityContext context = newEntityContext();
            try {
                branchService.createBranch(branch, context);
            } catch (Exception e) {
                // ignore
                e.printStackTrace();
            }
        }
        IMetadataService ms = repo.getMetadataService();

        MetaClass ma = new MetaClass();
        ma.setName("a");
        ma.setRepository(repoName);
        MetaAttribute fa = new MetaAttribute();
        fa.setName("name");
        ma.addField(fa);
        ms.createMetaClass(ma, new MetadataContext());

        MetaClass mb = new MetaClass();
        mb.setName("b");
        mb.setRepository(repoName);
        mb.setParent("a");
        MetaAttribute fb = new MetaAttribute();
        fb.setName("fb");
        mb.addField(fb);
        ms.createMetaClass(mb, new MetadataContext());

        MetaClass mc = new MetaClass();
        mc.setName("c");
        mc.setRepository(repoName);
        mc.setParent("b");
        MetaAttribute fc = new MetaAttribute();
        fc.setName("fc");
        mc.addField(fc);
        ms.createMetaClass(mc, new MetadataContext());

        // prepare runtime
        IEntityService entityService = ServiceFactory.getEntityService(getDataSource(), repositoryService, TestUtils.getTestDalImplemantation(dataSource));
        EntityContext context = newEntityContext();
        IEntity bE = new JsonEntity(ms.getMetaClass("b"));
        bE.addFieldValue("name", "bE-1");
        bE.addFieldValue("fb", "val-b");
        bE.setBranchId(IBranch.DEFAULT_BRANCH);
        entityService.create(bE, context);
        IEntity cE = new JsonEntity(ms.getMetaClass("c"));
        cE.addFieldValue("name", "cE-1");
        cE.addFieldValue("fc", "val-c");
        cE.setBranchId(IBranch.DEFAULT_BRANCH);
        entityService.create(cE, context);

        QueryContext queryContext = newQueryContext(repoName, RAPTOR_MAIN_BRANCH_ID);
        queryContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("a", queryContext);
        Assert.assertEquals(result.getEntities().size(), 0);

        result = queryService.query("b", queryContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        Assert.assertEquals("bE-1", result.getEntities().get(0).getFieldValues("name").get(0));

        result = queryService.query("c", queryContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        Assert.assertEquals("cE-1", result.getEntities().get(0).getFieldValues("name").get(0));
    }

    @Test(expected = QueryOptimizeException.class)
    public void testRejectFullTableScan() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setSmallTableThreshold(0);
        IQueryResult result = queryService.query("ServiceInstance[@activeManifestDiff = true]", tempContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(10, entities.size());
    }

    @Test
    public void testRejectRegexFullTableScan() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setSysAllowRegexFullScan(false);
        tempContext.setAllowFullTableScan(true);
//        try {
        tempContext.setRequestTrackingCode(null);
            queryService.query("ServiceInstance[@name =~ \"abc\"]", tempContext);
//            Assert.fail();
        Assert.assertEquals(201, tempContext.getRequestTrackingCode().getErrorCode());
//        } catch (QueryOptimizeException qoe) {
//            Assert.assertEquals(qoe.getErrorCode(), QueryErrCodeEnum.REJECT_REGEX_FULL_TABLE_SCAN.getErrorCode());
//        }
//        try {
        tempContext.setRequestTrackingCode(null);
        queryService.query("ServiceInstance[@name =~ \"^abc\"/i]", tempContext);
        Assert.assertEquals(201, tempContext.getRequestTrackingCode().getErrorCode());
//            Assert.fail();
//        } catch (QueryOptimizeException qoe) {
//            Assert.assertEquals(qoe.getErrorCode(), QueryErrCodeEnum.REJECT_REGEX_FULL_TABLE_SCAN.getErrorCode());
//        }
//        try {
        tempContext.setRequestTrackingCode(null);
        queryService.query("ServiceInstance[@name =~ \"abc\"/s]", tempContext);
        Assert.assertEquals(201, tempContext.getRequestTrackingCode().getErrorCode());
//            Assert.fail();
//        } catch (QueryOptimizeException qoe) {
//            Assert.assertEquals(qoe.getErrorCode(), QueryErrCodeEnum.REJECT_REGEX_FULL_TABLE_SCAN.getErrorCode());
//        }
    }

    @Test
    public void testIndexHit() {
        String query = "ApplicationService[@archTier=\"app\"]";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setSmallTableThreshold(0);
        tempContext.setAllowFullTableScan(false);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertTrue(entities.size() > 0);
    }

    @Test
    public void testIndexHit2() {
        String query = "ApplicationService[@name=\"srp-app:Raptor\"]";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(false);
        tempContext.setSmallTableThreshold(0);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertTrue(entities.size() > 0);
    }

    @Test(expected = QueryOptimizeException.class)
    public void testIndexNotHit() {
        String query = "ApplicationService[@activeManifestDiff=false]";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(false);
        tempContext.setSmallTableThreshold(0);

        queryService.query(query, tempContext);
    }

    @Test
    public void testQueryArrayLength() {
        String query = "ApplicationService[@updateStrategies.$_length>1]";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());

        query = "ApplicationService[@updateStrategies.$_length>2]";
        tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        result = queryService.query(query, tempContext);

        entities = result.getEntities();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testQueryProjectionArrayLength() {
        String query = "ApplicationService[@updateStrategies.$_length > 1]{@updateStrategies.$_length}";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());
        JsonEntity entity = (JsonEntity) entities.get(0);
        Assert.assertNotNull(entity.getNode().get("updateStrategies._length"));
        Assert.assertFalse(entity.getNode().get("updateStrategies._length").isNull());

        query = "ApplicationService[@updateStrategies.$_length>2]";
        tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        result = queryService.query(query, tempContext);

        entities = result.getEntities();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testQueryFilterLastModified() {
        String query = "ApplicationService[@updateStrategies.$_lastmodified > date(123)]";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());

        long t = new Date().getTime();
        query = "ApplicationService[@updateStrategies.$_lastmodified > date(" + t + ")]";
        tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        result = queryService.query(query, tempContext);

        entities = result.getEntities();
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void testQueryProjectionLastModified() {
        String query = "ApplicationService[@updateStrategies.$_lastmodified > date(123)] {@updateStrategies.$_lastmodified, @updateStrategies}";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());
        JsonEntity jsonEntity = (JsonEntity) entities.get(0);
        Assert.assertNotNull(jsonEntity.getNode().get("updateStrategies._lastmodified"));
        Assert.assertFalse(jsonEntity.getNode().get("updateStrategies._lastmodified").isNull());
    }

    /**
     * Case for using the field property ON the embed object
     */
    @Test
    public void testQueryEmbedFieldProperty() {
        String query = "Manifest{@versions.$_length, @versions.$_lastmodified}.versions[@name=\"Dummy ManifestVersion Bundle-0-0001\"]{*}";
        QueryContext tempContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());

        JsonEntity manifestEntity = (JsonEntity) entities.get(0);
        Assert.assertNotNull(manifestEntity.getNode().get("versions._lastmodified"));
        Assert.assertFalse(manifestEntity.getNode().get("versions._lastmodified").isNull());
        Assert.assertNotNull(manifestEntity.getNode().get("versions._length"));
        Assert.assertFalse(manifestEntity.getNode().get("versions._length").isNull());
    }

    /**
     * Case for using the field properties of the object that inside the embed
     * object
     */
    @Test
    public void testQueryFieldPropertyInEmbed01() {
        String baseQuery = "Manifest.versions[@name.$_lastmodified > ";
        String query = baseQuery + " date(123)]";
        QueryContext tempContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());

        long time = System.currentTimeMillis();
        // when change the query to find the one with time later then now,
        // should be empty result
        String secondQuery = baseQuery + " date(" + time + ")]";
        IQueryResult secondResult = queryService.query(secondQuery, tempContext);
        Assert.assertEquals(0, secondResult.getEntities().size());
    }

    @Test
    public void testQueryFieldPropertyInEmbed02() {
        String baseQuery = "Manifest{@versions.$_length, @versions.$_lastmodified}.versions[@name=\"Dummy ManifestVersion Bundle-0-0001\" and @name.$_lastmodified > ";
        String query = baseQuery + " date(123)]";
        QueryContext tempContext = newQueryContext(DEPLOY_REPO, SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, tempContext);
        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(1, entities.size());
    }

    @Test
    public void testQueryReferProjectFieldProperty() {
        String query = "ApplicationService{*}.services[@runsOn.$_lastmodified > date(123)] {@runsOn.$_lastmodified}.runsOn{*}";
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query(query, tempContext);

        List<IEntity> entities = result.getEntities();
        Assert.assertEquals(2, entities.size());
        JsonEntity jsonEntity = (JsonEntity) entities.get(0);
        JsonEntity serviceEntity = (JsonEntity) jsonEntity.getFieldValues("services").get(0);

        System.out.println(serviceEntity.getNode());
        Assert.assertNotNull(serviceEntity.getNode().get("runsOn._lastmodified"));
        Assert.assertFalse(serviceEntity.getNode().get("runsOn._lastmodified").isNull());

        Assert.assertNotNull(serviceEntity.getFieldValues("runsOn"));
        System.out.println(serviceEntity);
    }

    @Test
    public void testFieldProperty() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance{@runsOn, @runsOn.$_lastmodified}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(12, result.getEntities().size());
        int noRunsOnCount = 0;
        for (IEntity serv : result.getEntities()) {
            if (!serv.hasField("runsOn")) {
                noRunsOnCount++;
            } else {
                Assert.assertTrue(((JsonEntity) serv).getNode().has("runsOn"));
                Assert.assertTrue(((JsonEntity) serv).getNode().has("runsOn._lastmodified"));
            }
        }
        Assert.assertEquals(1, noRunsOnCount);
    }

    @Test
    public void testQueryContext01() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setDbTimeCost(100);
        Assert.assertEquals(tempContext.getDbTimeCost(), 100);

        tempContext.setTotalTimeCost(20000);
        Assert.assertEquals(tempContext.getTotalTimeCost(), 20000);

        tempContext.setStartProcessingTime(12345);
        Assert.assertEquals(tempContext.getStartProcessingTime(), 12345);

        tempContext.setSourceIP("1.2.3.4");
        Assert.assertEquals(tempContext.getSourceIP(), "1.2.3.4");

        tempContext.setRequestId("abc");
        Assert.assertEquals(tempContext.getRequestId(), "abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryContext02() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setSkips(new int[] { -1 });
    }

    @Test(expected = QueryOptimizeException.class)
    public void testQueryContext06() {
        QueryContext tempContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        tempContext.setLimits(new int[] { SYS_LIMIT_DOCUMENTS_MONGO_QUERY + 1 });
        queryService.query("abc", tempContext);
    }

    @Test
    public void testQueryCount() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setCountOnly(true);
        qContext.setAllowFullTableScan(true);
        IQueryResult qResult = queryService.query("ApplicationService", qContext);
        Assert.assertTrue(qResult.getCount() > 0);
        System.out.println(qResult.getCount());
        Assert.assertTrue(qResult.getEntities() == null || qResult.getEntities().isEmpty());
    }

    @Test
    public void testQueryCount2_withjoin() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setCountOnly(true);
        qContext.setAllowFullTableScan(true);
        try {
            queryService.query("ApplicationService.services", qContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.JOIN_COUNT_NOT_SUPPORT.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void testQueryCount1_count() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        qContext.setCountOnly(true);
        IQueryResult result = queryService.query("<VCluster, VPool>Resource[exists @environment]{*}", qContext);
        Assert.assertEquals(0, result.getEntities().size());
        Assert.assertEquals(15, result.getCount());

//        result = queryService.query("<VCluster>Resource[exists @environment]{*}", qContext);
//        long vClusterCount = result.getCount();
//        Assert.assertEquals(0, result.getEntities().size());
//        result = queryService.query("<VPool>Resource[exists @environment]{*}", qContext);
//        long vPoolCount = result.getCount();
//        Assert.assertEquals(0, result.getEntities().size());
//        Assert.assertEquals(15, vClusterCount + vPoolCount);
    }
    
    @Test
    public void testQueryhasMore() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        qContext.setLimits(new int[] { 1 });
        IQueryResult result = queryService.query("ApplicationService[@_oid=\"4fbb314fc681caf13e283a76\"]", qContext);
        Assert.assertFalse(result.hasMoreResults());
    }

    @Test
    public void testQueryTypeCast00Root() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("<VCluster, VPool>Resource[exists @environment]{*}", qContext);
        Assert.assertEquals(15, result.getEntities().size());
    }

    @Test
    public void testQueryTypeCast01JoinFilter() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);

        IQueryResult result0 = queryService.query("Compute[@_oid=\"101.94.10.20\"].parentCluster", qContext);
        Assert.assertEquals(0, result0.getEntities().size());

        IQueryResult result = queryService.query(
                "Compute[@_oid=\"101.94.10.20\"].<VCluster, VPool>parentCluster[@label= \"SocialCommerce\"]", qContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * Type cast doesn't affect on embed queries
     */
    @Test
    public void testQueryTypeCast02Embed() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(
                "PoolCluster[@_oid=\"dummy-pool-cluster-001\"].<PooledCluster>pools", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
    }

    @Test(expected = QueryParseException.class)
    public void testQueryTypeCast03() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        queryService.query("<Base, VPool>Resource", qContext);
    }

    @Test
    public void testQueryExplanation_typeCast() {
        String query = "<VCluster, VPool>Resource[exists @environment]{*}";
        QueryContext qc = newQueryContext(STRATUS_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNotNull(queryResult.getExplanations());
        List<IQueryExplanation> explans = queryResult.getExplanations();
        Assert.assertEquals(2, explans.size());
        ObjectNode objectNode = (ObjectNode) explans.get(0).getJsonExplanation();
        String queryType0 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();

        objectNode = (ObjectNode) explans.get(1).getJsonExplanation();
        String queryType1 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType0.equals("VCluster"));
        Assert.assertTrue(queryType1.equals("VPool"));
    }

    @Test
    public void testQueryExplanation_typeCast2() {
        String query = "ResourceGroup.<ResourceGroup>children{*}";
        QueryContext qc = newQueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(true);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertNotNull(queryResult.getExplanations());
        List<IQueryExplanation> explans = queryResult.getExplanations();
        Assert.assertEquals(3, explans.size());

        // first two is search explanation, the last one is the join explanation
        ObjectNode objectNode = (ObjectNode) explans.get(0).getJsonExplanation();
        String queryType0 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType0.equals("ResourceGroup"));
        Assert.assertTrue(objectNode.has("usedTime"));
        Assert.assertTrue(objectNode.has("limit"));
        Assert.assertTrue(objectNode.has("sort"));

        objectNode = (ObjectNode) explans.get(1).getJsonExplanation();
        String queryType1 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType1.equals("ResourceGroup"));
    }

    @Ignore
    @Test
    public void testQueryTypeCast04_type() {
        String query = "ResourceGroup.<ResourceGroup, Asset, Rack, ServiceInstance>children{*}";
        QueryContext qc = newQueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(false);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertEquals(3, queryResult.getEntities().size());

        // assertion
        int groupTypeCount = 0;
        int assetTypeCount = 0;
        int rackTypeCount = 0;
        int servTypeCount = 0;
        int resourceTypeCount = 0;
        for (IEntity entity : queryResult.getEntities()) {
            if (entity.getType().equals("ResourceGroup")) {
                groupTypeCount++;
            }
            if (entity.getType().equals("Asset")) {
                assetTypeCount++;
            }
            if (entity.getType().equals("Rack")) {
                rackTypeCount++;
            }
            if (entity.getType().equals("ServiceInstance")) {
                servTypeCount++;
            }
            if (entity.getType().equals("Resource")) {
                resourceTypeCount++;
            }
        }
        Assert.assertEquals(1, groupTypeCount);
        Assert.assertEquals(1, assetTypeCount);
        Assert.assertEquals(1, rackTypeCount);
        Assert.assertEquals(0, servTypeCount);
        Assert.assertEquals(0, resourceTypeCount);
    }

    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public void testQueryTypeCast04_type2() {
        String query = "ResourceGroup{*}";
        QueryContext qc = newQueryContext(CMSDB_REPO, IBranch.DEFAULT_BRANCH);
        qc.setAllowFullTableScan(true);
        qc.setExplain(false);
        IQueryResult queryResult = queryService.query(query, qc);
        Assert.assertEquals(1, queryResult.getEntities().size());
        IEntity resourceGroup = queryResult.getEntities().get(0);

        // assertion
        int groupTypeCount = 0;
        int assetTypeCount = 0;
        int rackTypeCount = 0;
        int servTypeCount = 0;
        int resourceTypeCount = 0;
        List<IEntity> children = (List<IEntity>) resourceGroup.getFieldValues("children");
        for (IEntity entity : children) {
            if (entity.getType().equals("ResourceGroup")) {
                groupTypeCount++;
            }
            if (entity.getType().equals("Asset")) {
                assetTypeCount++;
            }
            if (entity.getType().equals("Rack")) {
                rackTypeCount++;
            }
            if (entity.getType().equals("ServiceInstance")) {
                servTypeCount++;
            }
            if (entity.getType().equals("Resource")) {
                resourceTypeCount++;
            }
        }
        Assert.assertEquals(0, groupTypeCount);
        Assert.assertEquals(1, assetTypeCount);
        Assert.assertEquals(0, rackTypeCount);
        Assert.assertEquals(0, servTypeCount);
        Assert.assertEquals(3, resourceTypeCount);
    }

    /**
     * CMS-2621 : query on indexes that built index on parent
     */
    @Test
    public void testQueryBasedOnParentIndex() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("VPool[@label=\"SocialCommerce\"]{*}", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
    }

    @Test
    public void testQueryBasedOnReferenceIndex() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(false);
        qContext.setSmallTableThreshold(0);
        IQueryResult result = queryService.query("Environment[@applications=\"4fbb314fc681caf13e283a76\"]", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> apps = result.getEntities();
        Assert.assertEquals(1, apps.size());
    }

    // @Test
    // public void testQueryNestedRef01(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result =
    // queryService.query("Group[@_oid=\"group01\"]{*}.customer{*}", qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(1, groups.size());
    // }
    //
    // @Test
    // public void testQueryNestedRef02(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result = queryService.query("Group{*}.customer{*}",
    // qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(8, groups.size());
    //
    // for (IEntity entity : groups) {
    // if (entity.getId().equals("group02")) {
    // List<?> list = entity.getFieldValues("customer");
    // Assert.assertEquals(2, list.size());
    // } else if (entity.getId().equals("group01")) {
    // List<?> list = entity.getFieldValues("customer");
    // Assert.assertEquals(1, list.size());
    // IEntity customer = (IEntity) list.get(0);
    // Assert.assertEquals("Dep!dep000!team!team010!person!person012",
    // customer.getId());
    // }
    // }
    // }
    //
    // @Test
    // public void testQueryNestedRef03(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result =
    // queryService.query("Group{*}.customer[@_oid=\"Dep!dep000!team!team020!person!person022\"]{*}",
    // qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(1, groups.size());
    // }
    //
    // @Test
    // public void testQueryNestedRef04(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result =
    // queryService.query("Group{*}.customer[@_oid=\"Dep!dep000!team!team010!person!person012\"]{*}",
    // qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(2, groups.size());
    //
    // for(IEntity entity : groups){
    // List<?> list = entity.getFieldValues("customer");
    // Assert.assertEquals(1, list.size());
    // }
    // }
    //
    // @Test
    // public void testQueryNestedRef05(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result =
    // queryService.query("Group{*}.customer[@_oid=\"Dep!dep000!team!team710!person!person012\"]{*}",
    // qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(0, groups.size());
    // }
    //
    // @Test
    // public void testQueryNestedRef06(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result =
    // queryService.query("Group[@_oid=\"group02\"].customer{*}", qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(2, groups.size());
    // }
    //
    // @SuppressWarnings("rawtypes")
    // @Test
    // public void testQueryNestedRef07(){
    // QueryContext qContext = newQueryContext(RAPTOR_REPO,
    // RAPTOR_MAIN_BRANCH_ID);
    // qContext.setAllowFullTableScan(true);
    // IQueryResult result = queryService.query("Group.customer{@name}",
    // qContext);
    // Assert.assertNotNull(result);
    // Assert.assertNotNull(result.getEntities());
    // List<IEntity> groups = result.getEntities();
    // Assert.assertEquals(17, groups.size());
    //
    // for(IEntity entity : groups){
    // List list = entity.getFieldValues("age");
    // Assert.assertEquals(0, list.size());
    // list = entity.getFieldValues("name");
    // Assert.assertEquals(1, list.size());
    // }
    // }
    //
    // @Test
    // public void testQueryNestedRef08() {
    // String queryStr = "Group.customer[@name=~\"dev-0[24]\"]";
    // raptorContext.setHint(1);
    // IQueryResult result = queryService.query(queryStr, raptorContext);
    // Assert.assertNotNull(result.getEntities());
    // Assert.assertTrue(result.getEntities().size() == 3);
    // for (IEntity entity : result.getEntities()) {
    // Assert.assertEquals("Person", entity.getType());
    // Assert.assertTrue(entity.getFieldNames().size() > 2);
    // }
    // }
    //
    // @Test
    // public void testQueryNestedRef09() {
    // String queryStr = "Leader.head[@name=~\"dev-team-010\"]";
    // raptorContext.setHint(1);
    // IQueryResult result = queryService.query(queryStr, raptorContext);
    // Assert.assertNotNull(result.getEntities());
    // Assert.assertTrue(result.getEntities().size() == 3);
    // for (IEntity entity : result.getEntities()) {
    // Assert.assertEquals("Team", entity.getType());
    // Assert.assertTrue(entity.getFieldNames().size() > 2);
    // }
    // }

    public void testQuery_nonIdentifierField() {
        QueryContext qContext = newQueryContext(STRATUS_REPO, STRATUS_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);

        IQueryResult result = queryService.query("NetworkAddress[@802Address=~\"00:34:35:ae:.*\"]{*}", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(2, versions.size());
    }

    @Test
    public void testQueryIsNull() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = null;
        result = queryService.query("Room[ isnull @floor]", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());

        result = queryService.query("Room[not isnull @floor]", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
    }

    private void prepareEmptyArray() {
        EntityContext context = newEntityContext();

        MetaClass roomMetadata = repositoryService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass("Room");
        JsonEntity modifyEntity = new JsonEntity(roomMetadata);
        modifyEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        modifyEntity.setId("room09");
        modifyEntity.addFieldValue("window", "aaa");
        entityService.pullField(modifyEntity, modifyEntity, "window", context);
    }

    @Test
    public void testQueryIsEmptyArray() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = null;

        result = queryService.query("Room[isempty @window]", qContext);
        Assert.assertEquals(2, result.getEntities().size());

        result = queryService.query("Room[not isempty @window]", qContext);
        Assert.assertEquals(2, result.getEntities().size());

        result = queryService.query("Room[not isnull @window]", qContext);
        Assert.assertEquals(4, result.getEntities().size());

        prepareEmptyArray();

        result = queryService.query("Room[ isempty @window]", qContext);
        Assert.assertEquals(3, result.getEntities().size());

        result = queryService.query("Room[not isempty @window]", qContext);
        Assert.assertEquals(result.getEntities().size(), 1);

        result = queryService.query("Room[not isnull @window]", qContext);
        Assert.assertEquals(4, result.getEntities().size());
    }

    @Test
    public void testQueryIsEmptyReferences() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = null;

        result = queryService.query("Room[ isempty @path]", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());

        result = queryService.query("Room[not isempty @path]", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(4, result.getEntities().size());
    }

    @Test
    public void testQueryIsNullEmbededProp() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = null;

        result = queryService.query("Dep.team.person[isnull @age]{*}", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(0, result.getEntities().size());

        result = queryService.query("Dep.team.person[not isnull @age]{*}", qContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(2, result.getEntities().size());
    }

    private void prepareEmtpyEmbededArray() {
        EntityContext context = newEntityContext();

        MetaClass roomMetadata = repositoryService.getRepository(RAPTOR_REPO).getMetadataService()
                .getMetaClass("Person");
        JsonEntity modifyEntity = new JsonEntity(roomMetadata);
        modifyEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        modifyEntity.setId("Dep!dep005!team!team510!person!person511");
        modifyEntity.addFieldValue("address", "ddd");
        entityService.pullField(modifyEntity, modifyEntity, "address", context);
    }

    @Test
    public void testQueryIsEmptyEmbededArray() {
        QueryContext qContext = newQueryContext(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID);
        qContext.setAllowFullTableScan(true);
        IQueryResult result = null;

        result = queryService.query("Dep.team.person[isempty @address]{*}", qContext);
        Assert.assertEquals(1, result.getEntities().size());

        result = queryService.query("Dep.team.person[not isempty @address]{*}", qContext);
        Assert.assertEquals(14, result.getEntities().size());

        result = queryService.query("Dep.team.person[not isnull @address]{*}", qContext);
        Assert.assertEquals(15, result.getEntities().size());

        prepareEmtpyEmbededArray();

        result = queryService.query("Dep.team.person[isempty @address]{*}", qContext);
        Assert.assertEquals(2, result.getEntities().size());

        result = queryService.query("Dep.team.person[not isempty @address]{*}", qContext);
        Assert.assertEquals(13, result.getEntities().size());

        result = queryService.query("Dep.team.person[not isnull @address]{*}", qContext);
        Assert.assertEquals(15, result.getEntities().size());
    }

    @Test
    public void testCollectionCount() {
        PersistenceContext pContext = newPersistenceContext(raptorMetaService);
        MetaClass depClass = new MetaClass();
        depClass.setName("Dep");
        MetaClass roomClass = new MetaClass();
        roomClass.setName("Room");
        int depCount = raptorMetaService.getCollectionCount(pContext.getDBCollectionName(depClass));
        int roomCount = raptorMetaService.getCollectionCount(pContext.getDBCollectionName(roomClass));
        Assert.assertEquals(13, depCount);
        Assert.assertEquals(9, roomCount);

        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "Room", "room06");
        entityService.delete(qEntity, context);

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            ;
        }
        roomCount = raptorMetaService.getCollectionCount(pContext.getDBCollectionName(roomClass));
        Assert.assertEquals(9, roomCount); // cache
    }

    protected static PersistenceContext newPersistenceContext(IMetadataService metaService) {
        PersistenceContext pContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), "main");
        pContext.setDbConfig(dbConfig);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
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
    public void testEmbeddedQuery_index01() {
        deployContext.setAllowFullTableScan(false);
        try {
            queryService
                    .query("Manifest.versions[@description=\"Dummy ManifestVersion Bundle-0-0001\"]", deployContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            //
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
    }

    @Test
    public void testEmbeddedQuery_index02() {
        deployContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query("Manifest.versions[@name=\"Dummy ManifestVersion Bundle-0-0001\"]",
                deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
        for (IEntity v : versions) {
            Assert.assertNotNull(v);
            System.out.println(v.getFieldValues("name").get(0));
            Assert.assertEquals("Dummy ManifestVersion Bundle-0-0001", v.getFieldValues("name").get(0));
        }
    }

    @Test
    public void testEmbeddedQuery_index03() {
        deployContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query(
                "Manifest[@versions=\"Manifest!4fbdaccec681643199735a5b!versions!4fbdaccec681643199735a5c\"]",
                deployContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        List<IEntity> versions = result.getEntities();
        Assert.assertEquals(1, versions.size());
        for (IEntity v : versions) {
            Assert.assertNotNull(v);
            System.out.println(v.getFieldValues("name").get(0));
            Assert.assertEquals("Dummy Manifest Bundle-0-0001", v.getFieldValues("name").get(0));
        }
    }

    @Test
    public void testEmbeddedQuery_index04() {
        cmsdbContext.setAllowFullTableScan(false);
        String queryString = "AssetServer.networkControllers[@resourceId=\"101.89.14.146:tivoli\"]";
        queryService.query(queryString, cmsdbContext);

        try {
            queryString = "AssetServer.networkControllers[@allocatedIP=\"101.89.14.146:tivoli\"]";
            queryService.query(queryString, cmsdbContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
    }

    @Test
    public void testEmbeddedQuery_index05() {
        raptorContext.setAllowFullTableScan(false);
        String queryString = "Dep.team.person[@_oid=\"Dep!dep000!team!team010!person!person011\"]";
        IQueryResult result = queryService.query(queryString, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());

        try {
            queryString = "Dep.team.person[@name=\"dev-01\"]";
            queryService.query(queryString, raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
    }

    @Test
    public void testEmbeddedQuery_index06() {
        raptorContext.setAllowFullTableScan(false);
        String queryString = "Dep.team[@person=\"Dep!dep000!team!team020!person!person021\"]";
        IQueryResult result = queryService.query(queryString, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test(expected = QueryOptimizeException.class)
    public void testEmbeddedQuery_index07() {
        raptorContext.setAllowFullTableScan(false);
        String queryString = "Dep.team[@person.$_length > 1]";
        IQueryResult result = queryService.query(queryString, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(10, result.getEntities().size());
    }

    @Test
    public void testEmbeddedQuery_index08() {
        raptorContext.setAllowFullTableScan(false);
        MetaClass depMeta = raptorMetaService.getMetaClass("Dep");
        boolean depAllow = depMeta.isAllowFullTableScan();
        depMeta.setAllowFullTableScan(false);
        MetaClass teamMeta = raptorMetaService.getMetaClass("Team");
        boolean teamAllow = teamMeta.isAllowFullTableScan();
        teamMeta.setAllowFullTableScan(true);

        String queryString = "Dep.team";
        try {
            queryService.query(queryString, raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException e) {
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), e.getErrorCode());
        }

        depMeta.setAllowFullTableScan(depAllow);
        teamMeta.setAllowFullTableScan(teamAllow);
    }

    /**
     * CMS-3148, CMS-3302
     */
    @Test
    public void testHint() {
        EntityContext context = newEntityContext();
        // create network address
        MetaClass naClass = cmsdbMetaService.getMetaClass("NetworkAddress");
        JsonEntity naEntity = null;
        final Integer LIMIT = SearchOption.DEFAULT_LIMIT;
        for (int i = 0; i < LIMIT + 1; i++) {
            naEntity = new JsonEntity(naClass);
            naEntity.setBranchId(CMSDB_MAIN_BRANCH_ID);
            naEntity.addFieldValue("resourceId", "101.19.53." + i);
            naEntity.addFieldValue("address", "101.19.53." + i);
            naEntity.addFieldValue("ipVersion", "IPv4");
            String id = entityService.create(naEntity, context);
            naEntity.setId(id);
        }

        // create fqdn
        String value = "anonymous.ebay.com";
        MetaClass fqdnClass = cmsdbMetaService.getMetaClass("FQDN");

        JsonEntity fqdnEntity = null;
        for (int i = 0; i < LIMIT + 1; i++) {
            fqdnEntity = new JsonEntity(fqdnClass);
            fqdnEntity.setBranchId(CMSDB_MAIN_BRANCH_ID);
            fqdnEntity.addFieldValue("fqdn", value);
            fqdnEntity.addFieldValue("resourceId", value + i);
            String id = entityService.create(fqdnEntity, context);
            fqdnEntity.setId(id);
        }

        // create DNS record
        MetaClass dnsR = cmsdbMetaService.getMetaClass("DNSRecord");
        JsonEntity entity = new JsonEntity(dnsR);
        entity.addFieldValue("resourceId", value);
        entity.addFieldValue("type", "A");
        entity.addFieldValue("networkAddress", naEntity);
        entity.addFieldValue("fqdn", fqdnEntity);
        entity.setBranchId(CMSDB_MAIN_BRANCH_ID);
        entityService.create(entity, context);

        // cmsdbContext.setAllowFullTableScan(false);

        String str;
        IQueryResult result;
        str = "DNSRecord[(@resourceId=~\"^anonymous.ebay.com.*\"/s) and (@type=\"A\")].networkAddress";
        cmsdbContext.setHint(0);
        result = queryService.query(str, cmsdbContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertTrue(result.getEntities().size() == 1);

        cmsdbContext.setHint(1);
        cmsdbContext.setAllowFullTableScan(true);
        result = queryService.query(str, cmsdbContext);
        Assert.assertTrue(result.hasMoreResults());
        Assert.assertTrue(result.getEntities().size() == 0);
        // by default must work.
        cmsdbContext.setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        result = queryService.query(str, cmsdbContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertTrue(result.getEntities().size() == 1);

        // case 2: union
        str = "DNSRecord[(@resourceId=~\"^anonymous.ebay.com.*\"/s) and (@type=\"A\")]."
                + "(fqdn[@resourceId=\""
                + fqdnEntity.getFieldValues("resourceId").get(0)
                + "\"] "
                + "|| networkAddress{*} )";
        cmsdbContext.setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        result = queryService.query(str, cmsdbContext);
        Assert.assertFalse(result.hasMoreResults());

        // case 2.1: intersection
        str = "DNSRecord[(@resourceId=~\"^anonymous.ebay.com.*\"/s) and (@type=\"A\")]."
                + "(fqdn[@resourceId=\""
                + fqdnEntity.getFieldValues("resourceId").get(0)
                + "\"] "
                + "&& networkAddress{*} )";
        cmsdbContext.setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        result = queryService.query(str, cmsdbContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertEquals(1, result.getEntities().size());

        // case 3: CMS-3302
        String naAddress = (String) naEntity.getFieldValues("address").get(0);
        str = "DNSRecord[(@networkAddress =& NetworkAddress[@address=\"" + naAddress + "\"])and @type=\"A\"].fqdn{*}";
        cmsdbContext.setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        result = queryService.query(str, cmsdbContext);
        Assert.assertFalse(result.hasMoreResults());
        Assert.assertTrue(result.getEntities().size() == 1);
    }

    /**
     * From Brian: Bson expression calculated value always be double, but the
     * "available" field is defined as long. This cause the query comparison
     * failed.
     */
    @Test
    public void testNumberTypeCompare_inEmbed() {
        String query = "LBService[@resourceId in (\"192.168.3.1\") ]{@resourceId}.capacities[@available > 50]{@used,@available}";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertTrue(result.getEntities().size() == 1);
    }

    @Test
    public void testQuery_invalid() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "AssetServer{*}[@type=\"vm\"].networkControllers";
        try {
            queryService.query(query, cmsdbContext);
            Assert.fail();
        } catch (QueryParseException qpe) {
            // expected
            System.out.println(qpe.getMessage());
            Assert.assertEquals(QueryErrCodeEnum.PARSE_GRAMMER_ERROR.getErrorCode(), qpe.getErrorCode());
        }
    }

    @Test
    public void testNullData() {
        // case 0 : comparator
        cmsdbContext.setAllowFullTableScan(false);
        String query = null;
        IQueryResult result  = null;
        query =  "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[@ifIndex > 0 or @_status=\"active\"]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(2, result.getEntities().size());

        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[@ifIndex > 0]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());

        // case 1 : equality
        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[@ifIndex = 1]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());

        // case 2 : exists :: [[[ null for non-string type would be treated as not-existing ]]]
        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[exists @ifIndex]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());

        // case 3 : in set
        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[@ifIndex in (1, 0)]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());

        // case 4 : reg expr : null doesn't match the reg expr
        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[@ifdescr=~\".*\"]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(2, result.getEntities().size());

        // case 5 : isnull
        cmsdbContext.setAllowFullTableScan(false);
        query = "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[isnull @ifdescr]{@ifIndex, @resourceId, @_creator, @_modifier}";
        result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(0, result.getEntities().size());

        // case 5 : isempty
        // deployContext.setAllowFullTableScan(false);
        // query =
        // "AssetServer[@_oid=\"51f977a4171b7e36601ad3f0\"].networkControllers[isempty @ifdescr]{@ifIndex, @resourceId, @_creator, @_modifier}";
        // result = queryService.query(query, deployContext);
        // Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testNotExistsData(){
        raptorContext.setAllowFullTableScan(false);
        String queryString = "Dep[@_oid=\"dep006\"]{*}.team[not exists @seat]{*}";
        IQueryResult result = queryService.query(queryString, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        
        IEntity entityGet = (IEntity) result.getEntities().get(0);
        List<IEntity> list = (List<IEntity>) entityGet.getFieldValues("team");
        Assert.assertEquals(1, list.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExistsData(){
        raptorContext.setAllowFullTableScan(false);
        String queryString = "Dep[@_oid=\"dep006\"]{*}.team[exists @seat]{*}";
        IQueryResult result = queryService.query(queryString, raptorContext);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        
        IEntity entityGet = (IEntity) result.getEntities().get(0);
        List<IEntity> list = (List<IEntity>) entityGet.getFieldValues("team");
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testQueryExplanation() {
        String query = "NodeServer[@resourceId>\"10.14\" and @resourceId<\"10.15\"]{*}.(nodeServer!AssetServer.asset && ntpServers )";
        cmsdbContext.getCursor().setSkips(null);
        cmsdbContext.getCursor().setLimits(null);
        cmsdbContext.getCursor().setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        cmsdbContext.setExplain(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertNotNull(result.getExplanations());
        Assert.assertTrue(result.getExplanations().get(0) instanceof QueryExplanation);
    }

    @Test
    public void testEmbedQueryExplanation() {
        String query = "NodeServer[@resourceId>\"10\\.7\" and @resourceId<\"10\\.15\"]{*}.capacities[@type=\"cpu\"]{*}";
        cmsdbContext.getCursor().setSkips(null);
        cmsdbContext.getCursor().setLimits(null);
        cmsdbContext.getCursor().setHint(-1);
        cmsdbContext.setAllowFullTableScan(false);
        cmsdbContext.setExplain(true);
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertNotNull(result.getExplanations());
        Assert.assertTrue(result.getExplanations().get(0) instanceof QueryExplanation);
    }
    
    @Test(expected = QueryOptimizeException.class)
    public void testExceedSystemQueryLimits() {
    	Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
    	Map<String, Object> clonedCurrentConfig = new HashMap<String, Object>(currentConfig);
    	Map<String, Object> modConfig = new HashMap<String, Object>();
        modConfig.put(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY, 10);
        dbConfig.updateConfig(modConfig);
        
    	int[] limits = {20};
        raptorContext.setLimits(limits);
        try	{
        	queryService.query("Environment[@_oid=\"4fbb314fc681caf13e283a78\"]", raptorContext);
        } catch(QueryOptimizeException r) {
        	throw r;
        } finally {
            dbConfig.updateConfig(clonedCurrentConfig);
        }
    }

    @Test
    public void testGetQueryMetaClass() {
        String query = "Environment[@_oid =& Compute.runsOn!ServiceInstance.services!ApplicationService].applications[@label=\"\"].(services && updateStrategies)";
        Map<String, MetaClass> metadatas = queryService.getQueryMetaClass(query, raptorContext);
        Assert.assertEquals(5, metadatas.size());
        Assert.assertTrue(metadatas.containsKey("Environment"));
        Assert.assertTrue(metadatas.containsKey("Compute"));
        Assert.assertTrue(metadatas.containsKey("ServiceInstance"));
        Assert.assertTrue(metadatas.containsKey("ApplicationService"));
        Assert.assertTrue(metadatas.containsKey("UpdateStrategy"));
    }
    
    @Test
    public void testQueryWithTypeCast() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "ResourceGroup{@resourceId}.<NodeServer>manager{*}";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testQueryEmptyArray() {
        raptorContext.setAllowFullTableScan(false);
        IQueryResult result = queryService.query("Room[@_oid=\"room07\" and not @window=\"aaa\"]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("Room[@_oid=\"room07\" and not @window in (\"aaa\")]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testQueryEmbedWithEmptyArray() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query("AllowFullTableScanParentTest.embed2[exists @labels]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("AllowFullTableScanParentTest.embed2[not exists @labels]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("AllowFullTableScanParentTest.embed2[isempty @labels]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("AllowFullTableScanParentTest.embed2[not isempty @labels]", raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
        
        result = queryService.query("AllowFullTableScanParentTest.embed2[isempty @name]", raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
        
    }
    
    @Test
    public void testQueryEmbedWithArray() {
        raptorContext.setAllowFullTableScan(true);
        IQueryResult result = null;
        
        result = queryService.query("Dep[exists @team]{@name}", raptorContext);
        Assert.assertEquals(12, result.getEntities().size());
        
        result = queryService.query("Dep[not exists @team]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());

        result = queryService.query("Dep[isempty @team]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("Dep[not isempty @team]", raptorContext);
        Assert.assertEquals(11, result.getEntities().size());
        
        result = queryService.query("Dep[isnull @team]", raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
        
        result = queryService.query("Dep[not isnull @team]", raptorContext);
        Assert.assertEquals(12, result.getEntities().size());
        
        result = queryService.query("Dep[@_oid=\"dep011\"].team[isempty @person]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        result = queryService.query("Dep[@_oid=\"dep011\"].team[not isempty @person]", raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
                
        result = queryService.query("Dep.team[@person in (\"Dep!dep000!team!team010!person!person011\")]", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());

        result = queryService.query("Dep[@_oid=\"dep000\"].team[@person!=\"Dep!dep000!team!team010!person!person011\"]", raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
    }
    
    @Test
    public void testQueryWithEmptyList() {
        cmsdbContext.setAllowFullTableScan(true);
        String queryStr = "NetworkAddress.networkAddress!LBVirtualIP.(poolMaps || vip!AccessPoint)";
        IQueryResult result = queryService.query(queryStr, cmsdbContext);
        Assert.assertEquals(3, result.getEntities().size());

        IEntity queryEntity = buildQueryEntity(CMSDB_REPO, RAPTOR_MAIN_BRANCH_ID, "LBPoolMap", "52d3f0926ec8516d8026ba1b");
        EntityContext context = newEntityContext();
        entityService.delete(queryEntity, context);
        
        result = queryService.query(queryStr, cmsdbContext);
        Assert.assertEquals(2, result.getEntities().size());
    }

}
