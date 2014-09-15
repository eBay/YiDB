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

package com.ebay.cloud.cms.dal.entity.flatten.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBObject;

/**
 * base class for bson entity validator
 * 
 * @author xjiang
 *
 */
public abstract class AbstractBsonValidator implements IEntityVisitor {

    protected IEntity        parentEntity = null;
    private MetaRelationship parentField  = null;
    protected Date           visitTime    = null;
    protected boolean               checkMandatory;
    protected FlattenEntityIDHelper helper       = null;

    public AbstractBsonValidator(FlattenEntityIDHelper helper) {
        this.helper = helper;
    }
    
	@Override
	public void processAttribute(IEntity currentEntity, MetaField metaField) {
		String fieldName = metaField.getName();
		InternalFieldEnum fieldEnum = InternalFieldFactory.getInternalFieldEnum(fieldName);
		if (fieldEnum != null) {
		    processInternalAttribute(fieldEnum, (NewBsonEntity)currentEntity, parentField, (NewBsonEntity)parentEntity);		    		         
		} else {
		    processUserAttribute((NewBsonEntity)currentEntity, metaField);
        }
	}

    protected void processUserAttribute(NewBsonEntity currentEntity, MetaField metaField) {
	    String fieldName = metaField.getName();
	    
	    if(metaField.isMandatory()) {
	        boolean missingData = false;
	        if (!currentEntity.hasField(fieldName)) {
	            missingData = true;
	        } else {
	            List<?> values = currentEntity.getFieldValues(fieldName);
	            if (values.isEmpty()) {
	                missingData = true;
	            } else if (values.size() == 1) {
	                Object val = values.get(0);
	                missingData = (val == null);
	            }
	        }
	        
	        if (missingData) {
	            MetaClass metaClass = currentEntity.getMetaClass();
                throw new CmsDalException(DalErrCodeEnum.MISS_RUNTIME_FIELD, String.format(
                        "Mandatory field %s on type %s miss runtime data. Entity is %s", fieldName, metaClass.getName(), currentEntity.getNode()));
	        }
        }
	    if (currentEntity.hasField(fieldName) && currentEntity.getFieldTimestamp(fieldName) == null) {
	        currentEntity.setFieldTimestamp(fieldName, visitTime);
        }

        if (currentEntity.hasField(fieldName) && metaField.getCardinality() == CardinalityEnum.Many) {
            currentEntity.setFieldLength(fieldName);
        }
	}

	protected void processInternalAttribute(InternalFieldEnum fieldEnum, NewBsonEntity currentEntity, MetaRelationship refField, NewBsonEntity parentEntity) {
	    switch (fieldEnum) {
            case ID:
                processId(parentEntity, refField, currentEntity);
                break;
            case CREATETIME:
                if (currentEntity.getCreateTime() == null) {
                    processCreateTime(parentEntity, refField, currentEntity);
                }
                break;
            case LASTMODIFIED:
                if (!currentEntity.getMetaClass().isEmbed() || currentEntity.getLastModified() == null) {
                    currentEntity.setLastModified(visitTime);
                }
                break;
            case STATUS:
            	//2012/7/18 jianxu1: can NOT setStatus to Active here, otherwise, Delete will be ignored
            	//because setStatus=Active will override the Delete status 
                //currentEntity.setStatus(StatusEnum.ACTIVE);
            	processStatus(parentEntity, refField, currentEntity);
                break;
            case TYPE:
                if(!currentEntity.hasField(InternalFieldEnum.TYPE.getName())) {
                    currentEntity.addFieldValue(InternalFieldEnum.TYPE.getName(), currentEntity.getType());
                    List<String> ancestors = currentEntity.getMetaClass().getAncestors();
                    if (ancestors != null) {
                        for (String s : ancestors) {
                            currentEntity.addFieldValue(InternalFieldEnum.TYPE.getName(), s);
                        }
                    }
                } else {
                    String metaType = currentEntity.getType();
                    String fieldType = (String)currentEntity.getFieldValues(InternalFieldEnum.TYPE.getName()).get(0);
                    if (!metaType.equals(fieldType)) {
                        throw new CmsDalException(DalErrCodeEnum.MISMATCH_META_TYPE, String.format("Given _type is %s, but required metaclass type is %s. Entity is %s", fieldType, metaType, currentEntity.getNode()));
                    }
                }
                break;
            case VERSION:
                processVersion(parentEntity, refField, currentEntity);
                break;
            case PVERSION:
            	processParentVersion(parentEntity, refField, currentEntity);
            	break;
            case BRANCH:
                if (StringUtils.isNullOrEmpty(currentEntity.getBranchId()) && parentEntity != null) {
                    currentEntity.setBranchId(parentEntity.getBranchId());
                }
                break;
            case COMMENT:
                processComment(parentEntity, refField, currentEntity);
                break;
            case MODIFIER:
                processModifier(parentEntity, refField, currentEntity);
                break;
            case USER:
                processUser(parentEntity, refField, currentEntity);
                break;
            case CREATOR:
                processCreator(parentEntity, refField, currentEntity);
                break;
            case SHARD_KEY:
                processShardKey(parentEntity, refField, currentEntity);
                break;
            case HOSTENTITY:
                processHostEntity(parentEntity, refField, currentEntity);
                break;
            case METAVERSION:
                processMetaVersion(parentEntity, refField, currentEntity);
                break;
            default:
            	if(!currentEntity.hasField(fieldEnum.getName())){
            		throw new CmsDalException(DalErrCodeEnum.MISS_RUNTIME_FIELD, fieldEnum.getName());
            	}
                break;
        }
	}

