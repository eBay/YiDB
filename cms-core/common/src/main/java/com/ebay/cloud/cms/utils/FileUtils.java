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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FileUtils {
    public static void readToBuffer(StringBuffer buffer, String filePath, Charset charset) throws IOException {
        InputStream is = new FileInputStream(filePath);
        String line; 
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
        line = reader.readLine(); 
        while (line != null) {
            buffer.append(line); 
            buffer.append("\n"); 
            line = reader.readLine(); 
        }
        reader.close();
        is.close();
    }

    public static String readFile(String filePath, Charset charset) throws IOException {
        StringBuffer sb = new StringBuffer();
        FileUtils.readToBuffer(sb, filePath, charset);
        return sb.toString();
    }
}
