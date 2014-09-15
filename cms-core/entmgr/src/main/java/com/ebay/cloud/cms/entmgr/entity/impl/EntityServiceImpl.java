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

package com.ebay.cloud.cms.entmgr.entity.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSTrackingCodeEnum;
import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.branch.impl.PersistenceContextFactory;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback.Operation;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.BranchHelper;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.DanglingCheckAction;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.EntityCheckHelper;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.EntityValidator;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.InnerCheckAction;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.RefIntValidator;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.ReqInfoFillAction;
import com.ebay.cloud.cms.entmgr.entity.impl.validator.SystemLimitationHelper;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

/**
 * 
 * @author jianxu1 Each repository is a data base, mongo database name should be
 *         lowercase,so db name is repositoryName.lowercase Within a repository,
 *         there're following collections: metadata: all the meta data
 *         information of the given repository main: all the runtime data of
 *         main branch branches: all the runtime data of all the branches except
 *         main branch
 * 
 * @date 2012/5/27
 * 
 * @history
 * 
 */
public class EntityServiceImpl implements IEntityService {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityServiceImpl.class);

//	private static final String ID_SEP_REG = "\\" + IEntity.ID_SEP;
//	private static final int LENGTH_OF_MONGO_OBJECT_ID = 24;
	
	private final IRepositoryService repositoryService;
	private final IPersistenceService persistenceService;
	private IEntityOperationCallback callback;

	private final ReqInfoFillAction fillAction;
	private final DanglingCheckAction danglingAction;
	private final InnerCheckAction innerAction;
	private final RefIntValidator refInetegrationValidator;
	
	private final EntityCheckHelper entityHelper;
	private final SystemLimitationHelper systemLimitationHelper;
	private final BranchHelper branchHelper;
	private final EntityGetManager entityGetManager;
	
	private final EntityFieldTargetMerger targetFieldPushMerger;
	private final EntityFieldTargetMerger targetFieldPullMerger;
    private final EntityFieldDeltaMerger  deltaFieldPushMerger;

	public EntityServiceImpl(IRepositoryService repoService, IPersistenceService persistenceService,
			IBranchService branchService, ISearchService searchService) {
		this.repositoryService = repoService;
		this.persistenceService = persistenceService;
		this.fillAction = new ReqInfoFillAction();
		this.danglingAction = new DanglingCheckAction();
		this.innerAction = new InnerCheckAction();
		this.refInetegrationValidator = new RefIntValidator(searchService);
		this.entityHelper = new EntityCheckHelper(repoService, persistenceService);
		this.targetFieldPushMerger = new EntityFieldTargetMerger(false);
		this.targetFieldPullMerger = new EntityFieldTargetMerger(true);
		this.deltaFieldPushMerger = new EntityFieldDeltaMerger();
		this.systemLimitationHelper = new SystemLimitationHelper(this.entityHelper, branchService, repoService, persistenceService);
		this.branchHelper = new BranchHelper(branchService);
		this.entityGetManager = new EntityGetManager(repoService, persistenceService, branchService);
	}

