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

package com.ebay.cloud.cms.entmgr.entity.impl;

import org.apache.commons.lang.ObjectUtils;

import com.ebay.cloud.cms.dal.entity.IEntity;

/**
 * Interface for entity merger between user input and db values.
 * 
 * @author liasu
 *
 */
public abstract class AbstractEntityFieldMerger {
    
    public abstract boolean mergeEntityOnField(IEntity givenEntity, String fieldName, IEntity foundEntity);
    
    protected boolean compareFieldSingleValue(boolean isRelation, Object found, Object given) {
        if (isRelation) {
            String foundId = ((IEntity) found).getId();
            String givenId = ((IEntity) given).getId();
            return ObjectUtils.equals(foundId, givenId);
        } else {
            return ObjectUtils.equals(found, given);
        }
    }
}
