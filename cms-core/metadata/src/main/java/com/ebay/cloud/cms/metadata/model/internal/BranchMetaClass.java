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

import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;

/**
 * 
 * @author jianxu1
 */
public class BranchMetaClass {
    
    public static final String TYPE_NAME    = "Branch";

    // all the branch entity share same branchId
    public static final String BRANCH_ID    = "metabranch";

    // list all the field names
    public static final String IsMain       = "isMain";
    public static final String BranchStatus = "branchStatus";

    public static MetaClass getMetaClass(Repository repoInst) {
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass branchMetaCls = metaService.getMetaClass(TYPE_NAME);
        if (branchMetaCls != null) {
            return branchMetaCls;
        }
        throw new RuntimeException("no branch meta class found");
    }

    /**
     * Create meta data class on the repository
     * 
     * Not Thread-safe
     * 
     * @param repoInst
     */
    public static void createBranchMetaClass(Repository repoInst) {
        IMetadataService metaService = repoInst.getMetadataService();
        MetaClass branchMetaCls = metaService.getMetaClass(TYPE_NAME);
        if (branchMetaCls != null) {
            return;
        }

        branchMetaCls = new MetaClass();
        branchMetaCls.setName(TYPE_NAME);
        branchMetaCls.setLastModified(new Date());
        branchMetaCls.setpluralName(null);
        branchMetaCls.setRepository(repoInst.getRepositoryName());

        MetaAttribute isMainAttr = new MetaAttribute();
        isMainAttr.setCardinality(CardinalityEnum.One);
        isMainAttr.setDataType(DataTypeEnum.BOOLEAN);
        isMainAttr.setName(IsMain);
        isMainAttr.setMandatory(true);
        branchMetaCls.addField(isMainAttr);

        MetaAttribute nameAttr = new MetaAttribute();
        nameAttr.setCardinality(CardinalityEnum.One);
        nameAttr.setDataType(DataTypeEnum.STRING);
        nameAttr.setName("name");
        nameAttr.setMandatory(false);
        branchMetaCls.addField(nameAttr);

        metaService.createMetaClass(branchMetaCls, new MetadataContext());
    }
}
