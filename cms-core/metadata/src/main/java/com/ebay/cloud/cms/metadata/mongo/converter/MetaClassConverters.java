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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.exception.JsonParsingException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.StringUtils;

public abstract class MetaClassConverters {

    private static final ObjectMapper mapper = ObjectConverter.mapper;
    private static final Logger       logger = LoggerFactory.getLogger(MetaClassConverters.class);

    public static class MetaFieldSerializer extends JsonSerializer<Collection<MetaField>> {

        @Override
        public void serialize(Collection<MetaField> values, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {

            Map<String, MetaField> target = new HashMap<String, MetaField>();

            for (MetaField v : values) {
                // ignore internal fields
                if (!v.isInternal() && !v.isVirtual()) {
                    if (v.getDbName() == null) {
                        throw new MetaDataException(MetaErrCodeEnum.DB_NAME_REQUIRED, "dbName can not be null");
                    } else {
                        target.put(v.getName(), v);
                    }
                }
            }

            jgen.writeObject(target);
        }
    }

    public static class MetaFieldDeserializer extends JsonDeserializer<Collection<MetaField>> {

        @Override
        public Collection<MetaField> deserialize(JsonParser parser, DeserializationContext context) throws IOException,
                JsonProcessingException {
            MetaFieldNodeReader reader = new MetaFieldNodeReader("name");
            return reader.deserialze(parser, context);
        }

    }

    public static class DataTypeEnumDeserializer extends JsonDeserializer<MetaField.DataTypeEnum> {

        @Override
        public DataTypeEnum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            String str = mapper.readValue(jp, String.class);
            return DataTypeEnum.fromString(str);
        }
    }

    public static class DataTypeEnumSerializer extends JsonSerializer<MetaField.DataTypeEnum> {

        @Override
        public void serialize(DataTypeEnum value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }

    public static class MetaFieldNodeReader extends JsonMapNodeReader<MetaField> {

        public MetaFieldNodeReader(String nameNode) {
            super(nameNode, MetaField.class);
        }

        @Override
        public MetaField readObject(JsonNode node, String fieldName) throws JsonParseException,
                JsonMappingException, IOException {
            JsonNode typeNode = node.get("dataType");
            if (typeNode == null || typeNode.getTextValue() == null) {
                logger.error("didn't found dataType in field");
                throw new JsonParsingException("didn't found dataType in field");
            }
            String dataTypeStr = typeNode.getTextValue(); 

            MetaField.DataTypeEnum dataType = MetaField.DataTypeEnum.fromString(dataTypeStr);

            MetaField f;
            if (dataType == MetaField.DataTypeEnum.RELATIONSHIP) {
                f = mapper.readValue(node, MetaRelationship.class);
            } else {
                f = mapper.readValue(node, MetaAttribute.class);
            }

            if (f.getName() == null) {
                f.setName(fieldName);
            }
            return f;
        }

    }

    private static abstract class JsonMapNodeReader<T> {

        private final String   nodeName;
        private final Class<T> targetClass;

        public JsonMapNodeReader(String nameNode, Class<T> clz) {
            this.nodeName = nameNode;
            this.targetClass = clz;
        }

        public List<T> deserialze(JsonParser parser, DeserializationContext ctxt) throws JsonProcessingException,
                IOException {

            ObjectNode tree = (ObjectNode) mapper.readTree(parser);
            List<T> result = new ArrayList<T>(tree.size());

            Iterator<Entry<String, JsonNode>> iter = tree.getFields();
            while (iter.hasNext()) {
                Entry<String, JsonNode> e = iter.next();
                String name = e.getKey();
                JsonNode node = e.getValue();

                // field name can not be internal field names
                JsonNode fieldNode = node.get(nodeName);
                String nameInNode;
                if (fieldNode == null) {
                    nameInNode = name;
                } else {
                    nameInNode = fieldNode.getTextValue();
                    if (!name.equals(nameInNode)) {
                        logger.error(MessageFormat.format("{0} name not consistency", targetClass.getSimpleName()));
                        throw new JsonParsingException(MessageFormat.format("{0} name not consistency",
                                targetClass.getSimpleName()));
                    }
                }

                if (StringUtils.isNullOrEmpty(nameInNode)) {
                    logger.error(MessageFormat.format("no name found for {0}", targetClass.getSimpleName()));
                    throw new JsonParsingException(MessageFormat.format("no name found for {0}",
                            targetClass.getSimpleName()));
                }

                result.add(readObject(node, nameInNode));
            }

            return result;
        }

        protected abstract T readObject(JsonNode node, String fieldName) throws IOException, JsonParseException,
                JsonMappingException;
    }

    private static class MetaIndexNodeReader extends JsonMapNodeReader<IndexInfo> {

        public MetaIndexNodeReader(String nameNode) {
            super(nameNode, IndexInfo.class);
        }

        @Override
        protected IndexInfo readObject(JsonNode node, String fieldName) throws IOException, JsonParseException,
                JsonMappingException {
            IndexInfo f;
            f = mapper.readValue(node, IndexInfo.class);

            if (f.getIndexName() == null) {
                f.setIndexName(fieldName);
            }
            return f;
        }

    }

    public static class MetaIndexDeserializer extends JsonDeserializer<Collection<IndexInfo>> {

        @Override
        public Collection<IndexInfo> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            MetaIndexNodeReader nodeReader = new MetaIndexNodeReader("indexName");
            return nodeReader.deserialze(parser, ctxt);
        }

    }

    public static class MetaIndexSerializer extends JsonSerializer<Collection<IndexInfo>> {

        @Override
        public void serialize(Collection<IndexInfo> values, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            Map<String, IndexInfo> target = new HashMap<String, IndexInfo>();

            for (IndexInfo v : values) {
                if (!v.isInternal()) {
                    target.put(v.getIndexName(), v);
                }
            }
            jgen.writeObject(target);
        }
    }

}
