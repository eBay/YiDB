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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class CMSDBConfigTest extends CMSMongoTest {

    @Test
    public void test() throws IOException {
        MongoDataSource ds = new MongoDataSource(getConnectionString());

        Mongo mongo = ds.getMongoInstance();

        DBCollection propertiesCollection = mongo.getDB(CMSConsts.SYS_DB)
                .getCollection(CMSConsts.PROPERTIES_COLLECTION);

        propertiesCollection.drop();

        propertiesCollection.insert(new BasicDBObject().append("key1", "value1"));
        propertiesCollection.insert(new BasicDBObject().append("key2", 20));
        propertiesCollection.insert(new BasicDBObject().append("key4", 20l));

        CMSProperties cmsProperties = new CMSProperties(ds);
        Assert.assertEquals("value1", cmsProperties.get("key1"));
        Assert.assertEquals(Integer.valueOf(20), cmsProperties.get("key2"));
        Assert.assertNull(cmsProperties.get("key3"));

        Assert.assertNull(cmsProperties.get("key3"));

        Assert.assertNotNull(cmsProperties.get("key4"));

        // test db config
        CMSDBConfig dbConfig = new CMSDBConfig(ds);
        Assert.assertEquals("value1", dbConfig.get("key1"));
        Assert.assertEquals(20, dbConfig.get("key2"));
        Assert.assertEquals(1500, dbConfig.get(CMSDBConfig.MONGO_LOCK_RENEW_PERIOD));
        Assert.assertNull(dbConfig.get("non exist key"));
    }

    @Test
    public void configChange() {
        // case 0 : update existing key and insert new key
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        CMSDBConfig dbConfig = new CMSDBConfig(ds);
        Map<String, Object> currConfig = new HashMap<String, Object>(dbConfig.getCurrentConfiguration());
        Assert.assertTrue(currConfig.size() > 0);

        currConfig.put("value1", "new value for first key");
        currConfig.put("new-keys-for-change", "value for change of new key");

        dbConfig.updateConfig(new HashMap<String, Object>(currConfig));

        Map<String, Object> changedConfig = dbConfig.getCurrentConfiguration();
        Assert.assertEquals(currConfig.get("value1"), changedConfig.get("value1"));
        Assert.assertEquals(currConfig.get("new-keys-for-change"), changedConfig.get("new-keys-for-change"));

        // case 1 :
        // update empty should succeed
        dbConfig.updateConfig(new HashMap<String, Object>());
    }

    @Test
    public void config() {
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        CMSDBConfig dbConfig = new CMSDBConfig(ds);
        Map<String, Object> currConfig = new HashMap<String, Object>(dbConfig.getCurrentConfiguration());
        Assert.assertTrue(currConfig.size() > 0);

        currConfig.put(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN, "true");

        try {
            dbConfig.updateConfig(currConfig);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
    }

}
