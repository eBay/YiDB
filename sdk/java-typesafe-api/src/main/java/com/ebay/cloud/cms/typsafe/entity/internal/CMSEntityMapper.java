/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity.internal;

import java.text.MessageFormat;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.AbstractCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntityVisitor;
import com.ebay.cloud.cms.typsafe.entity.IGenericEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.google.common.base.Preconditions;

/**
 * @author liasu
 * 
 */
@SuppressWarnings("rawtypes")
public class CMSEntityMapper implements ICMSEntityVisitor {

    public static enum ProcessModeEnum {
        JSON, GENERIC, TYPE_SAFE, DYNAMIC;
    }

    private static final Logger               logger = LoggerFactory.getLogger(CMSEntityMapper.class);

    private final Class<? extends ICMSEntity> targetClass;

    /**
     * The code generation class that to be used as meta class for field information
     */
    private final Class<? extends ICMSEntity> metaClass;

    private final CMSClientConfig             config;
    private final ProcessModeEnum             mode;
    private final boolean                     dirtyOnly;
    private final ObjectNode                  rootNode;
    private ICMSEntity                        targetEntity;

    public CMSEntityMapper(ObjectNode rootNode, CMSClientConfig config, Class targetClass, ProcessModeEnum mode, Class metaClass) {
        this(rootNode, config, targetClass, mode, metaClass, false);
    }

    @SuppressWarnings("unchecked")
    public CMSEntityMapper(ObjectNode rootNode, CMSClientConfig config, Class targetClass, ProcessModeEnum mode, Class metaClass,
            boolean dirtyOnly) {
        this.config = config;
        this.rootNode = rootNode;
        Preconditions.checkArgument(ICMSEntity.class.isAssignableFrom(targetClass));
        Preconditions.checkArgument(ICMSEntity.class.isAssignableFrom(metaClass));
        this.targetClass = targetClass;
        this.mode = mode;
        this.metaClass = metaClass;
        this.dirtyOnly = dirtyOnly;
        // init root entity
        createRootEntity(rootNode, mode);
    }

    private void createRootEntity(ObjectNode rootNode, CMSEntityMapper.ProcessModeEnum mode) {
        if (this.targetClass == JsonCMSEntity.class) {
            targetEntity = new JsonCMSEntity(this.metaClass);
        } else if (mode == CMSEntityMapper.ProcessModeEnum.DYNAMIC && rootNode != null) {
            targetEntity = inferTargetEntity();
        } else {
            targetEntity = createEntity(this.targetClass);
        }
        validateRootEntityType();
        initEntity(targetEntity);
        // set branch for root
        if (config != null) {
            targetEntity.set_branch(config.getBranch());
        }
    }

    private void initEntity(ICMSEntity entity) {
        if (config != null) {
            entity.set_repo(config.getRepository());
        }
    }

