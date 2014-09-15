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

import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define the logical criteria  of "and" & "or" operator
 * 
 * NOTE: mongodb don't fully support "not" operator
 * 
 * @author xjiang
 * 
 */
public class LogicalSearchCriteria implements SearchCriteria {

    public enum LogicOperatorEnum {
        OR, AND
    }

    private final LogicOperatorEnum operator;

    private final List<SearchCriteria> children;

    public LogicalSearchCriteria(final LogicOperatorEnum operator) {
        CheckConditions.checkNotNull(operator);
        this.operator = operator;
        this.children = new LinkedList<SearchCriteria>();
    }

    public void addChild(final SearchCriteria criteria) {
        if (criteria instanceof LogicalSearchCriteria
                && ((LogicalSearchCriteria) criteria).operator == operator) {
            this.children.addAll(criteria.getChildren());
        } else {
            this.children.add(criteria);
        }
    }

    public LogicOperatorEnum getOperator() {
        return operator;
    }
    
    @Override
    public ISearchField getSearchField() {
        return null;
    }
    
    @Override
    public List<SearchCriteria> getChildren() {
        return children;
    }

    @Override
    public DBObject translate(final List<DBObject> subQueryList) {
        DBObject query = new BasicDBObject();
        if (operator == LogicOperatorEnum.AND) {
            query.put("$and", subQueryList);
        } else {
            query.put("$or", subQueryList);
        }        
        return query;
    }

    @Override
    public boolean evaluate(final IEntity entity) {        
        for(SearchCriteria criteria : children) {
            boolean childEval = criteria.evaluate(entity);
            if (operator == LogicOperatorEnum.AND) {
                if (!childEval) {
                    return false;
                }
            } else {
                if (childEval) {
                    return true;
                }
            }
        }
        if (operator == LogicOperatorEnum.AND) {            
            return true;
        }
        return false;       
    }
    
}
