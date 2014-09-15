/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

/**
 * Query executor that send out the service, assemble the result, and do
 * additional accumulate work based on user request and server response.
 * 
 * @author liasu
 * 
 */
public class QueryRestExecutor<T extends ICMSEntity> {

    private final String              fullUrl;
    private final CMSQuery            query;
    private final Client              restClient;
    private final HttpRequest         request;
    private final CMSClientConfig     config;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final Class<T>            targetClass;
    private final CMSClientContext    context;

    public QueryRestExecutor(CMSClientConfig config, Client restClient, CMSQuery query,
            String url, HttpRequest request, Map<String, String> queryParams, Map<String, String> headers, Class<T> targetClass, CMSClientContext context) {
        this.config = config;
        this.restClient = restClient;
        this.request = request;
        this.query = query;
        this.fullUrl = url;
        this.context = context;

        this.queryParams = new HashMap<String, String>();
        this.headers = new HashMap<String, String>();
        if (queryParams != null) {
            this.queryParams.putAll(queryParams);
        }
        if (headers != null) {
            this.headers.putAll(headers);
        }
        this.targetClass = targetClass;
    }

    public CMSQueryResult<T> build() {
        RestExecutor executor = new RestExecutor(config, restClient, request, query.getQueryString(), fullUrl, queryParams, headers, context);
        ClientResponse resp = executor.build();
        QueryResponseProcessor<T> queryProcessor = new QueryResponseProcessor<T>(config, resp, targetClass, context);
        CMSQueryResult<T> result = queryProcessor.build();

        for (ICMSEntity t : result.getEntities()) {
            t.clearDirtyBits();
        }
        return result;
    }

}
