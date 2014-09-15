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


/**
 * 
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

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.AccurateTimeWindowMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.HeapMemoryUsageMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IMonitorMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.MongoMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.QpsTimeWindowMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * Isolate initialization work
 * 
 * @author Liangfei(Ralph) Su
 * 
 */
public class MetricsRegister {

	private final MonitorStatisticsManager msm;

	public MetricsRegister(MonitorStatisticsManager msm) {
		super();

		CheckConditions.checkNotNull(msm);

		this.msm = msm;
	}

	public void initRegisterMetrics(MongoDataSource mongoDs) {
		CMSDBConfig dbConfig = new CMSDBConfig(mongoDs);

		IMonitorMetric metric = null;

		// ACSW read latency
		metric = new AccurateTimeWindowMetric(MetricConstants.LATENCY_READ_ACSW_SUCCESS,
				MetricConstants.READ_QPM_ACSW_SUCCESS, MetricConstants.READ_QPS_ACSW_SUCCESS, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.LATENCY_READ_ACSW_SUCCESS));
		msm.registMetric(metric);

		metric = new AccurateTimeWindowMetric(MetricConstants.LATENCY_READ_ACSW_FAILURE,
				MetricConstants.READ_QPM_ACSW_FAILURE, MetricConstants.READ_QPS_ACSW_FAILURE, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.LATENCY_READ_ACSW_FAILURE));
		msm.registMetric(metric);

		// ACSW write latency
		metric = new AccurateTimeWindowMetric(MetricConstants.LATENCY_WRITE_ACSW_SUCCESS,
				MetricConstants.WRITE_QPM_ACSW_SUCCESS, MetricConstants.WRITE_QPS_ACSW_SUCCESS, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.LATENCY_WRITE_ACSW_SUCCESS));
		msm.registMetric(metric);

		metric = new AccurateTimeWindowMetric(MetricConstants.LATENCY_WRITE_ACSW_FAILURE,
				MetricConstants.WRITE_QPM_ACSW_FAILURE, MetricConstants.WRITE_QPS_ACSW_FAILURE, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.LATENCY_WRITE_ACSW_FAILURE));
		msm.registMetric(metric);

		// qps
		metric = new QpsTimeWindowMetric(MetricConstants.READ_QPS_ACSW_ALL, null, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.READ_QPS_ACSW_ALL));
		msm.registMetric(metric);

		metric = new QpsTimeWindowMetric(MetricConstants.WRITE_QPS_ACSW_ALL, null, getPeriod(dbConfig),
				getMetricWindowsInSeconds(MetricConstants.WRITE_QPS_ACSW_ALL));
		msm.registMetric(metric);

		// top query - apsw
		long successWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_QUERY_TNSW_APSW_SUCCESS);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_QUERY_TNSW_APSW_SUCCESS, getTopQueryPeriod(dbConfig),
				successWindowInSeconds, successWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		long failureWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_QUERY_TNSW_APSW_FAILURE);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_QUERY_TNSW_APSW_FAILURE, getTopQueryPeriod(dbConfig),
				failureWindowInSeconds, failureWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		// top query - acsw
		successWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_QUERY_TNSW_ACSW_SUCCESS, getTopQueryPeriod(dbConfig),
				successWindowInSeconds, successWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		failureWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_QUERY_TNSW_ACSW_FAILURE);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_QUERY_TNSW_ACSW_FAILURE, getTopQueryPeriod(dbConfig),
				failureWindowInSeconds, failureWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		// top write - apsw
		successWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_WRITE_TNSW_APSW_SUCCESS);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_WRITE_TNSW_APSW_SUCCESS, getTopQueryPeriod(dbConfig),
				successWindowInSeconds, successWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		failureWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_WRITE_TNSW_APSW_FAILURE);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_WRITE_TNSW_APSW_FAILURE, getTopQueryPeriod(dbConfig),
				failureWindowInSeconds, failureWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		// top write - acsw
		successWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_WRITE_TNSW_ACSW_SUCCESS);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_WRITE_TNSW_ACSW_SUCCESS, getTopQueryPeriod(dbConfig),
				successWindowInSeconds, successWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		failureWindowInSeconds = getMetricWindowsInSeconds(MetricConstants.TOP_WRITE_TNSW_ACSW_FAILURE);
		metric = new TopTimeWindowMetric(MetricConstants.TOP_WRITE_TNSW_ACSW_FAILURE, getTopQueryPeriod(dbConfig),
				failureWindowInSeconds, failureWindowInSeconds / getTopQueryBucket(), getMetricMaxValue(dbConfig),
				getTopQueryMetricSize(dbConfig));
		((TopTimeWindowMetric) metric).initialize();
		msm.registMetric(metric);

		MongoMetric mongoMetric = new MongoMetric(mongoDs,
				(Integer) dbConfig.get(CMSDBConfig.MONGO_METRIC_SNAPSHOT_PERIOD),
				(Integer) dbConfig.get(CMSDBConfig.MONGO_METRIC_LISTDB_WAIT));
		msm.registMetric(mongoMetric);
		
	    // heap memory usage
        metric = new HeapMemoryUsageMetric((Integer) dbConfig.get(CMSDBConfig.HEAP_MEMORY_USAGE_CHECK_PERIOD));
        msm.registMetric(metric);

	}

	private int getTopQueryBucket() {
		return 5;
	}

	private int getTopQueryPeriod(CMSDBConfig dbConfig) {
		return (Integer) dbConfig.get(CMSDBConfig.TOP_QUERY_SNAPTSHOT_PERIOD);
	}

	private int getTopQueryMetricSize(CMSDBConfig dbConfig) {
		return (Integer) dbConfig.get(CMSDBConfig.TOP_QUERY_METRIC_SIZE);
	}

	/**
	 * 
	 * @param type
	 * @return - period in milliseconds
	 */
	private int getPeriod(CMSDBConfig dbConfig) {
		return (Integer) dbConfig.get(CMSDBConfig.TIME_WINDOW_SNAPSHOT_PERIOD);
	}

	private long getMetricWindowsInSeconds(String metricString) {
		String[] metricArray = metricString.split("_");
		long windowsInSeconds = 3600l * 24l;
		if (metricArray.length >= 2) {
			if ("24h".equalsIgnoreCase(metricArray[metricArray.length - 1])) {
				windowsInSeconds = 3600l * 24l;
			} else if ("1m".equalsIgnoreCase(metricArray[metricArray.length - 1])) {
				windowsInSeconds = 60l;
			}
		}
		return windowsInSeconds;
	}

	private int getMetricMaxValue(CMSDBConfig dbConfig) {
		return (Integer) dbConfig.get(CMSDBConfig.METRIC_MAX_VALUE);
	}

}
