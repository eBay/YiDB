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

package com.ebay.cloud.cms.entmgr.utils;


import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MapperTest extends CMSMongoTest{

    private static final String RAPTOR = "raptor-paas";
    private static final String raptorJsonPath = "raptorTopology.json";
    
    private static final String DEPLOYMENT= "software-deployment";
    private static final String deploymentJsonPath = "softwareDeploymentRuntime.json";
    
    private static final Logger logger = LoggerFactory.getLogger(MapperTest.class);
    
    private static MetadataDataLoader metaLoader = null;
    
    @BeforeClass
    public static void setUp(){
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        metaLoader = MetadataDataLoader.getInstance(ds);
        metaLoader.loadTestDataFromResource();
    }

    /**
     * Test all mapping combinations:
     * Json to Entity/Bson
     * Entity to Json/Bson
     * Bson to Entity/Json
     */
    private void mappingCombinations(String repoName, String runtimeJsonFile){
    	RuntimeDataLoader dataLoader = null;
    	try{
    		String collectionString = getConnectionString();
            MongoDataSource dataSource = new MongoDataSource(collectionString);
            
            IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
            dataLoader = RuntimeDataLoader.getDataLoader(dataSource, repoService, repoName);
    		List<IEntity> loadList = dataLoader.load(runtimeJsonFile);
        	for(IEntity loadEntity: loadList){
        		MetaClass metaClass = loadEntity.getMetaClass();
        		String metaType = metaClass.getName();
        		String instName = (String) loadEntity.getFieldValues("name").get(0);
        		logger.debug(String.format("Test mapping of instance %s of type %s",instName,metaType));
        		
        		logger.debug("Test mapping between json and bson");
        		 		
        	}
    	}catch(Exception ex){
    		ex.printStackTrace();
    		throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON,ex);
    	}finally{
    		if(dataLoader!=null){
    			dataLoader.cleanUp();
    			logger.debug(String.format("runtime data of repository %s is cleaned",dataLoader.getRepositoryName()));
    		}
    	}
	}
    
    @Test
    public void testMappingCombinations(){
    	mappingCombinations(RAPTOR, raptorJsonPath);
    	mappingCombinations(DEPLOYMENT, deploymentJsonPath);
    }
}
