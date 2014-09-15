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

package com.ebay.cloud.cms.sysmgmt.server;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.DalServiceFactory.RegistrationEnum;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.CollectionFinder;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBTimeCollector;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewCollectionFinder;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalEntityFactory;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.dal.persistence.impl.DalEntityFactory;
import com.ebay.cloud.cms.dal.persistence.impl.DalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.entmgr.branch.impl.BranchServiceImpl;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.history.IEntityHistoryService;
import com.ebay.cloud.cms.entmgr.history.impl.HistoryServiceImpl;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.IQueryService;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.impl.QueryServiceImpl;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.exception.NotSupportOperationException;
import com.ebay.cloud.cms.sysmgmt.healthy.HealthyManager;
import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IMonitorMetric;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.helper.CMSStateHelper;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityCasModifyFieldOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityCreateOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityDeleteFieldOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityDeleteOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityGetOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityModifyFieldOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityModifyOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityPullFieldOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.EntityReplaceOperation;
import com.ebay.cloud.cms.sysmgmt.server.helper.ExceptionHelper;
import com.ebay.cloud.cms.sysmgmt.server.helper.FieldHelper;
import com.ebay.cloud.cms.sysmgmt.server.helper.MonitorHelper;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;
import com.ebay.cloud.cms.sysmgmt.state.IEventListener;
import com.ebay.cloud.cms.sysmgmt.throttling.ThrottlingManager;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.WriteConcern;

/**
 * TODO: Spring AOP
 *
 */
public class CMSServer {

	private static final String DAL_FLATTEN = QueryContext.DAL_FLATTEN;

    private static final String DAL_HIERARCHY = QueryContext.DAL_HIERARCHY;

    private static final Logger logger = LoggerFactory.getLogger(CMSServer.class);

	private static CMSServer cmsServer;

	private ICMSConfig cmsConfig;

	private IRepositoryService repositoryService;
	private IQueryService queryService;
	private IEntityService entityService;
	private IEntityHistoryService historyService;
	private IBranchService branchService;

	private RequestIdSequence requestId = new RequestIdSequence();

	private CMSState cmsState = new CMSState();
	private CMSDBConfig dbConfig;

	private MonitorStatisticsManager monitor;
	private Map<String, IManagementServices> mgmtServices = new HashMap<String, IManagementServices>();
	public static final String MONITOR_SERVICE = "monitorService";
	public static final String HEALTHY_SERVICE = "healthyService";
	public static final String THROTTLING_SERVICE = "throttlingService";
	

	public static final List<DalErrCodeEnum> CMS_FAILURE_ERROR_CODES = Arrays.asList(
			DalErrCodeEnum.MONGO_EXCEPTION_UNKNOWN, DalErrCodeEnum.MONGO_EXCEPTION_NETWORK,
			DalErrCodeEnum.MONGO_EXCEPTION_CURSORNOTFOUND, DalErrCodeEnum.EXCEED_MAX_DOCUMENT_SIZE,
			DalErrCodeEnum.EXCEED_MAX_REPOSITORY_SIZE, DalErrCodeEnum.EXCEED_MAX_INDEXED_ARRAY_SIZE);

	private MongoDataSource dataSource;

    private List<Registration> dalImplementations;
    
    private volatile int throttlingLevel;

    // singleton class
	private CMSServer(ICMSConfig config) {
		cmsConfig = config;
		init();
	}

	private void createManagementServices(MongoDataSource ds) {
		monitor = new MonitorStatisticsManager(ds);
		mgmtServices.put(MONITOR_SERVICE, monitor);

		dbConfig = new CMSDBConfig(ds);
		String healthyExpression = (String) dbConfig.get(CMSDBConfig.HEALTHY_EXPRESSION);
		Integer healthyPeroidMs = (Integer) dbConfig.get(CMSDBConfig.HEALTHY_CHECK_PERIOD);

		HealthyManager healthyManager = new HealthyManager(monitor, cmsState, healthyExpression, healthyPeroidMs);
		mgmtServices.put(HEALTHY_SERVICE, healthyManager);
		
        Integer throttlingPeroidMs = (Integer) dbConfig.get(CMSDBConfig.THROTTLING_CHECK_PERIOD);
        String throttlingExpression = (String) dbConfig.get(CMSDBConfig.THROTTLING_EXPRESSION);
        
		ThrottlingManager throttlingManager = new ThrottlingManager(this, monitor, throttlingExpression, throttlingPeroidMs);
        mgmtServices.put(THROTTLING_SERVICE, throttlingManager);
	}

	private void initManagementServices() {
		for (IManagementServices s : mgmtServices.values()) {
			s.init();
		}
	}

	private void startManagementServices() {
		for (IManagementServices s : mgmtServices.values()) {
			s.startup();
		}
	}

