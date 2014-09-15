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

package com.ebay.cloud.cms.sysmgmt.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.sysmgmt.exception.SystemManagementException;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;
import com.ebay.cloud.cms.utils.CheckConditions;

public class CMSState {
    private static final Logger logger = LoggerFactory.getLogger(CMSState.class);
    
    public enum State {
        startup, normal, maintain, overload, critical, readonly, severe, shutdown
    }
    
    //dont use deamon thread since need to make sure all event listener have been notified 
    // before this thread stopped
    ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private static final Map<State, Set<State>> stateMap;
    
    static {
        stateMap = new HashMap<State, Set<State>>();
        Set<State> set;
        
        set = new HashSet<State>();
        set.add(State.normal);
        stateMap.put(State.startup, set);
        
        set = new HashSet<State>();
        set.add(State.maintain);
        set.add(State.overload);
        set.add(State.critical);
        set.add(State.readonly);
        set.add(State.severe);
        set.add(State.shutdown);
        stateMap.put(State.normal, set);
        
        set = new HashSet<State>();
        set.add(State.normal);
        set.add(State.severe);
        set.add(State.readonly);
        set.add(State.shutdown);
        stateMap.put(State.maintain, set);
        
        set = new HashSet<State>();
        set.add(State.maintain);
        set.add(State.normal);
        set.add(State.critical);
        set.add(State.severe);
        set.add(State.readonly);
        set.add(State.shutdown);
        stateMap.put(State.overload, set);
        
        set = new HashSet<State>();
        set.add(State.maintain);
        set.add(State.normal);
        set.add(State.severe);
        set.add(State.readonly);
        set.add(State.overload);
        set.add(State.shutdown);
        stateMap.put(State.critical, set);

        set = new HashSet<State>();
        set.add(State.normal);
        set.add(State.critical);
        set.add(State.maintain);
        set.add(State.severe);
        set.add(State.readonly);
        set.add(State.overload);
        set.add(State.shutdown);
        stateMap.put(State.severe, set);
        
        set = new HashSet<State>();
        set.add(State.normal);
        set.add(State.critical);
        set.add(State.maintain);
        set.add(State.readonly);
        set.add(State.severe);
        set.add(State.overload);
        set.add(State.shutdown);
        stateMap.put(State.readonly, set);

        // shut down state could be changed any further
        stateMap.put(State.shutdown, new HashSet<State>());
    }
    
    private State state;
    
    private List<IEventListener> listeners;
    
    public CMSState() {
        state = State.startup;
        listeners = new LinkedList<IEventListener>();
    }
    
    public void registerEvent(IEventListener listener) {
        synchronized (listener) {
            listeners.add(listener);
        }
    }
    
    public boolean removeEvent(IEventListener listener) {
        synchronized (listener) {
            return listeners.remove(listener);
        }
    }
    
    public synchronized State getState() {
        //get stale state is ok for client, here synchronized is not necessary
        //But to satisfy findbugs, finally I change this method to synchronized
        return state;
    }
    
    public synchronized boolean checkAndSetState(final State from, final State to) {
        if (state != from) {
            return false;
        }
        setState(to);
        return true;
    }
    
    public synchronized void setState(final State to) {
        CheckConditions.checkNotNull(to);
        if (to == state) {
            return;
        }
        
        if (stateMap.get(state).contains(to)) {
            
            
            final State oldState = state;
            state = to;
            
            logger.info("state changed from {} to {}", oldState, to);
            
            for (final IEventListener e : listeners) {
                executor.execute(new EventNotifier(e, oldState, to));
            }

            //TODO: what if some event listener can not finish?
            //  need to shutdown the executor forcely?
            if (to == State.shutdown) {
                executor.shutdown();
            }
        }
        else {
            logger.error("Can not switch state from " + state + " to " + to);
            throw new SystemManagementException("Can not switch state from " + state + " to " + to);
        }
    }
}

class EventNotifier implements Runnable {
    IEventListener e;
    CMSState.State from;
    CMSState.State to;
    
    public EventNotifier(IEventListener e, State from, State to) {
        super();
        this.e = e;
        this.from = from;
        this.to = to;
    }

    @Override
    public void run() {
        e.onEvent(from, to);
    }
}



