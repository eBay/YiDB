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

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.JoinSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;

public class JoinCriteriaTest extends SearchBaseTest {
    @Test
    public void testJoinOne() {
        List<IEntity> services = findServiceInstance();
        String id =  services.get(0).getId();
                
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        List<String> valueList = new ArrayList<String>();
        valueList.add(id);
        MetaField metaField = metadata.getFieldByName(InternalFieldEnum.ID.getName());
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);

        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }

    @Test
    public void testJoinMany() {
        List<IEntity> services = findServiceInstance();
        String id1 =  services.get(0).getId();
        String id2 =  services.get(1).getId();
        
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");
        List<String> valueList = new ArrayList<String>();
        valueList.add(id1);
        valueList.add(id2);

        MetaField metaField = metadata.getFieldByName(InternalFieldEnum.ID.getName());
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 2);
    }
    
    /**FIXME: should change interface of JoinSearchCriteria
     * modified by jianxu1
     */
    @Test
    public void testJoinArrayOne01() {
        List<IEntity> services = findServiceInstance();
        String id =  services.get(0).getId();
        
        MetaClass metadata = raptorMetaService.getMetaClass("ApplicationService");
        List<String> valueList = new ArrayList<String>();
        valueList.add(id);

        //SearchCriteria criteria = new JoinSearchCriteria("services", valueList);
        MetaField metaField = metadata.getFieldByName("services");
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }
    
    @Test
    public void testJoinArrayOne02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ApplicationService");
        List<String> valueList = new ArrayList<String>();
        valueList.add("11111111111111111111");

        MetaField metaField = metadata.getFieldByName("services");
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }
    
    /**FIXME: should change interface of JoinSearchCriteria
     * modified by jianxu1
     */
    @Test
    public void testJoinArrayMany01() {
        List<IEntity> services = findServiceInstance();
        String id1 =  services.get(0).getId();       
        
        MetaClass metadata = raptorMetaService.getMetaClass("ApplicationService");
        List<String> valueList = new ArrayList<String>();
        valueList.add(id1);
        valueList.add("11111111111");

        MetaField metaField = metadata.getFieldByName("services");
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 1);
    }
    
    @Test
    public void testJoinArrayMany02() {
        MetaClass metadata = raptorMetaService.getMetaClass("ApplicationService");
        List<String> valueList = new ArrayList<String>();
        valueList.add("111111111");
        valueList.add("222222222");

        MetaField metaField = metadata.getFieldByName("services");
        SearchCriteria criteria = new JoinSearchCriteria(metaField, valueList, strategy);
        SearchProjection projection = new SearchProjection();
        ISearchQuery query = new SearchQuery(metadata, criteria, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        Assert.assertEquals(result.getResultSize(), 0);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testJoinEmpty() {
        MetaField metaField = new MetaAttribute();
        metaField.setName(InternalFieldEnum.ID.getName());
        new JoinSearchCriteria(metaField, new ArrayList<String>(), strategy);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testJoinNull() {
        MetaField metaField = new MetaAttribute();
        metaField.setName(InternalFieldEnum.ID.getName());
        new JoinSearchCriteria(metaField, null, strategy);
    }
}
