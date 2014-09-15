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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.AbstractAction.ActionResult;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryActionTypeEnum;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.executor.SearchCursor;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.QueryCursor;

/**
 * populate query result
 * 
 * @author xjiang
 * 
 */
public class QueryResultPopulator {

    private static final String STRAT_OID = "";
    private final QueryExecPlan plan;
    private final QueryContext qryContext;

    public QueryResultPopulator(QueryExecPlan plan, QueryContext context) {
        this.plan = plan;
        this.qryContext = context;
    }

    public void execute() {
        
        QueryResult result = populateQueryResult();
        
        populateDisplayMeta(result);
        
        populateQueryExplanation(plan, result);
        
        populatePagination(result);
    }

    private void populateDisplayMeta(QueryResult queryResult) {
        List<SearchAction> searchActions = plan.getHeadSearchActions();
        for (SearchAction searchAction : searchActions) {
            visitDisplayMeta(queryResult, searchAction);
        }
    }

    private QueryResult populateQueryResult() {
        QueryResult queryResult = new QueryResult();
        plan.setQueryResult(queryResult);
        
        Map<String, ParseQueryNode> nodesMap = plan.getNodesMap();
        AbstractAction rootAction = plan.getAggAction() != null ? plan.getAggAction() : plan.getRootAction();
        for (ActionResult actionResult : rootAction.getActionResults()) {
            SearchResult searchResult = actionResult.searchResult;
            ParseQueryNode node = nodesMap.get(searchResult.getMetaClass().getName());
            visitSearchResult(searchResult, node, queryResult);
        }
        return queryResult;
    }

    private void visitDisplayMeta(QueryResult queryResult, SearchAction rootAction) {
        if (qryContext.isShowDisplayMeta()) {
            QueryProjectionVisitor visitor = new QueryProjectionVisitor(qryContext);
            visitor.findProjection(rootAction.getParseNode());
            queryResult.addDisplayMeta(visitor.getDisplayNode());
        }
    }

    private void visitSearchResult(SearchResult searchResult, ParseQueryNode node, QueryResult queryResult) {
        QueryEntityVisitor visitor = new QueryEntityVisitor(node, queryResult);
        List<IEntity> entities = searchResult.getResultSet();
        for (IEntity entity : entities) {
            entity.traverse(visitor);
        }
        if (qryContext.isCountOnly()) {
            queryResult.setCount(searchResult.getCount());
        }
    }

    private void populateQueryExplanation(QueryExecPlan plan, QueryResult result) {
        if (qryContext.needExplain()) {
            for (QueryExecPlan subPlan : plan.getSubPlans()) {
                populateQueryExplanation(subPlan, result);
            }
            result.addExplanations(plan.getExpanations());
        }
    }
    
