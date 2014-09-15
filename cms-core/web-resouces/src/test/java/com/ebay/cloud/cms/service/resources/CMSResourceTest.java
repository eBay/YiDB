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

package com.ebay.cloud.cms.service.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.MockUriInfo;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.sysmgmt.server.CMSConfig;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public class CMSResourceTest extends MongoBaseTest {

    private static final String SOURCE_IP = "127.0.0.1";

    protected static CMSServer                    server                             = null;

    protected static final String                 RAPTOR_REPO                        = "raptor-paas";
    protected static final String                 RAPTOR_TEST_DATA                   = "raptorTopology.json";

    protected static final String                 DEPLOY_REPO                        = "software-deployment";
    protected static final String                 DEPLOY_TEST_DATA                   = "softwareDeploymentRuntime.json";

    protected static final String                 APPLICATION_SERVICE                = "ApplicationService";

    protected static final String                 SERVICE_INSTANCE                   = "ServiceInstance";

    protected static final String                 ENVIRONMENT                        = "Environment";

    protected static final MockUriInfo            nullMockUri                        = new MockUriInfo();

    protected static final MockHttpServletRequest mockHttpRequest                    = new MockHttpServletRequest();

    protected static final String                 nullConsisPolicy                   = null;

    protected static final String                 nullMode                           = null;

    protected static final String                 nullPriority                       = null;

    protected static String                       RAPTOR_MAIN_BRANCH_ID              = null;
    protected static String                       SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID = null;

    static {
        String delimeter = "/";
        URL configFile = CMSResourceTest.class.getResource("/" + CMSConfig.CONFIG_FILE);
        String path = configFile.getPath();
        System.setProperty(CMSConfig.CMS_HOME, path.substring(0, path.lastIndexOf(delimeter)));
    }

    @BeforeClass
    public static void startUpCMSServer() {
        RAPTOR_MAIN_BRANCH_ID = IBranch.DEFAULT_BRANCH;
        SOFTWARE_DEPLOYMENT_MAIN_BRANCH_ID = IBranch.DEFAULT_BRANCH;

        server = CMSServer.getCMSServer();
        server.start();
        
        nullMockUri.getQueryParameters().put(CMSResourceUtils.REQ_PARAM_COMPONENT, Arrays.asList("unitTestUser"));
    }

    @AfterClass
    public static void teardownCMSServer() {

    }

    public static String loadJson(String fileName) {
        InputStream is = CMSResourceTest.class.getResourceAsStream(fileName);
        try {
            String json = convertStreamToString(is);
            return json;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static List<Object> assertOkAndNotNullResult(CMSResponse response) {
        assertOk(response);

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() > 0);
        System.out.println(result.get(0));

        return result;
    }

    @SuppressWarnings("unchecked")
    protected static void assertOkAndNullResult(CMSResponse response) {
        assertOk(response);

        List<Object> result = (List<Object>) response.get(CMSResponse.RESULT_KEY);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.size() == 0);
    }
    
    protected static void assertOk(CMSResponse response) {
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("200", err.getCode());
    }
    
    protected static void assert400(CMSResponse response) {
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("400", err.getCode());
    }
    
    protected static void assert404(CMSResponse response) {
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("404", err.getCode());
    }
    
    protected static void assert500(CMSResponse response) {
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals("500", err.getCode());
    }

    protected EntityContext newEntityContext() {
        EntityContext context = new EntityContext();
        context.setSourceIp(SOURCE_IP);
        context.setModifier("unitTestUser");
        context.setRegistration(RegistrationUtils.getDefaultDalImplementation(dataSource));
        context.setFetchFieldProperty(true);
        context.setDbConfig(config);
        return context;
    }

    protected MetadataContext newMetadataContext() {
        MetadataContext context = new MetadataContext();
        context.setSourceIp(SOURCE_IP);
        context.setSubject("unitTestUser");
        context.setDbConfig(config);
        return context;
    }
    
    protected void assertErrorCode(int errorCode, CMSResponse response) {
        Assert.assertNotNull(response);
        Error err = (Error) response.get(CMSResponse.STATUS_KEY);
        Assert.assertEquals(String.valueOf(errorCode), err.getCode());
    }

    protected static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

}
