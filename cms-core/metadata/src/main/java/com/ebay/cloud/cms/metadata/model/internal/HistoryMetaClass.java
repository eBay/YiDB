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

package com.ebay.cloud.cms.metadata.model.internal;

import java.util.Date;

import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;

public class HistoryMetaClass {
    
    public static final String NAME           = "History";
    public static final String INDEX_NAME     = "__logId_Version_Index__";

    public static final String SourceIp            = "sourceIp";
    public static final String EntityId            = "entityId";
    public static final String EntityVersion       = "entityVersion";
    public static final String EntityParentVersion = "entityParentVersion";
    public static final String OperType            = "operType";
    public static final String LogBody             = "logBody";

    public static MetaClass getMetaClass(Repository repoInst) {
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass historyMetaClass = metaService.getMetaClass(NAME);
        if (historyMetaClass != null) {
            return historyMetaClass;
        }
        throw new RuntimeException("no history meta class found");
    }

    /**
     * Create hsitory metaclass for given repo.
     * 
     * Not thread safe
     * 
     * @param repo
     */
    public static void createHistoryMetaClass(Repository repoInst) {
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass historyMetaClass = metaService.getMetaClass(NAME);
        if (historyMetaClass != null) {
            return;
        }

        // let's create a new history meta class
        historyMetaClass = new MetaClass();
        historyMetaClass.setName(NAME);
        historyMetaClass.setLastModified(new Date());
        historyMetaClass.setpluralName(null);
        historyMetaClass.setRepository(repoInst.getRepositoryName());

        MetaAttribute ipAttr = new MetaAttribute();
        ipAttr.setCardinality(CardinalityEnum.One);
        ipAttr.setDataType(DataTypeEnum.STRING);
        ipAttr.setName(SourceIp);
        ipAttr.setMandatory(true);
        historyMetaClass.addField(ipAttr);

        MetaAttribute idAttr = new MetaAttribute();
        idAttr.setCardinality(CardinalityEnum.One);
        idAttr.setDataType(DataTypeEnum.STRING);
        idAttr.setName(EntityId);
        idAttr.setMandatory(true);
        historyMetaClass.addField(idAttr);
        
        MetaAttribute versionAttr = new MetaAttribute();
        versionAttr.setCardinality(CardinalityEnum.One);
        versionAttr.setDataType(DataTypeEnum.INTEGER);
        versionAttr.setName(EntityVersion);
        versionAttr.setMandatory(true);
        historyMetaClass.addField(versionAttr);
        
        MetaAttribute parentVersionAttr = new MetaAttribute();
        parentVersionAttr.setCardinality(CardinalityEnum.One);
        parentVersionAttr.setDataType(DataTypeEnum.INTEGER);
        parentVersionAttr.setName(EntityParentVersion);
        parentVersionAttr.setMandatory(true);
        historyMetaClass.addField(parentVersionAttr);

        MetaAttribute opAttr = new MetaAttribute();
        opAttr.setCardinality(CardinalityEnum.One);
        opAttr.setDataType(DataTypeEnum.STRING);
        opAttr.setName(OperType);
        opAttr.setMandatory(true);
        historyMetaClass.addField(opAttr);

        MetaAttribute logAttr = new MetaAttribute();
        logAttr.setCardinality(CardinalityEnum.One);
        logAttr.setDataType(DataTypeEnum.JSON);
        logAttr.setName(LogBody);
        logAttr.setMandatory(true);
        historyMetaClass.addField(logAttr);

        IndexInfo ii = new IndexInfo(INDEX_NAME);
        ii.addKeyField(EntityId);
        ii.addKeyField(EntityVersion);
        historyMetaClass.getOptions().addIndex(ii);

        metaService.createMetaClass(historyMetaClass, new MetadataContext());
    }
}
