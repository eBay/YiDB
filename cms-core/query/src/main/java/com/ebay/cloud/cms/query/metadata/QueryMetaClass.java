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

package com.ebay.cloud.cms.query.metadata;

import java.util.Collection;

import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchGroup;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.utils.CheckConditions;

/**
 * An query metaclass is a transformed metaclass that to represent the search metaclass.
 * 
 * The reason to use query metaclass is that during search, there will be requirement to add some query-specific change
 * to the given metaclass that defined in the repository. Those change including dynamic attributes like aggregate function
 * attribute, group attribute, reverse relationship.
 * 
 * @author liasu
 * 
 */
public class QueryMetaClass extends MetaClass {

    public static QueryMetaClass newInstance(QueryContext context, MetaClass metaClass) {
        if (metaClass instanceof QueryMetaClass) {
            return (QueryMetaClass)metaClass;
        } else {
            // find in the given query meta service first
            QueryMetadataService queryMetaService = context.getMetadataService(metaClass.getRepository());
            MetaClass meta = queryMetaService.getMetaClass(metaClass.getName());
            if (meta instanceof QueryMetaClass) {
                return (QueryMetaClass) meta;
            }
            // create if not found
            QueryMetaClass qmc = new QueryMetaClass(metaClass);
            qmc.setMetadataService(queryMetaService);
            qmc.initialize(context);
            queryMetaService.addMetaClass(qmc);
            return qmc;
        }
    }

    private QueryMetaClass(MetaClass metaClass) {
        super(metaClass);
    }

    //
    // copy and update all relationships's meta service
    // FIXME : MetaGraph could not be updated. But it's ok.
    //
    private void initialize(QueryContext context) {
        Collection<MetaField> fields = getFields();
        this.fieldNameIndex.clear();
        this.dbNameIndex.clear();
        this.flattenValueDbNameIndex.clear();
        // only copy relationship fields: ADD ALL into current meta classes
        for (MetaField field : fields) {
            if (field.getDataType() != DataTypeEnum.RELATIONSHIP) {
                addField(field);
            } else {
                // copy and init
                QueryMetaRelationship queryRelationship = new QueryMetaRelationship((MetaRelationship) field, context);
                queryRelationship.setMetadataService(getMetadataService());
                addField(queryRelationship);
            }
        }
    }

    public void addAggregationFields(SearchGroup searchGroup) {
        CheckConditions.checkNotNull(searchGroup, "Search group can not be null");
        Collection<ISearchField> fields = searchGroup.getProjectFields();

        // add aggregation fields
        for (ISearchField searchField : fields) {
            AggregateMetaAttribute amf = new AggregateMetaAttribute(searchField);
            this.addField(amf);
        }
    }

    public MetaRelationship addReverseField(MetaClass nextMetaClass, MetaRelationship nextMetaReference) {
        String className = nextMetaClass.getName();
        String fieldName = nextMetaReference.getName();
        String reverseAttrName = new StringBuilder(fieldName).append("!").append(className).toString();                
        ReverseMetaRelationship metaRef = new ReverseMetaRelationship();
        metaRef.setName(reverseAttrName);
        metaRef.setDbName(reverseAttrName);
        metaRef.setFlattenValueDbName(reverseAttrName);
        metaRef.setRefDataType(className);
        metaRef.setCardinality(CardinalityEnum.Many);
        metaRef.setDataType(DataTypeEnum.RELATIONSHIP);
        metaRef.setMetadataService(getMetadataService());
        metaRef.setVirtual(true);
        metaRef.setReversedReference(nextMetaReference);
        if (this.getFieldByName(reverseAttrName) == null) {
            this.addField(metaRef);
        }

        return metaRef;
    }
    
    // override add Field to avoid validation on meta field name in group internal field case
    @Override
    public final void addField(MetaField metaField) {
        getFieldNameIndex().put(metaField.getName(), metaField);
        
        String dbName = metaField.getDbName();
        if (dbName != null) {
            getDbNameIndex().put(dbName, metaField);
        }
        String flattenValueDbName = metaField.getFlattenValueDbName();
        if (flattenValueDbName != null) {
            getFlattenValueDbNameIndex().put(flattenValueDbName, metaField);
        }
    }

}
