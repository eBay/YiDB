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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class MongoUtils {
    public static void ensureIndex(DBCollection collection, String fieldName, boolean unique, boolean sparse) {
        BasicDBObject indexKey = new BasicDBObject();
        indexKey.put(fieldName, 1);
        
        BasicDBObject option = new BasicDBObject();
        if (sparse) {
            option.put("sparse", true);
        }
        if (unique) {
            option.put("unique", true);
        }
        
        collection.ensureIndex(indexKey, option);
    }
    
    public static boolean wrapperUpdate(DBCollection collection, DBObject query, DBObject update) {
        WriteResult wr = collection.update(query, update, false, true);
        Boolean updated = (Boolean)wr.getCachedLastError().get("updatedExisting"); 
        if (updated == null || !updated) {
            return false;
        }
            
        return true;
    }
}