//	private IBranch getAndCheckCurrentBranch(String repoName, String branchId, EntityContext context) {
//		IBranch branchEntity = branchService.getBranch(repoName, branchId, context);
//		if (branchEntity == null) {
//			throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND, "Branch not found: "
//					+ branchId);
//		}
//		return branchEntity;
//	}

	/**
	 * Every time we call create, we create a new entity with unique object id.
	 * If branch structure is Main-->A-->B, we call create on A and B
	 * separately, we create two different entities. So If we call create on
	 * branch B only, no need to copy entity on A during creation time (this is
	 * different from modify), entity appear on branch A only when we call
	 * commit on B.
	 * 
	 * while in modify/replace semantics, if we modify entity on B and that
	 * entity does not in A, we have to copy entity from Main to A first before
	 * we do modify on B.
	 * 
	 */
	@Override
	public String create(IEntity entity, EntityContext context) {
	    CheckConditions.checkNotNull(entity);
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		IEntity foundEntity = null;
		MetaClass meta = entity.getMetaClass();
		if (entity.getId() != null) {
			if (meta.isEmbed()) {
				if (entity.getId() != null && !AbstractEntityIDHelper.isEmbedEntity(entity.getId()) && context.getPath() == null) {
					throw new CmsEntMgrException(EntMgrErrCodeEnum.INVALID_EMBED_ID_PATH, MessageFormat.format("Invalid embed ID: {0}, path: null", entity.getId()));
				}
				entity.setEmbedPath(context.getPath());
			}
			foundEntity = entityGetManager.findAndCopy(branchEntity, entity, persistenceContext);
        }

		entityHelper.checkACL(callback, entity, foundEntity, null, branchEntity, Operation.CREATE, context, persistenceContext);
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.CREATE, true,
				Collections.<IEntity> emptyList(), true);

		if (foundEntity == null) {
			if (meta.isInner()) {
				String host = entity.getHostEntity();
				if (StringUtils.isNullOrEmpty(host)) {
					host = context.getPath();
					entity.setHostEntity(host);
				}

				String[] idParts = entityHelper.splitPath(host, true);
				IEntity queryHostEntity = entityGetManager.getHostEntity(entity, host, idParts, persistenceContext, true);
				MetaRelationship innerRel = getHostInnerRelation(queryHostEntity.getMetaClass(), idParts, true);
				String fieldName = innerRel.getName();
				IEntity hostEntity = entityHelper.getAndCheckExsitingEntity(queryHostEntity, branchEntity, persistenceContext);
				if (innerRel.getCardinality() == CardinalityEnum.One && !hostEntity.getFieldValues(fieldName).isEmpty()) {
					throw new CmsEntMgrException(EntMgrErrCodeEnum.INNER_RELATIONSHIP_EXISTED, MessageFormat.format(
							"the inner relationship {1} in host entity {0} has already been set", hostEntity.getId(),
							fieldName));
				}

				String id = persistenceService.create(entity, persistenceContext);
				try {
				    IEntity innerEntity = createQueryEntity(branchId, id, meta);
				    IEntity modifyHostEntity = createQueryEntity(branchId, hostEntity.getId(), hostEntity.getMetaClass());
				    modifyHostEntity.addFieldValue(fieldName, innerEntity);
				    if (innerRel.getCardinality() == CardinalityEnum.One) {
				        modifyHostEntity.addFieldValue(InternalFieldEnum.VERSION.getName(), hostEntity.getVersion());
				    }
				    persistenceService.modifyField(modifyHostEntity, fieldName, persistenceContext);
				    return id;
				} catch (RuntimeException e) {
			        throw new CmsEntMgrException(EntMgrErrCodeEnum.INNER_PARTIAL_CREATION,
			                MessageFormat.format("the inner creation failed: manually delete the entity id={0}", id),
			                e);
				}
			} else {
				if (meta.isEmbed()) {
					entity.setEmbedPath(context.getPath());
				}
				return persistenceService.create(entity, persistenceContext);
			}
        } else if (foundEntity.getStatus() != StatusEnum.ACTIVE) {
            int parentVersion = foundEntity.getParentVersion();
            entity.setParentVersion(parentVersion);
            persistenceService.replace(entity, persistenceContext);
            return entity.getId();
        } else {
			throw new CmsDalException(DalErrCodeEnum.ENTITY_ALREADY_EXIST, String.format(
					"entity %s already exists in branch %s", entity.getId(), branchId));
		}
	}
	
	private String getPassedOperationWord(String operation) {
		String passedOperationWord = null;
		if ("create".equals(operation)) {
			passedOperationWord = "created";
		} else if ("modify".equals(operation)) {
			passedOperationWord = "modified";
		} else if ("delete".equals(operation)) {
			passedOperationWord = "deleted";
		}
		return passedOperationWord;
	}
	
	private void handleExceptionForBatchOperation(IEntity entity, Exception exp, BatchOperationFailReturnOption option, String operation, List<String> result, List<String> fails) {
		if (exp instanceof CmsEntMgrException) {
			CmsEntMgrException e = (CmsEntMgrException) exp;
			if (BatchOperationFailReturnOption.IMMEDIATE.equals(option)) {
				throw new CmsEntMgrException(e.getErrorEnum(), MessageFormat.format(
	                    "batch {0} failure: error code is {1} and error message is {2}. The following entities have been {3}: {4}",
	                    operation, Integer.toString(e.getErrorCode()), e.getMessage(), getPassedOperationWord(operation), Arrays.toString(result.toArray())));
			} else {
				fails.add(MessageFormat.format(
	                      "{0} entity type {1} with oid {2} failure: error code is {3} and error message is {4}.", 
	                      operation, entity.getType(), entity.getId(), Integer.toString(e.getErrorCode()), e.getMessage()));
			}
		} else if (exp instanceof CmsDalException) {
			CmsDalException e = (CmsDalException) exp;
			if (BatchOperationFailReturnOption.IMMEDIATE.equals(option)) {
				throw new CmsDalException(e.getErrorEnum(), MessageFormat.format(
		    			"batch {0} failure: error code is {1} and error message is {2}. The following entities have been {3}: {4}",
		    			operation, Integer.toString(e.getErrorCode()), e.getMessage(), getPassedOperationWord(operation), Arrays.toString(result.toArray())));
			} else {
				fails.add(MessageFormat.format(
	                      "{0} entity type {1} with oid {2} failure: error code is {3} and error message is {4}.", 
	                      operation, entity.getType(), entity.getId(), Integer.toString(e.getErrorCode()), e.getMessage()));
			}
		} else if (exp instanceof WebApplicationException) {
			WebApplicationException e = (WebApplicationException) exp;
			if (BatchOperationFailReturnOption.IMMEDIATE.equals(option)) {
				throw e;
			} else {
				fails.add(MessageFormat.format(
	                      "Unauthorized to {0} entity type {1} with oid {2} failure: {3}.", 
	                      operation, entity.getType(), entity.getId(), e.getMessage()));
			}
		} else {
			if (BatchOperationFailReturnOption.IMMEDIATE.equals(option)) {
				throw new CmsEntMgrException(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE, MessageFormat.format(
	    			"batch {0} failure: error code is {1} and error message is {2}. The following entities have been {3}: {4}",
	    			operation, Integer.toString(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE.getErrorCode()), exp.getMessage(), getPassedOperationWord(operation), Arrays.toString(result.toArray())));
			} else {
				fails.add(MessageFormat.format(
						"{0} entity type {1} with oid {2} failure: error code is {3} and error message is {4}.", 
						operation, entity.getType(), entity.getId(), Integer.toString(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE.getErrorCode()), exp.getMessage()));
			}
		}
	}

	@Override
	public List<String> batchCreate(List<IEntity> entities, EntityContext context, List<String> parseFails) {
		if (entities == null || entities.isEmpty()) {
			return Collections.emptyList();
		}
		context.setPath(null);
		
		IEntity ent = entities.get(0);
		String repoName = ent.getRepositoryName();
		String branchId = ent.getBranchId();
		
		List<String> result = new LinkedList<String>();
		List<String> fails = new ArrayList<String>(parseFails);
		
		for (IEntity entity : entities) {
		    try {
		    	entityHelper.checkOperation(entity, repoName, branchId);
		        String id = create(entity, context);
		        result.add(id);
		    } catch (Exception e) {
		    	handleExceptionForBatchOperation(entity, e, context.getBatchOperationFailReturnOption(), "create", result, fails);
		    }
		}
		
		if (BatchOperationFailReturnOption.ALL.equals(context.getBatchOperationFailReturnOption()) && !fails.isEmpty()) {
			throw new CmsEntMgrException(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE, MessageFormat.format(
                      "batch create failure: {0}. The following entities have been created: {1}",
                      Arrays.toString(fails.toArray()), Arrays.toString(result.toArray())));
		}

		return result;
	}

    private IEntity createQueryEntity(String branchId, String entityId, MetaClass meta) {
		IEntity qEntity = new JsonEntity(meta);
		qEntity.setBranchId(branchId);
		qEntity.setId(entityId);
		return qEntity;
	}

	@SuppressWarnings("unchecked")
	private boolean isInnerEntityExisted(IEntity hostEntity, IEntity entity, String fieldName) {
		List<IEntity> hostFieldVal = (List<IEntity>) hostEntity.getFieldValues(fieldName);
		for (IEntity e : hostFieldVal) {
			if (e.getId().equals(entity.getId())) {
				return true;
			}
		}
		return false;
	}

