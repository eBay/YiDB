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

package com.ebay.cloud.cms.dal.entity;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * 
 * @author jianxu1
 * @date 2012/5/18 
 * @history
 * 
 *
 */
public class EntityMapper implements IEntityVisitor {
    
    private static Logger logger = LoggerFactory.getLogger(EntityMapper.class);

    private final Class<? extends IEntity> targetClass;
    private IEntity targetEntity;
    private boolean needFieldProperty;

	public IEntity getBuildEntity(){
		return targetEntity;
	}

	public EntityMapper(Class<? extends IEntity> targetClass, MetaClass metaClass){
		this.targetClass = targetClass;
		targetEntity = createEntity(targetClass, metaClass);
	}

    public EntityMapper(Class<? extends IEntity> targetClass, MetaClass metaClass, boolean needFieldProperty) {
        this.targetClass = targetClass;
        targetEntity = createEntity(targetClass, metaClass);
        this.needFieldProperty = needFieldProperty;
    }

	@Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        String fieldName = metaField.getName();
        if (currentEntity.hasField(fieldName)) {
            List<?> fieldValues = currentEntity.getFieldValues(fieldName);
            if (fieldValues.size() > 0) {
                for (Object value : fieldValues) {
                    targetEntity.addFieldValue(fieldName, value);
                }
            } else {
                targetEntity.setFieldValues(fieldName, Collections.emptyList());
            }
            processFieldProperty(currentEntity, metaField);
        }
    }

    private void processFieldProperty(IEntity currentEntity, MetaField metaField) {
        if (metaField.isInternal() || !this.needFieldProperty) {
            return;
        }
        for (FieldProperty prop : FieldProperty.values()) {
            Object value = currentEntity.getFieldProperty(metaField.getName(), prop.getName());
            if (value != null) {
                targetEntity.setFieldProperty(metaField.getName(), prop.getName(), value);
            }
        }
    }
	
	@Override
	public void processReference(IEntity currentEntity, MetaRelationship metaRelationship){
	    IEntity oldTargetEntity = targetEntity;
		String fieldName = metaRelationship.getName();
		processFieldProperty(currentEntity, metaRelationship);
		@SuppressWarnings("unchecked")
		List<IEntity> refEntities = (List<IEntity>) currentEntity.getFieldValues(fieldName);
	    if (currentEntity.hasField(fieldName) && refEntities.size() == 0
                && metaRelationship.getCardinality() == CardinalityEnum.Many) {
            // empty handling
            targetEntity.setFieldValues(fieldName, Collections.emptyList());
            return;
        }
		
        if ( (targetClass.equals(JsonEntity.class) || metaRelationship.getRelationType() == RelationTypeEnum.Embedded)
        		&& !isSkippableRelationship(metaRelationship)) {
			MetaClass refMetaClass = metaRelationship.getRefMetaClass();
			for(IEntity refEntity: refEntities){
				//create new target entity before we call traverse of source entity
			    MetaClass meta = refMetaClass;
			    if (refEntity.getMetaClass() != null) {
			        meta = refEntity.getMetaClass();
			    }
			    targetEntity = createEntity(targetClass, meta);
			    oldTargetEntity.addFieldValue(fieldName, targetEntity);
				refEntity.traverse(this);
				targetEntity = oldTargetEntity;
			}
        } else {
            // if driven by meta data
            MetaClass refMetaClass = metaRelationship.getRefMetaClass();
            for (IEntity refEntity : refEntities) {
                MetaClass meta = refMetaClass;
                if (refEntity.getMetaClass() != null) {
                    meta = refEntity.getMetaClass();
                }
                IEntity newRefEntity = createEntity(targetEntity.getClass(), meta);
                String refId = refEntity.getId();
                newRefEntity.setId(refId);
                targetEntity.addFieldValue(fieldName, newRefEntity);
            }
        }
//        processFieldProperty(currentEntity, metaRelationship);
	}

	protected boolean isSkippableRelationship(MetaRelationship metaRelationship) {
		return metaRelationship.isVirtual();
	}

	/**
	 * if target is Json, traverse all runtime data
	 * if target is Bson, traverse all meta data fields
	 */
	@Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        if (targetClass.equals(JsonEntity.class)) {
            return currentEntity.getFieldNames();
        } else {
            return currentEntity.getMetaClass().getFieldNames();
        }
    }

    private IEntity createEntity(Class<? extends IEntity> targetClass, MetaClass metaClass) {
        Constructor<? extends IEntity> cotr;
        try {
            cotr = targetClass.getConstructor(MetaClass.class);
            return cotr.newInstance(metaClass);
        } catch (Exception e) {
            String msg = "Create entity failed!";
            logger.error(msg);
            throw new CmsDalException(DalErrCodeEnum.ENTITY_CREATE, msg, e);
        }
    }

}
