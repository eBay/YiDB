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

import java.util.Collection;

import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;

/**
 * 
 * @author jianxu1
 * @date 2012/5/24
 * 
 * @history
 * 2012/5/25 
 * change IMapperVisitor.process from IMapperVisitor.process(List<?> fieldValues, MetaField) to
 * IMapperVisitor.process(MetaField)
 * 
 * Implementation of IMapperVisitor usually has IWalkNode<T> sourceNode, so MapperVisitor can 
 * call sourceNode.getFieldValues to get List<?> fieldValues itself
 *
 */
public interface IEntityVisitor {
	
	
	/**
	 * Create a new visitor of same type. Usually visitor have an entity pay load 
	 * @param metaClass metaClass of visitor's pay load entity.
	 * @param metaField for reference filed, metaField.getRefMetaClass == metaClass
	 * @return
	 */
	
	Collection<String>  getVisitFields(IEntity currentEntity);
	void processAttribute(IEntity currentEntity, MetaField metaField);
	void processReference(IEntity currentEntity, MetaRelationship metaRelationship);

}
