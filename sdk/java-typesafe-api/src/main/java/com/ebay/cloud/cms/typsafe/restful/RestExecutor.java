/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.StringKeyStringValueIgnoreCaseMultivaluedMap;

/**
 * @author liasu
 * 
 */
public class RestExecutor {

    private static final Logger logger         = LoggerFactory.getLogger(RestExecutor.class);
    private static final String CORRELATION_ID_HEADER = "X-EVENT_TRACKER_CORRELATIONID";
    private static final String CORRELATION_ID_PATTERN = "{0}-{1}-{2}-{3}";

    public static enum HttpRequest {
        GET, POST, PUT, DELETE
    }

    private final String              fullUrl;
    private final Object              entity;
    private final Client              restClient;
    private final HttpRequest         request;
    private final CMSClientConfig     config;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final IJsonBuilder         builder;
    private final CMSClientContext    context;

    public RestExecutor(CMSClientConfig config, Client restClient, HttpRequest request, Object entity, String url,
            Map<String, String> queryParams, Map<String, String> headers, CMSClientContext context) {
        this(config, restClient, request, entity, url, queryParams, headers, (IJsonBuilder) null, context);
    }

    public RestExecutor(CMSClientConfig config, Client restClient, HttpRequest request, Object entity, String url,
            Map<String, String> queryParams, Map<String, String> headers, IJsonBuilder builder, CMSClientContext context) {
        this.config = config;
        this.restClient = restClient;
        this.request = request;
        this.entity = entity;
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

        if (builder == null) {
            this.builder = new JsonBuilder(this.config);
        } else  {
            this.builder = builder;
        }
    }

    public ClientResponse build() {
        String json = null;
        // rest standards recommends that get doesn't rely on the
        // request body
        if (entity != null && (request == HttpRequest.POST || request == HttpRequest.PUT)) {
            json = toJson(entity);
        }

        WebResource.Builder builder = buildResource(json);

        // store the request to context
        if (context != null) {
            context.setLastRequest(new CMSClientContext.RequestInfo(request, fullUrl, json));
        }

        logger.debug(MessageFormat.format("Sending request, with method: {0} base url: {1}. Payload: {2}\n", request.name(), fullUrl,
                json));
        long last = System.nanoTime();
        ClientResponse cr = null; 
        try {
            cr = builder.method(request.name(), ClientResponse.class);
        } catch (Exception e) {
            throw new CMSClientException(-1, "Request got exception! : " + e.getMessage() + " url is: " + fullUrl + " endpoint: " + config.getServerBaseUrl(), e, e.getMessage(), context.getLastRequest());
        }
        logger.debug(MessageFormat.format("Got server response, method: {0} base url: {1}. Payload: {2}\n. Used time: {3}(ns).", request.name(), fullUrl,
                json, (System.nanoTime() - last)));
        return cr;
    }

    private WebResource.Builder buildResource(String json) {
        WebResource resource = restClient.resource(fullUrl);
        MultivaluedMap<String, String> queryParamMap = new StringKeyStringValueIgnoreCaseMultivaluedMap();
        for (Entry<String, String> e : queryParams.entrySet()) {
            queryParamMap.add(e.getKey(), e.getValue());
        }

        resource = resource.queryParams(queryParamMap);
        // add correlation id
        String correlation = generateCorrelationId();
        WebResource.Builder builder = resource.header("Content-Type", "application/json").header("Accept", "application/json");
        for (Entry<String, String> e : headers.entrySet()) {
            builder.header(e.getKey(), e.getValue());
        }
        if (json != null) {
        	builder.entity(json);
            if (config.isUseGzip()) {
                builder.header("Content-Encoding", "gzip");
            }
        }
        if (config.isUseGzip()) {
            builder.header("Accept-Encoding", "gzip");
        }
        builder.header(CORRELATION_ID_HEADER, correlation);

        return builder;
    }
    
    protected String toJson(Object object) {
        return builder.buildJson(object);
    }
    
    private String generateCorrelationId() {
        String ip = "0.0.0.0";// NON-SONAR
        try {
            ip = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            logger.error("UnknowHostException when try to ip address, set as unknown!", uhe);
        }
        long timestamp = new Date().getTime();
        String user = config.getUser() == null ? "unknowUser" : config.getUser();

        String oid = "";
        if (entity instanceof ICMSEntity) {
            oid = ((ICMSEntity) entity).get_id();
        }
        return MessageFormat.format(CORRELATION_ID_PATTERN, ip, timestamp, user, oid);
    }

}