//	private Collection<String> getQueryFields(MetaClass meta, Collection<String> queryFields) {
//		if (queryFields.isEmpty()) {
//			return queryFields;
//		}
//		Collection<String> dbFieldNames = new ArrayList<String>();
//		for (String fieldName : queryFields) {
//			MetaField field = meta.getFieldByName(fieldName);
//			if (field == null) {
//				throw new CmsEntMgrException(EntMgrErrCodeEnum.FIELD_NOT_FOUND, MessageFormat.format(
//						"field {0} not found", fieldName));
//            }
//            // add both db name and flatten in project. no side affect
//            dbFieldNames.add(field.getDbName());
//            dbFieldNames.add(field.getFlattenValueDbName());
//		}
//		dbFieldNames.add(InternalFieldEnum.ID.getDbName());
//		dbFieldNames.add(InternalFieldEnum.TYPE.getDbName());
//		return dbFieldNames;
//	}

//	private IEntity findAndCopy(IBranch branch, IEntity rootQueryEntity, PersistenceContext persistenceContext) {
//		String currentBranchId = branch.getId();
//		rootQueryEntity.setBranchId(currentBranchId);
//		return persistenceService.get(rootQueryEntity, persistenceContext);
//	}

	@Override
	public List<String> batchModify(List<IEntity> entities, EntityContext context, List<String> parseFails) {
		if (entities == null || entities.isEmpty()) {
			return Collections.emptyList();
		}

		IEntity ent = entities.get(0);
		String repoName = ent.getRepositoryName();
		String branchId = ent.getBranchId();
		
		List<String> result = new LinkedList<String>();
		List<String> fails = new ArrayList<String>(parseFails);
		for (IEntity entity : entities) {
			try {
				entityHelper.checkOperation(entity, repoName, branchId);
				IEntity queryEntity = createQueryEntity(branchId, entity.getId(), entity.getMetaClass());
				modify(queryEntity, entity, context);
				result.add(entity.getId());
			} catch (Exception e) {
				handleExceptionForBatchOperation(entity, e, context.getBatchOperationFailReturnOption(), "modify", result, fails);
			}
		}
		
		if (BatchOperationFailReturnOption.ALL.equals(context.getBatchOperationFailReturnOption()) && !fails.isEmpty()) {
			throw new CmsEntMgrException(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE, MessageFormat.format(
                      "batch modify failure: {0}. The following entities have been modified: {1}",
                      Arrays.toString(fails.toArray()), Arrays.toString(result.toArray())));
		}
		
		return result;
	}
	
	@Override
	public List<String> batchDelete(List<IEntity> entities, EntityContext context, List<String> parseFails) {
		if (entities == null || entities.isEmpty()) {
			return Collections.emptyList();
		}
		
		IEntity ent = entities.get(0);
		String repoName = ent.getRepositoryName();
		String branchId = ent.getBranchId();

		List<String> result = new LinkedList<String>();
		List<String> fails = new ArrayList<String>(parseFails);
		for (IEntity entity : entities) {
			try {
				entityHelper.checkOperation(entity, repoName, branchId);
				delete(entity, context);
				result.add(entity.getId());
			} catch (Exception e) {
				handleExceptionForBatchOperation(entity, e, context.getBatchOperationFailReturnOption(), "delete", result, fails);
			}
		}
		
		if (BatchOperationFailReturnOption.ALL.equals(context.getBatchOperationFailReturnOption()) && !fails.isEmpty()) {
			throw new CmsEntMgrException(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE, MessageFormat.format(
                      "batch delete failure: {0}. The following entities have been deleted: {1}",
                      Arrays.toString(fails.toArray()), Arrays.toString(result.toArray())));
		}
		
		return result;
	}

	private PersistenceContext getWritePersistenceContext(String repoName, IBranch branchEntity, EntityContext context) {
		Repository repo = repositoryService.getRepository(repoName);
		IMetadataService metaService = repo.getMetadataService();
		// write
		ConsistentPolicy policy = context.getConsistentPolicy();
		if (policy == null) {
			policy = ConsistentPolicy.PRIMARY;
		} else {
			policy = new ConsistentPolicy(ConsistentPolicy.PRIMARY.getName(), policy.getWriteConcern());
		}
		PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(metaService,
				branchEntity, policy, context.getRegistration(), context.isFetchFieldProperty(), context.getDbConfig(),
				context.getAdditionalCriteria());
		return persistenceContext;
    }

