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

package com.ebay.cloud.cms.dal.entity.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.impl.bson.datahandler.BsonDataTypeHandlerFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * 
 * 
 * To keep a consist interface behavior for single value and list value.
 * The {@code hasField} ,{@code getFieldValue} ,{@code setFieldValue} and {@code addFieldValue} need to be used with care.
 * <br>
 * <b>
 * Use hasField to detect whether a field value is present in current entity, NOT only by checking the return value if getFieldValues
 * </b>
 * <br>
 * For non-relationship attributes:
 * <pre>
 * ------------------------------------------------------------------------------------------------------------------------
 *                  |           Cardinality=One                      |    Cardinality=Many
 * ------------------------------------------------------------------------------------------------------------------------
 * getFieldValues   | if field absent, return empty list             |   if field value is null/nullNode, return empty list
 *                  | if field is null, return list contains null    |   if field absent, return empty list
 *                  |                                                |   if field is empty list, return empty list
 * ------------------------------------------------------------------------------------------------------------------------
 * addFieldValue    | if given null, set the field as null           |   if given null/nullNode, add a null to the list
 * ------------------------------------------------------------------------------------------------------------------------
 * setFieldValues   | if given empty list, set field as null         |   if given null/nullNode, set as empty list
 *                  | if given null, set field as null               |   if given emptyList, set as empty list
 *                  | if List contains one null, set field as null   |
 * ------------------------------------------------------------------------------------------------------------------------
 * Note, we have some implication between null and empty to give more fault tolerance when encounter unexpected data.
 * 
 * <b>For relationship attributes, the null is treated as non-valid value<b>
 * 
 * </pre>
 * @author jianxu1, liasu
 *
 */
public class BsonEntity extends AbstractEntity {

	private BasicDBObject bsonObject = null;

    public BsonEntity(MetaClass metaClass){
        super(metaClass);
        bsonObject = new BasicDBObject();
        bsonObject.put(InternalFieldEnum.TYPE.getDbName(), metaClass.getName());
    }
    
    public BsonEntity(BsonEntity entity){
        super(entity.getMetaClass());
        bsonObject = new BasicDBObject((BasicDBObject)entity.getNode().copy());
    }

	public BsonEntity(MetaClass metaClass, DBObject given){
		super(metaClass);
		CheckConditions.checkArgument(given instanceof BasicDBObject, "initial entity should be BasicDBObject!");
        bsonObject = (BasicDBObject)given;
        bsonObject.put(InternalFieldEnum.TYPE.getDbName(), metaClass.getName());
	}	
	
	@Override
	public boolean hasField(String fieldName){
	    MetaField metaField = getMetaClass().getFieldByName(fieldName);
	    if (metaField == null) {
	        return false;
	    }
	    String dbName = metaField.getDbName();
		return bsonObject.containsField(dbName);
	}
	
