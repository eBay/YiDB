/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.exception.CMSErrorCodeEnum;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext.LOG_LEVEL;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext.RequestInfo;
import com.ebay.cloud.cms.typsafe.service.Status;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author liasu
 * 
 */
public abstract class AbstractResponseProcessor {

    public static final String  ERROR  = "error";
    public static final String  STATUS  = "status";
    public static final String  ODBSTATUS  = "ODBWriteStatus";
    public static final String  CODE   = "code";
    public static final String  MSG    = "msg";
    public static final String  STACK_TRACE = "stackTrace";
    public static final String  RESULT = "result";
    public static final Logger  logger = LoggerFactory.getLogger(AbstractResponseProcessor.class);

    protected final String         jsonResponse;
    protected RequestInfo          requestInfo;
    protected final HttpRequest    request;
    protected final ClientResponse response;
    protected final CMSClientContext context;
    final ObjectMapper objectMapper = Constants.objectMapper;

    public AbstractResponseProcessor(ClientResponse resp, HttpRequest request, CMSClientContext context) {
        this.request = request;
        this.response = resp;
        this.context = context;
        this.jsonResponse = this.response.getEntity(String.class);
        if (context != null) {
            context.setLastResponse(jsonResponse);
            requestInfo = context.getLastRequest();
        }
        if (Strings.isNullOrEmpty(jsonResponse)) {
            throw new CMSClientException(response.getStatus(), "empty reponse string!", jsonResponse, requestInfo);
        }
//        logger.debug(MessageFormat.format("Process reponse: {0}", jsonResponse));
    }
    
    AbstractResponseProcessor(ClientResponse resp, String jsonString, HttpRequest request, CMSClientContext context) {
        this.request = request;
        this.response = resp;
        this.jsonResponse = jsonString;
        this.context = context;
        if (context != null) {
            context.setLastResponse(jsonResponse);
            requestInfo = context.getLastRequest();
        }
        if (Strings.isNullOrEmpty(jsonResponse)) {
            throw new CMSClientException(response.getStatus(), "empty reponse string!", jsonResponse, requestInfo);
        }
    }

    /**
     * FIXME: For PaaS integration, we choose jackson 1.7 compatible deprecation API only 
     */
    @SuppressWarnings("deprecation")
    protected void parseResponseHeader() {
        // HTTP header checking
        int httpCode = response.getStatus();
        int cmsCode = -1;
        String msg = "";
        // keep extracting from ERROR and STATUS, as there would be an interval of transitioning from ERROR to STATUS in server side.
        JsonNode errorHeader = getRootNode().get(ERROR);
        JsonNode statusHeader = getRootNode().get(STATUS);
        if (errorHeader != null) {
            cmsCode = errorHeader.get(CODE).getValueAsInt();
            msg = errorHeader.get(MSG).getValueAsText();
        } else if (statusHeader != null) {
            cmsCode = statusHeader.get(CODE).getValueAsInt();
            msg = statusHeader.get(MSG).getValueAsText();
        }

        if (!isHttpSuccess(httpCode) && !isDeleteNotFound(httpCode, cmsCode)) {
            String errorMsg = MessageFormat
                    .format("Response returns error code that indicates failure, http response code {0}, the error message is {1}", httpCode, msg);
            if (loggingEnabled()) {
                // TRICKY::in some special client case, they want to supress the logging for given request to ease for issue tracking..
                logger.error(errorMsg);
            }
            throw new CMSClientException(httpCode, errorMsg, jsonResponse, requestInfo);
        }

        if (!isAppSuccess(cmsCode) && !isGetNotFound(cmsCode, msg) && !isDeleteNotFound(httpCode, cmsCode)) {
            String errorMsg = MessageFormat
                    .format("Response returns error code that not succeed, http response code  {0}, service error code: {1}, the error message is {2}", httpCode, cmsCode, msg);
            if (loggingEnabled()) {
                // TRICKY::in some special client case, they want to supress the logging for given request to ease for issue tracking..
                logger.error(errorMsg);
            }
            throw new CMSClientException(httpCode, errorMsg, jsonResponse, requestInfo);
        }
    }

    boolean loggingEnabled() {
        return context.getLogSetting() != LOG_LEVEL.SUPPRESS_LOG;
    }

    /**
     * Not found for get is not a error case.
     */
    private boolean isGetNotFound(int cmsCode, String msg) {
        if (cmsCode == Response.Status.NOT_FOUND.getStatusCode() && request == HttpRequest.GET && msg != null
                && msg.contains(ENTITY_NOT_FOUND_MSG)) {
            return true;
        }
        return false;
    }

    /**
     * Delete an entity that not exists. Donnot throw exception even when the
     * service return http 500 or 404.
     */
    private boolean isDeleteNotFound(int httpCode, int cmsCode) {
        if (request == HttpRequest.DELETE
                && (httpCode == Response.Status.BAD_REQUEST.getStatusCode() || httpCode == Response.Status.NOT_FOUND
                        .getStatusCode()) && cmsCode == CMSErrorCodeEnum.ENTITY_NOT_FOUND.getErrorCode()) {
            return true;
        }
        return false;
    }

    protected abstract JsonNode getRootNode();

    /**
     * Check http code based on the http standards
     */
    protected boolean isHttpSuccess(int errorCode) {
        if (errorCode == Response.Status.NOT_FOUND.getStatusCode()) {
            return true;// make the 404 to be determined later combined with other information(cmsCode, and msg).
        }
        // http spec: 200 + are succeed
        return errorCode < 300;
    }

    private static final String ENTITY_NOT_FOUND_MSG = "entity not found";
    
    /**
     * Checks CMS application specific error code
     */
    protected boolean isAppSuccess(int errorCode) {
        return errorCode == Response.Status.OK.getStatusCode();
    }

    protected JsonNode readJson(String jsonResponse) {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new CMSClientException(response.getStatus(), MessageFormat.format(
                    "Read json response error, the response string is '' {0} ''", jsonResponse), e, jsonResponse, requestInfo);
        }
        return rootNode;
    }

    public static List<Status> extractStatus(String jsonResponse) {
        Status   cmsStatus = null;
        Status   dualWriteStatus = null;
        JsonNode rootNode = null;
        try {
            rootNode = CMSClientException.objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            // not able to find the status from the response body
            return Arrays.asList(cmsStatus, dualWriteStatus);
        }
        // keep extracting from ERROR and STATUS, as there would be an interval of transitioning from ERROR to STATUS in server side.
        JsonNode errorHeader = rootNode.get(ERROR);
        JsonNode statusHeader = rootNode.get(STATUS);
        JsonNode dualWriteHeader = rootNode.get(ODBSTATUS);
        if (errorHeader != null) {
            cmsStatus = Status.fromJson(errorHeader);
        }
        if (statusHeader != null) {
            cmsStatus = Status.fromJson(statusHeader);
        }
        if (dualWriteHeader != null) {
            dualWriteStatus = Status.fromJson(dualWriteHeader);
        }
        return Arrays.asList(cmsStatus, dualWriteStatus);
    }
}
