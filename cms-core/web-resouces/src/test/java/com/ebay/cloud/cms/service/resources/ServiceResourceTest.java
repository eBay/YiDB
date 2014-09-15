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

import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.impl.ServiceResource;

/**
 * @author liasu
 * 
 */
public class ServiceResourceTest extends CMSResourceTest {

    private ServiceResource serviceResource;

    @Before
    public void setupResource() {
        serviceResource = new ServiceResource();
    }

    @Test
    public void testGetServices() {
        CMSResponse resp = serviceResource.getServices(nullMockUri, mockHttpRequest);
        assertOkAndNotNullResult(resp);
    }

    @Test
    public void testGetService() {
        CMSResponse resp = serviceResource.getService(nullMockUri, "monitorService", mockHttpRequest);
        assertOkAndNotNullResult(resp);

        try {
            serviceResource.getService(nullMockUri, "monitorService-invalid", mockHttpRequest);
        } catch (NotFoundException e) {
            // expected
        }
    }

    @Test
    public void setStatus() {
        // case 0: set to maintain
        String maintain = loadJson("/monitorService_stopped.json");
        serviceResource.setStatus(nullMockUri, "monitorService", maintain, mockHttpRequest);

        String normal = loadJson("/monitorService_running.json");
        serviceResource.setStatus(nullMockUri, "monitorService", normal, mockHttpRequest);

        // case1: invalid payload
        try {
            serviceResource.setStatus(nullMockUri, "monitorService", "{]", mockHttpRequest);
        } catch (BadParamException e) {
            // expected
        }

        // case2: invalid service
        try {
            serviceResource.setStatus(nullMockUri, "monitorService-invalid", normal, mockHttpRequest);
        } catch (NotFoundException e) {
            // expected
        }

        // case3: invalid service status
        try {
            serviceResource.setStatus(nullMockUri, "monitorService", "{\"monitorService\":\"runing\"}", mockHttpRequest);
        } catch (BadParamException e) {
            // expected
        }

        // case4: invalid service and payload mapping
        try {
            serviceResource.setStatus(nullMockUri, "healthyService", normal, mockHttpRequest);
        } catch (BadParamException e) {
            // expected
        }
    }

}
