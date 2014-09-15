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

package com.ebay.cloud.cms.metadata.service;

import java.util.List;

import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.Repository.AccessType;
import com.mongodb.WriteConcern;

/**
 * IRepositoryService
 * 
 * @author liubo
 *
 */
public interface IRepositoryService {

    /**
     * Create new repository named repositoryName. 
     * 
     * @param repositoryName
     */
    public Repository createRepository(Repository repository);
    
    /**
     * Create new repository named repositoryName. 
     * 
     * @param repositoryName
     * @param writeConcern
     */
    public Repository createRepository(Repository repository, WriteConcern writeConcern);
    
    public void updateRepository(Repository repository);
    
    /**
     * get repository by it's name
     * 
     * @param repositoryName
     * @return
     */
    public Repository getRepository(String repositoryName);
    
    /**
     * get all repositories in cms
     * 
     * @return
     */
    public List<Repository> getRepositories(MetadataContext context);
    
    /**
     * Delete repository by name
     * 
     * @param repositoryName
     */
    public void deleteRepository(String repositoryName);
    
    /**
     * Returns all repositories with the given access type.
     * 
     * @param type
     */
    public List<Repository> getRepositories(AccessType type);

    public void setHistoryService(IMetadataHistoryService service);

}
