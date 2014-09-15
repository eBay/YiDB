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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author liasu
 * 
 */
public interface IQueryResource {

    /**
     * Executes the queries
     * 
     * @param cmsPriority
     * @param reponame
     * @param branch
     * @param query
     * @param ui
     * @param modeVal
     * @param request
     * @return
     */
    public Response query(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String cmsPriority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, @PathParam("branch") String branch,
            @PathParam("query") String query, @Context UriInfo ui, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    /**
     * Executes the queries - the query string is expected in pay-load rather than URL compartment to avoid
     * too long query string exceeds URL length limit.
     * 
     * This API expected to have the same behavior to the <code>query()</code> in server side.
     * 
     * @param cmsPriority
     * @param reponame
     * @param branch
     * @param query
     * @param ui
     * @param modeVal
     * @param request
     * @return
     */
    public Response queryEntity(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String cmsPriority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, @PathParam("branch") String branch,
            String query, @Context UriInfo ui, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
}
