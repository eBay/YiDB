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
package com.ebay.cloud.cms.metadata.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

//remove business logic from metaclass to this file
public class MetaClassManager {
	public MetaClassManager() {

	}
	
    public boolean isAssignableFrom(MetaClass meta, MetaClass c) {
        if (c == null) {
            return false;
        }
        String cName = c.getName();
        if (cName == null) {
            return false;
        }
        if (meta.equals(c)) {
            return true;
        }

        if (meta.getRepository() == null || !meta.getRepository().equals(c.getRepository())) {
            return false;
        }

        String cParent = c.getParent();
        if (!StringUtils.isNullOrEmpty(cParent)) {
            List<String> cAncestors = c.getAncestors();
            CheckConditions.checkNotNull(cAncestors, new MetaDataException(MetaErrCodeEnum.SHOULD_SETUP_ANCESTORS, "need to setup ancestors before use this method isAssignableFrom"));
            return cAncestors.contains(meta.getName());
        }
        else {
            return false;
        }
    }
    
    public void setupAncestors(MetaClass meta, IMetadataService metadataService, Map<String, MetaClass> metas) {
        if (meta.getParent() == null) {
            return;
        }
        
        ArrayList<String> ancestorList = new ArrayList<String>();
        
        String p = meta.getParent();
        
        while(!StringUtils.isNullOrEmpty(p)) {
            ancestorList.add(p);
            MetaClass m = metadataService.getMetaClass(p);
            if (m == null) {
                m = metas.get(p);
            }
            CheckConditions.checkCondition(m != null, new IllegalMetaClassException("can not get metaclass: " + p + " while setup ancestors for " + meta.getName()));
            
            p = m.getParent();
            CheckConditions.checkCondition(p == null || !p.equals(meta.getName()), new IllegalMetaClassException("circle found for metaclass inherent"));
        }
        
        meta.setAncestors(ancestorList);
//        this.ancestors = ancestorList;
    }
    
    public Map<String, IndexInfo> getIndexesMap(MetaClass meta) {
        Map<String, IndexInfo> indexMap = null;
        MetaClass mp = meta.getParentMetaClass();
        if (mp != null) {
            indexMap = mp.getIndexesMap();
        } else {
            indexMap = new HashMap<String, IndexInfo>();
        }
        
        for (IndexInfo info : meta.getOptions().getIndexes()) {
//        	for (IndexInfo info : options.getIndexes()) {
            indexMap.put(info.getIndexName(), info);
        }

        // add indexes from embed fields
        Collection<MetaField> fields = meta.getClassFields();
        for (MetaField field : fields) {
            if (field.getDataType() == DataTypeEnum.RELATIONSHIP 
                    && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded) {
            	MetaRelationship relationship = (MetaRelationship) field;
            	relationship.setMetadataService(meta.getMetadataService());
                MetaClass targetMeta = relationship.getRefMetaClass();
                if (targetMeta != null) {
	                Collection<IndexInfo> embedIndexes = targetMeta.getIndexes();
	                for (IndexInfo ii : embedIndexes) {
	                    IndexInfo embedIndex = new IndexInfo(ii, (MetaRelationship)field);
	                    indexMap.put(embedIndex.getIndexName(), embedIndex);
	                }
                }
            }
        }

        return indexMap;
    }
    
    public Collection<IndexInfo> getIndexesOnField(MetaClass meta, String fieldName) {
        List<IndexInfo> indexesOnField = meta.getOptions().getIndexesByFieldName(fieldName);
//        List<IndexInfo> indexesOnField = options.getIndexesByFieldName(fieldName);
        MetaClass mp = meta.getParentMetaClass();
        if (mp != null) {
            indexesOnField.addAll(mp.getIndexesOnField(fieldName));
        }
        
        // add a dummy embed field's oidIndex for QueryCostAnalysor
        MetaField field = meta.getFieldByName(fieldName);
        if (field != null && field.getDataType() == DataTypeEnum.RELATIONSHIP
                && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded) {
        	MetaRelationship relationship = (MetaRelationship) field;
        	relationship.setMetadataService(meta.getMetadataService());
            MetaClass targetMeta = relationship.getRefMetaClass();
            Collection<IndexInfo> embedIndexes = targetMeta.getIndexesOnField(InternalFieldEnum.ID.getName());
            if (!embedIndexes.isEmpty()) {
                IndexInfo embedIndex = new IndexInfo(field.getName(), true);
                embedIndex.addKeyField(field.getName());
                indexesOnField.add(embedIndex);
            }
        }
        
        return indexesOnField;
    }

}
