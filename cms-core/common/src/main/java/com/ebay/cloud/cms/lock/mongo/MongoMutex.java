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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.mongo.MongoOperand;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.MongoUtils;
import com.ebay.cloud.cms.utils.StringUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

/**
 * MongoMutex is a distributed spin lock. 
 * 
 * Only one "mutex" can acquire the lock at any time for mutex instances with the same lockName.
 * 
 * Multiple call from same thread to one mutex instance's lock method are ok (Reentrantable).
 * Multiple call from different threads to one mutex's instance's lock will throw exception
 * 
 * @author liubo
 *
 */

public class MongoMutex {
    
    /**
     * The state of the lock
     * 
     * LOCKED: this lock is acquired by some thread
     * UNLOCKED: this lock is free now
     * LOST_LOCK: this lock was lost when in locked state, 
     *            client should stop any further process and release this lock once detect this state
     * 
     * @author liubo
     *
     */
    public enum State {
        LOCKED, UNLOCKED, LOST_LOCK, 
    }
    
    static final Logger logger = LoggerFactory.getLogger(MongoMutex.class);
    
    public static final String LOCK_NAME = "lockName";
    public static final String OWNER = "owner";
    public static final String UPDATE_TIME = "updateTime";
    
    private int lockCount = 0;
    
    String lockName;
    String clientName;
    private int expireTime;
    int renewPeriod;
    
    private volatile State lockState;
    
    DBCollection coll;
    
    private RenewThread renewThread;
    
    private Thread ownerThread;
    
    public MongoMutex(Mongo mongo, String dbName, String collName, String lockName, String clientName, int expireTime,
            int renewPeriod) {
        super();

        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(dbName), "dbName should not be null or empty");
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(collName), "collName should not be null or empty");
        CheckConditions.checkArgument(mongo != null, "mongo should not be null");
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(lockName), "lockName should not be null or empty");
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(clientName), "clientName should not be null or empty");
        CheckConditions.checkArgument(expireTime >= 1000, "expireTime must be larger than 1 second");
        
        CheckConditions.checkArgument(renewPeriod >= 400, "renewPeroid must larger than 0.4 second so renew will not cost cpu alot");
        CheckConditions.checkArgument(expireTime - renewPeriod >= 1000, "expireTime - renewPeriod must be larger than 1 second so client have enough time to renew");
        
        this.lockName = lockName;
        this.clientName = clientName;
        this.expireTime = expireTime;
        this.renewPeriod = renewPeriod;
        
        coll = mongo.getDB(dbName).getCollection(collName);
        coll.setWriteConcern(WriteConcern.SAFE);
        createIfNotExist(coll, lockName);
    }
    
    private static void createIfNotExist(DBCollection coll, String lockName) {
        BasicDBObject q = new BasicDBObject();
        q.put(LOCK_NAME, lockName);
        
        BasicDBObject u = new BasicDBObject();
        BasicDBObject o = new BasicDBObject();
        o.put(LOCK_NAME, lockName);
        u.put(MongoOperand.set, o);
        
        coll.findAndModify(q, null, null, false, u, true, true);
    }

    public synchronized void acquire() throws InterruptedException {
        
        while(true) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            
            if (tryAcquire()) {
                return;
            }
            
            //need to sleep?
            //follow findbugs suggestion, call wait instead of Thread.sleep
            this.wait(100); //Thread.sleep(100); 
        }
    }
    
    public synchronized boolean tryAcquire() {
        if (ownerThread == null) {
            BasicDBObject query = new BasicDBObject();
            query.put(LOCK_NAME, lockName);
            
            //owner == null || owner == self || (now - updateTime) > expireTime
            //owner logs the clientName who already acquired this lock
            BasicDBObject q1 = new BasicDBObject();
            q1.put(OWNER, null);
            BasicDBObject q2 = new BasicDBObject();
            q2.put(OWNER, clientName);
            BasicDBObject q3 = new BasicDBObject();
            BasicDBObject qq = new BasicDBObject();
            qq.put(MongoOperand.lt, new Date((new Date().getTime() - expireTime)));
            q3.put(UPDATE_TIME, qq);

            BasicDBList or = new BasicDBList();
            or.add(q1);
            or.add(q2);
            or.add(q3);
            
            query.put(MongoOperand.or, or);
            
            BasicDBObject update = new BasicDBObject();
            update.put(OWNER, clientName);
            update.put(UPDATE_TIME, new Date());
            BasicDBObject u = new BasicDBObject();
            u.put(MongoOperand.set, update);
            
            try {
                boolean success = MongoUtils.wrapperUpdate(coll, query, u); 
                if (success) {
                    ownerThread = Thread.currentThread();
                    lockCount++;
                    setLockState(State.LOCKED);
                    renewThread = new RenewThread(this);
                    renewThread.start();
                    logger.debug("{} acquired by {}, thread is {}", new Object[]{lockName, clientName, Thread.currentThread().getName()});
                }
                else {
                    logger.debug("tryacquir failed, {}, {}", query, u);
                }
                return success;
            } catch (MongoException e) {
                logger.error("mongo exception while trying acquire mongo lock: " + lockName + ", " + clientName, e);
                throw e; 
            }
        }
        else if(ownerThread == Thread.currentThread()) {
            if (lockCount <= 0) {
                logger.error("Current thread own this lock but the lockCount <= 0");
                throw new RuntimeException("Current thread own this lock but the lockCount <= 0");
            }
            if (getLockState().equals(State.LOCKED)) {
                lockCount++;
                return true;
            } else {
                return false;
            }
        }
        else {
            logger.error("lock already acquired by thread: " + Thread.currentThread().getName());
            throw new RuntimeException("lock already acquired by thread: " + Thread.currentThread().getName());
        }
    }

    public synchronized void release() {
        if (ownerThread == null) {
            logger.error("unexpected call to unlock {}", Thread.currentThread().getName());
        }
        else if (ownerThread != Thread.currentThread()) {
            logger.error("non-owner call to unlock {}", Thread.currentThread().getName());
            throw new RuntimeException("non-owner call to unlock " + Thread.currentThread().getName());
        }
        else {
            lockCount--;
            if (lockCount == 0) {
                //stop renew thread first
                renewThread.stopRenew();
                //fixme: need to join renewThread?
                
                BasicDBObject query = new BasicDBObject();
                query.put(LOCK_NAME, lockName);
                query.put(OWNER, clientName);
                
                BasicDBObject update = new BasicDBObject();
                update.put(OWNER, null);
                update.put(UPDATE_TIME, new Date());
                BasicDBObject u = new BasicDBObject();
                u.put(MongoOperand.set, update);
                
                try {
                    boolean success = MongoUtils.wrapperUpdate(coll, query, u);
                    if (!success) {
                        logger.warn("unlock failed: {}, {}", lockName, clientName);
                    }
                    else {
                        logger.debug("{} released by {}, thread is {}", new Object[]{lockName, clientName, Thread.currentThread().getName()});
                    }
                }
                catch (MongoException e) {
                    logger.error("mongo exception while trying release mongo lock: " + lockName + ", " + clientName, e);
                    throw e; 
                }
                finally {
                    ownerThread = null;
                    renewThread = null;
                    setLockState(State.UNLOCKED);
                }
            }
        }
    }

    public State getLockState() {
        return lockState;
    }

    public void setLockState(State lockState) {
        this.lockState = lockState;
    }
}

