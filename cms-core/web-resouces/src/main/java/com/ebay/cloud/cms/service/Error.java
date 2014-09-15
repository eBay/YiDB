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

package com.ebay.cloud.cms.service;

public class Error {
    public final static Error OK = new Error("200", "ok");

    private final String      code;
    private final String      msg;

    private final String      stackTrace;

    public Error(String code, String msg) {
        this(code, msg, null);
    }

    public Error(String code, String msg, String stackTrace) {
        super();
        this.code = code;
        this.msg = msg;
        this.stackTrace = stackTrace;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public final String getStackTrace() {
        return stackTrace;
    }

}