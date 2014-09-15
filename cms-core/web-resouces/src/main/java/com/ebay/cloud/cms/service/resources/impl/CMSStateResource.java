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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.resources.IStateResource;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.sysmgmt.state.CMSState;

@Path("/states/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CMSStateResource implements IStateResource {

    private CMSServer cmsServer = CMSServer.getCMSServer();
    
    private static ObjectMapper mapper = new ObjectMapper();

    @GET
    public CMSResponse getStates(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
        CMSResponse response = new CMSResponse();
        response.put("state", cmsServer.getState().toString());
        return response;
    }
    
    @PUT
    public CMSResponse changeState(@Context UriInfo uriInfo, String jsonState, @Context HttpServletRequest request) {
        
        JsonNode node;
        try {
            node = mapper.readTree(jsonState);
        } catch (Exception e) {
            throw new BadParamException("unknown state format " + jsonState);
        }
        
        JsonNode stateNode = node.get("state");
        if (stateNode == null) {
            throw new BadParamException("unknown state format " + jsonState);
        }
        
        String state = stateNode.getTextValue();
        CMSState.State cmsState;
        
        try {
            cmsState = CMSState.State.valueOf(state);
        } catch (IllegalArgumentException e) {
            throw new BadParamException("unknown state format " + jsonState);
        }
        
        switch (cmsState) {
        case maintain:
            cmsServer.pause();
            break;
        case normal:
            cmsServer.resume();
            break;
        case shutdown:
            cmsServer.shutdown();
            break;
        default:
            throw new BadParamException("can not change state to " + state);
        }

        return new CMSResponse();
    }
    
}
