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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryLexer;

/**
 * 
 * @author xjiang
 *
 */
public abstract class ParseBaseNode {
    
    public enum ParseNodeTypeEnum {
        QUERY, UNION, INTERSECTION, INNERJOIN, LEFTJOIN
    }
    
    private ParseBaseNode prevNode;
    
    private List<ParseBaseNode> nextNodes;
    private int querySequenceLower;
    private int querySequenceUpper;
    
    // the field criteria that current node's result will be used for.
    private FieldSearchCriteria subQueryCriteria;
    
    private final ParseNodeTypeEnum type;
    
    public ParseBaseNode(int lexerType) {
        switch (lexerType) {
            case CMSQueryLexer.QUERY_NODE:
                this.type = ParseNodeTypeEnum.QUERY;
                break;
            case CMSQueryLexer.UNION:
                this.type = ParseNodeTypeEnum.UNION;
                break;
            case CMSQueryLexer.INTERSECTION:
                this.type = ParseNodeTypeEnum.INTERSECTION;
                break;
            case CMSQueryLexer.LEFT_JOIN:
                this.type = ParseNodeTypeEnum.LEFTJOIN;
                break;
            case CMSQueryLexer.INNER_JOIN:
                this.type = ParseNodeTypeEnum.INNERJOIN;
                break;
            default:
                throw new IllegalArgumentException("Error type: " + lexerType);           
        }
        this.nextNodes = new LinkedList<ParseBaseNode>();
    }
    
    public ParseBaseNode getPrevNode() {
        return prevNode;
    }
    
    public List<ParseBaseNode> getNextNodes() {
        return nextNodes;
    }

    public void addNextNode(ParseBaseNode node) {
        if (node == null)
            return;
        nextNodes.add(node);
        node.prevNode = this;
    }
    public boolean hasNextNodes() {
        return !nextNodes.isEmpty();
    }

    public void setSubQueryCriteria(FieldSearchCriteria criteria) {
        this.subQueryCriteria = criteria;
    }

    public FieldSearchCriteria getSubQueryCriteria() {
        return subQueryCriteria;
    }
    
    public ParseNodeTypeEnum getType() {
        return type;
    }
    
    /**
     * template method for parse validation
     * 
     * @param context
     */
    public void validate(ParseContext context) {
        ParseQueryNode lastNode = context.getQueryNode();
        doValidate(context);
        for (ParseBaseNode nextNode : nextNodes) {
            nextNode.validate(context);
        }
        // restore the last query node
        context.setQueryNode(lastNode);
    }
    
    protected abstract void doValidate(ParseContext context);

    public int getQuerySequenceLowerBound() {
        return querySequenceLower;
    }
    public int getQuerySequenceUpperBound() {
        return querySequenceUpper;
    }
    public void setQuerySequenceLowerBound(int lower) {
        this.querySequenceLower = lower;
    }
    public void setQuerySequenceUpperBound(int upper) {
        this.querySequenceUpper = upper;
    }
    
    public abstract Set<String>  checkSetMetaClasses();
  
}
