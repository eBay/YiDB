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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
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
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/*
 * This test case class is for embed entity internal fields test only
 */
@SuppressWarnings("unchecked")
public class EmbedEntityServiceTest extends CMSMongoTest {
    protected static final String        LAST_MODIFIED_TIME  = "lastModifiedTime";
    protected static final String        MANIFEST            = "Manifest";
    protected static final String        SERVICE_INSTANCE    = "ServiceInstance";
    protected static final String        APPLICATION_SERVICE = "ApplicationService";
    protected static final String        COMPUTE             = "Compute";
    protected static RuntimeDataLoader   raptorLoader        = null;
    protected static final String        RAPTOR_REPO         = "raptor-paas";
    protected static final String        RAPTOR_DATA_FILE    = "raptorTopology.json";

    protected static RuntimeDataLoader   deployLoader        = null;
    protected static final String        DEPLOY_REPO         = "software-deployment";
    protected static final String        DEPLOY_DATA_FILE    = "softwareDeploymentRuntime.json";

    protected static RuntimeDataLoader   stratusLoader       = null;
    protected static final String        STRATUS_REPO        = "stratus-ci";
    protected static final String        STRATUS_DATA_FILE   = "stratusRuntime.json";
    protected static final String        NETWORK_ADDRESS     = "NetworkAddress";
    
    protected static final String        CMSDB_REPO          = "cmsdb";
    protected static final String        CMSDB_DATA_FILE     = "cmsdbRuntime.json";
    protected static RuntimeDataLoader   cmsdbLoader       = null;
    
    protected static MetadataDataLoader  metaLoader          = null;

    protected static IRepositoryService  repoService         = null;
    protected static IBranchService      branchService       = null;
    protected static IEntityService      entityService       = null;
    protected static IMetadataService    raptorMetaService   = null;
    protected static IMetadataService    deployMetaService   = null;
    protected static IMetadataService    cmsdbMetaService   = null;
    protected static IPersistenceService persistenceService  = null;

    protected static IMetadataService    stratusMetaService  = null;

    protected static final String        SOURCE_IP           = "127.0.0.1";
    protected static EntityContext       context;

    protected static MongoDataSource     dataSource;
    protected static CMSDBConfig         config;