//    private PersistenceContext getReadPersistenceContext(String repoName, EntityContext context, IBranch branchEntity) {
//        Repository repo = repositoryService.getRepository(repoName);
//        PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
//                repo.getMetadataService(), branchEntity, context.getConsistentPolicy(), context.getRegistration(),
//                context.isFetchFieldProperty(), context.getDbConfig(), context.getAdditionalCriteria());
//        return persistenceContext;
//    }

	public void setCallback(IEntityOperationCallback callback) {
		this.callback = callback;
	}

	@Override
	public IEntity get(IEntity queryEntity, EntityContext context) {
		return entityGetManager.get(queryEntity, context);
//		String repoName = queryEntity.getRepositoryName();
//		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, queryEntity.getBranchId(), context);
//		PersistenceContext persistenceContext = getReadPersistenceContext(repoName, context, branchEntity);
//
//		MetaClass queryMeta = queryEntity.getMetaClass();
//		persistenceContext.addQueryFields(getQueryFields(queryMeta, context.getQueryFields()));
//		IEntity findEntity = findAndCopy(branchEntity, queryEntity, persistenceContext);
//
//		if (findEntity == null || findEntity.getStatus() != StatusEnum.ACTIVE) {
//			return null;
//		}
//        return findEntity;
	}

	@Override
	public void replace(IEntity queryEntity, IEntity entity, EntityContext context) {
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		// find the nearest ancestor branch where we can find the entity to be
		// modified and copy from parent branch
		// for main branch, it's just a get
		IEntity foundEntity = entityGetManager.findAndCopy(branchEntity, queryEntity, persistenceContext);

		entity.setParentVersion(IEntity.START_VERSION);

		entityHelper.checkACL(callback, entity, foundEntity, null, branchEntity, Operation.REPLACE, context, persistenceContext);
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.REPLACE, true,
				Collections.<IEntity> emptyList(), false);

		entityHelper.checkNoInnerInMeta(entity.getMetaClass());

		persistenceService.replace(entity, persistenceContext);
	}

	@Override
	public void modify(IEntity queryEntity, IEntity entity, EntityContext context) {
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		// find the nearest ancestor branch where we can find the entity to be
		// modified and copy from parent branch
		IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);
		// now we can modify the given entity to current branch
		int parentVersion = foundEntity.getParentVersion();
		entity.setParentVersion(parentVersion);

		entityHelper.checkACL(callback, entity, foundEntity, null, branchEntity, Operation.MODIFY, context, persistenceContext);
		removeEmbedEntities(entity);
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.MODIFY, true,
				Collections.<IEntity> emptyList(), true);

		EntityComparator comparator = new EntityComparator(entity);
		foundEntity.traverse(comparator);
		if (!comparator.getDiffResult()) {
			LOGGER.info(MessageFormat.format(
					"Ignore modify that has no diff with current db entity, metaclass {0}, entity id {1}!",
					entity.getType(), entity.getId()));
			context.setRequestTrackingCode(CMSTrackingCodeEnum.MODIFY_ENTITY_NOCHANGE_ACCEPTED);
			return;
		}

		persistenceService.modify(entity, persistenceContext);
	}

	private void removeEmbedEntities(IEntity entity) {
		MetaClass meta = entity.getMetaClass();
		Collection<String> fields = entity.getFieldNames();
		for (String fieldName : fields) {
			MetaField field = meta.getFieldByName(fieldName);
			if (field instanceof MetaRelationship
					&& (((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded)) {
				entity.removeField(fieldName);
			}
		}

	}

	@Override
	public void delete(IEntity queryEntity, EntityContext context) {
		String repoName = queryEntity.getRepositoryName();
		String branchId = queryEntity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		final IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);

		/*
		 * -- For entity delete, do real delete on main branch. But for
		 * sub(non-main) branch, only do mark delete.
		 * 
		 * -- For auditing purpose(cms-2532): the root delete command would
		 * perform: mark delete, then real delete. Embed entity delete operation
		 * is an exception. As for embed entity no diff between mark delete and
		 * delete
		 */
		MetaClass meta = foundEntity.getMetaClass();
		boolean isEmbedEntity = meta.isEmbed();
		if (isEmbedEntity) {
			foundEntity.setVersion(context.getVersion());
		}

		entityHelper.checkACL(callback, foundEntity, foundEntity, null, branchEntity, Operation.DELETE, context, persistenceContext);
		preOperationCheck(foundEntity, foundEntity, context, persistenceContext, Operation.DELETE, false,
				Collections.<IEntity> emptyList(), false);

		// For idempotence, delete the inner relation in host entity before
		// deleting the entity itself
		if (meta.isInner()) {
			String host = foundEntity.getHostEntity();
			String[] idParts = entityHelper.splitPath(host, false);
			IEntity hEntity = entityGetManager.getHostEntity(foundEntity, host, idParts, persistenceContext, false);
			if (hEntity != null) {
			    MetaRelationship innerRel = getHostInnerRelation(hEntity.getMetaClass(), idParts, false);
			    if (innerRel != null) {
				    String fieldName = innerRel.getName();
				    hEntity.addFieldValue(fieldName, queryEntity);
				    // make sure no exception if the entity is not found
				    IEntity hostEntity = entityGetManager.findAndCopy(branchEntity, hEntity, persistenceContext);
				    if (hostEntity != null && isInnerEntityExisted(hostEntity, queryEntity, fieldName)) {
				        pullFieldInternal(hostEntity, hEntity, fieldName, context, persistenceContext, false);
				    }
			    }
			}
		}

		cascadingDelete(foundEntity, branchEntity, persistenceContext, true);
	}

	private MetaRelationship getHostInnerRelation(MetaClass hostMeta, String[] idParts, boolean check) {
		if (!(idParts.length > 2)) {
			// never be true for check=true case
			return null;
		}

		String fieldName = idParts[idParts.length - 1];
		MetaField field = hostMeta.getFieldByName(fieldName);
		if (field instanceof MetaRelationship && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Inner) {
			return (MetaRelationship) field;
		}
		if (check) {
			throw new CmsEntMgrException(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, MessageFormat.format(
					"the relationship {0} is not a valid inner relationship in meta class {1}", fieldName, hostMeta.getName()));
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private void cascadingDelete(IEntity entity, IBranch branchEntity, PersistenceContext persistenceContext, boolean deleting) {
		if (entity == null) {
			// if entity not found, just return.
			return;
		}
		MetaClass meta = entity.getMetaClass();
		for (MetaRelationship rel : meta.getToReference()) {
			if (rel.getRelationType() == RelationTypeEnum.Inner || rel.getRelationType() == RelationTypeEnum.Embedded) {
				List<IEntity> entities = (List<IEntity>) entity.getFieldValues(rel.getName());
                for (IEntity e : entities) {
					IEntity qEntity = createQueryEntity(branchEntity.getId(), e.getId(), rel.getRefMetaClass());
					IEntity en = entityGetManager.findAndCopy(branchEntity, qEntity, persistenceContext);
					cascadingDelete(en, branchEntity, persistenceContext, rel.getRelationType() == RelationTypeEnum.Inner);
				}
			}
		}

		if (deleting) {
			if (!meta.isEmbed()) {
				persistenceService.markDeleted(entity, persistenceContext);
			}
			persistenceService.delete(entity, persistenceContext);
		}
	}

	@Override
	public void modifyField(IEntity queryEntity, IEntity entity, String fieldName, EntityContext context) {
		context.setModifyFieldName(fieldName);
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		// find the nearest ancestor branch where we can find the entity to be
		// modified and copy from parent branch
		IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);

		// check before action
	    entityHelper.preFieldOperationCheck(entity, fieldName, true);
	    entityHelper.checkACL(callback, entity, foundEntity, fieldName, branchEntity, Operation.MODIFY, context, persistenceContext);
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.MODIFY, true,
				Collections.<IEntity> emptyList(), true);

		pushFieldInternal(foundEntity, entity, fieldName, persistenceContext, context);
	}

//	private void preFieldOperationCheck(IEntity entity, String fieldName, boolean isModifyField) {
//		MetaField mField = entity.getMetaClass().getFieldByName(fieldName);
//		if (mField instanceof MetaRelationship) {
//			MetaRelationship rel = (MetaRelationship) mField;
//			if (rel.getRelationType() == RelationTypeEnum.Inner) {
//				throw new CmsEntMgrException(EntMgrErrCodeEnum.INNER_RELATIONSHIP_IMMUTABLE, MessageFormat.format(
//						"inner relationship {0} is immutable", fieldName));
//			}
//			// modify field doesn't allowed on embed relationship
//			// FIXME: should we disallow the "delete field" on embed
//			// relationship?
//			if (rel.getRelationType() == RelationTypeEnum.Embedded && isModifyField) {
//				throw new CmsEntMgrException(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, MessageFormat.format(
//						"embed relationship {0} is immutable", fieldName));
//			}
//		}
//	}

	@Override
	@SuppressWarnings("unchecked")
	public void casModifyField(IEntity queryEntity, IEntity entity, String fieldName, Object oldValue,
			EntityContext context) {
		String repoName = entity.getRepositoryName();
		String branchId = entity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);
		IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);

		entityHelper.checkACL(callback, entity, foundEntity, fieldName, branchEntity, Operation.MODIFY, context, persistenceContext);
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.MODIFY, false,
				Collections.<IEntity> emptyList(), false);

		List<Object> originalValues = (List<Object>) foundEntity.getFieldValues(fieldName);
		if (originalValues.size() == 1) {
			Object original = originalValues.get(0);
			if (!original.equals(oldValue)) {
				throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED,
						"The original value in DB: " + original);
			}
		} else {
			throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED,
					"The value in DB is empty or more than one");
		}

		pushFieldInternal(foundEntity, entity, fieldName, persistenceContext, context);
	}

	private void pushFieldInternal(IEntity foundEntity, IEntity entity, String fieldName, PersistenceContext persistenceContext, EntityContext entityContext) {
	    AbstractEntityFieldMerger merger = null;
	    boolean hasExpression = entity.getMetaClass().hasExpressionFields();
		if (hasExpression) {
            // expression entities need to compute the whole field
		    merger = targetFieldPushMerger;
		} else {
		    // calculate the delta by delta merger
            merger = deltaFieldPushMerger;
		}

		boolean changed = merger.mergeEntityOnField(entity, fieldName, foundEntity);
		if (!changed) {
		    LOGGER.info(MessageFormat
		            .format("Ignore modify entity field that has no diff with current db entity, metaclass {0}, entity id {1}, field {2}!", entity.getType(), entity.getId(), fieldName));
		    entityContext.setRequestTrackingCode(CMSTrackingCodeEnum.MODIFY_ENTITY_NOCHANGE_ACCEPTED);
		    return;
		}
		if (hasExpression) {
		    persistenceService.modify(entity, persistenceContext);
		} else {
		    persistenceService.modifyField(entity, fieldName, persistenceContext);
		}
	}

    @Override
    public void pullField(IEntity queryEntity, IEntity entity, String fieldName, EntityContext context) {
        String repoName = entity.getRepositoryName();
        String branchId = entity.getBranchId();
        IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
        PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

        IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);
        if (!foundEntity.hasField(fieldName)) {
            return;
        }

        entityHelper.checkACL(callback, entity, foundEntity, fieldName, branchEntity, Operation.MODIFY, context, persistenceContext);
        entityHelper.preFieldOperationCheck(entity, fieldName, false);
       
        pullFieldInternal(foundEntity, entity, fieldName, context, persistenceContext, true);
    }
    
    private void pullFieldInternal(IEntity foundEntity, IEntity entity, String fieldName, EntityContext context,
            PersistenceContext persistenceContext, boolean needInnerCheck) {
        context.setModifyFieldName(fieldName);
        Operation op = Operation.MODIFY;
        if (!needInnerCheck) {
        	op = Operation.DELETE;
        }
        preOperationCheck(entity, foundEntity, context, persistenceContext, op, false,
                Collections.<IEntity> emptyList(), needInnerCheck);

        boolean changed = targetFieldPullMerger.mergeEntityOnField(entity, fieldName, foundEntity);
        if (!changed) {
            return;
        }

        entity.setMetaVersion(entity.getMetaClass().getVersion());

        if (entity.hasField(fieldName)) {
            persistenceService.modify(entity, persistenceContext);
        } else {
            persistenceService.deleteField(entity, fieldName, persistenceContext);            
        }
    }
    
	@Override
	public void deleteField(IEntity queryEntity, String fieldName, EntityContext context) {
	    context.setModifyFieldName(fieldName);
		String repoName = queryEntity.getRepositoryName();
		String branchId = queryEntity.getBranchId();
		IBranch branchEntity = branchHelper.getAndCheckCurrentBranch(repoName, branchId, context);
		PersistenceContext persistenceContext = getWritePersistenceContext(repoName, branchEntity, context);

		IEntity foundEntity = entityHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);
		if (!foundEntity.hasField(fieldName)) {
			return;
		}

		entityHelper.checkACL(callback, queryEntity, foundEntity, fieldName, branchEntity, Operation.MODIFY, context, persistenceContext);
		entityHelper.preFieldOperationCheck(queryEntity, fieldName, false);
        
		deleteFieldInternal(foundEntity, branchEntity, queryEntity, fieldName, context, persistenceContext, true);
	}

	@SuppressWarnings("unchecked")
    private void deleteFieldInternal(IEntity foundEntity, IBranch branchEntity, IEntity entity, String fieldName, EntityContext context,
			PersistenceContext persistenceContext, boolean needInnerCheck) {
		preOperationCheck(entity, foundEntity, context, persistenceContext, Operation.MODIFY, false,
				Collections.<IEntity> emptyList(), needInnerCheck);
		// cascading handle for embed delete
        MetaField field = foundEntity.getMetaClass().getFieldByName(fieldName);
        if (field instanceof MetaRelationship) {
            MetaRelationship rel = (MetaRelationship) field;
            if (rel.getRelationType() == RelationTypeEnum.Embedded) {
                List<IEntity> embedEntities = (List<IEntity>) foundEntity.getFieldValues(fieldName);
                for (IEntity embedEntity : embedEntities) {
                    cascadingDelete(embedEntity, branchEntity, persistenceContext, false);
                }
            }
        }

		entity.removeField(fieldName);
		entity.setMetaVersion(entity.getMetaClass().getVersion());
		
		persistenceService.deleteField(entity, fieldName, persistenceContext);
	}

	private void preOperationCheck(IEntity entity, IEntity foundEntity, EntityContext context,
			PersistenceContext persistenceContext, Operation op, boolean needDanglingCheck, List<IEntity> entities,
			boolean needInnerCheck) {
		if (op != Operation.CREATE) {
			entityHelper.checkVersion(entity, foundEntity);
		}

		if (op == Operation.DELETE) {
			refInetegrationValidator.checkReferenceIntegrity(foundEntity, persistenceContext);
		}

		EntityValidator visitor = new EntityValidator(foundEntity);

		fillAction.init(op, context.getUserId(), context.getComment(), context.getModifier());
		visitor.addAction(fillAction);
		
		systemLimitationHelper.checkSystemLimitation(entity, foundEntity, context, persistenceContext, op);

		if (needDanglingCheck) {
			Set<String> entityIds = null;
			if (!entities.isEmpty()) {
				entityIds = new HashSet<String>();
				for (IEntity en : entities) {
					entityIds.add(en.getId());
				}
			} else {
				entityIds = Collections.<String> emptySet();
			}

			danglingAction.init(persistenceService, entityIds, persistenceContext);
			visitor.addAction(danglingAction);
		}

		if (needInnerCheck) {
			visitor.addAction(innerAction);
		}

		entity.traverse(visitor);
	}
	
