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

package com.ebay.cloud.cms.service.resources;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.resources.impl.BranchResource;
import com.ebay.cloud.cms.service.resources.impl.EntityResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;

public class BranchResourceTest extends CMSResourceTest {

    BranchResource branchResource = new BranchResource();
    EntityResource entityResource = new EntityResource();

    @SuppressWarnings("unchecked")
    @Test
    public void getBranch() {
        String branchId = "main";
        CMSResponse resp = branchResource.getBranch(nullMockUri, CMSPriority.NEUTRAL.toString(), null, RAPTOR_REPO,
                branchId, new MockHttpServletRequest());
        assertOkAndNotNullResult(resp);
        List<Branch> branches = (List<Branch>) resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, branches.size());
        Assert.assertEquals(branchId, branches.get(0).getId());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateMainBranch() {
        Branch branch = new Branch();
        branch.setId("branchresource-test");
        branch.setMainBranch(true);
        branch.setRepositoryName(RAPTOR_REPO);

        branchResource.createBranch(nullMockUri, CMSPriority.NEUTRAL.toString(), null, RAPTOR_REPO, branch,
                new MockHttpServletRequest());

        CMSResponse resp = branchResource.getMainBranches(nullMockUri, CMSPriority.NEUTRAL.toString(), null, RAPTOR_REPO,
                new MockHttpServletRequest());

        List<Branch> branches = (List<Branch>) resp.get(CMSResponse.RESULT_KEY);
        Assert.assertNotNull(branches);
        boolean foundCreate = false;
        for (Branch b : branches) {
            if (branch.getId().equals(b.getId())) {
                foundCreate = true;
                break;
            }
        }
        Assert.assertTrue(foundCreate);
    }
    
    @Test
    public void testCreateSubBranch() {
        Branch branch = new Branch();
        branch.setId("branhresource-test-subbranch");
        branch.setMainBranch(false);
        branch.setRepositoryName(RAPTOR_REPO);
        
        try {
            branchResource.createBranch(nullMockUri, CMSPriority.NEUTRAL.toString(), null, RAPTOR_REPO, branch,
                new MockHttpServletRequest());
            Assert.fail();
        } catch(CMSServerException e) {
            
        }
    }

}
