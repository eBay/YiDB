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


/**
 * 
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
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * @author liasu
 * 
 */
public class LeftOutterJoinAction extends AbstractJoinAction {

    public LeftOutterJoinAction(QueryContext context, ParseBaseNode joinNode, boolean directional) {
        super(context, QueryActionTypeEnum.LEFTJOIN, joinNode, directional);
    }

    @Override
    protected SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        return parentAction.getTailJoinCriteria(referenceField);
    }

    @Override
    protected SearchCriteria getHeadJoinCriteria() {
        // left outter has no outer join criteria
        return NoJoinSearchCriteria.JOIN_CRITERIA;
    }

    @Override
    public void optimize() {
        // calculate sub tree cost
        this.subTreeCost = QueryCostEnum.FullScan.getValue();
        // setup and or join map
        AbstractAction childAction = childrenActions.get(0);
        this.andOrJoinMap = childAction.andOrJoinMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean joinField(IEntity fromEntity, Map<String, IEntity> idToEntityMap, String joinFieldName, List<IEntity>results) {
        List<IEntity> oldJoinEntities = (List<IEntity>) fromEntity.getFieldValues(joinFieldName);
        boolean hasUserProjection = isUserProjection(joinFieldName);
        if (!oldJoinEntities.isEmpty()) {
            for (int i = 0; i < oldJoinEntities.size(); i++) {
                IEntity oldRef = oldJoinEntities.get(i);
                String id = oldRef.getId();
                IEntity newEntity = idToEntityMap.get(id);
                if (newEntity != null) {
                    results.add(newEntity);
                } else if (hasUserProjection) {
                    // only preserve the old value when it's user projection
                    results.add(oldRef);
                }
            }

        }
        // left join will always return true
        return true;
    }

    private boolean isUserProjection(String joinFieldName) {
        ParseQueryNode parentNode = getParentQueryNode();
        SearchProjection userProjection = parentNode.getUserProjection();
        return (userProjection.hasStar() || userProjection.hasField(joinFieldName));
    }

    @Override
    public String toString() {
        return "LeftOutterJoinAction";
    }

    @Override
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta, 
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        return getChildrenActions().get(0).getHeadJoinQuery(prevEmbedQuery, meta, embedFieldList, true);
    }

    @Override
    public boolean isChildrenCriteriaReady() {
        return childrenActions.get(0).isChildrenCriteriaReady();
    }

}
