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

package com.ebay.cloud.cms.dal.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class JsonEntityTest extends CMSMongoTest {

    private static final String RAPTOR_REPO = "raptor-paas";

    private static final String RUNS_ON = "runsOn";

    private static final String SERVICES = "services";

    protected static MetaClass serviceInstanceMetadata = null;
    protected static MetaClass appServiceMetadata = null;
    protected static MetaClass computeMetadata = null;

    private static IMetadataService raptorMetaService = null;

    @BeforeClass
    public static void setupData() {
        MongoDataSource ds = new MongoDataSource(getConnectionString());

        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();

        raptorMetaService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer")
                .getRepository(RAPTOR_REPO).getMetadataService();

        appServiceMetadata = raptorMetaService.getMetaClass("ApplicationService");
        serviceInstanceMetadata = raptorMetaService.getMetaClass("ServiceInstance");
        computeMetadata = raptorMetaService.getMetaClass("Compute");
    }

    //
    // Directly manipulate the JsonObject to simulate the different cases
    //
    @Test
    public void testGetFieldValuesOne() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName("name");
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);
        
        List<String> names = jsonEntity.getFieldNames();
        Assert.assertTrue(names.isEmpty());

        //
        // case 1 : not existing Cardinality=ONE
        //
        Assert.assertFalse(jsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(jsonEntity.getFieldValues(nameField.getName()));
        Assert.assertTrue(jsonEntity.getFieldValues(nameField.getName()).isEmpty());

        //
        // case 2 : null for Cardinality=ONE
        //
        given.put(nameField.getName(), (JsonNode) null);
        Assert.assertTrue(jsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(jsonEntity.getFieldValues(nameField.getName()));
        Assert.assertTrue(jsonEntity.getFieldValues(nameField.getName()).isEmpty());
        Assert.assertTrue(jsonEntity.getFieldValues(nameField.getName()).size() == 0);

        //
        // case 3 : cardinality=ONE has non-null value
        //
        String serviceName = "servie-name-test-001";
        given.put(nameField.getName(), serviceName);
        Assert.assertTrue(jsonEntity.hasField(nameField.getName()));
        Assert.assertNotNull(jsonEntity.getFieldValues(nameField.getName()));
        Assert.assertFalse(jsonEntity.getFieldValues(nameField.getName()).isEmpty());
        Assert.assertTrue(jsonEntity.getFieldValues(nameField.getName()).size() == 1);
        Assert.assertTrue(jsonEntity.getFieldValues(nameField.getName()).get(0).equals(serviceName));
        
        names = jsonEntity.getFieldNames();
        Assert.assertEquals(1, names.size());
    }

    @Test
    public void testGetFieldValuesMany() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField activeManifestRefField = serviceInstanceMetadata.getFieldByName("activeManifestRef");
        Assert.assertTrue(activeManifestRefField.getCardinality() == CardinalityEnum.Many);
        String fieldName = activeManifestRefField.getName();

        //
        // case 1 : not existing Cardinality=Many
        //
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        //
        // case 2 : null for Cardinality=Many
        //
        given.put(fieldName, (JsonNode) null);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
//        try {
            Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
            Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());
//            Assert.fail();
//        } catch (Exception e) {
            // expected
