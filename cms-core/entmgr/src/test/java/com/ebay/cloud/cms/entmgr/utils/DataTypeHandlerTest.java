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

package com.ebay.cloud.cms.entmgr.utils;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.datahandler.IDataTypeHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonBooleanHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonDateHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonDoubleHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonEnumHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonIntegerHandler;
import com.ebay.cloud.cms.dal.entity.json.datahandler.JsonLongHandler;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;

/**
 * 
 * @author jianxu1
 * @date   2012/5/16
 * @history
 */
public class DataTypeHandlerTest {

	private static final double ep = 1e-10;

    @Test
    public void testInteger(){
         IDataTypeHandler handler = new JsonIntegerHandler();
         MetaField metaField = new MetaAttribute();
         metaField.setName("IntegerField");
         Integer value = new Integer("123");
         JsonNode writeNode = (JsonNode) handler.write(null, value, metaField);
         Integer readValue = (Integer) handler.read(null, writeNode, metaField);
         assertEquals(readValue, value);
         
         JsonNode nullWriteNode = (JsonNode) handler.write(null, null, metaField);
         Object nullReadValue = handler.read(null, nullWriteNode, metaField);
         Assert.assertNull(nullReadValue);
         
         String strValue = "invalid";
         JsonNode strValueNode = JsonNodeFactory.instance.textNode(strValue);
         try {
             handler.read(null, strValueNode, metaField);
             Assert.fail();
         } catch(IllegalArgumentException e) {

         }
    }
    
    @Test
    public void testBoolean(){
         IDataTypeHandler handler = new JsonBooleanHandler();
         MetaField metaField = new MetaAttribute();
         metaField.setName("BooleanField");
         Boolean value = new Boolean("true");
         JsonNode writeNode = (JsonNode) handler.write(null, value, metaField);
         Boolean readValue = (Boolean) handler.read(null, writeNode, metaField);
         assertEquals(readValue, value);
         
         JsonNode nullWriteNode = (JsonNode) handler.write(null, null, metaField);
         Object nullReadValue = handler.read(null, nullWriteNode, metaField);
         Assert.assertNull(nullReadValue);
         
         String strValue = "invalid";
         JsonNode strValueNode = JsonNodeFactory.instance.textNode(strValue);
         try {
             handler.read(null, strValueNode, metaField);
             Assert.fail();
         } catch(IllegalArgumentException e) {

         }
    }
    
    @Test
    public void testEnum(){
         IDataTypeHandler handler = new JsonEnumHandler();
         MetaAttribute metaField = new MetaAttribute();
         metaField.setName("EnumField");
         metaField.addEnumValue("enumtype");
         String value = new String("enumtype");
         JsonNode writeNode = (JsonNode) handler.write(null, value, metaField);
         String readValue = (String) handler.read(null, writeNode, metaField);
         assertEquals(readValue, value);
         
         JsonNode nullWriteNode = (JsonNode) handler.write(null, null, metaField);
         Object nullReadValue = handler.read(null, nullWriteNode, metaField);
         Assert.assertNull(nullReadValue);
    }
    
	@Test
    public void testLong(){
         IDataTypeHandler handler = new JsonLongHandler();
         MetaField metaField = new MetaAttribute();
         metaField.setName("LongField");
         Long value = new Long("2147483678");
         JsonNode valueNode = JsonNodeFactory.instance.numberNode(value);
         Long readValue = (Long) handler.read(null, valueNode, metaField);
         assertEquals(readValue, value);
         
         String strValue = "invalid";
         JsonNode strValueNode = JsonNodeFactory.instance.textNode(strValue);
         try {
             handler.read(null, strValueNode, metaField);
             Assert.fail();
         } catch(IllegalArgumentException e) {

         }
         
         JsonNode nullWriteNode = (JsonNode) handler.write(null, null, metaField);
         Object nullReadValue = handler.read(null, nullWriteNode, metaField);
         Assert.assertNull(nullReadValue);
    }
    
	@Test
	public void testDouble(){
        IDataTypeHandler handler = new JsonDoubleHandler();
        MetaField metaField = new MetaAttribute();
        metaField.setName("DoubleField");
        Double value = new Double("123.456");
        JsonNode writeNode = (JsonNode) handler.write(null, value, metaField);
        Double readValue = (Double) handler.read(null, writeNode, metaField);
        assertEquals(readValue, value, ep);
		 
        JsonNode nullWriteNode = (JsonNode) handler.write(null, null, metaField);
        Object nullReadValue = handler.read(null, nullWriteNode, metaField);
        Assert.assertNull(nullReadValue);
        
        String strValue = "invalid";
        JsonNode strValueNode = JsonNodeFactory.instance.textNode(strValue);
        try {
            handler.read(null, strValueNode, metaField);
            Assert.fail();
        } catch(IllegalArgumentException e) {

        }
	}
	
	@Test
	public void testDate(){
		IDataTypeHandler handler = new JsonDateHandler();
		MetaField metaField = new MetaAttribute();
		metaField.setName("DateField");
		Date value = new Date();
		JsonNode valueNode = JsonNodeFactory.instance.numberNode(value.getTime());
		Date readValue = (Date) handler.read(null, valueNode, metaField);
		assertEquals(value, readValue);
		
		valueNode = JsonNodeFactory.instance.numberNode(1);
		handler.read(null, valueNode, metaField);
		
        String strValue = "invalid";
        JsonNode strValueNode = JsonNodeFactory.instance.textNode(strValue);
        try {
            handler.read(null, strValueNode, metaField);
            Assert.fail();
        } catch(IllegalArgumentException e) {

        }
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testDateWithException1(){
		IDataTypeHandler handler = new JsonDateHandler();
		MetaField metaField = new MetaAttribute();
		metaField.setName("DateField");
		
		JsonNode valueNode = JsonNodeFactory.instance.textNode("wrong value");
		handler.read(null, valueNode, metaField);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testDateWithException2(){
		IDataTypeHandler handler = new JsonDateHandler();
		MetaField metaField = new MetaAttribute();
		metaField.setName("DateField");
		
		JsonNode valueNode = JsonNodeFactory.instance.numberNode(123.456);
		handler.read(null, valueNode, metaField);
	}
}
