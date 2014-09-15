/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author liasu
 * 
 */
public class CMSQueryResult<T extends ICMSEntity> {
    private final List<T> entities = new ArrayList<T>();
    /**
     * for mode=count query
     */
    private long count;
    private boolean hasMore;
    private long[] skips;
    private String cursor;
    private long[] limits;
    private int hint;
    private int maxFetch;
    private List<String> sortOn;
    private List<String> sortOrder;

    private long dbTimeCost;
    private long totalTimeCost;

    public final void addResult(T result) {
        this.entities.add(result);
    }

    public final void addResults(Collection<T> result) {
        this.entities.addAll(result);
    }

    public final List<T> getEntities() {
        return entities;
    }

    public final Long getCount() {
        return count;
    }

    public final void setCount(Long count) {
        this.count = count;
    }

    public final boolean isHasMore() {
        return hasMore;
    }

    public final void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public long[] getSkips() {
        return skips;
    }

    public void setSkips(long[] skips) {
        this.skips = skips;
    }

    public long[] getLimits() {
        return limits;
    }

    public void setLimits(long[] limits) {
        this.limits = limits;
    }

    public int getHint() {
        return hint;
    }

    public void setHint(int hint) {
        this.hint = hint;
    }

    public long getDbTimeCost() {
        return dbTimeCost;
    }

    public void setDbTimeCost(long dbTimeCost) {
        this.dbTimeCost = dbTimeCost;
    }

    public int getMaxFetch() {
        return maxFetch;
    }

    public void setMaxFetch(int maxFetch) {
        this.maxFetch = maxFetch;
    }

    public List<String> getSortOn() {
        return sortOn;
    }

    public void setSortOn(List<String> sortOn) {
        this.sortOn = ImmutableList.copyOf(sortOn);
    }

    public List<String> getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(List<String> sortOrder) {
        this.sortOrder = ImmutableList.copyOf(sortOrder);
    }

    public long getTotalTimeCost() {
        return totalTimeCost;
    }

    public void setTotalTimeCost(long totalTimeCost) {
        this.totalTimeCost = totalTimeCost;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

}
