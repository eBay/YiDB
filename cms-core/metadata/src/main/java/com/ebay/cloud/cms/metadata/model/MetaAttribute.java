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

package com.ebay.cloud.cms.metadata.model;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.factory.ScriptEngineProvider;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.utils.EqualsUtil;


public class MetaAttribute extends MetaField {
	
    private final static IExpressionEngine EXPR_ENGINE = ScriptEngineProvider.getEngine();
    
	private String defaultValue;
	private List<String> enumValues = new LinkedList<String>();
	private String expression;
	private String validation;
	
	private IExpression compiledExpression;
	private IExpression compiledValidation;
	
	public MetaAttribute() {
	    this(false);
	}
	
	public MetaAttribute(boolean intern) {
	    super(intern);
        super.setDataType(DataTypeEnum.STRING);
	}
	
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @JsonIgnore
    public Object getValidatedDefaultValue() {
        Object convertedValue = defaultValue;
        if (defaultValue != null) {
            switch (getDataType()) {
            case INTEGER:
                try {
                    convertedValue = Integer.valueOf(defaultValue);
                } catch (NumberFormatException e) {
                    throw new IllegalMetaClassException("default value of Integer format error: " + defaultValue, e);
                }
                break;
            case JSON:
                throw new IllegalMetaClassException("json field's default value must be null");
            case LONG:
                try {
                    convertedValue = Long.valueOf(defaultValue);
                } catch (NumberFormatException e) {
                    throw new IllegalMetaClassException("default value of long format error: " + defaultValue , e);
                }
                break;
            case STRING:
                break;
            case DOUBLE:
                try {
                    convertedValue = Double.valueOf(defaultValue);
                } catch (NumberFormatException e) {
                    throw new IllegalMetaClassException("default value of double format error: " + defaultValue, e);
                }
                break;
            case BOOLEAN:
                if (!defaultValue.equals("true") && !defaultValue.equals("false")) {
                    throw new IllegalMetaClassException("default value of boolean format error");
                }
                convertedValue = Boolean.valueOf(defaultValue);
                break;
            case DATE:
                if (!defaultValue.equals(MetaField.DATE_DEFAULT_VALUE)) {
                    throw new IllegalMetaClassException("default value of DATE can only be null or Now");
                }
                break;
            case ENUM:
                HashSet<String> set = new HashSet<String>();
                set.addAll(getEnumValues());
                if (!set.contains(defaultValue)) {
                    throw new IllegalMetaClassException("default value for enum must be one of enum values");
                }
                break;
            default:
                throw new IllegalMetaClassException("unknow datatype: " + getDataType().toString());
            }
        }
        return convertedValue;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }
    
    public void addEnumValue(String enumValue) {
        this.enumValues.add(enumValue);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
        if (expression != null) {
            this.compiledExpression = EXPR_ENGINE.compile(expression);
        }        
    }

    @JsonIgnore
    public IExpression getCompiledExpression() {
        return compiledExpression;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
        if (validation != null) {
            this.compiledValidation = EXPR_ENGINE.compile(validation);
        }   
    }
    
    @JsonIgnore
    public IExpression getCompiledValidation() {
        return compiledValidation;
    }

    @Override
    public String toString() {
        return "MetaAttribute [name=" + getName() + ", defaultValue=" + defaultValue + ", enumValues=" + enumValues + ", expression="
                + expression + ", validation=" + validation + "]";
    }


    public void validate(boolean readOnlyCheck) {
        if(getDataType() == DataTypeEnum.ENUM) {
            if (getEnumValues().isEmpty()) {
                throw new IllegalMetaClassException("enum values not defined");
            }

            HashSet<String> set = new HashSet<String>();
            set.addAll(getEnumValues());
            if (set.size() != getEnumValues().size()) {
                throw new IllegalMetaClassException("duplicate enum values found");
            }
            if (set.contains("")) {
                throw new IllegalMetaClassException("enum value can not be empty string");
            }
        } else {
        	if (!getEnumValues().isEmpty()) {
        		throw new IllegalMetaClassException("enum values only occurs when data type is ENUM");
        	}
        }
        
        if (!readOnlyCheck) {
            validateDbName();
        }
        
        //validate default value
        getValidatedDefaultValue();
        
        //expression and validation properties validation
        if (expression != null && !expression.isEmpty() && isMandatory()) {
            throw new IllegalMetaClassException("expression field can not be mandatory");
        }
    }
    
    @Override
    public int hashCode() {
        if (getName() == null) {
            return "MetaAttribute".hashCode();
        }
        
        return getName().hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (this == other) return true;
        
        if (!super.equals(other)) {
            return false;
        }
        
        if (!(other instanceof MetaAttribute)) return false;
        
        MetaAttribute o = (MetaAttribute)other;
        return EqualsUtil.equal(defaultValue, o.defaultValue) 
                && EqualsUtil.equal(expression, o.expression)
                && EqualsUtil.equal(isConstant(), o.isConstant())
                && EqualsUtil.equal(validation, o.validation) 
                && EqualsUtil.isEquals(enumValues, o.enumValues);
    }

    @Override
    public boolean isOverridable(MetaField parentAttr) {
        if (!(parentAttr instanceof MetaAttribute)) {
            return false;
        }
        
        if (!super.isOverridable(parentAttr)) {
            return false;
        }
        
        MetaAttribute o = (MetaAttribute)parentAttr;
        return EqualsUtil.equal(isConstant(), o.isConstant())
        		&& !this.equals(o);
    }
}
