package com.ebay.cloud.cms.typsafe.restful;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

@SuppressWarnings("deprecation")
public class Constants {

    public static final String        META_PACKAGE = "com.ebay.cloud.cms.model";

    public static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        //objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

}
