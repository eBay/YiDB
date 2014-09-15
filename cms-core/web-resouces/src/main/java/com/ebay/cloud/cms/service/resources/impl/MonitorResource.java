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

package com.ebay.cloud.cms.service.resources.impl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.IMonitorResource;
import com.ebay.cloud.cms.sysmgmt.monitor.MonitorStatisticsManager;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

/**
 * @author Liangfei(Ralph) Su
 *
 */
@Path("/monitors/")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorResource implements IMonitorResource {

    private CMSServer cmsServer = CMSServer.getCMSServer();

	@GET
	public CMSResponse getMetrics(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
		CMSResponse response = new CMSResponse();
		response.addResult(cmsServer.getStatistics());
		return response;
	}
	
	@GET
	@Path("/{metricName}")
	public CMSResponse getMetric(@Context UriInfo uriInfo, @PathParam("metricName") String metricName, @Context HttpServletRequest request) {
	    CMSResponse response = new CMSResponse();
	    Map<String, Object> result = cmsServer.getStatistics(metricName);
	    if(result == null) {
	        throw new NotFoundException("No such metric name exists");
	    }
        response.addResult(result);
        return response;
	    
	}
	
	public MonitorStatisticsManager getStatisticsManager() {
		return cmsServer.getMonitorStatisticsManager();
	}
	
	public Map<String, Object> getCurrentConfigurations() {
		return cmsServer.getCurrentConfigurations();
	}
}
