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

package com.ebay.cloud.cms.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * 
 * @author liubo
 * 
 */
@SuppressWarnings("rawtypes")
public class CMSDBConfig {
    private static final Logger logger = LoggerFactory.getLogger(CMSDBConfig.class);
    
    public static final String               REPOSITORY_CACHE_SIZE_KEY           = "RepositoryCacheSize";
    public static final String               REPOSITORY_CACHE_EXPIRE_SECONDS_KEY = "RepositoryCacheExpireSeconds";
    
    public static final String               COLLECTION_COUNT_CACHE_SIZE_KEY           = "CollectionCountCacheSize";
    public static final String               COLLECTION_COUNT_CACHE_EXPIRE_SECONDS_KEY = "CollectionCountCacheExpireSeconds";

    // metadata lock related
    public static final String               METADATA_LOCK_TYPE                  = "MetadataLockType";
    public static final String               METADATA_LOCK_NAME                  = "MetadataLockName";
    public static final String               MONGO_LOCK_EXPIRED_TIME             = "MongoLockExpiredTime";
    public static final String               MONGO_LOCK_RENEW_PERIOD             = "MongoLockRenewPeriod";

    public static final String               MONGO_CONNECTION_PER_HOST           = "MongoConnectionsPerHost";
    public static final String               MONGO_CONNECTION_POOL_NUM           = "MongoConnectionPoolNum";
    public static final String               MONGO_CONNECTION_SOCKET_TIMEOUT     = "MongoConnectionSocketTimeout";
    
    public static final String               HISTORY_COLLECTION_SIZE             = "HistoryCollectionSize";
    public static final String               SUB_BRANCH_COLLECTION_SIZE          = "SubBranchHistoryCollectionSize";

    // monitor related configurations
    public static final String               TOP_QUERY_SNAPTSHOT_PERIOD          = "TopQuerySnapshotPeroid";
    public static final String               TOP_QUERY_METRIC_SIZE               = "TopQueryMetricSize";
    public static final String               TIME_WINDOW_SNAPSHOT_PERIOD         = "TimeWindowSnapshotPeroid";
    public static final String               TIME_WINDOW_BUCKET_IN_SECONDS       = "TimeWindowBucketInSeconds";
    public static final String               MONGO_METRIC_SNAPSHOT_PERIOD        = "MongoMetricSnapshotPeriod";
    public static final String               MONGO_METRIC_LISTDB_WAIT            = "MongoMetricListDBPeriod";
    public static final String               METRIC_MAX_VALUE                    = "MetricMaxValue";
    public static final String               SLOT_COUNT                          = "SlotCount";
    
    public static final String               THROTTLING_CHECK_ENABLED            = "SystemThrottlingCheckEnabled";
    public static final String               THROTTLING_EXPRESSION               = "SystemThrottlingExpression";
    public static final String               THROTTLING_CHECK_PERIOD             = "SystemThrottlingCheckPeriod";
    public static final String               RATE_LIMIT_CHECK_ENABLED            = "RateLimitCheckEnabled";
    
    public static final String               HEAP_MEMORY_USAGE_CHECK_PERIOD      = "HeapMemoryUsageCheckPeriod";

    public static final String               HEALTHY_EXPRESSION                  = "HealthyExpression";
    public static final String               HEALTHY_CHECK_PERIOD                = "HealthyCheckPeriod";

    public static final String               SYS_SHOW_STACK_TRACE_PROP           = "SysShowStackTrace";
    public static final String               SYS_ALLOW_FULL_TABLE_SCAN           = "SysAllowFullTableScan";
    public static final String               SYS_ALLOW_METADATA_DELETE           = "MetadataDelete";
    public static final String               SYS_ALLOW_REPOSITORY_DELETE         = "SysAllowRepositoryDelete";

    public static final String               SYS_NOTIFICATION_READ_THREADS_NUM   = "SysNotificationReadThreadsNum";
    public static final String               SYS_NOTIFICATION_WRITE_THREADS_NUM  = "SysNotificationWriteThreadsNum";
    public static final String               JS_EXPRESSION_TIMEOUT_IN_SECONDS    = "JSExpressionTimeoutInSeconds";
    
    public static final String               SYS_WRITE_CONCERN                   = "SysWriteConcern";
    
    //
    // 0 - hierarchy, 1 - flatten
    //
    public static final String               SYS_DAL_DEFAULT_IMPLEMENTATION         = "SysDalDefaultImplementation";
    public static final String               SYS_DAL_MIGRATION_DUAL_WRITE           = "SysDalMigrationDualWrite";
    
