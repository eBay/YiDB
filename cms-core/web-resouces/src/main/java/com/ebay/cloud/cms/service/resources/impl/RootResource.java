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

package com.ebay.cloud.cms.service.resources.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.service.resources.IRootResource;

/**
 * User: Rene Xu Email: rene.xu@ebay.com Date: 5/8/12 2:38 PM
 */
@Path("/")
@Produces("application/json")
public class RootResource implements IRootResource {

	private static final Logger logger = LoggerFactory.getLogger(RootResource.class);
	
    @GET
    public WelcomeMessage welcomeMessage() {
        return new WelcomeMessage("Hello CMS!");
    }
    
    @GET
    @Path("/grabReleaseNotes")
    @Produces("text/html")
    public String grabReleaseNotes() {
    	URL u = null;
		BufferedReader in = null;
		StringBuffer sb = new StringBuffer();
		try {
			u = new URL("https://localhost/CMS+Release+Notes");
			in = new BufferedReader(new InputStreamReader(u.openStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
		} catch (MalformedURLException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				in = null;
			}
		}
		return sb.toString();
    }

}
