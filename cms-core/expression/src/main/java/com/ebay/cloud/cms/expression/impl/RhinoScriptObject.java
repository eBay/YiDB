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

package com.ebay.cloud.cms.expression.impl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;

/**
 * @author liasu
 * 
 */
public class RhinoScriptObject extends ScriptableObject {

    private static final long        serialVersionUID = 8006432524799627791L;

    private transient IExpressionContext context;

    public RhinoScriptObject(IExpressionContext context) {
        this.context = context;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        String param = extractParameter(name);
        if (param != null) {
            if (context.containsParameter(param)) {
                return true;
            } else {
                throw new ExpressionParseException("Unknow parameter: " + param);
            }
        }
        return super.has(name, start);
    }

    @Override
    public Object get(String name, Scriptable start) {
        // detect parameter existence
        has(name, start);

        String param = extractParameter(name);
        if (param != null) {
            Object val = context.getParamter(param);
            return Context.javaToJS(val, start);
        }
        return super.get(name, start);
    }

    private final String extractParameter(String name) {
        if (name.startsWith(IExpressionContext.PARAM_PREFIX)) {
            return name.substring(1);
        }
        return null;
    }

    @Override
    public String getClassName() {
        return "RhinoScriptObject";
    }

}
