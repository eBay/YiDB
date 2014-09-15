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


/**
 * 
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

package com.ebay.cloud.cms.query.executor.result;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.query.metadata.QueryMetaClass;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/**
 * @author liasu
 * 
 */
public class AggregateMetaclassTest extends CMSMongoTest {

    private static final String APPLICATION_SERVICE = "ApplicationService";

    private static String       RAPTOR_PAAS         = "raptor-paas";

    private static Repository   repo;

    private static IRepositoryService repositoryService;

    @BeforeClass
    public static void setup() {
        MetadataDataLoader loader = MetadataDataLoader.getInstance(getDataSource());
        loader.loadTestDataFromResource();

        repositoryService = RepositoryServiceFactory.createRepositoryService(getDataSource(), "localCMSServer");
        repo = repositoryService.getRepository(RAPTOR_PAAS);
    }

    @Test
    public void testEquals() {
        QueryContext queryContext = new QueryContext(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH);
        queryContext.setRepositoryService(repositoryService);
        MetaClass appMeta = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);
        QueryMetaClass appAmc = QueryMetaClass.newInstance(queryContext, appMeta);

        MetaClass servMeta = repo.getMetadataService().getMetaClass("ServiceInstance");
        QueryMetaClass servAmc = QueryMetaClass.newInstance(queryContext, servMeta);

        Assert.assertFalse(servAmc.equals(appAmc));
        Assert.assertFalse(appAmc.equals(servAmc));

        Assert.assertTrue(appAmc.equals(appAmc));
        Assert.assertTrue(servAmc.equals(servAmc));

        Assert.assertFalse(appAmc.equals(null));
        Assert.assertFalse(appAmc.equals(new MetaAttribute(false)));

        QueryMetaClass appAmc2 = QueryMetaClass.newInstance(queryContext, appMeta);
        Assert.assertTrue(appAmc.equals(appAmc2));
        Assert.assertTrue(appAmc2.equals(appAmc));

        appAmc2.setRepository(null);
        appAmc2.setName(APPLICATION_SERVICE);
        Assert.assertFalse(appAmc.equals(appAmc2));
        Assert.assertFalse(appAmc2.equals(appAmc));

        appAmc2.setRepository(RAPTOR_PAAS);

        Assert.assertTrue(appAmc.equals(appAmc2));
        Assert.assertTrue(appAmc2.equals(appAmc));
    }
}
