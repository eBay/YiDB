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
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.CheckConditions;

public abstract class AbstractEntity implements IEntity {

	protected MetaClass metaClass;
	
    private String      embedPath = null;
	
	protected AbstractEntity(MetaClass metaClass){
	    CheckConditions.checkNotNull(metaClass, "Metaclass for initializaing entity could not be null!");
		this.metaClass = metaClass;
	}
	
	@Override
	public String getId() {
	    return (String)getInternalFieldValue(InternalFieldEnum.ID);
	}
	
	@Override
	public void setId(String id){
		addFieldValue(InternalFieldEnum.ID.getName(),id);
	}
	
	@Override
	public String getBranchId() {
	    return (String)getInternalFieldValue(InternalFieldEnum.BRANCH);
	}
	
	@Override
	public void setBranchId(String branch){
		addFieldValue(InternalFieldEnum.BRANCH.getName(), branch);
	}
	
	@Override
	public void setCreator(String creator) {
	    addFieldValue(InternalFieldEnum.CREATOR.getName(), creator);
	}
	
	@Override
	public String getCreator() {
	    return (String)getInternalFieldValue(InternalFieldEnum.CREATOR);
	}
	
	@Override
	public void setModifier(String modifier) {
	    addFieldValue(InternalFieldEnum.MODIFIER.getName(), modifier);
	}

    @Override
    public String getModifier() {
        return (String) getInternalFieldValue(InternalFieldEnum.MODIFIER);
    }
    
    @Override
    public void setHostEntity(String host) {
        addFieldValue(InternalFieldEnum.HOSTENTITY.getName(), host);
    }

    @Override
    public String getHostEntity() {
        return (String) getInternalFieldValue(InternalFieldEnum.HOSTENTITY);
    }

    @Override
    public void setMetaVersion(int metaVersion) {
        addFieldValue(InternalFieldEnum.METAVERSION.getName(), metaVersion);
    }

    @Override
    public int getMetaVersion() {
        Integer metaVersion = (Integer)getInternalFieldValue(InternalFieldEnum.METAVERSION);
        if (metaVersion == null) {
            return IEntity.NO_VERSION;
        }
        return metaVersion.intValue();
    }

	@Override
	public String getRepositoryName() {
		return getMetaClass().getRepository();
	}

	@Override
	public int getVersion() {
		Integer version = (Integer)getInternalFieldValue(InternalFieldEnum.VERSION);
		if (version == null) {
		    return IEntity.NO_VERSION;
		}
		return version.intValue();
	}

	@Override
	public void setVersion(int version){
		addFieldValue(InternalFieldEnum.VERSION.getName(), version);
	}
	
	@Override
	public int getParentVersion() {
		Integer version = (Integer)getInternalFieldValue(InternalFieldEnum.PVERSION);
		if (version == null) {
            return IEntity.NO_VERSION;
        }
		return version.intValue();
	}

	@Override
	public void setParentVersion(int version){
		addFieldValue(InternalFieldEnum.PVERSION.getName(), version);
	}
	
	@Override
    public Date getCreateTime() {
        return (Date)getInternalFieldValue(InternalFieldEnum.CREATETIME);
    }

    @Override
    public void setCreateTime(Date date){
        addFieldValue(InternalFieldEnum.CREATETIME.getName(), date);
    }
	
	@Override
	public Date getLastModified() {
	    return (Date)getInternalFieldValue(InternalFieldEnum.LASTMODIFIED);
	}

	@Override
	public void setLastModified(Date date){
		addFieldValue(InternalFieldEnum.LASTMODIFIED.getName(),date);
    }

    @Override
    public String getShardKey() {
        return (String) getInternalFieldValue(InternalFieldEnum.SHARD_KEY);
    }

    @Override
    public void setShardKey(String shardKey) {
        addFieldValue(InternalFieldEnum.SHARD_KEY.getName(), shardKey);
    }

    @Override
	public StatusEnum getStatus() {
		Object value = getInternalFieldValue(InternalFieldEnum.STATUS);
		for(StatusEnum status: StatusEnum.values()){
			if(status.toString().equals(value)){
				return status;
			}
		}
		//TODO: should I add InValid?
		return StatusEnum.ACTIVE;
	}

    public void setEmbedPath(String embedPath) {
        this.embedPath = embedPath;
    }
    
    public String getEmbedPath() {
        return embedPath;
    }


	@Override
	public void setStatus(StatusEnum status){
		addFieldValue(InternalFieldEnum.STATUS.getName(), status.toString());
	}
	
	@Override
	public MetaClass getMetaClass() {
		return metaClass;
	}
	
	@Override
	public String getType(){
		return getMetaClass().getName();
	}
	
	@Override
    public void setFieldValues(String fieldName, List<?> value) {               
        throw new UnsupportedOperationException();
    }
	
	@Override
    public void removeField(String fieldName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasField(String fieldName) {        
        throw new UnsupportedOperationException();
    }

	@Override
	public String toString(){
		return getNode().toString();
	}
	
	@Override
	public void traverse(IEntityVisitor visitor){

		List<MetaField> attrFields = new ArrayList<MetaField>();
		List<MetaRelationship> refFields = new ArrayList<MetaRelationship>();
		Collection<String> visitFields = visitor.getVisitFields(this);
		if (visitFields == null) {
		    return;
		}
		
		for(String fieldName : visitFields){
			MetaField metaField = metaClass.getFieldByName(fieldName);
            if (metaField == null) {
                // ignore field name that not as a meta field
                continue;
			}
            if (metaField.getDataType() == DataTypeEnum.RELATIONSHIP) {
                refFields.add((MetaRelationship) metaField);
            } else {
                attrFields.add(metaField);
            }
		}
		//make sure we process attributes first
		for(MetaField attrField: attrFields){
			visitor.processAttribute(this, attrField);
		}
		
		for(MetaRelationship refField: refFields){
			visitor.processReference(this, refField);
		}
	}
		
	protected abstract Object getInternalFieldValue(InternalFieldEnum fieldEnum);
	
	@Override
	public long getEntitySize() {
		return this.toString().getBytes().length;
	}

}