	private void shutdownManagementServices() {
		for (IManagementServices s : mgmtServices.values()) {
			s.shutdown();
		}
	}

	private void loadConfiguration() {
		// 1. load default config
		cmsConfig.loadDefaultConfig();

		// 2. if cms.config exists in the CMS_HOME folder, use that
		// configuration to override default values
		cmsConfig.loadExternalConfig();

		loadLogConfiguration();
	}

	private MongoDataSource createMongoDataSource(String mongoConnection) {
		// use connection to connect to mongo, read options from mongo server,
		// return datasource using that options
		logger.info("connnect to mongo server : {}", mongoConnection);

		MongoDataSource bootstrapDS = new MongoDataSource(mongoConnection);
		try {
			CMSDBConfig dbConfig = new CMSDBConfig(bootstrapDS);
			return new MongoDataSource(mongoConnection, dbConfig);
		} finally {
			bootstrapDS.getMongoInstance().close();
		}
	}

	private void loadLogConfiguration() {
		String cmsHome = cmsConfig.getCMSHome();
		if (cmsHome == null) {
			// use default log configuration file
			return;
		}

		String configFile = cmsHome + File.separator + "logback.xml";
		File f = new File(configFile);
		if (f.exists()) {
			LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			JoranConfigurator jc = new JoranConfigurator();
			jc.setContext(context);
			context.reset();
			try {
				jc.doConfigure(f);
			} catch (JoranException e) {
				logger.error(MessageFormat.format("Load log configruation in CMS_HOME:{0} failed.", cmsHome), e);
			}
			StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		} else {
			logger.error(MessageFormat.format("Load log configruation in CMS_HOME:{0} failed.", cmsHome));
		}
	}

	private void init() {
		loadConfiguration();

		cmsState.setState(CMSState.State.startup);

		MongoDataSource dataSource = createMongoDataSource(cmsConfig.getMongoConnection());

		createManagementServices(dataSource);

		loadServices(dataSource);

		initManagementServices();
		
		this.dataSource = dataSource;
	}

    private void loadServices(MongoDataSource ds) {
        dalImplementations = initDalImplementation(ds);

        WriteConcern writeConcern = getWriteConcernFromDBConfig();

        repositoryService = RepositoryServiceFactory.createRepositoryService(ds, cmsConfig.getServerName(),
                writeConcern);

        entityService = ServiceFactory.getEntityService(ds, repositoryService, writeConcern, dalImplementations);

        ISearchService searchService = DalServiceFactory.getSearchService(ds);
        branchService = ServiceFactory.getBranchService(ds, writeConcern, dalImplementations);

        queryService = new QueryServiceImpl(repositoryService, branchService, searchService);

        historyService = ServiceFactory.getHistoryService(dbConfig, ds, dalImplementations);
    }

    public Registration getDalImplementation(String id) {
        RegistrationEnum result = RegistrationEnum.fromString(id);
        if (result == null) {
            Integer defaultIndex = (Integer) dbConfig.get(CMSDBConfig.SYS_DAL_DEFAULT_IMPLEMENTATION);
            result = RegistrationEnum.fromIndex(defaultIndex);
        }
        Registration find = null;
        for (Registration r : dalImplementations) {
            if (r.registrationId.equals(result.name())) {
                find = r;
                break;
            }
        }
        return find;
    }

    private List<Registration> initDalImplementation(MongoDataSource ds) {
        Registration reg = new Registration(DAL_HIERARCHY, new PersistenceServiceImpl(ds), BsonEntity.class,
                DalEntityFactory.getInstance(), DalSearchStrategy.getInstance(), EntityIDHelper.getInstance(), new CollectionFinder());

        Registration flatten = new Registration(DAL_FLATTEN, new NewPersistenceServiceImpl(ds), NewBsonEntity.class,
                NewDalEntityFactory.getInstance(), NewDalSearchStrategy.getInstance(), FlattenEntityIDHelper.getInstance(), new NewCollectionFinder());
        return Arrays.asList(reg, flatten);
    }

    private WriteConcern getWriteConcernFromDBConfig() {
        String concern = (String) dbConfig.get(CMSDBConfig.SYS_WRITE_CONCERN);
        return ConsistentPolicy.WRITE_CONCERN_MAP.get(concern);
    }

    public void addWriteRequest(int value, long timestamp, Object detail, boolean isSuccess) {
		MonitorHelper.addWriteRequest(monitor, value, timestamp, detail, isSuccess);
	}

	public MonitorStatisticsManager getMonitorStatisticsManager() {
		return monitor;
	}

	public CMSDBConfig getDBConfig() {
		return dbConfig;
	}

	public void start() {
		startManagementServices();

		cmsState.setState(CMSState.State.normal);
	}

	public void pause() {
		cmsState.setState(CMSState.State.maintain);
	}

