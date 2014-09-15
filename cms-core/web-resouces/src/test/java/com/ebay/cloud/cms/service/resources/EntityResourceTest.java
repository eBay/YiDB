/*
Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


/* 
Copyright 2012 eBay Software Foundation 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/ 

package com.ebay.cloud.cms.service.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.POJONode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.entity.JsonEntity;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.search.ISearchField;
import com.ebay.cloud.cms.dal.search.SearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.FieldSearchCriteria.FieldOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria;
import com.ebay.cloud.cms.dal.search.impl.criteria.LogicalSearchCriteria.LogicOperatorEnum;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.entmgr.entity.CallbackContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.entmgr.entity.IEntityOperationCallback;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException.EntMgrErrCodeEnum;
import com.ebay.cloud.cms.entmgr.utils.RegistrationUtils;
import com.ebay.cloud.cms.metadata.model.InternalFieldEnum;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.Error;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.MockUriInfo;
import com.ebay.cloud.cms.service.resources.QueryResourceTest.NormalModeInfo;
import com.ebay.cloud.cms.service.resources.impl.CMSResourceUtils;
import com.ebay.cloud.cms.service.resources.impl.EntityResource;
import com.ebay.cloud.cms.service.resources.impl.QueryResource;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.mongodb.BasicDBObject;

public class EntityResourceTest extends CMSResourceTest {

	private static final String NETWORK_ADDRESS = "NetworkAddress";
	private static final String _802_ADDRESS = "802Address";
	private static EntityResource resource;
	private static MockUriInfo uriInfo;

	@BeforeClass
	public static void setupResource() {
		resource = new EntityResource();

		uriInfo = new MockUriInfo();
		uriInfo.getQueryParameters().put(CMSResourceUtils.REQ_PARAM_COMPONENT, Arrays.asList("unitTestUser"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testGet() {
		String entityStr = loadJson("/Compute.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", "Compute",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();
		assertOkAndNotNullResult(resp);
		List node = (List) resp.get("result");
		String newId = (String) node.get(0);

		resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", newId,
				CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest())
				.getEntity();
		assertOk(resp);
    }

    @Test(expected = NotFoundException.class)
    public void testReadFilterGet() {
        // find service instance
        HttpServletRequest request = new MockHttpServletRequest();
        Map<String, List<SearchCriteria>> additionalCriteria = new HashMap<String, List<SearchCriteria>>();
        MetaClass metaClass = raptorMetaService.getMetaClass(SERVICE_INSTANCE);
        MetaField nameField = metaClass.getFieldByName("name");
        ISearchField searchField = new SelectionField(nameField,
                RegistrationUtils.getDefaultDalImplementation(dataSource).searchStrategy);
        // ## see raptorTopology.json for this value
        SearchCriteria criteria = new FieldSearchCriteria(searchField, FieldOperatorEnum.NE, "srp-app:Raptor-00001");
        
        LogicalSearchCriteria lsc = new LogicalSearchCriteria(LogicOperatorEnum.AND);
        lsc.addChild(criteria);

        additionalCriteria.put(SERVICE_INSTANCE, Arrays.<SearchCriteria>asList(lsc));
        request.setAttribute(CMSResourceUtils.REQ_READ_FILTER, additionalCriteria);

        resource.getEntity(uriInfo, "raptor-paas", "main", "ServiceInstance", "4fbb314fc681caf13e283a7a",
                CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), request).getEntity();
    }

    @Test
	public void testGet_withConsistPolicy() {
		CMSResponse resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute",
				"4fbb314fc681caf13e283a79", CMSPriority.NEUTRAL.toString(), "MAJORITY", CMSQueryMode.URI.toString(),
				new MockHttpServletRequest()).getEntity();
		assertOkAndNotNullResult(resp);

		resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", "4fbb314fc681caf13e283a79",
				CMSPriority.NEUTRAL.toString(), "PRIMARY", CMSQueryMode.URI.toString(), new MockHttpServletRequest())
				.getEntity();
		assertOkAndNotNullResult(resp);
	}

	@Test(expected = NotFoundException.class)
	public void testGetInvalidId() {
		resource.getEntity(uriInfo, "raptor-paas", "main", SERVICE_INSTANCE, "invalid-id",
				CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest());
	}

	@Test
	public void testGetBranch() {
		// case 0:
		CMSResponse response = (CMSResponse) resource.getBranch(uriInfo, null, null, RAPTOR_REPO, "main",
				new MockHttpServletRequest()).getEntity();
		assertOkAndNotNullResult(response);

		// case 1: invalid repo name
		try {
			resource.getBranch(uriInfo, null, null, "invalid-repo", "main", new MockHttpServletRequest());
		} catch (NotFoundException e) {
			// expected
		}

		// case 2: invalid branch name
		try {
			resource.getBranch(uriInfo, null, null, RAPTOR_REPO, "invalid-main", new MockHttpServletRequest());
		} catch (NotFoundException e) {
			// expected
		}
	}

	@Test(expected = NotFoundException.class)
	public void testInvalidBatchGetQuery() {
		try {
			resource.batchGetEntities(new MockUriInfo(), "raptor-paas", "main", "ApplicationService.services", null,
					ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(), new MockHttpServletRequest());
		} catch (NotFoundException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test(expected = NotFoundException.class)
	public void testInvalidBatchGetQuery01() {
		try {
			resource.batchGetEntities(uriInfo, "raptor-paas-invalid", "main", APPLICATION_SERVICE, null,
					ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(), new MockHttpServletRequest());
		} catch (NotFoundException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Test
	public void testBatchGetQuery02() {
		uriInfo.getQueryParameters().remove(QueryResource.QueryParameterEnum.allowFullTableScan.toString());
		CMSResponse resp = (CMSResponse) resource.batchGetEntities(uriInfo, "raptor-paas", "main", APPLICATION_SERVICE,
				null, ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(),
				new MockHttpServletRequest()).getEntity();
		assertOk(resp);
	}

	@Test(expected = NotFoundException.class)
	public void testInvalidBatchGetQuery03() {
		resource.batchGetEntities(uriInfo, "raptor-paas", "main1", APPLICATION_SERVICE, null, ConsistentPolicy
				.safePolicy().getName(), CMSQueryMode.URI.toString(), new MockHttpServletRequest());
		Assert.fail();
	}

	@Test(expected = BadParamException.class)
	public void testInvalidModeQuery() {
		try {
			resource.batchGetEntities(uriInfo, "raptor-paas", "main", "ApplicationService.services", "priori",
					ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(), new MockHttpServletRequest());
		} catch (BadParamException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	@Test
    public void testBatchGetQueryWithPagination() {
        UriInfo ui = new MockUriInfo();
        CMSResponse resp = (CMSResponse) resource.batchGetEntities(ui, "raptor-paas", "main", SERVICE_INSTANCE,
                null, ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(resp);
        Long count = (Long)resp.get("count");
	    
        UriInfo ui1 = new MockUriInfo();
        MultivaluedMap<String, String> qParameter = ui1.getQueryParameters();
        qParameter.add("skip", StringUtils.join(new String[] {"0"}, ","));
        qParameter.add("limit", StringUtils.join(new String[] {"9"}, ","));
        qParameter.add(CMSResourceUtils.REQ_PAGINATION_MODE, PaginationMode.SKIP_BASED.name());
        
        resp = (CMSResponse) resource.batchGetEntities(ui1, "raptor-paas", "main", SERVICE_INSTANCE,
                null, ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(resp);
        
        Long count1 = (Long)resp.get("count");
        Assert.assertEquals(9, count1.intValue());
        Boolean hasMore = (Boolean)resp.get("hasmore");
        Assert.assertTrue(hasMore);
        JsonNode pageNode = (JsonNode) resp.get("pagination");
        Assert.assertNotNull(pageNode);
        ArrayNode skipNode = (ArrayNode)pageNode.get("skip");
        Assert.assertNotNull(skipNode);
        JsonNode hintNode = (JsonNode) pageNode.get("hint");
        Assert.assertNotNull(hintNode);
        List<Integer> skips = new ArrayList<Integer>();
        Iterator<JsonNode> it = skipNode.getElements();
        while (it.hasNext()) {
            skips.add(it.next().asInt());
        }
        Assert.assertEquals(1, skips.size());
        List<Integer> limits = new ArrayList<Integer>();
        ArrayNode limitNode = (ArrayNode)pageNode.get("limit");
        Assert.assertNotNull(limitNode);
        Iterator<JsonNode> it2 = limitNode.getElements();
        while (it2.hasNext()) {
            limits.add(it2.next().asInt());
        }
        Assert.assertEquals(1, limits.size());
        
        UriInfo ui2 = new MockUriInfo();
        MultivaluedMap<String, String> qParameter2 = ui2.getQueryParameters();
        qParameter2.add("skip", StringUtils.join(skips.toArray(), ","));
        qParameter2.add("limit", StringUtils.join(limits.toArray(), ","));
        qParameter2.add(CMSResourceUtils.REQ_PAGINATION_MODE, PaginationMode.SKIP_BASED.name());
        qParameter2.add(CMSResourceUtils.REQ_PAGINATION_MODE, PaginationMode.SKIP_BASED.name());
        resp = (CMSResponse) resource.batchGetEntities(ui2, "raptor-paas", "main", SERVICE_INSTANCE,
                null, ConsistentPolicy.safePolicy().getName(), CMSQueryMode.URI.toString(),
                new MockHttpServletRequest()).getEntity();
        assertOkAndNotNullResult(resp);
        hasMore = (Boolean)resp.get("hasmore");
        Assert.assertFalse(hasMore);
        Long count2 = (Long)resp.get("count");
        Assert.assertEquals(count - count1, count2.intValue());
        pageNode = (JsonNode) resp.get("pagination");
        Assert.assertNull(pageNode);
    }

	@SuppressWarnings("unchecked")
    @Test
	public void testDelete() {
		String entityStr = loadJson("/Compute.json");

		uriInfo.map.add("comment", "create comment");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", "Compute",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();

		assertOkAndNotNullResult(resp);
		List<String> oidList = (List<String>)resp.get(CMSResponse.RESULT_KEY);
		String newId = oidList.get(0);
		
		resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", newId,
				CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest())
				.getEntity();
		
		assertOkAndNotNullResult(resp);
		List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
		Assert.assertEquals(1, resultList.size());
		ObjectNode foundNode = resultList.get(0);
		Assert.assertEquals(newId, foundNode.get("_oid").getTextValue());
		
		resp = (CMSResponse)resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute",
				newId, null, null, CMSQueryMode.NORMAL.toString(),
				new MockHttpServletRequest()).getEntity();
		assertOk(resp);
		
		try {
			resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", newId,
					CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest())
					.getEntity();
			Assert.fail();
		} catch (NotFoundException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testCreate() {
		String entityStr = loadJson("/Compute.json");

		uriInfo.map.add("comment", "create comment");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", "Compute",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();

		assertOkAndNotNullResult(resp);
	}

	@Test
	public void testCreate_invalidpolicy() {
		String entityStr = loadJson("/Compute.json");

		uriInfo.map.add("comment", "create comment");
		try {
			resource.createEntity(uriInfo, "raptor-paas", "main", "Compute", CMSPriority.NEUTRAL.toString(),
					"invalid-policy", entityStr, CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (BadParamException e) {
			// expected
		}
	}

	@Test
	public void testCreateNullReference() {
		String serviceString = "{\"name\" :\"new-service-name....\", \"runsOn\" : null}";
		try {
			resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ServiceInstance", nullPriority,
					nullConsisPolicy, serviceString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException bpe) {
			// expected
		}
	}

	@Test
	public void testCreateNullReference2() {
		// for array, when given null, store them as empty list
		String serviceString = "{\"name\" :\"new-app-service-name....\", \"services\" : null }";
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				"ApplicationService", nullPriority, nullConsisPolicy, serviceString, nullMode, mockHttpRequest)
				.getEntity();
		assertOk(resp);
	}

	@Test
	public void testCreateNullReference3() {
		String serviceString = "{\"name\" :\"new-app-service-name....\", \"services\" : [null]}";
		try {
			resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
					nullConsisPolicy, serviceString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException bpe) {
			// expected
		}
	}

	@Test
	public void testCreateStandaloneEmbed() {
		String entityStr = "{\"name\":\"cms-test-user\"}";

		uriInfo.map.add("comment", "create comment");
		try {
			resource.createEntity(uriInfo, "software-deployment", "main", "NoUse", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException cse) {
			// expected
			Assert.assertTrue(cse.getCause() instanceof CmsDalException);
			Assert.assertEquals(CmsDalException.DalErrCodeEnum.STANDALONE_EMBED,
					((CmsDalException) cse.getCause()).getErrorEnum());
		}
	}

	@Test
	public void testReplaceStandaloneEmbed() {
		String entityStr = "{\"name\":\"cms-test-user\", \"_oid\":\"replace-oid\"}";

		uriInfo.map.add("comment", "create comment");
		try {
			resource.replaceEntity(uriInfo, "software-deployment", "main", "NoUse", "replace-oid",
					CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().toString(), entityStr,
					CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (BadParamException e) {
			// expected
		}
		// } catch (CMSServerException cse) {
		// // expected
		// Assert.assertTrue(cse.getCause() instanceof CmsDalException);
		// Assert.assertEquals(CmsDalException.DalErrCodeEnum.STANDALONE_EMBED,
		// ((CmsDalException) cse.getCause()).getErrorEnum());
		// }
	}

	@Test
	public void testBatchCreate() {
		try {
			resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute",
					"compute-00001.lvs.ebay.com-id-1337667919417", null, null, CMSQueryMode.NORMAL.toString(),
					new MockHttpServletRequest());
		} catch (Exception e) {
			// ignore
		}

		String entityStr = loadJson("/Computes.json");

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");
		server.setEntityOperationCallback(new ContextCheckCallback());
		CMSResponse resp = (CMSResponse) resource.batchCreateEntities(uriInfo, "raptor-paas", "main",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), BatchOperationFailReturnOption.IMMEDIATE.toString(),
				new MockHttpServletRequest()).getEntity();
		server.setEntityOperationCallback(null);
		assertOkAndNotNullResult(resp);
	}

	@Test
	public void testBatchCreate2() {
		try {
			resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute",
					"compute-00001.lvs.ebay.com-id-1337667919417", null, null, CMSQueryMode.NORMAL.toString(),
					new MockHttpServletRequest());
		} catch (Exception e) {
			// ignore
		}

		String entityStr = loadJson("/Computes.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", "entities",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();

		assertOkAndNotNullResult(resp);
	}

	@Test(expected = CMSServerException.class)
	public void testBatchCreateInvalid01() {
		String entityStr = "{]";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}
	
    @Test
    public void testBatchCreateInvalid02() {
        String entityStr = loadJson("/batch-create-invalid-json.json");

        MockUriInfo uriInfo = new MockUriInfo();
        uriInfo.map.add("comment", "create comment");
        uriInfo.map.add("X-SECURITY-USER", "_CI_USER");
        server.setEntityOperationCallback(new ContextCheckCallback());
        try {
            resource.batchCreateEntities(uriInfo, "raptor-paas", "main",
                CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
                CMSQueryMode.NORMAL.toString(), BatchOperationFailReturnOption.ALL.toString(),
                new MockHttpServletRequest()).getEntity();
            Assert.fail();
        } catch (CMSServerException e) {
            CmsEntMgrException entExp = (CmsEntMgrException)e.getCause();
            Assert.assertEquals(EntMgrErrCodeEnum.BATCH_OPERATION_PARTIAL_FAILURE.getErrorCode(), entExp.getErrorCode());
        } finally {
            server.setEntityOperationCallback(null);
        }
    }

	@Test(expected = BadParamException.class)
	public void testBatchCreateInvalid2() {
		String entityStr = "{}";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}

	@Test(expected = BadParamException.class)
	public void testBatchCreateInvalid3() {
		String entityStr = "[{}]";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}

	@Test(expected = NotFoundException.class)
	public void testBatchCreateInvalid4() {
		String entityStr = "[{\"_type\" : \"invalid-meta\"}]";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}

	@Test(expected = NotFoundException.class)
	public void testBatchCreateInvalid5() {
		String entityStr = "[{\"_type\" : \"invalid-meta\"}]";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas-invalid", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}

	@Test(expected = BadParamException.class)
	public void testBatchCreateInvalid6() {
		String entityStr = "[{\"_type\" : \"\"}]";
		resource.batchCreateEntities(new MockUriInfo(), "raptor-paas-invalid", "main", CMSPriority.NEUTRAL.toString(),
				ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
				BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
	}

	@Test
	public void testBatchUpdate() {
		String entityStr = loadJson("/ComputesNew.json");
		CMSResponse resp = (CMSResponse) resource.batchCreateEntities(uriInfo, "raptor-paas", "main",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), BatchOperationFailReturnOption.IMMEDIATE.toString(),
				new MockHttpServletRequest()).getEntity();
		assertOkAndNotNullResult(resp);

		resp = (CMSResponse) resource.batchModifyEntities(new MockUriInfo(), "raptor-paas", "main",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), BatchOperationFailReturnOption.IMMEDIATE.toString(),
				new MockHttpServletRequest()).getEntity();
		assertOk(resp);
	}

	@SuppressWarnings("unchecked")
    @Test
	public void testReplace() {
		String entityStr = loadJson("/Compute.json");

		uriInfo.map.add("comment", "create comment");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", "Compute",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();

		assertOkAndNotNullResult(resp);
		List<String> oidList = (List<String>)resp.get(CMSResponse.RESULT_KEY);
		String newId = oidList.get(0);
		
		resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", newId,
				CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest())
				.getEntity();
		
		assertOkAndNotNullResult(resp);
		List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
		Assert.assertEquals(1, resultList.size());
		ObjectNode foundNode = resultList.get(0);
		Assert.assertEquals(newId, foundNode.get("_oid").getTextValue());
		
		String jsonString = "{\"_branch\": \"main\",\"_type\": \"Compute\",\"location\": \"SHA\",\"name\": \"compute-00001-replace\",\"label\": \"compute-00001-replace\",\"fqdn\": \"compute-00001-replace.lvs.ebay.com\",\"assetStatus\": \"maintenance\"}";
		resp = (CMSResponse)resource.replaceEntity(uriInfo, "raptor-paas", "main", "Compute", newId, 
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), jsonString, CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();
		
		assertOkAndNotNullResult(resp);
		resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
		Assert.assertEquals(1, resultList.size());
		
		resp = (CMSResponse) resource.getEntity(uriInfo, "raptor-paas", "main", "Compute", newId,
				CMSPriority.NEUTRAL.toString(), null, CMSQueryMode.URI.toString(), new MockHttpServletRequest())
				.getEntity();
		resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
		Assert.assertEquals(1, resultList.size());
		foundNode = resultList.get(0);
		Assert.assertEquals(newId, foundNode.get("_oid").getTextValue());
		Assert.assertEquals("SHA", foundNode.get("location").getTextValue());
		Assert.assertEquals("compute-00001-replace", foundNode.get("name").getTextValue());
		Assert.assertEquals("compute-00001-replace", foundNode.get("label").getTextValue());
		Assert.assertEquals("maintenance", foundNode.get("assetStatus").getTextValue());
		Assert.assertEquals("compute-00001-replace.lvs.ebay.com", foundNode.get("fqdn").getTextValue());
	}

	@Test
	public void testReplaceConstant() {
		String jsonString = "{ \"_oid\": \"employ-id-000001\", \"name\" : \"Thomas Edison\", \"company\": \"GE\"}";
		resource.replaceEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "Employee", "employ-id-000001",
				nullPriority, nullConsisPolicy, jsonString, nullMode, mockHttpRequest);
	}

	protected JsonEntity newNetworkAddress(String branchId, int seq) {
		String metaType = NETWORK_ADDRESS;
		MetaClass instCls = CMSServer.getCMSServer().getMetaClass(CMSPriority.NON_CRITICAL, STRATUS_REPO, metaType);
		JsonEntity newEntity = new JsonEntity(instCls);
		newEntity.setBranchId(branchId);
		newEntity.addFieldValue("ipaddress", "10.249.64.99");
		newEntity.addFieldValue("hostname", "vip.ebay.com");
		newEntity.addFieldValue("zone", "corp");
		newEntity.addFieldValue(_802_ADDRESS, "00:e0:ce:af:" + seq);
		return newEntity;
	}

	@Test
	public void modifyConstant() {
		String jsonString = "{ \"_oid\": \"employ-id-000001\", \"name\" : \"Thomas Edison\", \"company\": \"GE-Auto\"}";
		try {
			resource.modifyEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "Employee", "employ-id-000001",
					nullPriority, nullConsisPolicy, jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (CMSServerException e) {
			// expected
			Assert.assertEquals(500, e.getResponse().getStatus());
			CMSResponse resp = (CMSResponse) e.getResponse().getEntity();
			assertErrorCode(CmsDalException.DalErrCodeEnum.CONSTANT_FIELD_MODIFICATION.getErrorCode(), resp);
		}
	}

	public static class ContextCheckCallback implements IEntityOperationCallback {

		@Override
		public boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context) {
			Assert.assertNotNull(context);
			Assert.assertNotNull(context.getSubject());
			Assert.assertNotNull(context.getComment());
			Assert.assertNotNull(context.getRequest());
			return true;
		}
	}

    @Test
    public void modifyInvalidField() {
        try {
            resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
                RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, "invalid-oid", "invalidField", nullPriority, nullConsisPolicy, "false",
                "", nullMode, mockHttpRequest).getEntity();
            Assert.fail();
        } catch (NotFoundException e) {
            
        }
    }
    
	@Test
	public void modifyArrayField() {
		String entityStr = loadJson("/ServiceInstance01.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-001";

		String jsonString = "[\"mainfest-4.0\"]";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "manifestRef", nullPriority, nullConsisPolicy, "false",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode manifestRefs = (JsonNode) jEntity.get("manifestRef");

		Assert.assertNotNull(manifestRefs);
		Assert.assertEquals(4, manifestRefs.size());
		
		// pull string field by passing an array
		jsonString = "[]";
		try {
			resource.pullEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "name", nullPriority, nullConsisPolicy, 
				jsonString, nullMode, mockHttpRequest).getEntity();
			Assert.fail();
		} catch (BadParamException e) {
			Assert.assertEquals("java.lang.IllegalArgumentException: The given value of MetaField name is an array", e.getMessage());
		}
	}

	@Test
	public void modifyStringArrayFieldWithoutPayload() {
		String entityStr = loadJson("/ServiceInstance04.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-004";

		String jsonString = "";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "manifestRef", nullPriority, nullConsisPolicy, "false",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode manifestRefs = (JsonNode) jEntity.get("manifestRef");

		Assert.assertNotNull(manifestRefs);
		Assert.assertEquals(4, manifestRefs.size());
	}

	@Test
	public void modifyRelationshipManyFieldWithoutPayload() {
		String entityStr = loadJson("/Environment02.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				ENVIRONMENT, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-environment-002";

		String jsonString = "";
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"applications", nullPriority, nullConsisPolicy, "false", jsonString, nullMode, mockHttpRequest)
					.getEntity();
			Assert.fail();
		} catch (BadParamException e) {
			Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
			Assert.assertTrue(e.getMessage().contains(
					"Field 'applications' should be Object. But the value is org.codehaus.jackson.node.TextNode"));
		}
	}

	@Ignore
	//
	@Test
	@SuppressWarnings("unchecked")
	public void modify_ToSetArrayAsEmpty() {
		JsonEntity appEntity = new JsonEntity(server.getMetaClass(CMSPriority.NEUTRAL, RAPTOR_REPO, SERVICE_INSTANCE));
		String fieldName = "manifestRef";
		appEntity.addFieldValue(fieldName, "1");
		appEntity.addFieldValue(fieldName, "2");

		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, appEntity.toString(), nullMode, mockHttpRequest)
				.getEntity();
		assertOkAndNotNullResult(resp);
		String id = ((List<String>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(id);

		appEntity.setId(id);
		((ArrayNode) appEntity.getNode().get(fieldName)).removeAll();
		System.out.println(appEntity.getNode());

		resp = (CMSResponse) resource.modifyEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE,
				id, nullPriority, nullConsisPolicy, appEntity.toString(), nullMode, mockHttpRequest).getEntity();
		assertOk(resp);

		resp = (CMSResponse) resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, id,
				nullPriority, nullConsisPolicy, nullConsisPolicy, mockHttpRequest).getEntity();
		ObjectNode getEntityNode = (ObjectNode) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(getEntityNode);
		Assert.assertTrue(getEntityNode.has(fieldName));
		Assert.assertEquals(0, ((ArrayNode) getEntityNode.get(fieldName)).size());
	}

	@Test
	public void modifyEmbedArray() {
		// TODO
	}

	@Test
	public void modifyArrayFieldWithNonArrayPayload() {
		String entityStr = loadJson("/ServiceInstance011.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-011";

		// NOTE: the payload not an array now
		String jsonString = "\"mainfest-4.0\"";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "manifestRef", nullPriority, nullConsisPolicy, "false",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode manifestRefs = (JsonNode) jEntity.get("manifestRef");

		Assert.assertNotNull(manifestRefs);
		Assert.assertEquals(4, manifestRefs.size());
	}

	@Test
	public void modifyJsonField() {
		String entityStr = loadJson("/ServiceInstance02.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-web-resource-test-002";

		String jsonString = "{ \"f3\" : 10, \"fnew\": \"new property in side JSON type\"}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "properties", nullPriority, nullConsisPolicy, "false",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		POJONode pojoProp = (POJONode) jEntity.get("properties");
		BasicDBObject prop = (BasicDBObject) pojoProp.getPojo();
		Assert.assertEquals(10, prop.get("f3"));
		Assert.assertNotNull(prop.get("fnew"));
	}

	@Test
	public void casModifyStringField() {
		String entityStr = loadJson("/ServiceInstance05.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-005";

		// case 1: CAS with the correct original value
		String jsonString = "{ \"oldValue\" : \"up\", \"newValue\": \"down\"}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "healthStatus", nullPriority, nullConsisPolicy, "true",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode health = (JsonNode) jEntity.get("healthStatus");

		Assert.assertNotNull(health);
		Assert.assertEquals("down", health.getTextValue());

		// case 2: CAS with the incorrect original value
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId,
					"healthStatus", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
		} catch (CMSServerException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CmsEntMgrException) {
				Assert.assertEquals(((CmsEntMgrException) cause).getErrorEnum(),
						EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED);
			} else {
				Assert.fail();
			}
		}

		// case 3: CAS with the incorrect data type
		try {
			jsonString = "{ \"oldValue\" : 1024, \"newValue\": 2048}";
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId,
					"healthStatus", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			CMSResponse response = (CMSResponse) e.getResponse().getEntity();
			Error err = (Error) response.get(CMSResponse.STATUS_KEY);
			Assert.assertEquals("the value type should be string!", err.getMsg());
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@Test
	public void casModifyEnumField() {
		String entityStr = loadJson("/Environment01.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				ENVIRONMENT, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-environment-001";

		// case 1: CAS with the correct original value
		String jsonString = "{ \"oldValue\" : \"DONE\", \"newValue\": \"Doing\"}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, "actionStatus", nullPriority, nullConsisPolicy, "true",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, nullPriority, nullConsisPolicy, nullMode, mockHttpRequest)
				.getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode actionStatus = (JsonNode) jEntity.get("actionStatus");

		Assert.assertNotNull(actionStatus);
		Assert.assertEquals("Doing", actionStatus.getTextValue());

		// case 2: CAS with the incorrect original value
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"actionStatus", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
		} catch (CMSServerException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CmsEntMgrException) {
				Assert.assertEquals(((CmsEntMgrException) cause).getErrorEnum(),
						EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED);
			} else {
				Assert.fail();
			}
		}

		// case 3: CAS with the incorrect data type
		try {
			jsonString = "{ \"oldValue\" : 1024, \"newValue\": 2048}";
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"actionStatus", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			CMSResponse response = (CMSResponse) e.getResponse().getEntity();
			Error err = (Error) response.get(CMSResponse.STATUS_KEY);
			Assert.assertEquals("the value type should be enum!", err.getMsg());
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@Test
	public void casModifyBooleanField() {
		String entityStr = loadJson("/ServiceInstance05.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-005";

	    // case 0: CAS with invalid json
        String jsonString = "\"oldValue\" : false, \"newValue\": true";
        try {
            resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
                RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "https", nullPriority, nullConsisPolicy, "true",
                jsonString, nullMode, mockHttpRequest).getEntity();
            Assert.fail();
        } catch (BadParamException e) {
            
        }
        
		// case 1: CAS with the correct original value
		jsonString = "{ \"oldValue\" : false, \"newValue\": true}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "https", nullPriority, nullConsisPolicy, "true",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode https = (JsonNode) jEntity.get("https");

		Assert.assertNotNull(https);
		Assert.assertTrue(https.asBoolean());

		// case 2: CAS with the incorrect original value
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId,
					"https", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (CMSServerException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CmsEntMgrException) {
				Assert.assertEquals(((CmsEntMgrException) cause).getErrorEnum(),
						EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED);
			} else {
				Assert.fail();
			}
		}

		// case 3: CAS with the incorrect data type
		try {
			jsonString = "{ \"oldValue\" : \"true\", \"newValue\": \"false\"}";
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId,
					"https", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			CMSResponse response = (CMSResponse) e.getResponse().getEntity();
			Error err = (Error) response.get(CMSResponse.STATUS_KEY);
			Assert.assertEquals("the value type should be boolean!", err.getMsg());
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@Test
	public void casModifyIntegerField() {
		String entityStr = loadJson("/Environment01.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				ENVIRONMENT, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-environment-001";

		// case 1: CAS with the correct original value
		String jsonString = "{ \"oldValue\" : 1024, \"newValue\": 2048}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, "numService", nullPriority, nullConsisPolicy, "true",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, nullPriority, nullConsisPolicy, nullMode, mockHttpRequest)
				.getEntity();
		assertOkAndNotNullResult(getResp);

		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode numService = (JsonNode) jEntity.get("numService");
		Assert.assertNotNull(numService);
		Assert.assertEquals(2048, numService.asInt());

		// check js expression
		JsonNode total = (JsonNode) jEntity.get("total");
		Assert.assertNotNull(total);
		Assert.assertEquals(3072, total.asLong());

		// case 2: CAS with the incorrect original value
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"numService", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (CMSServerException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CmsEntMgrException) {
				Assert.assertEquals(((CmsEntMgrException) cause).getErrorEnum(),
						EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED);
			} else {
				Assert.fail();
			}
		}

		// case 3: CAS with the incorrect data type
		try {
			jsonString = "{\"oldValue\" : \"2048\", \"newValue\": \"1024\"}";
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"numService", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			CMSResponse response = (CMSResponse) e.getResponse().getEntity();
			Error err = (Error) response.get(CMSResponse.STATUS_KEY);
			Assert.assertEquals("the value type should be integer!", err.getMsg());
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@Test
	public void casModifyLongField() {
		String entityStr = loadJson("/Environment01.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				ENVIRONMENT, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-environment-001";

		// case 1: CAS with the correct original value
		String jsonString = "{ \"oldValue\" : 1024, \"newValue\": 2048}";
		CMSResponse modifyResp = (CMSResponse) resource.modifyEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, "numServices", nullPriority, nullConsisPolicy, "true",
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(modifyResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, nullPriority, nullConsisPolicy, nullMode, mockHttpRequest)
				.getEntity();
		assertOkAndNotNullResult(getResp);

		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		JsonNode numServices = (JsonNode) jEntity.get("numServices");
		Assert.assertNotNull(numServices);
		Assert.assertEquals(2048, numServices.asLong());

		// case 2: CAS with the incorrect original value
		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"numServices", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
		} catch (CMSServerException e) {
			Throwable cause = e.getCause();
			if (cause instanceof CmsEntMgrException) {
				Assert.assertEquals(((CmsEntMgrException) cause).getErrorEnum(),
						EntMgrErrCodeEnum.CONDITIONAL_UPDATE_FAILED);
			} else {
				Assert.fail();
			}
		}

		// case 3: CAS with the incorrect data type
		try {
			jsonString = "{\"oldValue\" : \"2048\", \"newValue\": \"1024\"}";
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId,
					"numServices", nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			CMSResponse response = (CMSResponse) e.getResponse().getEntity();
			Error err = (Error) response.get(CMSResponse.STATUS_KEY);
			Assert.assertEquals("the value type should be long!", err.getMsg());
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@Test
	public void casModifyRelationshipField() {
		String entityStr = loadJson("/Environment01.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				ENVIRONMENT, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-test-in-web-resource-environment-001";
		String jsonString = "{ \"oldValue\" : {\"_oid\": \"4fbb314fc681caf13e283a77\"}, \"newValue\": {\"_oid\": \"4fbb314fc681caf13e283a88\"}}";

		try {
			resource.modifyEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, "cos",
					nullPriority, nullConsisPolicy, "true", jsonString, nullMode, mockHttpRequest);
		} catch (BadParamException e) {
			Assert.assertTrue(true);
		} catch (Exception e) {
			Assert.fail();
		}

		resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, ENVIRONMENT, newId, null, null,
				CMSQueryMode.NORMAL.toString(), mockHttpRequest);
	}

	@SuppressWarnings("rawtypes")
    @Test
	public void deleteArrayField() {
		String entityStr = loadJson("/ServiceInstance03.json");
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = "oid-web-resource-test-003";

		String jsonString = "[\"manifest-5.0\"]";
		Response response = resource.deleteEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "manifestRef", nullPriority, nullConsisPolicy,
				jsonString, nullMode, mockHttpRequest);
		CMSResponse deleteResp = (CMSResponse)response.getEntity();
		assertOkAndNotNullResult(deleteResp);
		List list = (List) response.getMetadata().get("x-ebay-tracking-code");
		Assert.assertEquals(1, list.size());
		Assert.assertEquals(203, list.get(0));

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);
		ObjectNode jEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);
		ArrayNode manifest = (ArrayNode) jEntity.get("manifestRef");

		// assert deletion
		Assert.assertNotNull(manifest);
		Assert.assertEquals(2, manifest.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void deleteNotExistingField() {
		String entityStr = "{\"name\": \"service-name-randon....\"}";
		CMSResponse resp = (CMSResponse) resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, nullPriority, nullConsisPolicy, entityStr, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		String newId = ((List<String>) resp.get(CMSResponse.RESULT_KEY)).get(0);

		// delete non-existing array
		String jsonString = "[\"manifest-5.0\"]";
		CMSResponse deleteResp = (CMSResponse) resource.deleteEntityField(nullMockUri, RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, "manifestRef", nullPriority, nullConsisPolicy,
				jsonString, nullMode, mockHttpRequest).getEntity();
		assertOkAndNotNullResult(deleteResp);

		CMSResponse getResp = (CMSResponse) resource.getEntity(new NormalModeInfo(), RAPTOR_REPO,
				RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, newId, nullPriority, nullConsisPolicy, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(getResp);

		// delete non-existing array
		deleteResp = (CMSResponse) resource.deleteEntityField(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				SERVICE_INSTANCE, newId, "activeManifestDiff", nullPriority, nullConsisPolicy, null, nullMode,
				mockHttpRequest).getEntity();
		assertOkAndNotNullResult(deleteResp);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCreateWithParameters() {
		String entityStr = "{ \"name\" : \"unit-test-create-compute-001\", \"fqdn\" : \"fqdn-create-by-unit-test\"}";

		MockUriInfo uri = new MockUriInfo();
		uri.map.add(CMSResourceUtils.REQ_PARAM_COMMENT, "create comment");
		uri.map.add(CMSResourceUtils.REQ_PARAM_COMPONENT, "Unit-test");
		uri.map.add(CMSResourceUtils.REQ_PARAM_UID, "Unit-test-user");
		CMSResponse resp = (CMSResponse) resource.createEntity(uri, "raptor-paas", "main", "Compute",
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();

		assertOkAndNotNullResult(resp);
		String newId = ((List<String>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		CMSResponse getResp = (CMSResponse) resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				"Compute", newId, nullPriority, nullConsisPolicy, CMSQueryMode.NORMAL.toString(), mockHttpRequest)
				.getEntity();
		assertOkAndNotNullResult(resp);

		ObjectNode createdEntity = (ObjectNode) ((List<?>) getResp.get(CMSResponse.RESULT_KEY)).get(0);

		Assert.assertTrue(createdEntity.has(InternalFieldEnum.USER.getName()));
		Assert.assertTrue(createdEntity.get(InternalFieldEnum.USER.getName()) != null);
		Assert.assertTrue(createdEntity.has(InternalFieldEnum.COMMENT.getName()));
		Assert.assertTrue(createdEntity.get(InternalFieldEnum.COMMENT.getName()) != null);
		Assert.assertTrue(createdEntity.has(InternalFieldEnum.MODIFIER.getName()));
		Assert.assertTrue(createdEntity.get(InternalFieldEnum.MODIFIER.getName()) != null);
		System.out.println(createdEntity);
	}

	@Test
	public void testJsonTypeWithNormalMode() {
		MockUriInfo uri = new MockUriInfo();
		uri.map.add("mode", "normal");
		MockHttpServletRequest request = new MockHttpServletRequest();
		CMSResponse resp = (CMSResponse) resource.getEntity(uri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE,
				"4fbb314fc681caf13e283a8a", nullPriority, nullConsisPolicy, "uri", request).getEntity();

		assertOkAndNotNullResult(resp);
		ObjectNode node = (ObjectNode) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(node);
		Assert.assertNotNull(node.has("properties"));
		Assert.assertTrue(node.get("properties") instanceof POJONode);
		Assert.assertFalse(((POJONode) node.get("properties")).getPojo() instanceof POJONode);
		Assert.assertTrue(((POJONode) node.get("properties")).getPojo() instanceof BasicDBObject);
	}

	@Test
	public void testJsonTypeWithURIMode() {
		MockUriInfo uri = new MockUriInfo();
		uri.map.add("mode", "uri");
		MockHttpServletRequest request = new MockHttpServletRequest();
		CMSResponse resp = (CMSResponse) resource.getEntity(uri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE,
				"4fbb314fc681caf13e283a8a", nullPriority, nullConsisPolicy, "uri", request).getEntity();

		assertOkAndNotNullResult(resp);
		ObjectNode node = (ObjectNode) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(node);
		Assert.assertNotNull(node.has("properties"));
		Assert.assertTrue(node.get("properties") instanceof POJONode);
		Assert.assertFalse(((POJONode) node.get("properties")).getPojo() instanceof POJONode);
		Assert.assertTrue(((POJONode) node.get("properties")).getPojo() instanceof BasicDBObject);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testJsonTypeWithURIMode2() {
		MockUriInfo uri = new MockUriInfo();
		uri.map.add("mode", "uri");
		MockHttpServletRequest request = new MockHttpServletRequest();

		String entityStr = "{\"properties\":{}, \"name\":\"json-type-service-name\"}";
		uriInfo.map.add("comment", "create comment");
		CMSResponse resp = (CMSResponse) resource.createEntity(uriInfo, "raptor-paas", "main", SERVICE_INSTANCE,
				CMSPriority.NEUTRAL.toString(), ConsistentPolicy.safePolicy().getName(), entityStr,
				CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest()).getEntity();
		String id = ((List<String>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(id);

		resp = (CMSResponse) resource.getEntity(uri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, SERVICE_INSTANCE, id,
				nullPriority, nullConsisPolicy, "uri", request).getEntity();

		assertOkAndNotNullResult(resp);
		ObjectNode node = (ObjectNode) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(node);
		Assert.assertNotNull(node.has("properties"));
		Assert.assertTrue(node.get("properties") instanceof POJONode);
		Assert.assertFalse(((POJONode) node.get("properties")).getPojo() instanceof POJONode);
		System.out.println(((List<?>) resp.get(CMSResponse.RESULT_KEY)));

		// now query
		QueryResource queryResource = new QueryResource();
		resp = (CMSResponse)queryResource.query(uri, nullPriority, nullConsisPolicy, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID,
				"ServiceInstance[@name=\"json-type-service-name\"]", uriInfo, "uri", mockHttpRequest).getEntity();
		assertOkAndNotNullResult(resp);
		node = (ObjectNode) ((List<?>) resp.get(CMSResponse.RESULT_KEY)).get(0);
		Assert.assertNotNull(node);
		Assert.assertNotNull(node.has("properties"));
		Assert.assertTrue(node.get("properties") instanceof POJONode);
		Assert.assertFalse(((POJONode) node.get("properties")).getPojo() instanceof POJONode);
		System.out.println(((List<?>) resp.get(CMSResponse.RESULT_KEY)));
	}

	@Test
	public void testCreate_IncorrectType() {
		String appStr = "{ \"name\": \"appName\", \"_type\": \"V3\"}";
		try {
			resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
					nullConsisPolicy, appStr, nullMode, mockHttpRequest);
			Assert.fail();
		} catch (BadParamException e) {
			// expected
		}
	}
	
	@Test
    @SuppressWarnings("unchecked")
    public void testCreate_StringWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"nugget\":null}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        // doesn't has field named "nugget"
        Assert.assertFalse(foundNode.has("nugget"));
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }
	
	@Test
    @SuppressWarnings("unchecked")
    public void testModify_StringWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\"}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"nugget\":null}";
        resp = (CMSResponse)resource.modifyEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        
        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        // doesn't has field named "nugget"
        Assert.assertFalse(foundNode.has("nugget"));
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }
	
	@Test
    @SuppressWarnings("unchecked")
    public void testReplace_StringWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\"}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"nugget\":null}";
        resp = (CMSResponse)resource.replaceEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        
        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        // doesn't has field named "nugget"
        Assert.assertFalse(foundNode.has("nugget"));
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }
	
    @Test
    @SuppressWarnings("unchecked")
    public void testCreate_StringArrayWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"preManifestRef\":[null,null]}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        Assert.assertTrue(foundNode.get("preManifestRef").isArray());
        ArrayNode arr = (ArrayNode)foundNode.get("preManifestRef");
        Assert.assertEquals(0, arr.size());
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testModify_StringArrayWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\"}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"preManifestRef\":[null,null]}";
        resp = (CMSResponse)resource.modifyEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        
        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        Assert.assertTrue(foundNode.get("preManifestRef").isArray());
        ArrayNode arr = (ArrayNode)foundNode.get("preManifestRef");
        Assert.assertEquals(0, arr.size());
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testReplace_StringArrayWithNull() {
        String appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\"}";
        CMSResponse resp = (CMSResponse)resource.createEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        List<String> node = (List<String>) resp.get("result");
        String newId = (String) node.get(0);

        appStr = "{ \"name\": \"appName-StringArray\", \"_type\": \"ApplicationService\", \"preManifestRef\":[null,null]}";
        resp = (CMSResponse)resource.replaceEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, appStr, nullMode, mockHttpRequest).getEntity();
        
        resp = (CMSResponse)resource.getEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest).getEntity();
        
        List<ObjectNode> resultList = (List<ObjectNode>)resp.get(CMSResponse.RESULT_KEY);
        Assert.assertEquals(1, resultList.size());
        ObjectNode foundNode = resultList.get(0);
        Assert.assertTrue(foundNode.get("preManifestRef").isArray());
        ArrayNode arr = (ArrayNode)foundNode.get("preManifestRef");
        Assert.assertEquals(0, arr.size());
        
        resource.deleteEntity(nullMockUri, RAPTOR_REPO, RAPTOR_MAIN_BRANCH_ID, "ApplicationService", newId, nullPriority,
                nullConsisPolicy, nullMode, mockHttpRequest);
    }

	public static class UnthorizedCheckCallback implements IEntityOperationCallback {

		@Override
		public boolean preOperation(IEntity existingEntity, Operation op, IEntity newEntity, CallbackContext context) {
			throw new WebApplicationException(401);
		}
	}

	@Test
	public void callback_Unthorized() {
		String entityStr = loadJson("/Computes.json");

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");
		server.setEntityOperationCallback(new UnthorizedCheckCallback());
		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					BatchOperationFailReturnOption.IMMEDIATE.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (WebApplicationException wae) {
			Assert.assertEquals(401, wae.getResponse().getStatus());
		} finally {
			server.setEntityOperationCallback(null);
		}
	}

	@Test
	public void testBatchCreateFailure_Immediate() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.IMMEDIATE;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest()).getEntity();
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsDalException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.dal.exception.CmsDalException: batch create failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
	}

	@Test
	public void testBatchCreateFailure_All() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.ALL;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch create failure: [create entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main.]. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
	}

	@Test
	public void testBatchUpdateFailure_Immediate() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		// setup data for batch update, reuse testBatchCreateFailure test case
		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.ALL;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch create failure: [create entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main.]. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}

		// start batch update
		entityStr = loadJson("/ComputesNewForBatchUpdateFailure.json");
		option = BatchOperationFailReturnOption.IMMEDIATE;
		try {
			resource.batchModifyEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch modify failure: error code is 10023 and error message is Field 'label' should be string. But the value is 12345678. The following entities have been modified: [compute-00001.lvs.ebay.com-id-1347667919417]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
	}

	@Test
	public void testBatchUpdateFailure_All() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		// setup data for batch update, reuse testBatchCreateFailure test case
		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.ALL;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch create failure: [create entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main.]. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}

		// start batch update
		entityStr = loadJson("/ComputesNewForBatchUpdateFailure.json");
		option = BatchOperationFailReturnOption.ALL;
		try {
			resource.batchModifyEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch modify failure: [modify entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 10023 and error message is Field 'label' should be string. But the value is 12345678.]. The following entities have been modified: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
	}

	@Test
	public void testBatchDeleteFailure_Immediate() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		// setup data for batch delete, reuse testBatchCreateFailure test case
		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.ALL;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch create failure: [create entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main.]. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}

		// start batch delete
		entityStr = loadJson("/ComputesForBatchDeleteFailure.json");
		option = BatchOperationFailReturnOption.IMMEDIATE;
		try {
			resource.batchDeleteEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (NotFoundException e) {
			Assert.assertEquals(CmsDalException.class, e.getCause().getClass());
//			String expectedErrMsg = "com.ebay.cloud.cms.dal.exception.CmsDalException: batch delete failure: error code is 1004 and error message is entity {\"_branch\":\"main\",\"_type\":\"Compute\",\"_oid\":\"compute-00004.lvs.ebay.com-id-1347667919414\"} does not exist in main branch main. The following entities have been deleted: [compute-00001.lvs.ebay.com-id-1347667919417]";
//			Assert.assertEquals(e.get, e.getMessage());
		}
	}

	@Test
	public void testBatchDeleteFailure_All() {
		String[] oids = { "compute-00001.lvs.ebay.com-id-1347667919417", "compute-00002.lvs.ebay.com-id-1347667919418",
				"compute-00003.lvs.ebay.com-id-1347667919419" };

		for (String oid : oids) {
			try {
				resource.deleteEntity(new MockUriInfo(), RAPTOR_REPO, "main", "Compute", oid, null, null,
						CMSQueryMode.NORMAL.toString(), new MockHttpServletRequest());
			} catch (Exception e) {
				// ignore
				e.printStackTrace();
			}
		}

		// setup data for batch delete, reuse testBatchCreateFailure test case
		String entityStr = loadJson("/ComputesForBatchCreateFailure.json");
		BatchOperationFailReturnOption option = BatchOperationFailReturnOption.ALL;

		MockUriInfo uriInfo = new MockUriInfo();
		uriInfo.map.add("comment", "create comment");
		uriInfo.map.add("X-SECURITY-USER", "_CI_USER");

		try {
			resource.batchCreateEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch create failure: [create entity type Compute with oid compute-00002.lvs.ebay.com-id-1347667919418 failure: error code is 1016 and error message is entity compute-00002.lvs.ebay.com-id-1347667919418 already exists in branch main.]. The following entities have been created: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00002.lvs.ebay.com-id-1347667919418, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}

		// start batch delete
		entityStr = loadJson("/ComputesForBatchDeleteFailure.json");
		option = BatchOperationFailReturnOption.ALL;
		try {
			resource.batchDeleteEntities(uriInfo, "raptor-paas", "main", CMSPriority.NEUTRAL.toString(),
					ConsistentPolicy.safePolicy().getName(), entityStr, CMSQueryMode.NORMAL.toString(),
					option.toString(), new MockHttpServletRequest());
			Assert.fail();
		} catch (CMSServerException e) {
			Assert.assertEquals(CmsEntMgrException.class, e.getCause().getClass());
			String expectedErrMsg = "com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException: batch delete failure: [delete entity type Compute with oid compute-00004.lvs.ebay.com-id-1347667919414 failure: error code is 1004 and error message is entity {\"_branch\":\"main\",\"_type\":\"Compute\",\"_oid\":\"compute-00004.lvs.ebay.com-id-1347667919414\"} does not exist in main branch main.]. The following entities have been deleted: [compute-00001.lvs.ebay.com-id-1347667919417, compute-00003.lvs.ebay.com-id-1347667919419]";
			Assert.assertEquals(expectedErrMsg, e.getMessage());
		}
	}
    
}
