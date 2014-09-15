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

package com.ebay.cloud.cms.sysmgmt.throttling;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.utils.FileUtils;

public class ThrottlingTest {

    @Test
    public void testGetHealthyStatus() throws IOException {
        String expression = FileUtils.readFile(ThrottlingTest.class.getResource("/throttlingExpression.js").getPath(), Charset
                .forName("utf-8"));

        Throttling throttling = new Throttling(expression);

        // 1. no stat
        Assert.assertEquals(0, throttling.getThrottlingLevel(null));
        
        // 2. empty stat
        Map<String, Object> stat = new HashMap<String, Object>();
        Assert.assertEquals(0, throttling.getThrottlingLevel(stat));
        
        stat.put("currentTimestamp", System.currentTimeMillis());
        stat.put("fullGCTimestamp", System.currentTimeMillis() - 21 * 60000);
        stat.put("currentPercent", 0.9);
        stat.put("currentThrottlingLevel", 100);
        Assert.assertEquals(0, throttling.getThrottlingLevel(stat));
        
        stat.put("currentTimestamp", System.currentTimeMillis());
        stat.put("fullGCTimestamp", System.currentTimeMillis() - 10 * 60000);
        stat.put("currentPercent", 0.91);
        stat.put("currentThrottlingLevel", 100);
        Assert.assertEquals(10000, throttling.getThrottlingLevel(stat));
        
        stat.put("currentTimestamp", System.currentTimeMillis());
        stat.put("fullGCTimestamp", System.currentTimeMillis() - 19 * 60000);
        stat.put("currentPercent", 0.82);
        stat.put("currentThrottlingLevel", 100);
        Assert.assertEquals(100, throttling.getThrottlingLevel(stat));
        
        stat.put("currentTimestamp", System.currentTimeMillis());
        stat.put("fullGCTimestamp", System.currentTimeMillis() - 19 * 60000);
        stat.put("currentPercent", 0.79);
        stat.put("currentThrottlingLevel", 100);
        Assert.assertEquals(10, throttling.getThrottlingLevel(stat));
    }

}
