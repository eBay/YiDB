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

package com.ebay.cloud.cms.metadata;

import java.util.HashMap;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.lock.ICMSLock;
import com.ebay.cloud.cms.lock.mongo.MongoLockImpl;
import com.ebay.cloud.cms.metadata.mongo.MongoRepositoryServiceImpl;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

/**
 * Class to get RepositoryService for a datasource. 
 * 
 * MongoDataSource are identified by it's connections strings. Same connection string will get the same repository service.
 * Same connection string with different Mongo connection options will be treated as the same datasource and get the same repo service.
 * 
 * RepositoryServiceFactory will get configurations from the cms_sys_db.properties collection and create a new repository with those configs.
 * 
 *  
 * @author liubo
 *
 */
public class RepositoryServiceFactory {
    
    private static HashMap<MongoDataSource, IRepositoryService> repoServices = new HashMap<MongoDataSource, IRepositoryService>();
    
    private static synchronized ICMSLock createMetadataLock(Mongo mongo, CMSDBConfig dbConfig, String clientName) {
        String lockType = (String) dbConfig.get(CMSDBConfig.METADATA_LOCK_TYPE);
        
        if (lockType.equals("MongoLock")) {
            String lockName = (String) dbConfig.get(CMSDBConfig.METADATA_LOCK_NAME);
            
            int expireTime = (Integer) dbConfig.get(CMSDBConfig.MONGO_LOCK_EXPIRED_TIME);
            
            int renewPeriod = (Integer) dbConfig.get(CMSDBConfig.MONGO_LOCK_RENEW_PERIOD);
            
            return new MongoLockImpl(mongo, CMSConsts.SYS_DB, CMSConsts.LOCK_COLLECTION, lockName, clientName, expireTime, renewPeriod);
        }
        else {
            return null;
        }
    }
    
    //this method is used in some tests
    public static synchronized void clearRepositoryServiceCache() {
        repoServices.clear();
    }
    
    public static synchronized IRepositoryService createRepositoryService(MongoDataSource ds, String clientName) {
        return createRepositoryService(ds, clientName, WriteConcern.SAFE);
    }
    
    public static synchronized IRepositoryService createRepositoryService(MongoDataSource ds, String clientName, WriteConcern writeConcern) {
        IRepositoryService service = repoServices.get(ds);
        if (service == null) {
            CMSDBConfig dbConfig = new CMSDBConfig(ds);
            
            int cacheSize = (Integer) dbConfig.get(CMSDBConfig.REPOSITORY_CACHE_SIZE_KEY); 
            int expireSeconds = (Integer) dbConfig.get(CMSDBConfig.REPOSITORY_CACHE_EXPIRE_SECONDS_KEY); 
            
            int collectionCountCacheSize = (Integer) dbConfig.get(CMSDBConfig.COLLECTION_COUNT_CACHE_SIZE_KEY); 
            int collectionCountCacheExpiredTime = (Integer) dbConfig.get(CMSDBConfig.COLLECTION_COUNT_CACHE_EXPIRE_SECONDS_KEY); 
            
            ICMSLock lock = createMetadataLock(ds.getMongoInstance(), dbConfig, clientName);
            
            service = new MongoRepositoryServiceImpl(ds.getMongoInstance(), cacheSize, expireSeconds,
                    collectionCountCacheSize, collectionCountCacheExpiredTime, lock, writeConcern);
            repoServices.put(ds, service);
        }
        
        return service;
    }
}
