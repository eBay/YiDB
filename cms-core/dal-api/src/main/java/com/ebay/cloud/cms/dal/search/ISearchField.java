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

package com.ebay.cloud.cms.dal.search;

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;



/**
 * search field that will be used in projection & filter & group
 * 
 * @author xjiang
 * 
 */
public interface ISearchField {

    /**
     * The field name is the meta field name that this search/project based on.
     * 
     * @return
     */
    String getFieldName();

    /**
     * A full db name is the real name in database of this search field.
     * <p>
     * For example, a simple meta field "name", it's db name is defined by
     * metaclass, possibly a "ABC.v" format.
     * <p>
     * For nested search, the inner field is appended here.Like when search on a
     * json field, for search field "property.host"(property is the field name,
     * while the host is its inner json field), the db name is "ABC.v.host"
     * (says ABC is the db name for field "property" define in metaclass)
     * 
     * @return
     */
    String getFullDbName();


    /**
     * Extract the search field value from the outing entity. 
     * A search value could be the field value on the given entity or inner field properties, or even inner fields in side 
     * 
     * 
     * @param entity
     * @return
     */
    List<?> getSearchValue(IEntity entity);
    
    
    /**
     * embed path with dot
     * 
     * @param embedPath
     */
    void setEmbedPath(String embedPath);
    
    boolean isProjected();

    String getInnerField();

    FieldProperty getInnerProperty();
}
