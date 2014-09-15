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

package com.ebay.cloud.cms.dal.search.impl.query;

import java.util.ArrayList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * 
 * @author xjiang
 *
 */
public class EmbedSetQuery implements IEmbedQuery {

    public enum SetOpEnum {
        INTERSECTION, UNION
    }
    
    private final SetOpEnum type;
    private final boolean leftJoin;
    
    private List<IEmbedQuery> childrenQueryList;
    
    public EmbedSetQuery(SetOpEnum type, boolean leftJoin) {
        this.type = type;
        this.leftJoin = leftJoin;
        this.childrenQueryList = new ArrayList<IEmbedQuery>(2);
    }
    
    @Override
    public void addChildQuery(IEmbedQuery childQuery) {
        childrenQueryList.add(childQuery);
    }
    
    @Override
    public MetaClass getMetadata() {
        return null;
    }

    @Override
    public MongoQuery buildMongoQuery(PersistenceContext context) {
        List<DBObject> subQueryList = new ArrayList<DBObject>(2);
        MongoQuery mongoQuery = new MongoQuery();
        mongoQuery.project = new BasicDBObject();
        for (ISearchQuery childSearchQuery : childrenQueryList) {
            MongoQuery childMongoQuery = childSearchQuery.buildMongoQuery(context);
            // selection of child query 
            if (childMongoQuery.match != null) {
                subQueryList.add(childMongoQuery.match);
            }            
            // projection of child query
            if (childMongoQuery.project != null) {
                mongoQuery.project.putAll(childMongoQuery.project);
            }
            // aggregation of child query 
            if (childMongoQuery.group != null || childMongoQuery.groupMatch != null) {
                mongoQuery.group = childMongoQuery.group;
                mongoQuery.preGroupUnwinds = childMongoQuery.preGroupUnwinds;
                mongoQuery.groupMatch = childMongoQuery.groupMatch;
            }
        }
        
        if (!subQueryList.isEmpty()) {
            if (type == SetOpEnum.INTERSECTION) {
                LogicalSearchCriteria andCriteria = new LogicalSearchCriteria(LogicOperatorEnum.AND);
                mongoQuery.match = andCriteria.translate(subQueryList);
            } else if (type == SetOpEnum.UNION) {
                LogicalSearchCriteria orCriteria = new LogicalSearchCriteria(LogicOperatorEnum.OR);
                mongoQuery.match = orCriteria.translate(subQueryList);
            }
        }

        if (leftJoin) {
            // return empty query for left join
            return new MongoQuery();
        } else {
            return mongoQuery;
        }
    }
    
    @Override
    public boolean evaluateEntity(IEntity parentEntity, PersistenceContext context) {
        boolean match;
        if (type == SetOpEnum.INTERSECTION) {
            match = true;
            for (IEmbedQuery childSearchQuery : childrenQueryList) {
                match &= childSearchQuery.evaluateEntity(parentEntity, context);
            }
        } else {
            match = false;
            for (IEmbedQuery childSearchQuery : childrenQueryList) {
                match |= childSearchQuery.evaluateEntity(parentEntity, context);
            }
        }
        return match;
    }

    @Override
    public void appendSearchCriteria(SearchCriteria criteria) {
        // TODO - support partial embed set op        
    }

}
