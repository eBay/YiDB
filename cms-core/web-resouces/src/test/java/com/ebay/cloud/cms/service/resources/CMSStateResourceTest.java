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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.resources.impl.CMSStateResource;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;

public class CMSStateResourceTest extends CMSResourceTest {

    private CMSStateResource stateResource;

    @Before
    public void setUpResource() {
        stateResource = new CMSStateResource();
    }

    @Test
    public void testGet() {
        CMSResponse response = stateResource.getStates(nullMockUri, mockHttpRequest);

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
    }

    @Test
    public void testChangeState() {
        stateResource.changeState(nullMockUri, loadJson("/maintain_state.json"), mockHttpRequest);

        CMSResponse response = stateResource.getStates(nullMockUri, mockHttpRequest);

        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());

        Assert.assertEquals(State.maintain, server.getState());

        stateResource.changeState(nullMockUri, loadJson("/normal_state.json"), mockHttpRequest);
        Assert.assertEquals(State.normal, server.getState());
    }

    @Test
    public void payloadcheck() {
        // case 1 : invalid json
        try {
            stateResource.changeState(nullMockUri, "{]", mockHttpRequest);
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }

        // case2: no state in payload
        try {
            stateResource.changeState(nullMockUri, "{}", mockHttpRequest);
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }

        // case2: invalid state
        try {
            stateResource.changeState(nullMockUri, "{\"state\": \"overlord\"}", mockHttpRequest);
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }

        // case3: manually change from normal to overload if not allowed
        try {
            stateResource.changeState(nullMockUri, "{\"state\": \"overload\"}", mockHttpRequest);
            Assert.fail();
        } catch (BadParamException e) {
            // expected
        }
    }
}
