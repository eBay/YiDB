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

package com.ebay.cloud.cms.entmgr.entity;

import com.ebay.cloud.cms.dal.entity.IEntity;

/**
 * Call back for entity modification: create/update/replace/delete
 * 
 * @author liasu
 * 
 */
public interface IEntityOperationCallback {

    public enum Operation {
        GET,

        CREATE,

        MODIFY, // including entity field modify/delete

        REPLACE,

        DELETE
    }

    /**
     * Call to be called before an entity operation happens.
     * 
     * Implementation could throw exception itself to give more detailed message
     * and error code, like REST layer exceptions extend
     * WebApplicationException, e.g.
     * com.ebay.cloud.cms.service.exception.UnauthorizedException.
     * 
     * For batch modify, the check is called before any modify operation
     * performed.
     * 
     * @param existingEntity
     *            - the existing entity, might be null (replace/create entity operation
     *            support null existing)
     * @param operation
     *            - the operation enum. @SeeAlso <code>Operation</code>
     * @param newEntity
     *            - the new entity converted from payload. Could be null for
     *            deletion.
     * @param context
     *            - the additional information of this operation. like subject,
     *            comment.
     * @return - boolean value indicate the pre-operation check is passed. If
     *         the return value is false, an exception will be thrown
     */
    boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context);

}
