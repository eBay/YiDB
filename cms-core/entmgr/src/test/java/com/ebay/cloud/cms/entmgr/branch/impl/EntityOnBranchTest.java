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

import org.junit.Ignore;

import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

@Ignore
public class EntityOnBranchTest extends CMSMongoTest {

/*    private static RuntimeDataLoader raptorLoader = null;
    private static final String RAPTOR_REPO = "raptor-paas";
    private static final String RAPTOR_DATA_FILE = "raptorTopology.json";
    private static MetadataDataLoader metaLoader = null;
    
    private static IRepositoryService repoService = null;
    private static IBranchService branchService = null;
    private static IEntityService entityService = null;
    
    private static final String SOURCE_IP = "127.0.0.1";
    private static final String BRANCH_TEST = "test";
    private static final String BRANCH_TEST2 = "test2";
    
    @BeforeClass
    public static void setUp(){
        String connectionString = CMSMongoTest.getConnectionString();
        MongoDataSource dataSource = new MongoDataSource(connectionString);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
        metaLoader.loadTestDataFromResource();
        raptorLoader = RuntimeDataLoader.getDataLoader(dataSource, RAPTOR_REPO);
        raptorLoader.load(RAPTOR_DATA_FILE);
        
        //create a testing branch
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        branchService = ServiceFactory.getBranchService(dataSource);
        entityService = ServiceFactory.getEntityService(dataSource);
    }
    
    static private String createBranch(String parentBranchId, EntityContext context){
        Repository repoInst = repoService.getRepository(RAPTOR_REPO);
        JsonEntity branch = new JsonEntity(BranchMetaClass.getMetaClass(repoInst));
        branch.addFieldValue(BranchMetaClass.IsMain, true);
        branch.addFieldValue(BranchMetaClass.ParentId, parentBranchId);
        branch.addFieldValue(BranchMetaClass.RootId, parentBranchId);
        branch.addFieldValue("name", BRANCH_TEST);
        String newBranchId = branchService.createBranch(branch,context);
        return newBranchId;
    }
    
    static private String createSubBranch(String branchName, String parentBranchId, EntityContext context){
        Repository repoInst = repoService.getRepository(RAPTOR_REPO);
        JsonEntity branch = new JsonEntity(BranchMetaClass.getMetaClass(repoInst));
        branch.addFieldValue(BranchMetaClass.IsMain, false);
        branch.addFieldValue(BranchMetaClass.ParentId, parentBranchId);
        branch.addFieldValue("name", branchName);
        String newBranchId = branchService.createBranch(branch,context);
        return newBranchId;
    }
    
    static private JsonEntity newServiceInstance(String branchId){
        String metaType = "ServiceInstance";
        MetaClass instCls = repoService.getRepository(RAPTOR_REPO).getMetadataService().getMetaClass(metaType);
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(branchId);
        newEntity.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test");
        return newEntity;
    }
    

    
    @Test
    public void create(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        String testBranchId = createSubBranch(BRANCH_TEST2, IBranch.DEFAULT_BRANCH,context);
        
        JsonEntity newInst = newServiceInstance(testBranchId);
        String newId = entityService.create(newInst, context);
        
        IEntity entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        assertTrue(entityGet == null); //should not in main branch before commit
        entityGet = entityService.get(RAPTOR_REPO,testBranchId,"ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        
        branchService.commit(RAPTOR_REPO, testBranchId, context);
        entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        //after commit, we should be able to see that new service instance on main branch
        assertTrue(entityGet != null);
    }
    
    @Test
    public void delete(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        String testBranchId = createSubBranch("deletebranch", IBranch.DEFAULT_BRANCH,context);
        entityService.delete(RAPTOR_REPO, testBranchId, "ServiceInstance", newId, context);
        IEntity entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        entityGet = entityService.get(RAPTOR_REPO,testBranchId,"ServiceInstance", newId, context);
        assertTrue(entityGet == null);
        
        //let's commit
        branchService.commit(RAPTOR_REPO, testBranchId, context);
        entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        //after commit, we should be able to see that new service instance on main branch
        assertTrue(entityGet == null);
    }

    @Test
    public void deleteDirectlyOnBranch() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);

        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); // created on
                                                               // main

        entityService.delete(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        IEntity entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        assertTrue(entityGet == null);
    }

    @Test
    public void modify(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        String testBranchId = createSubBranch("modifybranch", IBranch.DEFAULT_BRANCH,context);
        JsonEntity modifyInst = newServiceInstance(testBranchId);
        String modifiedName = "Modified Name Test";
        modifyInst.addFieldValue("name", modifiedName);
        modifyInst.setId(newId);
        
        //FIXME: jianxu1: can NOT set version of modifyInst to value>0, otherwise, version conflict!!
        entityService.modify(modifyInst, context);
        IEntity entityGet = entityService.get(RAPTOR_REPO,testBranchId,"ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        assertEquals(modifiedName, entityGet.getFieldValues("name").get(0));
        
        entityGet = entityService.get(RAPTOR_REPO,IBranch.DEFAULT_BRANCH,"ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        assertTrue(modifiedName.equals(entityGet.getFieldValues("name").get(0))==false);
        
        //let's commit
        branchService.commit(RAPTOR_REPO, testBranchId, context);
        entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        //after commit, we should be able to see the change on main branch
        assertTrue(entityGet != null);
        assertEquals(modifiedName,entityGet.getFieldValues("name").get(0));
        
    }
    
    @Test
    public void modifyNoExistingEntity(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        
        String testBranchId = createSubBranch("modifynonexisting", IBranch.DEFAULT_BRANCH,context);
        JsonEntity modifyInst = newServiceInstance(testBranchId);
        String modifiedName = "Modified Name Test";
        modifyInst.addFieldValue("name", modifiedName);
        
        String noExistingId = ObjectId.get().toString();
        modifyInst.setId(noExistingId);
        
        boolean hasException = false;
        try{
            entityService.modify(modifyInst, context);
        }catch(CmsDalException ex){
            hasException = true;
            assertEquals(DalErrCodeEnum.Entity_NOT_FOUND.getErrorCode(), ex.getErrorCode());
        }
        assertTrue(hasException);
        
    }
    
    @Test
    public void modifyOnNoExistingBranch(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        
        String testBranchId = ObjectId.get().toString();
        JsonEntity modifyInst = newServiceInstance(testBranchId);
        String modifiedName = "Modified Name Test";
        modifyInst.addFieldValue("name", modifiedName);
        
        String noExistingId = ObjectId.get().toString();
        modifyInst.setId(noExistingId);
        
        boolean hasException = false;
        try{
            entityService.modify(modifyInst, context);
        }catch(CmsEntMgrException ex){
            hasException = true;
            assertEquals(EntMgrErrCodeEnum.BRANCH_NOT_FOUND.getErrorCode(), ex.getErrorCode());
        }
        assertTrue(hasException);
        
    }
    
    @Test
    public void replace(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        
        JsonEntity newInst = newServiceInstance(IBranch.DEFAULT_BRANCH);
        String newId = entityService.create(newInst, context); //created on main
        
        String testBranchId = createSubBranch("replacebranch", IBranch.DEFAULT_BRANCH,context);
        JsonEntity replaceInst = newServiceInstance(testBranchId);
        String replaceName = "Replaced Name Test";
        replaceInst.addFieldValue("name", replaceName);
        replaceInst.setId(newId);
        replaceInst.setStatus(StatusEnum.ACTIVE);
        
        //FIXME: jianxu1: can NOT set version of replaceInst to value>0, otherwise, version conflict!!
        entityService.replace(replaceInst, context);
        IEntity entityGet = entityService.get(RAPTOR_REPO,testBranchId,"ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        assertEquals(replaceName, entityGet.getFieldValues("name").get(0));
        
        entityGet = entityService.get(RAPTOR_REPO,IBranch.DEFAULT_BRANCH,"ServiceInstance", newId, context);
        assertTrue(entityGet != null);
        assertTrue(replaceName.equals(entityGet.getFieldValues("name").get(0))==false);
        
        //let's commit
        branchService.commit(RAPTOR_REPO, testBranchId, context);
        entityGet = entityService.get(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "ServiceInstance", newId, context);
        //after commit, we should be able to see the change on main branch
        assertTrue(entityGet != null);
        assertEquals(replaceName,entityGet.getFieldValues("name").get(0));
    }
    
    @Test
    public void createEmbed(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
    }
    
    @Test
    public void deleteEmbed(){
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
    }
    
    @Test
    public void modifyEmdbed(){
        
    }
    
    @Test
    public void replaceEmbed(){
        
    }
    
    @Test
    public void branchCreateWithMetaIndex() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        String testBranchId = createBranch(IBranch.DEFAULT_BRANCH,context);
        
        IBranch branch = branchService.getBranch(RAPTOR_REPO, testBranchId, context);
        Assert.assertNotNull(branch);
        
        JsonEntity newInst = newServiceInstance(testBranchId);
        String newId = entityService.create(newInst, context);
                
        IEntity entityGet = entityService.get(RAPTOR_REPO, testBranchId, "ServiceInstance", newId, context);
        //after commit, we should be able to see that new service instance on main branch
        assertTrue(entityGet != null);
    }
    
    @Test
    public void updateMetaDataWithIndexOnBranch() {
        Repository repo = repoService.getRepository(RAPTOR_REPO);
        IMetadataService metaService = repo.getMetadataService();

        String indexName = "metaWithIndexBranch_index";
        MetaClass metaClass = createMetaclassWithIndex("metaWithIndexBranch", indexName, false);

        metaService.createMetaClass(metaClass);

        //ensure index 
        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertNotNull(getMeta);
        Assert.assertTrue(getMeta.getIndexes().size() == 2);

        List<MetaClass> metadatas = new ArrayList<MetaClass>();
        metadatas.add(metaClass);
        branchService.ensureIndex(RAPTOR_REPO, metadatas, new EntityContext());

        //check index is created
        IBranch branch = branchService.getBranch(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, new EntityContext());
        
        PersistenceContext context = BranchServiceImpl.createEntityPersistenceConext(metaService, branch,
                ConsistentPolicy.defaultPolicy());
        context.setMongoDataSource(getDataSource());

        DBCollection collection = context.getDBCollection(getMeta);
        List<DBObject> indexInfo = collection.getIndexInfo();
        Assert.assertNotNull(indexInfo);
        Assert.assertTrue(indexInfo.size() > 0);
        boolean indexCreated = false;
        for (DBObject indexObject : indexInfo) {
            String name = (String)indexObject.get("name");
            if (indexName.equals(name)){
                indexCreated = true;
            }
        }
        Assert.assertTrue(indexCreated);
    }

    @Test(expected = CmsDalException.class)
    public void insertDupWithUniqueIndex() {
        //create metaclass with unique index
        //insert two entities with same value on the given field.
        Repository repo = repoService.getRepository(RAPTOR_REPO);
        IMetadataService metaService = repo.getMetadataService();
        String uniqueIndex = "metaWithIndexBranch_index";
        MetaClass metaClass = createMetaclassWithIndex("metaWithUniqueIndex", uniqueIndex, true);

        metaService.createMetaClass(metaClass);
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(metaClass);
        branchService.ensureIndex(RAPTOR_REPO, metas, context);
        
        metaClass = metaService.getMetaClass(metaClass.getName());
        Assert.assertNotNull(metaClass);
        
        JsonEntity jsonEntity = newEntity(metaClass);
        entityService.create(jsonEntity, context);

        jsonEntity = newEntity(metaClass);
        entityService.create(jsonEntity, context);
    }

    private JsonEntity newEntity(MetaClass metaClass) {
        MetaClass instCls = metaClass;
        JsonEntity newEntity = new JsonEntity(instCls);
        newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
        newEntity.addFieldValue("name", "Dummy Service Instance for Entity-Branch Test");
        newEntity.addFieldValue("attr_Name", "value");
        return newEntity;
    }

    private MetaClass createMetaclassWithIndex(String metaName, String indexName, boolean unique) {
        //create a new metaclass after branch creation : main branch is created at data loader
        MetaClass metaClass = new MetaClass();
        metaClass.setRepository(RAPTOR_REPO);
        metaClass.setLastModified(Calendar.getInstance().getTime());
        metaClass.setName(metaName);
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        
        MetaAttribute nameAttr = new MetaAttribute();
        nameAttr.setName("name");
        nameAttr.setDataType(DataTypeEnum.STRING);
        metaClass.addField(nameAttr);
        
        IndexInfo index = new IndexInfo(indexName);
        index.getKeyList().add(attribute.getName());
        if (unique) {
            index.addOption(IndexOptionEnum.unique);
        }
        metaClass.addIndex(index);
        return metaClass;
    }
    
*/}
