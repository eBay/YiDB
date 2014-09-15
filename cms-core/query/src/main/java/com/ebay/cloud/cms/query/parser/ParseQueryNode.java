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

package com.ebay.cloud.cms.query.parser;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.executor.SearchCursor;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryLexer;

/**
 * 
 * @author xjiang
 *
 */
public class ParseQueryNode  extends ParseBaseNode {
    
    private MetaClass metaClass;
    
    private MetaRelationship metaReference;
    
    private boolean reverseReference;
    
    private final List<MetaClass> typeCasts;
    
    private SearchProjection projection;
    private SearchProjection userProjection;
    private Collection<String> projectionFields;
    
    private SearchCriteria criteria;
    
    private SearchGroup group;
    
    private SearchCriteria groupCriteria;
    
    private List<ParseBaseNode> subQueryNodes;
    
    private int querySequence;
    
    /**
     * Cursor value as an entity which this query node would started begin with
     */
    private SearchCursor cursor;
    
    private boolean userDisplay;
    
    private boolean rootDisplay;
    
    private Map<String, List<ParseQueryNode>> nextQueryNodes;

    private Set<String> fieldProjectSet;
    /**
     * Projection field property map for each meta field.
     * Store: for a given meta field, the field properties that need to be displayed.
     */
    private Map<MetaField, Map<FieldProperty, MetaAttribute>> fieldPropProjectMap;
    
    public ParseQueryNode() {
        super(CMSQueryLexer.QUERY_NODE);
        projection = new SearchProjection();
        userProjection = new SearchProjection();
        typeCasts = new LinkedList<MetaClass>();
        subQueryNodes = new LinkedList<ParseBaseNode>();
        reverseReference = false;
        cursor = new SearchCursor(0, 0, null);
        userDisplay = false;
        rootDisplay = false;
        nextQueryNodes = new HashMap<String, List<ParseQueryNode>>();
    }
    
    public MetaClass getMetaClass() {
        return metaClass;
    }
    public void setMetaClass(MetaClass metadata) {
        this.metaClass = metadata;
    }
    
    public void addTypeCast(MetaClass metadata) {
        typeCasts.add(metadata);
    }    
    public List<MetaClass> getTypeCast() {
        return typeCasts;
    }
    public boolean hasTypeCast() {
        return typeCasts.size() > 0;
    }
    public void setTypeCasts(Collection<MetaClass> typeCasts) {
        this.typeCasts.clear();
        if (typeCasts != null) {
            this.typeCasts.addAll(typeCasts);
        }
    }
    
    public void setMetaReference(MetaRelationship metaRef) {
        this.metaReference = metaRef;
        this.reverseReference = false;
    }
    public void setReverseMetaReference(MetaRelationship metaRef) {
        this.metaReference = metaRef;
        this.reverseReference = true;
    }
    public MetaRelationship getMetaReference() {
        return this.metaReference;
    }
    public boolean isReverseReference() {
        return reverseReference;
    }
   
    public SearchProjection getProjection() {        
        return projection;
    }
    public void setProjection(SearchProjection projection) {
        this.projection = projection;
        // reset project fields;
        this.projectionFields = null;
    }
    public void addProjection(ISearchField field) {
        projection.addField(field);
    }
    public boolean hasProjection() {
        return !projection.isEmpty();
    }
    public SearchProjection getUserProjection() {
        return userProjection;
    }
    public void addUserSelection(ISearchField field) {
        userProjection.addField(field);
        projection.addField(field);
    }

    public Collection<String> getProjectionFields() {
        if (projectionFields == null) {
            projectionFields = new HashSet<String>();
            for (ISearchField searchField : projection.getFields()) {
                if (searchField.isProjected()) {
                    projectionFields.add(getProjectionName(searchField));
                }
            }
            // force _type as display
            projectionFields.add(InternalFieldEnum.TYPE.getName());
            projectionFields.add(InternalFieldEnum.ID.getName());
        }
        return projectionFields;
    }
    private String getProjectionName(ISearchField searchField) {
        if (searchField instanceof AggregationField) {
            StringBuilder sb = new StringBuilder(searchField.getFieldName());
            sb.setCharAt(0, '$');
            return sb.toString();
        }
        return searchField.getFieldName();
    }

    public void setCriteria(SearchCriteria criteria) {
        this.criteria = criteria;
    }
    public SearchCriteria getCriteria() {
        return criteria;
    }
    
