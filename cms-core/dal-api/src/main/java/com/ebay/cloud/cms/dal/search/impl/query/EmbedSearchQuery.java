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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * embed search query
 * 
 * @author xjiang
 *
 */
// TODO aggregation on list field of embed
public class EmbedSearchQuery extends SearchQuery implements IEmbedQuery {
    
    private static final String DOLLAR_PROJECT = "$project";
    private static final String DOLLAR_MATCH = "$match";
    private final String embedPath;
    private final String embedFieldName;
    // whether this embed query to its containing query is left-outer joined
    private final boolean leftJoin;
    // group flag to indicate that current embed query has group or its child query has group
    private boolean hasGroup;
    private IEmbedQuery childQuery;
    private SearchResult searchResult;
    
    public EmbedSearchQuery(SearchQuery query, LinkedList<MetaRelationship> embedFields, String embedFieldName, 
            boolean leftJoin) {
        super(query.getMetadata(), query, query.getStrategy());
        this.embedPath = generateEmbedPath(query, embedFields);
        this.embedFieldName = embedFieldName;
        this.leftJoin = leftJoin;
        this.childQuery = null;
        this.searchResult = new SearchResult(query.getMetadata());
    }
    
    private String generateEmbedPath(SearchQuery query, LinkedList<MetaRelationship> embedFields) {
        StringBuilder dbPathBuilder = new StringBuilder();
        if (embedFields != null && !embedFields.isEmpty()) {
            for (MetaRelationship metaRef : embedFields) {
                dbPathBuilder.append(strategy.getFieldValueName(metaRef));
                dbPathBuilder.append(".");          
                
                SearchCriteria criteria = query.getSearchCriteria();
                if(criteria instanceof FieldSearchCriteria){
                    FieldSearchCriteria fsc = (FieldSearchCriteria) criteria;
                    fsc.setEmbeddedObject(true);
                    if(metaRef.getCardinality() == CardinalityEnum.Many){
                        fsc.setEmbeddedCardinalityMany(true);
                    }
                }
            }
            return dbPathBuilder.toString();
        } else {
            return null;
        }
    }

    public EmbedSearchQuery(SearchQuery query, LinkedList<MetaRelationship> embedFields, boolean leftJoin) {
        super(query.getMetadata(), query, query.getStrategy());
        if (embedFields != null && !embedFields.isEmpty()) {
            this.embedPath = generateEmbedPath(query, embedFields);
            this.embedFieldName = embedFields.getLast().getName();
        } else {
            this.embedPath = null;
            this.embedFieldName = null;
        }
        this.leftJoin = leftJoin;
        this.childQuery = null;
        this.searchResult = new SearchResult(query.getMetadata());
    }
    
    public EmbedSearchQuery(SearchQuery query) {
        this(query, null, false);
    }

    public EmbedSearchQuery(SearchQuery query, LinkedList<MetaRelationship> embedFieldList) {
        this(query, embedFieldList, false);
    }

    @Override
    public void addChildQuery(IEmbedQuery childQuery) {
        this.childQuery = childQuery;
    }
    
    public IEmbedQuery getChildQuery() {
        return childQuery;
    }
    
    public void setSearchResult(SearchResult result) {
        this.searchResult = result;
    }
    
    public SearchResult getSearchResult() {
        return searchResult;
    }
    
    public String getEmbedFieldName() {
        return embedFieldName;
    }
    
    @Override
    protected void rewriteQuery(PersistenceContext context) {
        super.rewriteQuery(context);
        rewriteEmbedPath();
    }

    
    public MongoQuery buildMongoQuery(PersistenceContext context) {
        MongoQuery emptyQuery = new MongoQuery();

        // pre-order : generate mongo query of current search query
        MongoQuery mongoQuery = super.buildMongoQuery(context);
        // traversal child embed query
        MongoQuery childMongoQuery = null;
        if (this.childQuery != null) {
            childMongoQuery = childQuery.buildMongoQuery(context);
        }

        if (leftJoin) {
            // for outer_join, we need bypass the child query
            return emptyQuery;
        } else if (this.childQuery == null) {
            return mongoQuery;
        }

        hasGroup = childMongoQuery.group != null || mongoQuery.group != null;
        // trim operator before further operation
        mongoQuery.match = trimOperator(mongoQuery.match, DOLLAR_MATCH);
        mongoQuery.project = trimOperator(mongoQuery.project, DOLLAR_PROJECT);
        childMongoQuery.match = trimOperator(childMongoQuery.match, DOLLAR_MATCH);
        childMongoQuery.project = trimOperator(childMongoQuery.project, DOLLAR_PROJECT);

        // projection of child query
        // rule 1 : group query decide the project if group exists
        // rule 2 : merge projection if no group
        if (childMongoQuery.group != null) {
            // group query decides the projection
            mongoQuery.project = childMongoQuery.project;
        } else if (!hasGroup && childMongoQuery.project != null) {
            // merge projections
            mongoQuery.project.putAll(childMongoQuery.project);
        }
        // aggregation of child query 
        if (childMongoQuery.group != null || childMongoQuery.groupMatch != null) {
            mongoQuery.group = childMongoQuery.group;
            mongoQuery.preGroupUnwinds = childMongoQuery.preGroupUnwinds;
            mongoQuery.groupMatch = childMongoQuery.groupMatch;
        }
        // selection of child query
        if (childMongoQuery.match != null) {
            if (mongoQuery.match == null) {
                mongoQuery.match = childMongoQuery.match;
            } else {
                List<DBObject> queryMatchList = new ArrayList<DBObject>(2);
                queryMatchList.add(mongoQuery.match);
                queryMatchList.add(childMongoQuery.match);
                LogicalSearchCriteria andCriteria = new LogicalSearchCriteria(LogicOperatorEnum.AND);
                mongoQuery.match = andCriteria.translate(queryMatchList);
            }            
        }
        
        if (hasGroup) {
            DBObject match = new BasicDBObject();
            match.put(DOLLAR_MATCH, mongoQuery.match);
            mongoQuery.match = match;
            DBObject project = new BasicDBObject();
            project.put(DOLLAR_PROJECT, mongoQuery.project);
            mongoQuery.project = project;
        }
        return mongoQuery;
    }
    
