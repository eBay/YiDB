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

package com.ebay.cloud.cms.dal.persistence.impl.embed;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * @author liasu
 * 
 */
@SuppressWarnings("static-access")
public abstract class AbstractFieldEmbedCommand extends AbstractEmbedCommand implements IPersistenceCommand {

    protected final String        fieldName;
    protected final BsonEntity    entity;
    protected DBObject rootQuery;
    protected String embedPath;
    protected MetaClass rootMetaClass;
    protected static final String V   = MetaField.VALUE_KEY;
    protected static final String DOT = EntityIDHelper.DOT;

    protected AbstractFieldEmbedCommand(BsonEntity entity, String fieldName, AbstractEntityIDHelper helper) {
        super(entity.getBranchId(), helper);
        this.fieldName = fieldName;
        this.entity = entity;
    }

    protected MetaClass getRootEntityMetaClass(PersistenceContext context) {
        return super.getRootEntityMetaClass(entity.getId(), context);
    }

    /**
     * Mongo array operations doesn't not support $ with pullAll. Find the extract index number.
     */
    protected String getEmbedPath(BitSet arrayBits, String embedId, DBObject getObject, MetaClass rootMetaClass) {
        List<String> updatePathList = new ArrayList<String>();
        getArrayOffsetPath(arrayBits, embedId, getObject, rootMetaClass, updatePathList);
        return StringUtils.join(EntityIDHelper.DOT, updatePathList);
    }

    protected void buildRootModifyBody(DBObject modifyBody) {
        // update entity version
        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put(InternalFieldEnum.VERSION.getDbName(), 1);
        modifyBody.put("$inc", versionObject);

        buildRootUpdateObject(entity, null, modifyBody, rootMetaClass);
    }

    protected BsonEntity getEntity() {
        return entity;
    }

    protected MetaField getField() {
        return entity.getMetaClass().getFieldByName(fieldName);
    }
    
    protected void buildEmbedModifyBody(String parentPath, DBObject modifyBody){
        BasicDBObject setObject = (BasicDBObject) modifyBody.get("$set");
        if (setObject == null) {
            setObject = new BasicDBObject();
        }
        StringBuilder modifier = new StringBuilder();
        modifier.append(parentPath).append(DOT).append(InternalFieldEnum.MODIFIER.getDbName());
        StringBuilder lastModified= new StringBuilder();
        lastModified.append(parentPath).append(DOT).append(InternalFieldEnum.LASTMODIFIED.getDbName());
        
        setObject.put(modifier.toString(), entity.getModifier());
        setObject.put(lastModified.toString(), entity.getLastModified());
        modifyBody.put("$set", setObject);
    }

    protected abstract String getOperation();
    
    @Override
    public void execute(PersistenceContext context) {
        String entityId = entity.getId();
        rootMetaClass = getRootEntityMetaClass(context);
        BitSet arrayBits = helper.checkArrayOnPath(entityId, rootMetaClass);
        String parentId = helper.getParentId(entityId);
        rootQuery = buildGetQuery(arrayBits, parentId, rootMetaClass, context);
    
        // get the root entity for further embed path computing
        DBObject rootGetFields = buildGetRootFields(entityId, rootMetaClass);
        DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, rootQuery, rootGetFields);
        if(rootObject == null){
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, getOperation() + " on embed field, parenet document with id " + parentId + "doesn't exist!");
        }
        embedPath = getEmbedPath(arrayBits, entityId, rootObject, rootMetaClass);
        
        // new query with version
        buildModifyQuery(rootQuery, entity);

        DBObject modifyQuery = buildModifyQuery(rootQuery, entity);
        DBObject modifyBody = buildModifyBody(arrayBits, rootObject, rootMetaClass);

        WriteResult result = MongoExecutor.update(context, rootMetaClass, modifyQuery, modifyBody);
        if (result.getN() == 0) {
            // something happens between get and replace
            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, "Version check fails for " + entity.getId()
                    + " in class " + rootMetaClass.getName() + " and embed class " + getEntity().getType() + "! entity is " + entity.toString());
        }
    }

    protected abstract DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass);

    protected DBObject buildSetBody(DBObject rootObject) {
        BasicDBObject setModifyObject = new BasicDBObject();
        BasicDBObject obj = (BasicDBObject) rootObject.get(embedFieldName);
        if (obj == null) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND,
                    getOperation() + ", can not find embed field with Id: " + this.entity.getId());
        }
        obj.put(FieldProperty.TIMESTAMP.getDbName(), new Date());

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