	public void resume() {
		cmsState.setState(CMSState.State.normal);
	}

	public void shutdown() {
		// do some cleanup
		shutdownManagementServices();

		cmsState.setState(CMSState.State.shutdown);
	}

	/* **************************************************************************
	 * proxy methods for branch services
	 * *************************************************************************
	 */
	public IBranch createBranch(CMSPriority p, IBranch branch, EntityContext context) {
	    checkServerState(p, true);
		setupEntityContext(context);

		String id = requestId.getNext();
		context.setRequestId(id);
		logger.info("[cms createBranch {}, {}] {}, {}",
				new Object[] { id, context.getSourceIp(), branch.getRepositoryName(), branch.getId() });

		String branchName = branchService.createBranch(branch, context).getId();
		return branchService.getBranch(branch.getRepositoryName(), branchName, context);
	}

	public IBranch getBranch(CMSPriority p, String repoName, String branchId, EntityContext context) {
	    checkServerState(p, false);
		setupEntityContext(context);

		String id = requestId.getNext();
		context.setRequestId(id);
		logger.info("[cms getBranch {}, {}] {}, {}", new Object[] { id, context.getSourceIp(), repoName, branchId });
        context.setDbConfig(dbConfig);
		return branchService.getBranch(repoName, branchId, context);
	}

	public List<IBranch> getMainBranches(CMSPriority p, String repoName, EntityContext context) {
	    checkServerState(p, false);
		setupEntityContext(context);

		String id = requestId.getNext();
		context.setRequestId(id);
		context.setDbConfig(getDBConfig());
		logger.info("[cms getBranches {}, {}] {}", new Object[] { id, context.getSourceIp(), repoName });
        context.setDbConfig(dbConfig);
		return branchService.getMainBranches(repoName, context);
	}

	/* **************************************************************************
	 * proxy methods for monitor services
	 * *************************************************************************
	 */
	public Map<String, IManagementServices.ServiceStatus> getServiceStatuses() {
		HashMap<String, IManagementServices.ServiceStatus> result = new HashMap<String, IManagementServices.ServiceStatus>(
				mgmtServices.size());

		for (Entry<String, IManagementServices> e : mgmtServices.entrySet()) {
			result.put(e.getKey(), e.getValue().isRunning());
		}

		return result;
	}

	public IManagementServices.ServiceStatus getServiceStatus(String serviceName) {
		IManagementServices service = mgmtServices.get(serviceName);
		if (service != null) {
			return service.isRunning();
		} else {
			return null;
		}
	}

	public void setServiceStatus(String serviceName, IManagementServices.ServiceStatus status) {
		IManagementServices service = mgmtServices.get(serviceName);
		if (service != null) {
			if (status == IManagementServices.ServiceStatus.running) {
				service.startup();
			} else {
				service.shutdown();
			}
		}
	}

	/* **************************************************************************
	 * proxy methods for monitor metrics
	 * *************************************************************************
	 */
	public Map<String, Object> getStatistics() {
		if (cmsState.getState() != CMSState.State.startup) {
			return monitor.getStatistics();
		} else {
			return Collections.emptyMap();
		}
	}

	public Map<String, Object> getStatistics(String metricName) {
		if (cmsState.getState() != CMSState.State.startup) {
			IMonitorMetric metric = monitor.getMetric(metricName);
			if (metric != null) {
				return metric.output();
			}
			return null;// null as no such metric exists
		} else {
			return Collections.emptyMap();
		}
	}

	/* **************************************************************************
	 * proxy methods for state
	 * *************************************************************************
	 */
	public CMSState.State getState() {
		return cmsState.getState();
	}

	/* **************************************************************************
	 * proxy methods for metadata services
	 * *************************************************************************
	 */
	public List<Repository> getRepositories(CMSPriority p, MetadataContext context) {
	    checkServerState(p, false);
		return repositoryService.getRepositories(context);
	}

	public Repository getRepository(CMSPriority p, String repoName) {
	    checkServerState(p, false);
		return repositoryService.getRepository(repoName);
	}

