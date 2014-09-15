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
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;

public class QueryProjectionVisitor {

    private static final ObjectMapper mapper = ObjectConverter.mapper;

    private final ArrayNode displayNode;
    private final QueryContext queryContext;

    private ObjectNode currentNode;

    QueryProjectionVisitor(QueryContext qryContext) {
        this.queryContext = qryContext;
        displayNode = JsonNodeFactory.instance.arrayNode();
    }

    public ArrayNode getDisplayNode() {
        return displayNode;
    }

    public void findProjection(ParseQueryNode queryNode) {
        MetaClass curMeta = queryNode.getMetaClass();
        findMetaProjection(curMeta, queryNode);

        for (MetaClass typeCastMeta : queryNode.getTypeCast()) {
            findMetaProjection(typeCastMeta, queryNode);
        }
    }

    private void findMetaProjection(MetaClass metadata, ParseQueryNode queryNode) {
        if (queryNode.isUserDisplay()) {
            createJsonNode(metadata);
            if (queryNode.isRootDisplay()) {
                displayNode.add(currentNode);
            }
        } else {
            createJsonNode(metadata);
        }

        rewriteProjection(queryNode);
        for (String project : queryNode.getProjectionFields()) {
            MetaField metaField = metadata.getFieldByName(project);

            traverse(metaField, queryNode);
        }
    }

    private void rewriteProjection(ParseQueryNode queryNode) {
        MetaClass metadata = queryNode.getMetaClass();
        SearchProjection projection = queryNode.getProjection();
        if (queryNode.getGroup() == null) {
            // add default fields
            ProjectionField idField = new ProjectionField(metadata.getFieldByName(InternalFieldEnum.ID.getName()),
                    true, queryContext.getRegistration().searchStrategy);
            projection.addField(idField);
            // add all fields for star selection
            if (projection.hasStar()) {
                Collection<MetaField> metaFields = metadata.getFields();
                for (MetaField metaField : metaFields) {
                    projection.addField(new ProjectionField(metaField, true, queryContext.getRegistration().searchStrategy));
                }
            }
        } else {
            SearchGroup group = queryNode.getGroup();
            // add default fields
            if (projection.isEmpty()) {
                projection.addField(ProjectionField.STAR);
            }
            // add all fields for star selection
            if (projection.hasStar()) {
                Collection<ISearchField> searchFields = group.getProjectFields();
                for (ISearchField field : searchFields) {
                    projection.addField(field);
                }
            }
        }
    }

    private void createJsonNode(MetaClass metadata) {
        currentNode = mapper.valueToTree(metadata);
        currentNode.remove("fields");
        currentNode.remove("options");
        currentNode.put("fields", JsonNodeFactory.instance.objectNode());
    }

    private void traverse(MetaField field, ParseQueryNode queryNode) {
        if (field == null) {
            return;
        }

        if (field instanceof MetaAttribute) {
            addAttribute(field);
        } else {
            addRelationship((MetaRelationship) field, queryNode);
        }
    }

    private void addRelationship(MetaRelationship field, ParseQueryNode queryNode) {
        ObjectNode relNode = null;
        if (currentNode != null) {
            relNode = mapper.valueToTree(field);
            ((ObjectNode) currentNode.get("fields")).put(field.getName(), relNode);
        }

        List<ParseQueryNode> nextNodes = queryNode.getNextQueryNode(field);
        if (nextNodes != null) {
            for (ParseQueryNode nextNode : nextNodes) {
                ObjectNode oldCurNode = currentNode;
                
                // first for the query node metaclass
                MetaClass meta = nextNode.getMetaClass();
                findMetaProjection(meta, nextNode);
                addToRelNode(relNode, meta);
    
                // type casts
                for (MetaClass typeCastMeta : nextNode.getTypeCast()) {
                    findMetaProjection(typeCastMeta, nextNode);
                    addToRelNode(relNode, typeCastMeta);
                }
    
                currentNode = oldCurNode;
            }
        }
    }

    private void addToRelNode(ObjectNode relNode, MetaClass meta) {
        if (relNode != null) {
            JsonNode node = relNode.get("refDataType");
            if (!(node instanceof ObjectNode)) {
                node = JsonNodeFactory.instance.objectNode();
                relNode.put("refDataType", node);
            }
            ((ObjectNode) node).put(meta.getName(), currentNode);
        }
    }

    private void addAttribute(MetaField field) {
        if (currentNode != null) {
            ((ObjectNode) currentNode.get("fields")).put(field.getName(), mapper.valueToTree(field));
        }
    }

}
