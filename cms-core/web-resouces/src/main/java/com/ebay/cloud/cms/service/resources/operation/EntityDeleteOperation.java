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
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public class EntityDeleteOperation extends EntityOperation {

    public EntityDeleteOperation(CMSServer server, String mode, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String errorMsg, UriInfo uriInfo) {
        super(server, mode, request, priority, consistPolicy, repoName, metadata, branch, oid, jsonBody, errorMsg,
                uriInfo);
    }

    @Override
    protected void performAction() {
        MetaClass metaClass = null;
        try {
            metaClass = cmsServer.getMetaClass(priority, repoName, metadata);
            if (metaClass == null) {
                throw new NotFoundException(METACLASS_NOT_FOUND + metadata);
            }
        } catch (RepositoryNotExistsException e) {
            throw new NotFoundException(REPO_NOT_FOUND + repoName);
        }
        IEntity queryEntity = buildQueryEntity(repoName, branch, metadata, oid, priority, context);
        cmsServer.delete(priority, queryEntity, context);
        addTimeProperties(response);
    }

    @Override
    protected void performAction(EntityContext context, IEntity entity, IEntity queryEntity) {
        // do nothing
    }

    @Override
    protected void collectResult(CMSQueryMode mode, CMSResponse response, IEntity entity) {
        // do nothing
    }

}
