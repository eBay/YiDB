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

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.IServiceResource;
import com.ebay.cloud.cms.sysmgmt.IManagementServices;
import com.ebay.cloud.cms.sysmgmt.IManagementServices.ServiceStatus;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

@Path("/services/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceResource implements IServiceResource {

    private CMSServer cmsServer = CMSServer.getCMSServer();
    
    private static ObjectMapper mapper = new ObjectMapper();

    @GET
    public CMSResponse getServices(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
        CMSResponse response = new CMSResponse();
        Map<String, ServiceStatus> statuses = cmsServer.getServiceStatuses();
        for (Entry<String, ServiceStatus> e : statuses.entrySet()) {
            response.put(e.getKey(), e.getValue());
        }
        return response;
    }
    
    @GET
    @Path("/{serviceName}/")
    public CMSResponse getService(@Context UriInfo uriInfo, @PathParam("serviceName") String serviceName, @Context HttpServletRequest request) {
        ServiceStatus status = cmsServer.getServiceStatus(serviceName);
        if (status == null) {
            throw new NotFoundException("service not found: " + serviceName);
        }
        
        CMSResponse response = new CMSResponse();
        response.put(serviceName, status);
        return response;
    }
    
    @PUT
    @Path("/{serviceName}/")
    public CMSResponse setStatus(@Context UriInfo uriInfo, @PathParam("serviceName") String serviceName, String jsonState,
            @Context HttpServletRequest request) {
        
        if (serviceName == null || serviceName.isEmpty() || cmsServer.getServiceStatus(serviceName) == null) {
            throw new NotFoundException("service not found: " + serviceName);
        }
        
        JsonNode node;
        try {
            node = mapper.readTree(jsonState);
        } catch (Exception e) {
            throw new BadParamException("unknown status format " + jsonState);
        }
        
        JsonNode stateNode = node.get(serviceName);
        if (stateNode == null) {
            throw new BadParamException("unknown status format " + jsonState);
        }
        
        String state = stateNode.getTextValue();
        IManagementServices.ServiceStatus status;
        
        try {
            status = IManagementServices.ServiceStatus.valueOf(state);
        } catch (IllegalArgumentException e) {
            throw new BadParamException("unknown status format " + jsonState);
        }
        
        cmsServer.setServiceStatus(serviceName, status);
        
        return new CMSResponse();
    }
    
}
