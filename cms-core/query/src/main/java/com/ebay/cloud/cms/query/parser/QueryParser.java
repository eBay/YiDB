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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.RegexValue;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.parser.ParseBaseNode.ParseNodeTypeEnum;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryLexer;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryParser;
import com.ebay.cloud.cms.query.service.QueryContext;

/**
 * CMS query parser that convert anltr AST to query node graph
 *
 * @author xjiang
 */
public class QueryParser {
    private static final Logger logger = LoggerFactory.getLogger(QueryParser.class);

    private final QueryContext queryContext;
    private ParseContext parseContext;

    public QueryParser(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public ParseBaseNode parse(String query) {
        // parse string to AST by antlr parser
        CommonTree queryTree = parseAST(query);
        // parse AST to query node list
        try {
            ParseBaseNode rootNode = parseQuery(queryTree, query);
            rootNode.validate(parseContext);
            // post validation actions that need the overall statistics information like query node count
            postValidate(rootNode);
            return rootNode;
        } catch (QueryParseException e) {
            logger.error(e.getMessage());
            throw e;
        } catch (Throwable t) {
            throw new QueryParseException(QueryErrCodeEnum.SYNTAX_ERROR, MessageFormat.format("{0}!Syntax Error: {1}", query, t.getMessage()), t);
        }
    }

    private void postValidate(ParseBaseNode rootNode) {
        if (queryContext.getHint() >= parseContext.getQueryNodeCount() || parseContext.hasSet()) {
            // clear query hint if not feasible
            queryContext.setHint(-1);
        }
        if (ParseQueryNode.class.isInstance(rootNode)) {
            queryContext.parseQueryCursor((ParseQueryNode) rootNode, parseContext.getQueryNodeCount());
        }
        if (parseContext.getQueryNodeCount() > 1) {
            // joined query doesn't support sort on
            if (queryContext.hasSortOn()) {
                List<String> sortOns = queryContext.getSortOn();
                for (String sortOn : sortOns) {
                    if (!InternalFieldEnum.ID.getName().equals(sortOn)) {
                        throw new QueryParseException(QueryErrCodeEnum.JOIN_SORT_NOT_SUPPORT,
                                "Join query with sort is not supported! It's implicitly sort on _oid.");
                    }
                }
            }
            queryContext.removeSortOn();
            queryContext.removeSortOrder();
        }
        
        if (rootNode.getType() == ParseNodeTypeEnum.INTERSECTION
                || rootNode.getType() == ParseNodeTypeEnum.UNION) {
            rootNode.checkSetMetaClasses();
        }
    }
    
    /**
     * parse the query string into anltr AST
     * 
     * @param query
     * @return
     */
    private CommonTree parseAST(String query) {
        ANTLRStringStream stream = new ANTLRStringStream(query);
        CMSQueryLexer lexer = new CMSQueryLexer(stream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        CMSQueryParser parser = new CMSQueryParser(tokenStream);
        ParserRuleReturnScope result = null;
        try {
            result = parser.query();
        } catch (RecognitionException re) {
            throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, query, re);
        }
        CommonToken stopToken = (CommonToken) result.getStop();
        if (stopToken.getStopIndex() != query.length() - 1) {
            int stop = stopToken.getStopIndex();
            String part = "none";
            if (stop > 0) {
                part = query.substring(0, stop + 1);
            }
            throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, "query - " + query + " syntax error! - Consumed query part : " + part + " !");
        }
        return (CommonTree)result.getTree();
    }
    
    /**
     * parse the AST to parse node list. 
     * we will also do the metadata validation during the parsing. 
     * 
     * @param queryTree
     * @return
     */
    private ParseBaseNode parseQuery(Tree queryTree, String queryString) {
        this.parseContext = new ParseContext(queryContext);
        this.parseContext.setQueryString(queryString);
        Tree pathTree = queryTree.getChild(0);
        return parsePath(pathTree);
    }
    
