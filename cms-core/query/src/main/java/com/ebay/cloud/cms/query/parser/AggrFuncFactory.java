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

package com.ebay.cloud.cms.query.parser;

import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryLexer;

/**
 * Factory class to translate anltr aggregation function to search aggregation function 
 * 
 * @author xjiang
 *
 */
public class AggrFuncFactory {
    private static final  Map<Integer, AggregationField.AggFuncEnum> AGG_FUNC_MAP;
    static {
        AGG_FUNC_MAP = new HashMap<Integer, AggregationField.AggFuncEnum>();
        AGG_FUNC_MAP.put(CMSQueryLexer.MIN, AggFuncEnum.MIN);
        AGG_FUNC_MAP.put(CMSQueryLexer.MAX, AggFuncEnum.MAX);
        AGG_FUNC_MAP.put(CMSQueryLexer.SUM, AggFuncEnum.SUM);
        AGG_FUNC_MAP.put(CMSQueryLexer.AVG, AggFuncEnum.AVG);
        AGG_FUNC_MAP.put(CMSQueryLexer.COUNT, AggFuncEnum.COUNT);
    }
    
    public static AggFuncEnum getAggrFunc(Integer func) {
        return AGG_FUNC_MAP.get(func);
    }
}
