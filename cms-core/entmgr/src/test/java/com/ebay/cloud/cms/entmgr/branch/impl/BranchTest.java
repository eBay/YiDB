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
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.IBranchService;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class BranchTest extends CMSMongoTest {

    private static final String       RAPTOR_REPO      = "raptor-paas";
    private static final String       RAPTOR_TEST_DATA = "raptorTopology.json";
    
    private static IBranchService branchService;
    private static IRepositoryService repoService;
    private static IEntityService entityService;
    private static CMSDBConfig config;
    
    @BeforeClass
    public static void setUp(){
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

        config = new CMSDBConfig(dataSource);
        MetadataDataLoader metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();

        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        RuntimeDataLoader dataLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, RAPTOR_REPO);
        dataLoader.load(RAPTOR_TEST_DATA);

        branchService = ServiceFactory.getBranchService(dataSource, implementations);
        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);
    }
    
    private IBranch newBranchObject(String id, String parentBranchId) {
        Branch b = new Branch();
        b.setId(id);
        b.setMainBranch(parentBranchId == null);
        b.setRepositoryName(RAPTOR_REPO);
        
        return b;
    }

    private static EntityContext newContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        context.setModifier("unitTestUser");
        context.setDbConfig(config);
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        return context;
    }
    
    @Test
    public void testBranchCache() {
        BranchCache cache = new BranchCache();
        
        Branch nullB = new Branch();
        cache.putBranch(nullB);
        Assert.assertNull(cache.getBranch(null, null));
        
        nullB = new Branch();
        nullB.setRepositoryName(RAPTOR_REPO);
        cache.putBranch(nullB);
        Assert.assertNull(cache.getBranch(RAPTOR_REPO, null));
        
        nullB = new Branch();
        nullB.setId("main");
        cache.putBranch(nullB);
        Assert.assertNull(cache.getBranch(null, "main"));
        
        Branch b = new Branch();
        b.setRepositoryName(RAPTOR_REPO);
        b.setId("main");
        cache.putBranch(b);
        Assert.assertEquals(b, cache.getBranch(RAPTOR_REPO, "main"));
    }
    
    @Test
    public void getNullBranch() {
        try {
            branchService.getBranch(RAPTOR_REPO, null, newContext());
            Assert.fail();
        } catch(IllegalArgumentException e) {
            Assert.assertEquals("Miss valid entity id", e.getMessage());
        }
    }
    
    @Test
    public void createBranch() {
        IBranch branch = newBranchObject("newbranchid", null);
        branchService.createBranch(branch, newContext());

        IBranch getBranch = branchService.getBranch(RAPTOR_REPO, branch.getId(), newContext());
        Assert.assertNotNull(getBranch);
    }

    @Test
    public void deleteMetadata() {
        IBranch branch = newBranchObject("deleteMetadata", null);
        EntityContext context = newContext();
        branchService.createBranch(branch, context);
        Repository repo = repoService.getRepository(RAPTOR_REPO);

        IMetadataService metaService = repo.getMetadataService();
        MetaClass meta = new MetaClass();
        String name = "testmeta";
        meta.setName(name);
        meta.setRepository(RAPTOR_REPO);
        meta = metaService.createMetaClass(meta, new MetadataContext());
        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(meta);
        branchService.ensureIndex(repo.getRepositoryName(), metas, context);

        List<IBranch> branches = branchService.getMainBranches(RAPTOR_REPO, context);
        for (IBranch b : branches) {
            PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
                    repo.getMetadataService(), b.getId(), context.getConsistentPolicy(), context.getRegistration(), false, context.getDbConfig(),
                    context.getAdditionalCriteria());
            persistenceContext.setMongoDataSource(dataSource);
            DBCollection collection = persistenceContext.getDBCollection(meta);
            List<DBObject> indexInfo = collection.getIndexInfo();
            Assert.assertNotNull(indexInfo);
            Assert.assertTrue(indexInfo.size() > 0);
        }        

        branchService.deleteMetadata(RAPTOR_REPO, meta, newContext());
        
        branches = branchService.getMainBranches(RAPTOR_REPO, newContext());
        for (IBranch b : branches) {
            PersistenceContext persistenceContext = PersistenceContextFactory.createEntityPersistenceConext(
                    repo.getMetadataService(), b.getId(), context.getConsistentPolicy(), context.getRegistration(), false, 
                    context.getDbConfig(), context.getAdditionalCriteria());
            persistenceContext.setMongoDataSource(dataSource);
            DBCollection collection = persistenceContext.getDBCollection(meta);
            List<DBObject> indexInfo = collection.getIndexInfo();
            Assert.assertNotNull(indexInfo);
            Assert.assertTrue(indexInfo.size() == 0);
        }
    }
    
    @Test
    public void createBranchWithoutId() {
        IBranch branch = newBranchObject(null, null);
        try {
            branchService.createBranch(branch, newContext());
            Assert.fail();
        } catch(CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), CmsEntMgrException.EntMgrErrCodeEnum.ILLEGAL_BRANCH_ENTITY);
        }
    }
    
    @Test
    public void createBranchWithoutCreator() {
        IBranch branch = newBranchObject("newbranchid2", null);
        EntityContext context = newContext();
        context.setModifier(null);
        branchService.createBranch(branch, context);
    }
    
    @Test
    public void createSubBranch() {
        IBranch branch = newBranchObject("newbranchid", "main");
        try {
            branchService.createBranch(branch, newContext());
            Assert.fail();
        } catch(CmsEntMgrException e) {
            Assert.assertEquals(e.getErrorEnum(), CmsEntMgrException.EntMgrErrCodeEnum.ILLEGAL_BRANCH_ENTITY);
        }
    }
        
    //create double main branch
    public void createMM() {
        IBranch m2 = newBranchObject("m2", null);
        m2 = branchService.createBranch(m2, newContext());
        Assert.assertNotNull(m2);

        IBranch m3 = newBranchObject("m3", null);
        m3 = branchService.createBranch(m3, newContext());
        Assert.assertNotNull(m3);
    }
    
    @Test(expected=CmsEntMgrException.class) 
    //create double main branch
    public void createSameNameBranch() {
        IBranch m2 = newBranchObject("SameName", null);
        m2 = branchService.createBranch(m2, newContext());
        Assert.assertNotNull(m2);

        IBranch m3 = newBranchObject("SameName", null);
        m3 = branchService.createBranch(m3, newContext());
    }

    private JsonEntity newServiceInstance(String branchId, String name){
        String metaType = "ServiceInstance";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", name);
        return newEntity;
    }
    
    
    //strong reference integrity only on main branch
    //so no need this test
