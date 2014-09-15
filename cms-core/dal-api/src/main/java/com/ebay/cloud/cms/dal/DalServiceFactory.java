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

package com.ebay.cloud.cms.dal;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.mongo.MongoDataSource;

public class DalServiceFactory {

	private static ConcurrentHashMap<MongoDataSource, IPersistenceService> persistenceMap = 
	        new ConcurrentHashMap<MongoDataSource, IPersistenceService>();
	
	private static ConcurrentHashMap<MongoDataSource, ISearchService> searchMap =
	        new ConcurrentHashMap<MongoDataSource, ISearchService>();

	// registration of DAL implementation
    public static enum RegistrationEnum {
        hierarchy(0), flatten(1);
        private int index;
        private RegistrationEnum(int index) {
            this.index = index;
        }

        public static RegistrationEnum fromString(String id) {
            try {
                // proactively test if the given id is a integer
                Integer index = Integer.parseInt(id);
                return fromIndex(index);
            } catch (Exception e) {
                for (RegistrationEnum re : RegistrationEnum.values()) {
                    if (re.name().equals(id)) {
                        return re;
                    }
                }
            }
            return null;
        }

        public static RegistrationEnum fromIndex(int index) {
            for (RegistrationEnum re : RegistrationEnum.values()) {
                if (re.index == index) {
                    return re;
                }
            }
            return null;
        }
    }

	
    public static IPersistenceService getPersistenceService(MongoDataSource dataSource, List<Registration> property) {
		IPersistenceService persistenceService = persistenceMap.get(dataSource);
        if (persistenceService == null) {
			persistenceService = new PersistenceService(dataSource, property);
			persistenceMap.putIfAbsent(dataSource, persistenceService);
			persistenceService = persistenceMap.get(dataSource);
		}
		return persistenceService;
	}
	
	public static ISearchService getSearchService(MongoDataSource dataSource){
	    ISearchService searchService = searchMap.get(dataSource);
        if (searchService == null) {
            searchService = new SearchServiceImpl(dataSource);
            searchMap.putIfAbsent(dataSource, searchService);
            searchService = searchMap.get(dataSource);
        }
        return searchService;
    }

}
