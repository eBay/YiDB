/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exporter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.FileMetaJsonReader;
import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.MetaJsonReader;
import com.ebay.cloud.cms.typsafe.exporter.CodeGenerator.OnlineMetaJsonReader;
import com.ebay.cloud.cms.typsafe.metadata.model.MetadataManager;
import com.ebay.cloud.cms.web.RunTestServer;

/**
 * @author liasu
 * 
 */
public class CodeGeneratorTest {

    @BeforeClass
    public static void setUp() throws Exception {
        RunTestServer.startServer(new String[] { "-initData", "9000" });
    }

    @AfterClass
    public static void teardown() throws Exception {
        RunTestServer.stopServer();
    }

    @Test
    public void testGenerate() {
        String folder = EntityGeneratorTest.class.getResource("/").getFile();
        //
        MetaJsonReader reader = new OnlineMetaJsonReader("http://localhost:9000/cms", "raptor-paas", "user:fjao");
        CodeGenerator generator = new CodeGenerator(reader, folder, "test.cloud");
        generator.generate();

        URL packageUrl = CodeGeneratorTest.class.getResource("/test/cloud/");
        Assert.assertNotNull(packageUrl);
        File f = new File(packageUrl.getPath());
        Assert.assertTrue(f.exists());
    }

    @Test
    public void testGenerate2() {
        String folder = EntityGeneratorTest.class.getResource("/").getFile();
        //
        MetaJsonReader reader = new OnlineMetaJsonReader("http://localhost:9000/cms", "software-deployment",
                "user:fjao");
        CodeGenerator generator = new CodeGenerator(reader, folder, "test.cloud");
        generator.generate();

        URL packageUrl = CodeGeneratorTest.class.getResource("/test/cloud/");
        Assert.assertNotNull(packageUrl);
        File f = new File(packageUrl.getPath());
        Assert.assertTrue(f.exists());
    }

    @Test
    public void testGenerate3() {
        String metaFolder = EntityGeneratorTest.class.getResource("/raptor-paas.json").getFile();
        String genFolder = EntityGeneratorTest.class.getResource("/").getFile();

        MetaJsonReader reader = new FileMetaJsonReader(metaFolder);
        CodeGenerator generator = new CodeGenerator(reader, genFolder, "test.filegen.cloud");
        generator.generate();

        URL packageUrl = CodeGeneratorTest.class.getResource("/test/filegen/cloud/");
        Assert.assertNotNull(packageUrl);
        File f = new File(packageUrl.getPath());
        Assert.assertTrue(f.exists());

        File[] javaFiles = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".java");
            }
        });
        Assert.assertTrue(javaFiles.length > 0);
        Assert.assertEquals(10, javaFiles.length);
    }

    @Test
    public void testGenerate4() {
        String metaFolder = EntityGeneratorTest.class.getResource("/").getFile();
        String genFolder = EntityGeneratorTest.class.getResource("/").getFile();

        MetaJsonReader reader = new FileMetaJsonReader(metaFolder);
        CodeGenerator generator = new CodeGenerator(reader, genFolder, "test2.filegen.cloud");
        generator.generate();

        URL packageUrl = CodeGeneratorTest.class.getResource("/test2/filegen/cloud/");
        Assert.assertNotNull(packageUrl);
        File f = new File(packageUrl.getPath());
        Assert.assertTrue(f.exists());

        File[] javaFiles = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".java");
            }
        });
        Assert.assertTrue(javaFiles.length > 0);
        Assert.assertEquals(10, javaFiles.length);
    }

    @Test
    public void test5() throws IOException {
        Enumeration<URL> urls = CodeGeneratorTest.class.getClassLoader().getResources("");
        while (urls.hasMoreElements()) {
            // find everything under mongo/, actually returns a lot thing, will
            // filter later
            URL url = urls.nextElement();
            String[] segments = url.getFile().split("!");
            String jarPath = segments[0];
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring("file:".length());
            }
            System.out.println(jarPath);

            File file = new File(jarPath);
            if (!file.exists() || !jarPath.endsWith(".jar")) {
                System.out.println(MessageFormat.format("Reading meta json, ignore extracted {0} from full path {1}",
                        jarPath, url.getFile()));
                continue;
            }

            // find all names inside the jar file
            ZipFile zf = new ZipFile(file);
            Enumeration<? extends ZipEntry> zips = zf.entries();
            while (zips.hasMoreElements()) {
                ZipEntry ze = zips.nextElement();

                // say we only want the monog/mongo.properties in side
                // cms-common
                if (ze.getName().endsWith("mongo.properties")) {
                    System.out.println(ze.getName());
                    InputStream ins = CodeGeneratorTest.class.getClassLoader().getResourceAsStream(ze.getName());
                    Assert.assertNotNull(ins);

                    URL jsonUrl = CodeGeneratorTest.class.getClassLoader().getResource(ze.getName());
                    Assert.assertNotNull(jsonUrl);
                    ObjectMapper om = new ObjectMapper();
                    try {
                        om.readTree(jsonUrl);
                    } catch (Exception e) {
                        // expected
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    }

    @Test
    public void testPackageGeneration() {
        // this package is located in cms-common project src/test/resources
        String metaPackage = "com.ebay.cloud.cms.model.metadata";
        String genFolder = EntityGeneratorTest.class.getResource("/").getFile();

        MetaJsonReader reader = new FileMetaJsonReader(metaPackage);
        CodeGenerator generator = new CodeGenerator(reader, genFolder, "test3.packagegenerate");
        generator.generate();

        URL packageUrl = CodeGeneratorTest.class.getResource("/test3/packagegenerate/");
        Assert.assertNotNull(packageUrl);
        File f = new File(packageUrl.getPath());
        Assert.assertTrue(f.exists());

        File[] javaFiles = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".java");
            }
        });
        Assert.assertTrue(javaFiles.length > 0);
        Assert.assertEquals(73, javaFiles.length);
    }

    @Test
    public void testMetaManager_load() {
        // this package is located in cms-common project src/test/resources
        String metaPackage = "com.ebay.cloud.cms.model.metadata";

        MetaJsonReader reader = new FileMetaJsonReader(metaPackage);
        JsonNode metaNode = reader.getMetaJson();

        MetadataManager mm = MetadataManager.load(metaNode);
        Assert.assertEquals(73, mm.getMetadataNames().size());
    }

}
