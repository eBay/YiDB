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

package com.ebay.cloud.cms.utils;

public class CheckConditions {
    /**
     * throws exception if condition is false
     * 
     * @param condition
     * @param exception
     */
    public static void checkCondition(boolean condition, RuntimeException exception) {
        if (!condition) {
            throw exception;
        }
    }
    
    public static void checkNotNull(Object obj) {
        if (obj == null) {
            throw new RuntimeException();
        }
    }
    
    public static void checkNotNull(Object obj, RuntimeException exception) {
        if (obj == null) {
            throw exception;
        }
    }
    
    public static void checkNotNull(Object obj, String errorMessage) {
        if (obj == null) {
            throw new NullPointerException(errorMessage);
        }
    }
    
    public static void checkNotNull(Object obj, String errorMessageFormat, Object... args) {
        if (obj == null) {
            throw new NullPointerException(String.format(errorMessageFormat, args));
        }
    }
    
    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }
    
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
          }
    }
    
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    public static void checkArgument(boolean expression, String errorMessageFormat, Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(errorMessageFormat, args));
        }
    }

}
