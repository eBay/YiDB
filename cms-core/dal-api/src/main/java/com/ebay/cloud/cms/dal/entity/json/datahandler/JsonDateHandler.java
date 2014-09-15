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

import java.util.Date;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.DateTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * 
 * @author jianxu1
 * @date 2012/5/14
 * 
 * @history
 * 2012/5/22 change internal representation of Date from String to Long
 * The initial version use String as internal representation of Date (leverage SimpleDateFormat)
 * The problem is: when we do serialize, the precision is seconds, introduce error of mili-seconds
 * so when we do deserialize, Date.equals operation fails because of the error in mili-seconds
 * 
 * now I change the internal representation (say in mongo) from String to Long.
 * 
 * 2012/5/30 After code review with Liu Bo, Mongo support Date directly, so writeBson and readBson
 * directly persist Date
 * 
 */

public class JsonDateHandler extends DateTypeHandler {

	@Override
	public Object read(IEntity curEntity, Object valueNode, MetaField metaField) {
	    CheckConditions.checkNotNull(metaField);
	    CheckConditions.checkNotNull(valueNode);
		JsonNode jsonNode = (JsonNode)valueNode;
		if (jsonNode.isNull()) {
		    return null;
		}

		CheckConditions.checkArgument(jsonNode.isIntegralNumber(), "Field '%s' should be date. But the value is %s", 
		        metaField.getName(), jsonNode);
		long time = jsonNode.getLongValue();
		Date date = new Date();
		date.setTime(time);
		return date;
	}

	@Override
	public JsonNode write(IEntity currEntity, Object value, MetaField metaField) {
		if (value == null) {
		    return NullNode.getInstance();
        }
        validate(currEntity, value, metaField);

		long time = ((Date)value).getTime();
		return JsonNodeFactory.instance.numberNode(time);
	}



}
