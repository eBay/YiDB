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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ReadPreference;

public class CMSProperties {
    
    private Map<String, Object> cachedConfigurations;
    private MongoDataSource ds;
    
    public CMSProperties(MongoDataSource ds) {
        this.ds = ds;
        loadProperties(ds.getMongoInstance());
    }
    
    private DBCollection getPropertiesCollection(Mongo mongo) {
        return mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.PROPERTIES_COLLECTION);
    }
    
    private void loadProperties(Mongo mongo) {
        Map<String, Object> m = new ConcurrentHashMap<String, Object>();
        DBCollection collection = getPropertiesCollection(mongo);
        collection.setReadPreference(ReadPreference.primary());
        DBCursor cursor = collection.find();
        while (cursor.hasNext()) {
            BasicDBObject object = (BasicDBObject) cursor.next();
            String key = getKey(object);
            if (key != null) {
                m.put(key, object.get(key));
            }
        }

        cachedConfigurations  = m;
    }

    public Object get(String key) {
        return cachedConfigurations.get(key);
    }
    
    Map<String, Object> getCachedConfiguration() {
        return cachedConfigurations;
    }
    
    Map<String, Object> getLatestConfiguration() {
    	loadProperties(ds.getMongoInstance());
    	return cachedConfigurations;
    }

    public void updateConfig(Map<String, Object> configs) {
        DBCollection coll = getPropertiesCollection(ds.getMongoInstance());
        DBCursor cursor = coll.find();
        // update existing
        while (cursor.hasNext()) {
            BasicDBObject dbo = (BasicDBObject) cursor.next();
            String key = getKey(dbo);
            if (!configs.containsKey(key)) {
                continue;
            }

            BasicDBObject qObject = new BasicDBObject();
            BasicDBObject vObject = new BasicDBObject();
            qObject.append("_id", dbo.get("_id"));
            vObject.append(key, configs.get(key));

            coll.update(qObject, vObject);
            configs.remove(key);
        }

        // insert new config
        if (!configs.isEmpty()) {
            List<DBObject> list = new ArrayList<DBObject>();
            for (Entry<String, Object> entry : configs.entrySet()) {
                DBObject dbo = new BasicDBObject();
                dbo.put(entry.getKey(), entry.getValue());

                list.add(dbo);
            }
            coll.insert(list);
        }

        loadProperties(ds.getMongoInstance());
    }

    private String getKey(BasicDBObject dbo) {
        for (String key : dbo.keySet()) {
            if (!key.equals("_id")) {
                return key;
            }
        }
        return null;
    }
}
