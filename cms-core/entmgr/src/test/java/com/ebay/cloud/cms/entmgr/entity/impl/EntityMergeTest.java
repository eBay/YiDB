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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

@SuppressWarnings("unused")
public class EntityMergeTest extends CMSMongoTest {
    
    private static final EntityFieldTargetMerger targetFieldPushMerger = new EntityFieldTargetMerger(false);
    private static final EntityFieldTargetMerger targetFieldPullMerger = new EntityFieldTargetMerger(true);
    private static final EntityFieldDeltaMerger deltaFieldPushMerger = new EntityFieldDeltaMerger();


    private static final String ENVIRONMENT = "Environment";
    private static final String NODE_SERVER = "NodeServer";
    private static final String        LAST_MODIFIED_TIME  = "lastModifiedTime";
    private static final String        MANIFEST            = "Manifest";
    private static final String        SERVICE_INSTANCE    = "ServiceInstance";
    private static final String        APPLICATION_SERVICE = "ApplicationService";
    private static final String        COMPUTE             = "Compute";

    private static RuntimeDataLoader   raptorLoader        = null;
    private static final String        RAPTOR_REPO         = "raptor-paas";
    private static final String        RAPTOR_DATA_FILE    = "raptorTopology.json";

    private static RuntimeDataLoader   deployLoader        = null;
    private static final String        DEPLOY_REPO         = "software-deployment";
    private static final String        DEPLOY_DATA_FILE    = "softwareDeploymentRuntime.json";

    private static RuntimeDataLoader   stratusLoader       = null;
    private static final String        STRATUS_REPO        = "stratus-ci";
    private static final String        STRATUS_DATA_FILE   = "stratusRuntime.json";

    protected static RuntimeDataLoader cmsdbLoader         = null;
    protected static final String      CMSDB_REPO          = "cmsdb";
    protected static final String      CMSDB_DATA_FILE     = "cmsdbRuntime.json";


    private static MetadataDataLoader  metaLoader          = null;

    private static IRepositoryService  repoService         = null;
    private static IBranchService      branchService       = null;
    private static IEntityService      entityService       = null;
    private static IMetadataService    raptorMetaService   = null;
    private static IMetadataService    deployMetaService   = null;
    private static IMetadataService    stratusMetaService  = null;
    private static IMetadataService    cmsdbMetaService    = null;
    private static IPersistenceService persistenceService  = null;


    private static final String        SOURCE_IP           = "127.0.0.1";
    private static final String        MAIN_BRANCH         = IBranch.DEFAULT_BRANCH;
    private static EntityContext       context;

    private static MongoDataSource     dataSource;
    protected static CMSDBConfig         config;

    @BeforeClass
    public static void setUp() {
        ServiceFactory.clearServiceCaches();
        
        String connectionString = CMSMongoTest.getConnectionString();
        dataSource = new MongoDataSource(connectionString);
        config = new CMSDBConfig(dataSource);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        metaLoader.loadCMSDBMetaDataFromResource();
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        raptorLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, RAPTOR_REPO);
        raptorLoader.load(RAPTOR_DATA_FILE);

        deployLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, DEPLOY_REPO);
        deployLoader.load(DEPLOY_DATA_FILE);

        stratusLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, STRATUS_REPO);
        stratusLoader.load(STRATUS_DATA_FILE);
        
        cmsdbLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, CMSDB_REPO);
        cmsdbLoader.load(CMSDB_DATA_FILE);

        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);
        
        // create a testing branch
        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);

        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        cmsdbMetaService = repoService.getRepository(CMSDB_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = newEntityContext();

        // check indexes are loaded
        final MetadataContext metaContext = new MetadataContext(true, true);
        List<MetaClass> raptorMetas = raptorMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : raptorMetas) {
            checkIndexesLoaded(raptorMetaService, meta);
        }
        List<MetaClass> deployMetas = deployMetaService.getMetaClasses(metaContext);
        for (MetaClass meta : deployMetas) {
            checkIndexesLoaded(deployMetaService, meta);
        }
    }

    protected static EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setDbConfig(config);
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        return context;
    }
    
    protected static PersistenceContext newPersistentContext(IMetadataService metaService) {
        PersistenceContext pContext = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.PRIMARY, IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }

    private static void checkIndexesLoaded(IMetadataService metaService, MetaClass metaClass) {
        if (metaClass.getName().equals(HistoryMetaClass.NAME) || metaClass.getName().equals(BranchMetaClass.TYPE_NAME)) {
            return;
        }

        Map<String, DBObject> indexObjects = getCollectionIndexMap(metaService, metaClass);
        for (IndexInfo ii : metaClass.getIndexes()) {
            Assert.assertTrue(indexObjects.containsKey(ii.getIndexName()));
        }
    }

    private static Map<String, DBObject> getCollectionIndexMap(IMetadataService metaService, MetaClass metaClass) {
        PersistenceContext pc = newPersistentContext(metaService);
        pc.setMongoDataSource(dataSource);
        DBCollection collection = pc.getDBCollection(metaClass);
        List<DBObject> indexInfo = collection.getIndexInfo();
        Assert.assertNotNull(indexInfo);
        Assert.assertTrue(indexInfo.size() > 0);

        Map<String, DBObject> indexMap = new HashMap<String, DBObject>();
        for (DBObject indexObject : indexInfo) {
            String name = (String) indexObject.get("name");
            indexMap.put(name, indexObject);
        }
        return indexMap;
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testTargetPullMergeEntityOnField() {
        EntityContext context = newEntityContext();
        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        IEntity qEntity = new JsonEntity(metaClass);
        qEntity.setBranchId(MAIN_BRANCH);
        final String applicationId = "4fbb314fc681caf13e283a76";
        final String services = "services";
        qEntity.setId(applicationId);
        IEntity foundEntity = entityService.get(qEntity, context);

        IEntity givenEntity = new JsonEntity(metaClass);
        givenEntity.setId(applicationId);
        givenEntity.setBranchId(MAIN_BRANCH);
        final String name = "name";
        // an confusion for other
        givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
        
        // case 1 empty given
        {
            assertFalse(targetFieldPullMerger.mergeEntityOnField(givenEntity, services, foundEntity));
        }
        // case 2 all to be removed
        {
            givenEntity.setFieldValues(services, foundEntity.getFieldValues(services));
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            assertTrue(givenEntity.hasField(services));
            assertEquals(0, givenEntity.getFieldValues(services).size());
        }
        // case 3 something unkown
        MetaClass serviceMeta = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        IEntity unknown = new JsonEntity(serviceMeta);
        unknown.setId("random-id");
        givenEntity.setFieldValues(services, Arrays.asList(unknown));
        assertFalse(targetFieldPullMerger.mergeEntityOnField(givenEntity, services, foundEntity));
        // case 4 some yes some thing not
        final List<IEntity> subList = (List<IEntity>)foundEntity.getFieldValues(services).subList(0, 3);
        givenEntity.setFieldValues(services, subList);
        givenEntity.addFieldValue(services, unknown);
        assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, services, foundEntity));
        assertEquals(foundEntity.getFieldValues(services).size() - subList.size(), givenEntity.getFieldValues(services).size());
        // should assert the ids
        List<?> mergedValues = givenEntity.getFieldValues(services);
        CollectionUtils.filter( mergedValues, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                IEntity entity = (IEntity)object;
                for (IEntity e : subList) {
                    if (e.getId().equals(entity.getId())) {
                        return false;
                    }
                }
                return true;
            }
        } );
        assertEquals(mergedValues.size(), givenEntity.getFieldValues(services).size());

        // case 5 : normal non-many field
        {
            givenEntity.removeField(name);
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
            // it works as just pull
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            givenEntity.addFieldValue(name, "new-name");
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, name, foundEntity));
        }
        // case 6 : normal array field
        {
            final String manifest = "activeManifestRef";
            givenEntity.removeField(manifest);
            assertFalse(targetFieldPullMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            givenEntity.setFieldValues(manifest, foundEntity.getFieldValues(manifest));
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            givenEntity.addFieldValue(manifest, "new-name");
            assertFalse(targetFieldPullMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            givenEntity.setFieldValues(manifest, Arrays.asList("new-name"));
            assertFalse(targetFieldPullMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
        }
        // case 7 : cardinality = one reference
        {
            qEntity = new JsonEntity(serviceMeta);
            qEntity.setBranchId(MAIN_BRANCH);
            final String serviceId = "4fbb314fc681caf13e283a7c";
            final String compute = "runsOn";
            qEntity.setId(serviceId);
            foundEntity = entityService.get(qEntity, context);
            givenEntity = new JsonEntity(serviceMeta);
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            
            givenEntity.setFieldValues(compute, foundEntity.getFieldValues(compute));
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            assertFalse(givenEntity.hasField(compute));

            MetaClass computeMeta = raptorMetaService.getMetaClass(COMPUTE);
            JsonEntity computeEntity = new JsonEntity(computeMeta);
            computeEntity.setId("unknown-id");
            givenEntity.setFieldValues(compute, Arrays.asList(computeEntity));
            assertTrue(targetFieldPullMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTargetPushMergeEntityOnField() {
        EntityContext context = newEntityContext();
        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        IEntity qEntity = new JsonEntity(metaClass);
        qEntity.setBranchId(MAIN_BRANCH);
        final String applicationId = "4fbb314fc681caf13e283a76";
        final String services = "services";
        qEntity.setId(applicationId);
        IEntity foundEntity = entityService.get(qEntity, context);

        IEntity givenEntity = new JsonEntity(metaClass);
        givenEntity.setId(applicationId);
        givenEntity.setBranchId(MAIN_BRANCH);
        final String name = "name";
        // an confusion for other
        givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
        {
            // case 1 empty given
            {
                assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            }
            // case 2 same to the existing
            {
                givenEntity.setFieldValues(services, foundEntity.getFieldValues(services));
                assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            }
            // case 3 something unkown
            MetaClass serviceMeta = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
            IEntity unknown = new JsonEntity(serviceMeta);
            unknown.setId("random-id");
            givenEntity.setFieldValues(services, Arrays.asList(unknown));
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            assertEquals(foundEntity.getFieldValues(services).size() + 1, givenEntity.getFieldValues(services).size());
            
            // case 4 some yes some thing not
            final List<IEntity> subList = (List<IEntity>) foundEntity.getFieldValues(services).subList(0, 3);
            givenEntity.setFieldValues(services, subList);
            givenEntity.addFieldValue(services, unknown);
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            assertEquals(foundEntity.getFieldValues(services).size() + 1, givenEntity.getFieldValues(services).size());
            // should assert the ids
            final List<IEntity> foundValues = (List<IEntity>) foundEntity.getFieldValues(services);
            List<?> mergedValues = givenEntity.getFieldValues(services);
            CollectionUtils.filter(mergedValues, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    IEntity entity = (IEntity) object;
                    for (IEntity e : foundValues) {
                        if (e.getId().equals(entity.getId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            // unkown is removed
            assertEquals(mergedValues.size() + 1, givenEntity.getFieldValues(services).size());
        }
        // case 5 : normal non-many field
        final String expected = "new-name";
        {
            givenEntity.removeField(name);
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            
            givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            
            givenEntity.addFieldValue(name, expected);
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(name).size());
            assertEquals(expected, givenEntity.getFieldValues(name).get(0));
        }
        // case 6 : normal array field
        {
            final String manifest = "activeManifestRef";
            givenEntity.removeField(manifest);
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));

            givenEntity.setFieldValues(manifest, foundEntity.getFieldValues(manifest));
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            
            givenEntity.addFieldValue(manifest, expected);
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            assertEquals(foundEntity.getFieldValues(manifest).size() + 1, givenEntity.getFieldValues(manifest).size());
            assertTrue(givenEntity.getFieldValues(manifest).contains(expected));
            
            givenEntity.setFieldValues(manifest, Arrays.asList(expected));
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            assertEquals(foundEntity.getFieldValues(manifest).size() + 1, givenEntity.getFieldValues(manifest).size());
            assertTrue(givenEntity.getFieldValues(manifest).contains(expected));
        }
        // case 7 : cardinality = one reference
        {
            MetaClass serviceMeta = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
            qEntity = new JsonEntity(serviceMeta);
            qEntity.setBranchId(MAIN_BRANCH);
            final String serviceId = "4fbb314fc681caf13e283a7c";
            final String compute = "runsOn";
            qEntity.setId(serviceId);
            foundEntity = entityService.get(qEntity, context);
            assertEquals(1, foundEntity.getFieldValues(compute).size());
            givenEntity = new JsonEntity(serviceMeta);
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            // provide only the same with existing
            givenEntity.setFieldValues(compute, foundEntity.getFieldValues(compute));
            assertFalse(targetFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            // add another one.
            MetaClass computeMeta = raptorMetaService.getMetaClass(COMPUTE);
            JsonEntity computeEntity = new JsonEntity(computeMeta);
            computeEntity.setId("unknown-id");
            givenEntity.setFieldValues(compute, Arrays.asList(computeEntity));
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(compute).size());
            assertEquals(computeEntity.getId(), ((IEntity)givenEntity.getFieldValues(compute).get(0)).getId());
            
            // change found entity!!
            foundEntity.removeField(compute);
            givenEntity.setFieldValues(compute, Arrays.asList(computeEntity));
            assertTrue(targetFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(compute).size());
            assertEquals(computeEntity.getId(), ((IEntity)givenEntity.getFieldValues(compute).get(0)).getId());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeltaPushMerger() {
        EntityContext context = newEntityContext();
        MetaClass metaClass = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        IEntity qEntity = new JsonEntity(metaClass);
        qEntity.setBranchId(MAIN_BRANCH);
        final String applicationId = "4fbb314fc681caf13e283a76";
        final String services = "services";
        qEntity.setId(applicationId);
        IEntity foundEntity = entityService.get(qEntity, context);

        IEntity givenEntity = new JsonEntity(metaClass);
        givenEntity.setId(applicationId);
        givenEntity.setBranchId(MAIN_BRANCH);
        final String name = "name";
        // an confusion for other field
        givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
        {
            // case 1 empty given
            {
                assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            }
            // case 2 same to the existing
            {
                givenEntity.setFieldValues(services, foundEntity.getFieldValues(services));
                assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            }
            // case 3 something unkown
            MetaClass serviceMeta = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
            IEntity unknown = new JsonEntity(serviceMeta);
            unknown.setId("random-id");
            givenEntity.setFieldValues(services, Arrays.asList(unknown));
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(services).size());
            assertEquals(unknown.getId(), ((IEntity)givenEntity.getFieldValues(services).get(0)).getId());
            
            // case 4 some yes some thing not
            final List<IEntity> subList = (List<IEntity>) foundEntity.getFieldValues(services).subList(0, 3);
            givenEntity.setFieldValues(services, subList);
            givenEntity.addFieldValue(services, unknown);
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, services, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(services).size());
            assertEquals(unknown.getId(), ((IEntity)givenEntity.getFieldValues(services).get(0)).getId());
        }
        // case 5 : normal non-many field
        final String expected = "new-name";
        {
            givenEntity.removeField(name);
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            
            givenEntity.setFieldValues(name, foundEntity.getFieldValues(name));
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            
            givenEntity.addFieldValue(name, expected);
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, name, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(name).size());
            assertEquals(expected, givenEntity.getFieldValues(name).get(0));
        }
        // case 6 : normal array field
        {
            final String manifest = "activeManifestRef";
            givenEntity.removeField(manifest);
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));

            givenEntity.setFieldValues(manifest, foundEntity.getFieldValues(manifest));
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            
            givenEntity.addFieldValue(manifest, expected);
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(manifest).size());
            assertEquals(expected, givenEntity.getFieldValues(manifest).get(0));
            
            givenEntity.setFieldValues(manifest, Arrays.asList(expected));
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, manifest, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(manifest).size());
            assertEquals(expected, givenEntity.getFieldValues(manifest).get(0));
        }
        // case 7 : cardinality = one reference
        {
            MetaClass serviceMeta = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
            qEntity = new JsonEntity(serviceMeta);
            qEntity.setBranchId(MAIN_BRANCH);
            final String serviceId = "4fbb314fc681caf13e283a7c";
            final String compute = "runsOn";
            qEntity.setId(serviceId);
            foundEntity = entityService.get(qEntity, context);
            assertEquals(1, foundEntity.getFieldValues(compute).size());

            givenEntity = new JsonEntity(serviceMeta);
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));

            // provide only the same with existing
            givenEntity.setFieldValues(compute, foundEntity.getFieldValues(compute));
            assertFalse(deltaFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));

            // add another one.
            MetaClass computeMeta = raptorMetaService.getMetaClass(COMPUTE);
            JsonEntity computeEntity = new JsonEntity(computeMeta);
            computeEntity.setId("unknown-id");
            givenEntity.setFieldValues(compute, Arrays.asList(computeEntity));
            assertTrue(deltaFieldPushMerger.mergeEntityOnField(givenEntity, compute, foundEntity));
            assertEquals(1, givenEntity.getFieldValues(compute).size());
            assertEquals(computeEntity.getId(), ((IEntity)givenEntity.getFieldValues(compute).get(0)).getId());
        }
    }

}
