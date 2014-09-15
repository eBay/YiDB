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

package com.ebay.cloud.cms.entmgr.history.impl;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.entmgr.history.IEntityHistoryService;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.entmgr.service.ServiceFactory;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class HistoryServiceTest extends CMSMongoTest{
	private static IEntityHistoryService historyService = null;
	private static IRepositoryService repoService = null;
	private static IMetadataService metaService = null;

    private static final String       RAPTOR_REPO      = "raptor-paas";
    private static final String       RAPTOR_TEST_DATA = "raptorTopology.json";
    private static MetadataDataLoader metaLoader       = null;
    private static CMSDBConfig config = null;

	@BeforeClass
	public static void setUp(){
		String connectionString = CMSMongoTest.getConnectionString();
		MongoDataSource dataSource = new MongoDataSource(connectionString);
		config = new CMSDBConfig(dataSource);
		metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadTestDataFromResource();
		
		repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
		RuntimeDataLoader runtimeLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, RAPTOR_REPO);
		runtimeLoader.load(RAPTOR_TEST_DATA);
		
		metaService = repoService.getRepository(RAPTOR_REPO).getMetadataService();
		
		List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);
		
		historyService = ServiceFactory.getHistoryService(config, dataSource, implementations);
	}
	
	@Test
	public void testMetaHistory() throws InterruptedException {
	    MetadataContext context = new MetadataContext();
	    context.setSourceIp("127.0.0.1");
	    context.setDbConfig(config);
	    String name = "ServiceInstance";
	    MetaClass serviceCls = metaService.getMetaClass(name);

	    String historyId = historyService.addHistory(serviceCls, "create", context);

	    Assert.assertNotNull(historyId);
	    System.out.println(historyId);

	    List<IEntity> histories = historyService.getHistoryEntities(RAPTOR_REPO, name, context);
	    Assert.assertNotNull(histories);
	    Assert.assertTrue(histories.size() == 1);
	    
	    Thread.sleep(1000);
	    context.setStart(new Date());
	    List<IEntity> histories1 = historyService.getHistoryEntities(RAPTOR_REPO, name, context);
        Assert.assertNotNull(histories1);
        Assert.assertTrue(histories1.size() == 0);
        
        context.setStart(null);
        context.setEnd(null);
        
        context.setEnd(new Date());
        try {
            context.setStart(new Date( new Date().getTime() + 1000L));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expect
        }
        
        context.setEnd(null);
        context.setStart(new Date());
        try {
            context.setEnd(new Date( new Date().getTime() - 1000L));
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expect
        }
	}
	
	@Test
    public void testSupplyHistoryMetaIndex() {
    	// drop metaclass_history index
    	MetadataContext context = new MetadataContext();
    	context.setSourceIp("127.0.0.1");
    	context.setOptionChangeMode(UpdateOptionMode.DELETE);
    	MetaOption options = new MetaOption();
		IndexInfo ii = new IndexInfo(HistoryMetaClass.INDEX_NAME);
		options.addIndex(ii);
    	metaService.updateMetaOption(HistoryMetaClass.NAME, options, context);
    	
    	// check index was dropped
    	MetaClass historyMeta = metaService.getMetaClass(HistoryMetaClass.NAME);
    	Assert.assertTrue(!historyMeta.getIndexNames().contains(HistoryMetaClass.INDEX_NAME));
    	
    	// clear cache
    	ServiceFactory.clearServiceCaches();
    	
        List<PersistenceService.Registration> implementations = RegistrationUtils.getTestDalImplemantation(dataSource);

    	// reload history service
    	ServiceFactory.getHistoryService(config, dataSource, implementations);
    	historyMeta = metaService.getMetaClass(HistoryMetaClass.NAME);
    	
    	// check index was recreated
    	Assert.assertTrue(historyMeta.getIndexNames().contains(HistoryMetaClass.INDEX_NAME));
    	IndexInfo index = historyMeta.getIndexByName(HistoryMetaClass.INDEX_NAME);
    	Assert.assertEquals(2, index.getKeyList().size());
    	Assert.assertTrue(index.getKeyList().contains(HistoryMetaClass.EntityId));
    	Assert.assertTrue(index.getKeyList().contains(HistoryMetaClass.EntityVersion));
    }
}
