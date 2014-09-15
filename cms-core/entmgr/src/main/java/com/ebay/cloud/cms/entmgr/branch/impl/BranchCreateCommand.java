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

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.StringUtils;

public class BranchCreateCommand extends AbstractBranchCommand implements IBranchCommand {

	private final Branch branch;
	private String newBranchId;
	private final IRepositoryService repositoryService;
	private final IPersistenceService persistenceService;
	private final EntityContext entityContext;
	
	public BranchCreateCommand(IBranch branch, IRepositoryService repoService, 
			IPersistenceService persistenceService,
			EntityContext context) {
		this.branch = new Branch(branch);
		this.repositoryService = repoService;
		this.persistenceService = persistenceService;
		this.entityContext = context;
	}
		
	private void validateInput(IBranch branch, PersistenceContext persistenceContext) {
	    String branchId = branch.getId();
	    
	    if (branch.isMainBranch()) {
            if (branchId == null) {
                throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.ILLEGAL_BRANCH_ENTITY, "Must provide id to create main branch");
            }

            if (getBranchFromDB(branchId, persistenceContext) != null) {
                throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_EXISTS, "Branch already exists");
            }
	    } else {
            throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.ILLEGAL_BRANCH_ENTITY, "Create sub-branch is not allowed");
	    }
	}
	
	private Branch getBranchFromDB(String branchId, PersistenceContext persistenceContext) {
		BranchGetCommand getCommand = new BranchGetCommand(persistenceService, branchId);
        getCommand.execute(persistenceContext);
        
        return getCommand.getBranch();
	}
	
	
	private IEntity createBsonEntity(Branch branch, PersistenceContext persistenceContext) {
		MetaClass branchMeta = persistenceContext.getMetaClass(BranchMetaClass.TYPE_NAME);
        IEntity bsonEntity = toBson(branch, branchMeta, persistenceContext);

        if (StringUtils.isNullOrEmpty(entityContext.getModifier())) {
            bsonEntity.setCreator("system");
        } else {
            bsonEntity.setCreator(entityContext.getModifier());
        }

        return bsonEntity;
	}
	
	@Override
	public void execute(PersistenceContext persistenceContext) {
	    validateInput(branch, persistenceContext);

	    //this operation will reset the rootBranchId for branch and e
	    IEntity e = createBsonEntity(branch, persistenceContext);

	    //using 
	    newBranchId = persistenceService.create(e, persistenceContext);
	    
	    branch.setId(newBranchId);

	    //TODO: fix race condition here: error happens when a cms server is creating branch using cached metaclass
	    //      but another cmsserver already changed the index in db. 
	    //      A background thread to creating and deleting indexes also not work because client may depend on some "unique index" to avoid field confliction
       	prepareMainCollection(persistenceContext);
	}

	private void prepareMainCollection(PersistenceContext persistenceContext) {
        PersistenceContext branchPContext = PersistenceContextFactory.createEntityPersistenceConext(
                persistenceContext.getMetadataService(), branch.getId(), persistenceContext.getConsistentPolicy(),
                persistenceContext.getRegistration(), persistenceContext.isFetchFieldProperties(), persistenceContext.getDbConfig(),
                persistenceContext.getAdditionalCriteria());
		createIndexesOnCollection(persistenceService, branch.getRepositoryName(), true, branchPContext);
	}

    private void createIndexesOnCollection(IPersistenceService persistenceService, String repoName, boolean isMainBranch,
            PersistenceContext context) {
        Repository repo = repositoryService.getRepository(repoName);
        List<MetaClass> metasInRepo = repo.getMetadataService().getMetaClasses(new MetadataContext(true, true));

        if (metasInRepo.size() > 0) {
        	persistenceService.ensureIndex(metasInRepo, context, isMainBranch);
        }
    }

    public String getCreatedBranchId() {
		return newBranchId;
	}
    
}
