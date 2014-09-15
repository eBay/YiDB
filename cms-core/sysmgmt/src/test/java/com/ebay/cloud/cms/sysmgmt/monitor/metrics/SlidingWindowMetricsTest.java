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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.utils.EqualsUtil;

public class SlidingWindowMetricsTest {

	private static Logger logger = LoggerFactory.getLogger(SlidingWindowMetricsTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test01() {
		long startMillis = System.currentTimeMillis();
		long endMillis = startMillis + 3600L * 1000L;
		long deltaMillis = 20;
		int maxValue = 10000;
		TimedMetric[] testMetrics = generateMetrics(startMillis, endMillis, deltaMillis, maxValue);

		int windowSeconds = 3600;
		int bucketSeconds = 100;
		int slotNumber = 1000;
		AccurateTimeWindowMetric accWindow = new AccurateTimeWindowMetric("acc_metric_1", "qps_acc", null, 10, windowSeconds);
		ApproximateTimeWindowMetric appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100,
				windowSeconds, bucketSeconds, maxValue, slotNumber);
		appWindow.initialize();

		int testInterval = 1000;
		int size = testMetrics.length;
		for (int i = 0; i < size; i++) {
			TimedMetric metric = testMetrics[i];
			accWindow.addValue(metric.value, metric.timestamp, null);
			appWindow.addValue(metric.value, metric.timestamp, null);
			if (i % testInterval == 0) {
				long timestamp = System.currentTimeMillis();
				accWindow.snapshot(timestamp);
				appWindow.snapshot(timestamp);
				compareWindowMetrics(accWindow, appWindow, maxValue, slotNumber);
			}
			if (i % (10 * testInterval) == 0) {
				logger.info("left count = " + (size - i));
			}

		}

		Map<String, Object> accOutput = accWindow.output();
		Assert.assertNotNull(accOutput);
		Assert.assertEquals(5, accOutput.size());

		Map<String, Object> appOutput = appWindow.output();
		Assert.assertNotNull(appOutput);
		Assert.assertEquals(5, appOutput.size());
	}

	private void compareWindowMetrics(AccurateTimeWindowMetric accWindow, ApproximateTimeWindowMetric appWindow,
			int maxValue, int slotNumber) {

		int delta = maxValue / slotNumber;
		int percentile = 0;
		int errorCount = 0;
		while (percentile <= 100) {
			long accValue = accWindow.getPercentileValue(percentile);
			long appValue = appWindow.getPercentileValue(percentile);
			Assert.assertTrue(accValue <= accWindow.getMaxValue());
			Assert.assertTrue(appValue <= appWindow.getMaxValue());

			if (appValue < maxValue) {
				if (Math.abs(accValue - appValue) > delta * 100) {
					errorCount++;
				}
			} else {
				Assert.assertTrue(appValue >= maxValue);
			}
			percentile += 20;
		}
		Assert.assertEquals(errorCount, 5, 5);
	}

	private static class TimedMetric {
		public final long timestamp;
		public final long value;

		public TimedMetric(long timestamp, long value) {
			this.timestamp = timestamp;
			this.value = value;
		}
	}

	private static TimedMetric[] generateMetrics(long startMillis, long endMillis, long deltaMillis, int maxValue) {
		List<TimedMetric> metricsList = new ArrayList<TimedMetric>();
		long timestamp = startMillis;
		Random rand = new Random();
		while (timestamp < endMillis) {
			double d = Math.abs(rand.nextGaussian());
			int value = (int) (maxValue / 2 * d);
			TimedMetric metric = new TimedMetric(timestamp, value);
			metricsList.add(metric);
			timestamp += deltaMillis * d;
		}
		return metricsList.toArray(new TimedMetric[0]);
	}

