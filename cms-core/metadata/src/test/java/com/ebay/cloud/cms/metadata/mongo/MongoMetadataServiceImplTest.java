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

package com.ebay.cloud.cms.metadata.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.IndexExistsException;
import com.ebay.cloud.cms.metadata.exception.IndexNotExistsException;
import com.ebay.cloud.cms.metadata.exception.IndexOptionOperationException;
import com.ebay.cloud.cms.metadata.exception.MetaClassExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaFieldExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaFieldNotExistsException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.IndexInfo.IndexOptionEnum;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.ConsistencyTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.Repository.AccessType;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.Mongo;


public class MongoMetadataServiceImplTest extends CMSMongoTest {
    
    static IRepositoryService repositoryService;
    static String repoName = "MetadataServiceTest";
    static Repository repo;
    private static MongoDataSource ds;
    private static MetadataContext metaContext;
    
    @BeforeClass
    public static void createRepoService() {
        
        ds = new MongoDataSource(getConnectionString());
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();
        
        repositoryService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");
        repo = repositoryService.createRepository(new Repository(repoName));
        
        metaContext = new MetadataContext();
        metaContext.setSourceIp("127.0.0.1");
        metaContext.setSubject("unitTestUser");
        
        Assert.assertEquals(repo, repo.getMetadataService().getRepository());
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testInvalidCreation() {
        // 1. test constructor in-variable requirement
        try {
            new MongoMetadataServiceImpl(null, 1, 1, 1, 1, null, null);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoMetadataServiceImpl(new Mongo(), 0, 1, 1, 1, null, null);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoMetadataServiceImpl(new Mongo(), 0, 0, 1, 1, new Repository(), null);
        } catch (Exception e) {
            // expected
        }
        
        try {
            new MongoMetadataServiceImpl(new Mongo(), 1, 0, 1, 1, new Repository(), null);
        } catch (Exception e) {
            // expected
        }

        try {
            new MongoMetadataServiceImpl(new Mongo(), 1, 1, 1, 1, new Repository(), null);
        } catch (Exception e) {
            // expected
        }
    }
    
    private MetaClass createMetaClass() {
        MetaClass env = new MetaClass();
        env.setRepository(repoName);
        env.setLastModified(Calendar.getInstance().getTime());
        env.setName("Environment");
        
        return env;
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testInternalFields() {

        MetaClass metaClass = createMetaClass();
        
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("_type");
        attribute.setDataType(MetaField.DataTypeEnum.INTEGER);
        metaClass.addField(attribute);
    }
    
    @Test(expected = IllegalMetaClassException.class)
    public void testCreateMetaclassNull() {
        repo.getMetadataService().createMetaClass(null, new MetadataContext());
    }
    
    @Test
    public void testCreateMetaclassWithReservedNames() {
        for (String name : MetaClass.RESERVED_METACLASS_NAME) {
            try {
                MetaClass m = createMetaClass();
                m.setName(name);
                repo.getMetadataService().createMetaClass(m, new MetadataContext());
                Assert.fail("should have exception");
            }
            catch (Throwable e) {
                Assert.assertTrue(e instanceof IllegalMetaClassException);
            }
            
        }
        
        MetaClass m = createMetaClass();
        m.setName("reservedFieldName");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("_length");
        attribute.setDataType(DataTypeEnum.STRING);
        m.addField(attribute);
        try {
            repo.getMetadataService().createMetaClass(m, new MetadataContext());
            Assert.fail();
        }
        catch (Throwable e) {
            Assert.assertTrue(e instanceof IllegalMetaClassException);
        }
    }

    @Test
    public void testMetaClassCreation() {

        MetaClass metaClass = createMetaClass();
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertNull(nm);
        
        ms.createMetaClass(metaClass, new MetadataContext());
        
        nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertEquals(metaClass.getName(), nm.getName());
    }
    
    @Test
    public void testMetaClassCreation_embed() {

        MetaClass metaClass = createMetaClass();
        metaClass.setName("EmbedMetaClass");
        metaClass.setEmbed(true);

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertNull(nm);
        
        ms.createMetaClass(metaClass, new MetadataContext());
        
        nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertEquals(metaClass.getName(), nm.getName());
        
        Assert.assertEquals(true, nm.isEmbed());
    }
    
    
    @Test
    public void testMetaClassCreation_selfref() {

        MetaClass metaClass = createMetaClass();
        metaClass.setName("selfref");
        
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Reference);
        r1.setName("refs");
        r1.setRefDataType("selfref");
        metaClass.addField(r1);
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        ms.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertEquals(metaClass.getName(), nm.getName());
    }
    
    @Test
    public void testMetaClassCreation_selfref_embed() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("selfref_embed");
        
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Embedded);
        r1.setName("refs");
        r1.setRefDataType(metaClass.getName());
        metaClass.addField(r1);
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        try {
            ms.createMetaClass(metaClass, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
        }
    }
    
    @Test(expected=MetaClassExistsException.class)
    public void testMetaClassCreationFailure_NameConflict() {

        MetaClass metaClass = createMetaClass();
        metaClass.setName("creationFailure");
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertNull(nm);
        
        ms.createMetaClass(metaClass, new MetadataContext());
        
        nm = ms.getMetaClass(metaClass.getName());
        
        Assert.assertEquals(metaClass.getName(), nm.getName());
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=MetaClassExistsException.class)
    public void testMetaClassCreationFailure_pluralNameConflict() {

        MetaClass metaClass = createMetaClass();
        metaClass.setName("creationFailure1");
        metaClass.setpluralName("plural1");
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass nm = ms.getMetaClass("plural1");
        
        Assert.assertNull(nm);

        ms.createMetaClass(metaClass, new MetadataContext());
        nm = ms.getMetaClass(metaClass.getpluralName());
        Assert.assertEquals(metaClass.getName(), nm.getName());
        Assert.assertEquals(metaClass.getpluralName(), nm.getpluralName());
        
        MetaClass metaClass1 = createMetaClass();
        metaClass1.setName("creationFailure2");
        metaClass1.setpluralName("plural1");
        
        ms.createMetaClass(metaClass1, new MetadataContext());
    }
    
    @Test
    public void testMetaClassCreationWithIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("metaWithIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);

        IndexInfo index = new IndexInfo("indexName");
        index.getKeyList().add(attribute.getName());

        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());

        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertNotNull(getMeta);
        Assert.assertTrue(getMeta.getFields().size() > 2);
        Assert.assertNotNull(getMeta.getIndexes());
        Assert.assertTrue(getMeta.getIndexes().size() == 3);
        
        Collection<IndexInfo> getIndexes = getMeta.getIndexes();
        Set<String> getIndexNames = new HashSet<String>();
        for (IndexInfo getIndex : getIndexes) {
            getIndexNames.add(getIndex.getIndexName());
        }
        Assert.assertTrue(getIndexNames.contains(index.getIndexName()));

