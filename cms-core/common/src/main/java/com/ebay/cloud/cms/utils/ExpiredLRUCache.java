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

import java.util.List;

public class ExpiredLRUCache<T> {
//    Cache<String, T> cache;

    public ExpiredLRUCache() {
//        cache = CacheBuilder.newBuilder().maximumSize(maximumSize)
//                .expireAfterWrite(expireAfterSeconds, TimeUnit.SECONDS).build();
    }

    public T getObject(String name) {
//        return cache.getIfPresent(name);
        return null;
    }

    public void putObject(String name, T object) {
//        cache.put(name, object);
    }

    public void deleteObject(String name) {
//        cache.invalidate(name);
    }

    public void clear() {
//        cache.invalidateAll();
    }

    public List<T> values() {
//        return new ArrayList<T>(cache.asMap().values());
        return null;
    }
    
//    public static void main(String[] args) throws InterruptedException {
//        ExpiredLRUCache<Integer> cache = new ExpiredLRUCache<Integer>(2, 5);
//        cache.putObject("1", 1);
//        cache.putObject("2", 2);
//        
//        Thread.sleep(2000);
//        System.out.println(cache.getObject("2"));
//        cache.putObject("2", 2);
//        
//        cache.putObject("3", 3);
//        cache.putObject("4", 4);
//        
//        System.out.println(cache.getObject("1"));
//        System.out.println(cache.getObject("2"));
//        System.out.println(cache.getObject("3"));
//        System.out.println(cache.getObject("4"));
//        
//    }

}