    public static final String               SYS_LIMIT_JOINED_COLLECTIONS           = "SysLimitJoinedCollections";
    public static final String               SYS_LIMIT_DOCUMENTS_MONGO_QUERY        = "SysLimitDocumentsMongoQuery";
    public static final String               SYS_LIMIT_MEMORY_MONGO_QUERY           = "SysLimitMemoryMongoQuery";
    public static final String               SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO           = "SysLimitMemoryMongoQuerySamplingRatio";

    public static final String               SYS_QUERY_PAGINATION_MODE              = "SysQueryPaginationMode";
    
    public static final String               DEFAULT_SYS_LIMIT_MAX_REPOSITORY_SIZE  = "DefaultSysLimitMaxRepositorySize";
    public static final String               DEFAULT_SYS_LIMIT_MAX_INDEXES_NUM      = "DefaultSysLimitMaxIndexesNum";
    public static final String               DEFAULT_SYS_LIMIT_MAX_INDEXED_ARRAY_SIZE   = "DefaultSysLimitMaxIndexedArraySize";
    public static final String               DEFAULT_SYS_LIMIT_MAX_DOCUMENT_SIZE    = "DefaultSysLimitMaxDocumentSize";

    private static final Map<String, Object> map;
    private static final Map<String, Class> configType;
    
