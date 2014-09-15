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

package com.ebay.cloud.cms.dalapi.common;

import java.util.Date;
import java.util.List;
import java.util.Random;

import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.CheckConditions;

public class DummyEntity {

	private IMetadataService metaService = null;
	public DummyEntity(IMetadataService metaService){
		this.metaService = metaService;
	}
	
	public MetaClass newClass(String repoName,
									 String metaType){
		MetaClass metaClass = new MetaClass();
		metaClass.setRepository(repoName);
		metaClass.setName(metaType);
		metaService.createMetaClass(metaClass, new MetadataContext());
		return metaClass;
	}

	public MetaAttribute newAttribute(String fieldName,
									  DataTypeEnum dataType,
									  CardinalityEnum cardinality){
		MetaAttribute metaField = new MetaAttribute();
		metaField.setName(fieldName);
		metaField.setDataType(dataType);
		metaField.setCardinality(cardinality);
		return metaField;
	}
	
	public MetaAttribute newBooleanAttribute(String fieldName){
		return newAttribute(fieldName, DataTypeEnum.BOOLEAN, CardinalityEnum.One);
	}
	
	public MetaAttribute newDateAttribute(String fieldName){
		return newAttribute(fieldName, DataTypeEnum.DATE, CardinalityEnum.One);
	}
	
	public MetaAttribute newIntegerAttribute(String fieldName){
		return newAttribute(fieldName, DataTypeEnum.INTEGER, CardinalityEnum.One);
	}
	
	public MetaAttribute newStringAttribute(String fieldName){
		return newAttribute(fieldName, DataTypeEnum.STRING, CardinalityEnum.One);
	}
	
	public static MetaRelationship newReference(String fieldName,
												String refDataType,
												RelationTypeEnum refRelation,
												CardinalityEnum cardinality){
		MetaRelationship metaField = new MetaRelationship();
		metaField.setName(fieldName);
		metaField.setDataType(DataTypeEnum.RELATIONSHIP);
		metaField.setCardinality(cardinality);
		metaField.setRefDataType(refDataType);
		metaField.setRelationType(refRelation);
		return metaField;
	}
	
	public BsonEntity newEntity(String metaType,String branch,String name){
		MetaClass metaClass = metaService.getMetaClass(metaType);
		CheckConditions.checkNotNull(metaClass, "Unknown meta class: " + metaType);
		BsonEntity inst = new BsonEntity(metaClass);
		inst.setBranchId(branch);
		inst.addFieldValue("name", name);
		inst.setVersion(0);
		inst.setParentVersion(0);
		return inst;
	}
	
	public static boolean isInternalField(String fieldName){
		for(InternalFieldEnum internalField: InternalFieldEnum.values()){
			if(internalField.getName().equals(fieldName)){
				return true;
			}
		}
		return false;
	}
	
	
	private Object genValue(MetaField metaField){
		DataTypeEnum dataType = metaField.getDataType();
		Object value = null;
		Date seed = new Date();
		Random random = new Random(seed.getTime());
		switch(dataType){
			case BOOLEAN:
				value = random.nextBoolean();
				break;
			case DATE:
				value = new Date();
				break;
			case DOUBLE:
				value = random.nextDouble();
				break;
			case INTEGER:
				value = random.nextInt();
				break;
			case LONG:
				value = random.nextLong();
				break;
			case STRING:
                Integer sequence = random.nextInt();
                value = String.format("Dummy %s-%d-%04d", metaField.getName(), System.nanoTime(), sequence.intValue());
				break;
			case ENUM:
				Integer index = random.nextInt();
				if(index<0){
					index = -index;
				}
				MetaAttribute metaAttribute = (MetaAttribute)metaField;
				List<String> values = metaAttribute.getEnumValues();
				int selector = index % values.size();
				value = values.get(selector);
				break;
			default:
				break;
		}
		
		return value;
	}
	
	public BsonEntity newEntityWithDummyValues(String metaType,String branch,String name){
		
		MetaClass metaClass = metaService.getMetaClass(metaType);
		CheckConditions.checkNotNull(metaClass, "Unknown meta class: " + metaType);
		BsonEntity newEntity = newEntity(metaType,branch,name);
		
		for(MetaField metaField : metaClass.getFields()){
			String fieldName = metaField.getName();		
			if(isInternalField(fieldName)){
				continue;
			}
			if(!newEntity.hasField(fieldName)){
				CardinalityEnum cardinality = metaField.getCardinality();
				DataTypeEnum dataType = metaField.getDataType();
				if(dataType != DataTypeEnum.RELATIONSHIP){
					int valueCount = 1;
					if(cardinality == CardinalityEnum.Many){
						valueCount = 5; //pre-allocate 5 values in array
					}
					for(int valueIndex=0; valueIndex<valueCount; valueIndex++){
						Object value = genValue(metaField);
						newEntity.addFieldValue(fieldName, value);
					}
				}
			}
		}
		return newEntity;
		
	}
	
	
	
}
