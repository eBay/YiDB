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

package com.ebay.cloud.cms.entmgr.branch.impl;

import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.persistence.impl.PersistenceService.Registration;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.metadata.service.IMetadataService;

public class PersistenceContextFactory {
    
	public static PersistenceContext createEntityPersistenceConext(IMetadataService metaService, IBranch branch,
            ConsistentPolicy consistPolicy, Registration registration, boolean needFieldProperty, CMSDBConfig dbConfig, Map<String, List<SearchCriteria>> additionaCriteria) {
        return createEntityPersistenceConext(metaService, branch.getId(), consistPolicy, registration, needFieldProperty, dbConfig, additionaCriteria);
    }
	
	public static PersistenceContext createEntityPersistenceConext(IMetadataService metaService, String baseName,  
            ConsistentPolicy consistPolicy, Registration registration, boolean needFieldProperty, CMSDBConfig dbConfig,
            Map<String, List<SearchCriteria>> additionaCriteria) {
        PersistenceContext context = new PersistenceContext(metaService, DBCollectionPolicy.SplitByMetadata, consistPolicy,
                baseName);
        context.setRegistration(registration);
        context.setFetchFieldProperties(needFieldProperty);
        context.setDbConfig(dbConfig);
        context.setAdditionalCriteria(additionaCriteria);
        return context;
    }
}
