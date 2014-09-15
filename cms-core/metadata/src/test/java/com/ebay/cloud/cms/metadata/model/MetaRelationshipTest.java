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

package com.ebay.cloud.cms.metadata.model;

import java.util.Date;

import junit.framework.Assert;

import org.bson.types.ObjectId;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MetaRelationshipTest extends CMSMongoTest {

    private static final String APPLICATION_SERVICE = "ApplicationService";

    private static String       RAPTOR_PAAS         = "raptor-paas";

    private static Repository   repo;

    private static MetaClass    appMeta;

    @BeforeClass
    public static void setup() {
        MetadataDataLoader loader = MetadataDataLoader.getInstance(getDataSource());
        loader.loadTestDataFromResource();

        repo = RepositoryServiceFactory.createRepositoryService(getDataSource(), "localCMSServer").getRepository(
                RAPTOR_PAAS);

        appMeta = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);
    }

    @Test
    public void testClassInitliazation() {
        MetaRelationship mr = new MetaRelationship();
        Assert.assertEquals("MetaRelationship".hashCode(), mr.hashCode());

        try {
            mr.validate(false, null, null);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            //expected
        }
        
        mr.setName("relationship");

        Assert.assertEquals("relationship".hashCode(), mr.hashCode());

    }

    @Test
    public void testServiceReadyCall() {
        MetaRelationship mr = new MetaRelationship();
        mr.setName("relationship");

        try {
            mr.getRefMetaClass();
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            mr.getSourceMetaClass();
            Assert.fail();
        } catch (Exception e) {
            // expected
        }

        try {
            mr.getSourceMetaClass();
            Assert.fail();
        } catch (Exception e) {
            // expected
        }
        
        MetaField relationField = appMeta.getFieldByName("services");
        Assert.assertTrue(relationField instanceof MetaRelationship);
        MetaRelationship relation = (MetaRelationship)relationField;
        Assert.assertNotNull(relation.getRefMetaClass());
        
        Assert.assertNotNull(relation.getSourceMetaClass());

    }
    
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
        Assert.assertEquals(r.getRelationType(), r1.getRelationType());
    }

}
