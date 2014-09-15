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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.expression.factory.DaemonThreadFactory;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.CheckConditions;

public class ThrottlingManager implements IManagementServices {
    private static final Logger logger = LoggerFactory.getLogger(ThrottlingManager.class);
    
    private ScheduledExecutorService executor;
    private final int peroidMs;
    private final MonitorStatisticsManager monitorStatistics;
    private final CMSServer server;
    private Throttling throttling;
    
    private int      currentThrottlingLevel;
    private double   currentPercent;
    private long     fullGCTimestamp;

    public ThrottlingManager(CMSServer server, MonitorStatisticsManager monitorStatistics, String throttlingExpression, int peroidMs) {
        CheckConditions.checkNotNull(monitorStatistics, "monitorStatistics should not be null");
        this.server = server;
        this.monitorStatistics = monitorStatistics;
        this.throttling = new Throttling(throttlingExpression);
        this.peroidMs = peroidMs;
    }

    @Override
    public synchronized void init() {
        //do nothing
    }

    @Override
    public synchronized void startup() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.getInstance());
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        calculateMemoryUsage();
                    } catch (Throwable t) {
                        logger.error("exception during ThrottlingManager running", t);
                    }
                }
            }, 0, peroidMs, TimeUnit.MILLISECONDS);
        }
    }
    
    private void calculateMemoryUsage() throws Exception {
        boolean throttlingEnabled = (Boolean)server.getDBConfig().get(CMSDBConfig.THROTTLING_CHECK_ENABLED);
        
        if (throttlingEnabled) {
            Map<String, Object> stats = monitorStatistics.getStatistics();
            stats.put("currentThrottlingLevel", currentThrottlingLevel);
            
            currentPercent = (Double)stats.get("currentPercent");
            fullGCTimestamp = (Long)stats.get("fullGCTimestamp");
            
            if (throttling == null) {
                initThrottling();
            }
            
            currentThrottlingLevel = throttling.getThrottlingLevel(stats);
            
            if (currentThrottlingLevel > 0) {
                logger.info(String.format("Throttling manager info: Timestamp=%d;     ThrottlingLevel=%d;", System.currentTimeMillis(), currentThrottlingLevel));
                logger.info(String.format("Throttling manager info: GCTimestamp=%d;   MemoryUsedPercent=%f", fullGCTimestamp, currentPercent));
            }
        } else {
            throttling = null;
            currentThrottlingLevel = 0;
        }
        server.setThrottlingLevel(currentThrottlingLevel);
        
        logger.debug(String.format("Throttling manager debug: Timestamp=%d;   ThrottlingLevel=%d;   ThrottlingEnabled=%b", System.currentTimeMillis(), currentThrottlingLevel, throttlingEnabled));
        logger.debug(String.format("Throttling manager debug: GCTimestamp= %d;  MemoryUsedPercent=%f", fullGCTimestamp, currentPercent));
    }

    private void initThrottling() {
        CMSDBConfig dbConfig = server.getDBConfig();
        String throttlingExpression = (String) dbConfig.get(CMSDBConfig.THROTTLING_EXPRESSION);
        
        this.throttling = new Throttling(throttlingExpression);
        this.currentThrottlingLevel = 0;
    }

    @Override
    public synchronized void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    @Override
    public synchronized ServiceStatus isRunning() {
        if (executor != null) {
            return ServiceStatus.running;
        }
        else {
            return ServiceStatus.stopped;
        }
    }

    public double getCurrentPercent() {
        return currentPercent;
    }
    
    public long getFullGCTimestamp() {
        return fullGCTimestamp;
    }
    
    public int getCurrentThrottlingLevel() {
        return currentThrottlingLevel;
    }

    public void setCurrentThrottlingLevel(int currentThrottlingLevel) {
        this.currentThrottlingLevel = currentThrottlingLevel;
    }
}
