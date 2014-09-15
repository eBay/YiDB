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


/**
 * 
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

package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.ebay.cloud.cms.expression.factory.DaemonThreadFactory;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.mongodb.CommandResult;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ReplicaSetStatus;
import com.mongodb.ServerAddress;

/**
 * Metric for mongo
 * 
 * @author liasu
 * 
 */
public class MongoMetric implements IMonitorMetric {
    private static final String NOT_FOUND = "not found!";
    private final int period;
    private final int listWaitPeroid;
    private final ExecutorService executor;
    private final MongoDataSource dataSource;

    private volatile Map<String, Object> mongoStatus = new TreeMap<String, Object>();

    public MongoMetric(MongoDataSource dataSource, int period, int waitLimit) {
        this.dataSource = dataSource;
        this.period = period;
        this.listWaitPeroid = waitLimit;
        executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.getInstance());
        snapshot();
    }

    private void snapshot() {
        MongoClient client = dataSource.getMongoInstance();

        Map<String, Object> status = new TreeMap<String, Object>();
        // update driver
        status.put(MetricConstants.MONGO_DRIVER_VERSION, client.getVersion());
        // update status
        ReplicaSetStatus rss = client.getReplicaSetStatus();
        
        String master = NOT_FOUND;
        if (rss != null) {
            status.put(MetricConstants.REPL_STATUS, rss.toString());
            ServerAddress masterServer = rss.getMaster();
            if (masterServer != null) {
                master = masterServer.getHost();
            }
        } else {
            status.put(MetricConstants.REPL_STATUS, "no repl set found!");
        }
        // update mongo cluster master
        status.put(MetricConstants.REPL_MASTER, master);
        // list mongo databases
        Map<String, Object> databaseSizeMap = listDatabases(client);
        String databases = StringUtils.join(databaseSizeMap.keySet(), ',');
        status.put(MetricConstants.REPL_DATABASES, databases);
        status.put(MetricConstants.MONGO_DB_SIZE, databaseSizeMap);

        mongoStatus = status;
    }

    private Map<String, Object> listDatabases(final MongoClient client) {
        try {
            Future<Map<String, Object>> future = executor.submit(new Callable<Map<String, Object>>() {
                @Override
                public Map<String, Object> call() {
                	Map<String, Object> resultMap = new HashMap<String, Object>();
                    List<String> databaseNames = client.getDatabaseNames();
                    for (String databaseName : databaseNames) {
                    	DB db = client.getDB(databaseName);
                    	if (db != null) {
                    		CommandResult cr = db.getStats();
                    		if (cr != null) {
                    			Object dataSize = cr.get("dataSize");
                    			resultMap.put(databaseName, dataSize);
                    		}
                    	}
                    }
                    return resultMap;
                }
            });
            return future.get(listWaitPeroid, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public String getName() {
        return MetricConstants.MONGO_METRIC;
    }

    @Override
    public int getPeriod() {
        return period;
    }

    @Override
    public void snapshot(long timestamp) {
        snapshot();
    }

    @Override
    public Map<String, Object> output() {
        return mongoStatus;
    }

}
