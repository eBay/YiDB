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

package com.ebay.cloud.cms.dal.common.flatten;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.flatten.utils.TestUtils;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
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
public class TestDataLoader {
	private static final Logger logger = LoggerFactory.getLogger(TestDataLoader.class);
	
	private final Repository repo;
	private final IPersistenceService persistService;
	private final IMetadataService metaService;
	private final MongoDataSource dataSource;
	private final ISearchService searchService;

	private TestDataLoader(Repository repo,IPersistenceService persistService,  MongoDataSource dataSource){
	    CheckConditions.checkNotNull(repo);
	    CheckConditions.checkNotNull(persistService);
		this.repo = repo;
		this.persistService = persistService;
		this.dataSource = dataSource;
		this.metaService = repo.getMetadataService();
		this.searchService = new SearchServiceImpl(dataSource);
	}
	
	public String getRepositoryName(){
		return this.repo.getRepositoryName();
	}
	
	public static TestDataLoader getDataLoader(MongoDataSource dataSource, String repoName){
		IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		Repository repo = repoService.getRepository(repoName);
		IPersistenceService persistService = new NewPersistenceServiceImpl(dataSource);
		TestDataLoader loader = new TestDataLoader(repo, persistService, dataSource);
		return loader;
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
	public List<NewBsonEntity> load(List<NewBsonEntity> inputList, DBCollectionPolicy collectionPolicy){
		cleanUp();

		CheckConditions.checkNotNull(repo,"Failed to get meta data service from repository %s", repo.getRepositoryName());
		List<NewBsonEntity> loadList = new ArrayList<NewBsonEntity>();
		
		createDefaultBranch(repo);
	
		for(NewBsonEntity instNode: inputList){
		    persistBsonEntity(collectionPolicy, loadList, instNode);
		}
		return loadList;
	}

    void persistBsonEntity(DBCollectionPolicy collectionPolicy, List<NewBsonEntity> loadList, NewBsonEntity instNode) {
        String instName = "";
        if (instNode.hasField("name")) {
            instName = (String) instNode.getFieldValues("name").get(0);
        }
        String metaType = instNode.getType();
        String branchId = instNode.getBranchId();
        instNode.setCreator("unitTestUser");
        instNode.addFieldValue(InternalFieldEnum.MODIFIER.getName(), "unitTestUser");
        instNode.addFieldValue(InternalFieldEnum.COMMENT.getName(), "test data loader create comments");
        logger.debug(String.format("Load instance %s of type %s into Mongo",instName,metaType));

        PersistenceContext context = new PersistenceContext(metaService, collectionPolicy,
                ConsistentPolicy.safePolicy(), branchId + "_base");
        String savedId = persistService.create(instNode, context);
        instNode.setId(savedId);

        logger.debug("saved entity with id {}", savedId);
        loadList.add(instNode);
    }
	
	private String createDefaultBranch(Repository repo) {
		Repository repoInst = repo;
		MetaClass branchClass = BranchMetaClass.getMetaClass(repoInst);

        PersistenceContext context = new PersistenceContext(metaService, DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), DalTestCons.BRANCH_INFO);
        context.setRegistration(TestUtils.getTestDalImplemantation(dataSource));

		SearchProjection searchProject = new SearchProjection();
		searchProject.addField(ProjectionField.STAR);
		ISearchQuery query = new SearchQuery(branchClass, null, searchProject, TestUtils.getStrategy());
		SearchOption option = new SearchOption();
		SearchResult result = searchService.search(query, option, context);
		List<IEntity> bsonList = result.getResultSet();

		if (bsonList.size() == 0) {
			NewBsonEntity bsonEntity = new NewBsonEntity(branchClass);
			bsonEntity.addFieldValue("name", DalTestCons.MAIN_BRANCH);
			bsonEntity.addFieldValue(BranchMetaClass.IsMain, true);
			bsonEntity.setId(DalTestCons.MAIN_BRANCH);
			bsonEntity.setBranchId("metabranch");
			bsonEntity.addFieldValue(InternalFieldEnum.PVERSION.getName(), IEntity.NO_VERSION);
			bsonEntity.setVersion(IEntity.START_VERSION);

			persistService.replace(bsonEntity, context);//NOT CREATE here, use replace
			return bsonEntity.getId();
		} 
		return bsonList.get(0).getId();
	}
	
	/**
	 * clean up two collections: main and branches
	 */
	public void cleanUp(){
	    //FIXME: didn't drop collection properly, only dropped collection for main branch. But "load" method used other branch name. 
	    
		logger.debug(String.format("To clean up all runtime data in repository %s", repo.getRepositoryName()));
		//drop main & history repository
        PersistenceContext mainContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), DalTestCons.MAIN_BRANCH + "_base");
        mainContext.setMongoDataSource(dataSource);
        PersistenceContext histContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), DalTestCons.MAIN_BRANCH_HISTORY + "_base");
		histContext.setMongoDataSource(dataSource);
		
		List<MetaClass> metadataList = repo.getMetadataService().getMetaClasses(new MetadataContext());
        for (MetaClass metadata : metadataList) {
            DBCollection mainCollection = mainContext.getDBCollection(metadata);
            System.out.println("dropping runtime data in " + mainCollection.getName());
            mainCollection.drop();
                        
            DBCollection histCollection = histContext.getDBCollection(metadata);
            System.out.println("dropping runtime data in " + histCollection.getName());
            histCollection.drop();
        }
		
	}
}
