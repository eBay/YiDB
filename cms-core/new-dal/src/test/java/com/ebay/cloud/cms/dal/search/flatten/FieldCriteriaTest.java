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

import java.util.ArrayList;
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
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

public class FieldCriteriaTest extends SearchBaseTest {
    
    /*** root search query **/
    
    @Test
    public void testFieldEQ() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("activeManifestDiff");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, true);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 5);
    }

    @Test
    public void testFieldNE() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.NE, "srp-app:srp-app-00001");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 9);
    }

    @Test
    public void testFieldGT() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("_version");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GT, 0);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testFieldLT() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("_version");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LT, 0);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testFieldGE() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("_version");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GE, 0);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.getResultSize(), 10);
    }

    @Test
    public void testFieldLE() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("_version");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LE, 0);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 10);
    }

    @Test
    public void testFieldIN() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        List<Object> valueList = new ArrayList<Object>();
        valueList.add("compute-00001");
        valueList.add("compute-00002");
        valueList.add("compute-00020");
        valueList.add("compute-00030");
        MetaField metaField = metadata.getFieldByName("label");
        SearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaField, strategy),
                FieldOperatorEnum.IN, valueList);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 2);
    }

    @Test
    public void testFieldNIN() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        List<Object> valueList = new ArrayList<Object>();
        valueList.add("compute-00001");
        valueList.add("compute-00002");
        valueList.add("compute-00020");
        valueList.add("compute-00030");
        MetaField metaField = metadata.getFieldByName("label");
        SearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaField, strategy),
                FieldOperatorEnum.NIN, valueList);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 8);
    }

    @Test
    public void testFieldREGEX() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("label");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.REGEX, new RegexValue("compute-0000[1-3]"));
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 3);
    }

    @Test
    public void testFieldNREGEX() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        MetaField metaField = metadata.getFieldByName("label");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.NREGEX, new RegexValue("compute-0000[1-3]"));
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 7);
    }

    @Test
    public void testFieldCONTAINS() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        SearchCriteria criteria = new FieldSearchCriteria(createSearchField(metadata, "fqdn"),
                FieldOperatorEnum.CONTAINS);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 7);
    }
    
    @Test
    public void testFieldInner01() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("properties");
        SearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaField, "f1", strategy),
                FieldOperatorEnum.CONTAINS);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }
    
    @Test
    public void testFieldInner02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("manifestRef");
        SearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaField, FieldProperty.LENGTH.getName(), strategy),
                FieldOperatorEnum.GT, 1);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 10);
    }
    
    @Test
    public void testFieldInner03() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("properties");
        SearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaField, "f1", strategy),
                FieldOperatorEnum.GT, 1);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }

    @Test
    public void testFieldNCONTAINS() {
        MetaClass metadata = raptorMetaService.getMetaClass("Compute");
        SearchCriteria criteria = new FieldSearchCriteria(createSearchField(metadata, "fqdn"),
                FieldOperatorEnum.NCONTAINS);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 3);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testFieldScalar0() {
        MetaField metaField = new MetaAttribute();
        metaField.setName("test1");
        new FieldSearchCriteria(new SelectionField(metaField, strategy), FieldOperatorEnum.EQ);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testFieldScalar1() {
        MetaField metaField = new MetaAttribute();
        metaField.setName("test1");
        new FieldSearchCriteria(new SelectionField(metaField, strategy), FieldOperatorEnum.EQ, new ArrayList<Object>());
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testFieldScalar2() {
        MetaField metaField = new MetaAttribute();
        metaField.setName("test1");
        new FieldSearchCriteria(metaField, strategy, FieldOperatorEnum.IN, "value1");
    }
    
//    @Test (expected=NullPointerException.class)
//    public void testFieldNull() {
//        new FieldSearchCriteria(null, FieldOperatorEnum.CONTAINS);
//    }
    
    /*** embedded search query **/    
    @Test
    public void testEmbedEQ01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        MetaClass metaclass2 = metaref2.getRefMetaClass();
        
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaclass2.getFieldByName("name");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "Dummy ManifestVersion Bundle-1-0001");
        SearchQuery query2 = new SearchQuery(metaclass2, criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();
        // query on parent collection
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity manEnt = result.getResultSet().get(0);
        List<?> verEntList = manEnt.getFieldValues("versions");
        Assert.assertEquals(2, verEntList.size());
        IEntity verEnt = (IEntity)verEntList.get(0);
        Assert.assertEquals(verEnt.getFieldValues("name").get(0), "Dummy ManifestVersion Bundle-1-0001");
    }
    
    /**
     * modified by jianxu1
     */
    @Test
    public void testEmbedEQ02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);

        SearchProjection projection2 = new SearchProjection();  
        //SearchCriteria criteria2 = new FieldSearchCriteria("packages",FieldOperatorEnum.EQ, "4fbdaccec681643199735a60");
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        //EmbeddedQuery query2 = new EmbeddedQuery(metaref2, criteria2,projection2);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(),null,projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        SearchProjection projection3 = new SearchProjection();  
        MetaClass versionCls = metaref2.getRefMetaClass();
        MetaRelationship packagesField = (MetaRelationship)versionCls.getFieldByName("packages");
        MetaField metaField = versionCls.getFieldByName(InternalFieldEnum.ID.getName());
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);
        IEntity pkgEnt1 = (IEntity)verEnt1.getFieldValues("packages").get(0);
        String pkgId = pkgEnt1.getId();
        SearchCriteria criteria3 = new FieldSearchCriteria(metaField, strategy, FieldOperatorEnum.EQ, pkgId);
        SearchQuery query3 = new SearchQuery(packagesField.getRefMetaClass(), criteria3, projection3, strategy);
        embedFieldList.add(packagesField);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);
                      
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(1, result.getResultSize());
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        Assert.assertEquals(2, verEntList.size());      
    }       
    
    @Test
    public void testEmbedNE01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("name");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.NE, "Dummy ManifestVersion Bundle-0-0003");
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        Assert.assertEquals(verEntList.size(), 2);
        IEntity verEnt = (IEntity)verEntList.get(0);
        Assert.assertEquals(verEnt.getFieldValues("name").get(0), "Dummy ManifestVersion Bundle-1-0001");
    }
    
    @Test
    public void testEmbedNE02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("name");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.NE, "Dummy ManifestVersion Bundle-1-0001");
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(1, result.getResultSize());        
    }
    
    @Test
    public void testEmbedComp01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GT, new Date(System.currentTimeMillis() + 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testEmbedComp02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GT, new Object());
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        searchService.search(embedQuery1, option, deployContext);
    }
    
    @Test
    public void testEmbedGT01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GT, new Date(System.currentTimeMillis() - 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    public void testEmbedGT02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GT, new Date(System.currentTimeMillis() + 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);  
    }
    
    @Test
    public void testEmbedGT03() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        //SearchCriteria criteria2 = new FieldSearchCriteria("packages",FieldOperatorEnum.GT, "4fbdaccec681643199735a50");
        //EmbeddedQuery query2 = new EmbeddedQuery(metaref2, criteria2,projection2);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(),null,projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        //jianxu1: add
        MetaClass versionCls = metaref2.getRefMetaClass();
        MetaRelationship packagesField = (MetaRelationship)versionCls.getFieldByName("packages");
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
        SearchCriteria criteria3 = new FieldSearchCriteria(metaField, strategy, FieldOperatorEnum.GT, "4fbdaccec681643199735a50");
        SearchQuery query3 = new SearchQuery(packagesField.getRefMetaClass(),criteria3,projection2, strategy);
        embedFieldList.add(packagesField);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);

        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        int matchVersionCount = verEntList.size();
        //Assert.assertEquals(verEntList.size(), 0);    
        Assert.assertEquals(2, matchVersionCount); 
    }
    
    @Test
    public void testEmbedGE01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GE, new Date(1337830606378L));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    public void testEmbedGE02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.GE, new Date(9337830606378L));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);      
    }
    
    @Test
    public void testEmbedLT01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LT, new Date(System.currentTimeMillis() - 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);      
    }
    
    @Test
    public void testEmbedLT02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LT, new Date(System.currentTimeMillis() + 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);      
    }
    
    @Test
    public void testEmbedLE01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LE, new Date(System.currentTimeMillis() - 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);               
    }
    
    @Test
    public void testEmbedLE02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("createdTime");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.LE, new Date(System.currentTimeMillis() + 1000000));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);               
    }
    
    
    @Test
    public void testEmbedIN01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);
        IEntity verEnt2 = (IEntity)manEnt1.getFieldValues("versions").get(1);
        String desc1 = (String)verEnt1.getFieldValues("description").get(0);
        String desc2 = (String)verEnt2.getFieldValues("description").get(0);
        List<Object> valueList = new ArrayList<Object>();        
        valueList.add(desc1);
        valueList.add(desc2);
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("description");
        SearchCriteria criteria2 = new FieldSearchCriteria(new SelectionField(metaField, strategy),
                FieldOperatorEnum.IN, valueList);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    
    /**
     * modified by jianxu1
     */
    @Test
    public void testEmbedIN02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(),null,projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        MetaClass versionCls = metaref2.getRefMetaClass();
        MetaRelationship packagesField = (MetaRelationship)versionCls.getFieldByName("packages");
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
        
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);
        IEntity pkgEnt1 = (IEntity)verEnt1.getFieldValues("packages").get(0);
        IEntity pkgEnt2 = (IEntity)verEnt1.getFieldValues("packages").get(1);
        List<Object> valueList = new ArrayList<Object>();
        valueList.add(pkgEnt1.getId());
        valueList.add(pkgEnt2.getId());
        
        SearchCriteria criteria3 = new FieldSearchCriteria(new SelectionField(metaField, strategy), FieldOperatorEnum.IN, valueList);
        SearchQuery query3 = new SearchQuery(packagesField.getRefMetaClass(),criteria3,projection2, strategy);
        embedFieldList.add(packagesField);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);
        
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity manEnt = result.getResultSet().get(0);
        IEntity verEnt = (IEntity)manEnt.getFieldValues("versions").get(0);
        List<?> pkgEntList = verEnt.getFieldValues("packages");
        Assert.assertEquals(3, pkgEntList.size());  
    }
    
    @Test
    public void testEmbedNIN01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);               
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);   
        String desc1 = (String)verEnt1.getFieldValues("description").get(0);
        List<Object> valueList = new ArrayList<Object>();
        valueList.add(desc1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("description");
        SearchCriteria criteria2 = new FieldSearchCriteria(new SelectionField(metaField, strategy),
                FieldOperatorEnum.NIN, valueList);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(1, result.getResultSize());
    }
    
    @Test
    public void testEmbedNIN02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);               
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("description");
        List<Object> valueList = new ArrayList<Object>();
        valueList.add("testXYZ");
        SearchCriteria criteria2 = new FieldSearchCriteria(new SelectionField(metaField, strategy),
                FieldOperatorEnum.NIN, valueList);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }
    
    @Test
    public void testEmbedCONTAINS01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        SearchCriteria criteria2 = new FieldSearchCriteria(createSearchField(metaref2.getRefMetaClass(), "createdTime"),
                FieldOperatorEnum.CONTAINS);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    public void testEmbedCONTAINS02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();
        SearchCriteria criteria2 = new FieldSearchCriteria(createSearchField(metaref2.getRefMetaClass(), "packages"),        
                FieldOperatorEnum.CONTAINS);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    public void testEmbedNCONTAINS01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        SearchCriteria criteria2 = new FieldSearchCriteria(createSearchField(metaref2.getRefMetaClass(), "createdTime"),
                FieldOperatorEnum.NCONTAINS);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);     
    }
    
    @Test
    public void testEmbedNCONTAINS02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        SearchCriteria criteria2 = new FieldSearchCriteria(createSearchField(metaref2.getRefMetaClass(), "createdBy"),
                FieldOperatorEnum.NCONTAINS);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);     
    }
    
    @Test
    public void testEmbedREGEX01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("description");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.REGEX, new RegexValue("Dummy description-.*"));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
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
    public void testEmbedREGEX02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(),null,projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        MetaClass versionCls = metaref2.getRefMetaClass();
        MetaRelationship packagesField = (MetaRelationship)versionCls.getFieldByName("packages");
        
        
        MetaField metaField = packagesField.getRefMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);
        IEntity pkgEnt1 = (IEntity)verEnt1.getFieldValues("packages").get(0);
        String id1 = pkgEnt1.getId();
        String idreg = id1.substring(0, id1.length() - 2) + ".*";
        SearchCriteria criteria3 = new FieldSearchCriteria(metaField, strategy, FieldOperatorEnum.REGEX, new RegexValue(idreg));
        SearchQuery query3 = new SearchQuery(packagesField.getRefMetaClass(),criteria3,projection2, strategy);
        embedFieldList.add(packagesField);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);
        
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 1);
        
        IEntity pkgEnt = result.getResultSet().get(0);
        List<?> verEntList = pkgEnt.getFieldValues("versions");
        Assert.assertTrue(verEntList.size() > 0);
    }
    
    @Test
    public void testEmbedNREGEX01() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("description");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,        
                FieldOperatorEnum.NREGEX, new RegexValue("Dummy description-.*"));
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);
            
    }
    
    @Test
    public void testEmbedNREGEX02() {
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(),null,projection2, strategy);
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
               
        MetaClass versionCls = metaref2.getRefMetaClass();
        MetaRelationship packagesField = (MetaRelationship)versionCls.getFieldByName("packages");
        
        
        MetaField metaField = packagesField.getRefMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
        IEntity manEnt1 = findManifest();
        IEntity verEnt1 = (IEntity)manEnt1.getFieldValues("versions").get(0);
        IEntity pkgEnt1 = (IEntity)verEnt1.getFieldValues("packages").get(0);
        String id1 = pkgEnt1.getId();
        String idreg = id1.substring(0, id1.length() - 2) + ".*";
        SearchCriteria criteria3 = new FieldSearchCriteria(metaField, strategy, FieldOperatorEnum.NREGEX, new RegexValue(idreg));
        SearchQuery query3 = new SearchQuery(packagesField.getRefMetaClass(),criteria3,projection2, strategy);
        embedFieldList.add(packagesField);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);
        
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }

}
