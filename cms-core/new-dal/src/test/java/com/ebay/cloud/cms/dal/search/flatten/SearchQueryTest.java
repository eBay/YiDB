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

package com.ebay.cloud.cms.dal.search.flatten;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

public class SearchQueryTest extends SearchBaseTest {

    @Test
    public void testSearchQuery01() {
        MetaClass metadata = raptorMetaService.getMetaClass("ApplicationService");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }

    @Test
    public void testSearchQuery02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        projection.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(metadata, null, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 10);
    }

    /**
     * jianxu1: 2012/12/14
     * the original intention is to check PersistenContext's Collection Split Policy
     * because raptorContext is Split policy, so given null meta data will trigger Exception in Context.getCollection(null)
     * after jianxu1 add checkPreconditions in constructor of AbstractQuery, IllegalArgumentException is throw that place
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSearchQuery03() {
        ISearchQuery query = new SearchQuery(null, null, null, null);
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }

    /**
     * because deployContext has DBCollectionPolicy.Merged, so even given null meta data, we can still work
     * TODO: remove check meta data in getDBCollectionName because that will never happen after
     * jianxu1 add check in constructor in AbstractQuery
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSearchQuery04() {
        ISearchQuery query = new SearchQuery(null, null, null, null);
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        SearchResult result = searchService.search(query, option, deployContext);
        Assert.assertEquals(8, result.getResultSize());
    }

    @Test
    public void testEmbeddedQuery01() {
        MetaClass manifestMetadata = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection = new SearchProjection();
        projection.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(manifestMetadata, null, projection, strategy);
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        SearchResult result = searchService.search(query, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);

        IEntity entity = result.getResultSet().get(0);
        List<?> versionField = entity.getFieldValues("versions");
        Assert.assertEquals(versionField.size(), 2);
        IEntity versionIEntity = (IEntity) versionField.get(0);

        List<?> nameField = versionIEntity.getFieldValues("name");
        Assert.assertFalse(nameField.isEmpty());
    }

    @Test
    public void testEmbeddedQuery02() {
        MetaClass manifestMeta = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);
        SearchQuery query1 = new SearchQuery(manifestMeta, null, projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship versionMeta = (MetaRelationship) manifestMeta.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();
        projection2.addField(ProjectionField.STAR);
        SearchQuery query2 = new SearchQuery(versionMeta.getRefMetaClass(), null, projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(versionMeta);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);

        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);

        IEntity entity = result.getResultSet().get(0);
        List<?> versionField = entity.getFieldValues("versions");
        Assert.assertEquals(versionField.size(), 2);
        IEntity versionIEntity = (IEntity) versionField.get(0);
        List<?> nameField = versionIEntity.getFieldValues("name");
        Assert.assertFalse(nameField.isEmpty());
    }

    @Test
    public void testEmbeddedCriteria03() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);
        MetaField metaField1 = metaclass1.getFieldByName("name");
        SearchCriteria criteria1 = new FieldSearchCriteria(metaField1, strategy, FieldOperatorEnum.EQ,
                "Dummy Manifest Bundle-1-0001");
        SearchQuery query1 = new SearchQuery(metaclass1, criteria1, projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship) metaclass1.getFieldByName("versions");
        MetaClass metaclass2 = metaref2.getRefMetaClass();
        SearchProjection projection2 = new SearchProjection();
        MetaField metaField2 = metaclass2.getFieldByName("name");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField2, strategy, FieldOperatorEnum.EQ,
                "Dummy ManifestVersion Bundle-1-0001");
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2, projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);

        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity) manEnt1.getFieldValues("versions").get(0);
        IEntity aprEnt1 = (IEntity) verEnt1.getFieldValues("approvals").get(0);
        MetaRelationship metaref3 = (MetaRelationship) metaclass2.getFieldByName("approvals");
        SearchProjection projection3 = new SearchProjection();
        MetaField metaField3 = metaref3.getRefMetaClass().getFieldByName("name");
        SearchCriteria criteria3 = new FieldSearchCriteria(metaField3, strategy, FieldOperatorEnum.EQ, aprEnt1.getFieldValues(
                "name").get(0));
        SearchQuery query3 = new SearchQuery(metaref3.getRefMetaClass(), criteria3, projection3, strategy);
        embedFieldList.add(metaref3);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);

        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }
    
    @Test
    public void searchFieldEquality() {
        MetaClass metaclass = deployMetaService.getMetaClass("Manifest");
        MetaField metaField = metaclass.getFieldByName("name");
        AbstractSearchField sf1 = new SelectionField(metaField, strategy);
        AbstractSearchField sf12 = new SelectionField(metaField, strategy);
        AbstractSearchField sf2 = new SelectionField(metaField, FieldProperty.LENGTH.getName(), strategy);
        AbstractSearchField sf3 = new SelectionField(metaField, FieldProperty.TIMESTAMP.getName(), strategy);
        
        Assert.assertTrue(sf1.equals(sf1));
        Assert.assertTrue(sf1.equals(sf12));
        Assert.assertFalse(sf1.equals(null));
        
        Assert.assertFalse(sf1.equals(sf2));
        Assert.assertFalse(sf1.equals(sf3));
        
        Assert.assertFalse(sf2.equals(sf1));
        Assert.assertFalse(sf2.equals(sf3));

        Assert.assertFalse(sf1.equals(new AggregationField(AggFuncEnum.MAX, sf1)));
    }
}
