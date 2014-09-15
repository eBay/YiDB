/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;

/**
 * @author liasu
 * 
 */
public class FieldJsonBuilder extends JsonBuilder {

    protected final String fieldName;

    public FieldJsonBuilder(CMSClientConfig config, String fieldName) {
        super(config);
        this.fieldName = fieldName;
    }

    @Override
    public String buildJson(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        JsonNode node = buildJsonNode(object);
        if (node.has(fieldName)) {
            return node.get(fieldName).toString();
        }
        return null;
    }
}
