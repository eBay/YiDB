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
package com.ebay.cloud.cms.sysmgmt.server.helper;

import com.ebay.cloud.cms.sysmgmt.monitor.MetricConstants;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;

public class MonitorHelper {
    public static void addReadRequest(MonitorStatisticsManager monitor, int value, long timestamp, String detail, boolean isSuccess) {
        monitor.addMetricValue(MetricConstants.READ_QPS_ACSW_ALL, value, timestamp, detail);
        if (isSuccess) {
            monitor.addMetricValue(MetricConstants.LATENCY_READ_ACSW_SUCCESS, value, timestamp, detail);
            monitor.addMetricValue(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS, value, timestamp, detail);
            monitor.addMetricValue(MetricConstants.TOP_QUERY_TNSW_APSW_SUCCESS, value, timestamp, detail);
        } else {
            monitor.addMetricValue(MetricConstants.LATENCY_READ_ACSW_FAILURE, value, timestamp, detail);
            monitor.addMetricValue(MetricConstants.TOP_QUERY_TNSW_ACSW_FAILURE, value, timestamp, detail);
            monitor.addMetricValue(MetricConstants.TOP_QUERY_TNSW_APSW_FAILURE, value, timestamp, detail);
        }
    }

    public static void addWriteRequest(MonitorStatisticsManager monitor, int value, long timestamp, Object detail, boolean isSuccess) {
        int maxLengthOfWritePayloadForMonitors = 512;
        String detailString = null;
        if (detail != null) {
            detailString = detail.toString();
            if (detailString.length() > maxLengthOfWritePayloadForMonitors) {
                detailString = detailString.substring(0, maxLengthOfWritePayloadForMonitors);
            }
        }
        String msg = "";
        if (detail != null) {
            msg = detail.toString();
        }
        monitor.addMetricValue(MetricConstants.WRITE_QPS_ACSW_ALL, value, timestamp, msg);
        if (isSuccess) {
            monitor.addMetricValue(MetricConstants.LATENCY_WRITE_ACSW_SUCCESS, value, timestamp, detailString);
            monitor.addMetricValue(MetricConstants.TOP_WRITE_TNSW_ACSW_SUCCESS, value, timestamp, detailString);
            monitor.addMetricValue(MetricConstants.TOP_WRITE_TNSW_APSW_SUCCESS, value, timestamp, detailString);
        } else {
            monitor.addMetricValue(MetricConstants.LATENCY_WRITE_ACSW_FAILURE, value, timestamp, detailString);
            monitor.addMetricValue(MetricConstants.TOP_WRITE_TNSW_ACSW_FAILURE, value, timestamp, detailString);
            monitor.addMetricValue(MetricConstants.TOP_WRITE_TNSW_APSW_FAILURE, value, timestamp, detailString);
        }
    }
}
