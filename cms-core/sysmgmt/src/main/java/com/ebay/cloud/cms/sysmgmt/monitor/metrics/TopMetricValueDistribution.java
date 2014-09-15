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
public class TopMetricValueDistribution implements IMetricValueDistribution {

    private SynchronousMinMaxHeap<MetricItem> record;
    private final int                         size;

    public TopMetricValueDistribution(int heapSize) {
        this.size = heapSize;
        this.record = new SynchronousMinMaxHeap<MetricItem>(size);
    }

    @Override
    public void addValue(long value, long timestamp, String detail) {
        int heapSize = record.size();
        MetricItem item = record.peekLast();
        if (heapSize == size && item.value > value) {
            //check whether to insert into the heap
            return;
        }

        MetricItem newItem = new MetricItem();
        newItem.timestamp = timestamp;
        newItem.value = value;
        newItem.detail = detail;

        record.offer(newItem);
    }

    @Override
    public void reset() {
        record.clear();
    }

    public SynchronousMinMaxHeap<MetricItem> getHeap() {
        return record;
    }
}
