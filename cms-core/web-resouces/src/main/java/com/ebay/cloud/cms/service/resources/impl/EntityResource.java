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

package com.ebay.cloud.cms.service.resources.impl;


import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.dal.exception.CmsDalException;
import com.ebay.cloud.cms.dal.search.IQueryExplanation;
import com.ebay.cloud.cms.entmgr.entity.EntityContext;
import com.ebay.cloud.cms.entmgr.entity.EntityContext.BatchOperationFailReturnOption;
import com.ebay.cloud.cms.entmgr.exception.CmsEntMgrException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.optimizer.QueryOptimizeException;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.ebay.cloud.cms.query.service.IQueryResult;
import com.ebay.cloud.cms.query.service.QueryContext;
import com.ebay.cloud.cms.query.service.QueryContext.PaginationMode;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.ExceptionMapper;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.service.resources.IEntityResource;
import com.ebay.cloud.cms.service.resources.operation.EntityBatchCreationOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityBatchDeleteOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityBatchUpdateOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityCreateOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityDeleteFieldOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityDeleteOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityFieldOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityGetOperatin;
import com.ebay.cloud.cms.service.resources.operation.EntityModifyFieldOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityPullFieldOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityReplaceOperation;
import com.ebay.cloud.cms.service.resources.operation.EntityUpdateOperation;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;

/**
 * @author xjiang
 * @author jianxu1
 * @author liasu
 */
