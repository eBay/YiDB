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

package com.ebay.cloud.cms.dal.entity.datahandler;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.model.MetaField;

public abstract class AbstractDataTypeHandler implements IDataTypeHandler {

    @Override
    public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
        if (valueNode != null) {
            validate(curEntity, valueNode, metaField);
        }
        return valueNode;
    }
    
    @Override
    public Object write(IEntity curEntity, Object value, MetaField metaField) {
        if (value != null) {
            validate(curEntity, value, metaField);
        }
        return value;
    }

    protected abstract void validate(IEntity curEntity, Object value, MetaField metaField) throws IllegalArgumentException;
}
