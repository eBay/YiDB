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

package com.ebay.cloud.cms.dalapi.persistence.impl.root;

import java.text.MessageFormat;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * NOTE for https://jira.mongodb.org/browse/SERVER-831 . Mongo doesn't support positional operation on nested array
 * 
 * @author liasu
 * 
 */
public class RootFieldModifyCommand extends AbstractFieldCommand implements IPersistenceCommand {

    public RootFieldModifyCommand(BsonEntity entity, String fieldName) {
        super(entity, fieldName);
    }

    @Override
    public void execute(PersistenceContext context) {
        DBObject queryObject = buildModifyQuery();

        DBObject modifyBody = buildModifyBody();

        WriteResult result = MongoExecutor.update(context, getEntity().getMetaClass(), queryObject, modifyBody);
        if (result.getN() == 0) {
            throw new CmsDalException(DalErrCodeEnum.VERSION_CONFLICT, MessageFormat.format(
                    "Field not modified!! Query:{0}, Body:{1}", queryObject, modifyBody));
        }
    }

    private DBObject buildModifyBody() {
        MetaField field = getField();
        DBObject modifyBody = new BasicDBObject();

        // system modify body including entity version, entity time stamp
        buildInternalUpdate(modifyBody);

        if (field.getCardinality() == CardinalityEnum.Many) {
            // appending to list, $addToSet
            buildArrayBody(modifyBody);
        } else if (field.getDataType().equals(DataTypeEnum.JSON)) {
            // incremental $set
            buildJsonBody(modifyBody);
        } else {
            buildSetFieldBody(modifyBody);
        }

        return modifyBody;
    }

    private void buildSetFieldBody(DBObject modifyBody) {
        MetaField field = getField();
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());

        if (fieldObject != null) {
            BasicDBObject set = (BasicDBObject) modifyBody.get("$set");
            set.put(field.getDbName(), fieldObject);
        }
    }

    private void buildArrayBody(DBObject modifyBody) {
        MetaField field = getField();
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());

        if (fieldObject != null) {
            Object givenValue = (fieldObject).get(V);
            if (givenValue != null) {
                BasicDBObject eachDbo = new BasicDBObject();
                eachDbo.put("$each", givenValue);

                BasicDBObject addToSetDbo = new BasicDBObject();
                addToSetDbo.put(field.getValueDbName(), eachDbo);

                modifyBody.put("$addToSet", addToSetDbo);

                // field length, only update when we do have updates
                BasicDBObject inc = (BasicDBObject) modifyBody.get("$set");
                inc.put(field.getDbName() + DOT + FieldProperty.LENGTH.getDbName(), getEntity().getFieldLength(fieldName));
                // field time stamp
                BasicDBObject set = (BasicDBObject) modifyBody.get("$set");
                set.put(field.getDbName() + DOT + FieldProperty.TIMESTAMP.getDbName(), getEntity().getFieldTimestamp(fieldName));
            }
        }

    }

    private void buildJsonBody(DBObject modifyBody) {
        BasicDBObject set = (BasicDBObject) modifyBody.get("$set");

        MetaField field = getField();
        BasicDBObject enityObject = (BasicDBObject) getEntity().getNode();
        BasicDBObject fieldObject = (BasicDBObject) enityObject.get(field.getDbName());
        if (fieldObject != null) {
            BasicDBObject givenValue = (BasicDBObject) (fieldObject).get(V);

            if (givenValue != null) {
                for (String key : givenValue.keySet()) {
                    set.put(field.getValueDbName() + DOT + key, givenValue.get(key));
                }

                // field properties
                // no length here
                BasicDBObject setDbo = (BasicDBObject) modifyBody.get("$set");
                setDbo.put(field.getDbName() + DOT + FieldProperty.TIMESTAMP.getDbName(),
                        getEntity().getFieldTimestamp(field.getName()));
            }
        }

    }

}
