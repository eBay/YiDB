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

import java.util.Arrays;
import java.util.List;

public abstract class EqualsUtil {
    public static boolean isEquals(Object obj1, Object obj2) {
        if (obj1 instanceof List && obj2 instanceof List) {
            return isEquals((List<?>)obj1, (List<?>)obj2);
        } else {
            return equal(obj1, obj2);
        }
    }
    
    private static boolean isEquals(List<?> l1, List<?> l2) {
        if (l1 == null && l2 == null) {
            return true;
        }
        
        if (l1 == null || l2 == null) {
            return false;
        }
        
        if (l1.size() != l2.size()) {
            return false;
        }
        
        for (int i = 0; i < l1.size(); i++) {
            if (!equal(l1.get(i), l2.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int compare(int a, int b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }

    public static int compare(long a, long b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }
    
    public static int hashCode(Object... objects) {
        return Arrays.hashCode(objects);
    }
}
