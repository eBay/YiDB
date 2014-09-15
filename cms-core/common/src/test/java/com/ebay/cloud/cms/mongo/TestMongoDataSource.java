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

package com.ebay.cloud.cms.mongo;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.mongodb.ServerAddress;

public class TestMongoDataSource {
    
    @Test(expected=Exception.class)
    public void testNullServerString() {
        MongoDataSource.parseServerString(null);
    }
    
    @Test(expected=Exception.class)
    public void testEmptyServerString() {
        MongoDataSource.parseServerString("");
    }
    
    @Test(expected=RuntimeException.class)
    public void testBadServerString1() {
        MongoDataSource.parseServerString("localhost:3:2,");
    }
    
    @Test(expected=RuntimeException.class)
    public void testBadServerString2() {
        MongoDataSource.parseServerString("@@@:12345");
    }
    
    @Test
    public void testServerString() {
        List<ServerAddress> addrs1 = MongoDataSource.parseServerString("localhost:12345, www.ebay.com:2897");
        Assert.assertEquals(2, addrs1.size());
        
        Assert.assertEquals("localhost", addrs1.get(0).getHost());
        Assert.assertEquals("www.ebay.com", addrs1.get(1).getHost());
        
        List<ServerAddress> addrs2 = MongoDataSource.parseServerString("localhost, www.ebay.com:2897");
        Assert.assertEquals(2, addrs2.size());
        
        Assert.assertEquals("localhost", addrs2.get(0).getHost());
        Assert.assertEquals("www.ebay.com", addrs2.get(1).getHost());
    }
    
    @Test
    public void testEqual() {
        MongoDataSource ds1 = new MongoDataSource("localhost:12345, www.ebay.com:2897");
        MongoDataSource ds2 = new MongoDataSource("www.ebay.com:02897, localhost:012345");
        
        Assert.assertEquals(ds2, ds1);
        Assert.assertEquals(ds2.hashCode(), ds1.hashCode());
        
        MongoDataSource ds3 = new MongoDataSource("www.ebay.com:02897, localhost:01234");
        Assert.assertFalse(ds3.equals(ds1));
        Assert.assertFalse(ds3.hashCode() == ds1.hashCode());
    }
    
    @Test
    public void testNotEqual() {
        MongoDataSource ds1 = new MongoDataSource("localhost:12345, www.ebay.com:2897");
        String str = "localhost:12345, www.ebay.com:2897";
        Object obj = str;
        Assert.assertFalse(ds1.equals(obj));
        
        MongoDataSource ds2 = new MongoDataSource("www.ebay.com:02897, localhost:012345, localhost:23456");
        Assert.assertFalse(ds1.equals(ds2));
        Assert.assertFalse(ds1.hashCode() == ds2.hashCode());
    }
    
}
