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

package com.ebay.cloud.cms.sysmgmt.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.DalServiceFactory.RegistrationEnum;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.entmgr.entity.CallbackContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.ModifyAction;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IndexExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;
import com.ebay.cloud.cms.sysmgmt.state.IEventListener;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class CMSServerTest extends CMSMongoTest {

	private static final String ENVIRONMENT = "Environment";
    private static final String SOURCE_IP = "127.0.0.1";
	private static final String MANIFEST_VERSION = "ManifestVersion";
	private static final String MANIFEST = "Manifest";
	private static final String SERVICE_INSTANCE = "ServiceInstance";
	private static final String DEPLOY_REPO = "software-deployment";
	private static final String STRATUS_REPO = "stratus-ci";
	private static final String APPLICATION_SERVICE = "ApplicationService";
	private static final String RESOURCE_GROUP = "ResourceGroup";
	private static final String CMSDB_REPO = "cmsdb";
	private static final String NODE_SERVER = "NodeServer";
	private static final String SKU_CONFIGURATION = "SkuConfiguration";
	private static final String DEP = "Dep";
	private static final String BRANCH_MAIN = "main";
	private static final String RAPTOR_PAAS = "raptor-paas";
	private static final String RAPTOR_TEST_DATA = "raptorTopology.json";
	private static CMSServer server;
	private static EntityContext entityContext;
	private static QueryContext raptorQueryContext;
	private static MetadataContext metaContext;

	// private static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		String delimeter = "/";
		URL configFile = CMSServerTest.class.getResource("/" + CMSConfig.CONFIG_FILE);
		String path = configFile.getPath();
		System.setProperty(CMSConfig.CMS_HOME, path.substring(0, path.lastIndexOf(delimeter)));
	}

	private class EchoListener implements IEventListener {
		@Override
		public void onEvent(State from, State to) {
			System.out.println("From state:" + from + " to:" + to);
		}
	}

	@BeforeClass
	public static void setup() {
		MongoDataSource dataSource = new MongoDataSource(getConnectionString());

		MetadataDataLoader.getInstance(dataSource).loadTestDataFromResource();
		MetadataDataLoader.getInstance(dataSource).loadCMSDBMetaDataFromResource();

		IRepositoryService repositoryService = RepositoryServiceFactory.createRepositoryService(dataSource,
				"localCMSServer");
		RuntimeDataLoader.getDataLoader(dataSource, repositoryService, RAPTOR_PAAS).load(RAPTOR_TEST_DATA);
		RuntimeDataLoader.getDataLoader(dataSource, repositoryService, "software-deployment").load(
				"softwareDeploymentRuntime.json");
		RuntimeDataLoader.getDataLoader(dataSource, repositoryService, STRATUS_REPO).load("stratusRuntime.json");
		RuntimeDataLoader.getDataLoader(dataSource, repositoryService, CMSDB_REPO).load("cmsdbRuntime.json");

		server = CMSServer.getCMSServer();
		server.start();

		entityContext = newEntityContext();

		raptorQueryContext = newQueryContext(RAPTOR_PAAS);

		metaContext = newMetadataContext();
	}

	private static MetadataContext newMetadataContext() {
		MetadataContext context = new MetadataContext();
		context.setSourceIp(SOURCE_IP);
		context.setSubject("unitTestUser");
		context.addAdditionalParameter("dal", RegistrationEnum.hierarchy.name());
		return context;
	}

	private static QueryContext newQueryContext(String repo) {
		QueryContext context = new QueryContext(repo, BRANCH_MAIN);
		context.setSourceIP(SOURCE_IP);
		context.setDbConfig(config);
		context.setRegistration(server.getDalImplementation(RegistrationEnum.hierarchy.name()));
		return context;
	}

	private static EntityContext newEntityContext() {
		EntityContext context = new EntityContext();
		context.setSourceIp(SOURCE_IP);
		context.setModifier("unitTestUser");
		context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
		context.setFetchFieldProperty(true);
		context.setDbConfig(config);
		return context;
	}

	@Test
	public void testStart() {
		EchoListener ls = new EchoListener();
		server.registerEvent(ls);
		server.start();

		server.getRepositories(CMSPriority.NEUTRAL, metaContext);

		server.removeEvent(ls);
	}

	@Test
	public void testPause() {
		server.pause();

		expectCannoServe();
	}

	@Test
	public void testResume() {
		server.resume();
	}

	@Test
	public void testShutdown() {
		// server.shutdown();

		// expectCannoServe();

		server.start();
	}

	private void expectCannoServe() {
		try {
			server.getRepositories(CMSPriority.NEUTRAL, metaContext);
			Assert.fail();
		} catch (CannotServeException e) {
			// expected
		}
	}

	@Test
	public void testCreateBranch() {
		Repository repoInst = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS);

		IBranch branch = createBranch(repoInst, "testNewBranch-cmsServer");

		IBranch getBranch = server.createBranch(CMSPriority.NON_CRITICAL, branch, entityContext);
		Assert.assertNotNull(getBranch);
	}

	private IBranch createBranch(Repository repoInst, String branchName) {
		Branch branch = new Branch();
		branch.setMainBranch(true);
		branch.setId(branchName);
		branch.setRepositoryName(repoInst.getRepositoryName());
		return branch;
	}

	@Test
	public void testGetBranch() {
		Assert.assertNotNull(server.getBranch(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, entityContext));
	}

	@Test
	public void testGetBranches() {
		Assert.assertNotNull(server.getMainBranches(CMSPriority.NEUTRAL, RAPTOR_PAAS, entityContext));
	}

	@Test
	public void testGetServiceStatuses() {
		Assert.assertNotNull(server.getServiceStatuses());
	}

	@Test
	public void testGetServiceStatus() {
		Assert.assertNotNull(server.getServiceStatus(CMSServer.HEALTHY_SERVICE));

		Assert.assertNull(server.getServiceStatus("non-service"));
	}

	@Test
	public void testSetServiceStatus() {
		server.setServiceStatus(CMSServer.HEALTHY_SERVICE, IManagementServices.ServiceStatus.running);

		server.setServiceStatus("non-service", IManagementServices.ServiceStatus.running);

		server.setServiceStatus(CMSServer.HEALTHY_SERVICE, IManagementServices.ServiceStatus.stopped);
		server.setServiceStatus(CMSServer.HEALTHY_SERVICE, IManagementServices.ServiceStatus.running);
	}

	@Test
	public void testGetStatistics() {
		Assert.assertNotNull(server.getStatistics());
	}

	@Test
	public void testGetStatisticsString() {
		Assert.assertNotNull(server.getStatistics(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS));

		Assert.assertNull(server.getStatistics("non-metric"));
	}

	@Test
	public void testGetState() {
		Assert.assertEquals(State.normal, server.getState());
	}

	@Test
	public void testGetRepositories() {
		Assert.assertNotNull(server.getRepositories(CMSPriority.NEUTRAL, metaContext));
	}

	@Test
	public void testGetRepository() {
		Assert.assertNotNull(server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS));
	}

	@Test
	public void testCreateRepository() {
		Repository createRepo = new Repository();
		createRepo.setRepositoryName("testNewRepo-cmsServer");
		Repository repo = server.createRepository(CMSPriority.NON_CRITICAL, createRepo);

		Assert.assertNotNull(server.getRepository(CMSPriority.NON_CRITICAL, repo.getRepositoryName()));

		IBranch branch = server.getBranch(CMSPriority.NON_CRITICAL, repo.getRepositoryName(), IBranch.DEFAULT_BRANCH,
				entityContext);
		Assert.assertNotNull(branch);
	}

	@Test
	public void testDeleteRepository() {
		Map<String, Object> newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_REPOSITORY_DELETE, false);
		server.config(newConfigs);

		Repository createRepo = new Repository();
		String repositoryName = "testDeleteRepo-cmsServer";
		createRepo.setRepositoryName(repositoryName);
		server.createRepository(CMSPriority.NON_CRITICAL, createRepo);
		Repository getRepo = server.getRepository(CMSPriority.NEUTRAL, repositoryName);
		Assert.assertNotNull(getRepo);

		try {
			server.deleteRepository(CMSPriority.NEUTRAL, repositoryName);
			Assert.fail();
		} catch (Exception e) {
			// expected as the configuration "SysAllowRepositoryDelete" should
			// be false
		}

		newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_REPOSITORY_DELETE, true);
		server.config(newConfigs);

		server.deleteRepository(CMSPriority.NEUTRAL, repositoryName);
		try {
			server.getRepository(CMSPriority.NEUTRAL, repositoryName);
			Assert.fail();
		} catch (RepositoryNotExistsException rnee) {
			// expected
		}
	}

	@Test
	public void testGetMetaClasses() {
		MetadataContext context = newMetadataContext();
		Assert.assertNotNull(server.getMetaClasses(CMSPriority.NEUTRAL, RAPTOR_PAAS, context));

		try {
			server.getMetaClasses(CMSPriority.NEUTRAL, "non-repo", context);
			Assert.fail();
		} catch (RepositoryNotExistsException e) {
			// expected
		}
	}

	@Test
	public void testGetMetaClass() {
		Assert.assertNotNull(server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, APPLICATION_SERVICE));

		try {
			server.getMetaClass(CMSPriority.NEUTRAL, "non-repo", "meta1");
			Assert.fail();
		} catch (RepositoryNotExistsException e) {
			// expected
		}
	}

	@Test
	public void testGetMetaHistory() {
		Assert.assertNotNull(server.getMetaHistory(CMSPriority.NEUTRAL, RAPTOR_PAAS, APPLICATION_SERVICE, metaContext));

		try {
			server.getMetaHistory(CMSPriority.NEUTRAL, "non-repo", APPLICATION_SERVICE, metaContext);
			Assert.fail();
		} catch (RepositoryNotExistsException e) {
			// expected
		}
	}

	@Test
	public void testDeleteMetaClass() {
		Map<String, Object> newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		server.config(newConfigs);

		String metaType = "ResourceContainer";

		MetaClass mc = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, metaType);
		Assert.assertNotNull(mc);
		int version = mc.getVersion();

		server.deleteMetadata(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);

		mc = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, metaType);
		Assert.assertNull(mc);

		List<IEntity> entities = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);
		Assert.assertTrue(entities.size() > 0);
		String opType = (String) entities.get(0).getFieldValues(HistoryMetaClass.OperType).get(0);
		Assert.assertEquals("deleteMetaClass", opType);

		// try create one with the same name again
		MetaClass newMeta = new MetaClass();
		newMeta.setName(metaType);
		newMeta.setRepository(STRATUS_REPO);
		MetaAttribute ma = new MetaAttribute();
		ma.setName("name");
		ma.setDataType(DataTypeEnum.STRING);
		newMeta.addField(ma);

		MetaRelationship mr = new MetaRelationship();
		mr.setName("resources");
		mr.setCardinality(CardinalityEnum.Many);
		mr.setRefDataType("Resource");
		mr.setRelationType(RelationTypeEnum.Reference);
		newMeta.addField(mr);

		List<MetaClass> metas = new ArrayList<MetaClass>();
		metas.add(newMeta);

		MetadataContext context = new MetadataContext();
		context.setSourceIp(SOURCE_IP);
		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, metas, context);

		MetaClass getMeta = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, metaType);
		Assert.assertNotNull(getMeta);
		Assert.assertEquals(version + 1, getMeta.getVersion());
	}

	@Test
	public void testMetaClassVersion() {
		Map<String, Object> newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		server.config(newConfigs);

		// 1. create meta class
		String metaType = "TestMetaClassVersion";
		MetaClass newMeta = new MetaClass();
		newMeta.setName(metaType);
		newMeta.setRepository(STRATUS_REPO);
		MetaAttribute ma = new MetaAttribute();
		ma.setName("name1");
		ma.setDataType(DataTypeEnum.STRING);
		newMeta.addField(ma);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(newMeta), metaContext);

		// 2. check no history
		List<IEntity> entities = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);
		Assert.assertEquals(0, entities.size());

		// 3. update meta class
		MetaClass newMeta2 = new MetaClass();
		newMeta2.setName(metaType);
		newMeta2.setRepository(STRATUS_REPO);
		MetaAttribute ma2 = new MetaAttribute();
		ma2.setName("name1");
		ma2.setDataType(DataTypeEnum.STRING);
		newMeta2.addField(ma2);

		MetaRelationship mr = new MetaRelationship();
		mr.setName("resources");
		mr.setCardinality(CardinalityEnum.Many);
		mr.setRefDataType("Resource");
		mr.setRelationType(RelationTypeEnum.Reference);
		newMeta2.addField(mr);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(newMeta2), metaContext);

		// 3. check current version and older version
		MetaClass mc = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, metaType);
		Assert.assertNotNull(mc);
		Assert.assertNotNull(mc.getFieldByName("resources"));
		Assert.assertEquals(1, mc.getVersion());

		List<IEntity> oldMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);
		Assert.assertEquals(1, oldMetas.size());
		DBObject oldMeta = (DBObject) oldMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		ObjectConverter<MetaClass> converter = new ObjectConverter<MetaClass>();
		MetaClass oldMetaClass = converter.fromBson(oldMeta, MetaClass.class);
		Assert.assertNotNull(oldMetaClass);
		Assert.assertNotNull(oldMetaClass.getFieldByName("name1"));
		Assert.assertNull(oldMetaClass.getFieldByName("resources"));
		Assert.assertEquals(0, oldMetaClass.getVersion());

		// 4. delete meta class
		server.deleteMetadata(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);

		// 5. try create one with the same name again
		MetaClass newMeta3 = new MetaClass();
		newMeta3.setName(metaType);
		newMeta3.setRepository(STRATUS_REPO);
		MetaAttribute ma3 = new MetaAttribute();
		ma3.setName("name2");
		ma3.setDataType(DataTypeEnum.STRING);
		newMeta3.addField(ma3);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(newMeta3), metaContext);

		// 6. delete meta class
		server.deleteMetadata(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);

		// 7. try create one with the same name again
		MetaClass newMeta4 = new MetaClass();
		newMeta4.setName(metaType);
		newMeta4.setRepository(STRATUS_REPO);
		MetaAttribute ma4 = new MetaAttribute();
		ma4.setName("name3");
		ma4.setDataType(DataTypeEnum.STRING);
		newMeta4.addField(ma4);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(newMeta4), metaContext);

		// 8. check version
		MetaClass getMeta = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, metaType);
		Assert.assertNotNull(getMeta);
		Assert.assertEquals(3, getMeta.getVersion());

		oldMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, metaType, metaContext);
		Assert.assertEquals(3, oldMetas.size());

		MetaClass oldMetaClass2 = converter.fromBson((DBObject) oldMetas.get(0)
				.getFieldValues(HistoryMetaClass.LogBody).get(0), MetaClass.class);
		Assert.assertNotNull(oldMetaClass2);
		Assert.assertNotNull(oldMetaClass2.getFieldByName("name2"));
		Assert.assertNull(oldMetaClass2.getFieldByName("resources"));
		Assert.assertEquals(2, oldMetaClass2.getVersion());
		String opType2 = (String) oldMetas.get(0).getFieldValues(HistoryMetaClass.OperType).get(0);
		Assert.assertEquals("deleteMetaClass", opType2);
		Integer version2 = (Integer) oldMetas.get(0).getFieldValues(HistoryMetaClass.EntityVersion).get(0);
		Assert.assertEquals(2, version2.intValue());

		MetaClass oldMetaClass1 = converter.fromBson((DBObject) oldMetas.get(1)
				.getFieldValues(HistoryMetaClass.LogBody).get(0), MetaClass.class);
		Assert.assertNotNull(oldMetaClass1);
		Assert.assertNotNull(oldMetaClass1.getFieldByName("name1"));
		Assert.assertNotNull(oldMetaClass1.getFieldByName("resources"));
		Assert.assertEquals(1, oldMetaClass1.getVersion());
		String opType1 = (String) oldMetas.get(1).getFieldValues(HistoryMetaClass.OperType).get(0);
		Assert.assertEquals("deleteMetaClass", opType1);
		Integer version1 = (Integer) oldMetas.get(1).getFieldValues(HistoryMetaClass.EntityVersion).get(0);
		Assert.assertEquals(1, version1.intValue());

		MetaClass oldMetaClass0 = converter.fromBson((DBObject) oldMetas.get(2)
				.getFieldValues(HistoryMetaClass.LogBody).get(0), MetaClass.class);
		Assert.assertNotNull(oldMetaClass0);
		Assert.assertNotNull(oldMetaClass0.getFieldByName("name1"));
		Assert.assertNull(oldMetaClass0.getFieldByName("resources"));
		Assert.assertEquals(0, oldMetaClass0.getVersion());
		String opType0 = (String) oldMetas.get(2).getFieldValues(HistoryMetaClass.OperType).get(0);
		Assert.assertEquals("updateMetaClass", opType0);
		Integer version0 = (Integer) oldMetas.get(2).getFieldValues(HistoryMetaClass.EntityVersion).get(0);
		Assert.assertEquals(0, version0.intValue());
	}

	@Test
	public void testMetaClassParentVersion() {
		Map<String, Object> newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		server.config(newConfigs);

		// 1. create meta class
		String rootType = "RootClass";
		MetaClass rootMeta = new MetaClass();
		rootMeta.setName(rootType);
		rootMeta.setRepository(STRATUS_REPO);

		String childType = "ChildClass";
		MetaClass childMeta = new MetaClass();
		childMeta.setName(childType);
		childMeta.setParent(rootType);
		childMeta.setRepository(STRATUS_REPO);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(rootMeta, childMeta), metaContext);

		// 2. update root meta class and add grandson
		rootMeta = new MetaClass();
		rootMeta.setName(rootType);
		MetaAttribute ma = new MetaAttribute();
		ma.setName("rootName");
		ma.setDataType(DataTypeEnum.STRING);
		rootMeta.addField(ma);

		String grandsonType = "GrandsonClass";
		MetaClass grandsonMeta = new MetaClass();
		grandsonMeta.setName(grandsonType);
		grandsonMeta.setParent(childType);
		grandsonMeta.setRepository(STRATUS_REPO);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(rootMeta, grandsonMeta), metaContext);

		// 3. check version and parent version
		MetaClass getRoot = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, rootType);
		Assert.assertNotNull(getRoot);
		Assert.assertEquals(1, getRoot.getVersion());

		MetaClass getChild = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, childType);
		Assert.assertNotNull(getChild);
		Assert.assertEquals(1, getChild.getVersion());
		Assert.assertEquals(1, getChild.getParentVersion());

		MetaClass getGrandson = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, grandsonType);
		Assert.assertNotNull(getGrandson);
		Assert.assertEquals(0, getGrandson.getVersion());
		Assert.assertEquals(1, getGrandson.getParentVersion());

		// 4. check history
		List<IEntity> oldRootMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, rootType, metaContext);
		Assert.assertEquals(1, oldRootMetas.size());
		DBObject oldRootObj = (DBObject) oldRootMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		ObjectConverter<MetaClass> converter = new ObjectConverter<MetaClass>();
		MetaClass oldRootMeta = converter.fromBson(oldRootObj, MetaClass.class);
		Assert.assertNotNull(oldRootMeta);
		Assert.assertNull(oldRootMeta.getFieldByName("rootName"));
		Assert.assertEquals(0, oldRootMeta.getVersion());

		List<IEntity> oldChildMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, childType, metaContext);
		Assert.assertEquals(1, oldChildMetas.size());
		DBObject oldChildObj = (DBObject) oldChildMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		MetaClass oldChildMeta = converter.fromBson(oldChildObj, MetaClass.class);
		Assert.assertNotNull(oldChildMeta);
		Assert.assertEquals(0, oldChildMeta.getParentVersion());

		List<IEntity> oldGrandsonMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, grandsonType,
				metaContext);
		Assert.assertEquals(0, oldGrandsonMetas.size());

		// 5. update root and grandson
		rootMeta = new MetaClass();
		rootMeta.setName(rootType);
		ma = new MetaAttribute();
		ma.setName("rootName2");
		ma.setDataType(DataTypeEnum.STRING);
		rootMeta.addField(ma);

		grandsonMeta = new MetaClass();
		grandsonMeta.setName(grandsonType);
		ma = new MetaAttribute();
		ma.setName("grandsonName");
		ma.setDataType(DataTypeEnum.STRING);
		grandsonMeta.addField(ma);

		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(rootMeta, grandsonMeta), metaContext);

		// 6. check version and parent version
		getRoot = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, rootType);
		Assert.assertNotNull(getRoot);
		Assert.assertEquals(2, getRoot.getVersion());

		getChild = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, childType);
		Assert.assertNotNull(getChild);
		Assert.assertEquals(2, getChild.getVersion());
		Assert.assertEquals(2, getChild.getParentVersion());

		getGrandson = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, grandsonType);
		Assert.assertNotNull(getGrandson);
		Assert.assertEquals(2, getGrandson.getVersion());
		Assert.assertEquals(2, getGrandson.getParentVersion());

		// 7. check history
		oldRootMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, rootType, metaContext);
		Assert.assertEquals(2, oldRootMetas.size());
		oldRootObj = (DBObject) oldRootMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		oldRootMeta = converter.fromBson(oldRootObj, MetaClass.class);
		Assert.assertNotNull(oldRootMeta);
		Assert.assertNotNull(oldRootMeta.getFieldByName("rootName"));
		Assert.assertNull(oldRootMeta.getFieldByName("rootName2"));
		Assert.assertEquals(1, oldRootMeta.getVersion());

		oldChildMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, childType, metaContext);
		Assert.assertEquals(2, oldChildMetas.size());
		oldChildObj = (DBObject) oldChildMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		oldChildMeta = converter.fromBson(oldChildObj, MetaClass.class);
		Assert.assertNotNull(oldChildMeta);
		Assert.assertEquals(1, oldChildMeta.getVersion());
		Assert.assertEquals(1, oldChildMeta.getParentVersion());

		oldGrandsonMetas = server.getMetaHistory(CMSPriority.NEUTRAL, STRATUS_REPO, grandsonType, metaContext);
		Assert.assertEquals(2, oldGrandsonMetas.size());
		DBObject oldGrandsonObj = (DBObject) oldGrandsonMetas.get(0).getFieldValues(HistoryMetaClass.LogBody).get(0);
		MetaClass oldGrandsonMeta = converter.fromBson(oldGrandsonObj, MetaClass.class);
		Assert.assertNotNull(oldGrandsonMeta);
		Assert.assertEquals(1, oldGrandsonMeta.getVersion());
		Assert.assertEquals(2, oldGrandsonMeta.getParentVersion());

		oldGrandsonObj = (DBObject) oldGrandsonMetas.get(1).getFieldValues(HistoryMetaClass.LogBody).get(0);
		oldGrandsonMeta = converter.fromBson(oldGrandsonObj, MetaClass.class);
		Assert.assertNotNull(oldGrandsonMeta);
		Assert.assertEquals(0, oldGrandsonMeta.getVersion());
		Assert.assertEquals(1, oldGrandsonMeta.getParentVersion());
	}

	@Test
	public void testDeleteMetaField() {
		Map<String, Object> newConfigs = new HashMap<String, Object>();
		newConfigs.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		server.config(newConfigs);

		String metaType = "ServiceCluster";
		String fieldName = "poolClusters";
		server.deleteMetaField(CMSPriority.NEUTRAL, DEPLOY_REPO, metaType, fieldName, metaContext);

		MetaClass mc = server.getMetaClass(CMSPriority.NEUTRAL, DEPLOY_REPO, metaType);
		Assert.assertNotNull(mc);
		Assert.assertNull(mc.getFieldByName(fieldName));

		MetaClass oldMC = server.getMetaClass(CMSPriority.NEUTRAL, DEPLOY_REPO, metaType, mc.getVersion() - 1);
		Assert.assertNotNull(oldMC);
		Assert.assertNotNull(oldMC.getFieldByName(fieldName));

		List<IEntity> entities = server.getMetaHistory(CMSPriority.NEUTRAL, DEPLOY_REPO, metaType, metaContext);
		Assert.assertEquals(1, entities.size());
		String opType = (String) entities.get(0).getFieldValues(HistoryMetaClass.OperType).get(0);
		Assert.assertEquals("deleteMetaField", opType);
	}

	@Test
	public void testBatchGet() {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		raptorQueryContext.setAllowFullTableScan(true);
		IQueryResult result1 = server.batchGet(CMSPriority.NEUTRAL, APPLICATION_SERVICE, queryParams,
				raptorQueryContext);
		Assert.assertTrue(result1.getEntities().size() > 0);

		queryParams.add("filters", "@_type=\"ApplicationService\"");
		queryParams.add("fields", "@_oid");
		IQueryResult result2 = server.batchGet(CMSPriority.NEUTRAL, APPLICATION_SERVICE, queryParams,
				raptorQueryContext);
		Assert.assertTrue(result2.getEntities().size() > 0);
		IEntity queryEntity = result2.getEntities().get(0);
		Assert.assertTrue(queryEntity.getFieldValues("name").isEmpty());
	}

	@Test(expected = MetaClassNotExistsException.class)
	public void testBatchGetInvalidMeta() {
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		server.batchGet(CMSPriority.NEUTRAL, "invalid-meta", queryParams, raptorQueryContext);
	}

	@Test
	public void testBatchUpsert() {
	}

	@Test
	public void testBatchCreate() {
	}

	@Test
	public void testGet() {
		EntityContext entityContext = newEntityContext();
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		queryParams.add("fields", "@_oid");
		queryParams.add("fields", "@_type");
		queryParams.add("fields", "@updateStrategies");

		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE,
				"4fbb314fc681caf13e283a76");
		IEntity gEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(gEntity);
		Assert.assertNull(gEntity.getLastModified());
		Assert.assertTrue(gEntity.getFieldValues("name").isEmpty());
	}

	@Test
	public void testDelete() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(456);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-create\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);

		server.delete(CMSPriority.NEUTRAL, qEntity, entityContext);

		getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNull(getEntity);

		entityContext.setComment(null);
	}

	@Test
	public void testCreate() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-create\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);

		Assert.assertNotNull(getEntity);
		Assert.assertNotNull(getEntity.getCreateTime().equals(date));

		// clear comment
		entityContext.setComment(null);
	}

	@Test
	public void testCreateEmbed() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(DEP);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"dep101\", \"team\": { \"name\": \"team101-1\"} }");
		jsonEntity.setBranchId(BRANCH_MAIN);

		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The given value of MetaField team is not an array", e.getMessage());
		}
	}

	@Test
	public void testCreateNullReference2() {
		// for array, when given null, store them as empty list
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-app\", \"services\": null}");
		jsonEntity.setBranchId(BRANCH_MAIN);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertTrue(getEntity.hasField("services"));
		Assert.assertTrue(getEntity.getFieldValues("services").isEmpty());
	}

	@Test
	public void testEmbedCreate() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, DEPLOY_REPO).getMetadataService()
				.getMetaClass(MANIFEST);
		MetaClass versionMetaClass = server.getRepository(CMSPriority.NEUTRAL, DEPLOY_REPO).getMetadataService()
				.getMetaClass(MANIFEST_VERSION);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"embedcreate-test-manifest-001\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		// create manifest
		String creator = "root-creator";
		entityContext.setModifier(creator);
		entityContext.setComment("newlycreate");
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, DEPLOY_REPO, BRANCH_MAIN, MANIFEST, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);
		Assert.assertTrue(getEntity.getCreateTime().equals(date));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.USER.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.MODIFIER.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));

		// create embed entity
		JsonEntity versionJsonEntity = new JsonEntity(versionMetaClass);
		versionJsonEntity.setBranchId(BRANCH_MAIN);
		versionJsonEntity.setId(MANIFEST + "!" + newId + "!versions!" + "customizedVersionId");
		Date versionCreateTime = new Date();
		versionJsonEntity.addFieldValue("createdTime", versionCreateTime);
		versionJsonEntity.addFieldValue("name", "manifest-version-name-001");
		// set as null to test the populate userid/subject/comment to parent
		// entity case
		String embedCreateUserId = "embed-create-user";
		entityContext.setUserId(embedCreateUserId);
		String embedCreateModifier = "embed-create-subject";
		entityContext.setModifier(embedCreateModifier);
		String embedCreateComments = "embed-create-comment";
		entityContext.setComment(embedCreateComments);
		String versionId = server.create(CMSPriority.NEUTRAL, versionJsonEntity, entityContext);

		IEntity queryEntity = buildQueryEntity(CMSPriority.NEUTRAL, DEPLOY_REPO, BRANCH_MAIN, MANIFEST, newId);
		getEntity = server.get(CMSPriority.NEUTRAL, queryEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);
		// root create time is not changed
		Assert.assertTrue(getEntity.getCreateTime().equals(date));
		Assert.assertEquals(versionId, ((IEntity) getEntity.getFieldValues("versions").get(0)).getId());

		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()));
		Assert.assertEquals(embedCreateComments, getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()).get(0));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.USER.getName()));
		Assert.assertEquals(embedCreateUserId, getEntity.getFieldValues(InternalFieldEnum.USER.getName()).get(0));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.MODIFIER.getName()));
		Assert.assertEquals(embedCreateModifier, getEntity.getFieldValues(InternalFieldEnum.MODIFIER.getName()).get(0));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));
		// creator is not updated!
		Assert.assertEquals(creator, getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()).get(0));

		// clear comment
		entityContext.setComment(null);
	}

	@Test
	public void testEmbedCreateUserComment() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, DEPLOY_REPO).getMetadataService()
				.getMetaClass(MANIFEST);
		MetaClass versionMetaClass = server.getRepository(CMSPriority.NEUTRAL, DEPLOY_REPO).getMetadataService()
				.getMetaClass(MANIFEST_VERSION);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"embedcreate-test-manifest-002\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		// create manifest
		String creator = "root-creator";
		entityContext.setModifier(creator);
		String comment = "newlycreate";
		entityContext.setComment(comment);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, DEPLOY_REPO, BRANCH_MAIN, MANIFEST, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);
		Assert.assertTrue(getEntity.getCreateTime().equals(date));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.USER.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.MODIFIER.getName()));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));

		// create embed entity
		JsonEntity versionJsonEntity = new JsonEntity(versionMetaClass);
		versionJsonEntity.setBranchId(BRANCH_MAIN);
		versionJsonEntity.setId(MANIFEST + "!" + newId + "!versions!" + "customizedVersionId");
		Date versionCreateTime = new Date();
		versionJsonEntity.addFieldValue("createdTime", versionCreateTime);
		versionJsonEntity.addFieldValue("name", "manifest-version-name-001");
		// set as null to test the populate userid/subject/comment to parent
		// entity case
		String embedCreateUserId = null;
		entityContext.setUserId(embedCreateUserId);
		String embedCreateModifier = null;
		entityContext.setModifier(embedCreateModifier);
		String embedCreateComments = null;
		entityContext.setComment(embedCreateComments);
		String versionId = server.create(CMSPriority.NEUTRAL, versionJsonEntity, entityContext);

		IEntity queryEntity = buildQueryEntity(CMSPriority.NEUTRAL, DEPLOY_REPO, BRANCH_MAIN, MANIFEST, newId);
		getEntity = server.get(CMSPriority.NEUTRAL, queryEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);

		// root create time is not changed
		Assert.assertTrue(getEntity.getCreateTime().equals(date));
		Assert.assertEquals(versionId, ((IEntity) getEntity.getFieldValues("versions").get(0)).getId());

		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()));
		Assert.assertEquals(comment, getEntity.getFieldValues(InternalFieldEnum.COMMENT.getName()).get(0));

		// creator is not updated!
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));
		Assert.assertEquals(creator, getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()).get(0));

		// clear comment
		entityContext.setComment(null);
	}

	@Test
	public void testCreateWithNullOidStrongReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-001\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"services\":[{ \"name\" : \"test-null-oid-service-instance-name\", \"_oid\" : null }]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			// expected
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
		}
	}

	@Test
	public void testCreateWithNoOidStrongReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-002\", \"_createtime\": " + date.getTime() + ","
						+ "\"services\":[{ \"name\" : \"test-null-oid-service-instance-name\"}]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			// expected
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
		}
	}

	@Test
	public void testModifyWithNoOidStrongReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-002\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"services\":[{ \"_oid\": \"4fbb314fc681caf13e283a7a\", \"name\" : \"test-null-oid-service-instance-name\"}]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newOid = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		try {
			jsonEntity = new JsonEntity(metaClass, "{ \"_oid\": \"" + newOid
					+ "\", \"name\": \"cmsserver-test-entity-create-002\", \"_createtime\": " + date.getTime() + ","
					+ "\"services\":[{ \"name\" : \"test-null-oid-service-instance-name\"}]      } ");
			jsonEntity.setBranchId(BRANCH_MAIN);

			server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			// expected
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
		}
	}

	@Test
	public void testReplaceWithNoOidStrongReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-010\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"services\":[{ \"_oid\": \"4fbb314fc681caf13e283a7a\", \"name\" : \"test-null-oid-service-instance-name\"}]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newOid = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		try {
			jsonEntity = new JsonEntity(metaClass, "{ \"_oid\": \"" + newOid
					+ "\", \"name\": \"cmsserver-test-entity-create-010\", \"_createtime\": " + date.getTime() + ","
					+ "\"services\":[{ \"name\" : \"test-null-oid-service-instance-name\"}]      } ");
			jsonEntity.setBranchId(BRANCH_MAIN);

			server.replace(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			// expected
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
		}
	}

	@Test
	public void testCreateWithEmptyOidStrongReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-003\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"services\":[{ \"name\" : \"test-null-oid-service-instance-name\", \"_oid\": \"\"}]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			// expected
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, e.getErrorEnum());
		}
	}

	@Test
	public void testCreateWithNullOidWeakReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(CLUSTER);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cluster-test-entity-create-004\", \"_createtime\": " + date.getTime() + ","
						+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\", \"_oid\" : null }]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsDalException e) {
			// expected
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.MISS_REFID, e.getErrorEnum());
		}
	}

	@Test
	public void testModifyWithNoOidWeakReference() {

		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(CLUSTER);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cluster-test-entity-create-005\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"computes\":[{ \"_oid\" : \"computeOid-001\", \"name\" : \"test-null-oid-compute-name\" }] } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newOid = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
		try {
			jsonEntity = new JsonEntity(metaClass, "{ \"_oid\" : \"" + newOid
					+ "\", \"name\": \"cluster-test-entity-create-005\", \"_createtime\": " + date.getTime() + ","
					+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\" }] } ");
			jsonEntity.setBranchId(BRANCH_MAIN);
			server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsDalException e) {
			// expected
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.MISS_REFID, e.getErrorEnum());
		}

		jsonEntity = new JsonEntity(metaClass, "{ \"_oid\" : \"" + newOid
				+ "\", \"name\": \"cluster-test-entity-create-moidfy-005\", \"_createtime\": " + date.getTime() + ","
				+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\", \"_oid\":\"51921beda\" }] } ");
		jsonEntity.setBranchId(BRANCH_MAIN);
		server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, "main", CLUSTER, newOid);
		IEntity entity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertTrue("cluster-test-entity-create-moidfy-005".equals(entity.getFieldValues("name").get(0)));
	}

	@Test
	public void testReplaceWithNoOidWeakReference() {

		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(CLUSTER);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"name\": \"cluster-test-entity-create-011\", \"_createtime\": "
						+ date.getTime()
						+ ","
						+ "\"computes\":[{ \"_oid\" : \"computeOid-001\", \"name\" : \"test-null-oid-compute-name\" }] } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String newOid = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
		try {
			jsonEntity = new JsonEntity(metaClass, "{ \"_oid\" : \"" + newOid
					+ "\", \"name\": \"cluster-test-entity-create-011\", \"_createtime\": " + date.getTime() + ","
					+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\" }] } ");
			jsonEntity.setBranchId(BRANCH_MAIN);
			server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsDalException e) {
			// expected
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.MISS_REFID, e.getErrorEnum());
		}
	}

	@Test
	public void testCreateWithNoOidWeakReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(CLUSTER);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cluster-test-entity-create-005\", \"_createtime\": " + date.getTime() + ","
						+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\" }]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsDalException e) {
			// expected
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.MISS_REFID, e.getErrorEnum());
		}
	}

	@Test
	public void testCreateWithEmptyOidWeakReference() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(CLUSTER);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cluster-test-entity-create-006\", \"_createtime\": " + date.getTime() + ","
						+ "\"computes\":[{ \"name\" : \"test-null-oid-compute-name\", \"_oid\": \"\" }]      } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (CmsDalException e) {
			// expected
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.MISS_REFID, e.getErrorEnum());
		}
	}

	@Test
	public void testReplace() {
	}

	@Test
	public void testReplaceToCreate() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-create-005\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);
		String newId = generateRandomName("replace-to-create-id-0001");
		jsonEntity.setId(newId);

		String oldSubject = entityContext.getModifier();
		String subject = "replace-to-create-unitTestUser";
		entityContext.setModifier(subject);
		entityContext.setComment("newlycreate");
		server.replace(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);

		Assert.assertNotNull(getEntity);
		Assert.assertNotNull(getEntity.getCreateTime().equals(date));
		Assert.assertNotNull(getEntity.getCreator());

		entityContext.setModifier(oldSubject);
		// clear comment
		entityContext.setComment(null);
	}

	@Test
	public void testModify() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-modify\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);
		String createComment = "newlycreate-comments";
		entityContext.setComment(createComment);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		String modifyCommnet = "modify-comments";
		entityContext.setComment(modifyCommnet);
		jsonEntity.setId(newId);
		jsonEntity.getNode().remove("name");
		jsonEntity.addFieldValue("archTier", "any-arch-tier");
		server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);

		// clear comment
		entityContext.setComment(null);
	}

	@Test
	public void testModifyArray() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass,
				"{ \"name\": \"cmsserver-test-entity-modify-array\", \"_createtime\": " + date.getTime() + " } ");
		jsonEntity.setBranchId(BRANCH_MAIN);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		JsonEntity modifyEntity = new JsonEntity(metaClass,
				"{ \"updateStrategies\": {\"_type\" : \"UpdateStrategy\", \"_oid\" : \"34234324\" } }");
		modifyEntity.setBranchId(BRANCH_MAIN);
		modifyEntity.setId(newId);

		try {
			server.modify(CMSPriority.NEUTRAL, modifyEntity, modifyEntity, entityContext);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The given value of MetaField updateStrategies is not an array", e.getMessage());
		}
	}

	private static int seq = 0;
	// private static MongoDataSource dataSource;
	private static String CLUSTER = "Cluster";;

	@Test
	public void modifyEmbedArrayField() {
		seq++;
		MetaClass serviceInstanceMeta = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService()
				.getMetaClass(SERVICE_INSTANCE);
		MetaClass serviceAcessPointMeta = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService()
				.getMetaClass("ServiceAccessPoint");
		JsonEntity jsonEntity = new JsonEntity(serviceInstanceMeta);
		jsonEntity.setBranchId("main");
		jsonEntity.addFieldValue("description", "create service instance " + seq);

		JsonEntity embedEntity = new JsonEntity(serviceAcessPointMeta);
		String label1 = generateRandomName("accessPoint") + "-" + seq;
		embedEntity.addFieldValue("label", label1);
		embedEntity.addFieldValue("port", 80);
		embedEntity.addFieldValue("protocol", "tcp");
		jsonEntity.addFieldValue("serviceAccessPoints", embedEntity);
		String id = server.create(null, jsonEntity, entityContext);
		Assert.assertNotNull(id);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

		IEntity qEntity = buildQueryEntity(null, STRATUS_REPO, "main", SERVICE_INSTANCE, id);
		IEntity getEntity = server.get(null, qEntity, queryParams, entityContext);

		Assert.assertNotNull(getEntity);
		Assert.assertTrue(getEntity.getFieldValues("serviceAccessPoints").size() == 1);

		JsonEntity newEmbed = new JsonEntity(serviceAcessPointMeta);
		String label2 = generateRandomName("accessPoint") + "- " + seq + "by modify field";
		newEmbed.addFieldValue("label", label2);
		newEmbed.addFieldValue("port", 80);
		newEmbed.addFieldValue("protocol", "tcp");
		JsonEntity modifyEntity = new JsonEntity(serviceInstanceMeta);
		modifyEntity.setBranchId("main");
		modifyEntity.setId(id);
		modifyEntity.addFieldValue("serviceAccessPoints", newEmbed);

		try {
			server.modifyField(null, modifyEntity, modifyEntity, "serviceAccessPoints", entityContext);
			Assert.fail();
		} catch (CmsEntMgrException e) {
			Assert.assertEquals(EntMgrErrCodeEnum.EMBED_RELATIONSHIP_IMMUTABLE, e.getErrorEnum());
		}
	}

	@Test
	public void testQuery() {
		Assert.assertNotNull(server.query(CMSPriority.NEUTRAL, "ApplicationService[@archTier=\"app\"]",
				raptorQueryContext));
	}

	@Test
	public void testGetQueryMetaclass() {
		QueryContext context = new QueryContext(RAPTOR_PAAS, BRANCH_MAIN);
		Map<String, MetaClass> metadatas = server.getQueryMetaClass(CMSPriority.NEUTRAL, "ServiceInstance", context);
		Assert.assertEquals(1, metadatas.size());
	}

	@Test
	public void testAddIndex() {
		IndexInfo index = new IndexInfo("ut_appTierIndex2_cmsServer");
		index.addKeyField("appId");
		index.addKeyField("name");
		index.addOption(IndexOptionEnum.unique);
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();
		indexes.add(index);
		metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
		server.addIndex(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, APPLICATION_SERVICE, indexes, metaContext);
	}

	@Test
	public void testAddIndexOnParent() {
		IndexInfo index = new IndexInfo("descriptionIndex");
		index.addKeyField("description");
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();
		indexes.add(index);
		metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
		server.addIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, "Base", indexes, metaContext);

		// case 1 : the meta and all its existing metadata indexes are loaded
		IMetadataService metaService = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService();
		MetaClass meta = metaService.getMetaClass("Base");
		checkIndexesLoaded(metaService, meta);
		// also make sure indexes are added for descendants
		for (MetaClass descMeta : meta.getDescendants()) {
			checkIndexesLoaded(metaService, descMeta);
		}

		// case 2: all newly create meta after the index will have indexes
		// built.
		MetaClass newChild = new MetaClass();
		newChild.setName("newChildMeta");
		newChild.setRepository(meta.getRepository());
		newChild.setParent("Base");
		server.batchUpsert(CMSPriority.NEUTRAL, STRATUS_REPO, Arrays.asList(newChild), metaContext);
		MetaClass getChild = server.getMetaClass(CMSPriority.NEUTRAL, STRATUS_REPO, newChild.getName());
		Assert.assertTrue(getChild.getIndexNames().contains(index.getIndexName()));
		checkIndexesLoaded(metaService, getChild);
	}

	@Test
	public void testUpdateParentMetaToAddIndex() {
		IMetadataService metaService = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService();
		// create parent
		MetaClass parentMeta = new MetaClass();
		parentMeta.setName("newParentMeta");
		parentMeta.setRepository(RAPTOR_PAAS);

		server.batchUpsert(CMSPriority.NEUTRAL, RAPTOR_PAAS, Arrays.asList(parentMeta), metaContext);
		MetaClass getParent = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, parentMeta.getName());
		checkIndexesLoaded(metaService, getParent);
		// create child
		MetaClass child1 = new MetaClass();
		child1.setName("newChildMeta");
		child1.setRepository(parentMeta.getRepository());
		child1.setParent(parentMeta.getName());

		server.batchUpsert(CMSPriority.NEUTRAL, RAPTOR_PAAS, Arrays.asList(child1), metaContext);
		MetaClass getChild1 = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, child1.getName());
		checkIndexesLoaded(metaService, getChild1);

		// child2
		MetaClass child2 = new MetaClass();
		child2.setName("newChildMeta2");
		child2.setRepository(parentMeta.getRepository());
		child2.setParent(child1.getName());
		server.batchUpsert(CMSPriority.NEUTRAL, RAPTOR_PAAS, Arrays.asList(child2), metaContext);
		MetaClass getChild2 = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, child2.getName());
		checkIndexesLoaded(metaService, getChild2);

		// now update parent
		MetaClass uParentMeta = new MetaClass();
		uParentMeta.setName("newParentMeta");
		uParentMeta.setRepository(RAPTOR_PAAS);
		MetaAttribute attr = new MetaAttribute();
		attr.setName("newParentFieldName");
		attr.setDataType(DataTypeEnum.STRING);
		uParentMeta.addField(attr);
		IndexInfo newInfo = new IndexInfo("nameIndex");
		newInfo.addKeyField(attr.getName());
		newInfo.addOption(IndexOptionEnum.unique);
		uParentMeta.addIndex(newInfo);

		server.batchUpsert(CMSPriority.NEUTRAL, RAPTOR_PAAS, Arrays.asList(uParentMeta), metaContext);

		// assertion now
		MetaClass gMeta = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, uParentMeta.getName());
		Assert.assertEquals(3, gMeta.getIndexNames().size());
		checkIndexesLoaded(metaService, gMeta);

		gMeta = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, child1.getName());
		Assert.assertEquals(3, gMeta.getIndexNames().size());
		checkIndexesLoaded(metaService, gMeta);

		gMeta = server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_PAAS, child2.getName());
		Assert.assertEquals(3, gMeta.getIndexNames().size());
		Assert.assertTrue(gMeta.getIndexNames().contains(newInfo.getIndexName()));
		checkIndexesLoaded(metaService, gMeta);
	}

	@Test
	public void testUpdateMetaField() {
		MetaClass rootMeta = new MetaClass();
		rootMeta.setName("newMeta-1");
		rootMeta.setRepository(RAPTOR_PAAS);

		MetaClass newMeta = new MetaClass();
		newMeta.setName("newMeta-2");
		newMeta.setRepository(RAPTOR_PAAS);
		MetaRelationship rel = new MetaRelationship();
		rel.setName("rel-1");
		rel.setRelationType(RelationTypeEnum.Reference);
		rel.setRefDataType(newMeta.getName());

		rootMeta.addField(rel);

		MetaClass innerMeta = new MetaClass();
		innerMeta.setName("innerMeta");
		innerMeta.setRepository(RAPTOR_PAAS);
		innerMeta.setInner(true);

		List<MetaClass> metaList = new ArrayList<MetaClass>();
		metaList.add(rootMeta);
		metaList.add(newMeta);
		metaList.add(innerMeta);

		server.batchUpsert(CMSPriority.NEUTRAL, RAPTOR_PAAS, metaList, metaContext);

		rel.setRefDataType(innerMeta.getName());
		rel.setRelationType(RelationTypeEnum.Inner);
		rel.setDbName(null);
		rel.setValueDbName(null);
		rel.setFlattenValueDbName(null);
		server.updateMetaField(CMSPriority.NEUTRAL, RAPTOR_PAAS, rootMeta, "rel-1", metaContext);
	}

	@Test
	public void testAddIndexOnParentConflictWithChild() {
		IndexInfo index = new IndexInfo("fqdnIndex");
		index.addKeyField("description");
		index.addOption(IndexOptionEnum.unique);
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();
		indexes.add(index);
		metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
		try {
			server.addIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, "Base", indexes, metaContext);
			Assert.fail();
		} catch (IndexExistsException iee) {
			// expected : should conflict with the fqdn index in ServiceInstance
		}
	}

	@Test
	public void testDeleteIndex() {
		IndexInfo index = new IndexInfo("ut_appTierIndex3_cmsServer");
		index.addKeyField("appId");
		index.addKeyField("name");
		index.addKeyField("archTier");
		index.addOption(IndexOptionEnum.unique);
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();
		indexes.add(index);
		metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
		server.addIndex(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, APPLICATION_SERVICE, indexes, metaContext);

		metaContext.setOptionChangeMode(UpdateOptionMode.DELETE);
		server.deleteIndex(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, APPLICATION_SERVICE, "ut_appTierIndex3_cmsServer",
				metaContext);
	}

	@Test
	public void testDeleteIndexOnParent() {
		IndexInfo createIndex = new IndexInfo("descriptionLabelIndex");
		createIndex.addKeyField("description");
		createIndex.addKeyField("label");
		createIndex.addOption(IndexOptionEnum.unique);
		createIndex.addOption(IndexOptionEnum.sparse);
		List<IndexInfo> indexes1 = new ArrayList<IndexInfo>();
		indexes1.add(createIndex);
		metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
		server.addIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, "Base", indexes1, metaContext);

		IMetadataService metaService = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService();
		MetaClass meta = metaService.getMetaClass("Base");
		checkIndexesLoaded(metaService, meta);
		for (MetaClass descMeta : meta.getDescendants()) {
			checkIndexesLoaded(metaService, descMeta);
		}

		// delete don't need key specified
		metaContext.setOptionChangeMode(UpdateOptionMode.DELETE);
		server.deleteIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, "Base", "descriptionLabelIndex", metaContext);
		metaService = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService();
		meta = metaService.getMetaClass("Base");
		checkIndexesLoaded(metaService, meta);
		// make sure indexes are removed from descendants
		for (MetaClass descMeta : meta.getDescendants()) {
			checkIndexesLoaded(metaService, descMeta);
		}
	}

    @Test
    public void overrideIndex() {
        final String idxName = "descriptionOidIndex";
        IndexInfo parentIndex = new IndexInfo(idxName);
        parentIndex.addKeyField("description");
        parentIndex.addKeyField("_oid");
        parentIndex.addOption(IndexOptionEnum.sparse);
        List<IndexInfo> indexes1 = new ArrayList<IndexInfo>();
        indexes1.add(parentIndex);
        metaContext.setOptionChangeMode(UpdateOptionMode.ADD);
        server.addIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, "Base", indexes1, metaContext);
        // assert the child indexes
        {
            MetaClass nodeServer = server.getMetaClass(CMSPriority.NON_CRITICAL, STRATUS_REPO, ENVIRONMENT);
            IMetadataService metaService = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService();
            Map<String, DBObject> dbIndexes = getCollectionIndexMap(metaService, nodeServer);
            Assert.assertTrue(dbIndexes.containsKey(idxName));
        }
        // try override the index on sub meta class
        {
            IndexInfo newIndex = new IndexInfo();
            newIndex.setIndexName(idxName);
            newIndex.addKeyField("description");
            newIndex.addKeyField("_oid");
            newIndex.addKeyField("label");
            server.addIndex(CMSPriority.NON_CRITICAL, STRATUS_REPO, ENVIRONMENT, Arrays.asList(newIndex), metaContext);
            // assert metadata
            MetaClass nodeServer = server.getMetaClass(CMSPriority.NON_CRITICAL, STRATUS_REPO, ENVIRONMENT);
            IndexInfo overIndex = nodeServer.getIndexByName(idxName);
            Assert.assertEquals(parentIndex.getKeyList().size() + 1, overIndex.getKeyList().size());
            Assert.assertTrue(overIndex.getKeyList().contains("description"));
            Assert.assertTrue(overIndex.getKeyList().contains("_oid"));
            Assert.assertTrue(overIndex.getKeyList().contains("label"));
            Assert.assertFalse(overIndex.getIndexOptions().contains(IndexOptionEnum.sparse));
            Assert.assertFalse(overIndex.getIndexOptions().contains(IndexOptionEnum.unique));
            // assert db indexes
            IMetadataService metaService = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService();
            Map<String, DBObject> dbIndexes = getCollectionIndexMap(metaService, nodeServer);
            Assert.assertTrue(dbIndexes.containsKey(idxName));
            DBObject indexDbo = (DBObject) dbIndexes.get(idxName);
            Assert.assertFalse(indexDbo.containsField(IndexOptionEnum.sparse.name().toLowerCase()));
            Assert.assertFalse(indexDbo.containsField(IndexOptionEnum.unique.name().toLowerCase()));
            DBObject keyObject = (DBObject) indexDbo.get("key");
            Assert.assertEquals(3, keyObject.toMap().size());
        }
    }

	protected static void checkIndexesLoaded(IMetadataService metaService, MetaClass metaClass) {
		if (metaClass.getName().equals(HistoryMetaClass.NAME) || metaClass.getName().equals(BranchMetaClass.TYPE_NAME)) {
			return;
		}

		Map<String, DBObject> indexObjects = getCollectionIndexMap(metaService, metaClass);
		for (IndexInfo ii : metaClass.getIndexes()) {
			Assert.assertTrue(indexObjects.containsKey(ii.getIndexName()));
		}
	}

	protected static Map<String, DBObject> getCollectionIndexMap(IMetadataService metaService, MetaClass metaClass) {
		PersistenceContext pc = newPersistenceContext(metaService);
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

	protected static PersistenceContext newPersistenceContext(IMetadataService metaService) {
		PersistenceContext pc = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata,
				ConsistentPolicy.PRIMARY, IBranch.DEFAULT_BRANCH);
		pc.setDbConfig(config);
		pc.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
		return pc;
	}

	// configuration api test

	@Test
	public void getConfig() {
		Map<String, Object> configs = server.getCurrentConfigurations();
		Assert.assertNotNull(configs);
	}

	@Test
	public void getConfigNames() {
		Set<String> sets = server.getCurrentConfigurations().keySet();
		Set<String> names = server.getConfigNames();
		Assert.assertEquals(sets.size(), names.size());
		Assert.assertEquals(sets, names);
	}

	@Test
	public void chConfig() {
		Map<String, Object> current = server.getCurrentConfigurations();
		Assert.assertTrue((Boolean) current.get(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN));

		Map<String, Object> modConfig = new HashMap<String, Object>(current);
		modConfig.remove(CMSDBConfig.SYS_SHOW_STACK_TRACE_PROP);
		modConfig.put(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN, false);

		server.config(modConfig);

		Map<String, Object> newConfig = server.getCurrentConfigurations();
		Assert.assertEquals(Boolean.FALSE, (Boolean) newConfig.get(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN));

		modConfig.put(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN, true);
		server.config(modConfig);
	}

	@Test
	public void fullTableScanConfig() {
		// case 1 : query full table scan false + sys full table scan true
		QueryContext qc = newQueryContext(RAPTOR_PAAS);
		qc.setSmallTableThreshold(0);
		try {
			server.query(CMSPriority.NEUTRAL, "ApplicationService", qc);
			Assert.fail();
		} catch (QueryOptimizeException e) {
			// expected
		}

		// case 2 : query full table scan true + sys full table scan true
		qc.setAllowFullTableScan(true);
		server.query(CMSPriority.NEUTRAL, "ApplicationService", qc);

		// case 3 : query full table scan true + sys full table scan false
		Map<String, Object> modConfig = new HashMap<String, Object>();
		modConfig.put(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN, false);
		server.config(modConfig);
		try {
			server.query(CMSPriority.NEUTRAL, "ApplicationService", qc);
			Assert.fail();
		} catch (QueryOptimizeException e) {
			// expected
		}
		modConfig.put(CMSDBConfig.SYS_ALLOW_FULL_TABLE_SCAN, true);
		server.config(modConfig);
	}

	public static class FailedCallback implements IEntityOperationCallback {

		@Override
		public boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context) {
			return false;
		}
	}

	@Test
	public void entityCallback() {
		server.setEntityOperationCallback(new FailedCallback());
		try {

			MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
					.getMetaClass(APPLICATION_SERVICE);
			Date date = new Date(123);
			JsonEntity jsonEntity = new JsonEntity(metaClass,
					"{ \"name\": \"cmsserver-test-entity-modify-callback\", \"_createtime\": " + date.getTime() + " } ");
			jsonEntity.setBranchId(BRANCH_MAIN);
			String createComment = "newlycreate-comments";
			entityContext.setComment(createComment);

			String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

			jsonEntity.setId(newId);
			server.modify(CMSPriority.NEUTRAL, jsonEntity, jsonEntity, entityContext);

			Assert.fail();
		} catch (Exception e) {
			// expected
			Assert.assertTrue(e instanceof CmsEntMgrException);
			Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.OPERATION_CHECK_FAILED,
					((CmsEntMgrException) e).getErrorEnum());
		}
		// clear comment
		entityContext.setComment(null);
		server.setEntityOperationCallback(null);
	}

	@Test
	public void testCreator() {

		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		Date date = new Date(123);
		JsonEntity jsonEntity = new JsonEntity(metaClass, "{ \"name\": \"cmsserver-test-for-creator\" } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		entityContext.setModifier("unitTestUser");
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);

		Assert.assertNotNull(getEntity);
		Assert.assertNotNull(getEntity.getCreateTime().equals(date));
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));
		Assert.assertEquals(entityContext.getModifier(), getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName())
				.get(0));

		// clear comment
		entityContext.setComment(null);

	}

	@Test
	public void testUpdateToChangeCreator() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		JsonEntity jsonEntity = new JsonEntity(metaClass, "{ \"name\": \"cmsserver-test-for-creator-update\" } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String creator = "unitTestUser";
		entityContext.setModifier(creator);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		JsonEntity uJsonEntity = new JsonEntity(metaClass, "{ \"name\": \"cmsserver-test-for-creator-update-0001\" } ");
		entityContext.setModifier("update01");
		uJsonEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "updateUser01");
		uJsonEntity.setBranchId(BRANCH_MAIN);
		uJsonEntity.setId(newId);

		server.modify(CMSPriority.NEUTRAL, uJsonEntity, uJsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);
		// assert that creator is not changed even when user try to update it
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));
		Assert.assertEquals(creator, getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()).get(0));
	}

	@Test
	public void testReplaceToChangeCreator() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, RAPTOR_PAAS).getMetadataService()
				.getMetaClass(APPLICATION_SERVICE);
		JsonEntity jsonEntity = new JsonEntity(metaClass, "{ \"name\": \"cmsserver-test-for-creator-replace\" } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");
		String creator = "unitTestUser";
		entityContext.setModifier(creator);
		String newId = server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);

		JsonEntity uJsonEntity = new JsonEntity(metaClass, "{ \"name\": \"cmsserver-test-for-creator-replace-0001\" } ");
		entityContext.setModifier("replace01");
		uJsonEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "repalceUser01");
		uJsonEntity.setBranchId(BRANCH_MAIN);
		uJsonEntity.setId(newId);

		server.replace(CMSPriority.NEUTRAL, uJsonEntity, uJsonEntity, entityContext);

		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
		IEntity qEntity = buildQueryEntity(CMSPriority.NEUTRAL, RAPTOR_PAAS, BRANCH_MAIN, APPLICATION_SERVICE, newId);
		IEntity getEntity = server.get(CMSPriority.NEUTRAL, qEntity, queryParams, entityContext);
		Assert.assertNotNull(getEntity);
		// assert that creator is not changed even when user try to update it
		Assert.assertNotNull(getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()));
		Assert.assertEquals(1, getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()).size());
		Assert.assertEquals(creator, getEntity.getFieldValues(InternalFieldEnum.CREATOR.getName()).get(0));
	}

	@Test
	public void testCreateWithInvalidReferenceType() {
		MetaClass metaClass = server.getRepository(CMSPriority.NEUTRAL, STRATUS_REPO).getMetadataService()
				.getMetaClass(RESOURCE_GROUP);
		JsonEntity jsonEntity = new JsonEntity(
				metaClass,
				"{ \"type\": \"ApplicationService\", \"resourceId\": \"PHX:SI01\", "
						+ "\"children\":[{ \"_oid\" : \"50e7ee93e4b0fe5ae3aa34c8\", \"_type\" : \"UpdateStrategy\" }] } ");
		jsonEntity.setBranchId(BRANCH_MAIN);

		entityContext.setComment("newlycreate");

		try {
			server.create(CMSPriority.NEUTRAL, jsonEntity, entityContext);
			Assert.fail();
		} catch (IllegalArgumentException e) {
			// expected
			Assert.assertEquals("Meta relationship ref meta Resource couldn't be add a instance of UpdateStrategy",
					e.getMessage());
		}
	}

	private JsonEntity buildQueryEntity(CMSPriority p, String reponame, String branchname, String metadata, String oid) {
		MetaClass meta = server.getMetaClass(p, reponame, metadata);
		JsonEntity queryEntity = new JsonEntity(meta);
		queryEntity.setId(oid);
		queryEntity.setBranchId(branchname);
		return queryEntity;
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEnumMetaChange_ok() {
		// newly create meta class with enum; create an entity; then change the
		// enum value list; try to get/query the given entity
		MetaClass newMeta = new MetaClass();
		newMeta.setName("EnumModelTest");
		newMeta.setRepository(RAPTOR_PAAS);

		MetaAttribute enumAttr = new MetaAttribute();
		enumAttr.setName("enumAttr");
		enumAttr.setDataType(DataTypeEnum.ENUM);
		enumAttr.addEnumValue("datamodel");

		newMeta.addField(enumAttr);
		// create meta
		server.batchUpsert(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, Arrays.asList(newMeta), metaContext);
		MetaClass getMeta = server.getMetaClass(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, newMeta.getName());
		// create entity
		JsonEntity newEntity = new JsonEntity(getMeta);
		newEntity.setBranchId(IBranch.DEFAULT_BRANCH);
		newEntity.addFieldValue(enumAttr.getName(), "datamodel");
		String newId = server.create(CMSPriority.NON_CRITICAL, newEntity, entityContext);
		newEntity.setId(newId);
		Assert.assertNotNull(server.get(CMSPriority.NON_CRITICAL, newEntity,
				Collections.<String, List<String>> emptyMap(), entityContext));
		// now change enum meta
		MetaAttribute newEnumAttr = new MetaAttribute();
		newEnumAttr.setName("enumAttr");
		newEnumAttr.setDataType(DataTypeEnum.ENUM);
		newEnumAttr.addEnumValue("model");
		MetaClass updateMeta = new MetaClass();
		updateMeta.setName("EnumModelTest");
		updateMeta.setRepository(RAPTOR_PAAS);
		updateMeta.addField(newEnumAttr);
		server.updateMetaField(CMSPriority.NON_CRITICAL, RAPTOR_PAAS, updateMeta, newEnumAttr.getName(), metaContext);

		// try to get the entity with old data
		IEntity getEntity = server.get(CMSPriority.NON_CRITICAL, newEntity,
				Collections.<String, List<String>> emptyMap(), entityContext);
		Assert.assertNotNull(getEntity);
		List<String> getEnumValues = (List<String>) getEntity.getFieldValues(enumAttr.getName());
		Assert.assertNotNull(getEnumValues);
		Assert.assertEquals(1, getEnumValues.size());
		// try query
		IQueryResult result = server.query(CMSPriority.NON_CRITICAL, newMeta.getName(), raptorQueryContext);
		Assert.assertEquals(1, result.getEntities().size());
		IEntity queryEntity = result.getEntities().get(0);
		Assert.assertTrue(queryEntity.hasField(newEnumAttr.getName()));
	}

	@SuppressWarnings("unchecked")
    @Test
	public void testCheckSystemLimitationByCasModifyField() throws Exception {
		CMSPriority p = CMSPriority.NEUTRAL;
		String repoName = CMSDB_REPO;

		Repository repoInst = server.getRepository(p, repoName);
		RepositoryOption repoOption = repoInst.getOptions();
		repoOption.setMaxDocumentSize(524288L);
		repoOption.setMaxIndexedArraySize(5000);
		repoOption.setMaxNumOfIndexes(25);
		repoOption.setMaxRepositorySize(53687091200L);

		server.updateRepository(p, repoInst);

		JsonEntity queryEntity = buildQueryEntity(p, repoName, BRANCH_MAIN, NODE_SERVER, "51f977a3171b7e36601ad3ea");
		IEntity nodeServerEntity = server.get(p, queryEntity, Collections.<String, List<String>> emptyMap(),
				entityContext);
		Assert.assertNotNull(nodeServerEntity);
		List<IEntity> capacities = (List<IEntity>) nodeServerEntity.getFieldValues("capacities");
		Assert.assertNotNull(capacities);
		Assert.assertEquals(1, capacities.size());
		String fieldName = "reserved";
		Long oldValue = 0L;
		Long newValue = 1L;
		JsonEntity oldCapacity = (JsonEntity) capacities.get(0);
		JsonEntity newCapacity = new JsonEntity(oldCapacity);
		newCapacity.setFieldValues(fieldName, Arrays.asList(newValue));
		server.casModifyField(p, oldCapacity, newCapacity, fieldName, oldValue, entityContext);
		IEntity capacityEntity = server.get(p, oldCapacity, Collections.<String, List<String>> emptyMap(),
				entityContext);
		Assert.assertNotNull(capacityEntity);
		List<?> updatedValues = capacityEntity.getFieldValues(fieldName);
		Assert.assertNotNull(updatedValues);
		Assert.assertEquals(1, updatedValues.size());
		Assert.assertEquals(newValue, updatedValues.get(0));

		// reset value changes
		server.casModifyField(p, newCapacity, oldCapacity, fieldName, newValue, entityContext);
		capacityEntity = server.get(p, newCapacity, Collections.<String, List<String>> emptyMap(), entityContext);
		Assert.assertNotNull(capacityEntity);
		updatedValues = capacityEntity.getFieldValues(fieldName);
		Assert.assertNotNull(updatedValues);
		Assert.assertEquals(1, updatedValues.size());
		Assert.assertEquals(oldValue, updatedValues.get(0));

		// reset repository setting
		repoOption.setMaxDocumentSize(null);
		repoOption.setMaxIndexedArraySize(null);
		repoOption.setMaxNumOfIndexes(null);
		repoOption.setMaxRepositorySize(null);
		server.updateRepository(p, repoInst);
	}

	@SuppressWarnings("unchecked")
    @Test
	public void testCheckSystemLimitationByModifyField() throws Exception {
		CMSPriority p = CMSPriority.NEUTRAL;
		String repoName = CMSDB_REPO;

		Repository repoInst = server.getRepository(p, repoName);
		RepositoryOption repoOption = repoInst.getOptions();
		repoOption.setMaxDocumentSize(524288L);
		repoOption.setMaxIndexedArraySize(5000);
		repoOption.setMaxNumOfIndexes(25);
		repoOption.setMaxRepositorySize(53687091200L);

		server.updateRepository(p, repoInst);

		JsonEntity queryEntity = buildQueryEntity(p, repoName, BRANCH_MAIN, NODE_SERVER, "51f977a3171b7e36601ad3ea");
		IEntity nodeServerEntity = server.get(p, queryEntity, Collections.<String, List<String>> emptyMap(),
				entityContext);
		Assert.assertNotNull(nodeServerEntity);
		List<IEntity> capacities = (List<IEntity>) nodeServerEntity.getFieldValues("capacities");
		Assert.assertNotNull(capacities);
		Assert.assertEquals(1, capacities.size());
		String fieldName = "reserved";
		Long oldValue = 0L;
		Long newValue = 1L;
		JsonEntity oldCapacity = (JsonEntity) capacities.get(0);
		JsonEntity newCapacity = new JsonEntity(oldCapacity);
		newCapacity.setFieldValues(fieldName, Arrays.asList(newValue));
		server.modifyField(p, oldCapacity, newCapacity, fieldName, entityContext);
		IEntity capacityEntity = server.get(p, oldCapacity, Collections.<String, List<String>> emptyMap(),
				entityContext);
		Assert.assertNotNull(capacityEntity);
		List<?> updatedValues = capacityEntity.getFieldValues(fieldName);
		Assert.assertNotNull(updatedValues);
		Assert.assertEquals(1, updatedValues.size());
		Assert.assertEquals(newValue, updatedValues.get(0));

		// reset value changes
		server.modifyField(p, newCapacity, oldCapacity, fieldName, entityContext);
		capacityEntity = server.get(p, newCapacity, Collections.<String, List<String>> emptyMap(), entityContext);
		Assert.assertNotNull(capacityEntity);
		updatedValues = capacityEntity.getFieldValues(fieldName);
		Assert.assertNotNull(updatedValues);
		Assert.assertEquals(1, updatedValues.size());
		Assert.assertEquals(oldValue, updatedValues.get(0));

		// reset repository setting
		repoOption.setMaxDocumentSize(null);
		repoOption.setMaxIndexedArraySize(null);
		repoOption.setMaxNumOfIndexes(null);
		repoOption.setMaxRepositorySize(null);
		server.updateRepository(p, repoInst);
	}

	@SuppressWarnings("unchecked")
    @Test
	public void testCheckSystemLimitationByPullField() throws Exception {
		CMSPriority p = CMSPriority.NEUTRAL;
		String repoName = CMSDB_REPO;

		Repository repoInst = server.getRepository(p, repoName);
		RepositoryOption repoOption = repoInst.getOptions();
		repoOption.setMaxDocumentSize(524288L);
		repoOption.setMaxIndexedArraySize(5000);
		repoOption.setMaxNumOfIndexes(25);
		repoOption.setMaxRepositorySize(53687091200L);

		server.updateRepository(p, repoInst);

		String oid = "51f97777e4b0df04738cba25";
		String fieldName = "raidCtrlRaidLevel";
		JsonEntity queryEntity = null;
		MetaClass meta = server.getMetaClass(p, repoName, SKU_CONFIGURATION);
		queryEntity = new JsonEntity(meta);
		queryEntity.setId(oid);
		queryEntity.setBranchId(BRANCH_MAIN);

		IEntity skuConfigurationEntity = server.get(p, queryEntity, Collections.<String, List<String>> emptyMap(),
				entityContext);

		List<String> values = (List<String>) skuConfigurationEntity.getFieldValues(fieldName);
		Assert.assertNotNull(values);
		Assert.assertEquals(2, values.size());

		String jsonString = "\"raid0\"";

		ObjectNode node = JsonNodeFactory.instance.objectNode();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode fieldNode = mapper.readTree(jsonString);
		ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
		arrayNode.insert(0, fieldNode);
		fieldNode = arrayNode;
		node.put(fieldName, fieldNode);

		IEntity entity = new JsonEntity(meta, node);
		entity.setBranchId(BRANCH_MAIN);
		entity.setId(oid);

		ModifyAction originalModifyAction = entityContext.getModifyAction();
		entityContext.setModifyAction(ModifyAction.PULLFIELD);
		server.pullField(p, queryEntity, entity, fieldName, entityContext);

		skuConfigurationEntity = server.get(p, queryEntity, Collections.<String, List<String>> emptyMap(),
				entityContext);

		values = (List<String>) skuConfigurationEntity.getFieldValues(fieldName);
		Assert.assertNotNull(values);
		Assert.assertEquals(1, values.size());
		Assert.assertEquals("raid10", values.get(0));

		entityContext.setModifyAction(originalModifyAction);
		// reset repository setting
		repoOption.setMaxDocumentSize(null);
		repoOption.setMaxIndexedArraySize(null);
		repoOption.setMaxNumOfIndexes(null);
		repoOption.setMaxRepositorySize(null);
		server.updateRepository(p, repoInst);
	}
}
