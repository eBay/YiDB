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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;

public interface IEntity {
	
	static public final int START_VERSION = 0;
	static public final int NO_VERSION = -1;
	static public final String ID_SEP = "!";
	static public final String ID_SEP_REG = "\\" + ID_SEP;
	
	String getId();
	String getBranchId();
	String getRepositoryName();
	String getType();
	int getVersion();
	int getParentVersion();
	Date getCreateTime();
	Date getLastModified();
	StatusEnum getStatus();
	String getCreator();
	String getModifier();
	String getShardKey();
	String getHostEntity();
	int getMetaVersion();
	
	void setId(String id);
	void setBranchId(String branch);
	void setVersion(int version);
	void setParentVersion(int version);
	void setCreateTime(Date date);
	void setLastModified(Date date);
	void setStatus(StatusEnum status);
	void setCreator(String creator);
    void setModifier(String modifier);
    void setShardKey(String shardKey);
	void setHostEntity(String host);
	void setMetaVersion(int metaVersion);
	
	Collection<String> getFieldNames();
	boolean hasField(String fieldName);
	
	MetaClass getMetaClass();
	
	String getEmbedPath();
	void setEmbedPath(String path);
	
	List<?> getFieldValues(String fieldName);
	void setFieldValues(String fieldName, List<?> value);
	void addFieldValue(String fieldName, Object value);
	Object getFieldProperty(String fieldName, String propertyName);
	void setFieldProperty(String fieldName, String propertyName, Object value);
	boolean hasFieldProperty(String fieldName, String propertyName);
	Object getNode();
	
	void removeField(String fieldName);
	
	void traverse(IEntityVisitor visitor);
	
	long getEntitySize();
		
}
