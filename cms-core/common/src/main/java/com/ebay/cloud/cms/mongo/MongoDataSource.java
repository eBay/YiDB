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

package com.ebay.cloud.cms.mongo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;


/**
 * MongoDataSource to create mongo instance. 
 * 
 * Only one mongo instance will be created and returned for getMongoInstance per MongoDataSource. 
 * 
 * @author liubo
 *
 */
public class MongoDataSource {
    private List<ServerAddress> addrs;
//    private MongoClientOptions  mongoOptions;
    private MongoClient         mongo;

    public MongoDataSource(String servers) {
        this(servers, 20);
    }
    
    public MongoDataSource(String servers, int connectionsPerHost) {
        this(servers, connectionsPerHost, null, null);
    }
    
    public MongoDataSource(String servers, CMSDBConfig dbConfig) {
        this(servers, (Integer) dbConfig.get(CMSDBConfig.MONGO_CONNECTION_PER_HOST), ReadPreference.nearest(), dbConfig);
    }
    
    public MongoDataSource(String servers, int connectionsPerHost, ReadPreference readPreference, CMSDBConfig config) {
    	this.addrs = parseServerString(servers);
    	
    	Collections.sort(addrs, new Comparator<ServerAddress>() {
    		
    		@Override
    		public int compare(ServerAddress s1, ServerAddress s2) {
    			int result = s1.getHost().compareTo(s2.getHost());
    			if (result != 0) {
    				return result;
    			}
    			else {
    				return s1.getPort() - s2.getPort();
    			}
    		}
    		
    	});
    	
    	MongoClientOptions.Builder builder = MongoClientOptions.builder();
        builder.socketKeepAlive(false);
        builder.connectionsPerHost(connectionsPerHost);
        if (readPreference != null) {
            builder.readPreference(readPreference);
        }
        // set socket timeout
        if (config != null) {
            Integer socketTimeOut = (Integer) config.get(CMSDBConfig.MONGO_CONNECTION_SOCKET_TIMEOUT);
            builder.socketTimeout(socketTimeOut);
        }
        MongoClientOptions mongoOptions = builder.build();
        this.mongo = new MongoClient(addrs, mongoOptions);
    }

    public MongoClient getMongoInstance() {
        return this.mongo;
    }
    
    static List<ServerAddress> parseServerString(String servers) {
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(servers));
        ArrayList<ServerAddress> addrs = new ArrayList<ServerAddress>();
        String[] serverList = servers.split(",");
        for (String server : serverList) {
            String[] pair = server.split(":");
            try {
                if(pair.length == 1) {
                    addrs.add(new ServerAddress(pair[0].trim()));
                }
                else if (pair.length == 2) {
                    addrs.add(new ServerAddress(pair[0].trim(), Integer.valueOf(pair[1].trim())));
                }
                else {
                    throw new RuntimeException("Bad server string: " + servers);
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException("parse server string error", e);
            }
        }
        
        return addrs;
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        for (ServerAddress a : addrs) {
            hash += a.getHost().hashCode();
            hash += a.getPort();
        }
        
        if (hash == 0) {
            return "".hashCode();
        }
        
        return hash;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MongoDataSource)) {
            return false;
        }
        MongoDataSource other = (MongoDataSource)o;
        if (this.addrs.size() != other.addrs.size()) {
            return false;
        }
        
        for(int i = 0; i < this.addrs.size(); i++) {
            if (!this.addrs.get(i).equals(other.addrs.get(i))) {
                return false;
            }
        }
        
        return true;
        
    }
}
