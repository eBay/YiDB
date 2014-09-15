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

package com.ebay.cloud.cms.dal.entity.flatten;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class EntityTest extends CMSMongoTest {

    private static final String     HTTPS                   = "https";

    private static final String     RUNS_ON                 = "runsOn";

    private static final String     SERVICES                = "services";

    private static final String     RAPTOR_REPO             = "raptor-paas";

    protected static MetaClass      appServiceMetadata      = null;
    protected static MetaClass      serviceInstanceMetadata = null;

    private static IMetadataService raptorMetaService       = null;

    private static MetaClass        environmentMeta         = null;

    @BeforeClass
    public static void setupData() {
        MongoDataSource ds = new MongoDataSource(getConnectionString());
        
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();

        raptorMetaService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer")
                .getRepository(RAPTOR_REPO).getMetadataService();
        
        serviceInstanceMetadata = raptorMetaService.getMetaClass("ServiceInstance");
        
        appServiceMetadata = raptorMetaService.getMetaClass("ApplicationService");
        
        environmentMeta = raptorMetaService.getMetaClass("Environment");
    }

//TODO: add test to check ref class    
//    @Test
//    public void testAddReference(){
//    	MetaClass appSvcMetaClass = raptorMetaService.getMetaClass("ApplicationService");
//    	BsonEntity appSvcEntity = new BsonEntity(appSvcMetaClass);
//    	
//    	MetaClass cosMetaClass = raptorMetaService.getMetaClass("ClassOfService");
//    	BsonEntity cosEntity = new BsonEntity(cosMetaClass);
//    	
//    	appSvcEntity.addFieldValue("services", cosEntity);    	
//    	List<IEntity> sis = (List<IEntity>) appSvcEntity.getFieldValues("services");
//    	assertEquals(1,sis.size());
//    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdd() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        newEntity.addFieldValue("f1", "aa");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGet() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        newEntity.getFieldValues("f1");
    }

    @Test
    public void testHas() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        Assert.assertFalse(newEntity.hasField("f1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSet() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        newEntity.setFieldValues("f1", Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemove() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        newEntity.removeField("f1");
    }

    //
    // Test bson entity getFieldValue method behavior.
    // Directly manipulate the DBObject to simulate the different cases
    //
    @Test
    public void testGetFieldValuesOne() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName("name");
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        //
        // case 1 : not existing Cardinality=ONE
        //
        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(nameField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).isEmpty());

        //
        // case 2 : null for Cardinality=ONE
        //
        // Only db name object is present, but is null, so .v (value db name) is
        // not present
        // test case no more valid, since there is no more a dbobject contains (.v and .l)
//        given.put(nameField.getValueDbName(), null);
//        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
//        Assert.assertNotNull(bsonEntity.getFieldValues(nameField.getName()));
//        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).isEmpty());

        //
        // case 3 .v is present, and is set as null : has null value
        //
        given.put(nameField.getFlattenValueDbName(), null);
        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(nameField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).isEmpty());
        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).size() == 0);

        //
        // case 4 : cardinality=ONE has non-null value
        //
        String serviceName = "servie-name-test-001";
        given.put(nameField.getFlattenValueDbName(), serviceName);
        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(nameField.getName()));
        Assert.assertFalse(bsonEntity.getFieldValues(nameField.getName()).isEmpty());
        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).size() == 1);
        Assert.assertTrue(bsonEntity.getFieldValues(nameField.getName()).get(0).equals(serviceName));
    }
    
    @Test
    public void testGetFieldValuesMany() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField activeManifestRefField = serviceInstanceMetadata.getFieldByName("activeManifestRef");
        Assert.assertTrue(activeManifestRefField.getCardinality() == CardinalityEnum.Many);

        //
        // case 1 : not existing Cardinality=Many
        //
        Assert.assertFalse(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());

        //
        // case 2 : null for Cardinality=Many
        //
        // db name present, but is null
