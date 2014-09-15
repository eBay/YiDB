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

package com.ebay.cloud.cms.expression.entity;

import java.util.List;

import com.ebay.cloud.cms.expression.IExpressionContext;

public class SampleEntityExpressionContext implements IExpressionContext {

    protected SampleEntity entity;
    public SampleEntityExpressionContext(SampleEntity entity) {
        this.entity = entity;
    }
    
    @Override
    public boolean containsParameter(String name) {
        String fieldName = extractEntityField(name);
        if (fieldName != null) {
            return entity.hasField(fieldName);
        }
        return false;
    }

    @Override
    public Object getParamter(String name) {
        String fieldName = extractEntityField(name);
        if (fieldName != null) {
            List<?> values = entity.getFieldValues(fieldName);
            if (isCardinalityMany(name)) {
                return values.toArray();
            } else {
                return values.get(0);
            }
        }
        return null;
    }
    
    protected String extractEntityField(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        if (fieldName.startsWith("_")) {
            return fieldName.substring(1);
        }
        return fieldName;
    }
    
    protected boolean isCardinalityMany(String fieldName) {
        if (fieldName.startsWith("_")) {
            return true;
        }
        return false;
    }

}
