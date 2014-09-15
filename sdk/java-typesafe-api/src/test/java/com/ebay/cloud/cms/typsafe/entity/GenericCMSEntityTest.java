/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.service.CMSClientServiceTest;
import com.google.common.io.Files;

/**
 * @author liasu
 * 
 */
public class GenericCMSEntityTest {

    @Test
    public void testBuildEntity_string() throws IOException, URISyntaxException {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());
        ObjectMapper om = new ObjectMapper();
        JsonNode rawNode = om.readTree(json);

        List<GenericCMSEntity> entites = GenericCMSEntity.buildEntity(json);
        Assert.assertEquals(1, entites.size());

        GenericCMSEntity entity = entites.get(0);
        JsonNode node = entity.toJson();

        Assert.assertTrue(node.equals(rawNode));
        System.out.println(node);
    }
    
    @Test
    public void testBuildEntity_string_dirty() throws IOException, URISyntaxException {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());
        ObjectMapper om = new ObjectMapper();
        JsonNode rawNode = om.readTree(json);

        List<GenericCMSEntity> entites = GenericCMSEntity.buildEntity(json);
        Assert.assertEquals(1, entites.size());

        GenericCMSEntity entity = entites.get(0);
        JsonNode node = entity.toJson();

        Assert.assertTrue(node.equals(rawNode));
        System.out.println(node);

        // clear bits should doesn't affect the to string method
        entity.clearDirtyBits();
        node = entity.toJson();
        Assert.assertTrue(node.equals(rawNode));

        // disable dirty check doesn't affect the tostring method
        entity.disableDirtyCheck();
        node = entity.toJson();
        Assert.assertTrue(node.equals(rawNode));

        // enable dirty check with some dirty bit doesn't affect the to string
        entity.enableDirtyCheck();
        entity.setFieldValue("nugget", "srp");
        node = entity.toJson();
        Assert.assertTrue(node.equals(rawNode));
    }

    @Test
    public void testBuildEntity_stringInvalid() throws IOException, URISyntaxException {
        String json = "{invalid-json}";
        try {
            GenericCMSEntity.buildEntity(json);
            Assert.fail();
        } catch (CMSEntityException cme) {
            // expected
        }
    }

    @Test
    public void testBuildEntity_json() throws IOException, URISyntaxException {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());
        ObjectMapper om = new ObjectMapper();
        JsonNode rawNode = om.readTree(json);

        List<GenericCMSEntity> entites = GenericCMSEntity.buildEntity(rawNode);
        Assert.assertEquals(1, entites.size());

        GenericCMSEntity entity = entites.get(0);
        JsonNode node = entity.toJson();

        Assert.assertTrue(node.equals(rawNode));
        System.out.println(node);
    }
    
    @Test
    public void testIncludeExclude_json() throws IOException, URISyntaxException {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());
        ObjectMapper om = new ObjectMapper();
        JsonNode rawNode = om.readTree(json);

        List<GenericCMSEntity> entites = GenericCMSEntity.buildEntity(rawNode);
        Assert.assertEquals(1, entites.size());

        GenericCMSEntity entity = entites.get(0);
        Assert.assertEquals(9, entity.getFieldNames().size());

        entity.includeFields("nugget", "name", "archTier");
        Assert.assertEquals(4, entity.getFieldNames().size());
        // idemponent
        entity.includeFields("nugget", "name", "archTier");
        Assert.assertEquals(4, entity.getFieldNames().size());
        // fault tolerance
        entity.includeFields("nugget", "name", "archTier", "field1");
        Assert.assertEquals(4, entity.getFieldNames().size());

        entity.excludeFields("field1");
        Assert.assertEquals(4, entity.getFieldNames().size());

        entity.excludeFields("nugget");
        Assert.assertEquals(3, entity.getFieldNames().size());

        // boundary check
        entity.excludeFields((String[]) null);
        Assert.assertEquals(3, entity.getFieldNames().size());
        entity.excludeFields(new String[0]);
        Assert.assertEquals(3, entity.getFieldNames().size());

        entity.includeFields((String[]) null);
        Assert.assertEquals(3, entity.getFieldNames().size());
        entity.includeFields(new String[0]);
        Assert.assertEquals(3, entity.getFieldNames().size());
    }
    
    @Test
    public void testQueryUrl() throws Exception{
        String query = "http://localhost/ApplicationService[@ab=~\".?\"]?allowFullTableScan=true&skip=0,1000&limit=1000,1000";
        String encodedquery = URLEncoder.encode(query, "utf-8");
        System.out.println(encodedquery);
        URL url = new URL(query);
        System.out.println(url.getPath());
        System.out.println(url.getQuery());
    }
    
    @Test
    public void testQueryUrl2() throws Exception {
        String query = "A[@a=~\".?\"]?allowFullTableScan=true&skip=0,1000&limit=1000,1000";
        int questMarkIndex = CMSQuery.findQuestionMarkIndex(query);
        Assert.assertEquals(query.lastIndexOf('?'), questMarkIndex);
        System.out.println(query.substring(0, questMarkIndex));
    }
    
    @Test
    public void testQueryUrl3() throws Exception {
        String query = "A[@a=~\".\\?\"]?allowFullTableScan=true&skip=0,1000&limit=1000,1000";
        int questMarkIndex = CMSQuery.findQuestionMarkIndex(query);
        Assert.assertEquals(query.lastIndexOf('?'), questMarkIndex);
        System.out.println(query.substring(0, questMarkIndex));
    }


}
