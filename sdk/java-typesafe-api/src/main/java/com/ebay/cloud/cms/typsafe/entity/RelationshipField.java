/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

/**
 * @author liasu
 * 
 */
public class RelationshipField<T extends ICMSEntity, K extends ICMSEntity> {

    private final Class<T> source;
    private final Class<K> target;
    private final String name;

    public RelationshipField(Class<T> source, Class<K> target, String name) {
        this.source = source;
        this.target = target;
        this.name = name;
    }

    public Class<T> getSourceClass() {
        return this.source;
    }

    public Class<K> getTargetClass() {
        return this.target;
    }

    public String getFieldName() {
        return this.name;
    }
}
