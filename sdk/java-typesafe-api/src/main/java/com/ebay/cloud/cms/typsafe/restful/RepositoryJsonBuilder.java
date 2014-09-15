package com.ebay.cloud.cms.typsafe.restful;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;

/**
 * @author liasu
 * 
 */
public class RepositoryJsonBuilder implements IJsonBuilder {

//    private final Repository repo;

    public RepositoryJsonBuilder() {
//        this.repo = repo;
    }

    @Override
    public String buildJson(Object object) {
        try {
            return Constants.objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new CMSClientException(" convert repository to json string failed.", e);
        }
    }

}