	@Test
	public void test02InvalidInterval() {
		int maxValue = 10000;

		int windowSeconds = 0;
		int bucketSeconds = 100;
		int slotNumber = 1000;

		// 1. test snapshoting not starting case: should get 0 for max value,
		// qps
		AccurateTimeWindowMetric accWindow = new AccurateTimeWindowMetric("acc_metric_1", "qps_acc", null, 10, windowSeconds);
		Assert.assertTrue(accWindow.getMaxValue() == 0);
		Assert.assertTrue(accWindow.getQps() == 0);

		ApproximateTimeWindowMetric appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100,
				windowSeconds, bucketSeconds, maxValue, slotNumber);
		appWindow.initialize();
		Assert.assertTrue(appWindow.getMaxValue() == 0);
		Assert.assertTrue(appWindow.getQps() == 0);

		// 2. test snapshot after window flipping case: the data before the
		// window seconds, should be removed
		windowSeconds = 1;
		long now = System.currentTimeMillis();
		TimedMetric tm1 = new TimedMetric(now, 10), tm2 = new TimedMetric(now + 2000L, 5);
		accWindow = new AccurateTimeWindowMetric("acc_metric_1", "qps_acc", null, 10, 1);
		accWindow.addValue(tm1.value, tm1.timestamp, null);
		accWindow.snapshot(tm1.timestamp + 100);
		Assert.assertEquals(tm1.value, accWindow.getMaxValue());
		accWindow.addValue(tm2.value, tm2.timestamp, null);
		accWindow.snapshot(tm2.timestamp + 100);
		Assert.assertEquals(tm2.value, accWindow.getMaxValue());

		// 3. Can not call getPercentileValue() by given 0 and 100 to get the
		// 0th and max value
		try {
			accWindow.getPercentileValue(0);
		} catch (IllegalArgumentException e) {
			// expected illegal argument
		}

