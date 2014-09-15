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
package com.ebay.cloud.cms.typsafe.metadata.model;

import java.util.List;

/**
 * @author liasu
 * 
 */
public class IndexInfo {
    private String indexName;
    private List<String> keyList;
    private List<String> indexOptions;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public List<String> getKeyList() {
        return keyList;
    }

    public void setKeyList(List<String> keyList) {
        this.keyList = keyList;
    }

    public List<String> getIndexOptions() {
        return indexOptions;
    }

    public void setIndexOptions(List<String> indexOptions) {
        this.indexOptions = indexOptions;
    }

}
