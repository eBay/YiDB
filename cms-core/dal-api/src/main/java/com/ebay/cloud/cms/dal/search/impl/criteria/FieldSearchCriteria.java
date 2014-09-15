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

package com.ebay.cloud.cms.dal.search.impl.criteria;

import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DBObject;

/**
 * This class define the criteria on field 
 * 
 * @author xjiang
 * 
 */
public class FieldSearchCriteria implements SearchCriteria {

    public enum FieldOperatorEnum {
        EQ, NE, GT, LT, GE, LE, 
        IN(2), NIN(2), 
        REGEX, NREGEX, 
        CONTAINS(0), NCONTAINS(0),
        ISEMPTY(0), NISEMPTY(0),
        ISNULL(0), NISNULL(0)
        ;

        private int valueScalar;

        private FieldOperatorEnum(final int scalar) {
            valueScalar = scalar;
        }

        private FieldOperatorEnum() {
            this(1);
        }

        public int getScalar() {
            return valueScalar;
        }
    }

    public static class RegexValue {
        public String  value;
        public boolean caseSensitive = false;

        public RegexValue() {
        }
        public RegexValue(String value) {
            this.value = value;
            this.caseSensitive = false;
        }
        public RegexValue(String value, boolean caseSensitive) {
            this.value = value;
            this.caseSensitive = caseSensitive;
        }
    }

    public static interface IFieldValueProvider {
        Object getValue();

        List<Object> getValueList();
    }
    
    
    public static class StaticValueProvider implements IFieldValueProvider {
        private final Object value;
        private final List<Object> valueList;

        StaticValueProvider(Object value) {
            this.value = value;
            this.valueList = null;
        }

        StaticValueProvider(List<Object> valueList) {
            this.value = null;
            this.valueList = valueList;
        }

        StaticValueProvider(Object value, List<Object> valueList) {
            this.value = value;
            this.valueList = valueList;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public List<Object> getValueList() {
            return valueList;
        }

    }

    private final ISearchField searchField;
    private final FieldOperatorEnum operator;
    private IFieldValueProvider valueProvider;
    private IFieldCriteriaHandler handler;
    private boolean isEmbeddedObject;
    private boolean isEmbeddedCardinalityMany;

    public FieldSearchCriteria(final MetaField metaField, ISearchStrategy strategy, final FieldOperatorEnum operator, Object value) {
        this(new SelectionField(metaField, strategy), operator, value);
    }

    protected FieldSearchCriteria(final ISearchField searchField, final FieldOperatorEnum operator,
            final Object value, final List<Object> valueList) {        
        this.searchField = searchField;
        this.operator = operator;
        this.valueProvider = new StaticValueProvider(value, valueList);
        this.handler = FieldCriteriaHandlerFactory.getHandler(this.operator);
        
        if (searchField instanceof SelectionField && this.operator.getScalar() == 0) {
            SelectionField sField = (SelectionField)searchField;
            sField.setInnerField(null);
        }
    }
    
    public FieldSearchCriteria(final ISearchField searchField, final FieldOperatorEnum operator) {
        this(searchField, operator, null, null);
        CheckConditions.checkArgument(operator.getScalar() == 0,
                "operator %s don't support empty value!", operator);
    }
    
    public FieldSearchCriteria(final ISearchField searchField, final FieldOperatorEnum operator,
            Object value) {
        this(searchField, operator, value, null);
        CheckConditions.checkArgument(operator.getScalar() == 1,
                "operator %s don't support single value!", operator);
    }
    
    public FieldSearchCriteria(final ISearchField searchField, final FieldOperatorEnum operator,
            List<Object> valueList) {
        this(searchField, operator, null, valueList);
        CheckConditions.checkArgument(operator.getScalar() == 2,
                "operator %s don't support multiple value!", operator);
    }

    public ISearchField getSearchField() {
        return searchField;
    }

    public String getFieldName() {
        return searchField.getFieldName();
    }
    
    public String getFullDbName() {
        return searchField.getFullDbName();
    }

    public FieldOperatorEnum getOperator() {
        return operator;
    }

    public Object getValue() {
        return valueProvider.getValue();
    }

    public List<Object> getValueList() {
        return valueProvider.getValueList();
    }

    public void setValueProvider(IFieldValueProvider valueProvider) {
        this.valueProvider = valueProvider;
    }
    
    public boolean isEmbeddedObject() {
        return isEmbeddedObject;
    }

    public void setEmbeddedObject(boolean isEmbeddedObject) {
        this.isEmbeddedObject = isEmbeddedObject;
    }
    
    public boolean isEmbeddedCardinalityMany() {
        return isEmbeddedCardinalityMany;
    }

    public void setEmbeddedCardinalityMany(boolean isEmbeddedCardinalityMany) {
        this.isEmbeddedCardinalityMany = isEmbeddedCardinalityMany;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public List<SearchCriteria> getChildren() {
        return Collections.EMPTY_LIST;
    }

    /**
     * translate to mongodb query
     */
    @Override
    public DBObject translate(final List<DBObject> subQueryList) {
        return handler.translate(this);
    }

    @Override
    public boolean evaluate(final IEntity entity) {
        List<?> fieldValues = searchField.getSearchValue(entity);
        if (fieldValues == null) {
            return false;
        }
        
        if (fieldValues.isEmpty() && this.operator == FieldOperatorEnum.NISNULL) {
            MetaField metaField = entity.getMetaClass().getFieldByName(searchField.getFieldName());
            CardinalityEnum cardinality = metaField.getCardinality();
            boolean flag = entity.hasField(searchField.getFieldName());
            if (cardinality == CardinalityEnum.Many && flag && fieldValues.isEmpty()) { // array
                return true;
            }
        }
        return handler.evaluate(this, fieldValues);
    }
    
}
