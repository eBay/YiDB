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

package com.ebay.cloud.cms.dal.persistence.impl.embed;

import java.util.Map.Entry;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.mongodb.BasicDBObject;

/**
 * replace embedded entity command
 * 
 * https://jira.mongodb.org/browse/SERVER-831
 * 
 * 
 * @author xjiang,zhihzhang
 *
 */
//for inner field, only update root and leaf entity now
public class EmbedReplaceCommand extends EmbedModifyCommand implements IPersistenceCommand {

  public EmbedReplaceCommand(BsonEntity entity, AbstractEntityIDHelper helper) {
      super(entity, helper);
  }
  
  protected void setEmbedObjectValue(BasicDBObject embedObject){
      embedObject.clear();
      for (Entry<String, Object> entry : entity.getNode().entrySet()) {
          embedObject.put(entry.getKey(), entry.getValue());
      }
  }
//	
//	public EmbedReplaceCommand(BsonEntity entity){
//	    super(entity.getBranchId());
//        this.entity = entity;
//	}
//	
//
//	//https://jira.mongodb.org/browse/SERVER-831
//	@Override
//	public void execute(PersistenceContext context) {
//	    String entityId = entity.getId();
//	    MetaClass rootMetaClass = getRootEntityMetaClass(entityId, context);        
//        BitSet entityBits = EntityIDHelper.checkArrayOnPath(entityId, rootMetaClass);
//        DBObject rootQuery   = buildGetQuery(entityBits, entityId, rootMetaClass);
//        DBObject rootFields = buildGetFields(entityBits, entityId, false, rootMetaClass);
//        DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, rootQuery, rootFields);
//        if(rootObject == null){
//            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND,"Create, parenet document doesn't exist!  " + entityId);
//        }
//        
//        DBObject replaceQuery = buildModifyQuery(rootQuery, entity);
//        int newVersion = (Integer)rootObject.get(InternalFieldEnum.VERSION.getDbName()) + 1;
//        String embedPath = getUpdatePath(entityBits, entityId, rootObject, rootMetaClass);
//        DBObject replaceBody = buildReplaceBody(embedPath, entity, newVersion, rootMetaClass);
//        WriteResult result = MongoExecutor.update(context, rootMetaClass, replaceQuery, replaceBody);
//        if (result.getN() == 0) {
//            // something happens between get and replace
//            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT,"Version check fails");
//        }
//	}
//	
//	public DBObject buildReplaceBody(String embedPath, BsonEntity entity, int newVersion, MetaClass rootMetaClass) {
//        // mongo does not support $inc and $set at the same time, we have to use absolute version
//        // update operation
//        DBObject replaceBody = new BasicDBObject();
//        BasicDBObject updateObject = new BasicDBObject();
//        updateObject.put(embedPath, entity.getNode());
//        replaceBody.put("$set", updateObject);
//
//        buildRootUpdateObject(entity, newVersion, replaceBody, rootMetaClass);
//        return replaceBody;
//    }
//	
}
