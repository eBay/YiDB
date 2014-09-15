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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.ListUtils;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

/**
 * An entity manipulator based on the given mode. This object by given two
 * entity(given and found), it will find the target state on the given field set on the givenEntity.
 * 
 * @author liasu
 * 
 */
public class EntityFieldTargetMerger extends AbstractEntityFieldMerger {
    
    private final boolean pull;

    public EntityFieldTargetMerger(boolean isPull) {
        this.pull = isPull;
    }

    /**
     * Update the field operation entity based on the found entity. This step is
     * to make sure
     * <ul>
     * <li>1. the array length would be updated correctly</li>
     * <li>2. support reference update based on reference ID matching</li>
     * </ul>
     */
    public boolean mergeEntityOnField(IEntity givenEntity, String fieldName, IEntity foundEntity) {
        MetaClass metaClass = givenEntity.getMetaClass();
        MetaField field = metaClass.getFieldByName(fieldName);
        boolean isRelation = DataTypeEnum.RELATIONSHIP.equals(field.getDataType());
        boolean array = CardinalityEnum.Many.equals(field.getCardinality());
        boolean hasFoundField = foundEntity.hasField(fieldName);
        List<?> givenValues = givenEntity.getFieldValues(fieldName);
        List<?> foundValues = foundEntity.getFieldValues(fieldName);

        AtomicBoolean hasChange = new AtomicBoolean(false);
        if (array) {
            boolean hasGivenField = givenEntity.hasField(fieldName);
            if (!hasGivenField || givenValues.isEmpty()) {
                return false;
            }

            // do merge only when we found both for array
            if (hasFoundField) {
                List<?> mergeValues = null;
                if (isRelation) {
                    // relation will merge based on OID
                    mergeValues = mergeTargetReference(givenValues, foundValues, hasChange);
                } else {
                    // normal entity will merge by content
                    mergeValues = mergeTargetContent(givenValues, foundValues, hasChange);
                }
                if (!hasChange.get()) {
                    return false;
                }
                givenEntity.setFieldValues(fieldName, mergeValues);
            }
            return true;
        } else {
            // cardinality=ONE
            return mergeTargetSingle(givenEntity, fieldName, isRelation, hasFoundField, givenValues, foundValues);
        }
    }

    private boolean mergeTargetSingle(IEntity givenEntity, String fieldName, boolean isRelation, boolean hasFoundField,
            List<?> givenValues, List<?> foundValues) {
        boolean hasChange = false;
        if (pull) {
            hasChange = mergePullSingle(givenEntity, fieldName, hasFoundField);
        } else {
            hasChange = mergePushSingle(isRelation, givenValues, foundValues);
        }
        return hasChange;
    }

    private boolean mergePushSingle(boolean isRelation, List<?> givenValues, List<?> foundValues) {
        boolean hasChange = false;
        if (!foundValues.isEmpty()) {
            if (!givenValues.isEmpty()) {
                Object given= givenValues.get(0);
                Object found= foundValues.get(0);
                hasChange = !compareFieldSingleValue(isRelation, found, given);
            }
        } else {
            hasChange = true;
        }
        return hasChange;
    }

    private boolean mergePullSingle(IEntity givenEntity, String fieldName, boolean hasFoundField) {
        boolean hasChange = false;
        if (hasFoundField) {
            // remove this field from given entity to indicate this field should be removed
            givenEntity.removeField(fieldName);
            hasChange = true;
        } else {
            // pull on an non-existing field, means nothing
            hasChange = false;
        }
        return hasChange;
    }

    @SuppressWarnings("unchecked")
    private List<?> mergeTargetReference(List<?> givenValues, List<?> foundValues, AtomicBoolean hasChange) {
        List<?> mergeValues;
        if (pull) {
            mergeValues = mergeDeleteReference((List<IEntity>) givenValues, (List<IEntity>) foundValues, hasChange);
        } else {
            mergeValues = mergeUpdateReference((List<IEntity>) givenValues, (List<IEntity>) foundValues, hasChange);
        }
        return mergeValues;
    }
    
    /**
     * Merge the delete references.
     * 
     * @param giveValues
     * @param foundValues
     * @param hasChange 
     * @param output_DeleteValues
     *            - this is OUTPUT parameters. The delete values would be add
     *            into this list.
     * @return
     */
    private List<?> mergeDeleteReference(List<IEntity> giveValues, List<IEntity> foundValues, AtomicBoolean hasChange) {
        if (foundValues.isEmpty()) {
            hasChange.set(true);
            return Collections.<IEntity> emptyList();
        }

        Map<String, IEntity> oids = new HashMap<String, IEntity>();
        for (IEntity entity : foundValues) {
            oids.put(entity.getId(), entity);
        }

        for (IEntity obj : giveValues) {
            if (oids.containsKey(obj.getId())) {
                hasChange.set(true);
                oids.remove(obj.getId());
            }
        }
        return new ArrayList<IEntity>(oids.values());
    }

    private List<?> mergeUpdateReference(List<IEntity> giveValues, List<IEntity> foundValues, AtomicBoolean hasChange) {
        Map<String, IEntity> oids = new HashMap<String, IEntity>();
        for (IEntity entity : foundValues) {
            oids.put(entity.getId(), entity);
        }

        for (IEntity obj : giveValues) {
            if (!oids.containsKey(obj.getId())) {
                hasChange.set(true);
                oids.put(obj.getId(), obj);
            }
        }
        return new ArrayList<IEntity>(oids.values());
    }

    private List<?> mergeTargetContent(List<?> giveValues, List<?> foundValues, AtomicBoolean hasChange) {
        List<?> result = null;
        if (pull) {
            result = ListUtils.subtract(foundValues, giveValues);
        } else {
            result = ListUtils.sum(foundValues, giveValues);
        }
        hasChange.set(!ListUtils.isEqualList(result, foundValues));
        return result;
    }
}
