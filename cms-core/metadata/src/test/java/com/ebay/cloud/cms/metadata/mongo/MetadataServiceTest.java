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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.ConsistencyTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.sequence.MongoSequence;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MetadataServiceTest extends CMSMongoTest {

    private static final String       RAPTOR_PAAS       = "raptor-paas";
    private static final String       SERVICE_INSTANCE  = "ServiceInstance";
    private static final String       repoName          = "MetadataServiceTest";
    private static IRepositoryService repositoryService = null;
    private static IMetadataService   metaService       = null;
    private static Repository         repo              = null;
    private static MongoSequence      sequence          = null;
    private static MongoDataSource    dataSource        = null;
    private static IMetadataService   raptorMetaService;

    @BeforeClass
    public static void createRepoService() {
        dataSource = new MongoDataSource(getConnectionString());
        MetadataDataLoader.getInstance(dataSource).loadTestDataFromResource();

        repositoryService = RepositoryServiceFactory.createRepositoryService(dataSource, "localCMSServer");
        repo = repositoryService.createRepository(new Repository(repoName));
        metaService = repo.getMetadataService();

        sequence = new MongoSequence(dataSource.getMongoInstance(), CMSConsts.SYS_DB, CMSConsts.SEQUENCE_COLL,
                CMSConsts.NEXT_FIELD_NAME_SEQ);

        Assert.assertEquals(repo, repo.getMetadataService().getRepository());

        raptorMetaService = repositoryService.getRepository(RAPTOR_PAAS).getMetadataService();
    }

    @Test(expected = ExpressionParseException.class)
    public void invalidExpressionCreate() {
        MetaClass metaClass = metaService.getMetaClass(SERVICE_INSTANCE);
        MetaAttribute attribute = new MetaAttribute(false);
        attribute.setName("CalName");
        attribute.setDataType(DataTypeEnum.STRING);
        attribute.setDbName(sequence.getNext());
        attribute.setExpression("$name/");
        metaClass.addField(attribute);
    }

    @Test
    public void test01AddCrossReference() {
        MetaRelationship relation = new MetaRelationship();
        String fieldName = "crossRepoRef";
        relation.setName(fieldName);
        relation.setRelationType(RelationTypeEnum.CrossRepository);
        relation.setRefDataType("Manifest");
        relation.setRefRepository("software-deployment");

        MetaClass metadata = new MetaClass();
        metadata.setRepository(RAPTOR_PAAS);
        metadata.setName("NewReference");
        metadata.addField(relation);
        
        MetaClass createdMeta = raptorMetaService.createMetaClass(metadata, new MetadataContext());
        Assert.assertNotNull(createdMeta.getFieldByName(fieldName));
        Assert.assertTrue(createdMeta.getFieldByName(fieldName).getDataType() == DataTypeEnum.RELATIONSHIP);
    }

    @Test
    public void test02AddCrossRepoNotExistingMeta() {
        MetaRelationship relation = new MetaRelationship();
        String fieldName = "crossRepoRef";
        relation.setName(fieldName);
        relation.setRelationType(RelationTypeEnum.CrossRepository);
        relation.setRefDataType("Manifest11"); // this is non-existing metadata
        relation.setRefRepository("software-deployment");

        MetaClass metadata = new MetaClass();
        metadata.setRepository(RAPTOR_PAAS);
        metadata.setName("NewReference02");
        metadata.addField(relation);

        MetadataContext metaContext = new MetadataContext();
        metaContext.setSourceIp("127.0.0.1");
        metaContext.setSubject("tester");
        
        try {
            raptorMetaService.createMetaClass(metadata, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            // expected
        }
    }

    @Test
    public void test03AddCrossRepoStrongRef() {
        MetaRelationship relation = new MetaRelationship();
        String fieldName = "crossRepoRef03";
        relation.setName(fieldName);
        relation.setRelationType(RelationTypeEnum.CrossRepository);
        relation.setRefDataType("Manifest");
        relation.setRefRepository("software-deployment");
        relation.setConsistencyType(ConsistencyTypeEnum.Strong);

        MetaClass metadata = new MetaClass();
        metadata.setRepository(RAPTOR_PAAS);
        metadata.setName("NewReference03");
        metadata.addField(relation);
        
        MetadataContext metaContext = new MetadataContext();
        metaContext.setSourceIp("127.0.0.1");
        metaContext.setSubject("tester");

        try {
            raptorMetaService.createMetaClass(metadata, new MetadataContext());
            Assert.fail();
        } catch (IllegalMetaClassException e) {
            // expected
        }
    }

}
