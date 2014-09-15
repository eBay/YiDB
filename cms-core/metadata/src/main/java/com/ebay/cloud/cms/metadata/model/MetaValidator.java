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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.IndexExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaClassExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.exception.MetaFieldExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaFieldNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.mongo.MetadataOptionValidator;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.StringUtils;

public class MetaValidator {

  public void validateForCreation(MetaClass meta, Map<String, MetaClass> metas, MetaClassGraph tempGraph) {
      String name = meta.getName();
      String pluralName = meta.getpluralName();
      String parent = meta.getParent();
      IMetadataService metadataService = meta.getMetadataService();
      MetaOption options = meta.getOptions();
      Map<String, MetaField> fieldNameIndex = meta.getFieldNameIndex();
      if (name == null || MetaClass.getReservedMetaName().contains(name)) {
          throw new IllegalMetaClassException("Can not use reserved name :" + name + " as metaclass name");
      }
      validate(meta, metas, tempGraph);
      
      //a set of field name of this class, including ancestor fields that inheritted
      Set<String> fieldNames = new HashSet<String>(fieldNameIndex.keySet());
      MetaClass m1 = metadataService.getMetaClass(name);
      if (m1 != null) {
          throw new MetaClassExistsException("metaclass with name/plural name " + name + " already exist");
      }
      
      if (pluralName != null && metadataService.getMetaClass(pluralName) != null) {
          throw new MetaClassExistsException(MessageFormat.format(
                  "plural name ''{0}'' conflict with existing metadata", pluralName));
      }
      
      if (!StringUtils.isNullOrEmpty(parent)) {
          if (parent.equals(name)) {
              throw new IllegalMetaClassException("parent can not be the class itself");
          }
          meta.setupAncestors(metadataService, metas);

          //pm must be exist in either db or metas
          MetaClass pm = metadataService.getMetaClass(parent);
          if(pm == null) {
              pm = metas.get(parent);
          }
          
          validateParentConsistency(meta, metas, fieldNames, pm);
      }
      
      //index fields must existing
      for (IndexInfo idx : options.getIndexes()) {
          for (String idxField : idx.getKeyList()) {
              CheckConditions.checkCondition(fieldNames.contains(idxField), new MetaDataException(MetaErrCodeEnum.META_FIELD_NOT_EXISTS, MessageFormat.format("index {0} has field {1} that doesn''t exsiting", idx.getIndexName(), idxField)));
          }
      }
  }
  
  private void validateParentConsistency(MetaClass meta, Map<String, MetaClass> metas,
          Set<String> fieldNames, MetaClass pm) {
      IMetadataService metadataService = meta.getMetadataService();
      //get field names in this metaclass and it's ancestors's
      Map<String, MetaField> ancestorFields = new HashMap<String, MetaField>();
      Map<String, IndexInfo> ancestoreIndexes = new HashMap<String, IndexInfo>();
      
      String pName = pm.getName();
      while(pName != null) {
          MetaClass ancestor = metadataService.getMetaClass(pName);
          if (ancestor == null) {
              ancestor = metas.get(pName);
          }
          CheckConditions.checkNotNull(ancestor, new IllegalMetaClassException("can not find ancestor metaclass " + pName));
          
          for (String fieldName : ancestor.getFieldNameIndex().keySet()) {
              if (!ancestorFields.containsKey(fieldName)) {
                  ancestorFields.put(fieldName, ancestor.getFieldNameIndex().get(fieldName));
              }
          }
          
          for (String idxName : ancestor.getOptions().getIndexNames()) {
              if (!ancestoreIndexes.containsKey(idxName)) {
                  ancestoreIndexes.put(idxName, ancestor.getOptions().getIndexByName(idxName));
              }
          }
          pName = ancestor.getParent();
      }
      fieldNames.addAll(ancestorFields.keySet());
      
      for (MetaField f : meta.getClassFields()) {
          if (f.isInternal()) {
              continue;
          }
          
          MetaField ancestorField = ancestorFields.get(f.getName());
          if (ancestorField != null 
                  && !f.isOverridable(ancestorField)) {
              throw new MetaFieldExistsException(MessageFormat.format("field {0} already exist in ancestor's class. " +
                      "Override it by re-define Mandatory, DefaultValue or EnumValue.", f.getName()));
          }
      }
      
      //index name should not exist in the ancestor's indexes
      for (IndexInfo idx : meta.getOptions().getIndexes()) {
          if (idx.isInternal()) {
              continue;
          }
          IndexInfo info = ancestoreIndexes.get(idx.getIndexName());
          if (info != null && !idx.isOverridable(info)) {
              throw new IndexExistsException(MessageFormat.format("index {0} already exist in ancestor's class. " +                
                  "Override it by re-define the keyList or indexOptions",
                  idx.getIndexName()));
          }
      }
  }
  
