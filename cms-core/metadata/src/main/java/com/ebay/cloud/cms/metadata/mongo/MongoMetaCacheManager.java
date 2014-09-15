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


package com.ebay.cloud.cms.metadata.mongo;

import java.util.List;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.utils.ExpirableCache;

public class MongoMetaCacheManager {
    private ExpirableCache<MetaClass> nameCache;
    private ExpirableCache<MetaClass> pluralNameCache;
    private ExpirableCache<Integer> countCache;
    
	public MongoMetaCacheManager(int maxCacheSize, int cacheExpiredTime,
			int collectionCountCacheSize, int collectionCountCacheExpiredTime) {
		this.nameCache = new ExpirableCache<MetaClass>(maxCacheSize, cacheExpiredTime);
		this.pluralNameCache = new ExpirableCache<MetaClass>(maxCacheSize, cacheExpiredTime);
		this.countCache = new ExpirableCache<Integer>(collectionCountCacheSize, collectionCountCacheExpiredTime);
	}

	/**
	 * className is either metaclass name or pluralName
	 * 
	 * @param className
	 * @return
	 */
	public MetaClass getMetaClassFromCache(String className) {
		MetaClass m = nameCache.getObject(className);
		if (m == null) {
			m = pluralNameCache.getObject(className);
		}
		return m;
	}
	
	public List<MetaClass> getMetaClassesFromCache() {
	    return this.nameCache.values();
	}
	
    /**
	 * 
	 * className is either metaclass name or pluralName
	 * 
	 * @param className
	 * @return
	 */
	public void deleteMetaClassFromCache(MetaClass m) {
		nameCache.deleteObject(m.getName());
		String pluralName = m.getpluralName();
		if (pluralName != null) {
			pluralNameCache.deleteObject(pluralName);
		}
	}
	
	public void addMetaClassToCache(MetaClass m) {
		nameCache.putObject(m.getName(), m);
		String pluralName = m.getpluralName();
		if (pluralName != null) {
			pluralNameCache.putObject(pluralName, m);
		}
	}

	public void addMetaClassToCache(MetaClass m,
			ExpirableCache<MetaClass> newNameCache,
			ExpirableCache<MetaClass> pluralNameCache) {
		newNameCache.putObject(m.getName(), m);
		String pluralName = m.getpluralName();
		if (pluralName != null) {
			pluralNameCache.putObject(pluralName, m);
		}
	}
	
	public void refreshCache(ExpirableCache<MetaClass> newNameCache,
			ExpirableCache<MetaClass> pluralNameCache){
        this.nameCache = newNameCache;
        this.pluralNameCache = pluralNameCache;
	}
	
	public Integer getCountFromCache(String dbCollectionName){
		return countCache.getObject(dbCollectionName);
	}
	
	public void putCountToCache(String dbCollectionName, Integer count){
		countCache.putObject(dbCollectionName, count);
	}
}
