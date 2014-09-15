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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class MongoExecutor {
    
    private static Logger logger = LoggerFactory.getLogger(MongoExecutor.class);
    
    private static void logMongoAction(PersistenceContext context,                                
                                String operName,
                                long start,            
                                DBCollection dbCollection,
                                DBObject queryObject,
                                DBObject bodyObject,
                                SearchOption option,
                                Integer queryResultSize, String statusMsg){
        long end = System.currentTimeMillis();
        long cost = end - start;  //mili seconds
        
        //recording the time cost in mongo DB
        context.addDBTimeCost(cost);
        
        if (logger.isDebugEnabled()) {
        
            String costStr = Long.toString(cost);
            String queryObjectStr = "{}";
            if(queryObject != null){
                queryObjectStr = queryObject.toString();
            }
            String bodyObjectStr = "{}";
            if(bodyObject != null){
                bodyObjectStr = bodyObject.toString();
            }
            String sortObjectStr = "{}";
            if (option != null && option.hasSort()) {
                sortObjectStr = option.getSort().toString();
            }
            StringBuilder optionBuilder = new StringBuilder("{");
            if (option != null && (option.hasSkip() || option.hasLimit())) {
                optionBuilder.append("\"skip\" : ");
                optionBuilder.append(option.getSkip());                
                optionBuilder.append(", \"limit\" : ");
                optionBuilder.append(option.getLimit());
            }
            optionBuilder.append("}");
            
            StringBuilder readPref = new StringBuilder("{");
            StringBuilder writeCon = new StringBuilder("{");
            String dbName = "";
            String[] args;
            if (dbCollection != null) {
                readPref.append(dbCollection.getReadPreference().getName());
                writeCon.append(dbCollection.getWriteConcern().toString());
                dbName = dbCollection.getFullName();
            }
            readPref.append("}");
            writeCon.append("}");
            
            StringBuilder queryResult = new StringBuilder();
            if (queryResultSize != null) {
                queryResult.append("queryResultSize=").append(queryResultSize);
            }
            
            args = new String[] {operName, statusMsg, costStr, dbName, queryObjectStr, bodyObjectStr, sortObjectStr, optionBuilder.toString(), readPref.toString(), writeCon.toString(), queryResult.toString()};
            logger.debug("operation={}|status={}|cost={}ms|collection={}|query={}|body={}|sort={}|option={}|readPrefernce={}|writeConcern={}|{}",args);
        }
    }

    private static void handleMongoException(Throwable t) {
        try {
            throw t;
        } catch(CmsDalException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (MongoException.DuplicateKey ex1) {
            throw new CmsDalException(DalErrCodeEnum.MONGO_EXCEPTION_DUPLICATE, ex1.getLocalizedMessage(), ex1);
        } catch (MongoException.Network ex2) {
            throw new CmsDalException(DalErrCodeEnum.MONGO_EXCEPTION_NETWORK, ex2.getLocalizedMessage(), ex2);
        } catch (MongoException.CursorNotFound ex3) {
            throw new CmsDalException(DalErrCodeEnum.MONGO_EXCEPTION_CURSORNOTFOUND, ex3.getLocalizedMessage(), ex3);
        } catch (Throwable e) {
            String msg = t.getMessage();
            throw new CmsDalException(DalErrCodeEnum.MONGO_EXCEPTION_UNKNOWN, msg, t);
        }
    }
    
    public static long count(PersistenceContext context, MetaClass metadata, DBObject queryObject){
        long start = System.currentTimeMillis();
        long countResult = 0;
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try{
            countResult = dbCollection.count(queryObject);
        }catch(Throwable t){
            msg = t.getMessage();
            handleMongoException(t);
        }finally{
            logMongoAction(context, "count", start, dbCollection, queryObject, null, null, null, msg);
        }
        return countResult;
    }
    
    public static List<DBObject> find(PersistenceContext context, MetaClass metadata, DBObject queryObject, DBObject fieldObject, SearchOption option){
        long start = System.currentTimeMillis();
        String msg = "success";        
        DBCollection dbCollection = context.getDBCollection(metadata);
        DBCursor findCursor = null;
        Integer size = 0;
        try{
            findCursor = dbCollection.find(queryObject, fieldObject);
            // set option
            if (option.hasLimit()) {
                findCursor.limit(option.getLimit());
            }
            if (option.hasSkip()) {
                findCursor.skip(option.getSkip());
            }
            if (option.hasSort()) {
                findCursor.sort(option.getSort());
            }
            // populate search result
            List<DBObject> result = findCursor.toArray();
            size = result.size();
            return result;
        }catch(Throwable t){
            msg = t.getMessage();
            handleMongoException(t);
        }finally {
            if (findCursor != null) {
                findCursor.close();
            }
            logMongoAction(context, "find", start, dbCollection, queryObject, fieldObject, option, size, msg);
        }
        return Collections.emptyList();
    }
    
    public static DBObject findOne(PersistenceContext context, MetaClass metadata, DBObject queryObject, DBObject fieldObject){
        long start = System.currentTimeMillis();
        DBObject findResult = null;
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try{
            findResult = dbCollection.findOne(queryObject, fieldObject);
        } catch(Throwable t){
            msg = t.getMessage();
            handleMongoException(t);
        }finally{
            logMongoAction(context, "find", start, dbCollection, queryObject, fieldObject, null, null, msg);
        }
        return findResult;
    }
    
    public static WriteResult insert(PersistenceContext context, MetaClass metadata, DBObject insertObject){
        long start = System.currentTimeMillis();
        WriteResult writeResult = null;
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try {
            writeResult = dbCollection.insert(insertObject);
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            logMongoAction(context, "insert", start, dbCollection, null, insertObject, null, null, msg);
        }

        return writeResult;
    }
    
    public static WriteResult update(PersistenceContext context, MetaClass metadata, DBObject queryObject, DBObject bodyObject){
        long start = System.currentTimeMillis();
        WriteResult writeResult = null;
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try {
            writeResult = dbCollection.update(queryObject, bodyObject, false, false);
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            logMongoAction(context, "update", start, dbCollection, queryObject, bodyObject, null, null, msg);
        }
        return writeResult;
    }

    public static WriteResult delete(PersistenceContext context, MetaClass metadata, DBObject deleteObject) {
        long start = System.currentTimeMillis();
        WriteResult writeResult = null;
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try {
            writeResult = dbCollection.remove(deleteObject);
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            logMongoAction(context, "delete", start, dbCollection, deleteObject, null, null, null, msg);
        }
        return writeResult;
    }

    public static void ensureIndex(PersistenceContext context, DBCollection dbCollection, DBObject keyObject,
            DBObject optionObject) {
        long start = System.currentTimeMillis();
        String msg = "success";
        try {
            // Mongo driver has a simple cache field, which might cause the ensure index command not be executed.
            // This will cause a case failed: create "index1", drop it, then try to create it again. The second
            // will not be issued to mongo db, as Mongo driver will think it has been created. So here we reset the index
            // cache before an ensure index command.
            dbCollection.resetIndexCache();
            
            dbCollection.ensureIndex(keyObject, optionObject);
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            logMongoAction(context, "ensureIndex", start, dbCollection, keyObject, optionObject, null, null, msg);
        }
    }

    public static void dropIndex(PersistenceContext context, MetaClass metadata, String indexName) {
        long start = System.currentTimeMillis();
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        try {
            dbCollection.dropIndex(indexName);
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            DBObject dropDbo = new BasicDBObject();
            dropDbo.put("indexname", indexName);
            logMongoAction(context, "dropIndex", start, dbCollection, dropDbo, null, null, null, msg);
        }
    }
    
    public static void createHistoryCollection(PersistenceContext context, String collectionName) {
        long start = System.currentTimeMillis();
        String msg = "success";
        DB db = context.getDB();
        try {
            BasicDBObject o = new BasicDBObject();
            db.createCollection(collectionName, o);
       } catch (MongoException e) {
           //collection already exist
           if (db.getCollection(collectionName).isCapped()) {
               db.getCollection(collectionName).drop();
               BasicDBObject o = new BasicDBObject();
               db.createCollection(collectionName, o);
           }
       } catch (Throwable t) {        
           msg = t.getMessage();
           handleMongoException(t);
       } finally {
           logMongoAction(context, "createHistoryCollection", start, null, null, null, null, null, msg);
       }
    }

    public static AggregationOutput aggregate(PersistenceContext context, MetaClass metadata, DBObject firstObject,
            DBObject... aggrObjects) {
        long start = System.currentTimeMillis();
        String msg = "success";
        DBCollection dbCollection = context.getDBCollection(metadata);
        AggregationOutput output = null;
        try {
            output = dbCollection.aggregate(firstObject, aggrObjects);
            if (!output.getCommandResult().ok()) {
                throw new CmsDalException(DalErrCodeEnum.AGGREGATION_FAILED, output.getCommandResult()
                        .getErrorMessage());
            }
        } catch (Throwable t) {
            msg = t.getMessage();
            handleMongoException(t);
        } finally {
            DBObject followingOjbect = new BasicDBObject();
            followingOjbect.put("following aggreate operations: ", aggrObjects);
            logMongoAction(context, "aggregate", start, dbCollection, firstObject, followingOjbect, null, null, msg);
        }
        return output;
    }

}
