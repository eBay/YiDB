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

package com.ebay.cloud.cms.dal.search.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.IEntityFactory;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchQuery.MongoQuery;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define mongodb search service.
 * 
 * It will translate search query to mongodb query; execute the mongodb query and return the result
 * 
 * @author xjiang
 *
 */
public class SearchServiceImpl implements ISearchService {
	
	public static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final MongoDataSource dataSource;
    
	public SearchServiceImpl(MongoDataSource ds) {
	    this.dataSource = ds;
    }

	private Long getSysLimitMemoryForMongoQuery(CMSDBConfig dbConfig) {
		Map<String, Object> configs = dbConfig.getCurrentConfiguration();
		if (configs.containsKey(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY)
				&& (configs.get(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY) instanceof Number)) {
			return ((Number)(configs.get(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY))).longValue();
		}
		return null;
    }

    private Double getSysLimitMemoryForMongoQuerySamplingRatio(CMSDBConfig dbConfig) {
		Map<String, Object> configs = dbConfig.getCurrentConfiguration();
		if (configs.containsKey(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO)
				&& (configs.get(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO) instanceof Double)) {
			return (Double) (configs.get(CMSDBConfig.SYS_LIMIT_MEMORY_MONGO_QUERY_SAMPLING_RATIO));
		}
		return null;
	}
	
	/**
	 * main entry method to execute query 
	 * 1. build mongodb query
	 * 2. execute mongodb query
	 * 3. convert mongodb object to entity
	 */
	@Override
	public SearchResult search(ISearchQuery searchQuery, SearchOption option, PersistenceContext context) {
	    CheckConditions.checkNotNull(searchQuery, "query can not be null");
	    CheckConditions.checkNotNull(option, "option can not be null");
		setupContext(context);
		
		// build mongodb query
		MongoQuery mongoQuery = searchQuery.buildMongoQuery(context);
		
		// execute mongodb query
		SearchResult result = executeMongoQuery(mongoQuery, option, context, searchQuery);

		// execute in-memory filter for embedded entity
		if (searchQuery instanceof EmbedSearchQuery) {
		    EmbedSearchQuery embedQuery = (EmbedSearchQuery)searchQuery;
		    embedQuery.evaluate(result.getResultSet(), context);
		    embedQuery.setSearchResult(result);
		}
        // set execution plan
        addSearchExplanation(result, mongoQuery, option, context.getLastTimeCost());

		return result;
	}

    protected void setupContext(PersistenceContext context) {
        context.setMongoDataSource(dataSource);
        context.setCollectionFinder(context.getRegistration().collectionFinder);
    }

	/**
	 * @param result
	 */
	private void checkMemoryUsage(List<DBObject> dbObjectList, PersistenceContext context) {
		int totalCount = dbObjectList.size();
		if (totalCount > 0) {
			CMSDBConfig dbConfig = context.getDbConfig();
			if (dbConfig != null) {
				Long sysLimitMemory = getSysLimitMemoryForMongoQuery(dbConfig);
				if (sysLimitMemory != null) {
//					logger.info("sysLimitMemory is " + sysLimitMemory + " bytes.");
					double ratio = 0.02;
					Double sysLimitMemorySamplingRatio = getSysLimitMemoryForMongoQuerySamplingRatio(dbConfig);
					if (sysLimitMemorySamplingRatio != null) {
						ratio = sysLimitMemorySamplingRatio.doubleValue();
					}
					int samplingCount = (int)(totalCount * ratio);
					if (samplingCount == 0) {
						samplingCount = (int)(totalCount * ratio * 10);
					}
					if (samplingCount == 0 || samplingCount > totalCount) {
						samplingCount = 1;
					}
//					logger.info("samplingCount is " + samplingCount + ".");
					int[] randomIndexes = randomArray(0, totalCount - 1, samplingCount);
					long totalSamplingSize = 0l;
					for (int index : randomIndexes) {
						totalSamplingSize += dbObjectList.get(index).toString().getBytes().length;
					}
					double avgObjSize = totalSamplingSize / (double) samplingCount;
//					logger.info("avgObjSize is " + avgObjSize + " bytes.");
					long totalObjSize = (long) avgObjSize * totalCount;
//					logger.info("totalObjSize is " + totalObjSize + " bytes.");
					if (totalObjSize > sysLimitMemory) {
						String errorMessage = String.format("Exceed system limit memory usage for mongo query! Limit is %d, Actual is %d",
								sysLimitMemory, totalObjSize);
						throw new CmsDalException(DalErrCodeEnum.EXCEED_QUERY_LIMIT_MEMORY_USAGE, errorMessage);
					}
				}
			}
		}
	}

