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


// $ANTLR 3.4 /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g 2013-11-08 10:03:38

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

package com.ebay.cloud.cms.query.parser.antlr;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked"})
public class CMSQueryLexer extends Lexer {
    public static final int EOF=-1;
    public static final int AGGREGATION=4;
    public static final int AGGR_ATTR=5;
    public static final int AND=6;
    public static final int AT=7;
    public static final int AVG=8;
    public static final int BOOLEAN=9;
    public static final int BOOL_VAL=10;
    public static final int COMMA=11;
    public static final int COUNT=12;
    public static final int DATE=13;
    public static final int DATE_VAL=14;
    public static final int DECIMAL=15;
    public static final int DEC_VAL=16;
    public static final int DOLLAR=17;
    public static final int EQ=18;
    public static final int ESC=19;
    public static final int EXISTS=20;
    public static final int FILTER=21;
    public static final int GE=22;
    public static final int GROUP=23;
    public static final int GT=24;
    public static final int ID=25;
    public static final int IN=26;
    public static final int INNER_JOIN=27;
    public static final int INTEGER=28;
    public static final int INTERSECTION=29;
    public static final int INT_VAL=30;
    public static final int ISEMPTY=31;
    public static final int ISNULL=32;
    public static final int LBRACE=33;
    public static final int LBRACKET=34;
    public static final int LE=35;
    public static final int LEFT_JOIN=36;
    public static final int LPARENT=37;
    public static final int LT=38;
    public static final int MAX=39;
    public static final int MIN=40;
    public static final int NE=41;
    public static final int NOT=42;
    public static final int OR=43;
    public static final int PROJECTION=44;
    public static final int QUERY=45;
    public static final int QUERY_NODE=46;
    public static final int RBRACE=47;
    public static final int RBRACKET=48;
    public static final int REGEX=49;
    public static final int REG_INSENSITIVE=50;
    public static final int REG_SENSITIVE=51;
    public static final int REG_VAL=52;
    public static final int REVERSE=53;
    public static final int RIGHT_JOIN=54;
    public static final int RPARENT=55;
    public static final int SRCH_ATTR=56;
    public static final int STAR=57;
    public static final int STRING=58;
    public static final int STR_VAL=59;
    public static final int SUBQUERY=60;
    public static final int SUM=61;
    public static final int TYPE_LIST=62;
    public static final int UNION=63;
    public static final int VALUE_LIST=64;
    public static final int WS=65;

    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public CMSQueryLexer() {} 
    public CMSQueryLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public CMSQueryLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }
    public String getGrammarFileName() { return "/ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g"; }

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:11:5: ( 'and' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:11:7: 'and'
            {
            match("and"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "AT"
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:12:4: ( '@' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:12:6: '@'
            {
            match('@'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AT"

    // $ANTLR start "AVG"
    public final void mAVG() throws RecognitionException {
        try {
            int _type = AVG;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:13:5: ( 'avg' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:13:7: 'avg'
            {
            match("avg"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "AVG"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:14:7: ( ',' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:14:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "COUNT"
    public final void mCOUNT() throws RecognitionException {
        try {
            int _type = COUNT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:15:7: ( 'count' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:15:9: 'count'
            {
            match("count"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "COUNT"

    // $ANTLR start "DATE"
    public final void mDATE() throws RecognitionException {
        try {
            int _type = DATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:16:6: ( 'date' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:16:8: 'date'
            {
            match("date"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DATE"

    // $ANTLR start "DOLLAR"
    public final void mDOLLAR() throws RecognitionException {
        try {
            int _type = DOLLAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:17:8: ( '$' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:17:10: '$'
            {
            match('$'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DOLLAR"

    // $ANTLR start "EQ"
    public final void mEQ() throws RecognitionException {
        try {
            int _type = EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:18:4: ( '=' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:18:6: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EQ"

    // $ANTLR start "EXISTS"
    public final void mEXISTS() throws RecognitionException {
        try {
            int _type = EXISTS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:19:8: ( 'exists' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:19:10: 'exists'
            {
            match("exists"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "EXISTS"

    // $ANTLR start "GE"
    public final void mGE() throws RecognitionException {
        try {
            int _type = GE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:20:4: ( '>=' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:20:6: '>='
            {
            match(">="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GE"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:21:4: ( '>' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:21:6: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "IN"
    public final void mIN() throws RecognitionException {
        try {
            int _type = IN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:22:4: ( 'in' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:22:6: 'in'
            {
            match("in"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "IN"

    // $ANTLR start "INNER_JOIN"
    public final void mINNER_JOIN() throws RecognitionException {
        try {
            int _type = INNER_JOIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:23:12: ( '.' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:23:14: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INNER_JOIN"

    // $ANTLR start "INTERSECTION"
    public final void mINTERSECTION() throws RecognitionException {
        try {
            int _type = INTERSECTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:24:14: ( '&&' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:24:16: '&&'
            {
            match("&&"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INTERSECTION"

    // $ANTLR start "ISEMPTY"
    public final void mISEMPTY() throws RecognitionException {
        try {
            int _type = ISEMPTY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:25:9: ( 'isempty' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:25:11: 'isempty'
            {
            match("isempty"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ISEMPTY"

    // $ANTLR start "ISNULL"
    public final void mISNULL() throws RecognitionException {
        try {
            int _type = ISNULL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:26:8: ( 'isnull' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:26:10: 'isnull'
            {
            match("isnull"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ISNULL"

    // $ANTLR start "LBRACE"
    public final void mLBRACE() throws RecognitionException {
        try {
            int _type = LBRACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:27:8: ( '{' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:27:10: '{'
            {
            match('{'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LBRACE"

    // $ANTLR start "LBRACKET"
    public final void mLBRACKET() throws RecognitionException {
        try {
            int _type = LBRACKET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:28:10: ( '[' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:28:12: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LBRACKET"

    // $ANTLR start "LE"
    public final void mLE() throws RecognitionException {
        try {
            int _type = LE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:29:4: ( '<=' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:29:6: '<='
            {
            match("<="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LE"

    // $ANTLR start "LEFT_JOIN"
    public final void mLEFT_JOIN() throws RecognitionException {
        try {
            int _type = LEFT_JOIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:30:11: ( '+.' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:30:13: '+.'
            {
            match("+."); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_JOIN"

    // $ANTLR start "LPARENT"
    public final void mLPARENT() throws RecognitionException {
        try {
            int _type = LPARENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:31:9: ( '(' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:31:11: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LPARENT"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:32:4: ( '<' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:32:6: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "MAX"
    public final void mMAX() throws RecognitionException {
        try {
            int _type = MAX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:33:5: ( 'max' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:33:7: 'max'
            {
            match("max"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MAX"

    // $ANTLR start "MIN"
    public final void mMIN() throws RecognitionException {
        try {
            int _type = MIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:34:5: ( 'min' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:34:7: 'min'
            {
            match("min"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "MIN"

    // $ANTLR start "NE"
    public final void mNE() throws RecognitionException {
        try {
            int _type = NE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:35:4: ( '!=' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:35:6: '!='
            {
            match("!="); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NE"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:36:5: ( 'not' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:36:7: 'not'
            {
            match("not"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:37:4: ( 'or' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:37:6: 'or'
            {
            match("or"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "RBRACE"
    public final void mRBRACE() throws RecognitionException {
        try {
            int _type = RBRACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:38:8: ( '}' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:38:10: '}'
            {
            match('}'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RBRACE"

    // $ANTLR start "RBRACKET"
    public final void mRBRACKET() throws RecognitionException {
        try {
            int _type = RBRACKET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:39:10: ( ']' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:39:12: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RBRACKET"

    // $ANTLR start "REGEX"
    public final void mREGEX() throws RecognitionException {
        try {
            int _type = REGEX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:40:7: ( '=~' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:40:9: '=~'
            {
            match("=~"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "REGEX"

    // $ANTLR start "REG_INSENSITIVE"
    public final void mREG_INSENSITIVE() throws RecognitionException {
        try {
            int _type = REG_INSENSITIVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:41:17: ( '/i' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:41:19: '/i'
            {
            match("/i"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "REG_INSENSITIVE"

    // $ANTLR start "REG_SENSITIVE"
    public final void mREG_SENSITIVE() throws RecognitionException {
        try {
            int _type = REG_SENSITIVE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:42:15: ( '/s' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:42:17: '/s'
            {
            match("/s"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "REG_SENSITIVE"

    // $ANTLR start "REVERSE"
    public final void mREVERSE() throws RecognitionException {
        try {
            int _type = REVERSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:43:9: ( '!' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:43:11: '!'
            {
            match('!'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "REVERSE"

    // $ANTLR start "RIGHT_JOIN"
    public final void mRIGHT_JOIN() throws RecognitionException {
        try {
            int _type = RIGHT_JOIN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:44:12: ( '.+' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:44:14: '.+'
            {
            match(".+"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_JOIN"

    // $ANTLR start "RPARENT"
    public final void mRPARENT() throws RecognitionException {
        try {
            int _type = RPARENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:45:9: ( ')' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:45:11: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "RPARENT"

    // $ANTLR start "STAR"
    public final void mSTAR() throws RecognitionException {
        try {
            int _type = STAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:46:6: ( '*' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:46:8: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STAR"

    // $ANTLR start "SUBQUERY"
    public final void mSUBQUERY() throws RecognitionException {
        try {
            int _type = SUBQUERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:47:10: ( '=&' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:47:12: '=&'
            {
            match("=&"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SUBQUERY"

    // $ANTLR start "SUM"
    public final void mSUM() throws RecognitionException {
        try {
            int _type = SUM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:48:5: ( 'sum' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:48:7: 'sum'
            {
            match("sum"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "SUM"

    // $ANTLR start "UNION"
    public final void mUNION() throws RecognitionException {
        try {
            int _type = UNION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:49:7: ( '||' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:49:9: '||'
            {
            match("||"); 



            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "UNION"

    // $ANTLR start "BOOLEAN"
    public final void mBOOLEAN() throws RecognitionException {
        try {
            int _type = BOOLEAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:266:5: ( ( 'true' | 'false' ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:266:10: ( 'true' | 'false' )
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:266:10: ( 'true' | 'false' )
            int alt1=2;
            int LA1_0 = input.LA(1);

            if ( (LA1_0=='t') ) {
                alt1=1;
            }
            else if ( (LA1_0=='f') ) {
                alt1=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;

            }
            switch (alt1) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:266:11: 'true'
                    {
                    match("true"); 



                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:266:20: 'false'
                    {
                    match("false"); 



                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "BOOLEAN"

    // $ANTLR start "INTEGER"
    public final void mINTEGER() throws RecognitionException {
        try {
            int _type = INTEGER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:270:5: ( ( '-' )? ( '0' .. '9' )+ )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:270:8: ( '-' )? ( '0' .. '9' )+
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:270:8: ( '-' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='-') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:270:8: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:270:12: ( '0' .. '9' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0 >= '0' && LA3_0 <= '9')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "INTEGER"

    // $ANTLR start "DECIMAL"
    public final void mDECIMAL() throws RecognitionException {
        try {
            int _type = DECIMAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:5: ( ( '-' )? ( ( '.' ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ) ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:10: ( '-' )? ( ( '.' ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ) )
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:10: ( '-' )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='-') ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:10: '-'
                    {
                    match('-'); 

                    }
                    break;

            }


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:15: ( ( '.' ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* ) )
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0=='.') ) {
                alt8=1;
            }
            else if ( ((LA8_0 >= '0' && LA8_0 <= '9')) ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;

            }
            switch (alt8) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:16: ( '.' ( '0' .. '9' )+ )
                    {
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:16: ( '.' ( '0' .. '9' )+ )
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:17: '.' ( '0' .. '9' )+
                    {
                    match('.'); 

                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:21: ( '0' .. '9' )+
                    int cnt5=0;
                    loop5:
                    do {
                        int alt5=2;
                        int LA5_0 = input.LA(1);

                        if ( ((LA5_0 >= '0' && LA5_0 <= '9')) ) {
                            alt5=1;
                        }


                        switch (alt5) {
                    	case 1 :
                    	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt5 >= 1 ) break loop5;
                                EarlyExitException eee =
                                    new EarlyExitException(5, input);
                                throw eee;
                        }
                        cnt5++;
                    } while (true);


                    }


                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:37: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* )
                    {
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:37: ( ( '0' .. '9' )+ '.' ( '0' .. '9' )* )
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:38: ( '0' .. '9' )+ '.' ( '0' .. '9' )*
                    {
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:38: ( '0' .. '9' )+
                    int cnt6=0;
                    loop6:
                    do {
                        int alt6=2;
                        int LA6_0 = input.LA(1);

                        if ( ((LA6_0 >= '0' && LA6_0 <= '9')) ) {
                            alt6=1;
                        }


                        switch (alt6) {
                    	case 1 :
                    	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt6 >= 1 ) break loop6;
                                EarlyExitException eee =
                                    new EarlyExitException(6, input);
                                throw eee;
                        }
                        cnt6++;
                    } while (true);


                    match('.'); 

                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:275:54: ( '0' .. '9' )*
                    loop7:
                    do {
                        int alt7=2;
                        int LA7_0 = input.LA(1);

                        if ( ((LA7_0 >= '0' && LA7_0 <= '9')) ) {
                            alt7=1;
                        }


                        switch (alt7) {
                    	case 1 :
                    	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
                    	    {
                    	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9') ) {
                    	        input.consume();
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;
                    	    }


                    	    }
                    	    break;

                    	default :
                    	    break loop7;
                        }
                    } while (true);


                    }


                    }
                    break;

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "DECIMAL"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:279:5: ( '\"' ( options {greedy=false; } : ESC | . )* '\"' )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:279:10: '\"' ( options {greedy=false; } : ESC | . )* '\"'
            {
            match('\"'); 

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:279:14: ( options {greedy=false; } : ESC | . )*
            loop9:
            do {
                int alt9=3;
                int LA9_0 = input.LA(1);

                if ( (LA9_0=='\"') ) {
                    alt9=3;
                }
                else if ( (LA9_0=='\\') ) {
                    int LA9_2 = input.LA(2);

                    if ( (LA9_2=='\"') ) {
                        alt9=1;
                    }
                    else if ( (LA9_2=='\\') ) {
                        alt9=1;
                    }
                    else if ( (LA9_2=='\'') ) {
                        alt9=1;
                    }
                    else if ( ((LA9_2 >= '\u0000' && LA9_2 <= '!')||(LA9_2 >= '#' && LA9_2 <= '&')||(LA9_2 >= '(' && LA9_2 <= '[')||(LA9_2 >= ']' && LA9_2 <= '\uFFFF')) ) {
                        alt9=2;
                    }


                }
                else if ( ((LA9_0 >= '\u0000' && LA9_0 <= '!')||(LA9_0 >= '#' && LA9_0 <= '[')||(LA9_0 >= ']' && LA9_0 <= '\uFFFF')) ) {
                    alt9=2;
                }


                switch (alt9) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:279:40: ESC
            	    {
            	    mESC(); 


            	    }
            	    break;
            	case 2 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:279:46: .
            	    {
            	    matchAny(); 

            	    }
            	    break;

            	default :
            	    break loop9;
                }
            } while (true);


            match('\"'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:283:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )+ )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:283:10: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )+
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:283:10: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' )+
            int cnt10=0;
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( ((LA10_0 >= '0' && LA10_0 <= '9')||(LA10_0 >= 'A' && LA10_0 <= 'Z')||LA10_0=='_'||(LA10_0 >= 'a' && LA10_0 <= 'z')) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
            	    {
            	    if ( (input.LA(1) >= '0' && input.LA(1) <= '9')||(input.LA(1) >= 'A' && input.LA(1) <= 'Z')||input.LA(1)=='_'||(input.LA(1) >= 'a' && input.LA(1) <= 'z') ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt10 >= 1 ) break loop10;
                        EarlyExitException eee =
                            new EarlyExitException(10, input);
                        throw eee;
                }
                cnt10++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ID"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:287:5: ( ( ' ' | '\\t' | '\\r' | '\\n' )+ )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:287:10: ( ' ' | '\\t' | '\\r' | '\\n' )+
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:287:10: ( ' ' | '\\t' | '\\r' | '\\n' )+
            int cnt11=0;
            loop11:
            do {
                int alt11=2;
                int LA11_0 = input.LA(1);

                if ( ((LA11_0 >= '\t' && LA11_0 <= '\n')||LA11_0=='\r'||LA11_0==' ') ) {
                    alt11=1;
                }


                switch (alt11) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
            	    {
            	    if ( (input.LA(1) >= '\t' && input.LA(1) <= '\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();
            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;
            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt11 >= 1 ) break loop11;
                        EarlyExitException eee =
                            new EarlyExitException(11, input);
                        throw eee;
                }
                cnt11++;
            } while (true);


             _channel=HIDDEN; 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "ESC"
    public final void mESC() throws RecognitionException {
        try {
            int _type = ESC;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:291:5: ( '\\\\' ( '\"' | '\\'' | '\\\\' ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:291:10: '\\\\' ( '\"' | '\\'' | '\\\\' )
            {
            match('\\'); 

            if ( input.LA(1)=='\"'||input.LA(1)=='\''||input.LA(1)=='\\' ) {
                input.consume();
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;
            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        	// do for sure before leaving
        }
    }
    // $ANTLR end "ESC"

    public void mTokens() throws RecognitionException {
        // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:8: ( AND | AT | AVG | COMMA | COUNT | DATE | DOLLAR | EQ | EXISTS | GE | GT | IN | INNER_JOIN | INTERSECTION | ISEMPTY | ISNULL | LBRACE | LBRACKET | LE | LEFT_JOIN | LPARENT | LT | MAX | MIN | NE | NOT | OR | RBRACE | RBRACKET | REGEX | REG_INSENSITIVE | REG_SENSITIVE | REVERSE | RIGHT_JOIN | RPARENT | STAR | SUBQUERY | SUM | UNION | BOOLEAN | INTEGER | DECIMAL | STRING | ID | WS | ESC )
        int alt12=46;
        alt12 = dfa12.predict(input);
        switch (alt12) {
            case 1 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:10: AND
                {
                mAND(); 


                }
                break;
            case 2 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:14: AT
                {
                mAT(); 


                }
                break;
            case 3 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:17: AVG
                {
                mAVG(); 


                }
                break;
            case 4 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:21: COMMA
                {
                mCOMMA(); 


                }
                break;
            case 5 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:27: COUNT
                {
                mCOUNT(); 


                }
                break;
            case 6 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:33: DATE
                {
                mDATE(); 


                }
                break;
            case 7 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:38: DOLLAR
                {
                mDOLLAR(); 


                }
                break;
            case 8 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:45: EQ
                {
                mEQ(); 


                }
                break;
            case 9 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:48: EXISTS
                {
                mEXISTS(); 


                }
                break;
            case 10 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:55: GE
                {
                mGE(); 


                }
                break;
            case 11 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:58: GT
                {
                mGT(); 


                }
                break;
            case 12 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:61: IN
                {
                mIN(); 


                }
                break;
            case 13 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:64: INNER_JOIN
                {
                mINNER_JOIN(); 


                }
                break;
            case 14 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:75: INTERSECTION
                {
                mINTERSECTION(); 


                }
                break;
            case 15 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:88: ISEMPTY
                {
                mISEMPTY(); 


                }
                break;
            case 16 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:96: ISNULL
                {
                mISNULL(); 


                }
                break;
            case 17 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:103: LBRACE
                {
                mLBRACE(); 


                }
                break;
            case 18 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:110: LBRACKET
                {
                mLBRACKET(); 


                }
                break;
            case 19 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:119: LE
                {
                mLE(); 


                }
                break;
            case 20 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:122: LEFT_JOIN
                {
                mLEFT_JOIN(); 


                }
                break;
            case 21 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:132: LPARENT
                {
                mLPARENT(); 


                }
                break;
            case 22 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:140: LT
                {
                mLT(); 


                }
                break;
            case 23 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:143: MAX
                {
                mMAX(); 


                }
                break;
            case 24 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:147: MIN
                {
                mMIN(); 


                }
                break;
            case 25 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:151: NE
                {
                mNE(); 


                }
                break;
            case 26 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:154: NOT
                {
                mNOT(); 


                }
                break;
            case 27 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:158: OR
                {
                mOR(); 


                }
                break;
            case 28 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:161: RBRACE
                {
                mRBRACE(); 


                }
                break;
            case 29 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:168: RBRACKET
                {
                mRBRACKET(); 


                }
                break;
            case 30 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:177: REGEX
                {
                mREGEX(); 


                }
                break;
            case 31 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:183: REG_INSENSITIVE
                {
                mREG_INSENSITIVE(); 


                }
                break;
            case 32 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:199: REG_SENSITIVE
                {
                mREG_SENSITIVE(); 


                }
                break;
            case 33 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:213: REVERSE
                {
                mREVERSE(); 


                }
                break;
            case 34 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:221: RIGHT_JOIN
                {
                mRIGHT_JOIN(); 


                }
                break;
            case 35 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:232: RPARENT
                {
                mRPARENT(); 


                }
                break;
            case 36 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:240: STAR
                {
                mSTAR(); 


                }
                break;
            case 37 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:245: SUBQUERY
                {
                mSUBQUERY(); 


                }
                break;
            case 38 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:254: SUM
                {
                mSUM(); 


                }
                break;
            case 39 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:258: UNION
                {
                mUNION(); 


                }
                break;
            case 40 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:264: BOOLEAN
                {
                mBOOLEAN(); 


                }
                break;
            case 41 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:272: INTEGER
                {
                mINTEGER(); 


                }
                break;
            case 42 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:280: DECIMAL
                {
                mDECIMAL(); 


                }
                break;
            case 43 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:288: STRING
                {
                mSTRING(); 


                }
                break;
            case 44 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:295: ID
                {
                mID(); 


                }
                break;
            case 45 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:298: WS
                {
                mWS(); 


                }
                break;
            case 46 :
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:1:301: ESC
                {
                mESC(); 


                }
                break;

        }

    }


    protected DFA12 dfa12 = new DFA12(this);
    static final String DFA12_eotS =
        "\1\uffff\1\42\2\uffff\2\42\1\uffff\1\53\1\42\1\56\1\42\1\62\3\uffff"+
        "\1\65\2\uffff\1\42\1\71\2\42\5\uffff\1\42\1\uffff\2\42\1\uffff\1"+
        "\102\4\uffff\4\42\3\uffff\1\42\2\uffff\1\110\1\42\5\uffff\2\42\2"+
        "\uffff\1\42\1\116\2\uffff\3\42\1\102\1\uffff\1\122\1\123\3\42\1"+
        "\uffff\2\42\1\131\1\132\1\133\1\uffff\1\134\2\42\2\uffff\1\42\1"+
        "\140\3\42\4\uffff\1\144\1\42\1\146\1\uffff\3\42\1\uffff\1\144\1"+
        "\uffff\1\152\1\42\1\154\1\uffff\1\155\2\uffff";
    static final String DFA12_eofS =
        "\156\uffff";
    static final String DFA12_minS =
        "\1\11\1\156\2\uffff\1\157\1\141\1\uffff\1\46\1\170\1\75\1\156\1"+
        "\53\3\uffff\1\75\2\uffff\1\141\1\75\1\157\1\162\2\uffff\1\151\2"+
        "\uffff\1\165\1\uffff\1\162\1\141\2\56\4\uffff\1\144\1\147\1\165"+
        "\1\164\3\uffff\1\151\2\uffff\1\60\1\145\5\uffff\1\170\1\156\2\uffff"+
        "\1\164\1\60\2\uffff\1\155\1\165\1\154\1\56\1\uffff\2\60\1\156\1"+
        "\145\1\163\1\uffff\1\155\1\165\3\60\1\uffff\1\60\1\145\1\163\2\uffff"+
        "\1\164\1\60\1\164\1\160\1\154\4\uffff\1\60\1\145\1\60\1\uffff\1"+
        "\163\1\164\1\154\1\uffff\1\60\1\uffff\1\60\1\171\1\60\1\uffff\1"+
        "\60\2\uffff";
    static final String DFA12_maxS =
        "\1\175\1\166\2\uffff\1\157\1\141\1\uffff\1\176\1\170\1\75\1\163"+
        "\1\71\3\uffff\1\75\2\uffff\1\151\1\75\1\157\1\162\2\uffff\1\163"+
        "\2\uffff\1\165\1\uffff\1\162\1\141\1\71\1\172\4\uffff\1\144\1\147"+
        "\1\165\1\164\3\uffff\1\151\2\uffff\1\172\1\156\5\uffff\1\170\1\156"+
        "\2\uffff\1\164\1\172\2\uffff\1\155\1\165\1\154\1\71\1\uffff\2\172"+
        "\1\156\1\145\1\163\1\uffff\1\155\1\165\3\172\1\uffff\1\172\1\145"+
        "\1\163\2\uffff\1\164\1\172\1\164\1\160\1\154\4\uffff\1\172\1\145"+
        "\1\172\1\uffff\1\163\1\164\1\154\1\uffff\1\172\1\uffff\1\172\1\171"+
        "\1\172\1\uffff\1\172\2\uffff";
    static final String DFA12_acceptS =
        "\2\uffff\1\2\1\4\2\uffff\1\7\5\uffff\1\16\1\21\1\22\1\uffff\1\24"+
        "\1\25\4\uffff\1\34\1\35\1\uffff\1\43\1\44\1\uffff\1\47\4\uffff\1"+
        "\53\1\54\1\55\1\56\4\uffff\1\36\1\45\1\10\1\uffff\1\12\1\13\2\uffff"+
        "\1\42\1\15\1\52\1\23\1\26\2\uffff\1\31\1\41\2\uffff\1\37\1\40\4"+
        "\uffff\1\51\5\uffff\1\14\5\uffff\1\33\3\uffff\1\1\1\3\5\uffff\1"+
        "\27\1\30\1\32\1\46\3\uffff\1\6\3\uffff\1\50\1\uffff\1\5\3\uffff"+
        "\1\11\1\uffff\1\20\1\17";
    static final String DFA12_specialS =
        "\156\uffff}>";
    static final String[] DFA12_transitionS = {
            "\2\43\2\uffff\1\43\22\uffff\1\43\1\23\1\41\1\uffff\1\6\1\uffff"+
            "\1\14\1\uffff\1\21\1\31\1\32\1\20\1\3\1\37\1\13\1\30\12\40\2"+
            "\uffff\1\17\1\7\1\11\1\uffff\1\2\32\42\1\16\1\44\1\27\1\uffff"+
            "\1\42\1\uffff\1\1\1\42\1\4\1\5\1\10\1\36\2\42\1\12\3\42\1\22"+
            "\1\24\1\25\3\42\1\33\1\35\6\42\1\15\1\34\1\26",
            "\1\45\7\uffff\1\46",
            "",
            "",
            "\1\47",
            "\1\50",
            "",
            "\1\52\127\uffff\1\51",
            "\1\54",
            "\1\55",
            "\1\57\4\uffff\1\60",
            "\1\61\4\uffff\12\63",
            "",
            "",
            "",
            "\1\64",
            "",
            "",
            "\1\66\7\uffff\1\67",
            "\1\70",
            "\1\72",
            "\1\73",
            "",
            "",
            "\1\74\11\uffff\1\75",
            "",
            "",
            "\1\76",
            "",
            "\1\77",
            "\1\100",
            "\1\63\1\uffff\12\101",
            "\1\63\1\uffff\12\40\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "",
            "",
            "",
            "\1\103",
            "\1\104",
            "\1\105",
            "\1\106",
            "",
            "",
            "",
            "\1\107",
            "",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\111\10\uffff\1\112",
            "",
            "",
            "",
            "",
            "",
            "\1\113",
            "\1\114",
            "",
            "",
            "\1\115",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "",
            "\1\117",
            "\1\120",
            "\1\121",
            "\1\63\1\uffff\12\101",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\124",
            "\1\125",
            "\1\126",
            "",
            "\1\127",
            "\1\130",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\135",
            "\1\136",
            "",
            "",
            "\1\137",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\141",
            "\1\142",
            "\1\143",
            "",
            "",
            "",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\145",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "\1\147",
            "\1\150",
            "\1\151",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "\1\153",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            "\12\42\7\uffff\32\42\4\uffff\1\42\1\uffff\32\42",
            "",
            ""
    };

    static final short[] DFA12_eot = DFA.unpackEncodedString(DFA12_eotS);
    static final short[] DFA12_eof = DFA.unpackEncodedString(DFA12_eofS);
    static final char[] DFA12_min = DFA.unpackEncodedStringToUnsignedChars(DFA12_minS);
    static final char[] DFA12_max = DFA.unpackEncodedStringToUnsignedChars(DFA12_maxS);
    static final short[] DFA12_accept = DFA.unpackEncodedString(DFA12_acceptS);
    static final short[] DFA12_special = DFA.unpackEncodedString(DFA12_specialS);
    static final short[][] DFA12_transition;

    static {
        int numStates = DFA12_transitionS.length;
        DFA12_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA12_transition[i] = DFA.unpackEncodedString(DFA12_transitionS[i]);
        }
    }

    class DFA12 extends DFA {

        public DFA12(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 12;
            this.eot = DFA12_eot;
            this.eof = DFA12_eof;
            this.min = DFA12_min;
            this.max = DFA12_max;
            this.accept = DFA12_accept;
            this.special = DFA12_special;
            this.transition = DFA12_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( AND | AT | AVG | COMMA | COUNT | DATE | DOLLAR | EQ | EXISTS | GE | GT | IN | INNER_JOIN | INTERSECTION | ISEMPTY | ISNULL | LBRACE | LBRACKET | LE | LEFT_JOIN | LPARENT | LT | MAX | MIN | NE | NOT | OR | RBRACE | RBRACKET | REGEX | REG_INSENSITIVE | REG_SENSITIVE | REVERSE | RIGHT_JOIN | RPARENT | STAR | SUBQUERY | SUM | UNION | BOOLEAN | INTEGER | DECIMAL | STRING | ID | WS | ESC );";
        }
    }
 

}