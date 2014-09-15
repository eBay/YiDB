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

package com.ebay.cloud.cms.dalapi.persistence.impl.root;

import java.util.List;

import com.ebay.cloud.cms.dal.persistence.IRetrievalCommand;
import com.ebay.cloud.cms.dal.persistence.MongoExecutor;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.InternalFieldFactory.StatusEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * return the entity count
 * 
 * @author zhuang1
 *
 */
public class RootCountCommand implements IRetrievalCommand {
    private MetaClass meta;
    private List<String> oids;
    private String branchId;
    private long resultCount;
    
    public RootCountCommand(MetaClass meta, List<String> oids, String branchId) {
        this.meta = meta;
        this.oids = oids;
        this.branchId = branchId;
    }

	@Override
	public void execute(PersistenceContext context) {
        DBObject queryObject = new BasicDBObject();
        queryObject.put(InternalFieldEnum.ID.getDbName(), new BasicDBObject("$in", oids));
        queryObject.put(InternalFieldEnum.STATUS.getDbName(), StatusEnum.ACTIVE.toString());
        queryObject.put(InternalFieldEnum.BRANCH.getDbName(), branchId);
        resultCount = MongoExecutor.count(context, meta, queryObject);
    }

    @Override
    public Long getResult() {
        return resultCount;
    }
}
