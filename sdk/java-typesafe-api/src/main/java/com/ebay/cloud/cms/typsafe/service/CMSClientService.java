/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.RelationshipField;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.restful.FieldCASJsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.FieldJsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.restful.TokenProcessor;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder.Url;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

/**
 * The service methods must be state-less, so that we can have thread safe
 * guarantee.
 * 
 * @author liasu
 * 
 */
public class CMSClientService {
    private static final Logger   logger = LoggerFactory.getLogger(CMSClientService.class);

    private final Client          client;
    private final CMSClientConfig config;
    private volatile boolean      isClosed;

    private final RelationshipService   relationService;
    private final QueryService          queryService;
    private final GenericEntityService  genericEntityService;
    private final MetadataService       metadataService;

    private CMSClientService(CMSClientConfig config) {
        Preconditions.checkArgument(config != null, "client config can not be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getRepository()), "repoisotry can not be empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getBranch()), "branch can not be empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config.getClientPackagePrefix()), "client package prefix can not be null");
        this.config = config;
        this.client = Client.create();
        this.client.addFilter(new GZIPContentEncodingFilter(false));
        this.client.setReadTimeout(config.getTimeOut());
        this.client.setConnectTimeout(config.getConnnectionTimeout());
        this.isClosed = false;
        this.relationService = new RelationshipService(this);
        this.metadataService = new MetadataService(this);
        this.genericEntityService = new GenericEntityService(this, metadataService);
        this.queryService = new QueryService(this, metadataService);
    }

    public static CMSClientService getClientService(CMSClientConfig config) {
        return new CMSClientService(config);
    }

    public static CMSClientService getClientService(CMSClientConfig config, String user, String password) {
        CMSClientService client = new CMSClientService(config);
        String token = client.getToken(user, password);
        config.setAuthorization(token);
        config.setUser(user);
        return client;
    }

    public CMSClientConfig getClientConfig() {
        return config;
    }

    Client getClient() {
        return client;
    }

    public String getToken(String user, String password) {
        URLBuilder builder = new URLBuilder(config, Url.TOKEN_URL, user);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("X-Password", password);
        RestExecutor executor = new RestExecutor(config, client, HttpRequest.GET, user, builder.buildCanonicalPath(),
                null, headers, null);
        TokenProcessor tokenProcessor = new TokenProcessor(executor.build(), HttpRequest.GET, new CMSClientContext());
        return tokenProcessor.getBuildEntity();
    }

    @Deprecated
    public <T extends ICMSEntity> T get(String id, Class<T> entityClass) {
        return get(id, entityClass, new CMSClientContext());
    }

    public <T extends ICMSEntity> T get(String id, Class<T> entityClass, CMSClientContext context, String... includeFieldNames) {
        return genericEntityService.getTypeSafe(id, entityClass, context, includeFieldNames);
    }

    /**
     * Simple entity query method based on a field name and its value.
     * 
     * To get embed entities, use type like "Manifest.versions", and make the
     * entityClass as ManifestVersion.class. For embed query, the
     * fieldName/fieldValue are the field defined on the embed entity, ManifestVersion here..
     * 
     * @param entityClass
     *            - The return type of the expected get
     * @param queryPath
     *            - The type that to query. If given null, the query would based
     *            on the given entityClass.
     * @param fieldName
     * @param fieldValue
     *            - The field value that given. If given null, a query of
     *            "not exists" would be issued.
     * @param context
     * @param fieldNames -- the optional parameter to decide which fields are needed.
     * @return
     */
    public <T extends ICMSEntity> List<T> getEntitiesByField(Class<T> entityClass, String queryPath, String fieldName,
            Object fieldValue, CMSClientContext context, String... includeFieldNames) {
        return queryService.getEntitiesByField(entityClass, queryPath, fieldName, fieldValue, context, includeFieldNames);
    }

    public <T extends ICMSEntity> T getEntityByField(Class<T> entityClass, String queryPath, String fieldName,
            Object fieldValue, CMSClientContext context, String... includeFieldNames) {
        List<T> entities = getEntitiesByField(entityClass, queryPath, fieldName, fieldValue, context, includeFieldNames);
        if (entities.isEmpty()) {
            return null;
        } else if (entities.size() > 1) {
            throw new CMSClientException( MessageFormat
                            .format("getEntityByField expected only one field returned, but get {0}, consider getEntitiesByField if you want to get more than one entites.",
                                    entities.size()));
        } else {
            return entities.get(0);
        }
    }

