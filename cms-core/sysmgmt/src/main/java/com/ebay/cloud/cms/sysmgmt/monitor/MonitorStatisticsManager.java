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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.expression.factory.DaemonThreadFactory;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IMonitorMetric;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IRecordMetric;

/**
 * monitoring facade class
 * 
 * @author xjiang
 * 
 */
public class MonitorStatisticsManager implements IManagementServices {

	private static final Logger log = LoggerFactory.getLogger(MonitorStatisticsManager.class);

	private final Map<String, IMonitorMetric> metricsMap;

	private final MongoDataSource mongoDs;

	private ScheduledExecutorService scheduler;

	public MonitorStatisticsManager(MongoDataSource ds) {
		mongoDs = ds;
		metricsMap = new TreeMap<String, IMonitorMetric>();
	}

	public synchronized void registMetric(IMonitorMetric metric) {
		metricsMap.put(metric.getName(), metric);
		/**
		 *  The security service metrics are registered after scheduler already started, so the snapshot method will not be invoked.
		 *  work around is check if scheduler is executing then schedule metrcRunner one by one
		 */
		if(scheduler != null && !scheduler.isShutdown() && !scheduler.isTerminated()) {
			MetricRunner runner = new MetricRunner(metric);
			Random randDelay = new Random();
			long initialDelay = randDelay.nextInt(1000);
			long period = metric.getPeriod();
			scheduler.scheduleAtFixedRate(runner, initialDelay, period, TimeUnit.MILLISECONDS);
		}
	}

	public IMonitorMetric getMetric(String name) {
		return metricsMap.get(name);
	}

	public void addMetricValue(String metricName, int value, long timeInMills, String detail) {
		IMonitorMetric metric = getMetric(metricName);
		if (IRecordMetric.class.isInstance(metric)) {
			IRecordMetric editableMetric = IRecordMetric.class.cast(metric);
			editableMetric.addValue(value, timeInMills, detail);
		}
	}

	public Map<String, Object> getStatistics() {
		Map<String, Object> result = new TreeMap<String, Object>();

		for (IMonitorMetric metric : metricsMap.values()) {
			result.putAll(metric.output());
		}

		return result;
	}

	public JsonNode output() {
		ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
		for (Map.Entry<String, Object> entry : getStatistics().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			addJsonNode(jsonNode, key, value);
		}
		return jsonNode;
	}

	@SuppressWarnings("rawtypes")
	private void addJsonNode(ObjectNode jsonNode, String key, Object value) {
		if (value instanceof Integer) {
			jsonNode.put(key, (Integer) value);
		} else if (value instanceof String) {
			jsonNode.put(key, (String) value);
		} else if (value instanceof Long) {
			jsonNode.put(key, (Long) value);
		} else if (value instanceof List) {
			ArrayNode arrayNode = jsonNode.putArray(key);
			addListNode(arrayNode, (List) value);
		} else {
			jsonNode.put(key, String.valueOf(value));
		}
	}

	@SuppressWarnings("rawtypes")
	private void addListNode(ArrayNode parentListNode, List value) {
		for (Object val : value) {
			// not supporting list in list for now.
			POJONode pojoNode = parentListNode.POJONode(val);
			parentListNode.add(pojoNode);
		}
	}

	@Override
	public synchronized void init() {
		(new MetricsRegister(this)).initRegisterMetrics(this.mongoDs);
	}

	@Override
	public synchronized void startup() {
		if (scheduler == null) {
			scheduler = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.getInstance());
			Random randDelay = new Random();
			for (IMonitorMetric metric : metricsMap.values()) {
				MetricRunner runner = new MetricRunner(metric);
				long initialDelay = randDelay.nextInt(1000);
				long period = metric.getPeriod();
				scheduler.scheduleAtFixedRate(runner, initialDelay, period, TimeUnit.MILLISECONDS);
			}
		}
	}

	@Override
	public synchronized void shutdown() {
		if (scheduler != null) {
			List<Runnable> pendRunnig = scheduler.shutdownNow();
			log.debug(MessageFormat.format("System monitoring stopped,  {0} pending task in scheduler.",
					pendRunnig.size()));
			scheduler = null;
		}
	}

	@Override
	public synchronized ServiceStatus isRunning() {
		if (scheduler != null) {
			return ServiceStatus.running;
		} else {
			return ServiceStatus.stopped;
		}
	}

	// internal package method to get scheduler for test
	synchronized ScheduledExecutorService getScheduler() {
		return scheduler;
	}
}
