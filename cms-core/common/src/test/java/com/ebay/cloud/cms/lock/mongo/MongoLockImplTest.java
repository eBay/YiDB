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

public class MongoLockImplTest extends CMSMongoTest {
    private static Mongo  mongo;
    private static String dbName;
    private static String collName;

    @Before
    public void before() {
        mongo = getDataSource().getMongoInstance();
        dbName = "MongoMutexTest";
        collName = "lockColl";

        mongo.getDB(dbName).getCollection(collName).drop();
    }

    @Test
    public void testExclusion() throws Exception {
        final MongoLockImpl m = new MongoLockImpl(mongo, dbName, collName, "l1", "c1", 1400, 400);
        final AtomicInteger i = new AtomicInteger();
        i.set(0);
        final ArrayList<Exception> a = new ArrayList<Exception>();

        Thread t1 = new Thread() {
            @Override
            public void run() {
                try {
                    m.lock();
                    if (i.get() != 0) {
                        a.add(new RuntimeException("i != 0"));
                        return;
                    }
                    i.set(1);
                    // expire 2 times if renew didn't take effect
                    Thread.sleep(2500);
                    if (i.get() != 1) {
                        a.add(new RuntimeException("i != 1"));
                        return;
                    }
                    i.set(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    m.unlock();
                }
            }
        };

        Thread t2 = new Thread() {
            @Override
            public void run() {
                try {
                    // make sure t1 run first
                    Thread.sleep(30);
                    m.lock();
                    if (i.get() != 0) {
                        a.add(new RuntimeException("i != 0"));
                        return;
                    }
                    i.set(2);
                    Thread.sleep(500);
                    if (i.get() != 2) {
                        a.add(new RuntimeException("i != 2"));
                        return;
                    }
                    i.set(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    a.add(e);
                } finally {
                    m.unlock();
                }
            }
        };

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        Assert.assertTrue(a.isEmpty());
    }

    @Test
    public void testTryLock() {
        final MongoLockImpl m = new MongoLockImpl(mongo, dbName, collName, "l1", "c1", 1400, 400);
        System.out.println(null == null);

        try {
            if (m.tryLock()) {

                Thread t2 = new Thread() {
                    public void run() {
                        try {
                            Assert.assertTrue(!m.tryLock());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    };
                };
                t2.start();
                t2.join();
            } else {
                Assert.fail();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            m.unlock();
        }
    }

}
