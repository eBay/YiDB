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

package com.ebay.cloud.cms.dal.persistence.flatten.impl.embed;

import java.util.Map.Entry;

import com.ebay.cloud.cms.dal.entity.flatten.impl.FlattenEntityIDHelper;
import com.ebay.cloud.cms.dal.entity.flatten.impl.NewBsonEntity;
import com.ebay.cloud.cms.dal.persistence.IPersistenceCommand;
import com.mongodb.BasicDBObject;

/**
 * replace embedded entity command
 * 
 * https://jira.mongodb.org/browse/SERVER-831
 * 
 * 
 * @author xjiang,zhihzhang
 *
 */
// for inner field, only update root and leaf entity now
public class EmbedReplaceCommand extends EmbedModifyCommand implements IPersistenceCommand {

    public EmbedReplaceCommand(NewBsonEntity entity, FlattenEntityIDHelper helper) {
        super(entity, helper);
    }

    protected void setEmbedObjectValue(BasicDBObject embedObject) {
        embedObject.clear();
        for (Entry<String, Object> entry : entity.getNode().entrySet()) {
            embedObject.put(entry.getKey(), entry.getValue());
        }
    }

}
