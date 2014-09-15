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
import java.util.Date;
import java.util.List;

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
 * full replace root entity
 * 
 * @author jianxu1, xjiang
 *
 */
public class RootReplaceCommand implements IPersistenceCommand {

    private final static List<String> ROOT_FIELDS;
    static {
        ROOT_FIELDS = new ArrayList<String>(4);
        ROOT_FIELDS.add(InternalFieldEnum.CREATETIME.getDbName());
        ROOT_FIELDS.add(InternalFieldEnum.VERSION.getDbName());
        ROOT_FIELDS.add(InternalFieldEnum.TYPE.getDbName());
        ROOT_FIELDS.add(InternalFieldEnum.ID.getDbName());
    }
    
	private final NewBsonEntity entity;
	
	public RootReplaceCommand(NewBsonEntity entity){
		this.entity = entity;
	}

	@Override
	public void execute(PersistenceContext context) {							
		DBObject getResult = RootGetCommand.findDBObject(entity.getId(), entity.getBranchId(), context, entity.getMetaClass(), ROOT_FIELDS, false);		
		if (getResult != null) {
		    // check version
		    int currentVersion = (Integer) getResult.get(InternalFieldEnum.VERSION.getDbName());
		    int version = entity.getVersion();
		    if ((version != IEntity.NO_VERSION) && (currentVersion != version)) {		        
		        throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, 
                        "current version is " + currentVersion + ", but version in repalce body is "
                        + version + "! entity is " + entity.toString());
		    }
		    
		    Date createTime = (Date) getResult.get(InternalFieldEnum.CREATETIME.getDbName());
		    
		    DBObject queryObject = buildReplaceRootQuery(currentVersion);		    		
            DBObject updateObject = buildReplaceRootUpdate(currentVersion, createTime);
			try{				
				WriteResult result = MongoExecutor.update(context, entity.getMetaClass(), queryObject, updateObject);
				if (result.getN() == 0) {
					throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT,"current version is not " + currentVersion + "! entity is " + entity.toString());
				}
				//set version for response
				entity.setVersion(currentVersion+1);
			} catch (RuntimeException e) {
			    entity.setVersion(currentVersion);
			    throw e;
			} catch(Throwable t) {
				//if anything bad happens, need to set version back
				entity.setVersion(currentVersion);
			} finally {
				//in update scenario, have to set id value back into entity object because we
				//removed id field before
				entity.setId(entity.getId());
			}
		} else {
			// insert

			//2012/7/13 jianxu1 multi threading issue, if client A and client B send replace at same time
			//at T1, none of A and B find existing entity, so at T2, both A and B call insert, of we do not have 
			//unique index for combination of _branchId and _oid, we end up with duplicated entities.
			//FIX is: http://www.mongodb.org/display/DOCS/Indexes,  composite unique index
			//db.branches.ensureIndex({_branchId: 1, _oid: 1}, {unique: true});
			//TODO: change create repository to add composite unique index for branches, for main branch, we just need unique index on _oid

			DBObject insertObject = entity.getNode();
			insertObject.removeField("_id");
			insertObject.put(InternalFieldEnum.VERSION.getDbName(), IEntity.START_VERSION);			
			MongoExecutor.insert(context, entity.getMetaClass(), insertObject);
		}
	}

	private DBObject buildReplaceRootQuery(int currentVersion){
	    // add query version in case somebody make changes between get and update      
        DBObject rootQueryObject = new BasicDBObject();
        rootQueryObject.put(InternalFieldEnum.ID.getDbName(), entity.getId());
        rootQueryObject.put(InternalFieldEnum.TYPE.getDbName(), entity.getType());
        //rootQueryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        rootQueryObject.put(InternalFieldEnum.BRANCH.getDbName(), entity.getBranchId());  
        rootQueryObject.put(InternalFieldEnum.VERSION.getDbName(), currentVersion);
        return rootQueryObject;        
    }
	
    private DBObject buildReplaceRootUpdate(int currentVersion, Date createTime) {
        // add query version in case somebody make changes between get and update      
	    DBObject rootUpdateObject = entity.getNode();
	    rootUpdateObject.put(InternalFieldEnum.VERSION.getDbName(), currentVersion + 1);
	    rootUpdateObject.put(InternalFieldEnum.CREATETIME.getDbName(), createTime);
	    rootUpdateObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        // remove _id for sub branch
	    rootUpdateObject.removeField("_id");
        return rootUpdateObject;        
    }
}
