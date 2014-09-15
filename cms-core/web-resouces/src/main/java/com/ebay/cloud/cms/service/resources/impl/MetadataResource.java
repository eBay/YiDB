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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaClassNotExistsException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.mongo.converter.MetaClassConverters;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.metadata.service.MetadataContext.UpdateOptionMode;
import com.ebay.cloud.cms.service.CMSQueryMode;
import com.ebay.cloud.cms.service.CMSResponse;
import com.ebay.cloud.cms.service.exception.BadParamException;
import com.ebay.cloud.cms.service.exception.CMSServerException;
import com.ebay.cloud.cms.service.exception.ExceptionMapper;
import com.ebay.cloud.cms.service.exception.NotFoundException;
import com.ebay.cloud.cms.service.exception.ServiceUnavailableException;
import com.ebay.cloud.cms.service.resources.IMetadataResource;
import com.ebay.cloud.cms.service.resources.operation.MetaClassDeleteOperation;
import com.ebay.cloud.cms.service.resources.operation.MetaClassValidateOperation;
import com.ebay.cloud.cms.service.resources.operation.MetaFieldDeleteOperation;
import com.ebay.cloud.cms.sysmgmt.exception.CannotServeException;
import com.ebay.cloud.cms.sysmgmt.priority.CMSPriority;
import com.ebay.cloud.cms.sysmgmt.server.CMSServer;
import com.ebay.cloud.cms.utils.StringUtils;

