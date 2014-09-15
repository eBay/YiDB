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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author xjiang
 *
 */
public class SearchAction extends AbstractAction {

    private static final Logger   logger       = LoggerFactory.getLogger(SearchAction.class);
    
    private final boolean isSubQuery;
    
    protected final ParseQueryNode parseNode;
    protected SearchQuery searchQuery;
    private int execSeq;
    private int searchCost;
    private boolean hasMore;
    private SearchOption searchOption;
    protected SearchCriteria parentJoinCriteria;
    protected SearchCriteria childJoinCriteria;
    private SearchCriteriaJoinner joinner;
    protected EmbedSearchQuery embedQuery;
    
    private SearchCursor nextCursor;
    private EmbedActionContainer embedContainer;
    private boolean embedSearch;
    
    public SearchAction(QueryContext context, ParseQueryNode parseNode) {
        super(context, parseNode, QueryActionTypeEnum.SEARCH);
        this.parseNode = parseNode;
        this.isSubQuery = queryContext.isSubQuery();
        this.searchQuery = new SearchQuery(parseNode.getMetaClass(), parseNode.getCriteria(),
                parseNode.getProjection(), context.getRegistration().searchStrategy);
        this.searchCost = QueryCostEnum.FullScan.getValue();
        
        MetaRelationship metaRef = parseNode.getMetaReference();
        if (metaRef != null && metaRef.getRelationType() == RelationTypeEnum.Embedded) {
            this.embedSearch = true;
        } else {
            this.embedSearch = false;
        }
        
        initSearchOption();
        joinner = new SearchCriteriaJoinner(this, this.parseNode);
    }
    
    public EmbedActionContainer getEmbedActionContainer() {
        return embedContainer;
    }
    
    public void setEmbedActionContainer(EmbedActionContainer container) {
        this.embedContainer = container;
    }
    
    public boolean isEmbedSearch() {
        return embedSearch;
    }
    
    public SearchQuery getSearchQuery() {
        return searchQuery;
    }
    
    public void setSearchCost(int cost) {
        searchCost = cost;
    }
    public int getSearchCost() {
        return searchCost;
    }
    
    public ParseQueryNode getParseNode() {
        return parseNode;
    }
    
    public SearchOption getSearchOption() {
        return this.searchOption;
    }
    
    public EmbedSearchQuery getEmbedQuery() {
        return embedQuery;
    }
    public void setEmbedQuery(EmbedSearchQuery embedQuery) {
        this.embedQuery = embedQuery;
    }
    
