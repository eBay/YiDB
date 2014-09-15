/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author liasu
 * 
 */
class JsonObject extends AbstractCMSEntity {

    private final Map<String, Object> fieldsMap;
    private final Set<String> dirtyFlag;
    private boolean enableDirtyCheck;

    public JsonObject() {
        fieldsMap = new HashMap<String, Object>();
        dirtyFlag = new HashSet<String>();
        addInternalFlag();
        enableDirtyCheck = true;
    }

    private void addInternalFlag() {
        dirtyFlag.add("_oid");
        dirtyFlag.add("_type");
        // only _oid and _type is mandatory.
//        dirtyFlag.add("_branch");
//        dirtyFlag.add("_version");
//        dirtyFlag.add("_createTime");
//        dirtyFlag.add("_lastmodified");
//        dirtyFlag.add("_status");
//        dirtyFlag.add("_pversion");
//        dirtyFlag.add("_comment");
//        dirtyFlag.add("_user");
//        dirtyFlag.add("_modifier");
//        dirtyFlag.add("_creator");
    }

    // /generic entity methods

    public boolean hasField(String fieldName) {
        return fieldsMap.containsKey(fieldName);
    }

    public Object getFieldValue(String fieldName) {
        return fieldsMap.get(fieldName);
    }

    public Collection<String> getFieldNames() {
        return Collections.unmodifiableCollection(fieldsMap.keySet());
    }

    @SuppressWarnings("unchecked")
    public void addFieldValue(String fieldName, Object fieldValue) {
        List<Object> fieldValues = null;
        if (fieldsMap.containsKey(fieldName)) {
            Object existingValue = fieldsMap.get(fieldName);
            if (!(existingValue instanceof List)) {
                throw new RuntimeException(MessageFormat.format(
                        "addFieldValue is called, but the exisitng value is not a collection, fieldName ''{0}''",
                        fieldName));
            }
            fieldValues = (List<Object>) existingValue;
        } else {
            fieldValues = new ArrayList<Object>();
            fieldsMap.put(fieldName, fieldValues);
        }

        dirtyFlag.add(fieldName);
        fieldValues.add(fieldValue);
    }

    public void setFieldValue(String fieldName, Object fieldValue) {
        fieldsMap.put(fieldName, fieldValue);
        dirtyFlag.add(fieldName);
    }

    @Override
    public void removeFieldValue(String fieldName) {
        fieldsMap.remove(fieldName);
        removeDirtyFlag(fieldName);
    }

    private void removeDirtyFlag(String fieldName) {
        if (!fieldName.startsWith("_")) {
            dirtyFlag.remove(fieldName);
        }
    }

    public void traverse(ICMSEntityVisitor visitor) {
        // detect attributes and reference
        Iterator<Entry<String, Object>> entryItr = fieldsMap.entrySet().iterator();

        List<Entry<String, Object>> attributeList = new ArrayList<Map.Entry<String, Object>>();
        List<Entry<String, Object>> referenceList = new ArrayList<Map.Entry<String, Object>>();

        while (entryItr.hasNext()) {
            Entry<String, Object> entry = entryItr.next();
            if (isReference(entry.getValue())) {
                referenceList.add(entry);
            } else {
                attributeList.add(entry);
            }
        }

        for (Entry<String, Object> attrEntry : attributeList) {
            visitor.processAttribute(this, attrEntry.getKey());
        }

        for (Entry<String, Object> attrEntry : referenceList) {
            visitor.processReference(this, attrEntry.getKey());
        }

    }

    @SuppressWarnings("rawtypes")
    private boolean isReference(Object value) {
        Object val = value;
        if (value instanceof List) {
            List list = (List) value;
            if (list.size() > 0) {
                val = list.get(0);
            } else {
                val = null;
            }
        }
        if (val == null) {
            return false;
        }
        return val instanceof ICMSEntity;
    }

    public Date getDateField(String field) {
        Object o = getFieldValue(field);
        if (o instanceof Long) {
            return new Date((Long) o);
        }
        return (Date) getFieldValue(field);
    }

    public void setDateField(String fieldName, Date d) {
        setFieldValue(fieldName, d);
    }
    
    public Long getLongField(String field) {
        Object o = getFieldValue(field);
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return (Long) getFieldValue(field);
    }

    public void setLongField(String fieldName, Long d) {
        setFieldValue(fieldName, d);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    @Override
    public void includeFields(String... fieldNames) {
        if (fieldNames != null && fieldNames.length > 0) {
            Set<String> incluedFields = new HashSet<String>(Arrays.asList(fieldNames));
            incluedFields.add("_type");
            incluedFields.add("_oid");
            Iterator<String> it = this.fieldsMap.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (!incluedFields.contains(key)) {
                    it.remove();
                    removeDirtyFlag(key);
                } else {
                    dirtyFlag.add(key);
                }
            }
        }
    }

    @Override
    public void excludeFields(String... fieldNames) {
        if (fieldNames != null && fieldNames.length > 0) {
            for (String field : fieldNames) {
                if (!"_type".equals(field) && !"_oid".equals(field)) {
                    this.fieldsMap.remove(field);
                    removeDirtyFlag(field);
                }
            }
            // exclude means include field other than the exlude list : think
            // about the case, user get an entity(clean),
            // then call exclude try to include field other than the exclude
            // list
            for (String key : this.fieldsMap.keySet()) {
                dirtyFlag.add(key);
            }
        }
    }

    public void clearDirtyBits() {
        // recursively clear all the embed object's diry bits
        for (Object o : fieldsMap.values()) {
            if (o instanceof ICMSEntity) {
                ((ICMSEntity) o).clearDirtyBits();
            } else if (o instanceof Collection) {
                // check array fields
                for (Object oInColl : (Collection<?>) o) {
                    if (oInColl instanceof ICMSEntity) {
                        ((ICMSEntity) oInColl).clearDirtyBits();
                    }
                }
            }
        }
        dirtyFlag.clear();
        addInternalFlag();
    }

    public void disableDirtyCheck() {
        enableDirtyCheck = false;
    }

    public void enableDirtyCheck() {
        enableDirtyCheck = true;
    }

    @Override
    public boolean isDirty(String fieldName) {
        return dirtyFlag.contains(fieldName);
    }

    @Override
    public boolean isDirtyCheckEnabled() {
        return enableDirtyCheck;
    }

    @Override
    public Set<String> getDirtyFields() {
        return new HashSet<String>(this.dirtyFlag);
    }

    @Override
    public void setDirtyFields(Set<String> fields) {
        this.dirtyFlag.clear();
        addInternalFlag();
        this.dirtyFlag.addAll(fields);
    }

}
