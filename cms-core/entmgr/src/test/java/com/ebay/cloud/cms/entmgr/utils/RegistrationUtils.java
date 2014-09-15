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

import java.util.ArrayList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.EntityIDHelper;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.CollectionFinder;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewCollectionFinder;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalEntityFactory;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewDalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.flatten.impl.NewPersistenceServiceImpl;
import com.ebay.cloud.cms.dal.persistence.impl.DalEntityFactory;
import com.ebay.cloud.cms.dal.persistence.impl.DalSearchStrategy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.mongo.MongoDataSource;

public class RegistrationUtils {

	private static List<PersistenceService.Registration> implementations;

	public static List<Registration> getTestDalImplemantation(
			MongoDataSource dataSource) {
		if (implementations == null) {
			Registration reg = new Registration("hierarchy",
					new PersistenceServiceImpl(dataSource), BsonEntity.class,
					DalEntityFactory.getInstance(),
					DalSearchStrategy.getInstance(),
					EntityIDHelper.getInstance(), new CollectionFinder());

			Registration fReg = new Registration("flatten",
					new NewPersistenceServiceImpl(dataSource),
					NewBsonEntity.class, NewDalEntityFactory.getInstance(),
					NewDalSearchStrategy.getInstance(),
					FlattenEntityIDHelper.getInstance(),
					new NewCollectionFinder());

			implementations = new ArrayList<PersistenceService.Registration>();
			implementations.add(reg);
			implementations.add(fReg);
		}
		return implementations;
	}
	
	public static Registration getDefaultDalImplementation(MongoDataSource dataSource) {
		if (implementations == null) {
			getTestDalImplemantation(dataSource);
		}
		return implementations.get(0);
	}

}
