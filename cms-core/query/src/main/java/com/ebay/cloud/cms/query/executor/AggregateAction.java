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


/**
 * 
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

package com.ebay.cloud.cms.query.executor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * AggregationAction is a special search action.
 * 
 * @author liasu
 * 
 */
public class AggregateAction extends SearchAction {

    private final SearchAction searchAction;
    private final SearchProjection aggProjection;
    private SearchCriteria     inIdsCriteria;
    private boolean oneTableAggregation;
    
    public AggregateAction(QueryContext context, SearchAction action, ParseQueryNode node) {
        super(context, node);
        this.searchAction = action;
        // redefine the search query for aggregation
        this.aggProjection= new SearchProjection(node.getProjection());
        this.searchQuery = new SearchQuery(parseNode.getMetaClass(), node.getCriteria(), aggProjection,
                parseNode.getGroup(), parseNode.getGroupCriteria(), getQueryStrategy());
        // set default limit and remove skip for aggregation
        getSearchOption().setLimit(queryContext.getSysLimitDocuments() + SearchOption.LOOK_FORWARD);
        getSearchOption().setSkip(0);
    }
    
    public boolean isOneTableAggregation() {
        return oneTableAggregation;
    }

    public void setOneTableAggregation(boolean oneTableAggregation) {
        this.oneTableAggregation = oneTableAggregation;
    }

    protected SearchQuery rewriteQuery(SearchQuery query) {
        if (oneTableAggregation) {
            SearchCriteria statusCriteria = new FieldSearchCriteria(
                    InternalFieldFactory.getInternalMetaField(InternalFieldEnum.STATUS), getQueryStrategy(),
                    FieldOperatorEnum.EQ, StatusEnum.ACTIVE.toString());
            query.appendSearchCriteria(statusCriteria);
        } else if (inIdsCriteria != null) {
            query.appendSearchCriteria(inIdsCriteria);
        }
        return query;
    }
    
    @Override
    protected boolean isExecutable(ExecuteContext context) {
        if (inIdsCriteria == null) {
            List<ActionResult> results = this.searchAction.getActionResults();
            List<Object> ids = new LinkedList<Object>();
            for (ActionResult result : results) {
                SearchResult searchResult = result.searchResult;
                for (IEntity entity : searchResult.getResultSet()) {
                    ids.add(entity.getId());
                }
            }
            if (!ids.isEmpty()) {
                ISearchField searchField = new SelectionField(InternalFieldFactory.getInternalMetaField(InternalFieldEnum.ID), getQueryStrategy());
                inIdsCriteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.IN, ids);
            } else {
                // set empty criteria to skip aggregation actions
                inIdsCriteria = EmptySearchCriteria.EMPTY_CRITERIA;
            }
            // restored the search projection
            parseNode.setProjection(aggProjection);
        }
        return inIdsCriteria != null;
    }

    @Override
    protected boolean shortCircuitExecute() {
    	if (isOneTableAggregation()) {
    		return false;
    	}
        return inIdsCriteria == EmptySearchCriteria.EMPTY_CRITERIA;
    }

    @Override
    public void optimize() {
        // nothing to do
    }

    @Override
    public SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        // nothing to do
        return null;
    }

    @Override
    public SearchCriteria getHeadJoinCriteria() {
        // nothing to do
        return null;
    }

    @Override
    public List<AbstractAction> getNextExecActions() {
        // nothing to do
        return Collections.emptyList();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AggregateAction[metaclass=").append(parseNode.getMetaClass().getName());
        sb.append(" ,reference=[");
        if (parseNode.getMetaReference() != null) {
            sb.append(parseNode.getMetaReference().getName());
        } else {
            sb.append("null");
        }
        sb.append("]");
        sb.append(", typeCast=[");
        for (MetaClass meta : parseNode.getTypeCast()) {
            sb.append(meta.getName()).append(' ');
        }
        sb.append("]");
        return sb.toString();
    }

}
