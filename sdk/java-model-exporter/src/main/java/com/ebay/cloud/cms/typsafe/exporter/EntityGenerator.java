/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exporter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSModelException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

/**
 * @author gowang
 * @author liasu
 * 
 */
@SuppressWarnings("deprecation")
public class EntityGenerator {

    private static final Logger        logger         = LoggerFactory.getLogger(EntityGenerator.class);

    private static final Map<Character, String> CHAR_NAMES;
    private static final Configuration configuration;
    static {
        configuration = new Configuration();
        configuration.setClassForTemplateLoading(EntityGenerator.class, "/");
        configuration.setObjectWrapper(new DefaultObjectWrapper());
        // Set the first letter capitalized
        configuration.setSharedVariable("upperFC", new UpperFirstCharacter());

        CHAR_NAMES = new HashMap<Character, String>();
        CHAR_NAMES.put('+', "_PLUS_");
        CHAR_NAMES.put('-', "_MINUS_");
    }

    private final String               prefix;
    private final String               metaJson;
    private final JsonNode             metaNode;

    private boolean                    hasList        = false;
    private boolean                    hasDate        = false;
    private boolean                    hasJsonObject = false;

    private Map<String, Object>        parameterMap;

    private EntityInformation          output;
    private MetadataManager            metaManager;

    public enum PrimitiveType {
        Integer("integer", "Integer"),

        Long("long", "Long"),

        String("string", "String"),

        Json("json", "JsonNode"),

        Double("double", "Double"),

        Boolean("boolean", "Boolean"),

        Date("date", "Date"); // java.util
        // enum
        // relationship

        private final String metaTypeName;
        private final String javaTypeName;

        public String getMetaTypeName() {
            return metaTypeName;
        }

        public String getJavaTypeName() {
            return javaTypeName;
        }

        private PrimitiveType(String metaName, String javaName) {
            this.metaTypeName = metaName;
            this.javaTypeName = javaName;
        }

        public static PrimitiveType fromMetaTypeName(String metaName) {
            for (PrimitiveType mapping : PrimitiveType.values()) {
                if (mapping.getMetaTypeName().equals(metaName)) {
                    return mapping;
                }
            }
            return null;
        }

    }

    public static class EntityInformation {
        final String packageName;
        final String className;
        final String content;

        EntityInformation(String pack, String clzName, String clzContent) {
            packageName = pack;
            className = clzName;
            content = clzContent;
        }
    }

    EntityGenerator(String meta, String pkgPrefix, MetadataManager metaManager) {
        this.metaJson = meta;
        this.metaNode = parseJson();
        this.parameterMap = new HashMap<String, Object>();
        this.prefix = pkgPrefix;
        this.metaManager = metaManager;
    }

    EntityGenerator(JsonNode metaNode, String pkgPrefix, MetadataManager metaManager) {
        this.metaNode = metaNode;
        this.metaJson = metaNode.toString();
        this.parameterMap = new HashMap<String, Object>();
        this.prefix = pkgPrefix;
        this.metaManager = metaManager;
    }

