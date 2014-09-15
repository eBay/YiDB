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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.query.exception.QueryException;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.SortOrder;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.entity.visitor.ActiveAPIEntityVisitor;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.ExceptionMapper;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.service.resources.IQueryResource;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * query engine restful api
 * 
 * @author xjiang
 * 
 */
@Path("/repositories/{reponame}/branches/{branch}/query")
@Produces(MediaType.APPLICATION_JSON)
public class QueryResource implements IQueryResource {
    private static final Logger logger = LoggerFactory.getLogger(QueryResource.class);

    public enum QueryParameterEnum {
        explain, sortOn, sortOrder, limit, skip, hint, maxFetch, allowFullTableScan, cursor;
    }

    private CMSServer cmsServer = CMSServer.getCMSServer();

    @POST
    @Override
    public Response queryEntity(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") String cmsPriority,
            @HeaderParam("X-CMS-CONSISTENCY") String consistPolicy, @PathParam("reponame") String reponame,
            @PathParam("branch") String branch, String query, @Context UriInfo ui, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request) {
        return query(uriInfo, cmsPriority, consistPolicy, reponame, branch, query, ui, modeVal, request);
    }

    @GET
    @Path("/{query}")
    public Response query(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String cmsPriority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            final @PathParam("reponame") String reponame, final @PathParam("branch") String branch,
            final @PathParam("query") String query, final @Context UriInfo ui, @QueryParam("mode") String modeVal,
            final @Context HttpServletRequest request) {
        final CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
        final PaginationMode pageMode = CMSResourceUtils.parsePaginationMode(cmsServer, uriInfo.getQueryParameters().getFirst(CMSResourceUtils.REQ_PAGINATION_MODE));
        final QueryContext context = createContext(cmsServer, uriInfo, reponame, branch, mode, pageMode, consistPolicy, request);
        
        Callable<Response> run = new Callable<Response>() {
            
            @Override
            public Response call() {
                parseQueryParameters(ui.getQueryParameters(), context);
                CMSPriority priority = CMSResourceUtils.parsePriority(cmsPriority);

                IQueryResult queryResult = cmsServer.query(priority, query, context);
                return buildResponse(reponame, branch, mode, queryResult, context, request);
            }
        };
        
        return execute(reponame, query, run);
    }

