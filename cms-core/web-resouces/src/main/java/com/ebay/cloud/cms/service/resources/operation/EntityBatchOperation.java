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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBTimeCollector;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.sysmgmt.server.RequestIdSequence;
import com.ebay.cloud.cms.sysmgmt.server.helper.HttpRequest;

public abstract class EntityBatchOperation extends EntityOperation {
    private static final Logger logger = LoggerFactory.getLogger(EntityBatchOperation.class);

    BatchOperationFailReturnOption failReturnOption = BatchOperationFailReturnOption.IMMEDIATE;
    RequestIdSequence requestId = new RequestIdSequence();

    public EntityBatchOperation(CMSServer server, String mode, String failReturnOptionVal, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String errorMsg, UriInfo uriInfo) {
        super(server, mode, request, priority, consistPolicy, repoName, metadata, branch, oid, jsonBody, errorMsg,
                uriInfo);
        this.failReturnOption = BatchOperationFailReturnOption.fromString(failReturnOptionVal);
    }

    @Override
    protected void performAction() {
        context.setBatchOperationFailReturnOption(failReturnOption);
        
        long current = System.currentTimeMillis();
        context.setStartProcessingTime(current);

        String id = requestId.getNext();
        context.setRequestId(id);

        DBTimeCollector.reset();
        boolean isSuccess = true;
        List<IEntity> entities = new ArrayList<IEntity>();
        try {
            List<String> failures = new ArrayList<String>();
            entities = getEntities(priority.toString(), repoName, branch, jsonString, failures);
            performBatchAction(context, entities, failures);
            collectBatchResult(mode, response, entities);
        } catch (CmsDalException cde) {
            if (CMSServer.CMS_FAILURE_ERROR_CODES.contains(cde.getErrorEnum())) {
                isSuccess = false;
            }
            throw cde;
        } catch (RuntimeException re) {
            isSuccess = false;
            throw re;
        } finally {
            long total = System.currentTimeMillis() - current;
            context.setTotalTimeCost(total);
            context.setDbTimeCost(DBTimeCollector.getDBTimeCost());
            if (!entities.isEmpty()) {
                IEntity queryEntity = entities.get(0);
                cmsServer.addWriteRequest(
                        (int) total,
                        current,
                        new HttpRequest("POST", queryEntity.getRepositoryName(), queryEntity
                                .getBranchId(), entities), isSuccess);
            } else {
                cmsServer.addWriteRequest((int) total, current, new HttpRequest("POST", repoName, branch,
                        entities), isSuccess);
            }
            logger.info("[{}], totalTimeCost {}, dbTimeCost {}, isSuccess {}.",
                    new Object[] { id, context.getTotalTimeCost(), context.getDbTimeCost(), isSuccess });
        }
    }

    protected abstract void performBatchAction(EntityContext context, List<IEntity> entities, List<String> parseFails);

    protected void collectBatchResult(CMSQueryMode mode, CMSResponse response, List<IEntity> entities) {
        // default action: add time properties
        addTimeProperties(response);
    }

    @Override
    protected final void performAction(EntityContext context, IEntity entity, IEntity queryEntity) {
        // do nothing, prohibit overriding
    }

    @Override
    protected final void collectResult(CMSQueryMode mode, CMSResponse response, IEntity entity) {
        // do nothing, prohibit overriding
    }
    
    /**
     * 
     * @param priorityString
     * @param repoName
     * @param branch
     * @param jsonString
     * @return
     */
    public List<IEntity> getEntities(String priorityString, String repoName, String branch, String jsonString, List<String> failures) {
        JsonNode jsonNode = null;
        CMSPriority priority = CMSResourceUtils.parsePriority(priorityString);
        try {
            jsonNode = mapper.readTree(jsonString);
        } catch (IOException e) {
            throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON, "json parse error: " + e.getMessage(), e);
        }
        
        if (jsonNode.isArray()) {
            List<IEntity> entities = new ArrayList<IEntity>(jsonNode.size());
            Iterator<JsonNode> iter = jsonNode.getElements();
            while (iter.hasNext()) {
                ObjectNode n = (ObjectNode) iter.next();
                JsonNode typeNode = n.get("_type");
                try {
                    if (typeNode == null || typeNode.asText() == null || typeNode.asText().isEmpty()) {
                        throw new BadParamException(String.format("entity type must be provided for batch operation: %s", n));
                    }
                    String metaType = typeNode.asText();
                    MetaClass metaClass = null;
                    try {
                        metaClass = cmsServer.getMetaClass(priority, repoName, metaType);
                        if (metaClass == null) {
                            throw new NotFoundException(METACLASS_NOT_FOUND + metaType);
                        }
                    } catch (RepositoryNotExistsException e) {
                        throw new NotFoundException(REPO_NOT_FOUND + repoName);
                    }
                    
                    IEntity entity = new JsonEntity(metaClass, n);
                    entity.setBranchId(branch);
                    entities.add(entity);
                } catch (WebApplicationException e) {
                    if (BatchOperationFailReturnOption.ALL.equals(failReturnOption)) {
                        Object entity = e.getResponse().getEntity();
                        if (entity instanceof CMSResponse) {
                            CMSResponse resp = (CMSResponse)entity;
                            failures.add(resp.getErrorMsg());
                        } else {
                            failures.add(e.getMessage());
                        }
                    } else  {
                        throw e;
                    }
                }
            }
            return entities;
        } else {
            throw new BadParamException("batch create accept only array of entities");
        }
    }
}
