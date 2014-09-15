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

package com.ebay.cloud.cms.dal.search.impl.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * A group field is nothing more than a normal field that used in a group
 * search.
 * 
 * @author liasu
 * 
 */
public class GroupField implements ISearchField {

    private static final String _ID = "_id";
    private final AbstractSearchField searchFiled;
    private final String              groupFieldName;

    public GroupField(MetaField rootField, ISearchStrategy strategy) {
        validate(rootField);
        searchFiled = new SelectionField(rootField, strategy);
        groupFieldName = _ID + "." + searchFiled.getFieldName();
    }

    public GroupField(ISearchField searchField) {
        AbstractSearchField field = (AbstractSearchField) searchField;
        validate(field.getRootField());
        searchFiled = field;
        groupFieldName = _ID + "." + searchFiled.getFieldName();
    }

    private void validate(MetaField rootField) {
        CheckConditions.checkNotNull(rootField, "group field can not be null!");
        CheckConditions.checkArgument(rootField.getDataType() != DataTypeEnum.RELATIONSHIP,
                "Invalid group on referece type!");
        CheckConditions.checkArgument(rootField.getDataType() != DataTypeEnum.JSON, "Invalid group on json type!");
    }

    @Override
    public String getFullDbName() {
        return groupFieldName;
    }

    @Override
    public String getFieldName() {
        return searchFiled.getFieldName();
    }


    /**
     * Return the raw field the this group field refer to.
     * 
     * @return
     */
    public AbstractSearchField getSearchFiled() {
        return searchFiled;
    }

    @Override
    public String toString() {
        return "Group field:{ " + searchFiled.toString() + "}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        return result * prime + groupFieldName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return groupFieldName != null && groupFieldName.equals(((GroupField) obj).groupFieldName);
    }

    @Override
    public List<?> getSearchValue(IEntity entity) {
        Map<?, ?> idStructure = (Map<?, ?>) ((Map<?, ?>) entity.getNode()).get(_ID);
        if (idStructure == null || !idStructure.containsKey(getFieldName())) {
            return Collections.emptyList();
        }
        Object obj = idStructure.get(getFieldName());
        if (obj == null) {
            return Collections.emptyList();
        } else if (obj instanceof List) {
            return (List<?>) obj;
        } else {
            return Arrays.asList(obj);
        }
    }

    @Override
    public void setEmbedPath(String embedPath) {
        searchFiled.setEmbedPath(embedPath);
    }

    @Override
    public boolean isProjected() {
        return true;
    }

    @Override
    public String getInnerField() {
        return null;
    }

    @Override
    public FieldProperty getInnerProperty() {
        return null;
    }

}
