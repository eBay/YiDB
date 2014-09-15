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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_DEFAULT)
public class CMSResponse extends LinkedHashMap<String, Object> {

    private static final long  serialVersionUID = 1L;

    public static final String STATUS_KEY        = "status";

    public static final String RESULT_KEY       = "result";

    public CMSResponse() {
        this(Error.OK);
    }

    private CMSResponse(Error error) {
        super.put(STATUS_KEY, error);
    }

    public CMSResponse(String code, String msg) {
        this(new Error(code, msg));
    }

    public CMSResponse(String code, String msg, Throwable t) {
        if (t == null) {
            super.put(STATUS_KEY, new Error(code, msg));
            return;
        }

        String exceptTrace = null;
        StringWriter sw = new StringWriter();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
        } finally {
        	if (pw != null) {
        		pw.close();
        	}
        	pw = null;
        }
        exceptTrace = sw.toString();
        super.put(STATUS_KEY, new Error(code, msg, exceptTrace));
    }

    @SuppressWarnings("unchecked")
    public void addResult(Object value) {
        if (value != null) {
            if (value instanceof Collection) {
                addResult((Collection<Object>) value);
            } else {
                List<Object> result = getResultItem();
                result.add(value);
            }
        }
    }

    public void addResult(Collection<Object> values) {
        if (values != null) {
            List<Object> result = getResultItem();
            result.addAll(values);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getResultItem() {
        List<Object> result = (List<Object>) super.get(RESULT_KEY);
        if (result == null) {
            result = new LinkedList<Object>();
            super.put(RESULT_KEY, result);
        }
        return result;
    }

    @Override
    public Object put(String key, Object value) {
        Pair p = new Pair();
        p.setKey(key);
        p.setValue(value);
        this.addResult(p);
        return p;
    }

    public void addProperty(String key, Object value) {
        super.put(key, value == null ? null : value);
    }
    
    public String getErrorMsg() {
        if (this.containsKey(STATUS_KEY)) {
            Error err = (Error)this.get(STATUS_KEY);
            return err.getMsg();
        }
        return "";
    }
}
