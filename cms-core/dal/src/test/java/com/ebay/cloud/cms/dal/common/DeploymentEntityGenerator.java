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

package com.ebay.cloud.cms.dal.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
/**
 * 
 * @author jianxu1
 * @date  2012/5/23
 * 
 * @history
 * generate runtime data for deployment, including embed documents
 *
 */
public class DeploymentEntityGenerator {

	public static enum TypeEnum{
		Manifest,
		DeploymentEntityStatus,
		PackageVersion,
		Approval,
		Package,
		ManifestVersion,
		DeploymentEvent,
		DeploymentTarget,
		Deployment
	}
	
    private static IMetadataService metaService = null;
    private static DummyEntity help = null;
    private static final String DEPLOYMENT = "software-deployment";
    private static MongoDataSource dataSource = null;
    
    private static final Logger logger = LoggerFactory.getLogger(DeploymentEntityGenerator.class);
    
    private static IRepositoryService repositoryService = null;
    
    static {
        dataSource = new MongoDataSource(CMSMongoTest.getConnectionString());
        repositoryService  = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        Repository repo = repositoryService.getRepository(DEPLOYMENT);
        metaService = repo.getMetadataService();
        help = new DummyEntity(metaService);
    }
    
    
    private static BsonEntity newDummyEntity(String baseName, String branchId, TypeEnum type, int index){
    	String name = String.format("Dummy %s %s-%04d",type.name(),baseName, index);
    	BsonEntity newEntity = help.newEntityWithDummyValues(type.name(),branchId, name);
    	return newEntity;
    }
    
    
    public static BsonEntity generateManifest(String baseName, String branchId, List<BsonEntity> persistEntityList){
    	//create one manifest
     	BsonEntity manifest = newDummyEntity(baseName,branchId,TypeEnum.Manifest,1);
     	
    	//each manifest has 2 versions
    	int manifestVersionCnt = 2;
    	for(int manifestVersionIndex =1; manifestVersionIndex <= manifestVersionCnt; manifestVersionIndex++){
    		
    		BsonEntity manifestVersion = newDummyEntity(baseName,branchId, TypeEnum.ManifestVersion,manifestVersionIndex);
    		BsonEntity approval = newDummyEntity(baseName,branchId, TypeEnum.Approval,manifestVersionIndex);
    		
    		manifestVersion.removeField("createdBy");
    		//each manifest version has a approval
    		manifestVersion.addFieldValue("approvals", approval);
    		//add manifest version into manifest
    		manifest.addFieldValue("versions", manifestVersion);
    		
        	int packageCnt = 3;
        	for(int packageIndex =1; packageIndex <= packageCnt; packageIndex++){
        		BsonEntity packagerVersion = newDummyEntity(baseName,branchId, TypeEnum.PackageVersion,1);
        		
        		//currently each package only has one version
        		BsonEntity packageEntity = newDummyEntity(baseName,branchId ,TypeEnum.Package,packageIndex);
        		//add package version into package
        		packageEntity.addFieldValue("versions",packagerVersion);

        		persistEntityList.add(packageEntity);
        		
        		//add package into manifest version
        		manifestVersion.addFieldValue("packages", packageEntity);
        		

        	}
    	}
    	
    	//manifest must be created after packages
    	persistEntityList.add(manifest);
    	return manifest;
    }
    
    /**
     * Deployment bundle structure:
     * Deployment 
     * 			  -- Entity Status     (Many)
     *            -- Deployment Events (Many)
     *            -- Deployment Targets(Many)
     *            			-- Manifest (Many)
     *                              -- Manifest Version
     *                                    -- Approval (Many)
     *                                    -- Package  (Many)
     *                                         -- Package Version (Many)
     * @param baseName
     * @return
     */
    public static List<BsonEntity> generateDeploymentBundle(String baseName, String branchId){
    	
    	List<BsonEntity> resultList = new ArrayList<BsonEntity>();
    	
    	BsonEntity manifest = generateManifest(baseName,branchId,resultList);

    	BsonEntity deployment = newDummyEntity(baseName,branchId,TypeEnum.Deployment,1);
    	
    	int index = 0;
    	int deployValueCnt = 5;
    	for(index =1; index <= deployValueCnt; index++){
    		IEntity status = newDummyEntity(baseName,branchId, TypeEnum.DeploymentEntityStatus,index);
    		//resultList.add(status);
    		deployment.addFieldValue("entitiesStatus", status);
    		IEntity event = newDummyEntity(baseName,branchId, TypeEnum.DeploymentEvent,index);
    		//resultList.add(event);
    		deployment.addFieldValue("events", event);
    		IEntity target = newDummyEntity(baseName,branchId, TypeEnum.DeploymentTarget,index);
    		//resultList.add(target);
    		deployment.addFieldValue("targets", target);
    		target.addFieldValue("manifest", manifest);
    		
    		//deployment target has a reference to an embed document manifest version
    		BsonEntity manifestVersion = newDummyEntity(baseName,branchId, TypeEnum.ManifestVersion, 0);
    		target.addFieldValue("manifestVersion", manifestVersion);
    	}
    	//deployment should be persisted after manifest
    	
    	resultList.add(deployment);

    	return resultList;
    }
    
    public static void loadDeploymentBundle(DBCollectionPolicy collectionPolicy){

   		String baseName = "Bundle-1";
    	List<BsonEntity> resultList = generateDeploymentBundle(baseName, DalTestCons.MAIN_BRANCH);
    	logger.info(String.format("Bundle %s has %d entities", baseName, resultList.size()));
    	TestDataLoader deploymentLoader = TestDataLoader.getDataLoader(dataSource, DEPLOYMENT);
    	deploymentLoader.load(resultList, collectionPolicy);

    }
    
}
