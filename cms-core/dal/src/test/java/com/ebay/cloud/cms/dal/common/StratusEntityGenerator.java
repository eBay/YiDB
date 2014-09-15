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

import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class StratusEntityGenerator {

    private static final String MAIN = "main";
    private static final String STRATUS_CI = "stratus-ci";
    private static MongoDataSource dataSource = null;
    private static IRepositoryService repositoryService = null;
    private static IMetadataService metaService = null;

    public static enum TypeEnum {
        NetworkAddress
    }
    
    static {
        String collectionString = CMSMongoTest.getConnectionString();
        dataSource = new MongoDataSource(collectionString);
        repositoryService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        metaService = repositoryService.getRepository(STRATUS_CI).getMetadataService();
    }

    public static List<BsonEntity> generateStratusTopology() {
        List<BsonEntity> networkAddresses = loadNetworkAddress(20);
        return networkAddresses;
    }
    
    public static void loadStratusTopology(DBCollectionPolicy collectionPolicy){
        List<BsonEntity> resultList = generateStratusTopology();
        TestDataLoader raptorLoader = TestDataLoader.getDataLoader(dataSource, STRATUS_CI);
        raptorLoader.load(resultList, collectionPolicy);
    }

    private static List<BsonEntity> loadNetworkAddress(int count) {
        List<BsonEntity> result = new ArrayList<BsonEntity>();
        MetaClass metaClass = metaService.getMetaClass(TypeEnum.NetworkAddress.name());

        String IP_TEMPLATE = "10.249.66.%d";
        String HOSTNAME_TEMPLATE = "abcefd%d.corp.ebay.com";
        String MAC_TEMPLATE = "00:34:35:ae:%d";
        for (int i = 0; i < count; i++) {
            BsonEntity res = new BsonEntity(metaClass);
            res.setBranchId(MAIN);
            res.addFieldValue("ipaddress", String.format(IP_TEMPLATE, count));
            res.addFieldValue("hostname", String.format(HOSTNAME_TEMPLATE, count));
            res.addFieldValue("zone", "corp");
            res.addFieldValue("802Address", String.format(MAC_TEMPLATE, count));

            result.add(res);
        }
        return result;
    }
}
