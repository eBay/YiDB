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

import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;


/**
 * factory class for IFieldCriteriaHandler
 * 
 * @author xjiang
 *
 */
public final class FieldCriteriaHandlerFactory {

    private final static Map<FieldOperatorEnum, IFieldCriteriaHandler> handlers;
    
    static {
        handlers = new HashMap<FieldOperatorEnum, IFieldCriteriaHandler>();
        handlers.put(FieldOperatorEnum.EQ, new EqualityCriteriaHandler());
        handlers.put(FieldOperatorEnum.NE, new EqualityCriteriaHandler());
        handlers.put(FieldOperatorEnum.IN, new InSetCriteriaHandler());
        handlers.put(FieldOperatorEnum.NIN, new InSetCriteriaHandler());
        handlers.put(FieldOperatorEnum.GT, new ComparisonCriteriaHandler());
        handlers.put(FieldOperatorEnum.GE, new ComparisonCriteriaHandler());
        handlers.put(FieldOperatorEnum.LT, new ComparisonCriteriaHandler());
        handlers.put(FieldOperatorEnum.LE, new ComparisonCriteriaHandler());
        handlers.put(FieldOperatorEnum.CONTAINS, new ExistCriteriaHandler());
        handlers.put(FieldOperatorEnum.NCONTAINS, new ExistCriteriaHandler());
        handlers.put(FieldOperatorEnum.REGEX, new RegExpCriteriaHandler());
        handlers.put(FieldOperatorEnum.NREGEX, new RegExpCriteriaHandler());
        handlers.put(FieldOperatorEnum.ISEMPTY, new IsEmptyCriteriaHandler());
        handlers.put(FieldOperatorEnum.NISEMPTY, new IsEmptyCriteriaHandler());
        handlers.put(FieldOperatorEnum.ISNULL, new IsNullCriteriaHandler());
        handlers.put(FieldOperatorEnum.NISNULL, new IsNullCriteriaHandler());
    }
    
    /**
     * Factory method to create IFieldCriteriaHandler
     * 
     * @param op
     * @return
     */
    public static IFieldCriteriaHandler getHandler(FieldOperatorEnum op) {
        return handlers.get(op);
    }
        
}
