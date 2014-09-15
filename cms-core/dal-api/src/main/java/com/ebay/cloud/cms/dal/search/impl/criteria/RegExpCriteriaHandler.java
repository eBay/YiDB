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

package com.ebay.cloud.cms.dal.search.impl.criteria;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.RegexValue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * regular expression field handler
 * 
 * @author xjiang
 *
 */
public class RegExpCriteriaHandler extends AbstractFieldCriteriaHandler {

    @Override
    public DBObject translate(final FieldSearchCriteria criteria) { 
        DBObject query = new BasicDBObject();
        String fullDbName = criteria.getFullDbName();
        Pattern regex = getRegPattern(criteria);
        FieldOperatorEnum op = criteria.getOperator();
        
        switch (op) {
            case REGEX:
                BasicDBObject dbo = new BasicDBObject();
                dbo.put("$regex", regex);
                query.put(fullDbName, dbo);
                break;
            case NREGEX:
                query.put(fullDbName, new BasicDBObject("$not", regex));
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }
        return query;
    }

    @Override
    public boolean evaluate(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        boolean evalRes = false;
        FieldOperatorEnum op = criteria.getOperator();
        switch (op) {
            case REGEX:
                evalRes = evalRegExp(criteria, fieldValues);
                break;
            case NREGEX:
                evalRes = !evalRegExp(criteria, fieldValues);
                break;
            default:
                throw new IllegalArgumentException("Unsupport comparision operator: " + op);
        }            
        return evalRes;
    }
    
    private boolean evalRegExp(final FieldSearchCriteria criteria, final List<?> fieldValues) {
        for (Object fieldValue : fieldValues) {
            if (fieldValue == null || !singleRegex(criteria, fieldValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean singleRegex(final FieldSearchCriteria criteria, final Object fieldValue) {
        Pattern pattern = getRegPattern(criteria);
        Matcher matcher = pattern.matcher(fieldValue.toString());
        return matcher.matches();
    }

    private Pattern getRegPattern(final FieldSearchCriteria criteria) {
        RegexValue regValue = (RegexValue) criteria.getValue();
        Pattern pattern = null;
        try {
	        if (regValue.caseSensitive) {
	            pattern = Pattern.compile(regValue.value);
	        } else {
	            pattern = Pattern.compile(regValue.value, Pattern.CASE_INSENSITIVE);
	        }
        } catch (PatternSyntaxException pse) {
        	throw new CmsDalException(DalErrCodeEnum.INVALID_REGEXPRESSION, "Found invalid RegExpression: " + regValue.value);
        }
        return pattern;
    }

    @Override
    protected boolean useArrayElemMatch() {
        return true;
    }
}
