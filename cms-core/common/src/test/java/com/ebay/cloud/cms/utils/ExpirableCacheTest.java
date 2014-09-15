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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ExpirableCacheTest {
    String c[];

    @Before
    public void setUp() {
        c = new String[10];
        for (int i = 0; i < 10; i++) {
            c[i] = String.valueOf(i);
        }
    }
    
    @Test
    public void cacheTest() {
        ExpirableCache<String> cache = new ExpirableCache<String>(100, 60);
        
        for (String i : c) {
            cache.putObject(i, i);
        }
        
        for (String i : c) {
            String o = cache.getObject(i);
            assertTrue(o == c[Integer.valueOf(o)]);
        }
    }
    
    @Test
    public void expirationTest() throws InterruptedException {
        ExpirableCache<String> cache = new ExpirableCache<String>(100, 1);
        cache.putObject(c[0], c[0]);
        assertTrue(cache.getObject(c[0]) == c[0]);
        Thread.sleep(2000);
        assertNull(cache.getObject(c[0]));
    }
    
    @Test
    public void sizeTest() {
        ExpirableCache<String> cache = new ExpirableCache<String>(1, 60);
        cache.putObject(c[0], c[0]);
        cache.putObject(c[1], c[1]);
        assertNull(cache.getObject(c[0]));
        assertTrue(cache.getObject(c[1]) == c[1]);
    }
    
    @Test
    public void deleteTest() {
        ExpirableCache<String> cache = new ExpirableCache<String>(100, 60);
        cache.putObject(c[0], c[0]);
        assertTrue(cache.getObject(c[0]) == c[0]);
        cache.deleteObject(c[0]);
        assertNull(cache.getObject(c[0]));
    }
    
//    @Test
//    public void maxSizeTest() {
//        ExpiredLRUCache<Integer> cache = new ExpiredLRUCache<Integer>(2, 60);
//
//        cache.putObject("1", 1);
//        cache.putObject("2", 2);
//
//        System.out.println(cache.values().toString());
//
//        cache.putObject("3", 3);
//
//        System.out.println(cache.values().toString());
//        cache.putObject("4", 4);
//
//        System.out.println(cache.values().toString());
//    }
}
