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

import javax.script.CompiledScript;

import com.ebay.cloud.cms.expression.IExpression;

/**
 * 
 * @author xjiang
 *
 */
public class ScriptExpression implements IExpression {

    private final CompiledScript compiledExpression;
    private final String stringExpression;
    
    public ScriptExpression(CompiledScript jsScript, String source) {
        this.compiledExpression = jsScript;
        this.stringExpression = source;
    }

    @Override
    public CompiledScript getCompiledExpression() {
        return compiledExpression;
    }

    @Override
    public String getStringExpression() {
        return stringExpression;
    }

}
