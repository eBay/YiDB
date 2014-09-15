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

package com.ebay.cloud.cms.service.resources.impl;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.ExceptionMapper;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.service.resources.IBranchResource;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

@Path("/repositories/{reponame}/branches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BranchResource implements IBranchResource {

    private static final Logger logger = LoggerFactory
            .getLogger(BranchResource.class);
    
    private CMSServer cmsServer = CMSServer.getCMSServer();
    
    private void convertExceptionAndReThrow(Throwable ee) {
        try {
            throw ee;
        }
        catch (CannotServeException e) {
            throw new ServiceUnavailableException();
        } 
        catch (IllegalArgumentException e) {
            throw new BadParamException(e, e.getMessage());
        }
        catch(CmsDalException e) {
            throw ExceptionMapper.convert(e);
        }
        catch(CmsEntMgrException e) {
            throw ExceptionMapper.convert(e);
        }
        catch (WebApplicationException wae) {
            logger.error("web application exception", wae);
            throw wae;
        }
        catch(Throwable t) {
            throw new CMSServerException(t);
        }
    }

    @GET
    public CMSResponse getMainBranches(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority, 
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame,
            @Context HttpServletRequest request){
        CMSPriority p = CMSResourceUtils.parsePriority(priority);
        CMSResponse response = new CMSResponse();
        ConsistentPolicy policy = CMSResourceUtils.parsePolicy(cmsServer, consistPolicy);
        
        try {
            EntityContext context = createContext(uriInfo, request, policy);
            
            List<IBranch> branches = cmsServer.getMainBranches(p, reponame, context);
            if (branches == null) {
                branches = Collections.emptyList();
            }
            
            response.addProperty("count", branches.size());
            response.addResult(branches);

            return response;
            
        } 
        catch (Throwable e) {
            logger.error("exception while list branches on repository " + reponame, e);
            convertExceptionAndReThrow(e);
            return null;
        }
    }
    
    public CMSResponse getBranch(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, 
            @PathParam("branch") String branchId,
            @Context HttpServletRequest request) {
        CMSPriority p = CMSResourceUtils.parsePriority(priority);
        CMSResponse response = new CMSResponse();
        ConsistentPolicy policy = CMSResourceUtils.parsePolicy(cmsServer, consistPolicy);
        try {
            EntityContext context = createContext(uriInfo, request, policy);

            IBranch branch = cmsServer.getBranch(p, reponame, branchId, context);
            if (branch == null) {
                throw new NotFoundException("branch not found for " + branchId);
            }

            response.addResult(branch);
            return response;
        } catch (RepositoryNotExistsException e) {
            throw new NotFoundException("repository not found: " + e.getMessage());
        } catch (Throwable e) {
            logger.error(MessageFormat.format("exception while list branch {0} on repository {1}", branchId, reponame),
                    e);
            convertExceptionAndReThrow(e);
            return null;
        }
    }

    protected EntityContext createContext(UriInfo uriInfo, HttpServletRequest request, ConsistentPolicy policy) {
        EntityContext context = new EntityContext();
        String sourceIp = (String)request.getAttribute(CMSResourceUtils.X_CMS_CLIENT_IP);
        context.setSourceIp(sourceIp);
        context.setConsistentPolicy(policy);
        MultivaluedMap<String, String> mmap = uriInfo.getQueryParameters();
        String dal = mmap.getFirst(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION);
        context.setRegistration(cmsServer.getDalImplementation(dal));
        return context;
    }
    
    
    @POST
    public CMSResponse createBranch(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, 
            Branch branch, 
            @Context HttpServletRequest request){
        
        CMSPriority p = CMSResourceUtils.parsePriority(priority);
        CMSResponse response = new CMSResponse();
        ConsistentPolicy policy = CMSResourceUtils.parsePolicy(cmsServer, consistPolicy);

        try {
            EntityContext context = createContext(uriInfo, request, policy);

            if (!reponame.equals(branch.getRepositoryName())) {
                throw new BadParamException("repository name not consistency");
            }
            
            IBranch b = cmsServer.createBranch(p, branch, context);
            
            response.addResult(b);
            return response;
        }
        catch (Throwable e) {
            logger.error(MessageFormat.format("exception while create branch {0} on repository {1}", branch.getId(), reponame), e);
            convertExceptionAndReThrow(e);
            return null;
        }
    }

}
