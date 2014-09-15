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

package com.ebay.cloud.cms.entmgr.entity.impl.validator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * Check dangling strong reference
 * 
 * @author zhuang1
 *
 */


public class DanglingCheckAction implements IEntityAction {
    private ThreadLocal<IPersistenceService> persistenceService;
    private ThreadLocal<PersistenceContext> context;
    private ThreadLocal<Set<String>> newIdSet;
   
    public DanglingCheckAction() {
        this.persistenceService = new ThreadLocal<IPersistenceService>();
        this.context = new ThreadLocal<PersistenceContext>();
        this.newIdSet = new ThreadLocal<Set<String>>();
    }
    
    public void init(IPersistenceService persistenceService, Set<String> entityIds, PersistenceContext context) {
        this.persistenceService.set(persistenceService);
        this.context.set(context);
        this.newIdSet.set(entityIds);
    }
    
    @SuppressWarnings("unchecked")
    private void checkSingleDanglingReference(IEntity entity, MetaRelationship metaRef) {
        Map<MetaClass, List<String>> refOidsMap = new HashMap<MetaClass, List<String>>();
        List<IEntity> fieldValues = (List<IEntity>) entity.getFieldValues(metaRef.getName());
        // get _oid of reference field, category by concrete type
        for (IEntity value : fieldValues) {
            String id = value.getId();
            if (!newIdSet.get().contains(id)) {
                List<String> refOids = refOidsMap.get(value.getMetaClass());
                if (refOids == null) {
                    refOids = new LinkedList<String>();
                    refOidsMap.put(value.getMetaClass(), refOids);
                }
                refOids.add(id);
            }
        }

        if (refOidsMap.isEmpty()) {
            return;
        }

        // check reference oid in db
        for (Map.Entry<MetaClass, List<String>> entry : refOidsMap.entrySet()) {
            long dbCount = 0;
            MetaClass meta = entry.getKey();
            List<String> refOids = entry.getValue();
            dbCount = persistenceService.get().count(meta, refOids, entity.getBranchId(), context.get());
            if (dbCount < refOids.size()) {
                StringBuilder errInfo = new StringBuilder("Dangling reference! ");
                errInfo.append("Entity: ").append(entity.getId());
                errInfo.append(", MetaClass: ").append(entity.getType());
                errInfo.append(", Reference: ").append(metaRef.getName());
                errInfo.append(", Ref_MetaClass: ").append(meta.getName());
                errInfo.append(", DB_Count = ").append(dbCount);
                errInfo.append(", Ref_Count = ").append(refOids.size());
                errInfo.append(", Ref_Oids= ").append(refOids);
                throw new CmsEntMgrException(CmsEntMgrException.EntMgrErrCodeEnum.VIOLATE_REFERENCE_INTEGRITY, errInfo.toString());
            }
        }
    }

    @Override
    public void processAttribute(IEntity currentEntity, IEntity existingEntity, MetaField metaField) {
        MetaClass metaCls = currentEntity.getMetaClass();
        for (MetaRelationship metaRef : metaCls.getToReference()) {
            if (metaRef.getRelationType() == RelationTypeEnum.Reference
                    && metaRef.getConsistencyType() == MetaRelationship.ConsistencyTypeEnum.Strong) {
                checkSingleDanglingReference(currentEntity, metaRef);
            }            
        }
    }

    @Override
    public void processReference(IEntity currentEntity, IEntity existingEntity, MetaRelationship metaRelationship) {

    }
    
}
