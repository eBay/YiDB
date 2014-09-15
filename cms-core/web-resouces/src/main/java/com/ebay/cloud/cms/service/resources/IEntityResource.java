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
import javax.ws.rs.GET;
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
public interface IEntityResource {

    /**
     * Get branch api, this api semantically belongs to BranchResource, but
     * caused by api duplication and jersey path matching algorithm. Jersey will
     * only look into the EntityResource(this round called
     * RootResourceClassRule), and if it found nothing matched, throw a
     * exception and never look into BranchReource
     */
    @GET
    public Response getBranch(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @PathParam("reponame") String reponame, @PathParam("branch") String branchId,
            @Context HttpServletRequest request);

    /**
     * Gets an entity based on its oid
     * 
     * @param uriInfo
     * @param reponame
     * @param branchname
     * @param metadata
     * @param oid
     * @param priority
     * @param fetchHistory
     *            - query parameter to decide whether fetch history or the
     *            entity itself
     * @param modeVal
     * @param request
     * @return
     */
    public Response getEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    /**
     * Delete an entity
     * 
     * @param uriInfo
     * @param reponame
     * @param branchname
     * @param metadata
     * @param oid
     * @param priority
     * @param auth
     * @param modeVal
     * @param request
     * @return
     */
    public Response deleteEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    /**
     * Batch delete entities
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param priority
     * @param consistPolicy
     * @param jsonString
     * @param modeVal
     * @param failReturnOptionVal
     * @param request
     * @return
     */
    public Response batchDeleteEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
    		@PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
    		@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
    		String jsonString, @QueryParam("mode") String modeVal,
    		@QueryParam("failReturnOption") final String failReturnOptionVal,
    		@Context HttpServletRequest request);

    /**
     * Creates an entity
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param metadata
     * @param priority
     * @param auth
     * @param jsonString
     * @param modeVal
     * @param request
     * @return
     */
    public Response createEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, @QueryParam("mode") String modeVal, @Context HttpServletRequest request);

    /**
     * Replaces an entity
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param metadata
     * @param oid
     * @param priority
     * @param auth
     * @param jsonString
     * @param modeVal
     * @param request
     * @return
     */
    public Response replaceEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);

    /**
     * Modifies an entity
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param metadata
     * @param oid
     * @param priority
     * @param auth
     * @param jsonString
     * @param modeVal
     * @param request
     * @return
     */
    public Response modifyEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);

    /**
     * Batch gets the entities
     * 
     * @param uriInfo
     * @param reponame
     * @param branchname
     * @param metadata
     * @param priority
     * @param modeVal
     * @param request
     * @return
     */
    public Response batchGetEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @QueryParam("mode") final String modeVal,
            @Context final HttpServletRequest request);

    /**
     * Batch creates entities
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param priority
     * @param auth
     * @param jsonString
     * @param modeVal
     * @param request
     * @return
     */
    public Response batchCreateEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, @QueryParam("mode") String modeVal,
            @QueryParam("failReturnOption") final String failReturnOptionVal,
            @Context HttpServletRequest request);

    /**
     * Batch update entities
     * 
     * @param uriInfo
     * @param reponame
     * @param branch
     * @param priority
     * @param auth
     * @param jsonString
     * @param modeVal
     * @param request
     * @return
     */
    public Response batchModifyEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, @QueryParam("mode") String modeVal,
            @QueryParam("failReturnOption") final String failReturnOptionVal,
            @Context HttpServletRequest request);
    
    public Response modifyEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, 
            @PathParam("fieldname") final String fieldName,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @HeaderParam("X-CMS-CONDITIONAL-UPDATE") final String casMode,
            String jsonString, 
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    public Response deleteEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, 
            @PathParam("fieldname") final String fieldName,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, 
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);

    public Response pushEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, 
            @PathParam("fieldname") final String fieldName,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            @HeaderParam("X-CMS-CONDITIONAL-UPDATE") final String casMode,
            String jsonString, 
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    public Response pullEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
            @PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
            @PathParam("oid") final String oid, 
            @PathParam("fieldname") final String fieldName,
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
            String jsonString, 
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);

}