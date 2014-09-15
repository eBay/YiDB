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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.ebay.cloud.cms.entmgr.branch.impl.Branch;
import com.ebay.cloud.cms.service.CMSResponse;

/**
 * @author liasu
 * 
 */
public interface IBranchResource {

    /**
     * Lists all branches on the given repository
     * 
     * @param priority
     *            - X-CMS-PRIORITY header parameter
     * @param reponame
     *            - path parameter of repo
     * @param request
     *            - request context
     * @return
     */
    public CMSResponse getMainBranches(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, @Context HttpServletRequest request);

    /**
     * Creates a branch on the given repository
     * 
     * @param priority
     *            - X-CMS-PRIORITY header parameter
     * @param auth
     *            - Authentication header parameter
     * @param reponame
     * @param branch
     * @param request
     * @return
     */
    public CMSResponse createBranch(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, 
            Branch branch, 
            @Context HttpServletRequest request);

}
