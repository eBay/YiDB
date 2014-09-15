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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.lock.ICMSLock;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaClassExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.exception.MetaFieldExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaFieldNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MongoOperationException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaClassGraph;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaValidator;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.sequence.MongoSequence;
import com.ebay.cloud.cms.metadata.service.IMetadataHistoryService;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoOperand;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.ExpirableCache;
import com.ebay.cloud.cms.utils.MongoUtils;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

public class MongoMetadataServiceImpl implements IMetadataService {
    
    public static final Logger logger = LoggerFactory.getLogger(MongoMetadataServiceImpl.class);
    
    private static ObjectConverter<MetaClass> converter = new ObjectConverter<MetaClass>();
    
    private MetaClassGraph graph;
    private MongoMetaCacheManager cacheManager;
    
    private Mongo mongo;
    private Repository repo;
    private DBCollection collection;
    private IMetadataHistoryService historyService;
    
    private MongoSequence sequence;
    
    private ICMSLock metadataLock;
    
    private final int maxCacheSize;
    private final int cacheExpiredTime;
    
    private MetaValidator validator = new MetaValidator();

    public MongoMetadataServiceImpl(Mongo mongo, int maxCacheSize,
            int cacheExpiredTime, int collectionCountCacheSize, int collectionCountCacheExpiredTime,
            Repository repo, ICMSLock metadataLock) {
        
        CheckConditions.checkNotNull(mongo, "mongo can not be null");
        CheckConditions.checkNotNull(repo, "repository can not be null");
        CheckConditions.checkArgument(maxCacheSize > 0, "maximumSize can not be negtive value");
        CheckConditions.checkArgument(cacheExpiredTime > 0, "expireAfterSeconds can not be negtive value");
        CheckConditions.checkArgument(collectionCountCacheSize > 0, "collectionCountCacheSize can not be negtive value");
        CheckConditions.checkArgument(collectionCountCacheExpiredTime > 0, "collectionCountCacheExpiredTime can not be negtive value");
        CheckConditions.checkArgument(metadataLock != null, "metadataLock should not be null");

        this.metadataLock = metadataLock;
        this.maxCacheSize= maxCacheSize;
        this.cacheExpiredTime = cacheExpiredTime;

        this.mongo = mongo;
        this.repo = repo;
        this.cacheManager = new MongoMetaCacheManager(maxCacheSize, cacheExpiredTime, collectionCountCacheSize, collectionCountCacheExpiredTime);

        String collectionName = CMSConsts.METACLASS_COLL;
        collection = mongo.getDB(repo.getRepositoryName()).getCollection(collectionName);
        // read from primary only
        collection.setReadPreference(ReadPreference.primary());
        collection.setWriteConcern(WriteConcern.SAFE);

        sequence = new MongoSequence(mongo, CMSConsts.SYS_DB, CMSConsts.SEQUENCE_COLL, CMSConsts.NEXT_FIELD_NAME_SEQ);
        
        this.getMetaClasses(new MetadataContext(true, true));
    }
    
    @Override
    public Repository getRepository() {
        return this.repo;
    }
    
    private MetaClass getMetaClassFromDb(DBObject query) {
        DBObject object = collection.findOne(query);
        if (object == null) {
            return null;
        }

        MetaClass m = converter.fromBson(object, MetaClass.class);

        setUpMetaClass(m, graph);
        cacheManager.addMetaClassToCache(m);
        return m;
    }
    
    @Override
    public MetaClass getMetaClass(String className) {
        CheckConditions.checkNotNull(className, "MetaClass name can't be null!");
        MetaClass m = cacheManager.getMetaClassFromCache(className);
        if (m == null) {
            //use className as name to query
            BasicDBObject query = new BasicDBObject();
            query.put(MetaClass.NAME, className);
            
            m = getMetaClassFromDb(query);
            if (m != null) {
                return m;
            }
            
            //use className as pluralName to query
            query = new BasicDBObject();
            query.put(MetaClass.PLURAL_NAME, className);
            
            m = getMetaClassFromDb(query);
        }

        return m;
    }

