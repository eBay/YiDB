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

package com.ebay.cloud.cms.metadata.exception;



/**
 * 
 * @author liubo
 *
 */
public class MetaDataException extends RuntimeException {
	
	private static final long serialVersionUID = -587534448521373239L;
	
	public static enum MetaErrCodeEnum{
		ILLEGAL_INDEX(30000),
		ILLEGAL_META_CLASS(30001),
		ILLEGAL_REPOSITORY(30002),
		INDEX_EXISTS(30003),
		INDEX_NOT_EXISTS(30004),
		INDEX_OPTION_OPERATION_ERROR(30005),
		JSON_PARING_ERROR(30006),
		META_CLASS_EXISTS(30007),
		META_CLASS_NOT_EXISTS(30008),
		META_FIELD_EXISTS (30009),
		META_FIELD_NOT_EXISTS (30010),
		MONGO_OPERATION_ERROR(30011),
		REPOSITORY_EXISTS(30012),
		REPOSITORY_NOT_EXISTS(30013),
		SHOULD_SETUP_ANCESTORS(30014),
		NO_METASERVICE_PROVIDED(30015),
		NO_METACLASS_GRAPH_PROVIDED(30016),
		META_CLASS_NOT_FOUND_IN_GRAPH(30017),
		CREATE_REPOSITORY_ERROR(30018),
		LOCK_INTERRUPTED(30019),
		NONE_ORPHAN_META_CLASS(30020),
		VERSION_CONFLICTED(30021),
		DB_NAME_REQUIRED(30022),
		JSON_CONVERT_ERROR(30023);
		
		MetaErrCodeEnum(int errorCode){
			this.errorCode = errorCode;
		}
	
		private int errorCode;
		public int getErrorCode(){
			return errorCode;
		}
		
		public MetaErrCodeEnum valueFrom(int code) {
		    for (MetaErrCodeEnum e : MetaErrCodeEnum.values()) {
		        if (e.getErrorCode() == code) {
		            return e;
		        }
		    }
		    
		    throw new IllegalArgumentException("invalid code for MetaErrCodeEnum");
		}
	}
	
	private final MetaErrCodeEnum errorCodeEnum;

	public MetaDataException(MetaErrCodeEnum errorCode, Throwable cause){
		super(cause);
		this.errorCodeEnum = errorCode;
	}
	
	public MetaDataException(MetaErrCodeEnum errorCode, String msg, Throwable cause){
		super(msg, cause);
		this.errorCodeEnum = errorCode;
	}
	
	public MetaDataException(MetaErrCodeEnum errorCode,String msg){
		super(msg);
		this.errorCodeEnum = errorCode;
	}
	
	public int getErrorCode(){
		return errorCodeEnum.getErrorCode();
	}
	
	public MetaErrCodeEnum getErrorEnum(){
        return errorCodeEnum;
    }
}
