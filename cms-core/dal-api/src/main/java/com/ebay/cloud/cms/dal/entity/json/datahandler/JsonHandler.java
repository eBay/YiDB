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

package com.ebay.cloud.cms.dal.entity.json.datahandler;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.POJONode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
/**
 * @date 2012/6/21
 * @author jianxu1
 *
 */
public class JsonHandler implements IDataTypeHandler {

    @Override
    public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
        if (valueNode == null) {
            return null;
        }
        String jsonString = valueNode.toString();
        Object result = JSON.parse(jsonString);
        if (result != null) {
            CheckConditions.checkArgument(result instanceof DBObject,
                "Field '%s' should be Object. But the value is '%s'",
                    metaField.getName(), result);
        }
        
        return (DBObject) result;
    }

    @Override
    public JsonNode write(IEntity currEntity, Object value, MetaField metaField) {
        if (value instanceof POJONode) {
            return (JsonNode) value;
        }
        return JsonNodeFactory.instance.POJONode(value);
    }

}
