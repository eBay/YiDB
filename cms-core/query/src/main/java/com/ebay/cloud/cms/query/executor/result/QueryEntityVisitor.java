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

package com.ebay.cloud.cms.query.executor.result;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonDataTypeHandlerFactory;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.query.metadata.AggregateMetaAttribute;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;

public class QueryEntityVisitor implements IEntityVisitor {
    // stack data
    private ParseQueryNode parseNode;
    private QueryResult queryResult;
    private JsonEntity jsonEntity;
    // a map of json entity based on traverse level to avoid duplicate json enitty
    private Map<String, JsonEntity> resultEntityMap;
    private boolean hasDuplicatePath;
    
    public QueryEntityVisitor(ParseQueryNode parseNode, QueryResult queryResult) {
        this.parseNode = parseNode;
        this.queryResult = queryResult;
        this.resultEntityMap = new HashMap<String, JsonEntity>();
        this.hasDuplicatePath = false;
    }
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        if (parseNode.isRootDisplay()) {
            jsonEntity = resultEntityMap.get(currentEntity.getId());
            if (jsonEntity == null) {
                jsonEntity = new JsonEntity(currentEntity.getMetaClass());
                queryResult.addEntity(jsonEntity);
                addToResultMap(currentEntity.getId(), jsonEntity);
            }
        }
        return parseNode.getProjectionFields();
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        if (jsonEntity == null) {
            return;
        }

