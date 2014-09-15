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

package com.ebay.cloud.cms.query.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.service.IMetadataHistoryService;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;

/**
 * A decorated metadata service that take its own metaclass prior to its delegate.
 * 
 * @author liasu
 *
 */
public class QueryMetadataService implements IMetadataService {

    private final IMetadataService       delegate;

    /**
     * MetaClasse that takes higher priority than its delegation
     */
    private final Map<String, MetaClass> nameCache;
    private final Map<String, MetaClass> pluralNameCache;

    public QueryMetadataService(IMetadataService delegate) {
        this.delegate = delegate;
        this.nameCache = new HashMap<String, MetaClass>(8);
        this.pluralNameCache = new HashMap<String, MetaClass>(8);
    }

    @Override
    public Repository getRepository() {
        return delegate.getRepository();
    }

    @Override
    public MetaClass getMetaClass(String className) {
        MetaClass meta = nameCache.get(className);
        if (meta == null) {
            meta = pluralNameCache.get(className);
        }
        if (meta == null) {
            meta = delegate.getMetaClass(className);
        }
        return meta;
    }

    @Override
    public MetaClass getMetaClass(String className, int version, MetadataContext context) {
        MetaClass meta = getMetaClass(className);
        if (meta.getVersion() == version) {
            return meta;
        }
        return delegate.getMetaClass(className, version, context);
    }

    @Override
    public List<MetaClass> getMetaClasses(MetadataContext context) {
        List<MetaClass> allMetas = delegate.getMetaClasses(context);
        Map<String, MetaClass> metaMap = new HashMap<String, MetaClass>();
        for (MetaClass mc : allMetas) {
            metaMap.put(mc.getName(), mc);
        }
        metaMap.putAll(nameCache);
        return new ArrayList<MetaClass>(metaMap.values());
    }

    public void addMetaClass(QueryMetaClass metaClass) {
        this.nameCache.put(metaClass.getName(), metaClass);
        if (metaClass.getpluralName() != null) {
            this.pluralNameCache.put(metaClass.getpluralName(), metaClass);
        }
    }

    @Override
    public int getCollectionCount(String dbCollectionName) {
        return delegate.getCollectionCount(dbCollectionName);
    }

    @Override
    public MetaClass createMetaClass(MetaClass metaClass, MetadataContext context) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public void deleteMetaClass(String className, MetadataContext metaContext) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public MetaClass deleteMetaField(String className, String fieldName, MetadataContext metaContext) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public MetaClass updateMetaClass(MetaClass metaClass, MetadataContext context) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public List<MetaClass> batchUpsert(List<MetaClass> metas, MetadataContext context) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public void updateMetaOption(String className, MetaOption options, MetadataContext context) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public MetaClass updateMetaField(MetaClass metaClass, String fieldName, MetadataContext context) {
        throw new UnsupportedOperationException("Query metaclass can not change metaclass!");
    }

    @Override
    public void validateMetaClass(String className) {
        throw new UnsupportedOperationException("Query metaclass can not validate metaclass!");
    }

    @Override
    public void setMetaHistoryService(IMetadataHistoryService historyService) {
        throw new UnsupportedOperationException("Query metaclass can not set meta history service!");
    }

}
