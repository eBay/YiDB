package com.ebay.cloud.cms.typsafe.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.entmgr.branch.IBranch;
import com.ebay.cloud.cms.model.raptor_paas.ApplicationService;
import com.ebay.cloud.cms.model.raptor_paas.Compute;
import com.ebay.cloud.cms.model.raptor_paas.Door;
import com.ebay.cloud.cms.model.raptor_paas.Room;
import com.ebay.cloud.cms.model.raptor_paas.ServiceInstance;
import com.ebay.cloud.cms.model.raptor_paas.UpdateStrategy;
import com.ebay.cloud.cms.model.software_deployment.Manifest;
import com.ebay.cloud.cms.model.software_deployment.ManifestVersion;
import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.restful.Constants;
import com.ebay.cloud.cms.web.RunTestServer;

@SuppressWarnings("unused")
public class RelationshipServiceTest {

    private static final String CMSDB = "cmsdb";
    private static final String RAPTOR_PAAS = "raptor-paas";
    private static final String SOFTWARE_DEPLOYMENT = "software-deployment";
    private static final String STRATUS_CI = "stratus-ci";
    private static final String LOCAL_ENDPOINT = "http://localhost:9000/cms";
    private static final String RESOURCE_ID = "resourceId";
    private static final String NODE_SERVER = "NodeServer";
    private static final String RESOURCE_CAPCACITY = "ResourceCapacity";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static CMSClientService raptorService;
    private static CMSClientService sdService;
    private static CMSClientService stratusService;
    private static CMSClientService cmsdbService;
    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Random random = new Random(System.currentTimeMillis());
    private static CMSClientConfig config;

    @BeforeClass
    public static void setUp() throws Exception {
        RunTestServer.startServer(new String[] { "-initData", "9000" });

        config = new CMSClientConfig(LOCAL_ENDPOINT, RAPTOR_PAAS, IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE
                + ".raptor_paas");
        raptorService = CMSClientService.getClientService(config);

        sdService = CMSClientService.getClientService(new CMSClientConfig(LOCAL_ENDPOINT, SOFTWARE_DEPLOYMENT,
                IBranch.DEFAULT_BRANCH, Constants.META_PACKAGE + ".software_deployment"));

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
    }

    @AfterClass
    public static void teardown() throws Exception {
        RunTestServer.stopServer();
    }

    private String generateRandomName(String baseName) {
        return baseName + System.currentTimeMillis() + random.nextDouble();
    }