	public Repository createRepository(CMSPriority p, Repository repo) {
	    checkServerState(p, true);
	    RepositoryOption repositoryOption = repo.getOptions();
	    if (null == repositoryOption.getMaxDocumentSize()) {
	    	repositoryOption.setMaxDocumentSize((Long) dbConfig.get(CMSDBConfig.DEFAULT_SYS_LIMIT_MAX_DOCUMENT_SIZE));
	    }
	    if (null == repositoryOption.getMaxIndexedArraySize()) {
	    	repositoryOption.setMaxIndexedArraySize((Integer) dbConfig.get(CMSDBConfig.DEFAULT_SYS_LIMIT_MAX_INDEXED_ARRAY_SIZE));
	    }
	    if (null == repositoryOption.getMaxNumOfIndexes()) {
	    	repositoryOption.setMaxNumOfIndexes((Integer) dbConfig.get(CMSDBConfig.DEFAULT_SYS_LIMIT_MAX_INDEXES_NUM));
	    }
	    if (null == repositoryOption.getMaxRepositorySize()) {
	    	repositoryOption.setMaxRepositorySize((Long) dbConfig.get(CMSDBConfig.DEFAULT_SYS_LIMIT_MAX_REPOSITORY_SIZE));
	    }
		Repository repository = repositoryService.createRepository(repo);

//		boolean isDefaultBranchExists = false;
//		try {
//			isDefaultBranchExists = (branchService.getBranch(repo.getRepositoryName(), IBranch.DEFAULT_BRANCH,
//					new EntityContext()) != null);
//		} catch (Throwable e) {
//			isDefaultBranchExists = false;
//		}
//		if (!isDefaultBranchExists) {
        Branch branch = new Branch();
        branch.setRepositoryName(repo.getRepositoryName());
        branch.setMainBranch(true);
        branch.setId(IBranch.DEFAULT_BRANCH);
        EntityContext entityContext = new EntityContext();
        entityContext.setDbConfig(dbConfig);
        entityContext.setRegistration(getDalImplementation(null));
        branchService.createBranch(branch, entityContext);
//		}
		return repository;
	}

	public void updateRepository(CMSPriority p, Repository repo) {
	    checkServerState(p, true);
		repositoryService.updateRepository(repo);
	}

	public void deleteRepository(CMSPriority p, String repoName) {
	    checkServerState(p, true);

		Object value = getDBConfig().get(CMSDBConfig.SYS_ALLOW_REPOSITORY_DELETE);
		if (value instanceof Boolean && (Boolean) value) {
			repositoryService.deleteRepository(repoName);
			if (branchService instanceof BranchServiceImpl) {
				((BranchServiceImpl) branchService).clearBranchCache();
			}
		} else {
			throw new NotSupportOperationException("Delete repository not enabled!");
		}
	}

	public List<MetaClass> getMetaClasses(CMSPriority p, String repoName, MetadataContext context) {
	    checkServerState(p, false);
		Repository repo = repositoryService.getRepository(repoName);

		return repo.getMetadataService().getMetaClasses(context);
	}

	public MetaClass getMetaClass(CMSPriority p, String repoName, String metaclassName) {
		Repository repo = repositoryService.getRepository(repoName);

		return repo.getMetadataService().getMetaClass(metaclassName);
	}

    public Map<String, List<String>> getMetaClassHierarchy(CMSPriority p, String repoName, String metaclassName, MetadataContext context) {
        checkServerState(p, false);
        Repository repo = repositoryService.getRepository(repoName);
        MetaClass metadata = repo.getMetadataService().getMetaClass(metaclassName);
        if (metadata == null) {
            throw new MetaClassNotExistsException(repoName, metaclassName);
        }

        Map<String, List<String>> hierarchy = new TreeMap<String, List<String>>();
        hierarchy.put("ancestors", metadata.getAncestors() == null ? Collections.<String> emptyList() : metadata.getAncestors());
        List<MetaClass> childMetas = metadata.getDescendants();
        List<String> list =new ArrayList<String>();
        for (MetaClass child : childMetas) {
            list.add(child.getName());
        }
        hierarchy.put("descendants", list);
        return hierarchy;
    }

	public MetaClass getMetaClass(CMSPriority p, String repoName, String metaclassName, int version) {
		Repository repo = repositoryService.getRepository(repoName);
		MetadataContext context = new MetadataContext();
        context.setDbConfig(dbConfig);
        return repo.getMetadataService().getMetaClass(metaclassName, version, context);
	}

	public void addIndex(CMSPriority p, String repoName, String metaclassName, List<IndexInfo> index,
			MetadataContext context) {
		String[] indexNames = new String[index.size()];
		for (int i = 0; i < index.size(); i++) {
			indexNames[i] = index.get(i).getIndexName();
		}
		logger.info("cms addIndex: {}, {}, {}, {}",
				new Object[] { repoName, metaclassName, indexNames, context.getSourceIp() });
		checkServerState(p, true);

		Repository repo = repositoryService.getRepository(repoName);

		MetaOption options = new MetaOption();
		for (IndexInfo ii : index) {
			options.addIndex(ii);
		}
        context.setDbConfig(dbConfig);
		repo.getMetadataService().updateMetaOption(metaclassName, options, context);
		try {
			updateMetadataIndexOnBranch(metaclassName, context, repo);
		} catch (CmsDalException e) {
			for (IndexInfo ii : index) {
				context.setOptionChangeMode(UpdateOptionMode.DELETE);
				deleteIndex(p, repoName, metaclassName, ii.getIndexName(), context);
			}
			throw e;
		}
	}

