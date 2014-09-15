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
import java.util.List;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * 
 * @author liasu
 *
 */
public class RootFieldDeleteCommand extends AbstractFieldCommand implements IPersistenceCommand {

    public RootFieldDeleteCommand(BsonEntity entity, String fieldName) {
        super(entity, fieldName);
    }

    @Override
    public void execute(PersistenceContext context) {
        DBObject qryObject = buildModifyQuery();
        DBObject updateObject = buildDeleteBody();

        MetaClass meta = getEntity().getMetaClass();
        WriteResult result = MongoExecutor.update(context, meta, qryObject, updateObject);
        try {
            result.getLastError().throwOnError();
        } catch (Exception e) {
            //we should not throw exception when delete a not existing field
            throw new CmsDalException(DalErrCodeEnum.ENTITY_NOT_FOUND, MessageFormat.format(
                    "Can't found the field {0} on entity ''{1} : {2}'' for deletion!", getField().getName(),
                    getEntity().getMetaClass().getName(), getEntity().getId()));
        }
    }

    private DBObject buildDeleteBody() {
        BasicDBObject modifyBody = new BasicDBObject();

        // system internal modification body including version adding; time stamp update
        buildInternalUpdate(modifyBody);

        // field modification body
        buildFieldBody(modifyBody);

        return modifyBody;
    }

    private void buildFieldBody(BasicDBObject modifyBody) {
        MetaField field = getField();
        BasicDBObject unsetBody = new BasicDBObject();
        unsetBody.put(field.getDbName(), "");
        modifyBody.put("$unset", unsetBody);
        
        MetaClass meta = getEntity().getMetaClass();
        if (meta.hasExpressionFields()) {
            BasicDBObject setBody = null;
            if (!modifyBody.containsField("$set")) {
                setBody = new BasicDBObject();
                modifyBody.put("$set", setBody);
            } else {
                setBody = (BasicDBObject)modifyBody.get("$set");
            }
            
            List<MetaAttribute> expFields = meta.getExpressionFields();
            for (MetaAttribute expField : expFields) {
                String fieldDbName = expField.getDbName();
                setBody.put(fieldDbName, getEntity().getNode().get(fieldDbName));
            }
        }
    }

}
