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

package com.ebay.cloud.cms.dalapi.entity.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

/**
 * artificialId:  for non-embedded document, realId = artificialId
 * for embedded document, say TypeA.TypeB
 * artificialId = <root meta class type>-<ObjectID of A>-<name of field B in A>-<ObjectId of B>
 * realId = ObjectId of B
 * when saving document into mongo db, we only store ObjectId
 * when getting document from mongo db, we need to re-construct artificialId for embedded document
 * 
 * @author jianxu1, liasu
 * @date 2012/5/16
 * @history
 * 
 * 2012/5/24 FIX in getEmbedArtificialId(parentID, embedId)
 * parentID could also be an artificial id of an embed document (parent itself is embed too), in that case,
 * I can not use ObjectId.isValid to check parentID
 */

public class EntityIDHelper extends AbstractEntityIDHelper {
    private static final EntityIDHelper INSTANCE = new EntityIDHelper();
    private EntityIDHelper() {
    }
    public static EntityIDHelper getInstance() {
        return INSTANCE;
    }
    

	/**
	 * Returns the embed path in <b>*Db Name*</b> for mongo query usage
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
		if (parts == null || parts.length <= EntityIDHelper.FIELD_NAME_START_INDEX) {
		    return Collections.emptyList();
		}
		List<String>  fieldPath = new ArrayList<String>();
		String dbFieldName = null;
		MetaClass parentMetaClass = rootMetaClass;
		for(int partIndex = EntityIDHelper.FIELD_NAME_START_INDEX; partIndex < parts.length; partIndex++){
			if(partIndex % 2 == 0){
			    MetaRelationship metaField = (MetaRelationship)parentMetaClass.getFieldByName(parts[partIndex]);
                if (metaField == null) {
                    throw new RuntimeException(MessageFormat
                            .format("getEmbedPathSegs: Can not find relationship for meta field {0} on meta class {1}. Param artificialId: {2}",
                                            parts[partIndex], parentMetaClass.getName(), artificialId));
                }
                dbFieldName = metaField.getDbName();
                fieldPath.add(dbFieldName);

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
		    sb.append(DOT);
		    sb.append(MetaField.VALUE_KEY);
		}
		return sb.toString();
	}
	
	public BitSet checkArrayOnPath(String embedId, MetaClass rootMetaClass) {
        BitSet bs = new BitSet();
        MetaClass currentMetaClass = rootMetaClass;
        List<String> fieldNameList= getEmbedPathSegs(embedId, rootMetaClass);
        for(int index = 0; index < fieldNameList.size(); index++) {
            String fieldName = fieldNameList.get(index);
            MetaRelationship metaField = (MetaRelationship)currentMetaClass.getFieldByDbName(fieldName);
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
            metaField = (MetaRelationship)currentMetaClass.getFieldByDbName(fieldName);
            currentMetaClass = metaField.getRefMetaClass();
        }
        return metaField;
    }
    
}
