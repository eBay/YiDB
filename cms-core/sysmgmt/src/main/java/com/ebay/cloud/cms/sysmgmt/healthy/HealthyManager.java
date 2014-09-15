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

package com.ebay.cloud.cms.sysmgmt.healthy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.expression.factory.DaemonThreadFactory;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;
import com.ebay.cloud.cms.sysmgmt.state.CMSState.State;
import com.ebay.cloud.cms.utils.CheckConditions;

public class HealthyManager implements IManagementServices {
    private static final Logger logger = LoggerFactory.getLogger(HealthyManager.class);
    
    private ScheduledExecutorService executor;
    private final int peroidMs;
    private final Healthy healthy;
    private final MonitorStatisticsManager monitorStatistics;
    private final CMSState cmsState;

    public HealthyManager(MonitorStatisticsManager monitorStatistics, CMSState cmsState, String healthyExpression, int peroidMs) {
        super();
        
        CheckConditions.checkNotNull(monitorStatistics, "monitorStatistics should not be null");
        CheckConditions.checkNotNull(cmsState, "cmsState should not be null");
        this.monitorStatistics = monitorStatistics;
        this.cmsState = cmsState;
        this.healthy = new Healthy(healthyExpression);
        this.peroidMs = peroidMs;
    }

    @Override
    public synchronized void init() {
        //do nothing
    }

    @Override
    public synchronized void startup() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory.getInstance());
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        State state = cmsState.getState();
                        if (isImmutableState(state)) {
                            return;
                        }

                        double h = healthy.getHealthyStatus(monitorStatistics.getStatistics());
                        if (h > 2 && 3 > h) {
                            cmsState.checkAndSetState(state, CMSState.State.severe);
                        }
                        else if (h > 1 && 2 > h) {
                            // h betwhen 1 and 2 treated as mongo db issue
                            cmsState.checkAndSetState(state, CMSState.State.readonly);
                        }
                        else if (h > 0.9) {
                            cmsState.checkAndSetState(state, CMSState.State.critical);
                        }
                        else if (h > 0.80) {
                            cmsState.checkAndSetState(state, CMSState.State.overload);
                        }
                        else {
                            cmsState.checkAndSetState(state, CMSState.State.normal);
                        }
                    }
                    catch (Throwable t) {
                        logger.error("exception during HealthyManager running", t);
                    }
                }

                private boolean isImmutableState(State state) {
                    return state != State.normal && state != State.overload && state != State.severe && state != State.readonly;
                }
                
            }, 0, peroidMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    @Override
    public synchronized ServiceStatus isRunning() {
        
        if (executor != null) {
            return ServiceStatus.running;
        }
        else {
            return ServiceStatus.stopped;
        }
    }
}
