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

package com.ebay.cloud.cms.entmgr.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.entmgr.branch.impl.BranchServiceImpl;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DB;
import com.mongodb.DBCollection;

/**
 * 
 * @author jianxu1
 * @date 2012/5/22
 * 
 * @history
 * 2012/5/23  change from IEntityService.create to IEntityService.replace in Runtime load
 *
 */

public class RuntimeDataLoader {
	private static final Logger logger = LoggerFactory.getLogger(RuntimeDataLoader.class);
	
	private IRepositoryService repoService = null;
	
	private IEntityService entityService = null;
	//private MongoDataSource dataSource = null;
	private IBranchService branchService = null;

    private IMetadataService metaService;

    private SearchServiceImpl searchService;

	private MongoDataSource dataSource;

    private String repositoryName;

	private RuntimeDataLoader(MongoDataSource dataSource,
			IRepositoryService repoService,
			String repositoryName,
			List<Registration> implementations,
			IEntityService entityService){
	    CheckConditions.checkNotNull(repoService);
	    CheckConditions.checkNotNull(entityService);
		this.repoService = repoService;
		this.entityService = entityService;
		this.repositoryName = repositoryName;
		this.branchService = ServiceFactory.getBranchService(dataSource, implementations);
		this.dataSource = dataSource;
		this.metaService = repoService.getRepository(repositoryName).getMetadataService();
        this.searchService = new SearchServiceImpl(dataSource);
	}
	
	public String getRepositoryName(){
		return this.repositoryName;
	}
	