	private void updateMetadataIndexOnBranch(String metaclassName, MetadataContext context, Repository repo) {
		MetaClass meta = repo.getMetadataService().getMetaClass(metaclassName);
		List<MetaClass> metas = new ArrayList<MetaClass>();
		metas.add(meta);
		metas.addAll(meta.getDescendants());

		EntityContext entityContext = new EntityContext();
		entityContext.setDbConfig(context.getDbConfig());
		entityContext.setSourceIp(context.getSourceIp());
		entityContext.setRegistration(getDalImplementation(context.getAdditionalParameter().get(HistoryServiceImpl.DAL_PARAMETER)));
		branchService.ensureIndex(repo.getRepositoryName(), metas, entityContext);
	}

	public void deleteIndex(CMSPriority p, String repoName, String metaclassName, String indexName,
			MetadataContext context) {
		logger.info("cms deleteIndex: {}, {}, {}, {}",
				new Object[] { repoName, metaclassName, indexName, context.getSourceIp() });
		checkServerState(p, true);
		Repository repo = repositoryService.getRepository(repoName);

		MetaOption options = new MetaOption();
		IndexInfo ii = new IndexInfo(indexName);
		options.addIndex(ii);
		context.setDbConfig(dbConfig);
		repo.getMetadataService().updateMetaOption(metaclassName, options, context);

		updateMetadataIndexOnBranch(metaclassName, context, repo);
	}

	public List<IEntity> getMetaHistory(CMSPriority p, String repositoryId, String metaType, MetadataContext context) {
	    checkServerState(p, false);
		
		context.setDbConfig(getDBConfig());

		long current = System.currentTimeMillis();
		List<IEntity> entity = historyService.getHistoryEntities(repositoryId, metaType, context);

		long total = System.currentTimeMillis() - current;
		context.setTotalTimeCost(total);
		String query = repositoryId + ": " + metaType;
		MonitorHelper.addReadRequest(monitor, (int) total, current, query, true);
		return entity;
	}

	public List<MetaClass> batchUpsert(CMSPriority p, String repoName, List<MetaClass> metas, MetadataContext context) {
		String[] metaclassNames = new String[metas.size()];
		for (int i = 0; i < metas.size(); i++) {
			metaclassNames[i] = metas.get(i).getName();
		}
		logger.info("cms batchUpsert: {}, {}, {}", new Object[] { repoName, metaclassNames, context.getSourceIp() });
		checkServerState(p, true);
        context.setDbConfig(dbConfig);
		Repository repo = repositoryService.getRepository(repoName);

		List<MetaClass> metaClasses = repo.getMetadataService().batchUpsert(metas, context);
		// ensure index
		for (MetaClass clz : metaClasses) {
			updateMetadataIndexOnBranch(clz.getName(), context, repo);
		}

		return metaClasses;
	}

	public MetaClass updateMetaField(CMSPriority p, String repoName, MetaClass meta, String fieldName,
			MetadataContext context) {
		logger.info("cms updateMetaField: {}, {}, {}, {}",
				new Object[] { repoName, meta.getName(), fieldName, context.getSourceIp() });
		checkServerState(p, true);
		Repository repo = repositoryService.getRepository(repoName);
		context.setDbConfig(dbConfig);

		MetaClass metaClass = repo.getMetadataService().updateMetaField(meta, fieldName, context);
		// ensure index
		updateMetadataIndexOnBranch(metaClass.getName(), context, repo);
		return metaClass;
	}

	public void deleteMetadata(CMSPriority p, String repoName, String metaclassName, MetadataContext context) {
		logger.info("cms deleteMetadata: {}, {}, {}", new Object[] { repoName, metaclassName, context.getSourceIp() });
		
		checkServerState(p, true);
		String id = requestId.getNext();
		context.setRequestId(id);

		long current = System.currentTimeMillis();
		context.setStartProcessingTime(current);
		context.setDbConfig(dbConfig);

		Object value = getDBConfig().get(CMSDBConfig.SYS_ALLOW_METADATA_DELETE);
		if (value instanceof Boolean && (Boolean) value) {
			IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
			MetaClass meta = metaService.getMetaClass(metaclassName);
			metaService.deleteMetaClass(metaclassName, context);
			EntityContext ec = new EntityContext();
			ec.setSourceIp(context.getSourceIp());
			ec.setDbConfig(context.getDbConfig());
            ec.setRegistration(getDalImplementation(context.getAdditionalParameter().get(
                    HistoryServiceImpl.DAL_PARAMETER)));
			branchService.deleteMetadata(repoName, meta, ec);

			long total = System.currentTimeMillis() - current;
			context.setTotalTimeCost(total);
			context.addDbTimeCost(context.getDbTimeCost());
		} else {
			throw new NotSupportOperationException("Delete metadata not enabled!");
		}
	}

