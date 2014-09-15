/**
 * 
 */
package com.ebay.cloud.cms.typsafe.restful;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.List;

import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;

/**
 * @author liasu
 * 
 */
public class URLBuilder {

    public static enum Url {
        ENTITY_DETAIL_URL("/repositories/{0}/branches/{1}/{2}/{3}") {
            @Override
            public String build(Object e) {
                ICMSEntity entity = (ICMSEntity) e;
                return MessageFormat.format(getUrlTempalte(), entity.get_repo(), entity.get_branch(),
                        entity.get_metaclass(), urlEncoding(entity.get_id()));
            }
        },
        ENTITY_FACTORY_URL("/repositories/{0}/branches/{1}/{2}") {
            @Override
            public String build(Object e) {
                if (e instanceof ICMSEntity) {
                    ICMSEntity entity = (ICMSEntity) e;
                    return MessageFormat.format(getUrlTempalte(), entity.get_repo(), entity.get_branch(),
                            entity.get_metaclass());
                }
                throw new IllegalArgumentException(MessageFormat.format(
                        "build url for entity factory url failed with argument :{0}", e));
            }
        },
        ENTITY_BATCH_URL("/repositories/{0}/branches/{1}/entities") {
            @SuppressWarnings("rawtypes")
            @Override
            public String build(Object e) {
                if (e instanceof List) {
                    ICMSEntity entity = (ICMSEntity) ((List) e).get(0);
                    return MessageFormat.format(getUrlTempalte(), entity.get_repo(), entity.get_branch());
                }
                throw new IllegalArgumentException(MessageFormat.format(
                        "build url for entity factory url failed with argument :{0}", e));
            }
        },
        QUERY_URL("/repositories/{0}/branches/{1}/query") {
            @Override
            public String build(Object e) {
                CMSQuery entity = (CMSQuery) e;
                return MessageFormat.format(getUrlTempalte(), entity.getRepository(), entity.getBranch());
            }
        },
        GET_QUERY_URL("/repositories/{0}/branches/{1}/query/{2}") {
            @Override
            public String build(Object e) {
                CMSQuery query = (CMSQuery) e;
                return MessageFormat.format(getUrlTempalte(), query.getRepository(), query.getBranch(), urlEncoding(query.getQueryString()));
            }
        },
        ENTITY_FIELD_URL("/repositories/{0}/branches/{1}/{2}/{3}/{4}") {
            @Override
            public String build(Object entity) {
                Object[] params = (Object[]) entity;
                ICMSEntity cmsEntity = (ICMSEntity)params[0];
                String fieldName = (String) params[1];
                return MessageFormat.format(getUrlTempalte(), cmsEntity.get_repo(), cmsEntity.get_branch(),
                        cmsEntity.get_metaclass(), cmsEntity.get_id(), fieldName);
            }
        },
        ENTITY_FIELD_PUSH_URL("/repositories/{0}/branches/{1}/{2}/{3}/{4}/actions/push") {
            @Override
            public String build(Object entity) {
                Object[] params = (Object[]) entity;
                ICMSEntity cmsEntity = (ICMSEntity)params[0];
                String fieldName = (String) params[1];
                return MessageFormat.format(getUrlTempalte(), cmsEntity.get_repo(), cmsEntity.get_branch(),
                        cmsEntity.get_metaclass(), cmsEntity.get_id(), fieldName);
            }
        },
        ENTITY_FIELD_PULL_URL("/repositories/{0}/branches/{1}/{2}/{3}/{4}/actions/pull") {
            @Override
            public String build(Object entity) {
                Object[] params = (Object[]) entity;
                ICMSEntity cmsEntity = (ICMSEntity)params[0];
                String fieldName = (String) params[1];
                return MessageFormat.format(getUrlTempalte(), cmsEntity.get_repo(), cmsEntity.get_branch(),
                        cmsEntity.get_metaclass(), cmsEntity.get_id(), fieldName);
            }
        },
        META_URL("/repositories/{0}/metadata/{1}") {
            @Override
            public String build(Object entity) {
                String[] param = (String[]) entity;
                return MessageFormat.format(getUrlTempalte(), param[0], param[1]);
            }
        },
        METAS_URL("/repositories/{0}/metadata/") {
            @Override
            public String build(Object entity) {
                String[] param = (String[]) entity;
                return MessageFormat.format(getUrlTempalte(), param[0]);
            }
        },
        TOKEN_URL("/validate/user/{0}/") {
            @Override
            public String build(Object entity) {
                String param = (String) entity;
                return MessageFormat.format(getUrlTempalte(), param);
            }
        },
        REPOSITORIES_URL("/repositories/") {
            @Override
            public String build(Object entity) {
                return getUrlTempalte();
            }
        },
        REPOSITORY_URL("/repositories/{0}") {
            @Override
            public String build(Object entity) {
                String repo = (String) entity;
                return MessageFormat.format(getUrlTempalte(), repo);
            }
        }
        ;

        public abstract String build(Object entity);

        private String urlTemplate;

        private Url(String baseUrl) {
            urlTemplate = baseUrl;
        }

        public String getUrlTempalte() {
            return urlTemplate;
        }

        String urlEncoding(String url) {
            try {
                return URLEncoder.encode(url, "utf8").replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException e) {
                throw new CMSClientException("error build request url.", e);
            }
        }
    };

    private final CMSClientConfig config;
    private final Object          entity;
    private final Url             url;

    public URLBuilder(CMSClientConfig config, Url url, Object entity) {
        this.config = config;
        this.entity = entity;
        this.url = url;
    }

    public String buildCanonicalPath() {
        return config.getServerBaseUrl() + url.build(entity);
    }
}
