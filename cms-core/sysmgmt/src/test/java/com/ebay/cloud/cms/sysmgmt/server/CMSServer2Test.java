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

package com.ebay.cloud.cms.sysmgmt.server;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class CMSServer2Test {

    @Test
    public void test() throws Exception {

        CMSServer server = CMSServer.getCMSServer();
        Field f = CMSServer.class.getDeclaredField("cmsServer");
        f.setAccessible(true);
        f.set(server, null);

        Map<String, String> map = new HashMap<String, String>();
        map.put(CMSConfig.MONGO_CONNECTION, "localhodddst:27017");
        map.put(CMSConfig.SERVER_NAME, "localCMSServer");
        map.put(CMSConfig.CMS_HOME, "~/CMS_HOME/");

        try {
            CMSServer.getCMSServer(map);
            Assert.fail();
            // Assert.assertTrue(server != newServer);
        } catch (Exception e) {
            // expected
        }
        map.put(CMSConfig.MONGO_CONNECTION, "localhost:27017");
        CMSServer.getCMSServer(map);

    }
}
