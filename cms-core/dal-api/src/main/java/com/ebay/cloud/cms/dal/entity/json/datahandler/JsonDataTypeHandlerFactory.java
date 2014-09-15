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

import java.util.EnumMap;

import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

public class JsonDataTypeHandlerFactory {
    
    private JsonDataTypeHandlerFactory() {
        
    }
    
	private static EnumMap<DataTypeEnum, IDataTypeHandler> map; 
	
	static{
		map = new EnumMap<DataTypeEnum,IDataTypeHandler>(DataTypeEnum.class);
		map.put(DataTypeEnum.BOOLEAN, new JsonBooleanHandler());
		map.put(DataTypeEnum.DATE, new JsonDateHandler());
		map.put(DataTypeEnum.ENUM, new JsonEnumHandler());
		map.put(DataTypeEnum.INTEGER, new JsonIntegerHandler());
		map.put(DataTypeEnum.LONG, new JsonLongHandler());
		map.put(DataTypeEnum.DOUBLE, new JsonDoubleHandler());
		map.put(DataTypeEnum.STRING, new JsonStringHandler());
		map.put(DataTypeEnum.RELATIONSHIP, new JsonReferenceHandler());
		map.put(DataTypeEnum.JSON, new JsonHandler());
	}

	public static IDataTypeHandler getHandler(DataTypeEnum dataType){
		return map.get(dataType);
	}
}