    public SearchGroup getGroup() {
        return group;
    }
    public void setGroup(SearchGroup group) {
        this.group = group;
    }
    public boolean hasGroup() {
        return group != null;
    }
    
    public SearchCriteria getGroupCriteria() {
        return groupCriteria;
    }
    public void setGroupCriteria(SearchCriteria criteria) {
        this.groupCriteria = criteria;
    }

    public void addSubQuery(ParseBaseNode subQuery) {
        subQueryNodes.add(subQuery);
    }
    public List<ParseBaseNode> getSubQueryNodes() {
        return subQueryNodes;
    }

    public int getQuerySequence() {
        return querySequence;
    }
    public void setQuerySequence(int querySequence) {
        this.querySequence = querySequence;
    }

    public int getSkip() {
        return cursor.getSkip();
    }
    public void setSkip(int skip) {
        cursor.setSkip(skip);
    }
    public boolean hasSkip() {
        return cursor.hasSkip();
    }

    public int getLimit() {
        return cursor.getLimit();
    }
    public void setLimit(int limit) {
        cursor.setLimit(limit);
    }
    public boolean hasLimit() {
        return cursor.hasLimit();
    }

    public void markRootDisplay() {
        rootDisplay = true;
        userDisplay = true;
    }
    public boolean isRootDisplay() {
        return rootDisplay;
    }
    
    public void markUserDisplay() {
        userDisplay = true;
    }
    public boolean isUserDisplay() {
        return userDisplay;
    }

    public void addNextQueryNode(ParseQueryNode nextQueryNode) {
        String fieldName = nextQueryNode.getMetaReference().getName();
        List<ParseQueryNode> nodes = nextQueryNodes.get(fieldName);
        if (nodes == null) {
            nodes = new LinkedList<ParseQueryNode>();
        }
        
        nodes.add(nextQueryNode);
        nextQueryNodes.put(fieldName, nodes);
    }
    
    public List<ParseQueryNode> getNextQueryNode(MetaRelationship metaReference) {
        return nextQueryNodes.get(metaReference.getName());
    }

    public Map<String, List<ParseQueryNode>> getNextQueryNodes() {
        return nextQueryNodes;
    }
    
    public Set<String> getFieldProjectSet() {
        if (fieldProjectSet == null) {
            Set<String> set = new HashSet<String>();
            for (ISearchField searchField : projection.getFields()) {
                if (searchField instanceof AbstractSearchField && (((AbstractSearchField)searchField).getInnerProperty() == null)) {
                    set.add(searchField.getFieldName());
                }
            }
            fieldProjectSet = set;
        }
        return fieldProjectSet;
    }
    
    public Map<MetaField, Map<FieldProperty, MetaAttribute>> getFieldPropProjectMap() {
        if (fieldPropProjectMap == null) {
            Map<MetaField, Map<FieldProperty, MetaAttribute>> map = new HashMap<MetaField, Map<FieldProperty, MetaAttribute>>();
            for (ISearchField searchField : projection.getFields()) {
                addFieldProjection(map, searchField);
            }
            fieldPropProjectMap = map;
        }
        return fieldPropProjectMap;
    }
    private void addFieldProjection(Map<MetaField, Map<FieldProperty, MetaAttribute>> map, ISearchField projField) {
        if (projField instanceof AbstractSearchField && (((AbstractSearchField)projField).getInnerProperty() != null)) {
            // for field property projection, construct the proper information for projection
            MetaField metaField = null;
            AbstractSearchField field = (AbstractSearchField) projField;
            metaField = field.getRootField();
            Map<FieldProperty, MetaAttribute> sets = map.get(metaField);
            if (sets == null) {
                sets = new HashMap<FieldProperty, MetaAttribute>();
                map.put(metaField, sets);
            }
            FieldProperty property = field.getInnerProperty();
            String targetFieldName = field.getFieldName() + "." + field.getInnerProperty().getName();
            // fake attribute to the field property
            MetaAttribute attribute = new MetaAttribute(true);
            attribute.setName(targetFieldName);
            attribute.setDataType(property.getType());
            sets.put(property, attribute);
        }
    }

    protected void doValidate(ParseContext context) {
        validateCountOnly(context);
        validateTypeCast();
        validateAggregation(context);
        validateProjection(context);
        validateSubQuery(context);
        validateReverseQuery(context);
        context.setQueryNode(this);
    }
    
