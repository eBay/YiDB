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

import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * array field is empty handler
 * 
 * @author zhihzhang
 *
 */
public class IsEmptyCriteriaHandler extends AbstractFieldCriteriaHandler {
    @Override
    public DBObject translate(final FieldSearchCriteria criteria) { 
        DBObject query = new BasicDBObject();
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case ISEMPTY:
                BasicDBObject emptyFilter = new BasicDBObject();
                emptyFilter.put("$size", 0);
                buildCriteria(criteria, query, emptyFilter);
                break;
            case NISEMPTY:
                BasicDBObject nEmptyFilter = new BasicDBObject();
                nEmptyFilter.put("$not", new BasicDBObject("$size", 0));
                nEmptyFilter.put("$exists", true);
                buildCriteria(criteria, query, nEmptyFilter);
                break;            
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
        return query;
    }
    
    @Override
    public boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        boolean evalRes = false;
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case ISEMPTY:
                evalRes = evalIsEmpty(fieldValues);
                break;
            case NISEMPTY:
                evalRes = evalNotEmpty(fieldValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }            
        return evalRes;
    }
    
    private boolean evalIsEmpty(final List<?> fieldValues) {
        // CMS-4791: the fieldValues==EMPTY_LIST if it does not exist in the entity
        if (Collections.EMPTY_LIST != fieldValues && fieldValues.isEmpty()) {
            return true;
        }
        return false;             
    }
    
    private boolean evalNotEmpty(final List<?> fieldValues) {
        if (Collections.EMPTY_LIST != fieldValues && !fieldValues.isEmpty()) {
            return true;
        }
        return false;                
    }

    @Override
    protected boolean useArrayElemMatch() {
        return false;
    }

    @Override
    protected boolean useElemMatchName() {
        return false;
    }
    
}
