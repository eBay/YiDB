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
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Comparison criteria handler 
 * 
 * @author xjiang
 *
 */
public class ComparisonCriteriaHandler extends AbstractFieldCriteriaHandler {

    @Override
    public DBObject translate(final FieldSearchCriteria criteria) { 
        DBObject query = new BasicDBObject();
        Object criteriaValue = criteria.getValue();
        CheckConditions.checkArgument((criteriaValue instanceof Comparable), "value %s should be Comparable!", criteriaValue);
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case GT:
                buildCriteria(criteria, query, new BasicDBObject("$gt", criteriaValue));
                break;
            case LT:
                buildCriteria(criteria, query, new BasicDBObject("$lt", criteriaValue));
                break;
            case GE:
                buildCriteria(criteria, query, new BasicDBObject("$gte", criteriaValue));
                break;
            case LE:
                buildCriteria(criteria, query, new BasicDBObject("$lte", criteriaValue));
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
        return query;
    }

    @Override
    public boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        boolean evaluated = false;
        for (Object fieldValue : fieldValues) {
            evaluated = true;
            if (!evalComp(criteria, fieldValue))
                return false;
        }
        return evaluated;
    }

    private boolean evalComp(final FieldSearchCriteria criteria, final Object fieldValue) {
        Object criteriaValue = criteria.getValue();

        Object value = fieldValue;
        if (fieldValue == null) {
            // null value return false in comparison
            return false;
        }
        if (fieldValue instanceof Integer) {
        	value = Long.valueOf((Integer)fieldValue);
        }
        
        @SuppressWarnings("unchecked")        
        Comparable<Object> compVal = (Comparable<Object>)value;

        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case GT:
                return compVal.compareTo(criteriaValue) > 0;
            case LT:
                return compVal.compareTo(criteriaValue) < 0;                
            case GE:
                return compVal.compareTo(criteriaValue) >= 0;
            case LE:
                return compVal.compareTo(criteriaValue) <= 0;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
    }
    
    @Override
    protected boolean useArrayElemMatch() {
        return true;
    }
}
