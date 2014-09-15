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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.mongo.converter.MetaClassConverters;
import com.ebay.cloud.cms.metadata.mongo.converter.MongoObjectIdConverter;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.DateUtils;
import com.ebay.cloud.cms.utils.EqualsUtil;
import com.ebay.cloud.cms.utils.StringUtils;

@JsonPropertyOrder({"_id", "name", "pluralName", "description", "repository", "parent", "parentVersion", "ancestors",
    "allowFullTableScan", "lastModified", "fields", "options"})
public class MetaClass {
    
    public static final String NAME = "name";
    public static final String PLURAL_NAME = "pluralName";
    public static final String ANCESTERS_NAME = "ancestors";
    public static final String REPOSITORY_NAME = "repository";
    public static final String VERSION = "version";
    
    public static final List<String> RESERVED_METACLASS_NAME = 
            Arrays.asList("query", "branch", "branches", "history", "histories", "entities");
    
    private IMetadataService metadataService;
    
    private String name;
    private String pluralName;
    
    private String description;
    
    private String parent;
    private int    parentVersion;
    
    private Boolean allowFullTableScan;
    
    private Boolean embed;
    private Boolean inner;
    private boolean sharded;
    
    private List<String> ancestors;
    
    private String id;
    private String repository;
    private Date   lastModified;
    private int    version;
    
    protected Map<String, MetaField> fieldNameIndex  = new ConcurrentHashMap<String, MetaField>();
    protected Map<String, MetaField> dbNameIndex     = new ConcurrentHashMap<String, MetaField>();
    protected Map<String, MetaField> flattenValueDbNameIndex = new ConcurrentHashMap<String, MetaField>();
    protected MetaClassGraph         graph;
    
    private List<MetaAttribute>    exprFields      = new ArrayList<MetaAttribute>();
    private List<MetaAttribute>    validFields     = new ArrayList<MetaAttribute>();
    
    private MetaOption             options         = new MetaOption();
    
    private MetaClassManager manager = new MetaClassManager();
    
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    @JsonDeserialize(using = MongoObjectIdConverter.ObjectIdDeserializer.class)
    public void setId(String id) {
        this.id = id;
    }
    
    public MetaClass() {
        lastModified = new Date();
        parentVersion = -1;
        init();
    }

    public MetaClass(MetaClass other) {
        CheckConditions.checkNotNull(other, "Metaclass cannot be null");
        id = other.id;
        description = other.description;
        ancestors = new ArrayList<String>();
        if (other.ancestors != null) {
            ancestors.addAll(other.ancestors);
        }
        graph = other.graph;
        exprFields = new ArrayList<MetaAttribute>(other.exprFields);
        dbNameIndex = new HashMap<String, MetaField>(other.dbNameIndex);
        fieldNameIndex = new HashMap<String, MetaField>(other.fieldNameIndex);
        flattenValueDbNameIndex = new ConcurrentHashMap<String, MetaField>();
        metadataService = other.metadataService;
        name = other.name;
        pluralName = other.pluralName;
        repository = other.repository;
        parent = other.parent;
        options = new MetaOption(other.options);
        allowFullTableScan = other.allowFullTableScan;
        lastModified = other.lastModified;
        embed = other.embed;
        inner = other.inner;
        version = other.version;
        parentVersion = other.parentVersion;
    }

    @JsonProperty("allowFullTableScan")
    public Boolean getAllowFullTableScan() {
        return allowFullTableScan;
    }
    
    @JsonIgnore
    public Boolean isAllowFullTableScan() {
        if (allowFullTableScan != null) {
            return allowFullTableScan;
        }
        return false;
    }

    public void setAllowFullTableScan(Boolean allowFullTableScan) {
        this.allowFullTableScan = allowFullTableScan;
    }

    public String getpluralName() {
        return pluralName;
    }