  public void validateForUpdate(MetaClass meta, Map<String, MetaClass> metas, MetaClassGraph tempGraph) {
      validate(meta, metas, tempGraph);
      
      String name = meta.getName();
      String parent = meta.getParent();
      IMetadataService metadataService = meta.getMetadataService();
      MetaOption options = meta.getOptions();
      Map<String, MetaField> fieldNameIndex = meta.getFieldNameIndex();
      String repository = meta.getRepository();
      
      Collection<String> fieldNames = new HashSet<String>(fieldNameIndex.keySet());
      
      MetaClass existingMetadata = metadataService.getMetaClass(name);
      if (existingMetadata == null || !existingMetadata.getName().equals(name)) {
          throw new MetaClassNotExistsException(repository, name);
      }

      Boolean isEmbed = meta.getEmbed();
      if (isEmbed == null) {
          isEmbed = existingMetadata.getEmbed();
      }
      Boolean isInner = meta.getInner();
      if (isInner == null) {
          isInner = existingMetadata.getInner();
      }
      if (isEmbed != null && isInner != null) {
          CheckConditions.checkCondition(!(isEmbed && isInner), new IllegalMetaClassException("meta class " + name + " cannot be embed and inner"));
      }

      fieldNames.addAll(existingMetadata.getFieldNames());
      meta.setupAncestors(metadataService, metas);
      // the parent for update metaclass must be either "null" or the same with it's original metaclass's parent
//      CheckConditions.checkCondition(parent == null || !parent.equals(m1.getName()), new IllegalMetaClassException("parent could be the metaclass: " + name));
      
      // appending field should not exist in it's ancestors metaclasses
      // if origin metaclass has this field, the fields should be exact same (not only names)
      Iterator<Entry<String, MetaField>> iter = fieldNameIndex.entrySet().iterator();
      StringBuilder reDefineFields = new StringBuilder();
      Collection<String> newFieldNames = new HashSet<String>();
      while (iter.hasNext()) {
          MetaField newField = iter.next().getValue();
          String fieldName = newField.getName();
          newFieldNames.add(fieldName);
          MetaField existingField = existingMetadata.getFieldByName(fieldName);
          
          // non-internal field with same name exist in original metaclass or it's ancestor's metaclass
          if (existingField != null && !existingField.isInternal()) {
              MetaField parentField = null;
              if (!StringUtils.isNullOrEmpty(parent)) {
                  parentField = metadataService.getMetaClass(parent).getFieldByName(fieldName);
              }
              if (parentField != null) {
                  if (!newField.isOverridable(parentField)) {
                      throw new MetaFieldExistsException(MessageFormat.format("field {0} already exist in ancestor's class. " +
                              "Override it by re-define Mandatory, DefaultValue or EnumValue.", fieldName));
                  }
              } else if (newField.equals(existingField)) {
                  // exist in original metaclass
                  // then remove it for later update operation
                  iter.remove();
              } else {
                  reDefineFields.append(fieldName).append(",");
              }
          }
      }
      if (reDefineFields.length() > 0) {
          reDefineFields.setLength(reDefineFields.length() - 1);
          throw new MetaFieldExistsException("fields " + reDefineFields.toString() + " with different definition already exists in origin class");
      }
      
      //Fix CMS-4021
      if (org.apache.commons.lang.StringUtils.isEmpty(existingMetadata.getParent()) && !StringUtils.isNullOrEmpty(parent)) {
          Collection<String> difference = new HashSet<String>();
          difference.addAll(fieldNames);
          difference.removeAll(newFieldNames);
          for(String fieldName : difference) {
              MetaField existingField = existingMetadata.getFieldByName(fieldName);
              
              // non-internal field with same name exist in original metaclass or it's ancestor's metaclass
              if (existingField != null && !existingField.isInternal()) {
                  MetaField parentField = metadataService.getMetaClass(parent).getFieldByName(fieldName);
                  if (parentField != null) {
                      //exist in ancestor's metaclass
                      throw new MetaFieldExistsException("field " + fieldName + " already exist in ancestor's class");
                  }
              }
          }
      }
      
      //appending field should not exist in descendants fields
      List<MetaClass> descendants = existingMetadata.getDescendants(); 
      for (MetaClass d : descendants) {
          for (MetaField f : meta.getClassFields()) {
              if (!f.isInternal() && d.getFieldByName(f.getName()) != null) {
                  throw new MetaFieldExistsException("field " + f.getName() + " already exist in descandant's class " + d.getName());
              }
          }
      }
      
      // index should not be existing
      Iterator<IndexInfo> indexIter = options.getIndexes().iterator();
      while (indexIter.hasNext()) {
          IndexInfo indexInfo  = indexIter.next();
          if (indexInfo.isInternal()) {
              continue;
          }

          if (existingMetadata.getIndexByName(indexInfo.getIndexName()) != null) {
              indexIter.remove();
              continue;
          }

          for (String idxField : indexInfo.getKeyList()) {
              if (!fieldNames.contains(idxField)) {
                  throw new MetaDataException(MetaErrCodeEnum.META_FIELD_NOT_EXISTS, MessageFormat.format("index {0} has field {1} that doesn''t exsiting",
                          indexInfo.getIndexName(), idxField));
              }
          }

          if (existingMetadata.getIndexByName(indexInfo.getIndexName()) != null) {
              throw new IndexExistsException(MessageFormat.format("index name {0} already exsits. Can not update index options with metaclass update", indexInfo.getIndexName()));
          }
      }
  }
  
