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

package com.ebay.cloud.cms.metadata.mongo;

import java.util.Collection;

import com.ebay.cloud.cms.metadata.exception.IndexOptionOperationException;
import com.ebay.cloud.cms.metadata.exception.MongoOperationException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoOperand;
import com.ebay.cloud.cms.utils.MongoUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;

/**
 * @author liasu
 * 
 */
public class UpdateOptionCommand implements IMetadataCommand {

    private static ObjectConverter<MetaOption> converter = new ObjectConverter<MetaOption>();

    private final MetaClass                   metaClass;
    private final MetaOption                  targetOption;
    private final DBCollection                dbCollection;

    public UpdateOptionCommand(MetaClass metaClass, MetaOption option, DBCollection coll) {
        this.metaClass = metaClass;
        this.targetOption = option;
        this.dbCollection = coll;
    }

    @Override
    public void execute(MetadataContext context) {
        Collection<IndexInfo> updateIndex = targetOption.getIndexes();

        BasicDBObject query = new BasicDBObject();
        query.append(MetaClass.NAME, metaClass.getName());
        BasicDBObject update = new BasicDBObject();
        BasicDBObject ups = new BasicDBObject();

        BasicDBObject versionObject = new BasicDBObject();
        versionObject.put("version", 1);
        update.put(MongoOperand.inc, versionObject);
        
        BasicDBObject options = (BasicDBObject)converter.toBson(targetOption);
        BasicDBObject indexOptions = (BasicDBObject)options.get("indexes");
        if (!updateIndex.isEmpty()) {
            for (IndexInfo index : updateIndex) {
                if (index.isInternal()) {
                    continue;
                }
                appendCommand(query, ups, indexOptions, index, context);
            }
        }

        update.append(getOperand(context), ups);

        try {
            boolean updated = MongoUtils.wrapperUpdate(dbCollection, query, update);
            if (!updated) {
                StringBuilder sb = new StringBuilder();
                for (IndexInfo f : updateIndex) {
//                    sb.append(Objects.toStringHelper(f).toString());
                    sb.append(f.getClass().getName());
                }
                throw new IndexOptionOperationException(sb.toString());
            }
        } catch (MongoException e) {
            throw new MongoOperationException(e);
        }
    }

    private void appendCommand(BasicDBObject query, BasicDBObject ups, BasicDBObject indexOptions, IndexInfo index,
            MetadataContext context) {
        switch (context.getOptionChangeMode()) {
        case ADD:
            appendSetCommand(query, ups, indexOptions, index, false);
            break;
        case UPDATE:
            appendSetCommand(query, ups, indexOptions, index, true);
            break;
        case DELETE:
            appendUnsetCommand(query, ups, indexOptions, index);
            break;
        default:
            throw new IllegalStateException("Illegal update mode for metaclass option change");
        }
    }

    private String getOperand(MetadataContext context) {
        switch (context.getOptionChangeMode()) {
        case ADD:
            // pass through
        case UPDATE:
            return MongoOperand.set;
        case DELETE:
            return MongoOperand.unset;
        default:
            throw new IllegalStateException("Illegal update mode for metaclass option change");
        }
    }

    protected void appendSetCommand(BasicDBObject query, BasicDBObject ups, BasicDBObject indexOptions,
            IndexInfo index, boolean update) {
        String fieldKey = "options.indexes." + index.getIndexName();
        query.append(fieldKey, new BasicDBObject(MongoOperand.exists, update));
        ups.append(fieldKey, indexOptions.get(index.getIndexName()));
    }

    protected void appendUnsetCommand(BasicDBObject query, BasicDBObject ups, BasicDBObject indexOptions,
            IndexInfo index) {
        String fieldKey = "options.indexes." + index.getIndexName();
        query.append(fieldKey, new BasicDBObject(MongoOperand.exists, true));
        ups.append(fieldKey, 1);
    }

}
