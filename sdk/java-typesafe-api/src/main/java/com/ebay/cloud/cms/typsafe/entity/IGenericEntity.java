/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.util.Collection;
import java.util.Set;

/**
 * Interfaces defined the generic methods to get/set fields
 * 
 * @author liasu
 * 
 */
public interface IGenericEntity {

    boolean hasField(String fieldName);
    Collection<String> getFieldNames();

    Object getFieldValue(String fieldName);

    void addFieldValue(String fieldName, Object fieldValue);
    void setFieldValue(String fieldName, Object fieldValue);
    void removeFieldValue(String fieldName);
    
    void includeFields(String... fieldNames);
    void excludeFields(String... fieldNames);

    void traverse(ICMSEntityVisitor visitor);
    
    public boolean isDirty(String fieldName);
    public boolean isDirtyCheckEnabled();
    public void clearDirtyBits();
    public void enableDirtyCheck();
    public void disableDirtyCheck();
    public Set<String> getDirtyFields();
    /**
     * Enforce the dirty fields to be the given ones.
     */
    public void setDirtyFields(Set<String> fields);
}