  public void validateForUpdateField(MetaClass meta, Map<String, MetaClass> metas, String fieldName) {
      IMetadataService metadataService = meta.getMetadataService();
      String name = meta.getName();
      String repository = meta.getRepository();
      MetaClassGraph graph = meta.getGraph();
      if (graph == null) {
          if (metadataService != null) {
              Collection<MetaClass> metaClasses = metadataService.getMetaClasses(new MetadataContext());
              graph = new MetaClassGraph(metaClasses);
              graph.updateMetaClass(meta);
          } else {
              graph = new MetaClassGraph(Collections.<MetaClass> emptyList());
          }
      }
      validate(meta, metas, graph);
      
      MetaClass existingMetadata = null;
      if (metadataService != null) {
            existingMetadata = metadataService.getMetaClass(name);
      }
      if (existingMetadata == null || !existingMetadata.getName().equals(name)) {
          throw new MetaClassNotExistsException(repository, name);
      }
      
      MetaField newField = meta.getFieldByName(fieldName);
      MetaField existingField = existingMetadata.getFieldByName(fieldName);
      if(existingField == null){
          throw new MetaFieldNotExistsException("field " + newField.getName() + " doesn't exist in metaClass");
      }
      
      newField.setDbName(existingField.getDbName());
      
      if ((existingField.getDataType() == DataTypeEnum.RELATIONSHIP && newField.getDataType() != DataTypeEnum.RELATIONSHIP)
              || (existingField.getDataType() != DataTypeEnum.RELATIONSHIP && newField.getDataType() == DataTypeEnum.RELATIONSHIP)) {
          throw new IllegalMetaClassException("Can't change field type (Attribute <=> Reference)!");
      }
  }
  
  public void validate(MetaClass meta) {
      checkMetaClassName(meta.getName());
      
      checkNotInheritedFromEmbededClass(meta, Collections.<String, MetaClass> emptyMap());
      
      validateFields(meta, Collections.<String, MetaClass> emptyMap(), true);
      
      validateIndex(meta, Collections.<String, MetaClass> emptyMap(), meta.getGraph());
      
      for (MetaRelationship rel : meta.getFromReference()) {
          rel.validate(true, rel.getSourceMetaClass(), Collections.<String, MetaClass> emptyMap());
      }
  }
  
  private void checkNotInheritedFromEmbededClass(MetaClass meta, Map<String, MetaClass> metas) {
      String parentClassName = meta.getParent();
      if (!org.apache.commons.lang.StringUtils.isEmpty(parentClassName)) {
    	  MetaClass parentClass = metas.get(parentClassName);
    	  if (parentClass == null) {
              parentClass = meta.getMetadataService().getMetaClass(parentClassName);
    	  }
          if (parentClass != null && parentClass.isEmbed()) {
              throw new IllegalMetaClassException(String.format("meta class %s should not be inherited from an embeded class %s", meta.getName(), parentClass.getName()));
          }
      }
  }
  
