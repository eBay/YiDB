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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.utils.EqualsUtil;

/**
 * @author Liangfei(Ralph) Su
 *
 */
public class TopTimeWindowMetric extends ApproximateTimeWindowMetric {

    /**
     * Note: this class has a natural ordering that is inconsistent with equals
     */
    static class MetricItem implements Comparable<MetricItem> {
        public long   timestamp;
        public long    value;
        public String detail;

        @Override
        public int compareTo(MetricItem o) {
            if (this == o) {
                return 0;
            }
            return EqualsUtil.compare(this.value, o.value);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MetricItem)) {
                return false;
            }
            return (this.value == ((MetricItem) o).value) && (this.timestamp == ((MetricItem) o).timestamp)
                    && EqualsUtil.equal(this.detail, ((MetricItem) o).detail);
        }

        @Override
        public int hashCode() {
            return EqualsUtil.hashCode(value, timestamp, detail);
        }

        public String toString() {
            return detail;
        }
    }

    public TopTimeWindowMetric(String name, int period, long windowInSeconds, long bucketInSeconds, long maxScale,
            int slotCount) {
        super(name, null, period, windowInSeconds, bucketInSeconds, maxScale, slotCount);
    }

    @Override
    protected IMergedValueDistribution createMergedValueDistribution(long maxValue, int slotCount) {
        return new TopMergedValueDistribution(slotCount);
    }

    @Override
    protected TimedCountingBucket createTimeCountingBucket(long bucketMillis, long maxScale, int slotCount) {
        TimedCountingBucket bucket = new TopCountingBucket(bucketMillis, maxScale, slotCount);
        bucket.init();
        return bucket;
    }
    
    @Override
    public Map<String, Object> output() {
        int currentIndex = currentMergedIndex.get();
        TopMergedValueDistribution currentBucket = (TopMergedValueDistribution)mergedValues[currentIndex];
        Map<String, Object> outputMap = new HashMap<String, Object>();

        List<MetricItem> outputList = new ArrayList<MetricItem>();
        MetricItem[] metricItems = currentBucket.getHeap().toArray(new MetricItem[0]);
        for (MetricItem item : metricItems) {
            if (item != null) {
                outputList.add(item);
            }
        }
        //sort by weight for output
        Collections.sort(outputList);

        outputMap.put(getName(), outputList);
        return outputMap;
    }

}
