/**
 * 
 */
package com.ebay.cloud.cms.typsafe.metadata.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.ConsistencyTypeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaRelationship.RelationTypeEnum;

/**
 * @author liasu
 * 
 */
@SuppressWarnings("deprecation")
public class MetadataManager {

    private Map<String, MetaClass> metadatas = new HashMap<String, MetaClass>();
    private static final ObjectMapper OM = new ObjectMapper();
    private static final List<String> STATUS_VALUES = Collections.unmodifiableList(Arrays.asList("active", "deleted"));
    private static final Logger logger = LoggerFactory.getLogger(MetadataManager.class);

    public Collection<String> getMetadataNames() {
        return metadatas.keySet();
    }

    public MetaClass getMetadata(String metaName) {
        return metadatas.get(metaName);
    }

    public Map<String, MetaClass> getMetadatas() {
        return Collections.unmodifiableMap(metadatas);
    }

    /**
     * For some projects, jackson must be 1.7, which means some configuration
     * like null handling in jackson won't have a consistent way to set in
     * jackson. Choose to manually load the json.
     */
    public static MetadataManager load(JsonNode node) {
        MetadataManager mm = new MetadataManager();
        List<MetaRelationship> embedReference = new ArrayList<MetaRelationship>();
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (JsonNode metaNode : array) {
                MetaClass meta = loadMetadata(metaNode, embedReference);
                mm.metadatas.put(meta.getName(), meta);
            }
            for (MetaRelationship mr : embedReference) {
                String relName = mr.getRefDataType();
                mr.setRefDataType(mm.resolveEmbedReferenceName(relName));
            }
        }
        return mm;
    }

    public static MetadataManager load(String allMetaInOne) {
        try {
            JsonNode node = OM.readTree(allMetaInOne);
            if (!node.isArray() && node.isObject()) {
                // wrapper the object node
                ArrayNode an = JsonNodeFactory.instance.arrayNode();
                an.add(node);
                node = an;
            }
            return load(node);
        } catch (Exception e) {
            throw new CMSEntityException(String.format("fail to parse meta json: %s", allMetaInOne), e);
        }
    }

    //
    // Find the last embed name inside embed reference
    //
    public String resolveEmbedReferenceName(String relName) {
        String[] relTypes = relName.split("\\.");
        if (relTypes.length == 1) {
            return relTypes[0];
        }
        MetaClass rootMc = getMetadata(relTypes[0]);
        MetaClass lastMc = rootMc;
        for (int i = 1; i < relTypes.length; i++) {
            MetaRelationship mr = (MetaRelationship) lastMc.getField(relTypes[i]);
            String name = resolveEmbedReferenceName(mr.getRefDataType());
            lastMc = getMetadata(name);
        }
        return lastMc.getName();
    }

    private static MetaClass loadMetadata(JsonNode metaNode, List<MetaRelationship> embedReference) {
        MetaClass meta = new MetaClass();
        meta.setName(getStringField(metaNode, "name"));
        meta.setPluralName(getStringField(metaNode, "pluralName"));
        meta.setParent(getStringField(metaNode, "parent"));
        meta.setRepository(getStringField(metaNode, "repository"));
        meta.set_id(getStringField(metaNode, "id"));
        meta.setEmbed(getBooleanField(metaNode, "embed"));
        meta.setInner(getBooleanField(metaNode, "inner"));
        meta.setDescription(getStringField(metaNode, "description"));
        meta.setAllowFullTableScan(getBooleanField(metaNode, "allowFullTableScan"));

        JsonNode fieldNodes = metaNode.get("fields");
        Iterator<String> it = fieldNodes.getFieldNames();
        while (it.hasNext()) {
            String fName = it.next();
            MetaField field = loadMetafield(fName, fieldNodes.get(fName), embedReference);
            meta.addField(field);
        }

        // internal attributes
        meta.addField(createInternalAttribute("_oid", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_type", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_branch", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_version", DataTypeEnum.INTEGER));
        meta.addField(createInternalAttribute("_createTime", DataTypeEnum.DATE));
        meta.addField(createInternalAttribute("_lastmodified", DataTypeEnum.DATE));
        MetaAttribute statusAttr = createInternalAttribute("_status", DataTypeEnum.ENUM);
        statusAttr.setEnumValues(STATUS_VALUES);
        meta.addField(statusAttr);
        meta.addField(createInternalAttribute("_pversion", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_comment", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_user", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_modifier", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_creator", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_hostentity", DataTypeEnum.STRING));
        meta.addField(createInternalAttribute("_shardkey", DataTypeEnum.STRING));
        
        // indexes
        JsonNode optionNode = metaNode.get("options");
        addOptions(optionNode, meta);
        
        return meta;
    }
    
    private static MetaAttribute createInternalAttribute(String name, DataTypeEnum dataType) {
        MetaAttribute attr = new MetaAttribute();
        attr.setInternal(true);
        attr.setName(name);
        attr.setCardinality(CardinalityEnum.One);
        attr.setConstant(false);
        attr.setDataType(dataType);
        return attr;
    }

    private static MetaField loadMetafield(String fName, JsonNode node, List<MetaRelationship> embedReference) {
        String dt = getStringField(node, "dataType");
        DataTypeEnum dataType = DataTypeEnum.fromString(dt);
        MetaField field = null;
        if (dataType == DataTypeEnum.RELATIONSHIP) {
            field = new MetaRelationship();
            loadRelationship((MetaRelationship) field, node, embedReference);
        } else {
            field = new MetaAttribute();
            loadAttribute((MetaAttribute) field, node);
        }
        field.setName(fName);
        field.setConstant(getBooleanField(node, "constant"));
        field.setDescription(getStringField(node, "description"));
        field.setMandatory(getBooleanField(node, "mandatory"));
        String car = getStringField(node, "cardinality");
        field.setCardinality(CardinalityEnum.fromString(car));
        field.setDataType(dataType);

        return field;
    }

    private static void loadAttribute(MetaAttribute field, JsonNode node) {
        field.setDefaultValue(getStringField(node, "defaultValue"));
        field.setExpression(getStringField(node, "expression"));
        field.setValidation(getStringField(node, "validation"));
        ArrayNode enumArray = (ArrayNode) node.get("enumValues");
        if (enumArray != null) {
            for (JsonNode valNode : enumArray) {
                field.addEnumValue(valNode.getValueAsText());
            }
        }
    }

    private static void loadRelationship(MetaRelationship field, JsonNode node, List<MetaRelationship> embedReference) {
        String refDataType = getStringField(node, "refDataType");
        field.setRefDataType(refDataType);
        if (isEmbedReference(refDataType)) {
            embedReference.add(field);
        }
        field.setSrcDataType(getStringField(node, "srcDataType"));
        field.setRefRepository(getStringField(node, "refRepository"));
        field.setCascade(getBooleanField(node, "cascade"));
        String relType = getStringField(node, "relationType");
        if (relType != null) {
            field.setRelationType(RelationTypeEnum.fromString(relType));
        }
        String consistType = getStringField(node, "consistencyType");
        if (consistType != null) {
            field.setConsistencyType(ConsistencyTypeEnum.fromString(consistType));
        }
    }

    private static boolean isEmbedReference(String refDataType) {
        return refDataType.contains(".");
    }
    
    private static void addOptions(JsonNode optionNode, MetaClass meta) {
        if (optionNode == null) {
            return;
        }
        ObjectNode indexNode = (ObjectNode) optionNode.get("indexes");
        if (indexNode == null) {
            return;
        }
        Map<String, IndexInfo> indexes = meta.getOptions().getIndexes();

        Iterator<Entry<String, JsonNode>> it = indexNode.getFields();
        while (it.hasNext()) {
            Entry<String, JsonNode> indexEntry = it.next();
            ObjectNode iNode = (ObjectNode) indexEntry.getValue();
            try {
                IndexInfo info = OM.readValue(iNode, IndexInfo.class);
                indexes.put(indexEntry.getKey(), info);
            } catch (Exception e) {
                logger.error(MessageFormat.format("load index failed! This is ignored. The payload of index is {0}", iNode), e);
            }
        }
    }
    
    private static String getStringField(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).getValueAsText();
        } else {
            return null;
        }
    }

    private static Boolean getBooleanField(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).getValueAsBoolean();
        } else {
            return false;
        }
    }

}
