/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.AbstractCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntityVisitor;

public class JsonCMSEntity extends AbstractCMSEntity {

    private static final Logger         logger = LoggerFactory.getLogger(JsonCMSEntity.class);
    private ObjectNode                  objectNode;
    private Class<? extends ICMSEntity> metaClass;

    JsonCMSEntity() {
        objectNode = JsonNodeFactory.instance.objectNode();
    }
    
    public JsonCMSEntity(Class<? extends ICMSEntity> metaClass) {
        this();
        this.metaClass = metaClass;
    }

    public JsonCMSEntity(ObjectNode given, Class<? extends ICMSEntity> meta) {
        objectNode = given;
        this.metaClass = meta;
    }

    public boolean hasField(String fieldName) {
        return objectNode.has(fieldName);
    }

    public Object getFieldValue(String fieldName) {
        if (isJsonType(fieldName)) {
            return objectNode.get(fieldName);
        }
        return convertFromJson(fieldName, objectNode.get(fieldName));
    }

    private boolean isJsonType(String fieldName) {
        Class<?> clz = ClassUtil.getGetterReturnType(metaClass, fieldName);
        return clz == JsonNode.class;
    }

    private Object convertFromJson(String fieldName, JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isArray()) {
            ArrayNode an = (ArrayNode) jsonNode;
            List<Object> result = new ArrayList<Object>();
            Iterator<JsonNode> nodeItr = an.getElements();
            while (nodeItr.hasNext()) {
                Object val = convertFromJsonSingle(fieldName, nodeItr.next());
                if (val instanceof JsonNode) {
                    // WORKAROUND: If return a JsonNode, means this should not be casted as List. For example, it could be a newly added
                    // meta field in server side. In this case, we only return the json node
                    return jsonNode;
                }
                result.add(val);
            }

            return result;
        } else {
            return convertFromJsonSingle(fieldName, jsonNode);
        }
    }

    /**
     * FIXME: For PaaS integration, we choose jackson 1.7 compatible deprecation API only 
     * @param fieldName 
     */
    @SuppressWarnings("deprecation")
    private Object convertFromJsonSingle(String fieldName, JsonNode next) {
        if (next.isDouble()) {
            return next.getValueAsDouble();
        } else if (next.isInt()) {
            return next.getValueAsInt();
        } else if (next.isTextual()) {
            return next.getValueAsText();
        } else if (next.isBoolean()) {
            return next.getValueAsBoolean();
        } else if (next.isLong()) {
            return next.getValueAsLong();
        } else if (next.isNull()) {
            return null;
        } else if (next.isPojo()) {
            return ((POJONode)next).getPojo();
        } else if (next.isObject()) {
            ObjectNode objectNode = (ObjectNode) next;
            if (isReferenceNode(objectNode)) {
                return new JsonCMSEntity((ObjectNode) next, ClassUtil.getFieldClass(metaClass, fieldName));
            } else {
                return next;
            }
        }
        logger.error(MessageFormat.format(
                "convert from json encounting un-recoginizable node. The node value representation as : {0}",
                next.getValueAsText()));
        return null;
    }

    public Collection<String> getFieldNames() {
        Iterator<String> itr = objectNode.getFieldNames();
        List<String> fieldNames = new ArrayList<String>();
        while (itr.hasNext()) {
            fieldNames.add(itr.next());
        }
        return fieldNames;
    }

    public void addFieldValue(String fieldName, Object fieldValue) {
        JsonNode fieldNode = convertToJsonNode(fieldValue);

        JsonNode node = objectNode.get(fieldName);
        if (node != null && !node.isArray()) {
            throw new RuntimeException(
                    MessageFormat
                            .format("call addFieldValue encounting exisitng non-list field values, incorrectly set the field value as non-list value?. Field name {0}",
                                    fieldName));
        }
        if (node == null) {
            node = JsonNodeFactory.instance.arrayNode();
            objectNode.put(fieldName, node);
        }
        ArrayNode array = (ArrayNode) node;
        array.add(fieldNode);
    }

    public void setFieldValue(String fieldName, Object fieldValue) {
        JsonNode fieldNode = convertToJsonNode(fieldValue);
        objectNode.put(fieldName, fieldNode);
    }

    @SuppressWarnings({ "rawtypes" })
    private JsonNode convertToJsonNode(Object fieldValue) {
        if (fieldValue == null) {
            return NullNode.getInstance();
        } else if (fieldValue instanceof JsonNode) {
            return (JsonNode) fieldValue;
        }

        Class valueClass = fieldValue.getClass();
        if (List.class.isAssignableFrom(valueClass)) {
            List lists = (List) fieldValue;
            ArrayNode an = JsonNodeFactory.instance.arrayNode();

            for (Object obj : lists) {
                JsonNode valNode = converToJsonNodeSingle(obj);
                an.add(valNode);
            }
            return an;
        } else {
            return converToJsonNodeSingle(fieldValue);
        }
    }

    @SuppressWarnings("rawtypes")
    private JsonNode converToJsonNodeSingle(Object fieldValue) {
        if (fieldValue == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        Class valueClass = fieldValue.getClass();
        if (valueClass == Boolean.class) {
            return JsonNodeFactory.instance.booleanNode((Boolean) fieldValue);
        } else if (valueClass == Integer.class) {
            return JsonNodeFactory.instance.numberNode(((Integer) fieldValue).intValue());
        } else if (valueClass == Long.class) {
            return JsonNodeFactory.instance.numberNode(((Long) fieldValue).longValue());
        } else if (valueClass == Double.class){
            return JsonNodeFactory.instance.numberNode(((Double) fieldValue).doubleValue());
        } else if (valueClass.isEnum()) {
            return JsonNodeFactory.instance.textNode(fieldValue.toString());
        } else if (valueClass == String.class) {
            return JsonNodeFactory.instance.textNode(fieldValue.toString());
        } else if (valueClass == Date.class) {
            return JsonNodeFactory.instance.numberNode(((Date) fieldValue).getTime());
        } else if (valueClass == JsonCMSEntity.class) {
            return ((JsonCMSEntity) fieldValue).getNode();
        } else if (valueClass == Map.class) {
            return JsonNodeFactory.instance.POJONode(fieldValue);
        }
        logger.error(MessageFormat.format(
                "convert object to json node, fieldValue class {0}, with toString() value {1}", fieldValue.getClass()
                        .getName(), fieldValue.toString()));
        return null;
    }

    public void traverse(ICMSEntityVisitor visitor) {
        Iterator<Map.Entry<String, JsonNode>> subNodeItr = objectNode.getFields();

        List<Map.Entry<String, JsonNode>> attributeNode = new ArrayList<Map.Entry<String, JsonNode>>();
        List<Map.Entry<String, JsonNode>> referenceNode = new ArrayList<Map.Entry<String, JsonNode>>();
        while (subNodeItr.hasNext()) {
            Entry<String, JsonNode> entry = subNodeItr.next();

            JsonNode subNode = entry.getValue();
            if (isReferenceNode(subNode)) {
                referenceNode.add(entry);
            } else {
                attributeNode.add(entry);
            }
        }

        // traverse attribute
        for (Map.Entry<String, JsonNode> entry : attributeNode) {
            visitor.processAttribute(this, entry.getKey());
        }

        // traverse reference
        for (Map.Entry<String, JsonNode> entry : referenceNode) {
            visitor.processReference(this, entry.getKey());
        }
    }

    /**
     * Detect a reference/attribute node by inspecting the _type field existence
     * 
     * FIXME: based on code generation class?
     */
    private boolean isReferenceNode(JsonNode subNode) {
        JsonNode node = subNode;
        if (subNode.isArray()) {
            ArrayNode array = (ArrayNode) subNode;
            if (array.size() > 0) {
                node = array.get(0);
            } else {
                node = null;
            }
        }

        if (node == null) {
            // for the empty array case, reference and attribute should have
            // same behavior, just return false
            return false;
        }

        if (null == node.get("_oid")) {
            return false;
        }
        return true;
    }

    public ObjectNode getNode() {
        return objectNode;
    }

    @Override
    public Date getDateField(String field) {
        Long l = (Long) getFieldValue(field);
        if (l != null) {
            return new Date(l);
        } else {
            return null;
        }
    }

    @Override
    public void setDateField(String fieldName, Date d) {
        setFieldValue(fieldName, d.getTime());
    }

    @Override
    public void removeFieldValue(String fieldName) {
        objectNode.remove(fieldName);
    }

    @Override
    protected Long getLongField(String field) {
        Number num = (Number) getFieldValue(field);
        if (num instanceof Long) {
            return (Long) num;
        } else if (num != null) {
            return num.longValue();
        }
        return null;
    }

    @Override
    protected void setLongField(String fieldName, Long d) {
        setFieldValue(fieldName, d);
    }

    @Override
    public void includeFields(String... fieldNames) {
        throw new UnsupportedOperationException("include fields not supported by JsonCMSEntity!");
    }

    @Override
    public void excludeFields(String... fieldNames) {
        throw new UnsupportedOperationException("exclude fields not supported by JsonCMSEntity!");
    }

    @Override
    public void clearDirtyBits() {
        // do nothing, json doesn't need dirty
    }

    @Override
    public void enableDirtyCheck() {
        // do nothing, json doesn't need dirty
    }

    @Override
    public void disableDirtyCheck() {
        // do nothing, json doesn't need dirty
    }

    @Override
    public boolean isDirty(String fieldName) {
        // field on json cms entity always be taken care
        return true;
    }

    @Override
    public boolean isDirtyCheckEnabled() {
        // json doesn't need dirty
        return false;
    }

    @Override
    public Set<String> getDirtyFields() {
        // json doesn't need dirty
        return null;
    }

    @Override
    public void setDirtyFields(Set<String> fields) {
        // do nothing, json doesn't need dirty   
    }

}
