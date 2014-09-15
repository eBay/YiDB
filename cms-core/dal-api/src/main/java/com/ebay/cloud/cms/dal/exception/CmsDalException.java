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

package com.ebay.cloud.cms.dal.exception;

/**
 * 
 * @author jianxu1
 * @date 2011/5/8
 *
 */
@SuppressWarnings("serial")
public class CmsDalException extends RuntimeException {

	//reserve DAL error code from 1000 - 2000
	public static enum DalErrCodeEnum{
	    /**
	     * @deprecated
	     */
		NO_ERROR(1000),
		VERSION_CONFLICT(1001),
		/**
		 * @deprecated
		 */
		MISS_VERSION(1002),		
		INVALID_VERSION(1003),
		ENTITY_NOT_FOUND(1004),
		DIRTY_DATA_WRONG_TYPE(1005),
		PROCESS_JSON(1006),
		MISS_REFID(1007),
		MISS_META_FIELD(1008),
		MISS_RUNTIME_FIELD(1009),
		MISS_ID(1010),
		/**
         * @deprecated
         */
		NO_TYPE_IN_DBOBJECT(1011),
		/**
         * @deprecated
         */
		TYPE_INCONSISTENCY(1012),
		REPOSITORYNAME_INCONSISTENCY(1013),
		BRANCHID_INCONSISTENCY(1014),
		VIOLATE_REFERENCE_INTEGRITY(1015),
		ENTITY_ALREADY_EXIST(1016),
		CONSTANT_FIELD_MODIFICATION(1017),
		STANDALONE_EMBED(1018),
		MISMATCH_META_TYPE(1019),
		INVALID_EMBED_ID(1020),
		ENTITY_NOT_ACTIVE(1021),
		CANNOT_DELETE_EXPRESSION_FIELD(1022),
		VALIDATION_FAILED(1023),
	    DUPLICATE_REFERENCE(1024),
	    ENTITY_CREATE(1025),
		AGGREGATION_FAILED(1101),
		MONGO_EXCEPTION_DUPLICATE(1301),
        MONGO_EXCEPTION_NETWORK(1302),
        MONGO_EXCEPTION_CURSORNOTFOUND(1303),
		MONGO_EXCEPTION_UNKNOWN(1310),
		JS_EXPRESSION_EXECUTION_ERROR(1401),
		JS_EXPRESSION_TIMEOUT(1402),
		EXCEED_MAX_DOCUMENT_SIZE(1501),
		EXCEED_MAX_INDEX_SIZE(1502),
		EXCEED_MAX_INDEX_NAME(1503),
		EXCEED_MAX_FIELD_SIZE_OF_COMPOUND_INDEX(1504),
		EXCEED_MAX_INDEXED_ARRAY_SIZE(1505),
		EXCEED_QUERY_LIMIT_MEMORY_USAGE(1506),
		EXCEED_MAX_REPOSITORY_SIZE(1507),
		INVALID_REGEXPRESSION(1508);
		
		DalErrCodeEnum(int errorCode){
			this.errorCode = errorCode;
		}
	
		private int errorCode;
		public int getErrorCode(){
			return errorCode;
		}
		
		public DalErrCodeEnum valueFrom(int code) {
		    for (DalErrCodeEnum e : DalErrCodeEnum.values()) {
		        if (e.getErrorCode() == code) {
		            return e;
		        }
		    }
		    
		    throw new IllegalArgumentException("invalid code for DalErrCodeEnum");
		}
	}
	
	private final DalErrCodeEnum errorCodeEnum;
	
	public CmsDalException(DalErrCodeEnum errorCode, Throwable cause){
		super(cause);
		this.errorCodeEnum = errorCode;
	}
	
	public CmsDalException(DalErrCodeEnum errorCode, String msg, Throwable cause){
		super(msg, cause);
		this.errorCodeEnum = errorCode;
	}
	
	public CmsDalException(DalErrCodeEnum errorCode,String msg){
		super(msg);
		this.errorCodeEnum = errorCode;
	}
	
	public int getErrorCode(){
		return errorCodeEnum.getErrorCode();
	}
	
	public DalErrCodeEnum getErrorEnum(){
        return errorCodeEnum;
    }
}
