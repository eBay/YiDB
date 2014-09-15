package com.ebay.cloud.cms.typsafe.restful;

import java.text.MessageFormat;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

public class TokenProcessor extends AbstractResponseProcessor {

    private JsonNode           rootNode;
    private String             token;

    public TokenProcessor(ClientResponse resp, HttpRequest request, CMSClientContext context) {
        super(resp, request, context);

        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new CMSClientException(resp.getStatus(), MessageFormat.format(
                    "parse response string error for token retrieve, the response string is {0}", jsonResponse), jsonResponse, requestInfo);
        }

        parseResponseHeader();
        parseResponseBody();
    }

    @SuppressWarnings("deprecation")
    private void parseResponseBody() {
        JsonNode node = rootNode.get("token");
        if (node == null) {
            throw new CMSClientException(response.getStatus(), "no token returned!", jsonResponse, requestInfo);
        }
        token = node.getValueAsText();
    }

    @Override
    protected JsonNode getRootNode() {
        return rootNode;
    }

    public String getBuildEntity() {
        return token;
    }

}
