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

package com.ebay.cloud.cms.query.executor.result;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.QueryContext.QueryCursor;

public class QueryResult implements IQueryResult {

    private boolean                 hasmore;
    private List<IEntity>           entities;
    private List<IQueryExplanation> explanations;
    private long                    count;
    private QueryCursor             nextCursor;
    private ArrayNode               displayMeta;

    public QueryResult() {
        this.entities = new LinkedList<IEntity>();
        hasmore = false;
    }

    public void addEntity(IEntity entity) {
        entities.add(entity);
    }

    public List<IEntity> getEntities() {
        return entities;
    }

    public List<IQueryExplanation> getExplanations() {
        return explanations;
    }

    public void addExplanations(List<IQueryExplanation> explanations) {
        if (this.explanations == null) {
            this.explanations = new LinkedList<IQueryExplanation>();
        }
        this.explanations.addAll(explanations);
    }

    public boolean hasMoreResults() {
        return hasmore;
    }

    public void setMoreResults() {
        hasmore = true;
    }

    public ArrayNode getDisplayMeta() {
        return displayMeta;
    }

    public void addDisplayMeta(ArrayNode displayNode) {
        if (displayMeta == null) {
            displayMeta = JsonNodeFactory.instance.arrayNode();
        }
        displayMeta.addAll(displayNode);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("QueryResult");
        sb.append(", hasmore=").append(hasmore);
        sb.append(", entities=").append(entities);
        sb.append(", count=").append(count);
        if (nextCursor != null) {
            sb.append(", nextSkips=").append(Arrays.toString(nextCursor.getSkips()));
            sb.append(", nextLimits=").append(Arrays.toString(nextCursor.getLimits()));
            sb.append(", nextCursorValues=").append(nextCursor.getJoinCursorValues());
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public long getCount() {
        return this.count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public QueryCursor getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(QueryCursor nextCursor) {
        this.nextCursor = nextCursor;
    }

}
