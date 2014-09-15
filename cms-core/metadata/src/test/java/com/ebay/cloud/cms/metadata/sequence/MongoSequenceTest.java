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

package com.ebay.cloud.cms.metadata.sequence;

import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.Mongo;

public class MongoSequenceTest extends CMSMongoTest {
    
    public static final String db = "sequence_test_db";
    public static final String coll = "sequence";
    public static final String key = "seq";
    
    @Before
    public void setup() {
        Mongo mongo = getDataSource().getMongoInstance();
        mongo.dropDatabase(db);
    }
    
    @Test
    public void testSequenceCollCreation_1() {
        MongoSequence s = new MongoSequence(getDataSource().getMongoInstance(), db, coll, key);
        MongoSequence s1 = new MongoSequence(getDataSource().getMongoInstance(), db, coll, key);
        
        Assert.assertEquals("A", s.getNext());
        Assert.assertEquals("B", s1.getNext());
        Assert.assertEquals("C", s.getNext());
        Assert.assertEquals("D", s1.getNext());
    }
    
    @Test
    public void testSequence() {
        MongoSequence s = new MongoSequence(getDataSource().getMongoInstance(), db, coll, key);

        HashSet<String> set = new HashSet<String>();
        for(int i = 0; i < 1000; i++) {
            String str = s.getNext();
            Assert.assertFalse(set.contains(str));
            
            set.add(str);
        }
    }
}
