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
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * @author liasu
 *
 */
public abstract class AbstractSetAction extends AbstractAction {

    public AbstractSetAction(QueryContext context, ParseBaseNode node, QueryActionTypeEnum type) {
        super(context, node, type);
    }
    
    @Override
    protected SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        return parentAction.getTailJoinCriteria(referenceField);
    }


    @Override
    protected List<AbstractAction> getNextExecActions() {
        List<AbstractAction> nextActions = new LinkedList<AbstractAction>();
        // execute the not_full_table_scan children firstly
        for (AbstractAction childAction : childrenActions) {
            if (!childAction.isDescentsDone()) {
                if(!QueryCostEnum.isFullTableScan(childAction.subTreeCost)) {
                    nextActions.add(childAction);
                } else {
                    int hint = queryContext.getHint();
                    int lower = childAction.getQuerySequenceLowerBound();
                    int upper = childAction.getQuerySequenceUpperBound();
                    if (hint > lower && hint <= upper) {
                        nextActions.add(childAction);
                    }
                }
            }
        }
        // if all children are full table scan, return the lowest cost un-executed children
        if (nextActions.isEmpty()) {
            AbstractAction lowestCostChild = null;
            int lowestCost = QueryCostEnum.FullScan.getValue() + 1;
            for (AbstractAction childAction : childrenActions) {
                if (!childAction.isDescentsDone() && childAction.getSubTreeCost() < lowestCost) {
                    lowestCost = childAction.getSubTreeCost();
                    lowestCostChild = childAction;
                }
            }
            nextActions.add(lowestCostChild);
        }
        
        return nextActions;
    }

    @Override
    protected boolean doAction(ExecuteContext context, boolean postOrderVisit) {
        // refactor to method?
        boolean allDescentsDone = true;
        for (AbstractAction action : childrenActions) {
            allDescentsDone = allDescentsDone && action.isDescentsDone();
        }
        if (allDescentsDone) {
            for (AbstractAction action : childrenActions) {
                this.getActionResults().addAll(action.getActionResults());
            }
            return true;
        }
        return false;
    }

}
