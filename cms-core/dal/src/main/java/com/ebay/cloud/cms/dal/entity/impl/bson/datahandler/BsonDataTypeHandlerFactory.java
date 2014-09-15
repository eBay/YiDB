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

package com.ebay.cloud.cms.dal.entity.impl.bson.datahandler;

import java.util.EnumMap;

import com.ebay.cloud.cms.dal.entity.datahandler.BooleanTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.DateTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.DoubleTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.EnumTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.IntegerTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.JsonTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.LongTypeHandler;
import com.ebay.cloud.cms.dal.entity.datahandler.StringTypeHandler;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

public class BsonDataTypeHandlerFactory {
	
	private static EnumMap<DataTypeEnum, IDataTypeHandler> map;
	
	static{
		map = new EnumMap<DataTypeEnum,IDataTypeHandler>(DataTypeEnum.class);
		map.put(DataTypeEnum.BOOLEAN, new BooleanTypeHandler());
        map.put(DataTypeEnum.DATE, new DateTypeHandler());
		map.put(DataTypeEnum.ENUM, new EnumTypeHandler());
		map.put(DataTypeEnum.INTEGER, new IntegerTypeHandler());
		map.put(DataTypeEnum.LONG, new LongTypeHandler());
		map.put(DataTypeEnum.DOUBLE, new DoubleTypeHandler());
		map.put(DataTypeEnum.STRING, new StringTypeHandler());
		map.put(DataTypeEnum.RELATIONSHIP, new ReferenceTypeHandler());
		map.put(DataTypeEnum.JSON, new JsonTypeHandler());
	}

	public static IDataTypeHandler getHandler(DataTypeEnum dataType){
		return map.get(dataType);
	}
}
