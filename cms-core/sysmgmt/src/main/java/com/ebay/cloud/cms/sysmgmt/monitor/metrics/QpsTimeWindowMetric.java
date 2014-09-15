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


/*******************************************************************************
 * Copyright (c) 2012-2013 eBay Inc.
 * All rights reserved. 
 *  
 * eBay PE Cloud Foundation Team [DL-eBay-SHA-COE-PE-Cloud-Foundation@ebay.com]
 *******************************************************************************/
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

package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shuachen
 *
 * 2013-10-23
 */
public class QpsTimeWindowMetric extends AccurateTimeWindowMetric {

    public QpsTimeWindowMetric(String name, String qpmName, int period, long windowInSeconds) {
        super(name, qpmName, null, period, windowInSeconds);
    }

    /**
     * Provide different value: for each datapoint, store the QPS at that time.
     */
    @Override
    public final void addValue(long value, long timestamp, String detail) {
        flipWindow(timestamp);
        long qps = getQps();
        TimedEntry newEntry = new TimedEntry(timestamp, qps);
        windowedMetrics.add(newEntry);
    }

    @Override
    public Map<String, Object> output() {
        Map<String, Object> metricsMap = new HashMap<String, Object>();
        String percmaxname = getName() + "_max";
        metricsMap.put(percmaxname, getMaxValue());
        return metricsMap;
    }

}
