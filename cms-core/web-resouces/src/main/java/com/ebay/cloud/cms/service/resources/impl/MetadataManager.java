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


package com.ebay.cloud.cms.service.resources.impl;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.entity.visitor.ActiveAPIMetadataVisitor;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.impl.QueryResource.QueryParameterEnum;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.StringUtils;

public class MetadataManager {
	private CMSServer cmsServer = CMSServer.getCMSServer();
	private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);
	
	public MetadataManager() {
	    
	}
	
	public MetaClass getMetaClassDetail(String reponame, String metatype, String metaVersion, CMSPriority p) {
		MetaClass m;
		try {
		    if (StringUtils.isNullOrEmpty(metaVersion)) {
		        m = cmsServer.getMetaClass(p, reponame, metatype);
		    } else {
		        int version = Integer.parseInt(metaVersion);
		        m = cmsServer.getMetaClass(p, reponame, metatype, version);
		    }
			
		} catch (RepositoryNotExistsException e) {
			logger.error("Error when getMetadataDetail ", e);
			throw new NotFoundException("repository not found: " + reponame);
		} catch (NumberFormatException e) {
            logger.error("Error when getMetadataDetail ", e);
            throw new BadParamException("invalid version: " + metaVersion);
        }
		
		if (m == null) {
			logger.error("Error when getMetadataDetail: metaclass not found: " + metatype);
			String msg = null;
			if (!StringUtils.isNullOrEmpty(metaVersion)) {
			    msg = String.format("metaclass not found: %s with version %s", metatype, metaVersion);
			} else {
			    msg = String.format("metaclass not found: %s", metatype);
			}
			throw new NotFoundException(msg);
		}
		return m;
	}

	public Object getMetadataDetail(String reponame, String metatype, String metaVersion, CMSQueryMode mode, CMSPriority p) {
		MetaClass m = getMetaClassDetail(reponame, metatype, metaVersion, p);
		return metaUriFilter(mode, m);
	}
	
	public Object metaUriFilter(CMSQueryMode mode, List<MetaClass> result) {
		if (mode == CMSQueryMode.URI || mode == CMSQueryMode.SHOWALL) {
			ActiveAPIMetadataVisitor visitor = new ActiveAPIMetadataVisitor(mode);
			visitor.traverse(result);
			return visitor.getBuiltResult();
		}
		return result;
	}

	public Object metaUriFilter(CMSQueryMode mode, MetaClass result) {
		if (mode == CMSQueryMode.URI || mode == CMSQueryMode.SHOWALL) {
			ActiveAPIMetadataVisitor visitor = new ActiveAPIMetadataVisitor(mode);
			visitor.traverse(result);
			return visitor.getBuiltResult();
		}
		return result;
	}
	
    public MetadataContext getContext(UriInfo uriInfo, HttpServletRequest request) {
        MetadataContext context = new MetadataContext();
        String sourceIp = (String) request.getAttribute(CMSResourceUtils.X_CMS_CLIENT_IP);
        if (sourceIp == null) {
            context.setSourceIp(request.getRemoteAddr());
        } else {
            context.setSourceIp(sourceIp);
        }
        MultivaluedMap<String, String> mmap = uriInfo.getQueryParameters();
        String cid = (String) request.getAttribute(CMSResourceUtils.REQ_PARAM_COMPONENT);
        String uid = mmap.getFirst(CMSResourceUtils.REQ_PARAM_UID);
        context.setSubject(cid);
        context.setUserId(uid);
        String dal = mmap.getFirst(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION);
        context.addAdditionalParameter(CMSResourceUtils.REQ_PARAM_DAL_IMPLEMENTATION, dal);
        if (StringUtils.isNullOrEmpty(context.getUserId())) {
            context.setUserId(sourceIp);
        }
        String refresh = request.getParameter(CMSResourceUtils.REQ_REFRESH_CACHE);
        context.setRefreshRepsitory(Boolean.parseBoolean(refresh));
        return context;
    }
	
	public Object getMetadataHistory(UriInfo uriInfo, String reponame, String metatype, CMSPriority p,
			HttpServletRequest request) {
		MetadataContext context = getContext(uriInfo, request);
		try {
			context.setStart(MetadataHelper.getQueryTime(uriInfo, "start"));
			context.setEnd(MetadataHelper.getQueryTime(uriInfo, "end"));
		} catch (IllegalArgumentException iae) {
			throw new BadParamException(iae.getMessage());
		}
		context.setLimit(MetadataHelper.getQueryInteger(uriInfo, QueryParameterEnum.limit.name()));
		context.setSkip(MetadataHelper.getQueryInteger(uriInfo, QueryParameterEnum.skip.name()));

		List<IEntity> histories = cmsServer.getMetaHistory(p, reponame, metatype, context);

		List<JsonNode> arrayNode = new ArrayList<JsonNode>();
		for (IEntity entity : histories) {
			arrayNode.add((JsonNode) entity.getNode());
		}
		return arrayNode;
	}
}
