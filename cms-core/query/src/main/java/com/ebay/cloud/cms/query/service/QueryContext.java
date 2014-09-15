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

package com.ebay.cloud.cms.query.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSTrackingCodeEnum;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.PersistenceContextFactory;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.query.metadata.QueryMetadataService;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;


/**
 * query context
 * 
 * @author xjiang
 *
 */
        
public class QueryContext {

    public static final String DAL_FLATTEN   = "flatten";

    public static final String DAL_HIERARCHY = "hierarchy";

    public static enum SortOrder {
        asc, desc
    };
    public static enum PaginationMode {
        SKIP_BASED, ID_BASED;
        public static PaginationMode defaultMode() {
            return QueryContext.defaultPaginationMode;
        }
    };
    private static PaginationMode defaultPaginationMode = PaginationMode.ID_BASED;
    
    private static int defaultSmallTableThreshold = 1000;
    
    public static void setDefaultSmallTableThreshold(int defaultSmallTable) {
        defaultSmallTableThreshold = defaultSmallTable;
    }

    private final String repositoryName;
    private final String branchName;
    
    /*
     * A map of query meta service that would hold the override message for the given query. 
     */
    private Map<String, QueryMetadataService> metadataServicesMap;
    private IRepositoryService repoService;
    private ISearchService    searchService;
    private IBranchService    branchService;
    private PersistenceContext persistenceContext;
    private String queryString;
    
    private boolean subQuery;
    private boolean explain;
    private boolean showDisplayMeta;
    private int smallTableThreshold;

    private String cursorString;
    private QueryCursor cursor;
    private PaginationMode pageMode;
    
    private boolean allowFullTblScan;
    private boolean sysAllowRegexFullScan;
    private boolean countOnly;
    private int highLoadLevel;
    
    private long dbTimeCost;
    private long totalTimeCost;
    private long startProcessingTime;
    
    // per request information
    private String sourceIP;
    private String requestId;
    // this is the client authentication; for query, actually not the modifier
    private String subject;
    // the user is a optional client controlled string.
    private String userId;
    
    private ConsistentPolicy consistentPolicy;
    
    private CMSDBConfig dbConfig;
    
    private String dal;
    private Registration registration;
    
    private final Map<String, List<SearchCriteria>> additionalCriteria;
    /**
     * A requestTrackingCode is a simple flag. It could used to track specific type query request. This code is determined
     * in query enginee, and outside service could use this to decide return special information like has different
     * service response code. This is usefull when try to start tracking and controlling some kind of heavy query on production
     * from iteration to iteration. 
     * Used as http response status code.
     */
    private CMSTrackingCodeEnum requestTrackingCode;

    public QueryContext(String repositoryName, String branchName) {
        this.repositoryName = repositoryName;
        this.branchName = branchName;
        this.cursor = new QueryCursor();
        this.explain = false;
        this.allowFullTblScan = false;
        this.highLoadLevel = 0;
        this.showDisplayMeta = false;
        this.smallTableThreshold = defaultSmallTableThreshold;
        this.pageMode = PaginationMode.defaultMode();
        this.additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        this.sysAllowRegexFullScan = true;
        this.metadataServicesMap = new HashMap<String, QueryMetadataService>();
    }
    
    public QueryContext(QueryContext other) {
        this.repositoryName = other.repositoryName;
        this.branchName = other.branchName;
        this.explain = other.explain;
        this.allowFullTblScan = other.allowFullTblScan;
        this.smallTableThreshold = other.smallTableThreshold;
        this.pageMode = other.pageMode;
        this.subject = other.subject;
        this.userId = other.userId;
        this.sourceIP = other.sourceIP;
        this.requestId = other.requestId;
        this.consistentPolicy = other.consistentPolicy;
        this.showDisplayMeta = other.showDisplayMeta;
        this.countOnly = other.countOnly;
        this.registration = other.registration;
        this.cursor = new QueryCursor(other.cursor);
        // NOTE: this would cause a new empty metadataservice map, which violates the copy costructor semantic for convenience!
        this.metadataServicesMap = new HashMap<String, QueryMetadataService>();
        this.repoService = other.repoService;
        this.branchService = other.branchService;
        this.searchService = other.searchService;
		this.dal = other.dal;
        this.dbConfig = other.dbConfig;
        // not copy oneTableAggregation
        this.additionalCriteria = new HashMap<String, List<SearchCriteria>>(other.additionalCriteria);
        this.sysAllowRegexFullScan = other.sysAllowRegexFullScan;
    }

