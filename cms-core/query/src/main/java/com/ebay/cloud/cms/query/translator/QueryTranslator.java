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

package com.ebay.cloud.cms.query.translator;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.AbstractAction.QueryActionTypeEnum;
import com.ebay.cloud.cms.query.executor.AbstractSetAction;
import com.ebay.cloud.cms.query.executor.AggregateAction;
import com.ebay.cloud.cms.query.executor.EmbedActionContainer;
import com.ebay.cloud.cms.query.executor.InnerJoinAction;
import com.ebay.cloud.cms.query.executor.IntersectionAction;
import com.ebay.cloud.cms.query.executor.LeftOutterJoinAction;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.RootJoinAction;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.executor.SearchCursor;
import com.ebay.cloud.cms.query.executor.UnionAction;
import com.ebay.cloud.cms.query.executor.result.QueryFieldValueProvider;
import com.ebay.cloud.cms.query.metadata.QueryMetaClass;
import com.ebay.cloud.cms.query.metadata.ReverseMetaRelationship;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseLinkNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.QueryCursor;

/**
 * it translate the parse node tree into logic actions in execution plan
 * 
 * @author xjiang, liasu
 *
 */
public class QueryTranslator {

    private QueryContext queryContext;
    private int paginationIndex;
    // help information to quick locate the aggregation action, 
    // this is based on the pre-order traverse sequence of the query tree
    private int nodeIndex;
    private int aggregationIndex;

    private int querySeq;
    
    private boolean hasEmbed;
    private AbstractAction rootAction;
    
    private List<SearchAction> embedActionList;
    
    public QueryTranslator(QueryContext context) {
        this.queryContext = context;
        this.paginationIndex = 0;
        this.nodeIndex = 0;
        this.querySeq = -1;
        this.aggregationIndex = -1;
        this.hasEmbed = false;
        this.rootAction = null;
        this.embedActionList = new ArrayList<SearchAction>(10);
    }
    
    public QueryExecPlan translate(ParseBaseNode rootNode) {
        QueryExecPlan execPlan = new QueryExecPlan();
        
        rewriteParseTree(rootNode, null);

		rewriteProjection(rootNode);

		// reset the node index, traverse again for translating
		nodeIndex = 0;
		rootAction = translateParseTree(rootNode, execPlan);
		
		// check system config for query throttling
    	checkTooManyCollectionsQuery(execPlan);
		
		// support root level set join
		rewriteRootAction(execPlan);
        
        if (hasEmbed) {
            constructEmbedContainer();
        }
        
        execPlan.setRootAction(rootAction);
        execPlan.setRootNode(rootNode);

        // check for one table aggregation
        if (execPlan.getAggAction() != null) {
        	if (rootAction.getChildrenActions().isEmpty()) {
        		AggregateAction aggAction = (AggregateAction)execPlan.getAggAction();
        		aggAction.setOneTableAggregation(true);
        	}
        	if (queryContext.isCountOnly()) {
        		throw new QueryParseException(QueryErrCodeEnum.AGG_COUNT_NOT_SUPPORT,
	                    "Aggregation query does not support count mode!");
        	}
        }

        return execPlan;
    }

	private void rewriteRootAction(QueryExecPlan execPlan) {
		if (rootAction instanceof AbstractSetAction) {
		    if (execPlan.getAggAction() != null) {
	            throw new QueryParseException(QueryErrCodeEnum.ROOT_LEVEL_JOIN_WITH_AGG,
	                    "Root level set operator could not contain aggregation operation!");
		    }
		    
		    AbstractAction action = rootAction;
		    rootAction = new RootJoinAction(queryContext);
		    rootAction.addChildAction(action);
		}
	}

	private void constructEmbedContainer() {
        Map<AbstractAction, SearchAction> visitedActionMap = new HashMap<AbstractAction, SearchAction>();
        
        for (SearchAction embedAction : embedActionList) {
            AbstractAction action = embedAction;
            // the path from the embed search action to the root search action
            Set<AbstractAction> containedActionSet = new HashSet<AbstractAction>();
            
            while (action != null) {
                // if the action is visited, it means the path has been constructed.
                // break to the next embed search action.
                SearchAction visitedEmbedSearch = visitedActionMap.get(action);
                if (visitedEmbedSearch != null) {
                    visitedEmbedSearch.getEmbedActionContainer().addContainedActions(containedActionSet);
                    embedAction.setEmbedActionContainer(visitedEmbedSearch.getEmbedActionContainer());
                    break;
                }
                visitedActionMap.put(action, embedAction);
                
                containedActionSet.add(action);
                if (action.getType() == QueryActionTypeEnum.SEARCH) {
                    SearchAction searchAction = (SearchAction)action;
                    // if it is the root search action, add all actions on the current path to its container.
                    if (!searchAction.isEmbedSearch()) {
                        EmbedActionContainer container = searchAction.getEmbedActionContainer();
                        if (container == null) {
                            container = new EmbedActionContainer(queryContext, searchAction);
                            searchAction.setEmbedActionContainer(container);
                        }
                        container.addContainedActions(containedActionSet);
                        embedAction.setEmbedActionContainer(container);
                        action = null;
                    } else {
                        action = action.getParentAction();
                    }
                } else {
                    action = action.getParentAction();
                }
            }
        }
    }

