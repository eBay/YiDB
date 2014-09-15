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


/*******************************************************************************
 * Copyright (c) 2012-2013 eBay Inc.
 * All rights reserved. 
 *  
 * eBay PE Cloud Foundation Team [DL-eBay-SHA-COE-PE-Cloud-Foundation@ebay.com]
 *******************************************************************************/
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

package com.ebay.cloud.cms.metadata.model;

import java.util.Random;

import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

/**
 * @author shuachen
 * 
 *         2013-11-27
 */
public class IndexInfoTest extends CMSMongoTest {

	@Test(expected = IllegalIndexException.class)
	public void testValidate_indexName1() {
		IndexInfo idx = new IndexInfo(" ");
		idx.validate();
	}
	
	@Test(expected = IllegalIndexException.class)
	public void testValidate_indexName2() {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i <= CMSConsts.MAX_LENGTH_OF_INDEX_NAME; i++) {
			sb.append("a");
		}
		String indexName = sb.toString();
		IndexInfo idx = new IndexInfo(indexName);
		idx.validate();
	}
	
	@Test(expected = IllegalIndexException.class)
	public void testValidate_indexName3() {
		char[] invalidBeginChars = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')'}; 
		Random randomGenerator = new Random();
		
		String indexName = String.valueOf(invalidBeginChars[randomGenerator.nextInt(invalidBeginChars.length)]);
		IndexInfo idx = new IndexInfo(indexName);
		idx.validate();
	}
}