    Map<String, String> getQueryParameter(CMSClientContext context) {
        if (context == null) {
            return new HashMap<String, String>();
        }
        return context.getQueryParameters();
    }

    Map<String, String> getHeader(CMSClientContext context) {
        Map<String, String> headers = config.getHeaders();
        if (context != null) {
            headers.putAll(context.getHeader());
        }
        return headers;
    }

    @Deprecated
    public <T extends ICMSEntity> List<T> get(Class<T> entityClass, int limit) {
        return get(entityClass, limit, new CMSClientContext());
    }

    public <T extends ICMSEntity> List<T> get(Class<T> entityClass, int limit, CMSClientContext context, String... includeFieldNames) {
        return genericEntityService.batchGetTypeSafe(entityClass, limit, context, includeFieldNames);
    }

    @Deprecated
    public <T extends ICMSEntity> T create(T entity) {
        return create(entity, new CMSClientContext());
    }

    public <T extends ICMSEntity> T create(T entity, CMSClientContext context) {
        return genericEntityService.create(entity, context);
    }

    @Deprecated
    public <T extends ICMSEntity> CMSQueryResult<T> query(CMSQuery query, Class<T> targetClass) {
        return query(query, targetClass, null);
    }

    public <T extends ICMSEntity> QueryIterator<T> queryIterator(CMSQuery query, Class<T> clz, CMSClientContext context) {
        return queryService.queryIterator(query, clz, context);
    }

    /**
     * Returns a cast list of entity. Based on given query and target class.
     * 
     * To have pagination for the query result, use <code>queryIterator</code>.
     * 
     * @param query
     *            - The query object to be executed.
     * @param targetClass
     *            - The generated model class that query result to be cast to. -
     *            If given {@link ICMSEntity}, a list of ICMSEntity will be
     *            return. The type will be determined by query result. Always
     *            make sure _type is available if you want to dynamically
     *            determine the type.
     * @param context
     * @return
     */
    public <T extends ICMSEntity> CMSQueryResult<T> query(CMSQuery query, Class<T> targetClass, CMSClientContext context) {
        return queryService.query(query, targetClass, context);
    }

    @Deprecated
    public <T extends ICMSEntity> void delete(T entity) {
        delete(entity, null);
    }

    public <T extends ICMSEntity> void delete(T entity, CMSClientContext context) {
        genericEntityService.delete(entity, context);
    }

    @Deprecated
    public <T extends ICMSEntity> void update(T entity) {
        update(entity, new CMSClientContext());
    }

    public <T extends ICMSEntity> void update(T entity, CMSClientContext context) {
        genericEntityService.update(entity, context);
    }

    /**
     * Update an entity with retry.
     * <p/>
     * 
     * NOTE: This method would simply retry a couple of times (based on the context setting, or the config setting), it doesn't
     * try any check/compare with the latest state on the server which is supposed to be client biz logic. <p/>
     * <b><i>Call this method when you're really understand there is no data race-conditions in client biz logic,
     * otherwise data corruption against biz logic might happen.</i>
     * </b>
     * <p/>
     * 
     * A data race-condition is described as below:</br>
     * Two instance of below flow running on different thread(or even different host) <p/>
     * Flow Action: 
     * <pre>
     * Read ResourceCapacity-oid-A; 
     * Check the field "available" > 0, 
     * If true, allocate the resource;
     *      then set the used to 5 (old used was 3), which would automatically set the "available" to 0.
     * </pre>
     * For use cases like this, use updateWithRetry() might have the resoures be allocated twice, so resources are allocated more than it has in the read world.
     * 
     * @param entity
     * @param context
     */
    public <T extends ICMSEntity> void updateWithRetry(T entity, CMSClientContext context) {
        genericEntityService.updateWithRetry(entity, context);
    }

