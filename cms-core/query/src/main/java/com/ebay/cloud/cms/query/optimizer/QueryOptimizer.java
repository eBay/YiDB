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

package com.ebay.cloud.cms.query.optimizer;

import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSTrackingCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.RegexValue;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryActionTypeEnum;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryCostEnum;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * analysis query cost and generate physical execution plan based on cost and hint
 *
 * @author xjiang, liasu
 */
public class QueryOptimizer {
    
    private QueryContext context;
    
    public QueryOptimizer(QueryContext context) {
        this.context = context;

    }
    
    public void optimize(QueryExecPlan plan) {
        AbstractAction rootAction = plan.getRootAction();
        optimizeAction(rootAction);
        checkFullTableScan(rootAction);
    }
    
    private boolean getSysFullTableScan() {
    	CMSDBConfig dbConfig = context.getDbConfig();
		Map<String, Object> configs = dbConfig.getCurrentConfiguration();
		if (configs.containsKey(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN)
				&& (configs.get(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN) instanceof Boolean)) {
			return (Boolean) (configs.get(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN));
		}
		return false;
	}

    private void checkFullTableScan(AbstractAction rootAction) {
        int queryCost = rootAction.getSubTreeCost();
        boolean isFullTableScan = QueryCostEnum.isFullTableScan(queryCost);
        
        // query level flag
        boolean queryAllowFullTableScan = context.isAllowFullTableScan();
        // system level flag
        boolean systemAllowFullTableScan = getSysFullTableScan();
        // reject full table scan
        if (isFullTableScan && (!queryAllowFullTableScan || !systemAllowFullTableScan)) {
            String query = context.getQueryString();
            throw new QueryOptimizeException(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN, 
                    "Reject full table scan query! " 
                            + query);
        }        
        // reject full table scan if system is in overload or critical state
        if (isFullTableScan && context.getHighLoadLevel() >= 1) {
            String query = context.getQueryString();
            throw new QueryOptimizeException(QueryErrCodeEnum.REJECT_FULL_TABLE_SCAN,
                    "Reject full table scan query as system is in overload state now! Please try later! "
                            + query);
        }
        
        if (!context.isSysAllowRegexFullScan() && queryCost == QueryCostEnum.RangeRegxScan.getValue()) {
            // as a smooth query control on production, set request tracking code instead of reject directly as stage1
            context.setRequestTrackingCode(CMSTrackingCodeEnum.QUERY_REGEX_FULL_TABLE_SCAN);
//            throw new QueryOptimizeException(QueryErrCodeEnum.REJECT_REGEX_FULL_TABLE_SCAN,
//                    "Reject regex query that cannot use query index on this deployment. Please revise the query, case-sensitive/prefix-regex on index field with fine-grained wildcard is preferred."
//                            + context.getQueryString());
        }
    }

    /**
     * post-order traversal execution plan to calculate the cost
     * 
     * @param action
     */
    private void optimizeAction(AbstractAction action) {
        // pre-order : analysis search action cost
        if (action.getType() == QueryActionTypeEnum.SEARCH) {
            SearchAction searchAction = (SearchAction)action;
            int cost = analysisSearchCost(searchAction);
            searchAction.setSearchCost(cost);
        }
        
        // recursively call children actions
        for (AbstractAction childAction : action.getChildrenActions()) {
            optimizeAction(childAction);
        }
        
        // post-order : callback action to do some optimization
        action.optimize();
    }

    private int analysisSearchCost(SearchAction action) {
        int cost = QueryCostEnum.FullScan.getValue(); 
        SearchQuery searchQuery = action.getSearchQuery();
        ParseQueryNode parseNode = (ParseQueryNode)action.getParseNode();
        if (parseNode.hasTypeCast()) {
            cost = QueryCostEnum.EqualityIndex.getValue();
            for (MetaClass metaClass : parseNode.getTypeCast()) {
                int castTypeCost = calculateSearchCriteriaCost(searchQuery.getSearchCriteria(), metaClass);
                castTypeCost = calculateFullTableScanCost(metaClass, castTypeCost);
                if (castTypeCost > cost) {
                    cost = castTypeCost;
                }
            }
        } else {
            cost = calculateSearchCriteriaCost(searchQuery.getSearchCriteria(), searchQuery.getMetadata());
            cost = calculateFullTableScanCost(searchQuery.getMetadata(), cost);
        }
        return cost;
    }

