package com.ebay.cloud.cms.typsafe.restful;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;

/**
 * @author liasu
 * 
 */
public class MetadataJsonBuilder implements IJsonBuilder {

    public MetadataJsonBuilder() {
    }

    @Override
    public String buildJson(Object object) {
        try {
            return Constants.objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new CMSClientException("", e);
        }
    }

}
