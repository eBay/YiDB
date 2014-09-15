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

package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import com.ebay.cloud.cms.sysmgmt.monitor.metrics.TopTimeWindowMetric.MetricItem;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public class TopMergedValueDistribution implements IMergedValueDistribution {

    private SynchronousMinMaxHeap<MetricItem> record;

    public TopMergedValueDistribution(int heapSize) {
        int size = heapSize;
        record = new SynchronousMinMaxHeap<MetricItem>(size);
    }

    @Override
    public void reset(long timestamp) {
        record.clear();
    }

    @Override
    public void merge(IMetricValueDistribution metricDist) {
        TopMetricValueDistribution other = (TopMetricValueDistribution) metricDist;

        MetricItem[] metricItems = other.getHeap().toArray(new MetricItem[0]);
        for (MetricItem item : metricItems) {
            if (item != null) {
                record.offer(item);
            }
        }
    }

    @Override
    public void mergeDone() {
        // do nothing
    }

    @Override
    public long getValueCount() {
        return record.size();
    }

    @Override
    public long getMaxValue() {
        throw new UnsupportedOperationException("Get max value not supported in top N metric");
    }

    @Override
    public long getPercentile(int percentile) {
        throw new UnsupportedOperationException("Get percentile not supported in top N metric");
    }

    public SynchronousMinMaxHeap<MetricItem> getHeap() {
        return record;
    }

}
