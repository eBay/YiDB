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
import com.ebay.cloud.cms.dal.entity.datahandler.IntegerTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * 
 * @author jianxu1
 * @date 2012/5/22
 * @history
 */
public class JsonIntegerHandler extends IntegerTypeHandler {

	@Override
	public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
	    CheckConditions.checkNotNull(valueNode, "Faild to read Integer from %s",metaField.getName());
		JsonNode jsonNode = (JsonNode)valueNode;
		if (jsonNode.isNull()) {
		    return null;
		}
		CheckConditions.checkArgument(jsonNode.isInt(), "Field '%s' should be integer. But the value is %s",
                metaField.getName(), jsonNode);
		return jsonNode.getIntValue();
	}

	@Override
	public JsonNode write(IEntity currEntity, Object value, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
		if (value == null) {
		    return NullNode.getInstance();
		}
        validate(currEntity, value, metaField);
        return JsonNodeFactory.instance.numberNode(((Number) value).intValue());
	}


}
