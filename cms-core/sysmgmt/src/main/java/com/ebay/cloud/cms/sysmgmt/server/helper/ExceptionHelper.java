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
package com.ebay.cloud.cms.sysmgmt.server.helper;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public class ExceptionHelper {
    public static boolean handleRuntimeException(RuntimeException re) {
        if (re instanceof CmsDalException) {
            CmsDalException cde = (CmsDalException) re;
            if (CMSServer.CMS_FAILURE_ERROR_CODES.contains(cde.getErrorEnum())) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
