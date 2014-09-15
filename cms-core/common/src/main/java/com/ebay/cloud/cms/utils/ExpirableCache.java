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


package com.ebay.cloud.cms.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpirableCache<T> {
    private int maximumSize;
    private long timeToLive;
    private Map<String, CacheEntry<T>> cache = new ConcurrentHashMap<String, CacheEntry<T>>();
    
    public ExpirableCache(int maximumSize, int timeToLiveSeconds) {
        if (maximumSize <= 0) {
            throw new RuntimeException("Please provide a valid maximumSize!");
        }
        if(timeToLiveSeconds<=0){
            throw new RuntimeException("Please provide a valid timeToLiveSeconds");
        }
        this.maximumSize = maximumSize;
        this.timeToLive = timeToLiveSeconds * 1000l;
    }

    public T getObject(String name) {
        checkNotNull(name);
        CacheEntry<T> entry = cache.get(name);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - entry.getLastModified() > timeToLive) {
            return null;
        }
        return entry.getValue();
    }

    public void putObject(String name, T value) {
        checkNotNull(name);
        long now = System.currentTimeMillis();
        cleanup(now);
        CacheEntry<T> entry = new CacheEntry<T>(now, value);
        cache.put(name, entry);
    }

    public void deleteObject(String name) {
        checkNotNull(name);
        cache.remove(name);
    }

    public void clear() {
        cache.clear();
    }

    public List<T> values() {
        List<T> list = new ArrayList<T>();
        for (CacheEntry<T> entry : cache.values()) {
            list.add(entry.getValue());
        }
        return list;
    }
    
    private synchronized void cleanup(long now) {
        Iterator<Map.Entry<String, CacheEntry<T>>> iter = cache.entrySet().iterator();
        while (iter.hasNext()) {
            CacheEntry<T> entry = iter.next().getValue();
            if (now - entry.getLastModified() > timeToLive) {
                iter.remove();
            }
        }
        if (cache.size() == this.maximumSize) {
            String key = cache.keySet().iterator().next();
            cache.remove(key);
        }
    }
    
    private void checkNotNull(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
    }
}

class CacheEntry<T> {
    private long lastModified;
    private T obj;

    public CacheEntry(long ts, T value) {
        this.lastModified = ts;
        this.obj = value;
    }
    
    public T getValue(){
        return obj;
    }

    public long getLastModified() {
        return lastModified;
    }
}
