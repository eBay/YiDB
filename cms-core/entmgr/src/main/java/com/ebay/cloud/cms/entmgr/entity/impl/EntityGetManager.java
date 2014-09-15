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
package com.ebay.cloud.cms.entmgr.entity.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.PersistenceContextFactory;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.BranchHelper;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;

public class EntityGetManager {
	private final IRepositoryService repositoryService;
	private final IPersistenceService persistenceService;
	private final BranchHelper branchHelper;
	
	public EntityGetManager(IRepositoryService repoService, IPersistenceService persistenceService,
			IBranchService branchService){
		this.repositoryService = repoService;
		this.persistenceService = persistenceService;
		this.branchHelper = new BranchHelper(branchService);
	}
	
	private Collection<String> getQueryFields(MetaClass meta, Collection<String> queryFields) {
		if (queryFields.isEmpty()) {
			return queryFields;
		}
		Collection<String> dbFieldNames = new ArrayList<String>();
		for (String fieldName : queryFields) {
			MetaField field = meta.getFieldByName(fieldName);
			if (field == null) {
				throw new CmsEntMgrException(EntMgrErrCodeEnum.FIELD_NOT_FOUND, MessageFormat.format(
						"field {0} not found", fieldName));
            }
            // add both db name and flatten in project. no side affect
            dbFieldNames.add(field.getDbName());
            dbFieldNames.add(field.getFlattenValueDbName());
		}
		dbFieldNames.add(InternalFieldEnum.ID.getDbName());
		dbFieldNames.add(InternalFieldEnum.TYPE.getDbName());
		return dbFieldNames;
	}
	
    private PersistenceContext getReadPersistenceContext(String repoName, EntityContext context, IBranch branchEntity) {
        Repository repo = repositoryService.getRepository(repoName);
        PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
                repo.getMetadataService(), branchEntity, context.getConsistentPolicy(), context.getRegistration(),
                context.isFetchFieldProperty(), context.getDbConfig(), context.getAdditionalCriteria());
        return persistenceContext;
    }
	
	public IEntity get(IEntity queryEntity, EntityContext context) {
		String repoName = queryEntity.getRepositoryName();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, queryEntity.getBranchId(), context);
		PersistenceContext persistenceContext = getReadPersistenceContext(repoName, context, branchEntity);

		MetaClass queryMeta = queryEntity.getMetaClass();
		persistenceContext.addQueryFields(getQueryFields(queryMeta, context.getQueryFields()));
		IEntity findEntity = findAndCopy(branchEntity, queryEntity, persistenceContext);

		if (findEntity == null || findEntity.getStatus() != StatusEnum.ACTIVE) {
			return null;
		}
        return findEntity;
	}
	
	public IEntity getHostEntity(IEntity entity, String host, String[] idParts,
			PersistenceContext persistenceContext, boolean check) {
		if (!(idParts.length > 1)) {
			// never be true for check=true case
			return null;
		}
		
		IMetadataService metaService = persistenceContext.getMetadataService();
		
		MetaClass hostMeta = null;
	    String hostId = idParts[1];
		if (idParts.length > 3) {
		    int pos = host.lastIndexOf(IEntity.ID_SEP);		    
		    hostId = host.substring(0, pos);
		    
		    MetaClass meta = metaService.getMetaClass(idParts[0]);
		    for (int i = 2; i < idParts.length - 1; i += 2) {
		        String name = idParts[i];
		        MetaRelationship field = (MetaRelationship)meta.getFieldByName(name);
		        meta = field.getRefMetaClass();
		    }
		    hostMeta = meta;
		} else {
		    hostMeta = metaService.getMetaClass(idParts[0]);
		}
		
		if (hostMeta == null) {
			if (check) {
				throw new CmsEntMgrException(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, MessageFormat.format(
						"could not find host Meta Class in path {0}", entity.getHostEntity()));
			} else {
				return null;
			}
		}

        IEntity qEntity = new JsonEntity(hostMeta);
		qEntity.setBranchId(entity.getBranchId());
		qEntity.setId(hostId);
		return qEntity;
	}
	
	public IEntity findAndCopy(IBranch branch, IEntity rootQueryEntity,
			PersistenceContext persistenceContext) {
		String currentBranchId = branch.getId();
		rootQueryEntity.setBranchId(currentBranchId);
		return persistenceService.get(rootQueryEntity, persistenceContext);
	}
}