    public void setpluralName(String pluralName) {
        this.pluralName = pluralName;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
    
    public List<String> getAncestors() {
        return ancestors;
    }

    public void setAncestors(List<String> ancestors) {
        this.ancestors = ancestors;
    }
    
    public boolean isAssignableFrom(MetaClass c) {
//        if (c == null) {
//            return false;
//        }
//        String cName = c.getName();
//        if (cName == null) {
//            return false;
//        }
//        if (equals(c)) {
//            return true;
//        }
//
//        if (this.repository == null || !this.repository.equals(c.getRepository())) {
//            return false;
//        }
//
//        String cParent = c.getParent();
//        if (!StringUtils.isNullOrEmpty(cParent)) {
//            List<String> cAncestors = c.getAncestors();
//            CheckConditions.checkNotNull(cAncestors, new MetaDataException(MetaErrCodeEnum.SHOULD_SETUP_ANCESTORS, "need to setup ancestors before use this method isAssignableFrom"));
//            return cAncestors.contains(name);
//        }
//        else {
//            return false;
//        }
    	return manager.isAssignableFrom(this, c);
    }

    @JsonIgnore
    public void setMetadataService(IMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @JsonIgnore
    public IMetadataService getMetadataService() {
        return this.metadataService;
    }

    @JsonIgnore
    public MetaClass getParentMetaClass() {
        if (StringUtils.isNullOrEmpty(parent)) {
            return null;
        }
        
        if (metadataService == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METASERVICE_PROVIDED, "no MetadataService provided to retrieve parent MetaClass");
        }
        
        return metadataService.getMetaClass(parent, parentVersion, new MetadataContext());
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Date getLastModified() {
        return DateUtils.cloneDate(lastModified);
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = DateUtils.cloneDate(lastModified);
    }
    
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
    
    public int getParentVersion() {
        return parentVersion;
    }

    public void setParentVersion(int parentVersion) {
        this.parentVersion = parentVersion;
    }
    
    /**
     * Set the fields of this MetaClass, not include parent fields
     * 
     * Used by bson/json converter
     * 
     */
    @JsonProperty("fields")
    @JsonSerialize(using = MetaClassConverters.MetaFieldSerializer.class)
    public Collection<MetaField> getClassFields() {
        return fieldNameIndex.values();
    }

    /**
     * Set the fields of this MetaClass, not include parent fields
     * 
     * Used by bson/json converter
     * 
     * @param fields
     */
    @JsonProperty("fields")
    @JsonDeserialize(using = MetaClassConverters.MetaFieldDeserializer.class)
    void setClassFields(Collection<MetaField> fields) {
        fieldNameIndex.clear();
        dbNameIndex.clear();
        flattenValueDbNameIndex.clear();
        
        for (MetaField f : fields) {
            if (f.isInternal()) {
                throw new IllegalMetaClassException("MetaField name can not use reserved name");
            }
            
            addField(f);
        }
        
        init();
    }
    
    public void addField(MetaField metaField) {
         
        if (InternalFieldFactory.getInternalFieldEnum(metaField.getName()) != null) {
            throw new IllegalMetaClassException("MetaField name can not use reserved name");
        }
        if (metaField.getName() == null) {
            throw new IllegalMetaClassException("MetaField name can not use reserved name");
        }
        fieldNameIndex.put(metaField.getName(), metaField);
        
        String dbName = metaField.getDbName();
        if (dbName != null) {
            dbNameIndex.put(metaField.getDbName(), metaField);
        }
        if (metaField.getFlattenValueDbName() != null) {
            flattenValueDbNameIndex.put(metaField.getFlattenValueDbName(), metaField);
        }

        if (metaField instanceof MetaRelationship) {
            ((MetaRelationship)metaField).setSourceDataType(name);
            ((MetaRelationship)metaField).setMetadataService(metadataService);
        }
        else {
            MetaAttribute metaAttribute = (MetaAttribute)metaField;
            if (metaAttribute.getExpression() != null) {
                exprFields.add(metaAttribute);
            }
            if (metaAttribute.getValidation() != null) {
                validFields.add(metaAttribute);
            }
        }
        
    }

    public MetaField getFieldByName(String fieldName) {
        MetaField f = fieldNameIndex.get(fieldName);
        if (f != null) {
            return f;
        }
        MetaClass mp = getParentMetaClass();
        if (mp != null) {
            return mp.getFieldByName(fieldName);
        }
        else {
            return null;
        }
    }
    
    public MetaField getFieldByDbName(String dbName) {
        MetaField f = dbNameIndex.get(dbName);
        if (f != null) {
            return f;
        }
        MetaClass mp = getParentMetaClass();
        if (mp != null) {
            return mp.getFieldByDbName(dbName);
        }
        else {
            return null;
        }
    }

    public MetaField getFieldByFlattenValueDbName(String valueDbName) {
        MetaField f = flattenValueDbNameIndex.get(valueDbName);
        if (f != null) {
            return f;
        }
        MetaClass mp = getParentMetaClass();
        if (mp != null) {
            return mp.getFieldByFlattenValueDbName(valueDbName);
        } else {
            return null;
        }
    }

    @JsonIgnore
    public Collection<MetaField> getFields() {
        return getFieldsMap().values();
    }
    
    @JsonIgnore
    private Map<String, MetaField> getFieldsMap() {
        Map<String, MetaField> fieldMap = null;
        MetaClass mp = getParentMetaClass();
        if (mp != null) {
            fieldMap = mp.getFieldsMap();
        } else {
            fieldMap = new HashMap<String, MetaField>();
        }
        
        for (MetaField field : getClassFields()) {
            fieldMap.put(field.getName(), field);
        }
        
        return fieldMap;
    }
    
    @JsonIgnore
    public List<MetaAttribute> getExpressionFields() {
        return exprFields;
    }
    
    @JsonIgnore
    public List<MetaAttribute> getValidationFields() {
        return validFields;
    }
    
    @JsonIgnore
    public Collection<String> getFieldNames() {
        MetaClass mp = getParentMetaClass();
        if (mp != null) {
            Set<String> fields = new HashSet<String>();
            fields.addAll(mp.getFieldNames());
            fields.addAll(fieldNameIndex.keySet());
            return fields;
        } else {
            return fieldNameIndex.keySet();
        }
    }
    
    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }
    
    public void setupAncestors(IMetadataService metadataService, Map<String, MetaClass> metas) {
//        if (this.parent == null) {
//            return;
//        }
//        
//        ArrayList<String> ancestorList = new ArrayList<String>();
//        
//        String p = this.parent;
//        
//        while(!StringUtils.isNullOrEmpty(p)) {
//            ancestorList.add(p);
//            MetaClass m = metadataService.getMetaClass(p);
//            if (m == null) {
//                m = metas.get(p);
//            }
//            CheckConditions.checkCondition(m != null, new IllegalMetaClassException("can not get metaclass: " + p + " while setup ancestors for " + name));
//            
//            p = m.getParent();
//            CheckConditions.checkCondition(p == null || !p.equals(name), new IllegalMetaClassException("circle found for metaclass inherent"));
//        }
//        
//        this.ancestors = ancestorList;
    	manager.setupAncestors(this, metadataService, metas);
    }

    @JsonIgnore
    public List<MetaRelationship> getFromReference() {
        if (graph == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METACLASS_GRAPH_PROVIDED, "no MetaclassGraph provided to retrieve from reference");
        }
        return graph.getFromReference(this);
    }
    
