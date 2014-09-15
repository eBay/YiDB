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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.metadata.ReverseMetaRelationship;
import com.ebay.cloud.cms.query.parser.ParseBaseNode;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

public abstract class AbstractJoinAction extends AbstractAction {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractJoinAction.class);

    private final ParseBaseNode  joinNode;

    /**
     * By default join is post-order, from next joined to parent. There might be cases that need pre-order join: from pre to post.
     */
    protected final boolean        bidirectional;
    protected boolean preDone;
    protected boolean postDone;
    protected Map<String, IEntity> parentEntityMap;

    public static class JoinExplanation implements IQueryExplanation {
        private ObjectNode explanationNode;

        public JoinExplanation(String sourceEnityType, long fromBeforeSize, long fromAfterSize,
                long targetEntitySizeBefore, List<List<String>> joinMap) {
            explanationNode = JsonNodeFactory.instance.objectNode();
            ObjectNode joinNode = explanationNode.objectNode();
            joinNode.put("sourceEntityType", sourceEnityType);
            joinNode.put("joinMap", joinMap.toString());
            joinNode.put("source size before size: ", fromBeforeSize);
            joinNode.put("source size after size: ", fromAfterSize);
            joinNode.put("target entity size involved: ", targetEntitySizeBefore);
            explanationNode.put("join", joinNode);
        }

        @Override
        public JsonNode getJsonExplanation() {
            return explanationNode;
        }
    }

    public AbstractJoinAction(QueryContext context, QueryActionTypeEnum type, ParseBaseNode joinNode, boolean bidirectional) {        
        super(context, joinNode, type);
        this.preDone = false;
        this.postDone = false;
        this.joinNode = joinNode;
        this.bidirectional = bidirectional;
    }

    @Override
    public void optimize() {
        // calculate sub tree cost
        AbstractAction childAction = childrenActions.get(0);
        this.subTreeCost = childAction.getSubTreeCost();
        // setup and or join map
        this.andOrJoinMap = childAction.andOrJoinMap;
    }

    @Override
    protected boolean doAction(ExecuteContext context, boolean postOrderVisit) {
        if (bidirectional) {
            return biDirectionJoin(context, postOrderVisit);
        } else {
            return uniDirectionJoin(context);
        }
    }

    private boolean uniDirectionJoin(ExecuteContext context) {
        AbstractAction childAction = childrenActions.get(0);
        if (childAction.isDescentsDone() && parentAction.isSelfDone()) {
            // FIXME 1. After virtual reference population, the join action doesn't need to
            // justify the virtual reference.
            join(context);
            return true;
        }
        return false;
    }

    /**
     * For bidirectional join action. Invariance :
     * <ul>
     * <li>pre order join must after post join
     * <li>post order join just need adjacent action self done. while pre join
     * need ancestor done and child self done.
     * </ul>
     * @param context 
     */
    private boolean biDirectionJoin(ExecuteContext context, boolean postOrderVisit) {
        AbstractAction childAction = childrenActions.get(0);
        if (postOrderVisit && !postDone && parentAction.isSelfDone() && childAction.isSelfDone()) {
            join(context);
            postDone = true;
        }
        if (!postOrderVisit && postDone && parentAction.isAncestorsDone() && childAction.isSelfDone()) {
            join(context);
            preDone = true;
        }
        return preDone && postDone;
    }

    private void join(ExecuteContext context) {
        logger.debug(String.format("%s : execute join action, parent action=%s, child action=%s", getClass()
                .getSimpleName(), parentAction.toString(), childrenActions.get(0).toString()));
        if (parentAction.getActionResults().isEmpty() || parentAction.getActionResults().get(0).searchResult.isEmpty()) {
            clearChildResult();
            return;
        }
        SearchResult parentResult = parentAction.getActionResults().get(0).searchResult;
        // build hash table for all children entity
        Map<ParseQueryNode, Map<String, IEntity>> idToEntityMap = new HashMap<ParseQueryNode, Map<String, IEntity>>();
        long targetEntitySizeBefore = buildIdToEntityMap(idToEntityMap);
        // join explanation information
        long sourceEntitySizeBefore = parentResult.getResultSet().size();
        Map<ParseQueryNode, HashSet<String>> matchIdsMap = new HashMap<ParseQueryNode, HashSet<String>>();
        // join parent entity with children entities
        Iterator<IEntity> iter = parentResult.getResultSet().iterator();
        while (iter.hasNext()) {
            IEntity parentEntity = iter.next();
            boolean joined = joinEntityByReference(parentEntity, idToEntityMap, matchIdsMap);
            if (!joined) {
                iter.remove();
            }
        }
        
        // remove invalid entities from children
        if (bidirectional) {
            for (ActionResult actionResult : childrenActions.get(0).getActionResults()) {
                ParseQueryNode node = actionResult.queryNode;
                HashSet<String> idsSet = matchIdsMap.get(node);
                if (idsSet != null) {
                    Iterator<IEntity> it = actionResult.searchResult.getResultSet().iterator();
                    while (it.hasNext()) {
                        IEntity entity = it.next();
                        if (!idsSet.contains(entity.getId())) {
                            it.remove();
                        }
                    }
                }
            }
        }
        
        // add explanation
        if (queryContext.needExplain()) {
            addJoinExplanation(context, parentResult, sourceEntitySizeBefore, targetEntitySizeBefore);
        }
    }

    private void clearChildResult() {
        // clear child result if any
        List<ActionResult> childResults = childrenActions.get(0).getActionResults();
        if (!childResults.isEmpty()) {
            SearchResult childResult = childResults.get(0).searchResult;
            childResult.clearResultSet();
        }
    }

    private void addJoinExplanation(ExecuteContext context, SearchResult parentResult, long sourceEntitySizeBefore,
            long targetEntitySizeBefore) {
        long sourceEntitySizeTarget = parentResult.getResultSet().size();
        String sourceType = parentResult.getMetaClass().getName();
        List<List<String>> joinMap = new LinkedList<List<String>>();
        for (List<ParseQueryNode> andList : andOrJoinMap) {
            List<String> andField = new ArrayList<String>(andList.size());
            for (ParseQueryNode joinNode : andList) {
                andField.add(joinNode.getMetaReference().getName());
            }
            joinMap.add(andField);
        }
        context.explanations.add(new JoinExplanation(sourceType, sourceEntitySizeBefore, sourceEntitySizeTarget,
                targetEntitySizeBefore, joinMap));
    }

    private Map<String, IEntity> buildParentEntityMap() {
        SearchResult parentResult = parentAction.getActionResults().get(0).searchResult;
        Map<String, IEntity> parentResultMap = new HashMap<String, IEntity>();
        for (IEntity entity : parentResult.getResultSet()) {
            parentResultMap.put(entity.getId(), entity);
        }
        return parentResultMap;
    }

    private long buildIdToEntityMap(Map<ParseQueryNode, Map<String, IEntity>> referenceBaseEntityMap) {
        // the size of the entity in the nested map
        long resultSize = 0;
        // for every reference out, keep an id-Entity map
        for (ActionResult actionResult : childrenActions.get(0).getActionResults()) {
            ParseQueryNode node = actionResult.queryNode;
            // make sure we have idToEntityMap
            Map<String, IEntity> idToEntityMap = referenceBaseEntityMap.get(node);
            if (idToEntityMap == null) {
                idToEntityMap = new HashMap<String, IEntity>();
                referenceBaseEntityMap.put(node, idToEntityMap);
            }
            // construct idToEntityMap and reverseReference if needed
            for (IEntity entity : actionResult.searchResult.getResultSet()) {
                idToEntityMap.put(entity.getId(), entity);
                buildReverseReference(entity, actionResult.joinField);
            }
            resultSize += idToEntityMap.size();
        }
        return resultSize;
    }

    @SuppressWarnings("unchecked")
    private void buildReverseReference(IEntity entity, MetaRelationship join) {
        if (parentEntityMap == null) {
            parentEntityMap = buildParentEntityMap();
        }
        if (join instanceof ReverseMetaRelationship) {
            MetaRelationship rel = ((ReverseMetaRelationship) join).getReversedReference();
            List<IEntity> refEntities = (List<IEntity>) entity.getFieldValues(rel.getName());
            for (IEntity ref : refEntities) {
                IEntity parent = parentEntityMap.get(ref.getId());
                if (parent != null) {
                    parent.addFieldValue(join.getName(), entity);
                }
            }
        }
    }

    // FIXME 2. join will have multiple iteration on a given field.
    // for example, for below andOrJoinMap
    // [
    //  [a, f],
    //  [a, b],
    //  [a, e]
    // ]
    // join() will *unnecessary* iterate on a for three times..
    private boolean joinEntityByReference(IEntity fromEntity, Map<ParseQueryNode, Map<String, IEntity>> referenceBasedEntityMap, 
            Map<ParseQueryNode, HashSet<String>> matchIdsMap) {
        boolean orCond = false;
        List<Map<String, List<IEntity>>> resultsList = new LinkedList<Map<String, List<IEntity>>>();
        
        for (List<ParseQueryNode> andList: andOrJoinMap) {
            boolean andCond = true;
            Map<String, List<IEntity>> resultMap = new HashMap<String, List<IEntity>>();
            for (ParseQueryNode joinNode : andList) {
                // update reference field of from entity
                String joinFieldName = joinNode.getMetaReference().getName();
                Map<String, IEntity> idToEntityMap = referenceBasedEntityMap.get(joinNode);
                if (idToEntityMap == null) {
                    idToEntityMap = Collections.<String, IEntity> emptyMap();
                }
                
                List<IEntity> results = new LinkedList<IEntity>();
                boolean match = joinField(fromEntity, idToEntityMap, joinFieldName, results);
                andCond = andCond && match;
                
                if (bidirectional) {
                    HashSet<String> idsSet = matchIdsMap.get(joinNode);
                    if (idsSet == null) {
                        idsSet = new HashSet<String>();
                        matchIdsMap.put(joinNode, idsSet);
                    }
                    
                    for (IEntity resultEntity : results) {
                        idsSet.add(resultEntity.getId());
                    }
                }
                
                if (resultMap.containsKey(joinFieldName)) {
                    resultMap.get(joinFieldName).addAll(results);
                } else {
                    resultMap.put(joinFieldName, results);
                }
            }
            resultsList.add(resultMap);
            if (andCond) {
                orCond = true;
            }
        }
        
        if (orCond) {
            joinResult(fromEntity, resultsList);
        }
        
        return orCond;
    }

    private void joinResult(IEntity fromEntity,
            List<Map<String, List<IEntity>>> resultsList) {
        Map<String, List<IEntity>> fieldValueMap = new HashMap<String, List<IEntity>>();
        for (Map<String, List<IEntity>> result : resultsList) {
            for (Map.Entry<String, List<IEntity>> entry : result.entrySet()) {
                String fieldName = entry.getKey();
                List<IEntity> mergedValues = null;
                if (fieldValueMap.containsKey(fieldName)) {
                    mergedValues = mergeFieldValues(fieldValueMap.get(fieldName), entry.getValue());
                } else {
                    mergedValues = mergeFieldValues(entry.getValue(), Collections.<IEntity> emptyList());
                }
                
                if (mergedValues.isEmpty()) {
                    if (!fromEntity.getFieldValues(fieldName).isEmpty()) {
                        fromEntity.removeField(fieldName);
                    }
                } else {
                    fieldValueMap.put(fieldName, mergedValues);
                    fromEntity.setFieldValues(fieldName, mergedValues);
                }
            }
        }
    }
    
    private List<IEntity> mergeFieldValues(List<IEntity> entityList1, List<IEntity> entityList2) {
        List<IEntity> mergedList = new LinkedList<IEntity>();
        Map<String, IEntity> idEntityMap = new HashMap<String, IEntity>();
        for (IEntity entity : entityList1) {
            String entityId = entity.getId();
            IEntity originEntity = idEntityMap.get(entityId);
            
            if (originEntity != null) {
                mergeEntity(originEntity, entity);
            } else {
                mergedList.add(entity);
                idEntityMap.put(entityId, entity);
            }
        }
        
        for (IEntity entity2 : entityList2) {
            String entityId = entity2.getId();
            if (idEntityMap.containsKey(entityId)) {
                IEntity entity1 = idEntityMap.get(entityId);
                if (entity1 != entity2) {
                    mergeEntity(entity1, entity2);
                }
            } else {
                mergedList.add(entity2);
                idEntityMap.put(entityId, entity2);
            }
        }
        
        return mergedList;
    }

    // merge entity2 to entity1
    @SuppressWarnings("unchecked")
    protected void mergeEntity(IEntity entity1, IEntity entity2) {
        MetaClass meta = entity1.getMetaClass();
        for (String fieldName : entity2.getFieldNames()) {
            MetaField field = meta.getFieldByName(fieldName);
            if (field instanceof MetaRelationship) {
                List<IEntity> mergedEntities = mergeFieldValues(
                        (List<IEntity>) entity1.getFieldValues(fieldName), 
                        (List<IEntity>) entity2.getFieldValues(fieldName));
                entity1.setFieldValues(fieldName, mergedEntities);
            } else {
                entity1.setFieldValues(fieldName, entity2.getFieldValues(fieldName));
            }
        }
    }

    @Override
    protected List<AbstractAction> getNextExecActions() {
        return childrenActions;
    }

    protected ParseQueryNode getParentQueryNode() {
        return (ParseQueryNode) joinNode.getPrevNode();
    }

    protected abstract boolean joinField(IEntity fromEntity, Map<String, IEntity> idToEntityMap, String joinFieldName, List<IEntity> results);
    
}