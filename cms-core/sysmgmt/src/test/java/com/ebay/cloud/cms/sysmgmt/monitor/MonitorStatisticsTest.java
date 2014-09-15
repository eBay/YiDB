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

package com.ebay.cloud.cms.sysmgmt.monitor;

import static com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants.LATENCY_READ_ACSW_SUCCESS;
import static com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants.LATENCY_WRITE_ACSW_SUCCESS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.sysmgmt.IManagementServices.ServiceStatus;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IMonitorMetric;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MonitorStatisticsTest extends CMSMongoTest {

    private static Logger logger = LoggerFactory.getLogger(MonitorStatisticsTest.class);

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testInitRegisterMetrics() {

        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());
        facade.init();
        
        facade.startup();
        Assert.assertEquals(ServiceStatus.running, facade.isRunning());

        IMonitorMetric metric = facade.getMetric(LATENCY_READ_ACSW_SUCCESS);
        Assert.assertNotNull(metric);
        facade.addMetricValue(LATENCY_READ_ACSW_SUCCESS, 1, System.currentTimeMillis(), null);

        metric = facade.getMetric(LATENCY_READ_ACSW_SUCCESS);
        Assert.assertNotNull(metric);
        facade.addMetricValue(LATENCY_READ_ACSW_SUCCESS, 1, System.currentTimeMillis(), null);

        metric = facade.getMetric(LATENCY_WRITE_ACSW_SUCCESS);
        Assert.assertNotNull(metric);
        facade.addMetricValue(LATENCY_WRITE_ACSW_SUCCESS, 1, System.currentTimeMillis(), null);

        metric = facade.getMetric(LATENCY_WRITE_ACSW_SUCCESS);
        Assert.assertNotNull(metric);
        facade.addMetricValue(LATENCY_WRITE_ACSW_SUCCESS, 1, System.currentTimeMillis(), null);
        
        metric = facade.getMetric(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS);
        Assert.assertNotNull(metric);
        facade.addMetricValue(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS, 10, System.currentTimeMillis(), "query....");

//        metric = facade.getMetric(LONG_QUERY_ACSW);
//        Assert.assertNotNull(metric);
//        System.out.println(Objects.toStringHelper(metric).toString());

        long snapshotTime = System.currentTimeMillis();
        //force snapshot
        facade.getMetric(LATENCY_READ_ACSW_SUCCESS).snapshot(snapshotTime);
        facade.getMetric(LATENCY_READ_ACSW_SUCCESS).snapshot(snapshotTime);
        facade.getMetric(LATENCY_WRITE_ACSW_SUCCESS).snapshot(snapshotTime);
        facade.getMetric(LATENCY_WRITE_ACSW_SUCCESS).snapshot(snapshotTime);
        facade.getMetric(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS).snapshot(snapshotTime);
        
        //there must be some metric value snapshoted already
        Assert.assertNotNull(facade.getStatistics().get(LATENCY_READ_ACSW_SUCCESS+ "_p95"));
        
        Assert.assertNotNull(facade.output());
        
        
        IMonitorMetric mongoMetric = facade.getMetric(MetricConstants.MONGO_METRIC);
        Assert.assertNotNull(mongoMetric.output());
        System.out.println(mongoMetric.output());
        
        facade.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, facade.isRunning());
    }
    
    @Test
    public void testAddInvalidMetricValue() {
        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());
        facade.init();
        
        facade.startup();
        Assert.assertEquals(ServiceStatus.running, facade.isRunning());
        
        //add non existing metric value, will be ignored, no exception
        facade.addMetricValue("non-metric", 0, 0, null);

        Assert.assertEquals(42, facade.getStatistics().size());
        Assert.assertFalse(facade.getStatistics().containsKey("non-metric"));
        
        facade.shutdown();
        Assert.assertEquals(ServiceStatus.stopped, facade.isRunning());
    }

    @Test
    public void testDaemonThreadStart() throws InterruptedException, ExecutionException {
        MonitorStatisticsManager facade = new MonitorStatisticsManager(getDataSource());

        facade.init();

        facade.startup();

        final CountDownLatch latch = new CountDownLatch(1);

        Runnable run = new Runnable() {

            @Override
            public void run() {
                logger.debug("In sumbitted running ");
                latch.countDown();
            }
        };

        final Object result = null;
        Future<Object> f = facade.getScheduler().submit(run, result);
        Assert.assertNotNull(f);
        try {
            f.get(300, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            logger.error("time out", e);
            Assert.fail("Schduler doesn't run the schdule object in-time");
        }

        latch.await(300, TimeUnit.SECONDS);

        facade.shutdown();
    }

}
