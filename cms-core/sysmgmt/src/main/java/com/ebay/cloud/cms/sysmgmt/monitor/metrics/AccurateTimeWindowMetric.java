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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.ebay.cloud.cms.utils.CheckConditions;


/**
 * accurate time window algorithm
 * 
 * we use a queue to keep the metrics in the time window
 * 
 * @author xjiang
 *
 */
public class AccurateTimeWindowMetric extends AbstractTimeWindowMetric {
    
    protected static class TimedEntry {
        public long timestamp;
        public long value;
        
        public TimedEntry(long timestamp, long value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    protected final Queue<TimedEntry> windowedMetrics;
    protected long[] sortedMetrics;
    protected final long windowMillis;
    
    public AccurateTimeWindowMetric(String name, String qpmName, String qpsName, int period, long windowInSeconds) {
        super(name, qpmName, qpsName, period);
        sortedMetrics = new long[0];
        this.windowedMetrics = new ConcurrentLinkedQueue<TimedEntry>();
        this.windowMillis = windowInSeconds * 1000L;
    }
    
    @Override
    public void addValue(long value, long timestamp, String detail) {
        flipWindow(timestamp);
        TimedEntry newEntry = new TimedEntry(timestamp, value);
        windowedMetrics.add(newEntry);
    }
    
    @Override
    public long getPercentileValue(int percentile) {
        CheckConditions.checkArgument(percentile >= 0 && percentile <= 100, "percentile %s should be between 1 and 99", percentile);
        long[] tempMetrics = sortedMetrics;
        int offset = percentile * (tempMetrics.length - 1) / 100;

        long ret = 0;
        if (offset < tempMetrics.length) {
            ret = tempMetrics[offset];
        }
        return ret;
    }
    
    @Override
    public long getMaxValue() {       
        long[] tempMetrics = sortedMetrics;
        if (tempMetrics.length > 0) {
            return tempMetrics[tempMetrics.length - 1];
        } else {
            return 0;
        }
    }
    
    @Override
    public long getValueCount() {
        return sortedMetrics.length;
    }
    
    @Override
    public void snapshot(long timestamp) {
        flipWindow(timestamp);
        int size = windowedMetrics.size();
        long[] tempMetrics = new long[size];
        int index = 0;
        for (TimedEntry metric : windowedMetrics) {
            // check size as windowedMetrics is changing during snapshot
            if (index >= size) {
                break;
            }
            tempMetrics[index++] = metric.value;
        }
        Arrays.sort(tempMetrics);
        sortedMetrics = tempMetrics;
    }

    @Override
    public long getQps() {
        long count = getValueCount();
        int windowInSeconds = (int) (windowMillis / 1000L);
        if (windowInSeconds <= 0) {
            return count;
        }
        return count / windowInSeconds;
    }

    protected void flipWindow(long timestamp) {
        TimedEntry header = windowedMetrics.peek();
        while(header != null && header.timestamp < timestamp - windowMillis) {
            windowedMetrics.poll();
            header = windowedMetrics.peek();
        }
    }

	/* (non-Javadoc)
	 * @see com.ebay.cloud.cms.sysmgmt.monitor.metrics.AbstractTimeWindowMetric#getQpm()
	 */
	@Override
	public long getQpm() {
		long count = getValueCount();
        int windowInSeconds = (int) (windowMillis / 1000L);
        if (windowInSeconds <= 0) {
            return count;
        }
        return count * 60 / windowInSeconds;
	}
}
