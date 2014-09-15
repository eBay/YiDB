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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;


/**
 * 
 * @author xjiang
 * 
 */
public class ParseLinkNode extends ParseBaseNode {

    public ParseLinkNode(int lexerType) {
        super(lexerType);
    }

    protected void doValidate(ParseContext context) {
        // FIXME : check children number - 1 for join & 2 for set
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LinkNode[type= ").append(getType().name()).append("]");
        return sb.toString();
    }

    @Override
    public Set<String> checkSetMetaClasses() {
        if (getType() == ParseNodeTypeEnum.INNERJOIN
                || getType() == ParseNodeTypeEnum.LEFTJOIN) {
            return Collections.emptySet();
        }
        
        boolean isUnion = false;
        if (getType() == ParseNodeTypeEnum.INTERSECTION) {
            isUnion = false;
        } else if (getType() == ParseNodeTypeEnum.UNION) {
            isUnion = true;
        }
        
        Set<String> metaSet = new HashSet<String>();
        for (ParseBaseNode nextNode : getNextNodes()) {
            Set<String> metas = nextNode.checkSetMetaClasses();
            if (isUnion) {
                metaSet.addAll(metas);
            } else {
                metaSet.addAll(metas);
                if (metaSet.size() > 1) {
                    throw new QueryParseException(QueryErrCodeEnum.INTERSECTION_ON_DIFFERENT_ROOT_METACLASS, "Root Intersection on different meta classes");
                }
            }
        }
        
        return metaSet;

    }

}
