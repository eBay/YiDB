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
package com.ebay.cloud.cms.sysmgmt.server.helper;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBTimeCollector;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;

public abstract class EntityOperation {
    private static final Logger logger = LoggerFactory.getLogger(EntityOperation.class);
    
    protected final CMSPriority priority;
    protected final IEntityService entityService;
    protected final IEntity queryEntity;
    protected final IEntity entity;
    protected final String fieldName;
    protected final EntityContext context;
    protected final boolean write;
    protected String reqId;
    protected long current;
    protected CMSState cmsState;
    protected MonitorStatisticsManager monitor;
    
    public EntityOperation(CMSPriority priority, IEntityService entityService, IEntity queryEntity, 
            IEntity entity, String fieldName, EntityContext context, boolean write, CMSState cmsState, String reqId, MonitorStatisticsManager monitor) {
        this.priority = priority;
        this.entityService = entityService;
        this.queryEntity = queryEntity;
        this.entity = entity;
        this.fieldName = fieldName;
        this.context = context;
        this.write = write;
        this.cmsState = cmsState;
        this.reqId = reqId;
        this.monitor = monitor;
    }
    
    public final Object perform() {
        doInitAction();
        boolean isSuccess = true;
        try {
            return performAction();
        } catch (RuntimeException re) {
            isSuccess = ExceptionHelper.handleRuntimeException(re);
            throw re;
        } finally {
            performFinallyAction(isSuccess);
        }
    }

    protected void doInitAction() {
//        this.reqId = requestId.getNext();
        context.setRequestId(reqId);
        current = System.currentTimeMillis();
        context.setStartProcessingTime(current);
//        setupEntityContext(context);
        DBTimeCollector.reset();
    }
    
    protected void doFinalAction(IEntity entity, EntityContext context, String id, long current, boolean isSuccess, 
            String httpAction, Object payload, boolean isReadRequest) {
        long total = System.currentTimeMillis() - current;
        context.setTotalTimeCost(total);
        context.setDbTimeCost(DBTimeCollector.getDBTimeCost());
        if (isReadRequest) {
            String query = entity.getType() + "@" + entity.getId();
            MonitorHelper.addReadRequest(monitor, (int) total, current, query, isSuccess);
        } else {
            HttpRequest request = null;
            if (StringUtils.isEmpty(fieldName)) {
            	request = new HttpRequest(httpAction, entity.getRepositoryName(), entity.getBranchId(), entity.getType(), 
                        entity.getId(), payload);
            } else {
            	request = new HttpRequest(httpAction, entity.getRepositoryName(), entity.getBranchId(), entity.getType(), 
                        entity.getId(), fieldName, payload);
            }
            MonitorHelper.addWriteRequest(monitor, (int) total, current, request, isSuccess);
        }
        logger.info("[{}], totalTimeCost {}, dbTimeCost {}, isSuccess {}.",
                new Object[] { id, context.getTotalTimeCost(), context.getDbTimeCost(), isSuccess });
    }
    
    protected abstract Object performAction();
    
    protected abstract void performFinallyAction(boolean isSuccess);
}
