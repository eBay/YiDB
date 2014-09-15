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

package com.ebay.cloud.cms.expression.impl;

import javax.script.SimpleBindings;

import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;

/**
 * 
 * @author xjiang
 * 
 */
public class ExpressionContextBindings extends SimpleBindings {

    private final IExpressionContext context;

    public ExpressionContextBindings(IExpressionContext context) {
        this.context = context;
    }

    @Override
    public boolean containsKey(Object key) {
        String param = extractParameter(key);
        if (param != null) {
            if (context.containsParameter(param)) {
                return true;
            } else {
                throw new ExpressionParseException("Unknow parameter: " + param);
            }
        }
        return super.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        String param = extractParameter(key);
        if (param != null) {
            return context.getParamter(param);
        }
        return super.get(key);
    }

    private final String extractParameter(Object name) {
        if ((name instanceof String) && ((String) name).startsWith(IExpressionContext.PARAM_PREFIX)) {
            return ((String) name).substring(1);
        }
        return null;
    }
}
