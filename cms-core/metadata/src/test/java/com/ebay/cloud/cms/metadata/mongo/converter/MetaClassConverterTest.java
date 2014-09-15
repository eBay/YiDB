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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.FileUtils;

public class MetaClassConverterTest {
    
    private ObjectConverter<MetaClass> c = new ObjectConverter<MetaClass>();

    @Test
    public void testMetaClassConvert() {
        
        MetaClass m = new MetaClass();
        m.setName("name");
        m.setDescription("description");
        m.setLastModified(new Date());
        m.setId(new ObjectId().toString());
        
        MetaAttribute a = new MetaAttribute();
        a.setName("a");
        a.setDbName("f_a");
        a.setDataType(MetaField.DataTypeEnum.INTEGER);
        a.setCardinality(MetaField.CardinalityEnum.One);
        
        MetaRelationship r = new MetaRelationship();
        r.setName("r");
        r.setDbName("f_r");
        r.setDataType(MetaField.DataTypeEnum.RELATIONSHIP);
        r.setCardinality(MetaField.CardinalityEnum.Many);
        r.setRelationType(MetaRelationship.RelationTypeEnum.Embedded);
        m.addField(r);
        m.addField(a);
        
        ObjectConverter<MetaClass> c = new ObjectConverter<MetaClass>();
        String json = c.toJson(m);
        
        MetaClass m1 = c.fromJson(json, MetaClass.class);
        Assert.assertEquals(m.getName(), m1.getName());
        Assert.assertEquals(m.getDescription(), m1.getDescription());
        Assert.assertEquals(m.getRepository(), m1.getRepository());
        
        Assert.assertEquals(m.getLastModified(), m1.getLastModified());
        
        MetaAttribute a1 = (MetaAttribute)m1.getFieldByName("a");
        Assert.assertEquals(a.getDataType(), a1.getDataType());
        Assert.assertEquals(a.getCardinality(), a1.getCardinality());
        
        MetaRelationship r1 = (MetaRelationship)m1.getFieldByName("r");
        Assert.assertEquals(r.getName(), r1.getName());
        Assert.assertEquals(r.getDataType(), r1.getDataType());
        Assert.assertEquals(r.getCardinality(), r1.getCardinality());
    }
    
    @Test
    public void testPlurnalName() throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectConverter<MetaClass> c = new ObjectConverter<MetaClass>();

        MetaClass m = new MetaClass();
        m.setName("name");
        
        String json = c.toJson(m);
        
        JsonNode tree = mapper.readTree(json);
        Assert.assertNull(tree.get(MetaClass.PLURAL_NAME));
        
        m.setpluralName("xx");
        json = c.toJson(m);
        
        tree = mapper.readTree(json);
        Assert.assertEquals("xx", tree.get(MetaClass.PLURAL_NAME).asText());
    }
    
    @Test
    public void testJsonName() throws JsonProcessingException, IOException {
        String json = "{\"name\":\"meta\",\"fields\":{\"a\":{\"dbName\":\"adb\",\"dataType\":\"string\",\"cardinality\":\"One\",\"mandatory\":false}}}";
        
        MetaClass m1 = c.fromJson(json, MetaClass.class);
        Assert.assertEquals("a", m1.getFieldByName("a").getName());
    }
    
    @Test(expected = IllegalMetaClassException.class)
    public void testMissDbName() {
        MetaClass meta = new MetaClass();
        MetaAttribute attr = new MetaAttribute();
        meta.addField(attr);

        c.toJson(meta);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeNullName() {
        String json = "{\"name\":\"meta\",\"fields\":{\"a\":{ \"name\": null,\"dbName\":\"adb\",\"dataType\":\"string\",\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeEmptyName() {
        String json = "{\"name\":\"meta\",\"fields\":{\"a\":{ \"name\": \"\", \"dbName\":\"adb\",\"dataType\":\"string\",\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeEmptyName2() {
        String json = "{\"name\":\"meta\",\"fields\":{\"\":{ \"dbName\":\"adb\",\"dataType\":\"string\",\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeInternalField() {
        String json = "{\"name\":\"meta\",\"fields\":{\"_oid\":{ \"dbName\":\"adb\",\"dataType\":\"string\",\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeNullType() {
        String json = "{\"name\":\"meta\",\"fields\":{\"a\":{ \"dbName\":\"adb\",\"dataType\": null,\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    
    @Test(expected = MetaDataException.class)
    public void testDeserializeInvalidType() {
        String json = "{\"name\":\"meta\",\"fields\":{\"a\":{ \"dbName\":\"adb\",\"dataType\":\"stringInvalid\",\"cardinality\":\"One\",\"mandatory\":false}}}";

        c.fromJson(json, MetaClass.class);
    }
    

    public void testMetaDescription() throws IOException {
        URL apJsonUrl = MetaClassConverterTest.class.getResource("/com/ebay/cloud/cms/model/metadata/cmsdb/iaas/accesspoint.json");
        Assert.assertNotNull(apJsonUrl);
        String s = FileUtils.readFile(apJsonUrl.getFile(), Charset.defaultCharset());
//        List<String> fileContents = Files.readLines(new File(apJsonUrl.getFile()), Charset.defaultCharset());
//        StringBuilder sb =new StringBuilder();
//        for (String line : fileContents) {
//            sb.append(line);
//        }
        // print out for information
        System.out.println(s);
        
        ObjectMapper om = new ObjectMapper();
        MetaClass mc = om.readValue(apJsonUrl, MetaClass.class);
        Assert.assertNotNull(mc);
        Assert.assertNotNull(mc.getDescription());
        System.out.println(mc.getDescription());
        Assert.assertEquals(3 + InternalFieldEnum.values().length, mc.getClassFields().size());
    }
    
}