		// 4. Get max value of Approximate window should use getMaxValue() not
		// getPercentileValue(100)
		appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100, windowSeconds, bucketSeconds,
				maxValue, slotNumber);
		appWindow.initialize();
		try {
			appWindow.getPercentileValue(100);
			// Assert.fail();//not support percentile 100
		} catch (IllegalArgumentException e) {
			// expected illegal argument
		}

	}

	@Test
	public void test03FlipWindow() {
		long startMillis = System.currentTimeMillis();
		long endMillis = startMillis + 3600L * 1000L;
		long deltaMillis = 20;
		int maxValue = 10000;
		// metric sorted by time stamp
		TimedMetric[] testMetrics = generateMetrics(startMillis, endMillis, deltaMillis, maxValue);

		// make metric inverse sorting
		Arrays.sort(testMetrics, new Comparator<TimedMetric>() {
			@Override
			public int compare(TimedMetric object1, TimedMetric object2) {
				// reverse order
			    return EqualsUtil.compare(object2.timestamp, object1.timestamp);
			}
		});

		//
		int windowSeconds = 60;
		int bucketSeconds = 10;
		int slotNumber = 1000;

		ApproximateTimeWindowMetric appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100,
				windowSeconds, bucketSeconds, maxValue, slotNumber);
		appWindow.initialize();

		// int inWindowSize = 0;
		for (int i = 0; i < testMetrics.length; i++) {
			TimedMetric tm = testMetrics[i];
			appWindow.addValue(tm.value, tm.timestamp, null);
			// if ((endMillis - tm.timestamp) < ((windowSeconds - bucketSeconds)
			// * 1000L)) {
			// inWindowSize++;
			// }

			if (i > 0 && i % 1000 == 0) {
				appWindow.snapshot(tm.timestamp);
				Assert.assertTrue(appWindow.getQps() >= 0);
			}
			appWindow.getPercentileValue(95);
		}
		System.out.println(appWindow.getValueCount());
		// Assert.assertEquals(inWindowSize, appWindow.getValueCount());
	}

	@Test(expected = IllegalArgumentException.class)
	public void test04InvalidValue() {
		int maxValue = 10000;
		int windowSeconds = 60;
		int bucketSeconds = 10;
		int slotNumber = 1000;

		ApproximateTimeWindowMetric appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100,
				windowSeconds, bucketSeconds, maxValue, slotNumber);
		appWindow.initialize();

		appWindow.addValue(-1, System.currentTimeMillis(), null);
	}

	/**
	 * From data sync: maxvalue is not set as zero
	 */
	@Test
	public void test05MaxValueReset() {
		long startMillis = System.currentTimeMillis();
		long endMillis = startMillis + 3600L * 100L;
		long deltaMillis = 20;
		long maxValue = 3600L * 10L * 10L;
		TimedMetric[] testMetrics = fakeMetrics(startMillis, endMillis, deltaMillis, maxValue, 2);

		int windowSeconds = 360;
		int bucketSeconds = 10;
		int slotNumber = 1000;
		ApproximateTimeWindowMetric appWindow = new ApproximateTimeWindowMetric("app_metric_1", "qps_app", 100,
				windowSeconds, bucketSeconds, maxValue, slotNumber);
		appWindow.initialize();
		int testInterval = 1000;
		int size = testMetrics.length;

		int snapshotCount = 0;
		long lastSnapshoTime = 0;
		for (int i = 0; i < size; i++) {
			TimedMetric metric = testMetrics[i];
			if (i % testInterval == 0) {
				long timestamp = startMillis + i * deltaMillis;
				if (timestamp - startMillis > windowSeconds * 1000L) {
					appWindow.addValue(metric.value, metric.timestamp, null);

					// a flipnext() MUST happen in this snapshot
					long oldMax = appWindow.getMaxValue();
					appWindow.snapshot(timestamp);
					long newMax = appWindow.getMaxValue();
					System.out.println("i: " + i + ". oldMax: " + oldMax + ", new Max:" + newMax);
					Assert.assertTrue((oldMax == 0 && newMax == 0) || (newMax > oldMax));
					Assert.assertTrue(appWindow.getValueCount() < i);
					if (snapshotCount != 0 && (appWindow.getValueCount() == 0 || newMax == 0)) {
						Assert.fail();
					}
				} else {
					appWindow.addValue(metric.value, metric.timestamp, null);

					long oldMax = appWindow.getMaxValue();
					appWindow.snapshot(timestamp);
					long newMax = appWindow.getMaxValue();
					System.out.println("i: " + i + ". oldMax: " + oldMax + ", new Max:" + newMax);
				}

				lastSnapshoTime = timestamp;
				System.out.println("last snapshot time:" + lastSnapshoTime);
				snapshotCount++;
			} else {
				appWindow.addValue(metric.value, metric.timestamp, null);
			}
			if (i % (10 * testInterval) == 0) {
				logger.info("left count = " + (size - i));
			}
		}

		// make sure we could invalidate all metrics above
		long oldMax = appWindow.getMaxValue();
		Assert.assertTrue(oldMax > 0);
		Assert.assertTrue(appWindow.getValueCount() > 0);
		long lastRest = endMillis + windowSeconds * 1000L * 2;
		appWindow.snapshot(lastRest);
		Assert.assertTrue(appWindow.getMaxValue() == 0);
		Assert.assertTrue(appWindow.getValueCount() == 0);
	}

	private static TimedMetric[] fakeMetrics(long startMillis, long endMillis, long deltaMillis, long maxValue, int step) {
		List<TimedMetric> metricsList = new ArrayList<TimedMetric>();
		long timestamp = startMillis;
		Random rand = new Random();
		long base = 0;
		while (timestamp < endMillis) {
			double d = Math.abs(rand.nextGaussian());
			long value = (long) (step / 2 * d);
			value = value + base;
			TimedMetric metric = new TimedMetric(timestamp, value);
			metricsList.add(metric);

			base = base + step;
			timestamp += deltaMillis * d;
		}
		return metricsList.toArray(new TimedMetric[0]);
	}

}
