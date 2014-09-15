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

package com.ebay.cloud.cms.query.executor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;

/**
 * @author liasu
 * 
 */
public class SearchCursor {
    private int     skip;
    private int     limit;
    private JsonEntity cursorValue;

    public SearchCursor(int skip, int limit, JsonEntity cursorValue) {
        this.limit = limit;
        this.skip = skip;
        this.cursorValue = cursorValue;
    }

    public int getSkip() {
        return skip;
    }
    public void setSkip(int skip) {
        this.skip = skip;
    }
    public boolean hasSkip() {
        return skip >= 0;
    }

    public int getLimit() {
        return limit;
    }
    public void setLimit(int limit) {
        this.limit = limit;
    }
    public boolean hasLimit() {
        return this.limit > 0;
    }

    public boolean hasCursorCriteria() {
        return cursorValue != null;
    }

    public SearchCriteria getCursorCriteria(SearchOption option, ParseQueryNode node, QueryContext queryContext) {
        // only enable criteria fo ID_BASED mode or sub query or count only(this is to keep the same behavior with SKIP_BASED, mongo discard the skip() and limit() on count() query)
        if (queryContext.getPaginationMode() != PaginationMode.ID_BASED || queryContext.isSubQuery() || queryContext.isCountOnly()) {
            return null;
        }

        MetaField idField = node.getMetaClass().getFieldByName(InternalFieldEnum.ID.getName());
        if (cursorValue == null || !option.hasSort()) {
            if (option.hasSort() && option.getSortFields().size() > 1) {
                // found in mongo, when a query to single table, without _oid criteria and have sortOn on other fields, the query would not use _oid index_.
                return new FieldSearchCriteria(idField, queryContext.getRegistration().searchStrategy, FieldOperatorEnum.GT, "");
            }
            return null;
        }

        List<ISearchField> sortFields = option.getSortFields();
        List<Integer> sortOrders = option.getSortOrders();
        LinkedList<SearchCriteria> criterias = findPivotField(node.getMetaClass(), sortFields, sortOrders);
        int pivot = criterias.size();

        LogicalSearchCriteria orCriteria = new LogicalSearchCriteria(LogicOperatorEnum.OR);
        // from pivot to the start of the sortOn field
        for (int i = pivot - 1; i >= 0; i--) {
            criterias.pollLast();
            SearchCriteria exclusiveCriteria = getCriteria(sortOrders.get(i), sortFields.get(i), false);
            LogicalSearchCriteria andCriteria = new LogicalSearchCriteria(LogicOperatorEnum.AND);
            for (SearchCriteria sc : criterias) {
                andCriteria.addChild(sc);
            }
            andCriteria.addChild(exclusiveCriteria);

            // put into or criteria list
            orCriteria.addChild(andCriteria);
        }

        return orCriteria;
    }

    private LinkedList<SearchCriteria> findPivotField(MetaClass metaClass, List<ISearchField> sortFields, List<Integer> sortOrders) {
        LinkedList<SearchCriteria> criterias = new LinkedList<SearchCriteria>();
        final boolean inclusive = true;
        for (int i = 0; i < sortFields.size(); i++) {
            ISearchField field = sortFields.get(i);
            SearchCriteria cri = getCriteria(sortOrders.get(i), field, inclusive);
            criterias.add(cri);
            boolean isPivot = false;
            if (field.getInnerProperty() == null && field.getInnerField() == null) {
                // only field itself could build index
                Collection<IndexInfo> indexes = metaClass.getIndexesOnField(field.getFieldName());
                for (IndexInfo ii : indexes) {
                    if (ii.getIndexOptions().contains(IndexOptionEnum.unique) && ii.getKeyList().size() == 1) {
                        // only the field that itself is unique could be used as pivot
                        isPivot = true;
                        break;
                    }
                }
            }

            if (isPivot) {
                break;
            }
        }
        // no pivot found, return the last one
        return criterias;
    }

    private SearchCriteria getCriteria(Integer sortOrder, ISearchField field, boolean inclusive) {
        // check and get criteria value
        boolean exist = false;
        Object value = null;
        if (field.getInnerField() == null && field.getInnerProperty() == null) {
            exist = cursorValue.hasField(field.getFieldName());
            if (exist) {
                List<?> fieldValues = cursorValue.getFieldValues(field.getFieldName());
                value = fieldValues.isEmpty() ? null : fieldValues.get(0);
            }
        } else if (field.getInnerProperty() != null) {
            exist = cursorValue.hasField(field.getFieldName() + "." + field.getInnerProperty().getName());
            if (exist) {
                value = cursorValue.getFieldProperty(field.getFieldName(), field.getInnerProperty().getName());
            }
        } 

        // generate criteria based on the value and order
        SearchCriteria criteria = null;
        if (SearchOption.ASC_ORDER.equals(sortOrder)) {
            if (inclusive) {
                // asc order inclusive
                criteria = getInclusiveCriteria(field, exist, value);
            } else {
                if (exist && value != null) {
                    // asc order, has value case, the exclusive is $GT
                    criteria = new FieldSearchCriteria(field, FieldOperatorEnum.GT, value);
                } else {
                    // asc order, null or not-existing case : the exclusive is : $ne : null
                    criteria = new FieldSearchCriteria(field, FieldOperatorEnum.NE, (Object) null);
                }
            }
        } else {
            if (inclusive) {
                // desc order inclusive
                criteria = getInclusiveCriteria(field, exist, value);
            } else {
                if (exist && value != null) {
                    // desc order, has value case, the exlusive is  $or : [ < value, null, not-existing]
                    LogicalSearchCriteria or = new LogicalSearchCriteria(LogicOperatorEnum.OR);
                    or.addChild(new FieldSearchCriteria(field, FieldOperatorEnum.LT, value));
                    or.addChild(new FieldSearchCriteria(field, FieldOperatorEnum.EQ, (Object)null));
                    criteria = or;
                } else {
                    // desc order, null or not existing case : no exclusive criteria
                    criteria = null;
                }
            }
        }
        return criteria;
    }

    private SearchCriteria getInclusiveCriteria(ISearchField field, boolean exist, Object value) {
        SearchCriteria criteria = null;
        if (exist && value != null) {
            criteria = new FieldSearchCriteria(field, FieldOperatorEnum.EQ, value);
        } else {
            // mongo sortOn treat not-existing and null as one case... use $eq : null
            criteria = new FieldSearchCriteria(field, FieldOperatorEnum.EQ, (Object)null);
        }
        return criteria;
    }

    public JsonEntity getCursorValue() {
        return cursorValue;
    }

}
