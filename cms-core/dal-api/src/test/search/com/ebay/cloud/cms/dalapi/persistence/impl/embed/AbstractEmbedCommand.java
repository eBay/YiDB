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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dalapi.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * base class for all embedded command
 * 
 * @author xjiang
 *
 */
@SuppressWarnings("static-access")
public abstract class AbstractEmbedCommand {

    protected final String branchId;
    
    protected final AbstractEntityIDHelper helper;
    
    protected String embedFieldName;

    protected AbstractEmbedCommand(String branchId, AbstractEntityIDHelper helper) {
        this.branchId = branchId;
        this.helper = helper;
    }
    
    protected MetaClass getRootEntityMetaClass(String entityId, PersistenceContext context) {
        String rootType = helper.getRootEntityType(entityId);
        IMetadataService metaService = context.getMetadataService();
        MetaClass metaClass = metaService.getMetaClass(rootType);
        CheckConditions.checkArgument(metaClass!=null, "Can not find metaClass given embedId %s", entityId);
        return metaClass;
    }
    protected DBObject buildGetQuery(BitSet arrayBits, String embedId, MetaClass rootMetaClass){       
        DBObject queryObject = new BasicDBObject();
        queryObject.put(InternalFieldEnum.BRANCH.getDbName(), branchId);
        queryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());

