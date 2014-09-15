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

package com.ebay.cloud.cms.dal.search;

import java.util.Collection;
import java.util.HashSet;

import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;

/**
 * This class define the which fields will be return.
 * 
 * 1. "*":      return all fields
 * 2. empty:    only return _id, _type and _name
 * 3. fields:   return _id, _type, _name and fields
 * 
 * @author xjiang
 *
 */
public final class SearchProjection {

    private boolean                  isStar;
    private Collection<ISearchField> fields;
    private Collection<String>       fieldNames;

    public SearchProjection(SearchProjection other) {
        this.isStar = other.isStar;
        this.fields = new HashSet<ISearchField>(other.fields);
        this.fieldNames = new HashSet<String>(other.fieldNames);
    }

    public SearchProjection() {
        isStar = false;
        fields = new HashSet<ISearchField>();
        fieldNames = new HashSet<String>();
    }

    public Collection<ISearchField> getFields() {
        return fields;
    }

    public void addField(ISearchField field) {
        if (field == ProjectionField.STAR) {
            isStar = true;
            return;
        }
        this.fields.add(field);
        this.fieldNames.add(field.getFieldName());
    }

    public boolean hasField(String fieldName) {
        return fieldNames.contains(fieldName);
    }

    public boolean isEmpty() {
        if (isStar) 
            return false;
        return fields.isEmpty();
    }

    public boolean hasStar() {
        return isStar;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{selections=");
        if (isStar) {
            sb.append("*");
        } else {
            sb.append(fields);
        }
        
        sb.append('}');
        return sb.toString();
    }

}
