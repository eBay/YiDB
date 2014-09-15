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

import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;


public class CMSStateHelper {
    public static void checkServerState(CMSState cmsState, CMSPriority p, boolean write) {
        switch (cmsState.getState()) {
        case startup:
        case shutdown:
        case severe:
        case maintain:
            throw new CannotServeException("Server state is " + cmsState.getState() + ", can't serve now");
        case critical:
        case overload:
            if (p == CMSPriority.CRITICAL) {
                return;
            }
            throw new CannotServeException("Server state is " + cmsState.getState() + ", can't serve now");
        case normal:
            // do nothing
            break;
        case readonly:
            if (write) {
                throw new CannotServeException("Server state is " + cmsState.getState() + ", can't serve write now");
            }
            break;
        default:
            throw new CannotServeException("unknown server state: " + cmsState.getState());
        }
    }
}
