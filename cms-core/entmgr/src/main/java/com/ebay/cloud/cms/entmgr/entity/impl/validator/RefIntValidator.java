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

package com.ebay.cloud.cms.entmgr.entity.impl.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;

/**
 * 
 * @author zhuang1
 *
 */

public class RefIntValidator {

	private final ISearchService searchService;

	public RefIntValidator(ISearchService service) {
		searchService = service;
	}

    public void checkReferenceIntegrity(IEntity entity, PersistenceContext context) throws CmsDalException {
        // check from reference integrity
        List<String> ances = entity.getMetaClass().getAncestors();
        List<String> ancestors = new ArrayList<String>(ances == null ? Collections.<String> emptyList() : ances);
        ancestors.add(entity.getType());
        IMetadataService metaService = entity.getMetaClass().getMetadataService();
        for (String meta : ancestors) {
            MetaClass metaClass = metaService.getMetaClass(meta);
            List<MetaRelationship> fromReferences = getStrongFromReferences(metaClass);
            for (MetaRelationship fromRef : fromReferences) {
                checkFromStrongReferencesInDB(context, entity, fromRef);
            }
        }
    }
    
    //
    // TODO: the refDBPath should include the parent class if it's embedded object
    //
	private void checkFromStrongReferencesInDB(PersistenceContext context, IEntity entity, MetaRelationship metaRef) {
	    // call search service to get the referencing entity
        MetaClass fromClass = metaRef.getSourceMetaClass();
        MetaField idField = fromClass.getFieldByName(InternalFieldEnum.ID.getName());
        SearchOption option = new SearchOption();
        option.setLimit(1);
        SearchProjection projection = new SearchProjection();
        ISearchStrategy strategy = context.getRegistration().searchStrategy;
        projection.addField(new ProjectionField(idField, true, strategy));
        FieldSearchCriteria criteria = new FieldSearchCriteria(new SelectionField(metaRef, strategy), FieldOperatorEnum.EQ, entity.getId());
		SearchQuery query = new SearchQuery(fromClass, criteria, projection, strategy);
        SearchResult result = searchService.search(query, option, context);
        
        if (result.getResultSet().size() > 0) {
            StringBuilder errInfo = new StringBuilder();
            errInfo.append(" entity = ").append(result.getResultSet().get(0).getId());
            errInfo.append(" type = ").append(fromClass.getName());
            errInfo.append(" reference = ").append(metaRef.getName());
            throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, errInfo.toString());
        }
	}

    private List<MetaRelationship> getStrongFromReferences(MetaClass metaCls) {
        List<MetaRelationship> refFieldList = new LinkedList<MetaRelationship>();      
        for (MetaRelationship metaRef : metaCls.getFromReference()) {
            if (metaRef.getRelationType() == RelationTypeEnum.Reference
                    && metaRef.getConsistencyType() == MetaRelationship.ConsistencyTypeEnum.Strong) {
                refFieldList.add(metaRef);                 
            }            
        }
        return refFieldList;
    }

}