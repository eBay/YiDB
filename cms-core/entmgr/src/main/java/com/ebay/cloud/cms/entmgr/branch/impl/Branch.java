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

import com.ebay.cloud.cms.entmgr.branch.IBranch;

public class Branch implements IBranch {

    private String repositoryName;
    private String id;
    private boolean mainBranch;
    
    public Branch() {
    	
    }
    
    public Branch(IBranch branch) {
	    this.repositoryName =  branch.getRepositoryName();
	    this.id = branch.getId();
	    this.mainBranch = branch.isMainBranch();
    }

    public String getRepositoryName() {
        return repositoryName;
    }
    
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public boolean isMainBranch() {
        return mainBranch;
    }
    
    public void setMainBranch(boolean mainBranch) {
        this.mainBranch = mainBranch;
    }

}
