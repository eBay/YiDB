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

package com.ebay.cloud.cms.metadata.dataloader;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

public class MetadataDataLoaderTest extends CMSMongoTest {
    
    static MongoDataSource ds;
    static Mongo mongo;
    
    @BeforeClass
    public static void setUp() {
        ds = new MongoDataSource(getConnectionString());
        mongo = ds.getMongoInstance();
        
        MetadataDataLoader loader = MetadataDataLoader.getInstance(ds); 
        loader.cleanUp();
        loader.loadPropertiesFromResource("/mongo/properties.json");
        loader.loadMetaClassesFromResource("/mongo/metaclasses.json");
    }
    
    @AfterClass
    public static void shutDown() {
        
    }
    
    @Test
    public void testLoadProperties() {
        DBCollection coll = mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.PROPERTIES_COLLECTION);
        DBCursor cursor = coll.find();
        
        BasicDBObject object = (BasicDBObject)cursor.next();
        Assert.assertEquals(5000, object.getInt("RepositoryCacheSize"));
        
        object = (BasicDBObject)cursor.next();
        Assert.assertEquals(3600, object.getInt("RepositoryCacheExpireSeconds"));
    }
    
    @Test
    public void testLoadRepositories() {
        IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
        Repository repo = repoService.getRepository("raptor-paas");
        Assert.assertEquals("raptor-paas", repo.getRepositoryName());
        repo = repoService.getRepository("software-deployment");
        Assert.assertEquals("software-deployment", repo.getRepositoryName());
    }
    
    @Test
    public void testLoadMetaClass() {
        MetadataDataLoader loader = MetadataDataLoader.getInstance(ds);
        
        loader.cleanUp();
        loader.loadPropertiesFromResource("/mongo/properties.json");
        loader.loadMetaClassesFromResource("/mongo/metaclasses.json");
        
        IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
        MetaClass metaclass = repoService.getRepository("raptor-paas").getMetadataService().getMetaClass("ApplicationService");
        Assert.assertEquals("ApplicationService", metaclass.getName());
        
        metaclass = repoService.getRepository("raptor-paas").getMetadataService().getMetaClass("Environments");
        Assert.assertEquals("Environment", metaclass.getName());
        
        
        metaclass = repoService.getRepository("software-deployment").getMetadataService().getMetaClass("Deployment");
        Assert.assertEquals("Deployment", metaclass.getName());
        Assert.assertNotNull(metaclass.getFieldByName("targets"));
    }
    
}
