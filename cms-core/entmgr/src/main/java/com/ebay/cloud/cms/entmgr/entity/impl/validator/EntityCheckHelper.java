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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.CallbackContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.ModifyAction;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback.Operation;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.utils.StringUtils;

public class EntityCheckHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCheckHelper.class);
    
    private static final int LENGTH_OF_MONGO_OBJECT_ID = 24;
    
    private final IRepositoryService repositoryService;
    private final IPersistenceService persistenceService;
    
    public EntityCheckHelper(IRepositoryService repoService, IPersistenceService persistenceService) {
        this.repositoryService = repoService;
        this.persistenceService = persistenceService;
    }

    public void checkNoInnerInMeta(MetaClass meta) {
        for (MetaRelationship ref : meta.getToReference()) {
            if (ref.getRelationType() == RelationTypeEnum.Inner) {
                throw new CmsEntMgrException(EntMgrErrCodeEnum.META_CONTAINS_INNER_RELATIONSHIP, MessageFormat.format(
                        "Meta Class {0} contains inner relationship {1}", meta.getName(), ref.getName()));
            } else if (ref.getRelationType() == RelationTypeEnum.Embedded) {
                checkNoInnerInMeta(ref.getRefMetaClass());
            }
        }
    }
    
    public void checkVersion(IEntity entity, IEntity foundEntity) {
        if (foundEntity == null) {
            return;
        }

        if (!entity.hasField(InternalFieldEnum.VERSION.getName())) {
            entity.setVersion(foundEntity.getVersion());
        } else {
            Integer ver = (Integer) entity.getFieldValues(InternalFieldEnum.VERSION.getName()).get(0);
            int version = ver.intValue();
            int currentVersion = foundEntity.getVersion();
            if (version != currentVersion) {
                throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, "current version is " + currentVersion
                        + ", but version in request body is " + version  + "! entity is " + entity.toString());
            }
        }
    }
    
    public void checkDocumentSize(Operation op, IEntity entity, IEntity foundEntity, Long maxDocumentSize) {
        String type = entity.getType();
        String oid = entity.getId();
        long foundEntitySize = (foundEntity == null) ? 0 : foundEntity.getEntitySize();
        if (foundEntitySize > maxDocumentSize && !op.equals(Operation.REPLACE)) {
            String errorMessage = String.format(
                    "Exceed max document size! Max is %d Bytes, Actual found entity size is %d Bytes. Entity type is %s and oid is %s.", maxDocumentSize,
                    foundEntitySize, type, oid);
            LOGGER.info(errorMessage);
            throw new CmsDalException(DalErrCodeEnum.EXCEED_MAX_DOCUMENT_SIZE, errorMessage);
        }
        long payLoadSize = entity.getEntitySize();
        if (payLoadSize > maxDocumentSize) {
            String errorMessage = String.format(
                    "Exceed max document size! Max is %d Bytes, Actual payload size is %d Bytes. Entity type is %s and oid is %s.", maxDocumentSize,
                    payLoadSize, entity.getType(), oid);
            LOGGER.info(errorMessage);
            throw new CmsDalException(DalErrCodeEnum.EXCEED_MAX_DOCUMENT_SIZE, errorMessage);
        }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })    
    public IEntity getMergedEntity(IEntity givenEntity, IEntity foundEntity, EntityContext context, ModifyAction modifyAction) {
        IEntity finalEntity = new JsonEntity((JsonEntity) foundEntity);
        MetaClass metaClass = givenEntity.getMetaClass();
        if (ModifyAction.MODIFYENTITY.equals(modifyAction)) {
            Collection<String> currentEntityFieldNames = givenEntity.getFieldNames();
            for (String currentEntityFieldName : currentEntityFieldNames) {
                MetaField metaField = metaClass.getFieldByName(currentEntityFieldName);
                if (metaField != null) {
                    List fieldValues = finalEntity.getFieldValues(currentEntityFieldName);
                    if (fieldValues != null && !fieldValues.isEmpty()) {
                        finalEntity.removeField(currentEntityFieldName);
                    }
                    finalEntity.setFieldValues(currentEntityFieldName, givenEntity.getFieldValues(currentEntityFieldName));
                }
            }
        } else if (ModifyAction.PUSHFIELD.equals(modifyAction)) {
            String fieldName = context.getModifyFieldName();
            MetaField metaField = metaClass.getFieldByName(fieldName);
            if (metaField != null) {
                if (CardinalityEnum.One.equals(metaField.getCardinality())) {
                    // replace field value
                    finalEntity.removeField(fieldName);
                    finalEntity.setFieldValues(fieldName, givenEntity.getFieldValues(fieldName));
                } else {
                    // append given values
                    List finalValues = finalEntity.getFieldValues(fieldName);
                    List currentValues = givenEntity.getFieldValues(fieldName);
                    if (!currentValues.isEmpty()) {
                        if (finalValues.size() == 0) {
                            finalValues = new ArrayList(currentValues.size());
                        }
                        finalValues.addAll(currentValues);
                        finalEntity.setFieldValues(fieldName, finalValues);
                    }
                }
            }
        } 
        
        return finalEntity;
    }
    
    public void checkIndexedArraySizeForHostEntity(Operation op, Integer maxIndexedArraySize, IEntity hostEntity, String fieldName) {
        MetaClass hostMetaClass = hostEntity.getMetaClass();
        if(!hostMetaClass.getIndexesOnField(fieldName).isEmpty()) {
            List<?> currentHostEntityFieldValues = hostEntity.getFieldValues(fieldName);
            long currentHostEntityFieldValuesSize = currentHostEntityFieldValues.size();
            long targetHostEntityFieldValuesSize = 0;
            if (Operation.CREATE.equals(op)) {
                targetHostEntityFieldValuesSize = currentHostEntityFieldValuesSize + 1;
            } else {
                targetHostEntityFieldValuesSize = currentHostEntityFieldValuesSize;
            } 
            if (targetHostEntityFieldValuesSize > maxIndexedArraySize) {
                String errorMessage = String.format("Exceed max indexed array size on metafield %s of metaclass %s! Max is %d, Actual is %d",
                        fieldName, hostMetaClass.getName(), maxIndexedArraySize, targetHostEntityFieldValuesSize);
                throw new CmsDalException(DalErrCodeEnum.EXCEED_MAX_INDEXED_ARRAY_SIZE, errorMessage);
            }
        }
    }
    
    public String getEmbedEntityId(IEntity entity, EntityContext context) {
        String currentId = entity.getId();
        String embedPath = context.getPath();
        String embedEntityId = null;
        if (AbstractEntityIDHelper.isEmbedEntity(currentId)) {
            embedEntityId = currentId;
        } else {
            if (embedPath != null) {
                if (currentId == null) {
                    embedEntityId = AbstractEntityIDHelper.generateEmbedIdByEmbedPath(
                            embedPath, getDummyEntityId());
                } else {
                    embedEntityId = AbstractEntityIDHelper.generateEmbedIdByEmbedPath(embedPath, currentId);
                }
            } else {
                throw new CmsDalException(DalErrCodeEnum.STANDALONE_EMBED, String.format(
                        "Could not create standalone entity for embed class %s!", entity.getType()));
            }
        }
        if (!AbstractEntityIDHelper.isEmbedEntity(embedEntityId)) {
            throw new CmsDalException(DalErrCodeEnum.INVALID_EMBED_ID, String.format("Embed id %s is invalid!", embedEntityId));
        }
        return embedEntityId;
    }
    
    private String getDummyEntityId() {
        return generateRandomString(LENGTH_OF_MONGO_OBJECT_ID);
    }
    
    private String generateRandomString(int length) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }

    public Repository getRepository(PersistenceContext persistenceContext) {
        return persistenceContext.getMetadataService().getRepository();
    }
    

    private void checkACLInternal(IEntityOperationCallback callback, IEntity aclEntity, IEntity existingEntity, Operation operation, EntityContext context) {
        CallbackContext callbackContext = new CallbackContext(context.getModifier(), context.getComment(),
                context.getRequest());
        boolean checkResult = callback.preOperation(existingEntity, operation, aclEntity, callbackContext);
        if (!checkResult) {
            throw new CmsEntMgrException(EntMgrErrCodeEnum.OPERATION_CHECK_FAILED, MessageFormat.format(
                    "Operation check failed for entity {0}, type {1}. This is possibly an access control setting.",
                    aclEntity.getId(), aclEntity.getType()));
        }
    }

    public void checkACL(IEntityOperationCallback callback, IEntity newEntity, IEntity existingEntity, String fieldName, IBranch branchEntity,
            Operation operation, EntityContext context, PersistenceContext persistenceContext) {
        if (callback != null) {
            Operation oper = operation;
            IEntity aclEntity = newEntity;
            IEntity foundEntity = existingEntity;
            String entityId = aclEntity.getId();

            if (AbstractEntityIDHelper.isEmbedEntity(entityId)) {
                oper = Operation.MODIFY;
                 
                aclEntity = generateACLCheckEntity(newEntity, existingEntity, fieldName);
                String rootId = AbstractEntityIDHelper.getRootId(entityId);
                String rootType = AbstractEntityIDHelper.getRootEntityType(entityId);

                IMetadataService metaService = repositoryService.getRepository(newEntity.getRepositoryName())
                        .getMetadataService();
                MetaClass meta = metaService.getMetaClass(rootType);
                IEntity queryEntity = new JsonEntity(meta);
                queryEntity.setBranchId(newEntity.getBranchId());
                queryEntity.setId(rootId);

                foundEntity = getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);
            }
            checkACLInternal(callback, aclEntity, foundEntity, oper, context);
        }
    }
    
    public IEntity getAndCheckExsitingEntity(IEntity queryEntity, IBranch branchEntity,
            PersistenceContext persistenceContext) {
        
        String currentBranchId = branchEntity.getId();
        queryEntity.setBranchId(currentBranchId);
        IEntity foundEntity = persistenceService.get(queryEntity, persistenceContext);
        
        if (foundEntity == null) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, String.format(
                    "entity %s does not exist in main branch %s", queryEntity.toString(), branchEntity.getId()));
        } else if (foundEntity.getStatus() != StatusEnum.ACTIVE) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_ACTIVE, String.format(
                    "entity %s is not active in main branch %s", queryEntity.toString(), branchEntity.getId()));
        }
        return foundEntity;
    }

    public String[] splitPath(String path, boolean check) {
        if (!StringUtils.isNullOrEmpty(path)) {
            String[] idParts = path.split(IEntity.ID_SEP_REG);
            if (idParts.length % 2 == 1) {
                return idParts;
            }
        }
        if (check) {
            throw new CmsEntMgrException(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, MessageFormat.format(
                    "invalid inner class path {0}", path));
        } else {
            return new String[0];
        }
    }
    
    private IEntity generateACLCheckEntity(IEntity newEntity, IEntity existingEntity, String fieldName) {
        IEntity genEntity = null;
        IMetadataService metaService = repositoryService.getRepository(newEntity.getRepositoryName())
                .getMetadataService();
        String[] idParts = newEntity.getId().split(IEntity.ID_SEP_REG);
        StringBuffer id = new StringBuffer();
        IEntity parentEntity = null;
        MetaClass meta = null;
        IEntity entity = null;
        for (int i = 0; i < idParts.length; i += 2) {
            String name = idParts[i];
            if (i == 0) {
                id.append(idParts[i]).append(IEntity.ID_SEP).append(idParts[i + 1]);
                meta = metaService.getMetaClass(name);
                genEntity = new JsonEntity(meta);
                genEntity.setId(idParts[i + 1]);
                parentEntity = genEntity;
            } else {
                id.append(IEntity.ID_SEP).append(idParts[i]).append(IEntity.ID_SEP).append(idParts[i + 1]);
                MetaRelationship rel = (MetaRelationship) meta.getFieldByName(name);
                meta = rel.getRefMetaClass();
                entity = new JsonEntity(meta);
                entity.setId(id.toString());
                parentEntity.addFieldValue(name, entity);
                parentEntity = entity;
            }
        }

        if (fieldName != null) {
            List<?> values = newEntity.getFieldValues(fieldName);
            
            MetaField field = newEntity.getMetaClass().getFieldByName(fieldName);
            if (values.isEmpty() && field.getCardinality() == CardinalityEnum.One) {
                entity.setFieldValues(fieldName, existingEntity.getFieldValues(fieldName));
            } else {
                entity.setFieldValues(fieldName, values);
            }
        }

        return genEntity;
    }
    
    public void checkOperation(IEntity en, String repoName, String branchId) {
        if (!repoName.equals(en.getRepositoryName())) {
            throw new CmsDalException(CmsDalException.DalErrCodeEnum.REPOSITORYNAME_INCONSISTENCY,
                    "repository name must be the same for batch operations");
        }

        if (!branchId.equals(en.getBranchId())) {
            throw new CmsDalException(CmsDalException.DalErrCodeEnum.BRANCHID_INCONSISTENCY,
                    "branch id must be the same for batch operations");
        }
    }
    
	public void preFieldOperationCheck(IEntity entity, String fieldName, boolean isModifyField) {
		MetaField mField = entity.getMetaClass().getFieldByName(fieldName);
		if (mField instanceof MetaRelationship) {
			MetaRelationship rel = (MetaRelationship) mField;
			if (rel.getRelationType() == RelationTypeEnum.Inner) {
				throw new CmsEntMgrException(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE, MessageFormat.format(
						"inner relationship {0} is immutable", fieldName));
			}
			// modify field doesn't allowed on embed relationship
			// FIXME: should we disallow the "delete field" on embed
			// relationship?
			if (rel.getRelationType() == RelationTypeEnum.Embedded && isModifyField) {
				throw new CmsEntMgrException(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, MessageFormat.format(
						"embed relationship {0} is immutable", fieldName));
			}
		}
	}

}
