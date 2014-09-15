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
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.sysmgmt.IManagementServices.ServiceStatus;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;
import com.ebay.cloud.cms.utils.FileUtils;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class ThrottlingManagerTest extends CMSMongoTest {

    private static String expression;

    @BeforeClass
    public static void setUp() throws IOException {
        expression = FileUtils.readFile(ThrottlingTest.class.getResource("/throttlingExpression.js").getPath(), Charset
                .forName("utf-8"));
        
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testManager() throws InterruptedException {
        CMSState state = new CMSState();
        state.setState(State.normal);
        
        CMSServer server = CMSServer.getCMSServer();
        CMSDBConfig dbConfig = server.getDBConfig();
        Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
        Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
        // change config for temp
        newConfig.put(CMSDBConfig.HEAP_MEMORY_USAGE_CHECK_PERIOD, Integer.valueOf(100));
        dbConfig.updateConfig(newConfig);

        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());
        facade.init();
        facade.startup();

        ThrottlingManager tm = new ThrottlingManager(server, facade, expression, 100);
        tm.startup();
        Assert.assertEquals(ServiceStatus.running, tm.isRunning());
        
        Map<String, Object> entityMap = new HashMap<String, Object>();
        allocateMemory(1000000, entityMap);
        
        Thread.sleep(500);
        double currentPercent = getCurrentPercent();
        double tmPercent = tm.getCurrentPercent();
        Assert.assertEquals(currentPercent, tmPercent, 0.02);
            
        entityMap.clear();
        System.gc();
        
        Thread.sleep(500);
        currentPercent = getCurrentPercent();
        tmPercent = tm.getCurrentPercent();
        Assert.assertEquals(currentPercent, tmPercent, 0.02);
        
        tm.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, tm.isRunning());
    }

    private double getCurrentPercent() {
        MemoryMXBean memoryUsage = ManagementFactory.getMemoryMXBean();  
        MemoryUsage currentUsage = memoryUsage.getHeapMemoryUsage();
        double currentPercent = (double)currentUsage.getUsed() / (double)currentUsage.getMax();
        return currentPercent;
    }

    private void allocateMemory(int size, Map<String, Object> entityMap) {
        for (long index = 0; index < size; ++index) {
            Object value = new byte[1024];
            String key = Long.valueOf(index).toString();
            entityMap.put(key, value);
        }
    }

    @Test
    public void testImpedient() {
        CMSServer server = CMSServer.getCMSServer();
        ThrottlingManager tm = new ThrottlingManager(server, new MonitorStatisticsManager(getDataSource()), expression, 60 * 1000);

        tm.startup();
        tm.startup();
        Assert.assertEquals(ServiceStatus.running, tm.isRunning());

        tm.shutdown();
        tm.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, tm.isRunning());
    }
    
    @Test
    public void testJSExprReload() throws InterruptedException {
        CMSServer server = CMSServer.getCMSServer();
        CMSDBConfig dbConfig = server.getDBConfig();
        Map<String, Object> currentConfig = dbConfig.getCurrentConfiguration();
        Map<String, Object> newConfig = new HashMap<String, Object>(currentConfig);
        // change config for temp
        newConfig.put(CMSDBConfig.HEAP_MEMORY_USAGE_CHECK_PERIOD, Integer.valueOf(100));
        dbConfig.updateConfig(newConfig);
        
        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());
        facade.init();
        facade.startup();

        ThrottlingManager tm = new ThrottlingManager(server, facade, expression, 100);
        tm.startup();
        Assert.assertEquals(ServiceStatus.running, tm.isRunning());
        
        // disable throttling enabled
        currentConfig = dbConfig.getCurrentConfiguration();
        newConfig = new HashMap<String, Object>(currentConfig);
        
        tm.setCurrentThrottlingLevel(100);
        newConfig.put(CMSDBConfig.THROTTLING_CHECK_ENABLED, Boolean.FALSE);
        dbConfig.updateConfig(newConfig);
        Thread.sleep(500);
        Assert.assertEquals(0, tm.getCurrentThrottlingLevel());
        
        // update JS Expression
        currentConfig = dbConfig.getCurrentConfiguration();
        newConfig.put(CMSDBConfig.THROTTLING_CHECK_ENABLED, Boolean.TRUE);
        newConfig.put(CMSDBConfig.THROTTLING_EXPRESSION, "throttlingLevel = 10");
        dbConfig.updateConfig(newConfig);
        Thread.sleep(500);
        Assert.assertEquals(10, tm.getCurrentThrottlingLevel());
    }
    
}
