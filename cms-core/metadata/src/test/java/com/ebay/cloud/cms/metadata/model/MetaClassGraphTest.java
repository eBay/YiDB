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

package com.ebay.cloud.cms.metadata.model;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MetaClassGraphTest extends CMSMongoTest {
    
    static IRepositoryService repositoryService;
    static String repoName = "MetaClassGraphTest";
    static Repository repo;
    
    @BeforeClass
    public static void createRepoService() {
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();
        
        repositoryService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
        repo = repositoryService.createRepository(new Repository(repoName));
    }
    
    @Test
    public void testMetaClassCreation() {

        MetaClass b = new MetaClass();
        b.setName("B");
        
        MetaClass a = new MetaClass();
        a.setName("A");
        MetaRelationship r = new MetaRelationship();
        r.setName("r");
        r.setRelationType(RelationTypeEnum.Reference);
        r.setRefDataType("B");
        a.addField(r);
        
        MetaClass da = new MetaClass();
        da.setName("dA");
        da.setParent("A");
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setSubject("tester");
        ms.batchUpsert(Arrays.asList(b, a, da), context);
        
        Assert.assertEquals(0, ms.getMetaClass("A").getFromReference().size());
        Assert.assertEquals(1, ms.getMetaClass("B").getFromReference().size());
        Assert.assertEquals(0, ms.getMetaClass("dA").getFromReference().size());
        
        Assert.assertEquals(1, ms.getMetaClass("A").getToReference().size());
        Assert.assertEquals(0, ms.getMetaClass("B").getToReference().size());
        Assert.assertEquals(1, ms.getMetaClass("dA").getToReference().size());
        
        Assert.assertEquals(1, ms.getMetaClass("A").getDescendants().size());
        Assert.assertEquals(0, ms.getMetaClass("B").getDescendants().size());
        Assert.assertEquals(0, ms.getMetaClass("dA").getDescendants().size());
    }
    

}
