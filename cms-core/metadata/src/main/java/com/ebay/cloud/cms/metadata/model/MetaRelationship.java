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

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.EqualsUtil;
import com.ebay.cloud.cms.utils.StringUtils;

/**
 * 
 * @author 
 */

public class MetaRelationship extends MetaField {
    

	public enum RelationTypeEnum {
		Reference, Embedded, CrossRepository, Inner
	};

	public enum ConsistencyTypeEnum {
	    Normal, Strong
	};
	
	protected IMetadataService metadataService;
	// referenced metaclass name
	private String refDataType;
	// source metaclass name
	private String srcDataType;
	/*
	 * Indicates the repository of the relation ref data type for CrossRepository relation
	 */
	protected String refRepository;
	
    protected RelationTypeEnum    relationType;
	private ConsistencyTypeEnum consistencyType;
    private boolean             cascade;

	public MetaRelationship() {
        super(false);
        
        super.setDataType(DataTypeEnum.RELATIONSHIP);
        cascade = false;
        relationType = RelationTypeEnum.Reference;
        consistencyType = ConsistencyTypeEnum.Normal;
    }

    public MetaRelationship(MetaRelationship other) {
        super(other);
        this.metadataService = other.metadataService;
        this.cascade = other.cascade;
        this.relationType = other.relationType;
        this.consistencyType = other.consistencyType;
        this.refDataType = other.refDataType;
        this.srcDataType = other.srcDataType;
        this.refRepository = other.refRepository;
    }

    public String getRefDataType() {
        return refDataType;
    }
    public void setRefDataType(String refDataType) {
        this.refDataType = refDataType;
    }
    
    public String getSourceDataType() {
        return srcDataType;
    }
    public void setSourceDataType(String srcDataType) {
        this.srcDataType = srcDataType;
    }

    public RelationTypeEnum getRelationType() {
        return relationType;
    }

    public void setRelationType(RelationTypeEnum relationType) {
        this.relationType = relationType;
    }

    public String getRefRepository() {
        return refRepository;
    }

    public void setRefRepository(String refRepository) {
        this.refRepository = refRepository;
    }

    public ConsistencyTypeEnum getConsistencyType() {
        return consistencyType;
    }
    public void setConsistencyType(ConsistencyTypeEnum consistencyType) {
        this.consistencyType = consistencyType;
    }
        
    public boolean isCascade() {
        return cascade;
    }
    public void setCascade(boolean cascade) {
        this.cascade = cascade;
    }
    
    @Override
    public String toString() {
        return "MetaRelationship [metadataService=" + metadataService
                + ", refDataType=" + refDataType + ", relationType="
                + relationType + ", cascade="
                + cascade + "]";
    }

    @JsonIgnore
    public void setMetadataService(IMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    /**
     * 
     * Returns the target metaclass of current meta relationship. For reference
     * to some nested embed. This method returns the root meta class. If the
     * caller want to identify the nested embed reference, refDataType and
     * {@code isNestedRefDataType}.
     * if it's nested embed reference, it returns the leaf metaclass
     * 
     */
    @JsonIgnore
    public MetaClass getRefMetaClass() {
        if (metadataService == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METASERVICE_PROVIDED, "no MetadataService provided to retrieve referenced MetaClass");
        }
        String refTypeName = refDataType;
        IMetadataService metaService = getRefMetaService();
        return metaService.getMetaClass(refTypeName);
    }
    
    /**
     * Returns the root metaclass of nested embed reverse reference
     * 
     */
    @JsonIgnore
    protected IMetadataService getRefMetaService() {
        IMetadataService metaService = metadataService;
        if (relationType == RelationTypeEnum.CrossRepository) {
            CheckConditions.checkState(refRepository != null, "Cross repository relationship must have refRepository set!");
            metaService = metadataService.getRepository().getRepositoryService().getRepository(refRepository)
                    .getMetadataService();
        }
        return metaService;
    }

    @JsonIgnore
    public MetaClass getSourceMetaClass() {
        if (metadataService == null) {
            throw new MetaDataException(MetaErrCodeEnum.NO_METASERVICE_PROVIDED, "no MetadataService provided to retrieve referenced MetaClass");
        }
        
        return metadataService.getMetaClass(srcDataType);
    }

