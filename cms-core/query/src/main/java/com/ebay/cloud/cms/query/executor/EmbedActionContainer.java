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

package com.ebay.cloud.cms.query.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.AbstractAction.ActionResult;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryActionTypeEnum;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryCostEnum;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author xjiang
 *
 */
public class EmbedActionContainer {
    
    private final QueryContext queryContext;
    private final Set<AbstractAction> containedActions;
    private int embedSearchCost;
    private IEmbedQuery rootEmbedQuery;
    private SearchAction rootSearchAction;
    private SearchCriteria parentJoinCriteria;
    private boolean embedDone;
    
    public EmbedActionContainer(QueryContext context, SearchAction searchAction) {
        queryContext = context;
        embedSearchCost = QueryCostEnum.FullScan.getValue();
        containedActions = new HashSet<AbstractAction>();
        embedDone = false;
        rootSearchAction = searchAction;
    }
    
    public SearchAction getRootSearchAction() {
        return rootSearchAction;
    }
    
    private void createJoinCriteria() {
        AbstractAction parentAction = rootSearchAction.getParentAction();
        if (parentAction != null) {
            ParseQueryNode parseNode = rootSearchAction.getParseNode();
            MetaRelationship metaRef = parseNode.getMetaReference();
            parentJoinCriteria = parentAction.getTailJoinCriteria(metaRef);
        }
        rootEmbedQuery = rootSearchAction.getHeadJoinQuery(null, null, new LinkedList<MetaRelationship>(), false);
    }
    
    public boolean isExecutable(ExecuteContext context) {
        // query hint: hint action must be executed at the first
        if (queryContext.hasHint() && !context.hintExecuted) {
            for (AbstractAction action : containedActions) {
                if (action instanceof SearchAction) {
                    int hint = ((SearchAction)action).getParseNode().getQuerySequence();
                    if (hint == queryContext.getHint()) {
                        context.hintExecuted = true;
                        break;
                    }
                }
            }
            return context.hintExecuted;
        }
        
        if (rootSearchAction.getChildrenActions().get(0).isChildrenCriteriaReady()
                || embedSearchCost <= rootSearchAction.getSubTreeCost() 
                || parentJoinCriteria != null) { 
            return true;
        }        
        return false;
    }
    public ISearchStrategy getQueryStrategy() {
        return queryContext.getRegistration().searchStrategy;
    }
    private void execEmbedQuery(ExecuteContext context) {
        if (parentJoinCriteria == EmptySearchCriteria.EMPTY_CRITERIA) {
            return;
        }
        // rewrite the query with addtional criteria
        rewriteQuery();
        // execute embed query 
        executeEmbedQuery(context);
        // split SearchResult to internal SearchAction
        splitEmbedResult(rootSearchAction, context);
        context.searchExecSeq++;
    }

    private void rewriteQuery() {
        // append internal status criteria
        SearchCriteria statusCriteria = new FieldSearchCriteria(
                InternalFieldFactory.getInternalMetaField(InternalFieldEnum.STATUS),
                getQueryStrategy(),
                FieldOperatorEnum.EQ,
                StatusEnum.ACTIVE.toString());
        rootEmbedQuery.appendSearchCriteria(statusCriteria);

        // append parent criteria
        rootEmbedQuery.appendSearchCriteria(parentJoinCriteria);

        // append cursor criteria
        SearchCursor rootCursor = rootSearchAction.getParseNode().getCursor();
        SearchOption option = rootSearchAction.getSearchOption();
        SearchCriteria criteria = rootCursor.getCursorCriteria(option, rootSearchAction.getParseNode(), queryContext);
        rootEmbedQuery.appendSearchCriteria(criteria);
    }
    
    private void executeEmbedQuery(ExecuteContext context) {
        ISearchService searchService = queryContext.getSearchService();
        PersistenceContext persistContext = queryContext.getPersistenceContext(rootEmbedQuery.getMetadata());
        SearchOption searchOption = rootSearchAction.getSearchOption();
        SearchResult searchResult = searchService.search(rootEmbedQuery, searchOption, persistContext);
        // update pagination setting on root
        rootSearchAction.setHasMore(SearchAction.checkHasMore(searchOption, queryContext.isSubQuery(), searchResult));
        SearchCursor cursor = rootSearchAction.getParseNode().getCursor();
        JsonEntity cursorValue = SearchAction.createNextCursorValue(searchOption, searchResult);
        int limit = cursor.hasLimit() ? cursor.getLimit() : searchOption.getDisplayLimit();
        SearchCursor nextCursor = new SearchCursor(cursor.getSkip() + limit, limit, cursorValue);
        rootSearchAction.setNextCursor(nextCursor);
        // add explanation
        if (queryContext.needExplain()) {
            context.explanations.addAll(searchResult.getQueryExplanations());
        }
    }
    
    private void splitEmbedResult(AbstractAction action, ExecuteContext context) {
        // set search result on action
        if (action instanceof SearchAction) {
            SearchAction searchAction = (SearchAction)action;
            EmbedSearchQuery embedQuery = searchAction.getEmbedQuery();
            SearchResult searchResult = embedQuery.getSearchResult();
            ParseQueryNode parseNode = searchAction.getParseNode();
            ActionResult actionResult = new ActionResult(searchResult, parseNode);
            searchAction.getActionResults().add(actionResult);
            searchAction.state.selfDone = true;
            ((SearchAction)action).setExecSeq(context.searchExecSeq);
        }
        // traversal embed children actions
        for (AbstractAction childAction : action.childrenActions) {
            if (containedActions.contains(childAction)) {
                splitEmbedResult(childAction, context);
            }
        }
    }

    public void addContainedActions(Set<AbstractAction> actionSet) {
        containedActions.addAll(actionSet);
    }

    public Set<AbstractAction> getContainedActions() {
        return containedActions;
    }
    
    public void optimize() {
      // recursive traversal embed actions
      embedSearchCost = optimizeAction(rootSearchAction);           
  }
  
    private int optimizeAction(AbstractAction action) {
      // traversal embed children actions
      int childCost = QueryCostEnum.FullScan.getValue();
      List<Integer> childrenCosts = new ArrayList<Integer>(2);
      for (AbstractAction childAction : action.getChildrenActions()) {
          if (containedActions.contains(childAction)) {
              childCost = optimizeAction(childAction);
              childrenCosts.add(childCost);
          }
      }
      // post-order : calculate the cost
      int embedTreeCost = QueryCostEnum.FullScan.getValue();
      QueryActionTypeEnum actionType = action.getType();
      switch (actionType) {
          case SEARCH:
              int searchCost = ((SearchAction)action).getSearchCost();
              embedTreeCost = searchCost < childCost ? searchCost : childCost;
              break;
          case INNERJOIN:
              embedTreeCost = childrenCosts.get(0);
              break;
          case INTERSECTION:
              embedTreeCost = Collections.min(childrenCosts);
              break;
          case UNION:
              embedTreeCost = Collections.max(childrenCosts);
              break;
          default :
              break;
      }
      return embedTreeCost;
  }
  
    public boolean doAction(ExecuteContext context, boolean postOrderVisit) {
        if (embedDone) {
            return true;
        }
        createJoinCriteria();
        if (isExecutable(context)) {
            execEmbedQuery(context);
            embedDone = true;
            return true;
        }
        return false;
    }
    
}
