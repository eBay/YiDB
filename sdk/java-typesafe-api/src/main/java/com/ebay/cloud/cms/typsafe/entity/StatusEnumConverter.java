/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;

import com.ebay.cloud.cms.typsafe.entity.ICMSEntity._StatusEnum;

/**
 * @author liasu
 * 
 */
public class StatusEnumConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static class StatusEnumSerializer extends JsonSerializer<_StatusEnum> {

        @Override
        public void serialize(_StatusEnum value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonProcessingException {
            jgen.writeObject(value.toString());
        }

    }

    public static class StatusEnumDeserializer extends JsonDeserializer<_StatusEnum> {

        @Override
        public _StatusEnum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            String enumVal = mapper.readValue(jp, String.class);
            for (_StatusEnum se : _StatusEnum.values()) {
                if (se.toString().equals(enumVal)) {
                    return se;
                }
            }
            throw new RuntimeException("status enum not found");
        }

    }

}
