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
import com.ebay.cloud.cms.utils.CheckConditions;

public class BooleanTypeHandler extends AbstractDataTypeHandler {

    /**
     * Read from bson doesn't throw exception. Treat in-compatible value as
     * null(and be ignored).
     */
    @Override
    public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
        Object value = valueNode;
        if (!(value instanceof Boolean)) {
            value = null;
        }
        return value;
    }

    @Override
    protected void validate(IEntity curEntity, Object value, MetaField metaField) throws IllegalArgumentException {
        CheckConditions.checkArgument(value instanceof Boolean, "%s should have input value as boolean",
                metaField.getName());
    }

}