    private void rewriteProjection(ParseBaseNode parseNode) {
        List<ParseQueryNode> leafQueryNodes = new LinkedList<ParseQueryNode>();
        boolean hasProjection = rewriteParseNodeProjection(parseNode, null, false, leafQueryNodes);
        // case 0 : A. (b{*} || c) --> got B only
        // case 1 : A. (b || c{*}) --> got C only
        // case 2 : A. (b || c) --> got both B and C
        if (!hasProjection) {
            for (ParseQueryNode leafNode : leafQueryNodes) {
                leafNode.addProjection(ProjectionField.STAR);
                leafNode.markRootDisplay();
            }
        }
    }

    private boolean rewriteParseNodeProjection(ParseBaseNode parseNode, ParseQueryNode prevQueryNode,
            boolean parentDisplay, List<ParseQueryNode> leafNodes) {
    	ParseQueryNode thePrevQueryNode = prevQueryNode;
	    // pre-order: check current query node display setting
	    boolean currentDisplay = parentDisplay;
        if (parseNode instanceof ParseQueryNode) {
            ParseQueryNode queryNode = (ParseQueryNode) parseNode;
            // if found first user display mark root display
            if (!parentDisplay && queryNode.isUserDisplay()) {
                queryNode.markRootDisplay();
            }
            // update prev query node projection
            if (thePrevQueryNode != null) {
                // add reference field.
                MetaRelationship metaReference = queryNode.getMetaReference();
                ISearchField searchField = new ProjectionField(metaReference, true, queryContext.getRegistration().searchStrategy);
                thePrevQueryNode.addProjection(searchField);
            }
            // for reverse reference, need to add the reference to current query node
            if (queryNode.isReverseReference()) {
                MetaRelationship metaReference = ((ReverseMetaRelationship)queryNode.getMetaReference()).getReversedReference();
                ISearchField searchField = new ProjectionField(metaReference, false, queryContext.getRegistration().searchStrategy);
                queryNode.addProjection(searchField);
            }
            thePrevQueryNode = queryNode;
            // group handling for group node
            if (queryNode.hasGroup() && queryNode.getProjection().isEmpty()) {
                queryNode.addProjection(ProjectionField.STAR);
                queryNode.markRootDisplay();
            }
            // update current display setting for child traverse
            currentDisplay = parentDisplay || queryNode.isUserDisplay();
            // update leaf node list
            if (queryNode.getNextNodes().isEmpty()) {
                leafNodes.add(queryNode);
            }
        }

        // traverse children nodes
        boolean childDisplay = false;
        for (ParseBaseNode childNode : parseNode.getNextNodes()) {
            boolean nodeDisplay = rewriteParseNodeProjection(childNode, thePrevQueryNode, currentDisplay, leafNodes);
            childDisplay = nodeDisplay || childDisplay;
        }

        // post-order: if parent & children both have user display, then current node should user display
        boolean subTreeDisplay = childDisplay;
        if (parseNode instanceof ParseQueryNode) {
            ParseQueryNode queryNode = (ParseQueryNode) parseNode;
            if (!queryNode.isUserDisplay() && childDisplay && parentDisplay) {
                queryNode.markUserDisplay();
            }
            subTreeDisplay = childDisplay || queryNode.isUserDisplay();
        }
        return subTreeDisplay;
    }
    
    //
    // pre-order traverse the parse node tree.
    //
	private void rewriteParseTree(ParseBaseNode parseNode, ParseQueryNode prevQueryNode) {
		ParseQueryNode thePrevQueryNode = prevQueryNode;
        if (parseNode instanceof ParseQueryNode) {
            ParseQueryNode queryNode = (ParseQueryNode)parseNode;
            rewriteQueryNode(queryNode, thePrevQueryNode);
            if (thePrevQueryNode != null) {
            	thePrevQueryNode.addNextQueryNode(queryNode);
            }
            thePrevQueryNode = queryNode;
        } 
        
        nodeIndex++;
        // recursive traversal query tree
        for (ParseBaseNode childNode : parseNode.getNextNodes()) {
            rewriteParseTree(childNode, thePrevQueryNode);
        }
    }

