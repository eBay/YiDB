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

package com.ebay.cloud.cms.query.util;

import java.util.List;

import org.junit.Test;

import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.mongo.MongoDataSource;

public class TestUtils {

    public static List<Registration> getTestDalImplemantation(MongoDataSource dataSource) {
        return RegistrationUtils.getTestDalImplemantation(dataSource);
    }

	public static Registration getDefaultDalImplementation(MongoDataSource dataSource) {
		return RegistrationUtils.getDefaultDalImplementation(dataSource);
	}
	
    @Test
    public void testAll(){
        System.out.println(123);
    }

}
