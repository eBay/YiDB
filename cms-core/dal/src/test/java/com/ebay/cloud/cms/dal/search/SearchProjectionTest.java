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

package com.ebay.cloud.cms.dal.search;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

public class SearchProjectionTest extends SearchBaseTest {
    private static final Logger logger = LoggerFactory
            .getLogger(SearchProjectionTest.class);
    
    @Test
    public void testProjectionStarField() {
        ISearchField starField = ProjectionField.STAR;
        Assert.assertEquals("*", starField.getFieldName());
        Assert.assertEquals("*", starField.getFullDbName());
        
        try {
            starField.getSearchValue(null);
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            
        }
        
        try {
            starField.setEmbedPath("");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            
        }
    }
    
    @Test
    public void testProjectionField() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:srp-app-00001");
        SearchProjection projection = new SearchProjection();
        Assert.assertTrue(projection.isEmpty());
        projection.addField(createSearchField(metadata, "activeManifestDiff"));
        Assert.assertFalse(projection.isEmpty());
        logger.info(projection.toString());
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        
        SearchResult result = searchService.search(query, option, raptorContext);

        Assert.assertEquals(result.getResultSet().size(), 1);
        IEntity entity = result.getResultSet().get(0);
        List<?> field1 = entity.getFieldValues("activeManifestDiff");
        Assert.assertFalse(field1.isEmpty());
        List<?> field2 = entity.getFieldValues("port");
        Assert.assertTrue(field2.isEmpty());
    }
    
    @Test
    public void testProjectionEmpty() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:srp-app-00001");
        SearchProjection projection = new SearchProjection();             
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 1);
        IEntity entity = result.getResultSet().get(0);
        
        List<?> idField = entity.getFieldValues(InternalFieldEnum.ID.getName());
        Assert.assertFalse(idField.isEmpty());
        
        List<?> typeField = entity.getFieldValues(InternalFieldEnum.TYPE.getName());
        Assert.assertFalse(typeField.isEmpty());
        
        List<?> nameField = entity.getFieldValues("name");
        Assert.assertTrue(nameField.isEmpty());
        
        List<?> lastmodifiedField = entity.getFieldValues(InternalFieldEnum.LASTMODIFIED.getName());
        Assert.assertTrue(lastmodifiedField.isEmpty());
        
        List<?> field2 = entity.getFieldValues("port");
        Assert.assertTrue(field2.isEmpty());
    }

    @Test
    public void testProjectionStar() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:srp-app-00001");
        SearchProjection projection = new SearchProjection();         
        projection.addField(ProjectionField.STAR);
        Assert.assertFalse(projection.isEmpty());
        Assert.assertEquals(projection.toString(), "{selections=*}");
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 1);
        
        IEntity entity = result.getResultSet().get(0);
        System.out.println(entity.getFieldNames());
        Assert.assertEquals(20, entity.getFieldNames().size());
        
        List<?> field1 = entity.getFieldValues("activeManifestDiff");
        Assert.assertEquals(field1.get(0), false);
        List<?> field2 = entity.getFieldValues("port");
        Assert.assertEquals(field2.get(0), "8080");                
    }
    
    @Test
    public void testProjectionStarAndField() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:srp-app-00001");
        SearchProjection projection = new SearchProjection();         
        projection.addField(ProjectionField.STAR);
        projection.addField(createSearchField(metadata, "activeManifestDiff"));
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);       
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 1);
        IEntity entity = result.getResultSet().get(0);
        
        List<?> field1 = entity.getFieldValues("activeManifestDiff");
        Assert.assertFalse(field1.isEmpty());
        List<?> field2 = entity.getFieldValues("port");
        Assert.assertFalse(field2.isEmpty());
    }
    
    @Test
    public void testEmbeddedProjection() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Package");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(createSearchField(metaclass1, "type"));
        projection1.addField(createSearchField(metaclass1, "versions"));
        SearchQuery query1 = new SearchQuery(metaclass1, null,
                projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        IEntity pkgEnt1 = findPackage();
        IEntity verEnt1 = (IEntity)pkgEnt1.getFieldValues("versions").get(0);
        String extVal = (String)verEnt1.getFieldValues("externalId").get(0);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();
        projection2.addField(createSearchField(metaref2.getRefMetaClass(), "externalId"));
        MetaField metaField = metaref2.getRefMetaClass().getFieldByName("externalId");
        SearchCriteria criteria2 = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, extVal);
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), criteria2,
                projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);

        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        Assert.assertTrue(result.getResultSize() > 0);
        IEntity entity = result.getResultSet().get(0);       
        Assert.assertFalse(entity.getFieldValues("type").isEmpty());   
        Assert.assertTrue(entity.getFieldValues("lastModifiedTime").isEmpty());

        List<?> versionField = entity.getFieldValues("versions");                
        Assert.assertEquals(versionField.size(), 1);
        IEntity versionIEntity = (IEntity)versionField.get(0);      
        Assert.assertFalse(versionIEntity.getFieldValues("externalId").isEmpty());   
        Assert.assertTrue(versionIEntity.getFieldValues("metaData").isEmpty());
    }
}
