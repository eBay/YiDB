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

package com.ebay.cloud.cms.utils.mongo.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
//import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.config.CMSDBConfig;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.StringUtils;

import de.flapdoodle.embedmongo.MongoDBRuntime;
import de.flapdoodle.embedmongo.MongodExecutable;
import de.flapdoodle.embedmongo.MongodProcess;
import de.flapdoodle.embedmongo.config.MongodConfig;
import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.runtime.Network;

/**
 * A cms mongo instance for test
 * 
 * Test classes extends from it can use it's getConnectionString to get connection string 
 * 
 * By default, it will download a mongo binary and run it as a server for test. 
 * Mongo configuration file is in src/main/resources/mongo/mongo.properties, the properties will be override by values in pom file.
 * It can use external mongo server by changing the <external/> property in pom.xml.
 * 
 * @author liubo
 *
 */
public class CMSMongoTest {
    private static final Logger logger = LoggerFactory.getLogger(CMSMongoTest.class);
    
    public static final String MONGO_CONFIG_PATH = "/mongo";
    public static final String MONGO_PROPERTIES = MONGO_CONFIG_PATH + "/mongo.properties";
    
    private static String version = "V2_0_4";
    private static String server = "localhost";
    private static int port = 27017;
    protected static MongoDataSource dataSource;
    private static boolean external = false;
    
    protected static CMSDBConfig config;
    
    static {
        InputStream in = null;
        try {
            in = CMSMongoTest.class.getResourceAsStream(MONGO_PROPERTIES);
            Properties p = new Properties();
            
            p.load(in);
            String server = p.getProperty("server");
            if (!StringUtils.isNullOrEmpty(server)) {
                CMSMongoTest.server = server;
            }
            String port = p.getProperty("port");
            if (!StringUtils.isNullOrEmpty(port)) {
                CMSMongoTest.port = Integer.valueOf(port);
            }
            String version = p.getProperty("version");
            if (!StringUtils.isNullOrEmpty(version)) {
                CMSMongoTest.version = version;
            }
            String external = p.getProperty("external");
            if (!StringUtils.isNullOrEmpty(external)) {
                CMSMongoTest.external = Boolean.valueOf(external);
            }
            dataSource = new MongoDataSource(getConnectionString());
            config = new CMSDBConfig(dataSource);
        } catch (IOException e) {
            logger.error("failed to load mongo configuration");
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("failed to close mongo configuration fiel");
                    throw new RuntimeException(e);
                }
            }
        }       
    }
    
    private static MongodExecutable mongodExe;
    private static MongodProcess mongod;
    
    public static void setServer(String server) {
        CMSMongoTest.server = server;
    }
    
    public static void setPort(int port) {
        CMSMongoTest.port = port;
    }
    
    public static void setExternal(boolean external) {
        CMSMongoTest.external = external;
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        if (external) {
            logger.info("using external mongo server for test, server:{}, port:{}", server, port);
        }
        else {
            logger.info("using internal mongo server for test, mongo server version is {}", version);
            
            MongoDBRuntime runtime = MongoDBRuntime.getDefaultInstance();
            
            try {
                mongodExe = runtime.prepare(new MongodConfig(Version.valueOf(version), port, Network.localhostIsIPv6()));
                mongod = mongodExe.start();
            } catch (Exception e) {
                logger.error("failed to start internal mongo server", e);
                throw new RuntimeException(e);
            }
            
            logger.info("internal mongo server started");
        }
    }
    
    @AfterClass
    public static void tearDownBeforeClass() {
        if (!external) {
        	//jianxu1: we should check if mongod is running before stop
        	if(mongod != null){
        		mongod.stop();
        	}
        	
        	if(mongodExe != null){
        		mongodExe.cleanup();
        	}
        }
    }
    
    public static String getConnectionString() {
        return server + ":" + port;
    }
    
    public static MongoDataSource getDataSource() {
    	return dataSource;
    }


    protected static final Random    random = new Random(System.currentTimeMillis());

    protected static String generateRandomName(String baseName) {
        return baseName + "-" +System.currentTimeMillis() + random.nextDouble();
    }
    
//    @Test
//    public void testAll(){
//        this.getDataSource().getMongoInstance().getDB("r1").dropDatabase();
//        this.getDataSource().getMongoInstance().getDB("inheritRepo").dropDatabase();
//    }

}
