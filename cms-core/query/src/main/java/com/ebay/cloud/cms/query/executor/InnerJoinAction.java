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

package com.ebay.cloud.cms.query.executor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author xjiang
 * 
 */
public class InnerJoinAction extends AbstractJoinAction {

    public InnerJoinAction(QueryContext context, ParseBaseNode joinNode, boolean bidirectional) {
        super(context, QueryActionTypeEnum.INNERJOIN, joinNode, bidirectional);
    }

    @Override
    public SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        return parentAction.getTailJoinCriteria(referenceField);
    }

    @Override
    public SearchCriteria getHeadJoinCriteria() {
        return childrenActions.get(0).getHeadJoinCriteria();
    }
    
    @Override
    public String toString() {
        return "InnerJoinAction";
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected boolean joinField(IEntity fromEntity, Map<String, IEntity> idToEntityMap, String joinFieldName, List<IEntity> results) {
        boolean match = false;
        List<IEntity> oldJoinEntities = (List<IEntity>) fromEntity.getFieldValues(joinFieldName);
        if (!oldJoinEntities.isEmpty()) {
            for (IEntity oldRef : oldJoinEntities) {
                String id = oldRef.getId();
                IEntity newEntity = idToEntityMap.get(id);
                if (newEntity != null) {
                    match = true;
                    results.add(newEntity);
                }
            }
        }
        return match;
    }

    @Override
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta,
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        return getChildrenActions().get(0).getHeadJoinQuery(prevEmbedQuery, meta, embedFieldList, false);
    }

    @Override
    public boolean isChildrenCriteriaReady() {
        return childrenActions.get(0).isChildrenCriteriaReady();
    }

}
