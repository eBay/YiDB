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

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.lock.ICMSLock;
import com.mongodb.Mongo;

/*
 * An implementation of ICMSLock by using a reentrant lock and a mongo mutex.
 * 
 * Different thread in the same jvm call lock on a MongoLockImpl object will block on the local reentrant lock;
 * 
 * Different process will call lock on MongoLockImpl objects with the same lockName will exclude each other by the MongoMutex.
 * 
 * MongoMutex is a distributed spin lock. 
 * 
 */
public class MongoLockImpl implements ICMSLock {
    
    private static final Logger logger = LoggerFactory.getLogger(MongoLockImpl.class);

    ReentrantLock llock;
    MongoMutex dlock;
    
    public MongoLockImpl(Mongo mongo, String dbName, String collName, String lockName, String clientName, int expireTime,
            int renewPeriod) {
        llock = new ReentrantLock();
        dlock = new MongoMutex(mongo, dbName, collName, lockName, clientName, expireTime, renewPeriod);
    }
    
    @Override
    public void lock() throws InterruptedException {
        llock.lock();
        
        //exception in dlock still expect the client code call unlock in finally
        //llock will be unlocked their
        dlock.acquire();
        
        logger.debug("lock acquired");
    }

    @Override
    public boolean tryLock() throws InterruptedException {
        if (llock.tryLock()) {
            //exception in dlock still expect the client code call unlock in finally
            //llock will be unlocked their
            if (!dlock.tryAcquire()) {
                    llock.unlock();
                    return false;
            }
            else {
                logger.debug("try lock success, lock acquired");
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void unlock() {
        logger.debug("lock unlocked");
        try {
            dlock.release();
        }
        finally {
            llock.unlock();
        }
    }
}
