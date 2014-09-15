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

package com.ebay.cloud.cms.metadata.service;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.utils.CheckConditions;

public class MetadataContext {

    public static enum UpdateOptionMode {
        ADD, DELETE, UPDATE, VALIDATE
    }

    private String sourceIp;
    private String requestId;
    // this is the client authentication
    private String subject;
    // the user is a optional client controlled string.
    private String userId;

    private long dbTimeMili          = 0L;
    
    private long totalTimeCost       = 0;
    private long startProcessingTime = 0;

    private UpdateOptionMode optionChangeMode;
    
    // fields for meta history search
    private Integer          skip;
    private Integer          limit;
    private Date             start;
    private Date             end;
    
    private boolean refreshMetadata;
    private boolean refreshRepsitory;
    
    private CMSDBConfig      dbConfig;
    private Map<String, String> additionalParameter = new HashMap<String, String>();
    
    public MetadataContext() {
    }
    
    public MetadataContext(boolean refreshMeta, boolean refreshRepo) {
        this.refreshMetadata = refreshMeta;
        this.refreshRepsitory = refreshRepo;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public long getDbTimeCost() {
        return dbTimeMili;
    }

    public void addDbTimeCost(long dbTimeMili) {
        this.dbTimeMili += dbTimeMili;
    }


    public long getTotalTimeCost() {
        return totalTimeCost;
    }

    public void setTotalTimeCost(long totalTimeCost) {
        this.totalTimeCost = totalTimeCost;
    }

    public long getStartProcessingTime() {
        return startProcessingTime;
    }

    public void setStartProcessingTime(long startProcessingTime) {
        this.startProcessingTime = startProcessingTime;
    }

    public UpdateOptionMode getOptionChangeMode() {
        return optionChangeMode;
    }

    public void setOptionChangeMode(UpdateOptionMode optionChangeMode) {
        this.optionChangeMode = optionChangeMode;
    }

    public Integer getSkip() {
        return skip;
    }

    public void setSkip(Integer skip) {
        this.skip = skip;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Date getStart() {
        return start == null ? null : new Date(start.getTime());
    }

    public void setStart(Date start) {
        if (start != null && end != null) {
            CheckConditions.checkArgument(start.before(end),
                    MessageFormat.format("Start date {0} must be less than end date {1}!", start, end));
        }
        this.start = (start == null ? null : new Date(start.getTime()));
    }

    public Date getEnd() {
        return end == null ? null : new Date(end.getTime());
    }

    public void setEnd(Date end) {
        if (start != null && end != null) {
            CheckConditions.checkArgument(end.after(start),
                    MessageFormat.format("End date {0} must be greater than start date {1}!", end, start));
        }
        this.end = (end == null ? null : new Date(end.getTime()));
    }

    public void setRequestId(String id) {
        this.requestId = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

	public CMSDBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(CMSDBConfig dbConfig) {
		this.dbConfig = dbConfig;
    }

    public long getDbTimeMili() {
        return dbTimeMili;
    }

    public void setDbTimeMili(long dbTimeMili) {
        this.dbTimeMili = dbTimeMili;
    }

    public Map<String, String> getAdditionalParameter() {
        return additionalParameter;
    }

    public void addAdditionalParameter(String key, String value) {
        this.additionalParameter.put(key, value);
    }

    public boolean isRefreshMetadata() {
        return refreshMetadata;
    }

    public void setRefreshMetadata(boolean refreshMetadata) {
        this.refreshMetadata = refreshMetadata;
    }

    public boolean isRefreshRepsitory() {
        return refreshRepsitory;
    }
    public void setRefreshRepsitory(boolean refreshRepsitory) {
        this.refreshRepsitory = refreshRepsitory;
    }

}
