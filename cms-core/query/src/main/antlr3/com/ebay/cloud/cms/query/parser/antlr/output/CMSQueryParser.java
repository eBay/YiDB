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

import org.antlr.runtime.tree.*;


/**
* Grammer file for CMS Query Language
*
*    @author xjiang
*
**/
@SuppressWarnings({"all", "warnings", "unchecked"})
public class CMSQueryParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AGGREGATION", "AGGR_ATTR", "AND", "AT", "AVG", "BOOLEAN", "BOOL_VAL", "COMMA", "COUNT", "DATE", "DATE_VAL", "DECIMAL", "DEC_VAL", "DOLLAR", "EQ", "ESC", "EXISTS", "FILTER", "GE", "GROUP", "GT", "ID", "IN", "INNER_JOIN", "INTEGER", "INTERSECTION", "INT_VAL", "ISEMPTY", "ISNULL", "LBRACE", "LBRACKET", "LE", "LEFT_JOIN", "LPARENT", "LT", "MAX", "MIN", "NE", "NOT", "OR", "PROJECTION", "QUERY", "QUERY_NODE", "RBRACE", "RBRACKET", "REGEX", "REG_INSENSITIVE", "REG_SENSITIVE", "REG_VAL", "REVERSE", "RIGHT_JOIN", "RPARENT", "SRCH_ATTR", "STAR", "STRING", "STR_VAL", "SUBQUERY", "SUM", "TYPE_LIST", "UNION", "VALUE_LIST", "WS"
    };

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
    public Parser[] getDelegates() {
        return new Parser[] {};
    }

    // delegators


    public CMSQueryParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }
    public CMSQueryParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

protected TreeAdaptor adaptor = new CommonTreeAdaptor();

