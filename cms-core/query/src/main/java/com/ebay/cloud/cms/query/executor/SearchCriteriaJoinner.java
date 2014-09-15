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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.JoinSearchCriteria;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.AbstractAction.ActionResult;
import com.ebay.cloud.cms.query.metadata.ReverseMetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;

/**
 * @author liasu
 * 
 */
public class SearchCriteriaJoinner {
    protected SearchAction             action;
    protected final ParseQueryNode     parseNode;

    SearchCriteriaJoinner(SearchAction action, ParseQueryNode node) {
        this.action = action;
        this.parseNode = node;
    }

    SearchCriteria getHeadJoinCriteria() {
        // not executed
        if (!action.isSelfDone()) { 
            return null;
        }
        // executed with empty result
        if (action.getActionResults().isEmpty()) {
            return EmptySearchCriteria.EMPTY_CRITERIA;
        }

        Collection<String> ids = new HashSet<String>();
        ActionResult actionResult = action.getActionResults().get(0);
        MetaRelationship referenceField = parseNode.getMetaReference();
        List<IEntity> entityList = actionResult.searchResult.getResultSet();
        for (IEntity entity : entityList) {
            addHeadJoinId(entity, referenceField, ids);
        }
        if (ids.isEmpty()) {
            return EmptySearchCriteria.EMPTY_CRITERIA;
        }

        return new JoinSearchCriteria(getJoinField(referenceField, true), ids, action.getQueryStrategy());
    }

    /**
     * Returns the join field.
     * 
     * <pre>
     *   ----------------------------------------------------------------------------
     *   |                           |  Normal (A.b)        |   Reverse (A.a!B)     |
     *   ----------------------------------------------------------------------------
     *   | fromChildToParent = true  |  return field b      | return A's ID field   |
     *   ----------------------------------------------------------------------------
     *   | fromChildToParent = false |  return B's ID field | return field a        |
     *   ----------------------------------------------------------------------------
     * </pre>
     */
    private MetaField getJoinField(MetaRelationship referenceField, boolean fromChildToParent) {
        if (fromChildToParent) {
            if (referenceField instanceof ReverseMetaRelationship) {
                return ((ReverseMetaRelationship) referenceField).getReversedReference().getSourceMetaClass()
                        .getFieldByName(InternalFieldEnum.ID.getName());
            } else {
                return referenceField;
            }
        } else {
            if (referenceField instanceof ReverseMetaRelationship) {
                return ((ReverseMetaRelationship) referenceField).getReversedReference();
            } else {
                return referenceField.getSourceMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addHeadJoinId(IEntity entity, MetaRelationship referenceField, Collection<String> ids) {
		if (referenceField instanceof ReverseMetaRelationship) {
		    ReverseMetaRelationship reverseReference = (ReverseMetaRelationship) parseNode.getMetaReference();
            MetaRelationship reference = reverseReference.getReversedReference();
            // For A.a!B, add B's field value of a
            List<IEntity> refEntities = (List<IEntity>) entity.getFieldValues(reference.getName());
            for (IEntity ref : refEntities) {
                ids.add(ref.getId());
            }
        } else {
            // For A.b, add B's id
            ids.add(entity.getId());
        }
    }

    SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        // not executed
        if (!action.isSelfDone()) {
            return null;
        }
        // executed with empty result
        if (action.getActionResults().isEmpty()) {
            return EmptySearchCriteria.EMPTY_CRITERIA;
        }

        ActionResult actionResult = action.getActionResults().get(0);
        List<IEntity> entityList = actionResult.searchResult.getResultSet();
        Collection<String> ids = new HashSet<String>();
        for (IEntity entity : entityList) {
            addTailJoinIds(entity, referenceField, ids);
        }
        if (ids.isEmpty()) {
            return EmptySearchCriteria.EMPTY_CRITERIA;
        }
        return new JoinSearchCriteria(getJoinField(referenceField, false), ids, action.getQueryStrategy());
    }

    @SuppressWarnings("unchecked")
    private void addTailJoinIds(IEntity entity, MetaRelationship referenceField, Collection<String> ids) {
        if (referenceField instanceof ReverseMetaRelationship) {
            // For A.b!B -- add A's id
            ids.add(entity.getId());
        } else {
            // For A.b -- add A's field value of b
            List<IEntity> refValues = (List<IEntity>) entity.getFieldValues(referenceField.getName());
            for (IEntity ref : refValues) {
                ids.add(ref.getId());
            }
        }
    }

}
