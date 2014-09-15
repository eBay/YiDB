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

package com.ebay.cloud.cms.dal.entity;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonDataTypeHandlerFactory;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * 
 * @author jianxu1, liasu
 * @date 2012/5/31
 * @history
 */
public class JsonEntity extends AbstractEntity {

	private static final String PROPERTY_CONNECTOR = ".";
    private static ObjectMapper mapper = ObjectConverter.mapper;
	private ObjectNode jsonObject = null;
		
	public JsonEntity(MetaClass metaClass) {
	    super(metaClass);
	    this.jsonObject = JsonNodeFactory.instance.objectNode();
        this.jsonObject.put(InternalFieldEnum.TYPE.getName(), metaClass.getName());
	}
	
	public JsonEntity(MetaClass metaClass, String given){
		super(metaClass);
		this.jsonObject = parseJson(given);
	}
	
	public JsonEntity(MetaClass metaClass, ObjectNode given){
        super(metaClass);
        this.jsonObject = given;
    }
	
	public JsonEntity(JsonEntity other){
        this(other.getMetaClass(), other.jsonObject.toString());
    }
	
	@Override
	public ObjectNode getNode(){
		return jsonObject;
	}
	
	private ObjectNode parseJson(String jsonString) {
	    try {
            return (ObjectNode) mapper.readTree(jsonString);
        }catch (IOException e) {
            throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON,"json parse error: " + e.getMessage(), e);
        }
    }

	@Override
	public void setFieldValues(String fieldName, List<?> value) {
	    MetaField metaField = getMetaClass().getFieldByName(fieldName);
	    CheckConditions.checkNotNull(metaField, "MetaClass %s does not have MetaField %s", getMetaClass().getName(),fieldName);
        DataTypeEnum dataType = metaField.getDataType();
        CardinalityEnum cardinality = metaField.getCardinality();

        IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(dataType);
        if (cardinality.equals(CardinalityEnum.One)) {
            Object obj = (value == null || value.isEmpty()) ? null : value.get(0);
            if (obj != null) {
            	JsonNode node = (JsonNode) handler.write(this, obj, metaField);
            	getNode().put(fieldName, node);
            }
        } else {
            ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
            if (value != null) {
                for (Object obj : value) {
                    JsonNode node = (JsonNode) handler.write(this, obj, metaField);
                    arrayNode.add(node);
                }
            }
            getNode().put(fieldName, arrayNode);
        }
	}

    /**
     * @param fieldName
     *            - the field name
     * @param value
     *            - the value to set to the given field
     */
    @Override
    public void addFieldValue(String fieldName, Object value) {
    	
		MetaField metaField = getMetaClass().getFieldByName(fieldName);
		CheckConditions.checkNotNull(metaField, "MetaClass %s does not have MetaField %s", getMetaClass().getName(),fieldName);
		
		DataTypeEnum dataType = metaField.getDataType();
		CardinalityEnum cardinality = metaField.getCardinality();

		IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(dataType);
        JsonNode valueNode = (JsonNode) handler.write(this, value, metaField);

        MetaClass refMetaClass = null;
        // check meta class for type matching
        if (value != null && metaField.getDataType().equals(DataTypeEnum.RELATIONSHIP)) {
            refMetaClass = ((MetaRelationship) metaField).getRefMetaClass();
            MetaClass givenMeta = ((JsonEntity) value).getMetaClass();
            CheckConditions.checkArgument(refMetaClass.isAssignableFrom(givenMeta),
                    "Meta relationship ref meta %s couldn't be add a instance of %s", refMetaClass.getName(),
                    givenMeta.getName());
        }

        if (value != null) {
			if(cardinality == CardinalityEnum.Many){
				ArrayNode arrayNode = (ArrayNode)getNode().get(fieldName);
				if(arrayNode == null){
					arrayNode = JsonNodeFactory.instance.arrayNode();
					((ObjectNode)getNode()).put(fieldName, arrayNode);
				}
				arrayNode.add(valueNode);
			}else{
				getNode().put(fieldName, valueNode);
			}
        }
    
	}

    @Override
    public boolean hasField(String fieldName) {
        return getNode().has(fieldName);
    }
    
    @Override
    public void removeField(String fieldName) {
        getNode().remove(fieldName);
        for (FieldProperty fp : FieldProperty.values()) {
            removeFieldProperty(fieldName, fp.getName());
        }
    }

    private void removeFieldProperty(String fieldName, String propertyName) {
        FieldProperty fp = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkArgument(fp != null, MessageFormat.format("field property %s not found!", propertyName));
        String propertyValueDbName = fieldName + PROPERTY_CONNECTOR + propertyName;
        getNode().remove(propertyValueDbName);
    }

    /**
     * 
     */
	@Override
	public List<?> getFieldValues(String fieldName){
		MetaField metaField = getMetaClass().getFieldByName(fieldName);
		CheckConditions.checkNotNull(metaField, "MetaClass %s does not have MetaField %s", getMetaClass().getName(),fieldName);
		DataTypeEnum dataType = metaField.getDataType();
		CardinalityEnum cardinality = metaField.getCardinality();

        JsonNode valueNode = jsonObject.get(fieldName);
        if (valueNode == null) {
            return Collections.emptyList();
        }

		IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(dataType);
		List<Object> result = null;
		if(cardinality == CardinalityEnum.One) {
		    if (valueNode.isArray()) {
		        CheckConditions.checkArgument(!valueNode.isArray(),
	                    "The given value of MetaField %s is an array", fieldName);		        
		    }

			result = new ArrayList<Object>(1);
			Object value = handler.read(this, valueNode, metaField);
			addReadValue(result, value);
        } else {
			result = new ArrayList<Object>();
			if (!valueNode.isNull()) {
                if (!valueNode.isArray()) {
                    CheckConditions.checkArgument(valueNode.isArray(), "The given value of MetaField %s is not an array",
                            fieldName);
                }
			    ArrayNode arrayNode = (ArrayNode)valueNode;
		        Iterator<JsonNode> nodeIter = arrayNode.getElements();
			    while(nodeIter.hasNext()) {
			        JsonNode valNode = nodeIter.next();	
				    Object value = handler.read(this, valNode, metaField);
				    addReadValue(result, value);
			    }
			}
		}
		return result;
	}

    // ignore null
    private void addReadValue(List<Object> result, Object value) {
        if (value != null) {
            result.add(value);
        }
    }
    
	@Override
	public List<String> getFieldNames() {
		Iterator<String> iter = getNode().getFieldNames();
		List<String> names = new ArrayList<String>();
		while(iter.hasNext()){
			String fieldName = iter.next();
			names.add(fieldName);
		}
		return names;
	}	
	
	@Override
    protected Object getInternalFieldValue(InternalFieldEnum fieldEnum){
	    String fieldName = fieldEnum.getName();
	    MetaField metaField = getMetaClass().getFieldByName(fieldName);        
        DataTypeEnum dataType = metaField.getDataType();
        IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(dataType);
        JsonNode jsonValue = jsonObject.get(fieldName);
        if (jsonValue == null) {
            return null;
        }
        return handler.read(this, jsonValue, metaField);
	}

    @Override
    public Object getFieldProperty(String fieldName, String propertyName) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkNotNull(property, "Can not find field property %s!", propertyName);
        String propertyFullName = fieldName + PROPERTY_CONNECTOR + property.getName();
        JsonNode valueNode = getNode().get(propertyFullName);

        if (valueNode != null) {
            MetaAttribute attribute = getFieldPropertyField(property, propertyFullName);
            IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(property.getType());
            return handler.read(this, valueNode, attribute);
        }
        return null;
    }

    private MetaAttribute getFieldPropertyField(FieldProperty property, String propertyFullName) {
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName(propertyFullName);
        attribute.setDataType(property.getType());
        return attribute;
    }

    public void setFieldProperty(String fieldName, String propertyName, Object value) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        String propertyFullName = fieldName + PROPERTY_CONNECTOR + propertyName;
        if (property != null) {
            MetaAttribute attribute = getFieldPropertyField(property, propertyFullName);
            IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(property.getType());
            JsonNode valueNode = (JsonNode) handler.write(this, value, attribute);
            getNode().put(propertyFullName, valueNode);
        }
    }

    @Override
    public boolean hasFieldProperty(String fieldName, String propertyName) {
        FieldProperty property = FieldProperty.fromQueryName(propertyName);
        CheckConditions.checkNotNull(property, "Can not find field property %s!", propertyName);
        String propertyFullName = fieldName + PROPERTY_CONNECTOR + propertyName;
        return getNode().has(propertyFullName);
    }

}
