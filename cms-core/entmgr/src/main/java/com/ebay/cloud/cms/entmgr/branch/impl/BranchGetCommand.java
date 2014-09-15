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

package com.ebay.cloud.cms.entmgr.branch.impl;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;

public class BranchGetCommand extends AbstractBranchCommand implements IBranchCommand {

	private final String branchId;
	private Branch branchGet;
	private IPersistenceService persistenceService;
	
	public BranchGetCommand(IPersistenceService persistenceService, String branchId){
		this.branchId = branchId;
        this.persistenceService = persistenceService;
	}
	
	@Override
    public void execute(PersistenceContext persistenceContext) {
	    MetaClass branchMeta = persistenceContext.getMetaClass(BranchMetaClass.TYPE_NAME);
	    IEntity queryEntity = persistenceContext.getEntityFactory().createEntity(branchMeta);
	    queryEntity.setBranchId(BranchMetaClass.BRANCH_ID);
	    queryEntity.setId(branchId);
        IEntity bsonEntity = persistenceService.get(queryEntity, persistenceContext);
        if (bsonEntity != null) {
            Branch branch = toBranch(bsonEntity);
            branchGet = branch;
        } 
    }
	
	public Branch getBranch(){
		return branchGet;
	}
}
