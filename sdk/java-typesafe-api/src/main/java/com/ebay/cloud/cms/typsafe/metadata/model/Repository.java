package com.ebay.cloud.cms.typsafe.metadata.model;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author liasu
 * 
 */
public class Repository {
    @JsonProperty
    private String repositoryName;
    @JsonProperty
    private RepositoryOption options;

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public RepositoryOption getOptions() {
        return options;
    }

    public void setOptions(RepositoryOption options) {
        this.options = options;
    }

}
