/**
 * 
 */
package com.ebay.cloud.cms.typsafe.metadata.model;


/**
 * @author liasu
 * 
 */
public class MetaRelationship extends MetaField {
    public enum RelationTypeEnum {
        Reference, Embedded, CrossRepository, Inner;
        public static RelationTypeEnum fromString(String cte) {
            if (cte == null) {
                return Reference;
            }
            for (RelationTypeEnum c : RelationTypeEnum.values()) {
                if (cte.equals(c.name())) {
                    return c;
                }
            }
            return Reference;
        }
    }

    public enum ConsistencyTypeEnum {
        @Deprecated Weak, 
        Strong, 
        Normal;
        public static ConsistencyTypeEnum fromString(String cte) {
            if (cte == null) {
                return Normal;
            }
            for (ConsistencyTypeEnum c : ConsistencyTypeEnum.values()) {
                if (cte.equals(c.name())) {
                    return c;
                }
            }
            return Normal;
        }
    }

    // referenced meta class name
    private String refDataType;
    // source meta class name
    private String srcDataType;
    /*
     * Indicates the repository of the relation ref data type for
     * CrossRepository relation
     */
    private String refRepository;

    private RelationTypeEnum relationType;
    private ConsistencyTypeEnum consistencyType;
    private boolean cascade;

    public final String getRefDataType() {
        return refDataType;
    }

    public final void setRefDataType(String refDataType) {
        this.refDataType = refDataType;
    }

    public final String getSrcDataType() {
        return srcDataType;
    }

    public final void setSrcDataType(String srcDataType) {
        this.srcDataType = srcDataType;
    }

    public final String getRefRepository() {
        return refRepository;
    }

    public final void setRefRepository(String refRepository) {
        this.refRepository = refRepository;
    }

    public final RelationTypeEnum getRelationType() {
        return relationType;
    }

    public final void setRelationType(RelationTypeEnum relationType) {
        this.relationType = relationType;
    }

    public final ConsistencyTypeEnum getConsistencyType() {
        return consistencyType;
    }

    public final void setConsistencyType(ConsistencyTypeEnum consistencyType) {
        this.consistencyType = consistencyType;
    }

    public final boolean isCascade() {
        return cascade;
    }

    public final void setCascade(boolean cascade) {
        this.cascade = cascade;
    }

}
