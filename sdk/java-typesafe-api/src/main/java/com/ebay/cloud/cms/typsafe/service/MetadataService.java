/**
 * www.iCloudObject.com
 * 
 */
package com.ebay.cloud.cms.typsafe.service;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;
import com.ebay.cloud.cms.typsafe.metadata.model.Repository;
import com.ebay.cloud.cms.typsafe.restful.DefaultResponseProccessor;
import com.ebay.cloud.cms.typsafe.restful.MetaResponseProcessor;
import com.ebay.cloud.cms.typsafe.restful.MetadataJsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.RepositoryJsonBuilder;
import com.ebay.cloud.cms.typsafe.restful.RepositoryProcessor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor;
import com.ebay.cloud.cms.typsafe.restful.RestExecutor.HttpRequest;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder;
import com.ebay.cloud.cms.typsafe.restful.URLBuilder.Url;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author liasu
 * 
 */
public class MetadataService {
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private final CMSClientConfig config;
    private final CMSClientService clientService;
    private final LoadingCache<String, MetaClass> metadatas;
    private final LoadingCache<String, Repository> repositories;

    private static class NotFound extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    public MetadataService(CMSClientService service) {
        this.clientService = service;
        this.config = this.clientService.getClientConfig();
        this.metadatas = CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(30, TimeUnit.MINUTES)
                .build(new CacheLoader<String, MetaClass>() {
                    @Override
                    public MetaClass load(String key) throws Exception {
                        MetaClass meta = _getMetadata(key, null);
                        if (meta == null) {
                            throw new NotFound();
                        }
                        return meta;
                    }
                });
        this.repositories = CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(30, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Repository>() {
                    @Override
                    public Repository load(String key) throws Exception {
                        Repository repo =  _getRepository(key, null);
                        if (repo == null) {
                            throw new NotFound();
                        }
                        return repo;
                    }
                });
    }

    public Map<String, MetaClass> getMetadatas(CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        URLBuilder builder = new URLBuilder(config, Url.METAS_URL, new String[] { config.getRepository() });
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        queryParams.put("mode", "ShowAll");
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.GET, null,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), context);
        MetaResponseProcessor metaProcess = new MetaResponseProcessor(executor.build(), HttpRequest.GET, context);
        MetadataManager mm = metaProcess.getMetaManager();
        return mm.getMetadatas();
    }

    MetaClass _getMetadata(String metadata, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        Preconditions.checkArgument(StringUtils.isNotEmpty(metadata), "metadata could not be empty!");
        URLBuilder builder = new URLBuilder(config, Url.META_URL, new String[] { config.getRepository(), metadata });
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        queryParams.put("mode", "ShowAll");
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.GET, null,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), context);
        MetaResponseProcessor metaProcess = new MetaResponseProcessor(executor.build(), HttpRequest.GET, context);
        return metaProcess.getMetaManager().getMetadatas().get(metadata);
    }

    public MetaClass getMetadata(String fromType, CMSClientContext context) {
        MetaClass meta = null;
        try {
            meta = this.metadatas.get(fromType);
        } catch (Exception e) {
            if (e.getCause() instanceof NotFound) {
                return null;
            } else if (e.getCause() instanceof CMSClientException) {
                throw (CMSClientException) e.getCause();
            }
            String msg = MessageFormat.format("Unable to find metaclass {0}!", fromType);
            logger.error(msg);
            throw new CMSClientException(msg, e);
        }
        return meta;
    }

    public void createMetadatas(List<MetaClass> metas, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        Preconditions.checkArgument(metas != null && metas.size() > 0, "metadata could not be empty!");
        
        URLBuilder builder = new URLBuilder(config, Url.METAS_URL, new String[] { config.getRepository() });
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.POST, metas,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), new MetadataJsonBuilder(), context);
        new DefaultResponseProccessor(executor.build(), HttpRequest.POST, context);
    }

    public void updateMetadata(MetaClass meta, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        Preconditions.checkArgument(meta != null, "metadata could not be empty!");
        Preconditions.checkArgument(StringUtils.isNotBlank(meta.getName()) , "metadata could not be empty!");

        URLBuilder builder = new URLBuilder(config, Url.META_URL, new String[] { config.getRepository(), meta.getName() });
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.POST, meta,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), new MetadataJsonBuilder(), context);
        new DefaultResponseProccessor(executor.build(), HttpRequest.POST, context);
    }

    public void createRepository(Repository repo, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        Preconditions.checkArgument(repo != null, "repo could not be null!");
        URLBuilder builder = new URLBuilder(config, Url.REPOSITORIES_URL, null);
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.POST, repo,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), new RepositoryJsonBuilder(), context);
        new DefaultResponseProccessor(executor.build(), HttpRequest.POST, context);
    }

    public List<Repository> getRepositories(CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        URLBuilder builder = new URLBuilder(config, Url.REPOSITORIES_URL, null);
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.GET, null,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), context);
        RepositoryProcessor repoProcess = new RepositoryProcessor(executor.build(), HttpRequest.GET, context);
        return repoProcess.getResultRepos();
    }

    public Repository getRepository(String reponame, CMSClientContext context) {
        Repository repo = null;
        try {
            repo = this.repositories.get(reponame);
        } catch (Exception e) {
            if (e.getCause() instanceof NotFound) {
                return null;
            } else if (e.getCause() instanceof CMSClientException) {
                throw (CMSClientException) e.getCause();
            }
            String msg = MessageFormat.format("Unable to find repository {0}!", reponame);
            logger.error(msg);
            throw new CMSClientException(msg, e);
        }
        return repo;
    }

    Repository _getRepository(String reponame, CMSClientContext context) {
        context = context != null ? context : new CMSClientContext();

        clientService.checkLiveness();
        Preconditions.checkArgument(StringUtils.isNotEmpty(reponame), "reponame could not be empty!");
        URLBuilder builder = new URLBuilder(config, Url.REPOSITORY_URL, reponame);
        Map<String, String> queryParams = clientService.getQueryParameter(context);
        RestExecutor executor = new RestExecutor(config, clientService.getClient(), HttpRequest.GET, null,
                builder.buildCanonicalPath(), queryParams, clientService.getHeader(context), context);
        RepositoryProcessor repoProcess = new RepositoryProcessor(executor.build(), HttpRequest.GET, context);
        List<Repository> repos = repoProcess.getResultRepos();
        if (repos.size() > 0) {
            return repos.get(0);
        } else {
            return null;
        }
    }

    public void updateRepositoryOption(Repository repo, CMSClientContext context) {
        // not supported
    }

    public void deleteRepository(CMSClientContext context) {
        // not supported in service yet.
    }
}
