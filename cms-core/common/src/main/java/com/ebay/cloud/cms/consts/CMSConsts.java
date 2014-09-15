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

package com.ebay.cloud.cms.consts;

public interface CMSConsts {

    // system db
    public static final String SYS_DB                 = "cms_sys_db";

    // property collection
    public static final String PROPERTIES_COLLECTION  = "cms_properties";

    // lock collection
    public static final String LOCK_COLLECTION        = "cms_lock";

    // sequence collection
    public static final String SEQUENCE_COLL          = "sequencies";
    public static final String NEXT_FIELD_NAME_SEQ    = "next_field_name";

    // repository collection
    public static final String REPOSITORY_COLL        = "repository";

    // metaclass collection
    public static final String METACLASS_COLL         = "metaclass";

    // metaclass history collection
    public static final String METACLASS_HISTORY_COLL = "metaclass_history";

    // branch collection
    public static final String BRANCH_DB_COLL_NAME    = "branch_info";
    
    // 64 characters at most. see mongodb limitation http://docs.mongodb.org/manual/reference/limits/
    public static final int MAX_LENGTH_OF_REPO_NAME   = 64;
    
    public static final int MAX_INDEXES_PER_META_CLASS   = 64;
    
    public static final int MAX_LENGTH_OF_INDEX_NAME   = 125;
    
    public static final int MAX_FIELDS_OF_COMPOUND_INDEX   = 31;
    
    public static final char[] INVALID_META_FIELD_NAME_CHARACTERS = {'$','.'};
    
    public static final char[] INVALID_META_CLASS_NAME_CHARACTERS = {'$'};
    
    public static final char[] INVALID_REPOSITORY_NAME_CHARACTERS = {'/','\\','.','\"'};
    
    public static final String TRACKING_CODE_KEY = "x-ebay-tracking-code";
    
}
