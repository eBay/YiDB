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


/**
 * 
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

package com.ebay.cloud.cms.metadata.mongo.converter;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.model.MetaClass;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public class MetaIndexConverterTest {

    private ObjectConverter<MetaClass> c = new ObjectConverter<MetaClass>();

    @Test
    public void testNoIndexNameNode() throws JsonProcessingException, IOException {
        String json = "{\"name\":\"meta\", \"options\" : { \"indexes\": {\"index1\": { \"keyList\": [], \"indexOptions\" : [] }} }}";
        c.fromJson(json, MetaClass.class);
    }

    @Test(expected = MetaDataException.class)
    public void testNameInconsistent() throws JsonProcessingException, IOException {
        String json = "{\"name\":\"meta\", \"options\": { \"indexes\": {\"index1\" : { \"indexName\": \"index2\", \"keyList\": [], \"indexOptions\" : [] }} }}";
        c.fromJson(json, MetaClass.class);
    }

    @Test(expected = MetaDataException.class)
    public void testNoIndexNameNode2() throws JsonProcessingException, IOException {
        String json = "{\"name\":\"meta\", \"options\" : { \"indexes\": { \"\": { \"keyList\": [], \"indexOptions\" : [] }} }}";
        c.fromJson(json, MetaClass.class);
    }
    

}
