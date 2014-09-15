/**
 * 
 */
package com.ebay.cloud.cms.typsafe.metadata.model;

import java.text.MessageFormat;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.ebay.cloud.cms.typsafe.entity.DataTypeConverter.DataTypeDeserializer;
import com.ebay.cloud.cms.typsafe.entity.DataTypeConverter.DataTypeSerializer;

/**
 * @author liasu
 * 
 */
public class MetaField {
    public enum DataTypeEnum {
        INTEGER("integer"), LONG("long"), STRING("string"), JSON("json"), DOUBLE("double"), BOOLEAN("boolean"), DATE(
                "date"), ENUM("enumeration"), RELATIONSHIP("relationship");

        private String value;

        DataTypeEnum(String value) {
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
        One, Many;
        public static CardinalityEnum fromString(String car) {
            if (car == null) {
                return One;
            }
            for (CardinalityEnum ce : CardinalityEnum.values()) {
                if (car.equals(ce.name())) {
                    return ce;
                }
            }
            return One;
        }
    }

    private String          name;
    private String          description;
    private DataTypeEnum    dataType;
    private CardinalityEnum cardinality;
    private boolean         mandatory;
    private boolean         constant;
    private boolean         internal;

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    @JsonSerialize(using = DataTypeSerializer.class)
    public final DataTypeEnum getDataType() {
        return dataType;
    }
    @JsonDeserialize(using = DataTypeDeserializer.class)
    public final void setDataType(DataTypeEnum dataType) {
        this.dataType = dataType;
    }

    public final CardinalityEnum getCardinality() {
        return cardinality;
    }

    public final void setCardinality(CardinalityEnum cardinality) {
        this.cardinality = cardinality;
    }

    public final boolean isMandatory() {
        return mandatory;
    }

    public final void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public final boolean isConstant() {
        return constant;
    }

    public final void setConstant(boolean constant) {
        this.constant = constant;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

}
