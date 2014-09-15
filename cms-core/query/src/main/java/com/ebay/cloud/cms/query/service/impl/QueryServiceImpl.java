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

package com.ebay.cloud.cms.query.service.impl;

import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.QueryExecutor;
import com.ebay.cloud.cms.query.executor.result.QueryResultPopulator;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizer;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.parser.QueryParser;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.IQueryService;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.translator.QueryTranslator;

/**
 * query service implementation
 * 
 * @author xjiang
 * 
 */
public class QueryServiceImpl implements IQueryService {

	private final IRepositoryService repositoryService;
	private final ISearchService searchService;
	private final IBranchService branchService;

	public QueryServiceImpl(IRepositoryService repositoryService, IBranchService branchService,
			ISearchService searchService) {
		this.repositoryService = repositoryService;
		this.branchService = branchService;
		this.searchService = searchService;
	}

	@Override
	public IQueryResult query(String query, QueryContext context) {
		checkLimits(context);

		// prepare context
		prepareContext(context);

		// parse query to AST
		QueryParser parser = new QueryParser(context);
		ParseBaseNode queryNode = parser.parse(query.trim());

		// translate AST to logical execution plan
		QueryTranslator transaltor = new QueryTranslator(context);
		QueryExecPlan plan = transaltor.translate(queryNode);

		// execute plan
		executeQueryPlan(plan, context);

		// populate result
		populateResult(plan, context);

		return plan.getQueryResult();
	}
	
	
	public Map<String, MetaClass> getQueryMetaClass(String query, QueryContext context) {
	    checkLimits(context);
	    prepareContext(context);

	    QueryParser parser = new QueryParser(context);
        ParseBaseNode queryNode = parser.parse(query.trim());
        Map<String, MetaClass> metadataMap = new HashMap<String, MetaClass>();
        addMetaClass(queryNode, metadataMap);
        return metadataMap;
	}

    private void addMetaClass(final ParseBaseNode subTreeRoot, final Map<String, MetaClass> metadataMap) {
        if (subTreeRoot instanceof ParseQueryNode) {
            ParseQueryNode queryNode = (ParseQueryNode) subTreeRoot;
            metadataMap.put(queryNode.getMetaClass().getName(), queryNode.getMetaClass());
            // add type-cast
            for (MetaClass typeCast : queryNode.getTypeCast()) {
                metadataMap.put(typeCast.getName(), typeCast);
            }
            // add sub-query
            for (ParseBaseNode subQueryNode : queryNode.getSubQueryNodes()) {
                addMetaClass(subQueryNode, metadataMap);
            }
        }

        for (ParseBaseNode child : subTreeRoot.getNextNodes()) {
            addMetaClass(child, metadataMap);
        }
    }

    private void checkLimits(QueryContext context) {
        Integer sysLimitDocuments = context.getSysLimitDocuments();
        int[] limits = context.getLimits();
        if (limits != null) {
            for (int i : limits) {
                if (i < 0) {
					throw new QueryOptimizeException(QueryErrCodeEnum.SYS_LIMIT_DOCUMENTS_MUST_POSITIVE, "The value of limit should be positive!");
                } else if (i > sysLimitDocuments) {
					throw new QueryOptimizeException(QueryErrCodeEnum.EXCEED_SYS_LIMIT_DOCUMENTS, String.format("Exceed system limit %d documents!", sysLimitDocuments));
				}
			}
		}
	}

	private void prepareContext(QueryContext context) {
		context.setRepositoryService(repositoryService);
		context.setSearchService(searchService);
		context.setBranchService(branchService);
	}

	private void executeQueryPlan(QueryExecPlan plan, QueryContext context) {
		// recursively execute sub plans
		for (QueryExecPlan subPlan : plan.getSubPlans()) {
			executeQueryPlan(subPlan, context);
		}
		// optimize exec plan
		QueryOptimizer optimizer = new QueryOptimizer(context);
		optimizer.optimize(plan);

		// execute physical execution plan
		QueryExecutor executor = new QueryExecutor(context);
		executor.execute(plan);
	}

	public static void populateResult(QueryExecPlan plan, QueryContext context) {
	    plan.prepareProjectionTree();
		QueryResultPopulator populateAction = new QueryResultPopulator(plan, context);
		populateAction.execute();
	}

}
