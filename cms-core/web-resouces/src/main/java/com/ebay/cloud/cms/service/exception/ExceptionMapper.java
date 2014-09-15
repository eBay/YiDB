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

package com.ebay.cloud.cms.service.exception;

import javax.ws.rs.WebApplicationException;

import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.exception.CmsDalException.DalErrCodeEnum;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;

public class ExceptionMapper {
    public static WebApplicationException convert(CmsDalException e) {
        if (e.getErrorEnum() == DalErrCodeEnum.ENTITY_NOT_FOUND) {
            return new NotFoundException(e, e.getMessage());
        }
        
        return new CMSServerException(e.getErrorCode(), e.getMessage(), e);
    }
    
    public static WebApplicationException convert(CmsEntMgrException e) {
        if (e.getErrorEnum() == EntMgrErrCodeEnum.BRANCH_NOT_FOUND) {
            return new NotFoundException(e, "Branch not found: " + e.getMessage());
        }
        if (e.getErrorEnum() == EntMgrErrCodeEnum.REPOSITORY_NOT_FOUND) {
            return new NotFoundException(e, "Repository not found: " + e.getMessage());
        }
        
        return new CMSServerException(e.getErrorCode(), e.getMessage(), e);
    }
    
    public static WebApplicationException convert(MetaDataException e) {
        if (e.getErrorEnum() == MetaErrCodeEnum.REPOSITORY_NOT_EXISTS) {
            return new NotFoundException(e, e.getMessage());
        } else if (e.getErrorEnum() == MetaErrCodeEnum.META_CLASS_NOT_EXISTS) {
        	throw new NotFoundException(e, e.getMessage());
        } else if (e.getErrorEnum() == MetaErrCodeEnum.ILLEGAL_REPOSITORY) {
        	throw new BadParamException(e, e.getMessage());
        } 
        
        return new CMSServerException(e.getErrorCode(), e.getMessage(), e);
    }
}