    //
    // Per reversed execution order,
    // Find fist has more action, increase the skip/limit on it.
    // Actions pre this action's skip/limit is cleared.
    // Actions after this action's skip/limit is remained.
    // 2013.10.30 ## populate pagination need to support two pagination mechnism
    //            ## FIXME: remove the skip when all client adopt new pagination
    //
    private void populatePagination(QueryResult result) {
        List<SearchAction> execOrderActions = getExecutionOrder();
        int nextHint = execOrderActions.get(0).getParseNode().getQuerySequence();
        int pivot = execOrderActions.size() - 1;
        int[] nextSkips = new int[execOrderActions.size()];
        int[] nextLimits = new int[execOrderActions.size()];
        QueryCursor nextQueryCursor = new QueryCursor(qryContext.getQueryCursor());
        String[] joinCursorIds = new String[execOrderActions.size()];
        JsonEntity singleCursorValue = null;
        // find pivot according the reversed order of execution
        for (; pivot >= 0; pivot--) {
            SearchAction searchAction = execOrderActions.get(pivot);
            if (searchAction.hasMoreResults()) {
                SearchCursor nextCursor = searchAction.getNextCursor();
                int querySeq = searchAction.getParseNode().getQuerySequence();
                int skip = nextCursor.getSkip();
                int limit = nextCursor.getLimit();
                nextSkips[querySeq] = skip;
                nextLimits[querySeq] = limit;
                // join cursor use an single oid value while single cursor could have more than one sortOn
                if (nextQueryCursor.isJoinCursor()) {
                    joinCursorIds[querySeq] = nextCursor.getCursorValue()!=  null ? nextCursor.getCursorValue().getId() : STRAT_OID;
                } else {
                    singleCursorValue = nextCursor.getCursorValue();
                }
                break;
            }
        }
        // if no has more found
        if (pivot < 0) {
            return;
        }
        result.setMoreResults();
        // from pivot to the end of executed actions, clear the next skip/limit
        for (int i = pivot + 1; i < execOrderActions.size(); i++) {
            SearchAction action = execOrderActions.get(i);
            ParseQueryNode queryNode = action.getParseNode();
            int querySeq = queryNode.getQuerySequence();
            nextSkips[querySeq] = 0;
            int limit = queryNode.hasLimit() ? queryNode.getLimit() : action.getSearchOption().getDisplayLimit();
            nextLimits[querySeq] = limit;
            joinCursorIds[querySeq] = STRAT_OID;
        }
        // from the beginning to the pivot, keep the skip/limit as nextSkip/nextLimit
        for (int i = 0; i < pivot; i++) {
            SearchAction action = execOrderActions.get(i);
            ParseQueryNode queryNode = action.getParseNode();
            SearchCursor cursor = queryNode.getCursor();
            int querySeq = queryNode.getQuerySequence();
            nextSkips[querySeq] = queryNode.getSkip();
            if (queryNode.hasLimit()) {
                nextLimits[querySeq] = queryNode.getLimit();
            } else {
                // when no limit given for the node, set as default limit
                nextLimits[querySeq] = SearchOption.DEFAULT_LIMIT;
            }
            joinCursorIds[querySeq] = cursor.getCursorValue() != null ? cursor.getCursorValue().getId() : STRAT_OID;
        }
        // now update skip/limit/hint - based on pagination mode
        if (qryContext.getPaginationMode() == PaginationMode.SKIP_BASED) {
            nextQueryCursor.setSkips(nextSkips);
        } else if (nextQueryCursor.isJoinCursor()) {
            nextQueryCursor.setJoinCursorValues(Arrays.asList(joinCursorIds));
        } else {
            nextQueryCursor.setSingleCursorValue(singleCursorValue);
        }
        nextQueryCursor.setLimits(nextLimits);
        
        // identify the hint value
        int inputHint = qryContext.getHint();
        if (inputHint >= 0 && inputHint != nextHint) {
            for (SearchAction action : execOrderActions) {
                ParseQueryNode node = action.getParseNode();
                if (node.getQuerySequence() == inputHint && action.isEmbedSearch()) {
                    nextHint = inputHint;
                    break;
                }
            }
        }
        nextQueryCursor.setHint(nextHint);
        
        result.setNextCursor(nextQueryCursor);
    }

    private List<SearchAction> getExecutionOrder() {
        AbstractAction rootAction = plan.getRootAction();
        List<SearchAction> execOrder = new ArrayList<SearchAction>();
        execOrder(rootAction, execOrder);
        Collections.sort(execOrder, new Comparator<SearchAction>() {
            @Override
            public int compare(SearchAction object1, SearchAction object2) {
                return object1.getExecSeq() - object2.getExecSeq();
            }
        });
        return execOrder;
    }

    private void execOrder(AbstractAction rootAction, List<SearchAction> execOrder) {
        if (rootAction.getType() == QueryActionTypeEnum.SEARCH) {
            execOrder.add((SearchAction) rootAction);
        }
        for (AbstractAction child : rootAction.getChildrenActions()) {
            execOrder(child, execOrder);
        }
    }

}
