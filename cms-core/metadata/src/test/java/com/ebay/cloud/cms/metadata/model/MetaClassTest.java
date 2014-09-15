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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaFieldExistsException;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.mongo.MongoMetadataServiceImpl;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class MetaClassTest extends CMSMongoTest {

	private static final String APPLICATION_SERVICE = "ApplicationService";

	private static String RAPTOR_PAAS = "raptor-paas";

	private static Repository repo;
	
	private MetaValidator validator = new MetaValidator();

	@BeforeClass
	public static void setup() {
		MetadataDataLoader loader = MetadataDataLoader.getInstance(getDataSource());
		loader.loadTestDataFromResource();

		repo = RepositoryServiceFactory.createRepositoryService(getDataSource(), "localCMSServer").getRepository(
				RAPTOR_PAAS);
	}

	@Test
	public void testIsAssignableFrom() {
		MetaClass mClass = new MetaClass();

		MetaClass compareClass = new MetaClass();
		Assert.assertTrue(!mClass.isAssignableFrom(compareClass));
		compareClass.setName("compareClass");
		Assert.assertTrue(!mClass.isAssignableFrom(compareClass));

		mClass.setParent("parentClass");
		compareClass.setParent("parentClass");
		Assert.assertTrue(!mClass.isAssignableFrom(compareClass));

		try {
			mClass.getParentMetaClass();
			Assert.fail();
		} catch (MetaDataException e) {
			// expect - getParentMetaClass()
		}
	}

	@Test(expected = IllegalMetaClassException.class)
	public void testSetInternalClassFields() {
		MetaClass mClass = new MetaClass();

		List<MetaField> fields = new ArrayList<MetaField>();
		MetaField idField = InternalFieldFactory.getInternalMetaField(InternalFieldEnum.ID);
		fields.add(idField);
		mClass.setClassFields(fields);
	}

	@Test
	public void testGraphReady() {
		MetaClass mc = new MetaClass();

		try {
			mc.getFromReference();
			Assert.fail();
		} catch (MetaDataException e) {
			// expected graph setup()
		}
		try {
			mc.getToReference();
			Assert.fail();
		} catch (MetaDataException e) {
			// expected graph setup()
		}

		try {
			mc.getDescendants();
			Assert.fail();
		} catch (MetaDataException e) {
			// expected graph setup()
		}

	}

	@Test
	public void testCreateValidation() {
		MongoMetadataServiceImpl metaService = (MongoMetadataServiceImpl) repo.getMetadataService();

		MetaClass create = new MetaClass();
		create.setRepository(RAPTOR_PAAS);
		// 1. must have name
		try {
			metaService.createMetaClass(create, new MetadataContext());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			// validation
		}
		create.setName("mc1");

		// 2. must NOT have id
		try {
			create.setId(ObjectId.get().toString());
			metaService.createMetaClass(create, new MetadataContext());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			// validation
		}
		create.setId(null);

		// 3. must not have ancestor
		try {
			create.setParent(APPLICATION_SERVICE);
			create.setupAncestors(metaService, Collections.<String, MetaClass> emptyMap());
			metaService.createMetaClass(create, new MetadataContext());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			// validation
		}

		// 4. expression field must not be mandatory
		create = new MetaClass();
		create.setName("joke");
		create.setRepository(RAPTOR_PAAS);
		try {
			MetaAttribute label = new MetaAttribute();
			label.setName("label");
			label.setExpression("helloworld");
			label.setMandatory(true);
			create.addField(label);
			metaService.createMetaClass(create, new MetadataContext());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			// validation
		}

		create = new MetaClass();
		create.setName("joke");
		create.setRepository(RAPTOR_PAAS);
		try {
			MetaAttribute label = new MetaAttribute();
			label.setName("label");
			label.setExpression("");
			label.setMandatory(true);
			create.addField(label);
			metaService.createMetaClass(create, new MetadataContext());
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			Assert.fail();
			// validation
		}

		create = new MetaClass();
		create.setName("joke1");
		create.setRepository(RAPTOR_PAAS);
		try {
			MetaAttribute label = new MetaAttribute();
			label.setName("label");
			label.setExpression("helloworld");
			label.setMandatory(false);
			create.addField(label);
			metaService.createMetaClass(create, new MetadataContext());
		} catch (IllegalMetaClassException e) {
			System.out.println(e.getMessage());
			Assert.fail();
		}
	}

	@Test
	public void testIndexes() {
		MetaClass mc = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);
		Assert.assertNull(mc.getIndexByName("non-index"));
		Assert.assertNotNull(mc.getIndexByName("appNameIndex"));
		Assert.assertNotNull(mc.getIndexNames());
	}

	@Test
	public void testInternalFields() {
		MetaClass mc = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);

		Assert.assertNotNull(mc.getFieldByDbName(InternalFieldEnum.ID.getDbName()));

		Assert.assertNotNull(mc.getFields());
	}

	@Test
	public void testAncestors() {
		MongoMetadataServiceImpl metaService = (MongoMetadataServiceImpl) repo.getMetadataService();

		MetaClass appMeta = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);
		MetaClass newMeta = new MetaClass();
		newMeta.setName("newName");
		newMeta.setMetadataService(repo.getMetadataService());

		newMeta.setupAncestors(metaService, Collections.<String, MetaClass> emptyMap());

		try {
			newMeta.setParent("non-parent");
			newMeta.setupAncestors(metaService, Collections.<String, MetaClass> emptyMap());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			// expected
		}

		newMeta.setParent(appMeta.getName());
		newMeta.setupAncestors(metaService, Collections.<String, MetaClass> emptyMap());

		// circular case
		appMeta.setParent(newMeta.getName());
		try {
			newMeta.setupAncestors(metaService, Collections.<String, MetaClass> emptyMap());
			Assert.fail();
		} catch (IllegalMetaClassException e) {
			// expected
		}

		Assert.assertNotNull(appMeta.toString());
	}

	@Test
	public void testEquals() {
		MetaClass appMeta = repo.getMetadataService().getMetaClass(APPLICATION_SERVICE);
		MetaClass servMeta = repo.getMetadataService().getMetaClass("ServiceInstance");
		Assert.assertFalse(appMeta.equals(servMeta));
		Assert.assertFalse(servMeta.equals(appMeta));

		Assert.assertTrue(appMeta.equals(appMeta));
		Assert.assertTrue(servMeta.equals(servMeta));

		Assert.assertFalse(appMeta.equals(null));
		Assert.assertFalse(appMeta.equals(new MetaAttribute(false)));
		MetaClass mc = new MetaClass();
		Assert.assertFalse(appMeta.equals(mc));
		Assert.assertFalse(mc.equals(appMeta));

		mc.setName(APPLICATION_SERVICE);
		Assert.assertFalse(appMeta.equals(mc));
		Assert.assertFalse(mc.equals(appMeta));

		mc.setRepository(RAPTOR_PAAS);

		Assert.assertTrue(appMeta.equals(mc));
		Assert.assertTrue(mc.equals(appMeta));
	}

	@Test
	public void testPrefix() {
		Pattern p = Pattern.compile("^_[A-Za-z0-9].*");
		p.matcher("__1").matches();
		Assert.assertFalse("__1", p.matcher("__1").matches());
		Assert.assertFalse("a", p.matcher("a").matches());
		Assert.assertFalse("1", p.matcher("1").matches());
		Assert.assertFalse("_&", p.matcher("_&").matches());
		Assert.assertFalse("_*2", p.matcher("_*2").matches());
		Assert.assertFalse("__A", p.matcher("__A").matches());
		Assert.assertFalse("__f", p.matcher("__f").matches());
		Assert.assertFalse("K", p.matcher("K").matches());

		Assert.assertTrue("_7", p.matcher("_7").matches());
		Assert.assertTrue("_g", p.matcher("_g").matches());
		Assert.assertTrue("_K", p.matcher("_K").matches());
		Assert.assertTrue("_856", p.matcher("_856").matches());
		Assert.assertTrue("_g67", p.matcher("_g67").matches());
		Assert.assertTrue("_Kjd", p.matcher("_Kjd").matches());
		Assert.assertTrue("_8*", p.matcher("_8*").matches());
		Assert.assertTrue("_g67", p.matcher("_g67)").matches());
		Assert.assertTrue("_Kjd__", p.matcher("_Kjd__").matches());
	}

	@Test
	public void testExpressionField() {
		MetaClass depMeta = repo.getMetadataService().getMetaClass("Dep");
		Assert.assertTrue(depMeta.hasExpressionFields());
		Assert.assertTrue(depMeta.getEmbedWithExprs().size() == 1);
	}

	@Test
	public void testOverride() {
		MetaClass mClass = new MetaClass();
		mClass.setName("Manager");
		mClass.setRepository(RAPTOR_PAAS);
		mClass.setParent("Employee");
		MetaAttribute a1 = new MetaAttribute();
		a1.setName("title");
		a1.setDataType(DataTypeEnum.ENUM);
		List<String> enumValues = new ArrayList<String>(3);
		enumValues.add("junior");
		enumValues.add("staff");

		a1.setEnumValues(enumValues);
		mClass.addField(a1);

		MetadataContext context = new MetadataContext();
		context.setSourceIp("127.0.0.1");
		context.setSubject("tester");

		MongoMetadataServiceImpl metaService = (MongoMetadataServiceImpl) repo.getMetadataService();
		metaService.createMetaClass(mClass, new MetadataContext());

		MetaClass empMeta = metaService.getMetaClass("Employee");
		MetaAttribute titleAttr = (MetaAttribute) empMeta.getFieldByName("title");
		Assert.assertEquals(2, titleAttr.getEnumValues().size());

		MetaClass mgrMeta = metaService.getMetaClass("Manager");
		MetaAttribute mgrTitle = (MetaAttribute) mgrMeta.getFieldByName("title");
		Assert.assertEquals(2, mgrTitle.getEnumValues().size());
	}

	@Test(expected = IllegalMetaClassException.class)
	public void testValidateForUpdateField_metaClassName1() {
		MetaClass mClass = new MetaClass();
		String metaClassName = " ";
		mClass.setName(metaClassName);
		mClass.setRepository(RAPTOR_PAAS);
		mClass.setParent("Employee");
		MetaAttribute a1 = new MetaAttribute();
		String fieldName = "title";
		a1.setName(fieldName);
		a1.setDataType(DataTypeEnum.ENUM);
		List<String> enumValues = new ArrayList<String>(3);
		enumValues.add("junior");
		enumValues.add("staff");

		a1.setEnumValues(enumValues);
		mClass.addField(a1);

		Map<String, MetaClass> metas = new HashMap<String, MetaClass>();
		metas.put(metaClassName, mClass);
//		mClass.validateForUpdateField(metas, fieldName);
		validator.validateForUpdateField(mClass, metas, fieldName);
	}

	@Test(expected = IllegalMetaClassException.class)
	public void testValidateForUpdateField_metaClassName2() {
		MetaClass mClass = new MetaClass();
		String metaClassName = "system.MyClass";
		mClass.setName(metaClassName);
		mClass.setRepository(RAPTOR_PAAS);
		mClass.setParent("Employee");
		MetaAttribute a1 = new MetaAttribute();
		String fieldName = "title";
		a1.setName(fieldName);
		a1.setDataType(DataTypeEnum.ENUM);
		List<String> enumValues = new ArrayList<String>(3);
		enumValues.add("junior");
		enumValues.add("staff");

		a1.setEnumValues(enumValues);
		mClass.addField(a1);

		Map<String, MetaClass> metas = new HashMap<String, MetaClass>();
		metas.put(metaClassName, mClass);
//		mClass.validateForUpdateField(metas, fieldName);
		validator.validateForUpdateField(mClass, metas, fieldName);
	}

	@Test(expected = IllegalMetaClassException.class)
	public void testValidateForUpdateField_metaClassName3() {
		MetaClass mClass = new MetaClass();
		Random randomGenerator = new Random();
		char[] invalidChars = CMSConsts.INVALID_META_CLASS_NAME_CHARACTERS;
		String metaClassName = String.valueOf(invalidChars[randomGenerator.nextInt(invalidChars.length)]);
		mClass.setName(metaClassName);
		mClass.setRepository(RAPTOR_PAAS);
		mClass.setParent("Employee");
		MetaAttribute a1 = new MetaAttribute();
		String fieldName = "title";
		a1.setName(fieldName);
		a1.setDataType(DataTypeEnum.ENUM);
		List<String> enumValues = new ArrayList<String>(3);
		enumValues.add("junior");
		enumValues.add("staff");

		a1.setEnumValues(enumValues);
		mClass.addField(a1);

		Map<String, MetaClass> metas = new HashMap<String, MetaClass>();
		metas.put(metaClassName, mClass);
//		mClass.validateForUpdateField(metas, fieldName);
		validator.validateForUpdateField(mClass, metas, fieldName);
	}

	@Test(expected = IllegalIndexException.class)
	public void testValidateForUpdateField_indexSize1() {
		int maxIndexSize = CMSConsts.MAX_INDEXES_PER_META_CLASS;
		MetaClass mClass = new MetaClass();
		mClass.setName("TestIndexSize");
		mClass.setRepository(RAPTOR_PAAS);
		MetaOption metaOption = mClass.getOptions();

		for (int i = 0; i <= maxIndexSize; i++) {
			MetaAttribute attr = new MetaAttribute();
			attr.setName("field" + i);
			IndexInfo index = new IndexInfo("index" + i);
			metaOption.addIndex(index);
		}

		IMetadataService metaService = repo.getMetadataService();
		metaService.createMetaClass(mClass, new MetadataContext());
	}

	@Test(expected = IllegalIndexException.class)
	public void testValidateForUpdateField_indexSize2() {
		int defaultSystemMaxIndexSize = CMSConsts.MAX_INDEXES_PER_META_CLASS;
		MetaClass mClass = new MetaClass();
		mClass.setName("TestIndexSize");
		mClass.setRepository(RAPTOR_PAAS);
		MetaOption metaOption = mClass.getOptions();

		int maxIndexSizePerRepo = defaultSystemMaxIndexSize + 1;
		repo.getOptions().setMaxNumOfIndexes(maxIndexSizePerRepo);

		for (int i = 0; i <= maxIndexSizePerRepo; i++) {
			MetaAttribute attr = new MetaAttribute();
			attr.setName("field" + i);
			IndexInfo index = new IndexInfo("index" + i);
			metaOption.addIndex(index);
		}

		IMetadataService metaService = repo.getMetadataService();
		try {
			metaService.createMetaClass(mClass, new MetadataContext());
		} catch (IllegalIndexException e) {
			throw e;
		} finally {
			repo.getOptions().setMaxNumOfIndexes(null);
		}
	}
	
	@Test(expected = MetaFieldExistsException.class)
	public void testValidateForUpdate() {
		MetaClass self = new MetaClass();
		String selfName = "SelfClass";
		self.setName(selfName);
		self.setRepository(RAPTOR_PAAS);
		
		MetaField nameField = new MetaAttribute();
		nameField.setName("name");
		nameField.setDataType(DataTypeEnum.STRING);
		nameField.setCardinality(CardinalityEnum.One);
		self.addField(nameField);
		
		MetaField ageField = new MetaAttribute();
		ageField.setName("age");
		ageField.setDataType(DataTypeEnum.INTEGER);
		ageField.setCardinality(CardinalityEnum.One);
		self.addField(ageField);
		
		IMetadataService metaService = repo.getMetadataService();
		self = metaService.createMetaClass(self, new MetadataContext());
		
		MetaClass parent = new MetaClass();
		String parentName = "ParentClass";
		parent.setName(parentName);
		parent.setRepository(RAPTOR_PAAS);
		
		nameField = new MetaAttribute();
		nameField.setName("name");
		nameField.setDataType(DataTypeEnum.STRING);
		parent.addField(nameField);
		
		MetaField numberField = new MetaAttribute();
		numberField.setName("number");
		numberField.setDataType(DataTypeEnum.INTEGER);
		parent.addField(numberField);
		
		parent = metaService.createMetaClass(parent, new MetadataContext());
		
		MetaClass updateSelf = new MetaClass();
		updateSelf.setName(selfName);
		updateSelf.setParent(parentName);
		updateSelf.setMetadataService(metaService);
		
		Map<String, MetaClass> metas = new HashMap<String, MetaClass>();
		metas.put(updateSelf.getName(), updateSelf);
		
		List<MetaClass> metaClasses = metaService.getMetaClasses(new MetadataContext());
		MetaClassGraph tempGraph = new MetaClassGraph(metaClasses);
		tempGraph.updateMetaClass(updateSelf);
		
//		updateSelf.validateForUpdate(metas, tempGraph);
		validator.validateForUpdate(updateSelf, metas, tempGraph);
	}

}
