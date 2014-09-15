/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liasu
 * 
 */
public class CMSClientConfig {

    // ===== public constants definition
    public static final String AUTHORIZATION     = "Authorization";
    
    public enum BatchOperationFailReturnOption {
		IMMEDIATE, ALL;
    }

    public enum CMSConsistentPolicy {
        // seeAlso PRIMARY
        @Deprecated
        PRIMARY_ONLY, 
        // seeAlso PRIMARY
        @Deprecated
        PRIMARY_PREFERRED,
        /**
         * <ul>
         * <li>When apply for a read operation, PRIMARY consistent policy means only to read from the primary.</li>
         * <li>When apply for a write operation, PRIMARY consistent policy means the request will wait for write complete on primary, 
         * but not guarantee the result will be populate to other server in the cluster when it returns</li>  
         * </ul>
         */
        PRIMARY,
        /**
         * <ul>
         * <li>When apply for a read operation, SECONDARY_ONLY consistent policy means only to read from the secondary.</li>
         * <li>When apply for a write operation, SECONDARY_ONLY consistent policy means the request will wait for write complete on primary, 
         * but not guarantee the result will be populate to other server in the cluster when it returns</li>  
         * </ul>
         */
        SECONDARY_ONLY, 
        /**
         * <ul>
         * <li>When apply for a read operation, SECONDARY_PREFERRED consistent policy means prefer to read from the secondary is possible; otherwise, from primary.</li>
         * <li>When apply for a write operation, SECONDARY_PREFERRED consistent policy means the request will wait for write complete on primary, 
         * but not guarantee the result will be populate to other server in the cluster when it returns</li>  
         * </ul>
         */
        SECONDARY_PREFERRED, 
        /**
         * <ul>
         * <li>When apply for a read operation, NEAREST consistent policy means prefer to read from the nearest node.</li>
         * <li>When apply for a write operation, NEAREST consistent policy means the request will wait for write complete on primary, 
         * but not guarantee the result will be populate to other server in the cluster when it returns</li>  
         * </ul>
         */
        NEAREST,
        /**
         * <ul>
         * <li>When apply for a read operation, NEART consistent policy would be applied.
         * <li>When apply for a write operation, REPLICA_ACKNOWLEDGED would be applied.
         * </ul>
         */
        REPLICA_ACKNOWLEDGED,
        /**
         * <ul>
         * <li>When apply for a read operation, MAJORITY consistent policy means prefer to read from the nearest node.</li>
         * <li>When apply for a write operation, MAJORITY consistent policy means the request will wait for write complete on majority servers</li>  
         * </ul>
         */
        MAJORITY;
    }

    public enum CMSPriority {
        CRITICAL, IMPORTANT, NEUTRAL, NON_CRITICAL, DEBUG
    }

    // ==== instance variables
    private final String              branch;
    private final String              repository;
    private final String              modelPackagePrefix;
    private final Map<String, String> paramHeader   = Collections.synchronizedMap(new HashMap<String, String>());

    private String                    serverBaseUrl = "http://localhsot:9090/cms";
    private Integer                   connnectionTimeout;
    private Integer                   timeOut;
    /*
     * The user that to be passed in correlation, if not set, the
     * correlationUser will simple be the token
     */
    private String                    correlationUser;
    private Integer                   retryTime;
    
    private boolean                   useGzip;

    public CMSClientConfig(String serverBaseUrl, String repo, String branch, String clientPkgPrefix) {
        this.branch = branch;
        this.repository = repo;
        this.serverBaseUrl = serverBaseUrl;
        this.modelPackagePrefix = clientPkgPrefix;
        this.paramHeader.put(CMSClientContext.X_CMS_PRIORITY, CMSPriority.NEUTRAL.toString());
        this.paramHeader.put(CMSClientContext.X_CMS_CONSISTENCY, CMSConsistentPolicy.NEAREST.toString());
        this.timeOut = 10 * 6000;
        this.connnectionTimeout = 10 * 6000;
        this.retryTime = 5;
        this.useGzip = true;
    }

    public final String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public final void addHeader(String key, String value) {
        paramHeader.put(key, value);
    }

    public final Map<String, String> getHeaders() {
        return new HashMap<String, String>(paramHeader);
    }

    public final String getClientPackagePrefix() {
        return modelPackagePrefix;
    }

    public final String getBranch() {
        return branch;
    }

    public final String getRepository() {
        return repository;
    }

    public final CMSPriority getPriority() {
        String v = paramHeader.get(CMSClientContext.X_CMS_PRIORITY);
        return v == null ? null : CMSPriority.valueOf(v);
    }

    public final void setPriority(CMSPriority priority) {
        paramHeader.put(CMSClientContext.X_CMS_PRIORITY, priority == null ? null : priority.toString());
    }

    public final CMSConsistentPolicy getConsistentPolicy() {
        String v = paramHeader.get(CMSClientContext.X_CMS_CONSISTENCY);
        return v == null ? null : CMSConsistentPolicy.valueOf(v);
    }

    public final void setConsistentPolicy(CMSConsistentPolicy consistentPolicy) {
        paramHeader.put(CMSClientContext.X_CMS_CONSISTENCY, consistentPolicy == null ? null : consistentPolicy.toString());
    }

    public final String getAuthorization() {
        return paramHeader.get(AUTHORIZATION);
    }

    public final void setAuthorization(String authorization) {
        paramHeader.put(AUTHORIZATION, authorization);
        if (correlationUser == null) {
            correlationUser = authorization;
        }
    }

    public Integer getConnnectionTimeout() {
        return connnectionTimeout;
    }

    public void setConnnectionTimeout(Integer connnectionTimeout) {
        this.connnectionTimeout = connnectionTimeout;
    }

    public final Integer getTimeOut() {
        return timeOut;
    }

    public final void setTimeOut(Integer timeOut) {
        this.timeOut = timeOut;
    }

    public String getUser() {
        return correlationUser;
    }

    public void setUser(String user) {
        this.correlationUser = user;
    }

    /**
     * The retry time configured to control the special operation retry times.
     * 
     * NOTE: NOT all operations in {@code CMSClientService} would be retried.
     * Only createRelationship/deleteRelationship would be retried.
     * 
     * @return - never return null. 1. If retryTime is set as 0.
     */
    public Integer getRetryTime() {
        if (retryTime == null || retryTime < 0) {
            return 0;
        }
        return retryTime;
    }

    public void setRetryTime(Integer retryTime) {
        if (retryTime == null || retryTime < 0) {
            this.retryTime = 0;
        } else {
            this.retryTime = retryTime;
        }
    }

    public boolean isUseGzip() {
        return useGzip;
    }

    public void setUseGzip(boolean useGzip) {
        this.useGzip = useGzip;
    }

}
