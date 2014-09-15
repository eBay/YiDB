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

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.utils.CheckConditions;



/**
 * high performance approximate algorithm for sliding time window 
 * 
 * 1. divide time window into buckets
 * 2. each bucket contains a metrics distribution array.
 * 3. metrics distribution array contains 
 * 
 * @author xjiang
 *
 */
public class ApproximateTimeWindowMetric extends AbstractTimeWindowMetric {
    
    private static final Logger logger = LoggerFactory.getLogger(ApproximateTimeWindowMetric.class);

    protected static final int EXTRA_BUCKET = 1;
     
    
    protected final long                  bucketMillis;
    protected final int                   bucketNum;
    protected final AtomicInteger         currBktIdx;       
    protected final TimedCountingBucket[] samplingBuckets;
    protected final long                  windowMillis;
    private final long                     maxScale;
    private final int                     slotCount;
    
    protected IMergedValueDistribution[]  mergedValues;
    protected AtomicInteger               currentMergedIndex;
    
    public ApproximateTimeWindowMetric(String name, String qpmName, int period, long windowInSeconds, long bucketInSeconds, long maxScale, int slotCount) {
        super(name, qpmName, null, period);
        this.bucketMillis       = bucketInSeconds * 1000L;
        this.windowMillis       = windowInSeconds * 1000L;
        this.bucketNum          = (int)(windowInSeconds / bucketInSeconds) + EXTRA_BUCKET;
        this.samplingBuckets    = new TimedCountingBucket[bucketNum];        
        this.currBktIdx         = new AtomicInteger();
		this.mergedValues       = new IMergedValueDistribution[2];
        this.maxScale = maxScale;
        this.slotCount = slotCount;
    }

    public void initialize() {
        for (int i =0; i < 2; i++) {
		    mergedValues[i] = createMergedValueDistribution(maxScale, slotCount);
		}
		this.currentMergedIndex = new AtomicInteger(0);
        for (int i = 0; i < bucketNum; i++) {
            samplingBuckets[i] = createTimeCountingBucket(bucketMillis, maxScale, slotCount);
        }
    }

    protected TimedCountingBucket createTimeCountingBucket(long bucketMillis, long maxScale, int slotCount) {
        TimedCountingBucket bucket = new TimedCountingBucket(bucketMillis, maxScale, slotCount);
        bucket.init();
        return bucket;
    }

    protected IMergedValueDistribution createMergedValueDistribution(long maxValue, int slotCount) {
        return new MergedValueDistribution(maxValue, slotCount);
    }

    public void snapshot(long timestamp) {
        flipBucket(timestamp);
        int index = currBktIdx.get();
        //Do merge on next merge index while currentMergeIndex still serve for query. 
        int currentMergeIndex = currentMergedIndex.get();
        int nextMergeIndex = ((currentMergeIndex + 1) < mergedValues.length) ? (currentMergeIndex + 1)
                : (currentMergeIndex + 1 - mergedValues.length);

        int count = 0;
        IMergedValueDistribution nextMergeDist = mergedValues[nextMergeIndex];
        nextMergeDist.reset(timestamp);
        //merge backward
        while (count < (bucketNum - EXTRA_BUCKET)) {
            TimedCountingBucket bucket = samplingBuckets[index];
            merge(nextMergeDist, bucket);
            index = backward(index);
            count++;
        }
        nextMergeDist.mergeDone();

        // Move currentMergeIndex after merge done.
        setAtomicValue(currentMergedIndex, nextMergeIndex);
    }

    protected void merge(IMergedValueDistribution nextMergeDist, TimedCountingBucket bucket) {
        nextMergeDist.merge(bucket.getValues());
    }

    private void setAtomicValue(AtomicInteger ai, int targetValue) {
        boolean success = false;
        while(!success) {
            int oldValue = ai.get();
            success = ai.compareAndSet(oldValue, targetValue);
        }
    }

    @Override
    public long getPercentileValue(int percentile) {        
        CheckConditions.checkArgument(percentile >= 0 && percentile <= 100, "percentile %s should be between 0 and 100", percentile);
        int curMergedIndex = currentMergedIndex.get();
        return mergedValues[curMergedIndex].getPercentile(percentile);
    }
    
    @Override
    public long getMaxValue() {
        int curMergedIndex = currentMergedIndex.get();
        return mergedValues[curMergedIndex].getMaxValue();
    }
    
    @Override
    public long getValueCount() {
        int curMergedIndex = currentMergedIndex.get();
        return mergedValues[curMergedIndex].getValueCount();
    }
    
    private final synchronized TimedCountingBucket flipBucket(long timestamp) {
        // double-check the current index
        int index = currBktIdx.get();
        TimedCountingBucket bucket = samplingBuckets[index];
        int comp = bucket.compareTo(timestamp);
        if (comp == 0) {
            return bucket;
        } else if (comp > 0) {
            return findBackward(timestamp, index);
        }
        // flip the current index to next
        long startMillis = bucket.getStartTimestamp();
        while (true) {
            index = forward(index);
            bucket = samplingBuckets[index];
            startMillis = startMillis + bucketMillis;
            bucket.reset(startMillis, startMillis + bucketMillis);
            if (bucket.compareTo(timestamp) == 0) {
                currBktIdx.set(index);
                return bucket;
            }
        }
    }
        
    private final TimedCountingBucket findBackward(long timestamp, int startIndex) {
        int index = startIndex;
        TimedCountingBucket bucket = null;
        while (true) {
            index = backward(index);
            // reach the start-point
            if (index == startIndex) {
                return null;
            }
            bucket = samplingBuckets[index];
            if (bucket.compareTo(timestamp) == 0) {
                // find the right bucket
                return bucket;
            } else if (bucket.compareTo(timestamp) < 0) {
                // reach the start of cyclic array
                logger.warn(MessageFormat.format("could not find backward for timestamp {0}", timestamp));
                return null;
            }
            // else: bucket is still bigger than timestamp, keep go backward
        }
    }
    
    private final int forward(int index) {
        return (index + 1) % bucketNum; 
    }
    
    private final int backward(int index) {
        return (index + bucketNum - 1 ) % bucketNum; 
    }

    @Override
    public final void addValue(long val, long timestamp, String detail) {
        TimedCountingBucket bucket = flipBucket(timestamp);
        if (bucket != null) {
            bucket.addValue(val, timestamp, detail);
        }
    }
    
    @Override
    public long getQps() {
        long count = getValueCount();
        int windowInSeconds = (int) (this.windowMillis / 1000L);
        if (windowInSeconds <= 0) {
            return count;
        }
        return count / windowInSeconds;
    }

    @Override
    public long getQpm() {
        long count = getValueCount();
        int windowInSeconds = (int) ((this.windowMillis * 60L) / 1000L);
        if (windowInSeconds <= 0) {
            return count;
        }
        return count / windowInSeconds;
    }

}
