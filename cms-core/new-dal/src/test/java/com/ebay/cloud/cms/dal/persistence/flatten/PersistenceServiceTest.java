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

package com.ebay.cloud.cms.dal.persistence.flatten;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.dal.common.flatten.RaptorEntityGenerator;
import com.ebay.cloud.cms.dal.common.flatten.StratusEntityGenerator;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class PersistenceServiceTest extends CMSMongoTest{

	private static final String MAIN = "main";
    private static final String APPLICATION_SERVICE = "ApplicationService";
    private static final String COMPANY = "company";
    private static NewPersistenceServiceImpl persistenceService = null;
	private static IRepositoryService repoService = null;
	private static IMetadataService raptorMetaService = null;
	private static IMetadataService stratusMetaService;

	private static PersistenceContext raptorContext = null;
	
	private static final String RAPTOR_REPO = "raptor-paas";
    private static final String STRATUS_REPO = "stratus-ci";
	private static final String BRANCH_TEST = "test";
	private static MetadataDataLoader metaLoader = null;

	@BeforeClass
	public static void setUp(){
		String connectionString = CMSMongoTest.getConnectionString();
		MongoDataSource dataSource = new MongoDataSource(connectionString);
		metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadTestDataFromResource();
		repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
		stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
		persistenceService = new NewPersistenceServiceImpl(dataSource);
		raptorContext = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata, ConsistentPolicy.safePolicy(), BRANCH_TEST);
        StratusEntityGenerator.loadStratusTopology(DBCollectionPolicy.SplitByMetadata);
	}
	
    @Test
    @SuppressWarnings("unchecked")
    public void newResourceGroup() {
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        
        MetaClass rgMeta = stratusMetaService.getMetaClass("ResourceGroup");
        NewBsonEntity entity = new NewBsonEntity(rgMeta);
        entity.setBranchId(MAIN);
        
        MetaClass clusterMeta = stratusMetaService.getMetaClass("PoolCluster");
        NewBsonEntity cluster = new NewBsonEntity(clusterMeta);
        cluster.setBranchId(MAIN);
        String id = persistenceService.create(cluster, context);
        
        NewBsonEntity queryEntity = new NewBsonEntity(clusterMeta);
        queryEntity.setId(id);
        queryEntity.setBranchId(MAIN);        
        IEntity getCluster = persistenceService.get(queryEntity, context);
        
        MetaClass refAppMeta = stratusMetaService.getMetaClass("RefApplicationService");
        NewBsonEntity refApp = new NewBsonEntity(refAppMeta);
        refApp.setBranchId(MAIN);
        id = persistenceService.create(refApp, context);
        
        queryEntity = new NewBsonEntity(refAppMeta);
        queryEntity.setId(id);
        queryEntity.setBranchId(MAIN);
        IEntity getRefApp = persistenceService.get(queryEntity, context);
        
        List<IEntity> children = new ArrayList<IEntity>();
        children.add(getCluster);
        children.add(getRefApp);
        entity.setFieldValues("children", children);
        id = persistenceService.create(entity, context);

        queryEntity = new NewBsonEntity(rgMeta);
        queryEntity.setId(id);
        queryEntity.setBranchId(MAIN);
        IEntity rg = persistenceService.get(queryEntity, context);
        
        List<IEntity> getChildren = (List<IEntity>)rg.getFieldValues("children");
        Assert.assertEquals("PoolCluster", getChildren.get(0).getType());
        Assert.assertEquals("RefApplicationService", getChildren.get(1).getType());
    }
	
	private static NewBsonEntity newApplicationService(){
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ApplicationService.name());
        NewBsonEntity newEntity = new NewBsonEntity(metaClass);
        newEntity.addFieldValue("name", "ApplcationService-1");
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }
	
	private static NewBsonEntity newServiceInstance(int seq){
		MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
		NewBsonEntity newEntity = new NewBsonEntity(metaClass);
		newEntity.addFieldValue("name", "ServiceInstance-" + seq);
		newEntity.setBranchId(BRANCH_TEST);		
		newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
		return newEntity;
	}

    private static NewBsonEntity newCompute(int seq) {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.Compute.name());
        NewBsonEntity newEntity = new NewBsonEntity(metaClass);
        newEntity.addFieldValue("name", "Compute-" + seq + "-t" + System.currentTimeMillis());
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        newEntity.setBranchId(BRANCH_TEST);
        return newEntity;
    }

	private static NewBsonEntity createServiceInstance(int seq){
		NewBsonEntity entity1 = newServiceInstance(seq);
		String branchId = entity1.getBranchId();
		String newId = persistenceService.create(entity1, raptorContext);
		String metaType = entity1.getType();
		NewBsonEntity queryEntity = buildQueryEntity(branchId, metaType, newId);
        NewBsonEntity saveEntity = persistenceService.get(queryEntity, raptorContext);
		return saveEntity;
	}
	
	@Test
    public void createServiceInstanceWithId(){
	    NewBsonEntity entity1 = newServiceInstance(2891);
	    String id = "createServiceInstanceWithId";
	    entity1.setId(id);
	    
        String newId = persistenceService.create(entity1, raptorContext);
        
        Assert.assertEquals(id, newId);
    }

    @Test
    public void batchCreate() {
        IEntity entity1 = newServiceInstance(2891);
        String branchId = entity1.getBranchId();
        String id1 = "batchCreate-serviceinstance";
        entity1.setId(id1);
        
        IEntity entity2 = newApplicationService();
        String id2 = "batchCreate-applicationservice";
        entity2.setId(id2);
        entity2.addFieldValue("name", id2);
        
        NewBsonEntity tempE = new NewBsonEntity(entity1.getMetaClass());
        tempE.setId(entity1.getId());
        entity2.addFieldValue("services", tempE);
        
        persistenceService.batchCreate(Arrays.asList(entity1, entity2), raptorContext);

        NewBsonEntity queryEntity = buildQueryEntity(branchId, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), id1);
        Assert.assertNotNull(persistenceService.get(queryEntity,raptorContext));
        
        queryEntity = buildQueryEntity(branchId, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), id2);
        Assert.assertNotNull(persistenceService.get(queryEntity, raptorContext));
    }
	
	@Test (expected = CmsDalException.class)
    public void batchCreateNoId(){
        NewBsonEntity entity1 = newServiceInstance(2891);
        String id1 = "batchCreate-serviceinstance";
        entity1.setId(id1);
        
        NewBsonEntity entity2 = newApplicationService();
        
        NewBsonEntity tempE = new NewBsonEntity(entity1.getMetaClass());
        tempE.setId(entity1.getId());
        entity2.addFieldValue("services", tempE);
        
        persistenceService.batchCreate(Arrays.<IEntity> asList(entity1, entity2), raptorContext);
    }

	@Test
    public void batchCreateEmptyParameter(){
        List<String> newIds = persistenceService.batchCreate(null, raptorContext);
        Assert.assertTrue(newIds.isEmpty());

        newIds = persistenceService.batchCreate(Collections.<IEntity>emptyList(), raptorContext);
        Assert.assertTrue(newIds.isEmpty());
    }

	@Test
	public void createAndGet(){
		IEntity entity1 = createServiceInstance(1);
		IEntity entity2 = persistenceService.get(entity1, raptorContext);
    	
		String id1 = entity1.getId();
		String id2 = entity2.getId();
		Assert.assertEquals(id1, id2);
		
		Date createTime = entity1.getCreateTime();
		Date lastModified = entity1.getLastModified();
		Assert.assertEquals(createTime, lastModified);
		
		String name1 = (String) entity1.getFieldValues("name").get(0);
		String name2 = (String) entity2.getFieldValues("name").get(0);
		Assert.assertEquals(name1, name2);			
	}

    @Test
    public void createAndGetWithDefaultValue() {
        NewBsonEntity entity1 = createServiceInstance(1);
        NewBsonEntity entity2 = persistenceService.get(entity1, raptorContext);

        String id1 = entity1.getId();
        String id2 = entity2.getId();
        Assert.assertEquals(id1, id2);

        String name1 = (String) entity1.getFieldValues("name").get(0);
        String name2 = (String) entity2.getFieldValues("name").get(0);
        Assert.assertEquals(name1, name2);

        // https field of service instance has default value
        List<?> value = entity1.getFieldValues("https");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.size() > 0);
        Assert.assertTrue((Boolean)value.get(0));//verify default value specified in metclass.json
    }
    
	@Test
    public void createAndDelete() {
        NewBsonEntity saveEntity = createServiceInstance(1);

        NewBsonEntity getEntity = persistenceService.get(saveEntity, raptorContext);
        Assert.assertEquals(saveEntity.getId(), getEntity.getId());

        persistenceService.delete(saveEntity, raptorContext);
        IEntity deletedEntity = persistenceService.get(saveEntity, raptorContext);
        Assert.assertTrue(deletedEntity == null);
    }
	
    @Test
    public void createWithDuplicateReferences(){
        NewBsonEntity entity1 = createServiceInstance(2);
        
        NewBsonEntity appServ = newApplicationService();
        appServ.addFieldValue("services", entity1);
        appServ.addFieldValue("services", entity1);
        
        try {
            persistenceService.create(appServ, raptorContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertTrue(e.getMessage().startsWith("Reference field services contains duplicate references!"));
        }
    }
	
	@Test 
    public void replace() throws Exception {
		NewBsonEntity saveEntity = createServiceInstance(1);
		int originalVersion = saveEntity.getVersion();

		Thread.sleep(100);
		String replaceName = "This is replace entity";
		NewBsonEntity replaceEntity = newServiceInstance(1);
		replaceEntity.setId(saveEntity.getId());
		replaceEntity.addFieldValue("name", replaceName);
		replaceEntity.setVersion(originalVersion);
		replaceEntity.setParentVersion(0);
		replaceEntity.setStatus(StatusEnum.ACTIVE);
		persistenceService.replace(replaceEntity, raptorContext);

		String id = replaceEntity.getId();
		
        NewBsonEntity getEntity = persistenceService.get(replaceEntity, raptorContext);
		String newId   = getEntity.getId();
		String newName = (String) getEntity.getFieldValues("name").get(0); 
		int newVersion = getEntity.getVersion();
		Assert.assertEquals(id, newId);
		Assert.assertEquals(newName, replaceName);
		Assert.assertEquals(newVersion, originalVersion+1);
		
		Date createTime = getEntity.getCreateTime();
        Date lastModified = getEntity.getLastModified();
        
        Assert.assertTrue("create time < last modified", createTime.getTime() < lastModified.getTime());
	}
	
    @Test
    public void replaceWithDuplicateReferences(){
        NewBsonEntity entity1 = createServiceInstance(3);
        NewBsonEntity entity2 = createServiceInstance(3);
        
        NewBsonEntity appServ = newApplicationService();
        appServ.addFieldValue("services", entity1);
        appServ.addFieldValue("services", entity2);
        String id = persistenceService.create(appServ, raptorContext);
        
        appServ.setId(id);
        appServ.addFieldValue("services", entity1);
        
        try {
            persistenceService.replace(appServ, raptorContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertTrue(e.getMessage().startsWith("Reference field services contains duplicate references!"));
        }
    }
	
	@Test
    public void replaceServiceInstanceWithId(){
        NewBsonEntity entity1 = newServiceInstance(2892);
        String id = "replaceServiceInstanceWithId";
        entity1.setId(id);
        entity1.setParentVersion(IEntity.NO_VERSION);
        persistenceService.replace(entity1, raptorContext);
        
        NewBsonEntity saved1 = persistenceService.get(entity1, raptorContext);
        Assert.assertNotNull(saved1);
        Assert.assertEquals(id, saved1.getId());
        Assert.assertEquals("ServiceInstance-2892", saved1.getFieldValues("name").get(0));
        
        NewBsonEntity entity2 = newServiceInstance(2892);
        entity2.setId(id);
        entity2.setParentVersion(IEntity.NO_VERSION);
        entity2.addFieldValue("name", "ServiceInstance-abc");
        persistenceService.replace(entity2, raptorContext);
        
        NewBsonEntity saved2 = persistenceService.get(entity2, raptorContext);
        
        Assert.assertEquals("ServiceInstance-abc", saved2.getFieldValues("name").get(0));
    }

	@Test 
    public void replaceWithDefaultValue() throws Exception {
	    NewBsonEntity saveEntity = createServiceInstance(1);
        int originalVersion = saveEntity.getVersion();
        
        String replaceName = "This is replace entity";
        NewBsonEntity replaceEntity = newServiceInstance(1);
        replaceEntity.setId(saveEntity.getId());
        replaceEntity.addFieldValue("name", replaceName);
        replaceEntity.setVersion(originalVersion);
        replaceEntity.setParentVersion(0);
        replaceEntity.setStatus(StatusEnum.ACTIVE);
        persistenceService.replace(replaceEntity, raptorContext);
        
        String id = replaceEntity.getId();
        
        NewBsonEntity getEntity = persistenceService.get(replaceEntity, raptorContext);
        String newId   = getEntity.getId();
        String newName = (String) getEntity.getFieldValues("name").get(0); 
        int newVersion = getEntity.getVersion();
        Assert.assertEquals(id, newId);
        Assert.assertEquals(newName, replaceName);
        Assert.assertEquals(newVersion, originalVersion+1);
	    
        // https field of service instance has default value
        List<?> value = getEntity.getFieldValues("https");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.size() > 0);
        Assert.assertTrue((Boolean)value.get(0));//verify default value specified in metclass.json
	}
	
	@Test 
	public void replaceWithVersion() throws Exception{
		NewBsonEntity replaceEntity = createServiceInstance(1);
		int originalVersion = replaceEntity.getVersion();
		
		String replaceName = "This is replace entity with Version";
		replaceEntity.addFieldValue("name", replaceName);
		replaceEntity.setVersion(originalVersion);
		persistenceService.replace(replaceEntity,
		        raptorContext);
	}
	
	
	@Test
	public void modify01() throws InterruptedException{
		NewBsonEntity saveEntity = createServiceInstance(1);
		int originalVersion = saveEntity.getVersion();

		Thread.sleep(100);
		String modifyName = "This is modify entity";
		NewBsonEntity modifyEntity = newServiceInstance(1);
		modifyEntity.setId(saveEntity.getId());
		modifyEntity.addFieldValue("name", modifyName);
		modifyEntity.setCreateTime(saveEntity.getCreateTime());
        persistenceService.modify(modifyEntity,
                raptorContext);

        IEntity getEntity = persistenceService.get(modifyEntity, raptorContext);
		Assert.assertEquals(originalVersion+1, getEntity.getVersion());
		Assert.assertEquals(modifyName, getEntity.getFieldValues("name").get(0));
		
		Date createTime = getEntity.getCreateTime();
        Date lastModified = getEntity.getLastModified();        
        Assert.assertTrue("create time < last modified", createTime.getTime() < lastModified.getTime());
	}
	
   @Test
    public void modifyNotUseDefaultValue(){
       IEntity saveEntity = createServiceInstance(1);
       // https field of service instance has default value
       List<?> value = saveEntity.getFieldValues("https");
       Assert.assertNotNull(value);
       Assert.assertTrue(value.size() > 0);
       Assert.assertTrue((Boolean)value.get(0));//verify default value specified in metclass.json

       int originalVersion = saveEntity.getVersion();

       String modifyName = "This is modify entity";
       NewBsonEntity modifyEntity = newServiceInstance(1);
       modifyEntity.setId(saveEntity.getId());
       modifyEntity.addFieldValue("name", modifyName);
       List<Boolean> httpValue = new ArrayList<Boolean>();
       httpValue.add(false);
       modifyEntity.setFieldValues("https", httpValue); // override the default value
       persistenceService.modify(modifyEntity,
                raptorContext);

        IEntity getEntity = persistenceService.get(modifyEntity, raptorContext);
       Assert.assertEquals(originalVersion+1, getEntity.getVersion());
       Assert.assertEquals(modifyName, getEntity.getFieldValues("name").get(0));

       // verify https field value is changed from default value(true) to specified value(false)
       value = getEntity.getFieldValues("https");
       Assert.assertNotNull(value);
       Assert.assertTrue(value.size() == 1);
       Assert.assertTrue(((Boolean)value.get(0)) == false);

        // modify again with out specify the value for https, make sure this time the default value is not used
        NewBsonEntity modifyEntity2 = newServiceInstance(1);
        modifyEntity2.addFieldValue("name", "modify name 2");
        modifyEntity2.setId(saveEntity.getId());
        persistenceService.modify(modifyEntity2,
                raptorContext);

        getEntity = persistenceService.get(modifyEntity, raptorContext);
        // verify https field value is not changed after this modification
        value = getEntity.getFieldValues("https");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.size() == 1);
        Assert.assertTrue(((Boolean)value.get(0)) == false);
   }

	@Test
	public void modifyOverrideDefaultValue() {
        NewBsonEntity saveEntity = createServiceInstance(1);
        // https field of service instance has default value
        List<?> value = saveEntity.getFieldValues("https");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.size() > 0);
        Assert.assertTrue((Boolean)value.get(0));//verify default value specified in metclass.json

        int originalVersion = saveEntity.getVersion();

        String modifyName = "This is modify entity";
        NewBsonEntity modifyEntity = newServiceInstance(1);
        modifyEntity.setId(saveEntity.getId());
        modifyEntity.addFieldValue("name", modifyName);
        List<Boolean> httpValue = new ArrayList<Boolean>();
        httpValue.add(false);
        modifyEntity.setFieldValues("https", httpValue); // set the value
        persistenceService.modify(modifyEntity,
                raptorContext);

        IEntity getEntity = persistenceService.get(modifyEntity, raptorContext);
        Assert.assertEquals(originalVersion+1, getEntity.getVersion());
        Assert.assertEquals(modifyName, getEntity.getFieldValues("name").get(0));

        // verify https field value is changed from default value(true) to specified value(false)
        value = getEntity.getFieldValues("https");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.size() == 1);
        Assert.assertTrue(((Boolean)value.get(0)) == false);
    }
	
	@Test
	public void modifyWithVersion01(){
		NewBsonEntity modifyEntity = createServiceInstance(1);
		String modifyName = "This is modify entity with version";
		modifyEntity.addFieldValue("name", modifyName);		
        persistenceService.modify(modifyEntity, raptorContext);
        modifyEntity.setVersion(1);
        persistenceService.modify(modifyEntity, raptorContext);
	}
	
	@Test (expected=CmsDalException.class)
    public void modifyWithVersion02(){
        NewBsonEntity modifyEntity = createServiceInstance(1);
        String modifyName = "This is modify entity with version";
        modifyEntity.addFieldValue("name", modifyName);     
        persistenceService.modify(modifyEntity, raptorContext);
        modifyEntity.setVersion(0);
        persistenceService.modify(modifyEntity, raptorContext);
    }

	@Test
    public void modifySetReferenceNull() {
        // create a service with a referencing compute
        NewBsonEntity serviceInstance = newServiceInstance(1112);
        NewBsonEntity compute = newCompute(1112);
        String computeId = persistenceService.create(compute, raptorContext);
        compute.setId(computeId);
        serviceInstance.addFieldValue("runsOn", compute);
        String serviceInstanceId = persistenceService.create(serviceInstance, raptorContext);

        // now try to modify set the reference as null
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), serviceInstanceId);
        NewBsonEntity getServ = persistenceService.get(queryEntity, raptorContext);
        Assert.assertTrue(!getServ.getFieldValues("runsOn").isEmpty());// not
                                                                       // empty
                                                                       // after
                                                                       // creation
        try {
            getServ.setFieldValues("runsOn", null);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected, not to support null as a reference value
        }
