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

package com.ebay.cloud.cms.dal.entity.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.utils.StringUtils;

/**
 * validate the full content of bson entity based on metadata
 * 
 * @author xjiang
 *
 */
@SuppressWarnings("static-access")
public class FullBsonValidator extends AbstractBsonValidator {
    
    private final boolean isNew;
    private String rootMetaType = null;
    
    public FullBsonValidator(boolean isNew, AbstractEntityIDHelper helper) {
        super(helper);
        this.isNew = isNew;
    }
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        visitTime = new Date();
        return currentEntity.getMetaClass().getFieldNames();
    }
    
    @Override
    protected void processUserAttribute(BsonEntity currentEntity, MetaField metaField) {
        MetaAttribute metaAttr = (MetaAttribute) metaField;

        // apply default value from metaclass definition if any
        applyDefaultValue(currentEntity, metaAttr);

        super.processUserAttribute(currentEntity, metaField);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyDefaultValue(BsonEntity currentEntity, MetaAttribute metaField) {
        String fieldName = metaField.getName();
        Object defaultValue = metaField.getValidatedDefaultValue();
        List fieldValue = currentEntity.getFieldValues(fieldName);
        if (fieldValue.isEmpty() && defaultValue != null) {
            fieldValue = new ArrayList();
            fieldValue.add(defaultValue);
            currentEntity.setFieldValues(fieldName, fieldValue);
            // use current entity's last modified time if any when apply defualt value to field
            if (currentEntity.getLastModified() != null) {
                currentEntity.setFieldTimestamp(fieldName, currentEntity.getLastModified());
            }
        }
    }
    
    @Override
    protected void processId(BsonEntity parentEntity,
            MetaRelationship refField, BsonEntity currentEntity) {
        if (!isNew) {
            String currentId = currentEntity.getId();
            if (parentEntity == null && currentEntity.getId() == null) {
                // replace must have id set.
                throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Can not find the entity for operating!");
            }
            
            if(parentEntity!=null && helper.isEmbedEntity(currentId) && !parentEntity.getId().equals(helper.getParentId(currentId))){
                throw new CmsDalException(DalErrCodeEnum.INVALID_EMBED_ID, String.format("Embed id %s is invalid!", currentId));
            }
        }

        //2012/8/30 jianxu1: to better support CMS sync, we enable specify ID when create new entity
        String currentId = null;
        boolean noValidId = !currentEntity.hasField(InternalFieldEnum.ID.getName()) || StringUtils.isNullOrEmpty(currentEntity.getId());
        if (noValidId) {
        	currentId = ObjectId.get().toString();
        } else {
        	currentId = currentEntity.getId();
        }

        if(parentEntity == null){
            //for root document
        	//parent entity == null means the new created entity does not have parent entity yet before it's persisted
        	//e.g. I want to create a new ManifestApproval under ManifestVersion, before Approval instance is persisted
        	//it does not have parent entity, but in order to tell CMS where to persist the new Approval instance,
        	//embedPath of that new entity will be set
            rootMetaType = currentEntity.getType();
            String embedPath = currentEntity.getEmbedPath();
            if (embedPath != null && (!AbstractEntityIDHelper.isEmbedEntity(currentId))) {
                currentId = helper.generateEmbedIdByEmbedPath(embedPath, currentId);               
                rootMetaType = helper.getRootEntityType(embedPath);
            }
        }else{
            String parentId = parentEntity.getId();
            if (!helper.isEmbedEntity(currentId)) {
                currentId = helper.generateEmbedId(rootMetaType, parentId, currentId, refField);
            } else if (!parentId.equals(helper.getParentId(currentId))) {
            	throw new CmsDalException(DalErrCodeEnum.INVALID_EMBED_ID, String.format("Embed id %s is invalid!", currentId));
            }
        }
        //2012/7/12 jianxu1 validate id before setId, in data loader from data file, we should check if handwritten id in data file is valid
        helper.validateId(currentId);
        currentEntity.setId(currentId);
    }

    @Override
    protected void processVersion(BsonEntity parentEntity,
            MetaRelationship refField, BsonEntity currentEntity) {   
        if(parentEntity == null && !currentEntity.getMetaClass().isEmbed()) {
            // only set version on root entity
            if(!currentEntity.hasField(InternalFieldEnum.VERSION.getName())) {
                if (isNew) {
                    currentEntity.addFieldValue(InternalFieldEnum.VERSION.getName(), IEntity.START_VERSION);
                }
                else {
                    currentEntity.addFieldValue(InternalFieldEnum.VERSION.getName(), IEntity.NO_VERSION);
                }
            } else {
                int v = currentEntity.getVersion();
                if (isNew) {
                    if (v != IEntity.START_VERSION) {
                        throw new CmsDalException(DalErrCodeEnum.INVALID_VERSION, "Version must be " + IEntity.START_VERSION + " for new entity");
                    }
                } else {
                    if (v < IEntity.START_VERSION) {
                        throw new CmsDalException(DalErrCodeEnum.INVALID_VERSION, "invalid version: " + IEntity.START_VERSION);
                    }
                }
            }            
        } else if (refField != null && refField.getRelationType() == RelationTypeEnum.Embedded) {
            // remove version from embed entity
            currentEntity.removeField(InternalFieldEnum.VERSION.getName());
        }
    }

	@Override
	protected void processParentVersion(BsonEntity parentEntity,
			MetaRelationship refField, BsonEntity currentEntity) {
		//only set parent version on root entity for new entity
		//for replace entity, isNew is false, meaning given entity must already has parent version set
        if(parentEntity == null  && !helper.isEmbedEntity(currentEntity.getId())) {
        	if(isNew){
        		currentEntity.addFieldValue(InternalFieldEnum.PVERSION.getName(), IEntity.NO_VERSION);
            } else {
                if (!currentEntity.hasField(InternalFieldEnum.PVERSION.getName())) {
                    throw new CmsDalException(DalErrCodeEnum.MISS_RUNTIME_FIELD, "Miss Parent Version");
                }
            }
        } else if ((refField != null && refField.getRelationType() == RelationTypeEnum.Embedded)
                || helper.isEmbedEntity(currentEntity.getId())) {
            // remove version from embed entity
            currentEntity.removeField(InternalFieldEnum.PVERSION.getName());
        }
		
	}

	@Override
	protected void processStatus(BsonEntity parentEntity,
			MetaRelationship refField, BsonEntity currentEntity) {
		if(isNew){
			currentEntity.setStatus(StatusEnum.ACTIVE);
		}else{
    		if(!currentEntity.hasField(InternalFieldEnum.STATUS.getName())){
    		    currentEntity.setStatus(StatusEnum.ACTIVE);
    		}
		}
	}

    @Override
    protected void processCreateTime(BsonEntity parentEntity,
            MetaRelationship refField, BsonEntity currentEntity) {
        if (isNew || !currentEntity.hasField(InternalFieldEnum.CREATETIME.getName())) {
            currentEntity.setCreateTime(visitTime);
        } 
    }

    @Override
    protected void processCreator(BsonEntity parentEntity, MetaRelationship refField, BsonEntity currentEntity) {
        if (!currentEntity.hasField(InternalFieldEnum.CREATOR.getName()) && parentEntity != null) {
            currentEntity.setCreator(parentEntity.getCreator());
        }
    }
    
    @Override
    protected void processModifier(BsonEntity parentEntity, MetaRelationship refField, BsonEntity currentEntity) {
        if (!currentEntity.hasField(InternalFieldEnum.MODIFIER.getName()) && parentEntity != null) {
            currentEntity.setModifier(parentEntity.getModifier());
        }
    }

}
