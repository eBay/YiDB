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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.mongo.converter.MetaClassConverters;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.EqualsUtil;
import com.ebay.cloud.cms.utils.StringUtils;

public abstract class MetaField {
    
    public final static String DATE_DEFAULT_VALUE   = "$NOW";
    public final static String VALUE_KEY            = "v";
    public final static String VALUE_KEY_CONNECTOR  = "_";
    private final static Pattern INVALID_PATTERN = Pattern.compile("^_[A-Za-z0-9].*");
    private final static List<String> RESERVED_FIELD_NAME = Arrays.asList("hierarchy", "relationships");

    public static enum FieldProperty {
        LENGTH("l", DataTypeEnum.INTEGER, "_length"),

        TIMESTAMP("t", DataTypeEnum.DATE, "_lastmodified");

        private final String       dbName;
        private final DataTypeEnum type;
        private final String       queryName;

        FieldProperty(String dbName, DataTypeEnum type, String queryName) {
            this.type = type;
            this.dbName = dbName;
            this.queryName = queryName;
        }

        public final String getDbName() {
            return dbName;
        }

        public final String getName() {
            return queryName;
        }

        public final DataTypeEnum getType() {
            return type;
        }

        public static FieldProperty fromQueryName(String q) {
            if (q == null || q.length() == 0) {
                return null;
            }
            for (FieldProperty prop : FieldProperty.values()) {
                if (prop.getName().equals(q)) {
                    return prop;
                }
            }
            return null;
        }
    }
    
	public enum DataTypeEnum {

		INTEGER("integer"),
		LONG("long"),
		STRING("string"),
		JSON("json"),
		DOUBLE("double"),
		BOOLEAN("boolean"),
		DATE("date"),
		ENUM("enumeration"),
		RELATIONSHIP("relationship");
		
		private String value;
		
		DataTypeEnum(String value){
			this.value = value;
		}

		@Override
		public String toString() {
		    return value;
		}
		
		public static DataTypeEnum fromString(String value) {
		    if (value == null || value.length() < 1) {
		        throw new IllegalArgumentException();
		    }
		    
		    for (DataTypeEnum v : DataTypeEnum.values()) {
		        if (v.toString().equals(value)) {
		            return v;
		        }
		    }
		    
		    throw new IllegalArgumentException(MessageFormat.format("Invalid date type {0}", value));
		}

	}
	
	public static enum CardinalityEnum {
	    One, Many
	}
	
	private String name;
	private String description;
	private String dbName;
	private String valueDbName;
	private String flattenValueDbName;
	
	private DataTypeEnum dataType;
	private CardinalityEnum cardinality;
	
	private boolean mandatory;
	private boolean internal;
	private boolean constant;
	private boolean virtual;

	public MetaField(boolean isIntern) {
	    internal = isIntern;
	    cardinality = CardinalityEnum.One;
	}
	
    public MetaField(MetaField other) {
        this.name = other.name;
        this.description = other.description;
        this.dbName = other.dbName;
        this.valueDbName = other.valueDbName;
        this.flattenValueDbName = other.flattenValueDbName;
        this.dataType = other.dataType;
        this.cardinality = other.cardinality;
        this.mandatory = other.mandatory;
        this.internal = other.internal;
        this.constant = other.constant;
        this.virtual = other.virtual;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
        if (isInternal()) {
            this.valueDbName = dbName;
            this.flattenValueDbName = valueDbName;
        } else {
            this.valueDbName = dbName + "." + VALUE_KEY;
            this.flattenValueDbName = dbName + VALUE_KEY_CONNECTOR + VALUE_KEY;
        }
    }
    
    public String getValueDbName() {
        return valueDbName;
    }
    public void setValueDbName(String valueDbName) {
        this.valueDbName = valueDbName;
    }
    
    public String getFlattenValueDbName() {
        return flattenValueDbName;
    }
    public void setFlattenValueDbName(String flattenDbName) {
        this.flattenValueDbName = flattenDbName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @JsonSerialize(using = MetaClassConverters.DataTypeEnumSerializer.class)
    public DataTypeEnum getDataType() {
        return dataType;
    }

    @JsonDeserialize(using = MetaClassConverters.DataTypeEnumDeserializer.class)
    public final void setDataType(DataTypeEnum dataType) {
        this.dataType = dataType;
    }

    public CardinalityEnum getCardinality() {
        return cardinality;
    }

    public void setCardinality(CardinalityEnum cardinality) {
        this.cardinality = cardinality;
    }

    @JsonIgnore
    public boolean isInternal() {
        return internal;
    }

    public boolean isConstant() {
        return constant;
    }
    
    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    // internal method for extension
    @JsonIgnore
    protected void setInternal(boolean internal) {
        this.internal = internal;
    }
    @JsonIgnore
    public boolean isVirtual() {
        return virtual;
    }
    @JsonIgnore
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }
    
    @Override
    public int hashCode() {
        if (getName() == null) {
            return "MetaField".hashCode();
        }
        
        return getName().hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        
        if (!(other instanceof MetaField)) return false;
        
        MetaField o = (MetaField)other;
        return EqualsUtil.equal(name, o.name) 
                && (dataType == o.dataType)
                && (cardinality == o.cardinality) 
                && (mandatory == o.mandatory)
                && EqualsUtil.equal(description, o.description);
    }

    public boolean isOverridable(MetaField parentAttr) {
        return EqualsUtil.equal(name, parentAttr.name) 
                && (dataType == parentAttr.dataType)
                && (cardinality == parentAttr.cardinality) 
                && EqualsUtil.equal(description, parentAttr.description);
    }
    
    public void validate() {
        CheckConditions.checkCondition(!RESERVED_FIELD_NAME.contains(getName()), new IllegalMetaClassException(String.format("Reserved meta field name %s!", getName())));
    	CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(getName()), new IllegalMetaClassException("meta field name can not be empty"));
    	CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(getName().trim()), new IllegalMetaClassException("meta field name can not be empty"));
    	CheckConditions.checkCondition(org.apache.commons.lang.StringUtils.containsNone(getName(),CMSConsts.INVALID_META_FIELD_NAME_CHARACTERS), new IllegalMetaClassException("meta field " + name + " cannot contains invalid characters: " + Arrays.toString(CMSConsts.INVALID_META_FIELD_NAME_CHARACTERS)));
    	if(!isInternal()) {
    		CheckConditions.checkCondition(!INVALID_PATTERN.matcher(getName()).matches(), new IllegalMetaClassException("meta field name can not start with regex _[A-Za-z0-9]: " + getName()));
    	}
    }
    
    protected void validateDbName() {
        if(!StringUtils.isNullOrEmpty(getDbName())) {
            throw new IllegalMetaClassException(getName() + ": dbName must NOT be provided");
        }
        
        if(!StringUtils.isNullOrEmpty(getValueDbName())) {
            throw new IllegalMetaClassException(getName() + ": valueDbName must NOT be provided");
        }
        
        if(!StringUtils.isNullOrEmpty(getFlattenValueDbName())) {
            throw new IllegalMetaClassException(getName() + ": flattenValueDbName must NOT be provided");
        }
    }

    public String getFlattenPropertyValueDbName(FieldProperty fp) {
        CheckConditions.checkNotNull(fp, "fp could not be null!");
        return dbName + VALUE_KEY_CONNECTOR + fp.getDbName();
    }

    public String getPropertyValueDbName(FieldProperty fp) {
        CheckConditions.checkNotNull(fp, "fp could not be null!");
        return valueDbName + "." + fp.getDbName();
    }

}