//        given.put(activeManifestRefField.getDbName(), null);
//        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
//        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
//        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());
        
        //
        // case 3 : db name present, but .v (value db name) is null : has null value for the field
        // 
        given.put(activeManifestRefField.getFlattenValueDbName(), null);

        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());

        //
        // case 4 : empty list for Cardinality=Many
        //
        List<String> manifestValuesList = new ArrayList<String>();
        given.put(activeManifestRefField.getFlattenValueDbName(), manifestValuesList);
        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());

        //
        // case 5 : Cardinality=Many has list that contains null
        //
        manifestValuesList.add(null);
        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).size() == 0);

        //
        // case 6 : Cardinality=Many has non-null value
        //
        String manfiest = "manifest-1.0";
        manifestValuesList.add(manfiest);
        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertFalse(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).size() == 1);
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).get(0).equals(manfiest));
        
        //
        // case 7: 
        //
        String singleValue = "abc";
        given.put(activeManifestRefField.getFlattenValueDbName(), singleValue);
        Assert.assertTrue(bsonEntity.hasField(activeManifestRefField.getName()));
        Assert.assertNotNull(bsonEntity.getFieldValues(activeManifestRefField.getName()));
        Assert.assertFalse(bsonEntity.getFieldValues(activeManifestRefField.getName()).isEmpty());
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).size() == 1);
        Assert.assertTrue(bsonEntity.getFieldValues(activeManifestRefField.getName()).get(0).equals(singleValue));
    }

    @Test
    public void testAddFieldValueOne() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        String fieldName = "name";
        MetaField nameField = serviceInstanceMetadata.getFieldByName(fieldName);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        bsonEntity.addFieldValue(fieldName, null);
        Assert.assertFalse(bsonEntity.hasField(fieldName));

        String serviceName = "canssi-001";
        bsonEntity.addFieldValue(fieldName, serviceName);
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(bsonEntity.getFieldValues(fieldName).size() == 1);
        Assert.assertTrue(bsonEntity.getFieldValues(fieldName).get(0).equals(serviceName));
    }

    @Test
    public void testAddFieldValueMany() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        String fieldName = "activeManifestRef";
        MetaField activeManifestRefField = serviceInstanceMetadata.getFieldByName(fieldName);
        Assert.assertTrue(activeManifestRefField.getCardinality() == CardinalityEnum.Many);
        
        bsonEntity.addFieldValue(fieldName, null);
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertEquals(0, bsonEntity.getFieldValues(fieldName).size());

        String manifestName = "manifest-1.0";
        bsonEntity.addFieldValue(fieldName, manifestName);
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertEquals(1, bsonEntity.getFieldValues(fieldName).size());
        Assert.assertTrue(bsonEntity.getFieldValues(fieldName).get(0).equals(manifestName));
    }

    @Test
    public void testSetFieldValuesOne() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        String fieldName = "name";
        MetaField nameField = serviceInstanceMetadata.getFieldByName(fieldName);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        bsonEntity.setFieldValues(fieldName, null);
        Assert.assertFalse(bsonEntity.hasField(fieldName));

        bsonEntity.setFieldValues(fieldName, Collections.emptyList());
        Assert.assertFalse(bsonEntity.hasField(fieldName));

        List<String> names = new ArrayList<String>();
        names.add(null);
        bsonEntity.setFieldValues(fieldName, names);
        Assert.assertFalse(bsonEntity.hasField(fieldName));
    }
    
    @Test
    public void testSetFieldValuesMany() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        String fieldName = "activeManifestRef";
        MetaField nameField = serviceInstanceMetadata.getFieldByName(fieldName);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.Many);

        bsonEntity.setFieldValues(fieldName, null);
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(bsonEntity.getFieldValues(fieldName).isEmpty());

        bsonEntity.setFieldValues(fieldName, Collections.emptyList());
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(bsonEntity.getFieldValues(fieldName).isEmpty());

        List<String> names = new ArrayList<String>();
        names.add(null);
        bsonEntity.setFieldValues(fieldName, names);
        Assert.assertTrue(bsonEntity.hasField(fieldName));
        Assert.assertNotNull(bsonEntity.getFieldValues(fieldName));
        Assert.assertEquals(0, bsonEntity.getFieldValues(fieldName).size());
    }
    
    @Test
    public void testGetFieldValuesOne_nullReference() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName(RUNS_ON);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);
        
        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        given.put(nameField.getFlattenValueDbName(), null);
        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
        List<?> runsOnComputes = bsonEntity.getFieldValues(nameField.getName());
        Assert.assertNotNull(runsOnComputes);
        Assert.assertTrue(runsOnComputes.isEmpty());
    }

    @Test
    public void testGetFieldValuesMany_nullReference() {
        // case 1: whole field is null
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(appServiceMetadata, given);
        MetaField serviceField = appServiceMetadata.getFieldByName(SERVICES);
        Assert.assertTrue(serviceField.getCardinality() == CardinalityEnum.Many);

        Assert.assertFalse(bsonEntity.hasField(serviceField.getName()));
        given.put(serviceField.getFlattenValueDbName(), null);
        Assert.assertTrue(bsonEntity.hasField(serviceField.getName()));
        List<?> runsOnComputes = bsonEntity.getFieldValues(serviceField.getName());
        Assert.assertNotNull(runsOnComputes);
        Assert.assertTrue(runsOnComputes.isEmpty());

        given.put(serviceField.getFlattenValueDbName(), null);
        runsOnComputes = bsonEntity.getFieldValues(serviceField.getName());
        Assert.assertNotNull(runsOnComputes);
        Assert.assertTrue(runsOnComputes.isEmpty());

        // case 2: field list not null but it contains null
        BasicDBList services = new BasicDBList();
        BasicDBObject dbo = new BasicDBObject();
        services.add(dbo);
        services.add(null);
        given.put(serviceField.getFlattenValueDbName(), services);
        try {
            bsonEntity.getFieldValues(serviceField.getName());
            // should not fail now. see CMS-3503
            // Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testSetFieldValuesOne_nullReference() {
        // case 1: whole field is null
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName(RUNS_ON);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        try {
            bsonEntity.setFieldValues(nameField.getName(), null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            //expected 
        }

        List<?> computes = bsonEntity.getFieldValues(nameField.getName());
        Assert.assertNotNull(computes);
        Assert.assertTrue(computes.isEmpty());

        // case 2 : a list contains one null
        computes = new ArrayList<NewBsonEntity>();
        computes.add(null);
        try {
            bsonEntity.setFieldValues(nameField.getName(), computes);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            //expected 
        }
    }
    
    @Test
    public void testSetFieldValuesMany_nullReference() {
        // case 1: whole field is null
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(appServiceMetadata, given);
        MetaField nameField = appServiceMetadata.getFieldByName(SERVICES);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.Many);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        bsonEntity.setFieldValues(nameField.getName(), null);
        List<?> services = bsonEntity.getFieldValues(nameField.getName());
        Assert.assertNotNull(services);
        Assert.assertTrue(services.isEmpty());

        // case 2: field list has null
        List<NewBsonEntity> serviceEntities = new ArrayList<NewBsonEntity>();
        serviceEntities.add(new NewBsonEntity(serviceInstanceMetadata));
        serviceEntities.add(null);
        try {
            bsonEntity.setFieldValues(nameField.getName(), serviceEntities);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAddFieldValueOne_nullReference() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName(RUNS_ON);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        try {
            bsonEntity.addFieldValue(nameField.getName(), null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAddFieldValueMany_nullReference() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(appServiceMetadata, given);
        MetaField nameField = appServiceMetadata.getFieldByName(SERVICES);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.Many);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        try {
            bsonEntity.addFieldValue(nameField.getName(), null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expectec
        }
    }

    @Test
    public void testBooleanGetInvalidValue() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata, given);
        MetaField field = serviceInstanceMetadata.getFieldByName(HTTPS);

        BasicDBObject dbo = new BasicDBObject();
        // add an invalid value
        dbo.put(MetaField.VALUE_KEY, "true");
        given.put(field.getDbName(), dbo);

        List<?> invalidValue = bsonEntity.getFieldValues(HTTPS);
        Assert.assertEquals(0, invalidValue.size());
    }

    @Test
    public void testIntegerLongGetInvalidValue() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity bsonEntity = new NewBsonEntity(environmentMeta, given);
        // long
        String longFieldName = "numServices";
        MetaField longField = environmentMeta.getFieldByName(longFieldName);
        String intFieldName = "numService";
        MetaField intField = environmentMeta.getFieldByName(intFieldName);

        BasicDBObject dbo = new BasicDBObject();
        // add an invalid value
        dbo.put(MetaField.VALUE_KEY, "invalid-integer-value");
        given.put(longField.getDbName(), dbo);
        dbo = new BasicDBObject();
        // add an invalid value
        dbo.put(MetaField.VALUE_KEY, "invalid-long-value");
        given.put(intField.getDbName(), dbo);
        // assertion
        List<?> invalidValue = bsonEntity.getFieldValues(longFieldName);
        Assert.assertEquals(0, invalidValue.size());
        invalidValue = bsonEntity.getFieldValues(intFieldName);
        Assert.assertEquals(0, invalidValue.size());
    }

    @Test
    public void testReferenceGetInvalidValue() {
        BasicDBObject given = new BasicDBObject();
        NewBsonEntity appEntity = new NewBsonEntity(appServiceMetadata, given);
        MetaField refField = appServiceMetadata.getFieldByName(SERVICES);
        BasicDBObject dbo = new BasicDBObject();
        // invalid value
        dbo.put(MetaField.VALUE_KEY, null);
        given.put(refField.getName(), dbo);

        List<?> services = appEntity.getFieldValues(SERVICES);
        Assert.assertEquals(0, services.size());
    }

    @Test
    public void testAddInvalidValue() {
        NewBsonEntity bsonEntity = new NewBsonEntity(environmentMeta);
        String longFieldName = "numServices";
        String intFieldName = "numService";
        // case 0 : for long
        try {
            bsonEntity.addFieldValue(longFieldName, "invalid-long");
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        bsonEntity.addFieldValue(longFieldName, null);
        // return empty list
        Assert.assertFalse(bsonEntity.hasField(longFieldName));
        Assert.assertEquals(0, bsonEntity.getFieldValues(longFieldName).size());

        // case 1 : for integer
        try {
            bsonEntity.addFieldValue(intFieldName, "invalid-int");
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        bsonEntity.addFieldValue(intFieldName, null);
        // actually, this is tricky : hasField return true, while getFieldValue
        // return empty list
        Assert.assertFalse(bsonEntity.hasField(intFieldName));
        Assert.assertEquals(0, bsonEntity.getFieldValues(intFieldName).size());
    }

    @Test
    public void testAddBooleanInvalidValue() {
        NewBsonEntity bsonEntity = new NewBsonEntity(serviceInstanceMetadata);
        try {
            bsonEntity.addFieldValue(HTTPS, 1);// 1 is invalid boolean value
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        bsonEntity.addFieldValue(HTTPS, null);
        Assert.assertFalse(bsonEntity.hasField(HTTPS));
        Assert.assertEquals(0, bsonEntity.getFieldValues(HTTPS).size());
    }

    @Test
    public void testSetFieldValue_asEmpty() {
        NewBsonEntity newEntity = new NewBsonEntity(serviceInstanceMetadata);
        String fieldName = "manifestRef";
        MetaField field = serviceInstanceMetadata.getFieldByName(fieldName);
        newEntity.setFieldValues(fieldName, Collections.emptyList());
        Assert.assertTrue(newEntity.hasField(fieldName));
        Assert.assertEquals(Collections.emptyList(), newEntity.getFieldValues(fieldName));
        List<?> value = (List<?>) newEntity.getNode().get(field.getFlattenValueDbName());
        Assert.assertNotNull(value);
        Assert.assertEquals(0, value.size());
    }

}
