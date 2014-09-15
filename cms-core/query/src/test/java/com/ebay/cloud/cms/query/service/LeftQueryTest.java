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
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.metadata.QueryMetaClass;
import com.ebay.cloud.cms.query.parser.QueryParseException;

public class LeftQueryTest extends MongoBaseTest {

    @Test
    public void testLeftQuery() {
        raptorContext.setAllowFullTableScan(true);
        String query = "ApplicationService{*}.services[@_oid=\"xxx\"]";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(0, result.getEntities().size());

        query = "ApplicationService{*}+.services[@_oid=\"xxx\"]";
        result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
    }

    // projection filter : show the details if any.
    @Test
    @SuppressWarnings("unchecked")
    public void testProjection() {
        raptorContext.setAllowFullTableScan(false);
        raptorContext.setHint(-1);
        String serviceName = "srp-app:Raptor-00001";
        String query = "ApplicationService[@_oid=~\"^.*\"/s]{*}+.services[@name=\"" + serviceName + "\"]{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        IEntity app = result.getEntities().get(0);
        List<IEntity> services = (List<IEntity>) app.getFieldValues("services");
        Assert.assertEquals(10, services.size());
        int withNameCount = 0;
        for (IEntity serv : services) {
            if (serv.hasField("name")) {
                Assert.assertEquals(serviceName, serv.getFieldValues("name").get(0));
                Assert.assertEquals(3, serv.getFieldNames().size());
                withNameCount++;
            } else {
                Assert.assertEquals(2, serv.getFieldNames().size());
            }
        }
        Assert.assertEquals(1, withNameCount);
    }

    // Todd's requirement:
    @Test
    @SuppressWarnings("unchecked")
    public void testProject_2() {
        // pre-query to get count
        String rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets{*}";
        cmsdbContext.setAllowFullTableScan(true);
        IQueryResult result = queryService.query(rackServerQuery, cmsdbContext);
        int assetCount = result.getEntities().size();
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer{@_oid}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int assetServerCount = result.getEntities().size();
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer.nodeServer{@_oid}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int nodeServerCount = result.getEntities().size();

        // case 1 : find all the assets on the given rack, show the asset server
        // and node server details if any some asset has no asset server point
        // to him. See asset OID : 519b5cf4e4b06ba5808349b1
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets{*}+.asset!AssetServer{*}.nodeServer{*}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        Assert.assertEquals(assetCount, result.getEntities().size());
        int assetWihtoutAssetServerCount = 0;
        for (IEntity entity : result.getEntities()) {
            if (!entity.hasField("asset!AssetServer")) {
                assetWihtoutAssetServerCount++;
            } else {
                List<IEntity> assetServers = (List<IEntity>) entity.getFieldValues("asset!AssetServer");
                Assert.assertFalse(assetServers.isEmpty());
                for (IEntity assetServer : assetServers) {
                    Assert.assertTrue(assetServer.getFieldNames().size() > 2);

                    List<IEntity> nodeServers = (List<IEntity>) assetServer.getFieldValues("nodeServer");
                    for (IEntity nodeServer : nodeServers) {
                        Assert.assertTrue(nodeServer.getFieldNames().size() > 2);
                    }
                }
            }
        }
        Assert.assertEquals(1, assetWihtoutAssetServerCount);

        // case 2: some asset server without node server
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer{*}+.nodeServer{*}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        Assert.assertEquals(assetServerCount, result.getEntities().size());
        int assetServerNOnsCount = 0;
        int danglingToNodeServerCount = 0;
        int nodeServerInLeft = 0;
        Assert.assertEquals(assetServerCount, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            if (!entity.hasField("nodeServer")) {
                assetServerNOnsCount++;
            } else {
                List<IEntity> nodeServers = (List<IEntity>) entity.getFieldValues("nodeServer");
                for (IEntity nodeServer : nodeServers) {
                    if (nodeServer.getFieldNames().size() == 2) {
                        danglingToNodeServerCount++;
                    } else {
                        nodeServerInLeft++;
                    }
                }
            }
        }
        Assert.assertEquals(1, assetServerNOnsCount);
        Assert.assertEquals(1, danglingToNodeServerCount);
        Assert.assertEquals(nodeServerCount, nodeServerInLeft);

        // case 3: asset server
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer{*}.nodeServer";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int assetServerWithNodeServerCount = result.getEntities().size();

        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer{*}+.nodeServer";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int assetServerLeftNodeServerCount = result.getEntities().size();
        Assert.assertEquals(2, assetServerLeftNodeServerCount - assetServerWithNodeServerCount);
    }

