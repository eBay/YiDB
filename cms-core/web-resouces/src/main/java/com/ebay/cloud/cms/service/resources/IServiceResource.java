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

package com.ebay.cloud.cms.service.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.ebay.cloud.cms.service.CMSResponse;

/**
 * @author liasu
 * 
 */
public interface IServiceResource {

    /**
     * Gets all services components
     * 
     * @return
     */
    public CMSResponse getServices(@Context UriInfo uriInfo, @Context HttpServletRequest request);

    /**
     * Gets the given service component
     * 
     * @param serviceName
     * @return
     */
    public CMSResponse getService(@Context UriInfo uriInfo, @PathParam("serviceName") String serviceName, @Context HttpServletRequest request);

    /**
     * Sets the status of the given service component
     * 
     * @param auth
     * @param serviceName
     * @param jsonState
     * @return
     */
    public CMSResponse setStatus(@Context UriInfo uriInfo, @PathParam("serviceName") String serviceName, String jsonState, @Context HttpServletRequest request);

}
