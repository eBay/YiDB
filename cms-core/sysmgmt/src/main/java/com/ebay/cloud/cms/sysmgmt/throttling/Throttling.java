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

package com.ebay.cloud.cms.sysmgmt.throttling;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.factory.ScriptEngineProvider;
import com.ebay.cloud.cms.sysmgmt.healthy.expression.StatisticsExpressionContext;

/**
 * A throttling expression should be a expression that use the system statistics.
 * Return a double value to indicates what priority of users are blocked.
 * 
 * @author zhuang1
 * 
 */
public class Throttling {
    private static final Logger     logger = LoggerFactory.getLogger(Throttling.class);

    private final IExpressionEngine enginee = ScriptEngineProvider.getEngine();
    private final IExpression       expression;

    public Throttling(String expression) {
        this.expression = enginee.compile(expression);
    }

    int getThrottlingLevel(Map<String, Object> systemStatistics) {
        if (systemStatistics == null) {
            logger.error("systemStatistics should not be null");
            return 0;
        }

        StatisticsExpressionContext context = new StatisticsExpressionContext(systemStatistics);
        try {
            Object val = enginee.evaluate(expression, context);
            if (val instanceof Number) {
                return ((Number)val).intValue();
            }
        } catch (Exception e) {
            logger.error("failed to eval throttling expression", e);
        }

        return 0;
    }

}
