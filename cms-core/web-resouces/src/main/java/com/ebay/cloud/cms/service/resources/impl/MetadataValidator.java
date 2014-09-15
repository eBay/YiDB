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

import java.math.BigDecimal;
import java.util.Arrays;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalRepositoryException;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

public class MetadataValidator {
	public static void validateRepositoryOption(RepositoryOption option) {
		Long maxDocumentSize = option.getMaxDocumentSize();
		validateNumber(maxDocumentSize, "maxDocumentSize");
	
		Long maxRepositorySize = option.getMaxRepositorySize();
		validateNumber(maxRepositorySize, "maxRepositorySize");
	
		Integer maxIndexedArraySize = option.getMaxIndexedArraySize();
		validateNumber(maxIndexedArraySize, "maxIndexedArraySize");
	
		Integer maxNumOfIndexes = option.getMaxNumOfIndexes();
		validateNumber(maxNumOfIndexes, "maxNumOfIndexes");
	}

	public static void validateNumber(Number value, String key) {
		if (value != null) {
			CheckConditions.checkArgument(new BigDecimal(value.toString()).compareTo(BigDecimal.ZERO) > 0,
                "Configuration value must be greater than 0 for configuration item %s!", key);
		}
	}

	public static  void checkRepositoryName(String reponame) {
		CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(reponame), new IllegalRepositoryException("repository name can not be empty"));
		CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(reponame.trim()), new IllegalRepositoryException("repository name can not be empty"));
		CheckConditions.checkCondition((reponame.length() <= CMSConsts.MAX_LENGTH_OF_REPO_NAME), new IllegalRepositoryException("The length of repository name can not be exceed " + CMSConsts.MAX_LENGTH_OF_REPO_NAME));
		CheckConditions.checkCondition(org.apache.commons.lang3.StringUtils.containsNone(reponame,CMSConsts.INVALID_REPOSITORY_NAME_CHARACTERS), new IllegalRepositoryException("meta class " + reponame + " cannot contains invalid characters: " + Arrays.toString(CMSConsts.INVALID_REPOSITORY_NAME_CHARACTERS)));
	}
}
