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

package com.ebay.cloud.cms.query.executor.result;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.QueryExecuteException;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * An visitor to find the root projected entities in a result tree
 * 
 * @author liasu
 * 
 */
public class QueryRootProjectReader implements IEntityVisitor {

    private final boolean            isTargetReference;
    private final ParseQueryNode     rootQueryNode;
    private final Collection<Object> rootProjections;
    private final QueryContext       queryContext;
    private final Map<ParseQueryNode, ISearchField> nodeProjectField;

    private ParseQueryNode       prevQueryNode;
    private ParseQueryNode       currQueryNode;

    public QueryRootProjectReader(ParseQueryNode node, ISearchField targetField, QueryContext queryContext) {
        this.rootProjections = new HashSet<Object>();
        this.rootQueryNode = node;
        this.currQueryNode = node;
        this.prevQueryNode = null;
        this.queryContext = queryContext;
        this.isTargetReference = isRelationshipField(targetField);
        this.nodeProjectField = new HashMap<ParseQueryNode, ISearchField>();
    }

    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        if (currQueryNode == null) {
            return Collections.emptyList();
        }

        if (currQueryNode.isRootDisplay()) {
            ISearchField projectField = getProjectionField(currQueryNode);
            addSearchResult(currentEntity, projectField);
            // find the root entity, don't need to go any further
            return Collections.emptyList();
        } else {
            return currQueryNode.getProjectionFields();
        }
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        // do no thing
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(IEntity currentEntity, MetaRelationship metaRelationship) {
        ParseQueryNode oldPrevQueryNode = prevQueryNode;
        ParseQueryNode oldCurrQueryNode = currQueryNode;

        String relationName = metaRelationship.getName();
        List<ParseQueryNode> nextNodes = currQueryNode.getNextQueryNode(metaRelationship);

        for (ParseQueryNode nextNode : nextNodes) {
            prevQueryNode = currQueryNode;
            currQueryNode = nextNode;
    
            List<IEntity> relationEntityList = (List<IEntity>) currentEntity.getFieldValues(relationName);
            for (IEntity entity : relationEntityList) {
                entity.traverse(this);
            }
    
            prevQueryNode = oldPrevQueryNode;
            currQueryNode = oldCurrQueryNode;
        }
    }

    public Collection<Object> getRootValues() {
        return rootProjections;
    }

    public void reset() {
        currQueryNode = rootQueryNode;
        rootProjections.clear();
    }

    private ISearchField getProjectionField(ParseQueryNode projectNode) {
        if (nodeProjectField.containsKey(projectNode)) {
            return nodeProjectField.get(projectNode);
        }

        ISearchField field = null;
        // make sure the sub-query has only one user projection
        Collection<ISearchField> searchFields = projectNode.getUserProjection().getFields();
        boolean noUserSpecifyProject = projectNode.getUserProjection().hasStar() || projectNode.getUserProjection().isEmpty();
        if (noUserSpecifyProject) {
            if (isTargetReference) {
                // use the _oid when not specific user projection given for target reference. _oid is the mandatory internal project
                searchFields.add(new ProjectionField(projectNode.getMetaClass().getFieldByName(
                        InternalFieldEnum.ID.getName()), true, queryContext.getRegistration().searchStrategy));
            } else {
                searchFields = projectNode.getProjection().getFields();
            }
        }

        if (searchFields.size() > 1) {
            throw new QueryExecuteException(QueryErrCodeEnum.IILEGAL_PROJECTION, MessageFormat.format(
                    "sub query could only have one root projection, but actually get {0} fields!", searchFields.size()));
        }
        field = searchFields.iterator().next();
        nodeProjectField.put(projectNode, field);
        return field;
    }

    @SuppressWarnings("unchecked")
    private void addSearchResult(IEntity entity, ISearchField projectField) {
        String fieldName = projectField.getFieldName();
        if (isRelationshipField(projectField)) {
            // CMS-3430 : treat a projection of relationship field as
            // project on the reference oid
            List<IEntity> projectFieldValues = (List<IEntity>) entity.getFieldValues(fieldName);
            for (IEntity e : projectFieldValues) {
                rootProjections.add(e.getId());
            }
        } else {
            rootProjections.addAll(projectField.getSearchValue(entity));
        }
    }

    static boolean isRelationshipField(ISearchField field) {
        if (AbstractSearchField.class.isInstance(field)) {
            AbstractSearchField selection = (AbstractSearchField) field;
            return selection.getRootField().getDataType().equals(DataTypeEnum.RELATIONSHIP)
                    && (selection.getInnerField() == null || selection.getInnerField().equals(
                            InternalFieldEnum.ID.getDbName()));
        }
        return false;
    }

}
