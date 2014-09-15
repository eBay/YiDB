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


package com.ebay.cloud.cms.service.resources.operation;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.exception.NotSupportOperationException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

public abstract class MetaClassOperation {
	private static final Logger logger = LoggerFactory.getLogger(MetaClassOperation.class);
	
	protected final CMSPriority p;
	protected final MetadataContext context;
	protected final String repoName;
	protected final String metaName;
	protected final String errorMsg;
	protected CMSServer cmsServer = CMSServer.getCMSServer();

	public MetaClassOperation(CMSPriority p, String repoName, String metaName, MetadataContext context,
			String errorMsg) {
		this.p = p;
		this.context = context;
		this.repoName = repoName;
		this.metaName = metaName;
		this.errorMsg = errorMsg;
	}

	public final CMSResponse execute() {
		try {
			performAction();
		} catch (MetaClassNotExistsException e) {
			logger.error("Error when execute MetaClassOperation", e);
			throw new NotFoundException(e, "MetaClass not found!");
		} catch (MetaDataException e) {
			logger.error("Error when execute MetaClassOperation", e);
			throw new BadParamException(e, errorMsg);
		} catch (CannotServeException e) {
			logger.error("Error when execute MetaClassOperation", e);
			throw new ServiceUnavailableException(e.getMessage());
		} catch (WebApplicationException e) {
			logger.error("Error when execute MetaClassOperation", e);
			throw e;
		} catch (NotSupportOperationException nsoe) {
			logger.error("Error when execute MetaClassOperation", nsoe);
			throw new BadParamException(nsoe, errorMsg);
		} catch (Throwable t) {
			logger.error("Error when execute MetaClassOperation", t);
			throw new CMSServerException(t);
		}
		return new CMSResponse();
	}

	protected abstract void performAction();
}
