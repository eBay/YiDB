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

import junit.framework.Assert;

import org.junit.Test;

import com.ebay.cloud.cms.sysmgmt.exception.SystemManagementException;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;

class EventListener implements IEventListener {
    
    CMSState.State state;
    
    public CMSState.State getState() {
        return state;
    }

    @Override
    public void onEvent(State from, State to) {
        state = to;
    }
    
}

public class CMSStateTest {
    
    @Test
    public void testChangeState() throws InterruptedException {
        CMSState state = new CMSState();
        EventListener el = new EventListener();
        state.registerEvent(el);
        
        Assert.assertEquals(CMSState.State.startup, state.getState());
        state.setState(CMSState.State.normal);
        Thread.sleep(100);
        Assert.assertEquals(CMSState.State.normal, state.getState());
        Assert.assertEquals(CMSState.State.normal, el.getState());
        
        state.setState(CMSState.State.maintain);
        Thread.sleep(100);
        Assert.assertEquals(CMSState.State.maintain, state.getState());
        Assert.assertEquals(CMSState.State.maintain, el.getState());
        
        state.setState(CMSState.State.normal);
        
        state.setState(CMSState.State.overload);
        Thread.sleep(100);
        Assert.assertEquals(CMSState.State.overload, state.getState());
        Assert.assertEquals(CMSState.State.overload, el.getState());
        
        state.setState(CMSState.State.shutdown);
        Thread.sleep(100);
        Assert.assertEquals(CMSState.State.shutdown, state.getState());
        Assert.assertEquals(CMSState.State.shutdown, el.getState());
        
        state.removeEvent(el);
    }
    
    @Test
    public void testImpedent(){
        CMSState state = new CMSState();
        EventListener el = new EventListener();
        state.registerEvent(el);
        
        state.setState(State.startup);
        Assert.assertTrue(state.getState() == State.startup);
        state.setState(State.normal);
        Assert.assertTrue(state.getState() == State.normal);
        state.setState(State.normal);//test again
        Assert.assertTrue(state.getState() == State.normal);
    }
    
    @Test
    public void testCheckAdnSet() {
        CMSState state = new CMSState();
        EventListener el = new EventListener();
        state.registerEvent(el);

        state.setState(State.startup);
        Assert.assertEquals(false, state.checkAndSetState(State.maintain, State.maintain));
        Assert.assertEquals(true, state.checkAndSetState(State.startup, State.normal));
        Assert.assertTrue(state.getState() == State.normal);
    }

    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_1() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.normal);
        state.setState(CMSState.State.startup);
    }

    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_2() {
        CMSState state = new CMSState();

        state.setState(CMSState.State.shutdown);
    }
    
    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_3() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.overload);
    }
    
    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_4() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.maintain);
        state.setState(CMSState.State.overload);
    }
    
    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_5() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.shutdown);
        state.setState(CMSState.State.normal);
    }
    
    @Test (expected = SystemManagementException.class)
    public void testChangeStateExceptions_6() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.maintain);
        state.setState(CMSState.State.critical);
    }
    
    public void testChangeStateExceptions_7() {
        CMSState state = new CMSState();
        
        state.setState(CMSState.State.critical);
    }
    
    @Test(expected = SystemManagementException.class)
    public void testChangeStateException_8() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        state.setState(State.severe);
        state.setState(CMSState.State.startup);
    }
    
    @Test
    public void testChangeStateException_9() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        
        state.setState(State.severe);
        state.setState(CMSState.State.shutdown);
    }
    
    @Test
    public void testChangeStateException_10() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        state.setState(State.severe);
        state.setState(CMSState.State.maintain);
    }
    
    @Test(expected = SystemManagementException.class)
    public void testChangeStateException_11() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        state.setState(State.readonly);
        state.setState(CMSState.State.startup);
    }
    
    @Test
    public void testChangeStateException_12() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        state.setState(State.readonly);
        state.setState(CMSState.State.shutdown);
    }
    
    @Test
    public void testChangeStateException_13() {
        CMSState state = new CMSState();
        state.setState(State.normal);
        state.setState(State.readonly);
        state.setState(CMSState.State.maintain);
    }
    

}
