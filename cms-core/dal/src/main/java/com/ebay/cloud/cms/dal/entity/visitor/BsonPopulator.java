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

package com.ebay.cloud.cms.dal.entity.visitor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;


/**
 * populate necessary values on BsonEntity got from database
 * 
 * @author xjiang
 *
 */
public class BsonPopulator implements IEntityVisitor {
    
    private int rootVersion = -1;
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        List<String> fieldNames = null;        
        MetaClass metaClass = currentEntity.getMetaClass();
        for(MetaField metaField : metaClass.getFields()){
            String fieldName = metaField.getName();
            if(metaField.getDataType() == DataTypeEnum.RELATIONSHIP){
                MetaRelationship metaRelationship = (MetaRelationship)metaField;
                if(metaRelationship.getRelationType() == RelationTypeEnum.Embedded){
                    if (fieldNames == null) {
                        fieldNames = new LinkedList<String>();
                    }
                    fieldNames.add(fieldName);
                }
            }
        }
        if (fieldNames != null) {
            fieldNames.add(InternalFieldEnum.VERSION.getName());
        }
        return fieldNames;
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {        
        if (rootVersion <= IEntity.NO_VERSION) {
            rootVersion = currentEntity.getVersion();
        } else {
            currentEntity.setVersion(rootVersion);
        }        
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processReference(IEntity currentEntity,MetaRelationship metaRelationship) {        
        List<IEntity> refEntities = (List<IEntity>)currentEntity.getFieldValues(metaRelationship.getName());
        for(IEntity refEntity: refEntities){
            if (refEntity == null) {
                continue;
            }
            refEntity.traverse(this);
        }
    }

}
