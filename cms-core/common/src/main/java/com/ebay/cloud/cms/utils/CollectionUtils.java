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

import java.util.HashSet;
import java.util.Set;

public class CollectionUtils {
    //returned elements in set1 but not in set2
    public static Set<String> diffSet(Set<String> set1, Set<String> set2) {
        Set<String> result = new HashSet<String>();
        for (String s : set1) {
            if (!set2.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }
    
//    public static void main(String[] args) {
//        Set<String> set1 = new HashSet<String>();
//        Set<String> set2 = new HashSet<String>();
//        
//        set1.add("a");
//        set1.add("b");
//        set1.add("c");
//        
//        set2.add("c");
//        set2.add("f");
//        set2.add("g");
//        
//        System.out.println(diffSet(set1, set2).toString());
//    }

}