    private void initSearchOption() {        
        this.searchOption = new SearchOption();
        searchOption.setStrategy(getQueryStrategy());
        // set explain
        if (queryContext.needExplain()) {
            searchOption.setExplanation();
        }
        // set count query
        searchOption.setOnlyCount(queryContext.isCountOnly());
        // set pagination
        SearchActionHelper.initPagination(searchOption, parseNode);
//        if (parseNode.hasLimit()) {
//            searchOption.setLimit(parseNode.getLimit() + SearchOption.LOOK_FORWARD);
//        } else {
//            searchOption.setLimit(SearchOption.DEFAULT_LIMIT + SearchOption.LOOK_FORWARD);
//        }
//        searchOption.setLookForward(true);
//        
//        if (parseNode.hasSkip()) {
//            searchOption.setSkip(parseNode.getSkip());
//        }
        // sub query based on max fetch
        if (isSubQuery && queryContext.hasMaxFetch()) {
            searchOption.setLimit(queryContext.getMaxFetch());
        }
        
        if (queryContext.isSubQuery()) {
            // sub query need involve all data sets. Don't set sort order,otherwise
            // we might easily hit the 32m mongo limitation on sort.
            return;
        }
        if (!parseNode.isRootDisplay()) {
            searchOption.setSort(Arrays.asList(InternalFieldEnum.ID.getName()), Arrays.asList(SearchOption.ASC_ORDER),
                    parseNode.getMetaClass());
            return;
        }
        // validate and add sort on / sort order
        SearchActionHelper.initSortOn(searchOption, parseNode, queryContext, searchQuery, getQueryStrategy());
//        if (queryContext.hasSortOn()) {
//            List<String> sortOnList = queryContext.getSortOn();    
//            // append default sortOn _oid
//            if (!sortOnList.contains(InternalFieldEnum.ID.getName()) && queryContext.getPaginationMode() == PaginationMode.ID_BASED) {
//                sortOnList.add(InternalFieldEnum.ID.getName());
//            }
//            List<Integer> sortOrderList = new ArrayList<Integer>(sortOnList.size());
//            List<ISearchField> sortOnFieldList = new ArrayList<ISearchField>(sortOnList.size());
//            
//            MetaClass metaClass = parseNode.getMetaClass();
//            Map<String, GroupField> grpFields = null;
//            if (parseNode.getGroup() != null) {
//                grpFields = parseNode.getGroup().getGrpFields();
//            }
//            
//            for (String sortFieldName : sortOnList) {
//                String[] fields = sortFieldName.split("\\.");
//                MetaField sortMetaField = metaClass.getFieldByName(fields[0]);
//                if (sortMetaField == null) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.METAFIELD_NOT_FOUND, "Can't find sort field " + sortFieldName + " on " + metaClass.getName());
//                }
//                // array sort not supported
//                if (sortMetaField.getCardinality() == CardinalityEnum.Many && fields.length == 1) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.ARRAY_SORT_NOT_SUPPORT, "Can't sort on array field " + sortFieldName + " on " + metaClass.getName());
//                }
//                if (sortMetaField.getDataType() == DataTypeEnum.JSON) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.JSON_SORT_NOT_SUPPORT, "Can't sort on json field " + sortFieldName + " on " + metaClass.getName());
//                }
//                String innerField = fields.length > 1 ? StringUtils.join(ArrayUtils.subarray(fields, 1, fields.length), '.') : null;
//                
//                ISearchField sortOnField = null;
//                if (grpFields != null) {
//                    sortOnField = grpFields.get(fields[0]);
//                } 
//
//                if (sortOnField == null) {
//                    sortOnField = new SelectionField(sortMetaField, innerField, getQueryStrategy());
//                }
//                sortOnFieldList.add(sortOnField);
//                
//                // sortOn must be in projection for ID based pagination
//                ProjectionField projField = new ProjectionField(sortMetaField, innerField, false, getQueryStrategy());
//                if (!searchQuery.getSearchProjection().getFields().contains(projField)) {
//                    searchQuery.getSearchProjection().getFields().add(projField);
//                }
//            }
//            if (queryContext.hasSortOrder()) {
//                List<SortOrder> soList = queryContext.getSortOrder();
//                for (SortOrder order : soList) {
//                    if (order == SortOrder.asc) {
//                        sortOrderList.add(SearchOption.ASC_ORDER);
//                    } else {
//                        sortOrderList.add(SearchOption.DESC_ORDER);
//                    }
//                }
//            } else {
//                // set default sort order as ascend
//                for (int i = 0; i < sortOnFieldList.size(); i++) {
//                    sortOrderList.add(SearchOption.ASC_ORDER);
//                }
//            }
//            searchOption.setSortField(sortOnFieldList, sortOrderList, metaClass);
//        } else {
//            // sort on _oid if not given
//            searchOption.setSort(Arrays.asList(InternalFieldEnum.ID.getName()), Arrays.asList(SearchOption.ASC_ORDER),
//                    parseNode.getMetaClass());
//        }
    }

    @Override
    public void optimize() {
        // calculate sub tree cost
        calculateSubTreeCost();
        // setup and_or_join_map
        this.andOrJoinMap = new ArrayList<List<ParseQueryNode>>(1);
        List<ParseQueryNode> andJoinMap = new ArrayList<ParseQueryNode>(1);
        andJoinMap.add(this.parseNode);
        this.andOrJoinMap.add(andJoinMap);
        
        if (embedContainer != null && !embedSearch) {
            embedContainer.optimize();
        }
    }

    private void calculateSubTreeCost() {
        int childSubTreeCost = QueryCostEnum.FullScan.getValue();
        if (!this.childrenActions.isEmpty()) {
            // calculate sub tree cost
            AbstractAction childAction = this.childrenActions.get(0);
            childSubTreeCost = childAction.getSubTreeCost();
        }
        this.subTreeCost = this.searchCost < childSubTreeCost ? this.searchCost : childSubTreeCost;
    }
    
    @Override
    protected boolean doAction(ExecuteContext context, boolean postOrderVisit) {
        if (embedContainer != null) {
            return embedContainer.doAction(context, postOrderVisit);
        }
        else {
            createJoinCriteria();
            if (isExecutable(context))  {
                this.execSeq = context.searchExecSeq++;
                executeSearch(context);
                return true;
            }
            return false;
        }
    }