        String fieldName = metaField.getName();
        if (metaField instanceof AggregateMetaAttribute) {
            ((AggregateMetaAttribute) metaField).setAggregateFieldValue(currentEntity, jsonEntity);
            return;
        }
        if (currentEntity.hasField(fieldName)) {
            List<?> values = currentEntity.getFieldValues(fieldName);
            if (values.isEmpty()) {
                if (parseNode != null && parseNode.getFieldProjectSet().contains(fieldName)) {
                    jsonEntity.setFieldValues(fieldName, Collections.EMPTY_LIST);
                }
            } else {
                for (Object val : values) {
                    jsonEntity.addFieldValue(fieldName, val);
                }
            }
        }
        processFieldProperty(currentEntity, metaField);
    }

    private void processFieldProperty(IEntity currentEntity, MetaField metaField) {
        if (parseNode == null) {
            return;
        }
        IEntity bsonEntity = (IEntity) currentEntity;
        Map<MetaField, Map<FieldProperty, MetaAttribute>> projectFields = parseNode.getFieldPropProjectMap();
        if (projectFields.containsKey(metaField)) {
            // set field property value to the json entity according to the faked name
            for (FieldProperty property : projectFields.get(metaField).keySet()) {
                MetaAttribute fakeAttribute = projectFields.get(metaField).get(property);
                Object value = bsonEntity.getFieldProperty(metaField.getName(), property.getName());
                if (value != null) {
                    // covert value using data type handler
                    IDataTypeHandler handler = JsonDataTypeHandlerFactory.getHandler(property.getType());
                    JsonNode valueNode = (JsonNode) handler.write(currentEntity, value, fakeAttribute);
                    jsonEntity.getNode().put(fakeAttribute.getName(), valueNode);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(IEntity currentEntity,
            MetaRelationship metaRelationship) {
        // backtrack
        // back stack state
        JsonEntity oldJsonEntity = jsonEntity;
        ParseQueryNode oldParseNode = parseNode;
        Map<String, JsonEntity> oldResultMap = resultEntityMap;
        boolean oldHasDup = hasDuplicatePath;
        resultEntityMap = new HashMap<String, JsonEntity>();
        
        List<ParseQueryNode> parseNodes = parseNode.getNextQueryNode(metaRelationship);
        boolean isProjected = parseNode.getFieldProjectSet().contains(metaRelationship.getName());
        if (parseNodes == null) {
            parseNode = null;
            processReferenceDetail(currentEntity, metaRelationship, isProjected);
        } else {
            hasDuplicatePath = hasDuplicatePath || parseNodes.size() > 1;
            // build existing json entity map to avoid duplication in A.(b && b) case
            if (oldJsonEntity != null && hasDuplicatePath) {
                List<JsonEntity> existingEntityList = (List<JsonEntity>)oldJsonEntity.getFieldValues(metaRelationship.getName());
                for (JsonEntity existingEntity : existingEntityList) {
                    addToResultMap(existingEntity.getId(), existingEntity);
                }
            }
            
            for (ParseQueryNode node : parseNodes) {
                parseNode = node;
                processReferenceDetail(currentEntity, metaRelationship, isProjected);
            }
        }

        // restore stack state
        jsonEntity = oldJsonEntity;
        parseNode = oldParseNode;
        resultEntityMap = oldResultMap;
        hasDuplicatePath = oldHasDup;
        processFieldProperty(currentEntity, metaRelationship);
    }
    
    @SuppressWarnings("unchecked")
    private void processReferenceDetail(IEntity currentEntity, MetaRelationship metaRelationship, boolean isProjected) {
        // backtrack
        JsonEntity oldJsonEntity = jsonEntity;
        ParseQueryNode oldParseNode = parseNode;
        String referenceName = metaRelationship.getName();
        
        if (currentEntity.hasField(referenceName)) {
            List<IEntity> nextEntities = (List<IEntity>)currentEntity.getFieldValues(referenceName);
            if (nextEntities.isEmpty()) {
                if (isProjected && jsonEntity != null) {
                    jsonEntity.setFieldValues(referenceName, Collections.EMPTY_LIST);
                }
            } else {
                boolean isEmbed = metaRelationship.getRelationType() == RelationTypeEnum.Embedded;
                for (IEntity nextEntity : nextEntities) {
                    if (parseNode != null && parseNode.hasProjection()) {
                        visitQueryNode(nextEntity, oldJsonEntity, referenceName);
                    } else if (oldJsonEntity != null) {
                        if (isEmbed) {
                            jsonEntity = visitEmbedEntity(nextEntity, oldJsonEntity, referenceName);
                        } else {
                            jsonEntity = visitReference(nextEntity, oldJsonEntity, referenceName);
                        }
                    }
                }
            }
        }

        jsonEntity = oldJsonEntity;
        parseNode = oldParseNode;
    }

    private void visitQueryNode(IEntity nextEntity, JsonEntity oldJsonEntity, String referenceName) {
        JsonEntity jsonEntity = null;
        if (oldJsonEntity != null) {
            jsonEntity = resultEntityMap.get(nextEntity.getId());    
            if (jsonEntity == null) {
                jsonEntity = new JsonEntity(nextEntity.getMetaClass());
                addToResultMap(nextEntity.getId(), jsonEntity);
                oldJsonEntity.addFieldValue(referenceName, jsonEntity);
            }
            this.jsonEntity = jsonEntity;
        }
        nextEntity.traverse(this);
    }

    private JsonEntity visitEmbedEntity(IEntity relationEntity, JsonEntity oldJsonEntity, String referenceName) {
        JsonEntity jsonEntity = resultEntityMap.get(relationEntity.getId());
        if (jsonEntity == null) {
            EntityMapper mapper = new EntityMapper(JsonEntity.class, relationEntity.getMetaClass());
            relationEntity.traverse(mapper);
            jsonEntity = (JsonEntity)mapper.getBuildEntity();
            oldJsonEntity.addFieldValue(referenceName, jsonEntity);
        }
        addToResultMap(relationEntity.getId(), jsonEntity);
        return jsonEntity;
    }

    private void addToResultMap(String id, JsonEntity jsonEntity) {
        // aggregation might not have id
        if (id != null && hasDuplicatePath) {
            resultEntityMap.put(id, jsonEntity);
        }
    }

    private JsonEntity visitReference(IEntity relationEntity, JsonEntity oldJsonEntity, String referenceName) {
        JsonEntity jsonEntity = resultEntityMap.get(relationEntity.getId());
        if (jsonEntity == null) {
            jsonEntity = new JsonEntity(relationEntity.getMetaClass());
            // replace with reference only entity
            jsonEntity.setId(relationEntity.getId());
            jsonEntity.addFieldValue(InternalFieldEnum.TYPE.getName(), relationEntity.getType());
            oldJsonEntity.addFieldValue(referenceName, jsonEntity);
        }
        addToResultMap(relationEntity.getId(), jsonEntity);
        return jsonEntity;
    }

}
