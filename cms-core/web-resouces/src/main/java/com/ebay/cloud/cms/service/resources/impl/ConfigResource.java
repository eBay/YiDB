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


/**
 * 
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.resources.IConfigResource;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

/**
 * Apis for global system configuration. It assumes the configuration are
 * flatten as key=>value pair.
 * 
 * TODO: Every component should be able to register its own config into system
 * management module.
 * 
 * @author liasu
 * 
 */
@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource implements IConfigResource {

    private static ObjectMapper mapper    = new ObjectMapper();
    private CMSServer           cmsServer = CMSServer.getCMSServer();

    @GET
    public CMSResponse getConfigurations(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
        CMSResponse resp = new CMSResponse();
        resp.addResult(cmsServer.getCurrentConfigurations());
        return resp;
    }

    @POST
    public CMSResponse setConfig(@Context UriInfo uriInfo, String body, @Context HttpServletRequest request) {
        
        JsonNode node;
        try {
            node = mapper.readTree(body);
        } catch (Exception e) {
            throw new BadParamException("unknown configuration body" + body);
        }

        Map<String, Object> configMap = new HashMap<String, Object>();
        Set<String> supportConfigs = cmsServer.getConfigNames();
        Iterator<Map.Entry<String, JsonNode>> nameItr = node.getFields();
        while (nameItr.hasNext()) {
            Map.Entry<String, JsonNode> name = nameItr.next();
            if (!supportConfigs.contains(name.getKey())) {
                throw new BadParamException(MessageFormat.format("config {0} is not supported", name.getKey()));
            }

            JsonNode vNode = name.getValue();
            Object value = null;
            if (vNode.isBoolean()) {
                value = vNode.asBoolean();
            } else if (vNode.isInt() || vNode.isIntegralNumber()) {
                value = vNode.asInt();
            } else if (vNode.isLong()) {
                value = vNode.asLong();
            } else if (vNode.isDouble() || vNode.isFloatingPointNumber()) {
                value = vNode.asDouble();
            } else {
                value = vNode.asText();
            }
            configMap.put(name.getKey(), value);
        }
        try {
            cmsServer.config(configMap);
        } catch (IllegalArgumentException e) {
            throw new BadParamException(e, e.getMessage());
        }

        return new CMSResponse();
    }
}
