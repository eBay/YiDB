/**
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.internal.QueryFilterBuilder;
import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.typsafe.restful.CMSQueryConstants;
import com.ebay.cloud.cms.typsafe.restful.QueryRestExecutor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder.Url;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author liasu
 * 
 */
public class QueryService {
    
//    private Logger logger = LoggerFactory.getLogger(QueryService.class);

    private final CMSClientService service;
    private final MetadataService metaService;

    QueryService(CMSClientService service, MetadataService metaService) {
        this.service = service;
        this.metaService = metaService;
    }

    public <T extends ICMSEntity> QueryIterator<T> queryIterator(CMSQuery query, Class<T> clz, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        return new QueryIterator<T>(clz, query, service, context);
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
    public <T extends ICMSEntity> CMSQueryResult<T> query(CMSQuery query, Class<T> targetClass, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        service.checkLiveness();
        Preconditions.checkNotNull(targetClass, "target class can not be null");
        Preconditions.checkNotNull(query, "query can not be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(query.getQueryString()),
                "query string can not be null or empty");
        if (StringUtils.isEmpty(query.getRepository())) {
            query.setRepository(service.getRepository());
        } else {
            Preconditions.checkArgument(service.getRepository().equals(query.getRepository()), "query repository not match the client service configuration, make it consistent, or simple not set in query!");
        }
        if (StringUtils.isEmpty(query.getBranch())) {
            query.setBranch(service.getBranch());
        } else {
            Preconditions.checkArgument(service.getBranch().equals(query.getBranch()), "query branch not match the client service configuration, make it consistent, or simple not set in query!");
        }

        // check use get or post :: use get is better in the cases for server side simply check access log could find the request
        Map<String,String> parameters = service.getQueryParameter(context);
        parameters.putAll(query.getQueryParams());
        HttpRequest request = null;
        String fullUrl = null;
        {
            request = HttpRequest.GET;
            fullUrl = new URLBuilder(service.getClientConfig(), Url.GET_QUERY_URL, query).buildCanonicalPath();
            int urlLength = fullUrl.length();
            urlLength++; // the '?' character
            for (Entry<String, String> paramPair : parameters.entrySet()) {
                urlLength += paramPair.getKey() != null ? paramPair.getKey().length() : 0;
                urlLength += paramPair.getValue() != null ? paramPair.getValue().length() : 0;
                urlLength++; // the '&' character
            }
            // reserved 100+ character for the url prefix
            if (urlLength > 900) {
                // use POST for long query string
                request = HttpRequest.POST;
                fullUrl = new URLBuilder(service.getClientConfig(), Url.QUERY_URL, query).buildCanonicalPath();
            }
        }

        QueryRestExecutor<T> queryExecutor = new QueryRestExecutor<T>(service.getClientConfig(), service.getClient(), query, fullUrl,
                request, parameters, service.getHeader(context), targetClass, context);
        return queryExecutor.build();
    }
    
    public <T extends ICMSEntity> CMSQueryResult<T> fullQuery(CMSQuery query, Class<T> targetClass, CMSClientContext context) {        
        long dbTimeCost = 0;
        long totalTimeCost = 0;
        boolean hasMore = false;
        Map<String, T> resultsMap = new HashMap<String, T>();
        
        do {
            CMSQueryResult<T> result = query(query, targetClass, context);
            
            // merge entities
            List<T> entities = result.getEntities();
            for (T entity : entities) {
                String id = entity.get_id();
                ICMSEntity mergedEntity = resultsMap.get(id);
                if (mergedEntity == null) {
                    resultsMap.put(id, entity);
                } else {
                    mergeEntity(mergedEntity, entity, context);
                }
            }
            
            // next round
            hasMore = result.isHasMore();
            int nextHint = result.getHint();
            long[] nextSkips = result.getSkips();
            long[] nextLimits = result.getLimits();
            String cursorString = result.getCursor();
            dbTimeCost += result.getDbTimeCost();
            totalTimeCost += result.getTotalTimeCost();
            
            query.setHint(nextHint);
            query.setSkips(nextSkips);
            query.setLimits(nextLimits);
            query.setCursor(cursorString);
        } while (hasMore);
        
        CMSQueryResult<T> finalResult = new CMSQueryResult<T>();
        finalResult.addResults(resultsMap.values());
        finalResult.setCount(Long.valueOf(resultsMap.values().size()));
        finalResult.setDbTimeCost(dbTimeCost);
        finalResult.setTotalTimeCost(totalTimeCost);
        
        return finalResult;
    }
    
    // merge entity2 to entity1
    @SuppressWarnings("unchecked")
    private <T extends ICMSEntity> void mergeEntity(T entity1, T entity2, CMSClientContext context) {
        String type = entity1.get_type();
        MetaClass meta = metaService.getMetadata(type, context);
        
        for (String fieldName : entity2.getFieldNames()) {
            MetaField field = meta.getField(fieldName);
            if (field instanceof MetaRelationship) {
                List<T> list1 = null;
                List<T> list2 = null;
                Object value1 = entity1.getFieldValue(fieldName);
                Object value2 = entity2.getFieldValue(fieldName);
                if (!(value1 instanceof List)) {
                    list1 = Arrays.asList((T)value1);
                } else  {
                    list1 = (List<T>) value1;
                }
                
                if (!(value2 instanceof List)) {
                    list2 = Arrays.asList((T)value2);
                } else {
                    list2 = (List<T>) value2;
                }
                
                List<T> mergedEntities = mergeFieldValues(list1,  list2, context);
                entity1.setFieldValue(fieldName, mergedEntities);
            } else {
                entity1.setFieldValue(fieldName, entity2.getFieldValue(fieldName));
            }
        }
    }
    
    private <T extends ICMSEntity> List<T> mergeFieldValues(List<T> entityList1, List<T> entityList2, CMSClientContext context) {
        List<T> mergedList = new LinkedList<T>();
        Map<String, T> idEntityMap = new HashMap<String, T>();
        for (T entity : entityList1) {
            String entityId = entity.get_id();
            T originEntity = idEntityMap.get(entityId);
            
            if (originEntity != null) {
                mergeEntity(originEntity, entity, context);
            } else {
                mergedList.add(entity);
                idEntityMap.put(entityId, entity);
            }
        }
        
        for (T entity2 : entityList2) {
            String entityId = entity2.get_id();
            if (idEntityMap.containsKey(entityId)) {
                T entity1 = idEntityMap.get(entityId);
                if (entity1 != entity2) {
                    mergeEntity(entity1, entity2, context);
                }
            } else {
                mergedList.add(entity2);
                idEntityMap.put(entityId, entity2);
            }
        }
        
        return mergedList;
    }
    
    public <T extends ICMSEntity> QueryIterator<T> getDanglingReference(Class<T> clz, String attribute, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        String metadata = clz.getSimpleName();
        Map<String, MetaClass> mm = service.getMetadatas(context);
        MetaClass mc = mm.get(metadata);
        MetaField field = mc.getField(attribute);
        if (!(field instanceof MetaRelationship)) {
            throw new CMSEntityException(String.format("Attribute %s must be an existing relationship on give metadata %s!", attribute, metadata));
        }
        return danglingReference(metadata, attribute, ((MetaRelationship) field).getRefDataType(), clz, context);
    }


    <T extends ICMSEntity> QueryIterator<T> danglingReference(String metadata, String attribute, String refDataType, Class<T> targetClass, CMSClientContext context) {
        CMSQuery query = new CMSQuery(String.format(CMSQueryConstants.QUERY_STRING, metadata, attribute, attribute,
                refDataType));
        query.setAllowFullTableScan(true);
        // affect the object id selection in the sub-query. FIXME: make this number more reasonable, currently,
        // the max collection size more than 1, 000, 000. Fetch all object would be a 30M query from server side.
        query.setMaxFetch(2000000);
        return queryIterator(query, targetClass, context);
    }
    
    public <T extends ICMSEntity> QueryIterator<T> getEmptyReference(Class<T> clz, String attribute, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();
        Map<String, MetaClass> mm = service.getMetadatas(context);
        MetaClass mc = mm.get(clz.getSimpleName());
        MetaField field = mc.getField(attribute);
        if (!(field instanceof MetaRelationship)) {
            throw new CMSEntityException(String.format("Attribute %s must be an existing relationship on give metadata %s!", attribute, clz.getSimpleName()));
        }
        return emptyReference(clz.getSimpleName(), attribute, clz, context);
    }

    <T extends ICMSEntity> QueryIterator<T> emptyReference(String metadata, String attribute, Class<T> targetClass, CMSClientContext context) {
        CMSQuery query = new CMSQuery(String
                .format(CMSQueryConstants.QUERY_EMPTY_REFERENCE_STRING, metadata, attribute));
        query.setAllowFullTableScan(true);
        // affect the object id selection in the sub-query. FIXME: make this number more reasonable, currently,
        // the max collection size more than 1, 000, 000. Fetch all object would be a 30M query from server side.
        query.setMaxFetch(2000000);
        return queryIterator(query, targetClass, context);
    }

    public List<GenericCMSEntity> getEntitiesByField(String queryPath, String fieldName, Object fieldValue,
            CMSClientContext context, String... includeFieldNames) {
        context = context != null ? context : new CMSClientContext();

        Preconditions.checkArgument(queryPath != null && !queryPath.isEmpty(), "queryPath could not be empty.");
        Preconditions.checkArgument(fieldName != null && !fieldName.isEmpty(), "fieldName could not be empty.");

        CMSQuery query = new CMSQuery(queryPath + QueryFilterBuilder.convertQueryValue(fieldName, fieldValue, includeFieldNames));
        query.setAllowFullTableScan(true);
        return query(query, GenericCMSEntity.class, context).getEntities();
    }
    
    public <T extends ICMSEntity> List<T> getEntitiesByField(Class<T> entityClass, String queryPath, String fieldName,
            Object fieldValue, CMSClientContext context, String... includeFieldNames) {
        context = context != null ? context : new CMSClientContext();

        Preconditions.checkArgument(entityClass != null, "entity class could not be null.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fieldName), "fieldName could not be null.");
        String metaType = queryPath;
        if (queryPath == null) {
            metaType = entityClass.getSimpleName();
        }
        CMSQuery query = new CMSQuery(metaType
                + QueryFilterBuilder.convertQueryValue(entityClass, fieldName, fieldValue, includeFieldNames));
        query.setAllowFullTableScan(true);
        return query(query, entityClass, context).getEntities();
    }
    
}
