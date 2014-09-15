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
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.CheckConditions;
/**
 * 
 * @author jianxu1
 * @date 2012/5/14
 * 
 * @history
 * 2012/5/23 FIX writeJson can not expect input field value is always "String"
 * when dealing with embed reference field, the input field to writeJson is JsonNode representation of embed document.
 * so I removed the strict "instanceof string" checking
 * 
 * 2012/5/24 FIX writeBson, same reason as fix on 2012/5/23: writeBson can not expect input field value is always "String"
 * Also, for non embed reference, no matter we reference to root document or embed document, the reference id
 * will be persisted as String, instead of (ObjectId for referenced root document, String for referenced embed reference id) 
 * 
 * 2012/5/24 TODO: let Handler implementation has a common place to do argument checking!
 * 
 * 2012/5/25 How to support Mapping entity of join results to Json
 */
public class JsonReferenceHandler implements IDataTypeHandler {

	@Override
	public IEntity read(IEntity curEntity, Object value, MetaField metaField){
	    CheckConditions.checkNotNull(metaField);
	    CheckConditions.checkArgument(value instanceof ObjectNode,
		        "Field '%s' should be Object. But the value is %s",
					metaField.getName(), value.getClass().getCanonicalName());
		MetaRelationship metaRelation = (MetaRelationship)metaField;
		MetaClass refMetaClass = metaRelation.getRefMetaClass();
		
		ObjectNode objValue = (ObjectNode)value;
		JsonNode type = (JsonNode)objValue.get(InternalFieldEnum.TYPE.getName());
		if (type != null) {
		    MetaClass parent = refMetaClass;
		    refMetaClass = refMetaClass.getMetadataService().getMetaClass(type.getTextValue());
		    CheckConditions.checkArgument(parent.isAssignableFrom(refMetaClass),
                    "Meta relationship ref meta %s couldn't be add a instance of %s", parent.getName(),
                    type.getTextValue());
        }
		
		return new JsonEntity(refMetaClass, objValue);
	}
	
	@Override
	public JsonNode write(IEntity curEntity, Object value, MetaField metaField){
	    CheckConditions.checkArgument(value instanceof IEntity, "Expect IEntity for field %s in type %s, but is %s instead",
				metaField.getName(), curEntity.getType(), value == null ? null : value.getClass().getCanonicalName());
		return ((JsonEntity)value).getNode();
	}

}
