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

package com.ebay.cloud.cms.dal.entity.flatten.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

/**
 * @liasu 
 */
public class FlattenEntityIDHelper extends AbstractEntityIDHelper {
    
    private static final FlattenEntityIDHelper INSTANCE = new FlattenEntityIDHelper();
    private FlattenEntityIDHelper() {
    }
    public static FlattenEntityIDHelper getInstance() {
        return INSTANCE;
    }
    
	/**
	 * Returns the embed path in <b>*Db Value Name*</b> for mongo query usage
	 * 
	 * 2013.12.9 - to reduce memory consumption, use db value name directly in parent object instead of two level json : 
	 * From 
	 * <pre>
	 * {
	 *     dbname: { value_key : {$value} 
	 * }
	 * </pre>
	 * to 
	 * <pre>
	 * {
	 *     dbname + "_" +value_key : {$value}
	 * }
	 * </pre>
	 * 
	 * 
	 * @param artificialId = rootType-rootObjectId-fieldName1-embedObjectId-...
	 * e.g.  
	 * Given : ApplicationService-appSvcObjectId-serviceInstances-svcInstObjectId-runsOn-nodeServerObjectId
	 * Return: fsi.fro (here fsi stands for the serviceInstance's dbName in applciation service, while fro stands for
	 *             runsOn's db name in serviceInstance).
	 * @param rootMetaClass - the root meta class of the given artificial id.
	 * @return
	 */
	public List<String> getEmbedPathSegs(String artificialId, MetaClass rootMetaClass) {
		String[] parts = artificialId.split(ID_SEP_REG);
		if (parts == null || parts.length <= FlattenEntityIDHelper.FIELD_NAME_START_INDEX) {
		    return Collections.emptyList();
		}
		List<String>  fieldPath = new ArrayList<String>();
		String fieldValueDbName = null;
		MetaClass parentMetaClass = rootMetaClass;
		for(int partIndex = FlattenEntityIDHelper.FIELD_NAME_START_INDEX; partIndex < parts.length; partIndex++){
			if(partIndex % 2 == 0){
			    MetaRelationship metaField = (MetaRelationship)parentMetaClass.getFieldByName(parts[partIndex]);
                if (metaField == null) {
                    throw new RuntimeException(MessageFormat
                            .format("getEmbedPathSegs: Can not find relationship for meta field {0} on meta class {1}. Param artificialId: {3}",
                                            parts[partIndex], parentMetaClass.getName(), artificialId));
                }
                fieldValueDbName = metaField.getFlattenValueDbName();
                fieldPath.add(fieldValueDbName);

                parentMetaClass = metaField.getRefMetaClass();
			}
		}
		return fieldPath;
	}
	
	public String getEmbedPath(String artificialId, MetaClass rootMetaClass){
		List<String> segs = getEmbedPathSegs(artificialId, rootMetaClass);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < segs.size(); i++) {
		    if (i != 0) {
		        sb.append(DOT);
		    }
		    sb.append(segs.get(i));
		}
		return sb.toString();
	}
	
	public BitSet checkArrayOnPath(String embedId, MetaClass rootMetaClass) {
        BitSet bs = new BitSet();
        MetaClass currentMetaClass = rootMetaClass;
        List<String> fieldValueDbNameList= getEmbedPathSegs(embedId, rootMetaClass);
        for(int index = 0; index < fieldValueDbNameList.size(); index++) {
            String fieldValueDbName = fieldValueDbNameList.get(index);
            MetaRelationship metaField = (MetaRelationship)currentMetaClass.getFieldByFlattenValueDbName(fieldValueDbName);
            if(metaField.getCardinality() == CardinalityEnum.Many){
                bs.set(index);
            }
            currentMetaClass = metaField.getRefMetaClass();
        }
        return bs;
    }
    
    public MetaRelationship getLastMetaField(String embedId, MetaClass rootMetaClass) {
        MetaRelationship metaField = null;
        MetaClass currentMetaClass = rootMetaClass;
        List<String> fieldNameList= getEmbedPathSegs(embedId, rootMetaClass);
        for(int index = 0; index < fieldNameList.size(); index++) {
            String fieldName = fieldNameList.get(index);
            metaField = (MetaRelationship)currentMetaClass.getFieldByFlattenValueDbName(fieldName);
            currentMetaClass = metaField.getRefMetaClass();
        }
        return metaField;
    }
}