    @JsonIgnore
    public List<MetaRelationship> getToReference() {
        if (graph == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METACLASS_GRAPH_PROVIDED, "no MetaclassGraph provided to retrieve to reference");
        }
        return graph.getToReference(this);
    }
    
    @JsonIgnore
    public List<MetaClass> getDescendants() {
        if (graph == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METACLASS_GRAPH_PROVIDED, "no MetaclassGraph provided to retrieve descendants");
        }
        return graph.getDescendants(this);
    }
    
    @Override
    public String toString() {
        return "MetaClass [name=" + name + ", id=" + id + ", repository="
                + repository + ", lastModified="
                + lastModified + ", fields=" + fieldNameIndex
                + "]";
    }

    //jianxu1: discussed with JiangXu, internal fields are also meta data
    private void init(){
        Map<String, MetaField> internals = InternalFieldFactory.getInternalMetaFields();
        for (MetaField f : internals.values()) {
            fieldNameIndex.put(f.getName(), f);
            dbNameIndex.put(f.getDbName(), f);
            flattenValueDbNameIndex.put(f.getFlattenValueDbName(), f);
        }
    }

    protected Map<String, MetaField> getFieldNameIndex() {
        return fieldNameIndex;
    }

    protected Map<String, MetaField> getDbNameIndex() {
        return dbNameIndex;
    }

    protected Map<String, MetaField> getFlattenValueDbNameIndex() {
        return flattenValueDbNameIndex;
    }

    public void setMetaclassGraph(MetaClassGraph graph) {
        this.graph = graph;
    }
    
    
    // ===== Index related APIs =====

    @JsonIgnore
    public void addIndex(IndexInfo index) {
        options.addIndex(index);
    }

    @JsonIgnore
    public IndexInfo getIndexByName(String indexName) {
        Collection<IndexInfo> allIndexes = getIndexes();
        for (IndexInfo ii : allIndexes) {
            if (ii.getIndexName().equals(indexName)) {
                return ii;
            }
        }
        return null;
    }

    /**
     * Indexes could be defined for current metaclass, or inherited from parent, or auto-coparated from embed relationship
     * 
     * @return
     */
    @JsonIgnore
    public Collection<IndexInfo> getIndexes() {
        return getIndexesMap().values();
    }

    @JsonIgnore
    public Map<String, IndexInfo> getIndexesMap() {
//        Map<String, IndexInfo> indexMap = null;
//        MetaClass mp = getParentMetaClass();
//        if (mp != null) {
//            indexMap = mp.getIndexesMap();
//        } else {
//            indexMap = new HashMap<String, IndexInfo>();
//        }
//        
//        for (IndexInfo info : options.getIndexes()) {
//            indexMap.put(info.getIndexName(), info);
//        }
//
//        // add indexes from embed fields
//        Collection<MetaField> fields = getClassFields();
//        for (MetaField field : fields) {
//            if (field.getDataType() == DataTypeEnum.RELATIONSHIP 
//                    && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded) {
//            	MetaRelationship relationship = (MetaRelationship) field;
//            	relationship.setMetadataService(getMetadataService());
//                MetaClass targetMeta = relationship.getRefMetaClass();
//                if (targetMeta != null) {
//	                Collection<IndexInfo> embedIndexes = targetMeta.getIndexes();
//	                for (IndexInfo ii : embedIndexes) {
//	                    IndexInfo embedIndex = new IndexInfo(ii, (MetaRelationship)field);
//	                    indexMap.put(embedIndex.getIndexName(), embedIndex);
//	                }
//                }
//            }
//        }
//
//        return indexMap;
    	return manager.getIndexesMap(this);
    }

    @JsonIgnore
    public Collection<IndexInfo> getIndexesOnField(String fieldName) {
//        List<IndexInfo> indexesOnField = options.getIndexesByFieldName(fieldName);
//        MetaClass mp = getParentMetaClass();
//        if (mp != null) {
//            indexesOnField.addAll(mp.getIndexesOnField(fieldName));
//        }
//        
//        // add a dummy embed field's oidIndex for QueryCostAnalysor
//        MetaField field = getFieldByName(fieldName);
//        if (field != null && field.getDataType() == DataTypeEnum.RELATIONSHIP
//                && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded) {
//        	MetaRelationship relationship = (MetaRelationship) field;
//        	relationship.setMetadataService(getMetadataService());
//            MetaClass targetMeta = relationship.getRefMetaClass();
//            Collection<IndexInfo> embedIndexes = targetMeta.getIndexesOnField(InternalFieldEnum.ID.getName());
//            if (!embedIndexes.isEmpty()) {
//                IndexInfo embedIndex = new IndexInfo(field.getName(), true);
//                embedIndex.addKeyField(field.getName());
//                indexesOnField.add(embedIndex);
//            }
//        }
//        
//        return indexesOnField;
    	return manager.getIndexesOnField(this, fieldName);
    }
    
    @JsonIgnore
    public Collection<IndexInfo> getClassIndexes() {
        return options.getIndexes();
    }

    @JsonIgnore
    public Collection<String> getIndexNames() {
        Collection<IndexInfo> allIndexes = getIndexes();
        Collection<String> indexNames = new HashSet<String>();
        for (IndexInfo ii : allIndexes) {
            indexNames.add(ii.getIndexName());
        }
        return indexNames;
    }
    
    @JsonProperty("options")
    public MetaOption getOptions() {
        return this.options;
    }

    public void traverse(IMetadataVisistor visitor) {
        for (String fieldName : visitor.getVisitFields(this)) {
            MetaField field = getFieldByName(fieldName);
            visitor.processField(this, field);
        }
        visitor.processOption(this, this.getOptions(), graph);
    }

    @Override
    public int hashCode() {
        return EqualsUtil.hashCode(this.name, this.repository);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof MetaClass)) {
            return false;
        }
        MetaClass other = (MetaClass) o;
        if (this.name == null || other.name == null || this.repository == null || other.repository == null) {
            return false;
        }
        return this.name.equals(other.name) && this.repository.equals(other.repository);
    }

    @JsonProperty("embed")
    public Boolean getEmbed() {
        return embed;
    }
    
    @JsonIgnore
    public Boolean isEmbed() {
        if (embed != null) {
            return embed;
        }
        return false;
    }

    public void setEmbed(Boolean embed) {
        this.embed = embed;
    }
    
    @JsonProperty("inner")
    public Boolean getInner() {
        return inner;
    }
    
    @JsonIgnore
    public boolean isInner() {
        if (inner != null) {
            return inner;
        }
        return false;
    }

    public void setInner(Boolean inner) {
        this.inner = inner;
    }

    @JsonIgnore
    public boolean isMetaSharded() {
        MetaClass mp = getParentMetaClass();
        if (mp != null && mp.isMetaSharded()) {
            return true;
        }
        return isSharded();
    }

    boolean isSharded() {
        return sharded;
    }

    public void setSharded(boolean sharded) {
        this.sharded = sharded;
    }

    public void addReferenceIndexes() {
        Collection<MetaField> fields = getClassFields();
        for (MetaField field : fields) {
            if (field.getDataType() == DataTypeEnum.RELATIONSHIP 
                && ((MetaRelationship) field).getRelationType() != RelationTypeEnum.Embedded) {
                String indexName = IndexInfo.INDEX_PREFIX + field.getName() + IndexInfo.INDEX_POSTFIX;
                IndexInfo relationshipIndex = new IndexInfo(indexName, true);
                relationshipIndex.addKeyField(field.getName());
                options.addIndex(relationshipIndex);
            }
        }
    }

    public boolean hasExpressionFields() {
        return (!exprFields.isEmpty() || !getEmbedWithExprs().isEmpty());
    }

    @JsonIgnore
    public List<MetaField> getEmbedWithExprs() {
        List<MetaField> expressionFields = new ArrayList<MetaField>();
        List<MetaRelationship> relations = getToReference();
        for (MetaRelationship rel : relations) {
            if (rel.getRelationType() == RelationTypeEnum.Embedded
                    && rel.getRefMetaClass().hasExpressionFields()) {
                expressionFields.add(rel);
            }
        }
        return expressionFields;
    }

    public boolean hasValidationFields() {
        return (!validFields.isEmpty() || !getEmbedWithValidations().isEmpty());
    }

    @JsonIgnore
    public List<MetaField> getEmbedWithValidations() {
        List<MetaField> embedRels = new ArrayList<MetaField>();
        List<MetaRelationship> relations = getToReference();
        for (MetaRelationship rel : relations) {
            if (rel.getRelationType() == RelationTypeEnum.Embedded
                    && rel.getRefMetaClass().hasValidationFields()) {
                embedRels.add(rel);
            }
        }
        return embedRels;
    }
    
    protected static List<String> getReservedMetaName(){
        List<String> list = new ArrayList<String>();
        list.addAll(RESERVED_METACLASS_NAME);
        return list;
    }

    protected MetaClassGraph getGraph() {
        return graph;
    }
}