    static {
        Map<String, Object> m = new HashMap<String, Object>();
        
        m.put(REPOSITORY_CACHE_SIZE_KEY, 5000);
        m.put(REPOSITORY_CACHE_EXPIRE_SECONDS_KEY, 3600);
        m.put(COLLECTION_COUNT_CACHE_SIZE_KEY, 300);
        m.put(COLLECTION_COUNT_CACHE_EXPIRE_SECONDS_KEY, 60);
        m.put(METADATA_LOCK_TYPE, "MongoLock");
        m.put(METADATA_LOCK_NAME, "MetadataLock");
        m.put(MONGO_LOCK_EXPIRED_TIME, 3000);
        m.put(MONGO_LOCK_RENEW_PERIOD, 1500);
        m.put(MONGO_CONNECTION_PER_HOST, 50);
        m.put(MONGO_CONNECTION_POOL_NUM, 5);
        m.put(MONGO_CONNECTION_SOCKET_TIMEOUT, 5 * 60 * 1000);
        m.put(HISTORY_COLLECTION_SIZE, 1024 * 1024 * 10);
        m.put(SUB_BRANCH_COLLECTION_SIZE, 1024 * 1024 * 10);

        m.put(TOP_QUERY_SNAPTSHOT_PERIOD, 10 * 1000);
        m.put(TOP_QUERY_METRIC_SIZE, 10);
        m.put(TIME_WINDOW_BUCKET_IN_SECONDS, 3600l);
        m.put(TIME_WINDOW_SNAPSHOT_PERIOD, 10 * 1000);
        m.put(MONGO_METRIC_SNAPSHOT_PERIOD, 6 * 1000);
        m.put(MONGO_METRIC_LISTDB_WAIT, 300 * 1000);
        
        m.put(THROTTLING_CHECK_ENABLED, true);
        m.put(THROTTLING_EXPRESSION, getJsExpressionFromFile("/throttling.js"));
        m.put(THROTTLING_CHECK_PERIOD, 15 * 1000);
        m.put(RATE_LIMIT_CHECK_ENABLED, true);
        m.put(HEAP_MEMORY_USAGE_CHECK_PERIOD, 15 * 1000);
        
        
        m.put(METRIC_MAX_VALUE, 60 * 1000);
        m.put(SLOT_COUNT, 1000);
        m.put(JS_EXPRESSION_TIMEOUT_IN_SECONDS, 180L);

        m.put(HEALTHY_EXPRESSION, getJsExpressionFromFile("/healthy.js"));
        m.put(HEALTHY_CHECK_PERIOD, 6 * 1000);

        m.put(SYS_SHOW_STACK_TRACE_PROP, true);
        m.put(SYS_ALLOW_FULL_TABLE_SCAN, true);
        m.put(SYS_ALLOW_METADATA_DELETE, false);
        m.put(SYS_ALLOW_REPOSITORY_DELETE, false);
        m.put(SYS_WRITE_CONCERN, "ACKNOWLEDGED");
        m.put(SYS_DAL_DEFAULT_IMPLEMENTATION, 0);
        m.put(SYS_DAL_MIGRATION_DUAL_WRITE, true);
        m.put(SYS_LIMIT_JOINED_COLLECTIONS, 20);
        m.put(SYS_LIMIT_DOCUMENTS_MONGO_QUERY, 10000);
        m.put(SYS_LIMIT_MEMORY_MONGO_QUERY, 1024*100*10000L);
        m.put(SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO, 0.02D);
        m.put(SYS_QUERY_PAGINATION_MODE, "ID_BASED");
        m.put(DEFAULT_SYS_LIMIT_MAX_REPOSITORY_SIZE, 5*1024*1024*1024L);
        m.put(DEFAULT_SYS_LIMIT_MAX_INDEXES_NUM, 15);
        m.put(DEFAULT_SYS_LIMIT_MAX_INDEXED_ARRAY_SIZE, 2000);
        m.put(DEFAULT_SYS_LIMIT_MAX_DOCUMENT_SIZE, 512*1024L);
        
        map = Collections.unmodifiableMap(m);

        configType = new HashMap<String, Class>();
        configType.put(REPOSITORY_CACHE_SIZE_KEY, Integer.class);
        configType.put(REPOSITORY_CACHE_EXPIRE_SECONDS_KEY, Integer.class);
        configType.put(COLLECTION_COUNT_CACHE_SIZE_KEY, Integer.class);
        configType.put(COLLECTION_COUNT_CACHE_EXPIRE_SECONDS_KEY, Integer.class);
        configType.put(METADATA_LOCK_TYPE, String.class);
        configType.put(METADATA_LOCK_NAME, String.class);
        configType.put(MONGO_LOCK_EXPIRED_TIME, Integer.class);
        configType.put(MONGO_LOCK_RENEW_PERIOD, Integer.class);
        configType.put(MONGO_CONNECTION_PER_HOST, Integer.class);
        configType.put(MONGO_CONNECTION_POOL_NUM, Integer.class);
        configType.put(MONGO_CONNECTION_SOCKET_TIMEOUT, Integer.class);
        configType.put(HISTORY_COLLECTION_SIZE, Integer.class);
        configType.put(SUB_BRANCH_COLLECTION_SIZE, Integer.class);

        configType.put(TOP_QUERY_SNAPTSHOT_PERIOD, Integer.class);
        configType.put(TOP_QUERY_METRIC_SIZE, Integer.class);
        configType.put(TIME_WINDOW_BUCKET_IN_SECONDS, Long.class);
        configType.put(TIME_WINDOW_SNAPSHOT_PERIOD, Integer.class);
        configType.put(MONGO_METRIC_SNAPSHOT_PERIOD, Integer.class);
        configType.put(MONGO_METRIC_LISTDB_WAIT, Integer.class);
        
        configType.put(THROTTLING_CHECK_ENABLED, Boolean.class);
        configType.put(THROTTLING_EXPRESSION, String.class);
        configType.put(THROTTLING_CHECK_PERIOD, Integer.class);
        configType.put(RATE_LIMIT_CHECK_ENABLED, Boolean.class);
        configType.put(HEAP_MEMORY_USAGE_CHECK_PERIOD, Integer.class);
        
        configType.put(METRIC_MAX_VALUE, Integer.class);
        configType.put(SLOT_COUNT, Integer.class);
        configType.put(JS_EXPRESSION_TIMEOUT_IN_SECONDS, Long.class);

        configType.put(HEALTHY_EXPRESSION, String.class);
        configType.put(HEALTHY_CHECK_PERIOD, Integer.class);
        
        configType.put(SYS_SHOW_STACK_TRACE_PROP, Boolean.class);
        configType.put(SYS_ALLOW_FULL_TABLE_SCAN, Boolean.class);
        configType.put(SYS_ALLOW_METADATA_DELETE, Boolean.class);
        configType.put(SYS_ALLOW_REPOSITORY_DELETE, Boolean.class);
        configType.put(SYS_WRITE_CONCERN, String.class);
        configType.put(SYS_LIMIT_JOINED_COLLECTIONS, Integer.class);
        configType.put(SYS_LIMIT_DOCUMENTS_MONGO_QUERY, Integer.class);
        configType.put(SYS_LIMIT_MEMORY_MONGO_QUERY, Long.class);
        configType.put(SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO, Double.class);
        configType.put(SYS_QUERY_PAGINATION_MODE, String.class);
        configType.put(DEFAULT_SYS_LIMIT_MAX_REPOSITORY_SIZE, Long.class);
        configType.put(DEFAULT_SYS_LIMIT_MAX_INDEXES_NUM, Integer.class);
        configType.put(DEFAULT_SYS_LIMIT_MAX_INDEXED_ARRAY_SIZE, Integer.class);
        configType.put(DEFAULT_SYS_LIMIT_MAX_DOCUMENT_SIZE, Long.class);
    }

