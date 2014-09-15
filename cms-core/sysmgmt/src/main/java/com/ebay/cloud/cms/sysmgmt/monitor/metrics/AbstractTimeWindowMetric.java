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

package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * base class of sliding time window metrics
 * 
 * @author xjiang
 * 
 */
public abstract class AbstractTimeWindowMetric implements IRecordMetric {

	private final String name;
	private final String qpmName;
	private final String qpsName;
	private final int period;

	protected AbstractTimeWindowMetric(String name, String qpmName, String qpsName, int period) {
		this.name = name;
		this.period = period;
		this.qpmName = qpmName;
		this.qpsName = qpsName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPeriod() {
		return period;
	}

	@Override
	public Map<String, Object> output() {
		Map<String, Object> metricsMap = new HashMap<String, Object>();
		String perc50name = name + "_p50";
		metricsMap.put(perc50name, getPercentileValue(50));
		String perc95name = name + "_p95";
		metricsMap.put(perc95name, getPercentileValue(95));
		String perc99name = name + "_p99";
		metricsMap.put(perc99name, getPercentileValue(99));
		String percmaxname = name + "_max";
		metricsMap.put(percmaxname, getMaxValue());
		if (!StringUtils.isEmpty(qpmName)) {
			metricsMap.put(qpmName, getQpm());
		}
		if (!StringUtils.isEmpty(qpsName)) {
			metricsMap.put(qpsName, getQps());
		}
		return metricsMap;
	}

	public abstract void addValue(long val, long timestamp, String detail);

	public abstract long getPercentileValue(int percentile);

	public abstract long getMaxValue();

	public abstract long getValueCount();

	public abstract long getQps();

	public abstract long getQpm();

}
