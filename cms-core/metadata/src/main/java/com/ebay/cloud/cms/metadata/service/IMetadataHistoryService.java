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

package com.ebay.cloud.cms.metadata.service;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;

public interface IMetadataHistoryService {

    /**
     * Adds a metaclass history item
     * 
     * @param meta
     * @param operType
     * @param context
     * @return
     */
    String addHistory(MetaClass meta, String operType, MetadataContext context);

    /**
     * Gets the specific version of the meta class
     * 
     * @param repoName
     * @param metaName
     * @param context 
     * @param context
     * @return - a list of the history
     */
    MetaClass getHistory(String repoName, String metaName, int version, MetadataContext context);
    
    void ensureHistoryIndex(Repository repo, MetaClass meta);

}
