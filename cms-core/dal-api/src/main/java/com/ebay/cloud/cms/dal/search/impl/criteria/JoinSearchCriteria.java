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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define the join criteria that will search some ObjectIDs against the _id or reference field
 * 
 * @author xjiang
 * 
 */
public final class JoinSearchCriteria implements SearchCriteria {

    private final AbstractSearchField searchField;
    private final Collection<String> idList;

    public JoinSearchCriteria(final MetaField metaField, final Collection<String> ids, final ISearchStrategy strategy) {
        CheckConditions.checkArgument(ids != null && !ids.isEmpty(), "join id is empty!");
        this.searchField = new SelectionField(metaField, strategy);
        this.idList = ids;
    }
    
    public ISearchField getSearchField() {
        return searchField;
    }
    
    @Override
    @SuppressWarnings({ "unchecked" })
    public List<SearchCriteria> getChildren() {
        return Collections.EMPTY_LIST;
    }
    
    @Override
    public DBObject translate(final List<DBObject> subQueryList) {
        DBObject query = new BasicDBObject();
        if (idList.size() == 1) {
        	query.put(searchField.getFullDbName(), idList.iterator().next());
        } else {
            List<Object> oidList = new ArrayList<Object>(idList.size());
            for (String id : idList) {
                oidList.add(id);
            }
            DBObject inQry = new BasicDBObject("$in", oidList);
            query.put(searchField.getFullDbName(), inQry);
        }
        return query;
    }

    @Override
    public boolean evaluate(final IEntity entity) {
        return true;
    }

    @Override
    public String toString() {
        return "JoinSearchCriteria [joinField=" + searchField + ", idList="
                + idList + "]";
    }



}