//	private void checkSystemLimitation(IEntity entity, IEntity foundEntity, EntityContext context,
//			PersistenceContext persistenceContext, Operation op) {
//		if (!Operation.DELETE.equals(op)) {
//			if (Operation.MODIFY.equals(op)) {
//				ModifyAction modifyAction = context.getModifyAction();
//				if (modifyAction != null && (ModifyAction.DELETEFIELD.equals(modifyAction) || ModifyAction.PULLFIELD.equals(modifyAction))) {
//					return;
//				}
//			}
//			Repository repo = checkHelper.getRepository(persistenceContext);
//			RepositoryOption options = repo.getOptions();
//			Long maxDocumentSize = options.getMaxDocumentSize();
//			if (maxDocumentSize != null) {
//			    checkHelper.checkDocumentSize(op, entity, foundEntity, maxDocumentSize);
//			}
//			
//			Integer maxIndexedArraySize = options.getMaxIndexedArraySize();
//			if (maxIndexedArraySize != null) {
//				checkIndexedArraySize(entity, foundEntity, context, persistenceContext, op, maxIndexedArraySize);
//			}
//		}
//	}

//	private void checkIndexedArraySize(IEntity entity, IEntity foundEntity, EntityContext context,
//			PersistenceContext persistenceContext, Operation op, Integer maxIndexedArraySize) {
//		IEntity finalEntity = new JsonEntity((JsonEntity) entity);
//		if (Operation.MODIFY.equals(op)) {
//			ModifyAction modifyAction = context.getModifyAction();
//			finalEntity = checkHelper.getMergedEntity(finalEntity, foundEntity, context, modifyAction);
//		}
//		
//		// check indexed array size of entity self
//		EntityValidator entityVisitor = new EntityValidator(entity);
//		IndexedArraySizeCheckAction indexedArraySizeCheckAction = new IndexedArraySizeCheckAction(
//				maxIndexedArraySize);
//		entityVisitor.addAction(indexedArraySizeCheckAction);
//		finalEntity.traverse(entityVisitor);
//		
//		// check indexed array size of host entity if need
//		MetaClass metaClass = finalEntity.getMetaClass();
//		IEntity refEntity = null;
//		if (Operation.CREATE.equals(op)) {
//			refEntity = entity;
//		} else {
//			refEntity = foundEntity;
//		}
//		if (metaClass.isInner()) {
//			IEntity hostEntity = getUnMergedHostEntity(refEntity, context, persistenceContext);
//			if (hostEntity != null) {
//				String path = refEntity.getHostEntity();
//				String[] idParts = path.split(IEntity.ID_SEP_REG);
//				String hostFieldName = idParts[idParts.length - 1];
//				checkHelper.checkIndexedArraySizeForHostEntity(op, maxIndexedArraySize, hostEntity, hostFieldName);
//			} else {
//				throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, String.format(
//						"Could not find host entity for inner class %s!", metaClass.getName()));
//			}
//		} else if (metaClass.isEmbed()) {
//			IEntity parentEntity = getUnMergedParentEntityForEmbed(refEntity, context, persistenceContext);
//			if (parentEntity != null) {
//				String embedId = checkHelper.getEmbedEntityId(refEntity, context);
//				String parentFieldName = AbstractEntityIDHelper.getParentFieldName(embedId);
//				checkHelper.checkIndexedArraySizeForHostEntity(op, maxIndexedArraySize, parentEntity, parentFieldName);
//			} else {
//				throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, String.format(
//						"Could not find parent entity for embed class %s!", metaClass.getName()));
//			}
//		}
//	}
//	
//	private IEntity getUnMergedHostEntity(IEntity entity, EntityContext context, PersistenceContext persistenceContext) {
//		String repoName = entity.getRepositoryName();
//		String branchId = entity.getBranchId();
//		IBranch branchEntity = getAndCheckCurrentBranch(repoName, branchId, context);
//		
//		String host = entity.getHostEntity();
//		if (StringUtils.isNullOrEmpty(host)) {
//			host = context.getPath();
//			entity.setHostEntity(host);
//		}
//
//		String[] idParts = checkHelper.splitPath(host, true);
//		IEntity queryEntity = createHostEntity(entity, host, idParts, persistenceContext, true);
//		return checkHelper.getAndCheckExsitingEntity(queryEntity, branchEntity, persistenceContext);
//	}
	