	@Override 
	public Collection<String> getFieldNames(){
	    Collection<String> fielNames = new ArrayList<String>(bsonObject.size() * 2);
		for(String dbName : bsonObject.keySet()){
		    MetaField metaField = getMetaClass().getFieldByDbName(dbName);
		    if (metaField != null) {
		        String fieldName = metaField.getName();
	            fielNames.add(fieldName);
		    }
		}
		return fielNames;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<?> getFieldValues(String fieldName) {
        // validate the field name
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        CheckConditions.checkArgument(metaField != null, "Can't find meta field %s", fieldName);
        CheckConditions.checkNotNull(metaField.getDbName(), "Meta field doesn't have db name set!");
        // get parent bson & db name
        BasicDBObject fieldBsonObject = null;
        String dbValName = null;
        if (metaField.isInternal()) {
            dbValName = metaField.getDbName();
            fieldBsonObject = bsonObject;          
        } else {
            dbValName = MetaField.VALUE_KEY;
            fieldBsonObject = getBsonField(metaField.getDbName());
        }
        if (fieldBsonObject == null) {
            return Collections.EMPTY_LIST;
        }
        // get value from parent field
		CardinalityEnum cardinality = metaField.getCardinality();
		DataTypeEnum dataType = metaField.getDataType();
		IDataTypeHandler handler = BsonDataTypeHandlerFactory.getHandler(dataType);
		List<Object> result = Collections.EMPTY_LIST;
		// when has field
        if (fieldBsonObject.containsField(dbValName)) {
            if (cardinality == CardinalityEnum.One) {
                Object bsonValue = fieldBsonObject.get(dbValName);
                // for json type field, if data is Many while metatype changed to One,
                // it still can return not null bsonValue, but should ignore it
                if (!(bsonValue instanceof BasicDBList)) {
                    result = new ArrayList<Object>(1);
                    Object value = handler.read(this, bsonValue, metaField);
                    addReadValue(result, value);
                }
            } else {
                // cardinality = many. If there is a given non-list-value, wrapper it as list
                List<Object> bsonList = null;
                if (fieldBsonObject.get(dbValName) instanceof List) {
                    bsonList = (List<Object>) fieldBsonObject.get(dbValName);
                } else if (fieldBsonObject.get(dbValName) != null) {
                    bsonList = new ArrayList<Object>();
                    bsonList.add(fieldBsonObject.get(dbValName));
                }
                if (bsonList != null) {
                    result = new ArrayList<Object>();
                    for (Object bsonValue : bsonList) {
                        Object value = handler.read(this, bsonValue, metaField);
                        addReadValue(result, value);
                    }
                }
            }
        }
		return result;
	}

    // CMS-3041: null value for relationship fields are error-prone, don't allow null value for relationship
    // CMS-3503: null value for relationship fields should not block query result, just skip this 'bad' relationship
    // ignoring null
    private void addReadValue(List<Object> result, Object value) {
        if (value != null) {
            result.add(value);
        }
    }

    @Override
	public void addFieldValue(String fieldName, Object value) {
        putFieldValues(fieldName, value, false);
	}

    @Override
    public void setFieldValues(String fieldName, List<?> values) {
        putFieldValues(fieldName, values, true);
    }
    
    private void putFieldValues(String fieldName, Object values, boolean setFlag) {
        // validate the field name      
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        CheckConditions.checkArgument(metaField != null, "Can't find meta field %s", fieldName);
        // get field bson & db name
        BasicDBObject fieldBsonObject = null;
        String dbValName = null;
        if (metaField.isInternal()) {
            dbValName = metaField.getDbName();
            fieldBsonObject = bsonObject;
        } else {
            dbValName = MetaField.VALUE_KEY;
            fieldBsonObject = getOrNewBsonField(metaField.getDbName());
        }
        // set value to the field bson
        DataTypeEnum dataType = metaField.getDataType();
        IDataTypeHandler handler = BsonDataTypeHandlerFactory.getHandler(dataType);
        CardinalityEnum cardinality = metaField.getCardinality();      

        boolean updated = false;
        if (setFlag) {
            List<?> valuesList = (List<?>)values;
            if(cardinality == CardinalityEnum.Many){
                List<Object> bsonList = new ArrayList<Object>();
                if (values != null) {
                    for (Object val : valuesList) {
                        Object bsonValue = handler.write(this, val, metaField);
                        addWriteValue(bsonValue, bsonList);
                    }
                }
                // for list set, force udpate ignore whethere entity added, since user might want to set empty
                updated = true;
                fieldBsonObject.put(dbValName, bsonList);
            } else {
                Object givenValue = (values == null || valuesList.isEmpty()) ? null : valuesList.get(0);
                Object bsonValue = handler.write(this, givenValue, metaField);
                updated |= addWriteValue(fieldBsonObject, dbValName, bsonValue);
            }
        } else {
            Object bsonValue = handler.write(this, values, metaField);
            if (cardinality == CardinalityEnum.Many) {
                @SuppressWarnings("unchecked")
                List<Object> bsonList = (List<Object>) (fieldBsonObject.get(dbValName));
                if (bsonList == null) {
                    bsonList = new ArrayList<Object>();
                    fieldBsonObject.put(dbValName, bsonList);
                }
                addWriteValue(bsonValue, bsonList);
                updated = true;
            } else {
                updated |= addWriteValue(fieldBsonObject, dbValName, bsonValue);
            }
        }
        
        if (updated && !metaField.isInternal()) {
            bsonObject.put(metaField.getDbName(), fieldBsonObject);
        }
    }

    private boolean addWriteValue(BasicDBObject fieldBsonObject, String dbValName, Object bsonValue) {
        boolean added = false;
        if (bsonValue != null) {
            fieldBsonObject.put(dbValName, bsonValue);
            added = true;
        }
        return added;
    }
    private boolean addWriteValue(Object bsonValue, List<Object> bsonList) {
        boolean added = false;
        if (bsonValue != null) {
            bsonList.add(bsonValue);
            added = true;
        }
        return added;
    }

    @Override
    public void removeField(String fieldName) {
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        CheckConditions.checkArgument(metaField != null, "Can't find meta field %s", fieldName);
        String dbName = metaField.getDbName();
        bsonObject.removeField(dbName); 
    }
    
	@Override
	public BasicDBObject getNode() {
		return bsonObject;
	}
	
	@Override
	protected Object getInternalFieldValue(InternalFieldEnum fieldEnum){
	    MetaField metaField = getMetaClass().getFieldByName(fieldEnum.getName());
        String dbValName = metaField.getDbName();
        Object bsonValue = bsonObject.get(dbValName);
        if (bsonValue == null) 
            return null;
        DataTypeEnum dataType = metaField.getDataType();
        IDataTypeHandler handler = BsonDataTypeHandlerFactory.getHandler(dataType);
        return handler.read(this, bsonValue, metaField);        
	}
	
	public void setFieldTimestamp(String fieldName, Date date) {
	    MetaField metaField = getMetaClass().getFieldByName(fieldName);
        BasicDBObject fieldBsonObject = getOrNewBsonField(metaField.getDbName());
        fieldBsonObject.put(FieldProperty.TIMESTAMP.getDbName(), date);
        bsonObject.put(metaField.getDbName(), fieldBsonObject);
    }

    public Date getFieldTimestamp(String fieldName) {
        return (Date) getFieldProperty(fieldName, FieldProperty.TIMESTAMP.getName());
    }

    private BasicDBObject getOrNewBsonField(String dbName) {
	    BasicDBObject bsonField = (BasicDBObject)bsonObject.get(dbName);
        if (bsonField == null) {
            bsonField = new BasicDBObject();
        }
        return bsonField;
	}
	
	private BasicDBObject getBsonField(String dbName) {
        return (BasicDBObject)bsonObject.get(dbName);
    }

    public void setFieldLength(String fieldName) {
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        BasicDBObject fieldBsonObject = getOrNewBsonField(metaField.getDbName());
        int len = getFieldValues(fieldName).size();
        fieldBsonObject.put(FieldProperty.LENGTH.getDbName(), len);
        bsonObject.put(metaField.getDbName(), fieldBsonObject);
    }

    public Integer getFieldLength(String fieldName) {
        return (Integer) getFieldProperty(fieldName, FieldProperty.LENGTH.getName());
    }

    public Object getFieldProperty(String fieldName, String propertyName) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkArgument(property != null,
                MessageFormat.format("field property %s not found!", propertyName));
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        BasicDBObject fieldBsonObject = getBsonField(metaField.getDbName());
        if (fieldBsonObject != null) {
            return fieldBsonObject.get(property.getDbName());
        } else {
            return null;
        }
    }

    public void setFieldProperty(String fieldName, String propertyName, Object value) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkArgument(property != null,
                MessageFormat.format("field property %s not found!", propertyName));
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        BasicDBObject fieldBsonObject = getOrNewBsonField(metaField.getDbName());
        // FIXME: check value based on the data type
        fieldBsonObject.put(property.getDbName(), value);
        bsonObject.put(metaField.getDbName(), fieldBsonObject);
    }

    @Override
    public boolean hasFieldProperty(String fieldName, String propertyName) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkArgument(property != null,
                MessageFormat.format("field property %s not found!", propertyName));
        MetaField metaField = getMetaClass().getFieldByName(fieldName);
        BasicDBObject fieldBsonObject = getBsonField(metaField.getDbName());
        if (fieldBsonObject != null) {
            return fieldBsonObject.containsField(property.getDbName());
        } else {
            return false;
        }
    }

}
