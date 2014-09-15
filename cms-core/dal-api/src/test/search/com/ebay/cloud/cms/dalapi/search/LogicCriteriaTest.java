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

import java.util.Date;
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
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.RegexValue;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

public class LogicCriteriaTest extends SearchBaseTest {

    @Test
    public void testLogicAND() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField1 = metadata.getFieldByName("https");
        SearchCriteria criteria1 = new FieldSearchCriteria(metaField1, strategy,
                FieldOperatorEnum.EQ, true);
        MetaField metaField2 = metadata.getFieldByName("activeManifestDiff");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField2, strategy,
                FieldOperatorEnum.EQ, true);
        LogicalSearchCriteria criteria3 = new LogicalSearchCriteria(
                LogicOperatorEnum.AND);
        criteria3.addChild(criteria1);
        criteria3.addChild(criteria2);        
        Assert.assertEquals(criteria3.getOperator(), LogicOperatorEnum.AND);
        
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria3, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 4);
    }
    
    @Test
    public void testLogicOR() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField1 = metadata.getFieldByName("https");
        SearchCriteria criteria1 = new FieldSearchCriteria(metaField1,strategy,
                FieldOperatorEnum.EQ, true);
        MetaField metaField2 = metadata.getFieldByName("activeManifestDiff");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField2, strategy,
                FieldOperatorEnum.EQ, true);
        LogicalSearchCriteria criteria3 = new LogicalSearchCriteria(
                LogicOperatorEnum.OR);
        criteria3.addChild(criteria1);
        criteria3.addChild(criteria2);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria3, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 8);
    }
    
    @Test
    public void testEmbedAnd01() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null, 
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField1 = metaref2.getRefMetaClass().getFieldByName("description");                        
        SearchCriteria criteria21 = new FieldSearchCriteria(metaField1, strategy,
                FieldOperatorEnum.REGEX, new RegexValue("Dummy description-.*"));
        MetaField metaField2 = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria22 = new FieldSearchCriteria(metaField2,strategy,
                FieldOperatorEnum.LE, new Date());
        LogicalSearchCriteria criteria20 = new LogicalSearchCriteria(
                LogicOperatorEnum.AND);
        criteria20.addChild(criteria21);
        criteria20.addChild(criteria22);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria20,
                projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(1, result.getResultSize());
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        Assert.assertEquals(verEntList.size(), 2);
    }
    
    @Test
    public void testEmbedAnd02() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField1 = metaref2.getRefMetaClass().getFieldByName("description");                        
        SearchCriteria criteria21 = new FieldSearchCriteria(metaField1, strategy,
                FieldOperatorEnum.REGEX, new RegexValue("Dummy description-.*"));
        MetaField metaField2 = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria22 = new FieldSearchCriteria(metaField2, strategy,
                FieldOperatorEnum.GT, new Date());
        LogicalSearchCriteria criteria20 = new LogicalSearchCriteria(
                LogicOperatorEnum.AND);
        criteria20.addChild(criteria21);
        criteria20.addChild(criteria22);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria20,
                projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }
    
    @Test
    public void testEmbedOr01() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField1 = metaref2.getRefMetaClass().getFieldByName("description");                        
        SearchCriteria criteria21 = new FieldSearchCriteria(metaField1, strategy,
                FieldOperatorEnum.REGEX, new RegexValue("Dummy description-.*"));
        MetaField metaField2 = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria22 = new FieldSearchCriteria(metaField2, strategy,
                FieldOperatorEnum.GT, new Date());
        LogicalSearchCriteria criteria20 = new LogicalSearchCriteria(
                LogicOperatorEnum.OR);
        criteria20.addChild(criteria21);
        criteria20.addChild(criteria22);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria20,
                projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        Assert.assertEquals(verEntList.size(), 2);        
    }
    
    @Test
    public void testEmbedOr02() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField1 = metaref2.getRefMetaClass().getFieldByName("description");                        
        SearchCriteria criteria21 = new FieldSearchCriteria(metaField1, strategy,
                FieldOperatorEnum.NREGEX, new RegexValue("Dummy description-.*"));
        MetaField metaField2 = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria22 = new FieldSearchCriteria(metaField2, strategy,
                FieldOperatorEnum.GT, new Date());
        LogicalSearchCriteria criteria20 = new LogicalSearchCriteria(
                LogicOperatorEnum.OR);
        criteria20.addChild(criteria21);
        criteria20.addChild(criteria22);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria20,
                projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);     
    }
}
