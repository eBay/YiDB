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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.ebay.cloud.cms.dal.entity.AbstractEntityIDHelper;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.mongodb.DBObject;

/**
 * extract embed entity from root entity by embed id 
 * 
 * @author xjiang
 *
 */
public class EmbedDBObjectFilter {
	
    public static DBObject filter(String embedId, DBObject rootObject, MetaClass rootMetaClass,
            Collection<String> fields, AbstractEntityIDHelper helper) {
	    List<String> pathList = helper.getEmbedPathSegs(embedId, rootMetaClass);
	    return traverse(rootObject, embedId, pathList, 0, fields);
	}

	@SuppressWarnings("unchecked")
    private static DBObject traverse(DBObject currObject, String embedId, List<String> pathList, int index, Collection<String> fields) {
	    if (index == pathList.size()) {
	        if (compareEntityId(embedId, currObject)) {
	            return filterFields(currObject, fields);
	        }
	        return null;
	    }
	    String fieldDbValueName = pathList.get(index);
        Object fieldValue = currObject.get(fieldDbValueName);
        if (fieldValue == null) {
            return null;
        }
	    if (fieldValue instanceof List) {
	        List<DBObject> listValue = (List<DBObject>)fieldValue;
	        for (DBObject childObject : listValue) {
	            DBObject res = traverse(childObject, embedId, pathList, index + 1, fields);
	            if (res != null) {
	                return res;
	            }
	        }
	    } else if (fieldValue instanceof DBObject) {
	        DBObject childObject = (DBObject)fieldValue;
	        return traverse(childObject, embedId, pathList, index + 1, fields);
	    }
	    return null;
	}

    @SuppressWarnings("unchecked")
    private static DBObject filterFields(DBObject currObject, Collection<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return currObject;
        }

        Set<String> keys = (Set<String>) currObject.toMap().keySet();
        for (String key : keys) {
            if (!fields.contains(key)) {
                currObject.removeField(key);
            }
        }
        return currObject;
    }

    private static boolean compareEntityId(String embedId, DBObject dbObject) {
	    Object entityId = dbObject.get(InternalFieldEnum.ID.getDbName());
	    if (embedId.equals(entityId)) {
	        return true;
	    }
	    return false;
	}

}
