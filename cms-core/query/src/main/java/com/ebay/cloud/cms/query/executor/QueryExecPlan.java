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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryActionTypeEnum;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.IQueryResult;

/**
 * 
 * @author xjiang
 *
 */
public class QueryExecPlan {

    private ParseBaseNode rootNode;
    private AbstractAction rootAction;
    private AbstractAction aggAction;
    private List<QueryExecPlan> subPlans;
    private IQueryResult queryResult;
    private List<IQueryExplanation> explanations;
    private int actionCount;
    private int searchCount;
    private Map<String, ParseQueryNode> nodesMap;
    
    public QueryExecPlan() {
        actionCount = 0;
        subPlans = new LinkedList<QueryExecPlan>();
        explanations = new LinkedList<IQueryExplanation>();
    }

    public AbstractAction getRootAction() {
        return rootAction;
    }
    public void setRootAction(AbstractAction action) {
        this.rootAction = action;
    }
    
    public void incActionCount() {
        actionCount++;
    }
    public int getActionCount() {
       return actionCount;
    }

    public void incSearchCount(int count) {
        searchCount += count;
    }
    public int getSearchCount() {
        return searchCount;
    }

    public List<QueryExecPlan> getSubPlans() {
        return subPlans;
    }
    public void addSubPlan(QueryExecPlan plan) {
        subPlans.add(plan);
    }

    public IQueryResult getQueryResult() {
        return queryResult;
    }
    public void setQueryResult(IQueryResult queryResult) {
        this.queryResult = queryResult;        
    }

    public AbstractAction getAggAction() {
        return aggAction;
    }
    public void setAggAction(AbstractAction aggAction) {
        this.aggAction = aggAction;
    }

    public void addExplanations(Collection<IQueryExplanation> explanations) {
        this.explanations.addAll(explanations);
    }
    public List<IQueryExplanation> getExpanations() {
        return explanations;
    }
    
    public List<SearchAction> getHeadSearchActions() {
        List<SearchAction> outResult = new LinkedList<SearchAction>();
        getHeadSearchAction(rootAction, outResult);
        return outResult;
    }
    private void getHeadSearchAction(AbstractAction rootAction, List<SearchAction> outResult) {
        if (rootAction.getType() == QueryActionTypeEnum.SEARCH) {
            outResult.add((SearchAction) rootAction);
            return;
        }

        for (AbstractAction childAction : rootAction.getChildrenActions()) {
            getHeadSearchAction(childAction, outResult);
        }
    }

    /**
     * Finds head search action. e.g. (A.b || B.c) -> will have two root result
     * FIXME : what about (A.b || A.c), bug??
     * 
     * @param metaclass
     * @param action
     * @return
     */
    public static SearchAction findHeadSearchAction(MetaClass metaclass, AbstractAction action) {
        if (action instanceof SearchAction) {
            SearchAction searchAction = (SearchAction) action;
            MetaClass actionMetaclass = searchAction.getParseNode().getMetaClass();
            if (metaclass.equals(actionMetaclass)) {
                return searchAction;
            }
        }
        for (AbstractAction childAction : action.getChildrenActions()) {
            SearchAction childSearchAction = findHeadSearchAction(metaclass, childAction);
            if (childSearchAction != null) {
                return childSearchAction;
            }
        }
        return null;
    }

    public void setRootNode(ParseBaseNode rootNode) {
        this.rootNode = rootNode;
    }
    
    public void prepareProjectionTree() {
        List<ParseQueryNode> rootQueryNodes = new ArrayList<ParseQueryNode>();
        getRootNodesFromParseTree(rootNode, rootQueryNodes);
        
        nodesMap = new HashMap<String, ParseQueryNode>();
        
        for (ParseQueryNode rootNode : rootQueryNodes) {
            String meta = rootNode.getMetaClass().getName();
            ParseQueryNode rootProjectionNode = nodesMap.get(meta);
            if (rootProjectionNode == null) {
                rootProjectionNode = new ParseQueryNode();
                nodesMap.put(meta, rootProjectionNode);
            }
            
            if (rootNode.isRootDisplay()) {
                rootProjectionNode.markRootDisplay();
            }
            
            Map<String, List<ParseQueryNode>> nextNodes = rootNode.getNextQueryNodes();
            Map<String, List<ParseQueryNode>> mergedNodes = rootProjectionNode.getNextQueryNodes();
            
            for (Map.Entry<String, List<ParseQueryNode>> entry : nextNodes.entrySet()) {
                String relation = entry.getKey();
                List<ParseQueryNode> childNodes = mergedNodes.get(relation);
                if (childNodes == null) {
                    childNodes = new ArrayList<ParseQueryNode>();
                    mergedNodes.put(relation, childNodes);
                }
                
                childNodes.addAll(entry.getValue());
            }
            
            SearchProjection projection = rootNode.getProjection();
            if (projection.hasStar()) {
                rootProjectionNode.getProjection().addField(ProjectionField.STAR);
            }
            
            Collection<ISearchField> projectionFields = projection.getFields();
            for (ISearchField projectionField : projectionFields) {
                rootProjectionNode.getProjection().addField(projectionField);
            }
        }
    }

    private void getRootNodesFromParseTree(ParseBaseNode rootNode, List<ParseQueryNode> rootList) {
        if (getAggAction() != null) {
            ParseQueryNode node = ((AggregateAction)getAggAction()).getParseNode();
            rootList.add(node);
            return;
        }
        if (rootNode instanceof ParseQueryNode) {
            rootList.add((ParseQueryNode)rootNode);
        } else {
            for (ParseBaseNode node : rootNode.getNextNodes()) {
                getRootNodesFromParseTree(node, rootList);
            }
        }
    }

    public Map<String, ParseQueryNode> getNodesMap() {
        return nodesMap;
    }
    
}
