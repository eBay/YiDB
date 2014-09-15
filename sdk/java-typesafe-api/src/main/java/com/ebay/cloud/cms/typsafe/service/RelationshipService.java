package com.ebay.cloud.cms.typsafe.service;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.RelationshipField;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.RelationTypeEnum;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

class RelationshipService {
    private static final Logger logger = LoggerFactory.getLogger(RelationshipService.class);

    private final CMSClientService service;

    private LoadingCache<String, MetaClass> metadatas;

    private static String RELATIONSHIP_QUERY_TEMPLATE = "%s[@_oid=\"%s\"].%s[@_oid=\"%s\"]";

    private static final int MAX_RETRY_COUNT = 300;

    RelationshipService(CMSClientService service) {
        this.service = service;
        this.metadatas = CacheBuilder.newBuilder().maximumSize(500).expireAfterAccess(30, TimeUnit.MINUTES)
                .build(new CacheLoader<String, MetaClass>() {

                    @Override
                    public MetaClass load(String key) throws Exception {
                        return RelationshipService.this.service.getMetadata(key, null);
                    }
                });
    }

    /**
     * Creates relationship between the fromEntity and the toEntity. If the
     * fromEntity(toEntity) is not existing, a new entity will be created.
     * 
     * @param fromEntity
     * @param refField
     * @param toEntity
     * @param context
     */
    public <T extends ICMSEntity, K extends ICMSEntity> void createRelationship(final T fromEntity, final String refField,
            final K toEntity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        String fromType = fromEntity.get_type();
        MetaClass fromMetaClass = getMetadata(fromType);
        ensureFromEntity(fromEntity, refField, context);

        MetaField field = fromMetaClass.getField(refField);
        if (!(field instanceof MetaRelationship)) {
            throw new CMSEntityException(MessageFormat.format("Field {0} on class {1} is not relationship!", refField, fromType));
        }
        MetaRelationship relation = (MetaRelationship) field;
        ensureToEntity(fromEntity, refField, toEntity, context, fromType);

        if (relation.getRelationType().equals(RelationTypeEnum.Reference)) {
            // array handling
            final T opEntity = createNewEntity(fromEntity);
            if (field.getCardinality() == CardinalityEnum.Many) {
                opEntity.removeFieldValue(refField);
                opEntity.addFieldValue(refField, toEntity);
            } else {
                opEntity.setFieldValue(refField, toEntity);
            }
            final CMSClientContext fContext = context;
            // CMS-3930:: add retry for update operation in relationship API
            retryRelationshipOperation(service, logger, context, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    String query = String.format(RELATIONSHIP_QUERY_TEMPLATE, opEntity.get_type(), opEntity.get_id(), refField, toEntity.get_id());
                    CMSQuery cmsQuery = new CMSQuery(query);
                    CMSQueryResult<GenericCMSEntity> ge = service.query(cmsQuery, fContext);
                    if (ge.getEntities().size() > 0) {
                        // already existing
                        return 0;
                    }
                    service.updateEntityField(opEntity, refField, fContext);
                    return 0;
                }
            }, opEntity.get_type(), opEntity.get_id(), refField, toEntity.get_type(), toEntity.get_id(), "create");
        }
    }

    private <T extends ICMSEntity, K extends ICMSEntity> void ensureToEntity(T fromEntity, String refField, K toEntity,
            CMSClientContext context, String fromType) {
        if (!StringUtils.isEmpty(toEntity.get_id())) {
            K getTo = getEntity(toEntity, context);
            if (getTo == null) {
                // create to entity
                context.setPath(fromType, fromEntity.get_id(), refField);
                service.create(toEntity, context);
            }
        } else {
            context.setPath(fromType, fromEntity.get_id(), refField);
            service.create(toEntity, context);
        }
    }

    private <T extends ICMSEntity> void ensureFromEntity(T fromEntity, String refField, CMSClientContext context) {
        if (!StringUtils.isEmpty(fromEntity.get_id())) {
            T getFrom = getEntity(fromEntity, context);
            if (getFrom == null) {
                createOrUpdateFromEntity(fromEntity, refField, context, true);
            } else {
                createOrUpdateFromEntity(fromEntity, refField, context, false);
            }
        } else {
            createOrUpdateFromEntity(fromEntity, refField, context, true);
        }
    }

    private <T extends ICMSEntity> T getEntity(T givenEntity, CMSClientContext context) {
        if (givenEntity.getClass() == GenericCMSEntity.class) {
            return (T) service.get(givenEntity.get_id(), givenEntity.get_type(), context);
        } else {
            return service.get(givenEntity.get_id(), (Class<T>) givenEntity.getClass(), context);
        }
    }

    private <T extends ICMSEntity> void createOrUpdateFromEntity(final T fromEntity, final String refField, final CMSClientContext context, boolean create) {
        boolean hasOldValue = fromEntity.hasField(refField);
        Object oldFieldValue = fromEntity.getFieldValue(refField);
        fromEntity.removeFieldValue(refField);
        if (create) {
            service.create(fromEntity, context);
        } else {
            // CMS-3930:: add retry for update operation in relationship API
            GenericEntityService.retryOperation(service, logger, context, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    service.update(fromEntity, context);
                    return 0;
                }
            }, GenericEntityService.RETRY_LOG_MESSAGE, -1);
        }

        if (hasOldValue) {
            fromEntity.setFieldValue(refField, oldFieldValue);
        }
    }

    @Deprecated
    public <T extends ICMSEntity, K extends ICMSEntity> void createRelationship(T fromEntity, RelationshipField<T, K> field, K toEntity, CMSClientContext context) {
        Preconditions.checkNotNull(field, "Relationship field couldn't be null!");
        createRelationship(fromEntity, field.getFieldName(), toEntity, context);
    }

    /**
     * Deletes relationship between the fromEntity and the toEntity if any.
     * 
     * 
     * @param fromEntity
     * @param refField
     * @param toEntity
     * @param context
     */
    public <T extends ICMSEntity, K extends ICMSEntity> void deleteRelationship(final T fromEntity, final String refField,
            final K toEntity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        Preconditions.checkArgument(!StringUtils.isEmpty(refField), String.format("refField could not be null!"));
        T getFrom = getEntity(fromEntity, context);
        if (getFrom == null) {
            // no from entity found
            return;
        }
        K getTo = getEntity(toEntity, context);
        if (getTo == null) {
            // no to entity found
            return;
        }

        final String fromType = fromEntity.get_type();
        MetaClass meta = getMetadata(fromType);
        final MetaField field = meta.getField(refField);
        if (! (field instanceof MetaRelationship)) {
            throw new CMSEntityException(MessageFormat.format("Field {0} on class {1} is not relationship!", refField, fromType));
        }
        MetaRelationship relationship = (MetaRelationship)field;
        if (relationship.getRelationType().equals(RelationTypeEnum.Inner) || relationship.getRelationType().equals(RelationTypeEnum.Embedded)) {
            // delete inner/ebmed entity will automatically remove the reference
            service.delete(toEntity, context);
        } else {
            // array handling
            final T opEntity = createNewEntity(fromEntity);
            if (field.getCardinality() == CardinalityEnum.Many) {
                // create new Entity for operation. Can not rely on the given
                // entity, since user might pass untentional objects
                opEntity.setFieldValue(refField, Arrays.asList(toEntity));
            } else {
                opEntity.setFieldValue(refField, toEntity);
            }
            final CMSClientContext fContext = context;
            // CMS-3930:: add retry for update operation in relationship API
            retryRelationshipOperation(service, logger, context, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    String query = String.format(RELATIONSHIP_QUERY_TEMPLATE, opEntity.get_type(), opEntity.get_id(), refField, toEntity.get_id());
                    CMSQuery cmsQuery = new CMSQuery(query);
                    CMSQueryResult<GenericCMSEntity> ge = service.query(cmsQuery, fContext);
                    if (ge.getEntities().size() == 0) {
                        // already removed
                        return 0;
                    }
                    
                    service.deleteField(opEntity, refField, fContext);
                    return 0;
                }
            }, opEntity.get_type(), opEntity.get_id(), refField, toEntity.get_type(), toEntity.get_id(), "delete");
        }
    }

    // copy the entity with id and type set from given entity
    private <T extends ICMSEntity> T createNewEntity(T entity) {
        T newEntity;
        try {
            newEntity = (T) entity.getClass().newInstance();
            newEntity.set_id(entity.get_id());
            newEntity.set_type(entity.get_type());
            return newEntity;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CMSEntityException(MessageFormat.format("Can not create new entity for operation! Type: {0}",
                    entity.getClass().getSimpleName()));
        }
    }

    /**
     * A almost loop-ever retry. Use mass of parameter
     * for logging...
     * 
     */
    private <T extends ICMSEntity, K> K retryRelationshipOperation(CMSClientService service, Logger logger, CMSClientContext context, Callable<K> op,
            String fromType, String fromId, String refField, String toType, String toId, String opName) {
        String message = String.format(" failed operation of " + opName + " relationship " +
                "from entity of type: %s, _oid: %s, through field: %s, to entity of type: %s, _oid: %s ! " +
                "This is the relationship loop, will retry! Current retry count is {5}! ",
                fromType, fromId, refField, toType, toId);
        return GenericEntityService.retryOperation(service, logger, context, op, message, MAX_RETRY_COUNT);
    }

    @Deprecated
    public <T extends ICMSEntity, K extends ICMSEntity> void deleteRelationship(T fromEntity, RelationshipField<T, K>field, K toEntity, CMSClientContext context) {
        Preconditions.checkNotNull(field, "Relationship field couldn't be null!");
        deleteRelationship(fromEntity, field.getFieldName(), toEntity, context);
    }

    public MetaClass getMetadata(String fromType) {
        MetaClass meta = null;
        try {
            meta = this.metadatas.get(fromType);
        } catch (ExecutionException e) {
            String msg = MessageFormat.format("Unable to find metaclass {0}!", fromType);
            logger.error(msg);
            throw new CMSClientException(msg, e);
        }
        return meta;
    }
}