    private static String getJsExpressionFromFile(String exprFile) {
        InputStream is = CMSDBConfig.class.getResourceAsStream(exprFile);

        InputStreamReader isReader = null;
        Reader reader = null;
        try {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            isReader = new InputStreamReader(is, "UTF-8");
            reader = new BufferedReader(isReader);
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Fail to load healthy expression, this might means healthy.js not found in class path, or it contains invalid js expression!", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // ignore
                    logger.error("fail to close default healty expression stream!", e);
                }
            }
            if (isReader != null) {
                try {
                    isReader.close();
                } catch (Exception e) {
                    // ignore
                    logger.error("fail to close default healty expression stream!", e);
                }
            }
            try {
                is.close();
            } catch (Exception e) {
                // ignore
                logger.error("fail to close default healty expression stream!", e);
            }
        }
    }

    private CMSProperties                    dbProperties;
    private volatile Map<String, Object>     currentConfiguration;
    
    public CMSDBConfig(MongoDataSource ds) {
        this.dbProperties = new CMSProperties(ds);

        loadConfig();
    }
    
    private void loadConfig() {
        Map<String, Object> m = new HashMap<String, Object>(map);
        Map<String, Object> cachedDbConfig = dbProperties.getCachedConfiguration();
        for (Entry<String, Object> entry : cachedDbConfig.entrySet()) {
             m.put(entry.getKey(), entry.getValue());
        }
        
        currentConfiguration = m;
    }
    

    private void loadLatestConfig() {
        Map<String, Object> m = new HashMap<String, Object>(map);
        Map<String, Object> cachedDbConfig = dbProperties.getLatestConfiguration();
        for (Entry<String, Object> entry : cachedDbConfig.entrySet()) {
            // only loaded configured property??
//            if (map.containsKey(entry.getKey())) {
                m.put(entry.getKey(), entry.getValue());
//            }
        }

        currentConfiguration = m;
    }

    public Object get(String key) {
        return currentConfiguration.get(key);
    }

    public Set<String> getConfigName() {
        return map.keySet();
    }

    public Map<String, Object> loadAndGet() {
        loadLatestConfig();
        return getCurrentConfiguration();
    }

    public Map<String, Object> getCurrentConfiguration() {
        return Collections.unmodifiableMap(currentConfiguration);
    }

    public void updateConfig(Map<String, Object> configs) {
        validate(configs);
        
        dbProperties.updateConfig(configs);

        loadConfig();
    }

    private void validate(Map<String, Object> configs) {
        for (Entry<String, Object> entry : configs.entrySet()) {
            String entryKey = entry.getKey();
            if (configType.containsKey(entry.getKey())) {
                Class entryType = configType.get(entry.getKey());
                Object entryValue = entry.getValue();
                Object numberValue = null;
                if (Number.class.isAssignableFrom(entryType)) {
                    CheckConditions.checkArgument(entryValue instanceof Number,
                            "Configuration value must be instance of %s for configuration item %s!", entryType.getName(),
                            entryKey);
                    if (entryType.equals(Long.class)) {
                        numberValue = ((Number)entryValue).longValue();
                    } else if (entryType.equals(Double.class)) {
                        numberValue = ((Number)entryValue).doubleValue();
                    } else {
                        numberValue = ((Number)entryValue).intValue();
                    }
                    CheckConditions.checkArgument(numberValue.toString().equals(entryValue.toString()),
                            "Configuration value must be instance of %s for configuration item %s!", entryType.getName(),
                            entryKey);
                    CheckConditions.checkArgument(!(numberValue.toString().startsWith("-")), "Configuration value must be positive for configuration item %s!", entryKey);
                    if (entryKey.equals(SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO)) {
                        Double ratio = ((Number)entryValue).doubleValue();
                        CheckConditions.checkArgument(ratio < 1, "Configuration value must be less than 1 for configuration item %s!", entryKey);
                    }
                } else {
                    CheckConditions.checkArgument(entryType.isInstance(entryValue),
                        "Configuration value must be instance of %s for configuration item %s!", entryType.getName(),
                        entryKey);
                }
            }
        }
    }

}
