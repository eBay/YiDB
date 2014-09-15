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

package com.ebay.cloud.cms.service.entity.visitor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.EntityMapper;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * Json entity visistor to generate href instead of embeded entity/oid
 * 
 * @author Liangfei(Ralph) Su
 * 
 */
public class ActiveAPIEntityVisitor extends EntityMapper {

    private static final String OID = "id";
    private static final String URL = "url";
    private static final String ENTITY_TEMPLATE = "/repositories/%s/branches/%s/%s/%s?mode=uri";
    private static final String REF_NODE_NAME   = "ref";

	private final String repoName;
	private final String branchId;
	
	/**
	 * A meta field is referenced in a visitor in two way:
	 *     <p>1. Itself is reference in the projection
	 *     <p>2. Only its inner fields are referenced in projection but itself not projected, in this case we traverse the inner fields of metafield, but not metafeld itself
	 *     
	 * <br/>
	 * Implementation
	 * A filter set contains the meta fields that itself is projection
	 */
    private Set<String> projectedMetafields   = new HashSet<String>();

    public ActiveAPIEntityVisitor(String repoName, String branchId, MetaClass metaClass) {
        super(JsonEntity.class, metaClass);
        this.repoName = repoName;
        this.branchId = branchId;
    }
    
    @Override
    public JsonEntity getBuildEntity() {
        return (JsonEntity) super.getBuildEntity();
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        // override
        if (projectedMetafields.contains(metaField.getName())) {
            String fieldName = metaField.getName();
            if (currentEntity.hasField(fieldName)) {
                List<?> fieldValues = currentEntity.getFieldValues(fieldName);
                if (fieldValues.size() > 0) {
                    for (Object value : fieldValues) {
                        if (value != null) {
                            getBuildEntity().addFieldValue(fieldName, value);
                        } else {
                            addNullValue(getBuildEntity(), metaField);
                        }
                    }
                } else {
                    getBuildEntity().setFieldValues(fieldName, Collections.emptyList());
                }
            }
        }
        // field properties
        processFieldProperties(currentEntity, metaField);
    }

    private void addNullValue(JsonEntity buildEntity, MetaField metaField) {
        // preserver null CMS-4443
        JsonNode fieldNode = buildEntity.getNode().get(metaField.getName());
        if (metaField.getCardinality() == CardinalityEnum.Many) {
            ArrayNode fNode = null;
            if (fieldNode instanceof ArrayNode) {
                fNode = (ArrayNode) fieldNode;
            } else {
                fNode = JsonNodeFactory.instance.arrayNode();
                buildEntity.getNode().put(metaField.getName(), fNode);
            }
            fNode.add(JsonNodeFactory.instance.nullNode());
        } else {
            buildEntity.getNode().put(metaField.getName(), JsonNodeFactory.instance.nullNode());
        }
    }

    private void processFieldProperties(IEntity currentEntity, MetaField metaField) {
        FieldProperty[] properties = FieldProperty.values();
        JsonEntity jsonEntity = (JsonEntity) currentEntity;
        for (FieldProperty property : properties) {
            String propName = metaField.getName() + "." + property.getName();
            JsonNode propertyNode = jsonEntity.getNode().get(propName);
            if (propertyNode != null) {
                getBuildEntity().getNode().put(propName, propertyNode);
            }
        }
    }

    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        Collection<String> originalVisit = super.getVisitFields(currentEntity);
        Set<String> fields = new HashSet<String>(originalVisit);
        
        projectedMetafields = new HashSet<String>();
        for (String visit : originalVisit) {
            if (visit.indexOf(".") > 0) {
                fields.add(visit.substring(0, visit.indexOf(".")));
            } else {
                projectedMetafields.add(visit);
            }
        }
        return fields;
    }

    @Override
    public void processReference(IEntity currentEntity, MetaRelationship metaRelationship) {
        IEntity currentTargetEntity = getBuildEntity();
        String fieldName = metaRelationship.getName();

        @SuppressWarnings("unchecked")
        List<IEntity> refEntities = (List<IEntity>) currentEntity.getFieldValues(fieldName);

        processFieldProperties(currentEntity, metaRelationship);
        if (!projectedMetafields.contains(fieldName)) {
            return;
        }

        // for embed relationship, show all details as we can
        if (refEntities.size() > 0 && refEntities.get(0).getFieldNames().size() > 2) {
            Set<String> oldProjectFields = projectedMetafields;
            super.processReference(currentEntity, metaRelationship);
            projectedMetafields = oldProjectFields;
            return;
        }

        ObjectNode parentNode = (ObjectNode) currentTargetEntity.getNode();

        ObjectNode fieldNode = JsonNodeFactory.instance.objectNode();
        CardinalityEnum cardinality = metaRelationship.getCardinality();

        switch (cardinality) {
            case Many:
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                for (IEntity refEntity : refEntities) {
                    if (refEntity == null) {
                        continue;
                    }
                    String rawId = refEntity.getId();
                    String refRepo = repoName;
                    if (metaRelationship.getRelationType() == RelationTypeEnum.CrossRepository) {
                        refRepo = metaRelationship.getRefRepository();
                    }
                    ObjectNode hrefId = buildHRefId(refRepo, refEntity.getType(), rawId, branchId);
                    arrayNode.add(hrefId);
                }
                fieldNode.put(REF_NODE_NAME, arrayNode);
                break;
            case One:
                if (refEntities.size() > 0 && refEntities.get(0) != null) {
                    IEntity refEntity = refEntities.get(0);
                    String refRepo = repoName;
                    if (metaRelationship.getRelationType() == RelationTypeEnum.CrossRepository) {
                        refRepo = metaRelationship.getRefRepository();
                    }
                    ObjectNode hrefId = buildHRefId(refRepo, refEntity.getType(), refEntity.getId(), branchId);
                    fieldNode.put(REF_NODE_NAME, hrefId);
                }
                break;
            default:
                break;
        }
        parentNode.put(fieldName, fieldNode);
    }

    private ObjectNode buildHRefId(String repoName, String metaName, String rawId, String branchId) {
        String activeUrl = String.format(ENTITY_TEMPLATE, repoName, branchId, metaName,
                rawId);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put(OID, rawId);
        node.put(URL, activeUrl);
        return node;
    }
    
	protected boolean isSkippableRelationship(MetaRelationship metaRelationship) {
		return false;
	}

}
