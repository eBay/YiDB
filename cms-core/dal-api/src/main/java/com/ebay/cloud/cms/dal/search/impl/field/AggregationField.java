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
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * aggregation field that 
 * 
 * 
 * @author xjiang
 *
 */
public class AggregationField implements ISearchField {

    public enum AggFuncEnum {
        MIN("min"),
        MAX("max"),
        SUM("sum"),
        AVG("avg"),
        COUNT("count");
        
        private final String name;
        private AggFuncEnum(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public final AggFuncEnum    func;
    public final AbstractSearchField    field;
    public final String         aliasName;
    
    public AggregationField(AggFuncEnum func, ISearchField searchField) {
        AbstractSearchField field = (AbstractSearchField) searchField;
        //validation check
        this.validate(func, field);
        this.func = func;
        this.field = field;
        this.aliasName = getAggregationName(field);
    }

    private void validate(AggFuncEnum func2, AbstractSearchField aggreField) {
        if (aggreField != null) {
            CheckConditions.checkArgument(aggreField.getRootField().getDataType() != DataTypeEnum.RELATIONSHIP,
                    "Invalid aggregation on referece type!");
            CheckConditions.checkArgument(aggreField.getRootField().getDataType() != DataTypeEnum.JSON, "Invalid aggregation on json type!");
        }
        switch (func2) {
            case AVG:
                //pass through
            case SUM:
                DataTypeEnum dte = aggreField.getRootField().getDataType();
                CheckConditions.checkArgument(
                        (dte == DataTypeEnum.LONG || dte == DataTypeEnum.INTEGER || dte == DataTypeEnum.DOUBLE),
                        MessageFormat.format("{0} function could only applied on long/integer/double fields while field {1} is {2}", func2.getName(), aggreField.getFieldName(), dte.toString()));
                break;
            case COUNT:
                CheckConditions.checkArgument(aggreField == null, "count function can't have parameter. ");
                break;
            case MAX:
                //pass through
            case MIN:
                CheckConditions.checkArgument(aggreField != null, MessageFormat.format("{0} function must have a field parameter.", func2.getName()));
                break;
            default:
                break;
        }
    }

    private String getAggregationName(AbstractSearchField field) {
        StringBuilder sb = new StringBuilder("_");
        sb.append(func.getName());
        if (field != null) {
            sb.append("_");
            sb.append(field.getFieldName());
        }
        return sb.toString();
    }

    @Override
    public String getFullDbName() {
        return aliasName;
    }

    @Override
    public String getFieldName() {
        return aliasName;
    }

    public AbstractSearchField getSearchField() {
        return field;
    }

    @Override
    public String toString() {
        return "AggregationField [func=" + func + ", field=" + field
                + ", aliasName=" + aliasName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((aliasName == null) ? 0 : aliasName.hashCode());     
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
        AggregationField other = (AggregationField) obj;
        if (aliasName != null && !aliasName.equals(other.aliasName))
            return false;
        return true;
    }

    @Override
    public List<?> getSearchValue(IEntity entity) {
        List<Object> result = new ArrayList<Object>(1);
        result.add(((Map<?, ?>)((IEntity) entity).getNode()).get(aliasName));
        return result;
    }

    @Override
    public void setEmbedPath(String embedPath) {
        if (field != null) {
            field.setEmbedPath(embedPath);
        }
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
