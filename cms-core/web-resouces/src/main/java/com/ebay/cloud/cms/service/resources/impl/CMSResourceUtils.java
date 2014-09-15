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

package com.ebay.cloud.cms.service.resources.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.QueryCursor;
import com.ebay.cloud.cms.query.service.QueryContext.SortOrder;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.resources.impl.QueryResource.QueryParameterEnum;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public class CMSResourceUtils {

    public static CMSPriority parsePriority(String p) {
        if (p == null) {
            return CMSPriority.NEUTRAL;
        }
        try {
            return CMSPriority.valueOf(p);
        }
        catch (IllegalArgumentException e) {
            throw new BadParamException("unknow priority: " + p);
        }
    }

    public static ConsistentPolicy parsePolicy(CMSServer cmsServer, String policy) {
        ConsistentPolicy cp = cmsServer.parsePolicy(policy);
        if (cp == null) {
            throw new BadParamException(MessageFormat.format("Consistent policy {0} is not an valid value of {1}!",
                    policy, Arrays.toString(ConsistentPolicy.values())));
        }
        return cp;
    }
    
    public static PaginationMode parsePaginationMode(CMSServer server, String mode) {
        PaginationMode pageMode = server.parsePaginationMode(mode);
        if (pageMode == null) {
            throw new BadParamException(MessageFormat.format("Pagination mode {0} is not an valid value!", mode));
        }
        return pageMode;
    }

    public static final String REQ_PARAM_UID         = "uid";
    public static final String REQ_PARAM_COMPONENT   = "X-SECURITY-USER";
    public static final String REQ_PARAM_COMMENT     = "comment";
//    public static final String REQ_PARAM_FORCEUPDATE = "forceUpdate";
	public static final String X_CMS_CLIENT_IP       = "X-CMS-CLIENT-IP";
    public static final String REQ_PARAM_VERSION     = "version";
    public static final String REQ_PARAM_PATH        = "path";
    public static final String REQ_SHOW_META         = "showMeta";
    public static final String REQ_PARAM_DAL_IMPLEMENTATION = "dal";
    public static final String REQ_PAGINATION_MODE   = "paginationMode";
    public static final String REQ_READ_FILTER       = "X-CMS-READ-FILTER";
    public static final String REQ_REFRESH_CACHE     = "refreshCache";
    
    private static String urlEncoding(String url) {
        try {
            return URLEncoder.encode(url, "utf8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new CMSServerException(CMSServerException.UNKNOWN_ERROR, "error build request url.", e);
        }
    }
    
    private static String joinToParameter(int[] values) {
        StringBuilder sb = new StringBuilder();
        for (Object o : values) {
            sb.append(o.toString()).append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
    
    private static String joinSortOrder(List<SortOrder> sortOrder) {
        StringBuilder sb = new StringBuilder();
        for (SortOrder o : sortOrder) {
            sb.append(o.toString()).append(',');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
    
    public static Object getPagination(IQueryResult queryResult, QueryContext context, HttpServletRequest request) {
        ObjectNode pagination = JsonNodeFactory.instance.objectNode();
        QueryCursor nextCursor = queryResult.getNextCursor();
        if (context.getPaginationMode() == PaginationMode.SKIP_BASED) {
            ArrayNode skipNode = pagination.arrayNode();
            for (int i : nextCursor.getSkips()) {
                skipNode.add(i);
            }
            pagination.put(QueryParameterEnum.skip.name(), skipNode);
        } else {
            ArrayNode cursorNode = pagination.arrayNode();
            if (nextCursor.isJoinCursor()) {
                for (String oid : nextCursor.getJoinCursorValues()) {
                    cursorNode.add(oid);
                }
            } else if (nextCursor.getSingleCursorValue() != null) {
                cursorNode.add(((JsonEntity) nextCursor.getSingleCursorValue()).getNode());
            }
            if (cursorNode.size() > 0) {
                pagination.put(QueryParameterEnum.cursor.name(), cursorNode);
            }
        }
        ArrayNode limitNode = pagination.arrayNode();
        for (int i : nextCursor.getLimits()) {
            limitNode.add(i);
        }
        pagination.put(QueryParameterEnum.limit.name(), limitNode);
        pagination.put(QueryParameterEnum.hint.name(), nextCursor.getHint());
        if (nextCursor.hasSortOn()) {
            pagination.put(QueryParameterEnum.sortOn.name(), listToArrayNode(nextCursor.getSortOn()));
        }
        if (nextCursor.hasSortOrder()) {
            pagination.put(QueryParameterEnum.sortOrder.name(), listToArrayNode(nextCursor.getSortOrder()));
        }
        pagination.put(QueryParameterEnum.maxFetch.name(), nextCursor.getMaxFecth());
        return pagination;
    }
    
    public static Object getNext(IQueryResult queryResult, QueryContext context, HttpServletRequest request) {
        String method = request.getMethod();
        ObjectNode next = JsonNodeFactory.instance.objectNode();
        // build url
        String url = "/repositories/%s/branches/%s/query/%s";
        next.put("method", method);
        if ("POST".equals(method)) {
            next.put("body", context.getQueryString());
            url = String.format(url, context.getRepositoryName(), context.getBranchName(), "");
        } else {
            url = String.format(url, context.getRepositoryName(), context.getBranchName(), CMSResourceUtils.urlEncoding(context.getQueryString()));
        }
        QueryCursor nextCursor = queryResult.getNextCursor();
        // append query parameters
        StringBuilder sb = new StringBuilder(url);
        // from context
        sb.append("?").append(QueryParameterEnum.allowFullTableScan.name()).append('=').append(context.isAllowFullTableScan());
        sb.append("&").append(QueryParameterEnum.explain.name()).append('=').append(context.needExplain());
        if (context.getDal() != null) {
            sb.append("&").append(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION).append('=').append(context.getDal());
        }
        if (context.getPaginationMode() != null) {
            sb.append("&").append(CMSResourceUtils.REQ_PAGINATION_MODE).append('=').append(context.getPaginationMode());
        }
        sb.append("&").append(CMSResourceUtils.REQ_SHOW_META).append('=').append(context.isShowDisplayMeta());
        // no count only
        // from cursor
        if (nextCursor.getLimits() != null) {
            sb.append("&").append(QueryParameterEnum.limit).append('=').append(CMSResourceUtils.joinToParameter(nextCursor.getLimits()));
        }
        if (nextCursor.getSkips() != null) {
            sb.append("&").append(QueryParameterEnum.skip).append('=').append(CMSResourceUtils.joinToParameter(nextCursor.getSkips()));
        }
        if (context.getPaginationMode() == PaginationMode.ID_BASED) {
            if (nextCursor.isJoinCursor()) {
                sb.append('&').append(QueryParameterEnum.cursor).append('=')
                        .append(CMSResourceUtils.urlEncoding(listToArrayNode(nextCursor.getJoinCursorValues()).toString()));
            } else {
                sb.append('&').append(QueryParameterEnum.cursor).append('=').append(CMSResourceUtils.urlEncoding(nextCursor.getSingleCursorValue().toString()));
            }
        }
        if (nextCursor.hasSortOn()) {
            sb.append("&").append(QueryParameterEnum.sortOn.name()).append('=').append(StringUtils.join(nextCursor.getSortOn(), ","));
        }
        if (nextCursor.hasSortOrder()) {
            sb.append("&").append(QueryParameterEnum.sortOrder.name()).append('=').append(CMSResourceUtils.joinSortOrder(nextCursor.getSortOrder()));
        }
        if (nextCursor.hasHint()) {
            sb.append("&").append(QueryParameterEnum.hint.name()).append('=').append(nextCursor.getHint());
        }
        if (nextCursor.hasMaxFetch()) {
            sb.append("&").append(QueryParameterEnum.maxFetch.name()).append('=').append(nextCursor.getMaxFecth());
        }
        next.put("url", sb.toString());
        return next;
    }
    
    private static JsonNode listToArrayNode(List<? extends Object> sortOn) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (Object o : sortOn) {
            if (o instanceof Integer) {
                array.add((Integer) o);
            } else {
                array.add(o.toString());
            }
        }
        return array;
    }
    
}
