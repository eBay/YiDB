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

package com.ebay.cloud.cms.utils;

import java.util.HashSet;

import junit.framework.Assert;

import org.junit.Test;

public class SixtyTwoBaseTest {
    @Test
    public void testChange() {
        for (int i = 0; i < SixtyTwoBase.base; i++) {
            String v = SixtyTwoBase.tenBaseToSixtyTwo(i);
            Assert.assertEquals(1, v.length());
            Assert.assertEquals(SixtyTwoBase.characters[i], v.charAt(0));

            int vi = SixtyTwoBase.sixtyTwoBaseToTen(new Character(SixtyTwoBase.characters[i]).toString());
            Assert.assertEquals(i, vi);
        }

        Assert.assertEquals(2432, SixtyTwoBase.sixtyTwoBaseToTen("dE"));
        Assert.assertEquals("dE", SixtyTwoBase.tenBaseToSixtyTwo(2432));

        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < 10000; i++) {
            String v = SixtyTwoBase.tenBaseToSixtyTwo(i);
            Assert.assertFalse(set.contains(v));
            set.add(v);

            Assert.assertEquals(i + 1, SixtyTwoBase.sixtyTwoBaseToTen(SixtyTwoBase.addOne(v)));
        }

        try {
            SixtyTwoBase.sixtyTwoBaseToTen(null);
        } catch (Exception e) {
            // expected
        }
        try {
            SixtyTwoBase.sixtyTwoBaseToTen("");
        } catch (Exception e) {
            // expected
        }
    }
}
