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

import com.ebay.cloud.cms.utils.CheckConditions;


/**
 * value distribution of metrics value
 * 
 * @author xjiang
 *
 */
public class MetricValueDistribution extends AbstractValueDistribution implements IMetricValueDistribution {

    private final AtomicLong[] countArray;
    private final AtomicLong      maxValue;

    public MetricValueDistribution(long maxScale, int slotCnt) {
        super(maxScale, slotCnt);
        countArray = new AtomicLong[slotNum];
        maxValue = new AtomicLong();
        for (int i = 0; i < slotNum; i++) {
            countArray[i] = new AtomicLong();
        }
    }

    public final void addValue(long value, long timestamp, String detail) {
        CheckConditions.checkArgument(value >= 0);

        // increase count of right slot
        int slot = findSlot(value);

        countArray[slot].incrementAndGet();
        // change max value if necessary
        boolean success = false;
        while(!success) {
            long oldMax = maxValue.get();
            if (value > oldMax) {
                success = maxValue.compareAndSet(oldMax, value);
            } else {
                success = true;
            }
        }
    }

    public final long getCount(int index) {
        return countArray[index].get();
    }

    public final long getMaxValue() {
        return maxValue.get();
    }

    public final void reset() {
        for (AtomicLong count : countArray) {
            count.set(0);
        }
        maxValue.set(0);
    }

    private final int findSlot(long value) {
        long slot = value / slotWidth;
        if (slot >= slotNum) {
            slot = slotNum - 1;
        }
        return (int)slot;
    }

}