    @SuppressWarnings("deprecation")
    private void validateRootEntityType() {
        if (mode == ProcessModeEnum.TYPE_SAFE && rootNode != null && rootNode.has("_type")) {
            String responseType = rootNode.get("_type").getValueAsText();
            if (!targetClass.getSimpleName().equals(responseType)) {
                throw new CMSEntityException(MessageFormat.format(
                        "Got response type {0}, but given class type as {1}, this is possibly a client code issue, please use consistent type!", 
                        responseType, targetClass.getSimpleName()));
            }
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    private  ICMSEntity inferTargetEntity() {
        if (rootNode == null || !rootNode.has("_type")) {
            throw new CMSEntityException(
                    "Try to create entity based on query result, but can not find the _type hint. Please consider to add _type in your query projection!",
                    null);
        }
        String type = (String) rootNode.get("_type").getValueAsText();
        Class<?> t = inferFieldClass(type, null, null);
        Preconditions.checkState(t != GenericCMSEntity.class, MessageFormat.format(
                "Can not create instance for {0}, might be the code generation need to be updated?", type));
        ICMSEntity entity = createEntity((Class<? extends  ICMSEntity>) t);
        initEntity(entity);
        return entity;
    }

    private  ICMSEntity createEntity(Class<? extends ICMSEntity> target) {
        ICMSEntity entity = null;
        try {
            entity = target.newInstance();
        } catch (Exception e) {
            throw new CMSEntityException(MessageFormat.format("can not create instance for class {0}",
                    target.getSimpleName()), e);
        }
        return entity;
    }

    @Override
    public void processAttribute(ICMSEntity currentEntity, String fieldName) {
        if (dirtyOnly && currentEntity.isDirtyCheckEnabled() && !currentEntity.isDirty(fieldName)) {
            return;
        }

        Object fieldValue = currentEntity.getFieldValue(fieldName);
        targetEntity.setFieldValue(fieldName, fieldValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(ICMSEntity currentEntity, String fieldName) {
        if (dirtyOnly && currentEntity.isDirtyCheckEnabled() && !currentEntity.isDirty(fieldName)) {
            return;
        }

        Object fieldValue = currentEntity.getFieldValue(fieldName);
        ICMSEntity oldTarget = targetEntity;
        boolean isList = fieldValue instanceof List;
        if (targetClass == JsonCMSEntity.class) {
            // from java object to json
            if (isList) {
                List<AbstractCMSEntity> refEntities = (List<AbstractCMSEntity>) fieldValue;
                for (AbstractCMSEntity entity : refEntities) {
                    ICMSEntity newEntity = new JsonCMSEntity(ClassUtil.getFieldClass(entity.getClass(), fieldName));
                    processJsonList(fieldName, oldTarget, newEntity, entity);
                }
            } else {
                AbstractCMSEntity entity = (AbstractCMSEntity) fieldValue;
                ICMSEntity newEntity = new JsonCMSEntity(ClassUtil.getFieldClass(entity.getClass(), fieldName));
                processJsonSingle(fieldName, oldTarget, newEntity, entity);
            }
        } else {
            // convert from json to java object
            if (isList) {
                List<JsonCMSEntity> refJsonEntities = (List<JsonCMSEntity>) fieldValue;
                for (JsonCMSEntity jsonEntity : refJsonEntities) {
                    ICMSEntity newEntity = createReferenceEntity(targetEntity, fieldName, jsonEntity);
                    processJsonList(fieldName, oldTarget, newEntity, jsonEntity);
                }
            } else {
                JsonCMSEntity jsonEntity = (JsonCMSEntity) fieldValue;
                ICMSEntity newEntity = createReferenceEntity(targetEntity, fieldName, jsonEntity);
                processJsonSingle(fieldName, oldTarget, newEntity, jsonEntity);
            }
        }
    }

    private void processJsonSingle(String fieldName, ICMSEntity oldTarget, ICMSEntity newEntity,
            ICMSEntity subEntity) {
        targetEntity = newEntity;
        oldTarget.setFieldValue(fieldName, targetEntity);
        subEntity.traverse(this);
        targetEntity = oldTarget;
    }

    private void processJsonList(String fieldName, ICMSEntity oldTarget, ICMSEntity newEntity,
            ICMSEntity subEntity) {
        targetEntity = newEntity;
        oldTarget.addFieldValue(fieldName, targetEntity);
        subEntity.traverse(this);
        targetEntity = oldTarget;
    }

    private ICMSEntity createReferenceEntity(IGenericEntity parentEntity, String fieldName, JsonCMSEntity jsonNode) {
        if (mode == CMSEntityMapper.ProcessModeEnum.GENERIC) {
            ICMSEntity entity = createEntity(targetClass);
            initEntity(entity);
            return entity;
        }
 
        Class clz = inferFieldClass(jsonNode.get_type(), parentEntity.getClass(), fieldName);
        AbstractCMSEntity entity = null;
        try {
            entity = (AbstractCMSEntity) clz.newInstance();
        } catch (Exception e) {
            throw new CMSEntityException(
                    MessageFormat.format(
                            "convert from json to java object failed, not able to create entity with name:{0}. Please make sure you are operating entity on the correct repository;"
                                    + "otherwise, this possibly means that metadata is changed on cms service, so the model classes are out of date. Consider use generic api or re-generate model classes.",
                                    clz.getCanonicalName()), e);
        }
        entity.set_repo(config.getRepository());
//        entity.set_branch(config.getBranch());
        return entity;
    }

    /**
     * Prefer the json type return from cms service if any, otherwise based on the code generation method signature
     * @param jsonSpecifiedType
     * @param parentEntity
     * @param fieldName
     * @return - full 
     */
    private Class<?> inferFieldClass(String jsonSpecifiedType, Class<?> parentEntityClass, String fieldName) {
        Class<?> clz;
        StringBuilder builder = new StringBuilder(config.getClientPackagePrefix());
        if (jsonSpecifiedType != null) {
            builder.append(".").append(jsonSpecifiedType);
            try {
                clz = Class.forName(builder.toString());
            } catch (Exception e) {
                logger.error("can not load class with canonical path: " + builder.toString());
                clz = GenericCMSEntity.class;
            }
        } else {
            clz = ClassUtil.getGetterReturnType(parentEntityClass, fieldName);
            if (clz == null) {
                logger.error("Can not find field {0} in metaclass {1}, will use generic cms entity instead") ;
                clz = GenericCMSEntity.class;
            }
        }

        return clz;
    }

    public Object getTargetEntity() {
        return targetEntity;
    }

}
