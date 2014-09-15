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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.entity.impl.BsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.entity.visitor.ActiveAPIEntityVisitor;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.StringUtils;

public abstract class EntityOperation {
    private static final Logger logger = LoggerFactory.getLogger(EntityOperation.class);
    
    static final String METACLASS_NOT_FOUND = "metaclass not found: ";
    static final String REPO_NOT_FOUND = "repository not found: ";
    
    protected static final ObjectMapper mapper = new ObjectMapper();

    protected final CMSServer cmsServer;
    protected final CMSPriority priority;
    protected final String repoName;
    protected final String metadata;
    protected final String branch;
    protected final String jsonString;
    protected final String oid;
    protected final String errorMsg;
    protected final CMSQueryMode mode;
    protected final ConsistentPolicy policy;
    protected final CMSResponse response;
    protected final EntityContext context;
    protected final UriInfo uriInfo;

    public EntityOperation(CMSServer server, String mode, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String errorMsg, UriInfo uriInfo) {
        this.cmsServer = server;
        this.mode = CMSQueryMode.fromString(mode);
        this.priority = CMSResourceUtils.parsePriority(priority);
        this.policy = CMSResourceUtils.parsePolicy(cmsServer, consistPolicy);
        this.repoName = repoName;
        this.metadata = metadata;
        this.branch = branch;
        this.jsonString = jsonBody;
        this.oid = oid;
        this.errorMsg = errorMsg;
        this.response = new CMSResponse();
        this.uriInfo = uriInfo;

        context = createEntityContext(uriInfo, request);
        context.setConsistentPolicy(policy);
    }
    
    public EntityContext getContext() {
        return context;
    }

    @SuppressWarnings("unchecked")
    private EntityContext createEntityContext(UriInfo uriInfo, HttpServletRequest request) {
        EntityContext entityContext = new EntityContext();
        MultivaluedMap<String, String> mmap = uriInfo.getQueryParameters();
        // be compliance to get from request getAttribute, if not found, try
        // from request parameter
        String cid = (String) request.getAttribute(CMSResourceUtils.REQ_PARAM_COMPONENT);
        if (cid == null) {
            cid = mmap.getFirst(CMSResourceUtils.REQ_PARAM_COMPONENT);
        }

        // get sourceIp from request attribute, if not found, then use VIP
        // address
        String sourceIp = (String) request.getAttribute(CMSResourceUtils.X_CMS_CLIENT_IP);
        if (sourceIp == null) {
            sourceIp = request.getRemoteAddr();
        }

        String uid = mmap.getFirst(CMSResourceUtils.REQ_PARAM_UID);
        String comment = mmap.getFirst(CMSResourceUtils.REQ_PARAM_COMMENT);
        entityContext.setModifier(cid);
        entityContext.setSourceIp(sourceIp);
        if (entityContext.getSourceIp() == null) {
            entityContext.setSourceIp(request.getRemoteAddr());
        }
        entityContext.setUserId(uid);
        if (org.apache.commons.lang3.StringUtils.isEmpty(comment)) {
            comment = "";
        }
        entityContext.setComment(comment);

        String path = mmap.getFirst(CMSResourceUtils.REQ_PARAM_PATH);
        entityContext.setPath(path);

        String versionStr = mmap.getFirst(CMSResourceUtils.REQ_PARAM_VERSION);
        int version = IEntity.NO_VERSION;
        if (versionStr != null && !versionStr.isEmpty()) {
            try {
                version = Integer.valueOf(versionStr);
            } catch (NumberFormatException e) {
                logger.error("Version not valid!", e);
            }
            entityContext.setVersion(version);
        }

        String dal = mmap.getFirst(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION);
        entityContext.setRegistration(cmsServer.getDalImplementation(dal));
        entityContext.setRequest(request);

        Map<String, List<SearchCriteria>> addiotionalCriteria = (Map<String, List<SearchCriteria>>) request.getAttribute(CMSResourceUtils.REQ_READ_FILTER);
        entityContext.setAdditionalCriteria(addiotionalCriteria);
        return entityContext;
    }

    public final CMSResponse perform() {
        try {
            
            performAction();

        } catch (WebApplicationException e) {
            logger.error(MessageFormat.format(errorMsg, metadata, oid), e);
            throw e;
        } catch (Throwable t) {
            convertExceptionAndReThrow(t);
        }

        return response;
    }

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
        IEntity entity = createEntity(metaClass);

