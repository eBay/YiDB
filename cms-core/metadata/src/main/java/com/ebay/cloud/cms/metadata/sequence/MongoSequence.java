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

import com.ebay.cloud.cms.metadata.exception.MongoOperationException;
import com.ebay.cloud.cms.mongo.MongoOperand;
import com.ebay.cloud.cms.utils.SixtyTwoBase;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

public class MongoSequence implements ISequence {
    
    private final DBCollection coll;
    
    private final String key;
    
    public MongoSequence(Mongo mongo, String dbName, String collName, String key) {
        super();
        this.coll = mongo.getDB(dbName).getCollection(collName);
        this.key = key;
        
        this.coll.setWriteConcern(WriteConcern.SAFE);
    }
    
    private int getNextFromDB() {
        BasicDBObject q = new BasicDBObject();
        BasicDBObject exists = new BasicDBObject();
        exists.put(MongoOperand.exists, true);
        q.put(key, exists);
        
        BasicDBObject u = new BasicDBObject();
        BasicDBObject o = new BasicDBObject();
        o.put(key, 1);
        u.put(MongoOperand.inc, o);
        
        try {
            DBObject obj = coll.findAndModify(q, null, null, false, u, true, true);
            return (Integer)obj.get(key);
        } catch (MongoException e) {
            throw new MongoOperationException(e);
        }
    }
    
    public String getNext() {
        String v = SixtyTwoBase.tenBaseToSixtyTwo(getNextFromDB());
        char c = v.charAt(0);
        while ( c >= '0'  && c <= '9') {
            v = SixtyTwoBase.tenBaseToSixtyTwo(getNextFromDB());
            c = v.charAt(0);
        }
        
        return v;
    }
}
