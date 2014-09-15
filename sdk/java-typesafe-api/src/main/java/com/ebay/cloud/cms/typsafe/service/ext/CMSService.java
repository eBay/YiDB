
package com.ebay.cloud.cms.typsafe.service.ext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;
import com.ebay.cloud.cms.typsafe.service.CMSClientService;

/**
 * A facade service to provide access to cms service. Supporting metadata
 * operation and different repository
 * 
 * @author liasu
 * 
 */
public class CMSService {

    private String                        serviceEndpoint;

    private Map<String, CMSClientService> repoSeviceEntries = new ConcurrentHashMap<String, CMSClientService>();

    public CMSService(String baseUrl) {
        this.serviceEndpoint = baseUrl;
    }

    private CMSClientService createClientService(String repo, String codeGenPrefix) {
        CMSClientConfig config = new CMSClientConfig(serviceEndpoint, repo, "main", codeGenPrefix);
        CMSClientService clientService = CMSClientService.getClientService(config);
        return clientService;
    }

    public CMSClientService getClientService(String repo, String codeGenPrefix) {
        if (repoSeviceEntries.containsKey(repo)) {
            return repoSeviceEntries.get(repo);
        } else {
            repoSeviceEntries.put(repo, createClientService(repo, codeGenPrefix));
            return repoSeviceEntries.get(repo);
        }
    }

}
