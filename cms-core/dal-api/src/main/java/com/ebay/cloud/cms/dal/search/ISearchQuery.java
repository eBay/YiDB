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

package com.ebay.cloud.cms.dal.search;

import java.util.List;

import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.DBObject;

/**
 * interface of search query
 * 
 * @author xjiang
 * 
 */
public interface ISearchQuery {

    public final static class MongoQuery {
        public DBObject match      = null;
        public DBObject project    = null;
        public List<DBObject> preGroupUnwinds = null;
        // TODO : enhancements might be adopted by project before further aggregate pipeline 
        public DBObject preGroupProject = null;
        public DBObject group      = null;
        public DBObject groupMatch = null;
        @Override
        public String toString() {
            return new StringBuilder().append("match:").append(match).append(",\nproject:").append(project)
                    .append(",\n preGroupUnwind:").append(preGroupUnwinds)
                    .append(",\ngroup:").append(group).append(",\ngroupMatch:").append(groupMatch).toString();
        }
    }

    MetaClass getMetadata();

    MongoQuery buildMongoQuery(PersistenceContext context);
    
    void appendSearchCriteria(SearchCriteria criteria);

}