class RenewThread extends Thread {
    
    private MongoMutex lock;

    private volatile boolean stop = false;
    
    public RenewThread(MongoMutex lock) {
        super();
        this.lock = lock;
    }

    public void stopRenew() {
        stop = true;
        
        //wake up this thread if it is in sleep
        this.interrupt();
    }
    
    @Override
    public void run() {
        stop = false;
        
        while(true) {
            if (stop) {
                break;
            }
            
            boolean success = renew();
            if (success) {
                lock.setLockState(MongoMutex.State.LOCKED);
            }
            else {
                lock.setLockState(MongoMutex.State.LOST_LOCK);
            }
            
            try {
                Thread.sleep(lock.renewPeriod);
            } catch (InterruptedException e) {
                //do nothing;
            }
        }
    }
    
    private boolean renew() {
        BasicDBObject query = new BasicDBObject();
        query.put(MongoMutex.LOCK_NAME, lock.lockName);
        query.put(MongoMutex.OWNER, lock.clientName);
        
        BasicDBObject update = new BasicDBObject();
        update.put(MongoMutex.UPDATE_TIME, new Date());
        BasicDBObject u = new BasicDBObject();
        u.put(MongoOperand.set, update);
        
        //MongoMutex.logger.debug("renew: {}, {}", query, u);
        
        try {
            return MongoUtils.wrapperUpdate(lock.coll, query, u); 
        }
        catch (MongoException e) {
            MongoMutex.logger.error("mongo exception while trying renew mongo lock: " + lock.lockName + ", " + lock.clientName, e);
            return false; 
        }
        catch (Exception e) {
            MongoMutex.logger.error("unexpected exception while trying renew mongo lock: " + lock.lockName + ", " + lock.clientName, e);
            return false; 
        }
    }
}
