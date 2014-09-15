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

package com.ebay.cloud.cms.query.exception;

/**
 * base exception class
 * 
 * @author xjiang
 *
 */
public abstract class QueryException extends RuntimeException {		

	private static final long serialVersionUID = 5866580005765735249L;
	
	public static enum QueryErrCodeEnum {
	    /**
         * @deprecated
         */
	    NO_ERROR(20000),
	    METACLASS_NOT_FOUND(20001),
	    METAFIELD_NOT_FOUND(20002),
	    PARSE_GRAMMER_ERROR(20003),
	    REJECT_FULL_TABLE_SCAN(20004),
	    DIRTY_DATA_FORMAT(20005),
	    AGG_FIELD_IN_GROUP(20006),
	    AGG_WITHOUT_GROUP(20007),
	    AGG_FUNC_NOT_FOUND(20008),
	    MULTI_AGGR_FORBID(20009),
	    PROJECT_NON_AGGR(20010),
	    IILEGAL_PROJECTION(20011),
	    UNKNOWN_FIELD(20012),
	    SYNTAX_ERROR(20013),
	    TYPE_CAST_NOT_SUBMETA(20014),
	    INCOMPLETE_JOIN_QUERY(20015),
	    TOO_DEEP_EXECUTION(20016),
	    JOIN_COUNT_NOT_SUPPORT(20017),
	    AGGREGATION_MUST_BEFORE_SET(20018),
	    JOIN_SORT_NOT_SUPPORT(20019),
	    ARRAY_SORT_NOT_SUPPORT(20020),
	    JSON_SORT_NOT_SUPPORT(20021),
	    ROOT_LEVEL_JOIN_WITH_AGG(20022),
	    TOO_MANY_JOINED_COLLECTIONS(20023),
	    SYS_LIMIT_DOCUMENTS_MUST_POSITIVE(20024),
	    EXCEED_SYS_LIMIT_DOCUMENTS(20025),
	    INTERSECTION_ON_DIFFERENT_ROOT_METACLASS(20026),
        REVERSE_QUERY_ON_EMBED_NOT_SUPPORT(20027),
        AGG_COUNT_NOT_SUPPORT(20028),
        REJECT_REGEX_FULL_TABLE_SCAN(20029);
	    
	    QueryErrCodeEnum(int errorCode){
            this.errorCode = errorCode;
        }
    
        private int errorCode;
        public int getErrorCode(){
            return errorCode;
        }
	}
	
	private final QueryErrCodeEnum errorCodeEnum;

	protected QueryException(QueryErrCodeEnum errorCode, String query, Throwable t) {
		super("Execute query: " + query, t);
		this.errorCodeEnum = errorCode;
	}

	protected QueryException(QueryErrCodeEnum errorCode, String message) {
        super(message);
        this.errorCodeEnum = errorCode;
    }

    public int getErrorCode() {
        return errorCodeEnum.getErrorCode();
    }
}
