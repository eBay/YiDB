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
package com.ebay.cloud.cms.sysmgmt.server.helper;

import java.text.MessageFormat;

import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;

public class HttpRequest {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);
    
    @JsonProperty
    final String method;
    @JsonProperty
    final String url;
    @JsonProperty
    final String payload;

    // batch request
    public HttpRequest(String method, String repo, String branch, Object payload) {
        this.method = method;
        this.url = MessageFormat.format("/repositories/{0}/branches/{1}/entities", repo, branch);
        this.payload = payload.toString();
    }

    // entity create request
    HttpRequest(String method, String repo, String branch, String type, Object payload) {
        this.method = method;
        this.url = MessageFormat.format("/repositories/{0}/branches/{1}/{2}/", repo, branch, type);
        if (payload != null) {
            this.payload = payload.toString();
        } else {
            this.payload = null;
        }
    }

    // entity change request
    HttpRequest(String method, String repo, String branch, String type, String oid, Object payload) {
        this.method = method;
        this.url = MessageFormat.format("/repositories/{0}/branches/{1}/{2}/{3}", repo, branch, type, oid);
        if (payload != null) {
            this.payload = payload.toString();
        } else {
            this.payload = null;
        }
    }

    // field request
    HttpRequest(String method, String repo, String branch, String type, String oid, String field, Object payload) {
        this.method = method;
        this.url = MessageFormat.format("/repositories/{0}/branches/{1}/{2}/{3}/{4}", repo, branch, type, oid,
                field);
        if (payload != null) {
            this.payload = payload.toString();
        } else {
            this.payload = null;
        }
    }

    public String toString() {
        try {
            return ObjectConverter.mapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("unable to deserialize the request object!", e);
        }
        return method + ":" + url + "\n" + payload;
    }
}
