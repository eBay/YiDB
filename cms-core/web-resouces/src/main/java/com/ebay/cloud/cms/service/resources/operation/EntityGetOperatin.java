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
package com.ebay.cloud.cms.service.resources.operation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public class EntityGetOperatin extends EntityOperation {

    private Object entity;
    private String metaVersion;

    public EntityGetOperatin(CMSServer server, String mode, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String errorMsg, UriInfo uriInfo) {
        super(server, mode, request, priority, consistPolicy, repoName, metadata, branch, oid, jsonBody, errorMsg,
                uriInfo);
        this.metaVersion = request.getHeader("X-CMS-METAVERSION");
    }

    @Override
    protected void performAction() {

        entity = getEnityDetail(repoName, branch, metadata, metaVersion, oid, mode, priority, context);

        addTimeProperties(response);
        response.addResult(entity);
    }

    private Object getEnityDetail(final String reponame, final String branchname, final String metadata,
            final String metaVersion, final String oid, CMSQueryMode mode, CMSPriority p, EntityContext context) {
        JsonEntity queryEntity = buildQueryEntity(reponame, branchname, metadata, metaVersion, oid, p, context);
        IEntity getEntity = cmsServer.get(p, queryEntity, uriInfo.getQueryParameters(), context);
        if (getEntity == null) {
            throw new NotFoundException("entity not found");
        }
        return buildJsonNode(mode, getEntity);
    }

    @Override
    protected void performAction(EntityContext context, IEntity entity, IEntity queryEntity) {
        // do nothing
    }
}