    private void rewriteQueryNode(ParseQueryNode queryNode, ParseQueryNode prevQueryNode) {
        rewriteMetaClass(queryNode);
        rewriteAggregation(queryNode);
        rewriteMetaReference(queryNode, prevQueryNode);
        rewritePagination(queryNode);
    }

    //
    // rewrite the query meta class if not set yet.
    //
    private void rewriteMetaClass(ParseQueryNode queryNode) {
        // update query metaclass
        QueryMetaClass queryMeta = QueryMetaClass.newInstance(queryContext, queryNode.getMetaClass());
        queryNode.setMetaClass(queryMeta);
        // update query typecasts metaclass
        List<MetaClass> typeCasts = new ArrayList<MetaClass>(queryNode.getTypeCast().size());
        for (MetaClass mc : queryNode.getTypeCast()) {
            queryMeta = QueryMetaClass.newInstance(queryContext, mc);
            typeCasts.add(queryMeta);
        }
        queryNode.setTypeCasts(typeCasts);
    }

    private void rewriteAggregation(ParseQueryNode queryNode) {
        if (queryNode.hasGroup()) {
            // create virtual metadata with aggregation fields on current query node
            MetaClass oldMetaClass = queryNode.getMetaClass();
            QueryMetaClass queryMetaclass = QueryMetaClass.newInstance(queryContext, oldMetaClass);
            queryNode.setMetaClass(queryMetaclass);
            queryMetaclass.addAggregationFields(queryNode.getGroup());
            this.aggregationIndex = nodeIndex;
        }
    }
    
    private void rewriteMetaReference(ParseQueryNode queryNode, ParseQueryNode prevQueryNode) {
        if (queryNode.getMetaReference() != null) {
            String reverReferenceName = queryNode.getMetaReference().getName();
            if (queryNode.isReverseReference()) {
                // create virtual metadata with reverse reference on previous query node
                MetaClass prevMetaClass = prevQueryNode.getMetaClass();
                QueryMetaClass prevQueryMetaclass = QueryMetaClass.newInstance(queryContext, prevMetaClass);
                MetaRelationship reference = (MetaRelationship) queryNode.getMetaClass().getFieldByName( reverReferenceName);
                MetaRelationship reverseMetaReference = prevQueryMetaclass.addReverseField(queryNode.getMetaClass(),
                        reference);
                // change reverse reference on current node
                queryNode.setReverseMetaReference(reverseMetaReference);
            } else {
                // update meta reference
                queryNode.setMetaReference((MetaRelationship) prevQueryNode.getMetaClass().getFieldByName(
                        reverReferenceName));
            }
        }
    }

    private void rewritePagination(ParseQueryNode queryNode) {
        boolean isSkipBased = queryContext.getPaginationMode() == PaginationMode.SKIP_BASED;
        int skip = isSkipBased ? queryContext.getCursor().getSkip(paginationIndex) : 0;
        int limit = queryContext.getCursor().getLimit(paginationIndex);
        QueryCursor queryCursor = queryContext.getQueryCursor();
        // make skip and cursorValue exclusive from this entry point
        JsonEntity entity = null;
        if (!isSkipBased) {
            if (queryCursor.isJoinCursor()) {
                entity = queryCursor.getJoinCursorValue(paginationIndex, queryNode.getMetaClass());
            } else {
                entity = queryCursor.getSingleCursorValue();
            }
        }
        SearchCursor searchCursor = new SearchCursor(skip, limit, entity);
        queryNode.setCursor(searchCursor);
        // increase pagination index
        paginationIndex++;
    }
    
    private Integer getSysLimitJoinedCollections(CMSDBConfig dbConfig) {
		Map<String, Object> configs = dbConfig.getCurrentConfiguration();
		if (configs.containsKey(CMSDBConfig.SYS_LIMIT_JOINED_COLLECTIONS)
				&& (configs.get(CMSDBConfig.SYS_LIMIT_JOINED_COLLECTIONS) instanceof Number)) {
			return ((Number)(configs.get(CMSDBConfig.SYS_LIMIT_JOINED_COLLECTIONS))).intValue();
		}
		return null;
	}

