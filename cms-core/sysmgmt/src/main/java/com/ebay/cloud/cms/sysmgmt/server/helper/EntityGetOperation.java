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

import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.IEntityService;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;

public class EntityGetOperation extends EntityOperation {
    private Map<String, List<String>> params;
    
    public EntityGetOperation(CMSPriority priority, IEntityService entityService, IEntity queryEntity,
            IEntity entity, String fieldName, EntityContext context, Map<String, List<String>> params, CMSState cmsState, String reqId, MonitorStatisticsManager monitor) {
        super(priority, entityService, queryEntity, entity, fieldName, context, false, cmsState, reqId, monitor);
        this.params = params;
    }
    
    protected void doInitAction() {
        super.doInitAction();
        String fields = FieldHelper.getFieldsParameter(params);
        if (!org.apache.commons.lang3.StringUtils.isBlank(fields)) {
            String[] fieldNames = fields.split(",");
            for (String field : fieldNames) {
                if (!org.apache.commons.lang3.StringUtils.isBlank(field)) {
                    context.addQueryField(field.trim().substring(1));
                }
            }
        }
    }

    @Override
    protected Object performAction() {
        return entityService.get(queryEntity, context);
    }
    
    @Override
    protected void performFinallyAction(boolean isSuccess) {
        doFinalAction(queryEntity, context, reqId, current, isSuccess, null, null, true);
    }
}
