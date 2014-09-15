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

package com.ebay.cloud.cms.metadata.mongo;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.RepositoryExistsException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.Repository.AccessType;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;
import com.ebay.cloud.cms.metadata.model.internal.HistoryMetaClass;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;

public class MongoRepositoryServiceImplTest extends CMSMongoTest {
    
    static IRepositoryService repositoryService;
    static MetadataDataLoader metaLoader;
    
    @BeforeClass
    public static void createRepoService() {
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();

        repositoryService  = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testRepoServiceInvalidArg() {
        try {
            new MongoRepositoryServiceImpl(null, 1, 1, 1, 1, null, WriteConcern.SAFE);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoRepositoryServiceImpl(new Mongo(), 0, 1, 1, 1, null, WriteConcern.SAFE);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoRepositoryServiceImpl(new Mongo(), 1, 0, 1, 1, null, WriteConcern.SAFE);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoRepositoryServiceImpl(new Mongo(), 1, 1, 1, 1, null, WriteConcern.SAFE);
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testRepositoryCreation() {
        String repoName = "testRepoCreation";
        try {
            repositoryService.getRepository(repoName);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        
        Repository r = repositoryService.createRepository(new Repository("r1"));
        Assert.assertEquals("r1", r.getRepositoryName());
        
        Assert.assertNotNull(repositoryService.getRepository(r.getRepositoryName()));
        Assert.assertNotNull(repositoryService.getRepositories(new MetadataContext()));
    }

    @Test
    public void testRepsitory() {
        MetadataContext context = new MetadataContext();
        context.setRefreshRepsitory(false);
        List<Repository> oldRepos = repositoryService.getRepositories(context);
        Assert.assertTrue(oldRepos.size() > 0);
        
        List<Repository> cachedRepos = repositoryService.getRepositories(context);
        for (final Repository repo : oldRepos) {
            Assert.assertNotNull(CollectionUtils.find(cachedRepos, new Predicate() {
                
                @Override
                public boolean evaluate(Object object) {
                    if (object == repo) {
                        return true;
                    }
                    return false;
                }
            }));
        }
        
        context.setRefreshRepsitory(true);
        List<Repository> newRepos = repositoryService.getRepositories(context);
        for (final Repository repo : newRepos) {
            Assert.assertNull(CollectionUtils.find(cachedRepos, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    if (object == repo) {
                        return true;
                    }
                    return false;
                }
            }));
        }
    }

    @Test
    public void testRepositoryUpdate() {
        String repoName = "testRepoUpdate";
        String repoAdmin = "unit";
        Repository repo = new Repository(repoName);
        AccessType oldAccessType = AccessType.Public;
        repo.setAccessType(oldAccessType);
        repo.setCreateTime(new Date());
        repo.setRepositoryAdmin(repoAdmin);

        Repository r = repositoryService.createRepository(repo);
        Assert.assertEquals(oldAccessType, r.getAccessType());
        Assert.assertEquals(repoAdmin, r.getRepositoryAdmin());

        // case 0 : do update
        AccessType newAccessType = AccessType.Private;
        repo.setAccessType(newAccessType);
        String newAdmin = "newadmin";
        repo.setRepositoryAdmin(newAdmin);
        repositoryService.updateRepository(repo);
        Repository getRepo = repositoryService.getRepository(repoName);
        // access type changed also
        Assert.assertEquals(newAccessType, getRepo.getAccessType());
        Assert.assertEquals(newAdmin, getRepo.getRepositoryAdmin());

        // case 1 : admin not given, should return ok, no error
        repo.setRepositoryAdmin(null);
        repositoryService.updateRepository(repo);
        getRepo = repositoryService.getRepository(repoName);
        // access type changed also
        Assert.assertEquals(newAccessType, getRepo.getAccessType());
        // new admin not changed.
        Assert.assertEquals(newAdmin, getRepo.getRepositoryAdmin());
    }
    
    @Test
    public void testBadReponameCreation() {
        String repoName = "test\"RepoCreation";
        try {
            repositoryService.createRepository(new Repository(repoName));
            Assert.fail();
        }
        catch (MetaDataException e) {
            
        }

        //this repo name should not exist in the cms_sys db 
        try {
            repositoryService.getRepository(repoName);
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testRespositoryInitialization() {
        String repoName = "testRepoCreationInit";
        try {
            repositoryService.getRepository(repoName);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        Repository r = repositoryService.createRepository(new Repository(repoName));
        Assert.assertEquals(repoName, r.getRepositoryName());
        
        Assert.assertNotNull(HistoryMetaClass.getMetaClass(r));
        Assert.assertNotNull(BranchMetaClass.getMetaClass(r));

        try {
            HistoryMetaClass.createHistoryMetaClass(r);
            BranchMetaClass.createBranchMetaClass(r);
        } catch (Exception e) {
            //1. create history/branch metadata class doesn't throw error when the metadata is already exsits
            Assert.fail();
        }
    }
    
    @Test(expected=RepositoryExistsException.class)
    public void testRepositoryCreationException() {
        String repoName = "testRepoCreation";
        try {
            repositoryService.getRepository(repoName);
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        Repository r = null;
        
        r = repositoryService.createRepository(new Repository("r2"));
        Assert.assertEquals("r2", r.getRepositoryName());
        
        r = repositoryService.createRepository(new Repository("r2"));
    }
    
    
    @Test
    public void testGetAll() {
        List<Repository> repos = repositoryService.getRepositories(new MetadataContext());
        
        String repoName1 = "testGetAll1";
        String repoName2 = "testGetAll2";
        
        repositoryService.createRepository(new Repository(repoName1));
        repositoryService.createRepository(new Repository(repoName2));
        
        List<Repository> newRepos = repositoryService.getRepositories(new MetadataContext());
        Assert.assertEquals(repos.size() + 2, newRepos.size());
        
        boolean found1 = false;
        boolean found2 = false;
        for (Repository r : newRepos) {
            if (r.getRepositoryName().equals(repoName1)) found1 = true;
            if (r.getRepositoryName().equals(repoName2)) found2 = true;
        }
        Assert.assertTrue(found1 && found2);
    }
    
    @Test
    public void testDelete() {
        String r1 = "testDelete";
        
        repositoryService.createRepository(new Repository(r1));
        
        Assert.assertNotNull(repositoryService.getRepository(r1));
        
        MongoDataSource ds = new MongoDataSource(getConnectionString() + "," + getConnectionString());
        IRepositoryService repositoryService2  = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
        Assert.assertFalse(repositoryService == repositoryService2);
        Assert.assertNotNull(repositoryService2.getRepository(r1));
        
        
        repositoryService.deleteRepository(r1);
        try {
            repositoryService.getRepository(r1);
            Assert.fail();
        } catch (RepositoryNotExistsException e) {
            // expected
        }
        
        try {
            repositoryService2.getRepository(r1);
            Assert.fail();
        } catch (RepositoryNotExistsException e) {
            // expected
        }
    }

}
