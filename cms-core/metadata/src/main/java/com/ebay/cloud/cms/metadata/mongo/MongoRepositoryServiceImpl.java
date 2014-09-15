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

package com.ebay.cloud.cms.metadata.mongo;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.lock.ICMSLock;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.exception.MongoOperationException;
import com.ebay.cloud.cms.metadata.exception.RepositoryExistsException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.Repository.AccessType;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.IMetadataHistoryService;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.ExpirableCache;
import com.ebay.cloud.cms.utils.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * MongoRepositoryServiceImpl
 * 
 * 
 * 
 * @author liubo
 * 
 */
public class MongoRepositoryServiceImpl implements IRepositoryService {

	private static final Logger logger = LoggerFactory.getLogger(MongoRepositoryServiceImpl.class);

	private static ObjectConverter<Repository> repositoryConverter = new ObjectConverter<Repository>();

	private static ObjectConverter<RepositoryOption> repositoryOptionConverter = new ObjectConverter<RepositoryOption>();

	private final Mongo mongo;

	private final DBCollection repoCollection;

	private ExpirableCache<Repository> cache;
	private final int maxCacheSize;
	private final int cacheExpiredTime;
	private final int collectionCountCacheSize;
	private final int collectionCountCacheExpiredTime;

	private final ICMSLock metadataLock;
	
	private IMetadataHistoryService historyService;