    private final JsonNode parseJson() {
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readTree(metaJson);
        } catch (Exception e) {
            throw new CMSModelException(String.format("fail to parse meta json: %s", metaJson), e);
        }
    }

    public void build() {

        // prepare parameters' map
        processMetadata(metaNode);

        // apply to freemarker template
        String content = analysisTemplate();

        output = new EntityInformation((String) parameterMap.get("entityPackage"),
                (String) parameterMap.get("className"), content);
    }

    public EntityInformation getEntity() {
        return output;
    }

    private void processMetadata(JsonNode rootNode) {

        // 1. entity name and package name; inheritance information
        JsonNode entityName = rootNode.get("name");
        JsonNode repoName = rootNode.get("repository");
        if (repoName == null) {
            parameterMap.put("repoName", "");
        } else {
            parameterMap.put("repoName", repoName.getValueAsText());
        }
        parameterMap.put("jacksonPackage", "org.codehaus.jackson.annotate.JsonIgnore");

        // JIRA-2034 : don't need to add repository name prefix, let user fully specify the package
        parameterMap.put("entityPackage", prefix /*+ "." + convertToJavaIdentifier(repoName.asText())*/);

        if (rootNode.get("parent") != null && !rootNode.get("parent").isNull()) {
            parameterMap.put("inherit", rootNode.get("parent").getValueAsText());
        } else {
            parameterMap.put("inherit", "GenericCMSEntity");
            parameterMap.put("inheritParent", "com.ebay.cloud.cms.typsafe.entity");
        }
        parameterMap.put("className", entityName.getValueAsText());

        // 2. entity fields
        JsonNode fieldNode = rootNode.get("fields");
        Iterator<String> fieldsItetator = fieldNode.getFieldNames();

        List<Object> fieldProps = new ArrayList<Object>();
        while (fieldsItetator.hasNext()) {
            String fieldName = fieldsItetator.next();
            String metaTypeName = fieldNode.get(fieldName).get("dataType").getValueAsText();

            JsonNode cardinalityNode = fieldNode.get(fieldName).get("cardinality");
            // global setting of list
            hasList |= (cardinalityNode != null && cardinalityNode.getValueAsText().equals("Many"));

            PrimitiveType typeMapping = PrimitiveType.fromMetaTypeName(metaTypeName);
            if (typeMapping != null) {
                fieldProps.add(processPrimitveField(fieldNode, fieldName, typeMapping));
            } else if (metaTypeName.equals("enumeration")) {
                fieldProps.add(processEnumField(fieldNode, fieldName));
            } else if (metaTypeName.equals("relationship")) {
                fieldProps.add(processReferenceField(fieldNode, fieldName));
            } else {
                throw new CMSModelException("Unknown metafield type" + metaTypeName, null);
            }
        }

        // additional global setting based on parse result
        if (hasList) {
            parameterMap.put("listPackage", "java.util.List");
        } else {
            parameterMap.put("listPackage", "");
        }
        if (hasDate) {
            parameterMap.put("datePackage", "java.util.Date");
        } else {
            parameterMap.put("datePackage", "");
        }
        if (hasJsonObject) {
            parameterMap.put("jsonPackage", "org.codehaus.jackson.JsonNode");
        } else {
            parameterMap.put("jsonPackage", "");
        }

        parameterMap.put("properties", fieldProps);
    }

    private Map<String, Object> processEnumField(JsonNode fieldNode, String fieldName) {
        Map<String, Object> enumProp = new HashMap<String, Object>();
        enumProp.put("propType", "enumeration");
        enumProp.put("propName", fieldName);

        JsonNode cardinalityNode = fieldNode.get(fieldName).get("cardinality");
        enumProp.put("propCardinality", cardinalityNode == null ? "One" : cardinalityNode.getValueAsText());

        int enumLength = fieldNode.get(fieldName).get("enumValues").size();
        List<Object> enumList = new ArrayList<Object>();
        for (int i = 0; i < enumLength; i++) {
            Map<String, Object> enumMap = new HashMap<String, Object>();
            enumMap.put("enumElementValue", fieldNode.get(fieldName).get("enumValues").get(i).getValueAsText());
            String enumVal = convertToJavaIdentifier(fieldNode.get(fieldName).get("enumValues").get(i).getValueAsText());
            enumMap.put("enumElementName", enumVal);
            enumList.add(enumMap);
        }
        enumProp.put("enumLength", enumLength);
        enumProp.put("enumList", enumList);
        return enumProp;
    }

    private Map<String, Object> processPrimitveField(JsonNode fieldNode, String fieldName, PrimitiveType mapping) {
        Map<String, Object> primitiveProp = new HashMap<String, Object>();
        primitiveProp.put("propType", mapping.getJavaTypeName());
        primitiveProp.put("propName", fieldName);
        // json type doesn't care the cardinality option
        JsonNode cardinalityNode = fieldNode.get(fieldName).get("cardinality");
        if (mapping != PrimitiveType.Json && cardinalityNode != null) {
            primitiveProp.put("propCardinality", cardinalityNode.getValueAsText());
        } else {
            primitiveProp.put("propCardinality", "One");
        }
        if (mapping == PrimitiveType.Date) {
            hasDate = true;
        } else if (mapping == PrimitiveType.Json) {
            hasJsonObject = true;
        }
        return primitiveProp;
    }

    private Map<String, Object> processReferenceField(JsonNode fieldNode, String fieldName) {
        JsonNode currentFieldNode = fieldNode.get(fieldName);
        Map<String, Object> relationProp = new HashMap<String, Object>();
        relationProp.put("propType", "relationship");
        relationProp.put("propName", fieldName);
        JsonNode cardinalityNode = currentFieldNode.get("cardinality");
        relationProp.put("propCardinality", cardinalityNode == null ? "One" : cardinalityNode.getValueAsText());
        String refTypeName;
        JsonNode relationNode = currentFieldNode.get("relationType");
        if (relationNode != null && "CrossRepository".equals(relationNode.getValueAsText())) {
            // for cross repository, generate a generic entity in the interface
            refTypeName = "GenericCMSEntity";
        } else {
            String refDataType = currentFieldNode.get("refDataType").getValueAsText();
            refTypeName = metaManager.resolveEmbedReferenceName(refDataType);
        }
        relationProp.put("propRefName", refTypeName);
        return relationProp;
    }

    private String convertToJavaIdentifier(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char charAt = name.charAt(i);
            if (!Character.isJavaIdentifierPart(charAt)) {
                // special handling : their would be "UTC+8" and "UTC-8" in enum value for timezone. These two values
                // would be converted the same enum, which causes a compilation error. To resolve the error, a "100 percent correct" solution would be converted to 
                // ascii code. But that hurts the readability of the generated code. Would be define some dictionary of (special char -> english abbr).
                if (CHAR_NAMES.containsKey(charAt)) {
                    sb.append(CHAR_NAMES.get(charAt));
                } else {
                    sb.append('_');
                }
            } else {
                sb.append(charAt);
            }
        }
        return sb.toString();
    }
    

    private String analysisTemplate() {
        Writer outputWriter = null;
        try {
            Template template = configuration.getTemplate("CMSEntityTemplate.ftl", "utf8");
            outputWriter = new StringWriter();
            template.process(parameterMap, outputWriter);
            outputWriter.flush();
            return outputWriter.toString();
        } catch (Exception e) {
            throw new CMSModelException("error when analysis template", e);
        } finally {
            // close resource like files, streams...
            if (outputWriter != null) {
                try {
                    outputWriter.close();
                } catch (Exception e) {
                    logger.error("failed to close the string writer when analysis template", e);
                }
            }
        }
    }

}
