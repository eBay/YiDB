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

package com.ebay.cloud.cms.dal.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.IEntityFactory;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
/**
 * 
 * @author jianxu1
 * @history 
 * 
 * 2012/7/16 in branch service, commit need to interact with multi db collections
 * so I add setCollectionName
 */
public class PersistenceContext {
	
    public enum DBCollectionPolicy {
        Merged, SplitByMetadata
    }

    //record the db time cost in miliseconds
    public static class DBTimeCollector {
        private static final ThreadLocal < Integer > dbCostTime = 
            new ThreadLocal < Integer > () {
                    @Override protected Integer initialValue() {
                        return 0;
                }
            };
        private static final ThreadLocal<Integer> lastCost = new ThreadLocal<Integer>() {
            @Override protected Integer initialValue() {
                return 0;
            };
        };
            
        public static void reset() {
            dbCostTime.set(0);
            lastCost.set(0);
        }
        
        public static int getDBTimeCost() {
            return dbCostTime.get();
        }

        public static void addDBTimeCost(int value) {
            dbCostTime.set(dbCostTime.get() + value);
            lastCost.set(value);
        }

        public static int getLastTimeCost() {
            return lastCost.get();
        }
    }

    public static class CollectionFinder {
        public String getCollectionName(MetaClass metadata, DBCollectionPolicy collectionPolicy, String baseName) {
            if (metadata == null && collectionPolicy == DBCollectionPolicy.SplitByMetadata) {
                throw new IllegalArgumentException("Collection policy is SplitByMetadata but metaclass is not provided");
            }

            if (collectionPolicy == DBCollectionPolicy.SplitByMetadata) {
                StringBuilder sb = new StringBuilder(baseName);
                sb.append("_");
                sb.append(metadata.getName());
                return sb.toString();
            } else {
                return baseName;
            }
        }
    }

    private final IMetadataService   metaService;
    private final DBCollectionPolicy collectionPolicy;
    private ConsistentPolicy         consistentPolicy;
    private final String             collectionName;
    private final Set<String>        queryFields;
    private boolean                  fetchFieldProperties;
    private MongoDataSource          dataSource;
    private String                   path;
    private CMSDBConfig              dbConfig;
    private Registration             dalRegistration;
    private CollectionFinder         collectionFinder;
    private final Map<String, List<SearchCriteria>> additionalCriteria;

    public PersistenceContext(IMetadataService metaService, DBCollectionPolicy collectionPolicy,
            ConsistentPolicy consistPolicy, String collectionName) {
	    this.metaService = metaService;
        if (consistPolicy != null) {
            consistentPolicy = consistPolicy;
        } else {
            consistentPolicy = ConsistentPolicy.NEAREST;
        }
        this.collectionPolicy = collectionPolicy;
        this.collectionName = collectionName;
        this.queryFields = new HashSet<String>();
        this.collectionFinder = new CollectionFinder();
        this.additionalCriteria = new HashMap<String, List<SearchCriteria>>();
	}
    
    public PersistenceContext(IMetadataService metaService, DBCollectionPolicy collectionPolicy,
            ConsistentPolicy consistPolicy, String collectionName, Registration registration) {
        this(metaService, collectionPolicy, consistPolicy, collectionName);
        this.dalRegistration = registration;
    }
    
    public PersistenceContext(PersistenceContext other) {
        this.metaService = other.metaService;
        this.consistentPolicy = other.consistentPolicy;
        this.collectionPolicy = other.collectionPolicy;
        this.collectionName = other.collectionName;
        this.dataSource = other.dataSource;
        this.queryFields = new HashSet<String>(other.queryFields);
        this.dbConfig = other.dbConfig;
        this.collectionFinder = other.collectionFinder;
        this.additionalCriteria = new HashMap<String, List<SearchCriteria>>(other.additionalCriteria);
    }
	
	public IMetadataService getMetadataService(){
		return metaService;
	}

    public DBCollectionPolicy getCollectionPolicy() {
        return collectionPolicy;
    }
    
    public void setConsistentPolicy(ConsistentPolicy consistentPolicy) {
        this.consistentPolicy = consistentPolicy;
    }

    public ConsistentPolicy getConsistentPolicy() {
        return consistentPolicy;
    }

    public void setMongoDataSource(MongoDataSource ds) {
        this.dataSource = ds;
    }

    public MongoDataSource getMongoDataSource() {
        return this.dataSource;
    }
    
    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setCollectionFinder(CollectionFinder collectionFinder) {
        this.collectionFinder = collectionFinder;
    }

    public DB getDB() {
    	Mongo mongoInstance = dataSource.getMongoInstance();
		String dbName = metaService.getRepository().getRepositoryName();
		return mongoInstance.getDB(dbName);
    }

    public DBCollection getDBCollection(MetaClass metadata) {
		DB db = getDB();
		String collectionName = getDBCollectionName(metadata);
		DBCollection dbCol = db.getCollection(collectionName);
		dbCol.setReadPreference(consistentPolicy.getReadPreference());
        dbCol.setWriteConcern(consistentPolicy.getWriteConcern());
		return dbCol;
	}

	public String getDBCollectionName(MetaClass metadata) {
	    return collectionFinder.getCollectionName(metadata, collectionPolicy, collectionName);
    }

    public void addDBTimeCost(long deltaDbTimeMili){
		DBTimeCollector.addDBTimeCost((int)deltaDbTimeMili);
	}
    
    public int getLastTimeCost() {
        return DBTimeCollector.getLastTimeCost();
    }

    public final void addQueryField(String fields) {
        this.queryFields.add(fields);
    }

    public final void addQueryFields(Collection<String> fields) {
        this.queryFields.addAll(fields);
    }

    public final Collection<String> getQueryFields() {
        return Collections.unmodifiableCollection(queryFields);
    }

    public MetaClass getMetaClass(String metaType) {
		//check meta type exist		
		MetaClass metaClass = metaService.getMetaClass(metaType);
		CheckConditions.checkArgument(metaClass!=null,"MetaDataService does not have %s",metaType);
		return metaClass;
    }

	public CMSDBConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(CMSDBConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

    public IEntityFactory<?> getEntityFactory() {
        return dalRegistration.factory;
    }

    public boolean isFetchFieldProperties() {
        return fetchFieldProperties;
    }

    public void setFetchFieldProperties(boolean fetchFieldProperties) {
        this.fetchFieldProperties = fetchFieldProperties;
    }

    public Registration getRegistration() {
        return dalRegistration;
    }

    public void setRegistration(Registration registration) {
        this.dalRegistration = registration;
    }

    public Map<String, List<SearchCriteria>> getAdditionalCriteria() {
        return additionalCriteria;
    }

    public void setAdditionalCriteria(Map<String, List<SearchCriteria>> additionalCriteria) {
        this.additionalCriteria.clear();
        if (additionalCriteria != null) {
            this.additionalCriteria.putAll(additionalCriteria);
        }
    }

}
