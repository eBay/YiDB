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

package com.ebay.cloud.cms.query.executor;

import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * Marker class represent empty results
 * @author xjiang
 *
 */
public class EmptySearchCriteria implements SearchCriteria {
    
    public static final EmptySearchCriteria EMPTY_CRITERIA = new EmptySearchCriteria();
    
    private final DBObject impossibleQuery;
    
    private EmptySearchCriteria() {
        impossibleQuery = new BasicDBObject();
        impossibleQuery.put("1", "-1");
    }

    @Override
    public List<SearchCriteria> getChildren() {
    	// nothing
        return Collections.emptyList();
    }

    @Override
    public DBObject translate(List<DBObject> subQueryList) {
        return impossibleQuery;
    }

    @Override
    public boolean evaluate(IEntity entity) {
    	// nothing
        return false;
    }

    @Override
    public ISearchField getSearchField() {
        // nothing
        return null;
    }

}
