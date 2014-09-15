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

package com.ebay.cloud.cms.dalapi.search.utils;

import org.junit.Test;

import com.ebay.cloud.cms.dal.persistence.PersistenceContext.CollectionFinder;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dalapi.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dalapi.persistence.impl.DalEntityFactory;
import com.ebay.cloud.cms.dalapi.persistence.impl.DalSearchStrategy;
import com.ebay.cloud.cms.dalapi.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.mongo.MongoDataSource;

public class TestUtils {

    public static Registration getTestDalImplemantation(MongoDataSource dataSource) {
        Registration reg = new Registration("hierarchy", new PersistenceServiceImpl(dataSource), BsonEntity.class,
                DalEntityFactory.getInstance(), DalSearchStrategy.getInstance(), EntityIDHelper.getInstance(), new CollectionFinder());
        return reg;
    }
    
    public static ISearchStrategy getDalSearchStrategy() {
        return DalSearchStrategy.getInstance();
    }
    
    @Test
    public void testAll(){
        System.out.println(123);
    }
}
