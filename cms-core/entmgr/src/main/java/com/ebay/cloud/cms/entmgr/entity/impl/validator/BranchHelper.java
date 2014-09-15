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
package com.ebay.cloud.cms.entmgr.entity.impl.validator;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;

public class BranchHelper {
	private final IBranchService branchService;
	
	public BranchHelper(IBranchService branchService){
		this.branchService = branchService;
	}
	
	public IBranch getAndCheckCurrentBranch(String repoName, String branchId, EntityContext context) {
		IBranch branchEntity = branchService.getBranch(repoName, branchId, context);
		if (branchEntity == null) {
			throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND, "Branch not found: "
					+ branchId);
		}
		return branchEntity;
	}
}
