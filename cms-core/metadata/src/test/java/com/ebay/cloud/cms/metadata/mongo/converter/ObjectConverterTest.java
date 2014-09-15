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

package com.ebay.cloud.cms.metadata.mongo.converter;

import junit.framework.Assert;

import org.junit.Test;

import com.mongodb.DBObject;



public class ObjectConverterTest {
    private static class TestClass {
        int i;
        String s;
        
        public int getI() {
            return i;
        }
        public void setI(int i) {
            this.i = i;
        }
        public String getS() {
            return s;
        }
        public void setS(String s) {
            this.s = s;
        }
    }
    
    @Test
    public void testToJson() {
        ObjectConverter<TestClass> c = new ObjectConverter<ObjectConverterTest.TestClass>();
        TestClass t = new TestClass();
        t.setI(10);
        t.setS("str");
        
        String json = c.toJson(t);
        
        TestClass t1 = c.fromJson(json, TestClass.class);
        
        Assert.assertEquals(t.getI(), t1.getI());
        Assert.assertEquals(t.getS(), t1.getS());
    }
    
    @Test
    public void testToBson() {
        ObjectConverter<TestClass> c = new ObjectConverter<ObjectConverterTest.TestClass>();
        TestClass t = new TestClass();
        t.setI(10);
        t.setS("str");
        
        DBObject bson = c.toBson(t);
        
        TestClass t1 = c.fromBson(bson, TestClass.class);
        
        Assert.assertEquals(t.getI(), t1.getI());
        Assert.assertEquals(t.getS(), t1.getS());
    }
    
}