        Assert.assertEquals(1, getMeta.getIndexesOnField(attribute.getName()).size());
    }
    
    @Test
    public void testMetaClassCreationWithIndexInherit() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("metaWithIndex1");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name1");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName1");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass subMetaClass = createMetaClass();
        subMetaClass.setName("subMetaClass");
        subMetaClass.setParent(metaClass.getName());
        MetaAttribute subAttr = new MetaAttribute();
        subAttr.setName("subAttribute");
        subAttr.setDataType(DataTypeEnum.DOUBLE);
        subMetaClass.addField(subAttr);

        IndexInfo subIndex = new IndexInfo("subIndex");
        subIndex.addKeyField(subAttr.getName());
        subMetaClass.addIndex(subIndex);
        
        metaService.createMetaClass(subMetaClass, new MetadataContext());
        
        MetaClass getMeta = metaService.getMetaClass(subMetaClass.getName());
        Assert.assertNotNull(getMeta);
        Assert.assertTrue(getMeta.getFields().size() > 2);
        Assert.assertNotNull(getMeta.getIndexes());
        Assert.assertTrue(getMeta.getIndexes().size() == 4);

        Collection<IndexInfo> getIndexes = getMeta.getIndexes();
        Set<String> getIndexNames = new HashSet<String>();
        for (IndexInfo getIndex : getIndexes) {
            getIndexNames.add(getIndex.getIndexName());
        }
        
        Assert.assertTrue(getIndexNames.contains(index.getIndexName()));
        Assert.assertTrue(getIndexNames.contains(subIndex.getIndexName()));

        Assert.assertEquals(1, getMeta.getIndexesOnField(subAttr.getName()).size());
    }
    
    @Test(expected = IndexExistsException.class)
    public void testMetaClassCreationDuplicateIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("metaWithIndex2");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass subMetaClass = createMetaClass();
        subMetaClass.setName("subMetaClass2");
        subMetaClass.setParent(metaClass.getName());
        MetaAttribute subAttr = new MetaAttribute();
        subAttr.setName("subAttribute2");
        subAttr.setDataType(DataTypeEnum.DOUBLE);
        subMetaClass.addField(subAttr);

        IndexInfo subIndex = new IndexInfo("indexName2");
        subIndex.addKeyField(attribute.getName());
        subMetaClass.addIndex(subIndex);
        
        metaService.createMetaClass(subMetaClass, new MetadataContext());
    }
    
    @Test
    public void testMetaClassCreationOverrideIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("metaWithIndex3");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass subMetaClass = createMetaClass();
        subMetaClass.setName("subMetaClass3");
        subMetaClass.setParent(metaClass.getName());
        MetaAttribute subAttr = new MetaAttribute();
        subAttr.setName("subAttribute2");
        subAttr.setDataType(DataTypeEnum.DOUBLE);
        subMetaClass.addField(subAttr);

        IndexInfo subIndex = new IndexInfo("indexName2");
        subIndex.addKeyField(subAttr.getName());
        subMetaClass.addIndex(subIndex);
        
        metaService.createMetaClass(subMetaClass, new MetadataContext());
    }

    @Test
    public void testMetaClassCreationWithIndexEmbed() {
        // TODO
    }

    @Test
    public void testUpdateAddIndexWithInvalidKey() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionAddInvalidIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());

        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName2");
        newIndex.getKeyList().add("invalid_name");
        newIndex.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);

        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.ADD);
        try {
            metaService.updateMetaOption(metaClass.getName(), newOption, context);
        } catch (IndexOptionOperationException e) {
            Assert.assertTrue(e.getMessage().contains("index indexName2 key list contains non-existing field"));
        }
    }

    @Test
    public void testUpdateAddIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionAddIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName3");
        newIndex.getKeyList().add(attribute.getName());
        newIndex.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);
        
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.ADD);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);

        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertEquals(4, getMeta.getIndexes().size());
        Assert.assertTrue(getMeta.getIndexNames().contains(newIndex.getIndexName()));
        Assert.assertEquals(1, getMeta.getVersion());
    }
    
    @Test
    public void testUpdateAddIndex2() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("abc");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("a.b");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        try {
            metaService.createMetaClass(metaClass, new MetadataContext());
            Assert.fail();
        } catch (IllegalIndexException iie) {
            // expected
        }
    }
    
    @Test
    public void testUpdateAddIndex3() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("abc");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_b");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("a$b");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        try {
            metaService.createMetaClass(metaClass, new MetadataContext());
            Assert.fail();
        } catch (IllegalIndexException iie) {
            // expected
            iie.printStackTrace();
        }
    }
    
    @Test
    public void updateMultipleKeyUniqIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateMultipleKeyUniqIndex");
        MetaAttribute attribute1 = new MetaAttribute();
        attribute1.setName("attr_Name1");
        attribute1.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute1);
        
        MetaAttribute attribute2 = new MetaAttribute();
        attribute2.setName("attr_Name2");
        attribute2.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute2);
        
        IndexInfo index = new IndexInfo("indexName1");
        index.getKeyList().add(attribute1.getName());
        metaClass.addIndex(index);
        
        // create metaclass with on index
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Collection<IndexInfo> indexes = getMeta.getIndexesOnField(attribute1.getName());
        Assert.assertNotNull(indexes);
        Assert.assertTrue(indexes.size() == 1);
        
        // now add  another multiple-key unique index to the metaclass
        MetaClass newMetaClass = createMetaClass();
        newMetaClass.setName(metaClass.getName());
        IndexInfo index2 = new IndexInfo("indexName2");
        index2.getKeyList().add(attribute1.getName());
        index2.getKeyList().add(attribute2.getName());
        newMetaClass.addIndex(index2);
        
        metaService.updateMetaClass(newMetaClass, metaContext);
        getMeta = metaService.getMetaClass(newMetaClass.getName());
        indexes = getMeta.getIndexesOnField(attribute1.getName());
        Assert.assertNotNull(indexes);
        Assert.assertTrue(indexes.size() == 2);
    }
    
    @Test(expected=IndexExistsException.class)
    public void testUpdateAddIndexAlreadyExisting() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionAddIndexAlreadyExisitng");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        index.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);
        
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.ADD);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);
    }

    @Test
    public void testUpdateChangeIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionChangeIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());

        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName2");
        newIndex.getKeyList().add(attribute.getName());
        newIndex.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);

        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.UPDATE);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);

        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertEquals(3, getMeta.getIndexes().size());// check no new index added
        Assert.assertTrue(getMeta.getIndexByName(index.getIndexName()).getIndexOptions().size() == 1);
        Assert.assertTrue(getMeta.getIndexByName(index.getIndexName()).getIndexOptions().get(0) == IndexOptionEnum.unique);
    }
    
    @Test(expected = IndexNotExistsException.class)
    public void testUpdateChangeIndexNotExists() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionChangeIndexNotExists");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName3");//an not existing index
        index.getKeyList().add(attribute.getName());
        index.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);
        
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.UPDATE);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);
    }

    @Test
    public void testUpdateRemoveIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("updateOptionRemoveIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        //try to delete an index
        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName2");//an not existing index
//        index.getKeyList().add(attribute.getName());
//        index.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);
        
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.DELETE);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);
        
        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertEquals(2, getMeta.getIndexes().size());// check no new index added
        Assert.assertTrue(getMeta.getIndexes().iterator().next().isInternal());
    }
    
    @Test(expected = IndexNotExistsException.class)
    public void testUpdateRemoveIndexNotExists() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("removeOptionIndexNotExists");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name2");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName2");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaOption newOption = new MetaOption();
        IndexInfo newIndex = new IndexInfo("indexName3");//an not exsisting index
        index.getKeyList().add(attribute.getName());
        index.addOption(IndexOptionEnum.unique);
        newOption.addIndex(newIndex);
        
        MetadataContext context = new MetadataContext();
        context.setSourceIp("127.0.0.1");
        context.setOptionChangeMode(UpdateOptionMode.DELETE);
        metaService.updateMetaOption(metaClass.getName(), newOption, context);
    }

    @Test
    public void testUpdateMetaAddIndex() {
        MetaClass metaClass = createMetaClass();
        metaClass.setName("metaUpdateAddIndex");
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("attr_Name1");
        attribute.setDataType(DataTypeEnum.STRING);
        metaClass.addField(attribute);
        IndexInfo index = new IndexInfo("indexName1");
        index.getKeyList().add(attribute.getName());
        metaClass.addIndex(index);

        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertEquals(3, getMeta.getIndexes().size());
        
        MetaClass metaClass2 = createMetaClass();
        metaClass2.setName(metaClass.getName());
        attribute.setDbName(null);
        attribute.setValueDbName(null);
        attribute.setFlattenValueDbName(null);
        metaClass2.addField(attribute);
        IndexInfo addIndex = new IndexInfo("indexName2");
        addIndex.addKeyField(attribute.getName());
        metaClass2.addIndex(addIndex);
        metaService.updateMetaClass(metaClass2, metaContext);

        getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertEquals(4, getMeta.getIndexes().size());
    }

    @Test
    public void testIndex_shard() {
        MetaClass meta = createShardMeta("sharedClass");
        Collection<IndexInfo> indexes = meta.getIndexes();
        Assert.assertEquals(5, indexes.size());
        for (IndexInfo ii : indexes) {
            if (ii.getIndexName().equals(IndexInfo.PK_INDEX)) {
                Assert.assertTrue(ii.getIndexOptions().contains(IndexOptionEnum.unique));
            }
        }
    }

    private MetaClass createShardMeta(String name) {
        MetaClass metaClass = createMetaClass();
        metaClass.setName(name);
        metaClass.setSharded(true);
        MetaAttribute ma = new MetaAttribute();
        ma.setName("attr1");
        ma.setDataType(DataTypeEnum.STRING);
        metaClass.addField(ma);
        MetaAttribute ma2 = new MetaAttribute();
        ma2.setName("attr2");
        ma2.setDataType(DataTypeEnum.STRING);
        metaClass.addField(ma2);
        MetaAttribute ma3 = new MetaAttribute();
        ma3.setName("attr3");
        ma3.setDataType(DataTypeEnum.STRING);
        metaClass.addField(ma3);
        
        IndexInfo ii = new IndexInfo("unique_Attr1");
        ii.addOption(IndexOptionEnum.unique);
        ii.addKeyField(ma.getName());
        metaClass.addIndex(ii);
        
        IndexInfo ii2 = new IndexInfo("unique_Attr2");
        ii2.addOption(IndexOptionEnum.hashed);
        ii2.addKeyField(ma.getName());
        metaClass.addIndex(ii2);
        
        metaClass.getOptions().addPrimaryKey(ma3.getName());
        
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        return metaService.getMetaClass(metaClass.getName());
    }

    @Test
    public void testIndex_inheritShard() {
        MetaClass parentMeta = createShardMeta("shard_base");
        MetaClass metaClass = createMetaClass();
        metaClass.setName("childMeta");
        metaClass.setParent(parentMeta.getName());
        MetaAttribute ma = new MetaAttribute();
        ma.setName("childAttr1");
        ma.setDataType(DataTypeEnum.STRING);
        metaClass.addField(ma);
        
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        metaService.createMetaClass(metaClass, new MetadataContext());
        
        MetaClass getMeta = metaService.getMetaClass(metaClass.getName());
        Assert.assertFalse(getMeta.isMetaSharded());
        //Assert.assertArrayEquals(parentMeta.getPrimaryKeys().toArray(), getMeta.getPrimaryKeys().toArray());
    }
    
    @Test
    public void testUpdate() {
        MetaClass m1 = createMetaClass();
        m1.setName("update");
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        ms.createMetaClass(m1, new MetadataContext());
        
        MetaClass m2 = createMetaClass();
        m2.setName("update");
        m2.setpluralName("updates");
        ms.updateMetaClass(m2, metaContext);
        
        MetaAttribute attribute = new MetaAttribute();
        attribute.setName("classOfService");
        attribute.setDataType(MetaField.DataTypeEnum.INTEGER);
    }

    /**
     * CMS-3507
     */
    @Test
    public void testUpdateExistingMetaclass() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        // create a meta class
        MetaClass rack = createMetaClass();
        rack.setName("Rack");

        MetaClass createdRack = metaService.createMetaClass(rack, new MetadataContext());
        
        MetaClass row = createMetaClass();
        row.setName("Row");
        MetaRelationship relation = new MetaRelationship();
        relation.setName("racks");
        relation.setMandatory(false);
        relation.setCardinality(CardinalityEnum.Many);
        relation.setDataType(DataTypeEnum.RELATIONSHIP);
        relation.setRefDataType(createdRack.getName());
        row.addField(relation);

        metaService.createMetaClass(row, new MetadataContext());
        
        relation.setMandatory(true);
        try {
            metaService.updateMetaField(row, "racks", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals("racks: dbName must NOT be provided", e.getMessage());
        }
        
        try {
            relation.setDbName(null);
            metaService.updateMetaField(row, "racks", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals("racks: valueDbName must NOT be provided", e.getMessage());
        }

        try {
            relation.setDbName(null);
            relation.setValueDbName(null);
            metaService.updateMetaField(row, "racks", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals("racks: flattenValueDbName must NOT be provided", e.getMessage());
        }
        
        relation.setDbName(null);
        relation.setValueDbName(null);
        relation.setFlattenValueDbName(null);
        relation.setMandatory(true);
        metaService.updateMetaField(row, "racks", metaContext);

//        relation.setCardinality(CardinalityEnum.One);
        relation.setDbName(null);
        relation.setValueDbName(null);
        relation.setFlattenValueDbName(null);
        relation.setSourceDataType(null);// clean src data type
        row.setAllowFullTableScan(true);
        metaService.updateMetaClass(row, metaContext);
    }
    
    /**
     * CMS-4633
     */
    @Test
    public void testUpdateFieldOnNonExistingMetaclass() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        // create a meta class
        MetaClass rack = createMetaClass();
        rack.setName("RackNonExist");

        MetaAttribute attr = new MetaAttribute();
        attr.setName("racks");
        attr.setMandatory(false);
        attr.setCardinality(CardinalityEnum.Many);
        attr.setDataType(DataTypeEnum.INTEGER);
        rack.addField(attr);

        try {
            metaService.updateMetaField(rack, "racks", metaContext);
            Assert.fail();
        } catch (MetaClassNotExistsException e) {
            
        }
    }

    public void testUpdate_duplicateSameField() {

        MetaClass m1 = createMetaClass();
        m1.setName("update1");
        
        MetaAttribute a = new MetaAttribute();
        a.setName("classOfService");
        a.setDataType(MetaField.DataTypeEnum.INTEGER);
        m1.addField(a);
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        ms.createMetaClass(m1, new MetadataContext());
        
        MetaClass m2 = createMetaClass();
        m2.setName("update1");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("classOfService");
        a1.setDataType(MetaField.DataTypeEnum.INTEGER);
        m2.addField(a1);
        
        ms.updateMetaClass(m2, metaContext);
    }
    
    @Test(expected=MetaFieldExistsException.class)
    public void testUpdate_duplicateDiffField() {
        MetaClass m1 = createMetaClass();
        m1.setName("update2");
        
        MetaAttribute a = new MetaAttribute();
        a.setName("classOfService");
        a.setDataType(MetaField.DataTypeEnum.INTEGER);
        m1.addField(a);
        
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        ms.createMetaClass(m1, new MetadataContext());
        
        MetaClass m2 = createMetaClass();
        m2.setName("update2");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("classOfService");
        a1.setDataType(MetaField.DataTypeEnum.BOOLEAN);
        m2.addField(a1);
        
        ms.updateMetaClass(m2, metaContext);
    }

    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_datatype_int() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.INTEGER);
        b.setDefaultValue("ab");

        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_datatype_long() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.LONG);
        b.setDefaultValue("ab");

        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_datatype_double() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.DOUBLE);
        b.setDefaultValue("ab");

        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_datatype_boolean() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.BOOLEAN);
        b.setDefaultValue("ab");

        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }


    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_enum() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.ENUM);
        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_enum1() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.ENUM);
        List<String> enumValues = new ArrayList<String>();
        enumValues.add("TT");
        enumValues.add("TA");
        b.setEnumValues(enumValues);
        
        b.setDefaultValue("TC");
        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_enum2() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.ENUM);
        List<String> enumValues = new ArrayList<String>();
        enumValues.add("TT");
        enumValues.add("TA");
        enumValues.add("TT");
        b.setEnumValues(enumValues);
        
        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }

    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_dateDefault() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass metaClass = createMetaClass();
        
        MetaAttribute b = new MetaAttribute();
        b.setName("b");
        b.setDataType(DataTypeEnum.DATE);
        b.setDefaultValue("ab");
        
        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test
    public void testCreateValidation_dateDefault2() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();

        MetaClass metaClass = createMetaClass();
        metaClass.setName("create2");

        MetaAttribute b = new MetaAttribute();
        b.setName("dateMeta2");
        b.setDataType(DataTypeEnum.DATE);
        b.setDefaultValue("$NOW");

        metaClass.addField(b);
        ms.createMetaClass(metaClass, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_ref1() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass m1 = createMetaClass();
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Embedded);
        r1.setName("name1Ref");
        m1.addField(r1);
        
        ms.createMetaClass(m1, new MetadataContext());
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testCreateValidation_ref2() {

        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass m1 = createMetaClass();
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Reference);
        r1.setName("name1Ref");
        r1.setRefDataType("metaclass2");
        m1.addField(r1);
        
        ms.createMetaClass(m1, new MetadataContext());
        
        MetaClass getMeta = ms.getMetaClass(m1.getName());
        Assert.assertNotNull(getMeta);
        Assert.assertTrue(getMeta.getFields().size() > 1);
        Assert.assertNotNull(getMeta.getIndexes());
        Assert.assertTrue(getMeta.getIndexes().size() == 2);
        Assert.assertEquals(1, getMeta.getIndexesOnField("name1Ref").size());
    }
    
    @Test
    public void testBatchUpsert() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        //b1 ref b2, b2 refmap b1 , all create
        MetaClass b1 = createMetaClass();
        b1.setName("b1");
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Reference);
        r1.setName("r1");
        r1.setRefDataType("b2");
        b1.addField(r1);
        
        MetaClass b2 = createMetaClass();
        b2.setName("b2");
        b2.setParent(b1.getName());
        MetaRelationship r2 = new MetaRelationship();
        r2.setRelationType(RelationTypeEnum.Reference);
        r2.setName("r2");
        r2.setRefDataType(b1.getName());
        b2.addField(r2);
        
        ms.batchUpsert(Arrays.asList(b1, b2), metaContext);
        
        //b3 ref b1, b1 refmap b3, b1 update, b3 create
        b1 = createMetaClass();
        b1.setName("b1");
        MetaRelationship r3 = new MetaRelationship();
        r3.setRelationType(RelationTypeEnum.Reference);
        r3.setName("r3");
        r3.setRefDataType("b3");
        b1.addField(r3);
        
        //add a simple type for b2
        MetaClass updateB2 = createMetaClass();
        updateB2.setName(b2.getName());
        updateB2.setParent(b1.getName());
        MetaAttribute ma = new MetaAttribute();
        ma.setName("newB2Attribute");
        ma.setDataType(DataTypeEnum.STRING);
        updateB2.addField(ma);

        MetaClass b3 = createMetaClass();
        b3.setName("b3");
        MetaRelationship r4 = new MetaRelationship();
        r4.setRelationType(RelationTypeEnum.Reference);
        r4.setName("r4");
        r4.setRefDataType("b1");
        b3.addField(r4);
        
        ms.batchUpsert(Arrays.asList(b1, updateB2, b3), metaContext);
    }
    
    @Test
    public void testBatchUpsert_Override() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        //b1 ref b2, b2 refmap b1 , all create
        MetaClass parent = createMetaClass();
        parent.setName("OverrideParent");
        MetaAttribute attr1 = new MetaAttribute();
        attr1.setName("r1");
        attr1.setDataType(DataTypeEnum.INTEGER);
        attr1.setDefaultValue("0");
        parent.addField(attr1);
        
        MetaClass child = createMetaClass();
        child.setName("OverrideChild");
        child.setParent(parent.getName());
        
        ms.batchUpsert(Arrays.asList(parent, child), metaContext);
        
        child = createMetaClass();
        child.setName("OverrideChild");
        child.setParent(parent.getName());
        MetaAttribute attr2 = new MetaAttribute();
        attr2.setName("r1");
        attr2.setDataType(DataTypeEnum.INTEGER);
        attr2.setDefaultValue("1");
        child.addField(attr2);
        
        ms.batchUpsert(Arrays.asList(child), metaContext);
    }
    
    @Test(expected=IllegalMetaClassException.class)
    public void testBatchUpsert_exception1() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass be1 = createMetaClass();
        be1.setName("be1");
        MetaRelationship r1 = new MetaRelationship();
        r1.setRelationType(RelationTypeEnum.Reference);
        r1.setName("r1");
        r1.setRefDataType("be2");
        be1.addField(r1);
        
        MetaClass be2 = createMetaClass();
        be2.setName("be2");
        MetaRelationship r2 = new MetaRelationship();
        r2.setRelationType(RelationTypeEnum.Reference);
        r2.setName("r2");
        r2.setRefDataType("be3");
        be2.addField(r2);
        
        ms.batchUpsert(Arrays.asList(be1, be2), metaContext);
    }
    
    @Test
    public void testInherit() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp1 = createMetaClass();
        mp1.setName("mp1");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("a1");
        mp1.addField(a1);
        ms.createMetaClass(mp1, new MetadataContext());
        
        MetaClass mp2 = createMetaClass();
        mp2.setName("mp2");
        mp2.setParent("mp1");
        ms.createMetaClass(mp2, new MetadataContext());

        //mp2 extends from mp1, assert mp2 has field a1.
        MetaClass m = ms.getMetaClass("mp2");
        Assert.assertNotNull(m.getFieldByName("a1"));

        //update mp1, append field a2
        mp1 = createMetaClass();
        mp1.setName("mp1");
        MetaAttribute a2 = new MetaAttribute();
        a2.setName("a2");
        mp1.addField(a2);
        ms.updateMetaClass(mp1, metaContext);
        
        //assert can get field a2 from mp2
        m = ms.getMetaClass("mp2");
        Assert.assertNotNull(m.getFieldByName("a2"));
        
        Assert.assertTrue(ms.getMetaClass("mp1").isAssignableFrom(ms.getMetaClass("mp1")));
        Assert.assertTrue(ms.getMetaClass("mp1").isAssignableFrom(ms.getMetaClass("mp2")));
        
        Assert.assertFalse(ms.getMetaClass("mp2").isAssignableFrom(ms.getMetaClass("mp1")));
    }
    
    @Test (expected=MetaFieldExistsException.class)
    public void testInherit_createFieldConflict() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp11 = createMetaClass();
        mp11.setName("mp11");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("a1");
        a1.setDataType(DataTypeEnum.BOOLEAN);
        mp11.addField(a1);
        ms.createMetaClass(mp11, new MetadataContext());
        
        MetaClass mp12 = createMetaClass();
        mp12.setName("mp12");
        mp12.setParent("mp11");
        MetaAttribute a11 = new MetaAttribute();
        a11.setName("a1");
        a11.setDataType(DataTypeEnum.STRING);
        mp12.addField(a11);
        ms.createMetaClass(mp12, new MetadataContext());
    }
    
    @Test (expected=MetaFieldExistsException.class)
    public void testInherit_appendFieldToChildConflict() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp21 = createMetaClass();
        mp21.setName("mp21");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("a1");
        mp21.addField(a1);
        
        MetaClass mp22 = createMetaClass();
        mp22.setName("mp22");
        mp22.setParent("mp21");
        
        ms.batchUpsert(Arrays.asList(mp21, mp22), metaContext); 
        
        mp22 = createMetaClass();
        mp22.setName("mp22");
        mp22.setParent("mp21");
        MetaAttribute a11 = new MetaAttribute();
        a11.setName("a1");
        mp22.addField(a11);
        ms.updateMetaClass(mp22, metaContext);
    }
    
    @Test (expected=MetaFieldExistsException.class)
    public void testInherit_appendFieldToParentConflict() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp31 = createMetaClass();
        mp31.setName("mp31");
        MetaAttribute a1 = new MetaAttribute();
        a1.setName("a1");
        mp31.addField(a1);
        
        MetaClass mp32 = createMetaClass();
        mp32.setName("mp32");
        MetaAttribute a2 = new MetaAttribute();
        a2.setName("a2");
        mp32.addField(a2);
        mp32.setParent("mp31");
        
        ms.batchUpsert(Arrays.asList(mp31, mp32), metaContext); 
        
        mp31 = createMetaClass();
        mp31.setName("mp31");
        MetaAttribute a11 = new MetaAttribute();
        a11.setName("a2");
        mp31.addField(a11);
        ms.updateMetaClass(mp31, metaContext);
    }
    
    @Test (expected=IllegalMetaClassException.class)
    public void testInherit_referenceCircle() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp41 = createMetaClass();
        mp41.setName("mp41");
        mp41.setParent("mp42");
        
        MetaClass mp42 = createMetaClass();
        mp42.setName("mp42");
        mp42.setParent("mp41");
        
        ms.batchUpsert(Arrays.asList(mp42, mp42), metaContext); 
    }
    
    @Test
    public void testInherit_updateMetaClassSetParentToNull() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp51 = createMetaClass();
        mp51.setName("mp51");
        
        MetaClass mp52 = createMetaClass();
        mp52.setName("mp52");
        mp52.setParent("mp51");
        
        ms.batchUpsert(Arrays.asList(mp51, mp52), metaContext);
        
        mp52 = createMetaClass();
        mp52.setName("mp52");
        MetaAttribute a = new MetaAttribute();
        a.setName("ax");
        mp52.setParent("");
        ms.updateMetaClass(mp52, metaContext);
        
        MetaClass getChild = ms.getMetaClass("mp52");
        Assert.assertEquals("", getChild.getParent());
    }