  private void checkMetaClassName(String name) {
      CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(name), new IllegalMetaClassException("meta class name can not be empty"));
      CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(name.trim()), new IllegalMetaClassException("meta class name can not be empty"));
      CheckConditions.checkCondition(!(isSystem(name)), new IllegalMetaClassException("meta class name cannot begin with 'system.' prefix"));
      CheckConditions.checkCondition(containsValidCharacters(name,CMSConsts.INVALID_META_CLASS_NAME_CHARACTERS), new IllegalMetaClassException("meta class name cannot contains invalid characters: " + Arrays.toString(CMSConsts.INVALID_META_CLASS_NAME_CHARACTERS)));
  }
      
  private void validate(MetaClass meta, Map<String, MetaClass> metas, MetaClassGraph tempGraph) {
      String name = meta.getName();
      checkMetaClassName(name);
      checkNotInheritedFromEmbededClass(meta, metas);
      
      CheckConditions.checkCondition(!(meta.isEmbed() && meta.isInner()), new IllegalMetaClassException("meta class " + name + " cannot be embed and inner"));
      CheckConditions.checkCondition(meta.getId() == null, new IllegalMetaClassException("id should not be set for creation and update"));
      CheckConditions.checkCondition(meta.getAncestors() == null, new IllegalMetaClassException("ancestors should not be set for creation and update"));
      
      validateFields(meta, metas, false);
      
      validateIndex(meta, metas, tempGraph);
  }
  
  /**
   * @return
   */
  private boolean containsValidCharacters(String name, char[] invalidCharacters) {
      return org.apache.commons.lang.StringUtils.containsNone(name, invalidCharacters);
  }

  /**
   * @return
   */
  private boolean isSystem(String name) {
      return name.startsWith("system.");
  }

  private void validateFields(MetaClass meta, Map<String, MetaClass> metas, boolean readOnlyCheck) {
      for (MetaField f : meta.getClassFields()) {
          if (f.isInternal()) {
              continue;
          }
          // generic field name check
          f.validate();
          
          if (f instanceof MetaRelationship) {
              ((MetaRelationship) f).setMetadataService(meta.getMetadataService());
              ((MetaRelationship) f).validate(readOnlyCheck, meta, metas);
          }
          else {
              ((MetaAttribute)f).validate(readOnlyCheck);
          }
      }
  }
  
//  private void checkIndexSize() {
//      Repository repo = metadataService.getRepository();
//      RepositoryOption repositoryOption = repo.getOptions();
//      Integer maxNumOfIndexes = CMSConsts.MAX_INDEXES_PER_META_CLASS;
//      if (repositoryOption != null && repositoryOption.getMaxNumOfIndexes() != null) {
//          maxNumOfIndexes = repositoryOption.getMaxNumOfIndexes();
//      }
//      if(getIndexes().size() > maxNumOfIndexes) {
//          String errorMessage = String.format("The number of index on a metaclass should NOT be more than %d!",
//                  maxNumOfIndexes);
//          throw new IllegalIndexException(errorMessage);
//      }
//  }
  
  private void validateIndex(MetaClass meta, Map<String, MetaClass> metas, MetaClassGraph tempGraph) {
//      checkIndexSize();
      MetadataContext metaContext = new MetadataContext();
      metaContext.setOptionChangeMode(UpdateOptionMode.VALIDATE);
      
      if(metas.isEmpty()) {
          // existed
          MetadataOptionValidator validator = new MetadataOptionValidator(new MetaOption(), metaContext);
          validator.processOption(meta, meta.getOptions(), tempGraph);
      } else {
          // newOptions
          MetadataOptionValidator validator = new MetadataOptionValidator(metas.get(meta.getName()).getOptions(), metaContext);
          MetaClass existedMetaClass = meta.getMetadataService().getMetaClass(meta.getName());
          validator.processOption(existedMetaClass == null ? meta : existedMetaClass, meta.getOptions(), tempGraph);
      }
      
//      for (IndexInfo index : getClassIndexes()) {
//          index.validate();
//      }
  }
}