    /**
     * Update the entity field.
     * 
     * NOTE: The method has different behavior based on the field definition.
     * <pre>
     * Update entity field
     * 
     * ---------------------------------------------------------------------
     * fieldType             |             Effect
     * ---------------------------------------------------------------------
     * Cardinality=Many      |      append (Same as appendToArray)                  
     * ---------------------------------------------------------------------
     * Json                  |      append (Same as appendToJson)
     * ---------------------------------------------------------------------
     * Cardinality=One       |      set field value (Same as modify entity)
     * ---------------------------------------------------------------------
     * </pre>
     * 
     * @param entity
     * @param fieldName
     * @param context
     */
    public <T extends ICMSEntity> void updateEntityField(T entity, String fieldName, CMSClientContext context) {
        genericEntityService.modifyField(entity, fieldName, new FieldJsonBuilder(config, fieldName), context);
    }

    /**
     * CAS(Compare And Set) API for update an entity's field. An entity's field
     * could be used as CAS only when
     * <ul>
     * <li>The field's not internal field and cardinality=ONE</li>
     * <li>The field's type MUST be one of below</li>
     * <ul>
     * <li>STRING</li>
     * <li>BOOLEAN</li>
     * <li>INTEGER</li>
     * <li>LONG</li>
     * </ul>
     * </ul>
     * 
     * @param entity
     *            - the new entity which has the new field value filled.
     * @param fieldName
     *            - the name of the field to be updated.
     * @param oldValueEntity
     *            - the old entity which has the old value filled for the given
     *            field.
     * @param context
     */
    public <T extends ICMSEntity> void updateEntityField(T entity, String fieldName, T oldValueEntity,
            CMSClientContext context) {
        Preconditions.checkNotNull(entity, "New value entity could not be null for conditional update!");
        Preconditions.checkNotNull(oldValueEntity, "Old value entity could not be null for conditional update!");
        genericEntityService.modifyField(entity, fieldName, new FieldCASJsonBuilder(config, fieldName, oldValueEntity), context);        
    }

    @Deprecated
    public <T extends ICMSEntity> void replace(T entity) {
        replace(entity, new CMSClientContext());
    }
    
    public <T extends ICMSEntity> void replace(T entity, CMSClientContext context) {
        genericEntityService.replace(entity, context);
    }

    @Deprecated
    public <T extends ICMSEntity> List<String> batchCreate(List<T> entities) {
        return batchCreate(entities, new CMSClientContext());
    }

    public <T extends ICMSEntity> List<String> batchCreate(List<T> entities, CMSClientContext context) {
        return genericEntityService.batchCreate(entities, context);
    }

    @Deprecated
    public <T extends ICMSEntity> void batchUpdate(List<T> entities) {
        batchUpdate(entities, new CMSClientContext());
    }

    public <T extends ICMSEntity> void batchUpdate(List<T> entities, CMSClientContext context) {
        genericEntityService.batchUpdate(entities, context);
    }
    
    @Deprecated
    public <T extends ICMSEntity> void batchDelete(List<T> entities) {
    	batchDelete(entities, new CMSClientContext());
    }

    public <T extends ICMSEntity> void batchDelete(List<T> entities, CMSClientContext context) {
        genericEntityService.batchDelete(entities, context);
    }

    /// weak-type apis

    @Deprecated
    public GenericCMSEntity get(String id, String metadata) {
        return get(id, metadata, new CMSClientContext());
    }

    public GenericCMSEntity get(String id, String metadata, CMSClientContext context, String... includeFieldNames) {
        return genericEntityService.getGeneric(id, metadata, context, includeFieldNames);
    }

    @Deprecated
    public List<GenericCMSEntity> get(GenericCMSEntity queryEntity, int limit) {
        return get(queryEntity, limit, new CMSClientContext());
    }
    
    public List<GenericCMSEntity> get(GenericCMSEntity queryEntity, int limit, CMSClientContext context, String... includeFieldNames) {
        return genericEntityService.batchGetGeneric(queryEntity, limit, context, includeFieldNames);
    }

    @Deprecated
    public CMSQueryResult<GenericCMSEntity> query(CMSQuery query) {
        return query(query, new CMSClientContext());
    }

    public CMSQueryResult<GenericCMSEntity> query(CMSQuery query, CMSClientContext context) {
        checkLiveness();
        return query(query, GenericCMSEntity.class, context);
    }
    
    /**
     * Returns a cast list of entity. Based on given query and target class.
     * 
     * @param query
     *            - The query object to be executed.
     * @param targetClass
     *            - The generated model class that query result to be cast to. -
     *            If given {@link ICMSEntity}, a list of ICMSEntity will be
     *            return. The type will be determined by query result. Always
     *            make sure _type is available if you want to dynamically
     *            determine the type.
     * @param context
     * @return
     */
    public <T extends ICMSEntity> CMSQueryResult<T> fullQuery(CMSQuery query, Class<T> targetClass, CMSClientContext context) {
        return queryService.fullQuery(query, targetClass, context);
    }

