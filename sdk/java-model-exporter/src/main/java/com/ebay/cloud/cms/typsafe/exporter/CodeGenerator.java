/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exporter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.typsafe.exception.CMSModelException;
import com.ebay.cloud.cms.typsafe.exporter.EntityGenerator.EntityInformation;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author liasu
 * 
 */
public class CodeGenerator {

    private final static ObjectMapper mapper                 = new ObjectMapper();
    private final static Logger       logger                 = LoggerFactory.getLogger(CodeGenerator.class);

    public final static String        DEFAULT_PACKAGE_PREFIX = "com.ebay.cloud.cms.model";

    private final String              outputDirectory;
    private final String              packagePrefix;
    private final MetaJsonReader      metaReader;

    public CodeGenerator(MetaJsonReader reader, String outputDirectory, String packagePrefix) {
        this.metaReader = reader;
        if (packagePrefix != null) {
            this.packagePrefix = packagePrefix;
        } else {
            this.packagePrefix = DEFAULT_PACKAGE_PREFIX;
        }

        if (outputDirectory.endsWith("/")) {
            this.outputDirectory = outputDirectory.substring(0, outputDirectory.length() - 1);
        } else {
            this.outputDirectory = outputDirectory;
        }
    }

    public void generate() {
        JsonNode metaJsons = metaReader.getMetaJson();

        if (metaJsons.isArray()) {
            MetadataManager metaManager = MetadataManager.load(metaJsons);
            for (JsonNode metaNode : metaJsons) {
                EntityGenerator entityGenerator = new EntityGenerator(metaNode, packagePrefix, metaManager);
                entityGenerator.build();

                output(entityGenerator.getEntity());
            }
        }
    }

    private void output(EntityInformation entityInfo) {
        String[] pathSegments = StringUtils.split(entityInfo.packageName, ("."));

        StringBuilder sb = new StringBuilder(outputDirectory);
        for (String seg : pathSegments) {
            sb.append("/").append(seg);
        }
        sb.append("/").append(entityInfo.className).append(".java");

        try {
            File file = new File(sb.toString());
            Files.createParentDirs(file);
            Files.write(entityInfo.content, file, Charset.defaultCharset());
            logger.debug(String.format("write files to %s", sb.toString()));
        } catch (IOException e) {
            logger.error("failed to write the output", e);
            throw new CMSModelException("failed to write the output", e);
        }
    }

    public static interface MetaJsonReader {
        JsonNode getMetaJson();
    }

    public static class FileMetaJsonReader implements MetaJsonReader {
        
        private ArrayNode metaNode;

        public FileMetaJsonReader(String path) {
            metaNode = JsonNodeFactory.instance.arrayNode();
            // if the given file path only contains package, will read from the
            // class path
            boolean jarRead = path.contains(".") && !path.contains("/");
            try {
                if (jarRead) {
                    // path is the package inside some jar
                    readJarJson(path);
                } else {
                    readJsonFiles(path);
                }
            } catch (Exception e) {
                throw new CMSModelException("unable to get meta json", e);
            }
        }

        private void readJarJson(String packagePath) throws Exception {
            packagePath = packagePath.replace('.', '/');

            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(packagePath);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                // ZIP file doesn't support file:/ prefix
                String[] segments = url.getFile().split("!");
                String jarPath = segments[0];
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring("file:".length());
                }

                File file = new File(jarPath);
                if (!file.exists() || !jarPath.endsWith(".jar")) {
                    logger.info(MessageFormat.format("Ignore reading json from extracted {0} from full path {1}",
                            jarPath, url.getFile()));
                    continue;
                }
                logger.debug("Reading from jar:" + jarPath);

                // read all the json files inside the jar file; filter out the
                // file based on prefix and suffix
                ZipFile zf = new ZipFile(file);
                try {
                    Enumeration<? extends ZipEntry> zips = zf.entries();
                    while (zips.hasMoreElements()) {
                        String zipEntryName = zips.nextElement().getName();
                        if (zipEntryName.contains(packagePath) && zipEntryName.endsWith(".json")) {
                            logger.info("Reading inside jar json:" + zipEntryName);
                            URL jsonUrl = Thread.currentThread().getContextClassLoader().getResource(zipEntryName);
                            JsonNode fileNode = mapper.readTree(jsonUrl);
                            addJsonNode(fileNode);
                        }
                    }
                } finally {
                    try {
                        zf.close();
                    } catch (Exception e) {
                        logger.error("unable to close the zip file:" + zf.getName(), e);
                    }
                }
            }
            
            Preconditions.checkState(metaNode.size() > 0, "No meta loaded for generation!");
        }

        private void addJsonNode(JsonNode fileNode) {
            if (fileNode.isArray()) {
                ArrayNode an = (ArrayNode) fileNode;
                metaNode.addAll(an);
            } else {
                metaNode.add(fileNode);
            }
        }

        private void readJsonFiles(String filePath) throws Exception {
            List<File> filePaths = new ArrayList<File>();
            // find all json files
            File file = new File(filePath);
            if (file.isDirectory()) {
                addJsonFiles(filePaths, file); 
            } else if (file.isFile()) {
                filePaths.add(file);
            }

            Preconditions.checkState(filePaths.size() > 0, "No .json files found for generation!");
            // now read
            for (File f : filePaths) {
                JsonNode fNode = mapper.readTree(f);
                addJsonNode(fNode);
            }
        }
        // avoid introduce new jar dependency, write this ourself. FileUtils could do the same thing.
        private void addJsonFiles(List<File> filePaths, File file) {
            File[] files = file.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.endsWith(".json")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        addJsonFiles(filePaths, f);
                    } else {
                        filePaths.add(f);
                    }
                }
            }
        }

        @Override
        public JsonNode getMetaJson() {
            return metaNode;
        }

    }

    public static class OnlineMetaJsonReader implements MetaJsonReader {
        String                      baseUrl;
        String                      repo;
        String                      token;
        private final Client        restClient = new Client();
        private final static String META_URL   = "{0}/repositories/{1}/metadata";

        public OnlineMetaJsonReader(String cmsBaseUrl, String repo, String token) {
            this.baseUrl = cmsBaseUrl;
            this.repo = repo;
            this.token = token;
        }

        @Override
        public JsonNode getMetaJson() {
            String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            WebResource resource = restClient.resource(MessageFormat.format(META_URL, url, repo));
            resource.header("Authorization", token);

            ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            String jsonResponse = response.getEntity(String.class);

            try {
                JsonNode rootNode = mapper.readTree(jsonResponse);
                JsonNode resultNode = rootNode.get("result");
                if (resultNode == null) {
                    throw new Exception(MessageFormat.format("No metadata in json \"result\". The json response is {0}!", jsonResponse));
                }
                return resultNode;
            } catch (Exception e) {
                throw new CMSModelException("unable to get meta json", e);
            }
        }
    }
}
