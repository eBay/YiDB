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

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback.Operation;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * Check entity ACL Visitor
 * 
 * @author zhuang1
 *
 */


public class ReqInfoFillAction implements IEntityAction {
    private ThreadLocal<Operation> operation;
    private ThreadLocal<String>    userId;
    private ThreadLocal<String>    comment;
    private ThreadLocal<String>    name;
    private ThreadLocal<Boolean>   isRootLevel;
    private ThreadLocal<Boolean>   isIgnore;

    public ReqInfoFillAction() {
        operation    = new ThreadLocal<Operation>();
        userId       = new ThreadLocal<String>();
        comment      = new ThreadLocal<String>();
        name         = new ThreadLocal<String>();
        isRootLevel  = new ThreadLocal<Boolean>();
        isIgnore     = new ThreadLocal<Boolean>();
    }
    
    public void init(Operation operation, String userId, String comment, String name) {
        this.operation.set(operation);
        this.userId.set(userId);
        this.comment.set(comment);
        this.name.set(name);
        this.isRootLevel.set(true);
        this.isIgnore.set(false);
    }

    @Override
    public void processAttribute(IEntity currentEntity, IEntity existingEntity, MetaField metaField) {
        if (isIgnore.get()) {
            return;
        }
        
        if (existingEntity != null) {
            currentEntity.setCreator(existingEntity.getCreator());
            currentEntity.setStatus(existingEntity.getStatus());
            currentEntity.setCreateTime(existingEntity.getCreateTime());
        }

        if (isRootLevel.get()) {
            currentEntity.addFieldValue(InternalFieldEnum.USER.getName(), userId.get());
            currentEntity.addFieldValue(InternalFieldEnum.COMMENT.getName(), comment.get());
            currentEntity.addFieldValue(InternalFieldEnum.MODIFIER.getName(), name.get());
            currentEntity.removeField(InternalFieldEnum.LASTMODIFIED.getName());
            // populate create only for create and replace operation. 
            if (operation.get() == Operation.CREATE) {
                currentEntity.setCreator(name.get());
            } 
            if (operation.get() == Operation.REPLACE && existingEntity == null) {
                currentEntity.setCreator(name.get());
            } 
            if (operation.get() == Operation.MODIFY) {
                currentEntity.setStatus(existingEntity.getStatus());
            }
            isRootLevel.set(false);
        } else {
            currentEntity.removeField(InternalFieldEnum.MODIFIER.getName());
            if (existingEntity == null) {
                currentEntity.setCreator(name.get());
            }
        }
    }

    @Override
    public void processReference(IEntity currentEntity, IEntity existingEntity, MetaRelationship metaRelationship) {
        isIgnore.set(true);
        if (metaRelationship.getRelationType() == RelationTypeEnum.Embedded
                && (operation.get() == Operation.MODIFY
                    || operation.get() == Operation.REPLACE)) {
            isIgnore.set(false);
        }
    }
    
}
