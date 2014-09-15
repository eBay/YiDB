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

package com.ebay.cloud.cms.dalapi.common;

import java.util.ArrayList;
import java.util.List;

import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
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
    
    private static BsonEntity newCompute(int index){
		//Compute
		String computeName = String.format("compute-%05d", index);
		BsonEntity compute = help.newEntity(TypeEnum.Compute.name(),DalTestCons.MAIN_BRANCH, computeName);
		compute.addFieldValue("location", "lvs");
		compute.addFieldValue("label", computeName);
		if (index < 8) {
		    compute.addFieldValue("fqdn", computeName + ".lvs.ebay.com");
		}
		compute.addFieldValue("assetStatus", "normal");
		return compute;
    }
    
    private static BsonEntity newServiceInstance(String appSvcName, int index, BsonEntity compute){
    	String instName = String.format("%s-%05d", appSvcName, index);
    	BsonEntity instance = help.newEntity(TypeEnum.ServiceInstance.name(), DalTestCons.MAIN_BRANCH, instName);
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
    

    
    public static List<BsonEntity> generateRaptorTopology(COSEnum cos,
    												  String envName,    												  
    												  int serviceCnt
    												  ){
    	List<BsonEntity> result = new ArrayList<BsonEntity>();
    	
      	//Update Strategy
    	BsonEntity updateStrategy1 = help.newEntityWithDummyValues(TypeEnum.UpdateStrategy.name(),DalTestCons.MAIN_BRANCH,"1-25-50-100");
    	result.add(updateStrategy1);
    	
    	BsonEntity updateStrategy2 = help.newEntityWithDummyValues(TypeEnum.UpdateStrategy.name(),DalTestCons.MAIN_BRANCH,"1-100");
    	result.add(updateStrategy2);
    	
    	//AppSvc
    	String appSvcName = "srp-app:" + envName;
    	BsonEntity appSvc = help.newEntityWithDummyValues(TypeEnum.ApplicationService.name(),DalTestCons.MAIN_BRANCH, appSvcName);
    	
    	//add reference
    	appSvc.addFieldValue("updateStrategies", updateStrategy1);
    	appSvc.addFieldValue("updateStrategies", updateStrategy2);
    	
    	for(int index=0; index<serviceCnt; index++){
    		BsonEntity compute = newCompute(index+1);
    		result.add(compute);
    		
    		BsonEntity instance = newServiceInstance(appSvcName,index+1,compute);
    		result.add(instance);
    		appSvc.addFieldValue("services", instance);
    	}
    	
    	result.add(appSvc);

    	
    	//COS
    	BsonEntity cosInst = help.newEntityWithDummyValues(TypeEnum.ClassOfService.name(),DalTestCons.MAIN_BRANCH,cos.name());
    	result.add(cosInst);
    	
    	//Environment
    	BsonEntity envInst = help.newEntityWithDummyValues(TypeEnum.Environment.name(),DalTestCons.MAIN_BRANCH,"EnvRaptor");
    	result.add(envInst);
    	
    	envInst.addFieldValue("cos", cosInst);
    	envInst.addFieldValue("applications", appSvc);
    	
    	return result;
    }

    public static void loadRaptorTopology(DBCollectionPolicy collectionPolicy){
    	List<BsonEntity> resultList = generateRaptorTopology(COSEnum.Production,"srp-app",10);
    	TestDataLoader raptorLoader = TestDataLoader.getDataLoader(dataSource, RAPTOR);
    	raptorLoader.load(resultList, collectionPolicy);
    }

    public static void main(String[] args) {
        MongoDataSource ds = new MongoDataSource("localhost");
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();
        TestDataLoader.getDataLoader(ds, RAPTOR).cleanUp();

        long temp = System.currentTimeMillis();
        List<BsonEntity> resultList = RaptorEntityGenerator.generateRaptorTopology(COSEnum.QA, "srp-app" + temp, 3);
        TestDataLoader raptorLoader = TestDataLoader.getDataLoader(new MongoDataSource("localhost:27017"), RAPTOR);
        raptorLoader.load(resultList, DBCollectionPolicy.SplitByMetadata);
        
        System.out.println("abc");

    }

}