    private ApplicationService newApplicationService(ServiceInstance createdSi) {
        ApplicationService appServ = new ApplicationService();
        appServ.setName(generateRandomName("appServ_name_"));
        if (createdSi != null) {
            ServiceInstance refSi = new ServiceInstance();
            refSi.set_id(createdSi.get_id());
            List<ServiceInstance> sis = new ArrayList<ServiceInstance>();
            sis.add(createdSi);
            appServ.setServices(sis);
        }
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

    private GenericCMSEntity newGenericAppServ() {
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

    private GenericCMSEntity newGenericServiceInstance() {
        GenericCMSEntity siEntity = new GenericCMSEntity();
        siEntity.set_metaclass("ServiceInstance");
        siEntity.setFieldValue("name", generateRandomName("si-generic"));
        siEntity.setFieldValue("healthStatus", "healthy");
        siEntity.setFieldValue("manifestDiff", false);
        siEntity.setFieldValue("https", true);
        siEntity.setFieldValue("port", "80");
        return siEntity;
    }
    
    private UpdateStrategy newUpdateStrategy(String name) {
        UpdateStrategy si = new UpdateStrategy();
        si.setName(name);
        si.setLastModifiedTime(new Date());
        return si;
    }

    @Test
    public void deleteRelationship_reference() {
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

        ApplicationService getApp = raptorService.get(app.get_id(), ApplicationService.class, context);
        Assert.assertEquals(3, getApp.getUpdateStrategies().size());
        Assert.assertEquals(us1.get_id(), getApp.getUpdateStrategies().get(0).get_id());

        raptorService.deleteRelationship(getApp, "updateStrategies", us1, context);

        getApp = raptorService.get(app.get_id(), ApplicationService.class, context);
        Assert.assertEquals(2, getApp.getUpdateStrategies().size());
        boolean us2Kept = false;
        boolean us3Kept = false;
        boolean us1Found = false;
        for (UpdateStrategy us : getApp.getUpdateStrategies()) {
            if (us1.get_id().equals(us.get_id())) {
                us1Found = true;
            }
            if (us2.get_id().equals(us.get_id())) {
                us2Kept = true;
            }
            if (us3.get_id().equals(us.get_id())) {
                us3Kept = true;
            }
        }
        Assert.assertTrue(us2Kept && us3Kept && !us1Found);
    }

    @Test
    public void testReference_allnewlycreate() {
        ServiceInstance to = newServiceInstance();
        Assert.assertNull(to.get_id());
        ApplicationService as = newApplicationService(null);
        Assert.assertNull(as.get_id());
        CMSClientContext context = new CMSClientContext();

        raptorService.createRelationship(as, "services", to, context);

        assertAppServiceAndService(to, as, context);
    }

    @Test
    public void testReference_allnewlycreate_01() {
        ServiceInstance to = newServiceInstance();
        Assert.assertNull(to.get_id());
        ApplicationService as = newApplicationService(null);
        Assert.assertNull(as.get_id());
        as.addServices(to);
        CMSClientContext context = new CMSClientContext();

        raptorService.createRelationship(as, "services", to, context);

        assertAppServiceAndService(to, as, context);
    }

    @Test
    public void testReference_bothExisted() {
        CMSClientContext context = new CMSClientContext();
        ServiceInstance to = newServiceInstance();
        Assert.assertNull(to.get_id());
        raptorService.create(to, context);
        
        ApplicationService as = newApplicationService(null);
        Assert.assertNull(as.get_id());
        raptorService.create(as, context);

        as.addServices(to);

        raptorService.createRelationship(as, "services", to, context);
        assertAppServiceAndService(to, as, context);
    }

    @Test
    public void testReference_bothExisted_refExisting() {
        CMSClientContext context = new CMSClientContext();
        ServiceInstance to = newServiceInstance();
        Assert.assertNull(to.get_id());
        ApplicationService as = newApplicationService(null);
        Assert.assertNull(as.get_id());
        as.addServices(to);

        raptorService.createRelationship(as, "services", to, context);

        assertAppServiceAndService(to, as, context);
        // try create relationship again
        raptorService.createRelationship(as, "services", to, context);
    }

    private void assertAppServiceAndService(ServiceInstance to, ApplicationService as, CMSClientContext context) {
        Assert.assertNotNull(as.get_id());
        Assert.assertNotNull(to.get_id());
        ApplicationService getAs = raptorService.get(as.get_id(), ApplicationService.class, context);
        Assert.assertNotNull(getAs);
        Assert.assertNotNull(getAs.getServices());
        Assert.assertEquals(1, getAs.getServices().size());

        ServiceInstance getSi = getAs.getServices().get(0);
        Assert.assertNotNull(getSi);
        Assert.assertEquals(to.get_id(), getSi.get_id());
    }

    @Test
    public void testReference_newlycreate_partial() {
        // case 1 : From existed, To not-exsited
        ApplicationService as = newApplicationService(null);
        CMSClientContext context = new CMSClientContext();
        raptorService.create(as, context);
        Assert.assertNotNull(as.get_id());

        ServiceInstance to = newServiceInstance();
        Assert.assertNull(to.get_id());
        raptorService.createRelationship(as, "services", to, context);

        assertAppServiceAndService(to, as, context);

        // case 2 : From not-exsiting, To existed
        to = newServiceInstance();
        raptorService.create(to, context);

        as = newApplicationService(null);
        raptorService.createRelationship(as, "services", to, context);
        assertAppServiceAndService(to, as, context);
    }

    @Test
    public void testReference_Generic_allnewlycreate() {
        GenericCMSEntity genericAs = newGenericAppServ();
        GenericCMSEntity genericSi = newGenericServiceInstance();
        CMSClientContext context = new CMSClientContext();
        raptorService.createRelationship(genericAs, "services", genericSi, context);

        assertGenericAppAndService(genericAs, genericSi, context);
    }

    private void assertGenericAppAndService(GenericCMSEntity genericAs, GenericCMSEntity genericSi,
            CMSClientContext context) {
        Assert.assertNotNull(genericAs.get_id());
        Assert.assertNotNull(genericSi.get_id());
        GenericCMSEntity getAs = raptorService.get(genericAs.get_id(), "ApplicationService", context);
        Assert.assertNotNull(getAs);
        Assert.assertNotNull(getAs.getFieldValue("services"));
        Assert.assertEquals(1, ((List<?>)getAs.getFieldValue("services")).size());
        GenericCMSEntity service = ((List<GenericCMSEntity>) getAs.getFieldValue("services")).get(0);
        Assert.assertNotNull(service);
        Assert.assertEquals(genericSi.get_id(), service.get_id());
    }

    @Test
    public void testReference_Generic_newlycreate_pratial() {
        // case 1 : From existed, To not-exsited
        GenericCMSEntity as = newGenericAppServ();
        CMSClientContext context = new CMSClientContext();
        raptorService.create(as, context);
        Assert.assertNotNull(as.get_id());

        GenericCMSEntity to = newGenericServiceInstance();
        Assert.assertNull(to.get_id());
        raptorService.createRelationship(as, "services", to, context);

        assertGenericAppAndService(as, to, context);

        // case 2 : From not-exsiting, To existed
        to = newGenericServiceInstance();
        raptorService.create(to, context);

        as = newGenericAppServ();
        raptorService.createRelationship(as, "services", to, context);
        assertGenericAppAndService(as, to, context);
    }

    @Ignore
    @Test
    public void testEmbed_newlyCreate() {
        CMSClientContext context = new CMSClientContext();
        Manifest manifest = new Manifest();

        ManifestVersion mv = new ManifestVersion();
        mv.setCreatedTime(new Date());

        manifest.addVersions(mv);

        sdService.createRelationship(manifest, "versions", mv, context);

        assertManifestAndVersion(manifest, mv, context);
    }

    private void assertManifestAndVersion(Manifest manifest, ManifestVersion version, CMSClientContext context) {
        Assert.assertNotNull(manifest.get_id());
        Assert.assertNotNull(version.get_id());

        Manifest getManifest = sdService.get(manifest.get_id(), Manifest.class, context);
        Assert.assertEquals(1, getManifest.getVersions().size());
        Assert.assertEquals(version.get_id(), getManifest.getVersions().get(0));

        ManifestVersion getVersion = sdService.get(version.get_id(), ManifestVersion.class, context);
        Assert.assertEquals(version.get_id(), getVersion.get_id());
    }

    @Ignore
    @Test
    public void testEmbed_partialCreate() {
        CMSClientContext context = new CMSClientContext();
        Manifest manifest = new Manifest();

        raptorService.create(manifest, context);

        ManifestVersion mv = new ManifestVersion();
        mv.setCreatedTime(new Date());

        manifest.addVersions(mv);

        raptorService.createRelationship(manifest, "versions", mv, context);

        assertManifestAndVersion(manifest, mv, context);
    }

    @Test
    public void testEmbed_bothExisted() {
        // TODO - nothing to do
    }

    @Test
    public void testInner() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setFloor("second");
        room.addLevel("2");
        // create door
        Door door = new Door();
        door.setName(generateRandomName("door-name"));
        room.addPath(door);
        raptorService.createRelationship(room, "path", door, context);
        assertRoomAndDoor(room, door, context);
    }

    private void assertRoomAndDoor(Room room, Door door, CMSClientContext context) {
        Assert.assertNotNull(room.get_id());
        Assert.assertNotNull(door.get_id());
        Room getRoom = raptorService.get(room.get_id(), Room.class, context);
        Assert.assertNotNull(getRoom);
        Assert.assertNotNull(getRoom.getFieldValue("path"));

        Door getDoor = raptorService.get(door.get_id(), Door.class, context);
        Assert.assertNotNull(getDoor);
        Assert.assertNotNull(getDoor.get_id());
        Assert.assertNotNull(getDoor.get_hostEntity());
        Assert.assertEquals(door.get_id(), getDoor.get_id());
    }

    @Test
    public void testInnerParitial() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setName(generateRandomName("room-name"));
        room.setFloor("second");
        room.addLevel("2");
        raptorService.create(room, context);

        // create door
        Door door = new Door();
        door.setName(generateRandomName("door-name"));
        room.addPath(door);
        raptorService.createRelationship(room, "path", door, null);
        assertRoomAndDoor(room, door, context);
    }

