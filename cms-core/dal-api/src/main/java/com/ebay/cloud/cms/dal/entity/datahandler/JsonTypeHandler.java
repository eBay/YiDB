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
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
/**
 * @date 2012/6/21
 * @author jianxu1
 *
 */
public class JsonTypeHandler implements IDataTypeHandler {
	
	@Override
    public Object write(IEntity curEntity, Object value, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
        if (value == null) {
            return null;
        }

        String jsonString = null;
        if (value instanceof IEntity) {
            jsonString = ((IEntity) value).toString();
        } else {
            jsonString = value.toString();
        }
        return (DBObject) JSON.parse(jsonString);
    }

    @Override
    public Object read(IEntity curEntity, Object value, MetaField metaField) {
        CheckConditions.checkNotNull(metaField);
        if (!(value instanceof DBObject)) {
            return null;
        }
        return value;
    }

}
