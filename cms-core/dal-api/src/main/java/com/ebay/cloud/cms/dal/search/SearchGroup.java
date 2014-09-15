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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class define group in aggregation
 * 
 * @author xjiang
 * 
 */
public class SearchGroup {

    private final Map<String, GroupField> grpFields;
    private final Map<String, AggregationField> aggFields;
    
    public SearchGroup() {
        grpFields = new HashMap<String, GroupField>();
        aggFields = new HashMap<String, AggregationField>();
    }

    public void addGroupField(GroupField field) {
        grpFields.put(field.getFieldName(), field);
    }

    public void addAggregationField(AggregationField field) {
        aggFields.put(field.getFieldName(), field);
    }

    public Map<String, GroupField> getGrpFields() {
        return Collections.unmodifiableMap(grpFields);
    }

    public Collection<ISearchField> getProjectFields() {
        Collection<ISearchField> defaultProjects = new HashSet<ISearchField>();
        for (ISearchField sf : grpFields.values()) {
            defaultProjects.add(sf);
        }
        for (ISearchField sf : aggFields.values()) {
            defaultProjects.add(sf);
        }
        return defaultProjects;
    }

    public Map<String, AggregationField> getAggFields() {
        return Collections.unmodifiableMap(aggFields);
    }

    public final List<DBObject> buildUnwindQuery() {
        List<DBObject> results = null;
        for (GroupField groupField : grpFields.values()) {
            if (CardinalityEnum.Many.equals(groupField.getSearchFiled().getRootField().getCardinality())) {
                // build unwind object if find any group field cardinality=many
                if (results == null) {
                    results = new ArrayList<DBObject>();
                }
                DBObject unwind = new BasicDBObject();
                unwind.put("$unwind", '$' + groupField.getSearchFiled().getFullDbName());
                results.add(unwind);
            }
        }
        return results;
    }

    public final BasicDBObject buildGroupQuery() {
        BasicDBObject group = new BasicDBObject();
        translateGroupId(group);
        translateAggregation(group);

        BasicDBObject pipeline = new BasicDBObject();
        pipeline.put("$group", group);
        return pipeline;
    }

    private void translateAggregation(BasicDBObject group) {
        for (AggregationField aggField : aggFields.values()) {
            DBObject grpAttribute = new BasicDBObject();
            if (aggField.func == AggFuncEnum.COUNT) {
                grpAttribute.put("$" + AggFuncEnum.SUM.getName(), 1);
            } else {
                grpAttribute.put("$" + aggField.func.getName(), "$" + aggField.getSearchField().getFullDbName());
            }
            group.append(aggField.getFieldName(), grpAttribute);
        }
    }

    private void translateGroupId(BasicDBObject group) {
        BasicDBObject idAttributes = new BasicDBObject();
        for (GroupField groupField : grpFields.values()) {
            idAttributes.append(groupField.getFieldName(), "$" + groupField.getSearchFiled().getFullDbName());
        }
        group.put("_id", idAttributes);
    }

    @Override
    public String toString() {
        return "SearchGroup [grpFields=" + grpFields + ", aggFields=" + aggFields + "]";
    }
}
