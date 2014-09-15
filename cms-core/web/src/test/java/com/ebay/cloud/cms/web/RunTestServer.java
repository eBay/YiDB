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

package com.ebay.cloud.cms.web;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import com.ebay.cloud.cms.entmgr.loader.RuntimeDataLoader;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.sysmgmt.server.CMSConfig;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;
import com.mongodb.Mongo;

/**
 * @author jianxu1
 */
public class RunTestServer {

    protected static final String RAPTOR_REPO      = "raptor-paas";
    private static final String   RAPTOR_TEST_DATA = "raptorTopology.json";

    protected static final String DEPLOY_REPO      = "software-deployment";
    private static final String   DEPLOY_TEST_DATA = "softwareDeploymentRuntime.json";

    protected static final String STRATUS_REPO = "stratus-ci";
    protected static final String STRATUS_TEST_DATA = "stratusRuntime.json";
    private static final String CMSDB_REPO          = "cmsdb";
    private static final String CMSDB_TEST_DATA     = "cmsdbRuntime.json";

    private static URI getBaseURI(int port) {
        return UriBuilder.fromUri("http://localhost:" + port + "/ui/home.html").build();
    }

    private static Server   jettyServer;

    protected static Server startJettyServer(int port) throws Exception {
        System.out.println("Starting jetty...");

        Server server = new Server(port);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        ServletContextHandler rootHandler = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);
        
//        rootHandler.addFilter(CALServletFilter.class, "/*", FilterMapping.REQUEST);
        
        ServletHolder apiServletholder = new ServletHolder(com.sun.jersey.spi.container.servlet.ServletContainer.class);
        apiServletholder.setInitParameter("com.sun.jersey.config.property.packages",
                "com.ebay.cloud.cms.service.resources.impl");
        apiServletholder.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        apiServletholder.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.PostReplaceFilter,com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
        apiServletholder.setInitParameter("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
        rootHandler.addServlet(apiServletholder, "/cms/*");

        // ui servlet settting -
        // http://docs.codehaus.org/display/JETTY/Servlets+Bundled+with+Jetty
        ServletHolder uiServletHolder = new ServletHolder(new ResourceDefaultServlet());
        uiServletHolder.setInitParameter("resourceBase", getUIProjectResource());
        uiServletHolder.setInitParameter("dirAllowed ", Boolean.FALSE.toString());
        rootHandler.addServlet(uiServletHolder, "/ui/*");

        server.setHandler(rootHandler);

        server.start();
        return server;
    }

    @SuppressWarnings("serial")
    public static class ResourceDefaultServlet extends DefaultServlet {
        @Override
        public Resource getResource(String pathInContext) {
            if (pathInContext.startsWith("/ui")) {
                if (pathInContext.length() == 3 || pathInContext.length() == 4) {
                    pathInContext = "home.html";
                } else {
                    pathInContext = pathInContext.substring(3);
                }
            }
            return super.getResource(pathInContext);
        }
    }

    public static void loadTestData(MongoDataSource ds) {
        MetadataDataLoader.getInstance(ds).loadTestDataFromResource();
        MetadataDataLoader.getInstance(ds).loadCMSDBMetaDataFromResource();
        
        IRepositoryService repositoryService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");   
        RuntimeDataLoader.getDataLoader(ds, repositoryService, RAPTOR_REPO).load(RAPTOR_TEST_DATA);
        RuntimeDataLoader.getDataLoader(ds, repositoryService, DEPLOY_REPO).load(DEPLOY_TEST_DATA);
        RuntimeDataLoader.getDataLoader(ds, repositoryService, STRATUS_REPO).load(STRATUS_TEST_DATA);
        RuntimeDataLoader.getDataLoader(ds, repositoryService, CMSDB_REPO).load(CMSDB_TEST_DATA);
    }

    private static boolean isMongoRunning(MongoDataSource ds) {
        try {
            Mongo mongo = ds.getMongoInstance();
            mongo.getDB("test").getCollection("test").count();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static MongoDataSource getMongoDataSource() {
        CMSConfig cmsConfig = new CMSConfig();
        cmsConfig.loadDefaultConfig();
        cmsConfig.loadExternalConfig();

        String connectionString = cmsConfig.getMongoConnection();

        System.out.println("connection to Mongo db on " + connectionString);

        return new MongoDataSource(connectionString);
    }

    private static boolean startExternalMongoIfNeeded(MongoDataSource ds) {
        boolean externalMongo = false;
        if (isMongoRunning(ds)) {
            System.out.println("External MongoDB server is running");
            externalMongo = true;
        }

        if (!externalMongo) {
            System.out.println("------------------------------------------------------");
            System.out.println("Will start MongoDB server for test...");
            System.out.println("The first run will download Mongo server from internet.");

            CMSMongoTest.setServer("localhost");
            CMSMongoTest.setPort(27017);
            CMSMongoTest.setExternal(false);

            CMSMongoTest.setUpBeforeClass();

            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    CMSMongoTest.tearDownBeforeClass();
                    try {
                        mainThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return externalMongo;
    }

    public static void main(String[] args) throws Exception {

        startServer(args);

        int port = jettyServer.getConnectors()[0].getPort();
        System.out.println(String.format("CMS app started with WADL available at "
                + "%sapplication.wadl\nTry out %s\nHit enter to stop it...", getBaseURI(port), getBaseURI(port)));
        System.in.read();
        stopServer();
    }

    public static void stopServer() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
        }
    }

    public static void startServer(String[] args) throws Exception {
        MongoDataSource ds = getMongoDataSource();

        boolean externalMongo = startExternalMongoIfNeeded(ds);

        if (!externalMongo || (args.length > 0 && args[0].equals("-initData"))) {
            loadTestData(ds);
        }

        int port = 9090;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (Exception e) {
                port = 9090;
            }
        }
            
        CMSServer.getCMSServer().start();

        jettyServer = startJettyServer(port);
    }

    private static String getUIProjectResource() throws IOException {
        String uiProjectPath = System.getProperty("CMS_UI", "../cms-ui/src/main/webapp/ui/");
        System.out.println("using CMS_UI :" + uiProjectPath);
        File f = new File(uiProjectPath);
        if (f.exists()) {
            System.out.println("start ui project at uri: /ui");
            return f.getCanonicalPath();
        }
        return null;
    }
}