//    @Test (expected=IllegalMetaClassException.class)
    @Test
    public void testInherit_updateParent() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();

        MetaClass mp61 = createMetaClass();
        mp61.setName("mp61");

        MetaClass mp62 = createMetaClass();
        mp62.setName("mp62");

        MetaClass mp63 = createMetaClass();
        mp63.setName("mp63");
        mp63.setParent("mp61");

        ms.batchUpsert(Arrays.asList(mp61, mp62, mp63), metaContext);

        mp63 = createMetaClass();
        mp63.setName("mp63");
        mp63.setParent("mp62");
        mp63.setDescription("new description updated");
        mp63.setAllowFullTableScan(true);
        ms.updateMetaClass(mp63, metaContext);

        MetaClass getMC = ms.getMetaClass(mp63.getName());
        Assert.assertEquals("mp62", getMC.getParent());
        Assert.assertEquals(true, getMC.isAllowFullTableScan());
        Assert.assertEquals("new description updated", getMC.getDescription());
        List<String> ancestors = getMC.getAncestors();
        Assert.assertTrue(ancestors.contains(mp62.getName()));
        Assert.assertTrue(!ancestors.contains(mp61.getName()));
    }
    
    @Test
    public void testInherit_updateParent2() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();

        MetaClass mp61 = createMetaClass();
        mp61.setName("mp61");

        MetaClass mp62 = createMetaClass();
        mp62.setName("mp62");
        mp62.setParent("mp61");

        MetaClass mp63 = createMetaClass();
        mp63.setName("mp63");
        mp63.setParent("mp62");

        ms.batchUpsert(Arrays.asList(mp61, mp62, mp63), metaContext);

        MetaClass updateMC = createMetaClass();
        updateMC.setName("mp61");
        updateMC.setParent("mp63");
        updateMC.setDescription("new description updated");
        try {
            ms.updateMetaClass(updateMC, metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            //expected 
        }

        //assert not changed
        MetaClass getMC = ms.getMetaClass(updateMC.getName());
        Assert.assertEquals(null, getMC.getParent());
        Assert.assertEquals(null, getMC.getDescription());
    }

    @Test (expected=IllegalMetaClassException.class)
    public void testInherit_parentRefSelf() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass mp72 = createMetaClass();
        mp72.setName("mp72");
        mp72.setParent("mp72");
        ms.createMetaClass(mp72, new MetadataContext());
    }
    
    @Test
    public void testInherit_updateParentVersion1() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();

        MetaClass mp81 = createMetaClass();
        mp81.setName("mp81");

        ms.batchUpsert(Arrays.asList(mp81), metaContext);

        mp81 = createMetaClass();
        mp81.setName("mp81");
        mp81.setDescription("new description updated");
        mp81.setAllowFullTableScan(true);
        ms.updateMetaClass(mp81, metaContext);

        MetaClass mp82 = createMetaClass();
        mp82.setName("mp82");
        mp82.setParent("mp81");

        MetaClass mp83 = createMetaClass();
        mp83.setName("mp83");
        mp83.setParent("mp82");
        
        ms.batchUpsert(Arrays.asList(mp82, mp83), metaContext);
        
        MetaClass getMp81 = ms.getMetaClass("mp81");
        MetaClass getMp82 = ms.getMetaClass("mp82");
        MetaClass getMp83 = ms.getMetaClass("mp83");
        
        Assert.assertEquals(1, getMp81.getVersion());
        Assert.assertEquals(0, getMp82.getVersion());
        Assert.assertEquals(1, getMp82.getParentVersion());
        Assert.assertEquals(0, getMp83.getVersion());
        Assert.assertEquals(0, getMp83.getParentVersion());
        
        
        mp81 = createMetaClass();
        mp81.setName("mp81");
        MetaAttribute metaAttr = new MetaAttribute();
        metaAttr.setName("mp81-attr1");
        mp81.addField(metaAttr);
        ms.updateMetaClass(mp81, metaContext);
        
        getMp81 = ms.getMetaClass("mp81");
        getMp82 = ms.getMetaClass("mp82");
        getMp83 = ms.getMetaClass("mp83");
        
        Assert.assertEquals(2, getMp81.getVersion());
        Assert.assertEquals(1, getMp82.getVersion());
        Assert.assertEquals(2, getMp82.getParentVersion());
        Assert.assertEquals(1, getMp83.getVersion());
        Assert.assertEquals(1, getMp83.getParentVersion());
        
        
        MetaClass mp84 = createMetaClass();
        mp84.setName("mp84");
        mp84.setParent("mp81");
        
        MetaClass mp85 = createMetaClass();
        mp85.setName("mp85");
        mp85.setParent("mp84");
        
        MetaClass mp86 = createMetaClass();
        mp86.setName("mp86");
        mp86.setParent("mp85");
        
        MetaClass mp87 = createMetaClass();
        mp87.setName("mp87");
        mp87.setParent("mp81");
        
        ms.batchUpsert(Arrays.asList(mp84, mp85, mp86, mp87), metaContext);
        
        getMp81 = ms.getMetaClass("mp81");
        getMp82 = ms.getMetaClass("mp82");
        getMp83 = ms.getMetaClass("mp83");
        MetaClass getMp84 = ms.getMetaClass("mp84");
        MetaClass getMp85 = ms.getMetaClass("mp85");
        MetaClass getMp86 = ms.getMetaClass("mp86");
        MetaClass getMp87 = ms.getMetaClass("mp87");
        
        Assert.assertEquals(2, getMp84.getParentVersion());
        Assert.assertEquals(0, getMp85.getParentVersion());
        Assert.assertEquals(0, getMp86.getParentVersion());
        Assert.assertEquals(2, getMp87.getParentVersion());
        
        // update mp81
        mp81 = createMetaClass();
        mp81.setName("mp81");
        MetaAttribute metaAttr2 = new MetaAttribute();
        metaAttr2.setName("mp81-attr2");
        metaAttr2.setDataType(DataTypeEnum.LONG);
        mp81.addField(metaAttr2);
        ms.updateMetaClass(mp81, metaContext);
        
        getMp81 = ms.getMetaClass("mp81");
        getMp82 = ms.getMetaClass("mp82");
        getMp83 = ms.getMetaClass("mp83");
        getMp84 = ms.getMetaClass("mp84");
        getMp85 = ms.getMetaClass("mp85");
        getMp86 = ms.getMetaClass("mp86");
        getMp87 = ms.getMetaClass("mp87");
        
        Assert.assertEquals(3, getMp81.getVersion());
        Assert.assertEquals(2, getMp82.getVersion());
        Assert.assertEquals(3, getMp82.getParentVersion());
        Assert.assertEquals(2, getMp83.getVersion());
        Assert.assertEquals(2, getMp83.getParentVersion());
        Assert.assertEquals(1, getMp84.getVersion());
        Assert.assertEquals(3, getMp84.getParentVersion());
        Assert.assertEquals(1, getMp85.getVersion());
        Assert.assertEquals(1, getMp85.getParentVersion());
        Assert.assertEquals(1, getMp86.getVersion());
        Assert.assertEquals(1, getMp86.getParentVersion());
        Assert.assertEquals(1, getMp87.getVersion());
        Assert.assertEquals(3, getMp87.getParentVersion());
        
        //update mp84, mp81
        mp81 = createMetaClass();
        mp81.setName("mp81");
        MetaAttribute metaAttr3 = new MetaAttribute();
        metaAttr3.setName("mp81-attr3");
        mp81.addField(metaAttr3);
        
        mp84 = createMetaClass();
        mp84.setName("mp84");
        MetaAttribute metaAttr4 = new MetaAttribute();
        metaAttr4.setName("mp84-attr1");
        mp84.addField(metaAttr4);
        
        ms.batchUpsert(Arrays.asList(mp84, mp81), metaContext);
        
        getMp81 = ms.getMetaClass("mp81");
        getMp82 = ms.getMetaClass("mp82");
        getMp83 = ms.getMetaClass("mp83");
        getMp84 = ms.getMetaClass("mp84");
        getMp85 = ms.getMetaClass("mp85");
        getMp86 = ms.getMetaClass("mp86");
        getMp87 = ms.getMetaClass("mp87");
        
        Assert.assertEquals(4, getMp81.getVersion());
        Assert.assertEquals(3, getMp82.getVersion());
        Assert.assertEquals(4, getMp82.getParentVersion());
        Assert.assertEquals(3, getMp83.getVersion());
        Assert.assertEquals(3, getMp83.getParentVersion());
        Assert.assertEquals(3, getMp84.getVersion());
        Assert.assertEquals(4, getMp84.getParentVersion());
        Assert.assertEquals(3, getMp85.getVersion());
        Assert.assertEquals(3, getMp85.getParentVersion());
        Assert.assertEquals(3, getMp86.getVersion());
        Assert.assertEquals(3, getMp86.getParentVersion());
        Assert.assertEquals(2, getMp87.getVersion());
        Assert.assertEquals(4, getMp87.getParentVersion());
        
        // test updateMetaField
        mp81 = createMetaClass();
        mp81.setName("mp81");
        metaAttr2 = new MetaAttribute();
        metaAttr2.setName("mp81-attr2");
        metaAttr2.setDataType(DataTypeEnum.STRING);
        mp81.addField(metaAttr2);
        ms.updateMetaField(mp81, "mp81-attr2", metaContext);
        
        getMp81 = ms.getMetaClass("mp81");
        getMp82 = ms.getMetaClass("mp82");
        getMp83 = ms.getMetaClass("mp83");
        getMp84 = ms.getMetaClass("mp84");
        getMp85 = ms.getMetaClass("mp85");
        getMp86 = ms.getMetaClass("mp86");
        getMp87 = ms.getMetaClass("mp87");
        
        Assert.assertEquals(5, getMp81.getVersion());
        Assert.assertEquals(4, getMp82.getVersion());
        Assert.assertEquals(5, getMp82.getParentVersion());
        Assert.assertEquals(4, getMp83.getVersion());
        Assert.assertEquals(4, getMp83.getParentVersion());
        Assert.assertEquals(4, getMp84.getVersion());
        Assert.assertEquals(5, getMp84.getParentVersion());
        Assert.assertEquals(4, getMp85.getVersion());
        Assert.assertEquals(4, getMp85.getParentVersion());
        Assert.assertEquals(4, getMp86.getVersion());
        Assert.assertEquals(4, getMp86.getParentVersion());
        Assert.assertEquals(3, getMp87.getVersion());
        Assert.assertEquals(5, getMp87.getParentVersion());
    }
    
    @Test
    public void testInherit_updateParentVersion2() {
        IMetadataService ms = repositoryService.getRepository(repoName).getMetadataService();

        MetaClass mp91 = createMetaClass();
        mp91.setName("mp91");

        ms.batchUpsert(Arrays.asList(mp91), metaContext);

        mp91 = createMetaClass();
        mp91.setName("mp91");
        mp91.setDescription("new description updated");
        mp91.setAllowFullTableScan(false);
        
        MetaClass mp92 = createMetaClass();
        mp92.setName("mp92");
        ms.batchUpsert(Arrays.asList(mp91, mp92), metaContext);
        
        mp92 = createMetaClass();
        mp92.setName("mp92");
        mp92.setParent("mp91");
        ms.batchUpsert(Arrays.asList(mp92), metaContext);

        MetaClass getMp91 = ms.getMetaClass("mp91");
        Assert.assertEquals(1, getMp91.getVersion());
        MetaClass getMp92 = ms.getMetaClass("mp92");
        Assert.assertEquals(1, getMp92.getParentVersion());
        
        
        mp91 = createMetaClass();
        mp91.setName("mp91");
        mp91.setAllowFullTableScan(false);
        ms.updateMetaClass(mp91, metaContext);

        getMp92 = ms.getMetaClass("mp92");
        Assert.assertEquals(2, getMp92.getParentVersion());
        
        mp92 = createMetaClass();
        mp92.setName("mp92");
        mp92.setParent("");
        ms.updateMetaClass(mp92, metaContext);
        
        getMp92 = ms.getMetaClass("mp92");
        Assert.assertEquals(0, getMp92.getParentVersion());        
    }

    @Test
    public void testGetMetaClasses() {
        MetadataContext metaContext = new MetadataContext();
        IMetadataService ms = repositoryService.getRepository("raptor-paas").getMetadataService();
        List<MetaClass> metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(34, metas.size());
        
        ms = repositoryService.getRepository("software-deployment").getMetadataService();
        metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(16, metas.size());

        ms = repositoryService.createRepository(new Repository("newCreatedForGetMetaClasses")).getMetadataService();
        metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(2, metas.size());
    }

    @Test
    public void testGetMetaClass_refresh() {
        MetadataContext metaContext = new MetadataContext();
        IMetadataService ms = repositoryService.getRepository("raptor-paas").getMetadataService();
        List<MetaClass> metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(34, metas.size());
        // force refresh
        metaContext.setRefreshMetadata(true);
        List<MetaClass> newMetas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(34, newMetas.size());
        for (final MetaClass oldMeta : metas) {
            Object obj = CollectionUtils.find(newMetas, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    if (object == oldMeta) {
                        return true;
                    }
                    return false;
                }
            });
            Assert.assertNull(obj);
        }
        // cached get
        metaContext.setRefreshMetadata(false);
        List<MetaClass> new2metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(34, new2metas.size());
        for (final MetaClass newMeta : newMetas) {
            Object obj = CollectionUtils.find(new2metas, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    if (object == newMeta) {
                        return true;
                    }
                    return false;
                }
            });
            Assert.assertNotNull(obj);
        }
    }

    @Test
    public void testCheckMetaClassOverride() {
        IMetadataService ms = repositoryService.getRepository("raptor-paas").getMetadataService();
        List<MetaClass> metas = ms.getMetaClasses(metaContext);
        Assert.assertEquals(34, metas.size());
        
        MetaClass empMeta = ms.getMetaClass("Employee");
        // age field
        MetaAttribute empAgeAttr = (MetaAttribute)empMeta.getFieldByName("age");
        Assert.assertNull(empAgeAttr.getDefaultValue());
        Assert.assertFalse(empAgeAttr.isMandatory());
        // get fields
        int fieldLen = empMeta.getFields().size();
        // name index
        IndexInfo empNameInfo = empMeta.getIndexByName("nameIndex");
        List<String> empNamekeys = empNameInfo.getKeyList();
        Assert.assertEquals(1, empNamekeys.size());
        Assert.assertEquals("name", empNamekeys.get(0));
        List<IndexOptionEnum> empNameOptions = empNameInfo.getIndexOptions();
        Assert.assertEquals(1, empNameOptions.size());
        Assert.assertEquals(IndexOptionEnum.unique, empNameOptions.get(0));
        // age title index
        IndexInfo empAgeTitleInfo = empMeta.getIndexByName("ageTitleIndex");
        List<String> empAgeTitleKeys = empAgeTitleInfo.getKeyList();
        Assert.assertEquals(2, empAgeTitleKeys.size());
        Assert.assertEquals("age", empAgeTitleKeys.get(0));
        Assert.assertEquals("title", empAgeTitleKeys.get(1));
        List<IndexOptionEnum> empAgeTitleOptions = empAgeTitleInfo.getIndexOptions();
        Assert.assertEquals(0, empAgeTitleOptions.size());
        
        
        MetaClass workerMeta = ms.getMetaClass("Worker");
        // age field
        MetaAttribute workerAgeAttr = (MetaAttribute)workerMeta.getFieldByName("age");
        Assert.assertEquals("28", workerAgeAttr.getDefaultValue());
        Assert.assertTrue(workerAgeAttr.isMandatory());
        // get fields
        Collection<MetaField> workerFields = workerMeta.getFields();
        Assert.assertEquals(fieldLen, workerFields.size());
        // name index
        IndexInfo workerNameInfo = workerMeta.getIndexByName("nameIndex");
        List<String> workerKeys = workerNameInfo.getKeyList();
        Assert.assertEquals(1, workerKeys.size());
        Assert.assertEquals("name", workerKeys.get(0));
        List<IndexOptionEnum> workerNameOptions = workerNameInfo.getIndexOptions();
        Assert.assertEquals(1, workerNameOptions.size());
        Assert.assertEquals(IndexOptionEnum.hashed, workerNameOptions.get(0));
        
        
        MetaClass engMeta = ms.getMetaClass("Engineer");
        // age field
        MetaAttribute engAgeAttr = (MetaAttribute)engMeta.getFieldByName("age");
        Assert.assertEquals("34", engAgeAttr.getDefaultValue());
        Assert.assertTrue(engAgeAttr.isMandatory());
        // field length
        Collection<MetaField> engFields = engMeta.getFields();
        Assert.assertEquals(fieldLen, engFields.size());
        // name index
        IndexInfo engNameInfo = engMeta.getIndexByName("nameIndex");
        List<String> engNamekeys = engNameInfo.getKeyList();
        Assert.assertEquals(1, engNamekeys.size());
        Assert.assertEquals("name", engNamekeys.get(0));
        List<IndexOptionEnum> engNameOptions = engNameInfo.getIndexOptions();
        Assert.assertEquals(1, engNameOptions.size());
        Assert.assertEquals(IndexOptionEnum.unique, engNameOptions.get(0));
        // age title index
        IndexInfo engAgeTitleInfo = engMeta.getIndexByName("ageTitleIndex");
        List<String> engAgeTitleKeys = engAgeTitleInfo.getKeyList();
        Assert.assertEquals(3, engAgeTitleKeys.size());
        Assert.assertEquals("company", engAgeTitleKeys.get(0));
        Assert.assertEquals("title", engAgeTitleKeys.get(1));
        Assert.assertEquals("age", engAgeTitleKeys.get(2));
        List<IndexOptionEnum> engAgeTitleOptions = engAgeTitleInfo.getIndexOptions();
        Assert.assertEquals(1, engAgeTitleOptions.size());
        Assert.assertEquals(IndexOptionEnum.unique, engAgeTitleOptions.get(0));
    }

    @Test
    public void testDeleteMetaClass() {
        String pooledClusterMetaName = "PooledCluster";
        String resourceContainerName = "ResourceContainer";
        IMetadataService metaService = repositoryService.getRepository("stratus-ci").getMetadataService();
        
        MetaClass resourceContainerMeta = metaService.getMetaClass(resourceContainerName);
        
        // pre delete assertion
        MetaClass resourceMeta = metaService.getMetaClass(pooledClusterMetaName);
        List<MetaRelationship> inReference = resourceMeta.getFromReference();
        Set<String> resourceInNames = new HashSet<String>();
        for (MetaRelationship rel : inReference) {
            resourceInNames.add(rel.getSourceDataType());
        }
        Assert.assertTrue(resourceInNames.contains(resourceContainerName));
        // pre delete assertion
        MetaClass baseMeta = metaService.getMetaClass("Base");
        List<MetaClass> descentClass = baseMeta.getDescendants();
        Assert.assertTrue(descentClass.contains(resourceContainerMeta));

        // delete metaclass
        MetadataContext metaContext = new MetadataContext();
        metaService.deleteMetaClass(resourceContainerName, metaContext);

        MetaClass mc = metaService.getMetaClass(resourceContainerName);
        Assert.assertNull(mc);

        // make sure the deleted metaclass's out reference meta in MetaGraph are
        // updated.
        MetaClass resourceMeta2 = metaService.getMetaClass(pooledClusterMetaName);
        List<MetaRelationship> inReference2 = resourceMeta2.getFromReference();
        Set<String> resourceInNames2 = new HashSet<String>();
        for (MetaRelationship rel : inReference2) {
            resourceInNames2.add(rel.getSourceDataType());
        }
        Assert.assertFalse(resourceInNames2.contains(resourceContainerName));
        
        // make sure parent's descendants are updated in meta graph.
        baseMeta = metaService.getMetaClass("Base");
        descentClass = baseMeta.getDescendants();
        Assert.assertFalse(descentClass.contains(resourceContainerMeta));
    }

    @Test(expected = MetaClassNotExistsException.class)
    public void testDeleteMetaClasses01NotExistingMeta() {
        repo.getMetadataService().deleteMetaClass("any-meta-class", null);
    }

    @Test(expected = MetaDataException.class)
    public void testDeleteMetaClasses02StillRerferencing() {
        IMetadataService metaService = repositoryService.getRepository("stratus-ci").getMetadataService();
        MetadataContext metaContext = new MetadataContext();
        metaService.deleteMetaClass("Base", metaContext);
    }

    @Test
    public void testDeleteMetaField() {
        IMetadataService metaService = repositoryService.getRepository("software-deployment").getMetadataService();
        MetadataContext metaContext = new MetadataContext();

        String className = "ServiceCluster";
        metaService.deleteMetaField(className, "name", metaContext);
        MetaClass meta = metaService.getMetaClass(className);
        Assert.assertNull(meta.getFieldByName("name"));
    }
    
    @Test
    public void testDeleteMetaRelationshipAndIndex() {
        IMetadataService metaService = repositoryService.getRepository("software-deployment").getMetadataService();
        MetadataContext metaContext = new MetadataContext();
        String className = "ServiceCluster";
        
        MetaClass meta = metaService.getMetaClass(className);
        Assert.assertNotNull(meta.getFieldByName("poolClusters"));
        Assert.assertNotNull(meta.getIndexes());
        Assert.assertEquals(1, meta.getIndexesOnField("poolClusters").size());

        metaService.deleteMetaField(className, "poolClusters", metaContext);
        
        MetaClass getMeta = metaService.getMetaClass(className);
        Assert.assertNull(getMeta.getFieldByName("poolClusters"));
        Assert.assertNotNull(getMeta.getIndexes());
        Assert.assertTrue(getMeta.getIndexes().size() == 2);
        Assert.assertEquals(0, getMeta.getIndexesOnField("poolClusters").size());        
    }

    @Test(expected=MetaFieldNotExistsException.class)
    public void testDeleteMetaField01NotExisting() {
        IMetadataService metaService = repositoryService.getRepository("stratus-ci").getMetadataService();
        MetadataContext metaContext = new MetadataContext();
        metaService.deleteMetaField("Base", "noSuchField", metaContext);
    }

    @Test(expected = MetaClassNotExistsException.class)
    public void testDeleteMetaField02MetaNotExisting() {
        IMetadataService metaService = repositoryService.getRepository("stratus-ci").getMetadataService();
        MetadataContext metaContext = new MetadataContext();
        metaService.deleteMetaField("NoSuchBase", "noSuchField", metaContext);
    }

    @Test
    public void testGetRepositories() {
        Repository repository = new Repository("repo1");
        repository.setAccessType(AccessType.Private);
        repositoryService.createRepository(repository);

        List<Repository> publics = repositoryService.getRepositories(AccessType.Public);
        Assert.assertTrue(publics.size() > 0);
        List<Repository> privates = repositoryService.getRepositories(AccessType.Private);
        Assert.assertTrue(privates.size() > 0);
    }

    @Test
    public void testAddEmbedReference() {
        // create version meta
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass versionMeta = new MetaClass();
        versionMeta.setName("CassiniManifestVersion");
        versionMeta.setEmbed(true);
        versionMeta.setRepository(repoName);

        MetaField metaAttr = new MetaAttribute();
        metaAttr.setName("name");

        versionMeta.addField(metaAttr);
        metaService.createMetaClass(versionMeta, new MetadataContext());

        // case 1: create manifest meta
        MetaClass manifestMeta = new MetaClass();
        manifestMeta.setName("CassiniManifest");
        manifestMeta.setRepository(repoName);

        MetaField manifestAttr = new MetaAttribute();
        manifestAttr.setName("name");
        manifestMeta.addField(manifestAttr);

        MetaRelationship versionRel = new MetaRelationship();
        versionRel.setName("versions");
        versionRel.setRefDataType(versionMeta.getName());
        versionRel.setRelationType(RelationTypeEnum.Embedded);
        manifestMeta.addField(versionRel);

        metaService.createMetaClass(manifestMeta, new MetadataContext());

        // now assert
        MetaClass getManifest = metaService.getMetaClass(manifestMeta.getName());
        Assert.assertNotNull(getManifest);
        Assert.assertNotNull(getManifest.getFieldByName("versions"));
        Assert.assertNotNull(getManifest.getFieldByName("versions").getDataType().equals(DataTypeEnum.RELATIONSHIP));
        MetaRelationship getRel = (MetaRelationship) getManifest.getFieldByName("versions");
        Assert.assertNotNull(getRel.getRelationType() == RelationTypeEnum.Embedded);

        // case 2: add reference to an embed
        MetaClass newManifestMeta = new MetaClass();
        newManifestMeta.setName("StratusManifest");
        newManifestMeta.setRepository(repoName);

        MetaField newManifestAttr = new MetaAttribute();
        newManifestAttr.setName("name");
        newManifestMeta.addField(newManifestAttr);

        MetaRelationship newVersionRel = new MetaRelationship();
        newVersionRel.setName("versions");
        newVersionRel.setRefDataType("CassiniManifest.versions");
        newVersionRel.setRelationType(RelationTypeEnum.Reference);

        metaService.createMetaClass(newManifestMeta, new MetadataContext());
    }
    
    private static String generateRandomMetaName(String baseMetaName) {
        return baseMetaName + random.nextLong();
    }

    @Test
    public void relationshipValidation1EmbedNonEmbed() {
        // create non-embed version meta
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass versionMeta = new MetaClass();
        versionMeta.setName(generateRandomMetaName("CassiniManifestVersion"));
        versionMeta.setRepository(repoName);
        MetaField metaAttr = new MetaAttribute();
        metaAttr.setName("name");
        versionMeta.addField(metaAttr);
        metaService.createMetaClass(versionMeta, new MetadataContext());

        // create manifest meta try to ref the non-embed meta as embed
        MetaClass manifestMeta = new MetaClass();
        manifestMeta.setName(generateRandomMetaName("CassiniManifest"));
        manifestMeta.setRepository(repoName);
        MetaField manifestAttr = new MetaAttribute();
        manifestAttr.setName("name");
        manifestMeta.addField(manifestAttr);
        MetaRelationship versionRel = new MetaRelationship();
        versionRel.setName("versions");
        versionRel.setRefDataType(versionMeta.getName());
        versionRel.setRelationType(RelationTypeEnum.Embedded);
        manifestMeta.addField(versionRel);
        try {
            metaService.createMetaClass(manifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            System.out.println(imce.getMessage());
            Assert.assertTrue(imce.getMessage().contains("must be embed metaclass"));
        }
    }
    
    @Test
    public void relationshipValidation2EmbedDot() {
        // create embed version meta
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass versionMeta = new MetaClass();
        versionMeta.setName(generateRandomMetaName("CassiniManifestVersion"));
        versionMeta.setRepository(repoName);
        versionMeta.setEmbed(true);
        MetaField metaAttr = new MetaAttribute();
        metaAttr.setName("name");
        versionMeta.addField(metaAttr);
        metaService.createMetaClass(versionMeta, new MetadataContext());

        // create manifest meta try to embed the version meta
        MetaClass manifestMeta = new MetaClass();
        String versionMetaName = generateRandomMetaName("CassiniManifest");
        manifestMeta.setName(versionMetaName);
        manifestMeta.setRepository(repoName);
        MetaField manifestAttr = new MetaAttribute();
        manifestAttr.setName("name");
        manifestMeta.addField(manifestAttr);
        MetaRelationship versionRel = new MetaRelationship();
        versionRel.setName("versions");
        versionRel.setRefDataType(versionMeta.getName());
        versionRel.setRelationType(RelationTypeEnum.Embedded);
        manifestMeta.addField(versionRel);
        metaService.createMetaClass(manifestMeta, new MetadataContext());
        
        // stratus manifest try to embed the ManifestVersion in Manifest
        MetaClass newManifestMeta = new MetaClass();
        String manifestMetaName = generateRandomMetaName("StratusManifest");
        newManifestMeta.setName(manifestMetaName);
        newManifestMeta.setRepository(repoName);

        MetaRelationship newVersionRel = new MetaRelationship();
        newVersionRel.setName("versions");
        newVersionRel.setRelationType(RelationTypeEnum.Embedded);
        newVersionRel.setRefDataType(manifestMetaName + "." + versionRel.getName());
        newManifestMeta.addField(newVersionRel);
        try {
            metaService.createMetaClass(newManifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("could not reference embed metaclass"));
        }
    }
    
    @Ignore
    @Test
    public void relationshipValidation3DotReference() {
        // create embed version meta
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass versionMeta = new MetaClass();
        String versionMetaName = generateRandomMetaName("CassiniManifestVersion");
        versionMeta.setName(versionMetaName);
        versionMeta.setRepository(repoName);
        versionMeta.setEmbed(true);
        MetaField metaAttr = new MetaAttribute();
        metaAttr.setName("name");
        versionMeta.addField(metaAttr);
        metaService.createMetaClass(versionMeta, new MetadataContext());

        // create manifest meta try to embed the version meta
        MetaClass manifestMeta = new MetaClass();
        String cassiniManifestMetaName = generateRandomMetaName("CassiniManifest");
        manifestMeta.setName(cassiniManifestMetaName);
        manifestMeta.setRepository(repoName);
        MetaField manifestAttr = new MetaAttribute();
        manifestAttr.setName("name");
        manifestMeta.addField(manifestAttr);
        MetaRelationship versionRel = new MetaRelationship();
        versionRel.setName("versions");
        versionRel.setRefDataType(versionMeta.getName());
        versionRel.setRelationType(RelationTypeEnum.Embedded);
        manifestMeta.addField(versionRel);
        metaService.createMetaClass(manifestMeta, new MetadataContext());
        
        // create a ref meta "reference" to the manifest
        MetaClass serviceInstanceMeta = new MetaClass();
        String serviceInstaceMetaName = generateRandomMetaName("CassiniServiceInstance");
        serviceInstanceMeta.setName(serviceInstaceMetaName);
        serviceInstanceMeta.setRepository(repoName);
        MetaRelationship manifestRel = new MetaRelationship();
        manifestRel.setName("manifests");
        manifestRel.setRefDataType(cassiniManifestMetaName);
        manifestRel.setRelationType(RelationTypeEnum.Reference);
        serviceInstanceMeta.addField(manifestRel);
        metaService.createMetaClass(serviceInstanceMeta, new MetadataContext());

        // stratus manifest try to embed the ManifestVersion in Manifest
        MetaClass newManifestMeta = new MetaClass();
        String newManifestMetaName = generateRandomMetaName("StratusManifest");
        newManifestMeta.setName(newManifestMetaName);
        newManifestMeta.setRepository(repoName);

        MetaRelationship newVersionRel = new MetaRelationship();
        newVersionRel.setName("versions");
        newVersionRel.setRelationType(RelationTypeEnum.Reference);
        newVersionRel.setRefDataType(newManifestMetaName + "a." + versionRel.getName());
        newManifestMeta.addField(newVersionRel);
        try {
            metaService.createMetaClass(newManifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("could not reference embed metaclass"));
        }
        
        // case 2
        newVersionRel.setRefDataType(versionMetaName + ".name");
        try {
            metaService.createMetaClass(newManifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("could not reference embed metaclass"));
        }

        // case 3
        newVersionRel.setRefDataType(cassiniManifestMetaName + ".name");
        try {
            metaService.createMetaClass(newManifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("could not reference embed metaclass"));
        }
        
        // case 4
        newVersionRel.setRefDataType(serviceInstaceMetaName + ".manifests");
        try {
            metaService.createMetaClass(newManifestMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("could not reference embed metaclass"));
        }
        
    }
    
    @Test
    public void relationshipValidation4EmbedRefDataType() {
        // create embed version meta
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass personMeta = new MetaClass();
        personMeta.setName(generateRandomMetaName("Person"));
        personMeta.setRepository(repoName);
        personMeta.setEmbed(true);
        MetaField metaAttr1 = new MetaAttribute();
        metaAttr1.setName("name");
        personMeta.addField(metaAttr1);
        metaService.createMetaClass(personMeta, new MetadataContext());
        
        MetaClass teamMeta = new MetaClass();
        teamMeta.setName(generateRandomMetaName("Team"));
        teamMeta.setRepository(repoName);
        teamMeta.setEmbed(true);
        MetaField metaAttr2 = new MetaAttribute();
        metaAttr2.setName("name");
        teamMeta.addField(metaAttr2);
        MetaRelationship personRel = new MetaRelationship();
        personRel.setName("person");
        personRel.setRefDataType(personMeta.getName());
        personRel.setRelationType(RelationTypeEnum.Embedded);
        teamMeta.addField(personRel);
        metaService.createMetaClass(teamMeta, new MetadataContext());
        
        MetaClass depMeta = new MetaClass();
        depMeta.setName(generateRandomMetaName("Dep"));
        depMeta.setRepository(repoName);
        depMeta.setEmbed(true);
        MetaField metaAttr3 = new MetaAttribute();
        metaAttr3.setName("name");
        depMeta.addField(metaAttr3);
        MetaRelationship teamRel = new MetaRelationship();
        teamRel.setName("team");
        teamRel.setRefDataType(teamMeta.getName());
        teamRel.setRelationType(RelationTypeEnum.Embedded);
        depMeta.addField(teamRel);
        metaService.createMetaClass(depMeta, new MetadataContext());

        MetaClass groupMeta = new MetaClass();
        String groupMetaName = generateRandomMetaName("Group");
        groupMeta.setName(groupMetaName);
        groupMeta.setRepository(repoName);
        MetaField manifestAttr = new MetaAttribute();
        manifestAttr.setName("name");
        groupMeta.addField(manifestAttr);
        MetaRelationship customerRel = new MetaRelationship();
        customerRel.setName("customer");
        customerRel.setRefDataType(personMeta.getName());
        customerRel.setRelationType(RelationTypeEnum.Reference);
        groupMeta.addField(customerRel);
        
        try {
            metaService.createMetaClass(groupMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException imce) {
            // expected
            Assert.assertTrue(imce.getMessage().contains("cannot be referenced to embedded class"));
        }
    }
    
    @Test
    public void testUpdateSingleAttribute(){
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass g1Meta01 = new MetaClass();
        g1Meta01.setName("g1");
        g1Meta01.setRepository(repoName);
        List<String> enumList = new ArrayList<String>();
        enumList.add("12");
        enumList.add("13");
        MetaAttribute metaAttr1 = new MetaAttribute();
        metaAttr1.setName("level");
        metaAttr1.setCardinality(CardinalityEnum.Many);
        metaAttr1.setConstant(true);
        metaAttr1.setDataType(DataTypeEnum.ENUM);
        metaAttr1.setDefaultValue("12");
        metaAttr1.setDescription("le01");
        metaAttr1.setEnumValues(enumList);
//        metaAttr1.setExpression("lily");
        metaAttr1.setMandatory(false);
        metaAttr1.setValidation("jojo");
        metaAttr1.setVirtual(false);
        g1Meta01.addField(metaAttr1);
        metaService.createMetaClass(g1Meta01, new MetadataContext());
        
        MetaClass g1Get01 = metaService.getMetaClass("g1");
        String dbName = g1Get01.getFieldByName("level").getDbName();
        String valueDbName = g1Get01.getFieldByName("level").getValueDbName();
        
        MetaClass g1Meta02 = new MetaClass();
        g1Meta02.setName("g1");
        g1Meta02.setRepository("raptor-paas");
        MetaRelationship metaRel1 = new MetaRelationship();
        metaRel1.setName("level");
        metaRel1.setDataType(MetaField.DataTypeEnum.RELATIONSHIP);
        metaRel1.setCascade(false);
        g1Meta02.addField(metaRel1);
        
        try {
            metaService.updateMetaField(g1Meta02, "level", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals(e.getMessage(), "refDataType must point to referenced metaclass name!");
        }
        
        MetaClass g1Meta03 = new MetaClass();
        g1Meta03.setName("g1");
        g1Meta03.setRepository("raptor-paas");
        MetaRelationship metaRel2 = new MetaRelationship();
        metaRel2.setName("level");
        metaRel2.setDataType(MetaField.DataTypeEnum.RELATIONSHIP);
        metaRel2.setCascade(false);
        metaRel2.setRefDataType("g1");
        g1Meta03.addField(metaRel2);
        
        try {
            metaService.updateMetaField(g1Meta03, "level", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals(e.getMessage(), "Can't change field type (Attribute <=> Reference)!");
        }
        
        
        MetaClass g1Meta04 = new MetaClass();
        g1Meta04.setName("g1");
        g1Meta04.setRepository("raptor-paas");
        MetaAttribute metaAttr2 = new MetaAttribute();
        metaAttr2.setName("level");
        metaAttr2.setDataType(MetaField.DataTypeEnum.ENUM);
        enumList.add("14");
        metaAttr2.setEnumValues(enumList);
        metaAttr2.setDefaultValue("14");
        g1Meta04.addField(metaAttr2);
        
        
        try {
            metaService.updateMetaField(g1Meta04, "level", metaContext);
        } catch (IllegalMetaClassException e) {
            e.printStackTrace();
        }
        
        
        MetaClass g1Get02 = metaService.getMetaClass("g1");
        MetaAttribute ma02 = (MetaAttribute) g1Get02.getFieldByName("level");
        
        Assert.assertEquals(dbName, ma02.getDbName());
        Assert.assertEquals(valueDbName, ma02.getValueDbName());
        Assert.assertEquals("14", ma02.getDefaultValue());
        Assert.assertNull(ma02.getDescription());
        
        
        metaService.deleteMetaClass("g1", new MetadataContext());
    }
    
    @Test
    public void testUpdateSingleRelationship01(){
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass g2 = new MetaClass();
        g2.setName("g2");
        g2.setRepository(repoName);
        metaService.createMetaClass(g2, new MetadataContext());
        MetaClass g3 = new MetaClass();
        g3.setName("g3");
        g3.setRepository(repoName);
        metaService.createMetaClass(g3, new MetadataContext());
        
        MetaClass g1Meta01 = new MetaClass();
        g1Meta01.setName("g1");
        g1Meta01.setRepository(repoName);
        MetaRelationship metaRela01 = new MetaRelationship();
        metaRela01.setName("level");
        metaRela01.setCardinality(CardinalityEnum.Many);
        metaRela01.setConstant(true);
        metaRela01.setDataType(DataTypeEnum.RELATIONSHIP);
        metaRela01.setMandatory(false);
        metaRela01.setVirtual(false);
        metaRela01.setRefDataType("g3");
        metaRela01.setConsistencyType(ConsistencyTypeEnum.Normal);
        g1Meta01.addField(metaRela01);
        metaService.createMetaClass(g1Meta01, new MetadataContext());
        
        MetaClass g1Get01 = metaService.getMetaClass("g1");
        String dbName = g1Get01.getFieldByName("level").getDbName();
        String valueDbName = g1Get01.getFieldByName("level").getValueDbName();
        
        MetaClass g1Meta02 = new MetaClass();
        g1Meta02.setName("g1");
        g1Meta02.setRepository("raptor-paas");
        MetaAttribute metaAttr1 = new MetaAttribute();
        metaAttr1.setName("level");
        metaAttr1.setDataType(MetaField.DataTypeEnum.INTEGER);
        g1Meta02.addField(metaAttr1);
        
        try {
            metaService.updateMetaField(g1Meta02, "level", metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertEquals(e.getMessage(), "Can't change field type (Attribute <=> Reference)!");
        }
        
        MetaClass g1Meta04 = new MetaClass();
        g1Meta04.setName("g1");
        g1Meta04.setRepository("raptor-paas");
        MetaRelationship metaRel2 = new MetaRelationship();
        metaRel2.setName("level");
        metaRel2.setDataType(MetaField.DataTypeEnum.RELATIONSHIP);
        metaRel2.setRefDataType("g2");
        metaRel2.setMandatory(true);
        g1Meta04.addField(metaRel2);
        
        metaService.updateMetaField(g1Meta04, "level", metaContext);

        MetaClass g1Get02 = metaService.getMetaClass("g1");
        MetaRelationship mr02 = (MetaRelationship) g1Get02.getFieldByName("level");
        
        Assert.assertEquals(dbName, mr02.getDbName());
        Assert.assertEquals(valueDbName, mr02.getValueDbName());
        Assert.assertEquals("g2", mr02.getRefDataType());
        Assert.assertEquals(ConsistencyTypeEnum.Normal, mr02.getConsistencyType());
        Assert.assertTrue(mr02.isMandatory());
        
        metaService.deleteMetaClass("g3", new MetadataContext());
        metaService.deleteMetaClass("g1", new MetadataContext());
        metaService.deleteMetaClass("g2", new MetadataContext());
    }
    
    @Test
    public void testUpdateSingleRelationship02(){
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass g2 = new MetaClass();
        g2.setName("g2");
        g2.setRepository(repoName);
        metaService.createMetaClass(g2, new MetadataContext());
        
        MetaClass g1Meta01 = new MetaClass();
        g1Meta01.setName("g1");
        g1Meta01.setRepository(repoName);
        MetaRelationship metaRela01 = new MetaRelationship();
        metaRela01.setName("level");
        metaRela01.setCardinality(CardinalityEnum.Many);
        metaRela01.setConstant(true);
        metaRela01.setDataType(DataTypeEnum.RELATIONSHIP);
        metaRela01.setMandatory(false);
        metaRela01.setVirtual(false);
        metaRela01.setRefDataType("g1");
        metaRela01.setConsistencyType(ConsistencyTypeEnum.Normal);
        g1Meta01.addField(metaRela01);
        metaService.createMetaClass(g1Meta01, new MetadataContext());
        
        MetaClass g1Get01 = metaService.getMetaClass("g1");
        String dbName = g1Get01.getFieldByName("level").getDbName();
        String valueDbName = g1Get01.getFieldByName("level").getValueDbName();
        
        MetaClass g1Meta04 = new MetaClass();
        g1Meta04.setName("g1");
        g1Meta04.setRepository("raptor-paas");
        MetaRelationship metaRel2 = new MetaRelationship();
        metaRel2.setName("level");
        metaRel2.setDataType(MetaField.DataTypeEnum.RELATIONSHIP);
        metaRel2.setRefDataType("g2");
        metaRel2.setMandatory(true);
        g1Meta04.addField(metaRel2);
        
        try {
            metaService.updateMetaField(g1Meta04, "level", metaContext);
        } catch (IllegalMetaClassException e) {
            e.printStackTrace();
        }
        
        MetaClass g1Get02 = metaService.getMetaClass("g1");
        MetaRelationship mr02 = (MetaRelationship) g1Get02.getFieldByName("level");
        
        Assert.assertEquals(dbName, mr02.getDbName());
        Assert.assertEquals(valueDbName, mr02.getValueDbName());
        Assert.assertEquals("g2", mr02.getRefDataType());
        Assert.assertEquals(ConsistencyTypeEnum.Normal, mr02.getConsistencyType());
        Assert.assertTrue(mr02.isMandatory());
        
        metaService.deleteMetaClass("g1", new MetadataContext());
    }

    @Test
    public void testCreateInnerClass() {
        // 1. Meta class cannot be both embed and inner
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass innerMeta = new MetaClass();
        innerMeta.setName("InnerClass-1");
        innerMeta.setRepository(repoName);
        innerMeta.setEmbed(true);
        innerMeta.setInner(true);
        try {
            metaService.createMetaClass(innerMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("meta class InnerClass-1 cannot be embed and inner"));
        }
        
        innerMeta.setEmbed(false);
        metaService.createMetaClass(innerMeta, new MetadataContext());
        
        MetaClass updateInnerMeta = new MetaClass();
        updateInnerMeta.setName("InnerClass-1");
        updateInnerMeta.setRepository(repoName);
        updateInnerMeta.setEmbed(true);
        List<MetaClass> metas = new ArrayList<MetaClass>(1);
        metas.add(updateInnerMeta);
        try {
            metaService.batchUpsert(metas, metaContext);
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("meta class InnerClass-1 cannot be embed and inner"));
        }
        
        // 2. Inner relationship cannot be strong
        MetaClass hostMeta1 = new MetaClass();
        hostMeta1.setName("HostClass-1");
        hostMeta1.setRepository(repoName);
        hostMeta1.setEmbed(false);
        hostMeta1.setInner(false);
        
        MetaRelationship innerRel1 = new MetaRelationship();
        innerRel1.setName("innerRelationship-1");
        innerRel1.setRefDataType(innerMeta.getName());
        innerRel1.setRelationType(RelationTypeEnum.Reference);
        innerRel1.setConsistencyType(ConsistencyTypeEnum.Strong);
        hostMeta1.addField(innerRel1);
        try {
            metaService.createMetaClass(hostMeta1, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("The strong relationship innerRelationship-1 cannot be referenced to inner class"));
        }
 
        // 3. Inner relationship cannot be mandatory
        innerRel1.setConsistencyType(ConsistencyTypeEnum.Normal);
        innerRel1.setRelationType(RelationTypeEnum.Inner);
        innerRel1.setMandatory(true);
        hostMeta1.addField(innerRel1);
        try {
            metaService.createMetaClass(hostMeta1, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("The inner relationship innerRelationship-1 cannot be mandatory!"));
        }
        
        // 4. Inner relationship reference to itself
        MetaClass hostMeta2 = new MetaClass();
        hostMeta2.setName("HostClass-2");
        hostMeta2.setRepository(repoName);        
        hostMeta2.setEmbed(false);
        hostMeta2.setInner(true);
        MetaRelationship innerRel2 = new MetaRelationship();
        innerRel2.setName("innerRelationship-2");
        innerRel2.setRefDataType(hostMeta2.getName());
        innerRel2.setRelationType(RelationTypeEnum.Inner);
        hostMeta2.addField(innerRel2);
        metaService.createMetaClass(hostMeta2, new MetadataContext());

        // 5. Update class to inner class
        MetaClass innerMeta3 = new MetaClass();
        innerMeta3.setName("InnerClass-3");
        innerMeta3.setRepository(repoName);
        innerMeta3.setEmbed(false);
        innerMeta3.setInner(false);
        metaService.createMetaClass(innerMeta3, new MetadataContext());
        
        innerMeta3.setInner(true);
        metaService.updateMetaClass(innerMeta3, metaContext);
      
        MetaClass hostMeta3 = new MetaClass();
        hostMeta3.setName("HostClass-3");
        hostMeta3.setRepository(repoName);
        hostMeta3.setEmbed(false);
        hostMeta3.setInner(false);
        
        MetaRelationship innerRel3 = new MetaRelationship();
        innerRel3.setName("innerRelationship-3");
        innerRel3.setRefDataType(innerMeta3.getName());
        innerRel3.setRelationType(RelationTypeEnum.Inner);
        hostMeta3.addField(innerRel3);
        metaService.createMetaClass(hostMeta3, new MetadataContext());
        
        innerMeta3.setInner(false);
        metaService.updateMetaClass(innerMeta3, metaContext);
        
        // 6. Normal case
        MetaClass hostMeta4 = new MetaClass();
        hostMeta4.setName("HostClass-4");
        hostMeta4.setRepository(repoName);
        hostMeta4.setEmbed(false);
        hostMeta4.setInner(false);
        MetaRelationship innerRel4 = new MetaRelationship();
        innerRel4.setName("innerRelationship-4");
        innerRel4.setRefDataType(innerMeta.getName());
        innerRel4.setRelationType(RelationTypeEnum.Inner);
        hostMeta4.addField(innerRel4);
        metaService.createMetaClass(hostMeta4, new MetadataContext());
        
        // 6. Strong inner relationship case
        MetaClass hostMeta5 = new MetaClass();
        hostMeta5.setName("HostClass-5");
        hostMeta5.setRepository(repoName);
        hostMeta5.setEmbed(false);
        hostMeta5.setInner(false);
        MetaRelationship innerRel5 = new MetaRelationship();
        innerRel5.setName("innerRelationship-5");
        innerRel5.setRefDataType(innerMeta.getName());
        innerRel5.setRelationType(RelationTypeEnum.Inner);
        innerRel5.setConsistencyType(ConsistencyTypeEnum.Strong);
        hostMeta5.addField(innerRel5);
        try {
            metaService.createMetaClass(hostMeta5, new MetadataContext());
            Assert.fail();
        } catch(IllegalMetaClassException e) {
            Assert.assertEquals("The inner relationship innerRelationship-5 cannot be strong!", e.getMessage());
        }
    }
    
    @Test
    public void testCreateEmbedStrongRelationship() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass embedMeta = new MetaClass();
        embedMeta.setName("EmbedClass-1");
        embedMeta.setRepository(repoName);
        embedMeta.setEmbed(true);
        metaService.createMetaClass(embedMeta, new MetadataContext());
        
        MetaClass rootMeta = new MetaClass();
        rootMeta.setName("RootClass-1");
        rootMeta.setRepository(repoName);
        
        MetaRelationship embedRel = new MetaRelationship();
        embedRel.setName("embedRel");
        embedRel.setRefDataType(embedMeta.getName());
        embedRel.setRelationType(RelationTypeEnum.Embedded);
        embedRel.setConsistencyType(ConsistencyTypeEnum.Strong);
        rootMeta.addField(embedRel);
        try {
            metaService.createMetaClass(rootMeta, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("The strong relationship embedRel cannot be referenced to embed class EmbedClass-1!"));
        }
    }
    
    @Test
    public void testUpdateClass() {
        // 1. Meta class cannot be both embed and inner
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        MetaClass newMeta = new MetaClass();
        newMeta.setName("UpdateClass-1");
        newMeta.setRepository(repoName);
        metaService.createMetaClass(newMeta, new MetadataContext());
        
        MetaClass updateMeta = new MetaClass();
        updateMeta.setName("UpdateClass-1");
        updateMeta.setRepository(repoName);
        updateMeta.setEmbed(true);
        updateMeta.setVersion(0);
        List<MetaClass> metas = new ArrayList<MetaClass>(1);
        metas.add(updateMeta);
        metaService.batchUpsert(metas, metaContext);
        
        MetaClass getUpdateMeta = metaService.getMetaClass("UpdateClass-1");
        Assert.assertEquals(1, getUpdateMeta.getVersion());
        
        MetaAttribute attr1 = new MetaAttribute();
        attr1.setName("attr1");
        updateMeta.addField(attr1);
        updateMeta.setVersion(2);
        try {
            metaService.batchUpsert(metas, metaContext);
            Assert.fail();
        } catch (MetaDataException e) {
            Assert.assertTrue(e.getMessage().contains("MetaClass version conflict: UpdateClass-1"));
        }
        
        updateMeta.setVersion(1);
        metaService.batchUpsert(metas, metaContext);
        
        getUpdateMeta = metaService.getMetaClass("UpdateClass-1");
        Assert.assertEquals(2, getUpdateMeta.getVersion());
    }
    
    @Test
    public void testCollectionCount() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        
        int count = metaService.getCollectionCount("Environment");
        Assert.assertEquals(0,  count);
    }
    
    @Test
    public void testEmbedMetaClassValidate() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass embedMeta = new MetaClass();
        embedMeta.setName("EmbedClass-2");
        embedMeta.setRepository(repoName);
        embedMeta.setEmbed(true);
        metaService.createMetaClass(embedMeta, new MetadataContext());
        
        MetaClass rootMeta = new MetaClass();
        rootMeta.setName("RootClass-2");
        rootMeta.setRepository(repoName);
        
        MetaRelationship embedRel = new MetaRelationship();
        embedRel.setName("embedRel");
        embedRel.setRefDataType(embedMeta.getName());
        embedRel.setRelationType(RelationTypeEnum.Embedded);
        rootMeta.addField(embedRel);
        
        metaService.createMetaClass(rootMeta, new MetadataContext());
        
        embedMeta.setEmbed(false);
        metaService.updateMetaClass(embedMeta, metaContext);
        
        try {
            metaService.validateMetaClass(embedMeta.getName());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("Target metaclass EmbedClass-2 of embed relationship embedRel in RootClass-2 must be embed metaclass!"));
        }
    }
    
    @Test
    public void testInnerMetaClassValidate() {
        IMetadataService metaService = repositoryService.getRepository(repoName).getMetadataService();
        
        MetaClass innerMeta = new MetaClass();
        innerMeta.setName("InnerClass-2");
        innerMeta.setRepository(repoName);
        innerMeta.setInner(true);
        metaService.createMetaClass(innerMeta, new MetadataContext());
        
        MetaClass rootMeta = new MetaClass();
        rootMeta.setName("RootClass-3");
        rootMeta.setRepository(repoName);
        
        MetaRelationship innerRel = new MetaRelationship();
        innerRel.setName("innerRel");
        innerRel.setRefDataType(innerMeta.getName());
        innerRel.setRelationType(RelationTypeEnum.Inner);
        rootMeta.addField(innerRel);
        
        metaService.createMetaClass(rootMeta, new MetadataContext());
        
        innerMeta.setInner(false);
        metaService.updateMetaClass(innerMeta, metaContext);
        
        try {
            metaService.validateMetaClass(innerMeta.getName());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            Assert.assertTrue(e.getMessage().contains("The inner relationship innerRel cannot be referenced to a non-inner class InnerClass-2!"));
        }
    }
    
    @Test
    public void testAncestorsPropagation() {
    	IMetadataService metaService = repo.getMetadataService();
    	
    	// create parent
    	MetaClass parent = new MetaClass();
		String parentName = "ParentClass";
		parent.setName(parentName);
		parent.setRepository(repoName);
		
		MetaField nameField = new MetaAttribute();
		nameField.setName("name");
		nameField.setDataType(DataTypeEnum.STRING);
		parent.addField(nameField);
		
		MetaField numberField = new MetaAttribute();
		numberField.setName("number");
		numberField.setDataType(DataTypeEnum.INTEGER);
		parent.addField(numberField);
		
		parent = metaService.createMetaClass(parent, new MetadataContext());
		
		// create self
    	MetaClass self = new MetaClass();
		String selfName = "SelfClass";
		self.setName(selfName);
		self.setRepository(repoName);
		self.setParent(parentName);
		
		MetaField ageField = new MetaAttribute();
		ageField.setName("age");
		ageField.setDataType(DataTypeEnum.INTEGER);
		ageField.setCardinality(CardinalityEnum.One);
		self.addField(ageField);
		
		self = metaService.createMetaClass(self, new MetadataContext());
		
		Assert.assertEquals(1, self.getAncestors().size());
		Assert.assertEquals(parentName, self.getAncestors().get(0));
		
		// create son
		MetaClass son = new MetaClass();
		String sonName = "SonClass";
		son.setName(sonName);
		son.setRepository(repoName);
		son.setParent(selfName);
		
		nameField = new MetaAttribute();
		nameField.setName("sonName");
		nameField.setDataType(DataTypeEnum.STRING);
		son.addField(nameField);
		
		numberField = new MetaAttribute();
		numberField.setName("sonNumber");
		numberField.setDataType(DataTypeEnum.INTEGER);
		son.addField(numberField);
		
		son = metaService.createMetaClass(son, new MetadataContext());
		
		Assert.assertEquals(2, son.getAncestors().size());
		Assert.assertTrue(son.getAncestors().contains(parentName));
		Assert.assertTrue(son.getAncestors().contains(selfName));
		
		// create grandChildren
		MetaClass grandChildren = new MetaClass();
		String grandChildrenName = "GrandChildrenClass";
		grandChildren.setName(grandChildrenName);
		grandChildren.setRepository(repoName);
		grandChildren.setParent(sonName);
		
		nameField = new MetaAttribute();
		nameField.setName("grandChildrenName");
		nameField.setDataType(DataTypeEnum.STRING);
		grandChildren.addField(nameField);
		
		numberField = new MetaAttribute();
		numberField.setName("grandChildrenNumber");
		numberField.setDataType(DataTypeEnum.INTEGER);
		grandChildren.addField(numberField);
		
		grandChildren = metaService.createMetaClass(grandChildren, new MetadataContext());
		
		Assert.assertEquals(3, grandChildren.getAncestors().size());
		Assert.assertTrue(grandChildren.getAncestors().contains(parentName));
		Assert.assertTrue(grandChildren.getAncestors().contains(selfName));
		Assert.assertTrue(grandChildren.getAncestors().contains(sonName));
		
		// update self
		MetaClass updateSelf = new MetaClass();
		updateSelf.setName(selfName);
		updateSelf.setParent("");
		updateSelf.setMetadataService(metaService);
		
		List<MetaClass> metas = new ArrayList<MetaClass>();
		metas.add(updateSelf);
		metaService.batchUpsert(metas, metaContext);
		
		// assert
		self = metaService.getMetaClass(selfName);
		Assert.assertTrue(self.getAncestors().isEmpty());
		
		son = metaService.getMetaClass(sonName);
		Assert.assertEquals(1, son.getAncestors().size());
		Assert.assertTrue(son.getAncestors().contains(selfName));
		
		grandChildren = metaService.getMetaClass(grandChildrenName);
		Assert.assertEquals(2, grandChildren.getAncestors().size());
		Assert.assertTrue(grandChildren.getAncestors().contains(sonName));
		Assert.assertTrue(grandChildren.getAncestors().contains(selfName));
		
		// switch parent from "" to "Parent2Class"
		// create another parent
    	MetaClass parent2 = new MetaClass();
		String parent2Name = "Parent2Class";
		parent2.setName(parent2Name);
		parent2.setRepository(repoName);
		
		nameField = new MetaAttribute();
		nameField.setName("name2");
		nameField.setDataType(DataTypeEnum.STRING);
		parent2.addField(nameField);
		
		numberField = new MetaAttribute();
		numberField.setName("number2");
		numberField.setDataType(DataTypeEnum.INTEGER);
		parent2.addField(numberField);
		
		parent2 = metaService.createMetaClass(parent2, new MetadataContext());
		
		updateSelf = new MetaClass();
		updateSelf.setName(selfName);
		updateSelf.setParent(parent2Name);
		updateSelf.setMetadataService(metaService);
		
		metas = new ArrayList<MetaClass>();
		metas.add(updateSelf);
		metaService.batchUpsert(metas, metaContext);
		
		// assert
		self = metaService.getMetaClass(selfName);
		Assert.assertEquals(1, self.getAncestors().size());
		Assert.assertTrue(self.getAncestors().contains(parent2Name));
		
		son = metaService.getMetaClass(sonName);
		Assert.assertEquals(2, son.getAncestors().size());
		Assert.assertTrue(son.getAncestors().contains(selfName));
		Assert.assertTrue(son.getAncestors().contains(parent2Name));
		
		grandChildren = metaService.getMetaClass(grandChildrenName);
		Assert.assertEquals(3, grandChildren.getAncestors().size());
		Assert.assertTrue(grandChildren.getAncestors().contains(sonName));
		Assert.assertTrue(grandChildren.getAncestors().contains(selfName));
		Assert.assertTrue(grandChildren.getAncestors().contains(parent2Name));
		
		// switch parent from "Parent2Class" to "Parent3Class"
		// create third parent
    	MetaClass parent3 = new MetaClass();
		String parent3Name = "Parent3Class";
		parent3.setName(parent3Name);
		parent3.setRepository(repoName);
		
		nameField = new MetaAttribute();
		nameField.setName("name3");
		nameField.setDataType(DataTypeEnum.STRING);
		parent3.addField(nameField);
		
		numberField = new MetaAttribute();
		numberField.setName("number3");
		numberField.setDataType(DataTypeEnum.INTEGER);
		parent3.addField(numberField);
		
		parent3 = metaService.createMetaClass(parent3, new MetadataContext());
		
		updateSelf = new MetaClass();
		updateSelf.setName(selfName);
		updateSelf.setParent(parent3Name);
		updateSelf.setMetadataService(metaService);
		
		metas = new ArrayList<MetaClass>();
		metas.add(updateSelf);
		metaService.batchUpsert(metas, metaContext);
		
		// assert
		self = metaService.getMetaClass(selfName);
		Assert.assertEquals(1, self.getAncestors().size());
		Assert.assertTrue(self.getAncestors().contains(parent3Name));
		
		son = metaService.getMetaClass(sonName);
		Assert.assertEquals(2, son.getAncestors().size());
		Assert.assertTrue(son.getAncestors().contains(selfName));
		Assert.assertTrue(son.getAncestors().contains(parent3Name));
		
		grandChildren = metaService.getMetaClass(grandChildrenName);
		Assert.assertEquals(3, grandChildren.getAncestors().size());
		Assert.assertTrue(grandChildren.getAncestors().contains(sonName));
		Assert.assertTrue(grandChildren.getAncestors().contains(selfName));
		Assert.assertTrue(grandChildren.getAncestors().contains(parent3Name));
    }

}
