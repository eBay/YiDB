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

public class CMSServerException extends WebApplicationException {
    
    public static final int UNKNOWN_ERROR = 500;
    
    private static final long serialVersionUID = 1L;

    public CMSServerException(Throwable cause) {
        super(cause, createResponse(UNKNOWN_ERROR, "CMS server exception!", cause));
    }
    
    public CMSServerException(int errorCode, String msg, Throwable cause) {
            super(cause, createResponse(errorCode, msg, cause));
    }

    private static Response createResponse(int errorCode, String msg, Throwable cause) {
            CMSResponse jr = new CMSResponse(String.valueOf(errorCode), msg, cause);
            return Response.status(500).entity(jr).type(MediaType.APPLICATION_JSON).build();
    }
}
