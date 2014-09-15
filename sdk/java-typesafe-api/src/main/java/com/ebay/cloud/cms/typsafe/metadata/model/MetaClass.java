/**
 * 
 */
package com.ebay.cloud.cms.typsafe.metadata.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.RelationTypeEnum;


/**
 * @author liasu
 * 
 */
public class MetaClass {
    private String name;
    private String pluralName;
    private String description;
    private String parent;
    private boolean allowFullTableScan;
    private String _id;
    private String repository;
    private boolean embed;
    private boolean inner;

    private Map<String, MetaField> fields = new HashMap<String, MetaField>();
    
    private MetaOption options = new MetaOption();

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getPluralName() {
        return pluralName;
    }

    public final void setPluralName(String pluralName) {
        this.pluralName = pluralName;
    }

    public final String getDescription() {
        return description;
    }

    public final void setDescription(String description) {
        this.description = description;
    }

    public final String getParent() {
        return parent;
    }

    public final void setParent(String parent) {
        this.parent = parent;
    }

    public final boolean isAllowFullTableScan() {
        return allowFullTableScan;
    }

    public final void setAllowFullTableScan(boolean allowFullTableScan) {
        this.allowFullTableScan = allowFullTableScan;
    }

    public final String get_id() {
        return _id;
    }

    public final void set_id(String _id) {
        this._id = _id;
    }

    public final String getRepository() {
        return repository;
    }

    public final void setRepository(String repository) {
        this.repository = repository;
    }

    public final boolean isEmbed() {
        return embed;
    }

    public final void setEmbed(boolean embed) {
        this.embed = embed;
    }

    public boolean isInner() {
        return inner;
    }

    public void setInner(boolean inner) {
        this.inner = inner;
    }

    public final Map<String, MetaField> getFields() {
        return fields;
    }

    public final void setFields(Map<String, MetaField> fields) {
        this.fields = fields;
    }
    
    public MetaOption getOptions() {
        return options;
    }

    public void setOptions(MetaOption options) {
        this.options = options;
    }

    @JsonIgnore
    public final void addField(MetaField field) {
        this.fields.put(field.getName(), field);
    }

    @JsonIgnore
    public MetaField getField(String name) {
        return this.fields.get(name);
    }

    @JsonIgnore
    public boolean containsField(String name) {
        return this.fields.containsKey(name);
    }

    // / help methods
    @JsonIgnore
    public List<MetaRelationship> getInnerFields() {
        List<MetaRelationship> rels = new ArrayList<MetaRelationship>();
        for (MetaField field : this.fields.values()) {
            if (field instanceof MetaRelationship) {
                MetaRelationship rel = (MetaRelationship) field;
                if (RelationTypeEnum.Inner.equals(rel.getRelationType())) {
                    rels.add(rel);
                }
            }
        }
        return rels;
    }

    @JsonIgnore
    public List<MetaRelationship> getEmbedFields() {
        List<MetaRelationship> rels = new ArrayList<MetaRelationship>();
        for (MetaField field : this.fields.values()) {
            if (field instanceof MetaRelationship) {
                MetaRelationship rel = (MetaRelationship) field;
                if (RelationTypeEnum.Embedded.equals(rel.getRelationType())) {
                    rels.add(rel);
                }
            }
        }
        return rels;
    }

}