	public MongoRepositoryServiceImpl(Mongo mongo, int maxCacheSize, int cacheExpiredTime,
			int collectionCountCacheSize, int collectionCountCacheExpiredTime, ICMSLock metadataLock,
			WriteConcern writeConcern) {
	    CheckConditions.checkNotNull(mongo, "mongo can not be null");
	    CheckConditions.checkArgument(maxCacheSize > 0, "maximumSize can not be negtive value");
	    CheckConditions.checkArgument(cacheExpiredTime > 0, "expireAfterSeconds can not be negtive value");
	    CheckConditions.checkArgument(metadataLock != null, "metadataLock should not be null");

		this.metadataLock = metadataLock;

		this.mongo = mongo;

		this.maxCacheSize = maxCacheSize;
		this.cacheExpiredTime = cacheExpiredTime;
		cache = new ExpirableCache<Repository>(maxCacheSize, cacheExpiredTime);

		this.collectionCountCacheSize = collectionCountCacheSize;
		this.collectionCountCacheExpiredTime = collectionCountCacheExpiredTime;

		prepareSystemDB(mongo);

		repoCollection = this.mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.REPOSITORY_COLL);
		// read from primary only
		repoCollection.setReadPreference(ReadPreference.primary());
		repoCollection.setWriteConcern(writeConcern);
	}

	public static void prepareSystemDB(Mongo mongo) {
		DBCollection repoCollection = mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.REPOSITORY_COLL);

		MongoUtils.ensureIndex(repoCollection, Repository.REPOSITORY_FIELD_NAME, true, false);

		repoCollection.setWriteConcern(WriteConcern.SAFE);
	}

	private void createServiceForRepository(Repository repo) {
        IMetadataService metadataService = new MongoMetadataServiceImpl(mongo, maxCacheSize, cacheExpiredTime,
                collectionCountCacheSize, collectionCountCacheExpiredTime, repo, metadataLock);

        metadataService.setMetaHistoryService(historyService);
        repo.setMetadataService(metadataService);
        repo.setRepositoryService(this);
    }

	@Override
	public Repository createRepository(Repository repo) {
		return createRepository(repo, WriteConcern.SAFE);
	}

	@Override
	public Repository createRepository(Repository repo, WriteConcern writeConcern) {
	    CheckConditions.checkNotNull(repo, "repository can not be null");
	    CheckConditions.checkNotNull(repo.getRepositoryName(), "repository name can not be null");
		String repositoryName = repo.getRepositoryName();

		try {
			metadataLock.lock();

			try {
				repoCollection.insert(repositoryConverter.toBson(repo));
			} catch (MongoException.DuplicateKey e) {
				throw new RepositoryExistsException(repositoryName);
			}

			// build index
			try {
				DBCollection metaCollection = mongo.getDB(repo.getRepositoryName()).getCollection(
						CMSConsts.METACLASS_COLL);
				metaCollection.setReadPreference(ReadPreference.primary());
				MongoUtils.ensureIndex(metaCollection, MetaClass.NAME, true, false);
				MongoUtils.ensureIndex(metaCollection, MetaClass.PLURAL_NAME, true, true);

				createServiceForRepository(repo);
				// init created system meta class
				HistoryMetaClass.createHistoryMetaClass(repo);
				BranchMetaClass.createBranchMetaClass(repo);
			} catch (Throwable e) {
				logger.warn("create repo db failed, remove the repo entry in repository coll", e);
				try {
					repoCollection.remove(repositoryConverter.toBson(repo));
				} catch (Exception ee) {
					logger.error("remove repo db failed", ee);
				}
				throw new MetaDataException(MetaErrCodeEnum.CREATE_REPOSITORY_ERROR, "create repository " + repositoryName + " failed", e);
			}

			refreshRepositoryCache();
			return repo;
		} catch (InterruptedException e) {
			logger.info("lock interrupted for createRepository " + repositoryName);
			throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for createRepository " + repositoryName, e);
		} finally {
			metadataLock.unlock();
		}
	}

	@Override
	public void updateRepository(Repository repo) {
	    CheckConditions.checkNotNull(repo, "repository can not be null");
	    CheckConditions.checkNotNull(repo.getRepositoryName(), "repository name can not be null");
		String repositoryName = repo.getRepositoryName();
		try {
			metadataLock.lock();
			DBObject qryDbo = new BasicDBObject();
			qryDbo.put(Repository.REPOSITORY_FIELD_NAME, repositoryName);
			// only support admin change now
			DBObject updateDbo = new BasicDBObject();
			DBObject setDbo = new BasicDBObject();
			if (repo.getRepositoryAdmin() != null) {
				setDbo.put(Repository.REPOSITORY_FIELD_ADMIN_NAME, repo.getRepositoryAdmin());
				setDbo.put(Repository.REPOSITORY_FIELD_TYPE_NAME, repo.getAccessType().name());
			}
			if (repo.getOptions() != null) {
				setDbo.put(Repository.REPOSITORY_FIELD_OPTIONS_NAME,
						repositoryOptionConverter.toBson(repo.getOptions()));
			}
			if (!setDbo.keySet().isEmpty()) {
				updateDbo.put("$set", setDbo);
				WriteResult result = repoCollection.update(qryDbo, updateDbo);
				if (result.getN() != 1) {
					throw new RepositoryNotExistsException(repositoryName);
				}
			}
            refreshRepositoryCache();
		} catch (InterruptedException e) {
			logger.info("lock interrupted for createRepository " + repositoryName);
			throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for createRepository " + repositoryName, e);
		} finally {
			metadataLock.unlock();
		}
	}

	@Override
	public Repository getRepository(String repositoryName) {
		Repository repo = null;
		BasicDBObject query = new BasicDBObject();
		query.put(Repository.REPOSITORY_FIELD_NAME, repositoryName);
        query.put(Repository.STATE_FIELD, Repository.StateEnum.normal.toString());

        boolean suggestRefresh = false;
		DBObject object = repoCollection.findOne(query);
		if (object != null) {
			repo = cache.getObject(repositoryName);
			if (repo == null) {
                suggestRefresh = true;
			}
        } else if (cache.getObject(repositoryName) != null) {
			suggestRefresh = true;
		}
        // find something different between the cache and the read db.
		if (suggestRefresh) {
		    refreshRepositoryCache();
		    repo = cache.getObject(repositoryName);
		}

		if (repo == null) {
			throw new RepositoryNotExistsException(repositoryName);
		}
		return repo;
	}

    private void refreshRepositoryCache() {
        getRepositories(new MetadataContext(true, true));
    }

	@Override
	public List<Repository> getRepositories(MetadataContext ctx) {
        MetadataContext context = ctx != null ? ctx : new MetadataContext();
        if (!context.isRefreshRepsitory()) {
            return cache.values();
        }

	    ExpirableCache<Repository> newCache = new ExpirableCache<Repository>(maxCacheSize, cacheExpiredTime);
		BasicDBObject query = new BasicDBObject();
		query.put(Repository.STATE_FIELD, Repository.StateEnum.normal.toString());

		List<Repository> result = new ArrayList<Repository>();
		DBCursor cursor = repoCollection.find(query);
		while (cursor.hasNext()) {
			DBObject object = cursor.next();
			Repository r = repositoryConverter.fromBson(object, Repository.class);
			createServiceForRepository(r);
			result.add(r);
			newCache.putObject(r.getRepositoryName(), r);
		}
        cache = newCache;
		return result;
	}

	@Override
	public void deleteRepository(String repositoryName) {
		/*
		 * TODO: implement real delete repo
		 * 
		 * candidate algorithms: 1. mark repo's state as "deleting", background
		 * batch job will delete this repo later(batch will do it after cache
		 * time expired" repo name will be reusable after batch delete done
		 * cons: same repo name can only be reused after "long" time, batch copy
		 * may affect performance of mongo pros: simple
		 * 
		 * 2. 2.1 mark repo's state as "deleting" 2.2 sync other cms servers to
		 * clean cache 2.3 wait for a short safe time (some seconds?) other cms
		 * server's using repo out of usage (some server may still writing
		 * metaclass into this repo) 2.4 start copy and delete batch job 2.5
		 * mark repo's state to "deleted", repo name is reusable cons: repo
		 * name's reuse time is shorter than algorithm 1, but still long (short
		 * safe time plus copy repo's time), batch copy may affect performance
		 * of mongo pros:
		 * 
		 * 3. use different db name for repo name 3.1 mark repo's state as
		 * "deleting" 3.2 sync other cms servers to clean repo cache 3.3 mark
		 * repo's state to "deleted" (in using repositories in other servers
		 * will write metaclass into the deleted repo's db, no harm) cons:
		 * little complex pros: repo name can be reused immediately, no need to
		 * copy repo's db (batch copy may affect performance of mongo)
		 */

		try {
			metadataLock.lock();
			// BasicDBObject query = new BasicDBObject();
			// query.put(REPOSITORY_FIELD_NAME, repositoryName);
			// query.put(STATE_FIELD, Repository.StateEnum.normal.toString());
			//
			// BasicDBObject update = new BasicDBObject();
			// BasicDBObject ups = new BasicDBObject();
			// ups.put(STATE_FIELD, Repository.StateEnum.deleting.toString());
			// ups.put("deletingTime", new Date());
			//
			// update.append(MongoOperand.set, ups);
			//
			// try {
			// boolean updated = MongoUtils.wrapperUpdate(repoCollection, query,
			// update);
			// if (!updated) {
			// throw new RepositoryNotExistsException(repositoryName);
			// }
			// } catch (MongoException e) {
			// throw new MongoOperationException(e);
			// }

			BasicDBObject query = new BasicDBObject();
			query.put(Repository.REPOSITORY_FIELD_NAME, repositoryName);
			query.put(Repository.STATE_FIELD, Repository.StateEnum.normal.toString());

			try {
				repoCollection.remove(query);
				mongo.dropDatabase(repositoryName);
			} catch (MongoException e) {
				throw new MongoOperationException(e);
			}
			refreshRepositoryCache();
		} catch (InterruptedException e) {
			logger.info("lock interrupted for delete " + repositoryName);
			throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for delete " + repositoryName, e);
		} finally {
			metadataLock.unlock();
		}

	}

	@Override
	public List<Repository> getRepositories(AccessType type) {
		List<Repository> allRepos = cache.values();
		List<Repository> result = new ArrayList<Repository>();
		for (Repository repo : allRepos) {
			if (repo.getAccessType() == type) {
				result.add(repo);
			}
		}
		return result;
	}
	
	@Override
    public void setHistoryService(IMetadataHistoryService historyService) {
        this.historyService = historyService;
        List<Repository> repositories = cache.values();
        for(Repository repo : repositories) {
            IMetadataService metaService = repo.getMetadataService();
            metaService.setMetaHistoryService(historyService);
            
            // check if the HistoryMetaClass is the latest
            MetaClass historyMeta = metaService.getMetaClass(HistoryMetaClass.NAME);
            if (historyMeta == null) {
                HistoryMetaClass.createHistoryMetaClass(repo);
            }
            else if (historyMeta.getFieldByName(HistoryMetaClass.EntityParentVersion) == null) {
                MetadataContext context = new MetadataContext();
                context.setSourceIp("localhost");
                context.setSubject("cmsdb-core");
                
                metaService.deleteMetaClass(HistoryMetaClass.NAME, context);
                HistoryMetaClass.createHistoryMetaClass(repo);
            }
            else {
            	IndexInfo ii = new IndexInfo(HistoryMetaClass.INDEX_NAME);
                ii.addKeyField(HistoryMetaClass.EntityId);
                ii.addKeyField(HistoryMetaClass.EntityVersion);
                historyMeta.getOptions().addIndex(ii);
                
            	historyService.ensureHistoryIndex(repo, historyMeta);
            }
        }
    }
}
