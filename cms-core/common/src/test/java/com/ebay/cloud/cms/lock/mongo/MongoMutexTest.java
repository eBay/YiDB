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

package com.ebay.cloud.cms.lock.mongo;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.Mongo;

public class MongoMutexTest extends CMSMongoTest {
    
    private static Mongo mongo;
    private static String dbName;
    private static String collName;
    
    
    @Before
    public void  before() {
        mongo = getDataSource().getMongoInstance();
        dbName = "MongoMutexTest";
        collName = "lockColl";
        
        mongo.getDB(dbName).getCollection(collName).drop();
    }
    
    @Test
    public void testIntializationConstraint() {
        try {
            new MongoMutex(null, dbName, collName, "testRenewPeriod", "c1", 1200, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, null, collName, "testRenewPeriod", "c1", 1200, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, dbName, null, "testRenewPeriod", "c1", 1200, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, dbName, collName, null, "c1", 1200, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, dbName, collName, "testRenewPeriod", null, 1200, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, dbName, collName, "testRenewPeriod", "c1", 999, 200);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new MongoMutex(mongo, dbName, collName, "testRenewPeriod", "c1", 1200, 500);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    
    @Test (expected=IllegalArgumentException.class)
    public void testExpireTime() {
        new MongoMutex(mongo, dbName, collName, "testExpireTime", "c1",500, 0);
    }
    
    @Test (expected=IllegalArgumentException.class)
    public void testRenewPeriod() {
        MongoMutex m = new MongoMutex(mongo, dbName, collName, "testRenewPeriod", "c1", 1200, 200);
        Assert.assertNotNull(m);
        
        m = new MongoMutex(mongo, dbName, collName, "testRenewPeriod", "c1", 1200, 210);
    }
    
    @Test 
    public void testReentrantable() {
        MongoMutex m = new MongoMutex(mongo, dbName, collName, "testReentrantable", "c1", 3000, 1500);
        try {
            m.acquire();
            try {
                m.acquire();
                Thread.sleep(30);
            }
            finally {
                m.release();
            }
        } catch (InterruptedException e) {
            Assert.assertTrue(false);
        }
        finally {
            m.release();
        }
    }
    
    @Test 
    public void testMultipleThreadLock() throws Exception {
        final MongoMutex m = new MongoMutex(mongo, dbName, collName, "testMultipleThreadLock", "c1", 3000, 1500);
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    m.acquire();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    m.release();
                }
            }
        };
        
        final ArrayList<Exception> a = new ArrayList<Exception>();
        Thread t2 = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);//make sure t1 run first
                    m.acquire();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    a.add(e);
                }
                finally {
                    m.release();
                }
            }
        };
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        
        Assert.assertEquals(1, a.size());
        System.out.println(a.get(0));
    }
    
    @Test 
    public void testExclusion() throws Exception {
        final MongoMutex m1 = new MongoMutex(mongo, dbName, collName, "testExclusion", "c1", 1400, 400);
        final MongoMutex m2 = new MongoMutex(mongo, dbName, collName, "testExclusion", "c2", 1400, 400);
        final AtomicInteger i = new AtomicInteger();
        i.set(0);
        final ArrayList<Exception> a = new ArrayList<Exception>();
        
        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    m1.acquire();
                    if (i.get() != 0) {
                        System.out.println("i != 0, " + i.toString());
                        a.add(new RuntimeException("i != 0, " + i.toString()));
                        return;
                    }
                    i.set(1);
                    //expire 2 times if renew didn't take effect
                    Thread.sleep(3000);
                    if (i.get() != 1) {
                        System.out.println("i != 1, " + i.toString());
                        a.add(new RuntimeException("i != 1, " + i.toString()));
                        return;
                    }
                    i.set(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    m1.release();
                }
            }
        };
        
        Thread t2 = new Thread() {
            @Override
            public void run() {
                try {
                    //make sure t1 run first
                    Thread.sleep(50);
                    m2.acquire();
                    if (i.get() != 0) {
                        System.out.println("t2: i != 0, " + i.toString());
                        a.add(new RuntimeException("i != 0, " + i.toString()));
                        return;
                    }
                    i.set(2);
                    Thread.sleep(500);
                    if (i.get() != 2) {
                        System.out.println("t2: i != 2, " + i.toString());
                        a.add(new RuntimeException("i != 2, " + i.toString()));
                        return;
                    }
                    i.set(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    a.add(e);
                }
                finally {
                    m2.release();
                }
            }
        };
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        
        Assert.assertTrue(a.isEmpty());
    }

}
