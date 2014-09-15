/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery.QueryParameter;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.exception.CMSErrorCodeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.typsafe.restful.EntityResponseProcessor;
import com.ebay.cloud.cms.typsafe.restful.FieldJsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.JsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder.Url;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext.LOG_LEVEL;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.ClientResponse;

/**
 * All operation follows flow:
 * <ul>
 * <li>Check valid</li>
 * <li>Build url</li>
 * <li>Execute</li>
 * <li>Parse response</li>
 * </ul>
 * @author liasu
 *
 */
public class GenericEntityService {
    
    public static final String RETRY_LOG_MESSAGE = "See version conflict when retry operation, retry count: %d !";
    
    private static final Logger logger = LoggerFactory.getLogger(GenericEntityService.class);

    private final CMSClientService service;
    private final MetadataService metaService;
    
    GenericEntityService(CMSClientService service, MetadataService metaService) {
        this.service = service;
        this.metaService = metaService;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////Entity operations///////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    public GenericCMSEntity getGeneric(String id, String metadata, CMSClientContext context, String... includeFieldNames) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id can not be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(metadata), "metadata can not be null");
        GenericCMSEntity queryObject = createQueryObject(id, GenericCMSEntity.class);
        queryObject.set_metaclass(metadata);
        queryObject.set_repo(service.getRepository());

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_DETAIL_URL, queryObject).buildCanonicalPath();
        RestExecutor builder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.GET, queryObject, url, service.getQueryParameter(context),
                service.getHeader(context), context);

        
        ClientResponse resp = builder.build();
        EntityResponseProcessor<GenericCMSEntity> genericResponder = new EntityResponseProcessor<GenericCMSEntity>(
                GenericCMSEntity.class, resp, service.getClientConfig(), HttpRequest.GET, context);
        
        
        List<GenericCMSEntity> entities = genericResponder.getBuildEntity();
        GenericCMSEntity entity = (entities != null && entities.size() > 0) ? entities.get(0) : null;
        if (entity != null) {
            entity.includeFields(includeFieldNames);
        }
        return entity;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ICMSEntity> T getTypeSafe(String id, Class<T> entityClass, CMSClientContext context,
            String... includeFieldNames) {

        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        T entity = createQueryObject(id, entityClass);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(entity.get_id()), "id can not be null!");

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_DETAIL_URL, entity).buildCanonicalPath();

        RestExecutor builder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.GET,
                entity, url, service.getQueryParameter(context), service.getHeader(context), context);

        EntityResponseProcessor<T> responseBuilder = new EntityResponseProcessor<T>((Class<T>) entity.getClass(),
                builder.build(), service.getClientConfig(), HttpRequest.GET, context);

        List<T> objects = responseBuilder.getBuildEntity();
        T object = (objects == null || objects.size() == 0) ? null : objects.get(0);
        if (object != null) {
            object.includeFields(includeFieldNames);
            object.clearDirtyBits();
        }
        return object;
    }
    
    
    public <T extends ICMSEntity> T create(T entity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        Preconditions.checkNotNull(entity, "entity can not be null");
        validate(entity);

        // inner relationships
        MetaClass metaClass = metaService.getMetadata(entity.get_type(), context);
        Set<String> dirtyFields = Collections.unmodifiableSet(entity.getDirtyFields());
        Map<String, Object> oldInnerValues = detachFields(entity, metaClass.getInnerFields());

        try {
            // create the entity without inner field values
            String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_FACTORY_URL, entity).buildCanonicalPath();

            RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(),
                    HttpRequest.POST, entity, url, service.getQueryParameter(context), service.getHeader(context),
                    context);

            EntityResponseProcessor<String> respBuilder = new EntityResponseProcessor<String>(String.class,
                    reqBuilder.build(), service.getClientConfig(), HttpRequest.POST, context);

            List<String> oids = respBuilder.getBuildEntity();
            String entityId = oids.get(0);
            entity.set_id(entityId);

            // inner check create
            createOrUpdateRelationships(entity, oldInnerValues, context, dirtyFields);
        } finally {
            // make sure we restore the inner values
            attachFields(entity, oldInnerValues);
            entity.setDirtyFields(dirtyFields);
        }

        entity.clearDirtyBits();
        return entity;
    }

    /**
     * 
     * @param hostEntity
     * @param givenValues
     * @param context
     * @param dirtyFields
     *            - the dirt fields flag to control whether to operate on the
     *            given field. If given null, means don't do dirty check, operate on every given values.
     */
    private <T extends ICMSEntity> void createOrUpdateRelationships(final T hostEntity, Map<String, Object> givenValues,
            final CMSClientContext context, Set<String> dirtyFields) {
        for (Entry<String, Object> oldValue : givenValues.entrySet()) {
            final String fieldName = oldValue.getKey();
            // ignore non-dirty fields :: IAAS would not has enough permission
            // to modify the BM AssetServer's NetworkController
            if (isNonDirtyField(dirtyFields, fieldName)) {
                continue;
            }
            final Object fieldValue = oldValue.getValue();
            Callable<Integer> op = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    if (fieldValue instanceof List) {
                        for (Object innerEntity : (List<?>) fieldValue) {
                            createOrUpdateEntity(hostEntity, fieldName, innerEntity, context);
                        }
                    } else {
                        createOrUpdateEntity(hostEntity, fieldName, fieldValue, context);
                    }
                    return 0;
                }
            };
            retryOperation(service, logger, context, op, RETRY_LOG_MESSAGE, -1);
        }
    }

    private boolean isNonDirtyField(Set<String> dirtyFields, String fieldName) {
        return dirtyFields != null && !dirtyFields.contains(fieldName);
    }

    private <T extends ICMSEntity> void createOrUpdateEntity(T hostEntity, String fieldName, Object innerEntity,
            CMSClientContext context) {
        if (!(innerEntity instanceof ICMSEntity)) {
            return;
        }
        ICMSEntity entity = (ICMSEntity) innerEntity;
        if (Strings.isNullOrEmpty(entity.get_id())) {
            context.setPath(hostEntity.get_type(), hostEntity.get_id(), fieldName);
            create((ICMSEntity) innerEntity, context);
        } else {
            update((ICMSEntity) innerEntity, context);
        }
    }

    private <T extends ICMSEntity> Map<String, Object> detachFields(T entity, List<MetaRelationship> innerRelations) {
        Map<String, Object> oldValues = new HashMap<String, Object>();
        for (MetaRelationship rel : innerRelations) {
            if (entity.hasField(rel.getName())) {
                oldValues.put(rel.getName(), entity.getFieldValue(rel.getName()));
                entity.removeFieldValue(rel.getName());
            }
        }
        return oldValues;
    }

    private <T extends ICMSEntity> void attachFields(T entity, Map<String, Object> values) {
        for (String field : values.keySet()) {
            entity.setFieldValue(field, values.get(field));
        }
    }

    public <T extends ICMSEntity> void update(T entity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        service.checkLiveness();
        Preconditions.checkNotNull(entity, "entity can not be null");
        validate(entity);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(entity.get_id()), "id can not be null");

        // inner/embed relationships
        Set<String> dirtyFields = Collections.unmodifiableSet(entity.getDirtyFields());
        MetaClass metaClass = metaService.getMetadata(entity.get_type(), context);
        Map<String, Object> givenInnerValues = detachFields(entity, metaClass.getInnerFields());
        Map<String, Object> givenEmbedValues = detachFields(entity, metaClass.getEmbedFields());
        // make the sure the update doesn't contain the inner /embe fields
        try {
            String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_DETAIL_URL, entity).buildCanonicalPath();

            RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(),
                    HttpRequest.POST, (T) entity, url, service.getQueryParameter(context), service.getHeader(context),
                    context);

            // check return result using response builder
            new EntityResponseProcessor<String>(String.class, reqBuilder.build(), service.getClientConfig(),
                    HttpRequest.POST, context);

            updateInnerEmedRelationship(entity, context, givenInnerValues, givenEmbedValues, dirtyFields);
        } finally {
            // restore the remvoed inner values and embed values
            attachFields(entity, givenInnerValues);
            attachFields(entity, givenEmbedValues);
            entity.setDirtyFields(dirtyFields);
        }

        entity.clearDirtyBits();
    }

    public <T extends ICMSEntity> void updateWithRetry(final T entity, CMSClientContext context) {
        final CMSClientContext opContext = context != null ? context : new CMSClientContext();
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                GenericEntityService.this.update(entity, opContext);
                return 0;
            }
        };
        retryOperation(service, logger, opContext, callable, RETRY_LOG_MESSAGE, -1);
    }

    /**
     * This method to make sure the entity's given field value as the given values. It would
     *  delete - the existing field values that's not in the given values would be delete.
     *  modify - the existing field values that in the given values would be modified(updated).
     *  create - the given value that without oid is treated as new entity, it would be created. 
     * 
     * @param entity
     * @param context
     * @param givenInnerValues
     * @param givenEmbedValues
     */
    private <T extends ICMSEntity> void updateInnerEmedRelationship(T entity, CMSClientContext context,
            Map<String, Object> givenInnerValues, Map<String, Object> givenEmbedValues, Set<String> dirtyFields) {
        if (givenInnerValues.size() > 0 || givenEmbedValues.size() > 0) {
            T getEntity = null;
            if (entity.getClass().equals(GenericCMSEntity.class)) {
                getEntity = (T) getGeneric(entity.get_id(), entity.get_type(), context);
            } else {
                getEntity = getTypeSafe(entity.get_id(), (Class<T>) entity.getClass(), context);
            }
            // delete not need inner entities
            ensureRelationships(entity, context, givenInnerValues, dirtyFields, getEntity);
            // delete not need embed entities
            ensureRelationships(entity, context, givenEmbedValues, dirtyFields, getEntity);
        }
    }

    private <T extends ICMSEntity> void ensureRelationships(T entity, CMSClientContext context,
            Map<String, Object> givenValues, Set<String> dirtyFields, T getEntity) {
        // null check before delete
        if (getEntity != null) {
            for (Entry<String, Object> givenEntry : givenValues.entrySet()) {
                if (!isNonDirtyField(dirtyFields, givenEntry.getKey()) && getEntity.hasField(givenEntry.getKey())) {
                    List<ICMSEntity> getFieldValues = new ArrayList<ICMSEntity>();
                    Object value = getEntity.getFieldValue(givenEntry.getKey());
                    if (value instanceof ICMSEntity) {
                        getFieldValues.add((ICMSEntity) value);
                    } else if (value instanceof List) {
                        for (Object object : (List<?>) value) {
                            if (object instanceof ICMSEntity) {
                                getFieldValues.add((ICMSEntity) object);
                            }
                        }
                    }
                    deleteNotNeedFieldValue(entity, getFieldValues, givenEntry, context);
                }
            }
        }
        createOrUpdateRelationships(entity, givenValues, context, dirtyFields);
    }

    private <T extends ICMSEntity> void deleteNotNeedFieldValue(T parentEntity, List<ICMSEntity> getFieldValues,
            Entry<String, Object> givenValues, CMSClientContext context) {
        Object givenValue = givenValues.getValue();
        List<ICMSEntity> toBeDeleted = new ArrayList<ICMSEntity>();
        Set<String> giveIds = new HashSet<String>();
        // decide the to be delete entity ids
        if (givenValue instanceof ICMSEntity) {
            ICMSEntity e = (ICMSEntity) givenValue;
            giveIds.add(e.get_id());
        } else if (givenValue instanceof List) {
            for (Object o : (List<?>) givenValue) {
                if (o instanceof ICMSEntity) {
                    giveIds.add(((ICMSEntity) o).get_id());
                }
            }
        }
        // if the existing entity not in the given entity values
        for (ICMSEntity e : getFieldValues) {
            if (!giveIds.contains(e.get_id())) {
                toBeDeleted.add(e);
            }
        }
        if (!toBeDeleted.isEmpty()) {
            for (ICMSEntity toDelete : toBeDeleted) {
                try {
                    delete(toDelete, context);
                } catch (CMSClientException cce) {
                    if (cce.getHttpResponseCode() == Status.NOT_FOUND.getStatusCode() || 
                            (cce.getCmsResponseStatus() != null &&  CMSErrorCodeEnum.ENTITY_NOT_FOUND.equals(cce.getCmsResponseStatus().getErrorEnum())))
                    {
                        // there might be cases of race condition: the to deleted has been deleted.
                        // to end user, this is case that doesn't affect the "UPDATE" operation, ignore here.
                        logger.warn(MessageFormat.format("To be deleted entity got NOT FOUND reponse, this might indicate server data issue. Ignored this error! Entity : type:{0}, _oid:{1}", toDelete.get_type(), toDelete.get_id()), cce);
                    } else {
                        throw cce;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ICMSEntity> void replace(T entity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        service.checkLiveness();
        Preconditions.checkNotNull(entity, "entity can not be null");
        validate(entity);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(entity.get_id()), "id can not be null");

        // inner relationships
        MetaClass metaClass = metaService.getMetadata(entity.get_type(), context);
        Set<String> dirtyFields = Collections.unmodifiableSet(entity.getDirtyFields());
        Map<String, Object> oldValues = detachFields(entity, metaClass.getInnerFields());

        try {
            String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_DETAIL_URL, entity).buildCanonicalPath();

            RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.PUT,
                    (T) entity, url, service.getQueryParameter(context), service.getHeader(context), context);

            // check return result using response builder
            new EntityResponseProcessor<T>((Class<T>) entity.getClass(), reqBuilder.build(), service.getClientConfig(),
                    HttpRequest.PUT, context);

            // inner check create
            createOrUpdateRelationships(entity, oldValues, context, null);
        } finally {
            // make sure restore inner values
            attachFields(entity, oldValues);
            entity.setDirtyFields(dirtyFields);
        }

        entity.clearDirtyBits();
    }
    
    public <T extends ICMSEntity> void delete(T entity, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        service.checkLiveness();
        Preconditions.checkNotNull(entity, "entity can not be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(entity.get_id()), "id can not be null");
        validate(entity);

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_DETAIL_URL, entity).buildCanonicalPath();

        RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.DELETE,
                (T) null, url, service.getQueryParameter(context), service.getHeader(context), context);

        // check return result using response builder
        new EntityResponseProcessor<String>(String.class, reqBuilder.build(), service.getClientConfig(),
                HttpRequest.DELETE, context);
        entity.clearDirtyBits();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////Field Operations//////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    public <T extends ICMSEntity> void modifyField(T t, String fieldName, JsonBuilder jsonBuilder, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        fieldOperationCheck(t, fieldName);

        URLBuilder builder = new URLBuilder(service.getClientConfig(), Url.ENTITY_FIELD_PUSH_URL, new Object[] { t, fieldName });
        
        RestExecutor executor = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.POST, t, builder.buildCanonicalPath(),
                service.getQueryParameter(context), service.getHeader(context), jsonBuilder, context);
        
        new EntityResponseProcessor<String>(String.class, executor.build(), service.getClientConfig(), HttpRequest.POST, context);
        t.clearDirtyBits();
    }
    
    public <T extends ICMSEntity> void deleteField(T t, String fieldName, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        fieldOperationCheck(t, fieldName);
        
        URLBuilder builder = null;
        Map<String, String> headers = service.getHeader(context);
        if (t.hasField(fieldName)) {
            // pull
            builder = new URLBuilder(service.getClientConfig(), Url.ENTITY_FIELD_PULL_URL, new Object[] { t, fieldName });
            headers.put("X-HTTP-Method-Override", "POST");
        } else {
            // delete
            builder = new URLBuilder(service.getClientConfig(), Url.ENTITY_FIELD_URL, new Object[] { t, fieldName });
            // jersey solution for delete with payload: 
            // http://jersey.java.net/nonav/apidocs/latest/jersey/index.html?com/sun/jersey/api/container/filter/PostReplaceFilter.html
            headers.put("X-HTTP-Method-Override", "DELETE");
        }
        
        Map<String, String> queryParams = service.getQueryParameter(context);

        RestExecutor executor = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.POST, t, builder.buildCanonicalPath(),
                queryParams, headers, new FieldJsonBuilder(service.getClientConfig(), fieldName), context);
        
        new EntityResponseProcessor<String>(String.class, executor.build(), service.getClientConfig(), HttpRequest.POST, context);
        t.clearDirtyBits();
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////Batch operations///////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    public List<GenericCMSEntity> batchGetGeneric(GenericCMSEntity queryEntity, int limit, CMSClientContext context, String... includeFieldNames) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        Preconditions.checkNotNull(queryEntity, "query entity can not be null!");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(queryEntity.get_metaclass()), "meta class can not be null!");
        validate(queryEntity);

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_FACTORY_URL, queryEntity).buildCanonicalPath();

        Map<String, String> queryParam = service.getQueryParameter(context);
        queryParam.put(QueryParameter.allowFullTableScan.toString(), Boolean.TRUE.toString());
        if (limit >= 0) {
            queryParam.put(QueryParameter.limit.toString(), String.valueOf(limit));
        }
        
        RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.GET, queryEntity, url, queryParam,
                service.getHeader(context), context);
        
        EntityResponseProcessor<GenericCMSEntity> respBuilder = new EntityResponseProcessor<GenericCMSEntity>(
                GenericCMSEntity.class, reqBuilder.build(), service.getClientConfig(), HttpRequest.GET, context);
        
        
        List<GenericCMSEntity> entities = respBuilder.getBuildEntity();
        filterEntityFields(entities, includeFieldNames);
        return entities;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends ICMSEntity> List<T> batchGetTypeSafe(Class<T> entityClass, int limit, CMSClientContext context, String... includeFieldNames) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        T entity = createQueryObject(null, entityClass);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(entity.get_metaclass()), "meta data can not be null. Use genetric entity for the strong type api call?");
        
        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_FACTORY_URL, entity).buildCanonicalPath();

        Map<String, String> queryParam = service.getQueryParameter(context);
        queryParam.put(QueryParameter.allowFullTableScan.toString(), Boolean.TRUE.toString());
        if (limit >= 0) {
            queryParam.put(QueryParameter.limit.toString(), String.valueOf(limit));
        }
        
        RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.GET, entity, url, queryParam,
                service.getHeader(context), context);
        
        
        EntityResponseProcessor<T> respBuilder = new EntityResponseProcessor<T>((Class<T>) entity.getClass(),
                reqBuilder.build(), service.getClientConfig(), HttpRequest.GET, context);
        
        
        // filter entities
        List<T> entities = respBuilder.getBuildEntity();
        filterEntityFields(entities, includeFieldNames);
        return entities;
    }
    
    
    private <T extends ICMSEntity> void filterEntityFields(List<T> entities, String... includeFieldNames) {
        for (T e : entities) {
            e.includeFields(includeFieldNames);
            e.clearDirtyBits();
        }
    }

    // FIXME: inner/embed handling like create?
    public <T extends ICMSEntity> List<String> batchCreate(List<T> entities, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        checkBatchOperationArguments(entities);

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_BATCH_URL, entities).buildCanonicalPath();
        
        RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.POST,
                entities, url, service.getQueryParameter(context), service.getHeader(context), context);
        
        EntityResponseProcessor<String> respBuilder = new EntityResponseProcessor<String>(String.class,
                reqBuilder.build(), service.getClientConfig(), HttpRequest.POST, context);
        return respBuilder.getBuildEntity();
    }

    // FIXME: inner/embed handling like update?
    public <T extends ICMSEntity> void batchUpdate(List<T> entities, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        checkBatchOperationArguments(entities);

        String url = new URLBuilder(service.getClientConfig(), Url.ENTITY_BATCH_URL, entities).buildCanonicalPath();
        
        RestExecutor reqBuilder = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.PUT,
                entities, url, service.getQueryParameter(context), service.getHeader(context), context);
        
        new EntityResponseProcessor<String>(String.class, reqBuilder.build(), service.getClientConfig(),
                HttpRequest.PUT, context);
    }
    
    // FIXME: inner/embed handling like delete?
    public <T extends ICMSEntity> void batchDelete(List<T> entities, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        
        service.checkLiveness();
        checkBatchOperationArguments(entities);
        
        Map<String, String> headers = service.getHeader(context);
        URLBuilder builder = new URLBuilder(service.getClientConfig(), Url.ENTITY_BATCH_URL, entities);
        // jersey solution for delete with payload: 
        // http://jersey.java.net/nonav/apidocs/latest/jersey/index.html?com/sun/jersey/api/container/filter/PostReplaceFilter.html
        headers.put("X-HTTP-Method-Override", "DELETE");
        Map<String, String> queryParams = service.getQueryParameter(context);
        queryParams.putAll(context.getQueryParameters());

        RestExecutor executor = new RestExecutor(service.getClientConfig(), service.getClient(), HttpRequest.POST,
              entities, builder.buildCanonicalPath(), queryParams, headers, context);
        
        new EntityResponseProcessor<String>(String.class, executor.build(), service.getClientConfig(), HttpRequest.POST, context);
    }

    private <T extends ICMSEntity> void checkBatchOperationArguments(List<T> entities) {
        Preconditions.checkNotNull(entities, "entities could be not null");
        Preconditions.checkArgument(entities.size() > 0, "entites could be not empty");
        for (T t : entities) {
            validate(t);
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////Batch operations///////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////


    
    void validate(ICMSEntity queryEntity) {
        if (Strings.isNullOrEmpty(queryEntity.get_repo())) {
            queryEntity.set_repo(getRepository());
        } else if (!getRepository().equals(queryEntity.get_repo())) {
            throw new CMSClientException(MessageFormat.format(
                    "cms client service doesn''t have the same repository with the manipulated object: {0} vs {1}",
                    getRepository(), queryEntity.get_repo()));
        }

        if (Strings.isNullOrEmpty(queryEntity.get_branch())) {
            queryEntity.set_branch(getBranch());
        } else if (!getBranch().equals(queryEntity.get_branch())) {
            throw new CMSClientException(MessageFormat.format(
                    "cms client service doesn''t have the same branch with the manipulated object: {0} vs {1}",
                    getRepository(), queryEntity.get_branch()));
        }

        if (Strings.isNullOrEmpty(queryEntity.get_metaclass())) {
            throw new CMSClientException("entity metaclass can not be empty");
        }
    }
    

    private String getBranch() {
        return service.getBranch();
    }

    private String getRepository() {
        return service.getRepository();
    }

    <T extends ICMSEntity> T createQueryObject(String id, Class<T> entityClass) {
        Preconditions.checkArgument(entityClass != null, "entity class can not be null!");
        T entity = null;
        try {
            entity = entityClass.newInstance();
            entity.set_branch(getBranch());
            // enfore set-repository for both generic and type-safe. since to support re-use metaclass
            // in different repository.
            entity.set_repo(getRepository());
            entity.set_id(id);
        } catch (Exception e) {
            throw new CMSClientException(MessageFormat.format("Unable to create instance of class {0}",
                    entityClass.getName()));
        }
        return entity;
    }


    <T extends ICMSEntity> void fieldOperationCheck(T t, String fieldName) {
        service.checkLiveness();
        validate(t);

        Preconditions.checkArgument(StringUtils.isNotBlank(t.get_id()), "id could not be empty!");
        Preconditions.checkArgument(StringUtils.isNotBlank(fieldName), "fieldName could not be empty!");
    }

    /**
     * Retry count adoption by priority:
     *  <ul>
     *  <li> If given max count parameter (>0), use the higer between the given and context value.
     *  <li> Else If given through in client context, use it
     *  <li> Else If given in the client config (through client service), use it
     *  <li> Else 0 : means no retry
     *  </ul>
     * 
     * @param service
     * @param logger
     * @param context
     * @param op
     * @return
     */
    public static <T extends ICMSEntity, K> K retryOperation(CMSClientService service, Logger logger, CMSClientContext context, Callable<K> op,
            String message, int maxRetryCount) {
        int retryCount = 0;
        if (maxRetryCount > 0) {
            retryCount = context.hasRetryCount() ? Math.max(maxRetryCount, context.getRetryCount()) : maxRetryCount;
        } else {
            retryCount = service.getClientConfig().getRetryTime();
            if (context.hasRetryCount()) {
                retryCount = context.getRetryCount();
            }
            // avoid bad user setting
            if (retryCount < 0) {
                retryCount = 0;
            }
        }
        int totalCount = retryCount + 1;
        final LOG_LEVEL oldLevel = context.getLogSetting();
        // set as suppress log to avoid too much log in retry
        context.resetRetryMetric(0);
        context.setLogSetting(LOG_LEVEL.SUPPRESS_LOG);
        final int RETRY_MAX = totalCount;
        try {
            while (totalCount-- >= 0) {
                try {
                    return op.call();
                } catch (CMSClientException cce) {
                    if (totalCount > 0 && cce.getCmsResponseStatus() != null
                            && CMSErrorCodeEnum.VERSION_CONFLICT.equals(cce.getCmsResponseStatus().getErrorEnum())) {
                        logger.error(String.format("Not an error: " + message, totalCount));
                        // CMS-4500: wait for a random interval(random for random), use nano time and retryCount as random seed.
                        Random rd = new Random(System.nanoTime());
                        // find the interval between 3-100s, not to be real random :)
                        // increase interval after each retry failed
                        int interval = rd.nextInt((10 * (RETRY_MAX - totalCount)) % 300);
                        if (interval < 3) {
                            interval = interval + 3;
                        }
                        try {
                            Thread.sleep(interval * 1000);
                        } catch (InterruptedException ie) {
                            logger.info("retry operation interruptted! ignore this interruption, retry another around.", ie);
                        }
                        context.incRetryMetric();
                        continue;
                    } else {
                        logger.error(MessageFormat.format("Retry exhausted but still failed or non-version-conflict exception! Previous retry message are: " + message, retryCount), cce);
                        throw cce;
                    }
                } catch (Exception e) {
                    // should not happen
                    throw new CMSClientException("encounter unexpected exception in retry operation!", e);
                }
            }
        } finally {
            context.setLogSetting(oldLevel);
        }
        // nothing done
        return null;
    }
    
}