@Path("/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetadataResource implements IMetadataResource {

	private static final Logger logger = LoggerFactory.getLogger(MetadataResource.class);

	private CMSServer cmsServer = CMSServer.getCMSServer();
	private static ObjectMapper mapper = new ObjectMapper();
	private static final ObjectConverter<MetaClass> converter = new ObjectConverter<MetaClass>();
	private MetadataManager manager = new MetadataManager();

	@GET
	public CMSResponse getRepositories(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@Context HttpServletRequest request) {
        try {
            CMSResponse response = new CMSResponse();
            MetadataContext context = manager.getContext(uriInfo, request);
            for (Repository repo : cmsServer.getRepositories(CMSResourceUtils.parsePriority(priority), context)) {
                response.addResult(MetadataHelper.convertRepository(repo));
            }
            return response;
        } catch (Throwable t) {
			logger.error("Error when getRepositories ", t);
			handleException(t);
		}

		return new CMSResponse();
	}

	@Path("/{reponame}")
	@GET
	public CMSResponse getRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @Context HttpServletRequest request) {
		try {
			Repository repo = cmsServer.getRepository(CMSResourceUtils.parsePriority(priority), reponame);

			CMSResponse response = new CMSResponse();
			response.addResult(MetadataHelper.convertRepository(repo));

			return response;
		} catch (Throwable t) {
			logger.error("Error when getRepository ", t);
			handleException(t);
		}
		return new CMSResponse();
	}

	@Path("/{reponame}")
	@POST
	public CMSResponse updateRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, Repository newRepo, @Context HttpServletRequest request) {
		try {
			if (newRepo.getRepositoryName() != null && !reponame.equals(newRepo.getRepositoryName())) {
				throw new BadParamException("Repository name doesn't match!");
			}
			MetadataValidator.validateRepositoryOption(newRepo.getOptions());
			cmsServer.updateRepository(CMSResourceUtils.parsePriority(priority), newRepo);
		} catch (Throwable e) {
			logger.error("Error when update Repository ", e);
			handleException(e);
		}
		return new CMSResponse();
	}

	@POST
	public CMSResponse createRepository(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			Repository repository, @Context HttpServletRequest request) {
		String reponame = repository.getRepositoryName();
		try {
			MetadataValidator.checkRepositoryName(reponame);
			MetadataValidator.validateRepositoryOption(repository.getOptions());
			Repository repo = cmsServer.createRepository(CMSResourceUtils.parsePriority(priority), repository);
			CMSResponse response = new CMSResponse();
			response.addResult(MetadataHelper.convertRepository(repo));
			return response;
		} catch (Throwable t) {
			logger.error("Error when createRepository ", t);
			handleException(t);
		}
		return new CMSResponse();
	}

	@GET
	@Path("/{reponame}/metadata/")
	public CMSResponse getMetaClasses(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {
		CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
		try {
	        MetadataContext context = manager.getContext(uriInfo, request);
			List<MetaClass> metas = cmsServer.getMetaClasses(CMSResourceUtils.parsePriority(priority), reponame, context);

			@SuppressWarnings("unchecked")
			List<Object> filteredResult = (List<Object>) manager.metaUriFilter(mode, metas);

			CMSResponse response = new CMSResponse();
			for (Object m : filteredResult) {
				response.addResult(m);
			}
			return response;
		} catch (Throwable t) {
			logger.error("Error when getMetaClasses ", t);
			handleException(t);
		}
		return new CMSResponse();
	}

	private void handleException(Throwable e) {
		if (e instanceof MetaDataException) {
			throw ExceptionMapper.convert((MetaDataException) e);
		} else if (e instanceof RepositoryNotExistsException) {
			throw new NotFoundException("repository not found");
		} else if (e instanceof CannotServeException) {
			throw new ServiceUnavailableException(e.getMessage());
		} else if (e instanceof WebApplicationException) {
			throw (WebApplicationException) e;
		} 
		throw new CMSServerException(e);
	}

	@GET
	@Path("/{reponame}/metadata/{metatype}")
	public CMSResponse getMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metatype") String metatype,
			@QueryParam("fetchHistory") boolean fetchHistory, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {
		CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		String metaVersion = request.getHeader("X-CMS-METAVERSION");

		try {
			Object filterMetadata;
			if (fetchHistory) {
				filterMetadata = manager.getMetadataHistory(uriInfo, reponame, metatype, p, request);
			} else {
				filterMetadata = manager.getMetadataDetail(reponame, metatype, metaVersion, mode, p);
			}

			CMSResponse response = new CMSResponse();
			response.addResult(filterMetadata);
			return response;
		} catch (Throwable t) {
			logger.error("Error when getMetadata ", t);
			handleException(t);
		}
		return new CMSResponse();
	}
	
	@GET
	@Path("/{reponame}/metadata/{metatype}/hierarchy")
	public CMSResponse getMetadataHierarchy(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
            @PathParam("reponame") String reponame, @PathParam("metatype") String metatype,
            @Context HttpServletRequest request) {
        CMSPriority p = CMSResourceUtils.parsePriority(priority);
        try {
            MetadataContext context = manager.getContext(uriInfo, request);
            Map<String, List<String>> hierarchy = cmsServer.getMetaClassHierarchy(p, reponame, metatype, context);
            CMSResponse response = new CMSResponse();
            response.addResult(hierarchy);
            return response;
        } catch (Throwable t) {
            logger.error("Error when getMetadataHierarchy", t);
            handleException(t);
        }
        return new CMSResponse();
    }

	@GET
	@Path("/{reponame}/metadata/{metatype}/relationships")
	public CMSResponse getMetaClassReference(@Context UriInfo uriInfo,
			@HeaderParam("X-CMS-PRIORITY") final String priority, @PathParam("reponame") String reponame,
			@PathParam("metatype") String metatype, @Context HttpServletRequest request) {
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		MetaClass metadata = (MetaClass) manager.getMetadataDetail(reponame, metatype, null, CMSQueryMode.NORMAL, p);
		List<MetaRelationship> fromRelationships = metadata.getFromReference();
		List<MetaRelationship> toRelationships = metadata.getToReference();
		MetaReference metaReference = new MetaReference();
		metaReference.setInReference(fromRelationships);
		metaReference.setOutReference(toRelationships);

		CMSResponse resp = new CMSResponse();
		resp.addResult(metaReference);
		return resp;
	}

	@POST
	@Path("/{reponame}/metadata/")
	public CMSResponse createMetaClass(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, String metadataString, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {

	    if (metadataString == null || metadataString.isEmpty()) {
	        logger.error("Error when createMetaClass. Invalid input");
	        throw new BadParamException("invalid input");
	    }
	       
		CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
		boolean isArray = false;

		JsonNode readTree;
		List<MetaClass> metas = null;
		try {
			readTree = mapper.readTree(metadataString);
			if (readTree.isArray()) {
				metas = mapper.readValue(metadataString, new TypeReference<List<MetaClass>>() {
				});
				isArray = true;
			} else {
				MetaClass m = mapper.readValue(metadataString, MetaClass.class);
				metas = new ArrayList<MetaClass>();
				metas.add(m);
			}
		} catch (JsonParseException e) {
			logger.error("Error when createMetaClass", e);
			throw new BadParamException(e, "error while parsing json input: " + e.getMessage());
		} catch (Throwable e) {
			logger.error("Error when createMetaClass", e);
			throw new CMSServerException(e);
		}

		if (metas == null || metas.isEmpty()) {
			return new CMSResponse();
		}

		for (MetaClass m : metas) {
			String rname = m.getRepository();
			if (StringUtils.isNullOrEmpty(m.getRepository())) {
				m.setRepository(reponame);
			} else if (!rname.equals(reponame)) {
				throw new BadParamException("repository name not consistency");
			}
		}

		List<MetaClass> result = null;
		try {
			MetadataContext context = manager.getContext(uriInfo, request);
			result = cmsServer.batchUpsert(CMSResourceUtils.parsePriority(priority), reponame, metas, context);
		} catch (Throwable t) {
			logger.error("Error when createMetaClass", t);
			handleException(t);
		}

		@SuppressWarnings("unchecked")
		List<Object> filteredResult = (List<Object>) manager.metaUriFilter(mode, result);

		CMSResponse response = new CMSResponse();
		if (isArray) {
			for (Object r : filteredResult) {
				response.addResult(r);
			}
		} else {
			response.addResult(filteredResult.get(0));
		}
        return response;
    }

    @POST
    @Path("/{reponame}/metadata/{metaclass}")
    public CMSResponse updateMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName, String metaClassString,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
        CMSResponse response = new CMSResponse();
        try {
            MetaClass metaClass = converter.fromJson(metaClassString, MetaClass.class);
            CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
            if (metaClass.getRepository() != null && !metaClass.getRepository().equals(reponame)) {
                throw new BadParamException("repository name not consistency");
            }
            if (StringUtils.isNullOrEmpty(metaClass.getRepository())) {
                metaClass.setRepository(reponame);
            }

            if (metaClass.getName() != null && !metaClass.getName().equals(metaclassName)) {
                throw new BadParamException("MetaClass name not consistency");
            }

            List<MetaClass> result = null;
            MetadataContext context = manager.getContext(uriInfo, request);
            result = cmsServer.batchUpsert(CMSResourceUtils.parsePriority(priority), reponame,
                    Arrays.asList(metaClass), context);
            Object filterMeta = manager.metaUriFilter(mode, result.get(0));
            response.addResult(filterMeta);
            return response;
        } catch (Throwable t) {
			logger.error("Error when getMetadata ", t);
			handleException(t);
		}
        return response;
	}

	// body must contain dataType
	@POST
	@Path("/{reponame}/metadata/{metaclass}/{fieldname}")
	public CMSResponse updateMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
			@PathParam("fieldname") String fieldName, String metaFieldString, @QueryParam("mode") String modeVal,
			@Context HttpServletRequest request) {

		JsonNode readTree;
		MetaField field = null;
		try {
			readTree = mapper.readTree(metaFieldString);
			MetaClassConverters.MetaFieldNodeReader reader = new MetaClassConverters.MetaFieldNodeReader(fieldName);
			field = reader.readObject(readTree, fieldName);
		} catch (JsonParseException e) {
			logger.error("Error when updateMetadata", e);
			throw new BadParamException(e, "error while parsing json input: " + e.getMessage());
		} catch (Throwable e) {
			logger.error("Error when updateMetadata", e);
			throw new BadParamException(e, "error while parsing json input: " + e.getMessage());
		}

		if (field.getName() != null && !field.getName().equals(fieldName)) {
			logger.error("Error when updateMetadata: MetaField name not consistency");
			throw new BadParamException("MetaField name not consistency");
		}

		MetaClass meta = new MetaClass();
		meta.setName(metaclassName);

		CMSQueryMode mode = CMSQueryMode.fromString(modeVal);
		meta.setRepository(reponame);
		meta.addField(field);

		MetaClass result = null;
		try {
			MetadataContext context = manager.getContext(uriInfo, request);
			result = cmsServer.updateMetaField(CMSResourceUtils.parsePriority(priority), reponame, meta, fieldName,
					context);
		} catch (MetaClassNotExistsException e) {
			logger.error("Error when updateMetadata", e);
			throw new NotFoundException(e, "MetaClass not found!");
		} catch (IllegalMetaClassException e) {
			logger.error("Error when updateMetadata", e);
			throw new BadParamException(e, "metaclass validation failed");
		} catch (Throwable t) {
			logger.error("Error when updateMetadata ", t);
			handleException(t);
		}

		Object filterMeta = manager.metaUriFilter(mode, result);

		CMSResponse response = new CMSResponse();
		response.addResult(filterMeta);
		return response;
	}

	@DELETE
	@Path("/{reponame}/metadata/{metaclass}")
	public CMSResponse deleteMetadata(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
			@Context HttpServletRequest request) {
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		MetadataContext context = manager.getContext(uriInfo, request);
		MetaClassDeleteOperation operation = new MetaClassDeleteOperation(p, reponame, metaclassName, context,
				"exception while delete metaclass");
		return operation.execute();
	}

	@DELETE
	@Path("/{reponame}/metadata/{metaclass}/{fieldname}")
	public CMSResponse deleteMetaField(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
			@PathParam("fieldname") String fieldName, @Context HttpServletRequest request) {
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		MetadataContext context = manager.getContext(uriInfo, request);
        String metaVersion = request.getHeader("X-CMS-METAVERSION");
        
		// drop related index
		MetaClass meta = (MetaClass) manager.getMetadataDetail(reponame, metaclassName, metaVersion, CMSQueryMode.NORMAL, p);
		for (IndexInfo index : meta.getIndexes()) {
			if (!index.isInternal() && index.getKeyList().contains(fieldName)) {
				deleteMetadataIndex(uriInfo, priority, reponame, metaclassName, index.getIndexName(), request);
			}
		}

		MetaFieldDeleteOperation deleteOp = new MetaFieldDeleteOperation(p, reponame, metaclassName, fieldName,
				context, "exception while delete metaclass field");
		return deleteOp.execute();
	}

	@GET
	@Path("/{reponame}/metadata/{metaclass}/indexes")
	public CMSResponse getMetadataIndex(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") final String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
			@Context HttpServletRequest request) {
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
	    String metaVersion = request.getHeader("X-CMS-METAVERSION");
	    
		MetaClass meta = (MetaClass) manager.getMetadataDetail(reponame, metaclassName, metaVersion, CMSQueryMode.NORMAL, p);
		CMSResponse response = new CMSResponse();
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();
		for (IndexInfo index : meta.getIndexes()) {
			if (!index.isInternal()) {
				indexes.add(index);
			}
		}
		response.addResult(indexes);
		return response;
	}

	@POST
	@Path("/{reponame}/metadata/{metaclass}/indexes")
	public CMSResponse createMetadataIndex(@Context UriInfo uriInfo,
			@HeaderParam("X-CMS-PRIORITY") final String priority, @PathParam("reponame") String reponame,
			@PathParam("metaclass") String metaclassName, String indexString, @Context HttpServletRequest request) {

		CMSPriority p = CMSResourceUtils.parsePriority(priority);

		JsonNode readTree;
		List<IndexInfo> indexes = null;
		try {
			readTree = mapper.readTree(indexString);
			if (readTree.isArray()) {
				indexes = mapper.readValue(indexString, new TypeReference<List<IndexInfo>>() {
				});
			} else {
				IndexInfo m = mapper.readValue(indexString, IndexInfo.class);
				indexes = new ArrayList<IndexInfo>();
				indexes.add(m);
			}
		} catch (Throwable e) {
			logger.error("Error when createMetadataIndex", e);
			throw new BadParamException(e, "error while parsing json input: " + e.getMessage());
		}

		if (indexes == null || indexes.isEmpty()) {
			return new CMSResponse();
		}

		try {
			MetadataContext context = manager.getContext(uriInfo, request);
			context.setOptionChangeMode(UpdateOptionMode.ADD);
			cmsServer.addIndex(p, reponame, metaclassName, indexes, context);
		} catch (IllegalMetaClassException e) {
			logger.error("Error when createMetadataIndex", e);
			throw new BadParamException(e, "metaclass validation failed");
		} catch (Throwable t) {
			logger.error("Error when createMetadataIndex ", t);
			handleException(t);
		}

		return new CMSResponse();
	}

	@DELETE
	@Path("/{reponame}/metadata/{metaclass}/indexes/{indexName}")
	public CMSResponse deleteMetadataIndex(@Context UriInfo uriInfo,
			@HeaderParam("X-CMS-PRIORITY") final String priority, @PathParam("reponame") String reponame,
			@PathParam("metaclass") String metaclassName, @PathParam("indexName") String indexName,
			@Context HttpServletRequest request) {

		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		MetadataContext context = manager.getContext(uriInfo, request);
		context.setOptionChangeMode(UpdateOptionMode.DELETE);
		try {
			cmsServer.deleteIndex(p, reponame, metaclassName, indexName, context);
		} catch (Throwable t) {
			logger.error("Error when deleteMetadataIndex", t);
			handleException(t);
		}
		return new CMSResponse();
	}

	@POST
	@Path("/{reponame}/metadata/{metaclass}/actions/validate")
	public CMSResponse validateMetaClass(@Context UriInfo uriInfo, @HeaderParam("X-CMS-PRIORITY") String priority,
			@PathParam("reponame") String reponame, @PathParam("metaclass") String metaclassName,
			@QueryParam("mode") String modeVal, @Context HttpServletRequest request) {
		CMSPriority p = CMSResourceUtils.parsePriority(priority);
		MetadataContext context = manager.getContext(uriInfo, request);
		MetaClassValidateOperation validateOp = new MetaClassValidateOperation(p, reponame, metaclassName, context,
				"exception while validate metaclass");
		return validateOp.execute();
	}

}
