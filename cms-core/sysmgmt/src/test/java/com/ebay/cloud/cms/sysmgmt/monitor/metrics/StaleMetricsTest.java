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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Copied from cms-datasync.
 * 
 * @author liasu
 * 
 */
public class StaleMetricsTest {

	@Test
	public void testMetrics() throws NumberFormatException, IOException {

		ApproximateTimeWindowMetric staleMetric = new ApproximateTimeWindowMetric("Manifest", "Manifest", 60, // period
																												// is
																												// 60
																												// seconds,
																												// we
				// actually do not care about
				// period because we call
				// snapshot ourselves
				1440 * 60,// time window is 1 day
				60, // seconds per bucket is 60 second
				60, // max scale is 60 seconds
				1 // slot count is 60, meaning 1 second per slot
		);
		InputStream resourceAsStream = StaleMetricsTest.class.getResourceAsStream("/staleMetrics.txt");
		BufferedReader input = new BufferedReader(new InputStreamReader(resourceAsStream));
		String str = null;
		staleMetric.initialize();
		while ((str = input.readLine()) != null) {
			long val = Long.valueOf(str);
			// System.out.println("val:"+val);
			Assert.assertTrue(val > 0);
			staleMetric.addValue(val, new Date().getTime(), null);
		}
		staleMetric.snapshot(new Date().getTime());
		System.out.println(staleMetric.getPercentileValue(90));
		Assert.assertTrue(staleMetric.getPercentileValue(90) > 0);

		System.out.println(staleMetric.getPercentileValue(50));
		Assert.assertTrue(staleMetric.getPercentileValue(50) > 0);

		System.out.println(staleMetric.getPercentileValue(0));

		System.out.println(staleMetric.getMaxValue());
		Assert.assertTrue(staleMetric.getMaxValue() > 0);

	}

	@Test
	public void test01Metrics() throws NumberFormatException, IOException {

		ApproximateTimeWindowMetric staleMetric = new ApproximateTimeWindowMetric("Manifest", "Manifest", 60, // period
				// is
				// 60
				// seconds,
				// we
				// actually do not care about
				// period because we call
				// snapshot ourselves
				1440 * 60,// time window is 1 day
				60, // seconds per bucket is 60 second
				Long.MAX_VALUE, // max scale is 60 seconds
				100 // slot count is 60, meaning 1 second per slot
		);
		InputStream resourceAsStream = StaleMetricsTest.class.getResourceAsStream("/staleMetrics.txt");
		BufferedReader input = new BufferedReader(new InputStreamReader(resourceAsStream));
		String str = null;
		staleMetric.initialize();
		while ((str = input.readLine()) != null) {
			long val = Long.valueOf(str);
			// System.out.println("val:"+val);
			Assert.assertTrue(val > 0);
			staleMetric.addValue(val, new Date().getTime(), null);
		}
		staleMetric.snapshot(new Date().getTime());
		System.out.println(staleMetric.getPercentileValue(90));
		Assert.assertTrue(staleMetric.getPercentileValue(90) > 0);

		System.out.println(staleMetric.getPercentileValue(50));
		Assert.assertTrue(staleMetric.getPercentileValue(50) > 0);

		System.out.println(staleMetric.getPercentileValue(0));

		System.out.println(staleMetric.getMaxValue());
		Assert.assertTrue(staleMetric.getMaxValue() > 0);

	}
}
