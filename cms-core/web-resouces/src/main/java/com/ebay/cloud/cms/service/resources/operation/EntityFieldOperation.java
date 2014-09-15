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
package com.ebay.cloud.cms.service.resources.operation;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public abstract class EntityFieldOperation extends EntityOperation {
    protected final String fieldName;

    public EntityFieldOperation(CMSServer server, String mode, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String fieldName, String errorMsg, UriInfo uriInfo) {
        super(server, mode, request, priority, consistPolicy, repoName, metadata, branch, oid, jsonBody, errorMsg,
                uriInfo);
        this.fieldName = fieldName;
    }

    @Override
    protected IEntity createEntity(MetaClass metaClass) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        JsonNode fieldNode = null;
        MetaField field = metaClass.getFieldByName(fieldName);
        if (field == null) {
            throw new NotFoundException("field not found!!");
        }
        // FIXME: Even client doesn't send anything, the jsonString wouldn't
        // be null, but instead an empty string.
        // That' means we cannot determine whether client want to send a
        // empty string or not.
        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                fieldNode = mapper.readTree(jsonString);
                // if payload not an array for the given field, manually
                // construct the array
                if (fieldNode instanceof NullNode) {
                    fieldNode = null;
                } else if (field.getCardinality() == CardinalityEnum.Many && !fieldNode.isArray()) {
                    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                    arrayNode.insert(0, fieldNode);
                    fieldNode = arrayNode;
                }
            } catch (Exception e) {
                throw new BadParamException(e, "Invalid json. Please given a valid json payload!");
            }
        } else if (jsonString != null && !ignoreEmptyPayload()) {
            fieldNode = JsonNodeFactory.instance.textNode(jsonString);
            if (field.getCardinality() == CardinalityEnum.Many && !fieldNode.isArray()) {
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                arrayNode.insert(0, fieldNode);
                fieldNode = arrayNode;
            }
        }
        if (fieldNode != null) {
            node.put(fieldName, fieldNode);
        }
        JsonEntity jsonEntity = new JsonEntity(metaClass, node);
        jsonEntity.setBranchId(branch);
        jsonEntity.setId(oid);
        return jsonEntity;
    }

    protected boolean ignoreEmptyPayload() {
        return false;
    }

    @Override
    protected void collectResult(CMSQueryMode mode, CMSResponse response, IEntity entity) {
        response.addResult(oid);
    }
}