    @BeforeClass
    public static void setUp() {
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

        // create a testing branch
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

        entityService = ServiceFactory.getEntityService(dataSource, repoService, implementations);

        persistenceService = DalServiceFactory.getPersistenceService(dataSource, implementations);
        raptorMetaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
        deployMetaService = repoService.getRepository(DEPLOY_REPO).getMetadataService();
        stratusMetaService = repoService.getRepository(STRATUS_REPO).getMetadataService();
        cmsdbMetaService = repoService.getRepository(CMSDB_REPO).getMetadataService();

        branchService = ServiceFactory.getBranchService(dataSource, implementations);

        context = newEntityContext();
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
                ConsistentPolicy.safePolicy(), IBranch.DEFAULT_BRANCH);
        pContext.setDbConfig(config);
        pContext.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        return pContext;
    }
    
    @Test
    public void testEmbedReplace(){
        EntityContext context = newEntityContext();
        context.setModifier("jt01");
        
        JsonEntity Person = new JsonEntity(raptorMetaService.getMetaClass("Person"));
        Person.setId("Dep!dep003!team!team310!person!person311");
        Person.addFieldValue("name", "test01");
        Person.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", "Dep!dep003!team!team310!person!person311");
        IEntity personGet = entityService.get(qEntity, context);
//        Assert.assertEquals(personGet.getVersion(), 0);  //EmbedGetCommand equal to root version
        Assert.assertFalse(personGet.hasField(InternalFieldEnum.PVERSION.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        IEntity depGet = entityService.get(qEntity, context);
        int oldVersion = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        qEntity = buildQueryEntity(Person);
        entityService.replace(qEntity, Person, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", "Dep!dep003!team!team310!person!person311");
        personGet = entityService.get(qEntity, context);
        
        Assert.assertEquals(personGet.getCreator(), "unitTestUser");
        Assert.assertEquals(personGet.getModifier(), "jt01");
        Assert.assertEquals(personGet.getFieldValues("name").get(0), "test01");
        Assert.assertEquals(personGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(personGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(1, personGet.getVersion() - oldVersion);
        Assert.assertFalse(personGet.hasField(InternalFieldEnum.PVERSION.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        depGet = entityService.get(qEntity, context);
        
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt01");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        
        assertDepAndTeamVersion(depGet);
    }

    private void assertDepAndTeamVersion(IEntity depGet) {
        List<IEntity> list = (List<IEntity>) depGet.getFieldValues("team");
        int depVersion = depGet.getVersion();
        for (IEntity team : list) {
            Assert.assertEquals(depVersion, team.getVersion());
            Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
        }
    }
    
    @Test
    public void testEmbedModify(){
        EntityContext context = newEntityContext();
        context.setModifier("jt02");
        
        JsonEntity Person = new JsonEntity(raptorMetaService.getMetaClass("Person"));
        Person.setId("Dep!dep003!team!team310!person!person311");
        Person.addFieldValue("name", "test02");
        Person.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", "Dep!dep003!team!team310!person!person311");
        IEntity personGet = entityService.get(qEntity, context);
        Assert.assertNotNull(personGet);
        int oldPersonVersion = personGet.getVersion();
        
        qEntity = buildQueryEntity(Person);
        entityService.modify(qEntity, Person, context);
        
        personGet = entityService.get(qEntity, context);
        Assert.assertEquals(personGet.getCreator(), "unitTestUser");
        Assert.assertEquals(personGet.getModifier(), "jt02");
        Assert.assertEquals(personGet.getFieldValues("name").get(0), "test02");
        Assert.assertEquals(personGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(personGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(personGet.getVersion(), oldPersonVersion + 1);
        Assert.assertFalse(personGet.hasField(InternalFieldEnum.PVERSION.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getVersion(), version + 1);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt02");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        
        assertDepAndTeamVersion(depGet);
    }
    
    @Test
    public void testEmbedCreate01(){
        EntityContext context = newEntityContext();
        context.setModifier("jt03");
        
        JsonEntity person = new JsonEntity(raptorMetaService.getMetaClass("Person"));
        person.setId("Dep!dep003!team!team310!person!person312");
        person.addFieldValue("name", "test03");
        person.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        entityService.create(person, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", "Dep!dep003!team!team310!person!person312");
        IEntity personGet = entityService.get(qEntity, context);
        Assert.assertEquals(personGet.getCreator(), "jt03");
        Assert.assertEquals(personGet.getModifier(), "jt03");
        Assert.assertEquals(personGet.getFieldValues("name").get(0), "test03");
        Assert.assertEquals(personGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(personGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(personGet.getVersion(), version + 1);
        Assert.assertFalse(personGet.hasField(InternalFieldEnum.PVERSION.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getVersion(), version + 1);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt03");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        
        assertDepAndTeamVersion(depGet);
    }
    
    @Test
    public void testEmbedDelete01(){
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep001");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep001!team!team110");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet01);
        context.setVersion(version + 2);

        try {
            entityService.delete(qEntity, context);
            Assert.fail();
        } catch (Exception e) {
            //version check failed
        }
        
        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet02);
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep001");
        depGet = entityService.get(qEntity, context);
        Assert.assertTrue(depGet.getVersion() == version);
    }
    
    @Test
    public void testEmbedDelete02(){
        EntityContext context = newEntityContext();
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep000!team!team020");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet01);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep000!team!team020");
        entityService.delete(qEntity, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep000!team!team020");
        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertNull(entityGet02);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep000");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getVersion(), version + 1);
    }
    
    @Test
    public void testEmbedDelete03() {
        EntityContext context = newEntityContext();
        context.setModifier("jt05");
        IEntity qDepEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep002");
        IEntity depGet = entityService.get(qDepEntity, context);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        
        IEntity qTeamEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep002!team!team210");
        IEntity entityGet01 = entityService.get(qTeamEntity, context);
        Assert.assertNotNull(entityGet01);
        
        entityService.delete(qTeamEntity, context);
        
        IEntity entityGet02 = entityService.get(qTeamEntity, context);
        Assert.assertNull(entityGet02);
        
        depGet = entityService.get(qDepEntity, context);
        Assert.assertTrue(depGet.getVersion() > version);
        Assert.assertEquals("jt05", depGet.getModifier());
        Assert.assertTrue(depGet.getLastModified().getTime() > lastModified);
        Assert.assertEquals(createdTime, depGet.getCreateTime().getTime());
        Assert.assertEquals("unitTestUser", depGet.getCreator());
    }
    
    @Test
    public void testEmbedCreate02() {
        EntityContext context = newEntityContext();
        context.setModifier("jt04");
        
        JsonEntity team320 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team320.setId("Dep!dep002!team!team220");
        team320.addFieldValue("name", "junit320");
        team320.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep002");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep002!team!team220");
        IEntity entityGet01 = entityService.get(qEntity, context);
        Assert.assertNull(entityGet01);
        
        entityService.create(team320, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep002!team!team220");
        IEntity entityGet02 = entityService.get(qEntity, context);
        Assert.assertNotNull(entityGet02);
        Assert.assertFalse(entityGet02.hasField(InternalFieldEnum.PVERSION.getName()));
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep002");
        depGet = entityService.get(qEntity, context);
        Assert.assertTrue(depGet.getVersion() > version);
        Assert.assertEquals(depGet.getModifier(), "jt04");
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertEquals(entityGet02.getVersion(), depGet.getVersion());
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        Assert.assertEquals(entityGet02.getCreator(), "jt04");
    }
    
    @Test
    public void testEmbedCreate03() {
        EntityContext context = newEntityContext();
        context.setModifier("jt06");
        
        MetaClass depMeta = raptorMetaService.getMetaClass("Dep");
        JsonEntity dep = new JsonEntity(depMeta);
        dep.setId("Dep100");
        dep.addFieldValue("name", "dep100");
        dep.setBranchId(IBranch.DEFAULT_BRANCH);
        
        JsonEntity team = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team.setId("Team100");
        team.addFieldValue("name", "team100");
        team.setBranchId(IBranch.DEFAULT_BRANCH);
        
        JsonEntity person= new JsonEntity(raptorMetaService.getMetaClass("Person"));
        person.setId("Person100");
        person.addFieldValue("name", "person100");
        person.setBranchId(IBranch.DEFAULT_BRANCH);
        
        team.addFieldValue("person", person);
        dep.addFieldValue("team", team);
      
        String newDepId = entityService.create(dep, context);
        
        JsonEntity qDep = new JsonEntity(depMeta);
        qDep.setId(newDepId);
        qDep.setBranchId(IBranch.DEFAULT_BRANCH);
        IEntity getDep = entityService.get(qDep, context);
        IEntity getTeam = (IEntity) getDep.getFieldValues("team").get(0);
        team.setId(getTeam.getId());
        IEntity teamGet = entityService.get(team, context);
        Assert.assertNotNull(teamGet);
        
        IEntity getPerson = (IEntity)getTeam.getFieldValues("person").get(0);
        person.setId(getPerson.getId());
        IEntity personGet = entityService.get(person, context);
        Assert.assertNotNull(personGet);
    }
    
    @Test
    public void testEmbedCreate04(){
        EntityContext context = newEntityContext();
        context.setModifier("jt03");
        
        JsonEntity team010 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team010.setId("Dep!dep011!team!team010");
        team010.addFieldValue("name", "test03");
        team010.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep011");
        IEntity depGet = entityService.get(qEntity, context);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        entityService.create(team010, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Team", "Dep!dep011!team!team010");
        IEntity teamGet = entityService.get(qEntity, context);
        Assert.assertEquals(teamGet.getCreator(), "jt03");
        Assert.assertEquals(teamGet.getModifier(), "jt03");
        Assert.assertEquals(teamGet.getFieldValues("name").get(0), "test03");
        Assert.assertEquals(teamGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(teamGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(teamGet.getVersion(), version + 1);
        Assert.assertFalse(teamGet.hasField(InternalFieldEnum.PVERSION.getName()));
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep011");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getVersion(), version + 1);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt03");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        
        assertDepAndTeamVersion(depGet);
    }
    
    @Test
    public void testEmbedCreate05(){
        EntityContext context = newEntityContext();
        context.setModifier("jt03");
        
        // create Team
        JsonEntity team010 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team010.setId("Dep!dep011!team!team011");
        team010.addFieldValue("name", "test03");
        team010.setBranchId(IBranch.DEFAULT_BRANCH);
        
        entityService.create(team010, context);
        
        // create Person
        JsonEntity leader = new JsonEntity(raptorMetaService.getMetaClass("Person"));
        leader.setId("Dep!dep011!team!team011!leader!leader001");
        leader.addFieldValue("name", "test03");
        leader.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep011");
        IEntity depGet = entityService.get(qEntity, context);
        Assert.assertNotNull(depGet);
        int version = depGet.getVersion();
        long lastModified = depGet.getLastModified().getTime();
        long createdTime = depGet.getCreateTime().getTime();
        
        entityService.create(leader, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep011");
        depGet = entityService.get(qEntity, context);
        
        Assert.assertEquals(depGet.getVersion(), version + 1);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt03");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        
        assertDepAndTeamVersion(depGet);
    }
    
    @Test
    public void testEmbedMultiReplace(){
        EntityContext context = newEntityContext();
        context.setModifier("jt01");
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        IEntity entityGet = entityService.get(qEntity, context);
        int version = entityGet.getVersion();
        long lastModified = entityGet.getLastModified().getTime();
        long createdTime = entityGet.getCreateTime().getTime();

        // create team 320 and assertion
        JsonEntity team320 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team320.setId("Dep!dep003!team!team320");
        team320.addFieldValue("name", "junit320");
        team320.setBranchId(IBranch.DEFAULT_BRANCH);
        
        entityService.create(team320, context);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        entityGet = entityService.get(qEntity, context);
        Assert.assertTrue(entityGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(entityGet.getVersion() > version);
        version = entityGet.getVersion();
        lastModified = entityGet.getLastModified().getTime();
        Assert.assertTrue(entityGet.getCreateTime().getTime()==createdTime);
        Assert.assertEquals(entityGet.getCreator(), "unitTestUser");
        
        List<IEntity> list = (List<IEntity>) entityGet.getFieldValues("team");
        int depVersion = entityGet.getVersion();
        Assert.assertEquals(2, list.size());
        
        Assert.assertEquals(entityGet.getModifier(), "jt01");
        
        // assert that we have team 310 and team 320 after team 320 creation
        long createTime = 0l;
        int count = 0;
        for (IEntity team : list) {
            if (team.getId().equals("Dep!dep003!team!team320")) {
                count++;
                Assert.assertEquals(team.getCreator(), "jt01");
                Assert.assertEquals(team.getModifier(), "jt01");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
                createTime = team.getCreateTime().getTime();
            } else if (team.getId().equals("Dep!dep003!team!team310")) {
                count++;
                Assert.assertEquals(team.getCreator(), "unitTestUser");
                Assert.assertEquals(team.getModifier(), "unitTestUser");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            }
        }
        Assert.assertEquals(count, 2);
        Assert.assertEquals(entityGet.getModifier(), "jt01");
        
        
        //replace with full path id  xx!xx!xx!xx, it's modify operation
        context.setModifier("jt02");
        qEntity = buildQueryEntity(entityGet);
        entityService.replace(qEntity, entityGet, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        entityGet = entityService.get(qEntity, context);
//        Assert.assertTrue(entityGet.getLastModified().getTime() > lastModified);
//        lastModified = entityGet.getLastModified().getTime();
        
        list = (List<IEntity>) entityGet.getFieldValues("team");
        depVersion = entityGet.getVersion();
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(entityGet.getCreator(), "unitTestUser");
        
        Assert.assertEquals(entityGet.getModifier(), "jt02");
        
        count = 0;
        for (IEntity team : list) {
            if(team.getId().equals("Dep!dep003!team!team320")){
                count++;
                Assert.assertEquals(team.getCreator(), "jt01");
                Assert.assertEquals(team.getModifier(), "jt02");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertTrue(team.getCreateTime().getTime() == createTime);
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep003!team!team310")) {
                count++;
                Assert.assertEquals(team.getCreator(), "unitTestUser");
                Assert.assertEquals(team.getModifier(), "jt02");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            }
        }
        Assert.assertEquals(count, 2);
        Assert.assertEquals(entityGet.getModifier(), "jt02");
        
        //replace with native id xx , it's create operation
        context.setModifier("jt03");
        
        team320 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team320.setId("team320");
        team320.addFieldValue("name", "junit320");
        team320.setBranchId(IBranch.DEFAULT_BRANCH);
        
        JsonEntity team330 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team330.setId("team330");
        team330.addFieldValue("name", "junit330");
        team330.setBranchId(IBranch.DEFAULT_BRANCH);
        
        List<IEntity> teams = new ArrayList<IEntity>();
        // kept team 310
        teams.add((IEntity)entityGet.getFieldValues("team").get(0));
        teams.add(team320);
        teams.add(team330);
        
        JsonEntity dep003 = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        dep003.setId("dep003");
        dep003.setFieldValues("team", teams);
        dep003.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity queryEntity = buildQueryEntity(dep003);
        entityService.replace(queryEntity, dep003, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep003");
        entityGet = entityService.get(qEntity, context);
        Assert.assertTrue(entityGet.getVersion() > version);
        Assert.assertTrue(entityGet.getLastModified().getTime() > lastModified);
        Assert.assertTrue(entityGet.getCreateTime().getTime()==createdTime);
        
        list = (List<IEntity>) entityGet.getFieldValues("team");
        depVersion = entityGet.getVersion();
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(entityGet.getModifier(), "jt03");
        
        count = 0;
        for (IEntity team : list) {
            if (team.getId().equals("Dep!dep003!team!team320")) {
                count++;
                Assert.assertEquals(team.getCreator(), "jt03");
                Assert.assertEquals(team.getModifier(), "jt03");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertTrue(team.getCreateTime().getTime() > createTime);
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep003!team!team330")) {
                count++;
                Assert.assertEquals(team.getCreator(), "jt03");
                Assert.assertEquals(team.getModifier(), "jt03");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertTrue(team.getCreateTime().getTime() > createTime);
                Assert.assertEquals(depVersion, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep003!team!team310")) {
                // team 310 should be kept
                count ++;
            }
        }
        Assert.assertEquals(count, 3);
    }
    
    @Test
    public void testEmbedMultiModify(){
        EntityContext context = newEntityContext();
        context.setFetchFieldProperty(false);
        context.setModifier("jt01");
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep004");
        IEntity entityGet = entityService.get(qEntity, context);
        long lastModified = entityGet.getLastModified().getTime();
        int version = entityGet.getVersion();
        long createdTime = entityGet.getCreateTime().getTime();

        JsonEntity team420 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team420.setId("Dep!dep004!team!team420");
        team420.addFieldValue("name", "junit420");
        team420.setBranchId(IBranch.DEFAULT_BRANCH);
        
        entityService.create(team420, context);

        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep004");
        entityGet = entityService.get(qEntity, context);
        List<IEntity> list = (List<IEntity>) entityGet.getFieldValues("team");
        Assert.assertEquals(2, list.size());
        
        Assert.assertEquals(entityGet.getModifier(), "jt01");
        Assert.assertTrue(entityGet.getLastModified().getTime()>lastModified);
        lastModified = entityGet.getLastModified().getTime();
        Assert.assertTrue(entityGet.getVersion() > version);
        version = entityGet.getVersion();
        Assert.assertTrue(entityGet.getCreateTime().getTime()==createdTime);
        
        long createTime = 0l;
        int count = 0;
        for (IEntity team : list) {
            if(team.getId().equals("Dep!dep004!team!team420")){
                count++;
                Assert.assertEquals(team.getCreator(), "jt01");
                Assert.assertEquals(team.getModifier(), "jt01");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                createTime = team.getCreateTime().getTime();
                Assert.assertEquals(version, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep004!team!team410")) {
                count++;
                Assert.assertEquals(team.getCreator(), "unitTestUser");
                Assert.assertEquals(team.getModifier(), "unitTestUser");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertEquals(version, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            }
        }
        Assert.assertEquals(count, 2);
        Assert.assertEquals(entityGet.getModifier(), "jt01");
        
        
        //modify with full path id  xx!xx!xx!xx, it's modify operation
        context.setModifier("jt02");
        entityGet.addFieldValue("label", "cloudy");
        
        List<IEntity> teamList = (List<IEntity>) entityGet.getFieldValues("team");
        for(IEntity entity : teamList){
            entity.addFieldValue("label", "b");
        }
        
        qEntity = buildQueryEntity(entityGet);
        entityService.modify(qEntity, entityGet, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep004");
        entityGet = entityService.get(qEntity, context);
        
        Assert.assertTrue(entityGet.getLastModified().getTime() > lastModified);
        lastModified = entityGet.getLastModified().getTime();
        Assert.assertTrue(entityGet.getVersion() > version);
        version = entityGet.getVersion();
        Assert.assertTrue(entityGet.getCreateTime().getTime() == createdTime);
        
        list = (List<IEntity>) entityGet.getFieldValues("team");
        Assert.assertEquals(2, list.size());
        
        Assert.assertEquals(entityGet.getModifier(), "jt02");
        Assert.assertEquals(entityGet.getFieldValues("label").get(0), "cloudy");
        
        Assert.assertEquals(entityGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(entityGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        version = entityGet.getVersion();
        count = 0;
        for (IEntity team : list) {
            if(team.getId().equals("Dep!dep004!team!team420")){
                count++;
                Assert.assertEquals(team.getCreator(), "jt01");
                Assert.assertEquals(team.getModifier(), "jt01");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertTrue(team.getCreateTime().getTime() == createTime);
                Assert.assertEquals(version, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep004!team!team410")) {
                count++;
                Assert.assertEquals(team.getCreator(), "unitTestUser");
                Assert.assertEquals(team.getModifier(), "unitTestUser");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertEquals(version, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            }
        }
        Assert.assertEquals(count, 2);
        Assert.assertEquals(entityGet.getModifier(), "jt02");
        
        //modify with native id xx , it's create operation
        context.setModifier("jt03");
        
        team420 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team420.setId("team420");
        team420.addFieldValue("name", "junit420");
        team420.setBranchId(IBranch.DEFAULT_BRANCH);
        
        JsonEntity team430 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team430.setId("team430");
        team430.addFieldValue("name", "junit430");
        team430.setBranchId(IBranch.DEFAULT_BRANCH);
        
        List<IEntity> teams = new ArrayList<IEntity>();
        teams.add(team420);
        teams.add(team430);
        
        JsonEntity dep004 = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        dep004.setId("dep004");
        dep004.setFieldValues("team", teams);
        dep004.setBranchId(IBranch.DEFAULT_BRANCH);
        
        IEntity queryEntity = buildQueryEntity(dep004);
        entityService.modify(queryEntity, dep004, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep004");
        entityGet = entityService.get(qEntity, context);
        Assert.assertTrue(entityGet.getLastModified().getTime() == lastModified);
        Assert.assertTrue(entityGet.getVersion() == version);
        Assert.assertTrue(entityGet.getCreateTime().getTime() == createdTime);
        version = entityGet.getVersion();
        
        list = (List<IEntity>) entityGet.getFieldValues("team");
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(entityGet.getModifier(), "jt02");
        
        Assert.assertEquals(entityGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(entityGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        count = 0;
        for (IEntity team : list) {
            if (team.getId().equals("Dep!dep004!team!team420")) {
                count++;
                Assert.assertEquals(team.getCreator(), "jt01");
                Assert.assertEquals(team.getModifier(), "jt01");
                Assert.assertEquals(team.getStatus(), StatusEnum.ACTIVE);
                Assert.assertEquals(team.getBranchId(), IBranch.DEFAULT_BRANCH);
                Assert.assertNotNull(team.getCreateTime());
                Assert.assertTrue(team.getCreateTime().getTime() == createTime);
                Assert.assertEquals(version, team.getVersion());
                Assert.assertFalse(team.hasField(InternalFieldEnum.PVERSION.getName()));
            } else if (team.getId().equals("Dep!dep004!team!team430")) {
                Assert.fail();
            }
        }
        Assert.assertEquals(count, 1);
    }
    
    @Test
    public void testEmbedDeleteField(){
        EntityContext context = newEntityContext();
        context.setModifier("jt07");
        String fieldName = "team";
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep005");
        IEntity depGet = entityService.get(qEntity, context);
        long lastModified = depGet.getLastModified().getTime();
        
        JsonEntity team570 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team570.setId("Dep!dep005!team!team570");
        team570.addFieldValue("name", "junit570");
        team570.setBranchId(IBranch.DEFAULT_BRANCH);
        
        entityService.create(team570, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep005");
        depGet = entityService.get(qEntity, context);
        List<IEntity> list = (List<IEntity>) depGet.getFieldValues(fieldName);
        Assert.assertEquals(list.size(), 2);
        
        int version = depGet.getVersion();
        long createdTime = depGet.getCreateTime().getTime();
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getModifier(), "jt07");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertTrue(depGet.getLastModified().getTime() > lastModified);
        lastModified = depGet.getLastModified().getTime();
        
        context.setModifier("jt08");
        JsonEntity dep005 = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        dep005.setId("dep005");
        dep005.addFieldValue("name", "dep");
        dep005.setBranchId(IBranch.DEFAULT_BRANCH);
        
        JsonEntity team510 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team510.setId("Dep!dep005!team!team510");
        team510.addFieldValue("name", "junit570");
        team510.setBranchId(IBranch.DEFAULT_BRANCH);
        dep005.addFieldValue(fieldName, team510);
        
        entityService.pullField(dep005, dep005, fieldName, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep005");
        depGet = entityService.get(qEntity, context);
        list = (List<IEntity>) depGet.getFieldValues(fieldName);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(depGet.getModifier(), "jt08");
        Assert.assertTrue(depGet.getLastModified().getTime()>lastModified);
        Assert.assertTrue(depGet.getVersion() > version);
        Assert.assertTrue(depGet.getCreateTime().getTime()==createdTime);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
    }
    
    @Test
    public void testEmbedDeleteField01(){
        String id = "Dep!dep008!team!team810!person!person811";
        EntityContext context = newEntityContext();
        context.setFetchFieldProperty(false);
        context.setModifier("jt07");
        String fieldName = "address";
        
        IEntity qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep008");
        IEntity depGet = entityService.get(qEntity, context);
        long lastModified = depGet.getLastModified().getTime();
        int version = depGet.getVersion();
        long createdTime = depGet.getCreateTime().getTime();
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", id);
        IEntity personGet = entityService.get(qEntity, context);
        long plastModified = personGet.getLastModified().getTime();
        long pcreatedTime = personGet.getCreateTime().getTime();
        List<IEntity> list = (List<IEntity>) personGet.getFieldValues(fieldName);
        Assert.assertEquals(list.size(), 1);
        
        JsonEntity person811 = new JsonEntity(raptorMetaService.getMetaClass("Person"));
        person811.setId(id);
        person811.addFieldValue("address", "xxx");
        person811.setBranchId(IBranch.DEFAULT_BRANCH);

        // modify some field on the embed entity
        entityService.modifyField(person811, person811, fieldName, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", id);
        personGet = entityService.get(qEntity, context);
        list = (List<IEntity>) personGet.getFieldValues(fieldName);
        Assert.assertEquals(list.size(), 2);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep008");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getModifier(), "jt07");
        Assert.assertTrue(depGet.getLastModified().getTime() > lastModified);
        lastModified = depGet.getLastModified().getTime();
        Assert.assertTrue(depGet.getVersion() > version);
        version = depGet.getVersion();
        Assert.assertTrue(depGet.getCreateTime().getTime() == createdTime);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", id);
        personGet = entityService.get(qEntity, context);
        Assert.assertEquals(personGet.getModifier(), "jt07");
        Assert.assertTrue(personGet.getLastModified().getTime() > plastModified);
        plastModified = personGet.getLastModified().getTime();
        Assert.assertTrue(personGet.getCreateTime().getTime() == pcreatedTime);
        Assert.assertEquals(personGet.getCreator(), "unitTestUser");
        Assert.assertEquals(personGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(personGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(depGet.getVersion(), personGet.getVersion());
        
        context.setModifier("jt09");
        
        person811.setFieldValues("address", Arrays.asList("xxx"));
        person811.addFieldValue(InternalFieldEnum.VERSION.getName(), new Integer(1));
        entityService.pullField(person811, person811, fieldName, context);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", id);
        personGet = entityService.get(qEntity, context);
        list = (List<IEntity>) personGet.getFieldValues(fieldName);
        Assert.assertEquals(list.size(), 1);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Dep", "dep008");
        depGet = entityService.get(qEntity, context);
        Assert.assertEquals(depGet.getModifier(), "jt09");
        Assert.assertTrue(depGet.getLastModified().getTime() > lastModified);
        lastModified = depGet.getLastModified().getTime();
        Assert.assertTrue(depGet.getVersion() > version);
        version = depGet.getVersion();
        Assert.assertTrue(depGet.getCreateTime().getTime() == createdTime);
        Assert.assertEquals(depGet.getCreator(), "unitTestUser");
        Assert.assertEquals(depGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(depGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        
        qEntity = buildQueryEntity(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, "Person", id);
        personGet = entityService.get(qEntity, context);
        Assert.assertEquals(personGet.getModifier(), "jt09");
        Assert.assertTrue(personGet.getLastModified().getTime() > plastModified);
        plastModified = personGet.getLastModified().getTime();
        Assert.assertTrue(personGet.getCreateTime().getTime() == pcreatedTime);
        Assert.assertEquals(personGet.getCreator(), "unitTestUser");
        Assert.assertEquals(personGet.getStatus(), StatusEnum.ACTIVE);
        Assert.assertEquals(personGet.getBranchId(), IBranch.DEFAULT_BRANCH);
        Assert.assertEquals(depGet.getVersion(), personGet.getVersion());
    }
    
    @Test
    public void testModifyWithInvalidId() {
        EntityContext context = newEntityContext();
        JsonEntity team420 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team420.setId("Dep!dep012!team!team210");
        team420.addFieldValue("name", "junit420");
        team420.setBranchId(IBranch.DEFAULT_BRANCH);

        JsonEntity dep002 = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        dep002.setId("dep002");
        dep002.addFieldValue("team", team420);
        dep002.setBranchId(IBranch.DEFAULT_BRANCH);
        try {
            IEntity qEntity = buildQueryEntity(dep002);
            entityService.modify(qEntity, dep002, context);
        } catch (CmsDalException e) {
            Assert.assertEquals(CmsDalException.DalErrCodeEnum.INVALID_EMBED_ID, e.getErrorEnum());
        }
    }
    
    @Test
    public void testReplaceWithInvalidId() {
        EntityContext context = newEntityContext();
        JsonEntity team420 = new JsonEntity(raptorMetaService.getMetaClass("Team"));
        team420.setId("Dep!dep012!team!team210");
        team420.addFieldValue("name", "junit420");
        team420.setBranchId(IBranch.DEFAULT_BRANCH);

        JsonEntity dep002 = new JsonEntity(raptorMetaService.getMetaClass("Dep"));
        dep002.setId("dep002");
        dep002.addFieldValue("team", team420);
        dep002.setBranchId(IBranch.DEFAULT_BRANCH);

        try {
            IEntity qEntity = buildQueryEntity(dep002);
            entityService.replace(qEntity, dep002, context);
            Assert.fail();
        } catch (CmsDalException e) {
            Assert.assertEquals(CmsDalException.DalErrCodeEnum.INVALID_EMBED_ID, e.getErrorEnum());
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
    
    @Test
    public void testEmbedModify_nonArray() {
        EntityContext context = newEntityContext();
        // create manifest with nouse
        JsonEntity manifest = createManifestWithNoUse(context);
        IEntity getMani = entityService.get(manifest, context);
        String noUseId = ((IEntity) getMani.getFieldValues("noUses").get(0)).getId();
        // modify the NoUse with the given Id
        JsonEntity modifyNoUse = new JsonEntity(deployMetaService.getMetaClass("NoUse"));
        modifyNoUse.setId(noUseId);
        modifyNoUse.setBranchId(IBranch.DEFAULT_BRANCH);
        String newNoUseName = "liasu-liasu";
        modifyNoUse.addFieldValue("name", newNoUseName);
        entityService.modify(modifyNoUse, modifyNoUse, context);
        // now assertion
        IEntity getNoUse = entityService.get(modifyNoUse, context);
        Assert.assertEquals(newNoUseName, getNoUse.getFieldValues("name").get(0));
    }

    private JsonEntity createManifestWithNoUse(EntityContext context) {
        JsonEntity manifest = new JsonEntity(deployMetaService.getMetaClass("Manifest"));
        manifest.setBranchId(IBranch.DEFAULT_BRANCH);
        manifest.addFieldValue("name", "manifest-name-for-embed-modify");
        JsonEntity noUse = new JsonEntity(deployMetaService.getMetaClass("NoUse"));
        noUse.setBranchId(IBranch.DEFAULT_BRANCH);
        noUse.addFieldValue("name", "liasu");
        manifest.addFieldValue("noUses", noUse);
        String id = entityService.create(manifest, context);
        manifest.setId(id);
        return manifest;
    }

    @Test
    public void modifyEmbedFromTop() {
        JsonEntity nodeServer = new JsonEntity(cmsdbMetaService.getMetaClass("NodeServer"));
        nodeServer.setId("51f977a3171b7e36601ad3ea");
        nodeServer.setBranchId(IBranch.DEFAULT_BRANCH);
        JsonEntity capacity = new JsonEntity(cmsdbMetaService.getMetaClass("ResourceCapacity"));
        capacity.setBranchId(IBranch.DEFAULT_BRANCH);
        capacity.setId("NodeServer!51f977a3171b7e36601ad3ea!capacities!5216e0ba171bd2d7a58c87bc");

        IEntity getCapa = entityService.get(capacity, context);
        int fieldNumber = getCapa.getFieldNames().size();

        getCapa.addFieldValue("used", 4);
        nodeServer.addFieldValue("capacities", getCapa);
        entityService.modify(nodeServer, nodeServer, context);

        IEntity getCapa2 = entityService.get(capacity, context);
        Assert.assertEquals(fieldNumber, getCapa2.getFieldNames().size());
    }

}