	public static RuntimeDataLoader getDataLoader(MongoDataSource dataSource, IRepositoryService repoService, String repoName){
		List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);
		RuntimeDataLoader loader = new RuntimeDataLoader(dataSource, repoService,
				repoName,
				implementations, ServiceFactory.getEntityService(dataSource, repoService, implementations));
		return loader;
	}
	
	/**
	 * Load Json into memory
	 * @param entityDataFile
	 * @return
	 */
    public static Map<String, List<JsonNode>> loadRawData(String entityDataFile) {
        String resourceName = "/" + entityDataFile;
        InputStream inputJsonStream = RuntimeDataLoader.class.getResourceAsStream(resourceName);
        CheckConditions.checkNotNull(inputJsonStream, "Can not find %s", entityDataFile);

        Map<String, List<JsonNode>> rawData = new LinkedHashMap<String, List<JsonNode>>();
        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            JsonNode root = jsonMapper.readTree(inputJsonStream);
            inputJsonStream.close();
            Iterator<String> iter = root.getFieldNames();
            while (iter.hasNext()) {
                String metaType = iter.next();
                logger.debug(String.format("Load runtime data of type %s", metaType));
                ArrayNode child = (ArrayNode) root.get(metaType);
                List<JsonNode> loadList = new ArrayList<JsonNode>();
                Iterator<JsonNode> instIter = child.getElements();
                rawData.put(metaType, loadList);
                while (instIter.hasNext()) {
                    JsonNode instNode = instIter.next();
                    loadList.add(instNode);
                }
            }
        } catch (IOException ex) {
            throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON, ex);
        }

        return rawData;
    }
	
	/**
	 * 
	 * @param entityDataFile : data file path is relative to /
	 * the real file path is "/entityDataFile"
	 * data format of the runtime entity file is:
	 * {
	 * 		"meta type 1": [
	 * 			{
	 *              //entity instance of meta type 1
	 *          },
	 *          {
	 *              //entity instance of meta type 1
	 *          } 
	 *      ],
	 *      "meta type 2": [
	 *      	{
	 *              //entity instance of meta type 2
	 *          }
	 *      ]
	 * }
	 */
	public List<IEntity> load(String entityDataFile){
		cleanUp();

		Repository repo = repoService.getRepository(repositoryName);
    	IMetadataService metaService = repo.getMetadataService();
    	CheckConditions.checkNotNull(repo,"Failed to get meta data service from repository %s", repo.getRepositoryName());
		
		EntityContext context = initEntityContext();

		// create default branch
		String createdBranchId  = createDefaultBranch(repo, context);

		Map<String,List<JsonNode>> rawData = loadRawData(entityDataFile);
		List<IEntity> loadList = new ArrayList<IEntity>();
		for(Entry<String, List<JsonNode>> entry: rawData.entrySet()){
			String metaType = entry.getKey();
			MetaClass metaClass = metaService.getMetaClass(metaType);
			for(JsonNode instNode: entry.getValue()){
				IEntity jsonEntity = new JsonEntity(metaClass, (ObjectNode)instNode);
    			jsonEntity.setBranchId(createdBranchId);

                // create or replace entity
                if (jsonEntity.getId() == null) {
                    // create if not id given
                    entityService.create(jsonEntity, context);
                } else {
                    IEntity entity = entityService.get(jsonEntity, context);
                    if (entity == null) {
                        entityService.create(jsonEntity, context);
                    } else {
                        // call replace if an entity already exist
                        entityService.replace(jsonEntity, jsonEntity, context);
                    }
                }

                IEntity loadEntity = entityService.get(jsonEntity, context);
    			//save entity to mongo
    			
    			String savedId = loadEntity.getId();
    			logger.debug("saved entity with id {}", savedId);
    			
    			loadList.add(loadEntity);
			}
			
		}
		return loadList;
	}

    protected EntityContext initEntityContext() {
        EntityContext context = new EntityContext();
		context.setSourceIp("127.0.0.1"); //NOPMD
		context.setModifier("unitTestUser");
        context.setComment("unit test create comments.");
        context.setDbConfig(new CMSDBConfig(dataSource));
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return context;
    }
	
	private String createDefaultBranch(Repository repo, EntityContext context) {
        Repository repoInst = repo;
        MetaClass branchClass = BranchMetaClass.getMetaClass(repoInst);

        PersistenceContext pcontext = new PersistenceContext(metaService,
                DBCollectionPolicy.Merged, context.getConsistentPolicy(), CMSConsts.BRANCH_DB_COLL_NAME,
                context.getRegistration());

        SearchProjection searchProject = new SearchProjection();
        searchProject.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(branchClass, null, searchProject, context.getRegistration().searchStrategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, pcontext);
        List<IEntity> bsonList = result.getResultSet();

        if (bsonList.size() == 0) {
            Branch branch = new Branch();
            branch.setRepositoryName(repo.getRepositoryName());
            branch.setMainBranch(true);
            branch.setId(IBranch.DEFAULT_BRANCH);
            return branchService.createBranch(branch, context).getId();
        } else {
            // simply assume first one is the main
            return bsonList.get(0).getId();
        }
        
    }
	
	/**
	 * clean up two collections: main and branches
	 * FIXME: for testing purpose, we can only load entities from main branch
	 */
	public void cleanUp(){
		//main collection pattern: rootbranchid_metaclassname
		//sub branch collection pattern: rootbranchid_subs_metaclassname
		//history collection pattern: rootbranchid_metaclassname_history

		//TODO: find a way to clean up runtime  collections
		
		logger.debug(String.format("To clean up all runtime data in repository %s", repositoryName));
		//drop main repository
		DB db = dataSource.getMongoInstance().getDB(repositoryName);
		
		Set<String> allColNames = db.getCollectionNames();
		for (String colName : allColNames) {
		    if (isRuntimeCollection(colName)) {
		        DBCollection dbCollection = db.getCollection(colName);
		        dbCollection.drop();
		    } else {
		        logger.debug("don't drop collection " + colName);
		    }
		}
		
		BranchServiceImpl bimpl = (BranchServiceImpl)branchService;
		bimpl.clearBranchCache();
	}
	
	private final static String PATTERN = ".+_.+";
	
	private boolean isRuntimeCollection(String colName) {
        if (colName.matches(PATTERN))  {
            return true;
        }
		return false;
    }

}