    private AbstractAction translateParseTree(ParseBaseNode parseNode, QueryExecPlan execPlan) {
        parseNode.setQuerySequenceLowerBound(querySeq);
        
        ParseBaseNode.ParseNodeTypeEnum nodeType = parseNode.getType();
        AbstractAction queryAction = null;
        switch (nodeType) {
            case QUERY:
                queryAction = translateQueryNode((ParseQueryNode) parseNode, execPlan);
                querySeq = ((ParseQueryNode) parseNode).getQuerySequence();
                break;
            case UNION:
                queryAction = translateUnionNode(parseNode);
                break;
            case INTERSECTION:
                queryAction = translateIntersectionNode(parseNode);
                break;
            case INNERJOIN:
            	queryAction = translateInnerJoinNode((ParseLinkNode)parseNode);
                break;
            case LEFTJOIN:
                queryAction = translateLeftJoinNode((ParseLinkNode)parseNode);
                break;
        }
        execPlan.incActionCount();

        nodeIndex++;
        // recursive traversal query tree
        for (ParseBaseNode childNode : parseNode.getNextNodes()) {
            AbstractAction childAction = translateParseTree(childNode, execPlan);
            queryAction.addChildAction(childAction);
        }

        parseNode.setQuerySequenceUpperBound(querySeq);
        
        return queryAction;
    }
    
    private void checkTooManyCollectionsQuery(QueryExecPlan execPlan) {
    	CMSDBConfig dbConfig = queryContext.getDbConfig();
    	Integer sysLimitJoinedCollection = getSysLimitJoinedCollections(dbConfig);
    	if (sysLimitJoinedCollection != null) {
	    	int currentSearchCount = execPlan.getSearchCount();
	    	if(currentSearchCount > sysLimitJoinedCollection) {
	    		throw new QueryOptimizeException(QueryErrCodeEnum.TOO_MANY_JOINED_COLLECTIONS, String.format("Exceed system limit %d joins!", sysLimitJoinedCollection));
	    	}
    	}
    }


    private AbstractAction translateQueryNode(ParseQueryNode parseNode, QueryExecPlan execPlan) {
        // translate sub query execution plan
        for (ParseBaseNode subQueryNode : parseNode.getSubQueryNodes()) {
            QueryContext subQueryContext = createSubQueryContext(queryContext);
            QueryTranslator subTranslator = new QueryTranslator(subQueryContext);
            QueryExecPlan subQueryPlan = subTranslator.translate(subQueryNode);
            // update search criteria value provider
            FieldSearchCriteria fsc = subQueryNode.getSubQueryCriteria();
            fsc.setValueProvider(new QueryFieldValueProvider(subQueryPlan, fsc.getSearchField(), subQueryContext));
            execPlan.addSubPlan(subQueryPlan);
            execPlan.incSearchCount(subQueryPlan.getSearchCount());
        }
        
        SearchAction action = null;
        execPlan.incSearchCount(1);
        // check embed relationship
        MetaRelationship metaRef = parseNode.getMetaReference();
        if (metaRef != null && metaRef.getRelationType() == RelationTypeEnum.Embedded) {
            this.hasEmbed = true;
            action = new SearchAction(queryContext, parseNode);
            embedActionList.add(action);
        } else {
            action = new SearchAction(queryContext, parseNode);
        }
        
        if (this.nodeIndex == aggregationIndex) {
            AggregateAction aggAction = new AggregateAction(queryContext, action, parseNode);
            execPlan.setAggAction(aggAction);
        }
        return action;
    }

    private QueryContext createSubQueryContext(QueryContext queryContext) {
        QueryContext subContext = new QueryContext(queryContext);
        subContext.setSubQuery(true);
        subContext.setCountOnly(false);
        subContext.removeQueryCursor();
        subContext.clearMetadataServices();
        subContext.setMaxFetch(queryContext.getMaxFetch());
        return subContext;
    }

    private AbstractAction translateInnerJoinNode(ParseLinkNode parseNode) {
        return new InnerJoinAction(queryContext, parseNode, nodeIndex < aggregationIndex);
    }

    private AbstractAction translateIntersectionNode(ParseBaseNode node) {
        return new IntersectionAction(queryContext, node);
    }

    private AbstractAction translateUnionNode(ParseBaseNode node) {
        return new UnionAction(queryContext, node);
    }

    private AbstractAction translateLeftJoinNode(ParseLinkNode parseNode) {
        return new LeftOutterJoinAction(queryContext, parseNode, nodeIndex < aggregationIndex);
    }

}
