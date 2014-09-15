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

package com.ebay.cloud.cms.service.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ebay.cloud.cms.service.CMSResponse;

/**
 * User: Rene Xu
 * Email: rene.xu@ebay.com
 * Date: 5/30/12 4:44 PM
 */
public class NotFoundException extends WebApplicationException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String msg) {
            super(createResponse(Response.Status.NOT_FOUND.getStatusCode(), null, msg));
    }

    public NotFoundException(Throwable cause, String msg) {
            super(cause, createResponse(Response.Status.NOT_FOUND.getStatusCode(), cause, msg));
    }

    public NotFoundException(int errorCode, Throwable cause, String msg) {
        super(cause, createResponse(errorCode, cause, msg));
    }

    private static Response createResponse(int errorCode, Throwable cause, String msg) {
        CMSResponse jr = new CMSResponse(String.valueOf(errorCode), msg, cause);
        return Response.status(404).entity(jr).type(MediaType.APPLICATION_JSON).build();
    }

}