    protected void createJoinCriteria() {
        if (parentAction != null) {
            parentJoinCriteria = parentAction.getTailJoinCriteria(parseNode.getMetaReference());
        }
        if (!childrenActions.isEmpty()) {
            childJoinCriteria = childrenActions.get(0).getHeadJoinCriteria();
        }
    }

    protected boolean isExecutable(ExecuteContext context) {
        if (embedSearch) {
            return getEmbedActionContainer().isExecutable(context);
        }

        // step 1: check query hint: hint action must be executed at the first
        if (queryContext.hasHint() && !context.hintExecuted) {
            if (parseNode.getQuerySequence() == queryContext.getHint()) {
                context.hintExecuted = true;
            }
            return context.hintExecuted;
        }
        
        // step 2: check descent
        boolean isDescentsDone = true;
        if (!childrenActions.isEmpty()) {
            isDescentsDone = childrenActions.get(0).isDescentsDone();
        }
        
        // based on cost and parent/child execution
        if (isDescentsDone
                || searchCost <= subTreeCost 
                || parentJoinCriteria != null
                || childJoinCriteria != null) {
            return true;
        }
        return false;
    }
    
    private void executeSearch(ExecuteContext context) {
        logger.debug("executing action:" + toString());
        // short-circut the execution if parent or child has empty result
        if (shortCircuitExecute()) {
            return;
        }
        // rewrite the query
        SearchQuery rewrittenQuery = rewriteQuery(searchQuery);
        // execute the rewritten query
        SearchResult searchResult = executeQuery(rewrittenQuery, context);
        ActionResult actionResult = new ActionResult(searchResult, parseNode);
        this.getActionResults().add(actionResult);
        // add explanation
        if (queryContext.needExplain()) {
            context.explanations.addAll(searchResult.getQueryExplanations());
        }
    }

    protected boolean shortCircuitExecute() {
        return parentJoinCriteria == EmptySearchCriteria.EMPTY_CRITERIA 
                || childJoinCriteria == EmptySearchCriteria.EMPTY_CRITERIA;
    }
    
    protected SearchQuery rewriteQuery(SearchQuery query) {
        // append join criteria
        query.appendSearchCriteria(parentJoinCriteria);
        
        query.appendSearchCriteria(childJoinCriteria);
        
        // append internal status criteria
        SearchCriteria statusCriteria = new FieldSearchCriteria(
                InternalFieldFactory.getInternalMetaField(InternalFieldEnum.STATUS),
                getQueryStrategy(),
                FieldOperatorEnum.EQ,
                StatusEnum.ACTIVE.toString());
        query.appendSearchCriteria(statusCriteria);

        // append pagination criteria
        SearchCriteria criteria = parseNode.getCursor().getCursorCriteria(searchOption, parseNode, queryContext);
        query.appendSearchCriteria(criteria);

        return query;
    }

    public final ISearchStrategy getQueryStrategy() {
        return queryContext.getRegistration().searchStrategy;
    }

    protected SearchResult executeQuery(SearchQuery query, ExecuteContext context) {
        ISearchService searchService = queryContext.getSearchService();
        PersistenceContext persistContext = queryContext.getPersistenceContext(searchQuery.getMetadata());
        SearchResult searchResult = new SearchResult(parseNode.getMetaClass());
        if (parseNode.hasTypeCast()) {
            for (MetaClass queryCastType : parseNode.getTypeCast()) {
                SearchQuery castQuery = new SearchQuery(queryCastType, query, getQueryStrategy());
                SearchResult castResult = searchService.search(castQuery, searchOption, persistContext);
                // TODO : type cast pagination handling ??
                evaluateNextCursor(searchResult);
                searchResult.merge(castResult);
            }
        } else {
            searchResult = searchService.search(query, searchOption, persistContext);
            evaluateNextCursor(searchResult);
        }
        return searchResult;
    }
    
    private void evaluateNextCursor(SearchResult searchResult) {
        hasMore = checkHasMore(searchOption, isSubQuery, searchResult);
        if (hasMore) {
            SearchCursor cursor = parseNode.getCursor();
            JsonEntity cursorValue = createNextCursorValue(searchOption, searchResult);
            int limit = cursor.hasLimit() ? cursor.getLimit() : searchOption.getDisplayLimit();
            nextCursor = new SearchCursor(cursor.getSkip() + limit, limit, cursorValue);
        }
    }

