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


/*******************************************************************************
 * Copyright (c) 2012-2013 eBay Inc.
 * All rights reserved. 
 *  
 * eBay PE Cloud Foundation Team [DL-eBay-SHA-COE-PE-Cloud-Foundation@ebay.com]
 *******************************************************************************/
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

package com.ebay.cloud.cms.service.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.impl.MetadataResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;

/**
 * @author shuachen
 * 
 *         2014-2-17
 */
@Ignore
public class MaxNumOfIndexesTest extends CMSResourceTest {
	private static IMetadataResource resource;
	private static IRepositoryService repoService = null;
	private static MetadataDataLoader metaLoader = null;

	private static final String PARENT_ONESTRING_INDEX = "/parent-onestring-index.json";
	private static final String PARENT_MANYSTRING_INDEX = "/parent-manystring-index.json";

	private static final String EMBED_NAME_INDEX = "/embed-name-index.json";
	private static final String EMBED_NUMBER_INDEX = "/embed-number-index.json";

	private static final String UPDATE_PARENT_JSON = "/update-parent.json";
	private static final String SETUP_PARENT_JSON = "/setup-parent.json";

	private static final String UPDATE_EMBED_JSON = "/update-embed.json";
	private static final String SETUP_EMBED_JSON = "/setup-embed.json";

	private static final String CREATE_NEW_PARENT_JSON = "/create-new-parent.json";

	private static final String BATCH_CREATE_NEW_PARENT_EMBED1_JSON = "/batch-create-new-parent-embed1.json";
	private static final String BATCH_CREATE_NEW_PARENT_EMBED2_JSON = "/batch-create-new-parent-embed2.json";
	private static final String BATCH_CREATE_NEW_PARENT_EMBED3_JSON = "/batch-create-new-parent-embed3.json";

	private static final String BATCH_CREATE_NEW_PARENT_NESTED_EMBED1_JSON = "/batch-create-new-parent-nested-embed1.json";
	private static final String BATCH_CREATE_NEW_PARENT_NESTED_EMBED2_JSON = "/batch-create-new-parent-nested-embed2.json";
	
	private static final String BATCH_CREATE_NEW_PARENT_EMBED_INNER1_JSON = "/batch-create-new-parent-embed-inner1.json";
	private static final String BATCH_CREATE_NEW_PARENT_EMBED_INNER2_JSON = "/batch-create-new-parent-embed-inner2.json";

	@BeforeClass
	public static void setupBeforeClass() {
		ServiceFactory.clearServiceCaches();

		repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadMaxNumOfIndexSizeMetaDataFromResource();
		resource = new MetadataResource();

		Repository repository = new Repository();
		repository.setRepositoryName(RAPTOR_REPO);
		RepositoryOption repositoryOption = new RepositoryOption();
		repositoryOption.setMaxNumOfIndexes(11);
		repository.setOptions(repositoryOption);
		repoService.updateRepository(repository);
	}

	@AfterClass
	public static void tear() {
		cleanTestData();
		
		Repository repository = repoService.getRepository(RAPTOR_REPO);
		repository.getOptions().setMaxNumOfIndexes(null);
		repoService.updateRepository(repository);

		ServiceFactory.clearServiceCaches();
	}
	