@Path("/repositories/{reponame}/branches/{branch}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EntityResource implements IEntityResource {
	private static final String METACLASS_NOT_FOUND = "metaclass not found: ";

	private static final String REPO_NOT_FOUND = "repository not found: ";

	private static final Logger logger = LoggerFactory.getLogger(EntityResource.class);

	private CMSServer cmsServer = CMSServer.getCMSServer();

	/**
	 * To workaround the jersey resource matching
	 */
	private BranchResource branchResource = new BranchResource();

	// ======= branch apis ======
	/**
	 * Branch apis, these apis semantically belongs to BranchResource, but
	 * caused by api prefix duplication and jersey path matching algorithm.
	 * Jersey will only look into the EntityResource(this round of path matching
	 * could be found in RootResourceClassRule), and if it found nothing
	 * matched, throw a exception and never look into BranchReource
	 */
	@GET
	public Response getBranch(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, @PathParam("reponame") String reponame,
			@PathParam("branch") String branchId, @Context HttpServletRequest request) {
		return toRestResponse(branchResource.getBranch(uriInfo, priority, consistPolicy, reponame, branchId, request), null);
	}

	// ===== end of branch apis ====

	protected Response toRestResponse(CMSResponse resp, EntityContext context) {
	    ResponseBuilder builder = Response.ok(resp);
	    if (context != null && context.hasRequestTrackingCode()) {
	        builder.header(CMSConsts.TRACKING_CODE_KEY, context.getRequestTrackingCode().getErrorCode());
	    }
	    return builder.build();
	}

	@GET
	@Path("/{metadata}/{oid}")
	public Response getEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {

		String message = "exception while get entity, metadata: {0}, oid: {1}";
		EntityOperation getOperation = new EntityGetOperatin(cmsServer, modeVal, request, priority, consistPolicy,
				reponame, metadata, branchname, oid, null, message, uriInfo);
		return toRestResponse(getOperation.perform(), getOperation.getContext());
	}

	@DELETE
	@Path("/{metadata}/{oid}")
	public Response deleteEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {

		String message = "exception while delete entity, metadata: {0}, oid: {1}";

		EntityDeleteOperation deleteOperation = new EntityDeleteOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branchname, oid, null, message, uriInfo);
		return toRestResponse(deleteOperation.perform(), deleteOperation.getContext());
	}

	@POST
	@Path("/entities")
	public Response batchCreateEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @QueryParam("failReturnOption") final String failReturnOptionVal,
			@Context HttpServletRequest request) {

		String message = "exception while create entity, metadata : {0}";

		EntityBatchCreationOperation batchCreateOperation = new EntityBatchCreationOperation(cmsServer, modeVal,
				failReturnOptionVal, request, priority, consistPolicy, reponame, null, branch, null, jsonString,
				message, uriInfo);
		return toRestResponse(batchCreateOperation.perform(), batchCreateOperation.getContext());
	}

	@PUT
	@Path("/entities")
	public Response batchModifyEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @QueryParam("failReturnOption") final String failReturnOptionVal,
			@Context HttpServletRequest request) {

		String message = "exception while update entities ";
		EntityBatchUpdateOperation batchUpdateOperation = new EntityBatchUpdateOperation(cmsServer, modeVal,
				failReturnOptionVal, request, priority, consistPolicy, reponame, null, branch, null, jsonString,
				message, uriInfo);
		return toRestResponse(batchUpdateOperation.perform(), batchUpdateOperation.getContext());
	}

	@DELETE
	@Path("/entities")
	public Response batchDeleteEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @QueryParam("failReturnOption") String failReturnOptionVal,
			@Context HttpServletRequest request) {

		String message = "exception while delete entities ";
		EntityBatchDeleteOperation batchDeleteOperation = new EntityBatchDeleteOperation(cmsServer, modeVal,
				failReturnOptionVal, request, priority, consistPolicy, reponame, null, branch, null, jsonString,
				message, uriInfo);
		return toRestResponse(batchDeleteOperation.perform(), batchDeleteOperation.getContext());
	}

	@POST
	@Path("/{metadata}")
	public Response createEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {

		if ("entities".equals(metadata)) {
			return batchCreateEntities(uriInfo, reponame, branch, priority, consistPolicy, jsonString, modeVal,
					BatchOperationFailReturnOption.IMMEDIATE.toString(), request);
		}

		String message = "exception while create entity, metadata : {0}";
		EntityCreateOperation createOperation = new EntityCreateOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, null, jsonString, message, uriInfo);
		return toRestResponse(createOperation.perform(), createOperation.getContext());
	}

	@PUT
	@Path("/{metadata}/{oid}")
	public Response replaceEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {

		String message = "exception while replace entity, metadata : {0}, oid : {1}";
		EntityReplaceOperation replaceOperation = new EntityReplaceOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, oid, jsonString, message, uriInfo);
		return toRestResponse(replaceOperation.perform(), replaceOperation.getContext());
	}

	@POST
	@Path("/{metadata}/{oid}")
	public Response modifyEntity(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, final String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {

		String message = "exception while modify entity, metadata: {0}, oid : {1}";
		String entityBody = jsonString;
		if (entityBody == null) {
			entityBody = generateEmptyEntityBody();
		}
		EntityUpdateOperation updateOpeartion = new EntityUpdateOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, oid, entityBody, message, uriInfo);
		return toRestResponse(updateOpeartion.perform(), updateOpeartion.getContext());
	}

	@POST
	@Path("/{metadata}/{oid}/{fieldname}")
	@Override
	public Response modifyEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @PathParam("fieldname") final String fieldName,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
			@HeaderParam("X-CMS-CONDITIONAL-UPDATE") final String casMode, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
		String errorMsg = "exception when modify entity field ";
		boolean casFlag = false;
		if (casMode != null && casMode.equals("true")) {
			casFlag = true;
		}
		EntityFieldOperation modifyFieldOp = new EntityModifyFieldOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, oid, jsonString, fieldName, errorMsg, uriInfo, casFlag);

		return toRestResponse(modifyFieldOp.perform(), modifyFieldOp.getContext());
	}

	@POST
	@Path("/{metadata}/{oid}/{fieldname}/actions/push")
	@Override
	public Response pushEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @PathParam("fieldname") final String fieldName,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy,
			@HeaderParam("X-CMS-CONDITIONAL-UPDATE") final String casMode, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
		String errorMsg = "exception when modify entity field ";
		boolean casFlag = false;
		if (casMode != null && casMode.equals("true")) {
			casFlag = true;
		}
		EntityFieldOperation modifyFieldOp = new EntityModifyFieldOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, oid, jsonString, fieldName, errorMsg, uriInfo, casFlag);

		return toRestResponse(modifyFieldOp.perform(), modifyFieldOp.getContext());
	}

	@POST
	@Path("/{metadata}/{oid}/{fieldname}/actions/pull")
	@Override
	public Response pullEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @PathParam("fieldname") final String fieldName,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
		String errorMsg = "exception when pull entity field ";
		EntityPullFieldOperation pullFieldOp = new EntityPullFieldOperation(cmsServer, modeVal, request, priority,
				consistPolicy, reponame, metadata, branch, oid, jsonString, fieldName, errorMsg, uriInfo);
		return toRestResponse(pullFieldOp.perform(), pullFieldOp.getContext());
	}

	@DELETE
	@Path("/{metadata}/{oid}/{fieldname}")
	@Override
	public Response deleteEntityField(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branch, @PathParam("metadata") final String metadata,
			@PathParam("oid") final String oid, @PathParam("fieldname") final String fieldName,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, String jsonString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
		String errorMsg = "exception when delete entity field ";
		EntityDeleteFieldOperation deleteFieldOp = new EntityDeleteFieldOperation(cmsServer, modeVal, request,
				priority, consistPolicy, reponame, metadata, branch, oid, jsonString, fieldName, errorMsg, uriInfo);
		return toRestResponse(deleteFieldOp.perform(), deleteFieldOp.getContext());
	}

	private String generateEmptyEntityBody() {
		return "{}";
	}

	@GET
	@Path("/{metadata}")
	public Response batchGetEntities(@Context final UriInfo uriInfo, @PathParam("reponame") final String reponame,
			@PathParam("branch") final String branchname, @PathParam("metadata") final String metadata,
			@HeaderParam("X-CMS-PRIORITY") final String priority,
			@HeaderParam("X-CMS-CONSISTENCY") final String consistPolicy, @QueryParam("mode") final String modeVal,
			@Context final HttpServletRequest request) {
		try {
		    final PaginationMode pageMode = CMSResourceUtils.parsePaginationMode(cmsServer, uriInfo.getQueryParameters().getFirst(CMSResourceUtils.REQ_PAGINATION_MODE));
		    final CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
			final QueryContext context = QueryResource.createContext(cmsServer, uriInfo, reponame, branchname, mode, pageMode, consistPolicy, request);
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			QueryResource.parseQueryParameters(uriInfo.getQueryParameters(), context);
			context.setAllowFullTableScan(true);// by default allow full table
												// scan for batch get
			IQueryResult queryResult = cmsServer.batchGet(CMSResourceUtils.parsePriority(priority), metadata, params,
					context);

			CMSResponse response = new CMSResponse();
			response.addProperty("dbTimeCost", context.getDbTimeCost());
			response.addProperty("totalTimeCost", context.getTotalTimeCost());
			response.addProperty("hasmore", queryResult.hasMoreResults());
			response.addProperty("count", mode == CMSQueryMode.COUNT ? queryResult.getCount() : queryResult
					.getEntities().size());
			
	        if (queryResult.getExplanations() != null && !queryResult.getExplanations().isEmpty()) {
	            response.addProperty("explanation", getExplanation(queryResult));
	        }
	        if (queryResult.hasMoreResults()) {
	            response.addProperty("pagination", CMSResourceUtils.getPagination(queryResult, context, request));
	            // CMS-4620 : add next url for next page
	            response.addProperty("next", CMSResourceUtils.getNext(queryResult, context, request));
	        }
	        if (context.isShowDisplayMeta()) {
	            response.addProperty("display", queryResult.getDisplayMeta());
	        }

			response.addResult(QueryResource.getEntities(reponame, branchname, mode, queryResult));
			return toRestResponse(response, null);
		} catch (RepositoryNotExistsException e) {
			throw new NotFoundException(REPO_NOT_FOUND + e.getMessage());
		} catch (MetaClassNotExistsException e) {
			throw new NotFoundException(METACLASS_NOT_FOUND + e.getMessage());
		} catch (QueryParseException e) {
			if (e.getErrorCode() == QueryErrCodeEnum.METACLASS_NOT_FOUND.getErrorCode()) {
				throw new NotFoundException(METACLASS_NOT_FOUND + e.getMessage());
			}
			if (e.getErrorCode() == QueryErrCodeEnum.METAFIELD_NOT_FOUND.getErrorCode()) {
				throw new NotFoundException("metafield not found: " + e.getMessage());
			}
			throw e;
		} catch (CannotServeException e) {
			throw new ServiceUnavailableException(e.getMessage());
		} catch (QueryOptimizeException e) {
			throw new CMSServerException(e.getErrorCode(), e.getMessage(), e);
		} catch (WebApplicationException e) {
			logger.error("exception while execute query", e);
			throw e;
		} catch (CmsDalException e) {
			throw ExceptionMapper.convert(e);
		} catch (CmsEntMgrException e) {
			throw ExceptionMapper.convert(e);
		} catch (Throwable t) {
			logger.error("exception while execute query", t);
			throw new CMSServerException(t);
		}
	}

    private JsonNode getExplanation(IQueryResult queryResult) {
        List<IQueryExplanation> explanations = queryResult.getExplanations();
        if (explanations == null) {
            return null;
        }
        JsonNode explanationNode = null;
        if (explanations.size() == 1) {
            explanationNode = explanations.get(0).getJsonExplanation();
        } else {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            for (IQueryExplanation e : explanations) {
                array.add(e.getJsonExplanation());
            }
            explanationNode = array;
        }
        return explanationNode;
    }

}
