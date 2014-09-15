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

package com.ebay.cloud.cms.dal.persistence.flatten.impl.embed;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author liasu, zhihzhang
 * 
 */
public class EmbedFieldModifyCommand extends AbstractFieldEmbedCommand {

    public EmbedFieldModifyCommand(NewBsonEntity entity, String fieldName, FlattenEntityIDHelper helper) {
        super(entity, fieldName, helper);
    }
    
    @Override
    protected DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass) {
        BasicDBObject embedObject = (BasicDBObject) EmbedDBObjectFilter.filter(entity.getId(), rootObject,
                rootMetaClass, null, helper);
        MetaField field = getField();
        if (field.getCardinality() == CardinalityEnum.Many) {
            buildArrayBody(embedObject, field);
        } else if (field.getDataType().equals(DataTypeEnum.JSON)) {
            buildJsonBody(embedObject, field);
        } else {
            // non-array & non-json : replace the whole field
            buildRepalceBody(embedObject, field);
        }

        embedObject.put(InternalFieldEnum.MODIFIER.getDbName(), entity.getModifier());
        embedObject.put(InternalFieldEnum.LASTMODIFIED.getDbName(), entity.getLastModified());
        return buildSetBody(rootObject);
    }

    void buildRepalceBody(BasicDBObject embedObject, MetaField field) {
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        Object fieldObject = (Object) enityObject.get(field.getFlattenValueDbName());
        embedObject.put(field.getFlattenValueDbName(), fieldObject);
        // field property
        for (FieldProperty fp : FieldProperty.values()) {
            String fpValueDbName = field.getFlattenPropertyValueDbName(fp);
            if (enityObject.containsField(fpValueDbName)) {
                embedObject.put(fpValueDbName, enityObject.get(fpValueDbName));
            }
        }
    }

    void buildJsonBody(BasicDBObject embedObject, MetaField field) {
        // incremental $set
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getFlattenValueDbName());
        if (fieldObject != null) {
            DBObject valueObj = (DBObject) embedObject.get(field.getFlattenValueDbName());
            if (valueObj == null) {
                valueObj = new BasicDBObject();
                embedObject.put(field.getFlattenValueDbName(), valueObj);
            }

            BasicDBObject givenValue = fieldObject;
            if (givenValue != null) {
                for (String key : givenValue.keySet()) {
                    valueObj.put(key, givenValue.get(key));
                }
                // update field property
                embedObject.put(field.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), getEntity().getFieldTimestamp(field.getName()));
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void buildArrayBody(BasicDBObject embedObject, MetaField field) {
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        List<?> fieldObject = (List<?>) enityObject.get(field.getFlattenValueDbName());

        if (fieldObject != null) {
            List givenValue = fieldObject;
            if (givenValue != null) {
                List targetFieldList = new ArrayList();
                BasicDBList list = (BasicDBList) embedObject.get(field.getFlattenValueDbName());
                if (list != null) {
                    targetFieldList.addAll(list);
                }
                targetFieldList.addAll(givenValue);

                BasicDBList valueList = new BasicDBList();
                valueList.addAll(targetFieldList);
                // field value
                embedObject.put(field.getFlattenValueDbName(), valueList);
                // field property
                embedObject.put(field.getFlattenPropertyValueDbName(FieldProperty.LENGTH), targetFieldList.size());
                embedObject.put(field.getFlattenPropertyValueDbName(FieldProperty.TIMESTAMP), new Date());
            }
        }
    }

    @Override
    protected String getOperation() {
        return "Modify field";
    }
}
