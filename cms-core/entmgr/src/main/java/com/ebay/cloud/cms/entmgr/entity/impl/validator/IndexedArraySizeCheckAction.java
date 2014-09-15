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
package com.ebay.cloud.cms.entmgr.entity.impl.validator;

import java.util.Collection;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

/**
 * @author shuachen
 * 
 *         2013-11-12
 */
public class IndexedArraySizeCheckAction implements IEntityAction {

	private int maxIndexedArraySize;

	public IndexedArraySizeCheckAction(int maxIndexedArraySize) {
		this.maxIndexedArraySize = maxIndexedArraySize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ebay.cloud.cms.entmgr.entity.impl.validator.IEntityAction#
	 * processAttribute(com.ebay.cloud.cms.dal.entity.IEntity,
	 * com.ebay.cloud.cms.dal.entity.IEntity,
	 * com.ebay.cloud.cms.metadata.model.MetaField)
	 */
	@Override
	public void processAttribute(IEntity currentEntity, IEntity existingEntity, MetaField metaField) {
		MetaClass metaClass = currentEntity.getMetaClass();
		Collection<MetaField> metaFields = metaClass.getFields();
		for (MetaField field : metaFields) {
			String fieldName = field.getName();
			if (CardinalityEnum.Many.equals(field.getCardinality())
					&& !metaClass.getIndexesOnField(fieldName).isEmpty()) {
				int indexedArraySize = currentEntity.getFieldValues(fieldName).size();
				if (indexedArraySize > maxIndexedArraySize) {
					String errorMessage = String.format("Exceed max indexed array size on metafield %s of metaclass %s! Max is %d, Actual is %d",
							fieldName, metaClass.getName(), maxIndexedArraySize, indexedArraySize);
					throw new CmsDalException(DalErrCodeEnum.EXCEED_MAX_INDEXED_ARRAY_SIZE, errorMessage);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ebay.cloud.cms.entmgr.entity.impl.validator.IEntityAction#
	 * processReference(com.ebay.cloud.cms.dal.entity.IEntity,
	 * com.ebay.cloud.cms.dal.entity.IEntity,
	 * com.ebay.cloud.cms.metadata.model.MetaRelationship)
	 */
	@Override
	public void processReference(IEntity currentEntity, IEntity existingEntity, MetaRelationship metaRelationship) {
		
	}

}
