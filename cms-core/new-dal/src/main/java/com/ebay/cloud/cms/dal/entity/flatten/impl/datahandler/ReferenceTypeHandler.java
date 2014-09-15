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

package com.ebay.cloud.cms.dal.entity.flatten.impl.datahandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.DBObject;
/**
 * FIXME:  more flatten
 * @author jianxu1, liasu
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
		CheckConditions.checkArgument(value instanceof NewBsonEntity,
                "Expect data type for field %s is %s, but actually is %s", metaField.getName(), NewBsonEntity.class.getName(),
                value);
		NewBsonEntity givenEntity = (NewBsonEntity) value;
//		MetaRelationship relationship = (MetaRelationship)metaField;
//        if (/*givenEntity.isReference() && */!relationship.getRelationType().equals(RelationTypeEnum.Embedded)) {
//            return givenEntity.getId();
//        } else {
		    return givenEntity.getNode();
//		}
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
        NewBsonEntity be = null;
        try {
//            if (value instanceof DBObject) {
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
                be = new NewBsonEntity(refMetaClass, dbValue);
//            } else if (value instanceof String || value == null) {
//                String id = (String) value;
//                MetaRelationship metaRelation = (MetaRelationship) metaField;
//                MetaClass refMetaClass = metaRelation.getRefMetaClass();
//                be = new NewBsonEntity(refMetaClass);
//                be.setReference(true);
//                be.setId(id);
//            }
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
