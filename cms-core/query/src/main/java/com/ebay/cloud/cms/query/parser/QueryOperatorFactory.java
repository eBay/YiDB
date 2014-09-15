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

import java.util.HashMap;
import java.util.Map;

import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.query.parser.antlr.CMSQueryLexer;

/**
 * Factory class to translate anltr operator to search operator 
 * 
 * @author xjiang
 *
 */
public class QueryOperatorFactory {
    
    public enum SetOperationEnum {
        UNION, INTERSECTION
    }
    
    private final static Map<Integer, FieldOperatorEnum> FIELD_OP_MAP;
    private final static Map<Integer, FieldOperatorEnum> FIELD_NOT_OP_MAP;
    private final static Map<Integer, LogicOperatorEnum> LOGIC_OP_MAP;
    private final static Map<Integer, LogicOperatorEnum> LOGIC_NOT_OP_MAP;
    
    private final static Map<Integer, SetOperationEnum>  SET_OP_MAP;
        
    static {
        FIELD_OP_MAP = new HashMap<Integer, FieldOperatorEnum>();
        FIELD_OP_MAP.put(CMSQueryLexer.EQ, FieldOperatorEnum.EQ);
        FIELD_OP_MAP.put(CMSQueryLexer.NE, FieldOperatorEnum.NE);
        FIELD_OP_MAP.put(CMSQueryLexer.GT, FieldOperatorEnum.GT);
        FIELD_OP_MAP.put(CMSQueryLexer.GE, FieldOperatorEnum.GE);
        FIELD_OP_MAP.put(CMSQueryLexer.LT, FieldOperatorEnum.LT);
        FIELD_OP_MAP.put(CMSQueryLexer.LE, FieldOperatorEnum.LE);
        FIELD_OP_MAP.put(CMSQueryLexer.REGEX, FieldOperatorEnum.REGEX);
        FIELD_OP_MAP.put(CMSQueryLexer.IN, FieldOperatorEnum.IN);
        FIELD_OP_MAP.put(CMSQueryLexer.SUBQUERY, FieldOperatorEnum.IN);
        FIELD_OP_MAP.put(CMSQueryLexer.EXISTS, FieldOperatorEnum.CONTAINS);
        FIELD_OP_MAP.put(CMSQueryLexer.ISEMPTY, FieldOperatorEnum.ISEMPTY);
        FIELD_OP_MAP.put(CMSQueryLexer.ISNULL, FieldOperatorEnum.ISNULL);
        
        FIELD_NOT_OP_MAP = new HashMap<Integer, FieldOperatorEnum>();
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.EQ, FieldOperatorEnum.NE);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.NE, FieldOperatorEnum.EQ);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.GT, FieldOperatorEnum.LE);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.GE, FieldOperatorEnum.LT);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.LT, FieldOperatorEnum.GE);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.LE, FieldOperatorEnum.GT);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.REGEX, FieldOperatorEnum.NREGEX);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.IN, FieldOperatorEnum.NIN);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.SUBQUERY, FieldOperatorEnum.NIN);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.EXISTS, FieldOperatorEnum.NCONTAINS);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.ISEMPTY, FieldOperatorEnum.NISEMPTY);
        FIELD_NOT_OP_MAP.put(CMSQueryLexer.ISNULL, FieldOperatorEnum.NISNULL);
        
        LOGIC_OP_MAP = new HashMap<Integer, LogicOperatorEnum>();
        LOGIC_OP_MAP.put(CMSQueryLexer.AND, LogicOperatorEnum.AND);
        LOGIC_OP_MAP.put(CMSQueryLexer.OR, LogicOperatorEnum.OR);
        
        LOGIC_NOT_OP_MAP = new HashMap<Integer, LogicOperatorEnum>();
        LOGIC_NOT_OP_MAP.put(CMSQueryLexer.AND, LogicOperatorEnum.OR);
        LOGIC_NOT_OP_MAP.put(CMSQueryLexer.OR, LogicOperatorEnum.AND);
        
        SET_OP_MAP = new HashMap<Integer, SetOperationEnum>();
        SET_OP_MAP.put(CMSQueryLexer.INTERSECTION, SetOperationEnum.INTERSECTION);
        SET_OP_MAP.put(CMSQueryLexer.UNION, SetOperationEnum.UNION);
    }
    
    public static FieldOperatorEnum getFieldOperator(int op) {
        return FIELD_OP_MAP.get(op);
    }
    
    public static FieldOperatorEnum getFieldNotOperator(int op) {
        return FIELD_NOT_OP_MAP.get(op);
    }
    
    public static LogicOperatorEnum getLogicOperator(int op) {
        return LOGIC_OP_MAP.get(op);
    }
    
    public static LogicOperatorEnum getLogicNotOperator(int op) {
        return LOGIC_NOT_OP_MAP.get(op);
    }

    public static SetOperationEnum getSetOperator(int op) {
        return SET_OP_MAP.get(op);
    }
}
