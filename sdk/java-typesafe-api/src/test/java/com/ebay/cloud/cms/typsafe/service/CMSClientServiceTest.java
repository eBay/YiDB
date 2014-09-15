package com.ebay.cloud.cms.typsafe.service;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.model.raptor_paas.ApplicationService;
import com.ebay.cloud.cms.model.raptor_paas.Cluster;
import com.ebay.cloud.cms.model.raptor_paas.Compute;
import com.ebay.cloud.cms.model.raptor_paas.Door;
import com.ebay.cloud.cms.model.raptor_paas.DummyCMSEntity;
import com.ebay.cloud.cms.model.raptor_paas.Room;
import com.ebay.cloud.cms.model.raptor_paas.ServiceInstance;
import com.ebay.cloud.cms.model.raptor_paas.UpdateStrategy;
import com.ebay.cloud.cms.model.software_deployment.Approval;
import com.ebay.cloud.cms.model.software_deployment.Manifest;
import com.ebay.cloud.cms.model.software_deployment.ManifestVersion;
import com.ebay.cloud.cms.model.software_deployment.NoUse;
import com.ebay.cloud.cms.service.resources.CMSResourceTest;
import com.ebay.cloud.cms.typsafe.entity.AbstractCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.CMSQuery;
import com.ebay.cloud.cms.typsafe.entity.CMSQuery.PaginationEnum;
import com.ebay.cloud.cms.typsafe.entity.CMSQuery.SortOrder;
import com.ebay.cloud.cms.typsafe.entity.CMSQueryResult;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.entity.internal.CMSEntityMapper;
import com.ebay.cloud.cms.typsafe.entity.internal.JsonCMSEntity;
import com.ebay.cloud.cms.typsafe.exception.CMSClientException;
import com.ebay.cloud.cms.typsafe.exception.CMSEntityException;
import com.ebay.cloud.cms.typsafe.exception.CMSErrorCodeEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaAttribute;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaClass;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.CardinalityEnum;
import com.ebay.cloud.cms.typsafe.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.typsafe.restful.Constants;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.CMSConsistentPolicy;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig.CMSPriority;
import com.ebay.cloud.cms.web.RunTestServer;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * 
 * @author liasu
 * 
 */
@SuppressWarnings("deprecation")
public class CMSClientServiceTest {

    private static final class VersionConflictionCall implements Callable<Integer> {
        private final String jsonResponse;
        private int          exceptionCount = 0;

        private VersionConflictionCall(String jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        @Override
        public Integer call() throws Exception {
            throw new CMSClientException(500, " faild count:" + (exceptionCount++), jsonResponse, null);
        }

        public int getExceptionCount() {
            return exceptionCount;
        }

        public void setExceptionCount(int exceptionCount) {
            this.exceptionCount = exceptionCount;
        }

    }

    private static final String CMSDB = "cmsdb";
    private static final String LOCAL_ENDPOINT = "http://localhost:9000/cms";
    private static final String RAPTOR_PAAS = "raptor-paas";
    private static final String SOFTWARE_DEPLOYMENT = "software-deployment";
    private static final String STRATUS_CI = "stratus-ci";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String RESOURCE_ID = "resourceId";
    private static final String NODE_SERVER = "NodeServer";
    private static final String RESOURCE_CAPCACITY = "ResourceCapacity";
    public static final String configRoot = "/scripts/jython/";


    private static CMSClientService raptorService;
    private static CMSClientService sdService;
    private static CMSClientService stratusService;
    private static CMSClientService cmsdbService;

    private static final Random random = new Random(System.currentTimeMillis());
    private static CMSClientConfig config;
    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        RunTestServer.startServer(new String[] { "-initData", "9000" });

        config = new CMSClientConfig(LOCAL_ENDPOINT, RAPTOR_PAAS, IBranch.DEFAULT_BRANCH,
                Constants.META_PACKAGE + ".raptor_paas");
        config.setTimeOut(0);
        raptorService = CMSClientService.getClientService(config);

        sdService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT,
                SOFTWARE_DEPLOYMENT, IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".software_deployment"));

        stratusService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, STRATUS_CI,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".stratus_ci"));
        
        cmsdbService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, CMSDB,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".cmsdb"));

        Assert.assertNotNull(config.getPriority());
        config.setPriority(null);
        Assert.assertNull(config.getPriority());

        Assert.assertNotNull(config.getConsistentPolicy());
        config.setConsistentPolicy(null);
        Assert.assertNull(config.getConsistentPolicy());

        Assert.assertNotNull(config.getTimeOut());
        config.setTimeOut(100);
        Assert.assertNotNull(config.getTimeOut());

        Assert.assertNull(config.getAuthorization());
        config.setAuthorization("user:ci-test");
        Assert.assertNotNull(config.getAuthorization());
        
        // load jython test scripts