    private int calculateSearchCriteriaCost(SearchCriteria criteria, MetaClass metaClass) {
        int cost = QueryCostEnum.FullScan.getValue();
        if (criteria instanceof FieldSearchCriteria) {
            cost = calculateFieldSearchCost((FieldSearchCriteria)criteria, metaClass);
        } else if (criteria instanceof LogicalSearchCriteria) {
            cost = calculateLogicalSearchCost((LogicalSearchCriteria)criteria, metaClass);
        }
        return cost;
    }
    
    private int calculateFieldSearchCost(FieldSearchCriteria criteria, MetaClass metaClass) {
        int cost = QueryCostEnum.FullScan.getValue();
        boolean hasIndex = isIndexUsable(metaClass, criteria);
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case EQ :                    
                if (hasIndex) {
                    cost = QueryCostEnum.EqualityIndex.getValue();
                } else {
                    cost = QueryCostEnum.EqualityScan.getValue();
                }
                break;
            case GT:
            case LT:
            case GE:
            case LE:
            case IN:
            case CONTAINS:
            case ISNULL:
            case ISEMPTY:
                if (hasIndex) {
                    cost = QueryCostEnum.RangeIndex.getValue();
                } else {
                    cost = QueryCostEnum.RangeScan.getValue();
                }
                break;
            case REGEX:
                RegexValue regValue = (RegexValue) criteria.getValue();
                if (hasIndex && regValue.caseSensitive && regValue.value.startsWith("^")) {
                    cost = QueryCostEnum.RangeIndex.getValue();
                } else {
                    cost = QueryCostEnum.RangeRegxScan.getValue();
                }
                break;
            case NE:
            case NIN:
            case NREGEX:
            case NCONTAINS:
            case NISNULL:
            case NISEMPTY:
                cost = QueryCostEnum.NegativeScan.getValue();                
                break;
        }
        return cost;
    }
    

    private int calculateLogicalSearchCost(LogicalSearchCriteria criteria, MetaClass metaClass) {   
        int cost = QueryCostEnum.FullScan.getValue();
        int maxcost = QueryCostEnum.EqualityIndex.getValue();
        int mincost = QueryCostEnum.FullScan.getValue();
        List<SearchCriteria> children = criteria.getChildren();
        for (SearchCriteria childCriteria : children) {
            int childcost = calculateSearchCriteriaCost(childCriteria, metaClass);
            if (childcost > maxcost) {
                maxcost = childcost;
            }
            if (childcost < mincost) {
                mincost = childcost;
            }
        }
        
        LogicOperatorEnum op = criteria.getOperator();        
        if (op == LogicOperatorEnum.AND) {
            cost = mincost;
        } else {
            cost = maxcost;
        }
        return cost;
    }
    
    // FIXME : 1. use metadata directly; 2. move it to branchservice; 3. keep cache freshness
    private int calculateFullTableScanCost(MetaClass metaClass, int cost) {
        if (!metaClass.isEmbed() && QueryCostEnum.isFullTableScan(cost)) {
            if (metaClass.isAllowFullTableScan()) {
                return QueryCostEnum.AllowFullTableScan.getValue();
            }
            PersistenceContext pctxt = context.getPersistenceContext(metaClass);
            IMetadataService msvc = context.getMetadataService(metaClass.getRepository());
            String collectionName = pctxt.getDBCollectionName(metaClass);
            int docCnt = msvc.getCollectionCount(collectionName);
            if (docCnt < context.getSmallTableThreshold()) {
                return QueryCostEnum.AllowFullTableScan.getValue();
            } 
        }
        return cost;
    }

    private boolean isIndexUsable(MetaClass metaClass, FieldSearchCriteria criteria) {
        ISearchField field = criteria.getSearchField();
        ISearchStrategy strategy = context.getRegistration().searchStrategy;
        return strategy.isIndexUsable(field, metaClass);
    }

}
