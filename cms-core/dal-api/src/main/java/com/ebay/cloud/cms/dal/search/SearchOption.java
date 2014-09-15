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

import java.util.ArrayList;
import java.util.List;

import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define the search option 
 * 1. limit:        max number of records to return
 * 2. skip:         number of records to skip, used for pagination
 * 3. explanation:  query execution explanation
 * 
 * @author xjiang
 * 
 */
public final class SearchOption {
    
    public final static Integer ASC_ORDER = Integer.valueOf(1);
    public final static Integer DESC_ORDER = Integer.valueOf(-1);
    public final static int DEFAULT_MAX_LIMIT = 10000;
    public final static int DEFAULT_LIMIT = 1000;
    public static final int LOOK_FORWARD = 1;
    
    private int limit = 0;
    private int skip = 0;
    private List<ISearchField> sortFields = new ArrayList<ISearchField>();
    private List<Integer>  sortOrders = new ArrayList<Integer>();    
    private boolean explanation = false;
    
    private boolean onlyCount = false;
    private MetaClass metaClass = null;
    private boolean isLookForward = false;
    
    private ISearchStrategy strategy = null;
    
    public boolean isOnlyCount() {
        return onlyCount;
    }
    public void setOnlyCount(boolean onlyCount) {
        this.onlyCount = onlyCount;
    }
    public boolean hasLimit() {
        return limit > 0;
    }
    public int getLimit() {
        return limit;
    }
    public void setLimit(final int limit) {
        this.limit = limit;
    }
    public int getDisplayLimit() {
        return limit - LOOK_FORWARD;
    }

    public boolean hasSkip() {
        return skip > 0;
    }
    public int getSkip() {
        return skip;
    }
    public void setSkip(final int skip) {
        this.skip = skip;
    }

    public boolean needExplanation() {
        return this.explanation;
    }
    public void setExplanation() {
        this.explanation = true;
    }
    
    public boolean hasSort() {
        return sortFields != null && !sortFields.isEmpty();
    }
    public DBObject getSort() {
        DBObject sortObj = new BasicDBObject();
        for (int i = 0; i < sortFields.size(); i++) {
            Integer order = sortOrders.size() > i ? sortOrders.get(i) : ASC_ORDER;
            ISearchField field = sortFields.get(i);
            sortObj.put(field.getFullDbName(), order); 
        }
        return sortObj;
    }
  
    public void setSortField(List<ISearchField> sortFields, List<Integer> sortOrders, MetaClass metaClass) {
        this.metaClass  = metaClass;
        this.sortFields = sortFields;
        this.sortOrders = sortOrders;
    }
    public void setSort(List<String> sortFieldNames, List<Integer> sortOrders, MetaClass metaClass) {
        this.metaClass = metaClass;
        this.sortOrders = sortOrders;
        this.sortFields = new ArrayList<ISearchField>();
        for (String fieldName : sortFieldNames) {
            this.sortFields.add(new SelectionField(this.metaClass.getFieldByName(fieldName), strategy));
        }
    }

    public ISearchStrategy getStrategy() {
        return strategy;
    }
    public void setStrategy(ISearchStrategy strategy) {
        this.strategy = strategy;
    }
    public List<ISearchField> getSortFields() {
        return sortFields;
    }
    public List<Integer> getSortOrders() {
        return sortOrders;
    }

    @Override
    public String toString() {
        return "SearchOption [limit=" + limit + ", skip=" + skip
                + ", sortFields=" + sortFields
                + ", sortOrders=" + sortOrders
                + ", explanation=" + explanation + "]";
    }
    public void setLookForward(boolean lookForward) {
        this.isLookForward  = lookForward;
    }
    public boolean isLookForward() {
        return isLookForward;
    }
}
