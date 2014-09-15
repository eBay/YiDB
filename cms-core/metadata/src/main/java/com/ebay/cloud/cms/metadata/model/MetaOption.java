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

package com.ebay.cloud.cms.metadata.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.ebay.cloud.cms.metadata.mongo.converter.MetaClassConverters;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * @author liasu
 * 
 */
public class MetaOption {

    // indexes information
    private Map<String, IndexInfo>       indexes           = new ConcurrentSkipListMap<String, IndexInfo>();
    private Map<String, List<IndexInfo>> fieldToIndexesMap = new ConcurrentSkipListMap<String, List<IndexInfo>>();
    private List<String> primaryKeys = Collections.synchronizedList(new ArrayList<String>());

    public MetaOption() {
        initIndex();
    }

    public MetaOption(MetaOption option) {
        CheckConditions.checkArgument(option != null, "Option can not be null.");
        indexes = new HashMap<String, IndexInfo>(option.indexes);
        fieldToIndexesMap = new HashMap<String, List<IndexInfo>>(option.fieldToIndexesMap);
    }

    private void initIndex() {
        for(IndexInfo index : IndexInfo.getInternalIndexes()){
            addIndex(index);
        }
    }

    @JsonIgnore
    public List<IndexInfo> getIndexesByFieldName(String fieldName) {
        if (fieldToIndexesMap.containsKey(fieldName)) {
            return new ArrayList<IndexInfo>(fieldToIndexesMap.get(fieldName));
        } else {
            return new ArrayList<IndexInfo>();
        }
    }

    @JsonProperty("primaryKeys")
    public List<String> getPrimaryKeys() {
        return Collections.unmodifiableList(primaryKeys);
    }

    @JsonIgnore
    public void addPrimaryKey(String key) {
        if (primaryKeys.contains(key)) {
            return;
        } else {
            primaryKeys.add(key);
        }
    }

    @JsonProperty("primaryKeys")
    void setPrimaryKeys(List<String> primaryKey) {
        this.primaryKeys.addAll(primaryKey);
    }

    @JsonIgnore
    public Collection<String> getIndexNames() {
        return new HashSet<String>(indexes.keySet());
    }

    public final void addIndex(IndexInfo index) {
        indexes.put(index.getIndexName(), index);

        // update field mapping
        for (String fieldName : index.getKeyList()) {
            List<IndexInfo> fieldIndexes;
            if (fieldToIndexesMap.containsKey(fieldName)) {
                fieldIndexes = fieldToIndexesMap.get(fieldName);
            } else {
                fieldIndexes = new ArrayList<IndexInfo>();
                fieldToIndexesMap.put(fieldName, fieldIndexes);
            }
            fieldIndexes.add(index);
        }
    }
    
    public final void dropIndex(IndexInfo index) {
    	for (String fieldName : index.getKeyList()) {
    		if(fieldToIndexesMap.containsKey(fieldName)){
    			fieldToIndexesMap.remove(fieldName);
    		}
    	}
    	indexes.remove(index.getIndexName());
    }

    public IndexInfo getIndexByName(String indexName) {
        return indexes.get(indexName);
    }

    @JsonProperty("indexes")
    @JsonSerialize(using = MetaClassConverters.MetaIndexSerializer.class)
    public Collection<IndexInfo> getIndexes() {
        return indexes.values();
    }

    @JsonProperty("indexes")
    @JsonDeserialize(using = MetaClassConverters.MetaIndexDeserializer.class)
    void setIndexes(Collection<IndexInfo> indexes) {
        this.indexes.clear();
        this.fieldToIndexesMap.clear();

        for (IndexInfo index : indexes) {
            addIndex(index);
        }

        initIndex();
    }

}
