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

package com.ebay.cloud.cms.sysmgmt.healthy;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.ebay.cloud.cms.utils.FileUtils;

public class HealthyTest {

    @Test
    public void testGetHealthyStatus() throws IOException {
        String expression = FileUtils.readFile(HealthyTest.class.getResource("/healthExpression.js").getPath(), Charset
                .forName("utf-8"));

        Healthy healthy = new Healthy(expression);
        Map<String, Object> stat = new HashMap<String, Object>();
        stat.put("readqps_1m", 10l);
        stat.put("latencyRead_1m_p50", 50l);
        stat.put("writeqps_1m", 100l);
        stat.put("latencyWrite_1m_p50", 50l);
        stat.put("mongo_replMaster", "localhost");
        stat.put("mongo_databases", "[abc,def]");

        // 1. no stat
        Assert.assertEquals(0.6, healthy.getHealthyStatus(null), 0.01);
        // 2. empty stat
        Assert.assertEquals(0.2, healthy.getHealthyStatus(stat), 0.01);
        stat.put("latencyWrite_1m_p50", 2500l);
        // 3. with write latency, no read qps
        Assert.assertEquals(0.2, healthy.getHealthyStatus(stat), 0.01);

        stat.put("readqps_1m", 1000l);
        stat.put("latencyRead_1m_p50", 500l);
        stat.put("writeqps_1m", 1000l);
        stat.put("latencyWrite_1m_p50", 500l);
        // 4. with high write latency and high read qps
        Assert.assertEquals(0.9, healthy.getHealthyStatus(stat), 0.01);

        // 5. with normal write latency and normal read qps
        stat.put("latencyWrite_1m_p50", 100l);
        stat.put("readqps_1m", 500l);
        Assert.assertEquals(0.2, healthy.getHealthyStatus(stat), 0.01);

        stat.put("latencyWrite_1m_p50", 2500l);
        // 6. with high write latency, normal read qps
        Assert.assertEquals(0.9, healthy.getHealthyStatus(stat), 0.01);
        
        stat.put("latencyWrite_1m_p50", 2500l);
        stat.put("writeqps_1m", 2500l);
        Assert.assertEquals(0.95, healthy.getHealthyStatus(stat), 0.01);
        
        stat.put("latencyRead_1m_p50", 2500l);
        stat.put("readqps_1m", 2500l);
        Assert.assertEquals(0.95, healthy.getHealthyStatus(stat), 0.01);

        stat.put(MetricConstants.REPL_MASTER, "not found!");
        Assert.assertEquals(1.5, healthy.getHealthyStatus(stat), 0.01);
        
        stat.put(MetricConstants.REPL_DATABASES, "not found!");
        Assert.assertEquals(2.5, healthy.getHealthyStatus(stat), 0.01);
    }

}
