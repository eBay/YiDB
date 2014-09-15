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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class EqualsUtilTest {

    @Test
    public void testEquals() {
        Assert.assertTrue(EqualsUtil.isEquals(new String("test"), new String("test")));
        
        Assert.assertFalse(EqualsUtil.isEquals(new String("test"), new String("test1")));
        
        Assert.assertFalse(EqualsUtil.isEquals(new String("test"), null));
        
        Assert.assertTrue(EqualsUtil.isEquals(null, null));
        
        Assert.assertTrue(!EqualsUtil.isEquals(new ArrayList<String>(), null));
        
        Assert.assertTrue(!EqualsUtil.isEquals(null, new ArrayList<String>()));
        
        List<String> l1 = new ArrayList<String>();
        List<String> l2 = new ArrayList<String>();
        l2.add("str2");
        Assert.assertTrue(!EqualsUtil.isEquals(l1, l2));
        
        l1.add("str1");
        Assert.assertTrue(!EqualsUtil.isEquals(l1, l2));
        
        l1.remove(0);
        l1.add("str2");
        Assert.assertTrue(EqualsUtil.isEquals(l1, l2));
        Assert.assertTrue(EqualsUtil.isEquals(l2, l1));
        
    }

}
