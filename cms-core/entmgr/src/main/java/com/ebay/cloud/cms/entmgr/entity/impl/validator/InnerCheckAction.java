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
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * Check if the entity contains inner relationships which cannot be modified.
 * 
 * @author zhuang1
 *
 */


public class InnerCheckAction implements IEntityAction {
    @Override
    public void processAttribute(IEntity currentEntity, IEntity existingEntity, MetaField metaField) {

    }

    @Override
    public void processReference(IEntity currentEntity, IEntity existingEntity, MetaRelationship metaRelationship) {
        if (metaRelationship.getRelationType() == RelationTypeEnum.Inner) {
            String fieldName = metaRelationship.getName();
            if (currentEntity.hasField(fieldName)) {
                throw new CmsEntMgrException(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE,
                        String.format("inner relationship %s is immutable", fieldName));
            }
        }
    }
    
}
