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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * query executor
 * 
 * @author xjiang
 * 
 */
public class QueryExecutor {

	static class ExecuteContext {
		public ExecuteContext(QueryExecPlan plan) {
			explanations = new LinkedList<IQueryExplanation>();
			this.plan = plan;
		}

		int searchExecSeq = 0;
		// int lastExecCost = 0;
		boolean hintExecuted;
		final List<IQueryExplanation> explanations;
		final QueryExecPlan plan;
        public QueryExecPlan getPlan() {
            return plan;
        }
	}

	private static final Logger logger = LoggerFactory.getLogger(QueryExecutor.class);
	private QueryContext queryContext;

	public QueryExecutor(QueryContext context) {
		this.queryContext = context;
	}

	/**
	 * pull mode execution
	 * 
	 * @param plan
	 */
	public void execute(QueryExecPlan plan) {
		final int executeLimit = 2 * plan.getActionCount();
		int executeCounter = 0;
		AbstractAction rootAction = plan.getRootAction();
		ExecuteContext context = new ExecuteContext(plan);
        if (plan.getAggAction() != null) {
        	AggregateAction aggAction = (AggregateAction) plan.getAggAction();
        	if (aggAction.isOneTableAggregation()) {
        		execute(executeLimit, executeCounter, aggAction, context);
        	} else  {
        		executeCounter = execute(executeLimit, executeCounter, rootAction, context);
        		execute(executeLimit, executeCounter, aggAction, context);
        	}
        } else {
        	execute(executeLimit, executeCounter, rootAction, context);
        }
		plan.addExplanations(context.explanations);
	}

    private int execute(final int executeLimit, int executeCounter, AbstractAction rootAction,
            ExecuteContext context) {
        int counter = executeCounter;
        while (!rootAction.isDescentsDone()) {
			logger.debug("" + counter + " execution for query :" + queryContext.getQueryString());
			rootAction.exec(context);
			if (counter++ > executeLimit) {
				throw new QueryExecuteException(QueryErrCodeEnum.TOO_DEEP_EXECUTION, "Too deep execution: "
						+ counter + " of " + queryContext.getQueryString());
			}
		}
        return counter;
    }

}
