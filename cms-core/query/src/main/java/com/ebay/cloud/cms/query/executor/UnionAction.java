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

import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSetQuery;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSetQuery.SetOpEnum;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * @author liasu
 * 
 */
public class UnionAction extends AbstractSetAction {

    public UnionAction(QueryContext context, ParseBaseNode node) {
        super(context, node, QueryActionTypeEnum.UNION);
    }

    @Override
    public void optimize() {
        // build cost & join map
        subTreeCost = QueryCostEnum.EqualityIndex.getValue();
        for (AbstractAction childAction : childrenActions) {
            int childCost = childAction.getSubTreeCost();
            if (childCost > subTreeCost) {
                subTreeCost = childCost;
            }
            buildJoinMap(this.andOrJoinMap, childAction.andOrJoinMap);
        }
    }

    protected void buildJoinMap(List<List<ParseQueryNode>> parentAndOrJoinMap, List<List<ParseQueryNode>> childAndOrJoinMap) {
        parentAndOrJoinMap.addAll(childAndOrJoinMap);
    }

    @Override
    protected SearchCriteria getHeadJoinCriteria() {
        LogicalSearchCriteria unionCriteria = new LogicalSearchCriteria(
                LogicOperatorEnum.OR);
        for (AbstractAction childAction : childrenActions) {
            SearchCriteria childCriteria = childAction.getHeadJoinCriteria();
            if (childCriteria != null) {
                unionCriteria.addChild(childCriteria);
            } else {
                return null;
            }
        }
        // return non-empty criteria
        if (unionCriteria.getChildren().isEmpty()) {
            return EmptySearchCriteria.EMPTY_CRITERIA;
        } else {
            return unionCriteria;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta, 
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        IEmbedQuery setQuery =  new EmbedSetQuery(SetOpEnum.UNION, leftJoin);
        for (AbstractAction action : getChildrenActions()) {
            IEmbedQuery childQuery = action.getHeadJoinQuery(setQuery, meta, (LinkedList<MetaRelationship>)embedFieldList.clone(), leftJoin);
            if (childQuery == null) {
                return null;
            }
                setQuery.addChildQuery(childQuery);
        }
        
        return setQuery;
    }

    @Override
    public boolean isChildrenCriteriaReady() {
        boolean unionCriteriaReady = true;
        for (AbstractAction childAction : childrenActions) {
            unionCriteriaReady &= childAction.isChildrenCriteriaReady();
        }
        return unionCriteriaReady;
    }
}