//        PySystemState sys = Py.getSystemState();
//        sys.path.append(new PyString(CMSClientServiceTest.class.getResource(configRoot).getFile()));
//        PythonInterpreter interpreter = new PythonInterpreter();
//        interpreter.exec(MessageFormat.format("from {0} import {1}", "query", "Query"));
//        PyObject pyTestClass = interpreter.get("Query");
//        pyTest = (IPyTest) pyTestClass.__call__().__tojava__(IPyTest.class);
    }

    @AfterClass
    public static void teardown() throws Exception {
        RunTestServer.stopServer();
    }

    @Test
    public void testGetClientNoRepo() {
        try {
            CMSClientConfig nconfig = new CMSClientConfig(config.getServerBaseUrl(), null, IBranch.DEFAULT_BRANCH,
                    Constants.META_PACKAGE);
            CMSClientService.getClientService(nconfig);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            CMSClientConfig nconfig = new CMSClientConfig(config.getServerBaseUrl(), RAPTOR_PAAS, null,
                    Constants.META_PACKAGE);
            CMSClientService.getClientService(nconfig);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            CMSClientConfig nconfig = new CMSClientConfig(config.getServerBaseUrl(), RAPTOR_PAAS,
                    IBranch.DEFAULT_BRANCH, null);
            CMSClientService.getClientService(nconfig);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void getByOid() throws JsonGenerationException, JsonMappingException, IOException {
        ApplicationService appServ = raptorService.get("4fbb314fc681caf13e283a76", ApplicationService.class);
        Assert.assertNotNull(appServ);
        System.out.println(appServ.toString());

        Assert.assertNotNull(appServ.get_createtime());
    }

    @Test
    public void getByOid_different_repo() throws JsonGenerationException, JsonMappingException, IOException {
        Manifest getManfiest = cmsdbService.get("51e5ff3ee4b0f20e6850612", Manifest.class, new CMSClientContext());
        Assert.assertNotNull(getManfiest);
        System.out.println(getManfiest.toString());

        Assert.assertEquals("cmsdb", getManfiest.get_repo());
    }

    @Test
    public void getByOid_includeFields() throws JsonGenerationException, JsonMappingException, IOException {
        ApplicationService appServ = raptorService.get("4fbb314fc681caf13e283a76", ApplicationService.class, null, "name", "archTier");
        Assert.assertNotNull(appServ);

        Assert.assertNull(appServ.get_createtime());
        Assert.assertTrue(appServ.hasField("name"));
        Assert.assertTrue(appServ.hasField("archTier"));
        Assert.assertFalse(appServ.hasField("healthStatus"));
    }

    @Test
    public void getByField() throws Exception {
        // case 0 : by oid
        CMSClientContext context = new CMSClientContext();
        String applicationServiceId = "4fbb314fc681caf13e283a76";
        ApplicationService getApp = raptorService.getEntitiesByField(ApplicationService.class, null, "_oid",
                applicationServiceId, context).get(0);
        Assert.assertNotNull(getApp);
        ApplicationService as = raptorService.getEntityByField(ApplicationService.class, null, "_oid",
                applicationServiceId, context);
        Assert.assertNotNull(as);
        // case 0.b
        List<GenericCMSEntity> gEntities = raptorService.getEntitiesByField("ApplicationService", "_oid",
                applicationServiceId, context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ApplicationService", entity.get_type());
        }
        GenericCMSEntity gentity = raptorService.getEntityByField("ApplicationService", "_oid", applicationServiceId,
                context);
        Assert.assertNotNull(gentity);

        // case 1 : by string, note port is defined as string
        List<ServiceInstance> services = raptorService.getEntitiesByField(ServiceInstance.class, null, "port", "8080",
                context);
        Assert.assertNotNull(services);
        // we have multiple service instance
        Assert.assertTrue(services.size() > 1);
        try {
            raptorService.getEntityByField(ServiceInstance.class, null, "port", "8080", context);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
            
        }
        // case 1.b
        gEntities = raptorService.getEntitiesByField("ServiceInstance", "port", "8080", context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 1);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ServiceInstance", entity.get_type());
        }
        try {
            raptorService.getEntityByField("ServiceInstance", "port", "8080", context);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
        }

        // case 2 : by integer
        services = raptorService.getEntitiesByField(ServiceInstance.class, null, "_version", 0, context);
        Assert.assertNotNull(services);
        Assert.assertTrue(services.size() > 0);
        // case 2.b
        gEntities = raptorService.getEntitiesByField("ServiceInstance", "_version", 0, context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ServiceInstance", entity.get_type());
        }

        // case 3 : by boolean
        services = raptorService.getEntitiesByField(ServiceInstance.class, null, "https", false, context);
        Assert.assertNotNull(services);
        Assert.assertTrue(services.size() > 0);
        // case 3.b
        gEntities = raptorService.getEntitiesByField("ServiceInstance", "https", false, context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ServiceInstance", entity.get_type());
        }

        ServiceInstance si = raptorService.get("4fbb314fc681caf13e283a7a", ServiceInstance.class, context);
        // case 4 : by date
        services = raptorService.getEntitiesByField(ServiceInstance.class, null, "_lastmodified",
                si.get_lastmodified(), context);
        Assert.assertNotNull(services);
        Assert.assertTrue(services.size() > 0);
        // case 4.b
        gEntities = raptorService.getEntitiesByField("ServiceInstance", "_status", "active", context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ServiceInstance", entity.get_type());
        }

        // case 5 : by double // TODO no double in sample meta now

        // case 6 : by Enum // TODO no enum in sample meta now for type-safe ...
        List<GenericCMSEntity> vpools = stratusService.getEntitiesByField("VPool", "healthState", "UNKNOWN", context);
        Assert.assertNotNull(vpools);
        Assert.assertTrue(vpools.size() > 0);
        for (GenericCMSEntity entity : vpools) {
            Assert.assertEquals("VPool", entity.get_type());
        }

        // case 7: by reference
        List<ApplicationService> appServices = raptorService.getEntitiesByField(ApplicationService.class, null,
                "services", "4fbb314fc681caf13e283a7a", context);
        Assert.assertNotNull(appServices);
        Assert.assertTrue(appServices.size() > 0);
        // case 7.b
        gEntities = raptorService.getEntitiesByField("ApplicationService", "services", "4fbb314fc681caf13e283a7a",
                context);
        Assert.assertNotNull(gEntities);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("ApplicationService", entity.get_type());
        }

        // case 8 : json : no cases
    }

    @Test
    public void getByField2() throws Exception {
        // not existing case
        CMSClientContext context = new CMSClientContext();
        List<Compute> computes = raptorService.getEntitiesByField(Compute.class, null, "https", null, context);
        Assert.assertTrue(computes.size() > 0);

        List<ApplicationService> apps = raptorService.getEntitiesByField(ApplicationService.class, null, "name", null,
                context);
        Assert.assertTrue(apps.size() == 0);
        ApplicationService as = raptorService.getEntityByField(ApplicationService.class, null, "name", null, context);
        Assert.assertTrue(as == null);

        List<GenericCMSEntity> gEntities = raptorService.getEntitiesByField("Compute", "https", null, context);
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("Compute", entity.get_type());
        }

        gEntities = raptorService.getEntitiesByField("ApplicationService", "name", null, context);
        Assert.assertTrue(gEntities.size() == 0);
        GenericCMSEntity gEntity = raptorService.getEntityByField("ApplicationService", "name", null, context);
        Assert.assertNull(gEntity);
    }
    
    @Test
    public void getByField3_includeFields() throws Exception {
        // not existing case
        CMSClientContext context = new CMSClientContext();
        List<Compute> computes = raptorService.getEntitiesByField(Compute.class, null, "https", null, context, "name");
        Assert.assertTrue(computes.size() > 0);
        Assert.assertTrue(computes.get(0).hasField("name"));
        Assert.assertFalse(computes.get(0).hasField("https"));

        List<GenericCMSEntity> gEntities = raptorService.getEntitiesByField("Compute", "https", null, context, "name");
        Assert.assertTrue(gEntities.size() > 0);
        for (GenericCMSEntity entity : gEntities) {
            Assert.assertEquals("Compute", entity.get_type());
            Assert.assertTrue(entity.hasField("name"));
            Assert.assertFalse(entity.hasField("https"));
        }
    }

    @Test
    public void getByField3Embed() throws Exception {
        // a
        CMSClientContext context = new CMSClientContext();
        List<ManifestVersion> versions = sdService.getEntitiesByField(ManifestVersion.class, "Manifest.versions", "name",
                "Dummy ManifestVersion Bundle-0-0001", context);
        Assert.assertTrue(versions.size() > 0);

        // b
        List<GenericCMSEntity> gVersions = sdService.getEntitiesByField("Manifest.versions", "name",
                "Dummy ManifestVersion Bundle-0-0001", context);
        Assert.assertTrue(gVersions.size() > 0);
        for (GenericCMSEntity entity : gVersions) {
            Assert.assertEquals("ManifestVersion", entity.get_type());
        }
    }

    /**
     * TODO: Can't support a case when client library doens't conform to the
     * server metadata When the client library is out of date, says user add a
     * field
     * 
     * @throws Exception
     */
    @Test
    public void getJsonTypeField() throws Exception {
        // add a json-type field to the ApplicationService, add a entity
        // containing a such value
        // then get it through apis..

        Client c = Client.create();
        // update to add a field to application seervice
        WebResource wr = c.resource(LOCAL_ENDPOINT + "/repositories/raptor-paas/metadata/ApplicationService");
        String json = CMSResourceTest.loadJson("/ApplicationServiceAddJsonField.json");
        ClientResponse resp = wr.entity(json, MediaType.APPLICATION_JSON).post(ClientResponse.class);
        Assert.assertEquals(200, resp.getStatus());
        System.out.println(resp.getEntity(String.class));

        // Update a entity to add values to the json type field
        WebResource entityWr = c
                .resource(LOCAL_ENDPOINT + "/repositories/raptor-paas/branches/main/ApplicationService/4fbb314fc681caf13e283a76");
        String entityJson = CMSResourceTest.loadJson("/ApplicationServiceUpdateJsonField.json");
        ClientResponse entityCr = entityWr.entity(entityJson, MediaType.APPLICATION_JSON).post(ClientResponse.class);
//        Assert.assertEquals(200, entityCr.getStatus());
        String entityRespStr = entityCr.getEntity(String.class);
        System.out.println(entityRespStr);

        // case 0: get entities with json fields fetched
        ApplicationService getAppService = raptorService.get("4fbb314fc681caf13e283a76", ApplicationService.class);
        Assert.assertNotNull(getAppService);
        Object jsonField = getAppService.getFieldValue("jsonField");
        Object multiJsonField = getAppService.getFieldValue("multiJsonField");
        Assert.assertNotNull(jsonField);
        Assert.assertTrue(jsonField instanceof ObjectNode);
        Assert.assertNotNull(multiJsonField);
        Assert.assertTrue(multiJsonField instanceof ArrayNode);

        // case 1: create entities with json fields given
        ApplicationService as = new ApplicationService();
        as.setName(generateRandomName("appName-newWithJson"));
        ObjectNode jo = JsonNodeFactory.instance.objectNode();
        jo.put("title", "S/W Eng");
        ObjectNode embedJson = JsonNodeFactory.instance.objectNode();
        embedJson.put("embedField-Contact", "DL-CLOUD-CMS-SHA@ebay.com");
        jo.put("embedJsonObject", embedJson);
        as.setFieldValue("jsonField", jo);

        ArrayNode jos = JsonNodeFactory.instance.arrayNode();
        jos.add(jo);
        as.setFieldValue("multiJsonField", jos);

        ApplicationService createdAs = raptorService.create(as);
        Assert.assertNotNull(createdAs.get_id());
        ApplicationService getAs = raptorService.get(createdAs.get_id(), ApplicationService.class);
        Assert.assertNotNull(getAs);
        Assert.assertEquals(as.getName(), getAs.getName());
        Assert.assertNotNull(getAs.getFieldValue("jsonField"));
        Assert.assertTrue(getAs.getFieldValue("jsonField") instanceof JsonNode);
        Assert.assertNotNull(getAs.getFieldValue("multiJsonField"));
        Assert.assertNotNull(getAs.getFieldValue("multiJsonField") instanceof ArrayNode);

        CMSEntityMapper mapper = new CMSEntityMapper(null, config, JsonCMSEntity.class, CMSEntityMapper.ProcessModeEnum.TYPE_SAFE, ApplicationService.class);
        getAs.traverse(mapper);
        System.out.println(((JsonCMSEntity) mapper.getTargetEntity()).getNode());
    }

    @Test
    public void getNotFound() throws JsonGenerationException, JsonMappingException, IOException {
        ApplicationService appServ = raptorService.get("4fbb314fc681caf13e283a76-invalid", ApplicationService.class);
        Assert.assertNull(appServ);
    }

    @Test(expected = CMSClientException.class)
    public void getInvalidClass() throws JsonGenerationException, JsonMappingException, IOException {
        raptorService.get("4fbb314fc681caf13e283a76-invalid", AbstractCMSEntity.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNoOid() throws JsonGenerationException, JsonMappingException, IOException {
        raptorService.get(null, ApplicationService.class);
    }

    @Test
    public void list() throws JsonGenerationException, JsonMappingException, IOException {
        List<ApplicationService> appServ = raptorService.get(ApplicationService.class, 0);
        Assert.assertNotNull(appServ);
        Assert.assertTrue(appServ.size() > 0);
        System.out.println(appServ.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void listIncorrectGeneric() throws JsonGenerationException, JsonMappingException, IOException {
        raptorService.get(GenericCMSEntity.class, 0);
    }

    @Test
    public void listWithMetaChanged() throws Exception {
        Client c = Client.create();
        // update to add a field to application seervice
        WebResource wr = c.resource(LOCAL_ENDPOINT + "/repositories/raptor-paas/metadata/ApplicationService");
        String json = CMSResourceTest.loadJson("/ApplicationServiceUpdate.json");
        ClientResponse resp = wr.entity(json, MediaType.APPLICATION_JSON).post(ClientResponse.class);
        Assert.assertEquals(200, resp.getStatus());
        System.out.println(resp.getEntity(String.class));

        // Add a entity with new field
        WebResource entityWr = c
                .resource(LOCAL_ENDPOINT + "/repositories/raptor-paas/branches/main/ApplicationService");
        String entityJson = CMSResourceTest.loadJson("/ApplicationServiceWithNewField.json");
        ClientResponse entityCr = entityWr.entity(entityJson, MediaType.APPLICATION_JSON).post(ClientResponse.class);
        Assert.assertEquals(200, entityCr.getStatus());
        String entityRespStr = entityCr.getEntity(String.class);
        System.out.println(entityRespStr);

        // fetch entity using old model object that without new field
        List<ApplicationService> allApp = raptorService.get(ApplicationService.class, 0);
        boolean foundNew = false;
        Assert.assertNotNull(allApp);
        Assert.assertTrue(allApp.size() > 0);
        for (ApplicationService as : allApp) {
            if (as.getName().equals("new-field-entity")) {
                foundNew = true;
                break;
            }
        }
        Assert.assertTrue(foundNew);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(expected = IllegalArgumentException.class)
    public void listNoMetaclass() throws JsonGenerationException, JsonMappingException, IOException {
        raptorService.get(IBranch.DEFAULT_BRANCH, (Class) null);
    }

    @Test
    public void listEmpty() {
        List<Cluster> clusters = raptorService.get(Cluster.class, 0);
        Assert.assertNotNull(clusters);
        Assert.assertTrue(clusters.size() >= 0);
    }

    @Test
    public void query() throws JsonGenerationException, JsonMappingException, IOException {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, "ApplicationService{*}?allowFullTableScan=false");
        query.setAllowFullTableScan(true);

        CMSClientContext context = new CMSClientContext();
        List<ApplicationService> queryResults = raptorService.query(query, ApplicationService.class, context)
                .getEntities();
        Assert.assertNotNull(queryResults);
        Assert.assertTrue(queryResults.size() > 0);
        Assert.assertTrue(queryResults.get(0).get_metaclass().equals("ApplicationService"));
        for (ICMSEntity ent : queryResults) {
            System.out.println(ent.toString());
        }

        query.setQueryString("ApplicationService.services{*}");
        context.setComment("");
        context.setUser("");
        List<ServiceInstance> queryResults2 = raptorService.query(query, ServiceInstance.class, context).getEntities();
        Assert.assertNotNull(queryResults2);
        Assert.assertTrue(queryResults2.size() > 0);
        Assert.assertTrue(queryResults2.get(0).get_metaclass().equals("ServiceInstance"));
        for (ICMSEntity ent : queryResults2) {
            System.out.println(ent.toString());
        }

        query.setQueryString("ApplicationService.services.runsOn{@name}?skip=0&limit=1");
        List<Compute> queryResults3 = raptorService.query(query, Compute.class, null).getEntities();
        Assert.assertNotNull(queryResults3);
        Assert.assertTrue(queryResults3.size() > 0);
        Assert.assertTrue(queryResults3.get(0).get_metaclass().equals("Compute"));
        for (ICMSEntity ent : queryResults3) {
            System.out.println(ent.toString());
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void fullQuery() throws JsonGenerationException, JsonMappingException, IOException {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, "ApplicationService[@name=~\":Raptor\"]{*}?allowFullTableScan=false");
        query.setAllowFullTableScan(true);

        CMSClientContext context = new CMSClientContext();
        List<ApplicationService> queryResults = raptorService.query(query, ApplicationService.class, context)
                .getEntities();
        Assert.assertNotNull(queryResults);
        Assert.assertTrue(queryResults.size() > 0);
        Assert.assertTrue(queryResults.get(0).get_metaclass().equals("ApplicationService"));
        
        long count = 0;
        Map<String, Integer> servicesMap = new HashMap<String, Integer>();
        for (ICMSEntity ent : queryResults) {
            List<ServiceInstance> services = (List<ServiceInstance>)ent.getFieldValue("services");
            if (services != null) {
                servicesMap.put(ent.get_id(), services.size());
                ++count;
            }
        }

        query.setQueryString("ApplicationService[@name=~\":Raptor\"]{*}.services{*}");
        query.setLimits(new long[] {1, 2});
        context.setComment("");
        context.setUser("");
        
        CMSQueryResult<ApplicationService> result = raptorService.fullQuery(query, ApplicationService.class, context);
        List<ApplicationService> queryResults2 = result.getEntities();
        Assert.assertNotNull(queryResults2);
        Assert.assertEquals(count, queryResults2.size());
        Assert.assertEquals(count, result.getCount().longValue());
        Assert.assertTrue(result.getDbTimeCost() > 0);
        Assert.assertTrue(result.getTotalTimeCost() > 0);
        Assert.assertFalse(result.isHasMore());
        
        Assert.assertTrue(queryResults2.get(0).get_metaclass().equals("ApplicationService"));
        for (ICMSEntity ent : queryResults2) {
            List<ServiceInstance> services = (List<ServiceInstance>)ent.getFieldValue("services");
            Assert.assertTrue(servicesMap.containsKey(ent.get_id()));
            Assert.assertEquals(services.size(), servicesMap.get(ent.get_id()).intValue());
        }
    }
    
    @Test
    public void query_invalid() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, "ApplicationService[abc = 1]");
        // test for illegal query string, SHOULD not have NPE
        try {
            raptorService.query(query, Compute.class, null);
        } catch (CMSClientException cce) {
            Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cce.getHttpResponseCode());
            Assert.assertEquals(CMSErrorCodeEnum.PARSE_GRAMMER_ERROR, cce.getCmsResponseStatus().getErrorEnum());
        }
    }

    @Test
    public void query2() throws JsonGenerationException, JsonMappingException, IOException {
        CMSQuery query = new CMSQuery("ApplicationService{*}.services{*}");
        query.setAllowFullTableScan(true);

        CMSClientContext context = new CMSClientContext();
        List<ApplicationService> queryResults = raptorService.query(query, ApplicationService.class, context)
                .getEntities();
        Assert.assertNotNull(queryResults);
        Assert.assertTrue(queryResults.size() > 0);
        Assert.assertTrue(queryResults.get(0).get_metaclass().equals("ApplicationService"));
        for (ICMSEntity ent : queryResults) {
            System.out.println(ent.toString());
        }

        query.setQueryString("ApplicationService.services{*}");
        context.setComment("");
        context.setUser("");
        List<ServiceInstance> queryResults2 = raptorService.query(query, ServiceInstance.class, context).getEntities();
        Assert.assertNotNull(queryResults2);
        Assert.assertTrue(queryResults2.size() > 0);
        Assert.assertTrue(queryResults2.get(0).get_metaclass().equals("ServiceInstance"));
        for (ICMSEntity ent : queryResults2) {
            System.out.println(ent.toString());
        }

        query.setQueryString("ApplicationService.services.runsOn{@name}");
        List<Compute> queryResults3 = raptorService.query(query, Compute.class).getEntities();
        Assert.assertNotNull(queryResults3);
        Assert.assertTrue(queryResults3.size() > 0);
        Assert.assertTrue(queryResults3.get(0).get_metaclass().equals("Compute"));
        for (ICMSEntity ent : queryResults3) {
            System.out.println(ent.toString());
        }
    }

    @Test
    public void queryTypeHint() throws JsonGenerationException, JsonMappingException, IOException {
        CMSQuery query = new CMSQuery("ApplicationService{*}");
        query.setAllowFullTableScan(true);

        List<ApplicationService> queryResults = raptorService.query(query, ApplicationService.class).getEntities();
        Assert.assertNotNull(queryResults);
        Assert.assertTrue(queryResults.size() > 0);
        Assert.assertTrue(queryResults.get(0).get_metaclass().equals("ApplicationService"));

        for (ICMSEntity ent : queryResults) {
            System.out.println(mapper.writeValueAsString(ent));
        }

        query.setQueryString("ApplicationService.services{*}");
        List<ServiceInstance> serviceResults = raptorService.query(query, ServiceInstance.class).getEntities();
        Assert.assertNotNull(serviceResults);
        Assert.assertTrue(serviceResults.size() > 0);
        for (ICMSEntity ent : serviceResults) {
            System.out.println(mapper.writeValueAsString(ent));
        }

        query.setQueryString("ApplicationService.services.runsOn{@name}");
        List<Compute> computeResults = raptorService.query(query, Compute.class).getEntities();
        Assert.assertNotNull(computeResults);
        Assert.assertTrue(computeResults.size() > 0);
        for (ICMSEntity ent : computeResults) {
            System.out.println(mapper.writeValueAsString(ent));
        }
    }

    @Test
    public void queryRootUnion() {
        String queryString = "ApplicationService || Compute";
        CMSQuery query = new CMSQuery(queryString);
        CMSQueryResult<ICMSEntity> results = raptorService.query(query, ICMSEntity.class, new CMSClientContext());
        Assert.assertNotNull(results);
        Assert.assertFalse(results.isHasMore());
        for (ICMSEntity e : results.getEntities()) {
            Assert.assertTrue((e instanceof ApplicationService) || (e instanceof Compute));
        }
    }

    @Test
    public void queryTypeWrongHint() {
        CMSClientContext context = new CMSClientContext();
        CMSQuery query = new CMSQuery("ApplicationService");
        query.setAllowFullTableScan(true);
        try {
            raptorService.query(query, ServiceInstance.class, context);
            Assert.fail();
        } catch (CMSEntityException cee) {
            // expected
            System.out.println(cee.getMessage());
        }
    }

    @Test
    public void queryEmpty() {
        CMSQuery query = new CMSQuery("Cluster{*}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() >= 0);
    }

    @Test
    public void queryFullTableScan() {
        CMSQuery query = new CMSQuery("ApplicationService{*}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
    }

    @Test
    public void queryAllowFullTableScan() {
        CMSQuery query = new CMSQuery("ApplicationService{*}");
        query.setAllowFullTableScan(false);
        raptorService.query(query, new CMSClientContext());
    }

    @Test
    public void queryNoRepo() {
        CMSQuery query = new CMSQuery(null, "", "ApplicationService{*}");
        query.setAllowFullTableScan(true);
        raptorService.query(query);
    }

    @Test
    public void queryIncorretRepo() {
        CMSQuery query = new CMSQuery("not existing repo", "", "ApplicationService{*}");
        query.setAllowFullTableScan(true);
        try {
            raptorService.query(query);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
            Assert.assertTrue(iae.getMessage().contains("make it consistent, or simple not set in query!"));
        }
    }

    @Test
    public void queryIncorretBranch() {
        CMSQuery query = new CMSQuery(null, "wrong-branch", "ApplicationService{*}");
        query.setAllowFullTableScan(true);
        try {
            raptorService.query(query);
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // expected
            Assert.assertTrue(iae.getMessage().contains("make it consistent, or simple not set in query!"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void queryNoQueryString() {
        CMSQuery query = new CMSQuery((String) null);
        raptorService.query(query, new CMSClientContext());
    }

    @Test
    public void queryAggregationGeneric() {
        String queryStr = "ServiceInstance[exists @https and exists @activeManifestDiff]<@https, @activeManifestDiff>[ $max(@port) > \"123\"] { @https, @activeManifestDiff, $max(@port), $min(@port), $sum(@_pversion), $avg(@_version), $count() }";
        CMSQuery query = new CMSQuery(queryStr);
        query.setAllowFullTableScan(true);

        List<GenericCMSEntity> results = raptorService.query(query).getEntities();
        Assert.assertEquals(2, results.size());
        for (GenericCMSEntity res : results) {
            Assert.assertNotNull(res.getFieldValue("https"));
            Assert.assertNotNull(res.getFieldValue("activeManifestDiff"));
            Assert.assertNotNull(res.getFieldValue("$max_port"));
            Assert.assertNotNull(res.getFieldValue("$min_port"));
            Assert.assertNotNull(res.getFieldValue("$sum__pversion"));
            Assert.assertNotNull(res.getFieldValue("$avg__version"));
            Assert.assertNotNull(res.getFieldValue("$count"));
        }
    }

    @Test
    public void queryAggregationJoinGeneric() {
        String queryStr = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]{ @https, $max(@port) }";
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, queryStr);
        query.setAllowFullTableScan(true);

        List<GenericCMSEntity> results = raptorService.query(query).getEntities();
        Assert.assertEquals(2, results.size());
        for (GenericCMSEntity res : results) {
            Assert.assertNotNull(res.getFieldValue("https"));
            Assert.assertNotNull(res.getFieldValue("$max_port"));
            Assert.assertNull(res.getFieldValue("activeManifestDiff"));
            Assert.assertNull(res.getFieldValue("$min_port"));
            Assert.assertNull(res.getFieldValue("$sum__pversion"));
            Assert.assertNull(res.getFieldValue("$avg__version"));
            Assert.assertNull(res.getFieldValue("$count"));
        }
    }

    @Test
    public void queryAggregationTypeHint() {
        String queryStr = "ServiceInstance[exists @https and exists @activeManifestDiff]<@https, @activeManifestDiff>[ $max(@port) > \"123\"] { @https, @activeManifestDiff, $max(@port), $min(@port), $sum(@_pversion), $avg(@_version), $count() }";
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, queryStr);
        query.setAllowFullTableScan(true);

        List<ServiceInstance> results = raptorService.query(query, ServiceInstance.class).getEntities();
        Assert.assertEquals(2, results.size());
        for (ServiceInstance res : results) {
            Assert.assertNotNull(res.getFieldValue("https"));
            Assert.assertNotNull(res.getHttps());
            Assert.assertNotNull(res.getFieldValue("activeManifestDiff"));
            Assert.assertNotNull(res.getActiveManifestDiff());

            Assert.assertNotNull(res.getFieldValue("$max_port"));
            Assert.assertNotNull(res.getFieldValue("$min_port"));
            Assert.assertNotNull(res.getFieldValue("$sum__pversion"));
            Assert.assertNotNull(res.getFieldValue("$avg__version"));
            Assert.assertNotNull(res.getFieldValue("$count"));
        }
    }

    @Test
    public void queryAggregationJoinTypeHint() {
        String queryStr = "ApplicationService.services<@https, @activeManifestDiff>[ $max(@port) > \"123\"]{ @https, $max(@port) }";
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, queryStr);
        query.setAllowFullTableScan(true);

        List<ServiceInstance> results = raptorService.query(query, ServiceInstance.class).getEntities();
        Assert.assertEquals(2, results.size());
        for (ServiceInstance res : results) {
            Assert.assertNotNull(res.getFieldValue("https"));
            Assert.assertNotNull(res.getHttps());
            Assert.assertNull(res.getFieldValue("activeManifestDiff"));
            Assert.assertNull(res.getActiveManifestDiff());

            Assert.assertNotNull(res.getFieldValue("$max_port"));
            Assert.assertNull(res.getFieldValue("$min_port"));
            Assert.assertNull(res.getFieldValue("$sum__pversion"));
            Assert.assertNull(res.getFieldValue("$avg__version"));
            Assert.assertNull(res.getFieldValue("$count"));
        }
    }

    @Test
    public void createNoReference() throws Exception {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());

        ApplicationService appServ = mapper.readValue(json, ApplicationService.class);
        appServ.setName(appServ.getName() + System.currentTimeMillis());
        appServ.set_branch(IBranch.DEFAULT_BRANCH);
        CMSClientContext context = new CMSClientContext();
        context.setComment("general-comment-for-unit-test");
        context.setConsistentPolicy(CMSConsistentPolicy.PRIMARY_ONLY.name());
        context.setPriority(CMSPriority.NEUTRAL.name());
        context.setUser("unit-test-user");
        ApplicationService createServ = raptorService.create(appServ, context);
        Assert.assertNotNull(createServ.get_id());

        context = new CMSClientContext();
        ApplicationService getAppServ = raptorService.get(createServ.get_id(), ApplicationService.class, context);
        Assert.assertNotNull(getAppServ);
        System.out.println(mapper.writeValueAsString(getAppServ));

        checkContextResponseOk(context);
    }

    private void checkContextResponseOk(CMSClientContext context) {
        Assert.assertTrue(context.getLastResponse() != null);
        Assert.assertTrue(context.getLastCmsStatus() != null);
        Assert.assertTrue(context.getLastCmsStatus().getErrorCode() == 200);
        Assert.assertTrue(context.getLastDualWriteStatus() == null);
    }

    @Test
    public void createNoReference_consistentPolicy() throws Exception {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());
        ApplicationService appServ = mapper.readValue(json, ApplicationService.class);
        appServ.setName(generateRandomName(appServ.getName()));
        appServ.set_branch(IBranch.DEFAULT_BRANCH);
        CMSClientContext context = new CMSClientContext();
        context.setComment("general-comment-for-unit-test");
        context.setConsistentPolicy(CMSConsistentPolicy.PRIMARY);
        context.setPriority(CMSPriority.NEUTRAL);
        context.setUser("unit-test-user");
        ApplicationService createServ = raptorService.create(appServ, context);
        Assert.assertNotNull(createServ.get_id());

        context = new CMSClientContext();
        ApplicationService getAppServ = raptorService.get(createServ.get_id(), ApplicationService.class, context);
        Assert.assertNotNull(getAppServ);
        System.out.println(mapper.writeValueAsString(getAppServ));
    }

    @Test
    public void createNoBranchId() {
        ApplicationService appServ = new ApplicationService();
        appServ.setName("app_test_name_" + System.currentTimeMillis() + random.nextDouble());
        raptorService.create(appServ);
    }

    @Test(expected = NullPointerException.class)
    public void createNullEntity() {
        raptorService.create(null);
    }

    @Test
    public void createNullContext_1() {
        ApplicationService appServ = new ApplicationService();
        appServ.setName("app_test_name_" + System.currentTimeMillis() + random.nextDouble());
        raptorService.create(appServ, null);
    }

    @Test
    public void createReference() {
        ServiceInstance si = newServiceInstance();
        si.setHealthStatus("healstatus");

        ServiceInstance createdSi = raptorService.create(si);
        Assert.assertNotNull(createdSi);
        Assert.assertNotNull(createdSi.get_id());

        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        ServiceInstance refSi = new ServiceInstance();
        refSi.set_id(createdSi.get_id());
        refSi.setName(si.getName());// not necessary step
        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(refSi);
        appServ.setServices(sis);
        ApplicationService createdServ = raptorService.create(appServ);
        Assert.assertNotNull(createdServ);
        Assert.assertNotNull(createdServ.get_id());
        Assert.assertNotNull(createdServ.getServices());
        Assert.assertTrue(createdServ.getServices().size() > 0);
        Assert.assertTrue(createdServ.getServices().get(0) instanceof ServiceInstance);
        Assert.assertEquals(si.getName(), createdServ.getServices().get(0).getName());
        Assert.assertNull(createdServ.getServices().get(0).getHealthStatus());// other
                                                                              // field
                                                                              // is
                                                                              // not
                                                                              // fetched
    }

    private String generateRandomName(String baseName) {
        return baseName + System.currentTimeMillis() + random.nextDouble();
    }

    @Test
    public void createEnum() {
        // no metadata for test now.
    }

    @Test
    public void createEmbed_hierarchical() {
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));

        NoUse noUses = new NoUse();
        noUses.setName(generateRandomName("nouse_name_"));
        manifest.setNoUses(noUses);

        Manifest createdMani = sdService.create(manifest);

        Manifest query = new Manifest();
        query.set_branch(IBranch.DEFAULT_BRANCH);
        query.set_id(createdMani.get_id());
        Manifest getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani);
        Assert.assertNotNull(getMani.getNoUses());
        Assert.assertNotNull(getMani.getNoUses().getName());
        Assert.assertEquals(noUses.getName(), getMani.getNoUses().getName());
    }

    @Test
    public void createEmbed_withPath() {
        CMSClientContext context = new CMSClientContext();
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));
        sdService.create(manifest, context);

        NoUse noUses = new NoUse();
        noUses.setName(generateRandomName("nouse_name_"));
        context.setPath(Manifest.class.getSimpleName(), manifest.get_id(), "noUses");
        sdService.create(noUses, context);

        NoUse getNoUse = sdService.get(noUses.get_id(), NoUse.class, context);
        Assert.assertNotNull(getNoUse);
    }

    @Test
    public void createInner() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        raptorService.create(room, context);
        // create door
        Door door = new Door();
        door.setName(generateRandomName("door-name"));
        context.setPath(Room.class.getSimpleName(), room.get_id(), "path");
        raptorService.create(door, context);
        String doorId = door.get_id();
        // asssertion
        Door getDoor = raptorService.get(doorId, Door.class, context);
        Assert.assertNotNull(getDoor);
    }

    @Test
    public void createInner_2() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        Door door =new Door();
        room.addPath(door);
        raptorService.create(room, context);

        Assert.assertNotNull(room.get_id());
        Assert.assertNotNull(door.get_id());
        Room getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertEquals(1, getRoom.getPath().size());
        Door getDoor = raptorService.get(door.get_id(), Door.class, context);
        Assert.assertNotNull(getDoor);
        Assert.assertEquals(getDoor.get_id(), getRoom.getPath().get(0).get_id());
    }
    
    @Test
    public void createInner_3() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        Door door = new Door();
        door.set_id("wrong-id");
        room.addPath(door);
        try {
            raptorService.create(room, context);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected, as user given door an not-existing id
        }
        // verify
        Assert.assertNotNull(room.get_id());
        Assert.assertNotNull(door.get_id());
        Room getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertNotNull(getRoom);
        Assert.assertEquals(null, getRoom.getPath());
        // remove id and update room to create inner entities
        door.set_id(null);
        Door door2 = new Door();
        door.setName(generateRandomName("door2-name"));
        room.addPath(door2);
        // update the created room for inner relationships
        raptorService.update(room, null);

        getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertNotNull(getRoom);
        Assert.assertEquals(2, getRoom.getPath().size());
    }
    
    @Test
    public void modifyToCreate_inner() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        raptorService.create(room, context);

        Door door = new Door();
        room.addPath(door);
        raptorService.update(room, context);
        
        Assert.assertNotNull(room.get_id());
        Assert.assertNotNull(door.get_id());
        Room getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertEquals(1, getRoom.getPath().size());
        Door getDoor = raptorService.get(door.get_id(), Door.class, context);
        Assert.assertNotNull(getDoor);
        Assert.assertEquals(getDoor.get_id(), getRoom.getPath().get(0).get_id());
    }

    @Test
    public void modifyToReplace_inner() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("third");
        room.addLevel("3");
        raptorService.create(room, context);
        
        Door door = new Door();
        door.setName("door-1");
        context.setPath(Room.class.getSimpleName(), room.get_id(), "path");
        raptorService.create(door, context);
        // asser creation
        Room getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertEquals(1, getRoom.getPath().size());
        Assert.assertNotNull(raptorService.get(door.get_id(), Door.class, context));

        // case 0 : modify would delete the old inner
        door = new Door();
        door.setName("new-door-2");
        getRoom.setPath(Arrays.asList(door));
        raptorService.update(getRoom, context);
        Assert.assertNotNull(door.get_id());
        getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertEquals(1, getRoom.getPath().size()); // still length as 1
        Door getDoor = raptorService.get(getRoom.getPath().get(0).get_id(), Door.class, context);
        Assert.assertEquals(door.getName(), getDoor.getName());

        // case 1: modify with old/new inner together should work as well
        CMSQuery query = new CMSQuery("Room[@_oid=\"" + getRoom.get_id() + "\"]{*}.path{*}");
        query.setAllowFullTableScan(true);
        getRoom = raptorService.query(query, Room.class, context).getEntities().get(0);
        getDoor = getRoom.getPath().get(0);
        String getDoorId = getDoor.get_id();
        getDoor.setName("new-door-2-name-updated");

        door = new Door();
        door.setName("new-door-3");
        // critical to call add here.
        getRoom.addFieldValue("path", door);
        raptorService.update(getRoom, context);
        // assertion
        getRoom = raptorService.query(query, Room.class, context).getEntities().get(0);
        Assert.assertEquals(2, getRoom.getPath().size());
        // old door is not deleted - compare by id
        Assert.assertEquals(getDoorId, getRoom.getPath().get(0).get_id());
        // old door is updated - compare the name value
        Assert.assertEquals(getDoor.getName(), getRoom.getPath().get(0).getName());
        // new door is added
        Assert.assertEquals(door.getName(), getRoom.getPath().get(1).getName());
    }

    @Test
    public void modifyToReplace_innerGeneric() {
        CMSClientContext context = new CMSClientContext();
        // create room
        GenericCMSEntity room = new GenericCMSEntity();
        room.set_type("Room");
        room.setFieldValue("floor", "third");
        room.setFieldValue("level", Arrays.asList("3"));
        raptorService.create(room, context);

        GenericCMSEntity door = new GenericCMSEntity();
        door.set_type("Door");
        door.setFieldValue("name", "door-1");
        context.setPath(Room.class.getSimpleName(), room.get_id(), "path");
        raptorService.create(door, context);
        // asser creation
        GenericCMSEntity getRoom = raptorService.get(room.get_id(), "Room", context);
        Assert.assertEquals(1, ((List<?>) getRoom.getFieldValue("path")).size());
        Assert.assertNotNull(raptorService.get(door.get_id(), "Door", context));

        // case 0 : modify would delete the old inner
        door = new GenericCMSEntity();
        door.set_type("Door");
        door.setFieldValue("name", "new-door-2");
        getRoom.setFieldValue("path", Arrays.asList(door));
        raptorService.update(getRoom, context);
        Assert.assertNotNull(door.get_id());

        getRoom = raptorService.get(room.get_id(), "Room", context);
        Assert.assertEquals(1, ((List<?>) getRoom.getFieldValue("path")).size()); // still length as 1
        GenericCMSEntity getDoor = raptorService.get(((List<GenericCMSEntity>) getRoom.getFieldValue("path")).get(0)
                .get_id(), "Door", context);
        Assert.assertEquals(door.getFieldValue("name"), getDoor.getFieldValue("name"));

        // case 1: modify with old/new inner together should work as well
        CMSQuery query = new CMSQuery("Room[@_oid=\"" + getRoom.get_id() + "\"]{*}.path{*}");
        query.setAllowFullTableScan(true);
        getRoom = raptorService.query(query, context).getEntities().get(0);
        getDoor = ((List<GenericCMSEntity>) getRoom.getFieldValue("path")).get(0);

        String getDoorId = getDoor.get_id();
        getDoor.setFieldValue("name", "new-door-2-name-updated");

        door = new GenericCMSEntity();
        door.set_type("Door");
        door.setFieldValue("name", "new-door-3");
        // critical to call add here.
        getRoom.addFieldValue("path", door);
        raptorService.update(getRoom, context);
        // assertion
        getRoom = raptorService.query(query, context).getEntities().get(0);
        Assert.assertEquals(2, ((List<GenericCMSEntity>)getRoom.getFieldValue("path")).size());
        // old door is not deleted - compare by id
        Assert.assertEquals(getDoorId, ((List<GenericCMSEntity>)getRoom.getFieldValue("path")).get(0).get_id());
        // old door is updated - compare the name value
        Assert.assertEquals(getDoor.getFieldValue("name"), ((List<GenericCMSEntity>)getRoom.getFieldValue("path")).get(0).getFieldValue("name"));
        // new door is added
        Assert.assertEquals(door.getFieldValue("name"), ((List<GenericCMSEntity>)getRoom.getFieldValue("path")).get(1).getFieldValue("name"));
    }

    @Test
    public void replaceToCreate_inner() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        raptorService.create(room, context);

        Door door = new Door();
        room.addPath(door);
        try {
            raptorService.replace(room, context);
            Assert.fail();
        } catch (CMSClientException cce) {
            // expected : not support replace class with inner fields
        }
    }

    @Test
    public void batchCreate() {
        ApplicationService serv1 = new ApplicationService();
        serv1.set_id("application_service_oid_" + System.currentTimeMillis());// batch
                                                                              // create
                                                                              // must
                                                                              // have
                                                                              // oid
                                                                              // set
        serv1.set_branch(IBranch.DEFAULT_BRANCH);
        serv1.set_type("ApplicationService");
        serv1.setName("firstServ" + System.currentTimeMillis() + random.nextDouble());
        serv1.setArchTier("firstArchTier");
        serv1.setAppId("firstAppId");
        serv1.setActiveManifestCur("firstActiveManifestCur");

        ApplicationService serv2 = new ApplicationService();
        serv2.set_id("application_service_oid_" + System.currentTimeMillis() + random.nextDouble());// batch
                                                                                                    // create
                                                                                                    // must
                                                                                                    // have
                                                                                                    // oid
                                                                                                    // set
        serv2.set_branch(IBranch.DEFAULT_BRANCH);
        serv2.set_type("ApplicationService");
        serv2.setName("secondServ" + System.currentTimeMillis() + random.nextDouble());
        serv2.setArchTier("secondArchTier");
        serv2.setAppId("secondAppId");
        serv2.setActiveManifestCur("secondActiveManifestCur");
        serv2.setArchitecture("newArchitecture");

        List<ApplicationService> appService = new ArrayList<ApplicationService>();
        appService.add(serv1);
        appService.add(serv2);

        List<String> createOids = raptorService.batchCreate(appService);
        for (String oid : createOids) {
            ApplicationService getAppServ = raptorService.get(oid, ApplicationService.class);
            Assert.assertNotNull(getAppServ);
        }
    }

    @Test(expected = NullPointerException.class)
    public void batchCreateNoEntities() {
        raptorService.batchCreate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void batchCreateEmptyEntities() {
        raptorService.batchCreate(new ArrayList<ApplicationService>());
    }

    @Test
    public void modify() throws IOException, URISyntaxException {
        ApplicationService createServ = createApplicationService();

        ApplicationService getServ = raptorService.get(createServ.get_id(), ApplicationService.class);

        ApplicationService newAppServ = new ApplicationService();
        newAppServ.set_branch(getServ.get_branch());
        newAppServ.set_id(getServ.get_id());
        newAppServ.set_version(getServ.get_version());
        newAppServ.set_pversion(getServ.get_pversion());
        newAppServ.setManifestCur("new Manifest");// updated field

        CMSClientContext context = new CMSClientContext();
        context.setAllowPartialWrite(true);
        raptorService.update(newAppServ, context);

        getServ = raptorService.get(newAppServ.get_id(), ApplicationService.class);
        Assert.assertEquals(newAppServ.getManifestCur(), getServ.getManifestCur());
        Assert.assertTrue(!getServ.getArchTier().equals(newAppServ.getArchTier()));
        Assert.assertTrue(getServ.getArchTier().equals(createServ.getArchTier()));
        
        // update with retry
        {
            newAppServ.setManifestCur("new Manifest - 2.0");// updated field
            raptorService.updateWithRetry(newAppServ, context);
            getServ = raptorService.get(newAppServ.get_id(), ApplicationService.class);
            Assert.assertEquals(newAppServ.getManifestCur(), getServ.getManifestCur());
            Assert.assertTrue(!getServ.getArchTier().equals(newAppServ.getArchTier()));
            Assert.assertTrue(getServ.getArchTier().equals(createServ.getArchTier()));
        }
    }

    @Test
    public void test_Query_dirtyMark() {
        CMSClientContext context = new CMSClientContext();
        // test the query then update case
        CMSQuery query = new CMSQuery("ServiceInstance{*}.services!ApplicationService{*}");
        query.setAllowFullTableScan(true);
        CMSQueryResult<ServiceInstance> results = raptorService.query(query, ServiceInstance.class, context);
        // find entities
        for (ServiceInstance entity : results.getEntities()) {
            List<ApplicationService> ass = (List<ApplicationService>)entity.getFieldValue("services!ApplicationService");
            for (ApplicationService as : ass) {
                Assert.assertEquals(2, as.getDirtyFields().size());
            }
        }
    }

    ApplicationService createApplicationService() throws IOException, URISyntaxException, JsonParseException,
            JsonMappingException {
        String json = Files.toString(new File(CMSClientServiceTest.class.getResource("/simpleCreate.json").toURI()),
                Charset.defaultCharset());

        ApplicationService appServ = mapper.readValue(json, ApplicationService.class);

        appServ.setName(appServ.getName() + System.currentTimeMillis() + random.nextDouble());
        appServ.set_branch(IBranch.DEFAULT_BRANCH);
        ApplicationService createServ = raptorService.create(appServ);
        Assert.assertNotNull(createServ.get_id());
        return createServ;
    }

    @Test
    public void modifyWithUserComment() throws IOException, URISyntaxException {
        CMSClientContext context = new CMSClientContext();
        String comment = "comment";
        String user = "user";
        context.setComment(comment);
        context.setUser(user);

        GenericCMSEntity entity = new GenericCMSEntity();
        String APPLICATION_SERVICE = "ApplicationService";
        entity.set_type(APPLICATION_SERVICE);
        entity.set_id("cms-entity-oid-001");
        entity.setFieldValue("name", "client-app-name-001");
        raptorService.create(entity, context);

        String newId = entity.get_id();
        // assert create user or comment
        GenericCMSEntity getEntity = raptorService.get(newId, APPLICATION_SERVICE, context);
        Assert.assertEquals(user, getEntity.getFieldValue("_user"));
        Assert.assertEquals(comment, getEntity.getFieldValue("_comment"));

        String modifyComment = "modify-comment";
        String modifyUser = "modify-user";
        context.setComment(modifyComment);
        context.setUser(modifyUser);
        getEntity.setFieldValue("name", "client-app-name-modify-001");
        raptorService.update(getEntity, context);
        getEntity = raptorService.get(newId, APPLICATION_SERVICE, context);
        Assert.assertEquals(modifyUser, getEntity.getFieldValue("_user"));
        Assert.assertEquals(modifyComment, getEntity.getFieldValue("_comment"));

        String replaceComment = "replace-comment";
        String replaceUser = "repalce-user";
        context.setComment(replaceComment);
        context.setUser(replaceUser);
        getEntity.setFieldValue("name", "client-app-name-replace-001");
        raptorService.replace(getEntity, context);
        getEntity = raptorService.get(newId, APPLICATION_SERVICE, context);
        Assert.assertEquals(replaceUser, getEntity.getFieldValue("_user"));
        Assert.assertEquals(replaceComment, getEntity.getFieldValue("_comment"));
    }

    @Test
    public void modifyRefercne() {
        ServiceInstance firstSi = newServiceInstance();
        firstSi = raptorService.create(firstSi);

        ServiceInstance secondSi = newServiceInstance();
        secondSi = raptorService.create(secondSi);

        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        ServiceInstance firstRefSi = new ServiceInstance();
        firstRefSi.set_id(firstSi.get_id());
        ServiceInstance secondRefSi = new ServiceInstance();
        secondRefSi.set_id(secondSi.get_id());

        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(firstRefSi);
        sis.add(secondRefSi);
        appServ.setServices(sis);
        ApplicationService createdServ = raptorService.create(appServ);

        ApplicationService modifyServ = new ApplicationService();
        modifyServ.set_id(createdServ.get_id());
        List<ServiceInstance> newRefSis = new ArrayList<ServiceInstance>();
        newRefSis.add(secondRefSi);
        modifyServ.setServices(newRefSis);// set the new reference
        raptorService.update(modifyServ);// modify reference

        ApplicationService getServ = raptorService.get(modifyServ.get_id(), ApplicationService.class);
        Assert.assertNotNull(getServ);
        Assert.assertTrue(getServ.getServices().size() == 1);
    }

    @Test
    public void modifySetReferenceArrayEmpty() {
        ServiceInstance si = newServiceInstance();

        ServiceInstance createdSi = raptorService.create(si);

        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        ServiceInstance refSi = new ServiceInstance();
        refSi.set_id(createdSi.get_id());
        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(si);
        appServ.setServices(sis);
        ApplicationService createdServ = raptorService.create(appServ);
        Assert.assertNotNull(createdServ.getServices());
        Assert.assertTrue(createdServ.getServices().size() == 1);

        ApplicationService modifyServ = new ApplicationService();
        modifyServ.set_id(createdServ.get_id());
        List<ServiceInstance> listVal = new ArrayList<ServiceInstance>();
        modifyServ.setServices(listVal);// clear the
                                        // reference
        raptorService.update(modifyServ);

        ApplicationService queryServ = new ApplicationService();
        queryServ.set_id(modifyServ.get_id());
        ApplicationService getServ = raptorService.get(modifyServ.get_id(), ApplicationService.class);
        Assert.assertNotNull(getServ);
        Assert.assertNotNull(getServ.getServices());
        Assert.assertTrue(getServ.getServices().size() == 0);
    }

    @Test
    public void modifySetReferenceAsNull() {
        // create a service with referencing compute
        Compute newComp = newCompute();
        Compute createdComp = raptorService.create(newComp);
        ServiceInstance newSi = newServiceInstance();
        newSi.setRunsOn(createdComp);
        ServiceInstance createSi = raptorService.create(newSi);
        ServiceInstance getSi = raptorService.get(createSi.get_id(), ServiceInstance.class);
        Assert.assertNotNull(getSi.getRunsOn());

        // modify
        getSi.setRunsOn(null);
        try {
            raptorService.update(getSi);
            Assert.fail();
        } catch (CMSClientException cce) {
            // expected
            Assert.assertEquals(400, cce.getHttpResponseCode());
        }
    }

    @Test
    public void modifyEmbedFromTop() throws Exception {
        // create entity first
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));
        ManifestVersion version = new ManifestVersion();
        version.setName(generateRandomName("nouse_name_"));
        version.setCreatedTime(new Date());
        version.setCreatedBy("liasu-creation");
        manifest.addVersions(version);
        Manifest createdMani = sdService.create(manifest);
        String oldName = version.getName();

        // verify creation
        Manifest getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani.getVersions().get(0).get_id());

        // modify - to modify from the top, the embed entities should based on
        // the get.
        Manifest modifyMani = new Manifest();
        modifyMani.set_id(createdMani.get_id());
        ManifestVersion modifyVersion = getMani.getVersions().get(0);
        modifyVersion.setName(generateRandomName("new no use name"));
        // make sure dirty bit
        modifyMani.addVersions(modifyVersion);

        sdService.update(modifyMani, new CMSClientContext());

        // verify
        getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani);
        Assert.assertNotNull(getMani.getVersions());
        Assert.assertEquals(1, getMani.getVersions().size());
        Assert.assertNotNull(getMani.getVersions().get(0).getName());
        Assert.assertNotNull(getMani.getVersions().get(0).get_id());
        Assert.assertTrue(!modifyVersion.getName().equals(oldName));
        System.out.println(mapper.writeValueAsString(getMani));
    }

    @Test
    public void modifyEmbedFromTop_dirtyCheck() {
        CMSClientContext context = new CMSClientContext();
        // create entity first
        Manifest manifest = newManifestWithVersion(globalSeq++);
        Manifest createdMani = sdService.create(manifest, context);

        String newCreatdeBy = "ralph-update";
        Manifest getMani = sdService.get(createdMani.get_id(), Manifest.class, context);

        Assert.assertNotNull(getMani.get_id());
        Assert.assertEquals(1, getMani.getVersions().size());
        for (ManifestVersion getV : getMani.getVersions()) {
            getV.setCreatedBy(newCreatdeBy);
        }
        // "versions" field not dirty. the verions would be modified 
        sdService.update(getMani, context);
        getMani = sdService.get(createdMani.get_id(), Manifest.class, context);
        // assert embed ManifestVersion's field not change.
        Assert.assertEquals(createdMani.getVersions().get(0).getCreatedBy(), getMani.getVersions().get(0).getCreatedBy());

        // make sure the dirty bits
        Assert.assertNotNull(getMani.get_id());
        Assert.assertEquals(1, getMani.getVersions().size());
        for (ManifestVersion getV : getMani.getVersions()) {
            getV.setCreatedBy(newCreatdeBy);
        }
        getMani.setVersions(getMani.getVersions());
        // update
        sdService.update(getMani, context);
        // verification
        getMani = sdService.get(createdMani.get_id(), Manifest.class, context);
        Assert.assertNotNull(getMani.get_id());
        Assert.assertEquals(1, getMani.getVersions().size());
        ManifestVersion getV = getMani.getVersions().get(0);
        // assert that only change the created by won't change other field like
        // create time
        Assert.assertEquals(newCreatdeBy, getV.getCreatedBy());
        Assert.assertNotNull(getV.getCreatedTime());
    }

    private Manifest newManifestWithVersion(int globalSeq) {
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));
        ManifestVersion version = newManifestVersion(globalSeq);
        manifest.addVersions(version);
        return manifest;
    }

    private static int globalSeq = 0;
