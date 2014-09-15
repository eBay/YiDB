/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery.QueryParameter;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

/**
 * 
 * @author liasu
 * 
 */
public class QueryResponseProcessor<T extends ICMSEntity> extends AbstractResponseProcessor {

    private static final String     HAS_MORE     = "hasmore";
    private static final String     PAGINATION   = "pagination";
    private static final String     SKIP         = "skip";
    private static final String     LIMIT        = "limit";
    private static final String     HINT         = "hint";
    private static final String     COUNT        = "count";
    private static final String     CURSOR       = "cursor";

    private final ObjectMapper      objectMapper = Constants.objectMapper;

    private final Class<T>          targetClass;
    private final JsonNode          rootNode;
    private final CMSClientConfig   config;

    private final CMSQueryResult<T> queryResult;

    public  QueryResponseProcessor(CMSClientConfig config, ClientResponse resp, Class<T> targetClass, CMSClientContext context) {
        super(resp, HttpRequest.GET, context);
        this.config = config;
        this.targetClass = targetClass;
        this.queryResult = new CMSQueryResult<T>();

        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new CMSClientException(resp.getStatus(), MessageFormat.format(
                    "parse response string error for query, the response string is {0}", jsonResponse), jsonResponse, requestInfo);
        }

        parseResponseHeader();
    }

    @Override
    protected void parseResponseHeader() {
        super.parseResponseHeader();
        JsonNode countNode = getRootNode().get(COUNT);
        if (countNode != null) {
            queryResult.setCount(countNode.getLongValue());
        }
        JsonNode dbTimeNode = getRootNode().get("dbTimeCost");
        if (dbTimeNode != null) {
            queryResult.setDbTimeCost(dbTimeNode.getIntValue());
        }
        JsonNode totalTimeNode = getRootNode().get("totalTimeCost");
        if (totalTimeNode != null) {
            queryResult.setTotalTimeCost(totalTimeNode.getIntValue());
        }
        // pagination related result parsing
        JsonNode hasMoreNode = getRootNode().get(HAS_MORE);
        queryResult.setHasMore(hasMoreNode.getBooleanValue());
        JsonNode pagination = getRootNode().get(PAGINATION);
        if (pagination != null) {
            // limit
            long[] array = extractLongArray(pagination, LIMIT);
            queryResult.setLimits(array);
            // skip
            array = extractLongArray(pagination, SKIP);
            queryResult.setSkips(array);
            // hint
            JsonNode hintNode = pagination.get(HINT);
            if (hintNode != null) {
                queryResult.setHint(hintNode.getIntValue());
            }
            // cursor
            JsonNode cursorNode = pagination.get(CURSOR);
            if (cursorNode != null) {
                extractCursorArray((ArrayNode)cursorNode);
            }
            // maxFetch, sortOn, sortOrder
            JsonNode maxFetchNode = pagination.get(QueryParameter.maxFetch.name());
            if (maxFetchNode != null) {
                queryResult.setMaxFetch(maxFetchNode.getIntValue());
            }
            JsonNode sortOnNode = pagination.get(QueryParameter.sortOn.name());
            if (sortOnNode != null) {
                queryResult.setSortOn(extractStringArray((ArrayNode)sortOnNode));
            }
            JsonNode sortOrderNode = pagination.get(QueryParameter.sortOrder.name());
            if (sortOrderNode != null) {
                queryResult.setSortOrder(extractStringArray((ArrayNode)sortOrderNode));
            }
        }
    }

    private List<String> extractStringArray(ArrayNode sortNode) {
        List<String> result = new ArrayList<String>();
        for (JsonNode node : sortNode) {
            result.add(node.getTextValue());
        }
        return result;
    }

    private void extractCursorArray(ArrayNode cursorNode) {
        if (cursorNode.size() > 0) {
            List<String> cursors = new ArrayList<String>();
            for (JsonNode cNode : cursorNode) {
                if (cNode instanceof ObjectNode) {
                    cursors.add(cNode.toString());
                } else {
                    cursors.add(cNode.getTextValue());
                }
            }
            queryResult.setCursor(StringUtils.join(cursors, ','));
        }
    }

    private long[] extractLongArray(JsonNode pagination, String name) {
        ArrayNode skipNode = (ArrayNode) pagination.get(name);
        if (skipNode != null) {
            List<Long> skips = new ArrayList<Long>(skipNode.size());
            Iterator<JsonNode> it = skipNode.iterator();
            while (it.hasNext()) {
                JsonNode node = it.next();
                skips.add(node.getLongValue());
            }
            return ArrayUtils.toPrimitive(skips.toArray(new Long[0]));
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CMSQueryResult<T> build() {
        EntityResponseProcessor rb = new EntityResponseProcessor(targetClass, response, jsonResponse, rootNode, config,
                HttpRequest.GET, context);
        List<T> result = (List<T>) rb.getBuildEntity();
        queryResult.addResults(result);
        return queryResult;
    }

    @Override
    protected JsonNode getRootNode() {
        return rootNode;
    }

}
