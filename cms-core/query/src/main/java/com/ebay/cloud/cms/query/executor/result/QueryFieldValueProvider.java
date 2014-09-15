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

package com.ebay.cloud.cms.query.executor.result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.IFieldValueProvider;
import com.ebay.cloud.cms.query.executor.AbstractAction;
import com.ebay.cloud.cms.query.executor.AbstractAction.ActionResult;
import com.ebay.cloud.cms.query.executor.QueryExecPlan;
import com.ebay.cloud.cms.query.executor.SearchAction;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * @author liasu
 * 
 */
public class QueryFieldValueProvider implements IFieldValueProvider {
    private final QueryExecPlan plan;
    private final ISearchField  field;
    private final QueryContext queryContext;
    private List<Object>        valueList;

    public QueryFieldValueProvider(QueryExecPlan plan, ISearchField searchField, QueryContext queryContext) {
        this.plan = plan;
        this.queryContext = queryContext;
        this.field = searchField;
        this.valueList = null;
    }

    @Override
    public Object getValue() {
        List<Object> listValues = getValueList();
        if (!listValues.isEmpty()) {
            return listValues;
        } else {
            return null;
        }
    }

    @Override
    public List<Object> getValueList() {
        if (valueList != null) {
            return valueList;
        }

        // read the root projection value from the query result
        Set<Object> values = new HashSet<Object>();
        AbstractAction rootAction = plan.getAggAction() != null ? plan.getAggAction() : plan.getRootAction();
        for (ActionResult ar : rootAction.getActionResults()) {
            SearchResult result = ar.searchResult;
            SearchAction rootSearchAction = QueryExecPlan.findHeadSearchAction(result.getMetaClass(), rootAction);
            QueryRootProjectReader rootReader = new QueryRootProjectReader(rootSearchAction.getParseNode(), field, queryContext);
            // collection the root projection from each entity
            for (IEntity entity : result.getResultSet()) {
                rootReader.reset();
                entity.traverse(rootReader);
                values.addAll(rootReader.getRootValues());
            }
        }
        valueList = new ArrayList<Object>(values);
        return valueList;
    }


}