	public void validateMetadata(CMSPriority p, String repoName, String metaclassName, MetadataContext context) {
		logger.info("cms validateMetadata: {}, {}, {}", new Object[] { repoName, metaclassName, context.getSourceIp() });
		
        checkServerState(p, false);
        
		String id = requestId.getNext();
		context.setRequestId(id);

		long current = System.currentTimeMillis();
		context.setStartProcessingTime(current);
		context.setDbConfig(dbConfig);

		IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
		metaService.validateMetaClass(metaclassName);

		long total = System.currentTimeMillis() - current;
		context.setTotalTimeCost(total);
		context.addDbTimeCost(context.getDbTimeCost());
	}

	public void deleteMetaField(CMSPriority p, String repoName, String metaclassName, String fieldName,
			MetadataContext context) {
		logger.info("cms deleteMetaField: {}, {}, {}, {}",
				new Object[] { repoName, metaclassName, fieldName, context.getSourceIp() });
		
        checkServerState(p, true);
        
		String id = requestId.getNext();
		context.setRequestId(id);
        context.setDbConfig(dbConfig);
		long current = System.currentTimeMillis();
		context.setStartProcessingTime(current);
		Object value = getDBConfig().get(CMSDBConfig.SYS_ALLOW_METADATA_DELETE);
		if (value instanceof Boolean && (Boolean) value) {
			IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
			metaService.deleteMetaField(metaclassName, fieldName, context);

			long total = System.currentTimeMillis() - current;
			context.setTotalTimeCost(total);
			context.addDbTimeCost(context.getDbTimeCost());
		} else {
			throw new NotSupportOperationException("Delete meta field not enabled!");
		}
	}

	/* **************************************************************************
	 * proxy methods for entity services
	 * *************************************************************************
	 */
	
//	protected void doInitAction() {
//        String reqId = requestId.getNext();
//        context.setRequestId(reqId);
//        this.current = System.currentTimeMillis();
//        context.setStartProcessingTime(current);
//        checkServerState(priority, write);
//        setupEntityContext(context);
//        DBTimeCollector.reset();
//    }
	
	public IEntity get(CMSPriority p, IEntity queryEntity, Map<String, List<String>> params, EntityContext context) {
        checkServerState(p, false);
        
	    setupEntityContext(context);
		EntityOperation operation = new EntityGetOperation(p, entityService, queryEntity, null, null, context, params, cmsState, requestId.getNext(), monitor);
		return (IEntity) operation.perform();
	}