    public <T extends ICMSEntity> QueryIterator<T> getDanglingReference(Class<T> clz, String attribute, CMSClientContext context) {
        return queryService.getDanglingReference(clz, attribute, context);
    }

    /**
     * Generic API api for dangling check. 
     */
    public QueryIterator<GenericCMSEntity> getDanglingReference(String metadata, String attribute, String refDataType, CMSClientContext context) {
        return danglingReference(metadata, attribute, refDataType, GenericCMSEntity.class, context);
    }

    private <T extends ICMSEntity> QueryIterator<T> danglingReference(String metadata, String attribute, String refDataType, Class<T> targetClass, CMSClientContext context) {
        return queryService.danglingReference(metadata, attribute, refDataType, targetClass, context);
    }
    
    public <T extends ICMSEntity> QueryIterator<T> getEmptyReference(Class<T> clz, String attribute, CMSClientContext context) {
        return queryService.getEmptyReference(clz, attribute, context);
    }
    
    /**
     * Generic API api for empty reference check. 
     */
    public QueryIterator<GenericCMSEntity> getEmptyReference(String metadata, String attribute, CMSClientContext context) {
        return queryService.emptyReference(metadata, attribute, GenericCMSEntity.class, context);
    }

    /**
     * Generic API for query by field. Returns a list of the entity.
     * 
     * This api support embed entities query by accepting query typeName as "Manifest.versions".
     * Here the meta "Manifest" has a field "manifest" defined as "ManifestVersion". By given such a query type for embed,
     * the successing fieldName/fieldValue would be the field on "ManifestVersion"
     * 
     * @param queryPath
     * @param fieldName
     * @param fieldValue
     * @param context
     * @return
     */
    public List<GenericCMSEntity> getEntitiesByField(String queryPath, String fieldName, Object fieldValue,
            CMSClientContext context, String... includeFieldNames) {
        return queryService.getEntitiesByField(queryPath, fieldName, fieldValue, context, includeFieldNames);
    }
    
    /**
     * Generic API for query by field. Returns null or one entity, if there are more than one entity, throw exception
     * 
     * @param queryPath
     * @param fieldName
     * @param fieldValue
     * @param context
     * @return
     */
    public GenericCMSEntity getEntityByField(String queryPath, String fieldName, Object fieldValue,
            CMSClientContext context, String... includeFieldNames) {
        List<GenericCMSEntity> entities = getEntitiesByField(queryPath, fieldName, fieldValue, context, includeFieldNames);
        if (entities.size() == 0) {
            return null;
        } else if (entities.size() > 1) {
            throw new CMSClientException( MessageFormat
                    .format("getEntityByField expected only one field returned, but get {0}, consider getEntitiesByField if you want to get more than one entites.",
                            entities.size()));
        } else {
            return entities.get(0);
        }
    }

    @Deprecated
    public Set<String> getMetadataFields(String metadata) {
        return getMetadataFields(metadata, new CMSClientContext());
    }

    public Map<String, MetaClass> getMetadatas(CMSClientContext context) {
        return metadataService.getMetadatas(context);
    }
    
    public MetaClass getMetadata(String metadata, CMSClientContext context) {
        return metadataService.getMetadata(metadata, context);
    }

    public Set<String> getMetadataFields(String metadata, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        MetaClass meta = getMetadata(metadata, context);
        if (meta == null) {
            return Collections.emptySet();
        }
        Set<String> metaFields = new HashSet<String>(meta.getFields().keySet());
        return metaFields;
    }

    @Deprecated
    public <T extends ICMSEntity> void appendToArray(T t, String fieldName) {
        appendToArray(t, fieldName, new CMSClientContext());
    }

    /**
     * Appends the array field values to the given entity.
     * 
     * @param t
     *            - the payload entity.It must contain the entity id; the given
     *             value for the given arrau field.
     * @param fieldName
     *            - the field name that to be appended
     */
    public <T extends ICMSEntity> void appendToArray(T t, String fieldName, CMSClientContext context) {
        genericEntityService.modifyField(t, fieldName, new FieldJsonBuilder(config, fieldName), context);
    }

