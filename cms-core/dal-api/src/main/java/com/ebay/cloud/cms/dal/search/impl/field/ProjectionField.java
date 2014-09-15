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

import java.util.List;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.FieldProperty;

/**
 * @author liasu
 *
 */
public class ProjectionField extends AbstractSearchField {

    /**
     * A predefined field that presents the *
     */
    public static class StarField implements ISearchField {
        private static final String STAR = "*";
    
        StarField() {
        }
    
        @Override
        public String getFullDbName() {
            return STAR;
        }
    
        @Override
        public String getFieldName() {
            return STAR;
        }
    
        @Override
        public List<?> getSearchValue(IEntity entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setEmbedPath(String embedPath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isProjected() {
            return true;
        }

        @Override
        public String getInnerField() {
            return null;
        }

        @Override
        public FieldProperty getInnerProperty() {
            return null;
        }
        
    }

    public static final StarField STAR = new StarField();

    private boolean isProjected;
    
    public ProjectionField(MetaField rootField, boolean isProjected, ISearchStrategy strategy) {
        super(rootField, strategy);
        this.isProjected = isProjected;
    }
    
    public ProjectionField(MetaField rootField, String innerField, boolean isProjected, ISearchStrategy strategy) {
        super(rootField, innerField, strategy);
        this.isProjected = isProjected;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (isProjected ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ProjectionField))
            return false;
        if (isProjected != ((ProjectionField)obj).isProjected())
            return false;
        return super.equals(obj);
    }
    
    @Override
    public boolean isProjected() {
        return isProjected;
    }

}