    /**
     * traversal path tree to build path level tree
     * 
     * @param pathTree
     * @param query
     * @return
     */
    private ParseBaseNode parsePath(Tree pathTree) {        
        ParseBaseNode currNode = null;
        ParseBaseNode leftNode = null;
        ParseBaseNode rightNode = null;        
        
        int lexType = pathTree.getType();
        switch (lexType) {
            case CMSQueryLexer.QUERY_NODE:
                ParseQueryNode queryNode = parseQueryNode(pathTree);
                currNode = queryNode;
                parseContext.setQueryNode(queryNode);
                break;
            case CMSQueryLexer.UNION:
            case CMSQueryLexer.INTERSECTION:
                // post-order traversal 
                // 1. left child
                ParseQueryNode prevQueryNode = parseContext.getQueryNode();
                leftNode = parsePath(pathTree.getChild(0));                                
                // 2. right child
                parseContext.setQueryNode(prevQueryNode);
                rightNode = parsePath(pathTree.getChild(1));
                // 3. process set node
                currNode = new ParseLinkNode(lexType);
                currNode.addNextNode(leftNode);
                currNode.addNextNode(rightNode);
                break;
            case CMSQueryLexer.INNER_JOIN:         
            case CMSQueryLexer.LEFT_JOIN:
                // in-order traversal
                // 1. left child
                leftNode = parsePath(pathTree.getChild(0));
                // 2. process join node
                currNode = new ParseLinkNode(lexType);
                leftNode.addNextNode(currNode);
                // 3. right child
                rightNode = parsePath(pathTree.getChild(1));
                currNode.addNextNode(rightNode);
                break;
            default:
                throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, parseContext.getQueryString());
        }
        
