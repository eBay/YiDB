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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleEntity {

    protected Map<String, List<Object>> fields = new HashMap<String, List<Object>>();
    
    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }
    
    public List<Object> getFieldValues(String fieldName) {
        return fields.get(fieldName);
    }
    
    public void setFieldValues(String fieldName, List<Object> value) {
        fields.put(fieldName, value);
    }
    
    public void addFieldValue(String fieldName, Object value) {
        List<Object> values = fields.get(fieldName);
        if (values == null) {
            values = new ArrayList<Object>();
            fields.put(fieldName, values);
        }
        values.add(value);
    }
}
