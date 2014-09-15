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

package com.ebay.cloud.cms.entmgr.branch;

import java.util.List;

import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;


public interface IBranchService {

    /**
     * if main branch, branch id must be provided and parent id must be null
     * 
     * @param branch
     * @param context
     * @return
     */
	IBranch createBranch(IBranch branch, EntityContext context);
	
	/**
	 * get a branch
	 * 
	 * @param repoName
	 * @param branchId
	 * @param context
	 * @return
	 */
	IBranch getBranch(String repoName, String branchId, EntityContext context);
	
	/**
     * List all main branches in this repo
     * 
     * @param repoName
     * @param context
     * @return
     */
    List<IBranch> getMainBranches(String repoName, EntityContext context);
	
	/**
	 * 
	 * @param context
	 */
	void ensureIndex(String repoName, List<MetaClass> metadatas, EntityContext context);
	
	/**
	 * Clean the collections when given metadata is deleted
	 * 
	 * @param repoName
	 * @param metadata
	 * @param context
	 */
	void deleteMetadata(String repoName, MetaClass metadata, EntityContext context);
}