//        }

        //
        // case 3 : empty list for Cardinality=Many
        //
        ArrayNode manifestNode = JsonNodeFactory.instance.arrayNode();
        given.put(fieldName, manifestNode);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        //
        // case 7 : NullNode for Cardinality=Many
        //
        given.put(fieldName, JsonNodeFactory.instance.nullNode());
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        //
        // case 4 : Cardinality=Many has list that contains null
        //
        given.put(fieldName, manifestNode);
        manifestNode.add((JsonNode) null);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 0);

        //
        // case 5 : Cardinality=Many has list that contains NullNode
        //
        NullNode nullNode = JsonNodeFactory.instance.nullNode();
        manifestNode.removeAll();
        manifestNode.add(nullNode);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 0);

        //
        // case 6 : Cardinality=Many has non-null value
        //
        String manfiest = "manifest-1.0";
        manifestNode.add(manfiest);
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertFalse(jsonEntity.getFieldValues(fieldName).isEmpty());
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 1);
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).get(0).equals(manfiest));
    }

    @Test
    public void testAddFieldValueOne() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName("name");
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);
        String fieldName = nameField.getName();

        jsonEntity.addFieldValue(fieldName, null);
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        String serviceName = "canssi-001";
        jsonEntity.addFieldValue(fieldName, serviceName);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 1);
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).get(0).equals(serviceName));
    }

    @Test
    public void testAddFieldValueMany() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField activeManifestRefField = serviceInstanceMetadata.getFieldByName("activeManifestRef");
        Assert.assertTrue(activeManifestRefField.getCardinality() == CardinalityEnum.Many);
        String fieldName = activeManifestRefField.getName();

        jsonEntity.addFieldValue(fieldName, null);
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        String manifestName = "manifest-1.0";
        jsonEntity.addFieldValue(fieldName, manifestName);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 1);
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName).get(0));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).get(0).equals(manifestName));
    }

    @Test
    public void testSetFieldValuesOne() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName("name");
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);
        String fieldName = nameField.getName();

        jsonEntity.setFieldValues(fieldName, null);
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        jsonEntity.setFieldValues(fieldName, Collections.emptyList());
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        List<String> names = new ArrayList<String>();
        names.add(null);
        jsonEntity.setFieldValues(fieldName, names);
        Assert.assertFalse(jsonEntity.hasField(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());
    }

    @Test
    public void testSetFieldValuesMany() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();

        JsonEntity jsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField activeManifestRefField = serviceInstanceMetadata.getFieldByName("activeManifestRef");
        Assert.assertTrue(activeManifestRefField.getCardinality() == CardinalityEnum.Many);
        String fieldName = activeManifestRefField.getName();

        jsonEntity.setFieldValues(fieldName, null);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        jsonEntity.setFieldValues(fieldName, Collections.emptyList());
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).isEmpty());

        List<String> names = new ArrayList<String>();
        names.add(null);
        jsonEntity.setFieldValues(fieldName, names);
        Assert.assertTrue(jsonEntity.hasField(fieldName));
        Assert.assertNotNull(jsonEntity.getFieldValues(fieldName));
        Assert.assertTrue(jsonEntity.getFieldValues(fieldName).size() == 0);
    }

    /***
     * 
     * TODO
     * 
     */
    @Test
    public void testGetFieldValuesOne_nullReference() {
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName(RUNS_ON);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);
        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        given.put(nameField.getName(), (JsonNode)null);
        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
        try {
            bsonEntity.getFieldValues(nameField.getName());
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testGetFieldValuesMany_nullReference() {
        // case 1: whole field is null
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(appServiceMetadata, given);
        MetaField nameField = appServiceMetadata.getFieldByName(SERVICES);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.Many);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        given.put(nameField.getName(), (JsonNode) null);
        Assert.assertTrue(bsonEntity.hasField(nameField.getName()));
//        try {
        // json entity provides more fault-tolerance to avoid the exception for null node
            bsonEntity.getFieldValues(nameField.getName());
//            Assert.fail();
//        } catch (IllegalArgumentException iae) {
            // expected
//        }

        // case 2: field list not null but it contains null
        ArrayNode an = JsonNodeFactory.instance.arrayNode();
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        an.add(node);
        an.add((JsonNode) null);
        given.put(nameField.getName(), an);
        try {
            bsonEntity.getFieldValues(nameField.getName());
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetFieldValuesOne_nullReference() {
        // case 1: whole field is null
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(serviceInstanceMetadata, given);
        MetaField nameField = serviceInstanceMetadata.getFieldByName(RUNS_ON);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.One);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        try {
            bsonEntity.setFieldValues(nameField.getName(), null);
        } catch (Throwable e) {
        	Assert.fail(); 
        }

        List<JsonEntity> computes = (List<JsonEntity>)bsonEntity.getFieldValues(nameField.getName());
        Assert.assertNotNull(computes);
        Assert.assertTrue(computes.isEmpty());

        // case 2 : a list contains one null
        computes = new ArrayList<JsonEntity>();
        computes.add(null);
        try {
            bsonEntity.setFieldValues(nameField.getName(), computes);
        } catch (Throwable e) {
        	 Assert.fail();
        }
    }
    
    @Test
    public void testSetFieldValuesMany_nullReference() {
        // case 1: whole field is null
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(appServiceMetadata, given);
        MetaField nameField = appServiceMetadata.getFieldByName(SERVICES);
        Assert.assertTrue(nameField.getCardinality() == CardinalityEnum.Many);

        Assert.assertFalse(bsonEntity.hasField(nameField.getName()));
        bsonEntity.setFieldValues(nameField.getName(), null);
        List<?> services = bsonEntity.getFieldValues(nameField.getName());
        Assert.assertTrue(bsonEntity.hasField(SERVICES));
        Assert.assertNotNull(services);
        Assert.assertTrue(services.isEmpty());

        // case 2: field list has null
        List<JsonEntity> serviceEntities = new ArrayList<JsonEntity>();
        serviceEntities.add(new JsonEntity(serviceInstanceMetadata));
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
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(serviceInstanceMetadata, given);
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
        ObjectNode given = JsonNodeFactory.instance.objectNode();
        JsonEntity bsonEntity = new JsonEntity(appServiceMetadata, given);
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
    
}
