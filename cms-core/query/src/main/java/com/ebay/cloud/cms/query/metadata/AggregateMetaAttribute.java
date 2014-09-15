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


/**
 * 
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

package com.ebay.cloud.cms.query.metadata;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonDataTypeHandlerFactory;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * An aggregation field represents a meta field that used in aggregation query
 * 
 * @author liasu
 * 
 */
public class AggregateMetaAttribute extends MetaAttribute {

    @JsonIgnore
    private GroupField       groupField;
    @JsonIgnore
    private AggregationField aggregationField;

    public AggregateMetaAttribute(ISearchField searchField) {
        super(false);
        if (searchField instanceof GroupField) {
            initGroupMetaField((GroupField) searchField);
        } else if (searchField instanceof AggregationField) {
            initAggregationMetaField((AggregationField) searchField);
        } else {
            throw new IllegalArgumentException("aggregation meta field must built on group field or aggregation field");
        }
    }

    public AggregateMetaAttribute(GroupField grpField) {
        super(false);//
        initGroupMetaField(grpField);
    }

    @JsonIgnore
    private void initGroupMetaField(GroupField grpField) {
        this.groupField = grpField;
        this.aggregationField = null;
        MetaField metaField = grpField.getSearchFiled().getRootField();
        initFromMetaField((MetaAttribute)metaField);
    }

    @JsonIgnore
    private void initFromMetaField(MetaAttribute metaField) {
        setName(getFieldName());
        if (metaField != null) {
            setDataType(metaField.getDataType());
            setCardinality(metaField.getCardinality());
            setDescription(metaField.getDescription());
            setInternal(metaField.isInternal());
            setDbName(metaField.getDbName());

            setDefaultValue(metaField.getDefaultValue());
            setEnumValues(metaField.getEnumValues());
            setExpression(metaField.getExpression());
            setMandatory(metaField.isMandatory());
            setValidation(metaField.getValidation());
        } else {
            setDataType(DataTypeEnum.INTEGER);
            setDbName("count");
            setCardinality(CardinalityEnum.One);
        }
    }

    public AggregateMetaAttribute(AggregationField aggregationField) {
        super(false);//
        initAggregationMetaField(aggregationField);
    }

    @JsonIgnore
    private void initAggregationMetaField(AggregationField aggregationField) {
        this.groupField = null;
        this.aggregationField = aggregationField;
        if (aggregationField.getSearchField() != null) {
            MetaField metaField = aggregationField.getSearchField().getRootField();
            initFromMetaField((MetaAttribute) metaField);
        } else {
            setName(aggregationField.getFieldName());
            initFromMetaField(null);
        }
    }

    @JsonIgnore
    private Object getFieldValue(IEntity currentEntity) {
        BasicDBObject dbo = (BasicDBObject) currentEntity.getNode();
        Object value = null;
        if (groupField != null) {
            value = ((DBObject) dbo.get("_id")).get(groupField.getFieldName());
        } else {
            value = dbo.get(aggregationField.getFieldName());
        }
        return value;
    }

    @JsonIgnore
    private boolean hasFieldValue(IEntity currentEntity) {
        BasicDBObject dbo = (BasicDBObject) currentEntity.getNode();
        if (groupField != null) {
            return ((DBObject) dbo.get("_id")).containsField(groupField.getFieldName());
        } else {
            return dbo.containsField(aggregationField.getFieldName());
        }
    }
    
    @JsonIgnore
    public void setAggregateFieldValue(IEntity fromEntity, JsonEntity jsonEntity) {
        String targetFieldName = getName();
        // set values if result has value for current field
        if (!hasFieldValue(fromEntity)) {
            return;
        }

        Object value = getFieldValue(fromEntity);
        IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(getDataType());
        if (getCardinality() == CardinalityEnum.Many) {
            // compatible for single value in list field like 
            List<?> values = null;
            if (value instanceof List) { 
                values = (List<?>) value;
            } else {
                values = Arrays.asList(value);
            }
            // append to result node
            ArrayNode resultNode = JsonNodeFactory.instance.arrayNode();
            for (Object vObject : values) {
                JsonNode valueNode = (JsonNode) handler.write(jsonEntity, vObject, this);
                resultNode.add(valueNode);
            }
            jsonEntity.getNode().put(targetFieldName, resultNode);
        } else {
            JsonNode valueNode = (JsonNode) handler.write(jsonEntity, value, this);
            jsonEntity.getNode().put(targetFieldName, valueNode);
        }
    }

    @Override
    public int hashCode() {
        if (groupField != null) {
            return groupField.hashCode();
        } else {
            return aggregationField.hashCode();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof AggregateMetaAttribute)) {
            return false;
        }
        if (groupField != null) {
            return groupField.equals(((AggregateMetaAttribute) other).groupField);
        } else {
            return aggregationField.equals(((AggregateMetaAttribute) other).aggregationField);
        }
    }
    @JsonIgnore
    private String getFieldName() {
        if (groupField != null) {
            return groupField.getFieldName();
        } else {
            StringBuilder sb = new StringBuilder(aggregationField.getFieldName());
            sb.setCharAt(0, '$');
            return sb.toString();
        }
    }

}
