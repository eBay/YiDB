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

package com.ebay.cloud.cms.dal.persistence.flatten.impl.embed;


import java.util.BitSet;
import java.util.Date;
import java.util.Map.Entry;

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * modify partial embedded entity
 * 
 * @author jianxu1, xjiang, zhihzhang
 *
 */
@SuppressWarnings("static-access")
//for inner field, only update root and leaf entity now
public class EmbedModifyCommand extends AbstractEmbedCommand implements IPersistenceCommand {

    protected final NewBsonEntity entity;
    
    public EmbedModifyCommand(NewBsonEntity entity, FlattenEntityIDHelper helper){
        super(entity.getBranchId(), helper);
        this.entity = entity;
    }
    
    protected EmbedModifyCommand(MetaClass metaClass, String branchId, FlattenEntityIDHelper helper) {
        super(branchId, helper);
        this.entity = new NewBsonEntity(metaClass);
    }
    
    @Override
    public void execute(PersistenceContext context) {
        String entityId = entity.getId();
        MetaClass rootMetaClass = getRootEntityMetaClass(entityId, context);
        BitSet arrayBits = helper.checkArrayOnPath(entityId, rootMetaClass);
        String parentId = helper.getParentId(entity.getId());
        BitSet parentBits = helper.checkArrayOnPath(parentId, rootMetaClass);
        String id = parentId;
        if (parentBits.cardinality() == 0) {
            id = entity.getId();
        }
        
        // query root object with all intermediate _id
        DBObject rootQuery = buildGetQuery(parentBits, parentId, rootMetaClass);
        DBObject rootFields = buildGetRootFields(id, rootMetaClass);
        DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, rootQuery, rootFields);
        if (rootObject == null) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Modify, can not find embed document with Id: "
                    + entityId);
        }

        // update the embed object
        DBObject modifyQuery = buildModifyQuery(rootQuery, entity);
        DBObject modifyBody = buildModifyBody(arrayBits, rootObject, rootMetaClass);
        WriteResult result = MongoExecutor.update(context, rootMetaClass, modifyQuery, modifyBody);

        if (result.getN() == 0) {
            // TODO: need to send get query so we can tell WHY getN == 0
            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, "Version check fails for " + entityId + "! entity is " + entity.toString());
        }

    }
    
    protected void setEmbedObjectValue(BasicDBObject embedObject){
        for (Entry<String, Object> entry : entity.getNode().entrySet()) {
            embedObject.put(entry.getKey(), entry.getValue());
        }
    }
    
    protected DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass) {
        BasicDBObject embedObject = (BasicDBObject) EmbedDBObjectFilter.filter(entity.getId(), rootObject,
                rootMetaClass, null, helper);

        setEmbedObjectValue(embedObject);

        BasicDBObject setModifyObject = new BasicDBObject();
        
        DBObject obj = (DBObject) rootObject.get(embedFieldName);
        if(obj == null){
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Modify, can not find embed field with Id: "
                    + this.entity.getId());
        }
        // update root embed field's timestamp
        MetaField field = rootMetaClass.getFieldByFlattenValueDbName(embedFieldName);
        setModifyObject.put(field.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), new Date());

        setModifyObject.put(embedFieldName, obj);
        BasicDBObject modifyBody = new BasicDBObject();
        modifyBody.put("$set", setModifyObject);

        // increase version on root document
        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put(InternalFieldEnum.VERSION.getDbName(), 1);
        modifyBody.put("$inc", versionObject);

        buildRootUpdateObject(entity, null, modifyBody, rootMetaClass);

        return modifyBody;
    }

}
