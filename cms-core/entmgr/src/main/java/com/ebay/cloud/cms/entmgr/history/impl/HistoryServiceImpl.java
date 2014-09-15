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

package com.ebay.cloud.cms.entmgr.history.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.DalServiceFactory.RegistrationEnum;
import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.entmgr.history.IEntityHistoryService;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DBObject;

/**
 * 
 * @author jianxu1, liasu
 * @date 2012/6/28
 * 
 */
public class HistoryServiceImpl implements IEntityHistoryService {

    public static final String DAL_PARAMETER = "dal";
    private static final List<String> HIST_SORT_FIELDS;
    private static final List<Integer> HIST_SORT_ORDERS;
    
    static {
        HIST_SORT_FIELDS = new ArrayList<String>(1);
        HIST_SORT_ORDERS = new ArrayList<Integer>(1);
        HIST_SORT_FIELDS.add(HistoryMetaClass.EntityVersion);
        HIST_SORT_ORDERS.add(SearchOption.DESC_ORDER);
    }
    
    private IPersistenceService persistenceService = null;
    private ISearchService searchService = null;
    private IRepositoryService repoService = null;
    
    private ObjectConverter<MetaClass> converter;
    private CMSDBConfig dbConfig;
    
    public HistoryServiceImpl(CMSDBConfig dbConfig, IRepositoryService repoService,
                              IPersistenceService persistenceService,
                              ISearchService searchService){
    	this.dbConfig = dbConfig;
        this.repoService = repoService;
        this.persistenceService = persistenceService;
        this.searchService = searchService;
        this.converter = new ObjectConverter<MetaClass>();
    }

    private List<IEntity> toJsonEntity(List<IEntity> histBsonEntities, MetaClass histMetaClass) {
        List<IEntity> histJsonEnities = new ArrayList<IEntity>();
        for (IEntity histBsonEntity : histBsonEntities) {
            IEntity jsonEnity = histBsonEntity;
            if (!(histBsonEntity instanceof JsonEntity)) {
                EntityMapper mapper = new EntityMapper(JsonEntity.class, histMetaClass, false);
                histBsonEntity.traverse(mapper);
                jsonEnity = mapper.getBuildEntity();
            }
            histJsonEnities.add(jsonEnity);
        }
        return histJsonEnities;
    }
    
    private Registration getDalRegistration(MetadataContext context) {
        String regisrationId = (String) context.getAdditionalParameter().get(DAL_PARAMETER);
        RegistrationEnum result = RegistrationEnum.fromString(regisrationId);
		if (result == null) {
			Integer defaultIndex = (Integer) dbConfig.get(CMSDBConfig.SYS_DAL_DEFAULT_IMPLEMENTATION);
			result = RegistrationEnum.fromIndex(defaultIndex);
		}
        Registration find = null;
        List<Registration> dalImplementations = persistenceService.getRegistrations();
        for (Registration r : dalImplementations) {
            if (r.registrationId.equals(result.name())) {
                find = r;
                break;
            }
        }
        return find;
    }

