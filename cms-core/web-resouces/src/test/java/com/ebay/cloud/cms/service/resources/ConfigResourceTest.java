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

package com.ebay.cloud.cms.service.resources;

import org.junit.Test;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.resources.impl.ConfigResource;

/**
 * @author liasu
 * 
 */
public class ConfigResourceTest extends CMSResourceTest {

    private ConfigResource resource = new ConfigResource();

    @Test
    public void getConfig() {
        CMSResponse rep = resource.getConfigurations(nullMockUri, mockHttpRequest);
        assertOkAndNotNullResult(rep);
    }

    @Test
    public void setConfig() {
        CMSResponse resp = resource.setConfig(nullMockUri, "{\"SysAllowFullTableScan\":true}", mockHttpRequest);

        assertOk(resp);
    }
    
    @Test(expected = BadParamException.class)
    public void setConfig01() {
        resource.setConfig(nullMockUri, "{\"SysAllowFullTableScan\":\"true\"}", mockHttpRequest);
    }

    @Test(expected = BadParamException.class)
    public void invalidConfig() {
        CMSResponse resp = resource.setConfig(nullMockUri, "{\"invalid-key\" : \"1\"}", mockHttpRequest);
        assert400(resp);
    }
    
    @Test(expected = BadParamException.class)
    public void invalidConfig1() {
        CMSResponse resp = resource.setConfig(nullMockUri, "{]", mockHttpRequest);
        assert400(resp);
    }

}