    public static JsonEntity createNextCursorValue(SearchOption option, SearchResult result) {
        IEntity lastEntity = result.getCursorEntity();
        if (lastEntity == null) {
            return null;
        }
        // convert db entity to cursor entity
        EntityMapper mapper = new EntityMapper(JsonEntity.class, lastEntity.getMetaClass(), true);
        lastEntity.traverse(mapper);
        JsonEntity buildEntity = (JsonEntity) mapper.getBuildEntity();
        // remove every thing not in sort
        List<ISearchField> fields = option.getSortFields();
        Set<String> sortFieldSet = new HashSet<String>();
        for (ISearchField field : fields) {
            if (field.getInnerField() != null) {
                sortFieldSet.add(field.getFieldName() + field.getInnerField());
            } else {
                sortFieldSet.add(field.getFieldName());
            }
        }
        for (String fieldName : buildEntity.getFieldNames()) {
            if (!sortFieldSet.contains(fieldName)) {
                buildEntity.removeField(fieldName);
            }
        }
        return buildEntity;
    }

    static boolean checkHasMore(SearchOption searchOption, boolean isSubQuery, SearchResult result) {
        boolean hasMore = result.hasMore();
        //
        // sub query result has to be complete. As sub query result might be used in a negative (not @field=& SubQuery)
        // criteria, a incomplete query might have the negative criteria with incorrect result.
        // 
        if (hasMore && isSubQuery) {
            throw new QueryExecuteException(
                  QueryErrCodeEnum.INCOMPLETE_JOIN_QUERY,
                  MessageFormat.format("The sub query join might be imcomplete caused by insufficent data involved, sub query MaxFetch set: {0}, fetched {1}. Consider increase subQueryMaxFetch or rewrite the sub-query.",
                          searchOption.getLimit(), result.getRawCount()));
        }
        return hasMore;
    }

    @Override
    public SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        return joinner.getTailJoinCriteria(referenceField);
    }

    @Override
    public SearchCriteria getHeadJoinCriteria() {
        return joinner.getHeadJoinCriteria();
    }

    @Override
    protected List<AbstractAction> getNextExecActions() {
        return childrenActions;
    }

    public int getExecSeq() {
        return execSeq;
    }

    public void setExecSeq(int execSeq) {
        this.execSeq = execSeq;
    }
    
    public boolean hasMoreResults() {
        return hasMore;
    }
    void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public SearchCursor getNextCursor() {
        return nextCursor;
    }
    
    public void setNextCursor(SearchCursor cursor) {
        this.nextCursor = cursor;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SearchAction[metaclass=").append(parseNode.getMetaClass().getName());
        sb.append(" ,reference=");
        if (parseNode.getMetaReference() != null) {
            sb.append(parseNode.getMetaReference().getName());
        } else {
            sb.append("null");
        }
        sb.append(",isEmbed=" + embedSearch + "]");
        return sb.toString();
    }

    @Override
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta, 
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        
        if (prevEmbedQuery == null || embedSearch) {
            MetaRelationship embedField = parseNode.getMetaReference();
            // root search action's reference should not be in the embed field list
            if (embedField != null && embedSearch) {
                embedFieldList.addLast(embedField);
            }
            EmbedSearchQuery embedSearchQuery = new EmbedSearchQuery(searchQuery, embedFieldList, leftJoin);
            if (!embedSearch) {
                // this is the root search query of an embed
                embedSearchQuery.appendSearchCriteria(parentJoinCriteria);
            }
            setEmbedQuery(embedSearchQuery);
            
            if (childrenActions.size() > 0) {
                IEmbedQuery childQuery = childrenActions.get(0).getHeadJoinQuery(embedSearchQuery, parseNode.getMetaClass(), embedFieldList, leftJoin);
                embedSearchQuery.addChildQuery(childQuery);
            }
            return embedSearchQuery;
        } else {
            SearchProjection searchProject = new SearchProjection();
            ProjectionField pField = new ProjectionField(parseNode.getMetaReference(), false, getQueryStrategy());
            searchProject.addField(pField);
            SearchQuery query = null;
            if (state.selfDone) {
                query = new SearchQuery(meta, getHeadJoinCriteria(), searchProject, getQueryStrategy());
            } else  {
                query = new SearchQuery(meta, null, searchProject, getQueryStrategy());
            }
            return new EmbedSearchQuery(query, embedFieldList, null, leftJoin);
        }
    }

    @Override
    public boolean isChildrenCriteriaReady() {
        if (embedSearch) {
            if (childrenActions.size() > 0) {
                return childrenActions.get(0).isChildrenCriteriaReady();
            }
            return false;
        } else {
            return joinner.getHeadJoinCriteria() != null;
        }
    }

}