    @Test
    public void testInner_bothExisted() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setName(generateRandomName("room-name"));
        room.setFloor("second");
        room.addLevel("2");

        // create door
        Door door = new Door();
        door.setName(generateRandomName("door-name"));
        room.addPath(door);
        raptorService.createRelationship(room, "path", door, null);
        assertRoomAndDoor(room, door, context);

        raptorService.createRelationship(room, "path", door, null);
        assertRoomAndDoor(room, door, context);
    }
    
    @Test
    public void testInner_setNullParentId() {
        CMSClientContext context = new CMSClientContext();
        // create room
        Room room = new Room();
        room.setName(generateRandomName("room-name"));
        room.setFloor("second");
        room.addLevel("2");
        raptorService.create(room, context);

        room.set_id(null); // explicitly set null for parent id
        // create door
        Door door = new Door();
        door.setName(generateRandomName("door-name"));
        room.addPath(door);
        raptorService.createRelationship(room, "path", door, null);
        assertRoomAndDoor(room, door, context);
    }
    
    @Test
    public void testNetworkDeviceAndPort() {
        // create device
        GenericCMSEntity device = createDevice();
        CMSClientContext context = new CMSClientContext();
        cmsdbService.create(device, context);
        // create port
        GenericCMSEntity port = createPort(device, context);
        context.setPath("NetworkDevice", device.get_id(), "networkPorts");
        cmsdbService.create(port, context);
        // assertion for creation
        GenericCMSEntity getDevice = null;
        getDevice = cmsdbService.get(device.get_id(), "NetworkDevice", context);
        Assert.assertEquals(1, ((List<?>) getDevice.getFieldValue("networkPorts")).size());

        // now try to delete relationship, it should delete
        // the network port and detach frm the device
        cmsdbService.deleteRelationship(device, "networkPorts", port, context);
        // assertion
        GenericCMSEntity getPort = cmsdbService.get(port.get_id(), "NetworkPort", context);
        Assert.assertNull(getPort);
        getDevice = cmsdbService.get(device.get_id(), "NetworkDevice", context);
        Assert.assertEquals(0, ((List<?>) getDevice.getFieldValue("networkPorts")).size());
    }

    GenericCMSEntity createPort(GenericCMSEntity device, CMSClientContext context) {
        GenericCMSEntity port = new GenericCMSEntity();
        port.set_type("NetworkPort");
        port.setFieldValue("resourceId", "resource-id-network-port-1");
        port.setFieldValue("type", "type-network-port-1");
        port.setFieldValue("sourceId", "source-id-network-port-1");
        port.setFieldValue("ifIndex", "eth0");
        return port;
    }

    GenericCMSEntity createDevice() {
        GenericCMSEntity asset = new GenericCMSEntity();
        asset.set_type("Asset");
        asset.setFieldValue("assetId", "asset-id");
        asset.setFieldValue("resourceId", "resource-id-asset");
        cmsdbService.create(asset, new CMSClientContext());
        
        GenericCMSEntity device = new GenericCMSEntity();
        device.set_type("NetworkDevice");
        device.setFieldValue("nrServer", "nr-values");
        device.setFieldValue("sourceId", "device-source-id");
        device.setFieldValue("productionLevel", "device-source-production-level");
        device.setFieldValue("resourceId", "resource-id-network-device");
        device.setFieldValue("snmpProfile", "snmp-profile-network-device");
        device.setFieldValue("zone", "zone-network-device-");
        device.setFieldValue("switchLabel", "switch-label-network-device");
        device.setFieldValue("asset", asset);        
        return device;
    }

}
