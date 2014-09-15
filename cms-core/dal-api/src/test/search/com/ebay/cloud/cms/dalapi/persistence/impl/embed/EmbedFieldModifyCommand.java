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

package com.ebay.cloud.cms.dalapi.persistence.impl.embed;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
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

    public EmbedFieldModifyCommand(BsonEntity entity, String fieldName, AbstractEntityIDHelper helper) {
        super(entity, fieldName, helper);
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass) {
        BasicDBObject embedObject = (BasicDBObject) EmbedDBObjectFilter.filter(entity.getId(), rootObject, rootMetaClass, null, helper);

        MetaField field = getField();
        if (field.getCardinality() == CardinalityEnum.Many) {
            BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
            BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());

            if (fieldObject != null) {
                List givenValue = (List) fieldObject.get(V);
                if (givenValue != null) {
                    // merge with exsiting fields FIXME:: keep the same with RootFieldModifyCommand of using delta
                    List targetFilterList = new ArrayList();
                    BasicDBObject existingFieldObject = (BasicDBObject) embedObject.get(field.getDbName());
                    if (existingFieldObject != null) {
                        BasicDBList list = (BasicDBList) existingFieldObject.get(V);
                        if (list != null) {
                            targetFilterList.addAll(list);
                        }
                    }
                    targetFilterList.addAll(givenValue);

                    BasicDBList valueList = new BasicDBList();
                    valueList.addAll(targetFilterList);

                    DBObject obj = (DBObject) embedObject.get(field.getDbName());
                    if (obj == null) {
                        obj = new BasicDBObject();
                        embedObject.put(field.getDbName(), obj);
                    }

                    obj.put(V, valueList);
                    obj.put(FieldProperty.LENGTH.getDbName(), targetFilterList.size());
                    obj.put(FieldProperty.TIMESTAMP.getDbName(), new Date());
                }
            }
        } else if (field.getDataType().equals(DataTypeEnum.JSON)) {
            // incremental $set
            // buildJsonBody(parentPath, modifyBody);
            BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
            BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());
            if (fieldObject != null) {
                DBObject obj = (DBObject) embedObject.get(field.getDbName());
                if (obj == null) {
                    obj = new BasicDBObject();
                    embedObject.put(field.getDbName(), obj);
                }
                DBObject valueObj = (DBObject) obj.get(V);
                if (valueObj == null) {
                    valueObj = new BasicDBObject();
                    obj.put(V, valueObj);
                }

                BasicDBObject givenValue = (BasicDBObject) (fieldObject).get(V);
                if (givenValue != null) {
                    for (String key : givenValue.keySet()) {
                        valueObj.put(key, givenValue.get(key));
                    }
                    valueObj.put(FieldProperty.TIMESTAMP.getDbName(), getEntity().getFieldTimestamp(field.getName()));
                }
            }

        } else {
            // non-array: replace the whole field
            BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
            BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());
            embedObject.put(field.getDbName(), fieldObject);
            // buildSetFieldBody(parentPath, modifyBody);
        }

        embedObject.put(InternalFieldEnum.MODIFIER.getDbName(), entity.getModifier());
        embedObject.put(InternalFieldEnum.LASTMODIFIED.getDbName(), entity.getLastModified());

        return buildSetBody(rootObject);
    }

    @Override
    protected String getOperation() {
        return "Modify field";
    }
}
