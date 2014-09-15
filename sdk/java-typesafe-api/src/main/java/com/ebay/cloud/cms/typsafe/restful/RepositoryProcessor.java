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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.metadata.model.Repository;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author liasu
 * 
 */
public class RepositoryProcessor extends AbstractResponseProcessor {
    private static final Logger logger       = LoggerFactory.getLogger(RepositoryProcessor.class);

    private final JsonNode      rootNode;

    private List<Repository>    resultRepos;

    public RepositoryProcessor(ClientResponse resp, HttpRequest request, CMSClientContext context) {
        super(resp, request, context);
        this.resultRepos = new ArrayList<Repository>();
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new CMSClientException(resp.getStatus(), MessageFormat.format(
                    "parse response string error for metadata retrieve, the response string is {0}", jsonResponse),
                    jsonResponse, requestInfo);
        }
        parseResponseHeader();
        parseResponseBody();
    }

    private void parseResponseBody() {
        JsonNode resultNode = rootNode.get(RESULT);
        if (resultNode != null && resultNode.isArray() && resultNode.size() > 0) {
            ArrayNode array = (ArrayNode) resultNode;
            Iterator<JsonNode> it = array.iterator();
            while (it.hasNext()) {
                JsonNode node = it.next();
                try {
                    Repository repo = objectMapper.readValue(node, Repository.class);
                    resultRepos.add(repo);
                } catch (Exception e) {
                    logger.error("load repo error!" + node, e);
                    throw new CMSClientException("load repo error!" + node, e);
                }
            }
        }
    }

    /**
     * @return the resultRepos
     */
    public List<Repository> getResultRepos() {
        return resultRepos;
    }

    @Override
    protected JsonNode getRootNode() {
        return rootNode;
    }

}
