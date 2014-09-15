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

package com.ebay.cloud.cms.query.parser.antlr;

import junit.framework.Assert;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMSQueryParserTest {

    private static final Logger logger        = LoggerFactory.getLogger(CMSQueryParserTest.class);

    static String[]             setQueries    = new String[] {
            "ApplicationService{*}.( service{*} && updateStrategy{*})",
            "ApplicationService{*}.( service{*} || updateStrategy{*})",
            "ApplicationService{*}.( service{*} && ( updateStrategy{*} ))",
            "ApplicationService{*}.service{*}.(runsOn.abc && (updateStrategy{*}) )", 
            "A.<b,c>d",
            "QueryCaseSensitiveMatchParentTest[@name=~\"queryCaseSensitiveMatchParentTest-name\"]{@name,@label}"
    };

    static String[] reverseTraversal = new String[] {
        /*
         * A<-a-B
         */
        "A.a!B",
        /*
         * A->B<-C->D
         */
        "A.b.b!C.d",
        /*
         *      B
         *      ^
         *      |
         *  A <-X->C
         */
        "X.(a && b && c)",
        /*
         *           B
         *           |
         *           V
         *       A-> X <- C
         *           |
         *          \/
         *           e
         */
        "X.(x1!A && x2!B && x3!C && e)"
    };

    static String[]             badSetQueries = new String[] {
            "ApplicationService{*}.( (service{*} & updateStrategy{*}) || (runsOn{*}))",
            "ApplicationService{*}.( service{*} updateStrategy{*})",
//            "ApplicationService{*}.service{*}.( runsOn.abc && (updateStrategy{*}) ).abc",
    };

    @Test
    public void testSetQuery() throws Exception {
        for (String q : setQueries) {
            logger.debug("parsing {}", q);
            parse(q);
        }
    }

    @Test
    public void testBadQuery() throws Exception {
        for (String q : badSetQueries) {
            logger.debug("parsing {}", q);
            try {
                parse(q);
                Assert.fail();
            } catch (Exception e) {
                // expected
            }
        }
    }

    private CommonTree parse(String q) throws RecognitionException {
        ANTLRStringStream strStream = new ANTLRStringStream(q);
        CMSQueryLexer lexer = new CMSQueryLexer(strStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        CMSQueryParser parser = new CMSQueryParser(tokenStream);
        return (CommonTree) parser.query().getTree();
    }
}
