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

package com.ebay.cloud.cms.service.resources;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.impl.MonitorResource;

public class MonitorResourceTest extends CMSResourceTest {

    private MonitorResource monitorResource;

    @Before
    public void setupResource() {
        monitorResource = new MonitorResource();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGet() {
        CMSResponse response = monitorResource.getMetrics(nullMockUri, mockHttpRequest);

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
        System.out.println(result.get(0));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetString() {
        CMSResponse response = monitorResource.getMetric(nullMockUri, "latencyRead_1m", mockHttpRequest);

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
        System.out.println(result.get(0));
    }

    @Test(expected = NotFoundException.class)
    public void testGetInvalidMetric() {
        monitorResource.getMetric(nullMockUri, "latencyRead_1m__", mockHttpRequest);
    }

}
