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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.monitor.metrics.SynchronousMinMaxHeap.Link;
import com.ebay.cloud.cms.utils.EqualsUtil;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class TopNTimeWindowMetricTest extends CMSMongoTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testTopNTimeWinodwMetric() {
        // case 1: init set size as 10; add 10 + query metric; should only get
        // 10
        TopTimeWindowMetric topNMetric = generateTopQueryMetric();
        long timestamp = System.currentTimeMillis();
        topNMetric.snapshot(timestamp);

        Map<String, Object> outputMap = topNMetric.output();
        Assert.assertNotNull(outputMap);
        Assert.assertTrue(outputMap.get(topNMetric.getName()) instanceof List);
        Assert.assertTrue(((List) outputMap.get(topNMetric.getName())).size() == 10);
        Assert.assertEquals(topNMetric.getValueCount(), ((List) outputMap.get(topNMetric.getName())).size());

        // output test
        MonitorStatisticsManager msm = new MonitorStatisticsManager(getDataSource());
        msm.registMetric(topNMetric);
        JsonNode node = msm.output();
        System.out.println(node.toString());

    }

    private TopTimeWindowMetric generateTopQueryMetric() {
        long startMills = System.currentTimeMillis();
        long endMills = startMills + 2000l * 1000l;
        double amplitude = 0.5;
        long intervalMills = 1000;
        long valueBase = 15000;// 15s
        int requiredSize = 15;

        List<MetricItem> generatedMetrics = generateQueryMetric(startMills, endMills, valueBase, amplitude,
                intervalMills, requiredSize);
        Assert.assertTrue(generatedMetrics.size() > 10);
        System.out.println(generatedMetrics.size());

        TopTimeWindowMetric topNMetric = new TopTimeWindowMetric("top10Query", 1000, 10l * 600l, 600l,
                Integer.MAX_VALUE, 10);
        topNMetric.initialize();

        for (MetricItem mi : generatedMetrics) {
            topNMetric.addValue(mi.value, mi.timestamp, mi.detail);
        }
        return topNMetric;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTopQuryPercentileUnsupport() {
        TopTimeWindowMetric topNMetric = generateTopQueryMetric();
        topNMetric.getPercentileValue(50);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTopQuryMaxValueUnsupport() {
        TopTimeWindowMetric topNMetric = generateTopQueryMetric();
        topNMetric.getMaxValue();
    }

    private static class MetricItem implements Comparable<MetricItem> {
        public long   timestamp;
        public int    value;
        public String detail;

        @Override
        public String toString() {
            return detail;
        }

        @Override
        public int compareTo(MetricItem o) {
            return EqualsUtil.compare(this.value, o.value);
        }
    }

    private List<MetricItem> generateQueryMetric(long startMills, long endMills, long valueBase, double amplitude,
            long intervalMills, int requiredSize) {
        List<MetricItem> genedMetric = new ArrayList<MetricItem>();
        long start = startMills;
        Random rd = new Random();
        while (start <= endMills) {
            MetricItem mi = new MetricItem();
            mi.timestamp = start;
            mi.value = (int) (valueBase + new Double(valueBase * rd.nextDouble()).longValue());// hard-cast
                                                                                               // in
                                                                                               // unit
                                                                                               // test
                                                                                               // is
                                                                                               // ok:0
            mi.detail = "" + mi.timestamp;

            genedMetric.add(mi);

            start = start + intervalMills;
        }

        return genedMetric;
    }

    @Test
    public void testEnsureMetricItemCorrect() {
        com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric.MetricItem mi1 = new com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric.MetricItem();
        mi1.value = 5;
        mi1.timestamp = System.currentTimeMillis();

        com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric.MetricItem mi2 = new com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric.MetricItem();
        mi2.value = mi1.value;
        mi2.timestamp = System.currentTimeMillis() + 1;

        // metric item will be used in a map based heap
        // 1. have compareTo() based on value
        Assert.assertTrue(0 == mi1.compareTo(mi1));
        Assert.assertTrue(0 == mi1.compareTo(mi2));
        Assert.assertTrue(0 == mi2.compareTo(mi1));

        // 2. It must follow Object.equals() constraint
        Assert.assertTrue(mi1.equals(mi1));
        Assert.assertTrue(!mi1.equals(mi2));
        // when time, value, detail are the same, they are equal, and hashCode
        // must be the same
        mi2.value = mi1.value;
        mi2.timestamp = mi1.timestamp;
        Assert.assertTrue(mi1.equals(mi2));
        Assert.assertTrue(mi2.equals(mi1));
        Assert.assertTrue(mi1.hashCode() == mi2.hashCode());

        mi1.detail = "hello";
        mi2.detail = "hello1";
        Assert.assertTrue(mi1.equals(mi1));
        Assert.assertTrue(!mi1.equals(mi2));

        Assert.assertFalse(mi1.equals(null));
    }

    @Test
    public void testSyncHeapImpl() {
        SynchronousMinMaxHeap<MetricItem> heap = new SynchronousMinMaxHeap<MetricItem>(10);
        long startMills = System.currentTimeMillis();
        long endMills = startMills + 2000l * 1000l;
        double amplitude = 0.5;
        long intervalMills = 1000;
        long valueBase = 15000;// 15s
        int requiredSize = 15;

        List<MetricItem> generatedMetrics = generateQueryMetric(startMills, endMills, valueBase, amplitude,
                intervalMills, requiredSize);

        int i = 0;
        for (MetricItem mi : generatedMetrics) {
            heap.offer(mi);
            MetricItem newMi = new MetricItem();
            newMi.detail = mi.detail;
            newMi.timestamp = mi.timestamp;
            i++;
            if (i % 2 == 0) {
                newMi.value = (int) (mi.value * (new Random(System.currentTimeMillis()).nextFloat() * 2));
            } else {
                newMi.value = mi.value + 1;
            }

            if (newMi.value > mi.value) {
                System.out.println("genearte new mi with bigger value");
            }
            // offer again
            heap.offer(newMi);
        }
        Assert.assertEquals(10, heap.size());

        Map<String, Link<MetricItem>> itemMap = heap.getItemMap();
        Assert.assertEquals(itemMap.size(), heap.size());
        Link<MetricItem> voidLink = heap.getVoidLink();
        Link<MetricItem> p = voidLink.next;
        while (true) {
            Assert.assertNotNull(p.previous);
            Assert.assertNotNull(p.next);
            if (p == voidLink) {
                break;
            }
            Assert.assertTrue(itemMap.get(p.data.toString()) != null);
            Assert.assertTrue(itemMap.get(p.data.toString()) == p);
            Link<MetricItem> tmp = p;
            
            p = p.next;
            Assert.assertEquals(tmp, p.previous);
        }
    }
}
