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

package com.ebay.cloud.cms.dalapi.persistence.impl.root;


import java.util.Collection;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IRetrievalCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * get root entity
 * 
 * @author jianxu1, xjiang
 *
 */
public class RootGetCommand implements IRetrievalCommand {
	
    public static DBObject findDBObject(String entityId, String branchId, PersistenceContext context, MetaClass metadata, Collection<String> fields, boolean needActive) {
        DBObject fieldObject = buildGetRootFields(fields);
        DBObject queryObject = buildGetRootQuery(entityId, branchId, needActive);
        return MongoExecutor.findOne(context, metadata, queryObject, fieldObject);
    }
    
    private static DBObject buildGetRootQuery(String entityId, String branchId, boolean needActive){
        DBObject rootQueryObject = new BasicDBObject();
        rootQueryObject.put(InternalFieldEnum.ID.getDbName(), entityId);
        if (needActive) {
            rootQueryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        }
        rootQueryObject.put(InternalFieldEnum.BRANCH.getDbName(), branchId);
        return rootQueryObject;        
    }
    
    private static DBObject buildGetRootFields(Collection<String> fieldList){
        if (fieldList == null || fieldList.size() == 0) {
            return null;
        }
        BasicDBObject rootFieldObject = new BasicDBObject();
        for (String field : fieldList) {
            rootFieldObject.put(field, 1);
        }        
        return rootFieldObject;
    }
    
	private BsonEntity resultEntity = null;
	
	private final MetaClass entityType;
	private final String    branchId;
	private final String    entityId;

    public RootGetCommand(String branchId, String entityId, MetaClass entityType) {
        this.entityType = entityType;
        this.branchId = branchId;
        this.entityId = entityId;
    }

    @Override
	public void execute(PersistenceContext context) {
	    DBObject getObject = findDBObject(entityId, branchId, context, entityType, context.getQueryFields(), false);

        if (getObject != null) {
            // since we can't get root entity type from embedded entity, we
            // can't add type search criteria and have to check type by
            // ourself. :(
            resultEntity = new BsonEntity(entityType, getObject);
            String actualType = resultEntity.getType();
            if (!actualType.equals(entityType.getName())) {
                throw new CmsDalException(DalErrCodeEnum.DIRTY_DATA_WRONG_TYPE, String.format(
                        "Expected type is %s, but is %s instead", entityType, actualType));
            }
        }
	}

    @Override
    public Object getResult() {
		return resultEntity;
	}
	
}
