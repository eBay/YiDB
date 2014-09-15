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

package com.ebay.cloud.cms.metadata.mongo.converter;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class ObjectConverter<T> {
    public static final ObjectMapper mapper = new ObjectMapper();
    private static Logger logger = LoggerFactory.getLogger(ObjectConverter.class);

    static {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String toJson(T object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("convert to json error", e);
            throw new MetaDataException(MetaErrCodeEnum.JSON_CONVERT_ERROR, "convert to json error", e);
        }
    }
    
    /**
     * Generate json tree based on the given object.
     * FIXME: a better choice is use ObjectMapper.valuToTree(); but it seems a bug of jackson, this method doesn't generated
     * a valid tree for meta class fieldMap. 
     * 
     * @param object
     * @return
     */
    public JsonNode toJsonNode(T object) {
        JsonNode json = null;
        try {
            String jsonString = toJson(object);
            json = mapper.readTree(jsonString);
            return json;
        } catch (Exception e) {
            logger.error("convert to json error", e);
            throw new MetaDataException(MetaErrCodeEnum.JSON_CONVERT_ERROR, "convert to json error", e);
        }
    }
    
    public T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            logger.error("convert json to repo error", e);
            throw new MetaDataException(MetaErrCodeEnum.JSON_CONVERT_ERROR, "convert json to repo error", e);
        }
    }
    
    public DBObject toBson(T object) {
        String json = toJson(object);
        return (DBObject)JSON.parse(json);
    }
    
    public T fromBson(DBObject dbObject, Class<T> type) {
        return fromJson(JSON.serialize(dbObject), type);
    }
}
