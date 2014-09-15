/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.internal.CMSEntityMapper;
import com.ebay.cloud.cms.typsafe.entity.internal.JsonCMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.restful.Constants;
import com.google.common.base.Preconditions;


/**
 * @author liasu
 * 
 */
public class GenericCMSEntity extends AbstractCMSEntity {

    private static final String LASTMOTIFIED = "._lastmodified";
    private static final ObjectMapper OBJECT_MAPPER = Constants.objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(GenericCMSEntity.class);
    //private static final String LENGTH       = "._length";

    private final JsonObject    delegate;

    public GenericCMSEntity() {
        delegate = new JsonObject();
    }
    
    public GenericCMSEntity(String oid, String type) {
        delegate = new JsonObject();
        set_id(oid);
        set_type(type);
    }

    public boolean hasField(String fieldName) {
        return delegate.hasField(fieldName);
    }

    public Object getFieldValue(String fieldName) {
        Object o = delegate.getFieldValue(fieldName);
        if (isFieldLastmodifiedProperty(fieldName) && o instanceof Long) {
            return new Date((Long) o);
        }
        return o;
    }

    private boolean isFieldLastmodifiedProperty(String fieldName) {
        return fieldName.endsWith(LASTMOTIFIED);
    }

    public Collection<String> getFieldNames() {
        return delegate.getFieldNames();
    }

    public void addFieldValue(String fieldName, Object fieldValue) {
        delegate.addFieldValue(fieldName, fieldValue);
    }

    public void setFieldValue(String fieldName, Object fieldValue) {
        delegate.setFieldValue(fieldName, fieldValue);
    }
    
    @Override
    public void removeFieldValue(String fieldName) {
        delegate.removeFieldValue(fieldName);
    }

    public void traverse(ICMSEntityVisitor visitor) {
        delegate.traverse(visitor);
    }

    @Override
    public Date getDateField(String field) {
        return delegate.getDateField(field);
    }

    @Override
    public void setDateField(String fieldName, Date d) {
        delegate.setFieldValue(fieldName, d);
    }

    @Override
    protected Long getLongField(String field) {
        return delegate.getLongField(field);
    }

    @Override
    protected void setLongField(String fieldName, Long d) {
        delegate.setFieldValue(fieldName, d);
    }

    /**
     * Makes sure the entity doesn't contain the field values for the given
     * field array.
     * <p/>
     * NOTE: _type and _oid are not affected.
     * 
     * @param fieldNames
     *            - the field names to be dropped from the entity. A null or empty
     *            means no change.
     */
    @Override
    public void excludeFields(String... fieldNames) {
        this.delegate.excludeFields(fieldNames);
    }

    /**
     * Makes sure the entity only contains the field values for the given field
     * array.
     * <p/>
     * NOTE: _type and _oid are not affected.
     * 
     * @param fieldNames
     *            - the field names to be preserved from the entity. A null or empty
     *            value means no change.
     */
    @Override
    public void includeFields(String... fieldNames) {
        this.delegate.includeFields(fieldNames);
    }
    
    /// dynamic-update setting
    /**
     * Clear the dirty bits.
     */
    public void clearDirtyBits() {
        delegate.clearDirtyBits();
    }

    /**
     * Enable dirty check, which is by default enabled
     */
    public void enableDirtyCheck() {
        delegate.enableDirtyCheck();
    }
    public void disableDirtyCheck() {
        delegate.disableDirtyCheck();
    }

    /// end of dynamic-update setting

    @Override
    public String get_metaclass() {
        return (String) get_type();
    }

    @Override
    public void set_metaclass(String metaClass) {
        set_type(metaClass);
    }
    
    @Override
    public String toString() {
        CMSEntityMapper mapper = new CMSEntityMapper(null, null, JsonCMSEntity.class, CMSEntityMapper.ProcessModeEnum.GENERIC, getClass(), false);
        this.traverse(mapper);
        return ((JsonCMSEntity) mapper.getTargetEntity()).getNode().toString();
    }

    public JsonNode toJson() {
        CMSEntityMapper mapper = new CMSEntityMapper(null, null, JsonCMSEntity.class, CMSEntityMapper.ProcessModeEnum.GENERIC, getClass(), false);
        this.traverse(mapper);
        return ((JsonCMSEntity) mapper.getTargetEntity()).getNode();
    }

    /**
     * Build GenericCMSEntity from the given json string.
     * 
     * @param jsonString
     * @return
     */
    public static List<GenericCMSEntity> buildEntity(String jsonString) {
        JsonNode rootNode = null;
        try {
            rootNode = OBJECT_MAPPER.readTree(jsonString);
        } catch (Exception e) {
            String msg = MessageFormat.format("Parse entity json failed, json string is {0}", jsonString);
            logger.error(msg);
            throw new CMSEntityException(msg);
        }
        return buildEntity(rootNode);
    }

    /**
     * Build GenericCMSEntity from the given json node.
     * 
     * @param jsonString
     * @return
     */
    public static List<GenericCMSEntity> buildEntity(JsonNode rootNode) {
        List<GenericCMSEntity> result = new ArrayList<GenericCMSEntity>();
        if (rootNode != null && rootNode.isArray()) {
            ArrayNode resultArray = (ArrayNode) rootNode;
            for (JsonNode node : resultArray) {
                GenericCMSEntity entity = parseResponseObject(node);
                if (entity != null) {
                    result.add(entity);
                }
            }
        } else {
            GenericCMSEntity entity = parseResponseObject(rootNode);
            if (entity != null) {
                result.add(entity);
            }
        }
        return result;
    }

    private static GenericCMSEntity parseResponseObject(JsonNode node) {
        if (!node.isObject()) {
            return null;
        }
        JsonCMSEntity cmsJsonCMSEntity = new JsonCMSEntity((ObjectNode) node, GenericCMSEntity.class);
        CMSEntityMapper mapper = new CMSEntityMapper((ObjectNode) node, null, GenericCMSEntity.class,
                CMSEntityMapper.ProcessModeEnum.GENERIC, GenericCMSEntity.class, false);
        cmsJsonCMSEntity.traverse(mapper);
        GenericCMSEntity o = (GenericCMSEntity) mapper.getTargetEntity();
        return o;
    }

    @Override
    public boolean isDirty(String fieldName) {
        return delegate.isDirty(fieldName);
    }

    @Override
    public boolean isDirtyCheckEnabled() {
        return delegate.isDirtyCheckEnabled();
    }

    @Override
    public Set<String> getDirtyFields() {
        return delegate.getDirtyFields();
    }

    @Override
    public void setDirtyFields(Set<String> fields) {
        Preconditions.checkNotNull(fields, "Can not given null object to set the dirty fields!");
        delegate.setDirtyFields(fields);
    }

}
