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

import java.util.Arrays;

/**
 * value distribution of merged value
 * 
 * @author xjiang
 * 
 */
public class MergedValueDistribution extends AbstractValueDistribution implements IMergedValueDistribution {

    private long[] countArray;
    private long   totalCount;
    private long   maxValue;

	public MergedValueDistribution(long maxVal, int slotCnt) {
		super(maxVal, slotCnt);
		countArray = new long[slotNum];
		totalCount = 0;
		maxValue = 0;
	}

	public final void reset(long timestamp) {
		for (int i = 0; i < countArray.length; i++) {
			countArray[i] = 0;
		}
		maxValue = 0;
		totalCount = 0;
	}

	public void merge(IMetricValueDistribution metricDist) {
        MetricValueDistribution other = (MetricValueDistribution) metricDist;
		for (int i = 0; i < countArray.length; i++) {
			long otherCount = other.getCount(i);
			countArray[i] += otherCount;
			totalCount += otherCount;
		}
		if (maxValue < other.getMaxValue()) {
			maxValue = other.getMaxValue();
		}
	}

	public void mergeDone() {
		for (int i = 1; i < countArray.length; i++) {
			long preCount = countArray[i - 1];
			countArray[i] += preCount;
		}
	}

	public long getPercentile(int percentile) {
	    if (totalCount == 0) {
	        return 0;
	    }

        // specical case : when percentile is 100, return the max value
        if (percentile == 100) {
            return getMaxValue();
        }

		long percCount = percentile * totalCount / 100 + 1;
		int percSlot = 0;
		// find the right slot of percentile value
		if (totalCount < slotNum) {
			for (percSlot = 0; percSlot < slotNum && percCount > countArray[percSlot]; percSlot++) {
			}

			if (percSlot == slotNum) {
				percSlot--;
			}
		} else {
			percSlot = Arrays.binarySearch(countArray, percCount);
			if (percSlot < 0) {
				percSlot = 0 - percSlot - 1;
			}
		}
		if (percSlot > 0 && (percCount < countArray[percSlot - 1] || percCount > countArray[percSlot])) {
			throw new ArithmeticException("percCount=" + percCount + " is not between" + countArray[percSlot - 1]
			        + " and " + countArray[percSlot]);
		}

		// set percentile value to start value of right slot
		long percValue = startArray[percSlot];

		// adjust percentile value by proportional
        long adjustCount = percCount;
        if (percSlot > 0) {
            adjustCount = adjustCount - countArray[percSlot - 1];
        }

        long valueRange = 0;
        if ((percValue + slotWidth > maxValue) || (percSlot == slotNum - 1)) {
            valueRange = maxValue - startArray[percSlot] - 1;
        } else {
            valueRange = startArray[percSlot + 1] - startArray[percSlot] - 1;
        }
        long countRange = countArray[percSlot];
        if (percSlot > 0) {
            countRange = countRange - countArray[percSlot - 1];
        }
        if (countRange != 0) {
            long adjustValue = valueRange * adjustCount / countRange;
            percValue += adjustValue;
        }
        //  debug code
//        if (percValue > maxValue) {
//            String error = "max=" + maxValue + ", perc=" + percValue;
//            error = error + ", slotNum=" + slotNum + ", percSlot=" + percSlot;
//            error = error + ", start=" + start_array[percSlot] + ", adjustCount=" + adjustCount;
//            error = error + ", valueRange=" + valueRange + ", countRange=" + countRange;
//            throw new RuntimeException(error);
//        }
		return percValue;
	}

	public long getMaxValue() {
		return maxValue;
	}

	public long getValueCount() {
		return totalCount;
	}

}
