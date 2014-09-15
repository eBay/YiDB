/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import com.ebay.cloud.cms.typsafe.entity.AbstractCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.internal.CMSEntityMapper;
import com.ebay.cloud.cms.typsafe.entity.internal.CMSEntityMapper.ProcessModeEnum;
import com.ebay.cloud.cms.typsafe.entity.internal.JsonCMSEntity;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;

/**
 * @author liasu
 * 
 */
public class JsonBuilder implements IJsonBuilder {

    protected final CMSClientConfig config;

    JsonBuilder(CMSClientConfig config) {
        this.config = config;
    }

    public String buildJson(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        JsonNode node = buildJsonNode(object);
        return node.toString();
    }

    @SuppressWarnings("unchecked")
    protected JsonNode buildJsonNode(Object object) {
        if (object instanceof List) {
            List<AbstractCMSEntity> lists = (List<AbstractCMSEntity>) object;
            ArrayNode an = JsonNodeFactory.instance.arrayNode();
            for (AbstractCMSEntity entity : lists) {
                CMSEntityMapper mapper = new CMSEntityMapper(null, config, JsonCMSEntity.class, ProcessModeEnum.JSON, entity.getClass(), true);
                entity.traverse(mapper);
                JsonCMSEntity jsonEntity = (JsonCMSEntity) mapper.getTargetEntity();
                an.add(jsonEntity.getNode());
            }
            return an;
        } else {
            AbstractCMSEntity entity = (AbstractCMSEntity) object;
            CMSEntityMapper mapper = new CMSEntityMapper(null, config, JsonCMSEntity.class, ProcessModeEnum.JSON, entity.getClass(), true);
            entity.traverse(mapper);
            JsonCMSEntity jsonEntity = (JsonCMSEntity) mapper.getTargetEntity();
            return jsonEntity.getNode();
        }
    }
}
