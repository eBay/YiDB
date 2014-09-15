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

import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * An context object to store statistics information of parsing and traverseing, and facilitate the validation.
 * 
 * @author xjiang
 *
 */
public class ParseContext {

    private final QueryContext queryContext;
    private String queryString;
    private ParseQueryNode queryNode;
    private int queryNodeCount;
    private int aggregationCount;
    private int projectionCount;
    
    // this setting is set by traversing for aggregation/set check
    private boolean hasSet;

    public ParseContext(QueryContext queryContext) {
        this.queryContext = queryContext;
        aggregationCount = 0;
        projectionCount = 0;
        queryNodeCount = 0;
        hasSet = false;
    }
    
    public QueryContext getQueryContext() {
        return queryContext;
    }
    
    public String getQueryString() {
        return queryString;
    }
    public void setQueryString(String queryString) {
        this.queryString = queryString;
        queryContext.setQueryString(queryString);
    }
    
    public ParseQueryNode getQueryNode() {
        return queryNode;
    }
    public void setQueryNode(ParseQueryNode queryNode) {
        this.queryNode = queryNode;
    }
    
    public void addAggregation() {
        aggregationCount++;
    }
    public int getAggregationCount() {
        return aggregationCount;
    }
    public boolean hasAggregation() {
        return aggregationCount > 0;
    }

    public boolean isCountOnly() {
        return queryContext.isCountOnly();
    }
    

    public int getQueryNodeCount() {
        return queryNodeCount;
    }
    public void addQueryNodeCount() {
        queryNodeCount++;
    }
    
    public boolean hasSet() {
        return hasSet;
    }
    public void setHasSet(boolean hasSet) {
        this.hasSet = hasSet;
    }

    public void addProjection(SearchProjection projection) {
        if (projection.hasStar()) {
            projectionCount++;
        } else {
            projectionCount += projection.getFields().size();
        }
    }
    public int getProjectionCount() {
        return projectionCount;
    }
    
}