	private static void cleanTestData() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		String metaClassName2 = "EmbeddedTest4Index";
		String metaClassName3 = "InnerTest";
		String metaClassName4 = "QueryWithSortOnTest";
		String metaClassName5 = "Resource";
		String metaClassName6 = "Base";

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName3,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName4,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName5,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName6,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@SuppressWarnings("unchecked")
    private void checkIndexes(String metaClassName, List<String> expectedIndexNames) {
		CMSResponse response = resource.getMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO,
				metaClassName, new MockHttpServletRequest());
		if (expectedIndexNames.isEmpty()) {
			assertOkAndNullResult(response);
		} else {
			assertOkAndNotNullResult(response);
			List<IndexInfo> indexes = (List<IndexInfo>) response.get("result");
			Assert.assertEquals(expectedIndexNames.size(), indexes.size());
			List<String> indexNames = new ArrayList<String>();
			for (IndexInfo index : indexes) {
				indexNames.add(index.getIndexName());
			}
			for (String expectedIndexName : expectedIndexNames) {
				Assert.assertTrue(indexNames.contains(expectedIndexName));
			}
		}
	}

	@Test
	public void testAddIndexForParentMetaClass() {
		String metaClassName = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));
		String indexString = loadJson(PARENT_ONESTRING_INDEX);
		resource.createMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
				indexString, new MockHttpServletRequest());
		indexString = loadJson(PARENT_MANYSTRING_INDEX);
		try {
			resource.createMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
					indexString, new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, manyStringIndex, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, oneStringIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex", "oneStringIndex"));
		resource.deleteMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
				"oneStringIndex", new MockHttpServletRequest());
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testAddIndexForEmbedMetaClass() {
		String metaClassName = "EmbeddedTest4Index";
		checkIndexes(metaClassName, new ArrayList<String>());
		String indexString = loadJson(EMBED_NAME_INDEX);
		resource.createMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
				indexString, new MockHttpServletRequest());
		indexString = loadJson(EMBED_NUMBER_INDEX);
		try {
			resource.createMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
					indexString, new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.nameIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.numberIndex, manyEmbed.nameIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
		checkIndexes(metaClassName, Arrays.asList("nameIndex"));

		resource.deleteMetadataIndex(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
				"nameIndex", new MockHttpServletRequest());
		checkIndexes(metaClassName, new ArrayList<String>());
	}

	@Test
	public void testUpdateParentMetaClassByAddingIndexes() {
		String metaClassName = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));

		String meta = loadJson(UPDATE_PARENT_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, while target index names: __oidIndex__, __manyReference__Index__, manyStringIndex, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, oneStringIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));

		// reset
		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testUpdateEmbedMetaClassByAddingIndexes() {
		String metaClassName = "EmbeddedTest4Index";
		checkIndexes(metaClassName, new ArrayList<String>());

		String meta = loadJson(UPDATE_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.numberIndex, manyEmbed.nameIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}
		checkIndexes(metaClassName, new ArrayList<String>());

		// reset
		meta = loadJson(SETUP_EMBED_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName, new ArrayList<String>());
	}

	@Test
	public void testCreateNewParentMetaClass() {
		String metaClassName = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// set config back
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, false);
		dbConfig.updateConfig(newConfig);

		// create new parent
		String meta = loadJson(CREATE_NEW_PARENT_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, manyStringIndex, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, manyStringIndex, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, oneStringIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// reset
		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testBatchCreateMetaClasses1() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_EMBED1_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.numberIndex, manyEmbed.nameIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testBatchCreateMetaClasses2() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_EMBED2_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.numberIndex, manyEmbed.nameIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testBatchCreateMetaClasses3() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_EMBED3_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.numberIndex, manyEmbed.nameIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// check create action failure
		try {
			resource.getMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2, false,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(NotFoundException.class, e.getClass());
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}

	@Test
	public void testBatchCreateNestedEmbed1() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_NESTED_EMBED1_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.embed.__locks__Index__, manyEmbed.__oidIndex__, oneStringIndex, manyEmbed.embed.__oidIndex__";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}
	
	@Test
	public void testBatchCreateNestedEmbed2() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_NESTED_EMBED2_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.embed.__locks__Index__, manyEmbed.__oidIndex__, oneStringIndex, manyEmbed.oneStringIndex, manyEmbed.embed.__oidIndex__";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}
	
	@Test
	public void testBatchCreateEmbedInner1() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_EMBED_INNER1_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.__inner__Index__, oneStringIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}
	
	@Test
	public void testBatchCreateEmbedInner2() {
		String metaClassName1 = "SystemLimitationOnDocTest";
		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));

		String metaClassName2 = "EmbeddedTest4Index";
		checkIndexes(metaClassName2, new ArrayList<String>());

		CMSDBConfig dbConfig = server.getDBConfig();
		Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
		Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
		// change config for temp
		newConfig.put(CMSDBConfig.SYS_ALLOW_METADATA_DELETE, true);
		dbConfig.updateConfig(newConfig);

		// delete old parent and embed
		try {
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName1,
					new MockHttpServletRequest());
			resource.deleteMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, metaClassName2,
					new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		// create new parent and embed
		String meta = loadJson(BATCH_CREATE_NEW_PARENT_EMBED_INNER2_JSON);
		try {
			resource.createMetaClass(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			String expectedErrMsg = "com.ebay.cloud.cms.metadata.exception.IllegalIndexException: The number of index on a metaclass SystemLimitationOnDocTest should NOT be more than 8! Existed index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, oneStringIndex, while target index names: __oidIndex__, __manyReference__Index__, __manyInner__Index__, resourceIdIndex, labelIndex, manyEmbed.__oidIndex__, manyEmbed.__inner__Index__, oneStringIndex, manyEmbed.oneStringIndex";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		} catch (Exception e) {
			Assert.fail();
		}

		// reset: should create embed meta class first
		meta = loadJson(SETUP_EMBED_JSON);
		ObjectMapper mapper = new ObjectMapper();
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName2, new ArrayList<String>());

		meta = loadJson(SETUP_PARENT_JSON);
		try {
			MetaClass m = mapper.readValue(meta, MetaClass.class);
			resource.updateMetadata(nullMockUri, CMSPriority.NEUTRAL.toString(), RAPTOR_REPO, m.getName(), meta,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
		} catch (Exception e) {
			Assert.fail();
		}

		checkIndexes(metaClassName1, Arrays.asList("labelIndex", "resourceIdIndex"));
	}
}
