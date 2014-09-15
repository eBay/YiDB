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

package com.ebay.cloud.cms.dal.entity.flatten.visitor;

import java.util.Collection;
import java.util.Date;

import org.bson.types.ObjectId;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

/**
 * validate the partial content of bson entity based on runtime data
 * 
 * @author xjiang
 * 
 */
@SuppressWarnings("static-access")
public class PartialBsonValidator extends AbstractBsonValidator {

    public PartialBsonValidator(FlattenEntityIDHelper helper) {
        super(helper);
    }

    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        visitTime = new Date();
        Collection<String> fieldNames = currentEntity.getFieldNames();
        fieldNames.add(InternalFieldEnum.ID.getName());
        fieldNames.add(InternalFieldEnum.LASTMODIFIED.getName());
        fieldNames.add(InternalFieldEnum.VERSION.getName());
        fieldNames.add(InternalFieldEnum.MODIFIER.getName());
        fieldNames.add(InternalFieldEnum.COMMENT.getName());
        fieldNames.add(InternalFieldEnum.USER.getName());
        fieldNames.add(InternalFieldEnum.STATUS.getName());
        fieldNames.add(InternalFieldEnum.CREATETIME.getName());
        fieldNames.add(InternalFieldEnum.BRANCH.getName());
        fieldNames.add(InternalFieldEnum.CREATOR.getName());
        fieldNames.add(InternalFieldEnum.HOSTENTITY.getName());
        fieldNames.add(InternalFieldEnum.METAVERSION.getName());
        
        if (checkMandatory) {
            Collection<MetaField> fields = currentEntity.getMetaClass().getFields();
            for (MetaField field : fields) {
                if (field.isMandatory()) {
                    fieldNames.add(field.getName());
                }
            }
        }
        
        return fieldNames;
    }

    @Override
    protected void processId(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        if (parentEntity != null) {
            // embed entity two cases: no embed id or user set embed id that
            // doesn't conform to the embed id
            // how to detect new entity? - Using internal embed id encoding to
            // parse,
            // if matched, we think the current entity is already existing
            String parentId = parentEntity.getId();
            String rootType = parentEntity.getMetaClass().getName();
            if (helper.isEmbedEntity(parentId)) {
                // nested embed case:
                rootType = helper.getRootEntityType(parentId);
            }

            String curId = currentEntity.getId();
            if (!currentEntity.hasField(InternalFieldEnum.ID.getName())) {
                curId = ObjectId.get().toString();
            } else {
                curId = currentEntity.getId();
            }

            if (!helper.isEmbedEntity(curId)) {
                currentEntity.setId(helper.generateEmbedId(rootType, parentId, curId, refField));
            } else if (!parentId.equals(helper.getParentId(curId))) {
                throw new CmsDalException(DalErrCodeEnum.INVALID_EMBED_ID, String.format("Embed id %s is invalid!", curId));
            }
        }
    }

    @Override
    protected void processVersion(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        // remove version on embedded object
        if (parentEntity != null) {
            currentEntity.removeField(InternalFieldEnum.VERSION.getName());
        } else {
            if (currentEntity.hasField(InternalFieldEnum.VERSION.getName())) {
                int v = currentEntity.getVersion();
                if (v < IEntity.START_VERSION) {
                    throw new CmsDalException(DalErrCodeEnum.INVALID_VERSION, "invalid version: "
                            + IEntity.START_VERSION);
                }
            } else {
                currentEntity.addFieldValue(InternalFieldEnum.VERSION.getName(), IEntity.NO_VERSION);
            }
        }
    }

    @Override
    protected void processParentVersion(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        // remove version on embedded object
        if (parentEntity != null || helper.isEmbedEntity(currentEntity.getId())) {
            currentEntity.removeField(InternalFieldEnum.PVERSION.getName());
        }
    }

    @Override
    protected void processStatus(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        if (!currentEntity.hasField(InternalFieldEnum.STATUS.getName())) {
            currentEntity.setStatus(StatusEnum.ACTIVE);
        }
    }

    @Override
    protected void processCreateTime(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        if (!currentEntity.hasField(InternalFieldEnum.CREATETIME.getName())) {
            currentEntity.setCreateTime(visitTime);
        }
    }

    @Override
    protected void processUserAttribute(NewBsonEntity currentEntity, MetaField metaField) {
        String fieldName = metaField.getName();
        MetaAttribute attribute = (MetaAttribute) metaField;
        if (currentEntity.hasField(fieldName) && attribute.isConstant()) {
            throw new CmsDalException(DalErrCodeEnum.CONSTANT_FIELD_MODIFICATION, String.format(
                    "Constant field %s with type %s could not be updated!", fieldName, currentEntity.getType()));
        }
        super.processUserAttribute(currentEntity, metaField);
    }

    @Override
    protected void processCreator(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        // remove creator change
//        currentEntity.removeField(InternalFieldEnum.CREATOR.getName());
        if (!currentEntity.hasField(InternalFieldEnum.CREATOR.getName()) && parentEntity != null) {
            currentEntity.setCreator(parentEntity.getCreator());
        }
    }
    
    @Override
    protected void processModifier(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        if (!currentEntity.hasField(InternalFieldEnum.MODIFIER.getName()) && parentEntity != null) {
            currentEntity.setModifier(parentEntity.getModifier());
        }
    }

    @Override
    protected void processHostEntity(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        if (currentEntity.hasField(InternalFieldEnum.HOSTENTITY.getName())) {
            currentEntity.removeField(InternalFieldEnum.HOSTENTITY.getName());
        }
    }
    
}
