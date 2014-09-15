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

package com.ebay.cloud.cms.query.metadata;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * A query meta relationship represents a relationship that override some character of the <code>MetaRelationship</code>.
 * 
 * One obivious override is the cross repository handling.
 * 
 * @author liasu
 *
 */
public class QueryMetaRelationship extends MetaRelationship {
    
    private QueryContext context;
    
    public QueryMetaRelationship(MetaRelationship relationship, QueryContext context) {
        super(relationship);
        this.context = context;
    }
    
    @Override
    @JsonIgnore
    protected IMetadataService getRefMetaService() {
        IMetadataService metaService = metadataService;
        if (getRelationType() == RelationTypeEnum.CrossRepository) {
            CheckConditions.checkState(refRepository != null, "Cross repository relationship must have refRepository set!");
            metaService = context.getMetadataService(refRepository);
        }
        return metaService;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        
        return super.equals(other);
    }
    
    @Override
    public int hashCode() {
        if (getName() == null) {
            return "QueryMetaRelationship".hashCode();
        }
        
        return super.hashCode();
    }
}