        // return header node for linear path; return current node for other cases
        if (lexType == CMSQueryLexer.INNER_JOIN || lexType == CMSQueryLexer.LEFT_JOIN) {
            return leftNode;
        } else {
            return currNode;
        }
    }

    /**
     * traversal query node to build node level tree
     * 
     * @param queryNodeTree
     * @param prevQueryNode
     * @return
     */
    private ParseQueryNode parseQueryNode(Tree queryNodeTree) {
        ParseQueryNode queryNode = new ParseQueryNode();       
        Tree typeCast = null;
        for (int i = 0, count = queryNodeTree.getChildCount(); i < count; i++) {
            Tree child = queryNodeTree.getChild(i);
            switch (child.getType()) {
                case CMSQueryLexer.TYPE_LIST:
                    // type cast must be parsed after ID node
                    typeCast = child;
                    break;
                case CMSQueryLexer.ID:
                    parseNodeName(child, queryNode);
                    if (typeCast != null) {
                        parseTypeCast(typeCast, queryNode);
                    }
                    break;
                case CMSQueryLexer.REVERSE:
                    parseReverseName(child, queryNode);
                    break;
                case CMSQueryLexer.PROJECTION:
                    parseProjection(child, queryNode);                    
                    break;                
                case CMSQueryLexer.FILTER:
                    parseFilter(child, queryNode);
                    break;
                case CMSQueryLexer.AGGREGATION:
                    parseAggregation(child, queryNode);
                    break;
                default:
                    throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, parseContext.getQueryString());
            }
        }
        queryNode.setQuerySequence(parseContext.getQueryNodeCount());
        parseContext.addQueryNodeCount();
        return queryNode;
    }
    
    private void parseTypeCast(Tree typeListTree, ParseQueryNode queryNode) {
        for (int i = 0, count = typeListTree.getChildCount(); i < count; i++) {
            Tree child = typeListTree.getChild(i);
            String typeName = child.getText();
            // check inheritance in translation phase as node name will be parsed later
            MetaClass castType = findMetaClass(typeName, queryNode.getMetaClass().getRepository());
            queryNode.addTypeCast(castType);
        }
    }
    
    private void parseNodeName(Tree nodeNameTree, ParseQueryNode queryNode) {
        String nodeName = nodeNameTree.getText();
        ParseQueryNode prevQueryNode = parseContext.getQueryNode();
        if (prevQueryNode == null) {
            // anchor node
            // for 1st node in A.b.c, metaclass=A
            MetaClass metadata = findMetaClass(nodeName, null);
            queryNode.setMetaClass(metadata);
        } else {
            // reference node
            // for 2nd node in A.b.c, metaclass=B, metareference=A.b 
            MetaRelationship prevMetaRef = (MetaRelationship) findMetaField(prevQueryNode.getMetaClass(), nodeName);
			MetaClass metaClass = prevMetaRef.getRefMetaClass();            
            queryNode.setMetaReference(prevMetaRef);
            queryNode.setMetaClass(metaClass);
        }
    }
    
    private void parseReverseName(Tree nodeNameTree, ParseQueryNode queryNode) {
        // for the reverse node (b!C) in A.b.b!C.d
        // previous node: metaclass=B, metereference=A.b
        // current node: metaclass=C, metarelation=B.b!C
        String metaClassName = nodeNameTree.getChild(1).getText(); //C
        String referenceName = nodeNameTree.getChild(0).getText(); //b
        String repoName = null;
        if (parseContext.getQueryNode() != null) {
            // / inherit from parent query ndoe if any
            repoName = parseContext.getQueryNode().getMetaClass().getRepository();
        }
        MetaClass metaClass = findMetaClass(metaClassName, repoName); //C
        queryNode.setMetaClass(metaClass); //C
        MetaRelationship metaRef = (MetaRelationship) findMetaField(metaClass, referenceName); //C.b
        queryNode.setReverseMetaReference(metaRef); //C.b
    }
    
    private void parseProjection(Tree projectionTree, ParseQueryNode queryNode) {
        for (int i = 0, count = projectionTree.getChildCount(); i < count; i++) {
            Tree child = projectionTree.getChild(i);
            ISearchField searchField = null;
            switch (child.getType()) {
                case CMSQueryLexer.STAR:
                    searchField = ProjectionField.STAR;                    
                    break;
                case CMSQueryLexer.SRCH_ATTR:
                    searchField = parseSearchField(child, queryNode, false);
                    if (queryNode.getGroup() != null) {
                        searchField = new GroupField(searchField);
                    }
                    break;
                case CMSQueryLexer.AGGR_ATTR:
                    searchField = parseAggregationField(child, queryNode, false);
                    break;
                default:
                    throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, parseContext.getQueryString());
            }
            queryNode.addUserSelection(searchField);
            queryNode.markUserDisplay();
        }
    }
    
    private ISearchField parseSearchField(Tree fieldTree, ParseQueryNode queryNode, boolean isFilterField) {
        String fieldName = fieldTree.getChild(0).getText();
        MetaClass metaClass = queryNode.getMetaClass();
        MetaField rootField = findMetaField(metaClass, fieldName);
        String innerField = null;
        int count = fieldTree.getChildCount();
        if (count > 1) {            
            StringBuilder sb = new StringBuilder();       
            sb.append(fieldTree.getChild(1).getText());
            for (int i = 2; i < count; i++) {
                sb.append('.');
                sb.append(fieldTree.getChild(i).getText());                         
            }
            innerField = sb.toString();
        }

        if (isFilterField) {
            return new SelectionField(rootField, innerField, queryContext.getRegistration().searchStrategy);
        } else {
            return new ProjectionField(rootField, innerField, true, queryContext.getRegistration().searchStrategy);
        }
    }

    private ISearchField parseFilterField(Tree fieldTree, ParseQueryNode queryNode) {
        ISearchField field = null;
        if (fieldTree.getType() == CMSQueryLexer.SRCH_ATTR) {
            ISearchField searchField = parseSearchField(fieldTree, queryNode, true);
            field = searchField;
            if (queryNode.hasGroup()) {
                field = new GroupField(searchField);
            }
        } else if (fieldTree.getType() == CMSQueryLexer.AGGR_ATTR) {
            field = parseAggregationField(fieldTree, queryNode, true);
        }     
        return field;
    }
    
    private AggregationField parseAggregationField(Tree fieldTree, ParseQueryNode queryNode, boolean isFilterField) {
        if (queryNode.getGroup() == null) {
            throw new QueryParseException(QueryErrCodeEnum.AGG_WITHOUT_GROUP, "Aggregation " + fieldTree.toStringTree() + " without group");
        }
        
        Tree funcTree = fieldTree.getChild(0);
        Tree searchFieldTree = fieldTree.getChild(1);
        
        AggFuncEnum aggFunc = AggrFuncFactory.getAggrFunc(funcTree.getType());
        if (aggFunc == null) {
            throw new QueryParseException(QueryErrCodeEnum.AGG_FUNC_NOT_FOUND, "Can't find aggregation function " + funcTree.getText());
        }

        AggregationField aggField = null;
        // count should not have search field given
        if (searchFieldTree == null) {
            aggField = new AggregationField(AggFuncEnum.COUNT, null);
        } else {
            ISearchField searchField = parseSearchField(searchFieldTree, queryNode, isFilterField);
            aggField = new AggregationField(aggFunc, searchField);
        }
        queryNode.getGroup().addAggregationField(aggField);
        return aggField;
    }
    
    private void parseFilter(Tree filterTree, ParseQueryNode queryNode) {
        SearchCriteria criteria = parseCriteria(filterTree.getChild(0), queryNode, false);
        if (queryNode.hasGroup()) {
            queryNode.setGroupCriteria(criteria);
        } else {
            queryNode.setCriteria(criteria);
        }
    }
    
    private  SearchCriteria parseCriteria(Tree criteriaTree, ParseQueryNode query, boolean negative) {
        SearchCriteria criteria = null;
        int type = criteriaTree.getType();
        switch (type) {
            case CMSQueryLexer.NOT:
                criteria = parseCriteria(criteriaTree.getChild(0), query, true);
                break;
            case CMSQueryLexer.AND:
            case CMSQueryLexer.OR:
                criteria = parseLogicCriteria(criteriaTree, query, negative);
                break;
            default:
                criteria = parseFieldCriteria(criteriaTree, query, negative);
                break;
        }
        return criteria;
    }    
    
    private LogicalSearchCriteria parseLogicCriteria(Tree filterTree, ParseQueryNode queryNode, boolean negative) {
        LogicalSearchCriteria.LogicOperatorEnum logicOp = null;
        if (negative) {
            logicOp = QueryOperatorFactory.getLogicNotOperator(filterTree.getType());
        } else {
            logicOp = QueryOperatorFactory.getLogicOperator(filterTree.getType());
        }
        LogicalSearchCriteria criteria = new LogicalSearchCriteria(logicOp); 
        
        for (int i = 0, count = filterTree.getChildCount(); i < count; i++) {
            Tree child = filterTree.getChild(i);
            SearchCriteria subCriteria = parseCriteria(child, queryNode, negative);
            criteria.addChild(subCriteria);
        }
        return criteria;
    }
    
    private FieldSearchCriteria parseFieldCriteria(Tree filterTree, ParseQueryNode query, boolean negative) {                      
        ISearchField searchField = parseFilterField(filterTree.getChild(0), query);
        FieldSearchCriteria.FieldOperatorEnum fieldOp = null;
        int filterType = filterTree.getType();
        if (negative) {
            fieldOp = QueryOperatorFactory.getFieldNotOperator(filterType);
        } else {
            fieldOp = QueryOperatorFactory.getFieldOperator(filterType);
        }
        
        FieldSearchCriteria criteria = null;
        Object value = null;
        switch (fieldOp.getScalar()) {
            case 0:
                // like exists @field
                criteria = new FieldSearchCriteria(searchField, fieldOp);
                break;
            case 1:
                // like @field = ""
                Tree valueNode = filterTree.getChild(1);
                value = parseValue(valueNode, query);
                criteria = new FieldSearchCriteria(searchField, fieldOp, value);
                break;
            case 2: 
                if (filterType == CMSQueryLexer.SUBQUERY) {
                    // like @field =& A{@field}
                    Tree subQueryTree = filterTree.getChild(1);
                    QueryParser parser = new QueryParser(queryContext);
                    ParseBaseNode subQueryNode = parser.parseQuery(subQueryTree, subQueryTree.getText());
                    query.addSubQuery(subQueryNode);
                    // for sub query, put a place holder for the value list
                    criteria = new FieldSearchCriteria(searchField, fieldOp, new LinkedList<Object>());
                    subQueryNode.setSubQueryCriteria(criteria);
                } else {
                    // like @field in ("", "")
                    Tree valuesTree = filterTree.getChild(1);
                    List<Object> values = new ArrayList<Object>(valuesTree.getChildCount());
                    for (int i = 0, count = valuesTree.getChildCount(); i < count; i++) {
                        Tree child = valuesTree.getChild(i);
                        value = parseValue(child, query);
                        values.add(value);
                    }
                    criteria = new FieldSearchCriteria(searchField, fieldOp, values);
                }
                break;
            default:
                break;
        }
        return criteria;
    }       
    
    private Object parseValue(Tree valTree, ParseQueryNode query) {
        String valueText = valTree.getChild(0).getText();
        Object value = null;
        switch (valTree.getType()) {
            case CMSQueryLexer.STR_VAL:
                // strip double quote from string
                value = valueText.substring(1, valueText.length() - 1);
                break;
            case CMSQueryLexer.INT_VAL:
                value = Long.valueOf(valueText);
                break;
            case CMSQueryLexer.DEC_VAL:
                value = Double.valueOf(valueText);
                break;
            case CMSQueryLexer.BOOL_VAL:
                value = Boolean.valueOf(valueText);
                break;
            case CMSQueryLexer.DATE_VAL:
                long longVal = Long.parseLong(valueText);
                value = new Date(longVal);
                break;
            case CMSQueryLexer.REG_VAL:
                RegexValue regVal = new RegexValue();
                String regString = valTree.getChild(0).getChild(0).getText();
                regVal.value = regString.substring(1, regString.length() - 1);
                boolean caseSen = true;
                if (valTree.getChild(1) == null) {
                    caseSen = queryContext.getCaseSensitiveDefault(query.getMetaClass().getRepository());
                } else {
                    caseSen = (valTree.getChild(1).getType() == CMSQueryLexer.REG_SENSITIVE);
                }
                regVal.caseSensitive = caseSen;
                value = regVal;
                break;
            default:
                throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, "value - " + valueText + " - in query: " + parseContext.getQueryString());
        }
        return value;
    }
    
    private void parseAggregation(Tree aggTree, ParseQueryNode queryNode) {    
        for (int i = 0, count = aggTree.getChildCount(); i < count; i++) {
            Tree child = aggTree.getChild(i);
            switch (child.getType()) {
                case CMSQueryLexer.GROUP:
                    parseGroup(child, queryNode);
                    break;
                case CMSQueryLexer.FILTER:
                    parseFilter(child, queryNode);
                    break;
                default:
                    throw new QueryParseException(QueryErrCodeEnum.PARSE_GRAMMER_ERROR, parseContext.getQueryString());
            }
        }
        parseContext.addAggregation();
    }
    
    private void parseGroup(Tree groupTree, ParseQueryNode queryNode) {
        SearchGroup group = new SearchGroup();
        for (int i = 0, count = groupTree.getChildCount(); i < count; i++) {
            Tree child = groupTree.getChild(i);
            if (child.getType() == CMSQueryLexer.SRCH_ATTR) {
                ISearchField field = parseSearchField(child, queryNode, true);
                group.addGroupField(new GroupField(field));
            } else if (child.getType() == CMSQueryLexer.AGGR_ATTR) {
                throw new QueryParseException(QueryErrCodeEnum.AGG_FIELD_IN_GROUP, "Group can't contains aggregation field" + child.toStringTree());
            }            
        }
        queryNode.setGroup(group);
    }

    private MetaClass findMetaClass(String metaName, String repoName) {
        String repo = repoName;
        if (repo == null) {
            repo = queryContext.getRepositoryName();
        }
        IMetadataService metaService = queryContext.getMetadataService(repo);
        MetaClass metadata = metaService.getMetaClass(metaName);
        if (metadata == null) {
            throw new QueryParseException(QueryErrCodeEnum.METACLASS_NOT_FOUND, "Can't find class " + metaName);
        }
        return metadata;
    }
    
    private MetaField findMetaField(MetaClass metadata, String fieldName) {
        MetaField metaField = metadata.getFieldByName(fieldName);
        if (metaField == null) {
            throw new QueryParseException(QueryErrCodeEnum.METAFIELD_NOT_FOUND, "Can't find field {" + fieldName + "} in metadata " + metadata.getName());
        }
        return metaField;
    }

}
