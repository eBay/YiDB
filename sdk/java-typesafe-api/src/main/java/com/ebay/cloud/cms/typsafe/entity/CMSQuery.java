/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.google.common.base.Preconditions;

/**
 * @author liasu
 * 
 */
public final class CMSQuery {

    private static final String COUNT_ONLY = "COUNT";
    private static final Logger logger = LoggerFactory.getLogger(CMSQuery.class);

    /**
     * Enumeration of the query parameters
     * <ul>
     * <li>sortOn=field names</li>
     * <li>sortOrder=[asc|desc]</li>
     * <li>limit= list of non-negative integer value</li>
     * <li>skip = list of non-negative integer value. Only used when paginationMode=PaginationEnum.SKIP_LIMIT or not set</li>
     * <li>hint = non-negative integer value</li>
     * <li>allowFullTableScan = [true|false]</li>
     * <li>mode = [count]</li>
     * <li>maxFetch=A number of the max fetch size when query database. This size ONLY affects the SUB QUERY now.</li>
     * <li>paginationMode = PaginationEnum.BY_ID | PaginationEnum.SKIP_LIMIT </li>
     * <li>cursor={}   or _oid, _oid, _oid. Only used when PaginationEnum.BY_ID</li>
     * </ul>
     * 
     * @author liasu
     * 
     */
    public enum QueryParameter {
        sortOn, sortOrder, limit, skip, hint, allowFullTableScan, maxFetch, mode, paginationMode, cursor
    }
    
    public static enum PaginationEnum {
        ID_BASED, SKIP_BASED
    }

    /**
     * The max of the limit that user could set. CMSServer doesn't allow limit more than 1000 in one single query
     */
    public static final int LIMIT_MAX = 1000;

    public enum SortOrder {
        asc, desc
    }

    private String                    queryString;
    private final Map<String, String> queryParams;
    private String                    repository;
    private String                    branch;

    /**
     * @seeAlso CMSQuery(String queryString)
     * 
     * @param repo
     * @param branch
     * @param queryString
     *            - The query string. NOT including the query parameter in rest
     *            call, all the query parameters appended in the query parameter would be ignored.
     */
    public CMSQuery(String repo, String branch, String queryString) {
        this(queryString);
        this.repository = repo;
        this.branch = branch;
    }

    /**
     * 
     * @param queryString
     *            - The query string. NOT including the query parameter in rest
     *            call, all the query parameters appended in the query string
     *            would be ignored. It use a simply logic to detect the ? in the query string. This might not be accurate,
     *            if one don't want any parameter handling, try use <code></code>
     */
    public CMSQuery(String queryString) {
        this.queryString = getQueryString(queryString);
        this.queryParams = new HashMap<String, String>();
    }

    /**
     * 
     * @param queryString
     * @param removeParameter
     */
    public CMSQuery(String queryString, boolean removeParameter) {
        if (removeParameter) {
            this.queryString = getQueryString(queryString);
        } else {
            this.queryString = queryString;
        }
        this.queryParams = new HashMap<String, String>();
    }

    private String getQueryString(String queryString) {
        if (queryString == null) {
            return null;
        }
        int index = findQuestionMarkIndex(queryString);
        return queryString.substring(0, index);
    }

    /**
     * Return the index of the first question mark in un-quotated context
     */
    static int findQuestionMarkIndex(String query) {
        boolean inQuotation = false;
        int questMarkIndex = -1;
        for (int i = 0; i < query.length(); i++) {
            char charAt = query.charAt(i);
            if ('"' == charAt) {
                inQuotation = !inQuotation;
            } else if (!inQuotation && '?' == charAt) {
                questMarkIndex = i;
                break;
            }
        }
        if (questMarkIndex < 0) {
            questMarkIndex = query.length();
        }
        return questMarkIndex;
    }

    public CMSQuery(CMSQuery query) {
        this.branch = query.getBranch();
        this.repository = query.getRepository();
        this.queryString = query.queryString;
        this.queryParams = new HashMap<String, String>();
        this.queryParams.putAll(query.queryParams);
    }

    public String getQueryString() {
        return queryString;
    }

