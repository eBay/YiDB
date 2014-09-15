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

package com.ebay.cloud.cms.metadata.model;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

import com.ebay.cloud.cms.metadata.mongo.converter.MongoObjectIdConverter;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.utils.DateUtils;

/**
 * 
 * @author liubo
 * 
 */
public class Repository {

	public enum StateEnum {
		normal, deleting, deleted
	}

	public enum AccessType {
		Public, Private
	}

	private String id;

	private String repositoryName;
	private String repositoryAdmin;
	private StateEnum state;
	private Date createTime;
	private AccessType accessType;
	private RepositoryOption options = new RepositoryOption();

	@JsonIgnore
	private IMetadataService metadataService;

	@JsonIgnore
	private IRepositoryService repositoryService;

	public static final String STATE_FIELD = "state";
	public static final String REPOSITORY_FIELD_ADMIN_NAME = "repositoryAdmin";
	public static final String REPOSITORY_FIELD_TYPE_NAME = "accessType";
	public static final String REPOSITORY_FIELD_NAME = "repositoryName";
	public static final String REPOSITORY_FIELD_OPTIONS_NAME = "options";

	public Repository() {
		state = StateEnum.normal;
		createTime = new Date();
		accessType = AccessType.Public;
	}

	public Repository(String repoName) {
		repositoryName = repoName;
		state = StateEnum.normal;
		createTime = new Date();
		accessType = AccessType.Public;
	}

	@JsonProperty("_id")
	public String getId() {
		return id;
	}

	@JsonProperty("_id")
	@JsonDeserialize(using = MongoObjectIdConverter.ObjectIdDeserializer.class)
	public void setId(String id) {
		this.id = id;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public StateEnum getState() {
		return state;
	}

	public void setState(StateEnum state) {
		this.state = state;
	}

	public Date getCreateTime() {
		return DateUtils.cloneDate(createTime);
	}

	public void setCreateTime(Date createTime) {
		this.createTime = DateUtils.cloneDate(createTime);
	}

	public String getRepositoryAdmin() {
		return repositoryAdmin;
	}

	public void setRepositoryAdmin(String repositoryAdmin) {
		this.repositoryAdmin = repositoryAdmin;
	}

	public final AccessType getAccessType() {
		return accessType;
	}

	public final void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public IMetadataService getMetadataService() {
		return metadataService;
	}

	public void setMetadataService(IMetadataService metadataService) {
		this.metadataService = metadataService;
	}

	public IRepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(IRepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@JsonProperty("options")
	public RepositoryOption getOptions() {
		return options;
	}

	@JsonProperty("options")
	public void setOptions(RepositoryOption options) {
		this.options = options;
	}

    public boolean getCaseSensitiveDefault() {
        return options.isCaseSensitiveQuery();
    }

}
