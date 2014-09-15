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


import java.util.BitSet;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.dalapi.entity.impl.BsonEntity;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author liasu, zhihzhang
 *
 */
public class EmbedFieldDeleteCommand extends AbstractFieldEmbedCommand {

    public EmbedFieldDeleteCommand(BsonEntity entity, String fieldName, AbstractEntityIDHelper helper) {
        super(entity, fieldName, helper);
    }

    @Override
    protected DBObject buildModifyBody(BitSet arrayBits, DBObject rootObject, MetaClass rootMetaClass) {
        BasicDBObject embedObject = (BasicDBObject) EmbedDBObjectFilter.filter(entity.getId(), rootObject, rootMetaClass, null, helper);
        embedObject.removeField(getField().getDbName());

        MetaClass meta = getEntity().getMetaClass();
        if (meta.hasExpressionFields()) {
            List<MetaAttribute> expFields = meta.getExpressionFields();
            for (MetaAttribute expField : expFields) {
                String fieldDbName = expField.getDbName();
                embedObject.put(fieldDbName, getEntity().getNode().get(fieldDbName));
            }
        }
        
        embedObject.put(InternalFieldEnum.MODIFIER.getDbName(), entity.getModifier());
        embedObject.put(InternalFieldEnum.LASTMODIFIED.getDbName(), entity.getLastModified());
        
        return buildSetBody(rootObject);
    }
    
    @Override
    protected String getOperation() {
        return "Delete field";
    }
}
