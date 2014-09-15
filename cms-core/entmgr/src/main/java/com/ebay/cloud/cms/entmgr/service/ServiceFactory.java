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

package com.ebay.cloud.cms.entmgr.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.BranchServiceImpl;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.entity.impl.EntityServiceImpl;
import com.ebay.cloud.cms.entmgr.history.IEntityHistoryService;
import com.ebay.cloud.cms.entmgr.history.impl.HistoryServiceImpl;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.WriteConcern;

/**
 * 
 * @author jianxu1
 * @date   2012/5/23
 * 
 * 
 * @history
 *
 */
public class ServiceFactory {
    
    private ServiceFactory() {
        
    }
    
	private static ConcurrentHashMap<MongoDataSource, IPersistenceService> persistenceServiceMap 
		= new ConcurrentHashMap<MongoDataSource, IPersistenceService>();
	private static ConcurrentHashMap<MongoDataSource, ISearchService> searchServiceMap
		= new ConcurrentHashMap<MongoDataSource, ISearchService>();
	private static ConcurrentHashMap<MongoDataSource, IEntityHistoryService> historyServiceMap 
		= new ConcurrentHashMap<MongoDataSource, IEntityHistoryService>();
	private static ConcurrentHashMap<MongoDataSource, IBranchService> branchServiceMap 
		= new ConcurrentHashMap<MongoDataSource, IBranchService>();
	private static ConcurrentHashMap<MongoDataSource, IEntityService> entityServiceMap 
		= new ConcurrentHashMap<MongoDataSource, IEntityService>();

    // for test use
    public static void clearServiceCaches() {
        persistenceServiceMap.clear();
        searchServiceMap.clear();
        historyServiceMap.clear();
        branchServiceMap.clear();
        entityServiceMap.clear();
    }

    public static IPersistenceService getPersistenceService(MongoDataSource ds, List<PersistenceService.Registration> implementations) {
        CheckConditions.checkNotNull(ds);
		IPersistenceService service = persistenceServiceMap.get(ds);
		if(service == null){
			service = new PersistenceService(ds, implementations);
			persistenceServiceMap.putIfAbsent(ds, service);
			service = persistenceServiceMap.get(ds);
		}
		return service;
	}
	
	public static ISearchService getSearchService(MongoDataSource ds){
	    CheckConditions.checkNotNull(ds);
		ISearchService service = searchServiceMap.get(ds);
		if(service == null){
			service = new SearchServiceImpl(ds);
			searchServiceMap.putIfAbsent(ds, service);
			service = searchServiceMap.get(ds);
		}
		return service;
	}

    public static IBranchService getBranchService(MongoDataSource ds, List<PersistenceService.Registration> implementations){
        return getBranchService(ds, WriteConcern.SAFE, implementations);
    }
    
	public static IBranchService getBranchService(MongoDataSource ds, WriteConcern writeConcern, List<PersistenceService.Registration> implementations){
	    CheckConditions.checkNotNull(ds);
		IBranchService service = branchServiceMap.get(ds);
		if(service == null){
			IRepositoryService repositoryService  = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer", writeConcern);
			IPersistenceService persistenceService = getPersistenceService(ds, implementations);
			ISearchService searchService = getSearchService(ds);

            service = new BranchServiceImpl(ds, repositoryService, persistenceService, searchService);
			branchServiceMap.putIfAbsent(ds, service);
			service = branchServiceMap.get(ds);
		}
		return service;
	}

    public static IEntityHistoryService getHistoryService(CMSDBConfig dbConfig, MongoDataSource ds, List<PersistenceService.Registration> implementations){
        CheckConditions.checkNotNull(ds);
        IEntityHistoryService service = historyServiceMap.get(ds);
        if(service == null){
            IRepositoryService repositoryService  = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
            IPersistenceService persistenceService = getPersistenceService(ds, implementations);
            ISearchService searchService = getSearchService(ds);
            service = new HistoryServiceImpl(dbConfig, repositoryService, persistenceService, searchService);
            historyServiceMap.putIfAbsent(ds, service);
            service = historyServiceMap.get(ds);
            
            repositoryService.setHistoryService(service);
        }
        return service;
    }
	
    public static IEntityService getEntityService(MongoDataSource ds, IRepositoryService repositoryService, List<PersistenceService.Registration> implementations){
        return getEntityService(ds, repositoryService, WriteConcern.SAFE, implementations);
    }
    
	public static IEntityService getEntityService(MongoDataSource ds, IRepositoryService repositoryService, WriteConcern writeConcern, List<PersistenceService.Registration> implementations){
	    CheckConditions.checkNotNull(ds);
		IEntityService service = entityServiceMap.get(ds);
		if(service == null){
			IPersistenceService persistenceService = getPersistenceService(ds, implementations);
			ISearchService searchService = getSearchService(ds);
			IBranchService branchService = getBranchService(ds, writeConcern, implementations);
			service = new EntityServiceImpl(repositoryService,
											persistenceService,
											branchService,
											searchService);
			entityServiceMap.putIfAbsent(ds, service);
			service = entityServiceMap.get(ds);
		}
		return service;
	}
}
