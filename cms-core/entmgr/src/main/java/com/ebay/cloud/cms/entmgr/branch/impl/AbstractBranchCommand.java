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

package com.ebay.cloud.cms.entmgr.branch.impl;

import java.util.Date;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.internal.BranchMetaClass;

/**
 * 
 * @author bliu4
 *
 */
public abstract class AbstractBranchCommand {
	
	public static IEntity toBson(IBranch branch, MetaClass branchMeta, PersistenceContext persistenceContext) {
	    IEntity bsonEntity = persistenceContext.getEntityFactory().createEntity(branchMeta);

        bsonEntity.setId(branch.getId());
        bsonEntity.addFieldValue(BranchMetaClass.IsMain, branch.isMainBranch());

        //leave rootid blank

        //internal fields
        bsonEntity.addFieldValue(InternalFieldEnum.PVERSION.getName(), IEntity.NO_VERSION);
        bsonEntity.setBranchId(BranchMetaClass.BRANCH_ID);
        bsonEntity.setLastModified(new Date());
        bsonEntity.setStatus(StatusEnum.ACTIVE);
        bsonEntity.setVersion(IEntity.START_VERSION);
        
        return bsonEntity;
    }

    public static Branch toBranch(IEntity bsonEntity) {
        Branch b = new Branch();
        
        b.setId(bsonEntity.getId());
        Boolean isMain = (Boolean)bsonEntity.getFieldValues(BranchMetaClass.IsMain).get(0);
        b.setMainBranch(isMain.booleanValue());
        
        b.setRepositoryName(bsonEntity.getRepositoryName());
        
        return b;
    }
}
