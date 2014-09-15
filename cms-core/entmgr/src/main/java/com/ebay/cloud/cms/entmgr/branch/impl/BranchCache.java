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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.entmgr.branch.IBranch;

/*
 * 
 * Change the BranchCache as we only have one branch for now
 */
public class BranchCache {
    private static final Logger logger = LoggerFactory.getLogger(BranchCache.class);
    
    private static class Key {
        private String repoName;
        private String branchId;
        
        public Key(String repoName, String branchId) {
            super();
            this.repoName = repoName;
            this.branchId = branchId;
        }

        @Override
        public int hashCode() {
            return repoName.hashCode() + branchId.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            
            if (!(other instanceof Key)) { 
                return false;
            }
            
            Key o = (Key)other;
            return repoName.equals(o.repoName) && branchId.equals(o.branchId);
        }
    }
    
    private Map<Key, IBranch> cache;
    
    public BranchCache() {
        cache = new ConcurrentHashMap<Key, IBranch>();
    }

    public IBranch getBranch(String repoName, String branchId) {
        if (repoName == null || branchId == null) {
            return null;
        }
        return cache.get(new Key(repoName, branchId));
    }
    
    public void putBranch(IBranch b) {
        if (cache.size() > 10000) {
            logger.error("Branch Cache is full!");
            return;
        }
        
        String repoName = b.getRepositoryName();
        String branchId = b.getId();
        
        if (repoName != null && branchId != null) {
            cache.put(new Key(repoName, branchId), b);
        }
    }
    
    public void clearCache() {
        cache.clear();
    }
}
