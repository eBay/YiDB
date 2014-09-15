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

package com.ebay.cloud.cms.entmgr.entity.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.IEntityVisitor;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * An visitor to compare two entity (named source and current).
 * 
 * @author liasu
 * 
 */
public class EntityComparator implements IEntityVisitor {

    private IEntity sourceEntity;
    private boolean    diff;
    private boolean    isRoot = true;

    public EntityComparator(IEntity sourceEntity) {
        this(sourceEntity, true);
    }

    protected EntityComparator(IEntity sourceEntity, boolean isRoot) {
        this.sourceEntity = sourceEntity;
        this.isRoot = isRoot;
    }
    
    @Override
    public Collection<String> getVisitFields(IEntity currentEntity) {
        return sourceEntity.getFieldNames();
    }

    @Override
    public void processAttribute(IEntity currentEntity, MetaField metaField) {
        if (diff && !isRoot) {
            return;
        }

        if (metaField.isInternal()) {
            // internal fields are maintained by server. only check id
            if (InternalFieldEnum.ID.getName().equals(metaField.getName()) && !diff) {
                diff = !org.apache.commons.lang.StringUtils.equals(currentEntity.getId(), sourceEntity.getId());
            }
            return;
        }

        String fieldName = metaField.getName();
        List<?> sourceValues = sourceEntity.getFieldValues(fieldName);
        if (!currentEntity.hasField(fieldName)) {
            diff = true;
            return;
        }

        List<?> destValues = currentEntity.getFieldValues(fieldName);
        
        boolean flag = diffAttributeValue(sourceValues, destValues);
        if(isRoot && !flag){
            sourceEntity.removeField(metaField.getName());
        }
        if(!diff){
            diff = flag;
        }
    }

    private boolean diffAttributeValue(List<?> sourceValues, List<?> destValues) {
        int len = sourceValues.size();
        if (len != destValues.size()) {
            // check length first
            return true;
        }
        for (int i = 0; i < len; i++) {
            Object source = sourceValues.get(i);
            Object dest = destValues.get(i);
            boolean equals = (source == null && dest == null) || (source != null && source.equals(dest));
            if (!equals) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void processReference(IEntity currentEntity, MetaRelationship metaRelationship) {
        if (diff && !isRoot) {
            return;
        }
        String fieldName = metaRelationship.getName();
        List<?> sourceValues = sourceEntity.getFieldValues(fieldName);
        List<?> destValues = currentEntity.getFieldValues(fieldName);
        boolean isEmebedReference = metaRelationship.getRelationType().equals(RelationTypeEnum.Embedded);

        if (sourceValues.size() != destValues.size()) {
            diff = true;
            return;
        }

        boolean flag = false;
        int len = sourceValues.size();
        for (int i = 0; i < len; i++) {
            IEntity sourceRef = (IEntity) sourceValues.get(i);
            IEntity destRef = (IEntity) destValues.get(i);
            
            if ((sourceRef.getId() == null && destRef.getId() != null) 
            		|| (sourceRef.getId() != null && destRef.getId() == null)) {
            	flag = true;
            	break;
            }

            // only embed to go through for detail check
            if (isEmebedReference) {
                EntityComparator childComparator = new EntityComparator(sourceRef, false);
                destRef.traverse(childComparator);
                if (childComparator.getDiffResult()) {
                    flag = true;
                    break;
                }
            } else if (!StringUtils.equals(sourceRef.getId(), destRef.getId())
                    || !StringUtils.equals(sourceRef.getType(), destRef.getType())) {
                flag = true;
                break;
            }
        }
        if (flag) {
            diff = true;
        }
        if (isRoot && !flag) {
            sourceEntity.removeField(metaRelationship.getName());
        }
    }

    public boolean getDiffResult() {
        return diff;
    }

    public void setRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

}
