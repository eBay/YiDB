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

package com.ebay.cloud.cms.entmgr.exception;




public class CmsEntMgrException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public static enum EntMgrErrCodeEnum{
	    /**
         * @deprecated
         */
		NO_ERROR(10000),
		/**
         * @deprecated
         */
		Entity_CREATE(10001),
		BRANCH_NOT_FOUND(10002),
		BRANCH_COMMIT_VERSION_CONFLICT(10003),
		BRANCH_STATUS_WRONG(10004),
		/**
         * @deprecated
         */
		BRANCH_UNSOPPORT_HISTORY(10005),
		REPOSITORY_NOT_FOUND(10006),
		FIELD_NOT_FOUND(10007),
		BRANCH_EXISTS(10008), 
		ILLEGAL_BRANCH_ENTITY(10009),
		BRANCH_WRONG_OPS(10010),
		VIOLATE_REFERENCE_INTEGRITY(10011),
		ENTITY_EXISTED(10012),
		UNIQUE_INDEX_VIOLATION(10013),
		OPERATION_CHECK_FAILED(10014),
		CONDITIONAL_UPDATE_FAILED(10015),
		INNER_RELATIONSHIP_EXISTED(10016),
		/**
		 * Thrown when create an inner entity while server could not find the host entity
		 */
		INVALID_HOST_ENTITY_PATH(10017),
		/**
         * Inner relationship is not supposed to be modify through its
         * containing entity. If user want to directly all modify/delete of the
         * inner relationship field, this immutable error code is returned
         */
	    INNER_RELATIONSHIP_IMMUTABLE(10019),
	    META_CONTAINS_INNER_RELATIONSHIP(10020),
	    /**
	     * Embed relationship is not supposed to be modify through its containing entity. 
	     * Thrown when user try to modify embed entity through its containing entity.
	     */
	    EMBED_RELATIONSHIP_IMMUTABLE(10021),
	    INVALID_EMBED_ID_PATH(10022),
	    BATCH_OPERATION_PARTIAL_FAILURE(10023),
        INNER_PARTIAL_CREATION(10024);
		
		EntMgrErrCodeEnum(int errorCode){
			this.errorCode = errorCode;
		}
	
		private int errorCode;
		public int getErrorCode(){
			return errorCode;
		}
	}
	
	private final EntMgrErrCodeEnum errorCodeEnum;
	
	public CmsEntMgrException(EntMgrErrCodeEnum errorCode,String msg){
		super(msg);
		this.errorCodeEnum = errorCode;
	}

    public CmsEntMgrException(EntMgrErrCodeEnum errorCode, String msg, Throwable cause){
        super(msg, cause);
        this.errorCodeEnum = errorCode;
    }
	
	public int getErrorCode(){
		return errorCodeEnum.getErrorCode();
	}
	
	public EntMgrErrCodeEnum getErrorEnum(){
        return errorCodeEnum;
    }
}