    public void addSortOn(String sortOn) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(sortOn), "SortOn could not be empty!");
        String preSortOn = queryParams.get(QueryParameter.sortOn.toString());
        if (StringUtils.isEmpty(preSortOn)) {
            queryParams.put(QueryParameter.sortOn.toString(), sortOn);
        } else {
            StringBuilder sb = new StringBuilder(preSortOn);
            sb.append(",").append(sortOn);
            queryParams.put(QueryParameter.sortOn.toString(), sb.toString());
        }
    }

    public void setSortOn(List<String> sortOns) {
        queryParams.remove(QueryParameter.sortOn.toString());
        if (sortOns != null) {
            queryParams.put(QueryParameter.sortOn.name(), StringUtils.join(sortOns.toArray(), ','));
        }
    }

    public void setSortOrder(List<String> sortOrders) {
        queryParams.remove(QueryParameter.sortOrder.toString());
        if (sortOrders != null) {
            queryParams.put(QueryParameter.sortOrder.name(), StringUtils.join(sortOrders.toArray(), ','));
        }
    }

    public void addSortOrder(SortOrder sortOrder) {
        Preconditions.checkArgument(sortOrder != null, "SortOn could not be empty!");
        String preSortOn = queryParams.get(QueryParameter.sortOrder.toString());
        if (StringUtils.isEmpty(preSortOn)) {
            queryParams.put(QueryParameter.sortOrder.toString(), sortOrder.toString());
        } else {
            StringBuilder sb = new StringBuilder(preSortOn);
            sb.append(",").append(sortOrder.toString());
            queryParams.put(QueryParameter.sortOrder.toString(), sb.toString());
        }
    }

    /**
     * @seeAlso setLimits()
     * 
     * @param limit
     */
    @Deprecated
    public void setLimit(int limit) {
        setLimits(new long[] { limit });
    }

    /**
     * Should 
     * @seeAlso getLimits()
     * 
     * @return
     */
    @Deprecated
    public Integer getLimit() {
        long[] limits = getLimits();
        if (ArrayUtils.isNotEmpty(limits)) {
            return (int) limits[0];
        }
        return null;
    }

    public Integer getMaxFetch() {
        if (!queryParams.containsKey(QueryParameter.maxFetch.toString())) {
            return null;
        }
        return Integer.valueOf(queryParams.get(QueryParameter.maxFetch.toString()));
    }

    public void setMaxFetch(int maxFetch) {
        Preconditions.checkArgument(maxFetch >= 0, "maxFetch must be equal or greater than 0!");
        queryParams.put(QueryParameter.maxFetch.toString(), Integer.toString(maxFetch));
    }

    /**
     * @seeAlso setSkips()
     * 
     * @param skip
     */
    @Deprecated
    public void setSkip(int skip) {
        setSkips(new long[] {skip});
    }

    /**
     * @seeAlso getSkips().
     * 
     * @return
     */
    @Deprecated
    public Integer getSkip() {
        long[] skips = getSkips();
        if (skips != null) {
            return (int)skips[0];
        }
        return null;
    }
    
    public void setSkips(long[] skips) {
        if (skips == null || skips.length == 0) {
            queryParams.remove(QueryParameter.skip.toString());
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (long i : skips) {
            sb.append(i).append(",");
        }
        String skipStr = sb.substring(0, sb.length() - 1);
        if (!skipStr.isEmpty()) {
            queryParams.put(QueryParameter.skip.toString(), skipStr);
        }
    }

    public long[] getSkips() {
        if (queryParams.containsKey(QueryParameter.skip.toString())) {
            String skipStr = queryParams.get(QueryParameter.skip.toString());
            String[] skipStrs = StringUtils.split(skipStr, ',');
            long[] skips = new long[skipStrs.length];
            for (int i = 0; i < skipStrs.length; i++) {
                try {
                    skips[i] = Long.parseLong(skipStrs[i]);
                } catch (Exception e) {
                    logger.error("skip not valid integer, set to 0!");
                    skips[i] = 0;
                }
            }
            return skips;
        }
        return null;
    }

    public void setLimits(long[] limits) {
        if (limits == null || limits.length == 0) {
            queryParams.remove(QueryParameter.limit.toString());
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (long i : limits) {
            sb.append(i).append(",");
        }
        String limitStr = sb.substring(0, sb.length() - 1);
        if (!limitStr.isEmpty()) {
            queryParams.put(QueryParameter.limit.toString(), limitStr);
        }
    }

    public long[] getLimits() {
        if (queryParams.containsKey(QueryParameter.limit.toString())) {
            String limitStr = queryParams.get(QueryParameter.limit.toString());
            String[] limitStrs = StringUtils.split(limitStr, ',');
            long[] limits = new long[limitStrs.length];
            for (int i= 0; i < limitStrs.length; i++) {
                try {
                    limits[i] = Long.parseLong(limitStrs[i]);
                } catch (Exception e) {
                    logger.error("limit not valid integer, set to 0!");
                    limits[i] = 0;
                }
            }
            return limits;
        }
        return null;
    }

    public void setHint(Integer hint) {
        if (hint == null) {
            queryParams.remove(QueryParameter.hint.toString());
            return;
        }
        queryParams.put(QueryParameter.hint.toString(), Integer.toString(hint));
    }

    public Integer getHint() {
        if (!queryParams.containsKey(QueryParameter.hint.toString())) {
            return null;
        }
        return Integer.valueOf(queryParams.get(QueryParameter.hint.toString()));
    }

    public boolean hasHint() {
        return StringUtils.isNotEmpty(queryParams.get(QueryParameter.hint.toString()));
    }

    public void setAllowFullTableScan(boolean allowFullTableScan) {
        queryParams.put(QueryParameter.allowFullTableScan.toString(), Boolean.toString(allowFullTableScan));
    }

    public void setCountOnly(boolean countOnly) {
        if (countOnly) {
            queryParams.put(QueryParameter.mode.toString(), COUNT_ONLY);
        } else {
            queryParams.put(QueryParameter.mode.toString(), null);
        }
    }

    public boolean isCountOnly() {
        return COUNT_ONLY.equalsIgnoreCase(queryParams.get(QueryParameter.mode.toString()));
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getRepository() {
        return repository;
    }

    public String getBranch() {
        return branch;
    }

    public void setQueryString(String queryString) {
        this.queryString = getQueryString(queryString);
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    public PaginationEnum getPaginatonMode() {
        if (this.queryParams.containsKey(QueryParameter.paginationMode.name())) {
            return PaginationEnum.valueOf(this.queryParams.get(QueryParameter.paginationMode.name()));
        } else {
            return null;
        }
    }
    public void setPaginationMode(PaginationEnum mode) {
        if (mode == null ){
            this.queryParams.remove(QueryParameter.paginationMode.name());
        } else {
            this.queryParams.put(QueryParameter.paginationMode.name(), mode.name());
        }
    }

    public String getCursor() {
        return this.queryParams.get(QueryParameter.cursor.name());
    }
    public void setCursor(String cursorString) {
        if (cursorString == null) {
            this.queryParams.remove(QueryParameter.cursor.name());
        } else {
            this.queryParams.put(QueryParameter.cursor.name(), CMSClientContext.encodeString(cursorString));
        }
    }

}
