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

package com.ebay.cloud.cms.entmgr.entity;

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;

/**
 * 
 * @author jianxu1@ebay.com
 * @date   2012/5/8
 *
 */

public interface IEntityService 
{
	/**
	 * 
	 * @param entity 
	 * @return IEntity instance of the new created entity
	 * @throws 
	 * IllegalAugumentException: 
	 * 		repository id does not exist, 
	 * 		branch id does not exist in given repository
	 * 		meta data type is not defined in given repository
	 *  	entity is null or does not pass validation
	 * CmsDalException
	 * 		CmsPersistException is wrapper exception of mongo related exceptions
	 * 		TimeoutException if operation out of SLA, SLA is loaded from configuration of dal implementation	
	 */
	String create(IEntity entity, EntityContext context);
	
	/**
     * 
     * @param entities
     * @return entity ids
     * @throws 
     * IllegalAugumentException: 
     *      repository id does not exist, 
     *      branch id does not exist in given repository
     *      meta data type is not defined in given repository
     *      entity is null or does not pass validation
     * CmsDalException
     *      CmsPersistException is wrapper exception of mongo related exceptions
     *      TimeoutException if operation out of SLA, SLA is loaded from configuration of dal implementation    
     */
    List<String> batchCreate(List<IEntity> entities, EntityContext context, List<String> parseFails);
	
	/**
	 * 
	 * @param repositoryId
	 * @param branchId
	 * @param metaType
	 * @param entityId
	 * @return retrieved IEntity instance 
	 * @throws 
	 * IllegalAugumentException:
	 * 	 	repository id does not exist, 
	 * 		branch id does not exist in given repository
	 * 		meta data type is not defined in given repository
	 * 		entity id does not exist
	 * CmsDalException
	 * 	 	CmsPersistException is wrapper exception of mongo related exceptions
	 * 		TimeoutException if operation out of SLA, SLA is loaded from configuration of dal implementation	
	 */
	IEntity get(IEntity queryEntity, EntityContext context);
	
    /**
     * Replace the old existing entity with a new one. Use the query entity to find the existing entity.
     * 
     * @param queryEntity
     * @param entity
     * @param context
     */
	void replace(IEntity queryEntity, IEntity entity, EntityContext context);
	
	/**
	 * Modify the existing entity instance. Use the query entity to find the existing entity.
	 * 
	 * @param queryEntity
	 * @param entity
	 * @param context
	 */
	void modify(IEntity queryEntity, IEntity entity, EntityContext context);
	
	/**
     * Batch modify the existing entity instance, entity id remains same
     * 
     * @param entity
     * @return the modified entity instance
     * @throws 
     * IllegalAugumentException:
     *      repository id does not exist, 
     *      branch id does not exist in given repository
     *      meta data type is not defined in given repository
     *      entity id does not exist
     *      
     * CmsDalException:
     *      VersionException if newVersion!= oldVersion+1
     *      CmsPersistException is wrapper exception of mongo related exceptions
     *      TimeoutException if operation out of SLA, SLA is loaded from configuration of dal implementation    
     */
	List<String> batchModify(List<IEntity> entity, EntityContext context, List<String> parseFails);
	
	/**
	 * 
	 * @param repositoryId
	 * @param branchId
	 * @param metaType
	 * @param entityId
	 * @return
	 * @throws 
	 * IllegalAugumentException:
	 * 	 	repository id does not exist, 
	 * 		branch id does not exist in given repository
	 * 		meta data type is not defined in given repository
	 * 		entity id does not exist
	 * CmsDalException:
	 * 	    CmsPersistException is wrapper exception of mongo related exceptions
	 * 		TimeoutException if operation out of SLA, SLA is loaded from configuration of dal implementation
	 */	
	void delete(IEntity queryEntity, EntityContext context);
	
	/**
	 * Batch delete the existing entity instance
	 * 
	 * @param entity
	 * @param context
	 * @param parseFails
	 * @return
	 */
	List<String> batchDelete(List<IEntity> entity, EntityContext context, List<String> parseFails);
	
	/**
	 * 
	 * @param entity
	 * @param fieldName
	 * @param context
	 */    
    void modifyField(IEntity queryEntity, IEntity entity, String fieldName, EntityContext context);
    
    void pullField(IEntity queryEntity, IEntity entity, String fieldName, EntityContext context);
    
    void deleteField(IEntity queryEntity, String fieldName, EntityContext context);
    
    void setCallback(IEntityOperationCallback callback);

    void casModifyField(IEntity queryEntity, IEntity entity, String fieldName,
            Object oldValue, EntityContext context);

}
