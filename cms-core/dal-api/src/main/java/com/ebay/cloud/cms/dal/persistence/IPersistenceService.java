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

package com.ebay.cloud.cms.dal.persistence;

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.metadata.model.MetaClass;

public interface IPersistenceService {

	 String create(IEntity entity, PersistenceContext context);
	 
	 List<String> batchCreate(List<IEntity> entities, PersistenceContext context);
	 
	 void batchUpdate(List<IEntity> entities, PersistenceContext context);
	 
	 void batchDelete(List<IEntity> entities, PersistenceContext context);
	 
	 void   replace(IEntity entity,PersistenceContext context);
	 
	 void   modify(IEntity entity, PersistenceContext context);
	 
	 IEntity get(IEntity queryEntity, PersistenceContext context);
	 
	 void   delete(IEntity entity, PersistenceContext context);
	 
	 void ensureIndex(List<MetaClass> meta, PersistenceContext context, boolean onMainBranch);
	 
	 void markDeleted(IEntity deleteEntity, PersistenceContext context);

    /**
     * Adds field values to list field or json field.
     * 
     * EXPRESSION field not calculated
     * 
     * @param entity
     * @param fieldName
     * @param context
     */
	 void modifyField(IEntity entity, String fieldName, PersistenceContext context);

     void deleteField(IEntity entity, String fieldName, PersistenceContext context);

	 long count(MetaClass meta, List<String> refOids, String branchId,PersistenceContext context);
	 
    List<Registration> getRegistrations();
}
