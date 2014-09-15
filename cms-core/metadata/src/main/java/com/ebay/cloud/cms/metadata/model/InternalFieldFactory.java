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

package com.ebay.cloud.cms.metadata.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;

/**
 * 
 * @author jianxu1
 * @created 2012/5/14
 * @history
 * 2012/5/18, add new internal field: _status
 * because by default, when we do "delete", we mark entity as deleted by setting _status=deleted
 *
 */
public class InternalFieldFactory {
	
	public static enum StatusEnum{
		ACTIVE("active"),
		DELETED("deleted");
		
		private String value;
		StatusEnum(String value){
			this.value = value;
		}
		@Override
		public String toString(){
			return value;
		}
	}
	
	private final static Map<String, MetaField> METAFIELD_MAP;
	private final static Map<String, InternalFieldEnum> FIELDENUM_MAP;
	
	static{
		METAFIELD_MAP = new HashMap<String,MetaField>();
		FIELDENUM_MAP = new HashMap<String, InternalFieldEnum>();
		for(InternalFieldEnum internal: InternalFieldEnum.values()){
			MetaAttribute metaField = new MetaAttribute(true);
			metaField.setCardinality(CardinalityEnum.One);
			metaField.setDataType(internal.getDataType());
			metaField.setName(internal.getName());
			metaField.setDbName(internal.getDbName());
			metaField.setMandatory(true); //all internal fields are mandatory
			
			METAFIELD_MAP.put(internal.getName(), metaField);
			FIELDENUM_MAP.put(internal.getName(), internal);
		}
		//set the enum values for _status
		MetaAttribute statusAttribute = (MetaAttribute)METAFIELD_MAP.get(InternalFieldEnum.STATUS.getName());
		List<String> statusValues = new ArrayList<String>();
		for(StatusEnum status: StatusEnum.values()){
			statusValues.add(status.toString());
		}
		statusAttribute.setEnumValues(statusValues);
	}
	
	public static Map<String,MetaField> getInternalMetaFields(){
		return METAFIELD_MAP;
	}

	public static MetaField getInternalMetaField(InternalFieldEnum name){
		return METAFIELD_MAP.get(name.getName());
	}
	
	public static InternalFieldEnum getInternalFieldEnum(String name) {
	    return FIELDENUM_MAP.get(name);
	}
}
