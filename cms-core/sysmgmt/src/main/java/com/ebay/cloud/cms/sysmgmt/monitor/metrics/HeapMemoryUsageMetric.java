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


package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;

public class HeapMemoryUsageMetric implements IMonitorMetric {
    private static final double ceilingPercent = 0.85;
    private static final double floorPercent = 0.5;
    
    private long fullGCTimestamp;
    private final int period;
    private double lastPercent;
    private double currentPercent;
    
    public HeapMemoryUsageMetric(Integer period) {
        this.period = period;
        fullGCTimestamp = 0;
        lastPercent = 0;
        currentPercent = 0;
    }

    @Override
    public String getName() {
        return MetricConstants.JVM_HEAP_MEMORY_USAGE;
    }

    @Override
    public int getPeriod() {
        return period;
    }

    @Override
    public void snapshot(long timestamp) {
        lastPercent = currentPercent;

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();  
        MemoryUsage currentUsage = memory.getHeapMemoryUsage();
        currentPercent = (double)currentUsage.getUsed() / (double)currentUsage.getMax();
        
        if (lastPercent > ceilingPercent && currentPercent < floorPercent) {
            fullGCTimestamp = timestamp;
        } 
    }

    @Override
    public Map<String, Object> output() {
        //TODO: 
        Map<String, Object> metricsMap = new HashMap<String, Object>();
        metricsMap.put("fullGCTimestamp", fullGCTimestamp);
        metricsMap.put("currentTimestamp", System.currentTimeMillis());
        metricsMap.put("currentPercent", currentPercent);
        
        return metricsMap;
    }

}