//    @Test(expected=CmsEntMgrException.class)
//    public void commitMBBFailForStrongRefBroken() {
//        createMBB("mf", "ef", "ef1");
//        
//        //create object b on me branch
//        JsonEntity b = newServiceInstance("ef", "b");
//        String bId = entityService.create(b, newContext());
//        b.setId(bId);
//        
//        IBranch ef1 = branchService.getBranch(RAPTOR_REPO, "ef1", newContext());
//        IBranch ef2 = newBranchObject("ef2", "ef");
//        ef2 = branchService.createBranch(ef2, newContext());
//        
//        //create object a on eb2 sub branch strong reference to object b
//        JsonEntity a = newApplicationService("ef2", "a");
//        a.addFieldValue("services", b);
//        entityService.create(a, newContext());
//        
//        //delete object b on eb1 branch
//        entityService.delete(RAPTOR_REPO, "ef1", "ServiceInstance", bId, newContext());
//        
//        //commit branch eb2
//        branchService.commit(ef2, newContext());
//        
//        //commit branch eb1, success commit will break the strong reference
//        branchService.commit(ef1, newContext());
//    }
    
//    @Test(expected=CmsEntMgrException.class)
//    public void commitMBBFailForUniqueIndexConfliction() {
//        createMBB("mh", "eh", "eh1");
//        
//        IBranch eh1 = branchService.getBranch(RAPTOR_REPO, "eh1", newContext());
//        IBranch eh2 = newBranchObject("eh2", "eh");
//        eh2 = branchService.createBranch(eh2, newContext());
//        
//        //create object a on eg2 sub branch
//        JsonEntity a2 = newApplicationService("eh2", "uniquename");
//        entityService.create(a2, newContext());
//        
//      //create object a on eg2 sub branch
//        JsonEntity a1 = newApplicationService("eh1", "uniquename");
//        entityService.create(a1, newContext());
//        
//        //commit branch eb2
//        branchService.commit(eh2, newContext());
//        
//        //commit branch eb1, success commit will break the unique index on Name
//        branchService.commit(eh1, newContext());
//    }
//    
//    @Test(expected=CmsEntMgrException.class)
//    public void addEntityToMBFailForUniqueIndexConfliction() {
//        createMBB("mi", "ei", null);
//        
//        branchService.getBranch(RAPTOR_REPO, "ei1", newContext());
//        
//        //create object a on ei sub branch
//        JsonEntity a2 = newApplicationService("ei", "uniquename");
//        entityService.create(a2, newContext());
//        
//        //create object with same name on ei, break the unique index
//        JsonEntity a1 = newApplicationService("ei", "uniquename");
//        entityService.create(a1, newContext());
//    }
    
    
    /*****************************************************************
     * test create/update/delete operations on branch
     * **************************************************************/
    
    
    /**
     * Case from cms2207: operation on not existing branch should throw exception with branch not found error code
     */
    @Test
    public void testOperationOnNotExistingBranch() {
        String invalidBranchId = "invalidSubBranch";
        try {
            IEntity qEntity = buildQueryEntity(RAPTOR_REPO, invalidBranchId, "ServiceInstance", "whateverid");
            entityService.get(qEntity, newContext());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }

        JsonEntity entity = newServiceInstance(invalidBranchId, "nameForInvalidBranch");
        try {
            entityService.create(entity, newContext());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }

        try {
            entityService.modify(entity, entity, newContext());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }

        try {
            IEntity qEntity = buildQueryEntity(entity);
            entityService.replace(qEntity, entity, newContext());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }
        List<IEntity> entities = new ArrayList<IEntity>();
        entities.add(entity);
        try {
            entityService.batchCreate(entities, newContext(), new ArrayList<String>());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }
        try {
            entityService.batchModify(entities, newContext(), new ArrayList<String>());
            Assert.fail();
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), e.getErrorCode());
        }
    }
    
    private JsonEntity buildQueryEntity(IEntity entity) {
        return buildQueryEntity(entity.getRepositoryName(), entity.getBranchId(), entity.getType(), entity.getId());
    }
    
    private JsonEntity buildQueryEntity(String reponame, String branchname, String metadata, String oid) {
        MetaClass meta = repoService.getRepository(reponame).getMetadataService().getMetaClass(metadata);
        JsonEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }
}
