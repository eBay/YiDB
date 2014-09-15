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

package com.ebay.cloud.cms.dal.search;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.common.RaptorEntityGenerator;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;

/**
 * @author shuachen
 * 
 *         2013-11-27
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SearchServiceImpl.class)
@PowerMockIgnore({ "javax.management.*", "javax.xml.parsers.*",
    "com.sun.org.apache.xerces.internal.jaxp.*", "ch.qos.logback.*",
            "org.slf4j.*",  "com.sun.org.apache.xerces.internal.impl.dv.dtd.*" })
public class SearchServiceTest extends SearchBaseTest {

	private static final String BRANCH_TEST = "test";

	private List<BsonEntity> newEntityList = new ArrayList<BsonEntity>();

	@Ignore
	@Test(expected = CmsDalException.class)
	public void testExceedMemoryUsageForQuery() {
		ISearchService spy = PowerMockito.spy(searchService);
		try {
			PowerMockito.doReturn(10L).when(spy, "getSysLimitMemoryForMongoQuery", Mockito.any(CMSDBConfig.class));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
		
		prepareData();
		
		CMSDBConfig mockDBConfig = Mockito.mock(CMSDBConfig.class);
		raptorContext.setDbConfig(mockDBConfig);

		MetaClass metadata = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
		SearchProjection projection = new SearchProjection();
		projection.addField(ProjectionField.STAR);
		ISearchQuery query = new SearchQuery(metadata, null, projection, strategy);
		SearchOption option = new SearchOption();
		try {
			spy.search(query, option, raptorContext);
		} catch (CmsDalException cde) {
			throw cde;
		} finally {
			cleanData();
			raptorContext.setDbConfig(null);
		}

	}

	private void prepareData() {
		for (int i = 0; i < 100; i++) {
			BsonEntity entity1 = newServiceInstance(i);
			String newId = persistenceService.create(entity1, raptorContext);
			entity1.setId(newId);
			newEntityList.add(entity1);
		}
	}

	private void cleanData() {
		if (!newEntityList.isEmpty()) {
			for (BsonEntity entity : newEntityList) {
				persistenceService.delete(entity, raptorContext);
			}
		}
	}

	private static BsonEntity newServiceInstance(int seq) {
		MetaClass metaClass = raptorMetaService.getMetaClass(RaptorEntityGenerator.TypeEnum.ServiceInstance.name());
		BsonEntity newEntity = new BsonEntity(metaClass);
		newEntity.addFieldValue("name", "ServiceInstance-SearchServiceTest-" + seq);
		newEntity.setBranchId(BRANCH_TEST);
		newEntity.addFieldValue(InternalFieldEnum.CREATOR.getName(), "unitTestUser");
		return newEntity;
	}

}
