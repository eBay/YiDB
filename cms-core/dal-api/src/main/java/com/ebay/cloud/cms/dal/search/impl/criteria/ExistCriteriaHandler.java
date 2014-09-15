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
 * field exist handler
 * 
 * @author xjiang
 *
 */
public class ExistCriteriaHandler extends AbstractFieldCriteriaHandler {

    @Override
    public DBObject translate(final FieldSearchCriteria criteria) {
        DBObject query = new BasicDBObject();
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case CONTAINS:
                buildCriteria(criteria, query, new BasicDBObject("$exists", true));
                break;
            case NCONTAINS:
                buildCriteria(criteria, query, new BasicDBObject("$exists", false));
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
            case CONTAINS:
                evalRes = evalCONTAINS(fieldValues);
                break;
            case NCONTAINS:
                evalRes = !evalCONTAINS(fieldValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }            
        return evalRes;
    }
    
    private boolean evalCONTAINS(final List<?> fieldValues) {
        if (fieldValues == Collections.EMPTY_LIST) {
            return false;
        }
        return true;                
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
