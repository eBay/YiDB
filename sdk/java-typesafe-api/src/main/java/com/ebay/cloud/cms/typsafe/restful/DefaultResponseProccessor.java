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
package com.ebay.cloud.cms.typsafe.restful;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author liasu
 * 
 */
public class DefaultResponseProccessor extends AbstractResponseProcessor {

    private JsonNode rootNode;

    public DefaultResponseProccessor(ClientResponse resp, HttpRequest request, CMSClientContext context) {
        super(resp, request, context);
        rootNode = readJson(jsonResponse);
        parseResponseHeader();
    }

    @Override
    protected JsonNode getRootNode() {
        return rootNode;
    }

}
