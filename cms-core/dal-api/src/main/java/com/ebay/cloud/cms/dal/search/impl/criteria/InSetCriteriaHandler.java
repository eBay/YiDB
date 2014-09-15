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

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * in set criteria handler 
 * 
 * Notes: 
 * 
 * @author xjiang
 *
 */
public class InSetCriteriaHandler extends AbstractFieldCriteriaHandler {
    @Override
    public DBObject translate(final FieldSearchCriteria criteria) { 
        DBObject query = new BasicDBObject();
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case IN:
                buildCriteria(criteria, query, new BasicDBObject("$in", criteria.getValueList()));
                break;
            case NIN:
                buildCriteria(criteria, query, new BasicDBObject("$nin", criteria.getValueList()));
                // add exists=true for array
                query = buildNegativeArrayCriteria(criteria, query);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
        return query;
    }
    @Override
    public boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        List<Object> criteriaValueList = criteria.getValueList();
        boolean evalRes = false;
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case IN:
                evalRes = evalIn(fieldValues, criteriaValueList);
                break;
            case NIN:
                evalRes = evalNotIn(fieldValues, criteriaValueList);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }            
        return evalRes;
    }

    // FIXME : duplciate code clean.
    private boolean evalNotIn(List<?> fieldValues, List<Object> criteriaValueList) {
        if (fieldValues.isEmpty()) {
            // if has empy value, NOT evaluates return true
            return true;
        }
        boolean hasNotMatch = false;
        for (Object fieldValue : fieldValues) {
            if (!criteriaValueList.contains(fieldValue)) {
                hasNotMatch = true;
                break;
            }
        }
        return hasNotMatch;
    }

    private boolean evalIn(final List<?> fieldValues, final List<Object> criteriaValueList) {
        if (fieldValues.isEmpty()) {
            // if has empy value, NOT evaluates return true
            return false;
        }
        boolean hasMatch = false;
        for (Object fieldValue : fieldValues) {
            if (criteriaValueList.contains(fieldValue)) {
                hasMatch = true;
                break;
            }
        }
        return hasMatch;
    }

    @Override
    protected boolean useArrayElemMatch() {
        return true;
    }
}
