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


package com.ebay.cloud.cms.service.resources.impl;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.service.exception.BadParamException;

public class MetadataHelper {
	private static final Logger logger = LoggerFactory.getLogger(MetadataHelper.class);
	
	public static final Date getQueryTime(UriInfo uriInfo, String queryParam) {
		MultivaluedMap<String, String> parameters = uriInfo
				.getQueryParameters();
		Date date = null;
		try {
			List<String> time = parameters.get(queryParam);
			if (time != null && time.size() > 0
					&& time.get(0).trim().length() > 0) {
				date = new Date(Long.parseLong(time.get(0)));
			}
			return date;
		} catch (NumberFormatException nfe) {
			logger.error("Error when getQueryTime ", nfe);
			throw new BadParamException(nfe, MessageFormat.format(
					"Query {0} time must be valid time milliseconds!",
					queryParam));
		}
	}

	public static final Integer getQueryInteger(UriInfo uriInfo,
			String parameterName) {
		MultivaluedMap<String, String> parameters = uriInfo
				.getQueryParameters();
		Integer value = null;
		try {
			List<String> paramValue = parameters.get(parameterName);
			if (paramValue != null && paramValue.size() > 0
					&& paramValue.get(0).trim().length() > 0) {
				value = Integer.parseInt(paramValue.get(0));
			}
			return value;
		} catch (NumberFormatException nfe) {
			logger.error("Error when getQueryInteger ", nfe);
			throw new BadParamException(nfe, MessageFormat.format(
					"Query {0} must be valid integer!", parameterName));
		}
	}
	
	public static Map<String, Object> convertRepository(Repository repo) {
		if (repo == null) {
			return null;
		}
		
		Map<String, Object> option = new HashMap<String, Object>();
		RepositoryOption options = repo.getOptions();
		option.put(RepositoryOption.REPOSITORY_OPTION_MAX_DOCUMENT_SIZE, options.getMaxDocumentSize());
		option.put(RepositoryOption.REPOSITORY_OPTION_MAX_INDEXED_ARRAY_SIZE, options.getMaxIndexedArraySize());
		option.put(RepositoryOption.REPOSITORY_OPTION_MAX_NUM_OF_INDEXES, options.getMaxNumOfIndexes());
		option.put(RepositoryOption.REPOSITORY_OPTION_CASE_SENSITIVE_QUERY, options.isCaseSensitiveQuery());
		option.put(RepositoryOption.REPOSITORY_OPTION_MAX_REPOSITORY_SIZE, options.getMaxRepositorySize());
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put(Repository.REPOSITORY_FIELD_NAME, repo.getRepositoryName());
		result.put(Repository.REPOSITORY_FIELD_TYPE_NAME, repo.getAccessType());
		result.put(Repository.REPOSITORY_FIELD_ADMIN_NAME, repo.getRepositoryAdmin());
		result.put(Repository.REPOSITORY_FIELD_OPTIONS_NAME, option);
		
		return result;
	}
}
