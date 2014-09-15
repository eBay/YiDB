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

package com.ebay.cloud.cms.entmgr.history;

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.IMetadataHistoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;

public interface IEntityHistoryService extends IMetadataHistoryService {
    
    /**
     * Gets the history changes of the meta class
     * 
     * @param repoName
     * @param metaName
     * @param context
     * @return - a list of the history
     */
    List<IEntity> getHistoryEntities(String repoName, String metaName, MetadataContext context);
    
    
    /**
     * Adds a metaclass history item
     * 
     * @param meta
     * @param operType
     * @param context
     * @return
     */
    String addHistory(MetaClass meta, String operType, MetadataContext metaContext);


}