    // for testability
    public static PaginationMode getDefaultPaginationMode() {
        return defaultPaginationMode;
    }
    // for testability
    public static void setDefaultPaginationMode(PaginationMode paginationMode) {
        defaultPaginationMode = paginationMode;
    }

    public String getRepositoryName() {
        return repositoryName;
    }
    public String getBranchName() {
        return branchName;
    }
    
    public void setRepositoryService(IRepositoryService service) {
        this.repoService = service;
    }
    public void clearMetadataServices() {
        metadataServicesMap.clear();
    }
    public QueryMetadataService getMetadataService(String reponame) {
        if (metadataServicesMap.get(reponame) == null) {
            IMetadataService metaService = repoService.getRepository(reponame).getMetadataService();
            metadataServicesMap.put(reponame, new QueryMetadataService(metaService));
        }
        return metadataServicesMap.get(reponame);
    }

    public ISearchService getSearchService() {
        return searchService;
    }
    public void setSearchService(ISearchService searchService) {
        this.searchService = searchService;
    }

    public IBranchService getBranchService() {
        return branchService;
    }
    public void setBranchService(IBranchService branchService) {
        this.branchService = branchService;
    }

    public PersistenceContext getPersistenceContext(MetaClass metaClass) {
        PersistenceContext pContext = null;
        if (!repositoryName.equals(metaClass.getRepository())) {
            IMetadataService metaService = metaClass.getMetadataService();
            pContext = createPersistentContext(metaService);
        } else {
            if (persistenceContext == null) {
                persistenceContext = createPersistentContext(getMetadataService(metaClass.getRepository()));
            }
            pContext = persistenceContext;
        }
        pContext.setDbConfig(dbConfig);
        return pContext;
    }

    private PersistenceContext createPersistentContext(IMetadataService metaService) {
        String repoName = metaService.getRepository().getRepositoryName();
        EntityContext entContext = new EntityContext();
        entContext.setRequestId(requestId);
        entContext.setSourceIp(sourceIP);
        entContext.setRegistration(registration);
        entContext.setDbConfig(dbConfig);
        IBranch branch = branchService.getBranch(repoName, branchName, entContext);
        if (branch == null) {
            throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND, "Branch not found: " + branchName);
        }
        return PersistenceContextFactory.createEntityPersistenceConext(metaService, branch, consistentPolicy, registration, true, dbConfig,
                getAdditionalCriteria());
    }

    public long getDbTimeCost() {
        return dbTimeCost;
    }
    public void setDbTimeCost(long dbTimeCost) {
        this.dbTimeCost = dbTimeCost;
    }
    
    public long getTotalTimeCost() {
        return totalTimeCost;
    }
    public void setTotalTimeCost(long totalTimeCost) {
        this.totalTimeCost = totalTimeCost;
    }

    public long getStartProcessingTime() {
        return startProcessingTime;
    }
    public void setStartProcessingTime(long startProcessingTime) {
        this.startProcessingTime = startProcessingTime;
    }

    public String getRequestId() {
        return requestId;
    }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSourceIP() {
        return sourceIP;
    }
    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public boolean hasSortOn() {
        return cursor.hasSortOn();
    }
    public List<String> getSortOn() {
        return cursor.getSortOn();
    }
    public void addSortOn(String field) {
        cursor.addSortOn(field);
    }
    public void removeSortOn() {
        cursor.removeSortOn();
    }
    
    public boolean hasSortOrder() {
        return cursor.hasSortOrder();
    }
    public List<SortOrder> getSortOrder() {
        return cursor.getSortOrder();
    }
    public void addSortOrder(SortOrder field) {
        cursor.addSortOrder(field);
    }
    public void removeSortOrder() {
        cursor.removeSortOrder();
    }
    
    public boolean needExplain() {
        return explain;
    }
    public void setExplain(boolean explan) {
        this.explain = explan;
    }

    public boolean hasHint() {
        return cursor.hasHint();
    }
    public int getHint() {
        return cursor.getHint();
    }
    public void setHint(int hint) {
        cursor.setHint(hint);
    }

    public int[] getSkips() {
        return cursor.getSkips();
    }
    public void setSkips(int[] skips) {
        cursor.setSkips(skips);
    }
    public int getSkip(int index) {
        return cursor.getSkip(index);
    }

    public int[] getLimits() {
        return cursor.getLimits();
    }
    public void setLimits(int[] limits) {
        cursor.setLimits(limits);
    }
    public int getLimit(int index) {
        return cursor.getLimit(index);
    }

    public boolean isAllowFullTableScan() {
        return this.allowFullTblScan;
    }

    public void setAllowFullTableScan(boolean fullScan) {
        this.allowFullTblScan = fullScan;
    }
    