    @Override
	@SuppressWarnings("unchecked")
	public void processReference(IEntity currentEntity,MetaRelationship metaRelationship) {
	    IEntity oldParentEntity = parentEntity;
	    MetaRelationship oldParentField = parentField;
	    
        String fieldName = metaRelationship.getName();
        if (!currentEntity.hasField(fieldName)) {
            if (metaRelationship.isMandatory()) {
                throw new CmsDalException(DalErrCodeEnum.MISS_RUNTIME_FIELD, String.format("Mandatory field %s on type %s miss runtime data",
                        fieldName, currentEntity.getType()));
            }
            return;
        }

	    if (((NewBsonEntity)currentEntity).getFieldTimestamp(fieldName) == null) {
            ((NewBsonEntity)currentEntity).setFieldTimestamp(fieldName, visitTime);
        }

	    boolean needDupCheck = (metaRelationship.getCardinality() == CardinalityEnum.Many);
	    HashMap<String, HashSet<String>> typeMap = null;
	    if (needDupCheck) {
	        typeMap = new HashMap<String, HashSet<String>>();
	    }
	    List<NewBsonEntity> refEntities = (List<NewBsonEntity>) currentEntity.getFieldValues(fieldName);
        for (NewBsonEntity refEntity : refEntities) {
            if (refEntity == null) {
                continue;
            }

            if(metaRelationship.getRelationType() == RelationTypeEnum.Embedded) {
                parentEntity = currentEntity;
                parentField = metaRelationship;
                checkMandatory = true;
                refEntity.traverse(this);
                checkMandatory = false;
                parentEntity = oldParentEntity;
                parentField = oldParentField;
            } else {
                if (!refEntity.hasField(InternalFieldEnum.ID.getName()) || StringUtils.isNullOrEmpty(refEntity.getId())) {
                    throw new CmsDalException(DalErrCodeEnum.MISS_REFID,String.format("Reference field %s with type %s miss ref id. Ref id could not be null or empty!",
                            fieldName, refEntity.getMetaClass().getName()));
                }

                String refId = refEntity.getId();
                String refType = refEntity.getType();
                BasicDBObject refObject = refEntity.getNode();
                refObject.clear();
                refEntity.setId(refId);
                refEntity.addFieldValue(InternalFieldEnum.TYPE.getName(), refType);
            }
            
            if (needDupCheck) {
                String refId = refEntity.getId();
                String refType = refEntity.getType();
                HashSet<String> idSet = typeMap.get(refType);
                if (idSet == null) {
                    idSet = new HashSet<String>();
                    typeMap.put(refType, idSet);
                }
                idSet.add(refId);
            }
        }

        if (needDupCheck) {
            int total = 0;
            Collection<HashSet<String>> values = typeMap.values();
            for (HashSet<String> v : values) {
                 total += v.size();
            }
            if (total < refEntities.size()) {
                List<String> list = new ArrayList<String>();
                for(IEntity entity : refEntities){
//                    if(entity == null){
//                        list.add("null");
//                    }else{
                        list.add(entity.getId());
//                    }
                }
                throw new CmsDalException(DalErrCodeEnum.DUPLICATE_REFERENCE, String.format(
                        "Reference field %s contains duplicate references! Get ids: %s .", fieldName, list));
                
//                throw new CmsDalException(DalErrCodeEnum.DUPLICATE_REFERENCE, String.format("Reference field %s contains duplicate references! Get ids: %s .",
//                        fieldName, Collections2.transform(refEntities, new Function<IEntity, String>() {
//                            @Override
//                            public String apply(IEntity input) {
//                                return input.getId();
//                            }
//                        })));
            }
        }

		if (metaRelationship.getCardinality() == CardinalityEnum.Many) {
		    ((NewBsonEntity)currentEntity).setFieldLength(fieldName);
        }
	}
    
	protected abstract void processId(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);
	
	protected abstract void processVersion(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);
	
	protected abstract void processParentVersion(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);

	protected abstract void processStatus(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);

	protected abstract void processCreateTime(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);

	protected abstract void processCreator(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity);

    protected void processUser(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) { }

    protected void processModifier(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) { }

    protected void processComment(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) { }

    protected void processShardKey(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) { }

    protected void processHostEntity(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) { }

    protected void processMetaVersion(NewBsonEntity parentEntity, MetaRelationship refField, NewBsonEntity currentEntity) {
        currentEntity.setMetaVersion(currentEntity.getMetaClass().getVersion());
    }
}