    private <T> T execute(String reponame, String query, Callable<T> call) {
        try {
            return call.call();
        } catch (CannotServeException e) {
            logger.error("Error when exectue query {}", e, query);
            throw new ServiceUnavailableException(e.getMessage());
        } catch (RepositoryNotExistsException e) {
            logger.error("Error when exectue query {}", e, query);
            throw new NotFoundException("repository not found: " + reponame);
        } catch (QueryException e) {
            logger.error("Error when exectue query {}", e, query);
            throw new CMSServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (CmsDalException e) {
            logger.error("Error when exectue query {}", e, query);
            throw ExceptionMapper.convert(e);
        } catch (CmsEntMgrException e) {
            logger.error("Error when exectue query {}", e, query);
            throw ExceptionMapper.convert(e);
        } catch (WebApplicationException e) {
            logger.error("Error when exectue query {}", e, query);
            throw e;
        } catch (Throwable t) {
            logger.error("Error when exectue query {}", t, query);
            throw new CMSServerException(t);
        }
    }
    
    public Map<String, MetaClass> getQueryMetaClass(final @Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String cmsPriority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            final @PathParam("reponame") String reponame, final @PathParam("branch") String branch,
            final @PathParam("query") String query, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request) {
        final CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
        final PaginationMode pageMode = CMSResourceUtils.parsePaginationMode(cmsServer, uriInfo.getQueryParameters().getFirst(CMSResourceUtils.REQ_PAGINATION_MODE));
        final QueryContext context = createContext(cmsServer, uriInfo, reponame, branch, mode, pageMode, consistPolicy, request);

        Callable<Map<String, MetaClass>> call = new Callable<Map<String, MetaClass>>() {

            @Override
            public Map<String, MetaClass> call() throws Exception {
                CMSPriority priority = CMSResourceUtils.parsePriority(cmsPriority);
                return cmsServer.getQueryMetaClass(priority, query, context);
            }
        };
        return execute(reponame, query, call);
    }

    public static List<Object> getEntities(String reponame, String branch, CMSQueryMode mode, IQueryResult queryResult) {
        List<Object> entityNodes = new ArrayList<Object>();
        for (IEntity entity : queryResult.getEntities()) {
            JsonEntity jsonEntity = (JsonEntity) entity;
            JsonNode jsonNode = jsonEntity.getNode();
            if (mode == CMSQueryMode.URI) {
                ActiveAPIEntityVisitor visitor = new ActiveAPIEntityVisitor(reponame, branch, jsonEntity.getMetaClass());
                jsonEntity.traverse(visitor);
                jsonNode = (JsonNode) visitor.getBuildEntity().getNode();
            }

            entityNodes.add(jsonNode);
        }
        return entityNodes;
    }

    public static void parseQueryParameters(MultivaluedMap<String, String> params, QueryContext context) {
        String paramName = "";
        String paramVal = "";
        try {
            for (Map.Entry<String, List<String>> paramEntry : params.entrySet()) {
                CheckConditions.checkNotNull(paramEntry.getKey());

                paramName = paramEntry.getKey();
                CheckConditions.checkArgument(!paramEntry.getValue().isEmpty(), "%s has empty value", paramName);
                paramVal = paramEntry.getValue().get(0);
                QueryParameterEnum paraEnum = null;
                try {
                    paraEnum = QueryParameterEnum.valueOf(paramName);
                } catch (Exception e) {
                    continue;// ignore the non-query parameter
                }
                switch (paraEnum) {
                    case explain:
                        context.setExplain(Boolean.valueOf(paramVal));
                        break;
                    case sortOn:
                        for (String f : paramVal.split(",")) {
                            context.addSortOn(f.trim());
                        }
                        break;
                    case sortOrder:
                        for (String f : paramVal.split(",")) {
                            context.addSortOrder(SortOrder.valueOf(f.trim()));
                        }
                        break;
                    case limit:
                        int[] limits = extractIntArray(paramVal, Integer.MAX_VALUE);
                        context.setLimits(limits);
                        break;
                    case skip:
                        int[] skips = extractIntArray(paramVal, 0);
                        context.setSkips(skips);
                        break;
                    case maxFetch :
                        context.setMaxFetch(Integer.valueOf(paramVal));
                        break;
                    case hint:
                        context.setHint(Integer.valueOf(paramVal));
                        break;
                    case allowFullTableScan:
                        context.setAllowFullTableScan(Boolean.valueOf(paramVal));
                        break;
                    case cursor:
                        String param= extractCursorValue(paramVal);
                        context.setCursorString(param);
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable t) {
            throw new BadParamException(t, "Bad Parameter (" + paramName + " : " + paramVal + ")");
        }

    }

    private static int[] extractIntArray(String paramVal, int defaultValue) {
        List<Integer> intLists = null;
        if (paramVal.trim().startsWith("[")) {
            try {
                JsonNode node = ObjectConverter.mapper.readTree(paramVal);
                intLists = new ArrayList<Integer>(10);
                if (node.isInt()) {
                    intLists.add(node.getIntValue());
                } else if (node.isArray()) {
                    for (JsonNode skipNode : node) {
                        intLists.add(skipNode.isInt() ? skipNode.getIntValue() : defaultValue);
                    }
                }
            } catch (Exception e) {
                logger.info("read skip array as json node failed, will try to split by , !", e);
            }
        }
        int[] skips = null;
        if (intLists == null) {
            String[] intStringValues = org.apache.commons.lang3.StringUtils.splitPreserveAllTokens(paramVal, ",");
            skips = new int[intStringValues.length];
            for (int i = 0; i < intStringValues.length; i++) {
                skips[i] = NumberUtils.toInt(intStringValues[i].trim(), defaultValue);
            }
        } else {
            skips = ArrayUtils.toPrimitive(intLists.toArray(new Integer[] { 0 }));
        }
        return skips;
    }

    private static String extractCursorValue(String paramVal) {
        List<String> cursorList = null;
        final String cursorValue = paramVal.trim();
        if (cursorValue.startsWith("{")) {
            // single node cursor MUST start with {
            return cursorValue;
        } else {
            cursorList = new ArrayList<String>(10);
            if (!cursorValue.startsWith("[")) {
                // better user experience: when user copy the oid's with quotation, make sure parameter are handled correctly 
                String[] joinOids = cursorValue.split(",");
                for (String oid : joinOids) {
                    // assumption : the oid doesn't contain the character of "
                    cursorList.add(StringUtils.trim(oid.replace('"', ' ')));
                }
            } else {
                // expect to be an valid json string array
                try {
                    JsonNode node = ObjectConverter.mapper.readTree(cursorValue);
                    cursorList = new ArrayList<String>(10);
                    for (JsonNode cursorNode : node) {
                        if (cursorNode.isObject()) {
                            // when find the object node, expected to be single cursor
                            return cursorNode.toString();
                        } else {
                            cursorList.add(cursorNode.getTextValue());
                        }
                    }
                } catch (Exception e) {
                    logger.info("read cursor value as json node failed, will try to split by , !", e);
                }
            }
        }
        return org.apache.commons.lang3.StringUtils.join(cursorList, ",");
    }

    @SuppressWarnings("unchecked")
    public static QueryContext createContext(CMSServer server, UriInfo uriInfo, String reponame, String branch, CMSQueryMode mode,
            PaginationMode pageMode, String consistPolicy, HttpServletRequest request) {
        String sourceIp = (String)request.getAttribute(CMSResourceUtils.X_CMS_CLIENT_IP);
        if (sourceIp == null) {
            sourceIp = request.getRemoteAddr();
        }
        MultivaluedMap<String, String> mmap = uriInfo.getQueryParameters();
        QueryContext context = new QueryContext(reponame, branch);
        context.setSubject(mmap.getFirst(CMSResourceUtils.REQ_PARAM_COMPONENT));
        context.setUserId(mmap.getFirst(CMSResourceUtils.REQ_PARAM_UID));
        context.setShowDisplayMeta(Boolean.valueOf(mmap.getFirst(CMSResourceUtils.REQ_SHOW_META)));
        if (StringUtils.isEmpty(context.getUserId())) {
            context.setUserId(sourceIp);
        }
        context.setSourceIP(sourceIp);
        context.setCountOnly(mode == CMSQueryMode.COUNT);
        context.setPaginationMode(pageMode);
        String dal = mmap.getFirst(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION);
        context.setRegistration(server.getDalImplementation(dal));
        context.setConsistentPolicy(CMSResourceUtils.parsePolicy(server, consistPolicy));
        Map<String, List<SearchCriteria>> addiotionalCriteria = (Map<String, List<SearchCriteria>>) request.getAttribute(CMSResourceUtils.REQ_READ_FILTER);
        context.setAdditionalCriteria(addiotionalCriteria);
        return context;
    }

    private Response buildResponse(String reponame, String branch, CMSQueryMode mode, IQueryResult queryResult,
            QueryContext context, HttpServletRequest request) {
        CMSResponse response = new CMSResponse();
        response.addProperty("dbTimeCost", context.getDbTimeCost());
        response.addProperty("totalTimeCost", context.getTotalTimeCost());
        response.addProperty("hasmore", queryResult.hasMoreResults());
        response.addProperty("count", getCount(queryResult));

        if (context.isCountOnly()) {
            response.addProperty("count", queryResult.getCount());
        } else {
            response.addResult(getEntities(reponame, branch, mode, queryResult));
        }
        if (queryResult.getExplanations() != null && !queryResult.getExplanations().isEmpty()) {
            response.addProperty("explanation", getExplanation(queryResult));
        }
        if (queryResult.hasMoreResults()) {
            response.addProperty("pagination", CMSResourceUtils.getPagination(queryResult, context, request));
            // CMS-4620 : add next url for next page
            response.addProperty("next", CMSResourceUtils.getNext(queryResult, context, request));
        }
        if (context.isShowDisplayMeta()) {
            response.addProperty("display", queryResult.getDisplayMeta());
        }
        ResponseBuilder builder = Response.ok(response, MediaType.APPLICATION_JSON);
        // request tracking code
        if (context.getRequestTrackingCode() != null) {
            builder.header(CMSConsts.TRACKING_CODE_KEY, context.getRequestTrackingCode().getErrorCode());
        }
        return builder.build();
    }

    private long getCount(IQueryResult queryResult) {
        return queryResult.getEntities().size();
    }

    private JsonNode getExplanation(IQueryResult queryResult) {
        List<IQueryExplanation> explanations = queryResult.getExplanations();
        if (explanations == null) {
            return null;
        }
        JsonNode explanationNode = null;
        if (explanations.size() == 1) {
            explanationNode = explanations.get(0).getJsonExplanation();
        } else {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (IQueryExplanation e : explanations) {
                array.add(e.getJsonExplanation());
            }
            explanationNode = array;
        }
        return explanationNode;
    }

}
