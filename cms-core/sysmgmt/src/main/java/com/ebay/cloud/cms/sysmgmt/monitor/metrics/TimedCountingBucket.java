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

import java.util.concurrent.atomic.AtomicLong;

/**
 * timed bucket that keep the counting of different value distribution
 * 
 * @author xjiang
 *
 */
public class TimedCountingBucket {

    private final AtomicLong               startMillis;
    private final AtomicLong               endMillis;
    private final long                     bucketMillis;
    private IMetricValueDistribution valueDist;
    
    private final long maxScale;
    private final int slotCount;

    public TimedCountingBucket(long bucketMillis, long maxScale, int slotCount) {
        this.bucketMillis = bucketMillis;
        this.startMillis = new AtomicLong();
        this.endMillis = new AtomicLong();
        this.maxScale = maxScale;
        this.slotCount = slotCount;
    }

    protected IMetricValueDistribution createMetricValueDistribution(long maxScale, int slotCount) {
        return new MetricValueDistribution(maxScale, slotCount);
    }

    public final void addValue(long value, long timestamp, String detail) {
        valueDist.addValue(value, timestamp, detail);
    }

    public final int compareTo(long timestamp) {
        if (startMillis.get() == 0L) {
            long start = timestamp - timestamp % bucketMillis;
            while (startMillis.get() == 0L) {
                if (startMillis.compareAndSet(0L, start)) {
                    endMillis.set(start + bucketMillis);
                    break;
                }
            }
        }

        if (endMillis.get() <= timestamp) {
            return -1;
        }
        if (startMillis.get() > timestamp) {
            return 1;
        }
        return 0;
    }
    
    public final void reset(long start, long end) {
        startMillis.set(start);
        endMillis.set(end);
        valueDist.reset();
    }
    
    public long getStartTimestamp() {
        return startMillis.get();
    }
    
    public IMetricValueDistribution getValues() {
        return valueDist;
    }

    /*
     * Initialize the value distribution, should be called after constructor
     */
    protected void init() {
        this.valueDist = createMetricValueDistribution(maxScale, slotCount);
    }

}