//    private static IPyTest pyTest;
    private ManifestVersion newManifestVersion(int globalSeq) {
        ManifestVersion version = new ManifestVersion();
        version.setName(generateRandomName("version-001-name" + globalSeq));
        version.setCreatedBy(generateRandomName("liasu-creation" + globalSeq));
        version.setCreatedTime(new Date());
        return version;
    }

    @Test
    public void modify_embedParent_dirtyCheck() {
        CMSClientContext context = new CMSClientContext();
        // create entity first
        Manifest manifest = newManifestWithVersion(globalSeq++);
        sdService.create(manifest, context);
        // currently create with embed not able the get the embed id..
        Assert.assertNull(manifest.getVersions().get(0).get_id());

        manifest.setName("newName");
        sdService.update(manifest, context);
        Manifest getMani = sdService.get(manifest.get_id(), Manifest.class, context);
        Assert.assertNotNull(getMani.getVersions().get(0).get_id());
        Assert.assertEquals(1, getMani.getVersions().size());
        Assert.assertEquals("newName", getMani.getName());
    }

    @Test
    public void modify_toReplaceEmbed() {
        CMSClientContext context = new CMSClientContext();
        // create entity first
        Manifest manifest = newManifestWithVersion(globalSeq++);
        sdService.create(manifest, context);
        Manifest getMani = sdService.get(manifest.get_id(), Manifest.class, context);
        Assert.assertEquals(1, getMani.getVersions().size());
        String oldVersionId = getMani.getVersions().get(0).get_id();

        // case 0 : modify would delete the old embed version
        ManifestVersion version = newManifestVersion(globalSeq++);
        getMani.setVersions(Arrays.asList(version));
        sdService.update(getMani, context);
        getMani = sdService.get(manifest.get_id(), Manifest.class, context);
        Assert.assertEquals(1, getMani.getVersions().size());
        String newVersionId = getMani.getVersions().get(0).get_id();
        Assert.assertFalse(oldVersionId.equals(newVersionId));
        
        // case 1: modify with old/new version together should work as well
        getMani.addVersions(newManifestVersion(globalSeq++));
        sdService.update(getMani, context);
        getMani = sdService.get(manifest.get_id(), Manifest.class, context);
        Assert.assertEquals(2, getMani.getVersions().size());
        Assert.assertEquals(newVersionId, getMani.getVersions().get(0).get_id());
    }

    @Test
    public void modifyEmbedDirect() throws JsonGenerationException, JsonMappingException, IOException {
        // create entity first
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));
        ManifestVersion mv = new ManifestVersion();
        mv.setCreatedTime(new Date());
        Approval approval = new Approval();
        approval.setCreatedBy("unit test");
        approval.setManifestCreatedTime(new Date());
        approval.setCreatedTime(new Date());
        List<Approval> approvals = new ArrayList<Approval>();
        approvals.add(approval);
        mv.setApprovals(approvals);
        List<ManifestVersion> versions = new ArrayList<ManifestVersion>();
        versions.add(mv);
        manifest.setVersions(versions);

        Manifest createdMani = sdService.create(manifest);

        Manifest getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani.getVersions());
        Assert.assertTrue(getMani.getVersions().size() > 0);
        Assert.assertNotNull(getMani.getVersions().get(0).get_id());

        // modify
        ManifestVersion modifyMv = new ManifestVersion();
        modifyMv.set_branch(IBranch.DEFAULT_BRANCH);
        modifyMv.set_id(getMani.getVersions().get(0).get_id());
        modifyMv.setCreatedBy("update directly by unti test");

        sdService.update(modifyMv);

        // verify
        ManifestVersion getMv = sdService.get(modifyMv.get_id(), ManifestVersion.class);

        Assert.assertNotNull(getMv);
        Assert.assertNotNull(getMv.get_id());
        Assert.assertTrue(modifyMv.getCreatedBy().equals(getMv.getCreatedBy()));
        System.out.println(mapper.writeValueAsString(getMv));
    }

    @Test
    public void batchUpdate() throws Exception {
        ServiceInstance si = newServiceInstance();
        ServiceInstance createdSi = raptorService.create(si);
        ApplicationService appServ = newApplicationService(createdSi);
        ApplicationService createdServ = raptorService.create(appServ);

        createdSi.setActiveManifestDiff(false);
        createdSi.setPort("09090");
        createdSi.setActiveManifestCur("cur-batchupdate");

        createdServ.setServices(new ArrayList<ServiceInstance>());
        createdServ.setHealthStatus("now-set-active-batch-update");

        List<ICMSEntity> entities = new ArrayList<ICMSEntity>();
        entities.add(createdSi);
        entities.add(createdServ);
        CMSClientContext clientContext = new CMSClientContext();
        clientContext.setBatchFailOption(BatchOperationFailReturnOption.ALL);
        raptorService.batchUpdate(entities, clientContext);

        ServiceInstance getSi = raptorService.get(createdSi.get_id(), ServiceInstance.class);
        Assert.assertEquals(createdSi.getPort(), getSi.getPort());
        Assert.assertEquals(createdSi.getActiveManifestCur(), getSi.getActiveManifestCur());

        ApplicationService getApp = raptorService.get(createdServ.get_id(), ApplicationService.class);
        Assert.assertEquals(createdServ.getHealthStatus(), getApp.getHealthStatus());
        Assert.assertNotNull(getApp.getServices());
        Assert.assertEquals(0, getApp.getServices().size());
    }
    
    @Test
    public void batchDelete() throws Exception {
    	ApplicationService serv1 = new ApplicationService();
        serv1.set_id("application_service_oid_" + System.currentTimeMillis() + random.nextDouble());
        serv1.set_branch(IBranch.DEFAULT_BRANCH);
        serv1.set_type("ApplicationService");
        serv1.setName("firstServ" + System.currentTimeMillis() + random.nextDouble());
        serv1.setArchTier("firstArchTier");
        serv1.setAppId("firstAppId");
        serv1.setActiveManifestCur("firstActiveManifestCur");

        ApplicationService serv2 = new ApplicationService();
        serv2.set_id("application_service_oid_" + System.currentTimeMillis() + random.nextDouble());
        serv2.set_branch(IBranch.DEFAULT_BRANCH);
        serv2.set_type("ApplicationService");
        serv2.setName("secondServ" + System.currentTimeMillis() + random.nextDouble());
        serv2.setArchTier("secondArchTier");
        serv2.setAppId("secondAppId");
        serv2.setActiveManifestCur("secondActiveManifestCur");
        serv2.setArchitecture("newArchitecture");

        List<ApplicationService> appService = new ArrayList<ApplicationService>();
        appService.add(serv1);
        appService.add(serv2);

        List<String> createOids = raptorService.batchCreate(appService);
        appService = new ArrayList<ApplicationService>();
        for (String oid : createOids) {
            ApplicationService getAppServ = raptorService.get(oid, ApplicationService.class);
            Assert.assertNotNull(getAppServ);
            appService.add(getAppServ);
        }
        raptorService.batchDelete(appService);
        for (String oid : createOids) {
            ApplicationService getAppServ = raptorService.get(oid, ApplicationService.class);
            Assert.assertNull(getAppServ);
        }
    }

    private ApplicationService newApplicationService(ServiceInstance createdSi) {
        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        ServiceInstance refSi = new ServiceInstance();
        refSi.set_id(createdSi.get_id());
        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(createdSi);
        appServ.setServices(sis);
        return appServ;
    }

    private ServiceInstance newServiceInstance() {
        ServiceInstance si = new ServiceInstance();
        si.setName(generateRandomName("si_name_"));
        return si;
    }

    private Compute newCompute() {
        Compute c = new Compute();
        c.setName(generateRandomName("compute_name_"));
        return c;
    }

    @Test
    public void replace() throws IOException, URISyntaxException {
        ApplicationService createServ = createApplicationService();
        System.out.println(createServ.get_id());

        ApplicationService getApp = raptorService.get(createServ.get_id(), ApplicationService.class);

        ApplicationService newAppServ = new ApplicationService();
        newAppServ.setName(getApp.getName());
        newAppServ.set_branch(getApp.get_branch());
        newAppServ.set_id(getApp.get_id());
        newAppServ.set_version(getApp.get_version());
        newAppServ.set_pversion(getApp.get_pversion());
        newAppServ.setManifestCur("new Manifest");// replace field

        raptorService.replace(newAppServ);

        ApplicationService getServ = raptorService.get(createServ.get_id(), ApplicationService.class);
        Assert.assertEquals(newAppServ.getManifestCur(), getServ.getManifestCur());
        Assert.assertNull(getServ.getArchTier());
        Assert.assertTrue(!createServ.getArchTier().equals(getServ.getArchTier()));
    }

    @Test
    public void replaceEmbed() throws JsonGenerationException, JsonMappingException, IOException {

        // create entity first
        Manifest manifest = new Manifest();
        manifest.setName(generateRandomName("manifest_name_"));
        NoUse noUses = new NoUse();
        noUses.setName(generateRandomName("nouse_name_"));
        manifest.setNoUses(noUses);
        Manifest createdMani = sdService.create(manifest);

        // verify creation
        Manifest getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani.getNoUses().get_id());

        // replace
        Manifest modifyMani = new Manifest();
        modifyMani.set_id(createdMani.get_id());
        NoUse modifyNoUse = getMani.getNoUses();
        modifyNoUse.setName("new no use name");
        modifyMani.setNoUses(modifyNoUse);

        sdService.replace(modifyMani);

        // verify
        getMani = sdService.get(createdMani.get_id(), Manifest.class);
        Assert.assertNotNull(getMani);
        Assert.assertNotNull(getMani.getNoUses());
        Assert.assertNotNull(getMani.getNoUses().getName());
        Assert.assertNotNull(getMani.getNoUses().get_id());
        Assert.assertTrue(!noUses.getName().equals(getMani.getNoUses().getName()));
        System.out.println(mapper.writeValueAsString(getMani));

    }

    @Test
    public void replaceReference() {
        ServiceInstance firstSi = newServiceInstance();
        firstSi = raptorService.create(firstSi);

        ServiceInstance secondSi = newServiceInstance();
        secondSi = raptorService.create(secondSi);

        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        ServiceInstance firstRefSi = new ServiceInstance();
        firstRefSi.set_id(firstSi.get_id());
        ServiceInstance secondRefSi = new ServiceInstance();
        secondRefSi.set_id(secondSi.get_id());

        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(firstRefSi);
        sis.add(secondRefSi);
        appServ.setServices(sis);
        ApplicationService createdServ = raptorService.create(appServ);

        ApplicationService replaceServ = new ApplicationService();
        replaceServ.set_id(createdServ.get_id());
        List<ServiceInstance> newRefSis = new ArrayList<ServiceInstance>();
        newRefSis.add(secondRefSi);
        replaceServ.setServices(newRefSis);// set the new reference
        raptorService.replace(replaceServ);

        ApplicationService getServ = raptorService.get(replaceServ.get_id(), ApplicationService.class);
        Assert.assertNotNull(getServ);
        Assert.assertTrue(getServ.getServices().size() == 1);
    }

    @Test
    public void replaceClearRef() {
        // create app service and refernced service instance
        ServiceInstance si = new ServiceInstance();
        si.setName(generateRandomName("si_namex_"));

        ServiceInstance createdSi = raptorService.create(si);

        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_namex_"));
        ServiceInstance refSi = new ServiceInstance();
        refSi.set_id(createdSi.get_id());
        List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
        sis.add(si);
        appServ.setServices(sis);
        ApplicationService createdServ = raptorService.create(appServ);

        ApplicationService modifyServ = new ApplicationService();
        modifyServ.set_id(createdServ.get_id());
        modifyServ.setName(appServ.getName());
        modifyServ.setServices(new ArrayList<ServiceInstance>());// clear the
                                                                 // reference
        raptorService.replace(modifyServ); // replace entity

        ApplicationService getServ = raptorService.get(modifyServ.get_id(), ApplicationService.class);
        Assert.assertNotNull(getServ);
        Assert.assertNotNull(getServ.getServices());
        Assert.assertTrue(getServ.getServices().size() == 0);
    }

    @Test
    public void delete() throws IOException, URISyntaxException {
        ApplicationService createServ = createApplicationService();
        raptorService.delete(createServ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNoOid() {
        ApplicationService deleteServ = new ApplicationService();
        raptorService.delete(deleteServ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void modifyNoOid() {
        ApplicationService deleteServ = new ApplicationService();
        raptorService.update(deleteServ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceNoOid() {
        ApplicationService deleteServ = new ApplicationService();
        raptorService.replace(deleteServ);
    }

    @Test
    public void deleteNotExistingId() {
        ApplicationService deleteServ = new ApplicationService();
        deleteServ.set_id("invalid-oid");
        raptorService.delete(deleteServ);
        GenericCMSEntity entity = raptorService.get(deleteServ.get_id(), deleteServ.get_type(), new CMSClientContext());
        Assert.assertNull(entity);
    }

    @Test
    public void notExistingMeta() {
        try {
            raptorService.get(DummyCMSEntity.class, 0);
            Assert.fail();
        } catch (CMSClientException e) {
            e.printStackTrace();
            // Assert.assertEquals(500, e.getHttpResponseCode());
        }

    }

    @Test
    public void nonMainBranch() {
        // we don't have non-main branch for now
    }

    @Test
    public void createMissingMandatory() {
        UpdateStrategy us = new UpdateStrategy();
        try {
            raptorService.create(us);
            Assert.fail();
        } catch (CMSClientException e) {
            e.printStackTrace();
            Assert.assertEquals(500, e.getHttpResponseCode());
        }
    }

    @Test
    public void replaceMissingMandatory() {
        UpdateStrategy us = new UpdateStrategy();
        us.setName("us_name_" + System.currentTimeMillis() + random.nextDouble());
        us.setLastModifiedTime(new Date());

        UpdateStrategy createUs = raptorService.create(us);

        UpdateStrategy replaceUs = new UpdateStrategy();
        replaceUs.setName(us.getName());
        replaceUs.set_branch(IBranch.DEFAULT_BRANCH);
        replaceUs.set_id(createUs.get_id());
        try {
            raptorService.replace(replaceUs);
            Assert.fail();
        } catch (CMSClientException e) {
            e.printStackTrace();
            Assert.assertEquals(500, e.getHttpResponseCode());
        }
    }

    @Test
    public void updateNoBranchId() {
        UpdateStrategy us = new UpdateStrategy();
        us.setName("us_name_" + System.currentTimeMillis() + random.nextDouble());
        us.setLastModifiedTime(new Date());

        UpdateStrategy createUs = raptorService.create(us);

        UpdateStrategy modifyUs = new UpdateStrategy();
        modifyUs.setName(us.getName());
        modifyUs.set_id(createUs.get_id());
        raptorService.update(modifyUs);
    }

    // ////////////////////////////////////////
    // ////////////////////////////////////////
    // ////////////////////////////////////////
    // ////////////////////////////////////////
    // //////////////////////////////////////// test cases for generic apis
    // ////////////////////////////////////////
    // ////////////////////////////////////////
    // ////////////////////////////////////////
    // ////////////////////////////////////////

    @Test
    public void testGenericGet() {
        GenericCMSEntity genericEntity = raptorService.get("4fbb314fc681caf13e283a76", "ApplicationService");
        Assert.assertNotNull(genericEntity);
        Assert.assertEquals(genericEntity.get_metaclass(), genericEntity.get_type());
        Assert.assertEquals("ApplicationService", (genericEntity.get_metaclass()));
    }

    @Test
    public void testGenericGetMissingParam() {
        try {
            raptorService.get("4fbb314fc681caf13e283a76", (String) null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expect
        }

        try {
            raptorService.get((String) null, "ApplicationService");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // expect
        }
    }

    @Test
    public void testGenericBatchGet() {
        GenericCMSEntity cmsEntity = new GenericCMSEntity();
        cmsEntity.set_metaclass("ApplicationService");
        List<GenericCMSEntity> genericEntities = raptorService.get(cmsEntity, 100);
        Assert.assertNotNull(genericEntities);
        Assert.assertTrue(genericEntities.size() > 0);
        for (GenericCMSEntity gm : genericEntities) {
            Assert.assertNotNull(gm);
            Assert.assertNotNull(gm.get_metaclass());
            Assert.assertTrue("ApplicationService".equals(gm.get_metaclass()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenericBatchGetMissingMeta() {
        GenericCMSEntity cmsEntity = new GenericCMSEntity();
        raptorService.get(cmsEntity, 100);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGenericCreate() throws JsonGenerationException, JsonMappingException, IOException {
        GenericCMSEntity genericEntity = createGenericAppServ();
        GenericCMSEntity createEntity = raptorService.create(genericEntity);
        Assert.assertNotNull(createEntity.get_id());

        ApplicationService getAppServ = raptorService.get(createEntity.get_id(), ApplicationService.class);
        Assert.assertNotNull(getAppServ);
        System.out.println(mapper.writeValueAsString(getAppServ));
        Assert.assertEquals(genericEntity.getFieldValue("nugget"), getAppServ.getNugget());
        Assert.assertNotNull(getAppServ.getActiveManifestRef());
        Assert.assertTrue(getAppServ.getActiveManifestRef().size() > 0);

        Assert.assertEquals(((List<String>) genericEntity.getFieldValue("activeManifestRef")).size(), getAppServ
                .getActiveManifestRef().size());
        for (int i = 0; i < getAppServ.getActiveManifestRef().size(); i++) {
            Assert.assertEquals(((List<String>) genericEntity.getFieldValue("activeManifestRef")).get(i), getAppServ
                    .getActiveManifestRef().get(i));
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGenericCreateWithReference() {
        GenericCMSEntity siEntity = createGenericServiceInstance();
        GenericCMSEntity createSi = raptorService.create(siEntity);
        Assert.assertNotNull(createSi.get_id());

        GenericCMSEntity getSi = raptorService.get(createSi.get_id(), "ServiceInstance");
        Assert.assertNotNull(getSi);

        GenericCMSEntity appServEntity = createGenericAppServ();
        List<GenericCMSEntity> sis = new ArrayList<GenericCMSEntity>();
        sis.add(getSi);
        appServEntity.setFieldValue("services", sis);

        GenericCMSEntity createAppServ = raptorService.create(appServEntity);
        Assert.assertNotNull(createAppServ.get_id());

        GenericCMSEntity getAppServ = raptorService.get(createAppServ.get_id(), "ApplicationService");
        Assert.assertEquals(appServEntity.getFieldValue("name"), getAppServ.getFieldValue("name"));
        Assert.assertNotNull(getAppServ.getFieldValue("services"));
        Assert.assertTrue(((List) getAppServ.getFieldValue("services")).size() == 1);
        GenericCMSEntity getSiInApp = (GenericCMSEntity) ((List) getAppServ.getFieldValue("services")).get(0);
        Assert.assertEquals(createSi.get_id(), getSiInApp.get_id());
        Assert.assertNull(getSiInApp.getFieldValue("healthStatus"));
        Assert.assertNull(getSiInApp.getFieldValue("manifestDiff"));
        Assert.assertNull(getSiInApp.getFieldValue("https"));
        Assert.assertNull(getSiInApp.getFieldValue("port"));
    }

    private GenericCMSEntity createGenericAppServ() {
        GenericCMSEntity appServEntity = new GenericCMSEntity();
        appServEntity.set_metaclass("ApplicationService");
        appServEntity.setFieldValue("name", generateRandomName("generic-create"));
        appServEntity.setFieldValue("nugget", "srp-generic");
        appServEntity.setFieldValue("label", "label-generic");
        List<String> activeRefs = new ArrayList<String>();
        activeRefs.add("activeRef1-generic");
        activeRefs.add("activeRef2-generic");
        appServEntity.setFieldValue("activeManifestRef", activeRefs);
        return appServEntity;
    }

    private GenericCMSEntity createGenericServiceInstance() {
        GenericCMSEntity siEntity = new GenericCMSEntity();
        siEntity.set_metaclass("ServiceInstance");
        siEntity.setFieldValue("name", generateRandomName("si-generic"));
        siEntity.setFieldValue("healthStatus", "healthy");
        siEntity.setFieldValue("manifestDiff", false);
        siEntity.setFieldValue("https", true);
        siEntity.setFieldValue("port", "080");
        return siEntity;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGenericModifyWithReference() {
        // create one app service with one service instance
        GenericCMSEntity siEntity = createGenericServiceInstance();
        GenericCMSEntity createSi = raptorService.create(siEntity);
        GenericCMSEntity getSi = raptorService.get(createSi.get_id(), "ServiceInstance");
        GenericCMSEntity appServEntity = createGenericAppServ();
        List<GenericCMSEntity> sis = new ArrayList<GenericCMSEntity>();
        sis.add(getSi);
        appServEntity.setFieldValue("services", sis);
        GenericCMSEntity createAppServ = raptorService.create(appServEntity);

        // create another service instance, add to existing app service
        GenericCMSEntity siEntity2 = createGenericServiceInstance();
        GenericCMSEntity createSi2 = raptorService.create(siEntity2);
        GenericCMSEntity getSi2 = raptorService.get(createSi2.get_id(), "ServiceInstance");
        GenericCMSEntity getAppServ = raptorService.get(createAppServ.get_id(), "ApplicationService");
        List<GenericCMSEntity> servicesInApp = (List<GenericCMSEntity>) getAppServ.getFieldValue("services");
        Assert.assertEquals(1, servicesInApp.size());
        // modify the service
        getAppServ.addFieldValue("services", getSi2);
        raptorService.update(getAppServ);

        // validation
        getAppServ = raptorService.get(getAppServ.get_id(), "ApplicationService");
        Assert.assertNotNull(getAppServ);
        Assert.assertNotNull(((List<GenericCMSEntity>) getAppServ.getFieldValue("services")));
        Assert.assertTrue(((List<GenericCMSEntity>) getAppServ.getFieldValue("services")).size() == 2);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testGenericBatchUpdate() {
        // create instance
        GenericCMSEntity siEntity = createGenericServiceInstance();
        GenericCMSEntity appEntity = createGenericAppServ();
        appEntity.addFieldValue("services", siEntity);
        GenericCMSEntity createdSiEntity = raptorService.create(siEntity);
        GenericCMSEntity createdAppEntity = raptorService.create(appEntity);

        // update fields
        createdSiEntity.setFieldValue("port", "9191");
        // update to clear reference
        createdAppEntity.setFieldValue("services", new ArrayList());
        createdAppEntity.setFieldValue("name", generateRandomName("generic-batch-update"));

        List<ICMSEntity> entities = new ArrayList<ICMSEntity>();
        entities.add(createdAppEntity);
        entities.add(createdSiEntity);
        raptorService.batchUpdate(entities);

        GenericCMSEntity getSi = raptorService.get(createdSiEntity.get_id(), "ServiceInstance");
        Assert.assertEquals(createdSiEntity.getFieldValue("port"), getSi.getFieldValue("port"));

        GenericCMSEntity getApp = raptorService.get(createdAppEntity.get_id(), "ApplicationService");
        Assert.assertNotNull(getApp.getFieldValue("services"));
        Assert.assertEquals(0, ((List<?>) getApp.getFieldValue("services")).size());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testGenericReplaceWithReference() {
        // create one app service with one service instance
        GenericCMSEntity siEntity = createGenericServiceInstance();
        GenericCMSEntity createSi = raptorService.create(siEntity);
        GenericCMSEntity getSi = raptorService.get(createSi.get_id(), "ServiceInstance");
        GenericCMSEntity appServEntity = createGenericAppServ();
        List<GenericCMSEntity> sis = new ArrayList<GenericCMSEntity>();
        sis.add(getSi);
        appServEntity.setFieldValue("services", sis);
        GenericCMSEntity createAppServ = raptorService.create(appServEntity);

        GenericCMSEntity getAppServ = raptorService.get(createAppServ.get_id(), "ApplicationService");
        Assert.assertEquals(1, ((List) getAppServ.getFieldValue("services")).size());
        getAppServ.setFieldValue("services", new ArrayList());// clear reference
                                                              // to empty

        raptorService.replace(getAppServ);

        getAppServ = raptorService.get(createAppServ.get_id(), "ApplicationService");
        Assert.assertNotNull(getAppServ.getFieldValue("services"));
        Assert.assertEquals(0, ((List<?>) getAppServ.getFieldValue("services")).size());
    }

    @Test
    public void testGenericDelete() {
        GenericCMSEntity siEntity = createGenericServiceInstance();
        GenericCMSEntity createSi = raptorService.create(siEntity);
        Assert.assertNotNull(raptorService.get(createSi.get_id(), "ServiceInstance"));

        GenericCMSEntity deleteEntity = new GenericCMSEntity();
        deleteEntity.set_metaclass("ServiceInstance");
        deleteEntity.set_id(createSi.get_id());
        raptorService.delete(deleteEntity);
        Assert.assertNull(raptorService.get(createSi.get_id(), "ServiceInstance"));
    }

    @Test
    public void testGenericGetInvalidRepo() {
        GenericCMSEntity cmsEntity = new GenericCMSEntity();
        cmsEntity.set_metaclass("ApplicationService");
        cmsEntity.set_repo("invalid-repo");
        try {
            raptorService.get(cmsEntity, 100);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
        }
    }

    @Test
    public void testGenericGetInvalidBranch() {
        GenericCMSEntity cmsEntity = new GenericCMSEntity();
        cmsEntity.set_metaclass("ApplicationService");
        cmsEntity.set_branch("invalid-branch");
        try {
            raptorService.get(cmsEntity, 100);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
        }
    }

    @Test
    public void queryGenericQuery() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, "ApplicationService{*}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        Assert.assertEquals("ApplicationService", (queriedEntities.get(0).get_metaclass()));
    }

    @Test
    public void queryGenericQuery2() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, "ApplicationService.services{*}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        Assert.assertEquals("ServiceInstance", (queriedEntities.get(0).get_metaclass()));
    }

    @Test
    public void queryGenericQuery3() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, "ApplicationService.services.runsOn");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        Assert.assertEquals("Compute", (queriedEntities.get(0).get_metaclass()));
    }

    @Test
    public void queryInnerFieldWithGeneric() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null,
                "ApplicationService[@updateStrategies.$_length >= 1]{@updateStrategies.$_length, @updateStrategies.$_lastmodified}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        // after cms server remove the _type from default join query result,
        // client not able to detect the right
        // entity class in generic case. See Jian's change in QueryTranslator of
        // cms-core code.
        // Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        // Assert.assertEquals("ApplicationService",
        // (queriedEntities.get(0).get_metaclass()));

        GenericCMSEntity cms = queriedEntities.get(0);
        Assert.assertNotNull(cms.getFieldValue("updateStrategies._length"));
        Assert.assertTrue(Integer.class.isInstance(cms.getFieldValue("updateStrategies._length")));

        Assert.assertNotNull(cms.getFieldValue("updateStrategies._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(cms.getFieldValue("updateStrategies._lastmodified")));

        // access using date field should also work
        Assert.assertNotNull(cms.getDateField("updateStrategies._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(cms.getDateField("updateStrategies._lastmodified")));
        Assert.assertEquals(cms.getFieldValue("updateStrategies._lastmodified"),
                cms.getDateField("updateStrategies._lastmodified"));
    }

    @Test
    public void queryInnerFieldWithTypeHint() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null,
                "ApplicationService[@updateStrategies.$_length > 1]{@updateStrategies.$_length, @updateStrategies.$_lastmodified}");
        query.setAllowFullTableScan(true);
        List<ApplicationService> queriedEntities = raptorService.query(query, ApplicationService.class).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        Assert.assertEquals("ApplicationService", (queriedEntities.get(0).get_metaclass()));
        GenericCMSEntity cms = queriedEntities.get(0);
        Assert.assertNotNull(cms.getFieldValue("updateStrategies._length"));
        Assert.assertTrue(Integer.class.isInstance(cms.getFieldValue("updateStrategies._length")));

        Assert.assertNotNull(cms.getFieldValue("updateStrategies._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(cms.getFieldValue("updateStrategies._lastmodified")));

        // access using date field should also work
        Assert.assertNotNull(cms.getDateField("updateStrategies._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(cms.getDateField("updateStrategies._lastmodified")));
        Assert.assertEquals(cms.getFieldValue("updateStrategies._lastmodified"),
                cms.getDateField("updateStrategies._lastmodified"));
    }

    @Test
    public void queryInnerFieldInReferenceGeneric() {
        CMSQuery query = new CMSQuery(
                RAPTOR_PAAS,
                null,
                "ApplicationService{@services.$_length}.services[@runsOn.$_lastmodified > date(123)] {@runsOn.$_lastmodified}.runsOn{*}");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        // after cms server remove the _type from default join query result,
        // client not able to detect the right
        // entity class in generic case. See Jian's change in QueryTranslator of
        // cms-core code.
        // Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        // Assert.assertEquals("ApplicationService",
        // (queriedEntities.get(0).get_metaclass()));
        GenericCMSEntity cms = queriedEntities.get(0);
        Assert.assertTrue(Integer.class.isInstance(cms.getFieldValue("services._length")));

        @SuppressWarnings("unchecked")
        List<GenericCMSEntity> services = (List<GenericCMSEntity>) cms.getFieldValue("services");
        GenericCMSEntity service = services.get(0);
        Assert.assertNotNull(service.getFieldValue("runsOn._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(service.getFieldValue("runsOn._lastmodified")));
        Assert.assertNotNull(service.getFieldValue("runsOn"));
    }

    @Test
    public void queryInnerFieldInReferenceWithTypeHint() {
        CMSQuery query = new CMSQuery(
                RAPTOR_PAAS,
                null,
                "ApplicationService{@services.$_length}.services[@runsOn.$_lastmodified > date(123)] {@runsOn.$_lastmodified}.runsOn{*}");
        query.setAllowFullTableScan(true);
        List<ApplicationService> queriedEntities = raptorService.query(query, ApplicationService.class).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).get_metaclass());
        Assert.assertEquals("ApplicationService", (queriedEntities.get(0).get_metaclass()));
        ApplicationService cms = queriedEntities.get(0);
        Assert.assertTrue(Integer.class.isInstance(cms.getFieldValue("services._length")));

        @SuppressWarnings("unchecked")
        List<GenericCMSEntity> services = (List<GenericCMSEntity>) cms.getFieldValue("services");
        GenericCMSEntity service = services.get(0);
        Assert.assertNotNull(service.getFieldValue("runsOn._lastmodified"));
        Assert.assertTrue(Date.class.isInstance(service.getFieldValue("runsOn._lastmodified")));
        Assert.assertNotNull(service.getFieldValue("runsOn"));
        System.out.println(service);
    }

    @Test
    public void queryPagination01() {
        CMSQuery query = new CMSQuery("ServiceInstance");
        query.setAllowFullTableScan(true);
        query.setSkips(new long[] { 0 });
        query.setLimits(new long[] { 5 });
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertTrue(result.isHasMore());
        Assert.assertEquals(query.getLimits()[0], result.getCount().longValue());
        Assert.assertEquals(query.getLimits()[0], result.getEntities().size());

        QueryIterator<ServiceInstance> it = raptorService.queryIterator(query, ServiceInstance.class,
                new CMSClientContext());
        long count = 0;
        while (it.hasNext()) {
            ServiceInstance si = it.next();
            Assert.assertNotNull(si);
            count++;
        }
        Assert.assertEquals(count, it.getTotalCount());

        CMSQuery countQuery = new CMSQuery("ServiceInstance");
        countQuery.setAllowFullTableScan(true);
        CMSQueryResult<ServiceInstance> result2 = raptorService.query(countQuery, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertEquals(it.getTotalCount(), result2.getCount().longValue());
    }

    @Ignore
    @Test
    public void queryPagination02() {
        createServiceInstanceData("queryPagination02", 1050);

        CMSQuery query = new CMSQuery("ServiceInstance[@name=~\"^queryPagination02\"]{@_oid}.runsOn");
        query.setAllowFullTableScan(true);
        query.setHint(0);
        query.setLimits(new long[] { 1000 });
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertFalse(result.isHasMore());

        QueryIterator<ServiceInstance> iterator = raptorService.queryIterator(query, ServiceInstance.class,
                new CMSClientContext());
        int count = 0;
        while (iterator.hasNext()) {
            ServiceInstance si = iterator.next();
            if (si == null) {
                break;
            }
            count++;
        }
        Assert.assertEquals(0, count);
        Assert.assertEquals(0, iterator.getTotalCount());
        Assert.assertEquals(1, iterator.getRequestNum());
    }

    private void createServiceInstanceData(String name, int num) {
        for (int i = 0; i < num; i++) {
            ServiceInstance si = createServiceInstance(name);
            raptorService.create(si, null);
        }
    }

    @Test
    public void queryPagination04() {
        createServiceInstanceData("queryPagination03", 1050);

        CMSQuery query = new CMSQuery("ServiceInstance[@name=~\"queryPagination03.*\"].runsOn");
        query.setAllowFullTableScan(true);
        query.setPaginationMode(PaginationEnum.SKIP_BASED);
        query.setSkips(new long[] { 51 });
        query.setLimits(new long[] { 1000 });
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertTrue(!result.isHasMore());
    }

    @Test
    public void queryPagination_maxfetch_sub() {
        String queryString = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setMaxFetch(5);
        try {
            CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class,
                    new CMSClientContext());
            Assert.fail();
            Assert.assertTrue(!result.isHasMore());
        } catch (CMSClientException ce) {
            // expected
            Assert.assertEquals(CMSErrorCodeEnum.INCOMPLETE_JOIN_QUERY, ce.getCMSErrorCodeEnum());
        }
    }

    @Test
    public void queryPagination_maxfetch() {
        String queryString = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setMaxFetch(100);
        query.setSortOn(Arrays.asList("_oid", "name"));
        query.setSortOrder(Arrays.asList("asc", "desc"));
        query.setLimits(new long[] { 1 });
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertTrue(result.isHasMore());
        Assert.assertNotNull(result.getMaxFetch());
        Assert.assertNotNull(result.getSortOn());
        Assert.assertEquals(2, result.getSortOn().size());
        Assert.assertNotNull(result.getSortOrder());
        Assert.assertEquals(2, result.getSortOrder().size());
    }

    @Test
    public void testPagination_fixSize() {
        int limit = 100;
        createServiceInstanceData("testPagination_fixSize", limit + 1);
        CMSQuery query = new CMSQuery("ServiceInstance[@name=~\"^testPagination_fixSize.*\"]{@_oid}");
        query.setLimits(new long[]{limit});
        QueryIterator<ServiceInstance> iterator = raptorService.queryIterator(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(1, iterator.getRequestNum());
        List<ServiceInstance> nextPage = iterator.getNextPage(limit, 10);
        Assert.assertEquals(1, nextPage.size());
        Assert.assertEquals(2, iterator.getRequestNum());
    }

    @Test
    public void testPagination_getAll() {
        int limit = 100;
        createServiceInstanceData("testPagination_getAll", limit + 1);
        CMSQuery query = new CMSQuery("ServiceInstance[@name=~\"^testPagination_getAll.*\"]{@_oid}");
        query.setLimits(new long[]{limit});
        QueryIterator<ServiceInstance> iterator = raptorService.queryIterator(query, ServiceInstance.class,
                new CMSClientContext());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(1, iterator.getRequestNum());
        List<ServiceInstance> nextPage = iterator.getRemaining();
        Assert.assertEquals(limit + 1, nextPage.size());
        Assert.assertEquals(2, iterator.getRequestNum());
    }

    @Test
    public void testPagination_singleClass_BYID() {
        final int LIMIT = 100;
        createServiceInstanceData("test_single_pagiantion_BYID", LIMIT + 1);
        CMSQuery query = new CMSQuery("ServiceInstance[@name=~\"^test_single_pagiantion_BYID.*\"]");
        final int FIX_PAGE = 10;
        int fetchCount = 0;
        // case 0 : use server returned next page size
        query.addSortOn("name");
        query.setLimits(new long[] { FIX_PAGE });
        query.setPaginationMode(PaginationEnum.ID_BASED);
        CMSClientContext context = new CMSClientContext();
        QueryIterator<ServiceInstance> it = raptorService.queryIterator(query, ServiceInstance.class, context);
        Set<String> idSet = new HashSet<String>();
        List<String> dupIds = new ArrayList<String>();
        while (it.hasNext()) {
            fetchCount++;
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            String entityId = entity.get_id();
            // should not be duplicated
            if (idSet.contains(entityId)) {
                dupIds.add(entityId);
            } else {
                idSet.add(entityId);
            }
        }
        Assert.assertEquals(fetchCount, it.getTotalCount());
        Assert.assertEquals(fetchCount, idSet.size());
        Assert.assertEquals(LIMIT + 1, fetchCount);
        Assert.assertEquals(11, it.getRequestNum());
        Assert.assertEquals(0, dupIds.size());

        // case 1 : use fix pagination in client side for single collection
        fetchCount = 0;
        int requestCount = 0;
        query.setLimits(new long[] { FIX_PAGE });
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class, context);
        requestCount++;
        fetchCount += result.getEntities().size();
        while (result.isHasMore()) {
            query.setLimits(new long[] { FIX_PAGE });
            query.setCursor(result.getCursor());
            result = raptorService.query(query, ServiceInstance.class, context);
            requestCount++;
            fetchCount += result.getEntities().size();
        }
        Assert.assertEquals(11, requestCount);
        Assert.assertEquals(LIMIT + 1, fetchCount);
    }
    
    @Test
    public void testJythonQueryTestSuites() {
        // jython test case assumption, local server is started
//        Assert.assertNotNull(pyTest);
//        pyTest.run();
    }

    @Test
    public void testQuerySubQuery() {
        String queryString = "ServiceInstance[ not @runsOn =& Compute{@_oid} ]{*}";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setMaxFetch(100);
        query.setCountOnly(true);
        CMSClientContext context = new CMSClientContext();
        CMSQueryResult<ServiceInstance> result = raptorService.query(query, ServiceInstance.class, context);
        Assert.assertTrue(result.getCount().longValue() > 0);
        Assert.assertTrue(result.getEntities().isEmpty());
        checkContextResponseOk(context);
    }

    @Test
    public void queryIteration01_join() {
        String queryString = "VPool[exists @environment]{*}.computes[@fqdns=~\".*.com\"]";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setPaginationMode(PaginationEnum.SKIP_BASED);
        query.setLimits(new long[] { 1, 2 });
        CMSQueryResult<GenericCMSEntity> result = stratusService.query(query, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertTrue(result.isHasMore());
        Assert.assertEquals(0, result.getHint());
        Assert.assertEquals(2, result.getLimits().length);
        Assert.assertEquals(2, result.getSkips().length);

        QueryIterator<GenericCMSEntity> it = stratusService.queryIterator(query, GenericCMSEntity.class,
                new CMSClientContext());
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(count, it.getTotalCount());
        Assert.assertEquals(11, it.getRequestNum());
        CMSQuery newQuery = new CMSQuery(queryString);
        newQuery.setAllowFullTableScan(true);
        CMSQueryResult<GenericCMSEntity> newResult = stratusService.query(newQuery, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertFalse(newResult.isHasMore());
        Assert.assertTrue(it.getTotalCount() >= newResult.getCount());
    }
    
    @Test
    public void queryIteration01_BYID_join() {
        String queryString = "VPool[exists @environment]{*}.computes[@fqdns=~\".*.com\"]";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setPaginationMode(PaginationEnum.ID_BASED);
        query.setLimits(new long[] { 1, 2 });
        CMSQueryResult<GenericCMSEntity> result = stratusService.query(query, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertTrue(result.isHasMore());
        Assert.assertEquals(0, result.getHint());
        Assert.assertEquals(2, result.getLimits().length);
        Assert.assertNull(result.getSkips());
        Assert.assertNotNull(result.getCursor());

        QueryIterator<GenericCMSEntity> it = stratusService.queryIterator(query, GenericCMSEntity.class,
                new CMSClientContext());
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(count, it.getTotalCount());
        Assert.assertEquals(11, it.getRequestNum());
        CMSQuery newQuery = new CMSQuery(queryString);
        newQuery.setPaginationMode(PaginationEnum.ID_BASED);
        newQuery.setAllowFullTableScan(true);
        CMSQueryResult<GenericCMSEntity> newResult = stratusService.query(newQuery, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertFalse(newResult.isHasMore());
        Assert.assertTrue(it.getTotalCount() >= newResult.getCount());
    }

    @Test
    public void queryIteration01_reverse_join() {
        String queryString = "VPool[exists @environment]{*}.parentCluster!Compute[@fqdns=~\".*.com\"]";
        CMSQuery query = new CMSQuery(queryString);
        query.setPaginationMode(PaginationEnum.SKIP_BASED);
        query.setAllowFullTableScan(true);
        query.setLimits(new long[] { 1, 2 });
        CMSQueryResult<GenericCMSEntity> result = stratusService.query(query, GenericCMSEntity.class,
                new CMSClientContext());
        Preconditions.checkArgument(result.isHasMore());
        Assert.assertEquals(0, result.getHint());
        Assert.assertEquals(2, result.getLimits().length);
        Assert.assertEquals(2, result.getSkips().length);

        QueryIterator<GenericCMSEntity> it = stratusService.queryIterator(query, GenericCMSEntity.class,
                new CMSClientContext());
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(count, it.getTotalCount());
        Assert.assertEquals(11, it.getRequestNum());
        CMSQuery newQuery = new CMSQuery(queryString);
        newQuery.setAllowFullTableScan(true);
        CMSQueryResult<GenericCMSEntity> newResult = stratusService.query(newQuery, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertFalse(newResult.isHasMore());
        Assert.assertTrue(it.getTotalCount() >= newResult.getCount());
    }
    
    @Test
    public void queryIteration01_BYIDreverse_join() {
        String queryString = "VPool[exists @environment]{*}.parentCluster!Compute[@fqdns=~\".*.com\"]";
        CMSQuery query = new CMSQuery(queryString);
        query.setAllowFullTableScan(true);
        query.setPaginationMode(PaginationEnum.ID_BASED);
        query.setLimits(new long[] { 1, 2 });
        CMSQueryResult<GenericCMSEntity> result = stratusService.query(query, GenericCMSEntity.class,
                new CMSClientContext());
        Preconditions.checkArgument(result.isHasMore());
        Assert.assertEquals(0, result.getHint());
        Assert.assertEquals(2, result.getLimits().length);
        Assert.assertNull(result.getSkips());

        QueryIterator<GenericCMSEntity> it = stratusService.queryIterator(query, GenericCMSEntity.class,
                new CMSClientContext());
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(count, it.getTotalCount());
        Assert.assertEquals(11, it.getRequestNum());
        CMSQuery newQuery = new CMSQuery(queryString);
        newQuery.setAllowFullTableScan(true);
        newQuery.setPaginationMode(PaginationEnum.ID_BASED);
        CMSQueryResult<GenericCMSEntity> newResult = stratusService.query(newQuery, GenericCMSEntity.class,
                new CMSClientContext());
        Assert.assertFalse(newResult.isHasMore());
        Assert.assertTrue(it.getTotalCount() >= newResult.getCount());
    }


    @Test
    public void querySortOnSortOrder() {
        CMSQuery query0 = new CMSQuery("Compute");
        query0.setAllowFullTableScan(true);
        query0.addSortOn("name");
        query0.addSortOrder(SortOrder.asc);
        CMSQueryResult<Compute> result0 = raptorService.query(query0, Compute.class);
        Assert.assertTrue(result0.getEntities().size() > 1);
        ICMSEntity entity0 = result0.getEntities().get(0);

        CMSQuery query1 = new CMSQuery("Compute");
        query1.setAllowFullTableScan(true);
        query1.addSortOn("name");
        query1.addSortOrder(SortOrder.desc);
        CMSQueryResult<Compute> result1 = raptorService.query(query1, Compute.class);
        Assert.assertTrue(result1.getEntities().size() > 1);
        ICMSEntity entity1 = result1.getEntities().get(result1.getEntities().size() - 1);

        Assert.assertEquals(entity0.get_id(), entity1.get_id());
    }

    @Test
    public void querySortOnSortOrder01() {
        CMSQuery query0 = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, "Compute");
        query0.setAllowFullTableScan(true);
        query0.addSortOn("location");
        query0.addSortOn("name");
        query0.addSortOrder(SortOrder.desc);
        query0.addSortOrder(SortOrder.asc);
        CMSQueryResult<Compute> result0 = raptorService.query(query0, Compute.class);
        Assert.assertTrue(result0.getEntities().size() > 1);
        ICMSEntity entity0 = result0.getEntities().get(0);

        CMSQuery query1 = new CMSQuery(RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, "Compute");
        query1.setAllowFullTableScan(true);
        query1.addSortOn("location");
        query1.addSortOn("name");
        query1.addSortOrder(SortOrder.asc);
        query1.addSortOrder(SortOrder.desc);
        CMSQueryResult<Compute> result1 = raptorService.query(query1, Compute.class);
        Assert.assertTrue(result1.getEntities().size() > 1);
        ICMSEntity entity1 = result1.getEntities().get(result1.getEntities().size() - 1);

        Assert.assertEquals(entity0.get_id(), entity1.get_id());
    }

    private ServiceInstance createServiceInstance(String name) {
        ServiceInstance si = new ServiceInstance();
        si.setName(generateRandomName(name));
        return si;
    }
    
    private UpdateStrategy newUpdateStrategy(String name) {
        UpdateStrategy si = new UpdateStrategy();
        si.setName(name);
        si.setLastModifiedTime(new Date());
        return si;
    }

    @Test
    public void queryStringTest() {
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, "ApplicationService[@_oid=\"4fbb314fc681caf13e283a76\"]");
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
    }

    @Test
    public void queryStringTest1() {
        ApplicationService as = new ApplicationService();
        as.set_id("/path/stat/hub/...");
        as.setName("app_name_batch_create");
        raptorService.create(as);

        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, "ApplicationService[@_oid=\"" + as.get_id() + "\"]");
        query.setAllowFullTableScan(true);
        List<ApplicationService> queriedEntities = raptorService.query(query, ApplicationService.class).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNull(queriedEntities.get(0).getActiveManifestCur());

        ApplicationService updateAs = queriedEntities.get(0);
        updateAs.setActiveManifestCur("dfasdfasdfa");
        raptorService.update(updateAs);

        queriedEntities = raptorService.query(query, ApplicationService.class).getEntities();
        Assert.assertNotNull(queriedEntities);
        Assert.assertTrue(queriedEntities.size() > 0);
        Assert.assertNotNull(queriedEntities.get(0).getActiveManifestCur());
        Assert.assertEquals(updateAs.getActiveManifestCur(), queriedEntities.get(0).getActiveManifestCur());
    }

    @Test
    public void queryStringTest5() throws UnsupportedEncodingException {
        String str = "ApplicationService.services[@_lastmodified>date(123)]";

        // str = "Manifest[@_lastmodified > date(0)]";

        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, str);
        query.setAllowFullTableScan(true);
        List<GenericCMSEntity> queriedEntities = raptorService.query(query).getEntities();
        Assert.assertNotNull(queriedEntities);
        CMSEntityMapper mapper = new CMSEntityMapper(null, config, JsonCMSEntity.class, CMSEntityMapper.ProcessModeEnum.GENERIC, GenericCMSEntity.class);
        System.out.println(queriedEntities.size());
        for (GenericCMSEntity e : queriedEntities) {
            e.traverse(mapper);
            System.out.println(((JsonCMSEntity) mapper.getTargetEntity()).getNode());
        }
    }
    
    //
    // test for the special character in. Tests both in qa(standard deployment) and dev(programmatically deployment)
    //
    @Test
    public void queryString_convertedQuery_get() {
        final String q = "ApplicationService[@_oid=\"!\\\"#$%&'()*+,-./:;<=>?@\\[]^-=~`\"]";
        CMSClientContext context = new CMSClientContext();
        // case 1 : quoted " and ? won't work. It's not supported now!!
        {
            CMSQuery query = new CMSQuery(q);
            try {
                raptorService.query(query, context);
            } catch (CMSClientException cce) {
                Assert.assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cce.getHttpResponseCode());
            }
        }
        // case 1.1: use the constructor without additional parameter handling
        {
            CMSQuery query = new CMSQuery(q, false);
            CMSQueryResult<GenericCMSEntity> result = raptorService.query(query, context);
            Assert.assertEquals(0l, result.getCount().longValue());
            Assert.assertEquals(0, result.getEntities().size());
        }
        // case 2:
        {
            String q2 = "ApplicationService[@_oid=\"!\\#$%&'()*+,-./:;<=>?@\\[]^-=~`\"]";
            CMSQuery query = new CMSQuery(q2);
            CMSQueryResult<GenericCMSEntity> result = raptorService.query(query, context);
            Assert.assertEquals(0l, result.getCount().longValue());
            Assert.assertEquals(0, result.getEntities().size());
        }
        // case 3:
        {
            String q3 = "Subnet[@resourceId=\"10.109.188.0/22\"]{*} ";
            CMSQuery query = new CMSQuery(q3);
            CMSQueryResult<GenericCMSEntity> result = cmsdbService.query(query, context);
            Assert.assertEquals(0l, result.getCount().longValue());
            Assert.assertEquals(0, result.getEntities().size());
        }
    }

    @Test
    public void queryString_longQueryString() {
        // this is long query strin with 3300 + character, GET query will fail, but the query should
        // succeed without entity queried out!
        String q = "ApplicationService[@label in (12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345, 12345)]";
        CMSQuery query = new CMSQuery(q);
        CMSClientContext context = new CMSClientContext();
        CMSQueryResult<GenericCMSEntity> result = raptorService.query(query, context);
        Assert.assertEquals(0l, result.getCount().longValue());
        Assert.assertEquals(0, result.getEntities().size());
    }

    @Ignore
    @Test
    public void querySetQuery01() {
        String validQuery = "ApplicationService.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, validQuery);
        query.setSkip(0);
        query.setLimit(1000);
        query.setAllowFullTableScan(true);
        CMSClientContext context = new CMSClientContext();
        CMSQueryResult<ICMSEntity> resultEntities = raptorService.query(query, ICMSEntity.class, context);
        Assert.assertNotNull(resultEntities);
        Assert.assertNotNull(resultEntities.getEntities());
        Assert.assertTrue(resultEntities.getEntities().size() > 0);
        Assert.assertTrue(resultEntities.getEntities().size() == 12);
        for (ICMSEntity entity : resultEntities.getEntities()) {
            System.out.println("entity type: " + entity.get_type() + ", class name: "
                    + entity.getClass().getSimpleName());
            Assert.assertEquals(entity.get_type(), entity.getClass().getSimpleName());
        }
    }

    @Ignore
    @Test
    public void querySetQuery02Generic() {
        String validQuery = "ApplicationService.(services[@name=~\"srp-app.*\"]{*} || updateStrategies{*})";
        Set<String> resultType = new HashSet<String>();
        resultType.add("ServiceInstance");
        resultType.add("UpdateStrategy");

        CMSQuery query = new CMSQuery(RAPTOR_PAAS, null, validQuery);
        query.setSkip(0);
        query.setLimit(1000);
        query.setAllowFullTableScan(true);
        CMSClientContext context = new CMSClientContext();
        CMSQueryResult<GenericCMSEntity> resultEntities = raptorService.query(query, context);
        Assert.assertNotNull(resultEntities);
        Assert.assertNotNull(resultEntities.getEntities());
        Assert.assertTrue(resultEntities.getEntities().size() > 0);
        Assert.assertTrue(resultEntities.getEntities().size() == 12);
        for (ICMSEntity entity : resultEntities.getEntities()) {
            System.out.println("entity type: " + entity.get_type() + ", class name: "
                    + entity.getClass().getSimpleName());
            Assert.assertTrue(resultType.contains(entity.get_type()));
        }

        checkContextResponseOk(context);
    }

    @Test
    public void testDanglingCheck() {
        QueryIterator<ServiceInstance> it = raptorService.getDanglingReference(ServiceInstance.class, "runsOn", null);
        int count = 0;
        while (it.hasNext()) {
            ServiceInstance si = it.next();
            Compute compute = si.getRunsOn();
            Assert.assertNotNull(compute);

            Compute comp = raptorService.get(compute.get_id(), Compute.class, null);
            Assert.assertNull(comp);
            count++;
        }
        Assert.assertEquals(1, count);
    }

    @Test
    public void testGenericDanglingCheck() {
        String metadata = "ServiceInstance";
        QueryIterator<GenericCMSEntity> it = raptorService.getDanglingReference(metadata, "runsOn", "Compute", null);
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity si = it.next();
            Assert.assertEquals(metadata, si.get_type());

            GenericCMSEntity computeEntity = (GenericCMSEntity) si.getFieldValue("runsOn");
            Assert.assertNotNull(computeEntity);

            Compute comp = raptorService.get(computeEntity.get_id(), Compute.class, null);
            Assert.assertNull(comp);
            count++;
        }
        Assert.assertEquals(1, count);
    }

    @Test
    public void testEmptyReferenceCheck() {
        CMSClientContext context = new CMSClientContext();
        QueryIterator<GenericCMSEntity> ita = stratusService.getEmptyReference("VPool", "environment", context);
        int count = 0;
        while (ita.hasNext()) {
            GenericCMSEntity as = ita.next();
            Assert.assertNotNull(as);
            count++;
        }
        Assert.assertEquals(0, count);

        QueryIterator<Room> itr = raptorService.getEmptyReference(Room.class, "path", context);
        count = 0;
        while (itr.hasNext()) {
            Room entity = itr.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(0, count);

        checkContextResponseOk(context);
    }

    @Test
    public void testGenericEmptyReferenceCheck() {
        CMSClientContext context = new CMSClientContext();
        QueryIterator<GenericCMSEntity> it = raptorService.getEmptyReference("Dep.team", "name", context);
        int count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        // TODO: should make this clear for the server side empty reference behavior
//        Assert.assertEquals(0, count);

        it = raptorService.getEmptyReference("Room", "path", context);
        count = 0;
        while (it.hasNext()) {
            GenericCMSEntity entity = it.next();
            Assert.assertNotNull(entity);
            count++;
        }
        Assert.assertEquals(0, count);
    }

    @Test
    public void getMetaFields() {
        Set<String> fields = raptorService.getMetadataFields("ApplicationService");
        Assert.assertTrue(fields.contains("name"));
        Assert.assertTrue(fields.contains("services"));
        Assert.assertTrue(fields.contains("preManifestRef"));
        Assert.assertTrue(fields.contains("manifestRef"));
        Assert.assertTrue(fields.contains("activeManifestRef"));
        Assert.assertTrue(fields.contains("healthStatus"));
    }

    @Test
    public void getMetadatas() {
        Map<String, MetaClass> metadatas = raptorService.getMetadatas(null);
        boolean hasRel = false;
        boolean hasInternal = false;
        boolean hasMany = true;
        boolean hasAttribute = false;
        boolean hasEnum = false;
        for (MetaClass mc : metadatas.values()) {
            for (MetaField field : mc.getFields().values()) {
                Assert.assertNotNull(field.getCardinality());
                hasMany = hasMany || field.getCardinality().equals(CardinalityEnum.Many);
                Assert.assertNotNull(field.getDataType());
                hasRel = hasRel || field.getDataType().equals(DataTypeEnum.RELATIONSHIP);
                hasAttribute = hasAttribute || (!field.getDataType().equals(DataTypeEnum.RELATIONSHIP));

                if (field.getDataType().equals(DataTypeEnum.ENUM)) {
                    hasEnum = true;
                    MetaAttribute ma = (MetaAttribute) field;
                    Assert.assertFalse(ma.getEnumValues().isEmpty());
                }

                hasInternal = hasInternal || field.isInternal();
            }
        }
        Assert.assertTrue(hasRel && hasInternal && hasMany && hasAttribute && hasEnum);
    }

    @Test
    public void modifyArrayField() {
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);
        Assert.assertTrue(sis.size() > 0);
        ServiceInstance getSi = sis.get(0);
        int oldSize = getSi.getManifestRef().size();

        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        List<String> addManifests = new ArrayList<String>();
        addManifests.add("new manifest");
        service.setManifestRef(addManifests);
        raptorService.appendToArray(service, "manifestRef");

        ServiceInstance getSi2 = raptorService.get(getSi.get_id(), ServiceInstance.class);
        Assert.assertNotNull(getSi2);
        Assert.assertEquals(1, getSi2.getManifestRef().size() - oldSize);
    }

    /**
     * Server side change : modify embed field is not supported since cms_release_w38 (2013).
     */
    @Ignore
    @Test
    public void modifyEmbedArray() {
        CMSClientContext context = new CMSClientContext();
        String commentWithSpace = "comment with spaces !@#$%^&*()_+=`/\\%$&+,/:;=?@<>#%.. ";
        context.setComment(commentWithSpace);

        GenericCMSEntity entity = new GenericCMSEntity();
        entity.set_type("ServiceInstance");
        entity.setFieldValue("description", "create description");
        GenericCMSEntity ap = new GenericCMSEntity();
        ap.set_type("ServiceAccessPoint");
        String createApLabel = "create-ap -label";
        ap.setFieldValue("label", createApLabel);
        ap.setFieldValue("port", 80);
        ap.setFieldValue("protocol", "tcp");

        entity.setFieldValue("serviceAccessPoints", Arrays.asList(ap));
        stratusService.create(entity, context);
        String id = entity.get_id();

        GenericCMSEntity modifyEntity = new GenericCMSEntity();
        modifyEntity.set_type("ServiceInstance");
        modifyEntity.set_id(id);
        String modifyApLabel = "modify-field add ap-label";
        GenericCMSEntity ap2 = new GenericCMSEntity();
        ap2.set_type("ServiceAccessPoint");
        ap2.setFieldValue("label", modifyApLabel);
        ap2.setFieldValue("port", 80);
        ap2.setFieldValue("protocol", "tcp");
        modifyEntity.addFieldValue("serviceAccessPoints", ap2);

        stratusService.appendToArray(modifyEntity, "serviceAccessPoints", context);
        // now assertion
        GenericCMSEntity getSI = stratusService.get(id, "ServiceInstance", context);
        Assert.assertNotNull(getSI);
        List<GenericCMSEntity> getAps = (List<GenericCMSEntity>) getSI.getFieldValue("serviceAccessPoints");
        for (GenericCMSEntity getAp : getAps) {
            Assert.assertTrue(!getAp.get_id().isEmpty());
            Object getApLabel = getAp.getFieldValue("label");
            Assert.assertNotNull(getApLabel);
            Assert.assertTrue(getApLabel.equals(createApLabel) || getApLabel.equals(modifyApLabel));
        }
        String comment = (String) getSI.getFieldValue("_comment");
        Assert.assertNotNull(comment);
        Assert.assertEquals(commentWithSpace, comment);
    }

    @Test
    public void modifyJsonField() {
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);
        Assert.assertTrue(sis.size() > 0);
        ServiceInstance getSi = sis.get(0);
        String id = getSi.get_id();

        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        properties.put("f1", "values");
        properties.put("f2", 3);
        service.setProperties(properties);
        // first - add the properties to the given service instance entity
        raptorService.appendToJson(service, "properties");

        ServiceInstance get0 = raptorService.get(id, ServiceInstance.class);
        Assert.assertNotNull(get0.getProperties());
        Assert.assertNotNull(get0.getProperties().get("f1"));
        Assert.assertEquals("values", get0.getProperties().get("f1").getValueAsText());
        Assert.assertNotNull(get0.getProperties().get("f2"));
        Assert.assertEquals(3, get0.getProperties().get("f2").getValueAsInt());

        // update again to prove the incremental change
        service = new ServiceInstance();
        service.set_id(getSi.get_id());
        properties = JsonNodeFactory.instance.objectNode();
        // update f2 to value 5
        properties.put("f2", 5);
        ArrayNode an = JsonNodeFactory.instance.arrayNode();
        an.add(false);
        an.add(5);
        an.add("userdata");
        // add f3 as an array
        properties.put("f3", an);
        service.setProperties(properties);
        // second update
        raptorService.appendToJson(service, "properties");

        ServiceInstance get1 = raptorService.get(id, ServiceInstance.class);
        Assert.assertNotNull(get1.getProperties());
        Assert.assertNotNull(get1.getProperties().get("f1"));
        Assert.assertEquals("values", get1.getProperties().get("f1").getValueAsText());
        Assert.assertNotNull(get1.getProperties().get("f2"));
        Assert.assertEquals(5, get1.getProperties().get("f2").getValueAsInt());

        Assert.assertNotNull(get1.getProperties().get("f3"));
        Assert.assertTrue(get1.getProperties().get("f3") instanceof ArrayNode);

        ArrayNode getF3 = (ArrayNode) get1.getProperties().get("f3");
        Assert.assertEquals(3, an.size());
        Assert.assertEquals(Boolean.FALSE, getF3.get(0).getValueAsBoolean());
        Assert.assertEquals(5, getF3.get(1).getValueAsInt());
        Assert.assertEquals("userdata", getF3.get(2).getValueAsText());
    }

    @Test
    public void creatModifyWithJsonField() {
        // case 0: create with json
        CMSClientContext context = new CMSClientContext();
        ServiceInstance instance = new ServiceInstance();
        instance.setName(generateRandomName("json_field_test"));
        ObjectNode prop = JsonNodeFactory.instance.objectNode();
        String oldValue = "value-f1";
        prop.put("f1", oldValue);
        instance.setProperties(prop);
        raptorService.create(instance, context);
        // assert creation
        ServiceInstance getSi = raptorService.get(instance.get_id(), ServiceInstance.class, context);
        Assert.assertTrue(getSi.getProperties() instanceof ObjectNode);
        Assert.assertTrue(getSi.getProperties().has("f1"));
        Assert.assertEquals(oldValue, getSi.getProperties().get("f1").getTextValue());

        // case 1: update
        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        ObjectNode properties = JsonNodeFactory.instance.objectNode();
        String newValue = "new-value-f1";
        properties.put("f1", newValue);
        properties.put("f2", 3);
        service.setProperties(properties);
        raptorService.update(service, context);
        // assert update
        getSi = raptorService.get(instance.get_id(), ServiceInstance.class, context);
        Assert.assertTrue(getSi.getProperties() instanceof ObjectNode);
        Assert.assertTrue(getSi.getProperties().has("f2"));
        Assert.assertEquals(newValue, getSi.getProperties().get("f1").getTextValue());
    }
    
    @Test
    public void createModifyJsonField_asNonObject() {
        // TODO
    }

    @Test
    public void modifyNormalField() {
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);
        Assert.assertTrue(sis.size() > 0);
        ServiceInstance getSi = sis.get(0);

        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        String newName = generateRandomName("new-service-instance-name");
        service.setName(newName);
        CMSClientContext context = new CMSClientContext();
        context.setAllowPartialWrite(true);
        raptorService.updateEntityField(service, "name", context);

        ServiceInstance getSi2 = raptorService.get(getSi.get_id(), ServiceInstance.class, context);
        Assert.assertNotNull(getSi2);
        Assert.assertTrue(getSi2.get_version() - getSi.get_version() == 1);
        Assert.assertEquals(newName, getSi2.getName());
    }

    @Test
    public void testCASModifyField() {
        // create test data, node server with resource capacity
        CMSClientContext context = new CMSClientContext();
        context.setConditionalUpdate(true);

        GenericCMSEntity fqdn = new GenericCMSEntity();
        fqdn.set_type("FQDN");
        fqdn.setFieldValue(RESOURCE_ID, "fqdn-0001.vip.corp.ebay.com");
        fqdn.setFieldValue("fqdn", "fqdn-0001.vip.corp.ebay.com");
        cmsdbService.create(fqdn, context);
        
        GenericCMSEntity na = new GenericCMSEntity();
        na.set_type("NetworkAddress");
        String ip = "101.119.33.55";
		na.setFieldValue(RESOURCE_ID, ip);
        na.setFieldValue("address", ip);
        na.setFieldValue("ipVersion", "IPv4");
        cmsdbService.create(na, context);
        
        GenericCMSEntity nodeServer = new GenericCMSEntity();
        nodeServer.set_type(NODE_SERVER);
        nodeServer.setFieldValue(RESOURCE_ID, ip);
        nodeServer.setFieldValue("adminNotes", "no notesss.s....");
        nodeServer.setFieldValue("nodeType", "physical");
        nodeServer.setFieldValue("adminStatus", "NORMAL");
        
        GenericCMSEntity resourceCapacity = new GenericCMSEntity();
        resourceCapacity.set_type(RESOURCE_CAPCACITY);
        resourceCapacity.setFieldValue(RESOURCE_ID, generateRandomName("some-resource-capacity-id"));
        resourceCapacity.setFieldValue("type", "cpu");
        resourceCapacity.setFieldValue("total", 32);
        resourceCapacity.setFieldValue("used", 10);
        resourceCapacity.setFieldValue("unit", "unit");
        resourceCapacity.setFieldValue("reserved", 10);
        
        nodeServer.addFieldValue("capacities", resourceCapacity);
        nodeServer.setFieldValue("hostName", fqdn);
        nodeServer.addFieldValue("networkAddress", na);
        nodeServer.setFieldValue("assetStatus", "prep");

        cmsdbService.create(nodeServer, context);

        GenericCMSEntity getNodeServer = cmsdbService.get(nodeServer.get_id(), NODE_SERVER, context);

        String rcId = ((List<GenericCMSEntity>) getNodeServer.getFieldValue("capacities")).get(0).get_id();
        Assert.assertNotNull(rcId);
        GenericCMSEntity getRC = cmsdbService.get(rcId, RESOURCE_CAPCACITY, context);
        Assert.assertEquals(resourceCapacity.getFieldValue("used"), getRC.getFieldValue("used"));
        Assert.assertNotNull(getRC);

        // conditional update and assert
        GenericCMSEntity newCapacity = new GenericCMSEntity(getRC.get_id(), RESOURCE_CAPCACITY);
        newCapacity.setFieldValue("used", 15);
        cmsdbService.updateEntityField(newCapacity, "used", getRC, context);

        GenericCMSEntity getRC2 = cmsdbService.get(rcId, RESOURCE_CAPCACITY, context);
        Assert.assertEquals(newCapacity.getFieldValue("used"), getRC2.getFieldValue("used"));

        // an field conditional update
        newCapacity = new GenericCMSEntity(getRC.get_id(), RESOURCE_CAPCACITY);
        newCapacity.setFieldValue("used", 20);
        try {
            cmsdbService.updateEntityField(newCapacity, "used", getRC, context);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
            Assert.assertEquals(CMSErrorCodeEnum.CONDITIONAL_UPDATE_FAILED, e.getCmsResponseStatus().getErrorEnum());
        }
        
    }

    @Test
    public void modifyNormalFieldGivenNull() {
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);
        Assert.assertTrue(sis.size() > 0);
        ServiceInstance getSi = sis.get(0);

        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        String newName = generateRandomName("new-service-instance-name");
        // emulate a case, that user set incorrect field value
        service.setManifestCur(newName);
        CMSClientContext context = new CMSClientContext();
        context.setAllowPartialWrite(true);
        raptorService.updateEntityField(service, "name", context);

        ServiceInstance getSi2 = raptorService.get(getSi.get_id(), ServiceInstance.class, context);
        Assert.assertNotNull(getSi2);
        Assert.assertTrue(getSi2.get_version() - getSi.get_version() == 1);
        // if user given nothing, actually jersey would send empty string, other
        // than null... Suppose to be a jersey client bug.
        Assert.assertEquals("", getSi2.getName());
    }

    @Test
    public void modifyNormalFieldGivenEmpty() {
        CMSClientContext context = new CMSClientContext();
        context.setAllowPartialWrite(true);
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1, context);
        Assert.assertTrue(sis.size() > 0);
        ServiceInstance getSi = sis.get(0);

        // first set as some random values
        ServiceInstance service = new ServiceInstance();
        service.set_id(getSi.get_id());
        String newName = generateRandomName("new-service-instance-name-empty");
        service.setName(newName);
        raptorService.updateEntityField(service, "name", context);
        ServiceInstance getSi2 = raptorService.get(getSi.get_id(), ServiceInstance.class, context);

        // now set empty for string field
        newName = "";
        service.setName(newName);
        raptorService.updateEntityField(service, "name", context);

        getSi2 = raptorService.get(getSi.get_id(), ServiceInstance.class, context);
        Assert.assertNotNull(getSi2);
        Assert.assertTrue(getSi2.get_version() - getSi.get_version() == 2);
        Assert.assertEquals(newName, getSi2.getName());
    }

    @Test
    public void deleteNormalField() {
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);

        GenericCMSEntity entity = new GenericCMSEntity();
        entity.set_id(sis.get(0).get_id());
        entity.set_type(sis.get(0).get_type());
        raptorService.deleteField(entity, "https");

        GenericCMSEntity cms = raptorService.get(sis.get(0).get_id(), "ServiceInstance");
        Assert.assertNull(cms.getFieldValue("https"));
    }

    @Test
    public void deleteArrayField() {
        CMSClientContext context = new CMSClientContext();
        List<ServiceInstance> sis = raptorService.get(ServiceInstance.class, 1);
        Assert.assertNotNull(sis.get(0).getManifestRef());
        Assert.assertTrue(sis.get(0).getManifestRef().size() > 0);
        int oldLength = sis.get(0).getManifestRef().size();

        GenericCMSEntity entity = new GenericCMSEntity();
        entity.set_id(sis.get(0).get_id());
        entity.set_type(sis.get(0).get_type());
        entity.setFieldValue("manifestRef", sis.get(0).getManifestRef().get(0));
        raptorService.deleteField(entity, "manifestRef", context);

        // Thanga report issue: a getEntityByField follow the delete field would
        // get failure...
        GenericCMSEntity cms = raptorService.getEntityByField("ServiceInstance", "_oid", sis.get(0).get_id(), context);

        Assert.assertNotNull(cms.getFieldValue("manifestRef"));
        int newLength = ((List<?>) cms.getFieldValue("manifestRef")).size();
        Assert.assertEquals(1, oldLength - newLength);

    }
    
    @Test
    public void deleteArrayField_reference() {
        CMSClientContext context = new CMSClientContext();
        UpdateStrategy us1 = newUpdateStrategy("us-name-1");
        raptorService.create(us1, context);
        UpdateStrategy us2 = newUpdateStrategy("us-name-2");
        raptorService.create(us2, context);
        UpdateStrategy us3 = newUpdateStrategy("us-name-3");
        raptorService.create(us3, context);

        ApplicationService app = new ApplicationService();
        app.setName("appName");
        app.addUpdateStrategies(us1);
        app.addUpdateStrategies(us2);
        app.addUpdateStrategies(us3);
        
        raptorService.create(app, context);
        
        raptorService.delete(us1, context);
        Assert.assertNull(raptorService.get(us1.get_id(), UpdateStrategy.class, context));
        
        // now delete field
        ApplicationService getApp = raptorService.get(app.get_id(), ApplicationService.class, context);
        Assert.assertEquals(3, getApp.getUpdateStrategies().size());
        Assert.assertEquals(us1.get_id(), getApp.getUpdateStrategies().get(0).get_id());
        
        ApplicationService newAppService = new ApplicationService();
        newAppService.set_id(getApp.get_id());
        newAppService.addUpdateStrategies(us1);
        raptorService.deleteField(newAppService, "updateStrategies", context);
        
        getApp = raptorService.get(app.get_id(), ApplicationService.class, context);
        Assert.assertEquals(2, getApp.getUpdateStrategies().size());
        Assert.assertFalse(us1.get_id().equals(getApp.getUpdateStrategies().get(0).get_id()));
    }

    @Test
    public void testStaticIsAlive() {
        Assert.assertTrue(CMSClientService.isAlive(LOCAL_ENDPOINT));
    }

    @Test
    public void testInstanceIsAlive() throws InterruptedException {
        Assert.assertTrue(raptorService.isAlive());
        Assert.assertTrue(sdService.isAlive());

        CMSClientConfig config = new CMSClientConfig(LOCAL_ENDPOINT, RAPTOR_PAAS, IBranch.DEFAULT_BRANCH,
                Constants.META_PACKAGE + "." + RAPTOR_PAAS);
        CMSClientService localService = CMSClientService.getClientService(config);
        Assert.assertTrue(localService.isAlive());
        localService.close();
        Assert.assertFalse(localService.isAlive());

        // other client service not impacted
        Assert.assertTrue(raptorService.isAlive());
        for (int i = 0; i < 10; i++) {
            raptorService.get("4fbb314fc681caf13e283a76", ApplicationService.class);
            Thread.sleep(500);
        }
        Assert.assertTrue(sdService.isAlive());
        for (int i = 0; i < 10; i++) {
            sdService.getEntitiesByField(ManifestVersion.class, "Manifest.versions", "name", "Dummy ManifestVersion Bundle-0-0001", new CMSClientContext());
            Thread.sleep(500);
        }
        // through the instance is closed the static aliveness checking still
        // works
        Assert.assertTrue(CMSClientService.isAlive(LOCAL_ENDPOINT));
    }

    public void testGetToken() {
        raptorService.getToken("liasu", "no-valida-passwod");
    }

    @Test
    public void testGenericCMSEntity() {
        GenericCMSEntity gcmsEntity = new GenericCMSEntity();
        String str = gcmsEntity.toString();
        Assert.assertNotNull(str);
        Assert.assertFalse(str.isEmpty());
        System.out.println(str);

        ApplicationService app = new ApplicationService();
        str = app.toString();
        Assert.assertNotNull(str);
        Assert.assertFalse(str.isEmpty());
        System.out.println(str);
    }

    @Test
    public void testCorrelationId() {

    }

    @Test
    public void getHostIp() throws UnknownHostException, SocketException {
        Logger logger = LoggerFactory.getLogger(CMSClientServiceTest.class);
        try {
            // InetAddress ia = InetAddress.getByName(
            System.out.println(InetAddress.getLocalHost().getHostName());
            // );

            // for (InetAddress ia : ips) {
            // System.out.println(ia.getHostAddress());
            // System.out.println(ia.getHostName());
            // System.out.println(ia.getCanonicalHostName());

            System.out.println("================");
            // }
        } catch (UnknownHostException uhe) {
            logger.error("UnknowHostException when try to ip address, set as unknown!", uhe);
        }

        System.out.println("Host addr: " + InetAddress.getLocalHost().getHostAddress()); // often
                                                                                         // returns
                                                                                         // "127.0.0.1"
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();) {
            NetworkInterface e = n.nextElement();
            System.out.println("Interface: " + e.getName());
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();) {
                InetAddress addr = a.nextElement();
                System.out.println("  " + addr.getHostAddress());
            }
        }

    }

    @Test
    public void testCmsQuery() {
        CMSQuery query = new CMSQuery("");
        try {
            query.addSortOrder(null);
            Assert.fail();
        } catch (IllegalArgumentException lse) {
            // expected
        }
        query.setLimits(null);
        query.setLimits(new long[0]);
        Assert.assertNull(query.getLimits());
        query.setLimits(new long[] { 1001 });
        Assert.assertNull(query.getSkips());
        query.setSkips(null);
        query.setSkips(new long[0]);
        Assert.assertNull(query.getHint());
        query.setHint(0);

        Assert.assertFalse(query.isCountOnly());
        query.setCountOnly(true);
        Assert.assertTrue(query.isCountOnly());
        query.setCountOnly(false);
        Assert.assertFalse(query.isCountOnly());
    }

    @Test
    public void partialWriteSetting() {
        CMSClientContext context = new CMSClientContext();
        Map<String, String> headers = context.getHeader();
        Assert.assertTrue(!headers.containsKey(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE));

        context.setAllowPartialWrite(true);
        headers = context.getHeader();
        Assert.assertTrue(headers.containsKey(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE));
        Assert.assertTrue(headers.get(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE).equals(Boolean.TRUE.toString()));

        context.setAllowPartialWrite(false);
        headers = context.getHeader();
        Assert.assertTrue(headers.containsKey(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE));
        Assert.assertTrue(headers.get(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE).equals(Boolean.FALSE.toString()));

        context.setAllowPartialWrite(null);
        headers = context.getHeader();
        Assert.assertTrue(!headers.containsKey(CMSClientContext.X_CMS_ALLOW_PARTIAL_WRITE));
    }


    /**
     * For CMS-2900, from Zeba, platform framework dev
     */
    @Test
    public void testCreateIncorrect_SetType() throws JsonGenerationException, JsonMappingException, IOException {
        CMSClientContext context = new CMSClientContext();
        context.setComment("general-comment-for-unit-test");
        context.setConsistentPolicy(CMSConsistentPolicy.PRIMARY_ONLY.name());
        context.setPriority(CMSPriority.NEUTRAL.name());
        context.setUser("unit-test-user");

        ApplicationService appServ = new ApplicationService();
        appServ.setName(appServ.getName() + System.currentTimeMillis());
        appServ.set_branch(IBranch.DEFAULT_BRANCH);

        appServ.set_type("v3");

        try {
            raptorService.create(appServ, context);
            Assert.fail();
        } catch (CMSClientException e) {
            // expected
//            Assert.assertEquals(500, e.getHttpResponseCode());
//            Assert.assertEquals(1019, e.getCmsResponseCode());
            e.printStackTrace();
        }
    }

    @Test
    public void testRertry() throws IOException, URISyntaxException {
        CMSClientContext context = new CMSClientContext();
        Logger logger = LoggerFactory.getLogger(CMSClientServiceTest.class);
        final String jsonResponse = Files.toString(
                new File(CMSClientServiceTest.class.getResource("/version_confliction_response.json").toURI()),
                Charset.defaultCharset());
        raptorService.getClientConfig().setRetryTime(1);
        VersionConflictionCall op = new VersionConflictionCall(jsonResponse);

        {
            op.setExceptionCount(0);
            context.setRetryCount(null);
            try {
                GenericEntityService.retryOperation(raptorService, logger, context, op, "abc", -1);
                Assert.fail();
            } catch (CMSClientException ce) {
                // expected
            }
            Assert.assertEquals(raptorService.getClientConfig().getRetryTime().intValue() + 1, op.getExceptionCount());
        }
        // 
        {
            op.setExceptionCount(0);
            context.setRetryCount(1);
            Assert.assertTrue(context.hasRetryCount());
            try {
                GenericEntityService.retryOperation(raptorService, logger, context, op, "abc", -1);
                Assert.fail();
            } catch (CMSClientException ce) {
                // expected
            }
            Assert.assertEquals(context.getRetryCount().intValue() + 1, op.getExceptionCount());
        }
        // 
        {
            op.setExceptionCount(0);
            context.setRetryCount(1);
            Assert.assertTrue(context.hasRetryCount());
            try {
                GenericEntityService.retryOperation(raptorService, logger, context, op, "abc", -1);
                Assert.fail();
            } catch (CMSClientException ce) {
                // expected
            }

            Assert.assertEquals(2, op.getExceptionCount());
        }
        // 
        {
            op.setExceptionCount(0);
            context.setRetryCount(1);
            Assert.assertTrue(context.hasRetryCount());
            try {
                GenericEntityService.retryOperation(raptorService, logger, context, op, "abc", -1);
                Assert.fail();
            } catch (CMSClientException ce) {
                // expected
            }

            Assert.assertEquals(2, op.getExceptionCount());
        }
    }

    @Test
    public void testDirtyCheck() {
    List<ApplicationService> ass = raptorService.get(ApplicationService.class, 1, new CMSClientContext());
        ApplicationService as = ass.get(0);
        // simulate that the get the entity
        String arch = as.getArchitecture();
        as.setArchitecture(generateRandomName("newASID"));
        as.clearDirtyBits();
        as.setName(generateRandomName("newASname"));
        raptorService.update(as, new CMSClientContext());

        ApplicationService getAs = raptorService.get(as.get_id(), ApplicationService.class, new CMSClientContext());
        Assert.assertEquals(as.getName(), getAs.getName());
        // architecture is not modified
        Assert.assertEquals(arch, getAs.getArchitecture());
    }
    
    @Test
    public void testContextResponse() {
        CMSClientContext context = new CMSClientContext();

        context.setLastResponse(null);
        Assert.assertNull(context.getLastCmsStatus());
        Assert.assertNull(context.getLastDualWriteStatus());

        context.setLastResponse("{\"status\": { \"code\" : -1, \"msg\" : null, \"stackTrace\": null}}");
        Assert.assertNotNull(context.getLastCmsStatus());
        Assert.assertNull(context.getLastDualWriteStatus());

        context.setLastResponse("");
        Assert.assertNull(context.getLastCmsStatus());
        Assert.assertNull(context.getLastDualWriteStatus());

        context.setLastResponse("{\"status\": { code\" : -1, \"msg\" : null, \"stackTrace\": null}}");
        Assert.assertNull(context.getLastCmsStatus());
        Assert.assertNull(context.getLastDualWriteStatus());

        context.setLastResponse("{\"ODBWriteStatus\": { \"code\" : -1, \"msg\" : null, \"stackTrace\": null}}");
        Assert.assertNull(context.getLastCmsStatus());
        Assert.assertNotNull(context.getLastDualWriteStatus());
    }

//    public static void closeServiceTest() {
//        Assert.assertTrue(raptorService.isAlive());
//        raptorService.close();
//        Assert.assertFalse(raptorService.isAlive());
//
//        Assert.assertTrue(cmsdbService.isAlive());
//        cmsdbService.close();
//        Assert.assertFalse(cmsdbService.isAlive());
//        
//        Assert.assertTrue(sdService.isAlive());
//        sdService.close();
//        Assert.assertFalse(sdService.isAlive());
//    }


    @AfterClass
    public static void tearDown() throws Exception {
        testStopServerTest();
    }

    public static void testStopServerTest() throws Exception {
        RunTestServer.stopServer();
        try {
            raptorService.get(ApplicationService.class, 1, new CMSClientContext());
            Assert.fail();
        } catch (CMSClientException ce) {
            // expected
            Assert.assertEquals(-1, ce.getHttpResponseCode());
            Assert.assertEquals(-1, ce.getCmsResponseCode());
            Assert.assertTrue(ce.getCause() instanceof ClientHandlerException);
            Assert.assertTrue(ce.getCause().getCause() instanceof ConnectException);
        }
    }
}
