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
import org.codehaus.jackson.node.NullNode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.StringTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * 
 * @author jianxu1
 * @created 2012/5/12
 * @history
 * 2012/5/16 jianxu1
 * adding special handling logic for "_id", we store _id as string in Entity, as ObjectId in DBObject
 *
 */

public class JsonStringHandler extends StringTypeHandler {

	@Override
	public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
	    CheckConditions.checkNotNull(valueNode);
	    CheckConditions.checkNotNull(metaField);
		JsonNode jsonNode = (JsonNode)valueNode;
		if (jsonNode.isNull()) {
		    return null;
		}
		CheckConditions.checkArgument(jsonNode.isTextual(), "Field '%s' should be string. But the value is %s", metaField.getName(), jsonNode);
		return jsonNode.getTextValue();
	}

	@Override
	public JsonNode write(IEntity currEntity, Object value, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
        if (value == null) {
            return NullNode.getInstance();
        }
        validate(currEntity, value, metaField);
		return JsonNodeFactory.instance.textNode((String)value);
	}
}
