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

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.parser.QueryParseException;

/**
 * @author liasu
 * 
 */
public class SetQueryServiceTest extends MongoBaseTest {

    /**
     * A.(b || c)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUnion() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService{*}.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertTrue(services.size() > 0);
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() > 0);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(entity);
    }

    /**
     * A.(b && c)
     */
    @Test
    public void testIntersection() {
        String query = "ApplicationService.(services[@name=~\"srp-app.*\"] && updateStrategies[@name=\"1-100-novalid\"])";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    /**
     * A.(b && c)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testIntersection2() {
        String query = "ApplicationService{*}.(services[@name=~\"srp-app.*\"]{*} && updateStrategies[@name=\"1-25-50-100\" or @name=\"1-100\"]{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());

        IEntity entity = result.getEntities().get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertTrue(services.size() > 0);
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() > 0);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(entity);
    }

    @Test
    public void testIntersection3() {
        String query = "ServiceInstance{*}.(appService.(services || updateStrategies) && runsOn[@_oid=\"4fbb314fc681caf13e283a7b\"]{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testIntersection4() {
        String query = "ServiceInstance{*}.(appService && runsOn[@_oid=\"4fbb314fc681caf13e283a7b\"]{*})";
//        raptorContext.setHint(1);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        raptorContext.setHint(0);
    }

    /**
     * A.b.(c && d)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDeepIntersection() {
        String query = "Environment{*}.applications{*}.(services[@name=~\"srp-app.*\"]{*} && updateStrategies[@name=\"1-25-50-100\" or @name=\"1-100\"]{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertEquals(1, result.getEntities().get(0).getFieldValues("applications").size());
        IEntity entity = (IEntity) result.getEntities().get(0).getFieldValues("applications").get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertTrue(services.size() > 0);
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() > 0);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(entity);
    }

    /**
     * A.b.(c || d)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testDeepUnion() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment{*}.applications{*}.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);

        Assert.assertEquals(1, result.getEntities().get(0).getFieldValues("applications").size());
        IEntity appEntity = (IEntity) result.getEntities().get(0).getFieldValues("applications").get(0);
        // at least _oid, _type, services, updateStrategies
        Assert.assertTrue(appEntity.getFieldNames().size() > 4);

        List<IEntity> services = (List<IEntity>) appEntity.getFieldValues("services");
        Assert.assertEquals(10, services.size());
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) appEntity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() == 2);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(appEntity);
    }

    /**
     * A.(c || d || e)
     */
    @Test
    public void testMultipleUnion() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}.(parentCluster[@_oid=\"CLguf6sdle\"] || activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"] || activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testMultipleUnion02() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}.(parentCluster[@_oid=\"CLguf6sdle-invalid\"] || activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0-unknown\"] || activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0-unknown\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testMultipleUnion03() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}." +
        		"( <VPool>parentCluster[@_oid=\"CLguf6sdle\"] " +
        		"|| activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0-unknown\"] " +
        		"|| activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0-unknown\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * A.(c && d && e)
     */
    @Test
    public void testMultipleIntersection() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}." +
        		"( <VPool>parentCluster[@_oid=\"CLguf6sdle\"] " +
        		"&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"] " +
        		"&& activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    /**
     * A.(c && d && e)
     */
    @Test
    public void testMultipleIntersection2() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}." +
        		"( <VPool>parentCluster[@_oid=\"CLguf6sdle-invalid\"] " +
        		"&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"] " +
        		"&& activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    /**
     * A.(b && c || d)
     */
    @Test
    public void testUnionAdjentIntersection() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle\"] "
                + "&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]"
                + "|| activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * A.(b && c || d)
     */
    @Test
    public void testUnionAdjentIntersection1() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle-invalid\"] "
                + "&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]"
                + "|| activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * A.(b && c || d) -- project on no result path
     */
    @Test
    public void testUnionAdjentIntersection1_invalid_projection() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle-invalid\"]{*} "
                + "&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]"
                + "|| activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    /**
     * A.(b || c && d)
     * 
     * Note for the projection: 
     * b, c, d should all be projection, no miss
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUnionAdjentIntersection2() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle\"]{*} "
                + "|| activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]{*} "
                + "&& activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"] {*})";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());

        IEntity entity = result.getEntities().get(0);
        List<IEntity> clusters = (List<IEntity>)entity.getFieldValues("parentCluster");
        Assert.assertTrue(clusters.size() > 0);
        for (IEntity ent :clusters) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }

        List<IEntity> activeManifestRefs = (List<IEntity>)entity.getFieldValues("activeManifestRef");
        Assert.assertTrue(activeManifestRefs.size() > 0);
        for (IEntity ent :activeManifestRefs) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }

        List<IEntity> activeManifestCurs = (List<IEntity>)entity.getFieldValues("activeManifestCur");
        Assert.assertTrue(activeManifestCurs.size() > 0);
        for (IEntity ent :activeManifestCurs) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }
    }
    
    /**
     * A.(b[invalid filter] || c && d)
     */
    @Test
    public void testUnionAdjentIntersection3() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle-invalid\"] "
                + "|| activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"] "
                + "&& activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    /**
     * 
     * A.b.(c.d || e)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testJoinInSet() {
        String query = "Environment{*}.applications{*}.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] || updateStrategies{*})";
        QueryContext context = newQueryContext(raptorContext);
        context.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertEquals(1, result.getEntities().get(0).getFieldValues("applications").size());
        IEntity entity = (IEntity) result.getEntities().get(0).getFieldValues("applications").get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertTrue(services.size() > 0);
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() > 0);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(entity);
    }

    @Test
    public void testJoinInSet2() {
        String query = "Environment{*}.applications{*}.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=\"compute\"] && updateStrategies{*})";
        QueryContext context = newQueryContext(raptorContext);
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(0, result.getEntities().size());
    }

    /**
     * A.b.(c.d && e.f)
     */
    @Test
    public void testMultiJoinInset() {

    }

    /**
     * A.(b.(c.d && e) && f)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testMultiLevelSet() {
        String query = "Environment.( " + "applications{*}."
                + " (  services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] " + " && updateStrategies{*}) "
                + "&&" + " cos[@_oid=\"4fbb314fc681caf13e283a77\"] " + ")";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            System.out.println(entity);
            Assert.assertEquals("ApplicationService", entity.getType());
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
            Assert.assertTrue(!services.isEmpty());
            for (IEntity servEntity : services) {
                Assert.assertTrue(entity.getFieldNames().size() > 2);
                Assert.assertEquals("ServiceInstance", servEntity.getType());
            }

            List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
            Assert.assertTrue(!strategies.isEmpty());
            for (IEntity strategy : strategies) {
                Assert.assertTrue(entity.getFieldNames().size() > 2);
                Assert.assertEquals("UpdateStrategy", strategy.getType());
            }
        }
    }

    /**
     * A.(b.(c.d && e) && f)
     */
    @Test
    public void testMultiLevelSet2() {
        String query = "Environment.( " + "applications{*}."
                + " (  services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] " + " && updateStrategies{*}) "
                + "&&" + " cos[@_oid=\"4fbb314fc681caf13e283a77-invalid\"] " + ")";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    /**
     * A.(b.(c.d || e) && f)
     */
    @Test
    public void testMultiLevelSet3() {
        String query = "Environment.( " + "applications{*}."
                + " ( services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] " + " || " + " updateStrategies{*} ) "
                + "&&" + " cos[@_oid=\"4fbb314fc681caf13e283a77\"] " + ")";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    /**
     * A.(b.(c.d || e) && f)
     */
    @Test
    public void testMultiLevelSet4() {
        String query = "Environment.( " + "applications{*}."
                + " ( services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] " + " || " + " updateStrategies{*} ) "
                + "&&" + " cos[@_oid=\"4fbb314fc681caf13e283a77-valid\"] " + ")";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    /**
     * A.(b.(c.d && e) || f)
     */
    @Test
    public void testMultiLevelSet5() {
        String query = "Environment.( " + "applications{*}."
                + " ( services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute-invalid.*\"] " + " && " + " updateStrategies{*} ) "
                + "&&" + " cos[@_oid=\"4fbb314fc681caf13e283a77\"] " + ")";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    
    /**
     * A.((b || c) && d)
     * 
     * Note for the projection: 
     * b, c, d should all be projection, no miss
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testMultiLevelSet6() {
        String query = "Compute[@fqdns=\"slc4-0003.ebay.com\"]{*}."
                + "( (<VPool>parentCluster[@_oid=\"CLguf6sdle\"]{*} "
                + "|| activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]{*}) "
                + "&& activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"] {*})";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());

        IEntity entity = result.getEntities().get(0);
        List<IEntity> clusters = (List<IEntity>)entity.getFieldValues("parentCluster");
        Assert.assertTrue(clusters.size() > 0);
        for (IEntity ent :clusters) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }

        List<IEntity> activeManifestRefs = (List<IEntity>)entity.getFieldValues("activeManifestRef");
        Assert.assertTrue(activeManifestRefs.size() > 0);
        for (IEntity ent :activeManifestRefs) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }

        List<IEntity> activeManifestCurs = (List<IEntity>)entity.getFieldValues("activeManifestCur");
        Assert.assertTrue(activeManifestCurs.size() > 0);
        for (IEntity ent :activeManifestCurs) {
            Assert.assertTrue(ent.getFieldNames().size() > 2);
            System.out.println(ent.getFieldNames());
        }
    }
    
    /**
     * b is referenced in A; c and d are embed in b.
     * 
     * A.b.(c && d)
     * 
     */
    @Test
    public void testEmbed() {

    }

    /**
     * b is embed in a; c and d are embed in b.
     * 
     * A.b.(c && d)
     */
    @Test
    public void testEmbed2() {

    }

    /**
     * b is embed in a; c and d are embed in b.
     * 
     * A.b.(c.(d&&e) && f.(g||h))
     */
    @Test
    public void testEmbed3() {

    }

    /**
     * 
     * Manifest.versions.(approval[] || packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed4() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]{*}."
                + "("
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + "|| "
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageVersion Bundle.*\"]{*}"
                + ")";
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);
    }
    
    /**
     * 
     * Manifest.versions.(approval[] || packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed5() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]." + "("
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] " + "|| "
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageInvalid.*\"]{*}"
                + ")";
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);
    }
    
    /**
     * 
     * Manifest.versions.(approval[] || packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed5_reverseOrder() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]." + "("
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageInvalid.*\"]{*}" + "|| "
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + ")";
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);
    }

    /**
     * 
     * Manifest.versions.(approval[] && packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed6() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]{*}."
                + "("
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + "&& "
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageVersion Bundle.*\"]{*}"
                + ")";
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);
    }
    
    /**
     * 
     * Manifest.versions.(approval[] && packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed6_reverse() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]{*}."
                + "("
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageVersion Bundle.*\"]{*}"
                + "&& "
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] "
                + ")";
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertTrue(result.getEntities().get(0).getFieldNames().size() > 2);
    }

    /**
     * 
     * Manifest.versions.(approval[] && packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed7() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]{*}." + "("
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] " + "&& "
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageInvalid.*\"]{*}"
                + ")";
        deployContext.setHint(-1);
        deployContext.setSkips(null);
        deployContext.setLimits(null);
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    /**
     * 
     * Manifest.versions.(approval[] && packages[].version[]) embed embed refer
     * embed
     */
    @Test
    public void testEmbed7_reverse() {
        String query = "Manifest[@name=\"Dummy Manifest Bundle-0-0001\"]{*}."
                + "versions[@name=~\"Dummy ManifestVersion.*\"]{*}." + "("
                + "packages[@_oid=\"4fbdaccec681643199735a60\"]{*}.versions[@name=~\"Dummy PackageInvalid.*\"]{*}"
                + "&& "
                + "approvals[@name=~\"Dummy.*\"]{*}.classOfService[@_oid=\"4fbd4ec123456123456a5d\"] " 
                + ")";
        deployContext.setHint(-1);
        deployContext.setSkips(null);
        deployContext.setLimits(null);
        IQueryResult result = queryService.query(query, deployContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    public void testEmbed08() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{*}.department.(team{@name}.person{@name} && squad{@name}.human{@name})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testProject1() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);

        for (IEntity entity : result.getEntities()) {
            String type = entity.getType();
            System.out.println(type);
            Assert.assertTrue(type.equals("ServiceInstance") || type.equals("UpdateStrategy"));
            Assert.assertTrue(entity.getFieldNames().size() > 2);
        }
    }
    
    /**
     * A.(b || c)
     */
    @Test
    public void testProject2() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.(services[@name=~\"srp-app.*\"] || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(4, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertEquals("UpdateStrategy", entity.getType());
            Assert.assertTrue(entity.getFieldNames().size() > 2);
        }
    }
    
    /**
     * Test a missed projection in middle of the query path.
     * Cases are most the same as the testJoinInSet.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testProject3() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment{*}.applications.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());

        Assert.assertEquals(1, result.getEntities().get(0).getFieldValues("applications").size());
        IEntity entity = (IEntity) result.getEntities().get(0).getFieldValues("applications").get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertTrue(services.size() > 0);
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }

        List<IEntity> strategies = (List<IEntity>) entity.getFieldValues("updateStrategies");
        Assert.assertTrue(strategies.size() > 0);
        for (IEntity strategy : strategies) {
            // strategies are projected more than just _oid and _type
            Assert.assertTrue(strategy.getFieldNames().size() > 2);
            Assert.assertEquals("UpdateStrategy", strategy.getType());
        }
        System.out.println(entity);

    }

    @Test
    public void testProject4() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.(services[@name=~\"srp-appddbc.*\"]{*} || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(4, result.getEntities().size());

        for (IEntity entity : result.getEntities()) {
            String type = entity.getType();
            if ("UpdateStrategy".equals(type)) {
                Assert.assertTrue(entity.getFieldNames().size() > 2);
            } else {
                Assert.fail("projection test failure, expect ServiceInstance or UpdateStrategy only, but get " + type);
            }
        }
    }
    
    @Test
    public void testProject5() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService.(services[@name=\"srp-app:Raptor-00001\"]{*} || updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(5, result.getEntities().size());
        int serviceCount = 0;
        int strategyCount = 0;
        for (IEntity entity : result.getEntities()) {
            String type = entity.getType();
            if ("ServiceInstance".equals(type)) {
                Assert.assertTrue(entity.getFieldNames().size() > 2);
                serviceCount++;
            } else if ("UpdateStrategy".equals(type)) {
                Assert.assertTrue(entity.getFieldNames().size() > 2);
                strategyCount ++;
            } else {
                Assert.fail("projection test failure, expect ServiceInstance or UpdateStrategy only, but get " + type);
            }
        }
        Assert.assertEquals(4, strategyCount);
        Assert.assertEquals(1, serviceCount);
    }
    
    /**
     * A.((b && c) || (d || e)) -- make sure not miss of the multiple reference fields
     */
    @Test
    public void testProjection5() {
        
    }

    @Test
    public void testNoProject() {
        String query = "ApplicationService.(services[@name=~\"srp-app.*\"] && updateStrategies)";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(15, result.getEntities().size());
        boolean hasStartegy = false;
        boolean hasService = false;
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue("UpdateStrategy".equals(entity.getType()) || "ServiceInstance".equals(entity.getType()));
            Assert.assertTrue(entity.getFieldNames().size() > 2);
            
            hasService |= "ServiceInstance".equals(entity.getType());
            hasStartegy |= "UpdateStrategy".equals(entity.getType());
        }
        Assert.assertTrue(hasStartegy && hasService);
    }
//
//    /**
//     * A.(b && c)
//     */
//    @Test
//    public void testPassThroughQuery1() {
//        String query = "ApplicationService.(services[@name=~\"srp-appdbc.*\"] && updateStrategies{*})";
//        IQueryResult result = queryService.query(query, raptorContext);
//        Assert.assertEquals(0, result.getEntities().size());
//    }
//
//    @Test
//    public void testPassThroughQuery2() {
//        String query = "ApplicationService.(services[@name=~\"srp-app.*\"]{*} && updateStrategies{*})";
//        IQueryResult result = queryService.query(query, raptorContext);
//        Assert.assertEquals(12, result.getEntities().size());
//    }
//
//    /**
//     * Test pass through in multiple level cases
//     * 
//     * A.b.(c && d)
//     */
//    @Test
//    public void testPassThroughQuery3() {
//        String query = "Environment{*}.applications.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] && updateStrategies{*})";
//        IQueryResult result = queryService.query(query, raptorContext);
//        Assert.assertEquals(1, result.getEntities().size());
//    }

    @Test
    public void testQueryPlan() {
        String query = "Environment{*}.applications.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=~\"compute.*\"] && updateStrategies{*})";
        raptorContext.setExplain(true);
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertTrue(result.getExplanations().size() > 0);
    }

    @Test
    public void testDNSIntegeration() {
        String query = "DNS.(networkaddress[@_oid=\"testNetworkAddress1\"] && fqdn{*})";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("FQDN", result.getEntities().get(0).getType());
    }

    @Test
    public void testDNSIntegeration2() {
        String query = "DNS.(fqdn{*} && networkaddress[@_oid=\"testNetworkAddress2\"])";
        IQueryResult result = queryService.query(query, stratusContext);
        Assert.assertEquals(2, result.getEntities().size());
        for (IEntity entity :result.getEntities()) {
            Assert.assertEquals("FQDN", entity.getType());
        }
    }
    
    /**
     * A.(b && c || d || e)
     */
	@Test
    public void testMultipleSet01() {
        cmsdbContext.setAllowFullTableScan(false);
	    String query ="LBVirtualIP[@resourceId=\"192.168.3.5:80\"]{*}." +
	    		"(networkAddress{@resourceId, @address} " +
	    		"&& poolMaps{*}.pool{*}." +
	    		"    (lbService{*} || services{*}.lbMember{*}.(networkAddress{*})) " +
	    		"|| monitor{*} " +
	    		"|| lbService.networkAddress{@resourceId, @address})";
		//String query = "LBVirtualIP[@resourceId=\"192.168.3.5:80\"]{*}.(networkAddress{@resourceId, @address} || poolMaps{*}.pool{*}.(lbService{*} || services{*}.lbMember{*}.(networkAddress{*} || monitors{@resourceId})) || pools{*}.(lbService || services{*}.lbMember{*}.(networkAddress{*} || monitors{@resourceId})) || lbService.networkAddress{@resourceId, @address})";
	    IQueryResult result = queryService.query(query, cmsdbContext);
	    Assert.assertEquals(1, result.getEntities().size());
	    IEntity entity = result.getEntities().get(0);
	    verifyLBVirtualIP(entity);
	}
	
    /**
     * A.(b || c.(d || e) || f)
     */
	@Test
    public void testMultipleSet02() {
		String query = "LBVirtualIP[@resourceId=\"192.168.3.5:80\"]{*}." +
				"(networkAddress{@resourceId, @address} " +
				"|| poolMaps{*}.pool{*}.(lbService{*} || services{*}.lbMember{*}.(networkAddress{*} || monitors{@resourceId})) " +
				"|| monitor{*} " +
				"|| lbService.networkAddress{@resourceId, @address})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        verifyLBVirtualIP(entity);
    }
	
    /**
     * A.(b || (c || d) || e)
     */
	@Test
    public void testMultipleSet03(){
		String query = "LBVirtualIP[@resourceId=\"192.168.3.5:80\"]{*}." +
				"(networkAddress{@resourceId, @address} " +
				"|| (poolMaps{*}.pool{*}.(lbService{*} || services{*}.lbMember{*}.(networkAddress{*} || monitors{@resourceId}))) " +
				"|| lbService.networkAddress{@resourceId, @address})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        verifyLBVirtualIP(entity);
	}
	
    /**
     * A.(b || (c || d) || e)
     */
    @Test
    public void testMultipleSet03_nega(){
        String query = "LBVirtualIP[@resourceId=\"192.168.3.5:80\"]{*}." +
                "(networkAddress{@resourceId, @address} " +
                "|| (poolMaps{*}.pool{*}.(lbService{*} || services{*}.lbMember{*}.(networkAddress{*} || monitors{@resourceId}))) " +
                "|| lbService.networkAddress{@resourceId, @address})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        verifyLBVirtualIP(entity);
    }

    @SuppressWarnings("unchecked")
	private void verifyLBVirtualIP(IEntity entity) {
		Assert.assertEquals("LBVirtualIP", entity.getType());
		IEntity net = (IEntity)entity.getFieldValues("networkAddress").get(0);
        
        List<String> resources = (List<String>) net.getFieldValues("resourceId");
        Assert.assertEquals(resources.size(), 1);
        String resourceId = (String) resources.get(0);
        Assert.assertEquals("192.168.3.5", resourceId);
	            
        List<String> addresses = (List<String>) net.getFieldValues("address");
        Assert.assertEquals(addresses.size(), 1);
        String address = (String) addresses.get(0);
        Assert.assertEquals("192.168.3.5", address);
	            
        List<IEntity> poolMaps = (List<IEntity>) entity.getFieldValues("poolMaps");
        Assert.assertEquals(poolMaps.size(), 1);
        List<IEntity> pools = (List<IEntity>) poolMaps.get(0).getFieldValues("pool");
        Assert.assertEquals(pools.size(), 1);
        List<IEntity> services = (List<IEntity>)pools.get(0).getFieldValues("services");
        //Assert.assertEquals(services.size(), 1);
        List<IEntity> lbMembers = (List<IEntity>) services.get(0).getFieldValues("lbMember");
        List<IEntity> networks = (List<IEntity>) lbMembers.get(0).getFieldValues("networkAddress");
            
        resources = (List<String>) networks.get(0).getFieldValues("resourceId");
        Assert.assertEquals(resources.size(), 1);
        resourceId = (String) resources.get(0);
        Assert.assertEquals("192.168.3.5", resourceId);
           
        addresses = (List<String>) networks.get(0).getFieldValues("address");
        address = (String) addresses.get(0);
        Assert.assertEquals("192.168.3.5", address);
	}
    
    /**
     * A.((b && c) || d && e)
     */
	@Test
    public void testMultipleSet04(){
	    cmsdbContext.setAllowFullTableScan(true);
	    try {
            String query = "DNSRecord.((fqdn{*} && cname{*}) || fqdn{*} && networkAddress{*})";
            IQueryResult result = queryService.query(query, cmsdbContext);
            Assert.assertEquals(4, result.getEntities().size());
	    } catch (QueryParseException qpe) {
	        // 
	    }
	}

	@Ignore
	@Test
    public void testMultipleSet05(){
        String query = "Environment.applications.(appService!ServiceInstance.runsOn && appService!ServiceInstance.runsOn{*})";
        //String query = "Environment.applications.appService!ServiceInstance.runsOn{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(9, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
        	Assert.assertTrue(entity.getFieldNames().size() > 2);
        }
	}

	/**
	 * CMS-3486
	 */
	@Ignore
	@Test
    public void testSet_dupPath() {
        String query = "Dep.team[@_oid =& Dep[@_oid=~\"dep00[01]\"].(team{@_oid} && team.person[@_oid=\"Dep!dep000!team!team010!person!person012\"])]{*}";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.SYNTAX_ERROR.getErrorCode(), e.getErrorCode());
            System.out.println(e.getMessage());
        }
    }

    /**
     * CMS-3752
     */
    @Test
    public void testSet_dupPath_reverseSameName() {
        String query = "LBService[@resourceId=\"192.168.3.1\"]{*}.(lbService!LBVirtualIP{*} || lbService!LBPool{*}.services{*})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity iEntity = result.getEntities().get(0);
        Assert.assertTrue(iEntity.hasField("lbService!LBVirtualIP"));
        Assert.assertTrue(iEntity.hasField("lbService!LBPool"));
    }

    @Test
    public void testUnion_costOptimize() {
        raptorContext.setAllowFullTableScan(false);
        String query = "ApplicationService{*}.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryOptimizeException qoe) {
            // expected
            Assert.assertEquals(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN.getErrorCode(), qoe.getErrorCode());
        }
    }

    @Test
    public void testUnion_costOptimize_1() {
        raptorContext.setAllowFullTableScan(false);
        MetaClass serviceMeta = raptorMetaService.getMetaClass("ServiceInstance");
        MetaClass updateMeta = raptorMetaService.getMetaClass("UpdateStrategy");
        try {
            // 
            serviceMeta.setAllowFullTableScan(true);
            updateMeta.setAllowFullTableScan(true);
            String query = "ApplicationService{*}.(services{*} || updateStrategies{*})";
            queryService.query(query, raptorContext);
        } finally {
            // restore setting
            serviceMeta.setAllowFullTableScan(false);
            updateMeta.setAllowFullTableScan(false);
        }
    }
    
    /*
     * A.(b || b)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion1() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService{*}.(services[@name=~\"srp-app:Raptor-00001\"]{*} || services[@name=~\"srp-app:Raptor-00002\"]{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertEquals(2, services.size());
        for (IEntity serv : services) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            Assert.assertEquals("ServiceInstance", serv.getType());
        }
    }
    
    /*
     * A.(b && b)
     * no results
     */
    @Test
    public void testSameSubPathUnion2() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService{*}.(services[@name=\"srp-app:Raptor-00001\"]{*} && services[@name=\"srp-app:Raptor-00011\"]{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    /*
     * A.(b && b)
     * Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion3() {
        raptorContext.setAllowFullTableScan(true);
        
        String query = "ApplicationService{*}.(services[@https=true]{@name, @https, @healthStatus} && services[@healthStatus=\"down\"]{@activeManifestDiff, @https, @healthStatus})";

        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        IEntity entity = result.getEntities().get(0);
        List<IEntity> services = (List<IEntity>) entity.getFieldValues("services");
        Assert.assertEquals(4, services.size());
        for (IEntity serv : services) {
            Assert.assertEquals("ServiceInstance", serv.getType());
            // service are projected more than just _oid and _type
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            
            Boolean https = (Boolean)serv.getFieldValues("https").get(0);
            String health = (String)serv.getFieldValues("healthStatus").get(0);
            
            if (https) {
                Assert.assertNotNull(serv.getFieldValues("name").get(0));
            } 
            if (health.equals("down")) {
                Assert.assertNotNull(serv.getFieldValues("activeManifestDiff").get(0));
            }
        }
    }

    /*
     * A.(b || b)
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion4() {
        raptorContext.setAllowFullTableScan(true);
        
        String query = "ServiceInstance{*}.(runsOn[@assetStatus=\"unknown\"]{*} || runsOn[@name=\"compute-00002\"]{*})";

        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        IEntity entity = result.getEntities().get(0);
        List<IEntity> computes = (List<IEntity>) entity.getFieldValues("runsOn");
        Assert.assertEquals(1, computes.size());
        IEntity compute = computes.get(0);
        // service are projected more than just _oid and _type
        Assert.assertTrue(compute.getFieldNames().size() > 2);
        Assert.assertEquals("Compute", compute.getType());
    }
    
    /*
     * A.(b || b && b)
     * Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion5() {
        raptorContext.setAllowFullTableScan(true);
        
        String query = "ServiceInstance{*}.(runsOn[@location=~\"phx\"]{*} || runsOn[@location=\"pxc\"]{*} && runsOn[@assetStatus=\"deprecated\"]{*})";

        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(5, result.getEntities().size());
        
        List<IEntity> serviceInstances = result.getEntities();
        for (IEntity si : serviceInstances) {
            IEntity compute = ((List<IEntity>)si.getFieldValues("runsOn")).get(0);
            // service are projected more than just _oid and _type
            Assert.assertTrue(compute.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", compute.getType());
            
            String assetStatus = (String)compute.getFieldValues("assetStatus").get(0);
            String location = (String)compute.getFieldValues("location").get(0);
            
            if (location.equals("pxc")) {
                Assert.assertEquals("deprecated", assetStatus);
            } else {
                Assert.assertEquals("phx", location);
            }
        }
    }
    
    /*
     * A.(b && (b || b))
     * Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion10() {
        cmsdbContext.setAllowFullTableScan(true);
        
        String query = "NodeServer{@resourceId}.(capacities[@type=\"slot\" and @total=12]{@available,@type,@total} && (capacities[@type=\"cpu\" and @total=24]{@used,@type,@total} || capacities[@type=\"memory\" and @total=77298925568]{@used,@type,@available}))";

        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        
        List<IEntity> capacities = (List<IEntity>) result.getEntities().get(0).getFieldValues("capacities");
        Assert.assertEquals(3, capacities.size());
        for (IEntity capa : capacities) {
            // service are projected more than just _oid and _type
            Assert.assertTrue(capa.getFieldNames().size() > 2);
            Assert.assertEquals("ResourceCapacity", capa.getType());
        }
    }
    
    /*
     * A.((b || b) && (b && b))
     * Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion6() {
        raptorContext.setAllowFullTableScan(true);
        
        String query = "ServiceInstance{*}.((runsOn[@location=~\"phx\"]{*} || runsOn[@location=\"pxc\"]{*}) && (runsOn[@assetStatus=\"deprecated\"]{*} && runsOn[@label=\"laptop\"]{*}))";

        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(3, result.getEntities().size());
        
        List<IEntity> serviceInstances = result.getEntities();
        for (IEntity si : serviceInstances) {
            IEntity compute = ((List<IEntity>)si.getFieldValues("runsOn")).get(0);
            // service are projected more than just _oid and _type
            Assert.assertTrue(compute.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", compute.getType());
            
            String label = (String)compute.getFieldValues("label").get(0);
            String assetStatus = (String)compute.getFieldValues("assetStatus").get(0);
            String location = (String)compute.getFieldValues("location").get(0);
            
            Assert.assertEquals("laptop", label);
            Assert.assertEquals("deprecated", assetStatus);
            Assert.assertTrue("pxc".equals(location) || "phx".equals(location));
        }
    }

    /*
     * A.(b && b || b.(c && c || c)))
     * Merged results
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathUnion11() {
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setHint(-1);
        
        String query = "Dep{@label}.(team[@_oid=~\"Dep!dep.*!team!team010$\"]{@_oid}.person[@_oid=~\".*person012$\"]{@_oid} && team[@_oid=~\"Dep!dep.*!team!team[023][12]0$\"]{@_oid}.person[@_oid=~\".*person021$\"]{@name} || team[@_oid=~\"Dep!dep.*!team!team[02][12]0$\"]{@name}.(person[@_oid=~ \".*person!person[0123][12][12]$\"]{@address} && person[@_oid=~ \".*person!person.*11$\"]{@age} || person[@_oid=~ \".*person!person012$\"]{@name}))";

        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        
        int teamSize = 0;
        int personSize = 0;
        for (IEntity dep : result.getEntities()) {
            List<IEntity> teams = (List<IEntity>) dep.getFieldValues("team");
            teamSize += teams.size();
            for (IEntity team : teams) {
                List<IEntity> persons = (List<IEntity>) team.getFieldValues("person");
                personSize += persons.size();
            }
        }
        
        Assert.assertEquals(3, teamSize);
        Assert.assertEquals(4, personSize);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathEmbedUnion1() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "AssetServer[@type=\"bm\" and @healthState=~\"healthy\"].(nodeServer[@nodeType=\"vmm\" ]{@resourceId}.capacities[@type=\"slot\" and @available=0]{@used,@type} && nodeServer[@nodeType=\"vmm\" ]{@resourceId}.capacities[@type=\"cpu\" and @available>0]{@used,@available})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        List<IEntity> capacities = (List<IEntity>) entity.getFieldValues("capacities");
        Assert.assertEquals(2, capacities.size());
        
        IEntity capacity1 = capacities.get(0);
        Assert.assertEquals("slot", capacity1.getFieldValues("type").get(0));
        Long available1 = (Long)capacity1.getFieldValues("used").get(0);
        Assert.assertEquals(12, available1.longValue());
        
        IEntity capacity2 = capacities.get(1);
        Assert.assertEquals("cpu", capacity2.getFieldValues("type").get(0));
        Long available2 = (Long)capacity2.getFieldValues("available").get(0);
        Assert.assertEquals(23, available2.longValue());
    }
    
    @Test
    public void testSameSubPathEmbedUnion2() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "NodeServer[@nodeType=\"vmm\"]{@resourceId}.(capacities[@type=\"slot\" and @available=0]{@used,@type} && capacities[@type=\"cpu\" and @available=0]{@used,@available})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSameSubPathEmbedUnion3() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "NodeServer[@nodeType=\"vmm\"]{*}.(capacities[@type=\"slot\"]{*} || capacities[@type=\"cpu\"]{*})";
        IQueryResult result = queryService.query(query, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity entity = result.getEntities().get(0);
        List<IEntity> capacities = (List<IEntity>) entity.getFieldValues("capacities");
        Assert.assertEquals(2, capacities.size());
        
        IEntity capacity1 = capacities.get(0);
        Assert.assertEquals("slot", capacity1.getFieldValues("type").get(0));
        Long available1 = (Long)capacity1.getFieldValues("used").get(0);
        Assert.assertEquals(12, available1.longValue());
        
        IEntity capacity2 = capacities.get(1);
        Assert.assertEquals("cpu", capacity2.getFieldValues("type").get(0));
        Long available2 = (Long)capacity2.getFieldValues("available").get(0);
        Assert.assertEquals(23, available2.longValue());
    }
    
    /**
     * A || A
     */
    @Test
    public void testRootLevelUnion1() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Compute[@name=\"compute-00001\"]{@label} || Compute[@assetStatus=\"normal\"]{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(5, result.getEntities().size());
        for (IEntity comp : result.getEntities()) {
            Assert.assertTrue(comp.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", comp.getType());
            
            String name = (String)comp.getFieldValues("name").get(0);
            if (name.equals("compute-00001")) {
                Assert.assertEquals(1, comp.getFieldValues("label").size());
            }
        }
    }
    
    /**
     * A.c || A.c
     */
    @Test
    public void testRootLevelUnion2() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance[@name=\"srp-app:Raptor-00010\"].runsOn{@name,@location,@assetStatus} || ServiceInstance.runsOn[@location=\"pxc\"]{@name,@location,@label}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(4, result.getEntities().size());
        for (IEntity comp : result.getEntities()) {
            Assert.assertTrue(comp.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", comp.getType());
            String name = (String)comp.getFieldValues("name").get(0);
            if (name.equals("srp-app:Raptor-00010")) {
                Assert.assertNotNull(comp.getFieldValues("assetStatus").get(0));
            }
            
            String location = (String)comp.getFieldValues("location").get(0);
            if (location.equals("pxc")) {
                Assert.assertNotNull(comp.getFieldValues("label").get(0));
            }
        }
    }
    
    /**
     * A.c || B.c
     */
    @Test
    public void testRootLevelUnion3() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance[@name=\"srp-app:Raptor-00002\"].runsOn || Cluster[@name=\"cluster-00001\"].computes";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(6, result.getEntities().size());
        for (IEntity comp : result.getEntities()) {
            Assert.assertTrue(comp.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", comp.getType());
        }
    }

    /**
     * A.c && B.c 
     */
    @Test
    public void testRootLevelIntersection01() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance[@name=\"srp-app:Raptor-00002\"].runsOn && Cluster[@name=\"cluster-00001\"].computes";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            Assert.assertEquals(QueryErrCodeEnum.INTERSECTION_ON_DIFFERENT_ROOT_METACLASS.getErrorCode(), e.getErrorCode());
        }
    }
    
    /**
     * A.c && A.c
     */
    @Test
    public void testRootLevelIntersection02() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance[@name=\"srp-app:Raptor-00010\"].runsOn{@name,@location,@assetStatus} && ServiceInstance.runsOn[@location=\"pxc\"]{@name,@location,@label}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity comp : result.getEntities()) {
            Assert.assertTrue(comp.getFieldNames().size() > 2);
            Assert.assertEquals("Compute", comp.getType());
            Assert.assertNotNull(comp.getFieldValues("name").get(0));
            Assert.assertNotNull(comp.getFieldValues("location").get(0));
            Assert.assertNotNull(comp.getFieldValues("label").get(0));
            Assert.assertNotNull(comp.getFieldValues("assetStatus").get(0));
        }

    }
    
    @Test
    public void testRootLevelSetAggregation() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ServiceInstance<@https, @activeManifestDiff>{ @https, $max(@port)} || ServiceInstance[@name=\"srp-app:Raptor-00002\"]";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException e) {
            Assert.assertEquals(QueryErrCodeEnum.ROOT_LEVEL_JOIN_WITH_AGG.getErrorCode(), e.getErrorCode());
        }
    }

    @Test
    public void testEmbedSameUnionPath() {
        raptorContext.setAllowFullTableScan(true);
        raptorContext.setHint(-1);
        String query = "Dep[@_oid=\"dep000\"]{@_oid}.(team[@_oid=\"Dep!dep000!team!team010\"].person[@_oid=\"Dep!dep000!team!team010!person!person011\"] && team[@_oid=\"Dep!dep000!team!team010\"].person[@_oid=\"Dep!dep000!team!team010!person!person012\"])";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
    }
    
    @Test
    public void testSameCostSubSet01() {
        QueryContext context = newQueryContext(CMSDB_REPO, RAPTOR_MAIN_BRANCH_ID);
        context.setSmallTableThreshold(0);
        context.setAllowFullTableScan(true);
        context.setHint(-1);
        context.setExplain(true);
        String query = "DNSRecord.(fqdn[@resourceId=~\".*\"] && networkAddress{@address,@resourceId})";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        
        List<IQueryExplanation> explains = result.getExplanations();
        ObjectNode objectNode = (ObjectNode) explains.get(0).getJsonExplanation();
        String queryType0 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType0.equals("FQDN"));

        objectNode = (ObjectNode) explains.get(1).getJsonExplanation();
        String queryType1 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType1.equals("DNSRecord"));
        
        objectNode = (ObjectNode) explains.get(2).getJsonExplanation();
        String queryType2 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertTrue(queryType2.equals("NetworkAddress"));
    }
    
    @Test
    public void testSameCostSubSet02() {
        QueryContext context = newQueryContext(STRATUS_REPO, RAPTOR_MAIN_BRANCH_ID);
        context.setSmallTableThreshold(0);
        context.setAllowFullTableScan(true);
        context.setHint(-1);
        context.setExplain(true);
        String query = "Compute{*}.(<VPool>parentCluster && activeManifestRef && activeManifestCur[@_oid=~\"B.*\")";
        IQueryResult result = queryService.query(query, context);
        Assert.assertEquals(1, result.getEntities().size());
        
        List<IQueryExplanation> explains = result.getExplanations();
        ObjectNode objectNode = (ObjectNode) explains.get(0).getJsonExplanation();
        String queryType0 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertEquals("Manifest", queryType0);
        String queryReg = objectNode.get("criteria").get("$and").get(2).get("_i").get("$regex").get("$regex").getTextValue();
        Assert.assertEquals("B.*", queryReg);

        objectNode = (ObjectNode) explains.get(1).getJsonExplanation();
        String queryType1 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertEquals("Compute", queryType1);
        
        objectNode = (ObjectNode) explains.get(2).getJsonExplanation();
        String queryType2 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertEquals("VPool", queryType2);
        
        objectNode = (ObjectNode) explains.get(3).getJsonExplanation();
        String queryType3 = objectNode.get("criteria").get("$and").get(0).get("_t").getTextValue();
        Assert.assertEquals("Manifest", queryType3);
    }
}
