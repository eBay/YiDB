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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.search.ISearchQuery.MongoQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * The class define the result set and query execution explanation
 * 
 * @author xjiang
 * 
 */
public final class SearchResult {

    public static final class QueryExplanation implements IQueryExplanation {
        private final JsonNode explanationJson;

        public QueryExplanation(final MongoQuery query, SearchOption option, final int count, final long rawCount, final long embedCount, final long usedTime) {
            DBObject explanationBson = new BasicDBObject();
            if (query.match != null) {
                explanationBson.put("criteria", query.match);
            }
            if (query.preGroupUnwinds != null) {
                explanationBson.put("unwind", query.preGroupUnwinds);
            }
            if (query.group != null) {
                explanationBson.put("group", query.group);
            }
            if (query.groupMatch != null) {
                explanationBson.put("groupCriteria", query.groupMatch);
            }
            explanationBson.put("count", count);
            explanationBson.put("rawCount", rawCount);
            explanationBson.put("embedCount after evaluation", embedCount);
            if (option.hasSkip()) {
                explanationBson.put("skip", option.getSkip());
            }
            explanationBson.put("limit", option.getLimit());
            if (option.hasSort()) {
                explanationBson.put("sort", option.getSort());
            }
            explanationBson.put("usedTime", usedTime);
            ObjectMapper jsonMapper = ObjectConverter.mapper;
            try {
                explanationJson = jsonMapper.readTree(explanationBson.toString());
            } catch (IOException e) {
                throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON, e);
            }
        }

        public JsonNode getJsonExplanation() {
            return explanationJson;
        }
    }
    
    private final List<IEntity> resultSet;
    
    // meta class
    private final MetaClass metaclass;

    // the counter for count-only query
    private long count;

    // the raw count of the search result before in-memory filtering
    private long rawCount;
    
    private long embedCount;
    
    private List<IQueryExplanation> explanations = null;

    private boolean hasMore;
    
    private IEntity cursorEntity;
    
    public SearchResult(MetaClass metadata) {
        this.metaclass = metadata;
        this.resultSet = new LinkedList<IEntity>();
        this.explanations = new LinkedList<IQueryExplanation>();
    }

    public void addEntity(final IEntity entity) {
        resultSet.add(entity);
    }

    public List<IEntity> getResultSet() {
        return resultSet;
    }
    
    public void clearResultSet() {
        resultSet.clear();
    }
    
    public long getCount() {
        return count;
    }

    public void setCount(long countResult) {
        this.count = countResult;
    }
    
    public long getRawCount() {
        return rawCount;
    }

    public void setRawCount(long rawCount) {
        this.rawCount = rawCount;
    }

    public int getResultSize() {
        return resultSet.size();
    }

    public boolean isEmpty() {
        return resultSet.isEmpty() && count == 0;
    }
    
    public MetaClass getMetaClass() {
        return metaclass;
    }

    public void setQueryExplanation(final MongoQuery query, final SearchOption option, final int count, long usedTime) {
        explanations.add(new QueryExplanation(query, option, count, rawCount, embedCount, usedTime));
    }
    
    public List<IQueryExplanation> getQueryExplanations() {
        return explanations;
    }

    public void merge(SearchResult partialResult) {
        if (partialResult == null || partialResult.isEmpty()) {
            return;
        }
        this.count += partialResult.count;
        this.resultSet.addAll(partialResult.resultSet);
        this.explanations.addAll(partialResult.getQueryExplanations());
    }

    public long getEmbedCount() {
        return embedCount;
    }

    public void setEmbedCount(long embedCount) {
        this.embedCount = embedCount;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public boolean hasMore() {
        return this.hasMore;
    }

    public IEntity getCursorEntity() {
        return cursorEntity;
    }

    public void setCursorEntity(IEntity cursorEntity) {
        this.cursorEntity = cursorEntity;
    }
}
