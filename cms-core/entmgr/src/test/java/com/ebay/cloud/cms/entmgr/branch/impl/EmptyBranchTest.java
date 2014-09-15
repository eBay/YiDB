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

package com.ebay.cloud.cms.entmgr.branch.impl;

import org.junit.Ignore;

import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

@Ignore
public class EmptyBranchTest extends CMSMongoTest {

/*    private static final String       RAPTOR_REPO      = "raptor-paas";
    private static final String       RAPTOR_TEST_DATA = "raptorTopology.json";
    private static IRepositoryService repoService      = null;
    private static MetadataDataLoader metaLoader       = null;
    private static RuntimeDataLoader  dataLoader       = null;
    private static IBranchService     branchService    = null;
    private static MongoDataSource dataSource;

	@BeforeClass
	public static void setUp(){
		String connectionString = CMSMongoTest.getConnectionString();
		dataSource = new MongoDataSource(connectionString);
        metaLoader = MetadataDataLoader.getInstance(dataSource);
		metaLoader.loadTestDataFromResource();

		dataLoader = RuntimeDataLoader.getDataLoader(dataSource, RAPTOR_REPO);
		dataLoader.load(RAPTOR_TEST_DATA);

        branchService = ServiceFactory.getBranchService(dataSource);
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
    }
	
	private IEntity newBranch(String parentBranchId){
		Repository repoInst = repoService.getRepository(RAPTOR_REPO);
		BsonEntity branch = new BsonEntity(BranchMetaClass.getMetaClass(repoInst));
		branch.addFieldValue(BranchMetaClass.IsMain, false);
		branch.addFieldValue(InternalFieldEnum.PVERSION.getName(), IEntity.NO_VERSION);
		branch.addFieldValue(BranchMetaClass.ParentId, parentBranchId);
		return branch;
	}
	
	@Test
	public void createAndGet(){
	    
		EntityContext context = new EntityContext();
		context.setSourceIp("127.0.0.1");
		
		List<IBranch> branches = branchService.getBranches(RAPTOR_REPO, context);
		assertEquals(1, branches.size());
		
		context = new EntityContext();
        context.setSourceIp("127.0.0.1");
		
		IEntity branch = newBranch(IBranch.DEFAULT_BRANCH); 
		branch.addFieldValue("name", "branch1");
		String newBranchId = branchService.createBranch(branch, context);
		assertTrue(newBranchId != null);
		
		context = new EntityContext();
		context.setSourceIp("127.0.0.1");
		branches = branchService.getBranches(RAPTOR_REPO, context);
        assertEquals(2, branches.size());
		
		IBranch branchGet = branchService.getBranch(RAPTOR_REPO, newBranchId, context);
		assertTrue(branchGet != null);
		assertEquals(false, branchGet.isMainBranch());
		assertEquals(IBranch.DEFAULT_BRANCH, branchGet.getParentBranchId());
	}
	
	
	@Test
	public void createAndCommit(){
		EntityContext context = new EntityContext();
		context.setSourceIp("127.0.0.1");
		IEntity branch = newBranch(IBranch.DEFAULT_BRANCH);
		branch.addFieldValue("name", "branch2");
		String newBranchId = branchService.createBranch(branch, context);
		branchService.commit(RAPTOR_REPO, newBranchId, context);
		IBranch branchGet = branchService.getBranch(RAPTOR_REPO, newBranchId, context);
        assertEquals(BranchStatusEnum.COMMITTED,branchGet.getBranchStatus());
	}
	
	@Test
    public void createAndAbort(){
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        IEntity branch = newBranch(IBranch.DEFAULT_BRANCH);
        branch.addFieldValue("name", "branch3");
        String newBranchId = branchService.createBranch(branch, context);
        branchService.abort(RAPTOR_REPO, newBranchId, context);
        
        IBranch branchGet = branchService.getBranch(RAPTOR_REPO, newBranchId, context);
        assertEquals(BranchStatusEnum.ABORTED,branchGet.getBranchStatus());
        Assert.assertEquals(branch.getFieldValues("name").get(0), newBranchId);
    }

	@Test (expected = IllegalStateException.class)
	public void createAndCommit2(){
		EntityContext context = new EntityContext();
		context.setSourceIp("127.0.0.1");
		IEntity branch = newBranch(IBranch.DEFAULT_BRANCH); 
		branch.addFieldValue("name", "branch4");
		String childBranchId1 = branchService.createBranch(branch, context);
		
		IEntity branch2 = newBranch(childBranchId1); 
		branch2.addFieldValue("name", "branch42");
		String childBranchId2 = branchService.createBranch(branch2, context);
		
		branchService.commit(RAPTOR_REPO, childBranchId1, context);
		
		IBranch branchGet = branchService.getBranch(RAPTOR_REPO, childBranchId1, context);
		assertEquals(BranchStatusEnum.ACTIVE,branchGet.getBranchStatus());
		branchGet = branchService.getBranch(RAPTOR_REPO, childBranchId2, context);
		assertEquals(BranchStatusEnum.ACTIVE,branchGet.getBranchStatus());
		
	}
	
	
	@Test
	public void createAndAbort2(){
		EntityContext context = new EntityContext();
		context.setSourceIp("127.0.0.1");
		IEntity branch = newBranch(IBranch.DEFAULT_BRANCH);
		branch.addFieldValue("name", "branch5");
		String childBranchId1 = branchService.createBranch(branch, context);
		
		IEntity branch2 = newBranch(childBranchId1); 
		branch2.addFieldValue("name", "branch52");
		String childBranchId2 = branchService.createBranch(branch2, context);
		branchService.abort(RAPTOR_REPO, childBranchId1, context);
		
		IBranch branchGet1 = branchService.getBranch(RAPTOR_REPO, childBranchId1, context);
		IBranch branchGet2 = branchService.getBranch(RAPTOR_REPO, childBranchId2, context);
		assertEquals(BranchStatusEnum.ABORTED,branchGet1.getBranchStatus());
		assertEquals(BranchStatusEnum.ABORTED,branchGet2.getBranchStatus());
		
	}

    @Test
    public void deleteRepoClearBranchCache() {
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        IBranch branch = branchService.getBranch(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, context);
        Assert.assertNotNull(branch);

        repoService.deleteRepository(RAPTOR_REPO);

        // Simulate the case that the repo is deleted, then re-created in mongo.
        // NOTE: the repo service is cleared, so after the metaloader load, the old <code>repoService</code> is deprecated
        metaLoader.loadTestDataFromResource();
        
        repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        ServiceFactory.clearServiceCaches();
        branchService = ServiceFactory.getBranchService(dataSource);
        //meta loader will clean up the repo service, and only load the metadatas
        try {
            IBranch branch2 = branchService.getBranch(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, context);
            Assert.assertNull(branch2);
        } catch (CmsEntMgrException e) {
            // expected
            Assert.assertEquals(CmsEntMgrException.EntMgrErrCodeEnum.BRANCH_NOT_FOUND, e.getErrorEnum());
        }

        dataLoader.load(RAPTOR_TEST_DATA);
    }

    @Test
    public void deleteRepoClearBranchCache1() {
        EntityContext context = new EntityContext();
        context.setSourceIp("127.0.0.1");
        IBranch branch = branchService.getBranch(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, context);
        Assert.assertNotNull(branch);

        repoService.deleteRepository(RAPTOR_REPO);
        ServiceFactory.clearServiceCaches();
        branchService = ServiceFactory.getBranchService(dataSource);
        try {
            branchService.getBranch(RAPTOR_REPO, IBranch.DEFAULT_BRANCH, context);
            Assert.fail();
        } catch (RepositoryNotExistsException e) {
            // expected - the cache is hit and get null back, so go back for
            // mongo query, then get this repository exception
        }
    }
*/
}
