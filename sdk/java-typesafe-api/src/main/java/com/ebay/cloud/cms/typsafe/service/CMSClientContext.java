/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.restful.AbstractResponseProcessor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.CMSConsistentPolicy;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.CMSPriority;

/**
 * A common place to have per-request parameter/header information, like user,
 * comments, priority, consistent-policy.
 * 
 * The configuration that have overlap with CMSClientConfig will hide the
 * setting in the cms client config.
 * 
 * @author liasu
 * 
 */
public class CMSClientContext {

	public static final String X_CMS_CONSISTENCY = "X-CMS-CONSISTENCY";
	public static final String X_CMS_PRIORITY = "X-CMS-PRIORITY";
	@Deprecated
	public static final String X_CMS_ALLOW_PARTIAL_WRITE = "X-CMS-ALLOW-PARTIAL-WRITE";
	public static final String X_CMS_CONDITIONAL_UPDATE = "X-CMS-CONDITIONAL-UPDATE";
	public static final String REQ_PARAM_PATH = "path";
	public static final String REQ_PARAM_UID = "uid";
	public static final String REQ_PARAM_COMMENT = "comment";
	public static final String REQ_PARAM_BATCH_FAIL_OPTION = "failReturnOption";
	@Deprecated
	public static final String REQ_PARAM_FORCEUPDATE = "forceUpdate";

	// private String path;
	// private String user;
	// private String comment;
	// private CMSPriority priority;
	// private CMSConsistentPolicy consistentPolicy;
	// @Deprecated
	// private Boolean allowPartialWrite;
	// private Boolean forceUpdate;
	// private Boolean conditionalUpdate;

	/*
	 * This parameter would override the one inside the config if set.
	 */
	private Integer retryCount;

	private Map<String, String> queryParameters = new HashMap<String, String>();

	/**
	 * This is an internal tracking metric to count the retry count for each
	 * retry operation. It will be automatically reset when a retry operation
	 * happens. So this value expected to be used for development logging only.
	 * The value might change for an operation with multiple service call. Also,
	 * we don't guarantee about the thread-safeness. DON'T rely on this value to
	 * do any client logic.
	 */
	private int retryMetric = 0;

	public static enum LOG_LEVEL {
		ENABLE_LOG, SUPPRESS_LOG
	}

	private LOG_LEVEL logSetting = LOG_LEVEL.ENABLE_LOG;

	// per request response objects
	public static class RequestInfo {
		public RequestInfo(HttpRequest method, String url, String body) {
			this.requestMethod = method;
			this.requestUrl = url;
			this.requestBody = body;
		}

