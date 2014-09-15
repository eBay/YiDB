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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.query.IEmbedQuery;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.executor.QueryExecutor.ExecuteContext;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * 
 * @author zhuang1
 * 
 */
public class RootJoinAction extends AbstractJoinAction {

    public RootJoinAction(QueryContext context) {
        super(context, QueryActionTypeEnum.ROOTJOIN, null, false);
    }

    @Override
    public String toString() {
        return "RootJoinAction";
    }
    
    @Override
    protected boolean doAction(ExecuteContext context, boolean postOrderVisit) {
        AbstractAction childAction = childrenActions.get(0);
        if (childAction.isDescentsDone()) {
            join();
            return true;
        }
        return false;
    }
    
    private void join() {
        logger.debug(String.format("%s : execute root join action, parent action=null, child action=%s", getClass()
                .getSimpleName(), childrenActions.get(0).toString()));
        Map<ParseQueryNode, Map<String, IEntity>> nodeResultMap = buildQueryNodeResultMap();
        // meta --> results <id, entity>
        Map<String, Map<String, IEntity>> resultMap = new HashMap<String, Map<String, IEntity>>();
        
        for (List<ParseQueryNode> andNodeList: andOrJoinMap) {
            String meta = null;
            Map<String, IEntity> andListResults = null;
            Map<String, IEntity> tempResults1 = new HashMap<String, IEntity>();
            
            if (andNodeList.size() > 0) {
                Map<String, IEntity> tempResults2 = new HashMap<String, IEntity>();
                ParseQueryNode andNode = andNodeList.get(0);
                meta = andNode.getMetaClass().getName();
                Map<String, IEntity> idToEntityMap = nodeResultMap.get(andNode);
                for (Map.Entry<String, IEntity> entry : idToEntityMap.entrySet()) {
                    tempResults1.put(entry.getKey(), entry.getValue());
                }
            
                for (int i = 1; i < andNodeList.size(); ++i) {
                    andNode = andNodeList.get(i);
                    idToEntityMap = nodeResultMap.get(andNode);
                    if (idToEntityMap == null) {
                        idToEntityMap = Collections.<String, IEntity> emptyMap();
                    }
                
                    for (Map.Entry<String, IEntity> entry : idToEntityMap.entrySet()) {
                        String entityId = entry.getKey();
                        IEntity entity = tempResults1.get(entityId);
                        if (entity != null) {
                            mergeEntity(entity, entry.getValue());
                            tempResults2.put(entityId, entity);
                        }
                    }
                
                    Map<String, IEntity> temp = tempResults1;
                    tempResults1 = tempResults2;
                    tempResults2 = temp;
                    tempResults2.clear();
                }
            }
            
            andListResults = tempResults1;
            
            Map<String, IEntity> entityResult = resultMap.get(meta);
            if (entityResult != null) {
                for (Map.Entry<String, IEntity> entry : andListResults.entrySet()) {
                    String entityId = entry.getKey();
                    IEntity entity = entityResult.get(entityId);
                    if (entity != null) {
                        mergeEntity(entity, entry.getValue());
                    } else {
                        entityResult.put(entityId, entry.getValue());
                    }
                }
            } else {
                resultMap.put(meta, andListResults);
            }
        }
        
        for (Map.Entry<String, Map<String, IEntity>> entry : resultMap.entrySet()) {
            Map<String, IEntity> entities = entry.getValue();
            if (!entities.isEmpty()) {
                // find the meta class
                IEntity entity = entities.values().iterator().next();
                SearchResult searchResult = new SearchResult(entity.getMetaClass());
                searchResult.getResultSet().addAll(entities.values());

                ActionResult actionResult = new ActionResult(searchResult, null);
                this.getActionResults().add(actionResult);
            }
        }
    }

    private Map<ParseQueryNode, Map<String, IEntity>> buildQueryNodeResultMap() {
        Map<ParseQueryNode, Map<String, IEntity>> nodeResultMap = new HashMap<ParseQueryNode, Map<String, IEntity>>();
        for (ActionResult actionResult : childrenActions.get(0).getActionResults()) {
            ParseQueryNode node = actionResult.queryNode;
            // make sure we have idToEntityMap
            Map<String, IEntity> idToEntityMap = nodeResultMap.get(node);
            if (idToEntityMap == null) {
                idToEntityMap = new HashMap<String, IEntity>();
                nodeResultMap.put(node, idToEntityMap);
            }
            // construct idToEntityMap and reverseReference is needed
            for (IEntity entity : actionResult.searchResult.getResultSet()) {
                idToEntityMap.put(entity.getId(), entity);
            }
        }
        return nodeResultMap;
    }

    @Override
    protected SearchCriteria getTailJoinCriteria(MetaRelationship referenceField) {
        return null;
    }

    @Override
    protected SearchCriteria getHeadJoinCriteria() {
        return null;
    }
    
    @Override
    protected boolean joinField(IEntity fromEntity,
            Map<String, IEntity> idToEntityMap, String joinFieldName,
            List<IEntity> results) {
        return false;
    }

    @Override
    protected IEmbedQuery getHeadJoinQuery(IEmbedQuery prevEmbedQuery, MetaClass meta,
            LinkedList<MetaRelationship> embedFieldList, boolean leftJoin) {
        return null;
    }
    
    @Override
    public boolean isChildrenCriteriaReady() {
        return false;
    }

}
