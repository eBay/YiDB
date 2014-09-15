/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.text.MessageFormat;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author liasu
 * 
 */
public class MetaResponseProcessor extends AbstractResponseProcessor {

    private final JsonNode rootNode;
    private MetadataManager mm;

    public MetaResponseProcessor(ClientResponse resp, HttpRequest request, CMSClientContext context) {
        super(resp, request, context);

        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new CMSClientException(resp.getStatus(), MessageFormat.format(
                    "parse response string error for metadata retrieve, the response string is {0}", jsonResponse), jsonResponse, requestInfo);
        }
        parseResponseHeader();
        parseResponseBody();
    }

    private void parseResponseBody() {
        JsonNode resultNode = rootNode.get(RESULT);
        if (resultNode != null && resultNode.isArray() && resultNode.size() > 0) {
            mm = MetadataManager.load(resultNode);
        }
    }

    public MetadataManager getMetaManager() {
        return mm;
    }

    @Override
    protected JsonNode getRootNode() {
        return rootNode;
    }

}
