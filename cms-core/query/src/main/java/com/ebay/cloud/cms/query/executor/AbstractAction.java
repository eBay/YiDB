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
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author xjiang
 *
 */
public abstract class AbstractAction  {
    
    public enum QueryActionTypeEnum {
        SEARCH, INNERJOIN, LEFTJOIN, INTERSECTION, UNION, AGGREGATION, ROOTJOIN
    }
    
    public enum QueryCostEnum {
        EqualityIndex(1),
        RangeIndex(10),
        AllowFullTableScan(100),
        EqualityScan(1000),
        RangeScan(2000),
        RangeRegxScan(3000),
        NegativeScan(10000),
        FullScan(20000);
        
        private final int value;
        private QueryCostEnum(int cost) {
            this.value = cost;
        }
        public int getValue() {
            return value;
        }
        public static boolean isFullTableScan(int cost) {
            if (cost >= EqualityScan.getValue()) {
                return true;
            }
            return false;
        }
    }
    
    public static class ExecStat {
        public boolean ancestorsDone = false;
        public boolean selfDone = false;
        public boolean descentsDone = false;
    }
    
    public static class ActionResult {
        public final SearchResult searchResult;
        public final MetaRelationship joinField;
        public final ParseQueryNode queryNode;
        
        public ActionResult(SearchResult searchResult, ParseQueryNode queryNode) {
            this.searchResult = searchResult;
            this.queryNode = queryNode;
            if (queryNode != null) {
                this.joinField = queryNode.getMetaReference();
            } else {
                this.joinField = null;
            }
        }
    }
    
    protected final QueryContext queryContext;
    
    protected int subTreeCost;
    
    protected AbstractAction parentAction;
    
    protected final List<AbstractAction> childrenActions;
    
    private final List<ActionResult> actionResults;
    
    // 
    // Test Case for the andOrJoinMap construction....
    // 
    protected List<List<ParseQueryNode>> andOrJoinMap;
    
    protected IQueryExplanation explanation;

    private final QueryActionTypeEnum type;
    
    protected ExecStat state;

    private final ParseBaseNode node;
    
    public AbstractAction(QueryContext context, ParseBaseNode node, QueryActionTypeEnum type) {
        this.queryContext = context;
        this.type = type;
        this.parentAction = null;
        this.state = new ExecStat();
        this.node = node;
        this.subTreeCost = QueryCostEnum.FullScan.getValue();
        this.childrenActions = new ArrayList<AbstractAction>(2);
        this.actionResults = new ArrayList<ActionResult>();
        this.andOrJoinMap = new ArrayList<List<ParseQueryNode>>();
    }
    
    public ParseBaseNode getParseNode() {
        return node;
    }

    public QueryActionTypeEnum getType() {
        return type;
    }
    
    public boolean isSelfDone() {
        return state.selfDone;
    }
    
    public boolean isAncestorsDone() {
        return state.ancestorsDone;
    }
    
    public boolean isDescentsDone() {
        return state.descentsDone;
    }
    
    public AbstractAction getParentAction() {
        return parentAction;
    }
    
    public void setParentAction(AbstractAction action) {
        parentAction = action;
        action.childrenActions.add(this);
    }
    
    public List<AbstractAction> getChildrenActions() {
        return childrenActions;
    }

    public void addChildAction(AbstractAction action) {
        childrenActions.add(action);
        action.parentAction = this;
    }
    
    public void removeChildAction(AbstractAction action) {      
        childrenActions.remove(action);
        action.parentAction = null;
    }
    
    public List<ActionResult> getActionResults() {
        return actionResults;
    }
    public int getSubTreeCost() {
        return subTreeCost;
    }

    /**
     * treat subtree as a unit
     * 
     * FIXME: If parent node is not the lowest, then all sub tree must be executed
     * before current (even for those sub tree that have cost bigger than the parent?).
     * 
     * @param queryContext
     */
    public void exec(ExecuteContext context) {
        // pre-order execute current action
        if (!state.selfDone) {
            state.selfDone = doAction(context, false);
        }
        // update ancestorsDone
        state.ancestorsDone = state.selfDone;
        if (parentAction != null) {
            state.ancestorsDone &= parentAction.state.ancestorsDone;
        }
        
        // recursively execute children actions        
        for (AbstractAction childAction : getNextExecActions()) {
            if (!childAction.state.descentsDone) {
                childAction.exec(context);
            }
        }
        
        // post-order execute current action
        if (!state.selfDone) {
            state.selfDone = doAction(context, true);
        }
        // update descentsDone
        state.descentsDone = state.selfDone;
        for (AbstractAction childAction : childrenActions) {
            state.descentsDone &= childAction.state.descentsDone;
        }
    }

    public abstract void optimize();

    protected abstract SearchCriteria getTailJoinCriteria(MetaRelationship referenceField);

    protected abstract SearchCriteria getHeadJoinCriteria();
    
    protected abstract List<AbstractAction> getNextExecActions();

    protected abstract boolean doAction(ExecuteContext context, boolean postOrderVisit);
    
    protected abstract IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta, 
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin);

    public abstract boolean isChildrenCriteriaReady();

    public int getQuerySequenceLowerBound() {
        return node.getQuerySequenceLowerBound();
    }
    public int getQuerySequenceUpperBound() {
        return node.getQuerySequenceUpperBound();
    }
    
}
