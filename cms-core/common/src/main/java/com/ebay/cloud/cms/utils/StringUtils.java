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


package com.ebay.cloud.cms.utils;

import java.util.Arrays;
import java.util.List;


public class StringUtils {
    public static boolean isNullOrEmpty(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        return false;
    }

    public static boolean isNull(String s) {
        if (s == null) {
            return true;
        }
        return false;
    }
    
    public static String join(String separator, String... array) {
        return join(separator, Arrays.asList(array));
    }
    
    public static String join(String separator, List<String> list) {
        if (separator == null) {
            throw new RuntimeException("Separator is null!");
        }
        StringBuilder sb = new StringBuilder();
        if (list.size() > 0) {
            sb.append(list.get(0));
            for (int i = 1; i < list.size(); i++) {
                sb.append(separator);
                sb.append(list.get(i));
            }
        }
        return sb.toString();
    }
    
//    public static void main(String[] args) {
//        List<String> list = new ArrayList<String>();
//        list.add("a");
//        System.out.println(join(",", list));
//        list.add("b");
//        System.out.println(join(",", list));
//
//        System.out.println(join(",", "a"));
//        System.out.println(join(",", "a", "b"));
//    }
}
