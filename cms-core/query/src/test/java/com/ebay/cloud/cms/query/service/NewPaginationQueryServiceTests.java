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

package com.ebay.cloud.cms.query.service;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;

@RunWith(Suite.class)
@SuiteClasses({ EmbedQueryServiceTest.class, LeftQueryTest.class, QueryAggregationTest.class,
        QueryCrossRepositoryTest.class, QueryJsonFieldTest.class, QueryPaginationByIdTest.class,
        QueryServiceTest.class, ReverseQueryTest.class, SetQueryServiceTest.class,
        SubQueryTest.class })
public class NewPaginationQueryServiceTests {
    @BeforeClass
    public static void setUpQuerySuite() {
        QueryContext.setDefaultPaginationMode(PaginationMode.ID_BASED);
    }

    @AfterClass
    public static void tearDownQuerySuite() {
        QueryContext.setDefaultPaginationMode(PaginationMode.ID_BASED);
    }
}
