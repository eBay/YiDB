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
import java.util.List;

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
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * create embedded entity
 * 
 * https://jira.mongodb.org/browse/SERVER-831
 * 
 * @author jianxu1, xjiang, zhihzhang
 *
 */
@SuppressWarnings("static-access")
//for inner field, only update root and leaf entity now
public class EmbedCreateCommand extends AbstractEmbedCommand implements IPersistenceCommand {

	private final NewBsonEntity entity;
	
    public EmbedCreateCommand(NewBsonEntity entity, FlattenEntityIDHelper helper) {
	    super(entity.getBranchId(), helper);
        this.entity = entity;
	}
	
	@Override
	public void execute(PersistenceContext context) {
	    MetaClass rootMetaClass = getRootEntityMetaClass(entity.getId(), context);
	    String parentId = helper.getParentId(entity.getId());
        BitSet parentBits = helper.checkArrayOnPath(parentId, rootMetaClass);
        
        // query root object with parent id
        DBObject rootQuery   = buildGetQuery(parentBits, parentId, rootMetaClass);
        
        String id = parentId;
        if (parentBits.cardinality() == 0) {
            id = entity.getId();
        }
        DBObject rootFields = buildGetRootFields(id, rootMetaClass);
        DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, rootQuery, rootFields);
        if(rootObject == null){
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND,"Create, parenet document doesn't exist!  " + parentId);
        }
        
        // add embed document into parent document
        DBObject createQuery = buildModifyQuery(rootQuery, entity);
        int newVersion = (Integer)rootObject.get(InternalFieldEnum.VERSION.getDbName()) + 1;
        String parentPath = getUpdatePath(parentBits, parentId, rootObject, rootMetaClass);
        MetaRelationship lastField = helper.getLastMetaField(entity.getId(), rootMetaClass);
        DBObject createBody = buildCreateBody(lastField, parentPath, entity, newVersion, rootMetaClass, rootObject, parentId);
        
        WriteResult result = MongoExecutor.update(context, rootMetaClass, createQuery, createBody);
        if (result.getN() == 0) {
            // something happens between get and replace
            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, "Version check fails" + "! entity is " + entity.toString());
        }
	}
	
	public DBObject buildCreateBody(MetaRelationship lastField, String parentPath, NewBsonEntity entity, int newVersion, MetaClass rootMetaClass, DBObject rootObject, String parentId) {
        BasicDBObject embedParentObject = (BasicDBObject) EmbedDBObjectFilter.filter(parentId, rootObject,
                rootMetaClass, null, helper);
	    if (embedParentObject == null) {
	        throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Create, can not find embed field with Id: "
                    + parentId);
        }
	    
	    embedParentObject.remove("_id");
	    
	    // for flatten storage, the length change might differ between the embed under root and multiple level embed  
        List<String> embedSegList = helper.getEmbedPathSegs(entity.getId(), rootMetaClass);
        boolean justUnderRoot = embedSegList.size() == 1;
        
	    BasicDBObject setModifyObject = new BasicDBObject();
        if (lastField.getCardinality() == CardinalityEnum.Many) {
            BasicDBList valueList = (BasicDBList) embedParentObject.get(lastField.getFlattenValueDbName());
            if (valueList == null) {
                // creation
                valueList = new BasicDBList();
                valueList.add(entity.getNode());
                // set the value 
                embedParentObject.put(lastField.getFlattenValueDbName(), valueList);
                // set the field properties
                setFieldProperties(lastField, setModifyObject, embedParentObject, 1, new Date(), justUnderRoot);
            } else {
                // append
                int size = valueList.size();
                valueList.add(entity.getNode());
                setFieldProperties(lastField, setModifyObject, embedParentObject, size + 1, new Date(), justUnderRoot);
            }
        } else {
            embedParentObject.put(lastField.getFlattenValueDbName(), entity.getNode());
            setFieldProperties(lastField, setModifyObject, embedParentObject, null, new Date(), justUnderRoot);
        }

        DBObject obj = (DBObject) rootObject.get(embedFieldName);
        if(obj == null){
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Create, can not find embed field with Id: "
                    + this.entity.getId());
        }
        // update root field last modified
        MetaField rootEmbedfield = rootMetaClass.getFieldByFlattenValueDbName(embedFieldName);
        setModifyObject.put(rootEmbedfield.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), new Date());

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

    void setFieldProperties(MetaRelationship lastField, BasicDBObject setModifyObject, BasicDBObject embedParentObject,
            Integer length, Date d, boolean justUnderRoot) {
        if (justUnderRoot) {
            if (length != null) {
                setModifyObject.put(lastField.getFlattenPropertyValueDbName(FieldProperty.LENGTH), length);
            }
            setModifyObject.put(lastField.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), d);
        } else {
            if (length != null) {
                embedParentObject.put(lastField.getFlattenPropertyValueDbName(FieldProperty.LENGTH), length);
            }
            embedParentObject.put(lastField.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), d);
        }
    }
}
