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

package com.ebay.cloud.cms.dalapi.search;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * 
 */
public class AggregationQueryTest extends SearchBaseTest {

    /**
     * Case of use a non-aggregation field as filter value field
     * 
     * THIS is NOT SUPPORTED! Marked as @Ignore
     */
    @Ignore
    public void test00() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.AVG, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        // use aggregation field as filter VALUE: supported?
        ISearchField filterField = createSearchField(metadata, "port");
        SearchCriteria criteria = new FieldSearchCriteria(filterField, FieldOperatorEnum.LT, max_ports);

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, criteria, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
        System.out.println(result.getResultSet().get(0));
    }

    @Test
    public void test01() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        SearchCriteria criteria = new FieldSearchCriteria(max_ports, FieldOperatorEnum.GT, "123");

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, criteria, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());
    }

    @Test
    public void test02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "activeManifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);

        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        List<String> sortFields = new ArrayList<String>();
        sortFields.add("name");
        sortFields.add("port");
        List<Integer> sortOrders = new ArrayList<Integer>();
        sortOrders.add(SearchOption.DESC_ORDER);
        sortOrders.add(SearchOption.DESC_ORDER);
        option.setSort(sortFields, sortOrders, metadata);
        option.setSkip(1);
        option.setLimit(2);

        SearchResult result = searchService.search(query, option, raptorContext);

        /**
         * refer to <code>RaptorEntityGenerator</code for the expected data size
         */
        Assert.assertEquals(1, result.getResultSize());

        List<IEntity> entities = result.getResultSet();
        for (IEntity e : entities) {
            System.out.println(e);
        }
    }

    @Test
    public void test03() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "_oid"));
        group.addGroupField(createGroupField(metadata, "_type"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(10, result.getResultSize());
    }

    @Test
    public void test04() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        raptorContext.setMongoDataSource(ds);
        DBCollection coll = raptorContext.getDBCollection(metadata);
        DBObject firstOp = new BasicDBObject();

        firstOp.put("_id", "$_i");

        DBObject groupDbo = new BasicDBObject();
        groupDbo.put("$group", firstOp);
        AggregationOutput output = coll.aggregate(groupDbo);

        Assert.assertTrue(output.getCommandResult().ok());
    }

    /**
     * Case of use a search field(non-aggregation field) as filter field
     */
    @Test
    public void test05() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        SearchCriteria criteria = new FieldSearchCriteria(createGroupField(metadata, "healthStatus"),
                FieldOperatorEnum.EQ, "up");

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, criteria, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(1, result.getResultSize());
    }

    @Test
    public void test07FilterBeforeGroup() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        SearchCriteria groupCriteria = new FieldSearchCriteria(max_ports, FieldOperatorEnum.GT, "123");

        SearchCriteria fitlerCriteria = new FieldSearchCriteria(createSearchField(metadata, "name"),
                FieldOperatorEnum.EQ, "srp-app:srp-app-00001");

        ISearchQuery query = new SearchQuery(metadata, fitlerCriteria, projection, group, groupCriteria, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(1, result.getResultSize());

        // case 2: filter with invalid oid first
        SearchCriteria fitlerCriteria2 = new FieldSearchCriteria(createSearchField(metadata, "_oid"),
                FieldOperatorEnum.EQ, "5098d75742222700d1544cf5-invalid-oid");
        ISearchQuery query2 = new SearchQuery(metadata, fitlerCriteria2, projection, group, groupCriteria, strategy);
        SearchResult result2 = searchService.search(query2, option, raptorContext);
        Assert.assertEquals(0, result2.getResultSize());
    }

    @Test
    public void test08SingleGroupField() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "_oid"));

        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(max_ports);

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(10, result.getResultSize());
    }

    @Test
    public void test09Projection() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        projection.addField(createGroupField(metadata, "healthStatus"));

        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());

        BsonEntity entity = (BsonEntity) result.getResultSet().get(0);

        BasicDBObject idObject = (BasicDBObject) entity.getNode().get("_id");
        Assert.assertTrue(idObject.containsField("healthStatus"));
        Assert.assertFalse(idObject.containsField("manifestDiff"));
    }

    @Test
    public void test10NoProjection() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField max_ports = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(max_ports);

        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());

        BsonEntity entity = (BsonEntity) result.getResultSet().get(0);
        Assert.assertEquals(3, entity.getNode().size());
    }

    @Test
    public void test11Count() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField count = new AggregationField(AggFuncEnum.COUNT, null);
        group.addAggregationField(count);
        Assert.assertEquals("AggregationField [func=COUNT, field=null, aliasName=_count]", count.toString());
        
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());

        for (IEntity e : result.getResultSet()) {
            BsonEntity entity = (BsonEntity) e;
            List<?> values = count.getSearchValue(entity);
            Assert.assertEquals(5, values.get(0));
            Assert.assertEquals(3, entity.getNode().size());
            Assert.assertEquals(5, entity.getNode().getInt(count.getFieldName()));
        }
    }

    @Test
    public void test12Min() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField minPorts = createAggregationField(metadata, AggFuncEnum.MIN, "port");
        group.addAggregationField(minPorts);
        AggregationField maxPorts = createAggregationField(metadata, AggFuncEnum.MAX, "port");
        group.addAggregationField(maxPorts);

        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null, projection, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());

        BsonEntity entity = (BsonEntity) result.getResultSet().get(0);
        Assert.assertEquals(4, entity.getNode().size());
        String minPort = entity.getNode().getString(minPorts.getFieldName());
        Assert.assertEquals("8080", minPort);
        
        List<?> minValues = minPorts.getSearchValue(entity);
        Assert.assertEquals("8080", minValues.get(0));
        List<?> maxValues = maxPorts.getSearchValue(entity);
        Assert.assertEquals("8080", maxValues.get(0));
    }

    @Test
    public void test13Sum() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");

        SearchGroup group = new SearchGroup();
        group.addGroupField(createGroupField(metadata, "healthStatus"));
        group.addGroupField(createGroupField(metadata, "manifestDiff"));
        AggregationField sumPorts = createAggregationField(metadata, AggFuncEnum.SUM, "_pversion");
        group.addAggregationField(sumPorts);

        SearchProjection project = new SearchProjection();
        project.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(metadata, null, project, group, null, strategy);
        SearchOption option = new SearchOption();

        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(2, result.getResultSize());

        BsonEntity entity = (BsonEntity) result.getResultSet().get(0);
        Assert.assertEquals(3, entity.getNode().size());

        int sumPort = entity.getNode().getInt(sumPorts.getFieldName());
        System.out.println(sumPort);
        Assert.assertTrue(sumPort == -5);
    }
    
    @Test
    public void test14InvalidGroup() {
        // case1: testing for field grouping availability
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        try {
            new GroupField(metadata.getFieldByName("runsOn"), strategy);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            new GroupField(metadata.getFieldByName("properties"), strategy);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            new GroupField(new SelectionField(metadata.getFieldByName("properties"), strategy));
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            new GroupField((MetaField) null, strategy);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        // case 2: GroupField equality test
        GroupField g1 = new GroupField(metadata.getFieldByName("name"), strategy);
        GroupField g2 = new GroupField(metadata.getFieldByName("https"), strategy);
        Assert.assertTrue(g1.equals(g1));
        Assert.assertFalse(g1.equals(null));
        Assert.assertFalse(g1.equals(new SelectionField(metadata.getFieldByName("name"), strategy)));
        Assert.assertFalse(new SelectionField(metadata.getFieldByName("name"), strategy).equals(g1));
        Assert.assertFalse(g1.equals(g2));
        
        GroupField g3 = new GroupField(metadata.getFieldByName("name"), strategy);
        Assert.assertTrue(g1.equals(g3));
    }

    @Test
    public void aggregationField() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        //case 1: aggregation not supported fields
        try {
            new AggregationField(AggFuncEnum.COUNT, createSearchField(metadata, "name"));
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        try {
            new AggregationField(AggFuncEnum.SUM, createSearchField(metadata, "name"));
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        try {
            new AggregationField(AggFuncEnum.MIN, null);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        //case 2: equality test
        AggregationField af1 = new AggregationField(AggFuncEnum.MAX, createSearchField(metadata, "port"));
        AggregationField af2 = new AggregationField(AggFuncEnum.MAX, createSearchField(metadata, "name"));
        AggregationField af3 = new AggregationField(AggFuncEnum.MIN, createSearchField(metadata, "port"));
        AggregationField count1 = new AggregationField(AggFuncEnum.COUNT, null);
        Assert.assertTrue(af1.equals(af1));
        Assert.assertFalse(af1.equals(null));
        Assert.assertFalse(af1.equals(af2));
        Assert.assertFalse(af1.equals(af3));
        Assert.assertFalse(count1.equals(af1));

        Assert.assertFalse(af1.equals(createSearchField(metadata, "port")));

        Assert.assertTrue(af1.equals(new AggregationField(AggFuncEnum.MAX, createSearchField(metadata, "port"))));
        
        AggregationField count2 = new AggregationField(AggFuncEnum.COUNT, null);
        Assert.assertTrue(count1.equals(count2));
    }
    
}
