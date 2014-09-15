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

package com.ebay.cloud.cms.query.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.parser.ParseBaseNode.ParseNodeTypeEnum;


public class QueryParserTest extends MongoBaseTest {
    
    private static Logger logger =  LoggerFactory.getLogger(QueryParserTest.class);
    
    static String[]       queries    = new String[] {
            "ServiceInstance",
            "ServiceInstance[(@name =~ \"^foo.*\") and (@healthStatus = \"healthy\")]",
            "ServiceInstance[(@name =~ \"^foo.*\") or (@healthStatus = \"healthy\")]",
            "ServiceInstance[not ((@name =~ \"^foo.*\") and ((@healthStatus = \"healthy\") or (@healthStatus = \"fail\")))]",
            "ServiceInstance[exists @healthStatus]",
            "ServiceInstance[@properties.$inF1.$inF2 = 1]",            
            "ApplicationService[@manifestRef in (\"a\")]",
            "ApplicationService[not (@manifestRef in (\"a\",\"b\"))]",
            "ApplicationService[@_version in (1)]",
            "ApplicationService[@_version > 1]",
            "ApplicationService[@_version >= 1]",
            "ApplicationService[@_version < 1]",
            "ApplicationService[@_version <= 1]",
            "ApplicationService[@_version in (1.1)]",
            "ApplicationService.services",
            "ApplicationService.services{@name}",
            "ApplicationService.services[@name = \"comp-iq\"]",
            "ApplicationService.services[@name != \"comp-iq\"]{*}",
            "ApplicationService.services[@name = \"comp-iq\"]{@name,@_lastmodified}",
            "ApplicationService{*}.services[@name = \"comp-iq\"]{*}",
            "Compute{@location,@label}",
            "Compute[@label = \"compute-00001\"]{*}",
            "ApplicationService[@name = \"comp-iq\"].services[@name =  \"comp-iq\"]{*}.runsOn[@location = \"lvs\"]",
            "ApplicationService[@name = \"comp-iq\"]{*}.services.runsOn{*}",
            // aggregation queries
            "ServiceInstance<@_status>[@https = true and $sum(@_version) > 1234]",
            "ApplicationService[@name = \"srp\"].services<@_status, @activeManifestDiff>[@https = true and $sum(@_version) > 1234 ]{@_status, @activeManifestDiff}",
            // sub query queries
            "ApplicationService[@services =& ServiceInstance[@_oid=\"\"]]",
            "ApplicationService[@name =\"srp\" and (@services =& ServiceInstance[@_oid=\"538fca3df135\" and @runsOn =& Compute[@name=\"Raptor-compute\"]]) ]",
            // reverse traversal queries
            "ApplicationService[@_oid=\"\"]{*}.services[@_oid=\"\"]{*}.services!ApplicationService[@_oid=\"\"]{*}",
            "ApplicationService.appService!ServiceInstance[@name=~\".*\"]{*}.runsOn[@name=~\".*\"]{*}",
    };
    
    static String[] stratusQuries = new String[] {
    	// type cast queries
    	"PoolCluster[@_oid=\"dummy-pool-cluster-001\"].<PooledCluster>pools",
    };

    static String[] badQueries = new String[]{
            "",
            "Compute{*}[@label = \"compute-00001\"]",
            "ServiceInstance[@name = abc]",
            "A.B[aaa]",
            "Group{}",
            "<ServiceInstance, ApplicationService>Compute",
            "ServiceInstance<@_status>{ $count(@port) }",
            "ApplicationService[@name = \"srp\"].services<@_status, @activeManifestDiff>[@https = true and $sum(@port) > 1234 and @properties.f1 = \"test\"]{@name, @properties.f2}",
            "ApplicationService.appService[@_oid=\".*\"]!ServiceInstance[@name=~\".*\"]{*}",
            "ApplicationService{*} {*}",
    };
    
    static String[]             setQueries = new String[] {
        "ApplicationService{*}.( services{*} && updateStrategies{*})",
        "ApplicationService{*}.( services{*} || updateStrategies{*})",
        "ApplicationService{*}.( services{*} && (updateStrategies{*}))",
        "ApplicationService{*}.( services.runsOn{*} && (updateStrategies{*}))",
        "ApplicationService{*}.services{*}.( runsOn[@label=~\".*\"]{*} && (appService{*}))",
        "Environment.( applications{*}.(services[@name=~\"srp-app.*\"]{*}.runsOn[@name=\"compute\"] && updateStrategies{*}) && cos[@_oid=\"4fbb314fc681caf13e283a77\"] )"
    };
    
    @Test
    public void testParser() throws Exception {
        for (String q : queries) {
            logger.debug("parsing {}\n", q);
            ParseBaseNode node = raptorParser.parse(q);
            printNode(node);
        }
        
        for (String q : stratusQuries) {
        	ParseBaseNode node = stratusParser.parse(q);
        	printNode(node);
        }
    }
    
    @Test
    public void testParser01() throws Exception {
        String query = "Compute[@fqdns=\"aaaa.ebay.com\"]{*}."
                + "( <VPool>parentCluster[@_oid=\"CLguf6sdle-invalid\"] "
                + "&& activeManifestRef[@_oid=\"B-N-D-1.20111108154904.0\"]"
                + "|| activeManifestCur[@_oid=\"Black-Pearl-1.20111103221326.0\"])";

        ParseBaseNode baseNode = stratusParser.parse(query);
        Assert.assertTrue(baseNode.getType() == ParseNodeTypeEnum.QUERY);
        Assert.assertTrue(baseNode.getNextNodes().size() == 1);

        ParseBaseNode joinNode = baseNode.getNextNodes().get(0);
        Assert.assertTrue(joinNode.getType() == ParseNodeTypeEnum.INNERJOIN);
        Assert.assertTrue(joinNode.getNextNodes().size() == 1);

        ParseBaseNode unionNode = joinNode.getNextNodes().get(0);
        Assert.assertTrue(unionNode.getType() == ParseNodeTypeEnum.UNION);
        Assert.assertTrue(unionNode.getNextNodes().size() == 2);

        ParseBaseNode intersectNode = unionNode.getNextNodes().get(0);
        Assert.assertTrue(intersectNode.getType() == ParseNodeTypeEnum.INTERSECTION);
        Assert.assertTrue(intersectNode.getNextNodes().size() == 2);

        ParseBaseNode manifestCurNode = unionNode.getNextNodes().get(1);
        Assert.assertTrue(manifestCurNode.getType() == ParseNodeTypeEnum.QUERY);
    }

    protected void printNode(ParseBaseNode searchNode) {
        System.out.println(searchNode.toString());
        
        for (ParseBaseNode node : searchNode.getNextNodes()) {
            printNode(node);
        }
    }

    @Test
    public void testFailure() throws Exception {
        int errCount = 0;
        for (String q : badQueries) {
            try {
                raptorParser.parse(q);
            } catch(QueryParseException e) {
                logger.error("++++parsing {} - {}\n", q, e.getMessage());
                errCount++;
            }            
        }
        Assert.assertEquals(errCount, badQueries.length);
    }

    @Test
    public void testSetQueries() throws Exception {
        for (String q : setQueries) {
            logger.debug("parsing {}\n", q);
            ParseBaseNode node = raptorParser.parse(q);
            printNode(node);
        }
    }

}
