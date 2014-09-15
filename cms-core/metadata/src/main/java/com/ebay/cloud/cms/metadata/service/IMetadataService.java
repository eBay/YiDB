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

package com.ebay.cloud.cms.metadata.service;

import java.util.List;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.Repository;

/**
 * Metadata service for a specific repository
 * 
 * @author xjiang
 * 
 */
public interface IMetadataService {
    
    /**
     * Get Repository of this metadata service
     * 
     * @return
     */
    public Repository getRepository();

    /**
     * Get MetaClass by repository and clazz name
     * 
     * @param repositoryName
     * @param className is either pluralName or class name
     * @return MetaClass or null if not found
     */
    public MetaClass getMetaClass(String className);

    /**
     * Get MetaClass by repository and clazz name and version
     * 
     * @param repositoryName
     * @param className is either pluralName or class name
     * @return MetaClass or null if not found
     */
    public MetaClass getMetaClass(String className, int version, MetadataContext context);

	/**
	 *  Create a new meta class. 
	 *  
	 *  It will throw MetaClassExist exception if a meta class 
	 *  with the same name already exist. 
	 * 
	 * @param metaClass
	 * @return created meta class
	 */
	public MetaClass createMetaClass(MetaClass metaClass, MetadataContext context);
	
	/**
	 * 
	 * delete a meta class by repository and clazz name
	 * Unsupport now.
	 * 
	 * @param repositoryName
	 * @param className is either pluralName or class name
	 */
	public void deleteMetaClass(String className, MetadataContext metaContext);
	
	/**
	 * Delete an existing field from a metaclass.
	 * 
	 * @param className - either pluralName or class name
	 * @param fieldName - the name of the filed that to be dropped
	 */
	public MetaClass deleteMetaField(String className, String fieldName, MetadataContext metaContext);
	
	/**
	 * Update a meta class. 
	 * Currently it only support appending new fields to a meta class. 
	 * 
	 * Fields in this metaclass is either a new field (field name can not found in the existing metaclass), 
	 *   or a exist field (in this case the field name and other properties should be exact same with the on in existing metaclass)  
	 * 
	 * It will throw MetaFieldExist exception if appending field is conflict with the one in existing fields, 
	 * or exist in it's ancestor or it's decendent 
	 * 
	 * For meta options, the service also only support appending.
	 * 
	 * @param metaClass
	 */
	public MetaClass updateMetaClass(MetaClass metaClass, MetadataContext context);
	
	/**
	 * 
	 * batch upsert metaclasses
	 * 
	 * @param metas
	 * @return
	 */
	public List<MetaClass> batchUpsert(List<MetaClass> metas, MetadataContext context);
	
	/**
	 * Returns all meta class on given repository
	 * 
	 * @return
	 */
	public List<MetaClass> getMetaClasses(MetadataContext context);
	
	/**
	 * Updates the meta option of the given meta class.
	 * The update operation may have addNew/Modify/deleteExisting, the behavior depends on the context
	 * 
	 * @param options
	 * @param context
	 */
	public void updateMetaOption(String className, MetaOption options, MetadataContext context);
	
	   /**
     * 
     * get collection count 
     * 
     * @param dbCollectionName is collection name in database
     */
    public int getCollectionCount(String dbCollectionName);
    
    /**
     * Update a meta class. 
     * Currently it only support update one existing field
     * 
     * @param metaClass
     * @param fieldName
     */
    public MetaClass updateMetaField(MetaClass metaClass, String fieldName, MetadataContext context);
	
    /**
     * Validate a meta class. 
     * 
     * @param metaClass
     */
    public void validateMetaClass(String className);

    public void setMetaHistoryService(IMetadataHistoryService historyService);
}
