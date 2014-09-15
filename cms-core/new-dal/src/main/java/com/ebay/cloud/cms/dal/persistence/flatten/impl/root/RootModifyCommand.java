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

package com.ebay.cloud.cms.dal.persistence.flatten.impl.root;

import java.util.ArrayList;
import java.util.Collection;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * partial modify root entity 
 * 
 * @author jianxu1, xjiang
 *
 */
public class RootModifyCommand implements IPersistenceCommand {

    private final static Collection<String> ROOT_FIELDS;
    static {
        ROOT_FIELDS = new ArrayList<String>(1);
        ROOT_FIELDS.add(InternalFieldEnum.VERSION.getDbName());
    }
    
	private final NewBsonEntity entity;
	private int version;
	
	public RootModifyCommand(NewBsonEntity entity){
		this.entity = entity;
		this.version = IEntity.START_VERSION;
	}
		
	@Override 
	public void execute(PersistenceContext context) {
		DBObject queryObject   = buildModifyQuery();	        
		DBObject updateObject  = buildModifyBody();
		WriteResult result = null;		
		result = MongoExecutor.update(context, entity.getMetaClass(), queryObject, updateObject);
		
		//throw exception if modify failed to match any entity
		if(result.getN()==0){
		    DBObject getResult = RootGetCommand.findDBObject(entity.getId(), entity.getBranchId(), context, entity.getMetaClass(), ROOT_FIELDS, true);
		    if (getResult == null) {
		        throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Can't find entity: " + entity.toString());
		    } 
		    Object currentVersion = getResult.get(InternalFieldEnum.VERSION.getDbName());		
		    throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, 
                  "current version is " + currentVersion + ", but version in repalce body is " + version + "! entity is " + entity.toString());
		}
	}

	private DBObject buildModifyQuery(){
        BasicDBObject queryObject = new BasicDBObject();
        //check version 
        int entityVersion = entity.getVersion();
        if (entityVersion != IEntity.NO_VERSION) {
            version = entity.getVersion();
            queryObject.put(InternalFieldEnum.VERSION.getDbName(), version);
        }
        //add id criteria
        queryObject.put(InternalFieldEnum.ID.getDbName(), entity.getId());
        //add status criteria
        queryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        
        //add branching criteria
        queryObject.put(InternalFieldEnum.BRANCH.getDbName(), entity.getBranchId());
        
        return queryObject;
    }

   private DBObject buildModifyBody() {
        //Only root document have version, it's controlled by $inc
        BasicDBObject modifyBody = new BasicDBObject();
        BasicDBObject bsonObject = entity.getNode();
        
        //jianxu1: 2012/7/16 remove _id and _version
        bsonObject.removeField("_id");
        bsonObject.removeField(InternalFieldEnum.VERSION.getDbName());
        
        // CMS-2657 : for audit use case, make sure _status are in the modify body
        if (!entity.hasField(InternalFieldEnum.STATUS.getName())) {
            bsonObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        }
        
        modifyBody.put("$set", bsonObject);
        
        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put(InternalFieldEnum.VERSION.getDbName(), 1);
        modifyBody.put("$inc", versionObject);
        return modifyBody;
    }
}