	/**
	 * Execute mongodb query 
	 * 
	 */
    private SearchResult executeMongoQuery(MongoQuery mongoQuery, SearchOption option, PersistenceContext context,
            ISearchQuery query) {
        MetaClass metadata = query.getMetadata();
        SearchResult result = new SearchResult(metadata);

        if (option.isOnlyCount()) {
            long count = MongoExecutor.count(context, metadata, mongoQuery.match);
            result.setCount(count);
            result.setRawCount(count);
        } else if (mongoQuery.group == null) {
            List<DBObject> dbObjectList = null;
            dbObjectList = MongoExecutor.find(context, metadata, mongoQuery.match, mongoQuery.project, option);
            checkMemoryUsage(dbObjectList, context);
            buildResultObject(metadata, result, dbObjectList, context.getEntityFactory());
        } else {
            AggregationOutput output = null;
            DBObject firstAggr = null;
            List<DBObject> aggrObjects = new ArrayList<DBObject>();
            firstAggr = buildAggregationObjects(mongoQuery, option, aggrObjects);
            output = MongoExecutor.aggregate(context, metadata, firstAggr, aggrObjects.toArray(new DBObject[0]));
            List<DBObject> dbObjectList = new ArrayList<DBObject>();
            for(DBObject entity : output.results()) {
            	dbObjectList.add(entity);
            }
            checkMemoryUsage(dbObjectList, context);
            buildResultObject(metadata, result, output.results(), context.getEntityFactory());
        }
        
        // count node should not have hasmore set
        if (option.isLookForward() && !option.isOnlyCount() && result.getRawCount() >= option.getLimit()) {
            result.setHasMore(true);
            List<IEntity> resultSet = result.getResultSet();
            resultSet.remove(resultSet.size() - SearchOption.LOOK_FORWARD);
        }
        
        // set cursor entity
        List<IEntity> results = result.getResultSet();
        if (!results.isEmpty()) {
            IEntity lastEntity = results.get(results.size() - 1);
            result.setCursorEntity(lastEntity);
        }

        return result;
    }

    private DBObject buildAggregationObjects(MongoQuery mongoQuery, SearchOption option, List<DBObject> aggrObjects) {
        DBObject firstAggr;
        List<DBObject> allAggrObjects = new ArrayList<DBObject>();
        // set pipeline db objects
        addToList(mongoQuery.match, allAggrObjects);
        addToList(mongoQuery.preGroupUnwinds, allAggrObjects);
        addToList(mongoQuery.group, allAggrObjects);
        addToList(mongoQuery.groupMatch, allAggrObjects);
        addToList(mongoQuery.project, allAggrObjects);

        // set search options
        if (option.hasSort()) {
            DBObject sortObject = new BasicDBObject();
            sortObject.put("$sort", option.getSort());
            allAggrObjects.add(sortObject);
        }
        if (option.hasSkip()) {
            DBObject skipObject = new BasicDBObject();
            skipObject.put("$skip", option.getSkip());
            allAggrObjects.add(skipObject);
        }
        if (option.hasLimit()) {
            DBObject limitObject = new BasicDBObject();
            limitObject.put("$limit", option.getLimit());
            allAggrObjects.add(limitObject);
        }

        // set return object
        firstAggr = allAggrObjects.get(0);
        aggrObjects.addAll(allAggrObjects.subList(1, allAggrObjects.size()));
        return firstAggr;
    }

    private void addToList(List<DBObject> preGroupUnwinds, List<DBObject> aggrObjects) {
        if (preGroupUnwinds != null) {
            aggrObjects.addAll(preGroupUnwinds);
        }
    }

    private void addToList(DBObject groupMatch, List<DBObject> aggregationObjects) {
        if (groupMatch != null && groupMatch.keySet().size() > 0) {
            aggregationObjects.add(groupMatch);
        }
    }

    private void buildResultObject(MetaClass metadata, SearchResult result, Iterable<DBObject> dbObjectList,
            IEntityFactory<? extends IEntity> iEntityFactory) {
        for (DBObject dbo : dbObjectList) {
            IEntity bsonEntity = iEntityFactory.createEntity(metadata, dbo);
            result.addEntity(bsonEntity);
        }
        result.setRawCount(result.getResultSize());
    }

    /**
	 * set search explanation to search result
     * @param watch 
	 */
	private void addSearchExplanation(SearchResult result, MongoQuery mongoQuery, SearchOption option, long last) {
		if (option.needExplanation()) {
		    result.setQueryExplanation(mongoQuery, option, result.getResultSize(), last);
		}
	}
	
	private int[] randomArray(int min,int max,int n){
		int len = max-min+1;
		if(max < min || n > len){
			return null;
		}
		int[] source = new int[len];
        for (int i = min; i < min+len; i++){
        	source[i-min] = i;
        }
        int[] result = new int[n];
        Random rd = ThreadLocalRandom.current();
        int index = 0;
        for (int i = 0; i < result.length; i++) {
            index = Math.abs(rd.nextInt() % len--);
            result[i] = source[index];
            source[index] = source[len];
        }
        return result;
	}
}