    private DBObject trimOperator(DBObject match, String operator) {
        if (match == null) {
            return null;
        }
        return match.containsField(operator) ? (DBObject) match.get(operator) : match;
    }
    
    public void evaluate(List<? extends IEntity> resultSet, PersistenceContext context) {
        if (resultSet == null || childQuery == null) {
            return;
        }
        
        Iterator<? extends IEntity> iter = resultSet.iterator();
        while (iter.hasNext()) {
            IEntity entity = iter.next();
            if (!childQuery.evaluateEntity(entity, context) && !leftJoin) {
                iter.remove();
            }
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean evaluateEntity(IEntity parentEntity, PersistenceContext context) {
        if (hasGroup) {
            // group evaluation : populate entity down from the root search query
            IEntity entity = context.getEntityFactory().createEntity(searchResult.getMetaClass(), parentEntity.getNode());
            searchResult.addEntity(entity);
            if (childQuery != null) {
                childQuery.evaluateEntity(entity, context);
            }
            return true;
        }
        
        if (embedFieldName == null) {
            return true;
        }

        List<IEntity> fieldValues = (List<IEntity>)parentEntity.getFieldValues(embedFieldName);
        if (fieldValues == null) {
            return false;
        }
        Iterator<? extends IEntity> iter = fieldValues.iterator();
        while (iter.hasNext()) {
            IEntity currentEntity = iter.next();
            // evaluate on current search query
            boolean match = evaluateCriteria(currentEntity, getSearchCriteria());
            // evaluate on child query
            if (match && childQuery != null) {
                match &= childQuery.evaluateEntity(currentEntity, context); 
            }
            if (match) {
                BasicDBObject obj = (BasicDBObject)((BasicDBObject)currentEntity.getNode()).copy();
                searchResult.addEntity(context.getEntityFactory().createEntity(currentEntity.getMetaClass(), obj));
            }
        }

        if (!leftJoin) {
            // set it back to update bson entity
            return !fieldValues.isEmpty();
        } else {
            // left outer join doesn't affect the parent result.
            return true;
        }
    }
    
    private boolean evaluateCriteria(IEntity entity, SearchCriteria criteria) {
        if (criteria == null)
            return true;
        return criteria.evaluate(entity);
    }
    
    private void rewriteEmbedPath() {
        if (embedPath == null || embedPath.isEmpty()) {
            return;
        }
        rewriteSearchProjection(super.getSearchProjection());
        rewriteSearchCriteria(super.getSearchCriteria(), super.getSearchProjection());
        rewriteSearchGroup(super.getSearchGroup());
    }
    
    private void rewriteSearchProjection(SearchProjection searchProjection) {
        for (ISearchField searchField : searchProjection.getFields()) {
            searchField.setEmbedPath(embedPath);
        }
    }

    private void rewriteSearchCriteria(SearchCriteria criteria, SearchProjection searchProjection) {
        if (criteria == null) {
            return;
        }
        ISearchField searchField = criteria.getSearchField();
        if (searchField != null) {
            searchField.setEmbedPath(embedPath);
            // Add search fields for evaluation
            searchProjection.addField(searchField);
        }
        for (SearchCriteria subCriteria : criteria.getChildren()) {
            rewriteSearchCriteria(subCriteria, searchProjection);
        }
    }
    
    private void rewriteSearchGroup(SearchGroup group) {
        if (group == null) {
            return;
        }
        for (ISearchField aggField : group.getAggFields().values()) {
            aggField.setEmbedPath(embedPath);
        }
        for (ISearchField grpField : group.getGrpFields().values()) {
            grpField.setEmbedPath(embedPath);
        }
    }

}
