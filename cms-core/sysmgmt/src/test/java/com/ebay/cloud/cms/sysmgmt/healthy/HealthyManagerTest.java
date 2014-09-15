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

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.sysmgmt.IManagementServices.ServiceStatus;
import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;
import com.ebay.cloud.cms.utils.FileUtils;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class HealthyManagerTest extends CMSMongoTest {

    private static String expression;

    @BeforeClass
    public static void setUp() throws IOException {
        expression = FileUtils.readFile(HealthyTest.class.getResource("/healthExpression.js").getPath(), Charset
                .forName("utf-8"));
        
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testManager() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        
        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());
        facade.init();
        facade.startup();

        HealthyManager hm = new HealthyManager(facade, state, expression, 6 * 1000);

        hm.startup();
        Assert.assertEquals(ServiceStatus.running, hm.isRunning());
        facade.addMetricValue(MetricConstants.LATENCY_READ_ACSW_SUCCESS, 3000, System.currentTimeMillis(), null);

        hm.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, hm.isRunning());
    }

    @Test
    public void testImpedient() {
        HealthyManager hm = new HealthyManager(new MonitorStatisticsManager(getDataSource()), new CMSState(), expression, 6 * 1000);

        hm.startup();
        hm.startup();
        Assert.assertEquals(ServiceStatus.running, hm.isRunning());

        hm.shutdown();
        hm.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, hm.isRunning());
    }
    
    
}
