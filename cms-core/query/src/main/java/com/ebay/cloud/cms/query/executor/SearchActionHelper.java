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
package com.ebay.cloud.cms.query.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.parser.ParseQueryNode;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.query.service.QueryContext.SortOrder;

public class SearchActionHelper {	
	public static void initPagination(SearchOption searchOption, ParseQueryNode parseNode){
		if (parseNode.hasLimit()) {
            searchOption.setLimit(parseNode.getLimit() + SearchOption.LOOK_FORWARD);
        } else {
            searchOption.setLimit(SearchOption.DEFAULT_LIMIT + SearchOption.LOOK_FORWARD);
        }
        searchOption.setLookForward(true);
        
        if (parseNode.hasSkip()) {
            searchOption.setSkip(parseNode.getSkip());
        }
	}
	
	public static void initSortOn(SearchOption searchOption,
			ParseQueryNode parseNode, QueryContext queryContext,
			SearchQuery searchQuery, ISearchStrategy queryStrategy) {
		if (queryContext.hasSortOn()) {
            List<String> sortOnList = queryContext.getSortOn();    
            // append default sortOn _oid
            if (!sortOnList.contains(InternalFieldEnum.ID.getName()) && queryContext.getPaginationMode() == PaginationMode.ID_BASED) {
                sortOnList.add(InternalFieldEnum.ID.getName());
            }
            List<Integer> sortOrderList = new ArrayList<Integer>(sortOnList.size());
            List<ISearchField> sortOnFieldList = new ArrayList<ISearchField>(sortOnList.size());
            
            MetaClass metaClass = parseNode.getMetaClass();
            Map<String, GroupField> grpFields = null;
            if (parseNode.getGroup() != null) {
                grpFields = parseNode.getGroup().getGrpFields();
            }
            
            for (String sortFieldName : sortOnList) {
                String[] fields = sortFieldName.split("\\.");
                MetaField sortMetaField = metaClass.getFieldByName(fields[0]);
                validateSortField(sortMetaField, sortFieldName, fields, metaClass);
//                if (sortMetaField == null) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.METAFIELD_NOT_FOUND, "Can't find sort field " + sortFieldName + " on " + metaClass.getName());
//                }
//                // array sort not supported
//                if (sortMetaField.getCardinality() == CardinalityEnum.Many && fields.length == 1) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.ARRAY_SORT_NOT_SUPPORT, "Can't sort on array field " + sortFieldName + " on " + metaClass.getName());
//                }
//                if (sortMetaField.getDataType() == DataTypeEnum.JSON) {
//                    throw new QueryExecuteException(QueryErrCodeEnum.JSON_SORT_NOT_SUPPORT, "Can't sort on json field " + sortFieldName + " on " + metaClass.getName());
//                }
                String innerField = fields.length > 1 ? StringUtils.join(ArrayUtils.subarray(fields, 1, fields.length), '.') : null;
                
                ISearchField sortOnField = null;
                if (grpFields != null) {
                    sortOnField = grpFields.get(fields[0]);
                } 

                if (sortOnField == null) {
                    sortOnField = new SelectionField(sortMetaField, innerField, queryStrategy);
                }
                sortOnFieldList.add(sortOnField);
                
                // sortOn must be in projection for ID based pagination
                ProjectionField projField = new ProjectionField(sortMetaField, innerField, false, queryStrategy);
                if (!searchQuery.getSearchProjection().getFields().contains(projField)) {
                    searchQuery.getSearchProjection().getFields().add(projField);
                }
            }
            setOrder(queryContext, sortOrderList, sortOnFieldList);
//            if (queryContext.hasSortOrder()) {
//                List<SortOrder> soList = queryContext.getSortOrder();
//                for (SortOrder order : soList) {
//                    if (order == SortOrder.asc) {
//                        sortOrderList.add(SearchOption.ASC_ORDER);
//                    } else {
//                        sortOrderList.add(SearchOption.DESC_ORDER);
//                    }
//                }
//            } else {
//                // set default sort order as ascend
//                for (int i = 0; i < sortOnFieldList.size(); i++) {
//                    sortOrderList.add(SearchOption.ASC_ORDER);
//                }
//            }
            searchOption.setSortField(sortOnFieldList, sortOrderList, metaClass);
        } else {
            // sort on _oid if not given
            searchOption.setSort(Arrays.asList(InternalFieldEnum.ID.getName()), Arrays.asList(SearchOption.ASC_ORDER),
                    parseNode.getMetaClass());
        }
	}
	
	private static void setOrder(QueryContext queryContext, List<Integer> sortOrderList, List<ISearchField> sortOnFieldList){
        if (queryContext.hasSortOrder()) {
            List<SortOrder> soList = queryContext.getSortOrder();
            for (SortOrder order : soList) {
                if (order == SortOrder.asc) {
                    sortOrderList.add(SearchOption.ASC_ORDER);
                } else {
                    sortOrderList.add(SearchOption.DESC_ORDER);
                }
            }
        } else {
            // set default sort order as ascend
            for (int i = 0; i < sortOnFieldList.size(); i++) {
                sortOrderList.add(SearchOption.ASC_ORDER);
            }
        }
	}
	
	private static void validateSortField(MetaField sortMetaField,
			String sortFieldName, String[] fields, MetaClass metaClass) {
		if (sortMetaField == null) {
			throw new QueryExecuteException(
					QueryErrCodeEnum.METAFIELD_NOT_FOUND, "Can't find sort field " + sortFieldName + " on " + metaClass.getName());
		}
		// array sort not supported
		if (sortMetaField.getCardinality() == CardinalityEnum.Many
				&& fields.length == 1) {
			throw new QueryExecuteException(
					QueryErrCodeEnum.ARRAY_SORT_NOT_SUPPORT, "Can't sort on array field " + sortFieldName + " on " + metaClass.getName());
		}
		if (sortMetaField.getDataType() == DataTypeEnum.JSON) {
			throw new QueryExecuteException(
					QueryErrCodeEnum.JSON_SORT_NOT_SUPPORT,"Can't sort on json field " + sortFieldName + " on " + metaClass.getName());
		}
	}

}