//    public final boolean isSysAllowFullTableScan() {
//        return sysAllowFullTableScan;
//    }
//
//    public final void setSysAllowFullTableScan(boolean sysAllowFullTableScan) {
//        this.sysAllowFullTableScan = sysAllowFullTableScan;
//    }

    public boolean isCountOnly() {
        return countOnly;
    }

    public void setCountOnly(boolean countOnly) {
        this.countOnly = countOnly;
    }

    public final ConsistentPolicy getConsistentPolicy() {
        return consistentPolicy;
    }

    public final void setConsistentPolicy(ConsistentPolicy consistentPolicy) {
        this.consistentPolicy = consistentPolicy;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getHighLoadLevel() {
        return highLoadLevel;
    }

    //overload 1, critical 2, normal 0
    public void setHighLoadLevel(int highLoadLevel) {
        this.highLoadLevel = highLoadLevel;
    }

    public void setQueryString(String query) {
        this.queryString = query;
    }

    public String getQueryString() {
        return this.queryString;
    }

    public void setMaxFetch(int i) {
        this.cursor.setMaxFecth(i);
    }
    public boolean hasMaxFetch() {
        return this.cursor.hasMaxFetch();
    }
    public int getMaxFetch() {
        return this.cursor.getMaxFecth();
    }

    public void setSmallTableThreshold(int i) {
        this.smallTableThreshold = i;
    }
    public int getSmallTableThreshold() {
        return this.smallTableThreshold;
    }
    
    public void setSubQuery(boolean subQuery) {
        this.subQuery = subQuery;
    }
    public boolean isSubQuery() {
        return this.subQuery;
    }

    public boolean isShowDisplayMeta() {
        return showDisplayMeta;
    }
    public void setShowDisplayMeta(boolean showDisplayMeta) {
        this.showDisplayMeta = showDisplayMeta;
    }

    public QueryCursor getQueryCursor() {
        return cursor;
    }
    public void removeQueryCursor() {
        this.cursor = new QueryCursor();
    }

    public void parseQueryCursor(ParseQueryNode node, int totalQueryNodeCount) {
        if (StringUtils.isNullOrEmpty(cursorString)) {
            cursor.setJoinCursor(totalQueryNodeCount > 1);
            return;
        }
        if (totalQueryNodeCount == 1) {
            MetaClass meta = node.getMetaClass();
            JsonEntity entity = new JsonEntity(meta, cursorString);
            cursor.setSingleCursorValue(entity);
        } else {
            String[] joinOids = cursorString.split(",");
            cursor.setJoinCursorValues(Arrays.asList(joinOids));
        }
    }
    public void setCursor(QueryCursor cursor) {
        this.cursor = cursor;
    }
    public QueryCursor getCursor() {
        return cursor;
    }

    public String getCursorString() {
        return cursorString;
    }
    public void setCursorString(String cursorString) {
        this.cursorString = cursorString;
    }

    public PaginationMode getPaginationMode() {
        return pageMode;
    }

    public void setPaginationMode(PaginationMode pageMode) {
        this.pageMode = pageMode;
    }

    public CMSDBConfig getDbConfig() {
		return dbConfig;
	}

    public Integer getSysLimitDocuments() {
        Map<String, Object> configs = dbConfig.getCurrentConfiguration();
        if (configs.containsKey(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY)
                && (configs.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY) instanceof Number)) {
            return ((Number)(configs.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY))).intValue();
        }
        return SearchOption.DEFAULT_MAX_LIMIT;
    }
    
	public void setDbConfig(CMSDBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	public boolean isSysAllowRegexFullScan() {
        return sysAllowRegexFullScan;
    }
    public void setSysAllowRegexFullScan(boolean sysAllowRegexFullScan) {
        this.sysAllowRegexFullScan = sysAllowRegexFullScan;
    }



    /**
     * Cursor object that stands for pagination information for query request.
     * @author liasu
     *
     */
    public static class QueryCursor {
        private int             hint;
        private List<String>    sortOn;
        private List<SortOrder> sortOrder;
        private int[]           skips;
        private int[]           limits;
        private int             maxFecth;
        
        private boolean         isJoinCursor;
        private JsonEntity      singleCursorValue;
        // list of _oids as cursor value
        private List<String>    cursorValues;

        public QueryCursor() {
            this.hint = -1;
        }

        public void setJoinCursor(boolean b) {
            this.isJoinCursor = b;
        }
		
		public QueryCursor(QueryCursor other) {
            this.hint = other.hint;
            this.isJoinCursor = other.isJoinCursor;
            if (other.skips != null) {
                this.skips = Arrays.copyOf(other.skips, other.skips.length);
            }
            if (other.limits != null) {
                this.limits = Arrays.copyOf(other.limits, other.limits.length);
            }
            if (other.sortOn != null) {
                this.sortOn = new ArrayList<String>(other.sortOn);
            }
            if (other.sortOrder != null) {
                this.sortOrder = new ArrayList<QueryContext.SortOrder>(other.sortOrder);
            }
            if (other.cursorValues != null) {
                this.cursorValues = new ArrayList<String>(other.cursorValues);
            }
            if (other.singleCursorValue != null) {
                this.singleCursorValue = other.singleCursorValue;
            }
            this.maxFecth = other.maxFecth;
        }
        public int[] getSkips() {
            return ArrayUtils.clone(skips);
        }
        public void setSkips(int[] skips) {
            if (skips != null) {
                for (int i : skips) {
                    CheckConditions.checkArgument(i >= 0, "skip should be greater than 0");
                }
            }
            this.skips = ArrayUtils.clone(skips);
        }
        public int getSkip(int index) {
            if (skips == null || skips.length <= index) {
                return 0;
            }
            return skips[index];
        }
        public int[] getLimits() {
            return ArrayUtils.clone(limits);
        }
        public void setLimits(int[] limits) {
            this.limits = ArrayUtils.clone(limits);
        }
        public int getLimit(int index) {
            if (limits == null || limits.length <= index) {
                return 0;
            }
            return limits[index];
        }

        public boolean isJoinCursor() {
            return isJoinCursor;
        }

        public JsonEntity getSingleCursorValue() {
            return singleCursorValue;
        }
        public void setSingleCursorValue(JsonEntity cursor) {
            this.singleCursorValue = cursor;
            this.isJoinCursor = false;
        }

        public List<String> getJoinCursorValues() {
            return cursorValues;
        }

        public JsonEntity getJoinCursorValue(int index, MetaClass meta) {
            if (cursorValues == null) {
                return null;
            }
            JsonEntity entity = null;
            if (index >= 0 && cursorValues.size() > index) {
                String oid = cursorValues.get(index);
                if (!StringUtils.isNullOrEmpty(oid)) {
                    entity = new JsonEntity(meta);
                    entity.setId(oid);
                }
            }
            return entity;
        }
        
        public void setJoinCursorValues(List<String> joinCursorValue) {
            this.cursorValues = joinCursorValue;
            this.isJoinCursor = true;
        }

        public int getMaxFecth() {
            return maxFecth;
        }
        public void setMaxFecth(int maxFecth) {
            this.maxFecth = maxFecth;
        }
        public boolean hasMaxFetch() {
            return this.maxFecth > 0;
        }

        public boolean hasHint() {
            return hint >= 0;
        }
        public int getHint() {
            return this.hint;
        }
        public void setHint(int hint) {
            this.hint = hint;
        }

        public boolean hasSortOn() {
            return sortOn != null;
        }
        public List<String> getSortOn() {                   
            return sortOn;
        }
        public void addSortOn(String field) {    
            if (sortOn == null) {
                sortOn = new ArrayList<String>();
            }
            this.sortOn.add(field);
        }
        public void removeSortOn() {
            this.sortOn = null;
        }
        
        public boolean hasSortOrder() {
            return sortOrder != null;
        }
        public List<SortOrder> getSortOrder() {
            return sortOrder;
        }
        public void addSortOrder(SortOrder field) {    
            if (sortOrder == null) {
                sortOrder = new LinkedList<SortOrder>();
            }
            this.sortOrder.add(field);
        }
        public void removeSortOrder() {
            this.sortOrder = null;
        }
    }

    public boolean getCaseSensitiveDefault(String reponame) {
        return getMetadataService(reponame).getRepository().getCaseSensitiveDefault();
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

	public String getDal() {
		return dal;
	}

	public void setDal(String dal) {
		this.dal = dal;
	}

    public Map<String, List<SearchCriteria>> getAdditionalCriteria() {
        return additionalCriteria;
    }

    public void setAdditionalCriteria(Map<String, List<SearchCriteria>> additionalCriteria) {
        this.additionalCriteria.clear();
        if (additionalCriteria != null) {
            this.additionalCriteria.putAll(additionalCriteria);
        }
    }

    public CMSTrackingCodeEnum getRequestTrackingCode() {
        return requestTrackingCode;
    }
    public void setRequestTrackingCode(CMSTrackingCodeEnum requestTrackingCode) {
        this.requestTrackingCode = requestTrackingCode;
    }

}