    @Override
    public String addHistory(MetaClass meta, String operType, MetadataContext context) {
        CheckConditions.checkNotNull(meta, "Miss meta class");
        CheckConditions.checkNotNull(operType, "Miss operation type");
        String repoName = meta.getRepository();
        String sourceIp = context.getSourceIp();
        CheckConditions.checkNotNull(repoName, "Miss repository name");
        CheckConditions.checkNotNull(sourceIp, "Miss source ip");

        Registration registration = getDalRegistration(context);
        Repository repoInst = repoService.getRepository(repoName);
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass histClass = HistoryMetaClass.getMetaClass(repoInst);
        
        JsonEntity historyEntity = new JsonEntity(histClass);
        historyEntity.setBranchId("MetaHistoryBranch");
        historyEntity.setLastModified(new Date());
        historyEntity.setStatus(StatusEnum.ACTIVE);
        historyEntity.addFieldValue(HistoryMetaClass.EntityId, meta.getName());
        historyEntity.addFieldValue(HistoryMetaClass.EntityVersion, meta.getVersion());
        historyEntity.addFieldValue(HistoryMetaClass.EntityParentVersion, meta.getParentVersion());
        historyEntity.addFieldValue(HistoryMetaClass.SourceIp, sourceIp);
        historyEntity.addFieldValue(HistoryMetaClass.OperType, operType);
        historyEntity.setCreator(context.getSubject());
        JsonNode json = converter.toJsonNode(meta);
        historyEntity.addFieldValue(HistoryMetaClass.LogBody, json);

        PersistenceContext persistenceContext = new PersistenceContext(metaService, DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), CMSConsts.METACLASS_HISTORY_COLL);
        persistenceContext.setDbConfig(context.getDbConfig());
        persistenceContext.setRegistration(registration);
        return persistenceService.create(historyEntity, persistenceContext);
    }
    
    private Integer getSysLimitDocuments(CMSDBConfig dbConfig) {
    	if (dbConfig != null) {
			Map<String, Object> configs = dbConfig.getCurrentConfiguration();
			if (configs.containsKey(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY)
					&& (configs.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY) instanceof Integer)) {
				return (Integer) (configs.get(CMSDBConfig.SYS_LIMIT_DOCUMENTS_MONGO_QUERY));
			}
    	}
		return SearchOption.DEFAULT_MAX_LIMIT;
	}

    private SearchOption buildSearchOption(Integer maxLimit, Integer skip, MetaClass historyMetaClass, int limit, ISearchStrategy strategy) {
        SearchOption option = new SearchOption();
        option.setStrategy(strategy);
        option.setLimit(maxLimit != null ? maxLimit : limit);
        option.setSkip(skip != null ? skip : 0);
        option.setSort(HIST_SORT_FIELDS, HIST_SORT_ORDERS, historyMetaClass);
        return option;
    }

    private SearchCriteria buildSearchCritiera(String metaName, MetaClass historyMetaClass, int version, ISearchStrategy strategy) {
        LogicalSearchCriteria logicCriteria = new LogicalSearchCriteria(LogicOperatorEnum.AND);
        
        MetaField entityIdField = historyMetaClass.getFieldByName(HistoryMetaClass.EntityId);
        SearchCriteria idCriteria = new FieldSearchCriteria(entityIdField, strategy, FieldOperatorEnum.EQ, metaName);
        logicCriteria.addChild(idCriteria);

        if (version >= 0) {
            MetaField entityVersionField = historyMetaClass.getFieldByName(HistoryMetaClass.EntityVersion);
            SearchCriteria versionCriteria = new FieldSearchCriteria(entityVersionField, strategy, FieldOperatorEnum.EQ, version);
            logicCriteria.addChild(versionCriteria);
        }

        return logicCriteria;
    }
    
    private SearchCriteria buildSearchCritiera(String metaName, MetaClass historyMetaClass, Date start, Date end, ISearchStrategy strategy) {
        MetaField entityIdField = historyMetaClass.getFieldByName(HistoryMetaClass.EntityId);
        SearchCriteria idCriteria = new FieldSearchCriteria(entityIdField, strategy, FieldOperatorEnum.EQ, metaName);

        LogicalSearchCriteria logicCriteria = null;
        if (start != null || end != null) {
            logicCriteria = new LogicalSearchCriteria(LogicOperatorEnum.AND);
        }

        MetaField lastModifiedField = historyMetaClass.getFieldByName(InternalFieldEnum.LASTMODIFIED.getName());
        SearchCriteria dateStart = null;
        if (start != null) {
            dateStart= new FieldSearchCriteria(lastModifiedField, strategy, FieldOperatorEnum.GE, start);
            logicCriteria.addChild(dateStart);
        }

        SearchCriteria dateEnd = null;
        if (end != null) {
            dateEnd = new FieldSearchCriteria(lastModifiedField, strategy, FieldOperatorEnum.LE, end);
            logicCriteria.addChild(dateEnd);
        }

        if (logicCriteria != null) {
            logicCriteria.addChild(idCriteria);
            return logicCriteria;
        } else {
            return idCriteria;
        }
    }

    @Override
    public MetaClass getHistory(String repoName, String metaName, int version, MetadataContext context) {
        CheckConditions.checkNotNull(repoName,"Miss repository name");
        CheckConditions.checkNotNull(metaName,"Miss metadata name");
        Registration registration = getDalRegistration(context);
        Repository repoInst = repoService.getRepository(repoName);
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass historyMetaClass = HistoryMetaClass.getMetaClass(repoInst);
        SearchCriteria criteria = buildSearchCritiera(metaName, historyMetaClass, version, registration.searchStrategy);

        SearchProjection searchProject = new SearchProjection();
        searchProject.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(historyMetaClass, criteria, searchProject, registration.searchStrategy);
        SearchOption option = buildSearchOption(0, 0, historyMetaClass, SearchOption.DEFAULT_MAX_LIMIT, registration.searchStrategy);
        PersistenceContext persistenceContext = new PersistenceContext(metaService, DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), CMSConsts.METACLASS_HISTORY_COLL);
        persistenceContext.setDbConfig(context.getDbConfig());
        persistenceContext.setRegistration(registration);
        SearchResult result = searchService.search(query, option, persistenceContext);
        
        MetaClass meta = null;
        List<IEntity> entities = result.getResultSet();
        if (entities.size() > 0) {
            @SuppressWarnings("unchecked")
            List<DBObject> bodies = (List<DBObject>) entities.get(0).getFieldValues(HistoryMetaClass.LogBody);
            if (bodies.size() > 0) {
                DBObject body = bodies.get(0);
                meta = converter.fromBson(body, MetaClass.class);
            }
        }
        
        return meta;
    }

    @Override
    public List<IEntity> getHistoryEntities(String repoName, String metaName, MetadataContext context) {
        CheckConditions.checkNotNull(repoName,"Miss repository name");
        CheckConditions.checkNotNull(metaName,"Miss metadata name");

        Repository repoInst = repoService.getRepository(repoName);
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass historyMetaClass = HistoryMetaClass.getMetaClass(repoInst);
        Registration registration = getDalRegistration(context);
        SearchCriteria criteria = buildSearchCritiera(metaName, historyMetaClass, context.getStart(), context.getEnd(), registration.searchStrategy);

        SearchProjection searchProject = new SearchProjection();
        searchProject.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(historyMetaClass, criteria, searchProject, getDalRegistration(context).searchStrategy);
        SearchOption option = buildSearchOption(context.getLimit(), context.getSkip(), historyMetaClass, getSysLimitDocuments(context.getDbConfig()), registration.searchStrategy);
        PersistenceContext persistenceContext = new PersistenceContext(metaService, DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), CMSConsts.METACLASS_HISTORY_COLL);
        persistenceContext.setDbConfig(context.getDbConfig());
        persistenceContext.setRegistration(registration);
        SearchResult result = searchService.search(query, option, persistenceContext );
        return toJsonEntity(result.getResultSet(), historyMetaClass);
    }

    @Override
    public void ensureHistoryIndex(Repository repo, MetaClass meta) {
    	List<MetaClass> metas = new ArrayList<MetaClass>();
    	metas.add(meta);
        PersistenceContext context = new PersistenceContext(repo.getMetadataService(), DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), CMSConsts.METACLASS_HISTORY_COLL);
        context.setRegistration(getDalRegistration(new MetadataContext()));
    	persistenceService.ensureIndex(metas, context, true);
    }
    
}
