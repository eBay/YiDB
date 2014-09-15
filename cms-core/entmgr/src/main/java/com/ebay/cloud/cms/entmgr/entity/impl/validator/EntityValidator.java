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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * traverse the entity and invoke actions registered
 * 
 * @author zhuang1
 *
 */


public class EntityValidator implements IEntityVisitor {
    private IEntity existingEntity;
    private List<IEntityAction> actions;

    public EntityValidator(IEntity existingEntity) {
        this.existingEntity = existingEntity;
        actions = new ArrayList<IEntityAction>();
    }

    public void addAction(IEntityAction action) {
        if (action != null) {
            actions.add(action);
        }
    }
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        List<String> fieldNames = new LinkedList<String>();
        fieldNames.add(InternalFieldEnum.ID.getName());
        
        MetaClass metadata = currentEntity.getMetaClass();
        for (MetaField field : metadata.getFields()) {
            if (field instanceof MetaRelationship) {
                MetaRelationship ref = (MetaRelationship) field;
                if (ref.getRelationType() == RelationTypeEnum.Embedded
                        || ref.getRelationType() == RelationTypeEnum.Inner) {
                    fieldNames.add(ref.getName());
                }
            }
        }
        
        return fieldNames;
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        for (IEntityAction validator : actions) {
            validator.processAttribute(currentEntity, existingEntity, metaField);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(IEntity currentEntity, MetaRelationship metaRelationship) {
        for (IEntityAction action : actions) {
            action.processReference(currentEntity, existingEntity, metaRelationship);
        }
        
        if (metaRelationship.getRelationType() == RelationTypeEnum.Embedded) {
            IEntity originEntity = existingEntity;
            Map<String, IEntity> idEntityMap = getEmbedEntities(existingEntity, metaRelationship);
            List<IEntity> refList = (List<IEntity>) currentEntity.getFieldValues(metaRelationship.getName());
            for (IEntity ref : refList) {
                existingEntity = idEntityMap.get(ref.getId());
                ref.traverse(this);
            }
            existingEntity = originEntity;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, IEntity> getEmbedEntities(IEntity entity, MetaRelationship metaRelationship) {
        Map<String, IEntity> idEntityMap = new HashMap<String, IEntity>();

        if (entity != null) {
            List<IEntity> list = (List<IEntity>) entity.getFieldValues(metaRelationship.getName());
            for (IEntity embedEntity : list) {
                idEntityMap.put(embedEntity.getId(), embedEntity);
            }
        }
        
        return idEntityMap;
    }
    
}