    private void validateReverseQuery(ParseContext context) {
        if (reverseReference) {
            ParseQueryNode lastQueryNode = context.getQueryNode();
            MetaClass lastMetaQuery = lastQueryNode.getMetaClass();
            MetaClass refClass = this.metaReference.getRefMetaClass();
            if (!refClass.isAssignableFrom(lastMetaQuery)) {
                throw new QueryParseException(QueryErrCodeEnum.SYNTAX_ERROR, MessageFormat.format(
                        "Reverse reference doesn''t match! Expected {0}, but reverse reference is {1}",
                        lastMetaQuery.getName(), refClass.getName()));
            }

            if (metaReference.getRelationType() == RelationTypeEnum.Embedded) {
	                throw new QueryParseException(QueryErrCodeEnum.REVERSE_QUERY_ON_EMBED_NOT_SUPPORT, MessageFormat.format(
	                        "Reverse query {0} on embed meta class {1} is not supported",
	                        metaReference.getName(), metaReference.getSourceDataType()));
            }
        }
    }

    private void validateCountOnly(ParseContext context) {
        if (context.getQueryNodeCount() > 1 && context.isCountOnly()) {
            throw new QueryParseException(QueryErrCodeEnum.JOIN_COUNT_NOT_SUPPORT, MessageFormat.format("Join query does not support count-only: {0}!", context.getQueryString()));
        }
    }

    private void validateTypeCast() {
        for (MetaClass castType : typeCasts) {
            if (!metaClass.isAssignableFrom(castType)) {
                throw new QueryParseException(QueryErrCodeEnum.TYPE_CAST_NOT_SUBMETA, MessageFormat.format(
                        "Type cast {0} not sub-metaclass of {1}!", castType.getName(), metaClass.getName()));
            }
        }
    }
    
    private void validateAggregation(ParseContext context) {
        if (!hasGroup()) {
            return;
        }
        // Query could have only one group aggregation
        if (context.getAggregationCount() > 1) {
            throw new QueryParseException(QueryErrCodeEnum.MULTI_AGGR_FORBID,
                    "Query could have only one group aggregation!");
        }
        // Set operation could be not placed before aggregation query
        if (context.hasSet()) {
            throw new QueryParseException(QueryErrCodeEnum.AGGREGATION_MUST_BEFORE_SET,
                    "Set operator could not be placed before group aggregation!");
        }
    }
    
    private void validateProjection(ParseContext context) {
        if (!hasProjection()) {
            return;
        }
        
        if (hasGroup()) {
            // projection on group query must be either aggregation func or group field
            Collection<ISearchField> groupProjectFields = group.getProjectFields();
            Collection<ISearchField> projectFields = projection.getFields();
            for (ISearchField pField : projectFields) {
                if (!groupProjectFields.contains(pField)) {
                    throw new QueryParseException(QueryErrCodeEnum.IILEGAL_PROJECTION, MessageFormat.format(
                            "project field {0} is not a aggregation function or group fields!", pField.getFieldName()));
                }
            }
        } else {
            // aggregation query can has projection on one query node
            if (context.hasAggregation()) {
                throw new QueryParseException(QueryErrCodeEnum.PROJECT_NON_AGGR,
                        "projection on the non-aggreation group query is invalid!");
            }
        }
        
        // add projection
        context.addProjection(projection);
    }
        
    private void validateSubQuery(ParseContext context) {
        for (ParseBaseNode subQueryNode : subQueryNodes) {
            ParseContext subContext = new ParseContext(context.getQueryContext());
            subQueryNode.validate(subContext);
            if (subContext.getProjectionCount() > 1) {
                throw new QueryParseException(QueryErrCodeEnum.IILEGAL_PROJECTION, "sub query could only have one root projection!");
            }
        }
    }
    
    public boolean hasCursor() {
        return cursor != null;
    }
    public SearchCursor getCursor() {
        return cursor;
    }
    public void setCursor(SearchCursor cursorValue) {
        this.cursor = cursorValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QueryNode[metaclass=").append(metaClass.getName());
        if (metaReference != null) {
            sb.append(", metareference=").append(metaReference.getName());
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public Set<String> checkSetMetaClasses() {
        Set<String> metaSet = new HashSet<String>();
        metaSet.add(metaClass.getName());
        
        for (MetaClass meta : typeCasts) {
            metaSet.add(meta.getName());
        }
        
        for (ParseBaseNode nextNode : getNextNodes()) {
            Set<String> metas = nextNode.checkSetMetaClasses();
            metaSet.addAll(metas);
        }
        
        return metaSet;
    }
    
}
