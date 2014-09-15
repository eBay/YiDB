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

import org.apache.commons.collections.ListUtils;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

/**
 * An entity manipulator based on the given mode. This object by given two
 * entity(given and found), it will find the delta state on the given field set
 * on the givenEntity.
 * <p/>
 * The delta is defined as those could be used to efficiently apply on the
 * foundEntity. And this apply operation should not have duplicate work like
 * de-dup in db side.
 * <p/>
 * For most cases, the delta content operation would be more efficient than
 * target values.
 * <p/>
 * 2014-03-27: Currently, only do the PUSH operator action.
 * 
 * @author liasu
 * @see EntityFieldTargetMerger
 */
public class EntityFieldDeltaMerger extends AbstractEntityFieldMerger {
    // NOTE:: current only use in push operation
    private final boolean push;
    public EntityFieldDeltaMerger() {
        push = true;
    }

    /**
     * Update the field operation entity based on the found entity. This step is
     * to make sure 1. the array length would be updated correctly 2. support
     * reference update based on reference ID matching
     */
    @SuppressWarnings("unchecked")
    public boolean mergeEntityOnField(IEntity givenEntity, String fieldName, IEntity foundEntity) {
        MetaClass metaClass = givenEntity.getMetaClass();
        MetaField field = metaClass.getFieldByName(fieldName);
        boolean hasGivenField = givenEntity.hasField(fieldName);
        boolean hasFoundField = foundEntity.hasField(fieldName);
        List<?> givenValues = givenEntity.getFieldValues(fieldName);
        List<?> foundValues = foundEntity.getFieldValues(fieldName);
        // nothing given
        if (!hasGivenField || givenValues.isEmpty()) {
            return false;
        }

        boolean array = CardinalityEnum.Many.equals(field.getCardinality());
        boolean isRelation = DataTypeEnum.RELATIONSHIP.equals(field.getDataType());
        if (array) {
            // do merge only when we found both for array
            List<?> deltaValues = null;
            if (isRelation) {
                // relation will merge based on OID
                deltaValues = mergeDeltaReference((List<IEntity>) givenValues, (List<IEntity>) foundValues);
            } else {
                // normal entity will merge by content
                deltaValues = mergeDeltaContent(givenValues, foundValues);
            }
            if (push && deltaValues.isEmpty()) {
                // for array push that has nothing after delta merge, just ignore this change.
                return false;
            }
            givenEntity.setFieldValues(fieldName, deltaValues);
        } else if (hasFoundField && !foundValues.isEmpty()) {
            // cardinality = one, simply compare the givenValue and foundValue
            Object found = foundValues.get(0);
            Object given = givenValues.get(0);
            return !compareFieldSingleValue(isRelation, found, given);
        }
        return true;
    }

    private boolean isNullOrEmpty(List<?> given) {
        return given == null || given.isEmpty();
    }

    private List<?> mergeDeltaReference(List<IEntity> givenValues, List<IEntity> foundValues) {
        // if empty given values, mean no delta operation to apply, return empty
        // list
        if (isNullOrEmpty(givenValues)) {
            return Collections.<IEntity> emptyList();
        }
        // if found value is empty, mean to apply all of the given values
        if (isNullOrEmpty(foundValues)) {
            return givenValues;
        }

        List<IEntity> result = new ArrayList<IEntity>();
        Map<String, IEntity> givenOidEntityMaps = new HashMap<String, IEntity>();
        for (IEntity entity : givenValues) {
            if (entity != null) {
                givenOidEntityMaps.put(entity.getId(), entity);
            }
        }
        // remove the values that existing in the foundValues
        for (IEntity obj : foundValues) {
            if (givenOidEntityMaps.containsKey(obj.getId())) {
                givenOidEntityMaps.remove(obj.getId());
            }
        }
        result.addAll(givenOidEntityMaps.values());
        return result;
    }

    private List<?> mergeDeltaContent(List<?> giveValues, List<?> foundValues) {
        return ListUtils.subtract(giveValues, foundValues);
    }

}
