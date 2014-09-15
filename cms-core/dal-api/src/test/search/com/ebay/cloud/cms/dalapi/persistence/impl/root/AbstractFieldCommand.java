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


/**
 * 
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

package com.ebay.cloud.cms.dalapi.persistence.impl.root;

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dalapi.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author liasu
 * 
 */
public abstract class AbstractFieldCommand {

    protected static final String V   = MetaField.VALUE_KEY;
    protected static final String DOT = EntityIDHelper.DOT;

    protected final String        fieldName;
    protected final BsonEntity    entity;

    protected AbstractFieldCommand(BsonEntity entity, String fieldName) {
        this.entity = entity;
        this.fieldName = fieldName;
    }

    protected MetaField getField() {
        return entity.getMetaClass().getFieldByName(fieldName);
    }

    protected BsonEntity getEntity() {
        return entity;
    }

    protected DBObject buildModifyQuery() {
        BasicDBObject rootQueryObject = new BasicDBObject();
        if (entity.getVersion() != IEntity.NO_VERSION) {
            rootQueryObject.put(InternalFieldEnum.VERSION.getDbName(), entity.getVersion());
        }
        // add id/status/branch/type criteria
        rootQueryObject.put(InternalFieldEnum.ID.getDbName(), entity.getId());
        rootQueryObject.put(InternalFieldEnum.TYPE.getDbName(), entity.getType());
        rootQueryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        rootQueryObject.put(InternalFieldEnum.BRANCH.getDbName(), entity.getBranchId());
        return rootQueryObject;
    }

    protected void buildInternalUpdate(DBObject modifyBody) {
        // update entity version
        BasicDBObject inc = new BasicDBObject();
        inc.put(InternalFieldEnum.VERSION.getDbName(), 1);
        modifyBody.put("$inc", inc);
        
        // set internal fields
        BasicDBObject set = new BasicDBObject();
        set.put(InternalFieldEnum.LASTMODIFIED.getDbName(), getEntity().getLastModified());
        set.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        set.put(InternalFieldEnum.BRANCH.getDbName(), getEntity().getBranchId());
        set.put(InternalFieldEnum.MODIFIER.getDbName(), getEntity().getModifier());
        set.put(InternalFieldEnum.ID.getDbName(), getEntity().getId());
        set.put(InternalFieldEnum.TYPE.getDbName(), getEntity().getType());
        set.put(InternalFieldEnum.METAVERSION.getDbName(), getEntity().getMetaVersion());
    
        List<?> comments = entity.getFieldValues(InternalFieldEnum.COMMENT.getName());
        if (!comments.isEmpty()) {
            set.put(InternalFieldEnum.COMMENT.getDbName(), comments.get(0));
        }
        List<?> users = entity.getFieldValues(InternalFieldEnum.USER.getName());
        if (!users.isEmpty()) {
            set.put(InternalFieldEnum.USER.getDbName(), users.get(0));
        }
        
        modifyBody.put("$set", set);
    }

}
