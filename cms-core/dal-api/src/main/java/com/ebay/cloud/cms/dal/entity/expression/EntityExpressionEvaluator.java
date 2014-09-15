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

package com.ebay.cloud.cms.dal.entity.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.exception.ExpressionEvaluateException;
import com.ebay.cloud.cms.expression.exception.ExpressionTimeoutException;
import com.ebay.cloud.cms.expression.factory.ScriptEngineProvider;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.EqualsUtil;

/**
 * entity visitor to evaluate expression
 * 
 * @author xjiang
 *
 */
public class EntityExpressionEvaluator implements IEntityVisitor {

    private final IExpressionEngine exprEngine;

    private final IEntity dbEntity;
    private final Date date;
    private final CMSDBConfig dbConfig;
    
    public EntityExpressionEvaluator(IEntity dbEntity, CMSDBConfig dbConfig) {
        this.dbEntity = dbEntity;
        this.date = new Date();
        this.dbConfig = dbConfig;
        this.exprEngine = ScriptEngineProvider.getEngine((Long) this.dbConfig.get(CMSDBConfig.JS_EXPRESSION_TIMEOUT_IN_SECONDS));
    }
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        MetaClass metadata = currentEntity.getMetaClass();
        List<MetaField> jsAttrList = new ArrayList<MetaField>();
        
        jsAttrList.addAll(metadata.getExpressionFields());
        jsAttrList.addAll(metadata.getEmbedWithExprs());
        
        jsAttrList.addAll(metadata.getValidationFields());
        jsAttrList.addAll(metadata.getEmbedWithValidations());
        
        if (jsAttrList.isEmpty()) {
            return Collections.emptySet();
        } else {
            List<String> fieldNames = new ArrayList<String>(jsAttrList.size());
            for (MetaField expAttr : jsAttrList) {
                if (!fieldNames.contains(expAttr.getName())) {
                    fieldNames.add(expAttr.getName());
                }
            }
            return fieldNames;
        }        
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        // cms2202: last modified time lost for expression field.
        IEntity bsonEntity = (IEntity) currentEntity;
        String fieldName = metaField.getName();
        IExpression expression = ((MetaAttribute)metaField).getCompiledExpression();
        if (expression != null) {
            IExpressionContext exprContext = new EntityExpressionContext(bsonEntity, dbEntity);
            try {
	            Object exprValue = exprEngine.evaluate(expression, exprContext);
	            Object oldValue = bsonEntity.getFieldValues(fieldName);
	            if (!EqualsUtil.isEquals(exprValue, oldValue)) {
	                setAttributeValue(metaField, bsonEntity, fieldName, exprValue);
	            }
            } catch (ExpressionTimeoutException e) {
            	throw new CmsDalException(DalErrCodeEnum.JS_EXPRESSION_TIMEOUT, String.format(
                            "Time out of evaludating JS expression %s!", expression.getStringExpression()), e);
            } catch (ExpressionEvaluateException e) {
            	throw new CmsDalException(DalErrCodeEnum.JS_EXPRESSION_EXECUTION_ERROR, String.format(
                        "Error when evaludating JS expression %s!", expression.getStringExpression()), e);
            }
        }
        
        IExpression validation = ((MetaAttribute)metaField).getCompiledValidation();
        if (validation != null) {
            List<?> values = bsonEntity.getFieldValues(fieldName);
            if (values != null && !values.isEmpty()) {
                IExpressionContext exprContext = new EntityExpressionContext(bsonEntity, dbEntity);
                try {
	                Object exprValue = exprEngine.evaluate(validation, exprContext);
	                if (!(exprValue instanceof Boolean)
	                        || !((Boolean)exprValue).booleanValue()) {
	                    throw new CmsDalException(DalErrCodeEnum.VALIDATION_FAILED, String.format(
	                            "The validation of field %s failed!", fieldName));
	                }
                } catch (ExpressionTimeoutException e) {
                	throw new CmsDalException(DalErrCodeEnum.JS_EXPRESSION_TIMEOUT, String.format(
	                            "Time out of evaludating JS expression %s!", validation.getStringExpression()), e);
                } catch (ExpressionEvaluateException e) {
                	throw new CmsDalException(DalErrCodeEnum.JS_EXPRESSION_EXECUTION_ERROR, String.format(
                            "Error when evaludating JS expression %s!", validation.getStringExpression()), e);
                }
            }
        }
    }

    private void setAttributeValue(MetaField metaField, IEntity bsonEntity, String fieldName, Object exprValue) {
        if (exprValue instanceof List) {
            bsonEntity.setFieldValues(fieldName, (List<?>) exprValue);
        } else {
            List<Object> fieldValue = new ArrayList<Object>();
            fieldValue.add(exprValue);
            bsonEntity.setFieldValues(fieldName, fieldValue);
        }
        // update field properties
        if (metaField.getCardinality() == CardinalityEnum.Many) {
            bsonEntity.setFieldProperty(fieldName, FieldProperty.LENGTH.getName(), bsonEntity.getFieldValues(fieldName)
                    .size());
        }
        bsonEntity.setFieldProperty(fieldName, FieldProperty.TIMESTAMP.getName(), date);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(IEntity currentEntity, MetaRelationship metaRelationship) {
        String refName = metaRelationship.getName();
        List<IEntity> relationEntityList = (List<IEntity>) currentEntity.getFieldValues(refName);
        HashMap<String, IEntity> entityMap = generateRefEntityMap(refName);
        for (IEntity relationEntity : relationEntityList) {
            if (relationEntity != null) {
            	IEntity refEntity = entityMap.get(relationEntity.getId());
            	EntityExpressionEvaluator evaluator = new EntityExpressionEvaluator((IEntity)refEntity, dbConfig);
            	relationEntity.traverse(evaluator);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private HashMap<String, IEntity> generateRefEntityMap(String refName) {
    	HashMap<String, IEntity> entityMap = new HashMap<String, IEntity>();
    	if (dbEntity != null) {
    		List<IEntity> refEntities = (List<IEntity>) dbEntity.getFieldValues(refName);
    		for (IEntity refEntity : refEntities) {
    			entityMap.put(refEntity.getId(), (IEntity)refEntity);
    		}
    	}
    	return entityMap;
    }

}
