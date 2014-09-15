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

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;

public class EntityModifyOperation extends EntityOperation {
    
    public EntityModifyOperation(CMSPriority priority, IEntityService entityService, IEntity queryEntity,
            IEntity entity, String fieldName, EntityContext context, CMSState cmsState, String reqId, MonitorStatisticsManager monitor) {
        super(priority, entityService, queryEntity, entity, fieldName, context, true, cmsState, reqId, monitor);
    }

    @Override
    protected String performAction() {
        entityService.modify(queryEntity, entity, context);
        return null;
    }
    
    @Override
    protected void performFinallyAction(boolean isSuccess) {
        doFinalAction(entity, context, reqId, current, isSuccess, "POST", entity, false);
    }
}