    @Override
    public MetaClass getMetaClass(String className, int version, MetadataContext context) {
        MetaClass m = getMetaClass(className);
        if (m == null || version < 0 || version == m.getVersion()) {
            return m;
        }
        
        if (historyService != null) {
            m = historyService.getHistory(repo.getRepositoryName(), className, version, context);
            if (m != null) {
                m.setMetadataService(this);
                setUpMetaClass(m, graph);
            }
            return m;
        }
        
        return null;
    }
    
    private MetaClass innerCreateMetaClass(MetaClass metaClass, boolean updateGraph, MetadataContext context) {
        populateDbName(metaClass);
        metaClass.setLastModified(new Date());
        
        // check if it was deleted before in History
        if (!HistoryMetaClass.NAME.equals(metaClass.getName())
                && !BranchMetaClass.TYPE_NAME.equals(metaClass.getName())
                && historyService != null) {
            MetaClass oldMeta = historyService.getHistory(metaClass.getRepository(), metaClass.getName(), -1, context);
            if (oldMeta != null) {
                metaClass.setVersion(oldMeta.getVersion() + 1);
            }
        }
        
        // set parent version
        String parent = metaClass.getParent();
        if (!StringUtils.isNullOrEmpty(parent)) {
            MetaClass parentMeta = getMetaClass(parent);
            if (parentMeta != null) {
                metaClass.setParentVersion(parentMeta.getVersion());
            } else {
                metaClass.setParentVersion(0);
            }
        }

        DBObject object = converter.toBson(metaClass);
        try {
            collection.insert(object);
        } catch (MongoException.DuplicateKey e) {
            throw new MetaClassExistsException(metaClass.getName());
        } catch (MongoException e) {
            throw new MongoOperationException(e);
        }
        
        MetaClass result = getMetaClass(metaClass.getName());
        if (updateGraph) {
            graph.updateMetaClass(result);
        }
        return result;
    }

    @Override
    public MetaClass createMetaClass(MetaClass metaClass, MetadataContext context) {
        try {
            metadataLock.lock();
            if (metaClass == null) {
                throw new IllegalMetaClassException("meta class can not be null");
            }
            
            if (org.apache.commons.lang.StringUtils.isEmpty(metaClass.getName())) {
                throw new IllegalMetaClassException("meta class must have a name");
            }
            
            metaClass.setMetadataService(this);
            
            Collection<MetaClass> mergedMetaClasses = new ArrayList<MetaClass>(); 
            for (MetaClass mc : graph.getMetaClasses()) {
            	mergedMetaClasses.add(mc);
            }
            mergedMetaClasses.add(metaClass);
            MetaClassGraph tempGraph = new MetaClassGraph(mergedMetaClasses);
            for (MetaClass mc : mergedMetaClasses) {
            	mc.setMetadataService(this);
            	tempGraph.updateMetaClass(mc);
            }

            validator.validateForCreation(metaClass, Collections.<String, MetaClass> emptyMap(), tempGraph);

            if (!repo.getRepositoryName().equals(metaClass.getRepository())) {
                throw new IllegalMetaClassException("inconsistency repo name: should be " + repo.getRepositoryName() + " , but was " + metaClass.getRepository());
            }
            
            return innerCreateMetaClass(metaClass, true, context);
        } catch (InterruptedException e) {
            logger.info("lock interrupted for createMetaClass {}", metaClass.getName());
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for createMetaClass " + metaClass.getName(), e);
        }
        finally {
            metadataLock.unlock();
        }
    }
    
