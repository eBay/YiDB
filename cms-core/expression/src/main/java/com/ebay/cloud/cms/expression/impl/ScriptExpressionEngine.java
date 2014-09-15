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

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.exception.ExpressionEvaluateException;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;

/**
 * 
 * @author xjiang
 *
 */
public class ScriptExpressionEngine implements IExpressionEngine {

    private final static IExpressionEngine SINGLETON_ENGINE = new ScriptExpressionEngine();
    public static IExpressionEngine getInstance() {
        return SINGLETON_ENGINE;
    }
    
    private final Compilable compiler;
    private ScriptExpressionEngine() {
        compiler = (Compilable)new ScriptEngineManager().getEngineByName("javascript");
    }
    
    @Override
    public Object evaluate(IExpression expression, IExpressionContext context) {        
        try {
            CompiledScript script = ((ScriptExpression)expression).getCompiledExpression();
            Bindings bindings = new ExpressionContextBindings(context);
            return script.eval(bindings);
        } catch (ScriptException se) {
            Throwable cause = se.getCause();
            if (cause instanceof ExpressionParseException) {
                throw new ExpressionParseException(cause.getMessage() + "\n" + expression.getStringExpression(), se);
            } else {
                throw new ExpressionEvaluateException(expression.getStringExpression(), se);
            }
        }
    }

    @Override
    public IExpression compile(String source) {
        if (source == null || source.length() == 0) {
            throw new ExpressionParseException("expression can't be empty!");
        }
        try {
            CompiledScript jsScript = compiler.compile(source);
            return new ScriptExpression(jsScript, source);
        } catch(Throwable t) {
            throw new ExpressionParseException(source, t);
        }
    }
    
}
