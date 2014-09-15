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

import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSetQuery;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSetQuery.SetOpEnum;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author xjiang
 *
 */
public class IntersectionAction extends AbstractSetAction {

    public IntersectionAction(QueryContext context, ParseBaseNode node) {
        super(context, node, QueryActionTypeEnum.INTERSECTION);
    }
    
    @Override
    public void optimize() {
        // build cost & join map
        for (AbstractAction childAction : childrenActions) {
            int childCost = childAction.getSubTreeCost();
            if (childCost < subTreeCost) {
                subTreeCost = childCost;
            }
            buildJoinMap(this.andOrJoinMap, childAction.andOrJoinMap);
        }
    }

    protected void buildJoinMap(List<List<ParseQueryNode>> oldAndOrJoinMap,
            List<List<ParseQueryNode>> childAndOrJoinMap) {
        if (oldAndOrJoinMap.isEmpty()) {
            oldAndOrJoinMap.addAll(childAndOrJoinMap);
            return;
        }
        andOrJoinMap = new ArrayList<List<ParseQueryNode>>();
        // build join map for intersection:
        // [ [a, b], [c, d] ]  && [ [e], [f, g]] == [ [a, b, e], [a, b, f, g], [c, d, e], [c, d, f, g]]
        for (List<ParseQueryNode> childAndList : childAndOrJoinMap) {
            for (List<ParseQueryNode> oldAndList : oldAndOrJoinMap) {
                List<ParseQueryNode> newAndList = new ArrayList<ParseQueryNode>(childAndList.size()
                        + oldAndList.size());
                newAndList.addAll(oldAndList);
                newAndList.addAll(childAndList);
                andOrJoinMap.add(newAndList);
            }
        }
    }

    @Override
    public SearchCriteria getHeadJoinCriteria() {
        LogicalSearchCriteria rewrittenCriteria = new LogicalSearchCriteria(
                LogicalSearchCriteria.LogicOperatorEnum.AND);
        for (AbstractAction childAction : childrenActions) {
            SearchCriteria childCriteria = childAction.getHeadJoinCriteria();
            if (EmptySearchCriteria.EMPTY_CRITERIA.equals(childCriteria)) {
                return EmptySearchCriteria.EMPTY_CRITERIA;
            }
            if (childCriteria != null) {
                rewrittenCriteria.addChild(childCriteria);
            }
        }
        if (rewrittenCriteria.getChildren().isEmpty()) {
            return null;
        } else {
            return rewrittenCriteria;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta, 
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        
        boolean childJoinQueryReady = false;
        IEmbedQuery setQuery = new EmbedSetQuery(SetOpEnum.INTERSECTION, leftJoin);
        for (AbstractAction action : getChildrenActions()) {
            IEmbedQuery childQuery = action.getHeadJoinQuery(setQuery, meta, (LinkedList<MetaRelationship>)embedFieldList.clone(), leftJoin);
            if (childQuery != null) {
                setQuery.addChildQuery(childQuery);
                childJoinQueryReady = true;
            }
        }
        
        if (childJoinQueryReady) {
            return setQuery;
        }
        return null;
    }

    @Override
    public boolean isChildrenCriteriaReady() {
        for (AbstractAction action : getChildrenActions()) {
            if (action.isChildrenCriteriaReady()) {
                return true;
            }
        }
        return false;
    }

}