    @Override
    public void deleteMetaClass(String className, MetadataContext metaContext) {
        long start = System.currentTimeMillis();
        try {
            metadataLock.lock();

            MetaClass meta = repo.getMetadataService().getMetaClass(className);
            if (meta == null) {
                throw new MetaClassNotExistsException(repo.getRepositoryName(), className);
            }
            if (!meta.getFromReference().isEmpty() || !meta.getDescendants().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (MetaRelationship mr : meta.getFromReference()) {
                    sb.append(mr.getSourceDataType()).append(",");
                }
                for (MetaClass mc : meta.getDescendants()) {
                    sb.append(mc.getName()).append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                throw new MetaDataException(MetaErrCodeEnum.NONE_ORPHAN_META_CLASS, MessageFormat.format("MetaClass {0} is referenced by other meta class: {1}!", className, sb.toString()));
            }

            innerDeleteMetaClass(meta);
            addMetaHistory(meta, "deleteMetaClass", metaContext);

            long duration = System.currentTimeMillis() - start;
            metaContext.addDbTimeCost(duration);
        } catch (InterruptedException e) {
            logger.info("lock interrupted for deletaMetaClass {}", className);
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for deleteMetaClass " + className, e);
        } finally {
            metadataLock.unlock();
        }
    }
    
    private void innerDeleteMetaClass(MetaClass metaClass) {
        DBObject obj = new BasicDBObject();
        obj.put(MetaClass.NAME, metaClass.getName());

        collection.remove(obj);

        cacheManager.deleteMetaClassFromCache(metaClass);
        graph.deleteMetaClass(metaClass);
    }

    @Override
    public MetaClass deleteMetaField(String className, String fieldName, MetadataContext metaContext) {
        long start = System.currentTimeMillis();
        try {
            metadataLock.lock();

            //check meta existence
            MetaClass meta = repo.getMetadataService().getMetaClass(className);
            if (meta == null) {
                throw new MetaClassNotExistsException(repo.getRepositoryName(), className);
            }
            // check field existence, could only delete the field define on this meta class, not inherited ones
            Collection<MetaField> classFields = meta.getClassFields();
            boolean existing = false;
            for (MetaField mf : classFields) {
                if (mf.getName().equals(fieldName)) {
                    existing = true;
                    break;
                }
            }
            if (!existing) {
                throw new MetaFieldNotExistsException(MessageFormat.format("MetaField {0} doesn't exist!", fieldName));
            }

            MetaClass result = innerDropMetaField(meta, fieldName);
            addMetaHistory(meta, "deleteMetaField", metaContext);

            long duration = System.currentTimeMillis() - start;
            metaContext.addDbTimeCost(duration);

            return result;
        } catch (InterruptedException e) {
            logger.info("lock interrupted for deletaMetaField {}", className);
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for deleteMetaField " + className, e);
        } finally {
            metadataLock.unlock();
        }
    }

    private MetaClass innerDropMetaField(MetaClass metaClass, String fieldName) {
        DBObject queryObj = new BasicDBObject();
        queryObj.put(MetaClass.NAME, metaClass.getName());

        // delete the field with given field name
        DBObject updateObj = new BasicDBObject();
        BasicDBObject ups = new BasicDBObject();
        ups.put("fields." + fieldName, "");
        updateObj.put(MongoOperand.unset, ups);
        
        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put("version", 1);
        updateObj.put(MongoOperand.inc, versionObject);
        
        collection.update(queryObj, updateObj);

        cacheManager.deleteMetaClassFromCache(metaClass);
        MetaClass result = getMetaClass(metaClass.getName());
        graph.updateMetaClass(result);
        return result;
    }

    private void populateDbName(MetaClass metaClass) {
        for (MetaField f : metaClass.getClassFields()) {
            if (!f.isInternal() && f.getDbName() == null) {
                f.setDbName(sequence.getNext());
            }
        }
    }
    
    private MetaClass innerUpdateMetaClass(MetaClass metaClass, boolean updateGraph, boolean updateExistingField) {
        
        populateDbName(metaClass);
        metaClass.setLastModified(new Date());
        
        BasicDBObject object = (BasicDBObject)converter.toBson(metaClass);

        BasicDBObject query = new BasicDBObject();
        query.append(MetaClass.NAME, metaClass.getName());
        BasicDBObject update = new BasicDBObject();
        BasicDBObject ups = new BasicDBObject();

        Collection<MetaField> updateFields = metaClass.getClassFields();
        //update fields
        BasicDBObject objectFields = (BasicDBObject)object.get("fields");
        for (MetaField f : updateFields) {
            if (!f.isInternal()) {
                String fieldKey = "fields." + f.getName();
                if (!updateExistingField) {
                    query.append(fieldKey, new BasicDBObject(MongoOperand.exists, false));
                }
                ups.append(fieldKey, objectFields.get(f.getName()));
            }
        }
        // update options
        BasicDBObject objectIndexes = (BasicDBObject) object.get("options");
        Collection<IndexInfo> updateIndex = metaClass.getClassIndexes();
        if (!updateIndex.isEmpty()) {
            BasicDBObject indexOptions = (BasicDBObject)objectIndexes.get("indexes");
            for (IndexInfo index : updateIndex) {
                if (index.isInternal()) {
                    continue;
                }
                String fieldKey = "options.indexes." + index.getIndexName();
                query.append(fieldKey, new BasicDBObject(MongoOperand.exists, false));
                ups.append(fieldKey, indexOptions.get(index.getIndexName()));
            }
        }
        // add optional fields if given
        addIfGiven(object, ups, "description");
        if (object.containsField("parent")) {
            addIfGiven(object, ups, "parent");
            addIfGiven(object, ups, "ancestors");
            addIfGiven(object, ups, "parentVersion");
        }
        addIfGiven(object, ups, "allowFullTableScan");
        addIfGiven(object, ups, "embed");
        addIfGiven(object, ups, "lastModified");
        addIfGiven(object, ups, "inner");
        
        update.append(MongoOperand.set, ups);
        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put("version", 1);
        update.put(MongoOperand.inc, versionObject);

        try {
            boolean updated = MongoUtils.wrapperUpdate(collection, query, update);
            if (!updated) {
                StringBuilder sb = new StringBuilder();
                for (MetaField f : updateFields) {
                    sb.append(f.getName()).append(",");
                }
                throw new MetaFieldExistsException(sb.toString());
            }
        } catch (MongoException e) {
            throw new MongoOperationException(e);
        }

        cacheManager.deleteMetaClassFromCache(metaClass);        
        MetaClass result = getMetaClass(metaClass.getName());

        if (updateGraph) {
            getMetaClasses(new MetadataContext(true, true));
        }
        return result;
    }

    void addIfGiven(BasicDBObject object, BasicDBObject ups, String name) {
        if (object.containsField(name)) {
            ups.append(name, object.get(name));
        }
    }

    @Override
    public MetaClass updateMetaClass(MetaClass metaClass, MetadataContext context) {
        
        try {
            metadataLock.lock();
            MetaClass existingMeta = getMetaClass(metaClass.getName());
            checkMetaClassVersion(metaClass);
            
            metaClass.setMetadataService(this);
            
            Collection<MetaClass> mergedMetaClasses = new ArrayList<MetaClass>(); 
            for (MetaClass mc : graph.getMetaClasses()) {
            	mergedMetaClasses.add(mc);
            }
            mergedMetaClasses.add(metaClass);
            MetaClassGraph tempGraph = new MetaClassGraph(mergedMetaClasses);
            for (MetaClass mc : mergedMetaClasses) {
            	mc.setMetadataService(this);
            	tempGraph.updateMetaClass(mc);
            }
            
//            metaClass.validateForUpdate(Collections.<String, MetaClass> emptyMap(), tempGraph);
            validator.validateForUpdate(metaClass, Collections.<String, MetaClass> emptyMap(), tempGraph);
            setupParentVersion(metaClass);
            MetaClass meta = innerUpdateMetaClass(metaClass, true, false);
            addMetaHistory(existingMeta, "updateMetaClass", context);
            return meta;
        } catch (InterruptedException e) {
            logger.info("lock interrupted for updateMetaClass {}", metaClass.getName());
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for updateMetaClass " + metaClass.getName(), e);
        }
        finally {
            metadataLock.unlock();
        }
    }

    private void setupParentVersion(MetaClass metaClass) {
        // update the parentVersion if existing.
        String parent = metaClass.getParent();
        if (parent != null) {
            MetaClass parentMeta = metaClass.getParentMetaClass();
            if (parentMeta != null) {
                metaClass.setParentVersion(parentMeta.getVersion());
            } else {
                metaClass.setParentVersion(0);
            }
        }
    }

    private Map<String, MetaClass> prepareMetaClassMap(List<MetaClass> metaClasses) {
        HashMap<String, MetaClass> metas = new HashMap<String, MetaClass>();
        HashSet<String> pluralNames = new HashSet<String>();
        
        for (MetaClass m : metaClasses) {
            String name = m.getName();
            String pluralName = m.getpluralName();
            if (StringUtils.isNullOrEmpty(name)) {
                throw new IllegalMetaClassException("meta class name can not be empty");
            }
            
            if (metas.containsKey(name) || metas.containsKey(pluralName)) {
                throw new IllegalMetaClassException("duplicate metaClass name in batchUpsert");
            }
            
            if (pluralNames.contains(name) || pluralNames.contains(pluralName)) {
                throw new IllegalMetaClassException("duplicate metaClass plural name in batchUpsert");
            }
            
            if (pluralName != null) {
                pluralNames.add(pluralName);
            }
            
            metas.put(name, m);
        }
        
        return metas;
    }
    
    @Override
    public List<MetaClass> batchUpsert(List<MetaClass> metaClasses, MetadataContext context) {
        try {
            metadataLock.lock();
            if (metaClasses == null || metaClasses.isEmpty()) {
                return Collections.emptyList();
            }
            
            Map<String, MetaClass> metas = prepareMetaClassMap(metaClasses);
            
            ArrayList<MetaClass> result = new ArrayList<MetaClass>(metaClasses.size());
            
            Collection<MetaClass> mergedMetaClasses = new ArrayList<MetaClass>(); 
            for (MetaClass mc : graph.getMetaClasses()) {
            	mergedMetaClasses.add(mc);
            }
            mergedMetaClasses.addAll(metaClasses);
            
            MetaClassGraph tempGraph = new MetaClassGraph(mergedMetaClasses);
            
            for (MetaClass m : mergedMetaClasses) {
            	m.setMetadataService(this);
                tempGraph.updateMetaClass(m);
            }
            
            for (MetaClass m : metaClasses) {
                MetaClass m1 = getMetaClass(m.getName());
                
                //getmetaClass can return metaclass whose name is not this name, but pluralName is this name
                if (m1 != null && m1.getName().equals(m.getName())) {
                    checkMetaClassVersion(m);
//                    m.validateForUpdate(metas, tempGraph);
                    validator.validateForUpdate(m, metas, tempGraph);
                }
                else {
//                    m.validateForCreation(metas, tempGraph);
                    validator.validateForCreation(m, metas, tempGraph);
                }
            }
            
            for (MetaClass m : metaClasses){ 
            	String name = m.getName();
            	MetaClass value = m;
                MetaClass m1 = getMetaClass(name);
                //getmetaClass can return metaclass whose name is not this name, but pluralName is this name
                if (m1 == null || !m1.getName().equals(name)) {
                    MetaClass meta = innerCreateMetaClass(value, false, context);
                    result.add(meta);
                }
                else {
                	cacheManager.deleteMetaClassFromCache(m1);
                    setupParentVersion(value);
                	MetaClass meta = innerUpdateMetaClass(value, false, false);
                    result.add(meta);
                    addMetaHistory(m1, "updateMetaClass", context);
                }
            }
            for (MetaClass m : result) {
                graph.updateMetaClass(m);
            }
            
            return result;
        } catch (InterruptedException e) {
            logger.info("lock interrupted for batchUpsert");
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for batchUpsert ", e);
        }
        finally {
            metadataLock.unlock();
        }
    }

    @Override
    public final List<MetaClass> getMetaClasses(MetadataContext ctx) {
        MetadataContext context = ctx != null ? ctx : new MetadataContext();
        ArrayList<MetaClass> result = new ArrayList<MetaClass>();

        if (!context.isRefreshMetadata()) {
            return cacheManager.getMetaClassesFromCache();
        }

        DBCursor cursor = collection.find();
        while(cursor.hasNext()) {
            DBObject o = cursor.next();
            MetaClass m = converter.fromBson(o, MetaClass.class);
            result.add(m);
        }

        MetaClassGraph newGraph = new MetaClassGraph(result);
        ExpirableCache<MetaClass> newNameCache = new ExpirableCache<MetaClass>(maxCacheSize, cacheExpiredTime);
        ExpirableCache<MetaClass> pluralNameCache = new ExpirableCache<MetaClass>(maxCacheSize, cacheExpiredTime);
        for (MetaClass m : result) {
            setUpMetaClass(m, newGraph);
            cacheManager.addMetaClassToCache(m, newNameCache, pluralNameCache);
            newGraph.updateMetaClass(m);
        }
        // ## this might still not thread safe between the cache and graphs. But the time window should be small enough
        this.graph = newGraph;
        cacheManager.refreshCache(newNameCache, pluralNameCache);
        return result;
    }

    private void setUpMetaClass(MetaClass m, MetaClassGraph newGraph) {
        // set metaservice for metaclass
        m.setMetadataService(this);
        m.setMetaclassGraph(newGraph);

        //setup metaservice for metafields
        for (MetaField f : m.getClassFields()) {
            if (f.getDataType() == MetaField.DataTypeEnum.RELATIONSHIP) {
                ((MetaRelationship)f).setMetadataService(this);
                ((MetaRelationship)f).setSourceDataType(m.getName());
            }
        }

        List<String> classPrimaryKeys = m.getOptions().getPrimaryKeys();
        if (!classPrimaryKeys.isEmpty()) {
            // add indexes for primary key
            IndexInfo ii = new IndexInfo(IndexInfo.PK_INDEX, true);
            for (String pk : classPrimaryKeys) {
                ii.addKeyField(pk);
            }
            ii.addOption(IndexOptionEnum.unique);
            m.getOptions().addIndex(ii);
        }
        
        m.addReferenceIndexes();
    }

    @Override
    public void updateMetaOption(String className, MetaOption options, MetadataContext context) {
        CheckConditions.checkNotNull(context.getOptionChangeMode(),
                "option update mode must specify when update metaclass options");
        CheckConditions.checkNotNull(options, "option can not be null when update metaclass options");

        MetaClass targetMetadata = getMetaClass(className);
        if (targetMetadata == null) {
            throw new IllegalMetaClassException("can not find meta data with name " + className);
        }

        // validation here: add/update/delete
        MetadataOptionValidator validator = new MetadataOptionValidator(options, context);
        targetMetadata.traverse(validator);
        
//        checkIndexSize(className, options, targetMetadata);

        IMetadataCommand command = new UpdateOptionCommand(targetMetadata, options, this.collection);
        command.execute(context);
        
        addMetaHistory(targetMetadata, "updateMetaOption", context);
        
        cacheManager.deleteMetaClassFromCache(targetMetadata);
        MetaClass result = getMetaClass(targetMetadata.getName());
        graph.updateMetaClass(result);
    }
    
    @Override
    public int getCollectionCount(String dbCollectionName) {
        CheckConditions.checkNotNull(dbCollectionName, "Collection name can't be null!");
        Integer count = cacheManager.getCountFromCache(dbCollectionName);
        if (count == null) {
            DBCollection col = this.mongo.getDB(repo.getRepositoryName()).getCollection(dbCollectionName);
            // read from primary only
            col.setReadPreference(ReadPreference.primary());
            col.setWriteConcern(WriteConcern.SAFE);
            
            count = (Integer) col.getStats().get("count");
            if (count == null) {
                count = 0;
            } else {
            	cacheManager.putCountToCache(dbCollectionName, count);
            }
        }
        return count;
    }
    
    @Override
    public MetaClass updateMetaField(MetaClass metaClass, String fieldName, MetadataContext context) {
        try {
            metadataLock.lock();
            
            MetaClass existingMeta = getMetaClass(metaClass.getName());
            checkMetaClassVersion(metaClass);
            
            metaClass.setMetadataService(this);
            Map<String, MetaClass> metas = prepareMetaClassMap(Arrays.asList(metaClass));
            validator.validateForUpdateField(metaClass, metas, fieldName);
            
            cacheManager.deleteMetaClassFromCache(metaClass);
            graph.deleteMetaClass(metaClass);
            MetaClass meta = innerUpdateMetaClass(metaClass, true, true);
            addMetaHistory(existingMeta, "updateMetaField", context);
            return meta;
        } catch (InterruptedException e) {
            logger.info("lock interrupted for updateSingleMetaField");
            throw new MetaDataException(MetaErrCodeEnum.LOCK_INTERRUPTED, "lock interrupted for updateSingleMetaField ", e);
        }
        finally {
            metadataLock.unlock();
        }
    }

    private void addMetaHistory(MetaClass meta, String operType, MetadataContext context) {
        saveMetaHistory(meta, operType, context);
        
        if (operType.equals("deleteMetaClass")) {
            return;
        }
        
        // update the parent version in all children meta
        List<MetaClass> descendants = graph.getDescendants(meta);
        
        if (descendants.size() > 0) {
            Map<String, Integer> metaVersionMap = new HashMap<String, Integer>();
            metaVersionMap.put(meta.getName(), Integer.valueOf(meta.getVersion() + 1));

            String oldParent = null;
            String newParent = null;
            MetaClass newMeta = getMetaClass(meta.getName());
            if (newMeta != null) {
            	BasicDBObject newObject = (BasicDBObject)converter.toBson(newMeta);
            	BasicDBObject oldObject = (BasicDBObject)converter.toBson(meta);
            	if(!oldObject.containsField("parent") && newObject.containsField("parent")) {
            		oldParent = null;
            		newParent = (String)newObject.get("parent");
            	} else if (oldObject.containsField("parent") && !newObject.containsField("parent")) {
            		oldParent = (String)oldObject.get("parent");
              	 	newParent = null;
            	} else if (oldObject.containsField("parent") && newObject.containsField("parent") 
            			&& !oldObject.get("parent").equals(newObject.get("parent"))) {
              		oldParent = (String)oldObject.get("parent");
              		newParent = (String)newObject.get("parent");
            	}
            }
            
            // avoid endless loop by wrong descendants value
            boolean shrinking = true;
            while (descendants.size() > 0 && shrinking) {
                shrinking = false;
                for (int i = 0; i < descendants.size(); i++) {
                    MetaClass desc = descendants.get(i);
                    String name = desc.getParent();
                    Integer newVersion = metaVersionMap.get(name);
                    if (newVersion != null) {
                        // update descendant parentVersion
                        MetaClass newDesc = new MetaClass();
                        newDesc.setName(desc.getName());
                        newDesc.setParent(name);
                        newDesc.setParentVersion(newVersion.intValue());
                        
                        List<String> ancestors = desc.getAncestors();
                        if(!org.apache.commons.lang.StringUtils.isEmpty(oldParent)) {
                        	ancestors.remove(oldParent);
                        }
                        if(!org.apache.commons.lang.StringUtils.isEmpty(newParent)) {
                        	ancestors.add(newParent);
                        }
                        newDesc.setAncestors(ancestors);
                        
                        innerUpdateMetaClass(newDesc, true, false);
                        
                        // save the descendant history
                        saveMetaHistory(desc, "updateMetaClassByParentVersion", context);
                        
                        metaVersionMap.put(desc.getName(), Integer.valueOf(desc.getVersion() + 1));
                        descendants.remove(i);
                        shrinking = true;
                    }
                }
            }
        }
    }

    private void saveMetaHistory(MetaClass meta, String operType, MetadataContext context) {
        if (historyService != null 
                && !HistoryMetaClass.NAME.equals(meta.getName())
                && !BranchMetaClass.TYPE_NAME.equals(meta.getName())) {
            historyService.addHistory(meta, operType, context);
        }
    }
    
    private void checkMetaClassVersion(MetaClass metaClass) {
        MetaClass existingMeta = getMetaClass(metaClass.getName());
        if (existingMeta == null) {
            throw new MetaClassNotExistsException(metaClass.getRepository(), metaClass.getName());
        }
        if (metaClass.getVersion() > 0 && metaClass.getVersion() != existingMeta.getVersion()) {
            throw new MetaDataException(MetaErrCodeEnum.VERSION_CONFLICTED, "MetaClass version conflict: " + metaClass.getName());
        }
        metaClass.setVersion(existingMeta.getVersion());
    }

    @Override
    public void validateMetaClass(String className) {
        MetaClass meta = repo.getMetadataService().getMetaClass(className);
        if (meta == null) {
            throw new MetaClassNotExistsException(repo.getRepositoryName(), className);
        }

        validator.validate(meta);
    }

    @Override
    public void setMetaHistoryService(IMetadataHistoryService historyService) {
        this.historyService = historyService;
    }
    
}