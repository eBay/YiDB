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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * base class of metrics value distribution
 * 
 * @author xjiang
 *
 */
public class AbstractValueDistribution {
    
    private static final Logger log = LoggerFactory.getLogger(MetricValueDistribution.class);

    protected final long[]      startArray;
    protected final long[]      endArray;
    protected final long        slotWidth;
    protected final int         slotNum;
    
    
    public AbstractValueDistribution(long maxScale, int slotCnt) {
        slotWidth = maxScale / slotCnt;
        slotNum = slotCnt + 1;
        startArray = new long[slotNum];
        endArray = new long[slotNum];
        long startValue = 0;
        for (int i = 0; i < slotNum; i++) {
            startArray[i] = startValue;
            endArray[i] = startValue + slotWidth;
            startValue = endArray[i]; 
        }
        endArray[slotNum - 1] = Long.MAX_VALUE;
        
        log.debug(MessageFormat.format("Initializa metric value distribution with array size:{0}, max value:{1}",
                slotNum, maxScale));
    }
    
}
