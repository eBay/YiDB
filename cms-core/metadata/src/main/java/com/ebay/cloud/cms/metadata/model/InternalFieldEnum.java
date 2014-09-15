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

import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
/**
 * 
 * @author jianxu1
 *
 * @history
 * 2012/7/12 add parent version to support branch commit,entity in sub branch need to remember the origin parent version when
 * transaction starts, so when we commit entities from sub branch to parent branch, we know entity in parent branch has not 
 * been changed.
 */
public enum InternalFieldEnum {

	ID("_oid", DataTypeEnum.STRING, "_i"),
	TYPE("_type",DataTypeEnum.STRING, "_t"),
	BRANCH("_branch",DataTypeEnum.STRING, "_b"), 
	VERSION("_version",DataTypeEnum.INTEGER, "_v"),
	CREATETIME("_createtime",DataTypeEnum.DATE, "_c"),
	LASTMODIFIED("_lastmodified",DataTypeEnum.DATE, "_l"),
	STATUS("_status", DataTypeEnum.ENUM, "_s"),
	PVERSION("_pversion",DataTypeEnum.INTEGER,"_pv"),
	COMMENT("_comment", DataTypeEnum.STRING, "_cmt"),
	USER("_user", DataTypeEnum.STRING, "_u"),
	MODIFIER("_modifier", DataTypeEnum.STRING, "_m"),
	CREATOR("_creator", DataTypeEnum.STRING, "_o"),
	SHARD_KEY("_shardkey", DataTypeEnum.STRING, "_sk"),
	HOSTENTITY("_hostentity", DataTypeEnum.STRING, "_h"),
	METAVERSION("_metaversion", DataTypeEnum.INTEGER, "_mv")
	;

	private String name;
	private String dbName;
	private DataTypeEnum dataType;
	
	InternalFieldEnum(String name, DataTypeEnum dataType, String dbName){
		this.name = name;
		this.dataType = dataType;
		this.dbName = dbName;
	}
	
	public DataTypeEnum getDataType(){
		return this.dataType;
	}
	
	public String getName() {
        return this.name;
    }
	
	public String getDbName() {
	    return this.dbName;
	}
	
	@Override
	public String toString() {
        throw new RuntimeException("field name " + this.name);
    }
}
