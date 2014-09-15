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


/**
 * 
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

package com.ebay.cloud.cms.sysmgmt.healthy.expression;

import java.util.Map;

import com.ebay.cloud.cms.expression.IExpressionContext;

/**
 * @author liasu
 * 
 */
public class StatisticsExpressionContext implements IExpressionContext {

    private final Map<String, Object> statistics;

    public StatisticsExpressionContext(Map<String, Object> statistics) {
        this.statistics = statistics;
    }

    @Override
    public boolean containsParameter(String name) {
        if (statistics.keySet().contains(name)) {
            return true;
        }
        return false;
    }

    @Override
    public Object getParamter(String name) {
        return statistics.get(name);
    }

}
