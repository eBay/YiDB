/**
 * 
 */
package com.ebay.cloud.cms.typsafe.metadata.model;

import java.util.LinkedList;
import java.util.List;

/**
 * @author liasu
 * 
 */
public class MetaAttribute extends MetaField {
    private String       defaultValue;
    private List<String> enumValues = new LinkedList<String>();
    private String       expression;
    private String       validation;

    public final String getDefaultValue() {
        return defaultValue;
    }

    public final void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public final List<String> getEnumValues() {
        return enumValues;
    }
    public void addEnumValue(String value) {
        enumValues.add(value);
    }
    public final void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }

    public final String getExpression() {
        return expression;
    }
    public final void setExpression(String expression) {
        this.expression = expression;
    }

    public final String getValidation() {
        return validation;
    }
    public final void setValidation(String validation) {
        this.validation = validation;
    }

}
