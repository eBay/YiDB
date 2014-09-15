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
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;

public class EnumTypeHandler extends AbstractDataTypeHandler {

    @Override
    public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
        return valueNode;
    }

    @Override
    public Object write(IEntity curEntity, Object value, MetaField metaField) {
        validate(curEntity, value, metaField);
        return value;
    }

    @Override
    protected void validate(IEntity curEntity, Object value, MetaField metaField) throws IllegalArgumentException {
        MetaAttribute metaAttribute = (MetaAttribute) metaField;
        // enum actually are strings
        CheckConditions.checkArgument(value instanceof String, "Invalid enum value %s in MetaAttribute %s", value,
                metaField.getName());

        CheckConditions.checkArgument(metaAttribute.getEnumValues().contains(value),
                "Invalid enum value %s in MetaAttribute %s", value, metaField.getName());
    }

}
