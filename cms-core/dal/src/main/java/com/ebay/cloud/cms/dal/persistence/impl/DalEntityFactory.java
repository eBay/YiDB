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


/**
 * 
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

package com.ebay.cloud.cms.dal.persistence.impl;

import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.search.IEntityFactory;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.DBObject;

/**
 * @author liasu
 * 
 */
public class DalEntityFactory implements IEntityFactory<BsonEntity> {
    private static final DalEntityFactory INSTANCE = new DalEntityFactory();

    private DalEntityFactory() {
    }

    public static DalEntityFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public BsonEntity createEntity(MetaClass metaClass) {
        return new BsonEntity(metaClass);
    }

    @Override
    public BsonEntity createEntity(MetaClass metaClass, Object underlying) {
        return new BsonEntity(metaClass, (DBObject) underlying);
    }

}
