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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.ModifyAction;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public class EntityModifyFieldOperation extends EntityFieldOperation {
    private boolean casMode;
    private Object oldValue;

    public EntityModifyFieldOperation(CMSServer server, String mode, HttpServletRequest request, String priority,
            String consistPolicy, String repoName, String metadata, String branch, String oid, String jsonBody,
            String fieldName, String errorMsg, UriInfo uriInfo, boolean casMode) {
        super(server, mode, request, priority, consistPolicy, repoName, metadata, branch, oid, jsonBody, fieldName,
                errorMsg, uriInfo);
        this.casMode = casMode;
    }

    @Override
    protected void performAction(EntityContext context, IEntity entity, IEntity queryEntity) {
        context.setModifyAction(ModifyAction.PUSHFIELD);
        if (casMode) {
            cmsServer.casModifyField(priority, queryEntity, entity, fieldName, oldValue, context);
        } else {
            cmsServer.modifyField(priority, queryEntity, entity, fieldName, context);
        }
    }

    @Override
    protected IEntity createEntity(MetaClass metaClass) {
        if (casMode) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            JsonNode fieldNode = null;
            MetaField field = metaClass.getFieldByName(fieldName);
            if (jsonString != null && !jsonString.isEmpty()) {
                try {
                    if (field == null) {
                        throw new NotFoundException("field not found!!");
                    }
                    if (field.getCardinality() == CardinalityEnum.Many) {
                        throw new BadParamException("cas only support one cardinality!");
                    }

                    fieldNode = mapper.readTree(jsonString);
                    JsonNode oldNode = fieldNode.get("oldValue");
                    JsonNode newValue = fieldNode.get("newValue");
                    fieldNode = newValue;

                    if (oldNode == null || newValue == null) {
                        throw new BadParamException("invalid json!");
                    }
                    if (!oldNode.isValueNode() || !newValue.isValueNode()) {
                        throw new BadParamException("cas only support value node!");
                    }

                    if (field.getDataType() == DataTypeEnum.BOOLEAN) {
                        if (!oldNode.isBoolean() || !newValue.isBoolean()) {
                            throw new BadParamException("the value type should be boolean!");
                        }
                        oldValue = oldNode.asBoolean();
                    } else if (field.getDataType() == DataTypeEnum.INTEGER) {
                        if (!oldNode.isIntegralNumber() || !newValue.isIntegralNumber()) {
                            throw new BadParamException("the value type should be integer!");
                        }
                        oldValue = oldNode.asInt();
                    } else if (field.getDataType() == DataTypeEnum.LONG) {
                        if ((!oldNode.isInt() && !oldNode.isLong()) || (!newValue.isInt() && !newValue.isLong())) {
                            throw new BadParamException("the value type should be long!");
                        }
                        oldValue = oldNode.asLong();
                    } else if (field.getDataType() == DataTypeEnum.STRING) {
                        if (!oldNode.isTextual() || !newValue.isTextual()) {
                            throw new BadParamException("the value type should be string!");
                        }
                        oldValue = oldNode.asText();
                    } else if (field.getDataType() == DataTypeEnum.ENUM) {
                        if (!oldNode.isTextual() || !newValue.isTextual()) {
                            throw new BadParamException("the value type should be enum!");
                        }
                        oldValue = oldNode.asText();
                    } else {
                        throw new BadParamException("cas does not support the value type:" + field.getDataType());
                    }

                    // if payload not an array for the given field, manually
                    // construct the array
                    if (field.getCardinality() == CardinalityEnum.Many && !fieldNode.isArray()) {
                        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                        arrayNode.insert(0, fieldNode);
                        fieldNode = arrayNode;
                    }
                } catch (IOException e) {
                    throw new CmsDalException(DalErrCodeEnum.PROCESS_JSON, "json parse error: " + e.getMessage(), e);
                }
            } else if (jsonString != null && !ignoreEmptyPayload()) {
                throw new BadParamException("Invalid json. Please given a valid json payload!");
            }
            if (fieldNode != null) {
                node.put(fieldName, fieldNode);
            }
            JsonEntity jsonEntity = new JsonEntity(metaClass, node);
            jsonEntity.setBranchId(branch);
            jsonEntity.setId(oid);
            return jsonEntity;
        } else {
            return super.createEntity(metaClass);
        }
    }
}
