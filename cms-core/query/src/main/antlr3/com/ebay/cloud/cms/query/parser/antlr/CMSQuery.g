/**
* Grammer file for CMS Query Language
*
*    @author xjiang
*
**/


grammar CMSQuery;

options { 
    output = AST; 
    language = Java;
}

tokens {
    QUERY;
    QUERY_NODE;
    AGGREGATION;
    GROUP;
    FILTER;
    PROJECTION;
    TYPE_LIST;
    SRCH_ATTR;
    AGGR_ATTR;
    VALUE_LIST;
    INT_VAL;
    DEC_VAL;
    STR_VAL;
    BOOL_VAL;
    REG_VAL;
    DATE_VAL;
    INNER_JOIN  =   '.';
    LEFT_JOIN   =   '+.';
    RIGHT_JOIN  =   '.+';
    REVERSE     =   '!';
    DOLLAR      =   '$';
    AT          =   '@';
    EQ          =   '=';
    NE          =   '!=';
    GT          =   '>';
    GE          =   '>=';
    LT          =   '<';
    LE          =   '<=';
    REGEX       =   '=~';
    SUBQUERY    =   '=&';
    EXISTS      =   'exists';
    LBRACKET    =   '[';
    RBRACKET    =   ']';
    LBRACE      =   '{';  
    RBRACE      =   '}';
    LPARENT     =   '(';
    RPARENT     =   ')';
    UNION       =   '||';
    INTERSECTION=   '&&';
    REG_INSENSITIVE  = '/i';
    REG_SENSITIVE  = '/s';
    STAR        =   '*';
    COMMA       =   ',';
    AND         =   'and';
    OR          =   'or';
    NOT         =   'not';
    IN          =   'in';
    DATE        =   'date';
    MIN         =   'min';
    MAX         =   'max';
    AVG         =   'avg';
    SUM         =   'sum';
    COUNT       =   'count';
    ISNULL      =   'isnull';
    ISEMPTY     =   'isempty';
}

@header {
package com.ebay.cloud.cms.query.parser.antlr;
}

@lexer::header {
package com.ebay.cloud.cms.query.parser.antlr;
}

@rulecatch {
    catch (RecognitionException e)
    {
        throw e;
    }
}

// Parser Rules
start_rule: query;  

query 
    :   path -> ^(QUERY path)
    ;

//// path
path  
    :   unionPath
    ;

unionPath 
    :   intersectionPath (UNION^ intersectionPath)*
    ;

intersectionPath 
    :   primaryPath (INTERSECTION^ primaryPath)*
    ;
    
primaryPath
    :   LPARENT! path RPARENT!
    |   queryNode (options{greedy=true;}: (INNER_JOIN|LEFT_JOIN|RIGHT_JOIN)^ primaryPath)*
    ;
    
//// query node
queryNode
    :   (typeCast)? nodeName (filter)? (aggregation)? (projection)? -> ^(QUERY_NODE typeCast? nodeName filter? aggregation? projection?)
    ;

nodeName    
    :   ID (REVERSE^ ID)?
    ;

typeCast
    :    LT typeName (COMMA typeName)* GT -> ^(TYPE_LIST typeName (typeName)*)
    ;
    
typeName
    :    ID
    ;
    
aggregation
    :    group filter? -> ^(AGGREGATION group filter?)
    ;

group    
    :    LT attributeList GT -> ^(GROUP attributeList)
    ;

filter    
    :    LBRACKET expression RBRACKET -> ^(FILTER expression)
    ;
    
projection    
    :    LBRACE (STAR | attributeList) RBRACE  -> ^(PROJECTION attributeList? STAR?)
    ;

//// attribute
attributeList
    :    attribute (COMMA! attribute)*
    ;

attribute    
    :    searchAttr | aggregateAttr
    ;
    
searchAttr
    :    AT attrName (INNER_JOIN DOLLAR attrName)* -> ^(SRCH_ATTR attrName (attrName)*)
    ;
        
aggregateAttr 
    :    DOLLAR aggFunc LPARENT searchAttr? RPARENT -> ^(AGGR_ATTR aggFunc searchAttr?)
    ;
    
attrName
    :    ID
    ;

aggFunc
    :   MIN | MAX | AVG | SUM | COUNT
    ;

//// expression
expression
    :    orExpr
    ;

orExpr    
    :    andExpr (OR^ andExpr)*
    ;

andExpr    
    :    notExpr (AND^ notExpr)*
    ;
    
notExpr
    :    (NOT^)? primaryExpr
    ;

primaryExpr
    :    LPARENT! expression RPARENT!
    |    compExpr
    |    inExpr
    |    regexExpr
    |    existExpr 
    |    subQueryExpr
    |    isnullExpr
    |    isemptyExpr
    ;
    
compExpr
    :    attribute (EQ | NE | GT | LT | GE | LE)^ value
    ;

inExpr    
    :    attribute IN^ valueList
    ;

regexExpr
    :    attribute REGEX^ expValue
    ;
    
subQueryExpr
    :    attribute SUBQUERY^ query
    ;
    
existExpr
    :    EXISTS^ attribute
    ;
    
isnullExpr
    :    ISNULL^ attribute
    ;
    
isemptyExpr
    :    ISEMPTY^ attribute
    ;

valueList
    :    LPARENT value (COMMA value)* RPARENT -> ^(VALUE_LIST value (value)*)
    ;

value    
    :    intVal 
    |    doubleVal
    |    strVal
    |    boolVal
    |    dateVal
    ;
    
intVal    
    :    INTEGER -> ^(INT_VAL INTEGER)
    ;

doubleVal   
    :    DECIMAL -> ^(DEC_VAL DECIMAL)
    ;
    
expValue
    : strVal  (REG_SENSITIVE | REG_INSENSITIVE)? -> ^(REG_VAL strVal REG_SENSITIVE? REG_INSENSITIVE? )
    ;

strVal    
    :    STRING -> ^(STR_VAL STRING)
    ;

boolVal 
    :    BOOLEAN -> ^(BOOL_VAL BOOLEAN)
    ;
    
dateVal 
    :    DATE LPARENT INTEGER RPARENT -> ^(DATE_VAL INTEGER)
    ;

// Lexer Rules
BOOLEAN 
    :    ('true' | 'false')
    ;
    
INTEGER 
    :  '-'?('0'..'9')+
    ;

    
DECIMAL    
    :    '-'? (('.' ('0'..'9')+)  | (('0'..'9')+ '.' '0'..'9'*))
    ;

STRING    
    :    '"' (options {greedy=false;}: ESC | .)* '"'
    ;

ID    
    :    ('a'..'z'|'A'..'Z'|'0'..'9'|'_')+
    ;
    
WS    
    :    (' ' | '\t' | '\r' | '\n')+ { $channel=HIDDEN; }
    ;  
    
ESC    
    :    '\\' ('"'|'\''|'\\')
    ;