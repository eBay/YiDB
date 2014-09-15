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

package com.ebay.cloud.cms.dal.search.impl.criteria;

import java.util.List;

import com.mongodb.DBObject;

/**
 * main interface of field criteria handler that will translate mongo query & evaluate entity  
 * 
 * @author xjiang
 *
 */
public interface IFieldCriteriaHandler {
    /**
     * translate search criteria to mongodb query 
     * 
     * @param criteria
     * @return
     */
    DBObject translate(final FieldSearchCriteria criteria);
    
    /**
     * evaluate search criteria against entity 
     * 
     * @param criteria
     * @param entityField
     * @return 
     */
    boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues);
}
