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

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.persistence.IRetrievalCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.DBObject;

/**
 * get embedded entity
 * 
 * @author jianxu1, xjiang
 *
 */
public class EmbedGetCommand extends AbstractEmbedCommand implements IRetrievalCommand {

	private NewBsonEntity resultEntity = null;
	private final String entityId;
	private final MetaClass entityType;
	
	public EmbedGetCommand(String branchId, String entityId, MetaClass entityType, FlattenEntityIDHelper helper){
	    super(branchId, helper);
	    this.entityId = entityId;
	    this.entityType = entityType;
	}
	
	@Override
	public void execute(PersistenceContext context) {
	    MetaClass rootMetaClass = getRootEntityMetaClass(entityId, context);
	    BitSet arrayBits = helper.checkArrayOnPath(entityId, rootMetaClass);
        // find root object & embed objects	    			
		DBObject getQuery = buildGetQuery(arrayBits, entityId, rootMetaClass);
		DBObject getField = buildGetFields(arrayBits, entityId, true, rootMetaClass);
		
		DBObject rootObject = MongoExecutor.findOne(context, rootMetaClass, getQuery, getField);
		if(rootObject == null){
			return;
		}
		// extract embed object by id
        DBObject embedObject = EmbedDBObjectFilter.filter(entityId, rootObject, rootMetaClass,
                context.getQueryFields(), helper);
        if (embedObject != null) {
            resultEntity = new NewBsonEntity(entityType, embedObject);
            int rootVersion = (Integer) rootObject.get(InternalFieldEnum.VERSION.getDbName());
            resultEntity.setVersion(rootVersion);
        }
	}

    @Override
	public Object getResult(){
		return resultEntity;
	}
	
	
}
