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

package com.ebay.cloud.cms.service.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

/**
 * User: Rene Xu
 * Email: rene.xu@ebay.com
 * Date: 5/23/12 10:05 AM
 */
public class CMSConfigListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String cmsHome = System.getenv("CMS_HOME");
        if (cmsHome != null) {
            System.setProperty("CMS_HOME", cmsHome);
        }
        else {
            System.setProperty("CMS_HOME", ".");
        }
        CMSServer.getCMSServer().start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        CMSServer.getCMSServer().shutdown();
    }
}