	public void delete(CMSPriority p, IEntity queryEntity, EntityContext context) {
		logger.info("cms delete: {}, {}, {}",
				new Object[] { queryEntity.getType(), queryEntity.getId(), context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		EntityDeleteOperation operation = new EntityDeleteOperation(p, entityService, queryEntity, null, null, context, cmsState, requestId.getNext(), monitor);
		operation.perform();
	}

	public String create(CMSPriority p, IEntity entity, EntityContext context) {
		logger.info("cms create: {}, {}", new Object[] { entity.getType(), context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		EntityOperation operation = new EntityCreateOperation(p, entityService, null, entity, null, context, cmsState, requestId.getNext(), monitor);
		return (String) operation.perform();
	}

	@SuppressWarnings("unchecked")
	private void setMongoMetrics(EntityContext context) {
		Map<String, Object> mongoMetricsMap = getStatistics(MetricConstants.MONGO_METRIC);
		Map<String, Object> databaseSizeMap = (Map<String, Object>) mongoMetricsMap.get(MetricConstants.MONGO_DB_SIZE);
		context.setDatabaseSizeMap(databaseSizeMap);
	}

	public List<String> batchCreate(CMSPriority p, List<IEntity> entities, EntityContext context, List<String> parseFails) {
		logger.info("cms batchCreate: {}, {}", new Object[] { entities.size(), context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		return entityService.batchCreate(entities, context, parseFails);
	}

    public List<String> batchUpdate(CMSPriority p, List<IEntity> entities, EntityContext context, List<String> parseFails) {
		logger.info("cms batchUpdate: {}, {}", new Object[] { entities.size(), context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		return entityService.batchModify(entities, context, parseFails);
	}
	
	public List<String> batchDelete(CMSPriority p, List<IEntity> entities, EntityContext context, List<String> parseFails) {
		logger.info("cms batchDelete: {}, {}", new Object[] { entities.size(), context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		return entityService.batchDelete(entities, context, parseFails);
	}

	public IQueryResult batchGet(CMSPriority p, String metadata, Map<String, List<String>> params, QueryContext context) {
		logger.info("cms batchGet: {}, {}", new Object[] { metadata, context.getSourceIP() });
		
		checkServerState(p, false);
        
		Repository repo = repositoryService.getRepository(context.getRepositoryName());
		MetaClass meta = repo.getMetadataService().getMetaClass(metadata);
		if (meta == null) {
			throw new MetaClassNotExistsException(context.getRepositoryName(), metadata);
		}

		String filter = null;
		if (params.containsKey("filters")) {
			filter = params.get("filters").get(0);
		}
		String fields = FieldHelper.getFieldsParameter(params);
		StringBuilder queryBuilder = new StringBuilder(metadata);
		if (filter != null) {
			queryBuilder.append("[");
			queryBuilder.append(filter);
			queryBuilder.append("]");
		}
		if (fields != null) {
			queryBuilder.append("{");
			queryBuilder.append(fields);
			queryBuilder.append("}");
		}
		context.addSortOn(InternalFieldEnum.ID.getName());
		context.setDbConfig(dbConfig);
		return query(p, queryBuilder.toString(), context);
	}

	public void replace(CMSPriority p, IEntity queryEntity, IEntity entity, EntityContext context) {
		logger.info("cms replace: {}, {}, {}", new Object[] { queryEntity, entity, context.getSourceIp() });
		
		checkServerState(p, true);		
		setupEntityContext(context);
		EntityOperation operation = new EntityReplaceOperation(p, entityService, queryEntity, entity, null, context, cmsState, requestId.getNext(), monitor);
		operation.perform();
	}

	public void modify(CMSPriority p, IEntity queryEntity, IEntity entity, EntityContext context) {
		logger.info("cms modify: {}, {}, {}", new Object[] { queryEntity, entity, context.getSourceIp() });
		
		checkServerState(p, true);		
		setupEntityContext(context);
		EntityOperation operation = new EntityModifyOperation(p, entityService, queryEntity, entity, null, context, cmsState, requestId.getNext(), monitor);
		operation.perform();
	}


	public void modifyField(CMSPriority p, IEntity queryEntity, IEntity entity, String fieldName, EntityContext context) {
		logger.info("cms modify: {}, {}, {}, {}",
				new Object[] { queryEntity, entity, fieldName, context.getSourceIp() });
		
		checkServerState(p, true);        
	    setupEntityContext(context);
        EntityOperation operation = new EntityModifyFieldOperation(p, entityService, queryEntity, entity, fieldName, context, cmsState, requestId.getNext(), monitor);
        operation.perform();
	}

	public void casModifyField(CMSPriority p, IEntity queryEntity, IEntity entity, String fieldName, Object oldValue,
			EntityContext context) {
		logger.info("cms modify: {}, {}, {}, {}, {}",
				new Object[] { queryEntity, entity, fieldName, oldValue, context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		EntityOperation operation = new EntityCasModifyFieldOperation(p, entityService, queryEntity, entity, fieldName, context, oldValue, cmsState, requestId.getNext(), monitor);
		operation.perform();
	}

	public void deleteField(CMSPriority p, IEntity queryEntity, String fieldName, EntityContext context) {
		logger.info("cms deleteField: {}, {}, {}",
				new Object[] { queryEntity, fieldName, context.getSourceIp() });
		
		checkServerState(p, true);        
		setupEntityContext(context);
		EntityOperation operation = new EntityDeleteFieldOperation(p, entityService, queryEntity, null, fieldName, context, cmsState, requestId.getNext(), monitor);
		operation.perform();
	}
	
	private void setupEntityContext(EntityContext context) {
		setMongoMetrics(context);
		if (context.getRegistration() == null) {
			context.setRegistration(getDalImplementation(context.getDal()));
		}
		context.setDbConfig(dbConfig);
	}
	
	private void setupQueryContext(QueryContext context) {
		if (context.getRegistration() == null) {
			context.setRegistration(getDalImplementation(context.getDal()));
		}
		context.setDbConfig(dbConfig);
		context.setSysAllowRegexFullScan(cmsConfig.allowRegExpFullScan());
	}

	/* **************************************************************************
	 * proxy methods for query services
	 * *************************************************************************
	 */
	public IQueryResult query(CMSPriority p, String query, QueryContext context) {
		logger.info("cms query: {}, {}", new Object[] { query, context.getSourceIP() });

		long current = System.currentTimeMillis();
		context.setStartProcessingTime(current);
		
		String id = requestId.getNext();
		context.setRequestId(id);
		
		setupQueryContext(context);

		if (cmsState.getState() == CMSState.State.overload) {
			context.setHighLoadLevel(1);
		} else if (cmsState.getState() == CMSState.State.critical) {
			context.setHighLoadLevel(2);
		}

		checkServerState(p, false);
		DBTimeCollector.reset();

		IQueryResult result = null;
		boolean isSuccess = true;
		try {
			result = queryService.query(query, context);
		} catch (RuntimeException re) {
			isSuccess = ExceptionHelper.handleRuntimeException(re);
			throw re;
		} finally {
			long total = System.currentTimeMillis() - current;
			context.setTotalTimeCost(total);
			context.setDbTimeCost(DBTimeCollector.getDBTimeCost());
			MonitorHelper.addReadRequest(monitor, (int) total, current, query, isSuccess);
			logger.info("[{}], totalTimeCost {}, dbTimeCost {}, isSuccess {}.",
					new Object[] { id, context.getTotalTimeCost(), context.getDbTimeCost(), isSuccess });
		}

		return result;
	}

    public Map<String, MetaClass> getQueryMetaClass(CMSPriority priority, String query, QueryContext context) {
        logger.info("cms query get metaclass: {}, {}", new Object[] { query, context.getSourceIP() });
        
        long start = System.currentTimeMillis();
        context.setStartProcessingTime(start);
        String id = requestId.getNext();
        context.setRequestId(id);
        checkServerState(priority, false);
        DBTimeCollector.reset();

        setupQueryContext(context);

        Map<String, MetaClass> metadatas = null;
        boolean isSuccess = true;
        try {
            metadatas = queryService.getQueryMetaClass(query, context);
        } catch (RuntimeException e) {
            isSuccess = false;
            throw e;
        } finally {
            long total = System.currentTimeMillis() - start;
            context.setTotalTimeCost(total);
            context.setDbTimeCost(DBTimeCollector.getDBTimeCost());
            logger.info("[{}], totalTimeCost {}, dbTimeCost {}, isSuccess {}.",
                    new Object[] { id, context.getTotalTimeCost(), context.getDbTimeCost(), isSuccess });
        }
        return metadatas;
    }

	/* **************************************************************************
	 * proxy methods for state event listener register/remove
	 * *************************************************************************
	 */
	public void registerEvent(IEventListener listener) {
		cmsState.registerEvent(listener);
	}

	public boolean removeEvent(IEventListener listener) {
		return cmsState.removeEvent(listener);
	}

	public static synchronized CMSServer getCMSServer() {
		if (cmsServer == null) {
			cmsServer = new CMSServer(new CMSConfig());
		}

		return cmsServer;
	}

	public static synchronized CMSServer getCMSServer(Map<String, String> configs) {
		if (cmsServer == null) {
			cmsServer = new CMSServer(new SimpleCMSConfig(configs));
		}

		return cmsServer;
	}

	public void setEntityOperationCallback(IEntityOperationCallback callback) {
		this.entityService.setCallback(callback);
	}

	/* **************************************************************************
	 * proxy methods for config services
	 * *************************************************************************
	 */
	public Map<String, Object> getCurrentConfigurations() {
		return dbConfig.loadAndGet();
	}

	public Set<String> getConfigNames() {
		return dbConfig.getConfigName();
	}

	public void config(Map<String, Object> configs) {
		logger.info("change configs parameters: " + configs.toString());
		dbConfig.updateConfig(configs);
	}

	public ConsistentPolicy parsePolicy(String policy) {
		String rp = policy;
		if (StringUtils.isNullOrEmpty(rp)) {
            rp = ConsistentPolicy.SECONDARY_PREFERRED.getName();
		}
		String writeConcern = (String) dbConfig.get(CMSDBConfig.SYS_WRITE_CONCERN);
		return ConsistentPolicy.parseString(rp, writeConcern);
	}

    /**
     * Returns the pagination mode specified by the given string.
     * Return configed default mode if the given mode is null.
     * Return null if the given string is not null, but not parsable 
     */
    public PaginationMode parsePaginationMode(String mode) {
        String m = mode;
        if (StringUtils.isNullOrEmpty(m)) {
            m = (String) dbConfig.get(CMSDBConfig.SYS_QUERY_PAGINATION_MODE);
        }
        for (PaginationMode pm : PaginationMode.values()) {
            if (pm.name().equalsIgnoreCase(m)) {
                return pm;
            }
        }
        return null;
    }
    
    public void pullField(CMSPriority p, IEntity queryEntity,
            IEntity entity, String fieldName, EntityContext context) {
        logger.info("cms deleteField: {}, {}, {}, {}",
                new Object[] { queryEntity, entity, fieldName, context.getSourceIp() });
        
        checkServerState(p, true);
        setupEntityContext(context);
        EntityOperation operation = new EntityPullFieldOperation(p, entityService, queryEntity, entity, fieldName, context, cmsState, requestId.getNext(), monitor);
        operation.perform();
    }

    private void checkServerState(CMSPriority priority, boolean write) {
        CMSStateHelper.checkServerState(cmsState, priority, write);
    }

    public int getThrottlingLevel() {
        return throttlingLevel;
    }

    public void setThrottlingLevel(int throttlingLevel) {
        this.throttlingLevel = throttlingLevel;
    }
    
    public MongoDataSource getDataSource() {
        return dataSource;
    }

}
