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

import java.util.ArrayList;
import java.util.Arrays;

public class SixtyTwoBase {
    static final char[] characters = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    };
    
    static final int base;
    
    static {
        base = characters.length;
    }
    
    /**
     * change a 10 based value to 62 based value
     * 
     * @param value
     * @return
     */
    public static String tenBaseToSixtyTwo(int value) {
        int i = value;
        ArrayList<Character> array = new ArrayList<Character>();
        while(i >= base) {
            array.add(characters[i%base]);
            i = i / base;
        }
        array.add(characters[i%base]);

        
        StringBuilder sb = new StringBuilder();
        for (int x = (array.size() -1); x >= 0; x--) {
            sb.append(array.get(x));
        }

        return sb.toString();
    }
    
    /**
     * change a 62 based value to 10 based value
     * 
     * @param value
     * @return
     */
    public static int sixtyTwoBaseToTen(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("62 based value can not be null or empty");//("illegal value for 62 based value, " + value);
        }
        
        int result = 0;
        int length  = value.length();
        int cbase = 1;
        for (int i = 0; i < length; i++) {
            char c = value.charAt(length - i - 1);
            int index = Arrays.binarySearch(characters, c);
            if (index == -1) {
                throw new IllegalArgumentException("illegal value for 62 based value, " + value);
            }
            
            result += (index * cbase);
            cbase *= base;
        }
        
        return result;
    }
    
    /**
     * add ONE to a 62 based value
     * 
     * @param value a 62 based value in string format
     * @return the value plus ONE in 62 based format
     */
    public static String addOne(String value) {
        int v = sixtyTwoBaseToTen(value);
        return tenBaseToSixtyTwo(v + 1);
    }
}
