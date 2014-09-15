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

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;

/**
 * expression context for bson entity 
 * 
 * @author xjiang
 *
 */
public class EntityExpressionContext implements IExpressionContext {

    private final IEntity inputEntity;
    private final IEntity oldEntity;
    
    public EntityExpressionContext(IEntity inEnt) {
        this(inEnt, null);
    }
            
    public EntityExpressionContext(IEntity inEnt, IEntity oldEnt) {
        if (inEnt == null) {
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, "Can't create expression context will empty entity!");
        }
        this.inputEntity = inEnt;
        this.oldEntity = oldEnt;
    }
    
    @Override
    public boolean containsParameter(String name) {
        MetaClass metadata = inputEntity.getMetaClass();
        return metadata.getFieldByName(name) != null;        
    }

    @Override
    public Object getParamter(String name) {
        MetaClass metadata = inputEntity.getMetaClass();
        MetaField metafield = metadata.getFieldByName(name);
        List<?> values = inputEntity.getFieldValues(name);
        if (values == null || values.size() == 0) {
            if (oldEntity == null) {
                return null;
            }
            values = oldEntity.getFieldValues(name);
            if (values == null || values.size() == 0) {
                return null;
            }            
        }
        if (metafield.getCardinality() == CardinalityEnum.Many) {
            return values.toArray();
        } else {
            return values.get(0);
        }
    }

}
