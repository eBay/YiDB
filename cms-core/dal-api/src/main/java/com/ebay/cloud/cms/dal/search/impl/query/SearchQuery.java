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
import java.util.Collection;
import java.util.List;

import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define root query that may reference to embedded query.
 * 
 * @author xjiang
 * 
 */
public class SearchQuery implements ISearchQuery {

    private final MetaClass metadata;
    private final SearchProjection projection;
    private SearchCriteria searchCriteria;
    private final SearchGroup group;
    private final SearchCriteria groupCriteria;
    private final MongoQuery mongoQuery;
    protected final ISearchStrategy strategy;
    
    public SearchQuery(final MetaClass metadata, final SearchCriteria criteria, final SearchProjection projection,
            final SearchGroup group, final SearchCriteria groupCriteria, ISearchStrategy strategy) {
        CheckConditions.checkArgument(metadata != null, "SearchQuery must bind a valid MetaClass");
        CheckConditions.checkArgument(projection != null, "SearchProjection can't be null");
        this.metadata = metadata;
        this.strategy = strategy;
        this.searchCriteria = criteria;
        this.groupCriteria = groupCriteria;
        this.projection = projection;
        this.group = group;
        this.mongoQuery = new MongoQuery();
    }
    
    public SearchQuery(final MetaClass metadata,
            final SearchCriteria criteria, final SearchProjection projection, ISearchStrategy strategy) {
        this(metadata, criteria, projection, null, null, strategy);     
    }
    
    public SearchQuery(final MetaClass metadata, SearchQuery query, ISearchStrategy strategy) {
        this(metadata, query.searchCriteria, query.projection, query.group, query.groupCriteria, strategy);     
    }
    
    public SearchCriteria getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(SearchCriteria criteria) {
        this.searchCriteria = criteria;
    }
    
    public SearchGroup getSearchGroup() {
        return this.group;
    }
    
    public SearchProjection getSearchProjection() {
        return this.projection;
    }
    
    @Override
    public MetaClass getMetadata() {
        return metadata;
    }
    
    @Override
    public void appendSearchCriteria(SearchCriteria criteria) {
        if (criteria == null) {
            return;
        }
        if (searchCriteria == null) {
            searchCriteria = criteria;
        } else {
            LogicalSearchCriteria rewrittenCriteria = new LogicalSearchCriteria(LogicalSearchCriteria.LogicOperatorEnum.AND);
            rewrittenCriteria.addChild(criteria);
            rewrittenCriteria.addChild(searchCriteria);
            searchCriteria = rewrittenCriteria;
        }
    }
    
    public ISearchStrategy getStrategy() {
        return strategy;
    }

    @Override
    public MongoQuery buildMongoQuery(PersistenceContext context) {
        
        rewriteQuery(context);
        
        translateCriteria();
        
        translateProjection();
        
        translateGroup();
        
        return this.mongoQuery;
    }
    
    /**
     * plugin method for extension
     * @param context 
     */
    protected void rewriteQuery(PersistenceContext context) {
        rewriteProjection();
        rewriteCriteria(context);
    }
    
    private void rewriteProjection() {
        if (this.group == null) {
            // add default fields
            ProjectionField idField = new ProjectionField(metadata.getFieldByName(InternalFieldEnum.ID.getName()), true, strategy);
            this.projection.addField(idField);
            // add all fields for star selection
            if (this.projection.hasStar()) {
                Collection<MetaField> metaFields = metadata.getFields();
                for (MetaField metaField : metaFields) {
                    this.projection.addField(new ProjectionField(metaField, true, strategy));
                }
            }
        } else {
            // add default fields
            if (this.projection.isEmpty()) {
                this.projection.addField(ProjectionField.STAR);
            }
            // add all fields for star selection 
            if (this.projection.hasStar()) {
                Collection<ISearchField> searchFields = group.getProjectFields();
                for (ISearchField field : searchFields) {
                    this.projection.addField(field);
                }
            }
        }
    }
    
    private void rewriteCriteria(PersistenceContext context) {
        if (!metadata.isEmbed()) {
            // add type criteria
            MetaField typeMetaField = metadata.getFieldByName(InternalFieldEnum.TYPE.getName());
            SearchCriteria typeCriteria = new FieldSearchCriteria(typeMetaField, strategy, FieldOperatorEnum.EQ, metadata.getName());
            appendSearchCriteria(typeCriteria);

            // add additional criteria
            List<SearchCriteria> criterias = context.getAdditionalCriteria().get(metadata.getName());
            if (criterias != null) {
                for (SearchCriteria criteria : criterias) {
                    appendSearchCriteria(criteria);
                }
            }
        }
    }

    private void translateCriteria() {
        this.mongoQuery.match = generateMongoQuery(searchCriteria);
    }
    
    public static DBObject generateMongoQuery(final SearchCriteria searchCriteria) {
        if (searchCriteria == null) 
            return null;
        List<DBObject> subQueryList = null;
        // pre-order traverse the criteria tree
        for (SearchCriteria subCriteria : searchCriteria.getChildren()) {
            DBObject subQuery = generateMongoQuery(subCriteria);
            if (subQueryList == null) {
                subQueryList = new ArrayList<DBObject>(searchCriteria
                        .getChildren().size());
            }
            subQueryList.add(subQuery);
        }

        return searchCriteria.translate(subQueryList);
    }
    
    private void translateProjection() {
        this.mongoQuery.project = new BasicDBObject();
        for (ISearchField searchField : this.projection.getFields()) {
            mongoQuery.project.put(searchField.getFullDbName(), 1);
        }
    }
    
    private void translateGroup() {
        if (this.group == null) {
            return;
        }

        // translate group
        this.mongoQuery.preGroupUnwinds = this.group.buildUnwindQuery();
        this.mongoQuery.group = this.group.buildGroupQuery();
        // translate group criteria
        if (groupCriteria != null) {
            DBObject groupCriteriaObject = generateMongoQuery(groupCriteria);
            this.mongoQuery.groupMatch = new BasicDBObject();
            this.mongoQuery.groupMatch.put("$match",  groupCriteriaObject);
        }        
        // wrap projection
        DBObject projectWrapper = new BasicDBObject();
        projectWrapper.put("$project", this.mongoQuery.project);
        this.mongoQuery.project = projectWrapper;
        // wrap search criteria
        if (searchCriteria != null) {
            DBObject searchCriteriaObject = this.mongoQuery.match;
            this.mongoQuery.match = new BasicDBObject();
            this.mongoQuery.match.put("$match",  searchCriteriaObject);
        }
    }

}
