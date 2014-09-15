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

package com.ebay.cloud.cms.dalapi.persistence.impl.embed;

import java.util.BitSet;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dalapi.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * since we support entity history, we will delete the embed entity directly instead of mark it as deleted. 
 * 
 * @author xjiang
 *
 */
@SuppressWarnings("static-access")
//for inner field, only update root and leaf entity now
public class EmbedDeleteCommand extends AbstractEmbedCommand implements IPersistenceCommand {
    
    private final String entityId;
    private final MetaClass metaClass;
    private final BsonEntity entity;
    
    public EmbedDeleteCommand(BsonEntity embedEntity, AbstractEntityIDHelper helper) {
        super(embedEntity.getBranchId(), helper);
        this.entity = embedEntity;
        this.metaClass = embedEntity.getMetaClass();
        this.entityId = embedEntity.getId();
    }
    
    @Override
    public void execute(PersistenceContext context) {
        MetaClass rootMetaClass = getRootEntityMetaClass(entityId, context);
        String parentId = helper.getParentId(entityId);
        BitSet parentBits = helper.checkArrayOnPath(entityId, rootMetaClass);

        //TODO : adjacent query/update in code level, should always hit PRIMARY
        // query root object with parent id
        DBObject rootQuery  = buildGetQuery(parentBits, parentId, rootMetaClass);
        DBObject rootFields = buildGetFields(parentBits, parentId, false, rootMetaClass);
        DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, rootQuery, rootFields);
        if(rootObject == null){
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND,"Delete, parenet document doesn't exist!  " + parentId);
        }
        
        // add embed document into parent document
        DBObject createQuery = buildModifyQuery(rootQuery, entity);
        int newVersion = (Integer)rootObject.get(InternalFieldEnum.VERSION.getDbName()) + 1;
        String parentPath = getUpdatePath(parentBits, parentId, rootObject, rootMetaClass);
        MetaRelationship lastMetaField = helper.getLastMetaField(entityId, rootMetaClass);
        DBObject deleteBody = buildDeleteBody(lastMetaField, parentPath, entityId, newVersion, rootMetaClass);
        WriteResult result = MongoExecutor.update(context, rootMetaClass, createQuery, deleteBody);
        if (result.getN() == 0) {
            // something happens between get and replace
            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT,"Version check fails for " + entityId + " in class " + metaClass.getName());
        }
    }
    
    public DBObject buildDeleteBody(MetaRelationship metaField, String parentPath, String entityId, int newVersion, MetaClass rootMetaClass) {
        String embedPath = metaField.getValueDbName();
        String embedLengthPath = metaField.getDbName() + EntityIDHelper.DOT + FieldProperty.LENGTH.getDbName();
        if (!StringUtils.isNullOrEmpty(parentPath)) {
            embedPath = parentPath + EntityIDHelper.DOT + embedPath;
            embedLengthPath = parentPath + EntityIDHelper.DOT + embedLengthPath;
        }

        DBObject deleteBody = new BasicDBObject();
        if(metaField.getCardinality() == CardinalityEnum.Many) {
            // $pull this from array
            BasicDBObject idObject = new BasicDBObject();
            idObject.put(InternalFieldEnum.ID.getDbName(), entityId);
            BasicDBObject pullObject = new BasicDBObject();
            pullObject.put(embedPath, idObject);
            deleteBody.put("$pull", pullObject);

            DBObject decObject = new BasicDBObject();
            // maintain the _length value.
            decObject.put(embedLengthPath, -1);
            deleteBody.put("$inc", decObject);

            // root update object. 
            buildRootUpdateObject(entity, newVersion, deleteBody, rootMetaClass);
        }else{
            // $unset to remove this
            BasicDBObject unsetObject = new BasicDBObject();
            unsetObject.put(embedPath, 1);
            deleteBody.put("$unset", unsetObject);
            buildRootUpdateObject(entity, newVersion, deleteBody, rootMetaClass);
        }
        return deleteBody;
    }
        
}
