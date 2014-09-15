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

package com.ebay.cloud.cms.entmgr.branch.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DBCollection;

/**
 * 
 * @author jianxu1
 * @date 2012/7/10
 * 
 */
public class BranchServiceImpl extends AbstractBranchCommand implements IBranchService {

	// private static final Logger log =
	// LoggerFactory.getLogger(BranchServiceImpl.class);

	private final IRepositoryService repositoryService;
	private final ISearchService searchService;
	private final IPersistenceService persistenceService;

	private BranchCache branchCache;
	private MongoDataSource ds;

	public BranchServiceImpl(MongoDataSource ds, IRepositoryService repositoryService,
			IPersistenceService persistenceService, ISearchService searchService) {
		this.repositoryService = repositoryService;
		this.persistenceService = persistenceService;
		this.searchService = searchService;

		this.ds = ds;		
		branchCache = new BranchCache();
	}

	private PersistenceContext getBranchPersistenceContext(String repoName, EntityContext entityContext) {
		Repository repo = repositoryService.getRepository(repoName);
		IMetadataService metaService = repo.getMetadataService();
		PersistenceContext pc =  new PersistenceContext(metaService, DBCollectionPolicy.Merged, entityContext.getConsistentPolicy(),
				CMSConsts.BRANCH_DB_COLL_NAME, entityContext.getRegistration());
		pc.setDbConfig(entityContext.getDbConfig());
		pc.setAdditionalCriteria(entityContext.getAdditionalCriteria());
		pc.setFetchFieldProperties(entityContext.isFetchFieldProperty());
		return pc;
	}

	/**
	 * POST repository/{repoId}/branch/
	 */
	@Override
	public IBranch createBranch(IBranch branch, EntityContext context) {
	    CheckConditions.checkNotNull(branch);

		String repoName = branch.getRepositoryName();

		PersistenceContext persistenceContext = getBranchPersistenceContext(repoName, context);
		BranchCreateCommand createCommand = new BranchCreateCommand(branch, repositoryService, persistenceService, context);
		createCommand.execute(persistenceContext);
		String newBranchId = createCommand.getCreatedBranchId();

		// getBranch will lookup this branch in db and put it into cache
		return this.getBranch(repoName, newBranchId, context);
	}

	Branch getBranchFromDB(String repoName, String branchId, EntityContext context) {
		Branch branch;

		PersistenceContext persistenceContext = getBranchPersistenceContext(repoName, context);
		BranchGetCommand getCommand = new BranchGetCommand(persistenceService, branchId);

		ConsistentPolicy oldConsistency = persistenceContext.getConsistentPolicy();
		ConsistentPolicy cp = ConsistentPolicy.PRIMARY;
		persistenceContext.setConsistentPolicy(cp);
		getCommand.execute(persistenceContext);
		persistenceContext.setConsistentPolicy(oldConsistency);
		branch = getCommand.getBranch();

		return branch;
	}

	@Override
	public IBranch getBranch(String repoName, String branchId, EntityContext context) {
		IBranch branch = branchCache.getBranch(repoName, branchId);

		if (branch == null) {
			Branch b = getBranchFromDB(repoName, branchId, context);

			if (b != null) {
				branchCache.putBranch(b);
				branch = b;
			}
		}

		return branch;
	}

	@Override
	public List<IBranch> getMainBranches(String repoName, EntityContext context) {
		Repository repo = this.repositoryService.getRepository(repoName);

		MetaClass brancMetaData = BranchMetaClass.getMetaClass(repo);

        FieldSearchCriteria criteria = new FieldSearchCriteria(brancMetaData.getFieldByName(BranchMetaClass.IsMain),
                context.getRegistration().searchStrategy, FieldSearchCriteria.FieldOperatorEnum.EQ, Boolean.TRUE);
		SearchProjection searchProject = new SearchProjection();
		searchProject.addField(ProjectionField.STAR);
		ISearchQuery query = new SearchQuery(brancMetaData, criteria, searchProject, context.getRegistration().searchStrategy);
		SearchOption option = new SearchOption();

		PersistenceContext persistenceContext = getBranchPersistenceContext(repoName, context);
		SearchResult result = searchService.search(query, option, persistenceContext);
		List<IEntity> bsonList = result.getResultSet();

		List<IBranch> branches = new ArrayList<IBranch>(bsonList.size());
		for (IEntity e : bsonList) {
            branches.add(toBranch(e));
		}
		
		branchCache.clearCache();

		return branches;
	}

	public void clearBranchCache() {
		branchCache.clearCache();
	}

	@Override
	public void ensureIndex(String repoName, List<MetaClass> metadatas, EntityContext context) {
	    CheckConditions.checkNotNull(repoName, "Repository name can not be null");

		Repository repo = this.repositoryService.getRepository(repoName);
		List<IBranch> branches = getMainBranches(repoName, context);

		List<MetaClass> allMetadatas = new LinkedList<MetaClass>();
		getMetadata(metadatas, allMetadatas);

		for (IBranch b : branches) {
			// create index on main branch collection
			PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
					repo.getMetadataService(), b.getId(), context.getConsistentPolicy(), context.getRegistration(),
					context.isFetchFieldProperty(), context.getDbConfig(), context.getAdditionalCriteria());
			persistenceService.ensureIndex(allMetadatas, persistenceContext, true);
		}
	}

	/**
	 * @param metadatas
	 */
	private void getMetadata(List<MetaClass> baseMetadatas, List<MetaClass> allMetadatas) {
		for (MetaClass baseMeta : baseMetadatas) {
			allMetadatas.add(baseMeta);
			List<MetaRelationship> baseRelationships = baseMeta.getFromReference();
			if (!baseRelationships.isEmpty()) {
				List<MetaClass> outerMetadatas = new LinkedList<MetaClass>();
				for (MetaRelationship baseRelationship : baseRelationships) {
					if (baseRelationship.getRelationType().equals(RelationTypeEnum.Embedded)) {
						MetaClass fromMeta = baseRelationship.getSourceMetaClass();
						outerMetadatas.add(fromMeta);
					}
				}
				getMetadata(outerMetadatas, allMetadatas);
			}
		}
	}

	@Override
	public void deleteMetadata(String repoName, MetaClass metadata, EntityContext context) {
		// drop all metadata related collection for each branch
		Repository repo = this.repositoryService.getRepository(repoName);
		List<IBranch> branches = getMainBranches(repoName, context);
		for (IBranch b : branches) {
			PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
					repo.getMetadataService(), b.getId(), context.getConsistentPolicy(), context.getRegistration(),
					context.isFetchFieldProperty(), context.getDbConfig(), context.getAdditionalCriteria());
			persistenceContext.setMongoDataSource(ds);
			DBCollection coll = persistenceContext.getDBCollection(metadata);
			coll.drop();
		}
	}
}
