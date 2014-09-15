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

package com.ebay.cloud.cms.metadata.mongo.converter;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.metadata.model.Repository;

public class RepositoryConverterTest {
    
    @Test
    public void testRepositoryConvert() {
        String repositoryName = "name";
        
        Repository repo = new Repository();
        repo.setRepositoryName(repositoryName);
        Date now = new Date();
        repo.setCreateTime(now);
        repo.setState(Repository.StateEnum.normal);
        
        ObjectConverter<Repository> c = new ObjectConverter<Repository>();
        String json = c.toJson(repo);
        Repository repo1 = c.fromJson(json, Repository.class);
        Assert.assertEquals(repo1.getRepositoryName(), repo.getRepositoryName());
        Assert.assertEquals(repo1.getCreateTime(), repo.getCreateTime());
//        Assert.assertEquals(repo1.getDeletedTime(), repo.getDeletedTime());
//        Assert.assertEquals(repo1.getDeletingTime(), repo.getDeletingTime());
    }
    
    
}
