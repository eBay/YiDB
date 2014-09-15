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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple cms config backed by a map
 * 
 * @author liasu
 * 
 */
public class SimpleCMSConfig implements ICMSConfig {

    private final Map<String, String> configs;

    public SimpleCMSConfig(Map<String, String> configs) {
        this.configs = new HashMap<String, String>(configs);
    }

    @Override
    public String getMongoConnection() {
        return configs.get(CMSConfig.MONGO_CONNECTION);
    }

    @Override
    public String getServerName() {
        return configs.get(CMSConfig.SERVER_NAME);
    }

    @Override
    public String getCMSHome() {
        return configs.get(CMSConfig.CMS_HOME);
    }

    @Override
    public void loadDefaultConfig() {
        // do nothing
    }

    @Override
    public boolean loadExternalConfig() {
        // do nothing
        return false;
    }

    @Override
    public boolean allowRegExpFullScan() {
        return Boolean.parseBoolean(configs.get(CMSConfig.ALLOW_REGGEX_FULL_SCAN));
    }

}