    /**
     * CMS 3617 ::
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testLeftReverse() {
        cmsdbContext.setAllowFullTableScan(true);
        String query = "Rack{*}+.assets[@assetType=\"Server\"]{@resourceId,@assetType,@healthState,@healthState,@_lastmodified,@manufacturer,@locationCode}.asset!AssetServer{@resourceId}";
        IQueryResult result = queryService.query(query, cmsdbContext);

        Assert.assertEquals(1, result.getEntities().size());
        IEntity rack = result.getEntities().get(0);
        List<JsonEntity> assets = (List<JsonEntity>) rack.getFieldValues("assets");
        Assert.assertEquals(7, assets.size());
        int assetWihtoutAssetServerCount = 0;
        int assetWithAssetServerCount = 0;
        for (JsonEntity asset : assets) {
            if (!asset.getNode().has("asset!AssetServer")) {
                assetWihtoutAssetServerCount++;
            } else {
                assetWithAssetServerCount++;
            }
        }
        Assert.assertEquals(1, assetWihtoutAssetServerCount);
        Assert.assertEquals(assets.size() - assetWihtoutAssetServerCount, assetWithAssetServerCount);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testMultiLeft() {
        // pre-query to get count
        String rackServerQuery = null;
        IQueryResult result = null;
        cmsdbContext.setAllowFullTableScan(true);
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets{*}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int assetCount = result.getEntities().size();
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer{@_oid}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int assetServerCount = result.getEntities().size();
        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"].assets.asset!AssetServer.nodeServer{@_oid}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        int nodeServerCount = result.getEntities().size();

        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"]{*}.assets{*}+.asset!AssetServer{*}+.nodeServer{*}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity leftRack = result.getEntities().get(0);
        List<JsonEntity> leftAssets = (List<JsonEntity>) leftRack.getFieldValues("assets");

        rackServerQuery = "Rack[@location=\"LVS01-01-400-1605\"]{*}.assets{*}.asset!AssetServer{*}.nodeServer{*}";
        result = queryService.query(rackServerQuery, cmsdbContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity normalRack = result.getEntities().get(0);
        List<JsonEntity> normalAssets = (List<JsonEntity>) leftRack.getFieldValues("assets");

        // now assert the asset size
        Assert.assertEquals(normalAssets.size() + 1, leftAssets.size() + 1);

        QueryMetaClass assetQueryMetadata = createAssetQueryMeta();
        // assert asset server size
        int leftAssetServerCount = 0;
        int noAssetServerCount = 0;

        for (JsonEntity asset : leftAssets) {
            JsonEntity leftAsset = new JsonEntity(assetQueryMetadata, asset.getNode());
            if (leftAsset.hasField("asset!AssetServer")) {
                leftAssetServerCount += leftAsset.getFieldValues("asset!AssetServer").size();
            } else {
                noAssetServerCount++;
            }
        }
        Assert.assertEquals(1, noAssetServerCount);
        int normalAssetServerCount = 0;
        for (JsonEntity asset : normalAssets) {
            JsonEntity normalAsset = new JsonEntity(assetQueryMetadata, asset.getNode());
            normalAssetServerCount += normalAsset.getFieldValues("asset!AssetServer").size();
        }
        Assert.assertEquals(normalAssetServerCount, leftAssetServerCount);
    }

    private QueryMetaClass createAssetQueryMeta() {
        QueryContext context = newQueryContext(CMSDB_REPO, CMSDB_MAIN_BRANCH_ID);
        QueryMetaClass assetMeta = QueryMetaClass.newInstance(context, cmsdbMetaService.getMetaClass("Asset"));
        MetaClass assetServerMeta = cmsdbMetaService.getMetaClass("AssetServer");
        assetMeta.addReverseField(assetServerMeta, (MetaRelationship) assetServerMeta.getFieldByName("asset"));
        return assetMeta;
    }
    
    

    @Test
    public void testLeftJoin_2() {
        String query = "Rack[@location=\"LVS01-01-400-1605\"].assets{*}+.powerStrip{*}";
        IQueryResult queryResult = queryService.query(query, cmsdbContext);
        // Asset has no link to powerStrip - but assets should be returned
        Assert.assertEquals(7, queryResult.getEntities().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLeftWithSet() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Environment.applications{*}+.(services{*}+.runsOn{*} && updateStrategies{*})";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        List<IEntity> services = (List<IEntity>) result.getEntities().get(0).getFieldValues("services");
        List<IEntity> uss = (List<IEntity>) result.getEntities().get(0).getFieldValues("updateStrategies");
        int serviceWihtoutComputeCount = 0;
        for (IEntity serv : services) {
            Assert.assertTrue(serv.getFieldNames().size() > 2);
            if (!serv.hasField("runsOn")) {
                serviceWihtoutComputeCount++;
            } else {
                List<IEntity> runsOn = (List<IEntity>) serv.getFieldValues("runsOn");
                for (IEntity comp : runsOn) {
                    Assert.assertTrue(comp.getFieldNames().size() > 2);
                }
            }
        }
        Assert.assertEquals(1, serviceWihtoutComputeCount);
        for (IEntity us : uss) {
            Assert.assertTrue(us.getFieldNames().size() > 2);
        }
    }

    /**
     * CMS-3531
     */
    @Test
    @SuppressWarnings({ "unchecked" })
    public void testLeftWithSet2() {
        raptorContext.setAllowFullTableScan(true);
        String query = "LOJDataService{*}.(applicationServices{@name}+.serviceInstances{@name} && applicationService{@name}+.serviceInstance{@name})";
        IQueryResult result = queryService.query(query, raptorContext);

        String dsOid2 = "LOJDataServiceOID2";
        String dsOid3 = "LOJDataServiceOID3";
        String appOid1 = "LOJApplicationServiceOID1";
        String appOid2 = "LOJApplicationServiceOID2";
        String appOid3 = "LOJApplicationServiceOID3";
        Assert.assertEquals(2, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            // assert ds2
            if (dsOid2.equals(entity.getId())) {
                // assert path of
                // LOJDataService.applicationService.serviceInstance
                // app1
                List<IEntity> as = (List<IEntity>) entity.getFieldValues("applicationService");
                Assert.assertEquals(1, as.size());
                IEntity appService = as.get(0);
                Assert.assertEquals(3, appService.getFieldNames().size());// _oid,
                                                                          // _type,
                                                                          // name

                // now assert path of
                // LOJDataService.applicationServices.serviceInstances
                // app1 and app2
                as = (List<IEntity>) entity.getFieldValues("applicationServices");
                Assert.assertEquals(2, as.size());
                for (IEntity appEntity : as) {
                    if (appOid1.equals(appEntity.getId())) {
                        Assert.assertEquals(3, appEntity.getFieldNames().size());// _oid,_type,
                                                                                 // name
                    } else {
                        Assert.assertEquals(4, appEntity.getFieldNames().size());// _oid,_type,
                                                                                 // name,
                                                                                 // serviceInstances
                        List<IEntity> services = (List<IEntity>) appEntity.getFieldValues("serviceInstances");
                        Assert.assertEquals(2, services.size());
                        for (IEntity servEntity : services) {
                            Assert.assertEquals(3, servEntity.getFieldNames().size());
                        }
                    }
                }
            }
            // now asset ds3
            else {
                // assert LOJDataService.applicationService.serviceInstance
                List<IEntity> as = (List<IEntity>) entity.getFieldValues("applicationService");
                Assert.assertEquals(1, as.size());
                IEntity appService = as.get(0);
                Assert.assertEquals(4, appService.getFieldNames().size());// _oid,
                                                                          // _type,
                                                                          // name,
                                                                          // serviceInstance
                List<IEntity> services = (List<IEntity>) appService.getFieldValues("serviceInstance");
                Assert.assertEquals(1, services.size());
                Assert.assertEquals(3, services.get(0).getFieldNames().size());

                // assert LOJDataService.applicationServices.serviceInstances
                // app2 and app3 has the same assertion
                as = (List<IEntity>) entity.getFieldValues("applicationServices");
                Assert.assertEquals(2, as.size());
                for (IEntity appEntity : as) {
                    Assert.assertEquals(4, appEntity.getFieldNames().size()); // _oid,
                                                                              // _type,
                                                                              // name,
                                                                              // serviceInstances
                    services = (List<IEntity>) appEntity.getFieldValues("serviceInstances");
                    Assert.assertEquals(2, services.size());
                    for (IEntity serv : services) {
                        Assert.assertEquals(3, serv.getFieldNames().size());
                    }
                }
            }
        }
    }

