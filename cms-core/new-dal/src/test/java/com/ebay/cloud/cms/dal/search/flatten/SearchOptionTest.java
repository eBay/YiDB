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
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;

public class SearchOptionTest extends SearchBaseTest {
    
    private static final Logger logger = LoggerFactory
            .getLogger(SearchOptionTest.class);
    
    @Test
    public void testOptionLimit() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null,
                projection, strategy);
        SearchOption option = new SearchOption();
        option.setLimit(3);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(3, result.getResultSet().size());
    }

    @Test
    public void testOptionSkip() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null,
                projection, strategy);
        SearchOption option = new SearchOption();
        option.setSkip(3);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 7);
    }
    
    @Test
    public void testOptionOnlyCount() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null,
                projection, strategy);
        SearchOption option = new SearchOption();
        option.setOnlyCount(true);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getCount(), 10);
        Assert.assertEquals(result.getResultSet().size(), 0);
    }
    
    @Test
    public void testOptionSort01() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        projection.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(metadata, null,
                projection, strategy);
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        List<String> sortFields = new ArrayList<String>();
        sortFields.add("name");
        sortFields.add("port");
        List<Integer> sortOrders = new ArrayList<Integer>();
        sortOrders.add(SearchOption.DESC_ORDER);
        sortOrders.add(SearchOption.DESC_ORDER);        
        option.setSort(sortFields, sortOrders, metadata);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 10);
        String name0 = (String) result.getResultSet().get(0).getFieldValues("name").get(0);
        String name9 = (String) result.getResultSet().get(9).getFieldValues("name").get(0);
        Assert.assertTrue(name0.compareTo(name9) > 0);
        for (IEntity e : result.getResultSet()) {
            System.out.print(e.getFieldValues("name"));
            System.out.print("\t");
            System.out.println(e.getFieldValues("port"));
        }
    }
    
    @Test
    public void testOptionSort02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, null,
                projection, strategy);
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        List<String> sortFields = new ArrayList<String>();
        sortFields.add("https");
        sortFields.add("port");
        List<Integer> sortOrders = new ArrayList<Integer>();
        sortOrders.add(SearchOption.DESC_ORDER);
        sortOrders.add(SearchOption.ASC_ORDER);        
        option.setSort(sortFields, sortOrders, metadata);
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSet().size(), 10);
    }


    @Test
    public void testOptionExplanation() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:Raptor-00001");
        
        SearchProjection projection = new SearchProjection();
        projection.addField(createSearchField(metadata, "activeManifestDiff"));
        
        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        option.setExplanation();
        
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertNotNull(result.getQueryExplanations());
        logger.info(result.getQueryExplanations().get(0).getJsonExplanation().toString());
    }
    
    @Test
    public void testOptionExplanationEmpty() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        MetaField metaField = metadata.getFieldByName("name");
        SearchCriteria criteria = new FieldSearchCriteria(metaField, strategy,
                FieldOperatorEnum.EQ, "srp-app:Raptor-00001");
        SearchProjection projection = new SearchProjection();

        ISearchQuery query = new SearchQuery(metadata,
                criteria, projection, strategy);
        
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        option.setExplanation();
        SearchResult result = searchService.search(query, option, raptorContext);

        Assert.assertNotNull(result.getQueryExplanations());
        logger.info(result.getQueryExplanations().get(0).getJsonExplanation().toString());
    }
    
    @Test
    public void testToString() {
        SearchOption option = new SearchOption();
        Assert.assertNotNull(option.toString());
    }
}
