package com.ebay.cloud.cms.typsafe.service;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.typsafe.exception.CMSErrorCodeEnum;
import com.ebay.cloud.cms.typsafe.restful.AbstractResponseProcessor;

/**
 * 
 * @author liasu
 * 
 */
public class Status {

    private int errorCode;
    private CMSErrorCodeEnum errorEnum;
    private String msg;
    private String stackTrace;

    public int getErrorCode() {
        return errorCode;
    }

    public String getMsg() {
        return msg;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public CMSErrorCodeEnum getErrorEnum() {
        return errorEnum;
    }

    @SuppressWarnings("deprecation")
    public static Status fromJson(JsonNode errorHeader) {
        Status status = new Status();
        status.errorCode = errorHeader.get(AbstractResponseProcessor.CODE).getValueAsInt();
        status.msg = errorHeader.get(AbstractResponseProcessor.MSG).getValueAsText();
        status.stackTrace = errorHeader.get(AbstractResponseProcessor.STACK_TRACE).getValueAsText();
        status.errorEnum = CMSErrorCodeEnum.getErrorEnum(status.errorCode);
        return status;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("service error code : ").append(errorCode).append(", error message : ").append(msg);
        sb.append(", stackTrace : ").append(stackTrace);
        return sb.toString();
    }

}
