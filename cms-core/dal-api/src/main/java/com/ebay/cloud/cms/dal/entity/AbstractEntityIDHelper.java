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

package com.ebay.cloud.cms.dal.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

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

public abstract class AbstractEntityIDHelper {

	public static final String DOT = ".";
	public static final int FIELD_NAME_START_INDEX = 2;
	
	public static final String ID_SEP_REG = "\\" + IEntity.ID_SEP;
	
	public static String getParentId(String embedId) {
        String[] idParts = embedId.split(ID_SEP_REG);
        if(idParts.length <= 4){
            return idParts[1];
        }
        return StringUtils.join(IEntity.ID_SEP, Arrays.asList(idParts).subList(0, idParts.length-2));
    }
	
    public static String getParentOid(String embedId) {
        String[] idParts = embedId.split(ID_SEP_REG);
        return idParts[idParts.length-3];
    }
	
	public static MetaClass getParentMetaClass(String embedId, IMetadataService metadataService) {
		String parentType = getRootEntityType(embedId);
		MetaClass parentMetaClass = metadataService.getMetaClass(parentType);
		String[] idParts = embedId.split(ID_SEP_REG);
		List<String> idLists = Arrays.asList(idParts);
		String fieldName = null;
		
		while(idLists.size() > 4) {
			fieldName = idLists.get(2);
			MetaRelationship embedRel = (MetaRelationship) parentMetaClass.getFieldByName(fieldName);
			parentMetaClass = embedRel.getRefMetaClass();
			idLists = idLists.subList(2, idLists.size());
		}
		
        return parentMetaClass;
	}
	
	public static String getParentFieldName(String embedId) {
		String[] idParts = embedId.split(ID_SEP_REG);
		return idParts[idParts.length - 2];
	}

	public static String getRootEntityType(String artificialId){
        String[] idParts = artificialId.split(ID_SEP_REG);
        return idParts[0];
    }

	public static boolean isEmbedEntity(String artificialId) {
		if (artificialId == null) {
			return false;
		}
		
		int index = artificialId.indexOf(IEntity.ID_SEP);
		if(index == -1){
			return false;
		}
		return true;
	}
	

	public static String getRootId(String entityId) {
		String rootId = null;
		if(isEmbedEntity(entityId)){
			String[] idParts = entityId.split(ID_SEP_REG);
			rootId = idParts[1];
		}else{
			rootId = entityId;
		}
		return rootId;
	}
    

    public abstract List<String> getEmbedPathSegs(String artificialId, MetaClass rootMetaClass);
    
    public abstract String getEmbedPath(String artificialId, MetaClass rootMetaClass);
    
    public abstract BitSet checkArrayOnPath(String embedId, MetaClass rootMetaClass);
    
    public abstract MetaRelationship getLastMetaField(String embedId, MetaClass rootMetaClass);

    public static String getEmbedPath(String embedId) {
        String[] idParts = embedId.split(ID_SEP_REG);
        if (idParts.length < 4) {
            return null;
        }
        return StringUtils.join(IEntity.ID_SEP, Arrays.asList(idParts).subList(0, idParts.length - 1));
    }
    
    public static List<String> generateAncestorIds(String artificialId) {
        // root type
        int index = artificialId.indexOf(IEntity.ID_SEP);
        // root id
        index = artificialId.indexOf(IEntity.ID_SEP, index + 1);
        List<String>  embedIdList = new ArrayList<String>();
        boolean isId = false;
        while ((index = artificialId.indexOf(IEntity.ID_SEP, index + 1)) > 0) {
            if (isId) {
                embedIdList.add(artificialId.substring(0, index));
            }
            isId = !isId;
        }
        embedIdList.add(artificialId);
        return embedIdList;
    }
    
    public static String generateEmbedId(String rootType, String parentId, String embedId, MetaField metaField){
        //2012/8/31 lzhijun validate embedded id, we should check if handwrite id in data file is valid. We need to lower check id constraints
        validateId(embedId);
        String artificialId = null;
        if(parentId.startsWith(rootType + "!")){
        	artificialId = parentId + IEntity.ID_SEP + metaField.getName() + IEntity.ID_SEP + embedId;
        }else{
        	artificialId = rootType + IEntity.ID_SEP + parentId + IEntity.ID_SEP + metaField.getName() + IEntity.ID_SEP + embedId;
        }
        return artificialId;
    }
    
    public static String generateEmbedIdByEmbedPath(String embedPath, String embedId) {
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(embedPath));
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(embedId));
        return StringUtils.join(IEntity.ID_SEP, embedPath, embedId);
    }
    
	public static String generateEmbedPath(String rootType, String parentId, MetaField metaField) {
	    CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(parentId));
	    CheckConditions.checkNotNull(metaField);
        String genPath = null;
        if(parentId.startsWith(rootType)){
            genPath = StringUtils.join(IEntity.ID_SEP, parentId, metaField.getName());
        } else {
            genPath = StringUtils.join(IEntity.ID_SEP, rootType, parentId, metaField.getName());
        }
	    return genPath;
	}
	
	/**
	 * 
	 * @param currentId
	 * @history:
	 * 
	 */
	public static void validateId(String currentId) {
        String[] idParts = currentId.split(ID_SEP_REG);
        if(idParts.length > 1){ //embed document
            for(int index = 1; index < idParts.length; index++){
                if((index & 1) == 1){ //this part is id
                    String id = idParts[index];
                    //2012/8/30, jianxu1: to support client set CMS entity id when creating new entity, do not use ObjectId.isValid 
               	    //e.g. in cms sync, we use user provided id which is not necessary following mongo object id format
                    //CheckConditions.checkArgument(ObjectId.isValid(id), "Invalid Id %s in %s",id,currentId);
                    CheckConditions.checkArgument(id.length() > 0, "Invalid Id %s in %s",id,currentId);
                }
            }
        }else{
            //CheckConditions.checkArgument(ObjectId.isValid(currentId), "Invalid Id %s", currentId);
            CheckConditions.checkNotNull(currentId, "Invalid empty id");
            CheckConditions.checkArgument(currentId.length() > 0, "Invalid empty id");
        }
        
    }
	
}