    public void validate(boolean readOnlyCheck, MetaClass metaClass, Map<String, MetaClass> metas) {
        if (!readOnlyCheck) {
            validateDbName();
        }
        
        if (StringUtils.isNullOrEmpty(getName())) {
            throw new IllegalMetaClassException("field name must be provided!");
        }

        // refDataType must be provided
        String refDataType = getRefDataType();
        if (refDataType == null) {
            throw new IllegalMetaClassException("refDataType must point to referenced metaclass name!");
        }

        // self reference must be reference
        if (refDataType.equals(metaClass.getName()) && relationType == RelationTypeEnum.Embedded) {
            throw new IllegalMetaClassException("Self relationship relationType must be reference!");
        }
        // cross repo relationship must be weak reference
        if (relationType == RelationTypeEnum.CrossRepository && consistencyType == ConsistencyTypeEnum.Strong) {
            throw new IllegalMetaClassException("Cross repository must be weak reference!");
        }
        
        boolean refDot = refDataType.contains(".");
        if (refDot) {
            // an embed relationship should not have . style ref data type.
            throw new IllegalMetaClassException(String.format(
                    "the relationship %s could not reference embed metaclass: %s!",
                    getName(), refDataType));

        } else {
            validateRelationship(metaClass, metas, refDataType);
        }
    }

    private void validateRelationship(MetaClass metaClass,
            Map<String, MetaClass> metas, String refDataType) {
        // if not self reference, the referenced meta class must exist
        MetaClass ref = metas.get(refDataType);
        if (!refDataType.equals(metaClass.getName())) {
            if (ref == null) {
                ref = getRefMetaClass();
                if (ref == null) {
                    throw new IllegalMetaClassException("referenced meta class must be exist: " + refDataType);
                }
            }
        } else {
            // self reference
            ref = metaClass;
        }
        // embed relationship could only ref to emebed class when there is no . in the ref data type
        if (relationType == RelationTypeEnum.Embedded) {
            if (!ref.isEmbed()) {
                throw new IllegalMetaClassException(String.format(
                        "Target metaclass %s of embed relationship %s in %s must be embed metaclass!", refDataType, getName(), metaClass.getName()));
            }
            if (consistencyType == ConsistencyTypeEnum.Strong) {
                throw new IllegalMetaClassException(String.format(
                        "The strong relationship %s cannot be referenced to embed class %s!", getName(), refDataType));
            }
        } else if (relationType == RelationTypeEnum.Reference) {
            if (ref.isEmbed()) {
                // the relationship cannot be referenced to an embed class directly.
                throw new IllegalMetaClassException(String.format(
                    "The relationship %s cannot be referenced to embedded class %s!", getName(), refDataType));
            } else if (ref.isInner() && consistencyType == ConsistencyTypeEnum.Strong) {
                throw new IllegalMetaClassException(String.format(
                    "The strong relationship %s cannot be referenced to inner class %s!", getName(), refDataType));
            }
        } else if (relationType == RelationTypeEnum.Inner) {
            if (!ref.isInner()) {
                throw new IllegalMetaClassException(String.format(
                    "The inner relationship %s cannot be referenced to a non-inner class %s!", getName(), refDataType));
            }
            if (isMandatory()) {
                throw new IllegalMetaClassException(String.format(
                        "The inner relationship %s cannot be mandatory!", getName()));
            }
            if (consistencyType == ConsistencyTypeEnum.Strong) {
                throw new IllegalMetaClassException(String.format(
                        "The inner relationship %s cannot be strong!", getName()));
            }
        }
    }
    
    @Override
    public int hashCode() {
        if (getName() == null) {
            return "MetaRelationship".hashCode();
        }
        
        return super.hashCode();
    }
    
    @Override
    public boolean equals(Object other) {
        
        if (this == other) return true;
        
        if (!super.equals(other)) {
            return false;
        }
        
        if (!(other instanceof MetaRelationship)) return false;
        
        MetaRelationship o = (MetaRelationship)other;
        return EqualsUtil.equal(refDataType, o.refDataType) 
                && (relationType == o.relationType)
                && (cascade == o.cascade)
                && (consistencyType == o.consistencyType)
                // only compare src data type when not null
                && ( (srcDataType == null || o.srcDataType == null ) || EqualsUtil.equal(srcDataType, o.srcDataType));
    }

    @Override
    public boolean isOverridable(MetaField parentAttr) {
        return false;
    }
}
