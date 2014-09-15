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

package com.ebay.cloud.cms.expression.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.impl.RhinoExpressionEngine;

/**
 * @author liasu
 * 
 */
public class ScriptEngineProvider {

    private static final long JS_INTERVAL_DEFAULT = 180l;

    private static Map<Long, IExpressionEngine> cachedEnginee = new ConcurrentHashMap<Long, IExpressionEngine>();

    public static IExpressionEngine getEngine(Long jsExpressionTimeoutInSeconds) {
        if (cachedEnginee.get(jsExpressionTimeoutInSeconds) == null) {
            cachedEnginee.put(jsExpressionTimeoutInSeconds, new RhinoExpressionEngine(jsExpressionTimeoutInSeconds));
        }
        return cachedEnginee.get(jsExpressionTimeoutInSeconds);
    }

    public static IExpressionEngine getEngine() {
        if (cachedEnginee.get(180l) == null) {
            cachedEnginee.put(180l, new RhinoExpressionEngine(180l));
        }
        return cachedEnginee.get(JS_INTERVAL_DEFAULT);
    }

}
