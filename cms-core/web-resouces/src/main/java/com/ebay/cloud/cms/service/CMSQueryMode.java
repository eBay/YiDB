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


/**
 * 
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

package com.ebay.cloud.cms.service;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public enum CMSQueryMode {
    NORMAL, URI, COUNT, SHOWALL;

    public static CMSQueryMode fromString(String mode) {
        CMSQueryMode ret = NORMAL;
        if (mode == null) {
            return ret;
        }

        for (CMSQueryMode qm : CMSQueryMode.values()) {
            if (qm.name().equalsIgnoreCase(mode)) {
                ret = qm;
                break;
            }
        }

        return ret;
    }
}
