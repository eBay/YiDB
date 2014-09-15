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
import javax.ws.rs.core.UriInfo;

import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.service.CMSResponse;

/**
 * @author liasu
 * 
 */
public interface IMetadataResource {

    /**
     * Get all repositries
     * @param priority
     * @return
     */
    public CMSResponse getRepositories(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @Context HttpServletRequest request);

    public CMSResponse getRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @Context HttpServletRequest request);

    public CMSResponse createRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            Repository repository, @Context HttpServletRequest request);
    
    public CMSResponse updateRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, Repository newRepo, @Context HttpServletRequest request);

    public CMSResponse getMetaClasses(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);

    public CMSResponse getMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @PathParam("metatype") String metatype,
            @QueryParam("fetchHistory") boolean fetchHistory, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    public CMSResponse getMetaClassReference(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metatype") String metatype,
            @Context HttpServletRequest request);

    public CMSResponse getMetadataHierarchy(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @PathParam("metatype") String metatype,
            @Context HttpServletRequest request);

    public CMSResponse createMetaClass(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            String metadataString, @QueryParam("mode") String modeVal, @Context HttpServletRequest request);

    public CMSResponse updateMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metaclass") String metaclassName, String metaClassString, @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    public CMSResponse deleteMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metaclass") String metaclassName,
            @Context HttpServletRequest request);
    
    public CMSResponse deleteMetaField(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metaclass") String metaclassName,
            @PathParam("fieldname") String fieldName,
            @Context HttpServletRequest request);

    public CMSResponse getMetadataIndex(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
            @Context HttpServletRequest request);

    public CMSResponse createMetadataIndex(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metaclass") String metaclassName, String indexString, @Context HttpServletRequest request);

    public CMSResponse deleteMetadataIndex(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            @PathParam("metaclass") String metaclassName, @PathParam("indexName") String indexName,
            @Context HttpServletRequest request);
    
    public CMSResponse updateMetadata(@Context UriInfo uriInfo, 
            @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, 
            @PathParam("metaclass") String metaclassName, 
            @PathParam("fieldname") String fieldName,
            String metaFieldString,
            @QueryParam("mode") String modeVal,
            @Context HttpServletRequest request);
    
    public CMSResponse validateMetaClass(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame,
            String metadataString, @QueryParam("mode") String modeVal, @Context HttpServletRequest request);


}
