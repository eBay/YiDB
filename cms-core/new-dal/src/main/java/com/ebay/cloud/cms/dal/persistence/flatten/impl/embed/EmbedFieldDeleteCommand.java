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


import java.util.BitSet;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author liasu, zhihzhang
 *
 */
public class EmbedFieldDeleteCommand extends AbstractFieldEmbedCommand {

    public EmbedFieldDeleteCommand(NewBsonEntity entity, String fieldName, FlattenEntityIDHelper helper) {
        super(entity, fieldName, helper);
    }

    @Override
    protected DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass) {
        MetaField field = getField();
        BasicDBObject embedObject = (BasicDBObject) EmbedDBObjectFilter.filter(entity.getId(), rootObject,
                rootMetaClass, null, helper);
        embedObject.removeField(field.getFlattenValueDbName());
        // field properties
        for (FieldProperty fp : FieldProperty.values()) {
            String fpValueDbName = field.getFlattenPropertyValueDbName(fp);
            embedObject.removeField(fpValueDbName);
        }

        // need to update expression fields : set the payload from the given object to found rootObject
        MetaClass meta = getEntity().getMetaClass();
        if (meta.hasExpressionFields()) {
            List<MetaAttribute> expFields = meta.getExpressionFields();
            for (MetaAttribute expField : expFields) {
                String fieldValueDbName = expField.getFlattenValueDbName();
                embedObject.put(fieldValueDbName, getEntity().getNode().get(fieldValueDbName));
                // updateFieldProperty
                updateFieldProperty(embedObject, expField);
            }
        }
        
        embedObject.put(InternalFieldEnum.MODIFIER.getDbName(), entity.getModifier());
        embedObject.put(InternalFieldEnum.LASTMODIFIED.getDbName(), entity.getLastModified());
        
        return buildSetBody(rootObject);
    }

    void updateFieldProperty(BasicDBObject embedObject, MetaAttribute expField) {
        for (FieldProperty fp : FieldProperty.values()) {
            String fpValueDbName = expField.getFlattenPropertyValueDbName(fp);
            if (getEntity().getNode().containsField(fpValueDbName)) {
                embedObject.put(fpValueDbName, getEntity().getNode().get(fpValueDbName));
            }
        }
    }
    
    @Override
    protected String getOperation() {
        return "Delete field";
    }
}
