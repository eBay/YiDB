/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exception;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

import com.ebay.cloud.cms.typsafe.restful.AbstractResponseProcessor;
import com.ebay.cloud.cms.typsafe.restful.Constants;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext.RequestInfo;
import com.ebay.cloud.cms.typsafe.service.Status;

/**
 * General exception that the api would throw. The exception contains the http
 * code and cms application error code
 * 
 * No error code(both http and cms) means the code is not set or returned by
 * cms server.
 * 
 * @author liasu
 * 
 */
public class CMSClientException extends RuntimeException {

    private static final long serialVersionUID = -6960965988624065259L;
    public static final ObjectMapper objectMapper = Constants.objectMapper;

    private int httpResponseCode = -1;
    private Status   cmsStatus;
    private Status   dualWriteStatus;
    private String   jsonResponse;
    private RequestInfo request;

    public CMSClientException(String msg) {
        super(msg);
    }

    public CMSClientException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CMSClientException(int httpCode, String msg, String jsonResponse, RequestInfo request) {
        super(msg);
        httpResponseCode = httpCode;
        this.request = request;
        getStatus(jsonResponse);
    }

    public CMSClientException(int httpCode, String msg, Throwable cause, String jsonResponse, RequestInfo request) {
        super(msg, cause);
        httpResponseCode = httpCode;
        this.request = request;
        // extract CMS header/dual write header for response
        getStatus(jsonResponse);
    }

    private void getStatus(String json) {
        jsonResponse = json;
        List<Status> status = AbstractResponseProcessor.extractStatus(json);
        cmsStatus = status.get(0);
        dualWriteStatus = status.get(0);
    }

    public final int getHttpResponseCode() {
        return httpResponseCode;
    }

    /**
     * Use CMSClientException.getCmsResponseStatus() instead. 
     * @return
     */
    @Deprecated
    public final int getCmsResponseCode() {
        if (cmsStatus == null) {
            return -1;
        }
        return cmsStatus.getErrorCode();
    }

    public final Status getCmsResponseStatus() {
        return cmsStatus;
    }

    public final Status getDualWriteStatus() {
        return dualWriteStatus;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public RequestInfo getRequest() {
        return request;
    }

    public void setRequest(RequestInfo jsonRequest) {
        this.request = jsonRequest;
    }

    /**
     * Returns a {@link CMSErrorCodeEnum} to give hint on the error code.
     * If the {@code cmsResponseCode} is not defined in error code enum, 
     * a common UNDEFINED_ERROR_CODE will be returned.
     * @return
     */
    @Deprecated
    public final CMSErrorCodeEnum getCMSErrorCodeEnum() {
        return CMSErrorCodeEnum.getErrorEnum(getCmsResponseCode());
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(" App error message: ").append(getLocalizedMessage()).append(",");
        if (httpResponseCode != -1) {
            buffer.append(" HTTP status code: ").append(httpResponseCode).append("\n");
        }
        if (request != null) {
            buffer.append(request);
        }
        if (jsonResponse != null) {
            buffer.append(jsonResponse);
        }
        return buffer.toString();
    }

}