//        persistenceService.modify(getServ, context);
        // verify
//        getServ = persistenceService.get(BRANCH_TEST, serviceInstanceId,
//                RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), context);
//        Assert.assertTrue(getServ.hasField("runsOn"));
//        Assert.assertTrue(getServ.getFieldValues("runsOn").size() == 1);
//        Assert.assertTrue(getServ.getFieldValues("runsOn").get(0) == null);
    }

    @Test
    public void modifySetReferenceListNull() {
        // create a application service with referencing service instance with a referencing compute
        NewBsonEntity appServ = newApplicationService();
        List<String> namelist = new ArrayList<String>();
        namelist.add("appservice-name-modify-null-reference");
        appServ.setFieldValues("name", namelist);

        NewBsonEntity serviceInstance = newServiceInstance(1112);
        NewBsonEntity compute = newCompute(1112);
        String computeId = persistenceService.create(compute, raptorContext);
        compute.setId(computeId);
        serviceInstance.addFieldValue("runsOn", compute);
        String serviceInstanceId = persistenceService.create(serviceInstance, raptorContext);
        serviceInstance.setId(serviceInstanceId);

        List<IEntity> servlist = new ArrayList<IEntity>();
        servlist.add(serviceInstance);
        appServ.setFieldValues("services", servlist);

        String newAppId = persistenceService.create(appServ, raptorContext);

        // now try to modify set the reference as null
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), newAppId);
        NewBsonEntity getApp = persistenceService.get(queryEntity, raptorContext);
        Assert.assertTrue(!getApp.getFieldValues("services").isEmpty());// not
                                                                       // empty
                                                                       // after
                                                                       // creation
        getApp.setFieldValues("services", null);
        persistenceService.modify(getApp, raptorContext);

        // verify
        getApp = persistenceService.get(queryEntity, raptorContext);
        Assert.assertTrue(getApp.hasField("services"));
        Assert.assertTrue(getApp.getFieldValues("services").isEmpty());// should
                                                                      // be
                                                                      // empty
    }
    
    @Test
    public void modifyWithDuplicateReferences(){
        NewBsonEntity entity1 = createServiceInstance(4);
        NewBsonEntity entity2 = createServiceInstance(4);
        
        NewBsonEntity appServ = newApplicationService();
        appServ.addFieldValue("services", entity1);
        appServ.addFieldValue("services", entity2);
        String id = persistenceService.create(appServ, raptorContext);
        
        appServ.setId(id);
        appServ.addFieldValue("services", entity1);
        
        try {
            persistenceService.modify(appServ, raptorContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertTrue(e.getMessage().startsWith("Reference field services contains duplicate references!"));
        }
    }

	@Test (expected=CmsDalException.class)
    public void modifyNotExist(){
        NewBsonEntity modifyEntity = createServiceInstance(1);
        modifyEntity.setId("123");
        String modifyName = "This is modify entity with version";
        modifyEntity.addFieldValue("name", modifyName);     
        persistenceService.modify(modifyEntity, raptorContext);
    }

	@Test
    public void createWithConstant() {
        NewBsonEntity entity = newEmployeeEntity();
        String newId = persistenceService.create(entity, raptorContext);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, entity.getType(), newId);
        NewBsonEntity getEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertEquals(entity.getFieldValues(COMPANY).get(0),
                getEntity.getFieldValues(COMPANY).get(0));
    }

    private NewBsonEntity newEmployeeEntity() {
        MetaClass meta = raptorMetaService.getMetaClass("Employee");
        MetaAttribute companyField = (MetaAttribute) meta.getFieldByName(COMPANY);
        Assert.assertTrue(companyField.isConstant());
        NewBsonEntity entity = new NewBsonEntity(meta);
        entity.setBranchId(BRANCH_TEST);
        entity.addFieldValue("name", "employee-test");
        entity.addFieldValue(COMPANY, "ebay-cloud");
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
        return entity;
    }

	@Test
    public void modifyConstant() {
        NewBsonEntity entity = newEmployeeEntity();
        String newId = persistenceService.create(entity, raptorContext);

        NewBsonEntity modifyEntity = newEmployeeEntity();
        modifyEntity.setId(newId);
        entity.addFieldValue(COMPANY, "ebay-cloud-coe");

        try {
            persistenceService.modify(modifyEntity, raptorContext);
            Assert.fail();
        } catch (CmsDalException e) {
            // expected
            Assert.assertEquals(CmsDalException.DalErrCodeEnum.CONSTANT_FIELD_MODIFICATION, e.getErrorEnum());
        }
    }

	@Test
    public void replaceWithConstant() {
        NewBsonEntity entity = newEmployeeEntity();
        String newId = persistenceService.create(entity, raptorContext);

        NewBsonEntity replaceEntity = newEmployeeEntity();
        replaceEntity.setId(newId);
        replaceEntity.addFieldValue(COMPANY, "ebay-cloud-coe");
        replaceEntity.setParentVersion(0);
        // replace are allowed to update the constant field value
        persistenceService.replace(replaceEntity, raptorContext);
    }

	@Test
    public void deleteAndGet() {
        NewBsonEntity saveEntity = createServiceInstance(1);
        Assert.assertTrue(saveEntity != null);

        persistenceService.delete(saveEntity, raptorContext);

        IEntity getEntity = persistenceService.get(saveEntity, raptorContext);
        Assert.assertTrue(getEntity == null);
    }

    @Test
    public void markDeleteAndGet() {
        NewBsonEntity saveEntity = createServiceInstance(1);
        Assert.assertTrue(saveEntity != null);

        String branchId = saveEntity.getBranchId();
        String metaType = saveEntity.getType();
        String id = saveEntity.getId();
        MetaClass metaClass = raptorMetaService.getMetaClass(metaType);
        NewBsonEntity bsonEntity = new NewBsonEntity(metaClass);
        bsonEntity.setBranchId(branchId);
        bsonEntity.setId(id);

        persistenceService.markDeleted(bsonEntity, raptorContext);

        IEntity getEntity = persistenceService.get(saveEntity, raptorContext);
        Assert.assertTrue(getEntity.getStatus() == StatusEnum.DELETED);
    }

	@Test
    public void testStrongReferenceCreateGood(){        
        // create service instance
        NewBsonEntity service1 = createServiceInstance(1);
        NewBsonEntity service2 = createServiceInstance(2);
        // create application service with service instance
        NewBsonEntity application1 = newApplicationService();
        application1.addFieldValue("services", service1);
        application1.addFieldValue("services", service2);
        persistenceService.create(application1,
                raptorContext);
    }
	
	@Test 
    public void testStrongReferenceDeleteNoRef(){
	    // create service instance
        NewBsonEntity service1 = createServiceInstance(1);
        
        NewBsonEntity deleteService = new NewBsonEntity(service1);
        // create application service with service instance
        NewBsonEntity application1 = newApplicationService();
        application1.addFieldValue("services", service1);
        persistenceService.create(application1, raptorContext);
        // remove the reference 
        application1.setFieldValues("services", Collections.EMPTY_LIST);
        persistenceService.modify(application1, raptorContext);
        // delete
        persistenceService.delete(deleteService, raptorContext);
    }	
	
	@Test 
	public void uniqueIDVialation(){

	}
	
	@Test 
	public void wrongIDwhenModify(){

	}
	
	@Test(expected= CmsDalException.class)
	public void updateWithWrongVersion(){
        NewBsonEntity entity1 = newServiceInstance(2899);
        String id = "updateServiceInstanceWithWrongVersion";
        entity1.setId(id);
        persistenceService.create(entity1, raptorContext);
        entity1.setVersion(entity1.getVersion() + 1);//set wrong version
        persistenceService.modify(entity1, raptorContext);
	}

    @Test
    public void testEnvironment() {
		String envType = "Environment";
		MetaClass envCls = raptorMetaService.getMetaClass(envType);
		NewBsonEntity envEntity = new NewBsonEntity(envCls);
		envEntity.setBranchId(BRANCH_TEST);
		envEntity.addFieldValue("name", "Dummy Test Environment");
		envEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
		//String envId = ObjectId.get().toString();
		//envEntity.setId(envId);
		
		//now let's replace COS
		MetaClass cosCls = raptorMetaService.getMetaClass("ClassOfService");
		NewBsonEntity cosEntity = new NewBsonEntity(cosCls);
		cosEntity.addFieldValue("name", "Test Second ClassOfService");
		envEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
		cosEntity.setBranchId(BRANCH_TEST);
		String cosId = ObjectId.get().toString();
		cosEntity.setId(cosId);
		
		envEntity.addFieldValue("cos", cosEntity);
        String envId = persistenceService.create(envEntity,
                raptorContext);
		
		cosEntity.setBranchId(BRANCH_TEST);//branch id is reset here
		NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, envType, envId);
        NewBsonEntity getCosEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertNotNull(getCosEntity);
		
		NewBsonEntity cosRefInst = (NewBsonEntity)getCosEntity.getFieldValues("cos").get(0);
		Assert.assertTrue(cosRefInst.hasField("name")==false);
    }
    
    private static final String idxName = "indexName";
    private static final String indexMetaName = "metaWithIndex";

    @Test
    public void testEnsureIndex() {
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        MetaClass metaClass = createMetaClassWithIndex(indexMetaName, idxName, metaService);

        Assert.assertTrue(getCollectionIndexMap(metaClass).size() > 1);
        Assert.assertTrue(getCollectionIndexMap(metaClass).containsKey(idxName));
    }

    private Map<String, DBObject> getCollectionIndexMap(MetaClass metaClass) {
        DBCollection collection = raptorContext.getDBCollection(metaClass);
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

    private MetaClass createMetaClassWithIndex(String metaclassName, String idxName, IMetadataService metaService) {
        MetaClass metaClass = new MetaClass();
        metaClass.setName(metaclassName);
        metaClass.setRepository(RAPTOR_REPO);
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);

        IndexInfo index = new IndexInfo(idxName);
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);
        MetaClass createMeta = metaService.createMetaClass(metaClass, new MetadataContext());
        
        Assert.assertNotNull(metaService.getMetaClass(metaClass.getName()));

        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(createMeta);
        persistenceService.ensureIndex(metas, raptorContext, true);

        return createMeta;
    }

    @Test
    public void testAddEnsureIndex() {
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        MetaClass metaClass = metaService.getMetaClass(indexMetaName);
        if (metaClass == null) {
            metaClass = createMetaClassWithIndex(indexMetaName, idxName, metaService);
        }

        IndexInfo indexInfo = new IndexInfo("addedIndex");
        indexInfo.addKeyField(metaClass.getFieldNames().iterator().next());
        indexInfo.addOption(IndexOptionEnum.unique);
        metaClass.addIndex(indexInfo);

        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(metaClass);
        persistenceService.ensureIndex(metas, raptorContext, true);

        Assert.assertTrue(getCollectionIndexMap(metaClass).containsKey(indexInfo.getIndexName()));
    }

    @Test
    public void testAddInvalidEnsureIndex() {
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        MetaClass metaClass = metaService.getMetaClass("Team");

        IndexInfo indexInfo = new IndexInfo("addedIndex");
        indexInfo.addKeyField("team");
        indexInfo.addOption(IndexOptionEnum.unique);
        metaClass.addIndex(indexInfo);

        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(metaClass);

        try {
            persistenceService.ensureIndex(metas, raptorContext, true);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("does not have field"));
        }
    }

    @Test
    public void testDeleteEnsureIndex() {
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        MetaClass metaClass = metaService.getMetaClass(indexMetaName);
        if (metaClass == null) {
            metaClass = createMetaClassWithIndex(indexMetaName, idxName, metaService);
        }
        int count =  getCollectionIndexMap(metaClass).size();
        //no indexes added, means we are going to delete a index
        MetaOption metaOption = metaClass.getOptions();
        IndexInfo index = metaOption.getIndexByName(idxName);
        metaOption.dropIndex(index);

        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(metaClass);
        persistenceService.ensureIndex(metas, raptorContext, true);
        Map<String, DBObject> indexMap = getCollectionIndexMap(metaClass);
        Assert.assertEquals(1, count - indexMap.size());
        Assert.assertFalse(indexMap.containsKey(idxName));
    }

    @Test
    public void testUpdateIndex() {
        IMetadataService metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        final String indexMetaName = "NewMetaWithIndex";
        MetaClass metaClass = metaService.getMetaClass(indexMetaName);
        if (metaClass == null) {
            metaClass = createMetaClassWithIndex(indexMetaName, idxName, metaService);
        }
        Map<String, DBObject> oldIndexes = getCollectionIndexMap(metaClass);
        IndexInfo info = metaClass.getIndexByName(idxName);
        // asset equals
        final int oldSize = info.getKeyList().size();
        Assert.assertTrue(oldIndexes.containsKey(info.getIndexName()));
        Assert.assertEquals(((DBObject)oldIndexes.get(info.getIndexName()).get("key")).keySet().size(), info.getKeyList().size());
        
        List<MetaClass> metas = new ArrayList<MetaClass>();
        metas.add(metaClass);

        // update index : add field
        {
            info.addKeyField("_oid");
            final int newSize = info.getKeyList().size();
            persistenceService.ensureIndex(metas, raptorContext, true);
            Map<String, DBObject> newIndexes = getCollectionIndexMap(metaClass);
            Assert.assertTrue(newIndexes.containsKey(info.getIndexName()));
            Assert.assertEquals(((DBObject) newIndexes.get(info.getIndexName()).get("key")).keySet().size(), info
                    .getKeyList().size());
            Assert.assertEquals(oldSize + 1, newSize);
        }
        
        // update index : remove field
        {
            info.getKeyList().remove("_oid");
            final int newSize = info.getKeyList().size();
            persistenceService.ensureIndex(metas, raptorContext, true);
            Map<String, DBObject> newIndexes = getCollectionIndexMap(metaClass);
            Assert.assertTrue(newIndexes.containsKey(info.getIndexName()));
            Assert.assertEquals(((DBObject) newIndexes.get(info.getIndexName()).get("key")).keySet().size(), info
                    .getKeyList().size());
            Assert.assertEquals(oldSize, newSize);
        }
        {
            // update index : add sparse option
            info.addOption(IndexOptionEnum.sparse);
            persistenceService.ensureIndex(metas, raptorContext, true);
            Map<String, DBObject> new2Indexes = getCollectionIndexMap(metaClass);
            Assert.assertTrue(new2Indexes.containsKey(info.getIndexName()));
            DBObject indexDbo = new2Indexes.get(info.getIndexName());
            for (IndexOptionEnum ioe : info.getIndexOptions()) {
                Assert.assertTrue(indexDbo.containsField(ioe.name().toLowerCase()));
            }
        }
        {
            // update index : remove option
            info.removeOption(IndexOptionEnum.sparse);
            persistenceService.ensureIndex(metas, raptorContext, true);
            Map<String, DBObject> newDbMap = getCollectionIndexMap(metaClass);
            Assert.assertTrue(newDbMap.containsKey(idxName));
            DBObject indexDbo = newDbMap.get(idxName);
            Assert.assertFalse(indexDbo.containsField(IndexOptionEnum.sparse.name().toLowerCase()));
        }
    }

    @Test
    public void testJsonTypeField01() {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        NewBsonEntity newEntity = new NewBsonEntity(metaClass);
        newEntity.setBranchId(BRANCH_TEST);
        newEntity.addFieldValue("name", "jsontype-si01");      
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("f1", "val1");
        newEntity.addFieldValue("properties", node);
        
        String newId = persistenceService.create(newEntity, raptorContext);
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), newId);
        NewBsonEntity saveEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertEquals(saveEntity.getFieldValues("properties").size(), 1);
        Object propField = saveEntity.getFieldValues("properties").get(0);
        DBObject bsonField = (DBObject)propField;
        Assert.assertEquals(bsonField.get("f1"), "val1");
    }
    
    @Test
    public void testJsonTypeField02() {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        NewBsonEntity newEntity = new NewBsonEntity(metaClass);
        newEntity.setBranchId(BRANCH_TEST);
        newEntity.addFieldValue("name", "jsontype-si02");      
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        NewBsonEntity inner = new NewBsonEntity(metaClass);
        inner.addFieldValue("name", "jsontype-inner-02");
        inner.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        newEntity.addFieldValue("properties", inner);
        
        String newId = persistenceService.create(newEntity, raptorContext);
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), newId);
        NewBsonEntity saveEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertEquals(saveEntity.getFieldValues("properties").size(), 1);
        Object propField = saveEntity.getFieldValues("properties").get(0);
        DBObject bsonField = (DBObject)propField;
        Assert.assertEquals(bsonField.get("_t"), "ServiceInstance");
    }
    
    @Test
    public void testJsonTypeField03() {
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        NewBsonEntity newEntity = new NewBsonEntity(metaClass);
        newEntity.setBranchId(BRANCH_TEST);
        newEntity.addFieldValue("name", "jsontype-si02");      
        newEntity.addFieldValue("properties", "{\"f1\" : \"val1\"}");
        newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        
        String newId = persistenceService.create(newEntity, raptorContext);
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), newId);
        NewBsonEntity saveEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertEquals(saveEntity.getFieldValues("properties").size(), 1);
        Object propField = saveEntity.getFieldValues("properties").get(0);
        DBObject bsonField = (DBObject)propField;
        Assert.assertEquals(bsonField.get("f1"), "val1");
    }

    @Test (expected=IllegalArgumentException.class)
    public void testContext02() {
        PersistenceContext testContext = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata, ConsistentPolicy.safePolicy(), BRANCH_TEST);
        testContext.getMetaClass("errorType");
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument01() {
        NewBsonEntity queryEntity = buildQueryEntity(null, APPLICATION_SERVICE, "123");
        persistenceService.get(queryEntity, raptorContext);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument02() {
        NewBsonEntity queryEntity = buildQueryEntity("123", APPLICATION_SERVICE, null);
        persistenceService.get(queryEntity, raptorContext);        
    }
    
    @Test (expected=NullPointerException.class)
    public void testIllegalArgument03() {
        NewBsonEntity queryEntity = buildQueryEntity("123", null, APPLICATION_SERVICE);
        persistenceService.get(queryEntity, raptorContext);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument04() {
        // miss branch
        MetaClass meta = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        NewBsonEntity entity = new NewBsonEntity(meta);
        persistenceService.delete(entity, raptorContext);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument05() {
        MetaClass meta = raptorMetaService.getMetaClass(APPLICATION_SERVICE);
        NewBsonEntity entity = new NewBsonEntity(meta);
        entity.setBranchId(BRANCH_TEST);
        persistenceService.delete(entity, raptorContext);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument07() {
        persistenceService.ensureIndex(null, raptorContext, true);        
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testIllegalArgument08() {
        persistenceService.ensureIndex(new ArrayList<MetaClass>(), raptorContext, true);        
    }

    @Test
    public void testGivenCreateTimeTest() {
        NewBsonEntity entity = newApplicationService();
        entity.addFieldValue("name", "ApplcationService-1" + "testGivenCreateTimeCreate");
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        Date date = new Date(1000);
        entity.setCreateTime(date);
        String id = persistenceService.create(entity, raptorContext);
        Assert.assertNotNull(id);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, entity.getMetaClass().getName(), id);
        NewBsonEntity getEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertEquals(date, getEntity.getCreateTime());
    }
    
    @Test
    public void testGivenLastModifieTimeCreate() {
        NewBsonEntity entity = newApplicationService();
        entity.addFieldValue("name", "ApplcationService-1" + "testGivenLastModifieTimeCreate");
        Date date = new Date(1000);
        entity.setLastModified(date);
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        String id = persistenceService.create(entity, raptorContext);
        Assert.assertNotNull(id);

        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, entity.getMetaClass().getName(), id);
        NewBsonEntity getEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertFalse(date.equals(getEntity.getLastModified()));
    }

    @Test
    public void testGivenLastModifieTimeModify() {
        //create a entity
        NewBsonEntity entity = newApplicationService();
        entity.addFieldValue("name", "ApplcationService-1" + "testGivenLastModifieTimeModify");
        entity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitUnitTest");
        String id = persistenceService.create(entity, raptorContext);
        Assert.assertNotNull(id);

        Date date = new Date(1000);
        String archtier = "modifiedArchTier";

        //verify last modified
        NewBsonEntity queryEntity = buildQueryEntity(BRANCH_TEST, entity.getMetaClass().getName(), id);
        NewBsonEntity getEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertNotNull(getEntity.getLastModified());
        Assert.assertFalse(date.equals(getEntity.getLastModified()));
        Assert.assertTrue(getEntity.getFieldValues("archTier").isEmpty());

        //update last modified by modify
        entity.setLastModified(date);
        List<String> archTiers = new ArrayList<String>();
        archTiers.add(archtier);
        entity.setFieldValues("archTier", archTiers);
        persistenceService.modify(entity, raptorContext);

        // verify : get again
        queryEntity = buildQueryEntity(BRANCH_TEST, entity.getMetaClass().getName(), id);
        getEntity = persistenceService.get(queryEntity, raptorContext);
        Assert.assertFalse(date.equals(getEntity.getLastModified()));
    }
    
    @Test
    public void createAndFilterGet() {
        PersistenceContext pc = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);

        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        pc.addQueryField(InternalFieldEnum.ID.getDbName());
        pc.addQueryField(InternalFieldEnum.TYPE.getDbName());
        pc.addQueryField(metaClass.getFieldByName("https").getFlattenValueDbName());
        pc.addQueryField(metaClass.getFieldByName("manifestDiff").getFlattenValueDbName());

        NewBsonEntity entity1 = createServiceInstance(1);
        NewBsonEntity entity2 = persistenceService.get(entity1, pc);

        // case 1: queried fields should exist if any
        Assert.assertNotNull(entity2);
        Assert.assertNotNull(entity2.getId());
        Assert.assertNotNull(entity2.getType());
        Assert.assertNotNull(entity2.getFieldValues("https"));
        Assert.assertTrue(entity2.getFieldValues("https").size() > 0);
        Assert.assertArrayEquals(entity1.getFieldValues("https").toArray(), entity2.getFieldValues("https").toArray());

        Assert.assertNotNull(entity2.getFieldValues("manifestDiff"));
        Assert.assertArrayEquals(entity1.getFieldValues("manifestDiff").toArray(),
                entity2.getFieldValues("manifestDiff").toArray());

        // case 2: not queried fields should not be returned
        Assert.assertTrue(entity1.getFieldNames().size() > pc.getQueryFields().size());
        for (String fieldName : entity1.getFieldNames()) {
            String dbName = metaClass.getFieldByName(fieldName).getFlattenValueDbName();
            if (!pc.getQueryFields().contains(dbName)) {
                Assert.assertTrue(entity2.getFieldValues(fieldName) == null || entity2.getFieldValues(fieldName).isEmpty());
            }
        }
    }

    @Test
    public void batchUpdate() {
        PersistenceContext pc = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_TEST);
        List<IEntity> entities = new ArrayList<IEntity>();

        NewBsonEntity entity1 = newServiceInstance(2891);
        String branchId = entity1.getBranchId();
        String id1 = "batchcreate-for-Update-serviceinstance";
        entity1.setId(id1);

        NewBsonEntity entity2 = newApplicationService();
        String id2 = "batchcreate-for-Update-applicationservice";
        entity2.setId(id2);
        entity2.addFieldValue("name", id2);

        NewBsonEntity tempE = new NewBsonEntity(entity1.getMetaClass());
        tempE.setId(entity1.getId());
        entity2.addFieldValue("services", tempE);

        entities.add(entity1);
        entities.add(entity2);

        persistenceService.batchCreate(entities, pc);

        NewBsonEntity queryEntity = buildQueryEntity(branchId, RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), id1);
        Assert.assertNotNull(persistenceService.get(queryEntity, raptorContext));
        queryEntity = buildQueryEntity(branchId, RaptorEntityGenerator.TypeEnum.ApplicationService.name(), id2);
        Assert.assertNotNull(persistenceService.get(queryEntity, raptorContext));

        persistenceService.batchUpdate(entities, pc);
    }

    @Test
    public void batchUpdateEmptyParameter() {
        // case: no error for empty parameters input
        persistenceService.batchUpdate(null, raptorContext);
        persistenceService.batchUpdate(Collections.<IEntity> emptyList(), raptorContext);
    }

    @Test
    public void testMandatoryReferenceCreate() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Environment");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setBranchId(MAIN);
        entity.addFieldValue("label", "local-dev-env");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        try {
            persistenceService.create(entity, context);
            Assert.fail();
        } catch (CmsDalException cde) {
            // expected
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, cde.getErrorEnum());
        }
    }

    @Test
    public void testMandatoryFieldCreateWithNull() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Manifest");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id1");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        entity.addFieldValue("softwareVersion", null);
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);

        try {
            persistenceService.create(entity, context);
            Assert.fail();
        } catch (CmsDalException cde) {
            // expected
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, cde.getErrorEnum());
        }
    }
    
    @Test
    public void testMandatoryFieldModify() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Manifest");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id2");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        entity.addFieldValue("softwareVersion", "1.0");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        persistenceService.create(entity, context);
        
        entity.addFieldValue("softwareVersion", null);
        persistenceService.modify(entity, context);
    }
    
    @Test
    public void testMandatoryFieldReplace() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Manifest");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id3");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        entity.addFieldValue("softwareVersion", "1.0");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        persistenceService.create(entity, context);
        
        entity.addFieldValue("softwareVersion", null);
        persistenceService.replace(entity, context);
    }
    
    @Test
    public void testMandatoryFieldDeleteField() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Manifest");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id4");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        entity.addFieldValue("softwareVersion", "1.0");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        persistenceService.create(entity, context);
        
        try {
            persistenceService.deleteField(entity, "softwareVersion", context);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
            Assert.assertEquals(e.getMessage(), "Could not delete mandatory field!");
        }
    }
    
    @Test
    public void testMandatoryFieldModifyField() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Manifest");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id5");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        entity.addFieldValue("softwareVersion", "1.0");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        persistenceService.create(entity, context);
        
        entity.addFieldValue("softwareVersion", null);
        persistenceService.modifyField(entity, "softwareVersion", context);
    }
    
    @Test
    public void modifyFieldWithDuplicateReferences(){
        NewBsonEntity entity1 = createServiceInstance(4);
        NewBsonEntity entity2 = createServiceInstance(4);
        
        NewBsonEntity appServ = newApplicationService();
        appServ.addFieldValue("services", entity1);
        appServ.addFieldValue("services", entity2);
        String id = persistenceService.create(appServ, raptorContext);
        
        NewBsonEntity queryApp = persistenceService.get(appServ, raptorContext);
        List<?> services = queryApp.getFieldValues("services");
        Assert.assertEquals(2, services.size());
        
        NewBsonEntity newAppServ = newApplicationService();
        newAppServ.setId(id);
        newAppServ.addFieldValue("services", entity1);
        
        persistenceService.modifyField(newAppServ, "services", raptorContext);
        queryApp = persistenceService.get(appServ, raptorContext);
        services = queryApp.getFieldValues("services");
        Assert.assertEquals(2, services.size());
        
        
        NewBsonEntity newAppServ2 = newApplicationService();
        newAppServ2.setId(id);
        newAppServ2.addFieldValue("services", entity1);
        newAppServ2.addFieldValue("services", entity2);
        newAppServ2.addFieldValue("services", entity1);
        
        try {
            persistenceService.modifyField(newAppServ2, "services", raptorContext);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertTrue(e.getMessage().startsWith("Reference field services contains duplicate references!"));
        }
    }
    
    @Test
    public void testMandatoryReferenceReplace() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Environment");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setId("any-ref-id");
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        try {
            persistenceService.replace(entity, context);
            Assert.fail();
        } catch (CmsDalException cde) {
            // expected
            cde.printStackTrace();
            Assert.assertEquals(DalErrCodeEnum.MISS_RUNTIME_FIELD, cde.getErrorEnum());
        }
    }
    
    @Test
    public void testReplaceNoOid() {
        IMetadataService stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        MetaClass envMetaclass = stratusMetaService.getMetaClass("Environment");
        NewBsonEntity entity = new NewBsonEntity(envMetaclass);
        entity.setBranchId(MAIN);
        entity.setParentVersion(0);
        entity.addFieldValue("label", "local-dev-env");
        MetaClass cosMetaclass = stratusMetaService.getMetaClass("ClassOfService");
        NewBsonEntity cos = new NewBsonEntity(cosMetaclass);
        cos.setId("any-other-id");
        entity.addFieldValue("classOfService", cos);
        PersistenceContext context = new PersistenceContext(stratusMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), MAIN);
        try {
            persistenceService.replace(entity, context);
            Assert.fail();
        } catch (CmsDalException cde) {
            // expected
            cde.printStackTrace();
            Assert.assertEquals(DalErrCodeEnum.ENTITY_NOT_FOUND, cde.getErrorEnum());
        }
    }
    
    @Test
    public void testCount() {
        List<String> ids = new ArrayList<String>(3);
        
        NewBsonEntity entity1 = newServiceInstance(3000);
        String newId = persistenceService.create(entity1, raptorContext);
        ids.add(newId);
        
        NewBsonEntity entity2 = newServiceInstance(3001);
        newId = persistenceService.create(entity2, raptorContext);
        ids.add(newId);
        
        NewBsonEntity entity3 = newServiceInstance(3002);
        newId = persistenceService.create(entity3, raptorContext);
        ids.add(newId);
        
        long count = persistenceService.count(entity1.getMetaClass(), ids, BRANCH_TEST, raptorContext);
        Assert.assertEquals(3, count);
    }

    private static NewBsonEntity buildQueryEntity(String branchname, String metadata, String oid) {
        MetaClass meta = raptorMetaService.getMetaClass(metadata);
        NewBsonEntity queryEntity = new NewBsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

    @Test
    public void testContextAdditionalFilter() {
        NewBsonEntity entity1 = createServiceInstance(1);
        NewBsonEntity entity2 = (NewBsonEntity)persistenceService.get(entity1, raptorContext);
        
        String id1 = entity1.getId();
        String id2 = entity2.getId();
        Assert.assertEquals(id1, id2);
        
        Date createTime = entity1.getCreateTime();
        Date lastModified = entity1.getLastModified();
        Assert.assertEquals(createTime, lastModified);

        String name1 = (String) entity1.getFieldValues("name").get(0);
        String name2 = (String) entity2.getFieldValues("name").get(0);
        Assert.assertEquals(name1, name2);          

        // get with filter
        PersistenceContext context = new PersistenceContext(raptorContext);

        Map<String, List<SearchCriteria>> additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
        MetaField nameField = metaClass.getFieldByName("name");
        ISearchField searchField = new SelectionField(nameField, NewDalSearchStrategy.getInstance());
        SearchCriteria criteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.NE, entity1
                .getFieldValues("name").get(0));
        additionalCriteria.put(RaptorEntityGenerator.TypeEnum.ServiceInstance.name(), Arrays.asList(criteria));

        context.setAdditionalCriteria(additionalCriteria);
        
        NewBsonEntity getEntity2 = (NewBsonEntity)persistenceService.get(entity1, context);
        Assert.assertNull(getEntity2);
    }
}