public void setTreeAdaptor(TreeAdaptor adaptor) {
    this.adaptor = adaptor;
}
public TreeAdaptor getTreeAdaptor() {
    return adaptor;
}
    public String[] getTokenNames() { return CMSQueryParser.tokenNames; }
    public String getGrammarFileName() { return "/ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g"; }


    public static class start_rule_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "start_rule"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:90:1: start_rule : query ;
    public final CMSQueryParser.start_rule_return start_rule() throws RecognitionException {
        CMSQueryParser.start_rule_return retval = new CMSQueryParser.start_rule_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.query_return query1 =null;



        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:90:11: ( query )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:90:13: query
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_query_in_start_rule908);
            query1=query();

            state._fsp--;

            adaptor.addChild(root_0, query1.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "start_rule"


    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "query"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:92:1: query : path -> ^( QUERY path ) ;
    public final CMSQueryParser.query_return query() throws RecognitionException {
        CMSQueryParser.query_return retval = new CMSQueryParser.query_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.path_return path2 =null;


        RewriteRuleSubtreeStream stream_path=new RewriteRuleSubtreeStream(adaptor,"rule path");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:93:5: ( path -> ^( QUERY path ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:93:9: path
            {
            pushFollow(FOLLOW_path_in_query925);
            path2=path();

            state._fsp--;

            stream_path.add(path2.getTree());

            // AST REWRITE
            // elements: path
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 93:14: -> ^( QUERY path )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:93:17: ^( QUERY path )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(QUERY, "QUERY")
                , root_1);

                adaptor.addChild(root_1, stream_path.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "query"


    public static class path_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "path"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:97:1: path : unionPath ;
    public final CMSQueryParser.path_return path() throws RecognitionException {
        CMSQueryParser.path_return retval = new CMSQueryParser.path_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.unionPath_return unionPath3 =null;



        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:98:5: ( unionPath )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:98:9: unionPath
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_unionPath_in_path955);
            unionPath3=unionPath();

            state._fsp--;

            adaptor.addChild(root_0, unionPath3.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "path"


    public static class unionPath_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "unionPath"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:101:1: unionPath : intersectionPath ( UNION ^ intersectionPath )* ;
    public final CMSQueryParser.unionPath_return unionPath() throws RecognitionException {
        CMSQueryParser.unionPath_return retval = new CMSQueryParser.unionPath_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token UNION5=null;
        CMSQueryParser.intersectionPath_return intersectionPath4 =null;

        CMSQueryParser.intersectionPath_return intersectionPath6 =null;


        Object UNION5_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:102:5: ( intersectionPath ( UNION ^ intersectionPath )* )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:102:9: intersectionPath ( UNION ^ intersectionPath )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_intersectionPath_in_unionPath975);
            intersectionPath4=intersectionPath();

            state._fsp--;

            adaptor.addChild(root_0, intersectionPath4.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:102:26: ( UNION ^ intersectionPath )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==UNION) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:102:27: UNION ^ intersectionPath
            	    {
            	    UNION5=(Token)match(input,UNION,FOLLOW_UNION_in_unionPath978); 
            	    UNION5_tree = 
            	    (Object)adaptor.create(UNION5)
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(UNION5_tree, root_0);


            	    pushFollow(FOLLOW_intersectionPath_in_unionPath981);
            	    intersectionPath6=intersectionPath();

            	    state._fsp--;

            	    adaptor.addChild(root_0, intersectionPath6.getTree());

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "unionPath"


    public static class intersectionPath_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "intersectionPath"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:105:1: intersectionPath : primaryPath ( INTERSECTION ^ primaryPath )* ;
    public final CMSQueryParser.intersectionPath_return intersectionPath() throws RecognitionException {
        CMSQueryParser.intersectionPath_return retval = new CMSQueryParser.intersectionPath_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token INTERSECTION8=null;
        CMSQueryParser.primaryPath_return primaryPath7 =null;

        CMSQueryParser.primaryPath_return primaryPath9 =null;


        Object INTERSECTION8_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:106:5: ( primaryPath ( INTERSECTION ^ primaryPath )* )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:106:9: primaryPath ( INTERSECTION ^ primaryPath )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_primaryPath_in_intersectionPath1003);
            primaryPath7=primaryPath();

            state._fsp--;

            adaptor.addChild(root_0, primaryPath7.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:106:21: ( INTERSECTION ^ primaryPath )*
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==INTERSECTION) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:106:22: INTERSECTION ^ primaryPath
            	    {
            	    INTERSECTION8=(Token)match(input,INTERSECTION,FOLLOW_INTERSECTION_in_intersectionPath1006); 
            	    INTERSECTION8_tree = 
            	    (Object)adaptor.create(INTERSECTION8)
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(INTERSECTION8_tree, root_0);


            	    pushFollow(FOLLOW_primaryPath_in_intersectionPath1009);
            	    primaryPath9=primaryPath();

            	    state._fsp--;

            	    adaptor.addChild(root_0, primaryPath9.getTree());

            	    }
            	    break;

            	default :
            	    break loop2;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "intersectionPath"


    public static class primaryPath_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "primaryPath"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:109:1: primaryPath : ( LPARENT ! path RPARENT !| queryNode ( options {greedy=true; } : ( INNER_JOIN | LEFT_JOIN | RIGHT_JOIN ) ^ primaryPath )* );
    public final CMSQueryParser.primaryPath_return primaryPath() throws RecognitionException {
        CMSQueryParser.primaryPath_return retval = new CMSQueryParser.primaryPath_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LPARENT10=null;
        Token RPARENT12=null;
        Token set14=null;
        CMSQueryParser.path_return path11 =null;

        CMSQueryParser.queryNode_return queryNode13 =null;

        CMSQueryParser.primaryPath_return primaryPath15 =null;


        Object LPARENT10_tree=null;
        Object RPARENT12_tree=null;
        Object set14_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:110:5: ( LPARENT ! path RPARENT !| queryNode ( options {greedy=true; } : ( INNER_JOIN | LEFT_JOIN | RIGHT_JOIN ) ^ primaryPath )* )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==LPARENT) ) {
                alt4=1;
            }
            else if ( (LA4_0==ID||LA4_0==LT) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;

            }
            switch (alt4) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:110:9: LPARENT ! path RPARENT !
                    {
                    root_0 = (Object)adaptor.nil();


                    LPARENT10=(Token)match(input,LPARENT,FOLLOW_LPARENT_in_primaryPath1034); 

                    pushFollow(FOLLOW_path_in_primaryPath1037);
                    path11=path();

                    state._fsp--;

                    adaptor.addChild(root_0, path11.getTree());

                    RPARENT12=(Token)match(input,RPARENT,FOLLOW_RPARENT_in_primaryPath1039); 

                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:111:9: queryNode ( options {greedy=true; } : ( INNER_JOIN | LEFT_JOIN | RIGHT_JOIN ) ^ primaryPath )*
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_queryNode_in_primaryPath1050);
                    queryNode13=queryNode();

                    state._fsp--;

                    adaptor.addChild(root_0, queryNode13.getTree());

                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:111:19: ( options {greedy=true; } : ( INNER_JOIN | LEFT_JOIN | RIGHT_JOIN ) ^ primaryPath )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( (LA3_0==INNER_JOIN||LA3_0==LEFT_JOIN||LA3_0==RIGHT_JOIN) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:111:43: ( INNER_JOIN | LEFT_JOIN | RIGHT_JOIN ) ^ primaryPath
                    	    {
                    	    set14=(Token)input.LT(1);

                    	    set14=(Token)input.LT(1);

                    	    if ( input.LA(1)==INNER_JOIN||input.LA(1)==LEFT_JOIN||input.LA(1)==RIGHT_JOIN ) {
                    	        input.consume();
                    	        root_0 = (Object)adaptor.becomeRoot(
                    	        (Object)adaptor.create(set14)
                    	        , root_0);
                    	        state.errorRecovery=false;
                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        throw mse;
                    	    }


                    	    pushFollow(FOLLOW_primaryPath_in_primaryPath1070);
                    	    primaryPath15=primaryPath();

                    	    state._fsp--;

                    	    adaptor.addChild(root_0, primaryPath15.getTree());

                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);


                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "primaryPath"


    public static class queryNode_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "queryNode"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:115:1: queryNode : ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? -> ^( QUERY_NODE ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? ) ;
    public final CMSQueryParser.queryNode_return queryNode() throws RecognitionException {
        CMSQueryParser.queryNode_return retval = new CMSQueryParser.queryNode_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.typeCast_return typeCast16 =null;

        CMSQueryParser.nodeName_return nodeName17 =null;

        CMSQueryParser.filter_return filter18 =null;

        CMSQueryParser.aggregation_return aggregation19 =null;

        CMSQueryParser.projection_return projection20 =null;


        RewriteRuleSubtreeStream stream_aggregation=new RewriteRuleSubtreeStream(adaptor,"rule aggregation");
        RewriteRuleSubtreeStream stream_projection=new RewriteRuleSubtreeStream(adaptor,"rule projection");
        RewriteRuleSubtreeStream stream_nodeName=new RewriteRuleSubtreeStream(adaptor,"rule nodeName");
        RewriteRuleSubtreeStream stream_typeCast=new RewriteRuleSubtreeStream(adaptor,"rule typeCast");
        RewriteRuleSubtreeStream stream_filter=new RewriteRuleSubtreeStream(adaptor,"rule filter");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:5: ( ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? -> ^( QUERY_NODE ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:9: ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )?
            {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:9: ( typeCast )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==LT) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:10: typeCast
                    {
                    pushFollow(FOLLOW_typeCast_in_queryNode1097);
                    typeCast16=typeCast();

                    state._fsp--;

                    stream_typeCast.add(typeCast16.getTree());

                    }
                    break;

            }


            pushFollow(FOLLOW_nodeName_in_queryNode1101);
            nodeName17=nodeName();

            state._fsp--;

            stream_nodeName.add(nodeName17.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:30: ( filter )?
            int alt6=2;
            int LA6_0 = input.LA(1);

            if ( (LA6_0==LBRACKET) ) {
                alt6=1;
            }
            switch (alt6) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:31: filter
                    {
                    pushFollow(FOLLOW_filter_in_queryNode1104);
                    filter18=filter();

                    state._fsp--;

                    stream_filter.add(filter18.getTree());

                    }
                    break;

            }


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:40: ( aggregation )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==LT) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:41: aggregation
                    {
                    pushFollow(FOLLOW_aggregation_in_queryNode1109);
                    aggregation19=aggregation();

                    state._fsp--;

                    stream_aggregation.add(aggregation19.getTree());

                    }
                    break;

            }


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:55: ( projection )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==LBRACE) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:56: projection
                    {
                    pushFollow(FOLLOW_projection_in_queryNode1114);
                    projection20=projection();

                    state._fsp--;

                    stream_projection.add(projection20.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: typeCast, filter, projection, nodeName, aggregation
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 116:69: -> ^( QUERY_NODE ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:72: ^( QUERY_NODE ( typeCast )? nodeName ( filter )? ( aggregation )? ( projection )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(QUERY_NODE, "QUERY_NODE")
                , root_1);

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:85: ( typeCast )?
                if ( stream_typeCast.hasNext() ) {
                    adaptor.addChild(root_1, stream_typeCast.nextTree());

                }
                stream_typeCast.reset();

                adaptor.addChild(root_1, stream_nodeName.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:104: ( filter )?
                if ( stream_filter.hasNext() ) {
                    adaptor.addChild(root_1, stream_filter.nextTree());

                }
                stream_filter.reset();

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:112: ( aggregation )?
                if ( stream_aggregation.hasNext() ) {
                    adaptor.addChild(root_1, stream_aggregation.nextTree());

                }
                stream_aggregation.reset();

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:116:125: ( projection )?
                if ( stream_projection.hasNext() ) {
                    adaptor.addChild(root_1, stream_projection.nextTree());

                }
                stream_projection.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "queryNode"


    public static class nodeName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "nodeName"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:119:1: nodeName : ID ( REVERSE ^ ID )? ;
    public final CMSQueryParser.nodeName_return nodeName() throws RecognitionException {
        CMSQueryParser.nodeName_return retval = new CMSQueryParser.nodeName_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID21=null;
        Token REVERSE22=null;
        Token ID23=null;

        Object ID21_tree=null;
        Object REVERSE22_tree=null;
        Object ID23_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:120:5: ( ID ( REVERSE ^ ID )? )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:120:9: ID ( REVERSE ^ ID )?
            {
            root_0 = (Object)adaptor.nil();


            ID21=(Token)match(input,ID,FOLLOW_ID_in_nodeName1159); 
            ID21_tree = 
            (Object)adaptor.create(ID21)
            ;
            adaptor.addChild(root_0, ID21_tree);


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:120:12: ( REVERSE ^ ID )?
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==REVERSE) ) {
                alt9=1;
            }
            switch (alt9) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:120:13: REVERSE ^ ID
                    {
                    REVERSE22=(Token)match(input,REVERSE,FOLLOW_REVERSE_in_nodeName1162); 
                    REVERSE22_tree = 
                    (Object)adaptor.create(REVERSE22)
                    ;
                    root_0 = (Object)adaptor.becomeRoot(REVERSE22_tree, root_0);


                    ID23=(Token)match(input,ID,FOLLOW_ID_in_nodeName1165); 
                    ID23_tree = 
                    (Object)adaptor.create(ID23)
                    ;
                    adaptor.addChild(root_0, ID23_tree);


                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "nodeName"


    public static class typeCast_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "typeCast"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:123:1: typeCast : LT typeName ( COMMA typeName )* GT -> ^( TYPE_LIST typeName ( typeName )* ) ;
    public final CMSQueryParser.typeCast_return typeCast() throws RecognitionException {
        CMSQueryParser.typeCast_return retval = new CMSQueryParser.typeCast_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LT24=null;
        Token COMMA26=null;
        Token GT28=null;
        CMSQueryParser.typeName_return typeName25 =null;

        CMSQueryParser.typeName_return typeName27 =null;


        Object LT24_tree=null;
        Object COMMA26_tree=null;
        Object GT28_tree=null;
        RewriteRuleTokenStream stream_GT=new RewriteRuleTokenStream(adaptor,"token GT");
        RewriteRuleTokenStream stream_LT=new RewriteRuleTokenStream(adaptor,"token LT");
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleSubtreeStream stream_typeName=new RewriteRuleSubtreeStream(adaptor,"rule typeName");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:5: ( LT typeName ( COMMA typeName )* GT -> ^( TYPE_LIST typeName ( typeName )* ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:10: LT typeName ( COMMA typeName )* GT
            {
            LT24=(Token)match(input,LT,FOLLOW_LT_in_typeCast1187);  
            stream_LT.add(LT24);


            pushFollow(FOLLOW_typeName_in_typeCast1189);
            typeName25=typeName();

            state._fsp--;

            stream_typeName.add(typeName25.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:22: ( COMMA typeName )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==COMMA) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:23: COMMA typeName
            	    {
            	    COMMA26=(Token)match(input,COMMA,FOLLOW_COMMA_in_typeCast1192);  
            	    stream_COMMA.add(COMMA26);


            	    pushFollow(FOLLOW_typeName_in_typeCast1194);
            	    typeName27=typeName();

            	    state._fsp--;

            	    stream_typeName.add(typeName27.getTree());

            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);


            GT28=(Token)match(input,GT,FOLLOW_GT_in_typeCast1198);  
            stream_GT.add(GT28);


            // AST REWRITE
            // elements: typeName, typeName
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 124:43: -> ^( TYPE_LIST typeName ( typeName )* )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:46: ^( TYPE_LIST typeName ( typeName )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(TYPE_LIST, "TYPE_LIST")
                , root_1);

                adaptor.addChild(root_1, stream_typeName.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:124:67: ( typeName )*
                while ( stream_typeName.hasNext() ) {
                    adaptor.addChild(root_1, stream_typeName.nextTree());

                }
                stream_typeName.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "typeCast"


    public static class typeName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "typeName"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:127:1: typeName : ID ;
    public final CMSQueryParser.typeName_return typeName() throws RecognitionException {
        CMSQueryParser.typeName_return retval = new CMSQueryParser.typeName_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID29=null;

        Object ID29_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:128:5: ( ID )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:128:10: ID
            {
            root_0 = (Object)adaptor.nil();


            ID29=(Token)match(input,ID,FOLLOW_ID_in_typeName1235); 
            ID29_tree = 
            (Object)adaptor.create(ID29)
            ;
            adaptor.addChild(root_0, ID29_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "typeName"


    public static class aggregation_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "aggregation"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:131:1: aggregation : group ( filter )? -> ^( AGGREGATION group ( filter )? ) ;
    public final CMSQueryParser.aggregation_return aggregation() throws RecognitionException {
        CMSQueryParser.aggregation_return retval = new CMSQueryParser.aggregation_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.group_return group30 =null;

        CMSQueryParser.filter_return filter31 =null;


        RewriteRuleSubtreeStream stream_group=new RewriteRuleSubtreeStream(adaptor,"rule group");
        RewriteRuleSubtreeStream stream_filter=new RewriteRuleSubtreeStream(adaptor,"rule filter");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:5: ( group ( filter )? -> ^( AGGREGATION group ( filter )? ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:10: group ( filter )?
            {
            pushFollow(FOLLOW_group_in_aggregation1259);
            group30=group();

            state._fsp--;

            stream_group.add(group30.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:16: ( filter )?
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0==LBRACKET) ) {
                alt11=1;
            }
            switch (alt11) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:16: filter
                    {
                    pushFollow(FOLLOW_filter_in_aggregation1261);
                    filter31=filter();

                    state._fsp--;

                    stream_filter.add(filter31.getTree());

                    }
                    break;

            }


            // AST REWRITE
            // elements: filter, group
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 132:24: -> ^( AGGREGATION group ( filter )? )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:27: ^( AGGREGATION group ( filter )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(AGGREGATION, "AGGREGATION")
                , root_1);

                adaptor.addChild(root_1, stream_group.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:132:47: ( filter )?
                if ( stream_filter.hasNext() ) {
                    adaptor.addChild(root_1, stream_filter.nextTree());

                }
                stream_filter.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "aggregation"


    public static class group_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "group"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:135:1: group : LT attributeList GT -> ^( GROUP attributeList ) ;
    public final CMSQueryParser.group_return group() throws RecognitionException {
        CMSQueryParser.group_return retval = new CMSQueryParser.group_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LT32=null;
        Token GT34=null;
        CMSQueryParser.attributeList_return attributeList33 =null;


        Object LT32_tree=null;
        Object GT34_tree=null;
        RewriteRuleTokenStream stream_GT=new RewriteRuleTokenStream(adaptor,"token GT");
        RewriteRuleTokenStream stream_LT=new RewriteRuleTokenStream(adaptor,"token LT");
        RewriteRuleSubtreeStream stream_attributeList=new RewriteRuleSubtreeStream(adaptor,"rule attributeList");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:136:5: ( LT attributeList GT -> ^( GROUP attributeList ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:136:10: LT attributeList GT
            {
            LT32=(Token)match(input,LT,FOLLOW_LT_in_group1297);  
            stream_LT.add(LT32);


            pushFollow(FOLLOW_attributeList_in_group1299);
            attributeList33=attributeList();

            state._fsp--;

            stream_attributeList.add(attributeList33.getTree());

            GT34=(Token)match(input,GT,FOLLOW_GT_in_group1301);  
            stream_GT.add(GT34);


            // AST REWRITE
            // elements: attributeList
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 136:30: -> ^( GROUP attributeList )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:136:33: ^( GROUP attributeList )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(GROUP, "GROUP")
                , root_1);

                adaptor.addChild(root_1, stream_attributeList.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "group"


    public static class filter_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "filter"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:139:1: filter : LBRACKET expression RBRACKET -> ^( FILTER expression ) ;
    public final CMSQueryParser.filter_return filter() throws RecognitionException {
        CMSQueryParser.filter_return retval = new CMSQueryParser.filter_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LBRACKET35=null;
        Token RBRACKET37=null;
        CMSQueryParser.expression_return expression36 =null;


        Object LBRACKET35_tree=null;
        Object RBRACKET37_tree=null;
        RewriteRuleTokenStream stream_LBRACKET=new RewriteRuleTokenStream(adaptor,"token LBRACKET");
        RewriteRuleTokenStream stream_RBRACKET=new RewriteRuleTokenStream(adaptor,"token RBRACKET");
        RewriteRuleSubtreeStream stream_expression=new RewriteRuleSubtreeStream(adaptor,"rule expression");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:140:5: ( LBRACKET expression RBRACKET -> ^( FILTER expression ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:140:10: LBRACKET expression RBRACKET
            {
            LBRACKET35=(Token)match(input,LBRACKET,FOLLOW_LBRACKET_in_filter1333);  
            stream_LBRACKET.add(LBRACKET35);


            pushFollow(FOLLOW_expression_in_filter1335);
            expression36=expression();

            state._fsp--;

            stream_expression.add(expression36.getTree());

            RBRACKET37=(Token)match(input,RBRACKET,FOLLOW_RBRACKET_in_filter1337);  
            stream_RBRACKET.add(RBRACKET37);


            // AST REWRITE
            // elements: expression
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 140:39: -> ^( FILTER expression )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:140:42: ^( FILTER expression )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(FILTER, "FILTER")
                , root_1);

                adaptor.addChild(root_1, stream_expression.nextTree());

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "filter"


    public static class projection_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "projection"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:143:1: projection : LBRACE ( STAR | attributeList ) RBRACE -> ^( PROJECTION ( attributeList )? ( STAR )? ) ;
    public final CMSQueryParser.projection_return projection() throws RecognitionException {
        CMSQueryParser.projection_return retval = new CMSQueryParser.projection_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LBRACE38=null;
        Token STAR39=null;
        Token RBRACE41=null;
        CMSQueryParser.attributeList_return attributeList40 =null;


        Object LBRACE38_tree=null;
        Object STAR39_tree=null;
        Object RBRACE41_tree=null;
        RewriteRuleTokenStream stream_STAR=new RewriteRuleTokenStream(adaptor,"token STAR");
        RewriteRuleTokenStream stream_RBRACE=new RewriteRuleTokenStream(adaptor,"token RBRACE");
        RewriteRuleTokenStream stream_LBRACE=new RewriteRuleTokenStream(adaptor,"token LBRACE");
        RewriteRuleSubtreeStream stream_attributeList=new RewriteRuleSubtreeStream(adaptor,"rule attributeList");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:5: ( LBRACE ( STAR | attributeList ) RBRACE -> ^( PROJECTION ( attributeList )? ( STAR )? ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:10: LBRACE ( STAR | attributeList ) RBRACE
            {
            LBRACE38=(Token)match(input,LBRACE,FOLLOW_LBRACE_in_projection1373);  
            stream_LBRACE.add(LBRACE38);


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:17: ( STAR | attributeList )
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0==STAR) ) {
                alt12=1;
            }
            else if ( (LA12_0==AT||LA12_0==DOLLAR) ) {
                alt12=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;

            }
            switch (alt12) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:18: STAR
                    {
                    STAR39=(Token)match(input,STAR,FOLLOW_STAR_in_projection1376);  
                    stream_STAR.add(STAR39);


                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:25: attributeList
                    {
                    pushFollow(FOLLOW_attributeList_in_projection1380);
                    attributeList40=attributeList();

                    state._fsp--;

                    stream_attributeList.add(attributeList40.getTree());

                    }
                    break;

            }


            RBRACE41=(Token)match(input,RBRACE,FOLLOW_RBRACE_in_projection1383);  
            stream_RBRACE.add(RBRACE41);


            // AST REWRITE
            // elements: attributeList, STAR
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 144:48: -> ^( PROJECTION ( attributeList )? ( STAR )? )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:51: ^( PROJECTION ( attributeList )? ( STAR )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(PROJECTION, "PROJECTION")
                , root_1);

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:64: ( attributeList )?
                if ( stream_attributeList.hasNext() ) {
                    adaptor.addChild(root_1, stream_attributeList.nextTree());

                }
                stream_attributeList.reset();

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:144:79: ( STAR )?
                if ( stream_STAR.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_STAR.nextNode()
                    );

                }
                stream_STAR.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "projection"


    public static class attributeList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "attributeList"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:148:1: attributeList : attribute ( COMMA ! attribute )* ;
    public final CMSQueryParser.attributeList_return attributeList() throws RecognitionException {
        CMSQueryParser.attributeList_return retval = new CMSQueryParser.attributeList_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token COMMA43=null;
        CMSQueryParser.attribute_return attribute42 =null;

        CMSQueryParser.attribute_return attribute44 =null;


        Object COMMA43_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:149:5: ( attribute ( COMMA ! attribute )* )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:149:10: attribute ( COMMA ! attribute )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_attribute_in_attributeList1417);
            attribute42=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute42.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:149:20: ( COMMA ! attribute )*
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( (LA13_0==COMMA) ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:149:21: COMMA ! attribute
            	    {
            	    COMMA43=(Token)match(input,COMMA,FOLLOW_COMMA_in_attributeList1420); 

            	    pushFollow(FOLLOW_attribute_in_attributeList1423);
            	    attribute44=attribute();

            	    state._fsp--;

            	    adaptor.addChild(root_0, attribute44.getTree());

            	    }
            	    break;

            	default :
            	    break loop13;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "attributeList"


    public static class attribute_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "attribute"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:152:1: attribute : ( searchAttr | aggregateAttr );
    public final CMSQueryParser.attribute_return attribute() throws RecognitionException {
        CMSQueryParser.attribute_return retval = new CMSQueryParser.attribute_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.searchAttr_return searchAttr45 =null;

        CMSQueryParser.aggregateAttr_return aggregateAttr46 =null;



        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:153:5: ( searchAttr | aggregateAttr )
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==AT) ) {
                alt14=1;
            }
            else if ( (LA14_0==DOLLAR) ) {
                alt14=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 14, 0, input);

                throw nvae;

            }
            switch (alt14) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:153:10: searchAttr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_searchAttr_in_attribute1449);
                    searchAttr45=searchAttr();

                    state._fsp--;

                    adaptor.addChild(root_0, searchAttr45.getTree());

                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:153:23: aggregateAttr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_aggregateAttr_in_attribute1453);
                    aggregateAttr46=aggregateAttr();

                    state._fsp--;

                    adaptor.addChild(root_0, aggregateAttr46.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "attribute"


    public static class searchAttr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "searchAttr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:156:1: searchAttr : AT attrName ( INNER_JOIN DOLLAR attrName )* -> ^( SRCH_ATTR attrName ( attrName )* ) ;
    public final CMSQueryParser.searchAttr_return searchAttr() throws RecognitionException {
        CMSQueryParser.searchAttr_return retval = new CMSQueryParser.searchAttr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token AT47=null;
        Token INNER_JOIN49=null;
        Token DOLLAR50=null;
        CMSQueryParser.attrName_return attrName48 =null;

        CMSQueryParser.attrName_return attrName51 =null;


        Object AT47_tree=null;
        Object INNER_JOIN49_tree=null;
        Object DOLLAR50_tree=null;
        RewriteRuleTokenStream stream_AT=new RewriteRuleTokenStream(adaptor,"token AT");
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_INNER_JOIN=new RewriteRuleTokenStream(adaptor,"token INNER_JOIN");
        RewriteRuleSubtreeStream stream_attrName=new RewriteRuleSubtreeStream(adaptor,"rule attrName");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:5: ( AT attrName ( INNER_JOIN DOLLAR attrName )* -> ^( SRCH_ATTR attrName ( attrName )* ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:10: AT attrName ( INNER_JOIN DOLLAR attrName )*
            {
            AT47=(Token)match(input,AT,FOLLOW_AT_in_searchAttr1477);  
            stream_AT.add(AT47);


            pushFollow(FOLLOW_attrName_in_searchAttr1479);
            attrName48=attrName();

            state._fsp--;

            stream_attrName.add(attrName48.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:22: ( INNER_JOIN DOLLAR attrName )*
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( (LA15_0==INNER_JOIN) ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:23: INNER_JOIN DOLLAR attrName
            	    {
            	    INNER_JOIN49=(Token)match(input,INNER_JOIN,FOLLOW_INNER_JOIN_in_searchAttr1482);  
            	    stream_INNER_JOIN.add(INNER_JOIN49);


            	    DOLLAR50=(Token)match(input,DOLLAR,FOLLOW_DOLLAR_in_searchAttr1484);  
            	    stream_DOLLAR.add(DOLLAR50);


            	    pushFollow(FOLLOW_attrName_in_searchAttr1486);
            	    attrName51=attrName();

            	    state._fsp--;

            	    stream_attrName.add(attrName51.getTree());

            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);


            // AST REWRITE
            // elements: attrName, attrName
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 157:52: -> ^( SRCH_ATTR attrName ( attrName )* )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:55: ^( SRCH_ATTR attrName ( attrName )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(SRCH_ATTR, "SRCH_ATTR")
                , root_1);

                adaptor.addChild(root_1, stream_attrName.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:157:76: ( attrName )*
                while ( stream_attrName.hasNext() ) {
                    adaptor.addChild(root_1, stream_attrName.nextTree());

                }
                stream_attrName.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "searchAttr"


    public static class aggregateAttr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "aggregateAttr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:160:1: aggregateAttr : DOLLAR aggFunc LPARENT ( searchAttr )? RPARENT -> ^( AGGR_ATTR aggFunc ( searchAttr )? ) ;
    public final CMSQueryParser.aggregateAttr_return aggregateAttr() throws RecognitionException {
        CMSQueryParser.aggregateAttr_return retval = new CMSQueryParser.aggregateAttr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token DOLLAR52=null;
        Token LPARENT54=null;
        Token RPARENT56=null;
        CMSQueryParser.aggFunc_return aggFunc53 =null;

        CMSQueryParser.searchAttr_return searchAttr55 =null;


        Object DOLLAR52_tree=null;
        Object LPARENT54_tree=null;
        Object RPARENT56_tree=null;
        RewriteRuleTokenStream stream_DOLLAR=new RewriteRuleTokenStream(adaptor,"token DOLLAR");
        RewriteRuleTokenStream stream_LPARENT=new RewriteRuleTokenStream(adaptor,"token LPARENT");
        RewriteRuleTokenStream stream_RPARENT=new RewriteRuleTokenStream(adaptor,"token RPARENT");
        RewriteRuleSubtreeStream stream_aggFunc=new RewriteRuleSubtreeStream(adaptor,"rule aggFunc");
        RewriteRuleSubtreeStream stream_searchAttr=new RewriteRuleSubtreeStream(adaptor,"rule searchAttr");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:5: ( DOLLAR aggFunc LPARENT ( searchAttr )? RPARENT -> ^( AGGR_ATTR aggFunc ( searchAttr )? ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:10: DOLLAR aggFunc LPARENT ( searchAttr )? RPARENT
            {
            DOLLAR52=(Token)match(input,DOLLAR,FOLLOW_DOLLAR_in_aggregateAttr1530);  
            stream_DOLLAR.add(DOLLAR52);


            pushFollow(FOLLOW_aggFunc_in_aggregateAttr1532);
            aggFunc53=aggFunc();

            state._fsp--;

            stream_aggFunc.add(aggFunc53.getTree());

            LPARENT54=(Token)match(input,LPARENT,FOLLOW_LPARENT_in_aggregateAttr1534);  
            stream_LPARENT.add(LPARENT54);


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:33: ( searchAttr )?
            int alt16=2;
            int LA16_0 = input.LA(1);

            if ( (LA16_0==AT) ) {
                alt16=1;
            }
            switch (alt16) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:33: searchAttr
                    {
                    pushFollow(FOLLOW_searchAttr_in_aggregateAttr1536);
                    searchAttr55=searchAttr();

                    state._fsp--;

                    stream_searchAttr.add(searchAttr55.getTree());

                    }
                    break;

            }


            RPARENT56=(Token)match(input,RPARENT,FOLLOW_RPARENT_in_aggregateAttr1539);  
            stream_RPARENT.add(RPARENT56);


            // AST REWRITE
            // elements: searchAttr, aggFunc
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 161:53: -> ^( AGGR_ATTR aggFunc ( searchAttr )? )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:56: ^( AGGR_ATTR aggFunc ( searchAttr )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(AGGR_ATTR, "AGGR_ATTR")
                , root_1);

                adaptor.addChild(root_1, stream_aggFunc.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:161:76: ( searchAttr )?
                if ( stream_searchAttr.hasNext() ) {
                    adaptor.addChild(root_1, stream_searchAttr.nextTree());

                }
                stream_searchAttr.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "aggregateAttr"


    public static class attrName_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "attrName"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:164:1: attrName : ID ;
    public final CMSQueryParser.attrName_return attrName() throws RecognitionException {
        CMSQueryParser.attrName_return retval = new CMSQueryParser.attrName_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ID57=null;

        Object ID57_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:165:5: ( ID )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:165:10: ID
            {
            root_0 = (Object)adaptor.nil();


            ID57=(Token)match(input,ID,FOLLOW_ID_in_attrName1574); 
            ID57_tree = 
            (Object)adaptor.create(ID57)
            ;
            adaptor.addChild(root_0, ID57_tree);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "attrName"


    public static class aggFunc_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "aggFunc"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:168:1: aggFunc : ( MIN | MAX | AVG | SUM | COUNT );
    public final CMSQueryParser.aggFunc_return aggFunc() throws RecognitionException {
        CMSQueryParser.aggFunc_return retval = new CMSQueryParser.aggFunc_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set58=null;

        Object set58_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:169:5: ( MIN | MAX | AVG | SUM | COUNT )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:
            {
            root_0 = (Object)adaptor.nil();


            set58=(Token)input.LT(1);

            if ( input.LA(1)==AVG||input.LA(1)==COUNT||(input.LA(1) >= MAX && input.LA(1) <= MIN)||input.LA(1)==SUM ) {
                input.consume();
                adaptor.addChild(root_0, 
                (Object)adaptor.create(set58)
                );
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "aggFunc"


    public static class expression_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "expression"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:173:1: expression : orExpr ;
    public final CMSQueryParser.expression_return expression() throws RecognitionException {
        CMSQueryParser.expression_return retval = new CMSQueryParser.expression_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.orExpr_return orExpr59 =null;



        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:174:5: ( orExpr )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:174:10: orExpr
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_orExpr_in_expression1630);
            orExpr59=orExpr();

            state._fsp--;

            adaptor.addChild(root_0, orExpr59.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "expression"


    public static class orExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "orExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:177:1: orExpr : andExpr ( OR ^ andExpr )* ;
    public final CMSQueryParser.orExpr_return orExpr() throws RecognitionException {
        CMSQueryParser.orExpr_return retval = new CMSQueryParser.orExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token OR61=null;
        CMSQueryParser.andExpr_return andExpr60 =null;

        CMSQueryParser.andExpr_return andExpr62 =null;


        Object OR61_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:178:5: ( andExpr ( OR ^ andExpr )* )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:178:10: andExpr ( OR ^ andExpr )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_andExpr_in_orExpr1654);
            andExpr60=andExpr();

            state._fsp--;

            adaptor.addChild(root_0, andExpr60.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:178:18: ( OR ^ andExpr )*
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( (LA17_0==OR) ) {
                    alt17=1;
                }


                switch (alt17) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:178:19: OR ^ andExpr
            	    {
            	    OR61=(Token)match(input,OR,FOLLOW_OR_in_orExpr1657); 
            	    OR61_tree = 
            	    (Object)adaptor.create(OR61)
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(OR61_tree, root_0);


            	    pushFollow(FOLLOW_andExpr_in_orExpr1660);
            	    andExpr62=andExpr();

            	    state._fsp--;

            	    adaptor.addChild(root_0, andExpr62.getTree());

            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "orExpr"


    public static class andExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "andExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:181:1: andExpr : notExpr ( AND ^ notExpr )* ;
    public final CMSQueryParser.andExpr_return andExpr() throws RecognitionException {
        CMSQueryParser.andExpr_return retval = new CMSQueryParser.andExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token AND64=null;
        CMSQueryParser.notExpr_return notExpr63 =null;

        CMSQueryParser.notExpr_return notExpr65 =null;


        Object AND64_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:182:5: ( notExpr ( AND ^ notExpr )* )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:182:10: notExpr ( AND ^ notExpr )*
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_notExpr_in_andExpr1686);
            notExpr63=notExpr();

            state._fsp--;

            adaptor.addChild(root_0, notExpr63.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:182:18: ( AND ^ notExpr )*
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( (LA18_0==AND) ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:182:19: AND ^ notExpr
            	    {
            	    AND64=(Token)match(input,AND,FOLLOW_AND_in_andExpr1689); 
            	    AND64_tree = 
            	    (Object)adaptor.create(AND64)
            	    ;
            	    root_0 = (Object)adaptor.becomeRoot(AND64_tree, root_0);


            	    pushFollow(FOLLOW_notExpr_in_andExpr1692);
            	    notExpr65=notExpr();

            	    state._fsp--;

            	    adaptor.addChild(root_0, notExpr65.getTree());

            	    }
            	    break;

            	default :
            	    break loop18;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "andExpr"


    public static class notExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "notExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:185:1: notExpr : ( NOT ^)? primaryExpr ;
    public final CMSQueryParser.notExpr_return notExpr() throws RecognitionException {
        CMSQueryParser.notExpr_return retval = new CMSQueryParser.notExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token NOT66=null;
        CMSQueryParser.primaryExpr_return primaryExpr67 =null;


        Object NOT66_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:186:5: ( ( NOT ^)? primaryExpr )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:186:10: ( NOT ^)? primaryExpr
            {
            root_0 = (Object)adaptor.nil();


            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:186:10: ( NOT ^)?
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==NOT) ) {
                alt19=1;
            }
            switch (alt19) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:186:11: NOT ^
                    {
                    NOT66=(Token)match(input,NOT,FOLLOW_NOT_in_notExpr1719); 
                    NOT66_tree = 
                    (Object)adaptor.create(NOT66)
                    ;
                    root_0 = (Object)adaptor.becomeRoot(NOT66_tree, root_0);


                    }
                    break;

            }


            pushFollow(FOLLOW_primaryExpr_in_notExpr1724);
            primaryExpr67=primaryExpr();

            state._fsp--;

            adaptor.addChild(root_0, primaryExpr67.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "notExpr"


    public static class primaryExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "primaryExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:189:1: primaryExpr : ( LPARENT ! expression RPARENT !| compExpr | inExpr | regexExpr | existExpr | subQueryExpr | isnullExpr | isemptyExpr );
    public final CMSQueryParser.primaryExpr_return primaryExpr() throws RecognitionException {
        CMSQueryParser.primaryExpr_return retval = new CMSQueryParser.primaryExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LPARENT68=null;
        Token RPARENT70=null;
        CMSQueryParser.expression_return expression69 =null;

        CMSQueryParser.compExpr_return compExpr71 =null;

        CMSQueryParser.inExpr_return inExpr72 =null;

        CMSQueryParser.regexExpr_return regexExpr73 =null;

        CMSQueryParser.existExpr_return existExpr74 =null;

        CMSQueryParser.subQueryExpr_return subQueryExpr75 =null;

        CMSQueryParser.isnullExpr_return isnullExpr76 =null;

        CMSQueryParser.isemptyExpr_return isemptyExpr77 =null;


        Object LPARENT68_tree=null;
        Object RPARENT70_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:190:5: ( LPARENT ! expression RPARENT !| compExpr | inExpr | regexExpr | existExpr | subQueryExpr | isnullExpr | isemptyExpr )
            int alt20=8;
            alt20 = dfa20.predict(input);
            switch (alt20) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:190:10: LPARENT ! expression RPARENT !
                    {
                    root_0 = (Object)adaptor.nil();


                    LPARENT68=(Token)match(input,LPARENT,FOLLOW_LPARENT_in_primaryExpr1744); 

                    pushFollow(FOLLOW_expression_in_primaryExpr1747);
                    expression69=expression();

                    state._fsp--;

                    adaptor.addChild(root_0, expression69.getTree());

                    RPARENT70=(Token)match(input,RPARENT,FOLLOW_RPARENT_in_primaryExpr1749); 

                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:191:10: compExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_compExpr_in_primaryExpr1761);
                    compExpr71=compExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, compExpr71.getTree());

                    }
                    break;
                case 3 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:192:10: inExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_inExpr_in_primaryExpr1772);
                    inExpr72=inExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, inExpr72.getTree());

                    }
                    break;
                case 4 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:193:10: regexExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_regexExpr_in_primaryExpr1783);
                    regexExpr73=regexExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, regexExpr73.getTree());

                    }
                    break;
                case 5 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:194:10: existExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_existExpr_in_primaryExpr1794);
                    existExpr74=existExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, existExpr74.getTree());

                    }
                    break;
                case 6 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:195:10: subQueryExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_subQueryExpr_in_primaryExpr1806);
                    subQueryExpr75=subQueryExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, subQueryExpr75.getTree());

                    }
                    break;
                case 7 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:196:10: isnullExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_isnullExpr_in_primaryExpr1817);
                    isnullExpr76=isnullExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, isnullExpr76.getTree());

                    }
                    break;
                case 8 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:197:10: isemptyExpr
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_isemptyExpr_in_primaryExpr1828);
                    isemptyExpr77=isemptyExpr();

                    state._fsp--;

                    adaptor.addChild(root_0, isemptyExpr77.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "primaryExpr"


    public static class compExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "compExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:200:1: compExpr : attribute ( EQ | NE | GT | LT | GE | LE ) ^ value ;
    public final CMSQueryParser.compExpr_return compExpr() throws RecognitionException {
        CMSQueryParser.compExpr_return retval = new CMSQueryParser.compExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token set79=null;
        CMSQueryParser.attribute_return attribute78 =null;

        CMSQueryParser.value_return value80 =null;


        Object set79_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:201:5: ( attribute ( EQ | NE | GT | LT | GE | LE ) ^ value )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:201:10: attribute ( EQ | NE | GT | LT | GE | LE ) ^ value
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_attribute_in_compExpr1852);
            attribute78=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute78.getTree());

            set79=(Token)input.LT(1);

            set79=(Token)input.LT(1);

            if ( input.LA(1)==EQ||input.LA(1)==GE||input.LA(1)==GT||input.LA(1)==LE||input.LA(1)==LT||input.LA(1)==NE ) {
                input.consume();
                root_0 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(set79)
                , root_0);
                state.errorRecovery=false;
            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }


            pushFollow(FOLLOW_value_in_compExpr1879);
            value80=value();

            state._fsp--;

            adaptor.addChild(root_0, value80.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "compExpr"


    public static class inExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "inExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:204:1: inExpr : attribute IN ^ valueList ;
    public final CMSQueryParser.inExpr_return inExpr() throws RecognitionException {
        CMSQueryParser.inExpr_return retval = new CMSQueryParser.inExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token IN82=null;
        CMSQueryParser.attribute_return attribute81 =null;

        CMSQueryParser.valueList_return valueList83 =null;


        Object IN82_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:205:5: ( attribute IN ^ valueList )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:205:10: attribute IN ^ valueList
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_attribute_in_inExpr1903);
            attribute81=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute81.getTree());

            IN82=(Token)match(input,IN,FOLLOW_IN_in_inExpr1905); 
            IN82_tree = 
            (Object)adaptor.create(IN82)
            ;
            root_0 = (Object)adaptor.becomeRoot(IN82_tree, root_0);


            pushFollow(FOLLOW_valueList_in_inExpr1908);
            valueList83=valueList();

            state._fsp--;

            adaptor.addChild(root_0, valueList83.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "inExpr"


    public static class regexExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "regexExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:208:1: regexExpr : attribute REGEX ^ expValue ;
    public final CMSQueryParser.regexExpr_return regexExpr() throws RecognitionException {
        CMSQueryParser.regexExpr_return retval = new CMSQueryParser.regexExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token REGEX85=null;
        CMSQueryParser.attribute_return attribute84 =null;

        CMSQueryParser.expValue_return expValue86 =null;


        Object REGEX85_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:209:5: ( attribute REGEX ^ expValue )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:209:10: attribute REGEX ^ expValue
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_attribute_in_regexExpr1928);
            attribute84=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute84.getTree());

            REGEX85=(Token)match(input,REGEX,FOLLOW_REGEX_in_regexExpr1930); 
            REGEX85_tree = 
            (Object)adaptor.create(REGEX85)
            ;
            root_0 = (Object)adaptor.becomeRoot(REGEX85_tree, root_0);


            pushFollow(FOLLOW_expValue_in_regexExpr1933);
            expValue86=expValue();

            state._fsp--;

            adaptor.addChild(root_0, expValue86.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "regexExpr"


    public static class subQueryExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "subQueryExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:212:1: subQueryExpr : attribute SUBQUERY ^ query ;
    public final CMSQueryParser.subQueryExpr_return subQueryExpr() throws RecognitionException {
        CMSQueryParser.subQueryExpr_return retval = new CMSQueryParser.subQueryExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token SUBQUERY88=null;
        CMSQueryParser.attribute_return attribute87 =null;

        CMSQueryParser.query_return query89 =null;


        Object SUBQUERY88_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:213:5: ( attribute SUBQUERY ^ query )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:213:10: attribute SUBQUERY ^ query
            {
            root_0 = (Object)adaptor.nil();


            pushFollow(FOLLOW_attribute_in_subQueryExpr1957);
            attribute87=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute87.getTree());

            SUBQUERY88=(Token)match(input,SUBQUERY,FOLLOW_SUBQUERY_in_subQueryExpr1959); 
            SUBQUERY88_tree = 
            (Object)adaptor.create(SUBQUERY88)
            ;
            root_0 = (Object)adaptor.becomeRoot(SUBQUERY88_tree, root_0);


            pushFollow(FOLLOW_query_in_subQueryExpr1962);
            query89=query();

            state._fsp--;

            adaptor.addChild(root_0, query89.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "subQueryExpr"


    public static class existExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "existExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:216:1: existExpr : EXISTS ^ attribute ;
    public final CMSQueryParser.existExpr_return existExpr() throws RecognitionException {
        CMSQueryParser.existExpr_return retval = new CMSQueryParser.existExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token EXISTS90=null;
        CMSQueryParser.attribute_return attribute91 =null;


        Object EXISTS90_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:217:5: ( EXISTS ^ attribute )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:217:10: EXISTS ^ attribute
            {
            root_0 = (Object)adaptor.nil();


            EXISTS90=(Token)match(input,EXISTS,FOLLOW_EXISTS_in_existExpr1986); 
            EXISTS90_tree = 
            (Object)adaptor.create(EXISTS90)
            ;
            root_0 = (Object)adaptor.becomeRoot(EXISTS90_tree, root_0);


            pushFollow(FOLLOW_attribute_in_existExpr1989);
            attribute91=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute91.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "existExpr"


    public static class isnullExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "isnullExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:220:1: isnullExpr : ISNULL ^ attribute ;
    public final CMSQueryParser.isnullExpr_return isnullExpr() throws RecognitionException {
        CMSQueryParser.isnullExpr_return retval = new CMSQueryParser.isnullExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ISNULL92=null;
        CMSQueryParser.attribute_return attribute93 =null;


        Object ISNULL92_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:221:5: ( ISNULL ^ attribute )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:221:10: ISNULL ^ attribute
            {
            root_0 = (Object)adaptor.nil();


            ISNULL92=(Token)match(input,ISNULL,FOLLOW_ISNULL_in_isnullExpr2013); 
            ISNULL92_tree = 
            (Object)adaptor.create(ISNULL92)
            ;
            root_0 = (Object)adaptor.becomeRoot(ISNULL92_tree, root_0);


            pushFollow(FOLLOW_attribute_in_isnullExpr2016);
            attribute93=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute93.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "isnullExpr"


    public static class isemptyExpr_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "isemptyExpr"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:224:1: isemptyExpr : ISEMPTY ^ attribute ;
    public final CMSQueryParser.isemptyExpr_return isemptyExpr() throws RecognitionException {
        CMSQueryParser.isemptyExpr_return retval = new CMSQueryParser.isemptyExpr_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token ISEMPTY94=null;
        CMSQueryParser.attribute_return attribute95 =null;


        Object ISEMPTY94_tree=null;

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:225:5: ( ISEMPTY ^ attribute )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:225:10: ISEMPTY ^ attribute
            {
            root_0 = (Object)adaptor.nil();


            ISEMPTY94=(Token)match(input,ISEMPTY,FOLLOW_ISEMPTY_in_isemptyExpr2040); 
            ISEMPTY94_tree = 
            (Object)adaptor.create(ISEMPTY94)
            ;
            root_0 = (Object)adaptor.becomeRoot(ISEMPTY94_tree, root_0);


            pushFollow(FOLLOW_attribute_in_isemptyExpr2043);
            attribute95=attribute();

            state._fsp--;

            adaptor.addChild(root_0, attribute95.getTree());

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "isemptyExpr"


    public static class valueList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "valueList"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:228:1: valueList : LPARENT value ( COMMA value )* RPARENT -> ^( VALUE_LIST value ( value )* ) ;
    public final CMSQueryParser.valueList_return valueList() throws RecognitionException {
        CMSQueryParser.valueList_return retval = new CMSQueryParser.valueList_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token LPARENT96=null;
        Token COMMA98=null;
        Token RPARENT100=null;
        CMSQueryParser.value_return value97 =null;

        CMSQueryParser.value_return value99 =null;


        Object LPARENT96_tree=null;
        Object COMMA98_tree=null;
        Object RPARENT100_tree=null;
        RewriteRuleTokenStream stream_LPARENT=new RewriteRuleTokenStream(adaptor,"token LPARENT");
        RewriteRuleTokenStream stream_RPARENT=new RewriteRuleTokenStream(adaptor,"token RPARENT");
        RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
        RewriteRuleSubtreeStream stream_value=new RewriteRuleSubtreeStream(adaptor,"rule value");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:5: ( LPARENT value ( COMMA value )* RPARENT -> ^( VALUE_LIST value ( value )* ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:10: LPARENT value ( COMMA value )* RPARENT
            {
            LPARENT96=(Token)match(input,LPARENT,FOLLOW_LPARENT_in_valueList2063);  
            stream_LPARENT.add(LPARENT96);


            pushFollow(FOLLOW_value_in_valueList2065);
            value97=value();

            state._fsp--;

            stream_value.add(value97.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:24: ( COMMA value )*
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==COMMA) ) {
                    alt21=1;
                }


                switch (alt21) {
            	case 1 :
            	    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:25: COMMA value
            	    {
            	    COMMA98=(Token)match(input,COMMA,FOLLOW_COMMA_in_valueList2068);  
            	    stream_COMMA.add(COMMA98);


            	    pushFollow(FOLLOW_value_in_valueList2070);
            	    value99=value();

            	    state._fsp--;

            	    stream_value.add(value99.getTree());

            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);


            RPARENT100=(Token)match(input,RPARENT,FOLLOW_RPARENT_in_valueList2074);  
            stream_RPARENT.add(RPARENT100);


            // AST REWRITE
            // elements: value, value
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 229:47: -> ^( VALUE_LIST value ( value )* )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:50: ^( VALUE_LIST value ( value )* )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(VALUE_LIST, "VALUE_LIST")
                , root_1);

                adaptor.addChild(root_1, stream_value.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:229:69: ( value )*
                while ( stream_value.hasNext() ) {
                    adaptor.addChild(root_1, stream_value.nextTree());

                }
                stream_value.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "valueList"


    public static class value_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "value"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:232:1: value : ( intVal | doubleVal | strVal | boolVal | dateVal );
    public final CMSQueryParser.value_return value() throws RecognitionException {
        CMSQueryParser.value_return retval = new CMSQueryParser.value_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        CMSQueryParser.intVal_return intVal101 =null;

        CMSQueryParser.doubleVal_return doubleVal102 =null;

        CMSQueryParser.strVal_return strVal103 =null;

        CMSQueryParser.boolVal_return boolVal104 =null;

        CMSQueryParser.dateVal_return dateVal105 =null;



        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:233:5: ( intVal | doubleVal | strVal | boolVal | dateVal )
            int alt22=5;
            switch ( input.LA(1) ) {
            case INTEGER:
                {
                alt22=1;
                }
                break;
            case DECIMAL:
                {
                alt22=2;
                }
                break;
            case STRING:
                {
                alt22=3;
                }
                break;
            case BOOLEAN:
                {
                alt22=4;
                }
                break;
            case DATE:
                {
                alt22=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 22, 0, input);

                throw nvae;

            }

            switch (alt22) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:233:10: intVal
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_intVal_in_value2111);
                    intVal101=intVal();

                    state._fsp--;

                    adaptor.addChild(root_0, intVal101.getTree());

                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:234:10: doubleVal
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_doubleVal_in_value2123);
                    doubleVal102=doubleVal();

                    state._fsp--;

                    adaptor.addChild(root_0, doubleVal102.getTree());

                    }
                    break;
                case 3 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:235:10: strVal
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_strVal_in_value2134);
                    strVal103=strVal();

                    state._fsp--;

                    adaptor.addChild(root_0, strVal103.getTree());

                    }
                    break;
                case 4 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:236:10: boolVal
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_boolVal_in_value2145);
                    boolVal104=boolVal();

                    state._fsp--;

                    adaptor.addChild(root_0, boolVal104.getTree());

                    }
                    break;
                case 5 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:237:10: dateVal
                    {
                    root_0 = (Object)adaptor.nil();


                    pushFollow(FOLLOW_dateVal_in_value2156);
                    dateVal105=dateVal();

                    state._fsp--;

                    adaptor.addChild(root_0, dateVal105.getTree());

                    }
                    break;

            }
            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "value"


    public static class intVal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "intVal"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:240:1: intVal : INTEGER -> ^( INT_VAL INTEGER ) ;
    public final CMSQueryParser.intVal_return intVal() throws RecognitionException {
        CMSQueryParser.intVal_return retval = new CMSQueryParser.intVal_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token INTEGER106=null;

        Object INTEGER106_tree=null;
        RewriteRuleTokenStream stream_INTEGER=new RewriteRuleTokenStream(adaptor,"token INTEGER");

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:241:5: ( INTEGER -> ^( INT_VAL INTEGER ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:241:10: INTEGER
            {
            INTEGER106=(Token)match(input,INTEGER,FOLLOW_INTEGER_in_intVal2184);  
            stream_INTEGER.add(INTEGER106);


            // AST REWRITE
            // elements: INTEGER
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 241:18: -> ^( INT_VAL INTEGER )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:241:21: ^( INT_VAL INTEGER )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(INT_VAL, "INT_VAL")
                , root_1);

                adaptor.addChild(root_1, 
                stream_INTEGER.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "intVal"


    public static class doubleVal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "doubleVal"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:244:1: doubleVal : DECIMAL -> ^( DEC_VAL DECIMAL ) ;
    public final CMSQueryParser.doubleVal_return doubleVal() throws RecognitionException {
        CMSQueryParser.doubleVal_return retval = new CMSQueryParser.doubleVal_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token DECIMAL107=null;

        Object DECIMAL107_tree=null;
        RewriteRuleTokenStream stream_DECIMAL=new RewriteRuleTokenStream(adaptor,"token DECIMAL");

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:245:5: ( DECIMAL -> ^( DEC_VAL DECIMAL ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:245:10: DECIMAL
            {
            DECIMAL107=(Token)match(input,DECIMAL,FOLLOW_DECIMAL_in_doubleVal2215);  
            stream_DECIMAL.add(DECIMAL107);


            // AST REWRITE
            // elements: DECIMAL
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 245:18: -> ^( DEC_VAL DECIMAL )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:245:21: ^( DEC_VAL DECIMAL )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(DEC_VAL, "DEC_VAL")
                , root_1);

                adaptor.addChild(root_1, 
                stream_DECIMAL.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "doubleVal"


    public static class expValue_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "expValue"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:248:1: expValue : strVal ( REG_SENSITIVE | REG_INSENSITIVE )? -> ^( REG_VAL strVal ( REG_SENSITIVE )? ( REG_INSENSITIVE )? ) ;
    public final CMSQueryParser.expValue_return expValue() throws RecognitionException {
        CMSQueryParser.expValue_return retval = new CMSQueryParser.expValue_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token REG_SENSITIVE109=null;
        Token REG_INSENSITIVE110=null;
        CMSQueryParser.strVal_return strVal108 =null;


        Object REG_SENSITIVE109_tree=null;
        Object REG_INSENSITIVE110_tree=null;
        RewriteRuleTokenStream stream_REG_SENSITIVE=new RewriteRuleTokenStream(adaptor,"token REG_SENSITIVE");
        RewriteRuleTokenStream stream_REG_INSENSITIVE=new RewriteRuleTokenStream(adaptor,"token REG_INSENSITIVE");
        RewriteRuleSubtreeStream stream_strVal=new RewriteRuleSubtreeStream(adaptor,"rule strVal");
        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:5: ( strVal ( REG_SENSITIVE | REG_INSENSITIVE )? -> ^( REG_VAL strVal ( REG_SENSITIVE )? ( REG_INSENSITIVE )? ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:7: strVal ( REG_SENSITIVE | REG_INSENSITIVE )?
            {
            pushFollow(FOLLOW_strVal_in_expValue2244);
            strVal108=strVal();

            state._fsp--;

            stream_strVal.add(strVal108.getTree());

            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:15: ( REG_SENSITIVE | REG_INSENSITIVE )?
            int alt23=3;
            int LA23_0 = input.LA(1);

            if ( (LA23_0==REG_SENSITIVE) ) {
                alt23=1;
            }
            else if ( (LA23_0==REG_INSENSITIVE) ) {
                alt23=2;
            }
            switch (alt23) {
                case 1 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:16: REG_SENSITIVE
                    {
                    REG_SENSITIVE109=(Token)match(input,REG_SENSITIVE,FOLLOW_REG_SENSITIVE_in_expValue2248);  
                    stream_REG_SENSITIVE.add(REG_SENSITIVE109);


                    }
                    break;
                case 2 :
                    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:32: REG_INSENSITIVE
                    {
                    REG_INSENSITIVE110=(Token)match(input,REG_INSENSITIVE,FOLLOW_REG_INSENSITIVE_in_expValue2252);  
                    stream_REG_INSENSITIVE.add(REG_INSENSITIVE110);


                    }
                    break;

            }


            // AST REWRITE
            // elements: REG_SENSITIVE, strVal, REG_INSENSITIVE
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 249:50: -> ^( REG_VAL strVal ( REG_SENSITIVE )? ( REG_INSENSITIVE )? )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:53: ^( REG_VAL strVal ( REG_SENSITIVE )? ( REG_INSENSITIVE )? )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(REG_VAL, "REG_VAL")
                , root_1);

                adaptor.addChild(root_1, stream_strVal.nextTree());

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:70: ( REG_SENSITIVE )?
                if ( stream_REG_SENSITIVE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_REG_SENSITIVE.nextNode()
                    );

                }
                stream_REG_SENSITIVE.reset();

                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:249:85: ( REG_INSENSITIVE )?
                if ( stream_REG_INSENSITIVE.hasNext() ) {
                    adaptor.addChild(root_1, 
                    stream_REG_INSENSITIVE.nextNode()
                    );

                }
                stream_REG_INSENSITIVE.reset();

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "expValue"


    public static class strVal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "strVal"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:252:1: strVal : STRING -> ^( STR_VAL STRING ) ;
    public final CMSQueryParser.strVal_return strVal() throws RecognitionException {
        CMSQueryParser.strVal_return retval = new CMSQueryParser.strVal_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token STRING111=null;

        Object STRING111_tree=null;
        RewriteRuleTokenStream stream_STRING=new RewriteRuleTokenStream(adaptor,"token STRING");

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:253:5: ( STRING -> ^( STR_VAL STRING ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:253:10: STRING
            {
            STRING111=(Token)match(input,STRING,FOLLOW_STRING_in_strVal2293);  
            stream_STRING.add(STRING111);


            // AST REWRITE
            // elements: STRING
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 253:17: -> ^( STR_VAL STRING )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:253:20: ^( STR_VAL STRING )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(STR_VAL, "STR_VAL")
                , root_1);

                adaptor.addChild(root_1, 
                stream_STRING.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "strVal"


    public static class boolVal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "boolVal"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:256:1: boolVal : BOOLEAN -> ^( BOOL_VAL BOOLEAN ) ;
    public final CMSQueryParser.boolVal_return boolVal() throws RecognitionException {
        CMSQueryParser.boolVal_return retval = new CMSQueryParser.boolVal_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token BOOLEAN112=null;

        Object BOOLEAN112_tree=null;
        RewriteRuleTokenStream stream_BOOLEAN=new RewriteRuleTokenStream(adaptor,"token BOOLEAN");

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:257:5: ( BOOLEAN -> ^( BOOL_VAL BOOLEAN ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:257:10: BOOLEAN
            {
            BOOLEAN112=(Token)match(input,BOOLEAN,FOLLOW_BOOLEAN_in_boolVal2322);  
            stream_BOOLEAN.add(BOOLEAN112);


            // AST REWRITE
            // elements: BOOLEAN
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 257:18: -> ^( BOOL_VAL BOOLEAN )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:257:21: ^( BOOL_VAL BOOLEAN )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(BOOL_VAL, "BOOL_VAL")
                , root_1);

                adaptor.addChild(root_1, 
                stream_BOOLEAN.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "boolVal"


    public static class dateVal_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };


    // $ANTLR start "dateVal"
    // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:260:1: dateVal : DATE LPARENT INTEGER RPARENT -> ^( DATE_VAL INTEGER ) ;
    public final CMSQueryParser.dateVal_return dateVal() throws RecognitionException {
        CMSQueryParser.dateVal_return retval = new CMSQueryParser.dateVal_return();
        retval.start = input.LT(1);


        Object root_0 = null;

        Token DATE113=null;
        Token LPARENT114=null;
        Token INTEGER115=null;
        Token RPARENT116=null;

        Object DATE113_tree=null;
        Object LPARENT114_tree=null;
        Object INTEGER115_tree=null;
        Object RPARENT116_tree=null;
        RewriteRuleTokenStream stream_INTEGER=new RewriteRuleTokenStream(adaptor,"token INTEGER");
        RewriteRuleTokenStream stream_LPARENT=new RewriteRuleTokenStream(adaptor,"token LPARENT");
        RewriteRuleTokenStream stream_RPARENT=new RewriteRuleTokenStream(adaptor,"token RPARENT");
        RewriteRuleTokenStream stream_DATE=new RewriteRuleTokenStream(adaptor,"token DATE");

        try {
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:261:5: ( DATE LPARENT INTEGER RPARENT -> ^( DATE_VAL INTEGER ) )
            // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:261:10: DATE LPARENT INTEGER RPARENT
            {
            DATE113=(Token)match(input,DATE,FOLLOW_DATE_in_dateVal2355);  
            stream_DATE.add(DATE113);


            LPARENT114=(Token)match(input,LPARENT,FOLLOW_LPARENT_in_dateVal2357);  
            stream_LPARENT.add(LPARENT114);


            INTEGER115=(Token)match(input,INTEGER,FOLLOW_INTEGER_in_dateVal2359);  
            stream_INTEGER.add(INTEGER115);


            RPARENT116=(Token)match(input,RPARENT,FOLLOW_RPARENT_in_dateVal2361);  
            stream_RPARENT.add(RPARENT116);


            // AST REWRITE
            // elements: INTEGER
            // token labels: 
            // rule labels: retval
            // token list labels: 
            // rule list labels: 
            // wildcard labels: 
            retval.tree = root_0;
            RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.tree:null);

            root_0 = (Object)adaptor.nil();
            // 261:39: -> ^( DATE_VAL INTEGER )
            {
                // /ebay/repositories/cms.open/cms-core/query/src/main/antlr3/com/ebay/cloud/cms/query/parser/antlr/CMSQuery.g:261:42: ^( DATE_VAL INTEGER )
                {
                Object root_1 = (Object)adaptor.nil();
                root_1 = (Object)adaptor.becomeRoot(
                (Object)adaptor.create(DATE_VAL, "DATE_VAL")
                , root_1);

                adaptor.addChild(root_1, 
                stream_INTEGER.nextNode()
                );

                adaptor.addChild(root_0, root_1);
                }

            }


            retval.tree = root_0;

            }

            retval.stop = input.LT(-1);


            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }

            catch (RecognitionException e)
            {
                throw e;
            }

        finally {
        	// do for sure before leaving
        }
        return retval;
    }
    // $ANTLR end "dateVal"

    // Delegated rules


    protected DFA20 dfa20 = new DFA20(this);
    static final String DFA20_eotS =
        "\27\uffff";
    static final String DFA20_eofS =
        "\27\uffff";
    static final String DFA20_minS =
        "\1\7\1\uffff\1\31\1\10\3\uffff\1\22\1\45\1\21\4\uffff\1\7\2\31\2"+
        "\22\1\33\1\21\1\31\1\33";
    static final String DFA20_maxS =
        "\1\45\1\uffff\1\31\1\75\3\uffff\1\74\1\45\1\21\4\uffff\1\67\2\31"+
        "\2\74\1\67\1\21\1\31\1\67";
    static final String DFA20_acceptS =
        "\1\uffff\1\1\2\uffff\1\5\1\7\1\10\3\uffff\1\2\1\3\1\4\1\6\11\uffff";
    static final String DFA20_specialS =
        "\27\uffff}>";
    static final String[] DFA20_transitionS = {
            "\1\2\11\uffff\1\3\2\uffff\1\4\12\uffff\1\6\1\5\4\uffff\1\1",
            "",
            "\1\7",
            "\1\10\3\uffff\1\10\32\uffff\2\10\24\uffff\1\10",
            "",
            "",
            "",
            "\1\12\3\uffff\1\12\1\uffff\1\12\1\uffff\1\13\1\11\7\uffff\1"+
            "\12\2\uffff\1\12\2\uffff\1\12\7\uffff\1\14\12\uffff\1\15",
            "\1\16",
            "\1\17",
            "",
            "",
            "",
            "",
            "\1\20\57\uffff\1\21",
            "\1\22",
            "\1\23",
            "\1\12\3\uffff\1\12\1\uffff\1\12\1\uffff\1\13\10\uffff\1\12"+
            "\2\uffff\1\12\2\uffff\1\12\7\uffff\1\14\12\uffff\1\15",
            "\1\12\3\uffff\1\12\1\uffff\1\12\1\uffff\1\13\1\11\7\uffff\1"+
            "\12\2\uffff\1\12\2\uffff\1\12\7\uffff\1\14\12\uffff\1\15",
            "\1\24\33\uffff\1\21",
            "\1\25",
            "\1\26",
            "\1\24\33\uffff\1\21"
    };

    static final short[] DFA20_eot = DFA.unpackEncodedString(DFA20_eotS);
    static final short[] DFA20_eof = DFA.unpackEncodedString(DFA20_eofS);
    static final char[] DFA20_min = DFA.unpackEncodedStringToUnsignedChars(DFA20_minS);
    static final char[] DFA20_max = DFA.unpackEncodedStringToUnsignedChars(DFA20_maxS);
    static final short[] DFA20_accept = DFA.unpackEncodedString(DFA20_acceptS);
    static final short[] DFA20_special = DFA.unpackEncodedString(DFA20_specialS);
    static final short[][] DFA20_transition;

    static {
        int numStates = DFA20_transitionS.length;
        DFA20_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA20_transition[i] = DFA.unpackEncodedString(DFA20_transitionS[i]);
        }
    }

    class DFA20 extends DFA {

        public DFA20(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 20;
            this.eot = DFA20_eot;
            this.eof = DFA20_eof;
            this.min = DFA20_min;
            this.max = DFA20_max;
            this.accept = DFA20_accept;
            this.special = DFA20_special;
            this.transition = DFA20_transition;
        }
        public String getDescription() {
            return "189:1: primaryExpr : ( LPARENT ! expression RPARENT !| compExpr | inExpr | regexExpr | existExpr | subQueryExpr | isnullExpr | isemptyExpr );";
        }
    }
 

    public static final BitSet FOLLOW_query_in_start_rule908 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_path_in_query925 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_unionPath_in_path955 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_intersectionPath_in_unionPath975 = new BitSet(new long[]{0x8000000000000002L});
    public static final BitSet FOLLOW_UNION_in_unionPath978 = new BitSet(new long[]{0x0000006002000000L});
    public static final BitSet FOLLOW_intersectionPath_in_unionPath981 = new BitSet(new long[]{0x8000000000000002L});
    public static final BitSet FOLLOW_primaryPath_in_intersectionPath1003 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_INTERSECTION_in_intersectionPath1006 = new BitSet(new long[]{0x0000006002000000L});
    public static final BitSet FOLLOW_primaryPath_in_intersectionPath1009 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_LPARENT_in_primaryPath1034 = new BitSet(new long[]{0x0000006002000000L});
    public static final BitSet FOLLOW_path_in_primaryPath1037 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_RPARENT_in_primaryPath1039 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_queryNode_in_primaryPath1050 = new BitSet(new long[]{0x0040001008000002L});
    public static final BitSet FOLLOW_set_in_primaryPath1061 = new BitSet(new long[]{0x0000006002000000L});
    public static final BitSet FOLLOW_primaryPath_in_primaryPath1070 = new BitSet(new long[]{0x0040001008000002L});
    public static final BitSet FOLLOW_typeCast_in_queryNode1097 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_nodeName_in_queryNode1101 = new BitSet(new long[]{0x0000004600000002L});
    public static final BitSet FOLLOW_filter_in_queryNode1104 = new BitSet(new long[]{0x0000004200000002L});
    public static final BitSet FOLLOW_aggregation_in_queryNode1109 = new BitSet(new long[]{0x0000000200000002L});
    public static final BitSet FOLLOW_projection_in_queryNode1114 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_nodeName1159 = new BitSet(new long[]{0x0020000000000002L});
    public static final BitSet FOLLOW_REVERSE_in_nodeName1162 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_ID_in_nodeName1165 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_typeCast1187 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_typeName_in_typeCast1189 = new BitSet(new long[]{0x0000000001000800L});
    public static final BitSet FOLLOW_COMMA_in_typeCast1192 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_typeName_in_typeCast1194 = new BitSet(new long[]{0x0000000001000800L});
    public static final BitSet FOLLOW_GT_in_typeCast1198 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_typeName1235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_group_in_aggregation1259 = new BitSet(new long[]{0x0000000400000002L});
    public static final BitSet FOLLOW_filter_in_aggregation1261 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LT_in_group1297 = new BitSet(new long[]{0x0000000000020080L});
    public static final BitSet FOLLOW_attributeList_in_group1299 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_GT_in_group1301 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACKET_in_filter1333 = new BitSet(new long[]{0x0000042180120080L});
    public static final BitSet FOLLOW_expression_in_filter1335 = new BitSet(new long[]{0x0001000000000000L});
    public static final BitSet FOLLOW_RBRACKET_in_filter1337 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LBRACE_in_projection1373 = new BitSet(new long[]{0x0200000000020080L});
    public static final BitSet FOLLOW_STAR_in_projection1376 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_attributeList_in_projection1380 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_RBRACE_in_projection1383 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_attributeList1417 = new BitSet(new long[]{0x0000000000000802L});
    public static final BitSet FOLLOW_COMMA_in_attributeList1420 = new BitSet(new long[]{0x0000000000020080L});
    public static final BitSet FOLLOW_attribute_in_attributeList1423 = new BitSet(new long[]{0x0000000000000802L});
    public static final BitSet FOLLOW_searchAttr_in_attribute1449 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_aggregateAttr_in_attribute1453 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_AT_in_searchAttr1477 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_attrName_in_searchAttr1479 = new BitSet(new long[]{0x0000000008000002L});
    public static final BitSet FOLLOW_INNER_JOIN_in_searchAttr1482 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_DOLLAR_in_searchAttr1484 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_attrName_in_searchAttr1486 = new BitSet(new long[]{0x0000000008000002L});
    public static final BitSet FOLLOW_DOLLAR_in_aggregateAttr1530 = new BitSet(new long[]{0x2000018000001100L});
    public static final BitSet FOLLOW_aggFunc_in_aggregateAttr1532 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_LPARENT_in_aggregateAttr1534 = new BitSet(new long[]{0x0080000000000080L});
    public static final BitSet FOLLOW_searchAttr_in_aggregateAttr1536 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_RPARENT_in_aggregateAttr1539 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_attrName1574 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_orExpr_in_expression1630 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_andExpr_in_orExpr1654 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_OR_in_orExpr1657 = new BitSet(new long[]{0x0000042180120080L});
    public static final BitSet FOLLOW_andExpr_in_orExpr1660 = new BitSet(new long[]{0x0000080000000002L});
    public static final BitSet FOLLOW_notExpr_in_andExpr1686 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_AND_in_andExpr1689 = new BitSet(new long[]{0x0000042180120080L});
    public static final BitSet FOLLOW_notExpr_in_andExpr1692 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_NOT_in_notExpr1719 = new BitSet(new long[]{0x0000002180120080L});
    public static final BitSet FOLLOW_primaryExpr_in_notExpr1724 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPARENT_in_primaryExpr1744 = new BitSet(new long[]{0x0000042180120080L});
    public static final BitSet FOLLOW_expression_in_primaryExpr1747 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_RPARENT_in_primaryExpr1749 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_compExpr_in_primaryExpr1761 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_inExpr_in_primaryExpr1772 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_regexExpr_in_primaryExpr1783 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_existExpr_in_primaryExpr1794 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_subQueryExpr_in_primaryExpr1806 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isnullExpr_in_primaryExpr1817 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isemptyExpr_in_primaryExpr1828 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_compExpr1852 = new BitSet(new long[]{0x0000024801440000L});
    public static final BitSet FOLLOW_set_in_compExpr1854 = new BitSet(new long[]{0x040000001000A200L});
    public static final BitSet FOLLOW_value_in_compExpr1879 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_inExpr1903 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_IN_in_inExpr1905 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_valueList_in_inExpr1908 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_regexExpr1928 = new BitSet(new long[]{0x0002000000000000L});
    public static final BitSet FOLLOW_REGEX_in_regexExpr1930 = new BitSet(new long[]{0x0400000000000000L});
    public static final BitSet FOLLOW_expValue_in_regexExpr1933 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_attribute_in_subQueryExpr1957 = new BitSet(new long[]{0x1000000000000000L});
    public static final BitSet FOLLOW_SUBQUERY_in_subQueryExpr1959 = new BitSet(new long[]{0x0000006002000000L});
    public static final BitSet FOLLOW_query_in_subQueryExpr1962 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EXISTS_in_existExpr1986 = new BitSet(new long[]{0x0000000000020080L});
    public static final BitSet FOLLOW_attribute_in_existExpr1989 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ISNULL_in_isnullExpr2013 = new BitSet(new long[]{0x0000000000020080L});
    public static final BitSet FOLLOW_attribute_in_isnullExpr2016 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ISEMPTY_in_isemptyExpr2040 = new BitSet(new long[]{0x0000000000020080L});
    public static final BitSet FOLLOW_attribute_in_isemptyExpr2043 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_LPARENT_in_valueList2063 = new BitSet(new long[]{0x040000001000A200L});
    public static final BitSet FOLLOW_value_in_valueList2065 = new BitSet(new long[]{0x0080000000000800L});
    public static final BitSet FOLLOW_COMMA_in_valueList2068 = new BitSet(new long[]{0x040000001000A200L});
    public static final BitSet FOLLOW_value_in_valueList2070 = new BitSet(new long[]{0x0080000000000800L});
    public static final BitSet FOLLOW_RPARENT_in_valueList2074 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_intVal_in_value2111 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_doubleVal_in_value2123 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_strVal_in_value2134 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolVal_in_value2145 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_dateVal_in_value2156 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_INTEGER_in_intVal2184 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DECIMAL_in_doubleVal2215 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_strVal_in_expValue2244 = new BitSet(new long[]{0x000C000000000002L});
    public static final BitSet FOLLOW_REG_SENSITIVE_in_expValue2248 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_REG_INSENSITIVE_in_expValue2252 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_strVal2293 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_BOOLEAN_in_boolVal2322 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_DATE_in_dateVal2355 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_LPARENT_in_dateVal2357 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_INTEGER_in_dateVal2359 = new BitSet(new long[]{0x0080000000000000L});
    public static final BitSet FOLLOW_RPARENT_in_dateVal2361 = new BitSet(new long[]{0x0000000000000002L});

}