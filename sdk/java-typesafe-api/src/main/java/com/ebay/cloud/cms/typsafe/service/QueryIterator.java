/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.google.common.base.Preconditions;

/**
 * 
 * @author liasu
 * 
 */
public class QueryIterator<T extends ICMSEntity> implements Iterator<T> {
    private static final Logger logger = LoggerFactory.getLogger(QueryIterator.class);
    private CMSQuery query;
    private Integer nextHint;
    private long[] nextSkip;
    private long[] nextLimit;
    private int maxFetch;
    private List<String> sortOn;
    private List<String> sortOrder;
    private String cursorString;
    private boolean hasMore;
    private final Class<T> clz;
    private CMSClientService service;
    private CMSClientContext context;
    private LinkedList<T> entities;

    //
    // statistics information for the query iteration
    //
    // the total count of the entities that fetched from the server
    private long totalCount;
    // the number of the HTTP requests 
    private int requestNum;

    QueryIterator(Class<T> clz, CMSQuery query, CMSClientService service, CMSClientContext context) {
        if (query.isCountOnly()) {
            throw new CMSClientException("Please use the query instead of query iteration for count only query!");
        }
        this.clz = clz;
        this.query = new CMSQuery(query);
        this.service = service;
        this.context = context;
        this.entities = new LinkedList<T>();
        this.nextHint = query.getHint();
        this.nextLimit = query.getLimits();
        this.nextSkip = query.getSkips();
        this.cursorString = query.getCursor();
        this.hasMore = true;
        this.requestNum = 0;
        this.totalCount = 0l;
    }

    @Override
    public boolean hasNext() {
        if (entities.size() <= 0) {
            fetch();
        }

        if (entities.size() > 0) {
            return true;
        }
        return hasMore;
    }

    /**
     * Iterative get next entity.
     */
    @Override
    public T next() {
        if (hasNext() && entities.size() > 0) {
            return entities.remove();
        } else {
            return null;
        }
    }

    /**
     * Returns the entities in the given page(skip,limit).
     * 
     * NOTE: iterator is stateful, this page is started from the current
     * iterator cursor.
     * 
     * @param skip
     * @param limit
     *            - 0 means no limit, get all left.
     * @return
     */
    public List<T> getNextPage(int skip, int limit) {
        Preconditions.checkArgument(skip >= 0, "skip must be >=0!");
        Preconditions.checkArgument(limit >= 0, "limit must be >=0!");
        while (skip > 0 && hasNext()) {
            skip--;
            next();
        }
        if (skip > 0) {
            return Collections.emptyList();
        }
        List<T> result = new LinkedList<T>();
        if (limit == 0) {
            while (hasNext()) {
                result.add(next());
            }
        } else {
            while (limit > 0 && hasNext()) {
                result.add(next());
                limit--;
            }
        }
        return result;
    }

    /**
     * Returns all the <b>left</b> entities not fetched in this iterator.
     * 
     * NOTE: iterator is stateful, the returned result is started from the
     * current iterator cursor.
     * 
     * @return
     */
    public List<T> getRemaining() {
        List<T> result = new LinkedList<T>();
        while (hasNext()) {
            result.add(next());
        }
        return result;
    }

    private void fetch() {
        try {
            int fetchedSize = 0;
            while (hasMore && fetchedSize <= 0) {
                query.setHint(nextHint);
                query.setSkips(nextSkip);
                query.setLimits(nextLimit);
                query.setMaxFetch(maxFetch);
                query.setSortOn(sortOn);
                query.setSortOrder(sortOrder);
                query.setCursor(cursorString);
                requestNum++;
                logger.debug(String.format("query iteration: %d round, parameter: %s", requestNum, query.getQueryParams()));

                CMSQueryResult<T> result = service.query(query, clz, context);
                nextHint = result.getHint();
                nextSkip = result.getSkips();
                nextLimit = result.getLimits();
                cursorString = result.getCursor();
                sortOn = result.getSortOn();
                sortOrder = result.getSortOrder();
                maxFetch = result.getMaxFetch();
                hasMore = result.isHasMore();
                fetchedSize += result.getEntities().size();
                entities.addAll(result.getEntities());
                totalCount += fetchedSize;

                logger.debug(String.format(
                        "%s round query iteration, fetch entity number : %d, total fetched count : %d!", requestNum, fetchedSize, totalCount));
            }
        } catch (CMSClientException ce) {
            String msg = "query iteration error, got CMSClientException";
            logger.error(msg, ce);
            throw ce;
        } catch (Exception e) {
            String msg = "query iteration error, got exception";
            logger.error(msg, e);
            throw new CMSClientException(msg, e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove query entities not supported");
    }

    public long getTotalCount() {
        return totalCount;
    }

    public int getRequestNum() {
        return requestNum;
    }

    public int getNextHint() {
        return nextHint;
    }

    public long[] getNextSkip() {
        return nextSkip;
    }

    public String getCursorString() {
        return cursorString;
    }

    public long[] getNextLimit() {
        return nextLimit;
    }

    public boolean isHasMore() {
        return hasMore;
    }

}
