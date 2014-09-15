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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.sysmgmt.monitor.metrics.IMonitorMetric;

/**
 * metrics batch job runner
 * 
 * @author xjiang
 * 
 */
public class MetricRunner implements Runnable {

	private static final Logger	 log	= LoggerFactory.getLogger(MetricRunner.class);

	private final IMonitorMetric	metric;

	public MetricRunner(IMonitorMetric metric) {
		this.metric = metric;
	}

	public void run() {
		try {
		    long timestamp = System.currentTimeMillis();
			metric.snapshot(timestamp);
		} catch (Throwable e) {
			log.error(MessageFormat.format("Metric snapshot encounting error {0}", e.getMessage()), e);
		}
	}
}