		public final HttpRequest requestMethod;
		public final String requestUrl;
		public final String requestBody;

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Request Method : " + requestMethod.name() + " ,");
			sb.append("Request Url : " + requestUrl + " ,");
			sb.append("Request Body : " + requestBody + " ,");
			return sb.toString();
		}
	}

	private RequestInfo lastRequest;
	private String lastResponse;
	private Status cmsStatus;
	private Status dualWriteStatus;

	public String getUser() {
		return queryParameters.get(REQ_PARAM_UID);
	}

	public void setUser(String user) {
		queryParameters.put(REQ_PARAM_UID, user);
	}

	public String getComment() {
		return (String) queryParameters.get(REQ_PARAM_COMMENT);
	}

	public void setComment(String comment) {
		queryParameters.put(REQ_PARAM_COMMENT, comment);
	}

	@Deprecated
	public String getPriority() {
		return queryParameters.get(X_CMS_PRIORITY);
	}

	public CMSPriority getPriorityEnum() {
		String cmsPriorityString = queryParameters.get(X_CMS_PRIORITY);
		if (cmsPriorityString != null) {
			return CMSPriority.valueOf(cmsPriorityString);
		} else {
			return null;
		}
	}

	@Deprecated
	public void setPriority(String priority) {
		if (priority != null) {
			queryParameters.put(X_CMS_PRIORITY, priority);
		} else {
			queryParameters.put(X_CMS_PRIORITY, null);
		}
	}

	public void setPriority(CMSPriority priority) {
		if (priority != null) {
			queryParameters.put(X_CMS_PRIORITY, priority.name());
		} else {
			queryParameters.put(X_CMS_PRIORITY, null);
		}
	}

	/**
	 * @see getConsistentPolicyEnum()
	 * @return
	 */
	@Deprecated
	public String getConsistentPolicy() {
		return queryParameters.get(X_CMS_CONSISTENCY);
	}

	public CMSConsistentPolicy getConsistentPolicyEnum() {
		String cmsConsistentPolicyString = queryParameters.get(X_CMS_CONSISTENCY);
		if (cmsConsistentPolicyString != null) {
			return CMSConsistentPolicy.valueOf(cmsConsistentPolicyString);
		} else {
			return null;
		}
	}

	@Deprecated
	public Boolean getForceUpdate() {
		String booleanString = queryParameters.get(REQ_PARAM_FORCEUPDATE);
		if (booleanString != null) {
			return Boolean.valueOf(booleanString);
		} else {
			return null;
		}
	}

	@Deprecated
	public void setForceUpdate(Boolean forceUpdate) {
		if (forceUpdate != null) {
			queryParameters.put(REQ_PARAM_FORCEUPDATE, forceUpdate.toString());
		} else {
			queryParameters.put(REQ_PARAM_FORCEUPDATE, null);
		}
	}

	public String getPath() {
		return queryParameters.get(REQ_PARAM_PATH);
	}

	/**
	 * If parent class is embed class, if given id is a full parent embed id.
	 * The parentClass would be ignored. E.g.
	 * 
	 * <pre>
	 *  Parent is normal class.
	 *  <code>
	 *      setPath("ApplicationService", "app-id", "serviceInstances")
	 *  </code>
	 *  Parent is embed class, says NetworkController(itself is embed in AssetServer) has an embed interfce, then.
	 *  <code>
	 *      setPath("NetworkController", "AssetServer!<as-id>!networkControllers!<network-controller-id>", "serviceInstances")
	 *  </code>
	 * </pre>
	 * 
	 * Assumption that for parent is embed class case, user just need to simply
	 * set the id of parent entity.
	 */
	public void setPath(String parentClass, String id, String field) {
		if (id.contains("!")) {
			queryParameters.put(REQ_PARAM_PATH, id + field);
		} else {
			queryParameters.put(REQ_PARAM_PATH, parentClass + "!" + id + "!" + field);
		}
	}
	
	public void setBatchFailOption(BatchOperationFailReturnOption option) {
		if (option != null) {
			queryParameters.put(REQ_PARAM_BATCH_FAIL_OPTION, option.name());
		} else {
			queryParameters.put(REQ_PARAM_BATCH_FAIL_OPTION, null);
		}
	}
	
	public BatchOperationFailReturnOption getBatchFailOption() {
		String optionString = queryParameters.get(REQ_PARAM_BATCH_FAIL_OPTION);
		if (optionString != null) {
			return BatchOperationFailReturnOption.valueOf(optionString);
		} else {
			return null;
		}
	}

	public void setConsistentPolicy(CMSConsistentPolicy policy) {
		if (policy != null) {
			queryParameters.put(X_CMS_CONSISTENCY, policy.name());
		} else {
			queryParameters.put(X_CMS_CONSISTENCY, null);
		}
	}

	@Deprecated
	public void setConsistentPolicy(String consistentPolicy) {
		if (consistentPolicy != null) {
			queryParameters.put(X_CMS_CONSISTENCY, consistentPolicy);
		} else {
			queryParameters.put(X_CMS_CONSISTENCY, null);
		}
	}

    Map<String, String> getQueryParameters() {
        return new HashMap<String, String>(queryParameters);
    }

	public static String encodeString(String rawString) {
		String encoded = rawString;
		try {
			encoded = URLEncoder.encode(rawString, "utf8");
			encoded = encoded.replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new CMSClientException(MessageFormat.format(
					"error build request url when encoding query parameter to utf8.", rawString), e);
		}
		return encoded;
	}

	Map<String, String> getHeader() {
		Map<String, String> headers = new HashMap<String, String>();
		if (getPriorityEnum() != null) {
			headers.put(X_CMS_PRIORITY, getPriorityEnum().name());
		}
		if (getConsistentPolicyEnum() != null) {
			headers.put(X_CMS_CONSISTENCY, getConsistentPolicyEnum().name());
		}
		if (getAllowPartialWrite() != null) {
			headers.put(X_CMS_ALLOW_PARTIAL_WRITE, getAllowPartialWrite().toString());
		}
		if (getConditionalUpdate() != null) {
			headers.put(X_CMS_CONDITIONAL_UPDATE, getConditionalUpdate().toString());
		}
		return headers;
	}

	@Deprecated
	public Boolean getAllowPartialWrite() {
		String booleanString = queryParameters.get(X_CMS_ALLOW_PARTIAL_WRITE);
		if (booleanString != null) {
			return Boolean.valueOf(booleanString);
		} else {
			return null;
		}
	}

	@Deprecated
	public void setAllowPartialWrite(Boolean allowPartialWrite) {
		if (allowPartialWrite != null) {
			queryParameters.put(X_CMS_ALLOW_PARTIAL_WRITE, allowPartialWrite.toString());
		} else {
			queryParameters.put(X_CMS_ALLOW_PARTIAL_WRITE, null);
		}
	}

	public Boolean getConditionalUpdate() {
		String booleanString = queryParameters.get(X_CMS_CONDITIONAL_UPDATE);
		if (booleanString != null) {
			return Boolean.valueOf(booleanString);
		} else {
			return null;
		}
	}

	public void setConditionalUpdate(Boolean cas) {
		if (cas != null) {
			queryParameters.put(X_CMS_CONDITIONAL_UPDATE, cas.toString());
		} else {
			queryParameters.put(X_CMS_CONDITIONAL_UPDATE, null);
		}
	}

	// //////////////////
	// request-scope response
	// //////////////////

	public String getLastResponse() {
		return lastResponse;
	}

	public void setLastResponse(String lastResponse) {
		this.lastResponse = lastResponse;
		// fill dual write status
		List<Status> status = AbstractResponseProcessor.extractStatus(lastResponse);
		cmsStatus = status.get(0);
		dualWriteStatus = status.get(1);
	}

	public Status getLastCmsStatus() {
		return cmsStatus;
	}

	public Status getLastDualWriteStatus() {
		return dualWriteStatus;
	}

	public RequestInfo getLastRequest() {
		return lastRequest;
	}

	public void setLastRequest(RequestInfo request) {
		this.lastRequest = request;
	}

	public LOG_LEVEL getLogSetting() {
		return logSetting;
	}

	public void setLogSetting(LOG_LEVEL logSetting) {
		this.logSetting = logSetting;
	}

	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

	public boolean hasRetryCount() {
		return this.retryCount != null;
	}

	public int getRetryMetric() {
		return retryMetric;
	}

	public void resetRetryMetric(int retryMetric) {
		this.retryMetric = 0;
	}

	public void incRetryMetric() {
		this.retryMetric++;
	}

}
