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

package com.ebay.cloud.cms.service.entity.visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public class ActiveAPIMetadataVisitor {

    private static final String META_ID = "id";

    private static final String URL = "url";

    private static final String META_URL_TEMPLATE = "/repositories/%s/metadata/%s?mode=uri";

    private static ObjectMapper        m = ObjectConverter.mapper;

    private ObjectNode          currentNode;

    private Object              result;

    /**
     * Flag that indicates whether to show in uri style
     */
    private boolean             isUri             = false;
    /**
     * Flag that indicates whether to show inherited meta fields
     */
    private boolean             showAll           = false;

    public ActiveAPIMetadataVisitor(CMSQueryMode mode) {
        this.isUri = CMSQueryMode.URI.equals(mode);
        this.showAll = CMSQueryMode.SHOWALL.equals(mode);
    }

    public void traverse(List<MetaClass> targets) {
        List<JsonNode> arrayNode = new ArrayList<JsonNode>();
        for (MetaClass target : targets) {
            traverse(target);
            arrayNode.add(currentNode);
        }

        result = arrayNode;
    }

    public Object getBuiltResult() {
        if (result == null) {
            return currentNode;
        } else {
            return result;
        }
    }

    public void traverse(MetaClass target) {
        List<MetaAttribute> attrFields = new ArrayList<MetaAttribute>();
        List<MetaRelationship> refFields = new ArrayList<MetaRelationship>();
        Collection<String> visitFields = getVisitFields(target);

        for (String fieldName : visitFields) {
            MetaField metaField = target.getFieldByName(fieldName);
            if (metaField == null) {
                throw new CmsDalException(DalErrCodeEnum.MISS_META_FIELD, "Does not have find field {" + fieldName
                        + "} in metadata " + target.getName());
            }
            if(!metaField.isVirtual()) {
            	if (metaField.getDataType() == DataTypeEnum.RELATIONSHIP) {
            		refFields.add((MetaRelationship) metaField);
            	} else if (!metaField.isInternal()) {
            		attrFields.add((MetaAttribute) metaField);
            	}
            }
        }

        // meta class internal field
        processMetaclassFields(target);

        // make sure we process attributes first
        for (MetaAttribute attrField : attrFields) {
            processMetaAttribute(target, attrField);
        }

        // process references
        for (MetaRelationship refField : refFields) {
            processMetaReference(target, refField);
        }

        //process meta options
        processMetaOption(target, target.getOptions());
    }

    protected void processMetaOption(MetaClass target, MetaOption options) {
        //nothing to do 
    }

    private void processMetaclassFields(MetaClass target) {
        ObjectNode objectNode;
        objectNode = objectToJsonNode(target);
        currentNode = objectNode;
    }

    private ObjectNode objectToJsonNode(Object target) {
        String value;
        ObjectNode objectNode;
        try {
            value = m.writeValueAsString(target);
            objectNode = (ObjectNode) m.readTree(value);
        } catch (Throwable t) {
            throw new CMSServerException(QueryErrCodeEnum.DIRTY_DATA_FORMAT.getErrorCode(), "Error while convert metadata to JSON.", t);
        }
        return objectNode;
    }

    protected Collection<String> getVisitFields(MetaClass target) {
        if (showAll) {
            return target.getFieldNames();
        }

        Collection<MetaField> fields = target.getClassFields();
        Collection<String> visitFields = new HashSet<String>();
        for (MetaField mf : fields) {
            visitFields.add(mf.getName());
        }
        return visitFields;
    }

    protected void processMetaAttribute(MetaClass meta, MetaAttribute attribute) {
        ObjectNode objectNode = (ObjectNode) currentNode;
        // remove fields node
        if (!objectNode.get("fields").isObject()) {
            objectNode.remove("fields");
            ObjectNode fieldsNode = objectNode.objectNode();
            objectNode.put("fields", fieldsNode);
        }

        ObjectNode fieldsNode = (ObjectNode) objectNode.get("fields");
        ObjectNode attrNode = objectToJsonNode(attribute);
        fieldsNode.put(attribute.getName(), attrNode);
    }

    protected void processMetaReference(MetaClass meta, MetaRelationship relationship) {
        CheckConditions.checkNotNull(currentNode);

        String repoName = meta.getRepository();
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(repoName));
        CheckConditions.checkArgument(!StringUtils.isNullOrEmpty(relationship.getName()));

        ObjectNode objectNode = (ObjectNode) currentNode;
        ObjectNode fieldsNode = (ObjectNode) objectNode.get("fields");

        ObjectNode relNode = objectToJsonNode(relationship);
        if (isUri) {
            ObjectNode referenceNode = JsonNodeFactory.instance.objectNode();
            String refRepo = repoName;
            if (relationship.getRelationType() == RelationTypeEnum.CrossRepository) {
                refRepo = relationship.getRefRepository();
            }
            referenceNode.put("ref", buildHrefIdNode(refRepo, relationship.getRefDataType()));
            relNode.put("refDataType", referenceNode);
        }

        fieldsNode.put(relationship.getName(), relNode);
    }

    private ObjectNode buildHrefIdNode(String repoName, String refMetaName) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put(META_ID, refMetaName);
        node.put(URL, String.format(META_URL_TEMPLATE, repoName, refMetaName));
        return node;
    }

}