        performAction(context, entity, queryEntity);

        addTimeProperties(response);
        collectResult(mode, response, entity);
    }

    protected IEntity createEntity(MetaClass metaClass) {
        IEntity entity = null;
        try {
            entity = new JsonEntity(metaClass, jsonString);
        } catch (Exception e) {
            throw new BadParamException("invalid json entity: " + jsonString);
        }
        
        ObjectNode objNode = (ObjectNode)entity.getNode();
        JsonNode typeNode = objNode.get("_type");
        if (typeNode != null) {
            String type = (String)typeNode.getTextValue();
            if (type != null && !type.equals(metaClass.getName())) {
                throw new BadParamException("inconsistent meta type: " + type);
            }
        }

        entity.setBranchId(branch);
        if (oid != null) {
            entity.setId(oid);
        }
        return entity;
    }

    protected abstract void performAction(EntityContext context, IEntity entity, IEntity queryEntity);

    protected void collectResult(CMSQueryMode mode, CMSResponse response, IEntity entity) {
        response.addResult(buildJsonNode(mode, entity));
    }

    protected final JsonNode buildJsonNode(CMSQueryMode mode, IEntity entity) {
        JsonNode node = null;
        if (mode == CMSQueryMode.URI) {
            ActiveAPIEntityVisitor apiVisitor = new ActiveAPIEntityVisitor(repoName, branch,
                    ((IEntity) entity).getMetaClass());
            ((IEntity) entity).traverse(apiVisitor);
            node = (JsonNode) apiVisitor.getBuildEntity().getNode();
        } else {
            if (entity instanceof JsonEntity) {
                node = (JsonNode) entity.getNode();
            } else if (entity instanceof BsonEntity) {
                EntityMapper mapperVisitor = new EntityMapper(JsonEntity.class, entity.getMetaClass());
                entity.traverse(mapperVisitor);
                node = (JsonNode) mapperVisitor.getBuildEntity().getNode();
            }
        }
        return node;
    }

    protected void addTimeProperties(CMSResponse response) {
        response.addProperty("dbTimeCost", context.getDbTimeCost());
        response.addProperty("totalTimeCost", context.getTotalTimeCost());
    }

    protected JsonEntity buildQueryEntity(String reponame, String branchname, String metadata, String oid,
            CMSPriority p, EntityContext context) {
        JsonEntity queryEntity = null;
        MetaClass meta = cmsServer.getMetaClass(p, reponame, metadata);
        if (meta == null) {
            throw new NotFoundException(METACLASS_NOT_FOUND + metadata);
        }
        queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }

    protected JsonEntity buildQueryEntity(String reponame, String branchname, String metadata, String metaVersion,
            String oid, CMSPriority p, EntityContext context) {
        if (StringUtils.isNullOrEmpty(metaVersion)) {
            return buildQueryEntity(reponame, branchname, metadata, oid, p, context);
        }

        int version = 0;
        try {
            version = Integer.parseInt(metaVersion);
        } catch (NumberFormatException e) {
            throw new BadParamException("invalid version: " + metaVersion);
        }

        MetaClass meta = cmsServer.getMetaClass(p, reponame, metadata, version);
        if (meta == null) {
            throw new NotFoundException(METACLASS_NOT_FOUND + metadata + " with version: " + metaVersion);
        }
        JsonEntity queryEntity = new JsonEntity(meta);
        queryEntity.setId(oid);
        queryEntity.setBranchId(branchname);
        return queryEntity;
    }
    
    protected static void convertExceptionAndReThrow(Throwable ee) {
        try {
            throw ee;
        } catch (CannotServeException e) {
            throw new ServiceUnavailableException(e, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new BadParamException(e, e.getMessage());
        } catch (CmsDalException e) {
            if (e.getErrorCode() == DalErrCodeEnum.ENTITY_NOT_FOUND.getErrorCode()) {
                throw new NotFoundException(e.getErrorCode(), e, e.getMessage());
            }
            throw new CMSServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (CmsEntMgrException e) {
            throw new CMSServerException(e.getErrorCode(), e.getMessage(), e);
        } catch (RepositoryNotExistsException e) {
            throw new NotFoundException(e, e.getMessage());
        } catch (Throwable t) {
            throw new CMSServerException(t);
        }
    }
}
