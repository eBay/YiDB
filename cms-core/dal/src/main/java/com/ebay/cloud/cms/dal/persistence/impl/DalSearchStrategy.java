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


/**
 * 
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

package com.ebay.cloud.cms.dal.persistence.impl;

import java.util.Collection;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;

/**
 * @author liasu
 * 
 */
public class DalSearchStrategy implements ISearchStrategy {

    private static final DalSearchStrategy INSTANCE = new DalSearchStrategy();

    private DalSearchStrategy() {
    }

    public static DalSearchStrategy getInstance() {
        return INSTANCE;
    }

    @Override
    public String getFieldValueName(MetaField field) {
        return field.getValueDbName();
    }

    @Override
    public String getFieldPropertyName(MetaField field, FieldProperty property) {
        StringBuilder sb = new StringBuilder();
        sb.append(field.getDbName());
        sb.append('.');
        sb.append(property.getDbName());
        return sb.toString();
    }

    @Override
    public String getFieldInnerPropertyName(MetaField field, String innerField) {
        StringBuilder sb = new StringBuilder();
        sb.append(field.getValueDbName());
        sb.append('.');
        sb.append(innerField);
        return sb.toString();
    }

	@Override
	public boolean isIndexUsable(ISearchField field, MetaClass metaClass) {
		Collection<IndexInfo> indexes = metaClass.getIndexesOnField(field.getFieldName());
		for (IndexInfo index : indexes) {
            String firstKey = index.getKeyList().get(0);
            MetaField metaField = metaClass.getFieldByName(firstKey);
			String physicalKeyName = getFieldValueName(metaField);
			if (metaField.getDataType().equals(DataTypeEnum.RELATIONSHIP)) {
				physicalKeyName = metaField.getValueDbName() + AbstractEntityIDHelper.DOT + InternalFieldEnum.ID.getDbName();
			}
            if (physicalKeyName.equals(field.getFullDbName())) {
                return true;
            }
        }
        return false;
	}

}
