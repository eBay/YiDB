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

package com.ebay.cloud.cms.sysmgmt.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.utils.StringUtils;

public class CMSConfig implements ICMSConfig {
    private static final Logger logger = LoggerFactory.getLogger(CMSConfig.class);
    
    public static final String MONGO_CONNECTION = "mongo.connection";
    public static final String ALLOW_REGGEX_FULL_SCAN = "config.allow.regex.fullscan";
    public static final String SERVER_NAME = "server.name";
    public static final String DEFAULT_CONFIG_FILE = "/defaultCms.config";
    public static final String CONFIG_FILE = "cms.config";
    public static final String CMS_HOME = "CMS_HOME";
    
    private String mongoConnection = "localhost:27017";
    private String serverName = "localCMSServer";
    private boolean allowReggexFullScan = false;

    public CMSConfig() {
        super();
        
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            serverName = hostname;
        } catch (UnknownHostException e) {
            logger.error("can not get hostname from system, use default host name as serverName");
        }
    }

    CMSConfig(String connectionString, String server) {
        this.mongoConnection = connectionString;
        this.serverName = server;
    }

    public String getMongoConnection() {
        return mongoConnection;
    }
    
    public String getServerName() {
        return serverName;
    }
    
    public void loadDefaultConfig() {
        InputStream in = null;

        try {
            in = CMSConfig.class.getResourceAsStream(DEFAULT_CONFIG_FILE);
            loadConfigFromInputStream(in);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("failed to close config file", e);
                    throw new RuntimeException("failed to close config file", e);
                }
            }
        }

    }
    
    private void loadConfigFromInputStream(InputStream in) {
        try {
            Properties p = new Properties();
            p.load(in);

            String conn = p.getProperty(MONGO_CONNECTION);
            if (!StringUtils.isNullOrEmpty(conn)) {
                mongoConnection = conn;
            }
            String serverName = p.getProperty(SERVER_NAME);
            if (!StringUtils.isNullOrEmpty(serverName)) {
                this.serverName = serverName;
            }
            allowReggexFullScan = Boolean.parseBoolean(p.getProperty(ALLOW_REGGEX_FULL_SCAN));
        } catch (IOException e) {
            logger.error("failed to load file from default config file", e);
            throw new RuntimeException("failed to load file from default config file", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("failed to close config file", e);
                    throw new RuntimeException("failed to close config file", e);
                }
            }
        }
    }

    public String getCMSHome() {
        return getEnvCMSHome();
    }

    public static String getEnvCMSHome() {
        String cmsHome = System.getenv().get(CMS_HOME);
        if (StringUtils.isNullOrEmpty(cmsHome)) {
            cmsHome = System.getProperty(CMS_HOME);
            if (StringUtils.isNullOrEmpty(cmsHome)) {
                return null;
            }
        }
        
        return cmsHome;
    }

    public boolean loadExternalConfig() {
        String cmsHome = getCMSHome();
        if (cmsHome == null) {
            logger.info("CMS_HOME not set, use default configuration");
            return false;
        }

        File cmsHomeFolder = new File(cmsHome);
        if (!cmsHomeFolder.exists() || !cmsHomeFolder.isDirectory()) {
            logger.info("{} folder not exists, use default configuration", cmsHomeFolder.getAbsoluteFile());
            return false;
        }
        
        File configFile = new File(cmsHomeFolder, CONFIG_FILE);
        if (!configFile.exists() || !configFile.isFile()) {
            logger.info("{} file not exists, use default configuration", configFile.getAbsoluteFile());
            return false;
        }
        
        InputStream in = null;
        try {
            in = new FileInputStream(configFile);
            logger.info("loading configurations from external config file {}", configFile.getAbsolutePath());
            loadConfigFromInputStream(in);
        } catch (FileNotFoundException e) {
            logger.error("fail to open config File in CMS_HOME");
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.error("failed to close config file", e);
                    throw new RuntimeException("failed to close config file", e);
                }
            }
        }

        return true;
    }

    @Override
    public boolean allowRegExpFullScan() {
        return allowReggexFullScan;
    }

}