        String embedPath = helper.getEmbedPath(embedId, rootMetaClass);
        if (StringUtils.isNullOrEmpty(embedPath)) {
            // generate field for root
            queryObject.put(InternalFieldEnum.ID.getDbName(), embedId);
            queryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        } else if (arrayBits.isEmpty()) {     
            // generate dot notation field for single 
            String idFieldName = embedPath + EntityIDHelper.DOT + InternalFieldEnum.ID.getDbName();
            String statusFieldName = embedPath + EntityIDHelper.DOT + InternalFieldEnum.STATUS.getDbName();
            queryObject.put(idFieldName, embedId);
            queryObject.put(statusFieldName, StatusEnum.ACTIVE.toString());            
        } else {
            // generate $elemMatch for array 
            DBObject idQueryObject = new BasicDBObject();
            idQueryObject.put(InternalFieldEnum.ID.getDbName(), embedId);
            idQueryObject.put(InternalFieldEnum.STATUS.getDbName(),StatusEnum.ACTIVE.toString());
            BasicDBObject elemMatchObject = new BasicDBObject();
            elemMatchObject.put("$elemMatch", idQueryObject);   
            queryObject.put(embedPath, elemMatchObject);
        }
        return queryObject;
    }
    
    protected DBObject buildGetFields(BitSet arrayBits, String embedId, boolean needFull, MetaClass rootMetaClass) {
        BasicDBObject fieldObject = new BasicDBObject();
        // get version from root document
        fieldObject.put(InternalFieldEnum.VERSION.getDbName(), 1);
        // get id of all intermediate document
        String idPath = null;
        if (arrayBits.cardinality() >= 1) {
            List<String> embedSegList = helper.getEmbedPathSegs(embedId, rootMetaClass);
            StringBuilder idPathBuilder = new StringBuilder();
            for(int index = 0; index < embedSegList.size(); index++){
                String dbFieldName = embedSegList.get(index);
                idPathBuilder.append(dbFieldName);                
                idPathBuilder.append(EntityIDHelper.DOT);
                idPathBuilder.append(MetaField.VALUE_KEY);
                idPathBuilder.append(EntityIDHelper.DOT);
                idPath =  idPathBuilder.toString() + InternalFieldEnum.ID.getDbName();
                fieldObject.put(idPath, 1);
            }
        }
        // get leaf document 
        if (needFull) {
            // since {a.b._i : 1, a.b : 1} only return a.b, we have to remove a.b._i 
            fieldObject.remove(idPath);
            String embedPath = helper.getEmbedPath(embedId, rootMetaClass);
            fieldObject.put(embedPath, 1);
        }
        return fieldObject;
    }

    protected DBObject buildGetRootFields(String embedId, MetaClass rootMetaClass) {
        BasicDBObject fieldObject = new BasicDBObject();
        // get version from root document
        fieldObject.put(InternalFieldEnum.VERSION.getDbName(), 1);
        fieldObject.put(InternalFieldEnum.ID.getDbName(), 1);
        // get field on root entity only
        List<String> embedSegList = helper.getEmbedPathSegs(embedId, rootMetaClass);
        if (!embedSegList.isEmpty()) {
            String dbFieldName = embedSegList.get(0);
            fieldObject.put(dbFieldName, 1);
            this.embedFieldName = dbFieldName;

        }
        return fieldObject;
    }
    
    protected DBObject buildModifyQuery(DBObject rootQuery, BsonEntity entity){
        int currentVersion = entity.getVersion();
        if (currentVersion != IEntity.NO_VERSION) {
            rootQuery.put(InternalFieldEnum.VERSION.getDbName(), currentVersion);
        }
        if(helper.isEmbedEntity(entity.getId())){
            entity.removeField(InternalFieldEnum.VERSION.getName());
        }
        return rootQuery;
    }
    
    protected String getUpdatePath(BitSet arrayBits, String embedId, DBObject getObject, MetaClass rootMetaClass) {
        // mongodb only support one $ in the dot notation path
        List<String> updatePathList = new ArrayList<String>();
        if (arrayBits.cardinality() <= 1) {
            // use $ operator for 1- array in the path: a.$.b or a.b.$
            getArrayLocatorPath(arrayBits, embedId, rootMetaClass, updatePathList);
        } else {
            // use offset for 2+ array in the path: a.0.b.1
            getArrayOffsetPath(arrayBits, embedId, getObject, rootMetaClass, updatePathList);
        }
        return StringUtils.join(EntityIDHelper.DOT, updatePathList);
    }

    private void getArrayLocatorPath(BitSet arrayBits, String embedId, MetaClass rootMetaClass,
            List<String> updatePathList) {
        List<String> dbFieldList = helper.getEmbedPathSegs(embedId, rootMetaClass);
        for (int index = 0; index < dbFieldList.size(); index++) {
            String dbFieldName = dbFieldList.get(index);
            updatePathList.add(dbFieldName);
            updatePathList.add(MetaField.VALUE_KEY);
            if (arrayBits.get(index)) {
                updatePathList.add("$");
            }
        }
    }

    protected void getArrayOffsetPath(BitSet arrayBits, String embedId, DBObject getObject, MetaClass rootMetaClass,
            List<String> updatePathList) {
        DBObject currentObject = getObject;
        List<String> idList = helper.generateAncestorIds(embedId);
        List<String> dbFieldList = helper.getEmbedPathSegs(embedId, rootMetaClass);
        for (int index = 0; index < dbFieldList.size(); index++) {
            String dbFieldName = dbFieldList.get(index);
            updatePathList.add(dbFieldName);
            updatePathList.add(MetaField.VALUE_KEY);
            DBObject fieldObject = (DBObject) currentObject.get(dbFieldName);
            if (arrayBits.get(index)) {
                @SuppressWarnings("unchecked")
                List<DBObject> refValues = (List<DBObject>) fieldObject.get(MetaField.VALUE_KEY);
                String idVal = idList.get(index);
                int offset = findOffsetById(refValues, idVal);
                currentObject = refValues.get(offset);
                updatePathList.add(String.valueOf(offset));
            } else {
                currentObject = (DBObject) fieldObject.get(MetaField.VALUE_KEY);
            }
        }
    }
    
    protected int findOffsetById(List<DBObject> refValues, String embedId) {      
        DBObject matchObject = null;
        int elemIndex = 0;      
        for (; elemIndex < refValues.size(); elemIndex++) {
            DBObject refObject = refValues.get(elemIndex);
            String objectId = (String) refObject.get(InternalFieldEnum.ID.getDbName());
            if (objectId.equals(embedId)) {
                matchObject = refObject;
                break;
            }
        }
        if (matchObject == null) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Can not find embed document with id=" + embedId);                                    
        }
        return elemIndex;
    }

    protected void buildRootUpdateObject(BsonEntity entity, Integer newVersion, DBObject updateBody, MetaClass rootMetaClass) {
        BasicDBObject setObject = (BasicDBObject) updateBody.get("$set");
        if (setObject == null) {
            setObject = new BasicDBObject();
        }

        // populate the data to the root entity for the internal fields
        if (newVersion != null) {
            setObject.put(InternalFieldEnum.VERSION.getDbName(), newVersion);
        }
        setObject.put(InternalFieldEnum.MODIFIER.getDbName(), entity.getModifier());
        setObject.put(InternalFieldEnum.LASTMODIFIED.getDbName(), entity.getLastModified());
        setObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        setObject.put(InternalFieldEnum.BRANCH.getDbName(), entity.getBranchId());
        setObject.put(InternalFieldEnum.TYPE.getDbName(), rootMetaClass.getName());
        String rootId = helper.getRootId(entity.getId());
        setObject.put(InternalFieldEnum.ID.getDbName(), rootId);

        setInternalField(entity, setObject, InternalFieldEnum.COMMENT);
        setInternalField(entity, setObject, InternalFieldEnum.USER);

        updateBody.put("$set", setObject);
    }

    private void setInternalField(BsonEntity entity, BasicDBObject setObject,
            InternalFieldEnum internalField) {
        if (entity.hasField(internalField.getName())) {
            List<?> comments = entity.getFieldValues(internalField.getName());
            if (!comments.isEmpty()) {
                setObject.put(internalField.getDbName(), comments.get(0));
            } else {
                setObject.put(internalField.getDbName(), null);
            }
        }
    }
    

}
