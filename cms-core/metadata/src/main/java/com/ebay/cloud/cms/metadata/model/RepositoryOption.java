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


/*******************************************************************************
 * Copyright (c) 2012-2013 eBay Inc.
 * All rights reserved. 
 *  
 * eBay PE Cloud Foundation Team [DL-eBay-SHA-COE-PE-Cloud-Foundation@ebay.com]
 *******************************************************************************/
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

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author shuachen
 * 
 *         2013-11-7
 */
public class RepositoryOption {

	@JsonProperty
	private Long maxDocumentSize;

	@JsonProperty
	private Integer maxIndexedArraySize;

	@JsonProperty
	private Integer maxNumOfIndexes;

	@JsonProperty
	private boolean caseSensitiveQuery;
	
	@JsonProperty
	private Long maxRepositorySize;
	
	public static final String REPOSITORY_OPTION_MAX_DOCUMENT_SIZE = "maxDocumentSize";
	public static final String REPOSITORY_OPTION_MAX_INDEXED_ARRAY_SIZE = "maxIndexedArraySize";
	public static final String REPOSITORY_OPTION_MAX_NUM_OF_INDEXES = "maxNumOfIndexes";
	public static final String REPOSITORY_OPTION_CASE_SENSITIVE_QUERY = "caseSensitiveQuery";
	public static final String REPOSITORY_OPTION_MAX_REPOSITORY_SIZE = "maxRepositorySize";

	public Long getMaxDocumentSize() {
		return maxDocumentSize;
	}

	public void setMaxDocumentSize(Long maxDocumentSize) {
		this.maxDocumentSize = maxDocumentSize;
	}

	public Integer getMaxIndexedArraySize() {
		return maxIndexedArraySize;
	}

	public void setMaxIndexedArraySize(Integer maxIndexedArraySize) {
		this.maxIndexedArraySize = maxIndexedArraySize;
	}

	public Integer getMaxNumOfIndexes() {
		return maxNumOfIndexes;
	}

	public void setMaxNumOfIndexes(Integer maxNumOfIndexes) {
		this.maxNumOfIndexes = maxNumOfIndexes;
	}

	public boolean isCaseSensitiveQuery() {
		return caseSensitiveQuery;
	}

	public void setCaseSensitiveQuery(boolean caseSensitiveQuery) {
		this.caseSensitiveQuery = caseSensitiveQuery;
	}

	public Long getMaxRepositorySize() {
		return maxRepositorySize;
	}

	public void setMaxRepositorySize(Long maxRepositorySize) {
		this.maxRepositorySize = maxRepositorySize;
	}
	
}
