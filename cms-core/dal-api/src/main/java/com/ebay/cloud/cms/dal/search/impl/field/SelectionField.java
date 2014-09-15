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


/**
 * 
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

package com.ebay.cloud.cms.dal.search.impl.field;

import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

/**
 * 
 * @author liasu
 * 
 */
public class SelectionField extends AbstractSearchField {

    public SelectionField(MetaField metaField, String innerField, ISearchStrategy strategy) {
        super(metaField, innerField, strategy);
        if (innerField == null && metaField.getDataType() == DataTypeEnum.RELATIONSHIP) {
            setInnerField(InternalFieldEnum.ID.getDbName());
        }
    }

    public SelectionField(MetaField rootField, ISearchStrategy strategy) {
        this(rootField, null, strategy);
    }

    @Override
    public boolean isProjected() {
        return false;
    }

}