//	private IEntity createHostEntity(IEntity entity, String host, String[] idParts,
//			PersistenceContext persistenceContext, boolean check) {
//		if (!(idParts.length > 1)) {
//			// never be true for check=true case
//			return null;
//		}
//		
//		IMetadataService metaService = persistenceContext.getMetadataService();
//		
//		MetaClass hostMeta = null;
//	    String hostId = idParts[1];
//		if (idParts.length > 3) {
//		    int pos = host.lastIndexOf(IEntity.ID_SEP);		    
//		    hostId = host.substring(0, pos);
//		    
//		    MetaClass meta = metaService.getMetaClass(idParts[0]);
//		    for (int i = 2; i < idParts.length - 1; i += 2) {
//		        String name = idParts[i];
//		        MetaRelationship field = (MetaRelationship)meta.getFieldByName(name);
//		        meta = field.getRefMetaClass();
//		    }
//		    hostMeta = meta;
//		} else {
//		    hostMeta = metaService.getMetaClass(idParts[0]);
//		}
//		
//		if (hostMeta == null) {
//			if (check) {
//				throw new CmsEntMgrException(EntMgrErrCodeEnum.INVALID_HOST_ENTITY_PATH, MessageFormat.format(
//						"could not find host Meta Class in path {0}", entity.getHostEntity()));
//			} else {
//				return null;
//			}
//		}
//
//        IEntity qEntity = new JsonEntity(hostMeta);
//		qEntity.setBranchId(entity.getBranchId());
//		qEntity.setId(hostId);
//		return qEntity;
//	}

//	private IEntity getUnMergedParentEntityForEmbed(IEntity entity, EntityContext context, PersistenceContext persistenceContext) {
//		String embedId = checkHelper.getEmbedEntityId(entity, context);
//		IMetadataService metaService = persistenceContext.getMetadataService();
//		
//		// get parent entity
//		MetaClass parentMetaClass = AbstractEntityIDHelper.getParentMetaClass(embedId, metaService);
//		String parentId = AbstractEntityIDHelper.getParentId(embedId);
//		
//		CheckConditions.checkNotNull(parentMetaClass);
//		JsonEntity queryEntity = new JsonEntity(parentMetaClass);
//		queryEntity.setId(parentId);
//
//		queryEntity.setBranchId(entity.getBranchId());
//		queryEntity.setEmbedPath(parentId);
//		
//		return this.get(queryEntity, context);
//	}
}