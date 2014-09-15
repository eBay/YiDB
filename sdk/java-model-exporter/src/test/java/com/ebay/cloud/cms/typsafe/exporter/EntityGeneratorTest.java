package com.ebay.cloud.cms.typsafe.exporter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.ebay.cloud.cms.typsafe.exporter.EntityGenerator.EntityInformation;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;
import com.google.common.io.Files;

/**
 * 
 * @author liasu
 * 
 */
public class EntityGeneratorTest {

    @Test
    public void generate() throws IOException {
        URL jsonUrl = EntityGeneratorTest.class.getResource("/ApplicationService.json.bak");
        String meta = Files.toString(new File(jsonUrl.getFile()), Charset.defaultCharset());
        MetadataManager mm = MetadataManager.load(meta);
        EntityGenerator eg = new EntityGenerator(meta, CodeGenerator.DEFAULT_PACKAGE_PREFIX, mm);
        eg.build();
        EntityInformation entityInfo = eg.getEntity();

        String[] pathSegments = StringUtils.split(entityInfo.packageName, ".");

        String folder = EntityGeneratorTest.class.getResource("/").getFile();

        StringBuilder sb = new StringBuilder(folder.substring(0, folder.length() - 1));
        for (String seg : pathSegments) {
            sb.append("/").append(seg);
        }
        sb.append("/").append(entityInfo.className).append(".java");

        File file = new File(sb.toString());
        Files.createParentDirs(file);
        Files.write(entityInfo.content, file, Charset.defaultCharset());
    }
}