    @Deprecated
    public <T extends ICMSEntity> void appendToJson(T t, String fieldName) {
        appendToJson(t, fieldName, new CMSClientContext());
    }

    /**
     * Appends the json values to the given entity.
     * 
     * @param t
     *            - the payload entity.It must contain the entity id; the given
     *            value for the given json field.
     * @param fieldName
     *            - the field name that to be appended
     */
    public <T extends ICMSEntity> void appendToJson(T t, String fieldName, CMSClientContext context) {
        genericEntityService.modifyField(t, fieldName, new FieldJsonBuilder(config, fieldName), context);
    }

    @Deprecated
    public <T extends ICMSEntity> void deleteField(T t, String fieldName) {
        deleteField(t, fieldName, new CMSClientContext());
    }

    public <T extends ICMSEntity> void deleteField(T t, String fieldName, CMSClientContext context) {
        genericEntityService.deleteField(t, fieldName, context);
    }
    
    
    /////
    ///// Start of conveinent API for relationship
    /////
    /**
     * Creates relationship between the fromEntity and the toEntity. If the fromEntity(toEntity) is not existing, a new
     * entity will be created.
     * 
     * @param fromEntity
     * @param refField
     * @param toEntity
     * @param context
     */
    public <T extends ICMSEntity, K extends ICMSEntity> void createRelationship(T fromEntity, String refField, K toEntity, CMSClientContext context) {
        relationService.createRelationship(fromEntity, refField, toEntity, context != null ? context : new CMSClientContext());
    }

    @Deprecated
    public <T extends ICMSEntity, K extends ICMSEntity> void createRelationship(T fromEntity, RelationshipField<T, K> field, K toEntity, CMSClientContext context) {
        relationService.createRelationship(fromEntity, field, toEntity, context != null ? context : new CMSClientContext());
    }

    /**
     * Deletes relationship between the fromEntity and the toEntity if any.
     * 
     * @param fromEntity
     * @param refField
     * @param toEntity
     * @param context
     */
    public <T extends ICMSEntity,  K extends ICMSEntity> void deleteRelationship(T fromEntity, String refField, K toEntity, CMSClientContext context) {
        relationService.deleteRelationship(fromEntity, refField, toEntity, context != null ? context : new CMSClientContext());
    }

    @Deprecated
    public <T extends ICMSEntity,  K extends ICMSEntity> void deleteRelationship(T fromEntity, RelationshipField<T, K> field, K toEntity, CMSClientContext context) {
        relationService.deleteRelationship(fromEntity, field, toEntity, context != null ? context : new CMSClientContext());
    }
    /////
    ///// End of Conveinent API for relationship
    /////


    public boolean isAlive() {
        if (isClosed) {
            return false;
        }
        return getAliveness(client, config.getServerBaseUrl());
    }

    /**
     * Testing whether a given url is alive or not.
     * 
     * @param url
     * @return
     */
    public static boolean isAlive(String url) {
        return getAliveness(pingClient, url);
    }
    
    /* one singleton client for pre-testing the url availability */
    private static Client pingClient = Client.create();

    private static boolean getAliveness(Client client, String url) {
        Preconditions.checkArgument(!StringUtils.isEmpty(url), "url couldn't be empty!");
        WebResource resource = client.resource(url);
        boolean isAlive = false;
        try {
            ClientResponse response = resource.get(ClientResponse.class);
            isAlive = (response.getStatus() == Status.OK.getStatusCode());
        } catch (Exception e) {
            // any exception would treat the url not alive
            logger.error(MessageFormat.format("test live status for {0} failed with error: {1}.", url, e.getMessage()), e);
            isAlive = false;
        }
        return isAlive;
    }

    /**
     * Destroy the client.
     * <p>
     * Requirement from Jersey client: This method must be called when there are
     * not responses pending otherwise undefined behavior will occur.
     * <p>
     * The client must not be reused after this method is called otherwise
     * undefined behavior will occur.
     */
    public synchronized void close() {
        if (!isClosed) {
            isClosed = true;
            client.destroy();
        }
    }

    String getRepository() {
        return config.getRepository();
    }

    String getBranch() {
        return config.getBranch();
    }

    void checkLiveness() {
        if (isClosed) {
            throw new CMSClientException("client service already closed!");
        }
    }

}
