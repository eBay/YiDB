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

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.ModifyAction;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback.Operation;
import com.ebay.cloud.cms.entmgr.entity.impl.EntityGetManager;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

public class SystemLimitationHelper {
	private final EntityCheckHelper entityHelper;
	private final BranchHelper branchHelper;
	private final EntityGetManager entityGetManager;
	
	public SystemLimitationHelper(EntityCheckHelper helper,
			IBranchService branchService, IRepositoryService repoService,
			IPersistenceService persistenceService) {
		this.entityHelper = helper;
		this.branchHelper = new BranchHelper(branchService);
		this.entityGetManager = new EntityGetManager(repoService,
				persistenceService, branchService);
	}
	
	public void checkSystemLimitation(IEntity entity, IEntity foundEntity,
			EntityContext context, PersistenceContext persistenceContext,
			Operation op) {
		if (!Operation.DELETE.equals(op)) {
			if (Operation.MODIFY.equals(op)) {
				ModifyAction modifyAction = context.getModifyAction();
				if (modifyAction != null
						&& (ModifyAction.DELETEFIELD.equals(modifyAction) || ModifyAction.PULLFIELD
								.equals(modifyAction))) {
					return;
				}
			}
			Repository repo = entityHelper.getRepository(persistenceContext);
			RepositoryOption options = repo.getOptions();
			Long maxDocumentSize = options.getMaxDocumentSize();
			if (maxDocumentSize != null) {
				entityHelper.checkDocumentSize(op, entity, foundEntity,
						maxDocumentSize);
			}

			Integer maxIndexedArraySize = options.getMaxIndexedArraySize();
			if (maxIndexedArraySize != null) {
				checkIndexedArraySize(entity, foundEntity, context,
						persistenceContext, op, maxIndexedArraySize);
			}
		}
	}

	private void checkIndexedArraySize(IEntity entity, IEntity foundEntity,
			EntityContext context, PersistenceContext persistenceContext,
			Operation op, Integer maxIndexedArraySize) {
		IEntity finalEntity = new JsonEntity((JsonEntity) entity);
		if (Operation.MODIFY.equals(op)) {
			ModifyAction modifyAction = context.getModifyAction();
			finalEntity = entityHelper.getMergedEntity(finalEntity, foundEntity,
					context, modifyAction);
		}

		// check indexed array size of entity self
		EntityValidator entityVisitor = new EntityValidator(entity);
		IndexedArraySizeCheckAction indexedArraySizeCheckAction = new IndexedArraySizeCheckAction(
				maxIndexedArraySize);
		entityVisitor.addAction(indexedArraySizeCheckAction);
		finalEntity.traverse(entityVisitor);

		// check indexed array size of host entity if need
		MetaClass metaClass = finalEntity.getMetaClass();
		IEntity refEntity = null;
		if (Operation.CREATE.equals(op)) {
			refEntity = entity;
		} else {
			refEntity = foundEntity;
		}
		if (metaClass.isInner()) {
			IEntity hostEntity = getUnMergedHostEntity(refEntity, context,
					persistenceContext);
			if (hostEntity != null) {
				String path = refEntity.getHostEntity();
				String[] idParts = path.split(IEntity.ID_SEP_REG);
				String hostFieldName = idParts[idParts.length - 1];
				entityHelper.checkIndexedArraySizeForHostEntity(op,
						maxIndexedArraySize, hostEntity, hostFieldName);
			} else {
				throw new CmsDalException(
						DalErrCodeEnum.ENTITY_NOT_FOUND,
						String
								.format(
										"Could not find host entity for inner class %s!",
										metaClass.getName()));
			}
		} else if (metaClass.isEmbed()) {
			IEntity parentEntity = getUnMergedParentEntityForEmbed(refEntity,
					context, persistenceContext);
			if (parentEntity != null) {
				String embedId = entityHelper.getEmbedEntityId(refEntity,
						context);
				String parentFieldName = AbstractEntityIDHelper
						.getParentFieldName(embedId);
				entityHelper.checkIndexedArraySizeForHostEntity(op,
						maxIndexedArraySize, parentEntity, parentFieldName);
			} else {
				throw new CmsDalException(
						DalErrCodeEnum.ENTITY_NOT_FOUND,
						String
								.format(
										"Could not find parent entity for embed class %s!",
										metaClass.getName()));
			}
		}
	}

	private IEntity getUnMergedHostEntity(IEntity entity,
			EntityContext context, PersistenceContext persistenceContext) {
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId,
				context);

		String host = entity.getHostEntity();
		if (StringUtils.isNullOrEmpty(host)) {
			host = context.getPath();
			entity.setHostEntity(host);
		}

		String[] idParts = entityHelper.splitPath(host, true);
		IEntity queryEntity = entityGetManager.getHostEntity(entity, host, idParts,
				persistenceContext, true);
		return entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity,
				persistenceContext);
	}

	private IEntity getUnMergedParentEntityForEmbed(IEntity entity,
			EntityContext context, PersistenceContext persistenceContext) {
		String embedId = entityHelper.getEmbedEntityId(entity, context);
		IMetadataService metaService = persistenceContext.getMetadataService();

		// get parent entity
		MetaClass parentMetaClass = AbstractEntityIDHelper.getParentMetaClass(
				embedId, metaService);
		String parentId = AbstractEntityIDHelper.getParentId(embedId);

		CheckConditions.checkNotNull(parentMetaClass);
		JsonEntity queryEntity = new JsonEntity(parentMetaClass);
		queryEntity.setId(parentId);

		queryEntity.setBranchId(entity.getBranchId());
		queryEntity.setEmbedPath(parentId);

		return entityGetManager.get(queryEntity, context);
	}
}
