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

package com.ebay.cloud.cms.dal.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.CollectionFinder;
import com.ebay.cloud.cms.dal.search.IEntityFactory;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.mongo.MongoDataSource;

/**
 * Proxy persistence service implementation
 * 
 * FIXME : using spring for AOP??
 * 
 * @author liasu
 * 
 */
public class PersistenceService implements IPersistenceService {

    public static class Registration {
        public final String                            registrationId;
        public final IPersistenceService               service;
        public final Class<? extends IEntity>          entityClass;
        public final IEntityFactory<? extends IEntity> factory;
        public final AbstractEntityIDHelper            entityHelper;
        public final ISearchStrategy                   searchStrategy;
        public final CollectionFinder                  collectionFinder;

        public Registration(String id, IPersistenceService service, Class<? extends IEntity> entityClass,
                IEntityFactory<? extends IEntity> factory, ISearchStrategy strategy, AbstractEntityIDHelper helper,
                CollectionFinder finder) {
            this.registrationId = id;
            this.entityClass = entityClass;
            this.service = service;
            this.entityHelper = helper;
            this.factory = factory;
            this.searchStrategy = strategy;
            this.collectionFinder = finder;
        }
    }

    private static class ExecDelegate<T> {
        public T exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
            return null;
        }
        public T batchExec(Registration r, List<IEntity> entity, PersistenceContext context) {
            return null;
        }
        public T ensureIndex(Registration r, PersistenceContext context) {
            return null;
        }
    }

    private static final Logger       logger        = LoggerFactory.getLogger(PersistenceService.class);

    private final Map<String, Registration> registrations = new ConcurrentHashMap<String, PersistenceService.Registration>();
    
    private final MongoDataSource dataSource;

    public PersistenceService(MongoDataSource dataSource, List<Registration> registration) {
        this.dataSource = dataSource;
        for (Registration r : registration) {
            registrations.put(r.registrationId, r);
        }
    }

    @Override
    public String create(IEntity entity, PersistenceContext context) {
        ExecDelegate<String> delegate = new ExecDelegate<String>() {
            public String exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                String id = r.service.create(entity, context);
                givenEntity.setId(id);
                return id;
            };
        };
        return deletgateExec(entity, context, delegate);
    }

    private <T> T deletgateExec(IEntity givenEntity, PersistenceContext context, ExecDelegate<T> exec) {
        Registration main = context.getRegistration();
        
        IEntity e = convertEntity(main.entityClass, givenEntity, context);
        T result = exec.exec(main, givenEntity, e, context);

        Boolean flag = (Boolean) context.getDbConfig().get(CMSDBConfig.SYS_DAL_MIGRATION_DUAL_WRITE);
        if (flag) {
            for (Registration r : registrations.values()) {
                if (r.registrationId.equals(main.registrationId)) {
                    continue;
                }
                try {
                    IEntity converted = convertEntity(r.entityClass, givenEntity, context);
                    exec.exec(r, givenEntity, converted, context);
                } catch (Throwable t) {
                    logger.error(String.format("Dual write for entity id %s failed!", givenEntity.getId()), t);
                }
            }
        }
        return result;
    }

    private IEntity convertEntity(Class<? extends IEntity> targetClass, IEntity givenEntity, PersistenceContext context) {
        EntityMapper mapper = new EntityMapper(targetClass, givenEntity.getMetaClass(),
                context.isFetchFieldProperties());
        givenEntity.traverse(mapper);
        IEntity e = mapper.getBuildEntity();
        if (givenEntity.getEmbedPath() != null) {
            e.setEmbedPath(givenEntity.getEmbedPath());
        }
        return e;
    }

    private <T> T deletgateBatch(List<IEntity> givenEntity, PersistenceContext context, ExecDelegate<T> exec) {
        Registration main = context.getRegistration();
        T result = null;
        List<IEntity> converted = new ArrayList<IEntity>(givenEntity.size());
        for (IEntity given : givenEntity) {
            converted.add(convertEntity(main.entityClass, given, context));
        }
        result = exec.batchExec(main, converted, context);

        Boolean flag = (Boolean) context.getDbConfig().get(CMSDBConfig.SYS_DAL_MIGRATION_DUAL_WRITE);
        if (flag) {
            for (Registration r : registrations.values()) {
                if (r.registrationId.equals(main.registrationId)) {
                    continue;
                }
                List<IEntity> convertedEntities = new ArrayList<IEntity>(givenEntity.size());
                try {
                    for (IEntity given : givenEntity) {
                        IEntity newConverted = convertEntity(r.entityClass, given, context);
                        convertedEntities.add(newConverted);
                    }
                    exec.batchExec(r, convertedEntities, context);
                } catch (Throwable t) {
                    logger.error("Dual write batch failed!", t);
                }
            }
        }
        return result;
    }

    private void deletgateEnsureIndex(PersistenceContext context, ExecDelegate<Integer> delegate) {
        Registration main = context.getRegistration();
        delegate.ensureIndex(main, context);

        // per data flow, ensure index might not have db config set. In that case, directly read the config
        // from data source.
        CMSDBConfig config = context.getDbConfig();
        if (config == null) {
            config = new CMSDBConfig(dataSource);
        }
        Boolean flag = (Boolean) config.get(CMSDBConfig.SYS_DAL_MIGRATION_DUAL_WRITE);
        if (flag) {
            for (Registration r : registrations.values()) {
                if (r.registrationId.equals(main.registrationId)) {
                    continue;
                }
                try {
                    delegate.ensureIndex(r, context);
                } catch (Throwable t) {
                    logger.error("Dual ensure index failed!! ", t);
                }
            }
        }
    }

    @Override
    public List<String> batchCreate(List<IEntity> entities, PersistenceContext context) {
        ExecDelegate<List<String>> delegate = new ExecDelegate<List<String>>() {
            @Override
            public List<String> batchExec(Registration r, List<IEntity> converted, PersistenceContext context) {
                return r.service.batchCreate(converted, context);
            }
        };
        return deletgateBatch(entities, context, delegate);
    }

    @Override
    public void batchUpdate(List<IEntity> entities, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            @Override
            public Integer batchExec(Registration r, List<IEntity> converted, PersistenceContext context) {
                r.service.batchUpdate(converted, context);
                return 0;
            }
        };
        deletgateBatch(entities, context, delegate);
    }
    
    @Override
	public void batchDelete(List<IEntity> entities, PersistenceContext context) {
		if (entities == null || entities.isEmpty()) {
			return;
		}
		
		for (IEntity entity : entities) {
			delete(entity, context);
		}
	}

    @Override
    public IEntity get(IEntity queryEntity, PersistenceContext context) {
        // get is special, no need to convert the entity
        Registration main = context.getRegistration();
        IEntity result = null;
        IEntity getEntity = main.service.get(queryEntity, context);
        if (getEntity != null && !(getEntity instanceof JsonEntity)) {
            result = convertEntity(JsonEntity.class, getEntity, context);
        } else {
            result = getEntity;
        }
        return result;
    }

    @Override
    public void replace(IEntity entity, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.replace(entity, context);
                return 0;
            };
        };
        deletgateExec(entity, context, delegate);
    }

    @Override
    public void modify(IEntity entity, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.modify(entity, context);
                return 0;
            };
        };
        deletgateExec(entity, context, delegate);
    }

    @Override
    public void delete(IEntity entity, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.delete(entity, context);
                return 0;
            };
        };
        deletgateExec(entity, context, delegate);
    }

    @Override
    public void ensureIndex(final List<MetaClass> meta, PersistenceContext context, final boolean onMainBranch) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            @Override
            public Integer ensureIndex(Registration r, PersistenceContext context) {
                r.service.ensureIndex(meta, context, onMainBranch);
                return 0;
            }
        };
        deletgateEnsureIndex(context, delegate);
    }

    @Override
    public void markDeleted(IEntity entity, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.markDeleted(entity, context);
                return 0;
            };
        };
        deletgateExec(entity, context, delegate);
    }

    @Override
    public void modifyField(IEntity bsonEntity, final String fieldName, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.modifyField(entity, fieldName, context);
                return 0;
            };
        };
        deletgateExec(bsonEntity, context, delegate);
    }

    @Override
    public void deleteField(IEntity bsonEntity, final String fieldName, PersistenceContext context) {
        ExecDelegate<Integer> delegate = new ExecDelegate<Integer>() {
            public Integer exec(Registration r, IEntity givenEntity, IEntity entity, PersistenceContext context) {
                r.service.deleteField(entity, fieldName, context);
                return 0;
            };
        };
        deletgateExec(bsonEntity, context, delegate);
    }

    @Override
    public long count(final MetaClass meta, final List<String> refOids, final String branchId,
            PersistenceContext context) {
        Registration main = context.getRegistration();
        return main.service.count(meta, refOids, branchId, context);
    }

    @Override
    public List<Registration> getRegistrations() {
        return new ArrayList<Registration>(this.registrations.values());
    }

}
