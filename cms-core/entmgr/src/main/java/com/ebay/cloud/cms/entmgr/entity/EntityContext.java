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

package com.ebay.cloud.cms.entmgr.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSTrackingCodeEnum;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.SearchCriteria;

public class EntityContext {
	public enum ModifyAction {
		MODIFYENTITY,

		PUSHFIELD,

		PULLFIELD,

		DELETEFIELD
	}

	public enum BatchOperationFailReturnOption {
		IMMEDIATE, ALL;

		public static BatchOperationFailReturnOption fromString(String failReturnOption) {
			BatchOperationFailReturnOption ret = IMMEDIATE;
			if (failReturnOption == null) {
				return ret;
			}

			for (BatchOperationFailReturnOption qm : BatchOperationFailReturnOption.values()) {
				if (qm.name().equalsIgnoreCase(failReturnOption)) {
					ret = qm;
					break;
				}
			}

			return ret;
		}
	}

	// per request information
	private String sourceIp;
	private String requestId;
	// this is the client authentication
	private String modifier;
	// the user is a optional client controlled string.
	private String userId;
	private String comment;
	// path for create purpose
	private String path;
	// for delete purpose
	private int version = IEntity.NO_VERSION;
	private HttpServletRequest request;

	private ConsistentPolicy consistentPolicy;

	private String modifyFieldName;

	private ModifyAction modifyAction;

	private CMSDBConfig dbConfig;

	private Map<String, Object> databaseSizeMap;

	private String dal;
	private Registration registration;

	private boolean fetchFieldProperty;
	
	private BatchOperationFailReturnOption batchOperationFailReturnOption;

	private final Map<String, List<SearchCriteria>> additionalCriteria;
	
	private CMSTrackingCodeEnum requestTrackingCode;

	public EntityContext() {
	    this.additionalCriteria = new HashMap<String, List<SearchCriteria>>();
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public ModifyAction getModifyAction() {
		return modifyAction;
	}

	public void setModifyAction(ModifyAction modifyAction) {
		this.modifyAction = modifyAction;
	}

	public long getDbTimeCost() {
		return dbTimeMili;
	}

	public void setDbTimeCost(int dbTimeCost) {
		this.dbTimeMili = dbTimeCost;
	}

	private long dbTimeMili = 0L;

	public String getSourceIp() {
		return sourceIp;
	}

	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	private long totalTimeCost = 0;
	private long startProcessingTime = 0;

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

	public final ConsistentPolicy getConsistentPolicy() {
		return consistentPolicy;
	}

	public final void setConsistentPolicy(ConsistentPolicy consistPolicy) {
		this.consistentPolicy = consistPolicy;
	}

	/**
	 * A collection of the field in current options
	 */
	private Collection<String> queryFields = new HashSet<String>();

	public void addQueryField(String field) {
		queryFields.add(field);
	}

	public final Collection<String> getQueryFields() {
		return queryFields;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comments) {
		this.comment = comments;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getModifyFieldName() {
		return modifyFieldName;
	}

	public void setModifyFieldName(String modifyFieldName) {
		this.modifyFieldName = modifyFieldName;
	}

	public CMSDBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(CMSDBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	public Map<String, Object> getDatabaseSizeMap() {
		return databaseSizeMap;
	}

	public void setDatabaseSizeMap(Map<String, Object> databaseSizeMap) {
		this.databaseSizeMap = databaseSizeMap;
	}

	public Registration getRegistration() {
		return registration;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	public boolean isFetchFieldProperty() {
		return fetchFieldProperty;
	}

	public void setFetchFieldProperty(boolean fetchFieldProperty) {
		this.fetchFieldProperty = fetchFieldProperty;
	}

	public BatchOperationFailReturnOption getBatchOperationFailReturnOption() {
		if (batchOperationFailReturnOption != null) {
			return batchOperationFailReturnOption;
		} else {
			return BatchOperationFailReturnOption.IMMEDIATE;
		}
	}

	public void setBatchOperationFailReturnOption(BatchOperationFailReturnOption batchOperationFailReturnOption) {
		this.batchOperationFailReturnOption = batchOperationFailReturnOption;
	}

	public String getDal() {
		return dal;
	}

	public void setDal(String dal) {
		this.dal = dal;
	}

    public Map<String, List<SearchCriteria>> getAdditionalCriteria() {
        return additionalCriteria;
    }

    public void setAdditionalCriteria(Map<String, List<SearchCriteria>> additionalCriteria) {
        this.additionalCriteria.clear();
        if (additionalCriteria != null) {
            this.additionalCriteria.putAll(additionalCriteria);
        }
    }

    public boolean hasRequestTrackingCode() {
        return requestTrackingCode != null;
    }
    public CMSTrackingCodeEnum getRequestTrackingCode() {
        return requestTrackingCode;
    }
    public void setRequestTrackingCode(CMSTrackingCodeEnum requestTrackingCode) {
        this.requestTrackingCode = requestTrackingCode;
    }
	
}
