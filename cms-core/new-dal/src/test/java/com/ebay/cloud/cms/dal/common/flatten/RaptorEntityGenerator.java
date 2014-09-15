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

package com.ebay.cloud.cms.dal.common.flatten;

import java.util.ArrayList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;


/**
 * 
 * @author jianxu1
 * @date 2012/5/17
 * @history
 * 
 * 2012/5/17
 * load raptor meta data and generate all entity instances
 */

//TODO: need a generic way to construct runtime date based on loaded meta data
public class RaptorEntityGenerator {

    private static IMetadataService metaService = null;
    private static DummyEntity help = null;
    private static final String RAPTOR = "raptor-paas";
    
    private static MongoDataSource dataSource = null;
    
    
    private static IRepositoryService repositoryService = null;
    
    static {
		String collectionString = CMSMongoTest.getConnectionString();
		dataSource = new MongoDataSource(collectionString);      
        repositoryService  = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");        
        metaService = repositoryService.getRepository(RAPTOR).getMetadataService();
        help = new DummyEntity(metaService);
        
    }

    public static enum TypeEnum {
    	ApplicationService,
    	Environment,
    	ClassOfService,
    	UpdateStrategy,
    	ServiceInstance,
    	Cluster,
    	Compute,
    	Dep,
    	Team,
    	Person
    }
    
    public static enum COSEnum{
    	Production,
    	QA,
    	Infrastrusture,
    	PreProduction
    }
    
    private static NewBsonEntity newCompute(int index){
		//Compute
		String computeName = String.format("compute-%05d", index);
		NewBsonEntity compute = help.newEntity(TypeEnum.Compute.name(),DalTestCons.MAIN_BRANCH, computeName);
		compute.addFieldValue("location", "lvs");
		compute.addFieldValue("label", computeName);
		if (index < 8) {
		    compute.addFieldValue("fqdn", computeName + ".lvs.ebay.com");
		}
		compute.addFieldValue("assetStatus", "normal");
		return compute;
    }
    
    private static NewBsonEntity newServiceInstance(String appSvcName, int index, NewBsonEntity compute){
    	String instName = String.format("%s-%05d", appSvcName, index);
    	NewBsonEntity instance = help.newEntity(TypeEnum.ServiceInstance.name(), DalTestCons.MAIN_BRANCH, instName);
    	if (index % 3 == 0) {
    	    instance.addFieldValue("https", false);
    	} else {
    	    instance.addFieldValue("https", true);
    	}    	
    	instance.addFieldValue("port", "8080");
    	//reference map 
    	instance.addFieldValue("manifestRef", "manifest-1.0");
    	instance.addFieldValue("manifestRef", "manifest-2.0");
    	
    	instance.addFieldValue("activeManifestRef", "manifest-3.0");
    	if (index % 2 == 0) {
    	    instance.addFieldValue("activeManifestDiff", true);
    	    instance.addFieldValue("healthStatus", "up");
    	} else {
    	    instance.addFieldValue("activeManifestDiff", false);
            instance.addFieldValue("healthStatus", "down");
        }
    	//because entity compute does not have "_ref" field, IEntity.isRef returns false
    	instance.addFieldValue("runsOn", compute);
    	return instance;
    }
    

    
    public static List<NewBsonEntity> generateRaptorTopology(TestDataLoader raptorLoader, COSEnum cos,
    												  String envName,    												  
    												  int serviceCnt
    												  ){
        List<NewBsonEntity> persistResult = new ArrayList<NewBsonEntity>();
    	
      	//Update Strategy
    	NewBsonEntity updateStrategy1 = help.newEntityWithDummyValues(TypeEnum.UpdateStrategy.name(),DalTestCons.MAIN_BRANCH,"1-25-50-100");
    	save(raptorLoader, persistResult, updateStrategy1);
    	
    	NewBsonEntity updateStrategy2 = help.newEntityWithDummyValues(TypeEnum.UpdateStrategy.name(),DalTestCons.MAIN_BRANCH,"1-100");
    	save(raptorLoader, persistResult, updateStrategy2);
    	
    	//AppSvc
    	String appSvcName = "srp-app:" + envName;
    	NewBsonEntity appSvc = help.newEntityWithDummyValues(TypeEnum.ApplicationService.name(),DalTestCons.MAIN_BRANCH, appSvcName);
    	
    	//add reference
    	appSvc.addFieldValue("updateStrategies", updateStrategy1);
    	appSvc.addFieldValue("updateStrategies", updateStrategy2);
    	
        for (int index = 0; index < serviceCnt; index++) {
    		NewBsonEntity compute = newCompute(index+1);
    		save(raptorLoader, persistResult, compute);
    		
    		NewBsonEntity instance = newServiceInstance(appSvcName,index+1,compute);
    		save(raptorLoader, persistResult, instance);

    		appSvc.addFieldValue("services", instance);
    	}
    	
        save(raptorLoader, persistResult, appSvc);
    	
    	//COS
    	NewBsonEntity cosInst = help.newEntityWithDummyValues(TypeEnum.ClassOfService.name(),DalTestCons.MAIN_BRANCH,cos.name());
    	save(raptorLoader, persistResult, cosInst);
    	
    	//Environment
    	NewBsonEntity envInst = help.newEntityWithDummyValues(TypeEnum.Environment.name(),DalTestCons.MAIN_BRANCH,"EnvRaptor");
    	
    	envInst.addFieldValue("cos", cosInst);
    	envInst.addFieldValue("applications", appSvc);
    	save(raptorLoader, persistResult, envInst);
    	
    	return persistResult;
    }

    static void save(TestDataLoader raptorLoader, List<NewBsonEntity> persistResult, NewBsonEntity cosInst) {
        raptorLoader.persistBsonEntity(DBCollectionPolicy.SplitByMetadata, persistResult, cosInst);
    }

    public static void loadRaptorTopology(DBCollectionPolicy collectionPolicy){
        TestDataLoader raptorLoader = TestDataLoader.getDataLoader(dataSource, RAPTOR);
    	generateRaptorTopology(raptorLoader, COSEnum.Production,"srp-app",10);
    }

    public static void main(String[] args) {
        MongoDataSource ds = new MongoDataSource("localhost");
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();
        TestDataLoader.getDataLoader(ds, RAPTOR).cleanUp();

        long temp = System.currentTimeMillis();
        TestDataLoader raptorLoader = TestDataLoader.getDataLoader(new MongoDataSource("localhost:27017"), RAPTOR);
        generateRaptorTopology(raptorLoader, COSEnum.QA, "srp-app" + temp, 3);
    }

}
