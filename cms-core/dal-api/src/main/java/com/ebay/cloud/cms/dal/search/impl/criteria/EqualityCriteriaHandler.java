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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * equality criteria handler 
 * 
 * @author xjiang
 *
 */
public class EqualityCriteriaHandler extends AbstractFieldCriteriaHandler {
    @Override
    public DBObject translate(final FieldSearchCriteria criteria) { 
        DBObject query = new BasicDBObject();
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case EQ:
//                // FIXME:: Wrapp the dbo, since there might be $elemMatch outside. There no $eq operator, use $in instead.
//                DBObject dbo = new BasicDBObject("$in", Arrays.asList(criteria.getValue()));
//                buildCriteria(criteria, query, dbo);
                query.put(criteria.getFullDbName(), criteria.getValue());
                break;
            case NE:
                // wrapper elemMatch if any
                buildCriteria(criteria, query, new BasicDBObject("$ne", criteria.getValue()));
                // add exists=false for array
                query = buildNegativeArrayCriteria(criteria, query);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
        return query;
    }

    @Override
    public boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        Object criteriaValue = criteria.getValue();
        boolean evalRes = false;
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case EQ:
                evalRes = evalEq(fieldValues, criteriaValue);
                break;
            case NE:
                evalRes = evalNotEq(fieldValues, criteriaValue);  
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }            
        return evalRes;
    }

    private boolean evalEq(final List<?> fieldValues, final Object criteriaValue) {
        Predicate eqPre = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if (criteriaValue instanceof Long && object instanceof Integer) {
                    Long value = Long.valueOf((Integer) object);
                    return criteriaValue.equals(value);
                } else {
                    return criteriaValue.equals(object);
                }
            }
        };
        return CollectionUtils.exists(fieldValues, eqPre);
    }
    
    private boolean evalNotEq(final List<?> fieldValues, final Object criteriaValue) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return true;
        }
        
        Predicate notEqPre = new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                if (criteriaValue instanceof Long && object instanceof Integer) {
                    Long value = Long.valueOf((Integer) object);
                    return !criteriaValue.equals(value);
                } else {
                    return !criteriaValue.equals(object);
                }
            }
        };
        return CollectionUtils.exists(fieldValues, notEqPre);
    }

    @Override
    protected boolean useArrayElemMatch() {
        return true;
    }
}
