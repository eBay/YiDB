/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.ebay.cloud.cms.typsafe.entity.AbstractCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.internal.CMSEntityMapper;
import com.ebay.cloud.cms.typsafe.entity.internal.JsonCMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.ebay.cloud.cms.typsafe.service.CMSClientContext;
import com.sun.jersey.api.client.ClientResponse;

/**
 * @author liasu
 * 
 */
public class EntityResponseProcessor<T> extends AbstractResponseProcessor {

    private final Class<T>        meta;
    private final List<T>         result;
    private final JsonNode        rootNode;
//    private final boolean         isGeneric;
    private final CMSEntityMapper.ProcessModeEnum mode;
    private final CMSClientConfig config;

    public EntityResponseProcessor(Class<T> amc, ClientResponse resp, CMSClientConfig config, HttpRequest request,
            CMSClientContext context) {
        super(resp, request, context);
        this.config = config;
        this.meta = amc;
        this.mode = getMode(amc);
        this.rootNode = readJson(jsonResponse);
        this.result = new ArrayList<T>();

        parseResponseHeader();
        parseReponseBody();
    }

    private CMSEntityMapper.ProcessModeEnum getMode(Class<T> amc) {
        if (GenericCMSEntity.class == amc) {
            return CMSEntityMapper.ProcessModeEnum.GENERIC;
        } else if (ICMSEntity.class == amc) {
            return CMSEntityMapper.ProcessModeEnum.DYNAMIC;
        } else {
            return CMSEntityMapper.ProcessModeEnum.TYPE_SAFE;
        }
    }

    EntityResponseProcessor(Class<T> amc, ClientResponse resp, String jsonString, JsonNode rNode, CMSClientConfig config,
            HttpRequest request, CMSClientContext context) {
        super(resp, jsonString, request, context);
        this.config = config;
        this.meta = amc;
        this.mode = getMode(amc);
        this.rootNode = rNode;
        this.result = new ArrayList<T>();

        parseResponseHeader();
        parseReponseBody();
    }

    private void parseReponseBody() {
        JsonNode resultNode = rootNode.get(RESULT);
        if (resultNode != null && resultNode.isArray()) {
            ArrayNode resultArray = (ArrayNode) resultNode;
            for (JsonNode node : resultArray) {
                T o = parseResponseObject(node);
                result.add(o);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation"})
    private T parseResponseObject(JsonNode node) {
        T o = null;
        if (!ICMSEntity.class.isAssignableFrom(meta)) {
            try {
                o = objectMapper.readValue(node, meta);
            } catch (Exception e) {
                throw new CMSClientException(response.getStatus(), MessageFormat.format(
                        "Parsing reponse result node error, walformed node string {0}, target meta class name {1}",
                        node.getValueAsText(), meta.getName()), e, jsonResponse, requestInfo);
            }
            return o;
        }

        JsonCMSEntity cmsJsonCMSEntity = new JsonCMSEntity((ObjectNode) node, (Class<? extends ICMSEntity>) meta);
        CMSEntityMapper mapper = new CMSEntityMapper((ObjectNode) node, config, meta, mode, meta);
        cmsJsonCMSEntity.traverse(mapper);
        o = (T) mapper.getTargetEntity();
        if (o instanceof AbstractCMSEntity) {
            ((AbstractCMSEntity) o).set_repo(config.getRepository());
            ((AbstractCMSEntity) o).set_branch(config.getBranch());
        }
        return o;
    }
    
    protected final JsonNode getRootNode() {
        return rootNode;
    }

    public List<T> getBuildEntity() {
        return result;
    }

}
