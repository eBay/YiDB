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

package com.ebay.cloud.cms.dal.entity.impl.bson.datahandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.DBObject;
/**
 * 
 * @author jianxu1
 * @date 2012/5/14
 * 
 * @history
 * 2012/5/23 FIX writeJson can not expect input field value is always "String"
 * when dealing with embed reference field, the input field to writeJson is JsonNode representation of embed document.
 * so I removed the strict "instanceof string" checking
 * 
 * 2012/5/24 FIX writeBson, same reason as fix on 2012/5/23: writeBson can not expect input field value is always "String"
 * Also, for non embed reference, no matter we reference to root document or embed document, the reference id
 * will be persisted as String, instead of (ObjectId for referenced root document, String for referenced embed reference id) 
 * 
 * 2012/5/24 TODO: let Handler implementation has a common place to do argument checking!
 * 
 * 2012/5/25 How to support Mapping entity of join results to Json
 */
public class ReferenceTypeHandler implements IDataTypeHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(ReferenceTypeHandler.class);

	/**
	 * 2012/5/24, for embed document, store string style artificial id as id instead of ObjectId
	 * 
	 */
	@Override
    public Object write(IEntity curEntity, Object value, MetaField metaField) {
		validate(value, metaField);
		CheckConditions.checkArgument(value instanceof BsonEntity,
                "Expect data type for field %s is %s, but actually is %s", metaField.getName(), BsonEntity.class.getName(),
                value);
		return ((BsonEntity)value).getNode();
	}

    private void validate(Object value, MetaField metaField) throws IllegalArgumentException {
        CheckConditions.checkNotNull(metaField);
        CheckConditions.checkArgument(value != null, "reference could not be null!");
    }

	/**
	 * @param metaField 
	 * @param value
	 */
	@Override
	public Object read(IEntity curEntity, Object value, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
        if (value == null) {
            // return null, rather than throws exception. this null should be
            // ignored. Related: CMS-3121, CMS-3536
            return null;
        }
        BsonEntity be = null;
        try {
            DBObject dbValue = (DBObject) value;
            MetaRelationship metaRelation = (MetaRelationship) metaField;
            MetaClass refMetaClass = metaRelation.getRefMetaClass();
            String type = (String) dbValue.get(InternalFieldEnum.TYPE.getDbName());
            if (!StringUtils.isNullOrEmpty(type) && !type.equals(refMetaClass.getName())) {
                refMetaClass = refMetaClass.getMetadataService().getMetaClass(type);
                if (refMetaClass == null) {
                    refMetaClass = metaRelation.getRefMetaClass();
                }
            }
            be = new BsonEntity(refMetaClass, dbValue);
        } catch (IllegalArgumentException e) {
            String metaClassName = curEntity.getMetaClass().getName();
            String repoName = curEntity.getRepositoryName();
            String branchId = curEntity.getBranchId();
            String fieldName = metaField.getName();
            logger.error(
                    "Fail to validate reference type field. Entity: {}, RepoName: {}, branchId: {}, fieldName: {}, errorMessage: {}",
                    new Object[] { metaClassName, repoName, branchId, fieldName, e.getMessage() });
        }
        return be;
    }

}