    @Test
    public void testLeftWithHint() {
        // TODO
    }

    @Test
    public void testLeftWithPagination() {
        // TODO
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft01() {
        deployContext.setAllowFullTableScan(true);
        String query = null;
        IQueryResult result = null;
        query = "Manifest.versions[@_oid=\"XXXX\"]";
        result = queryService.query(query, deployContext);
        Assert.assertEquals(0, result.getEntities().size());

        query = "Manifest{*}+.versions[@_oid=\"XXXX\"]{*}";
        result = queryService.query(query, deployContext);
        Assert.assertEquals(1, result.getEntities().size());
        IEntity manifest = result.getEntities().get(0);
        List<IEntity> versions = (List<IEntity>) manifest.getFieldValues("versions");
        Assert.assertEquals(2, versions.size());
        for (IEntity version : versions) {
            System.out.println(version.getFieldNames());
            Assert.assertTrue(version.getFieldNames().size() > 2);
        }
    }

    @Test
    public void testEmbedLeft02() {
        String query = null;
        IQueryResult result = null;
        query = "Manifest.versions{*}+.approvals[@_oid=\"xxxx\"]";
        result = queryService.query(query, deployContext);
        Assert.assertTrue(result.getEntities().size() > 0);
    }

    @Test
    public void testEmbedLeft021() {
        String query = null;
        IQueryResult result = null;
        query = "Manifest.versions+.approvals[@_oid=\"xxxx\"]";
        result = queryService.query(query, deployContext);
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft03() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{*}+.department{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        int noDepCount = 0;
        for (IEntity country : result.getEntities()) {
            if (!country.hasField("department")) {
                noDepCount++;
            } else {
                List<IEntity> departments = (List<IEntity>) country.getFieldValues("department");
                for (IEntity dep : departments) {
                    Assert.assertTrue(dep.getFieldNames().size() > 2);
                    System.out.println(dep.getFieldNames());
                }
            }
        }
        Assert.assertEquals(1, noDepCount);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft04() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{@name}+.department{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        int noDepCount = 0;
        for (IEntity country : result.getEntities()) {
            if (!country.hasField("department")) {
                Assert.assertEquals(3, country.getFieldNames().size());
                noDepCount++;
            } else {
                Assert.assertEquals(4, country.getFieldNames().size());
                List<IEntity> departments = (List<IEntity>) country.getFieldValues("department");
                for (IEntity dep : departments) {
                    Assert.assertEquals(3, dep.getFieldNames().size());
                }
            }
        }
        Assert.assertEquals(1, noDepCount);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft05() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{@name, @department}+.department{@name}.team{@name, @person}+.person{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        int noDepCount = 0;
        int hasDepCount = 0;
        for (IEntity country : result.getEntities()) {
            if (!country.hasField("department")) {
                noDepCount++;
            } else {
                hasDepCount++;
                List<IEntity> departments = (List<IEntity>) country.getFieldValues("department");
                Assert.assertEquals(2, departments.size());
                int noTeamCount = 0;
                int noPersonCount = 0;
                for (IEntity dep : departments) {
                    if (!dep.hasField("team")) {
                        noTeamCount++;
                        Assert.assertEquals(3, dep.getFieldNames().size());
                    } else {
                        Assert.assertEquals(4, dep.getFieldNames().size());
                        List<IEntity> teams = (List<IEntity>) dep.getFieldValues("team");
                        Assert.assertEquals(2, teams.size());
                        for (IEntity team : teams) {
                            if (!team.hasField("person")) {
                                noPersonCount++;
                                Assert.assertEquals(3, team.getFieldNames().size());
                            } else {
                                Assert.assertEquals(4, team.getFieldNames().size());
                                List<IEntity> people = (List<IEntity>) team.getFieldValues("person");
                                Assert.assertEquals(2, people.size());
                                for (IEntity person : people) {
                                    Assert.assertEquals(3, person.getFieldNames().size());
                                }
                            }
                        }
                    }
                }
                Assert.assertEquals(1, noTeamCount);
                Assert.assertEquals(1, noPersonCount);
            }
        }
        Assert.assertEquals(1, noDepCount);
        Assert.assertEquals(1, hasDepCount);
    }
    
    /**
     * Compare to testEmbedLeft05 for the left join query projection
     * 
     * 
     * <pre>
        [{
            "_type": "Company",
            "name": "company01",
            "_oid": "company01"
        }, {
            "_type": "Company",
            "name": "company02",
            "_oid": "company02",
            "department": [{
                "_type": "Department",
                "name": "department0202",
                "_oid": "Company!company02!department!department0202",
                "team": [{
                    "_type": "Team",
                    "name": "team020201",
                    "_oid": "Company!company02!department!department0202!team!team020201"
                }, {
                    "_type": "Team",
                    "name": "team020202",
                    "_oid": "Company!company02!department!department0202!team!team020202",
                    "person": [{
                        "_type": "Person",
                        "name": "dev-91",
                        "_oid": "Company!company02!department!department0202!team!team020202!person!person02020201"
                    }, {
                        "_type": "Person",
                        "name": "dev-92",
                        "_oid": "Company!company02!department!department0202!team!team020202!person!person02020202"
                    }]
                }]
            }]
        }]
        </pre>
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft05_compare() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{@name}+.department{@name}.team{@name}+.person{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        int noDepCount = 0;
        int hasDepCount = 0;
        for (IEntity country : result.getEntities()) {
            if (!country.hasField("department")) {
                noDepCount++;
            } else {
                hasDepCount++;
                List<IEntity> departments = (List<IEntity>) country.getFieldValues("department");
                Assert.assertEquals(1, departments.size()); // assertion of department is different
                int noTeamCount = 0;
                int noPersonCount = 0;
                for (IEntity dep : departments) {
                    if (!dep.hasField("team")) {
                        noTeamCount++;
                        Assert.assertEquals(3, dep.getFieldNames().size());
                    } else {
                        Assert.assertEquals(4, dep.getFieldNames().size());
                        List<IEntity> teams = (List<IEntity>) dep.getFieldValues("team");
                        Assert.assertEquals(2, teams.size());
                        for (IEntity team : teams) {
                            if (!team.hasField("person")) {
                                noPersonCount++;
                                Assert.assertEquals(3, team.getFieldNames().size());
                            } else {
                                Assert.assertEquals(4, team.getFieldNames().size());
                                List<IEntity> people = (List<IEntity>) team.getFieldValues("person");
                                Assert.assertEquals(2, people.size());
                                for (IEntity person : people) {
                                    Assert.assertEquals(3, person.getFieldNames().size());
                                }
                            }
                        }
                    }
                }
                // left query without join field projection
                Assert.assertEquals(0, noTeamCount);
                Assert.assertEquals(1, noPersonCount);
            }
        }
        Assert.assertEquals(1, noDepCount);
        Assert.assertEquals(1, hasDepCount);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmbedLeft06() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company{@name}.department{@name}.team{@name}+.person{@name}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity country : result.getEntities()) {
            List<IEntity> departments = (List<IEntity>) country.getFieldValues("department");
            Assert.assertEquals(1, departments.size());
            int noPersonCount = 0;
            int hasPersonCount = 0;
            for (IEntity dep : departments) {
                Assert.assertEquals(4, dep.getFieldNames().size());
                List<IEntity> teams = (List<IEntity>) dep.getFieldValues("team");
                Assert.assertEquals(2, teams.size());
                for (IEntity team : teams) {
                    if (!team.hasField("person")) {
                        noPersonCount++;
                        Assert.assertEquals(3, team.getFieldNames().size());
                    } else {
                        hasPersonCount++;
                        Assert.assertEquals(4, team.getFieldNames().size());
                        List<IEntity> people = (List<IEntity>) team.getFieldValues("person");
                        Assert.assertEquals(2, people.size());
                        for (IEntity person : people) {
                            Assert.assertEquals(3, person.getFieldNames().size());
                        }
                    }
                }
            }
            Assert.assertEquals(1, noPersonCount);
            Assert.assertEquals(1, hasPersonCount);
        }
    }

    @Test
    public void testEmbedLeft07() {
        raptorContext.setAllowFullTableScan(true);
        String query = "Company+.department{*}";
        IQueryResult result = queryService.query(query, raptorContext);
        Assert.assertEquals(2, result.getEntities().size());
        for (IEntity country : result.getEntities()) {
            Assert.assertEquals("Department", country.getType());
        }
    }

    @Test
    public void testLeft_invalid() {
        raptorContext.setAllowFullTableScan(true);
        String query = "LOJDataService{*}+applicationService[@_oid=\"notsdfsfasdfa\"]{*}";
        try {
            queryService.query(query, raptorContext);
            Assert.fail();
        } catch (QueryParseException qpe) {
            // expected
            System.out.println(qpe.getMessage());
            Assert.assertEquals(QueryErrCodeEnum.PARSE_GRAMMER_ERROR.getErrorCode(), qpe.getErrorCode());
        }
    }

}
