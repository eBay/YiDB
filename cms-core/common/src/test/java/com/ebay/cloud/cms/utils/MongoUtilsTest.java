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

package com.ebay.cloud.cms.utils;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

public class MongoUtilsTest extends CMSMongoTest {
    
    private static final String DB = "mongo_utils_test_db";
    private static final String COLL = "mongo_utils_test_coll";
    
    @Test
    public void testEnsureIndex() {
        MongoDataSource ds = getDataSource();
        Mongo mongo = ds.getMongoInstance();
        
        mongo.dropDatabase(DB);
        
        DBCollection coll = mongo.getDB(DB).getCollection(COLL);
        MongoUtils.ensureIndex(coll, "fieldName", false, false);
        
        MongoUtils.ensureIndex(coll, "fieldName", true, false);
        
        MongoUtils.ensureIndex(coll, "fieldName", true, true);
        
        MongoUtils.ensureIndex(coll, "fieldName", false, true);
    }
    
    @Test
    public void testWrapperUpdate() {
        MongoDataSource ds = getDataSource();
        Mongo mongo = ds.getMongoInstance();
        
        mongo.dropDatabase(DB);
        
        DBCollection coll = mongo.getDB(DB).getCollection(COLL);
        coll.setWriteConcern(WriteConcern.SAFE);
        
        BasicDBObject o1 = new BasicDBObject().append("name", "value");
        coll.insert(o1);
        
        BasicDBObject o2 = new BasicDBObject().append("name", "newValue");
        
        BasicDBObject update = new BasicDBObject().append("$set", 
                o2);
        Assert.assertTrue(MongoUtils.wrapperUpdate(coll, o1, update));
        
        Assert.assertEquals(1, coll.count(o2));
        
        Assert.assertFalse(MongoUtils.wrapperUpdate(coll, o1, update));
        
        
    }
}
