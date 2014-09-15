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

package com.ebay.cloud.cms.dal.search.impl.field;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * basic search field that support basic field and embedded field
 * 
 * @author xjiang
 * 
 */
public abstract class AbstractSearchField implements ISearchField {

    private final MetaField       rootField;
    private final FieldProperty   fieldProperty;
    private final ISearchStrategy strategy;
    private String                embedPath;
    private String                fullDbName;
    private String                innerField;

    public AbstractSearchField(MetaField rootField, String innerField, ISearchStrategy strategy) {
        this.rootField = rootField;
        this.innerField = innerField;
        this.fieldProperty = FieldProperty.fromQueryName(innerField);
        this.strategy = strategy;
        if (!InternalFieldEnum.ID.getDbName().equals(innerField) && innerField != null
                && rootField.getDataType() != DataTypeEnum.JSON) {
            CheckConditions.checkArgument(fieldProperty != null, MessageFormat.format("field property {0} invalid!", innerField));
        }
        initFullDbName();
    }

    private void initFullDbName() {
        StringBuilder sb = new StringBuilder();
        if (embedPath != null) {
            sb.append(embedPath);
        }
        if (innerField == null) {
            sb.append(strategy.getFieldValueName(rootField));
        } else if (fieldProperty != null) {
            sb.append(strategy.getFieldPropertyName(rootField, fieldProperty));
        } else {
            sb.append(strategy.getFieldInnerPropertyName(rootField, innerField));
        }
        fullDbName = sb.toString();
    }

    public AbstractSearchField(MetaField rootField, ISearchStrategy strategy) {
        this(rootField, null, strategy);
    }

    public MetaField getRootField() {
        return rootField;
    }

    public String getRootFieldDbName() {
        return strategy.getFieldValueName(rootField);
    }
    
    public String getRootFieldElemMatchDbName() {
        if (innerField == null) {
            return strategy.getFieldValueName(rootField);
        } else if (fieldProperty != null) {
            return strategy.getFieldPropertyName(rootField, fieldProperty);
        } else {
            return strategy.getFieldInnerPropertyName(rootField, innerField);
        }
    }

    public String getInnerField() {
        return innerField;
    }

    public FieldProperty getInnerProperty() {
        return fieldProperty;
    }

    @Override
    public String getFullDbName() {
        return fullDbName;
    }

    @Override
    public String getFieldName() {
        return rootField.getName();
    }
    
    public void setEmbedPath(String embedPath) {
        this.embedPath = embedPath;
        initFullDbName();
    }
    
    public void setInnerField(String innerField) {
        this.innerField = innerField;
        initFullDbName();
    }

    @Override
    public String toString() {
        return "SearchField [rootField=" + rootField + ", innerField=" + fieldProperty + ", fullDbName=" + fullDbName
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = rootField.hashCode();
        result = prime * result + (fullDbName == null ? 0 : fullDbName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractSearchField other = (AbstractSearchField) obj;
        return fullDbName != null && fullDbName.equals(other.fullDbName);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<?> getSearchValue(IEntity entity) {
        List valueList = Collections.EMPTY_LIST;
        if (getInnerProperty() != null) {
            Object proertyValue = entity.getFieldProperty(getFieldName(), fieldProperty.getName());
            if (proertyValue != null) {
                valueList = new ArrayList();
            	valueList.add(proertyValue);
            }
        } else if (getInnerField() != null) {
        	if (InternalFieldEnum.ID.getDbName().equals(innerField)) {
        		List<IEntity> fieldValues = (List<IEntity>) entity.getFieldValues(getFieldName());
        		if (fieldValues != Collections.EMPTY_LIST) {
                    valueList = new ArrayList();
                    for (IEntity e : fieldValues) {
                        valueList.add(e.getFieldValues(InternalFieldEnum.ID.getName()).get(0));
                    }
        		}
            } else {
                List<?> fieldValues = entity.getFieldValues(getFieldName());
                String[] innerFields = StringUtils.split(innerField, ".");
                if (fieldValues != Collections.EMPTY_LIST) {
                    valueList = new ArrayList();
                    traverseFields(fieldValues, 0, innerFields, valueList);
                }
            }
        } else {
            valueList = entity.getFieldValues(getFieldName());
        }
        return valueList;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void traverseFields(List fieldValues, int i, final String[] innerFields, List targetList) {
        if (i == innerFields.length) {
            // match all, add to target list
            targetList.addAll(fieldValues);
            return;
        }

        String currentField = innerFields[i];
        for (Object obj : fieldValues) {
            if (obj instanceof Map) {
                Object currentValue = ((Map) obj).get(currentField);
                List valueContainer = new ArrayList();
                valueContainer.clear();
                valueContainer.add(currentValue);
                traverseFields(valueContainer, i + 1, innerFields, targetList);
            } else if (obj instanceof List) {
                traverseFields((List) obj, i, innerFields, targetList);
            }
        }
    }
    
    public String getEmbedPath() {
        return embedPath;
    }

}
