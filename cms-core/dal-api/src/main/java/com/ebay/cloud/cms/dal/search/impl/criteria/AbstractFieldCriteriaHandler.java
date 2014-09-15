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
import java.util.List;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public abstract class AbstractFieldCriteriaHandler implements IFieldCriteriaHandler {
    
    /**
     * Build query objects
     * 
     * @param criteria
     * @param query
     * @param atomQueryObject
     * @param checkFieldArray -- when the field on the criteria, decide to use $elemMatch or not. A couple of 
     */
    protected void buildCriteria(FieldSearchCriteria criteria, DBObject query, DBObject atomQueryObject) {
        String fullDbName = criteria.getFullDbName();
        ISearchField searchField = criteria.getSearchField();
        if (!(searchField instanceof AbstractSearchField)) {
            // GroupField : won't be array
            // AggregationField : won't be array
            query.put(fullDbName, atomQueryObject);
            return;
        }
        
        // AbstractSearchField
        AbstractSearchField asf = (AbstractSearchField) criteria.getSearchField();
        MetaField metaField = asf.getRootField();
        // inner property is an primitive type
        // embed handling : FIXME:: what about multiple layer of embedded(MANY)? We support only one layer of array for now
        String eleMatchName = asf.getRootFieldDbName();
        String innerField = asf.getInnerField();
        // 1. embed many
        if (criteria.isEmbeddedCardinalityMany() && criteria.isEmbeddedObject() && asf.getInnerProperty() == null) {
            // embed array
            String embedPath = asf.getEmbedPath();
            String parentPath = embedPath.substring(0, embedPath.length() - 1);
            String key = asf.getRootFieldDbName();
            if (useElemMatchName()) {
                key = asf.getRootFieldElemMatchDbName();  
            }
            query.put(parentPath, new BasicDBObject("$elemMatch", new BasicDBObject(key, atomQueryObject)));
            return;
        }

        // 2. for cardinality=MANY case
        if (useArrayElemMatch() && asf.getInnerProperty() == null && metaField.getCardinality() == CardinalityEnum.Many) {
            if (criteria.isEmbeddedObject()) {
                String embedPath = asf.getEmbedPath();
                eleMatchName = embedPath + asf.getRootFieldDbName();
            }
            // add $elemMatch query object
            if (innerField == null) {
                query.put(eleMatchName, new BasicDBObject("$elemMatch", atomQueryObject));
            } else {
                query.put(eleMatchName, new BasicDBObject("$elemMatch", new BasicDBObject(innerField, atomQueryObject)));
            }
            return;
        } 

        // 3. if not array handling, this is the default behavior
        query.put(fullDbName, atomQueryObject);
    }

    protected DBObject buildNegativeArrayCriteria(FieldSearchCriteria criteria, DBObject query) {
        String fullDbName = criteria.getFullDbName();
        ISearchField searchField = criteria.getSearchField();
        if (!(searchField instanceof AbstractSearchField)) {
            // GroupField : won't be array
            // AggregationField : won't be array
            return query;
        }

        AbstractSearchField asf = (AbstractSearchField) criteria.getSearchField();
        MetaField metaField = asf.getRootField();
        if (useArrayElemMatch() && asf.getInnerProperty() == null && metaField.getCardinality() == CardinalityEnum.Many) {
            // for array of not, $elemMatch will force the $exists=true.
            BasicDBObject or = new BasicDBObject();
            List<DBObject> ors = new ArrayList<DBObject>();
            // exists
            ors.add(new BasicDBObject(fullDbName, new BasicDBObject("$exists", false)));
            ors.add(new BasicDBObject(fullDbName, new BasicDBObject("$size", 0)));
            ors.add(query);
            or.put("$or", ors);
            return or;
        }
        return query;
    }
    
    protected abstract boolean useArrayElemMatch();
    
    protected boolean useElemMatchName() {
        return true;
    }
    
}
