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


/**
 * 
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

package com.ebay.cloud.cms.metadata.mongo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.metadata.exception.IndexExistsException;
import com.ebay.cloud.cms.metadata.exception.IndexNotExistsException;
import com.ebay.cloud.cms.metadata.exception.IndexOptionOperationException;
import com.ebay.cloud.cms.metadata.model.IMetadataVisistor;
import com.ebay.cloud.cms.metadata.model.IndexInfo;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaClassGraph;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;
import com.ebay.cloud.cms.metadata.model.MetaOption;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.model.MetaRelationship.RelationTypeEnum;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.model.RepositoryOption;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;

/**
 * @author liasu
 * 
 */
public class MetadataOptionValidator implements IMetadataVisistor {

    private final MetadataContext context;
    private final MetaOption      newOption;
    private MetaOption            tempOption;
    private Set<String>           visited;

    public MetadataOptionValidator(MetaOption option, MetadataContext context) {
        this.context = context;
        this.newOption = option;
        this.visited = new HashSet<String>();
    }

    @Override
    public void processField(MetaClass metaClass, MetaField field) {
        // nothing to do
    }

    @Override
    public void processOption(MetaClass metaClass, MetaOption option, MetaClassGraph tempGraph) {
    	tempOption = new MetaOption();
        Collection<IndexInfo> newIndexes = newOption.getIndexes();
        for (IndexInfo ii : newIndexes) {
        	IndexInfo newIndex = new IndexInfo(ii.getIndexName());
        	tempOption.addIndex(newIndex);
        }

        Collection<IndexInfo> oldIndexes = option.getIndexes();
        Map<String, IndexInfo> oldIndexMap = new HashMap<String, IndexInfo>();
        for (IndexInfo oldi : oldIndexes) {
            oldIndexMap.put(oldi.getIndexName(), oldi);
        }

        StringBuilder sb = new StringBuilder();
        switch (context.getOptionChangeMode()) {
            case ADD:
            	checkIndexSize(metaClass, newOption, tempGraph);
                for (IndexInfo ii : newIndexes) {
                    if (ii.isInternal()) {
                        continue;
                    }
                    if (oldIndexMap.containsKey(ii.getIndexName())) {
                        sb.append(MessageFormat.format("index {0} already exsits on metaclass {1}!", ii.getIndexName(), metaClass.getName())).append("\n");
                    }
                    ii.validate();
                    validateKeyList(metaClass, ii);
                }
                if (sb.length() > 0) {
                    throw new IndexExistsException(sb.toString());
                }
    
                // check all descendant metaclass
                List<MetaClass> descendants = metaClass.getDescendants();
                for (MetaClass desc : descendants) {
                    desc.traverse(this);
                }
                break;
            case UPDATE:
            	checkIndexSize(metaClass, newOption, tempGraph);
                for (IndexInfo ii : newIndexes) {
                    if (ii.isInternal()) {
                        continue;
                    }
                    ii.validate();
                    validateKeyList(metaClass, ii);
                }
                // pass through
            case DELETE:
                for (IndexInfo ii : newIndexes) {
                    if (ii.isInternal()) {
                        continue;
                    }
    
                    if (!oldIndexMap.containsKey(ii.getIndexName())) {
                        sb.append(MessageFormat.format("index {0} doesn''t exsits", ii.getIndexName())).append("\n");
                    }
                }
                if (sb.length() > 0) {
                    throw new IndexNotExistsException(sb.toString());
                }
                break;
            case VALIDATE:
            	checkIndexSize(metaClass, newOption, tempGraph);
                for (IndexInfo ii : oldIndexes) {
                    if (ii.isInternal()) {
                        continue;
                    }
                    ii.validate();
                }
                break;
            default:
                throw new IndexOptionOperationException("Illegal update mode for metaclass option change");
        }

    }

    private void validateKeyList(MetaClass meta, IndexInfo ii) {
        for (String key : ii.getKeyList()) {
            if (meta.getFieldByName(key) == null) {
                throw new IndexOptionOperationException("MetaClass " + meta.getName() + "'sindex " + ii.getIndexName()
                        + " key list contains non-existing field " + key + " in meta " + meta.getName());
            }
        }
    }

    private Collection<IndexInfo> getEmbededIndexes(MetaClass metaClass, MetaClassGraph graph) {
    	Collection<IndexInfo> embedIndexes = new ArrayList<IndexInfo>(); 
        Collection<MetaField> fields = metaClass.getClassFields();
        for (MetaField field : fields) {
            if (field.getDataType() == DataTypeEnum.RELATIONSHIP 
                    && ((MetaRelationship) field).getRelationType() == RelationTypeEnum.Embedded) {
            	MetaRelationship relationship = (MetaRelationship) field;
            	String refDataType = relationship.getRefDataType();
                MetaClass targetMeta = graph.getMetaClass(refDataType);
                if (targetMeta != null) {
                	// add reference indexes
                	targetMeta.addReferenceIndexes();
                	// add self indexes
                	Collection<IndexInfo> targetIndexes = new ArrayList<IndexInfo>(targetMeta.getOptions().getIndexes());
                	// add nested embed indexes
                	targetIndexes.addAll(getEmbededIndexes(targetMeta, graph));
                	for (IndexInfo targetIndex : targetIndexes) {
                		embedIndexes.add(new IndexInfo(targetIndex, relationship));
                	}
                }
            }
        }
        return embedIndexes;
    }
    
    private void checkIndexSize(MetaClass metaClass, MetaOption newOption, MetaClassGraph tempGraph) {
    	// add indexes from reference fields
    	metaClass.addReferenceIndexes();
    	
    	// add indexes from embed fields
    	Collection<IndexInfo> embedIndexes = getEmbededIndexes(metaClass, tempGraph);
    	
    	IMetadataService metadataService = metaClass.getMetadataService();
        Repository repo = metadataService.getRepository();
        RepositoryOption repositoryOption = repo.getOptions();
        Integer maxNumOfIndexes = CMSConsts.MAX_INDEXES_PER_META_CLASS;
        if (repositoryOption != null && repositoryOption.getMaxNumOfIndexes() != null) {
            maxNumOfIndexes = repositoryOption.getMaxNumOfIndexes();
        }
        
        Collection<IndexInfo> existedIndexes = metaClass.getIndexes();
		Collection<IndexInfo> newIndexes = newOption.getIndexes();
		Set<String> mergedIndexNames = new HashSet<String>();
		Set<String> existedIndexNames = new HashSet<String>();
		for (IndexInfo ii : existedIndexes) {
			existedIndexNames.add(ii.getIndexName());
			mergedIndexNames.add(ii.getIndexName());
		}
		for (IndexInfo ii : newIndexes) {
			mergedIndexNames.add(ii.getIndexName());
		}
		for (IndexInfo ii : embedIndexes) {
			mergedIndexNames.add(ii.getIndexName());
		}
		
        int actualIndexSize = mergedIndexNames.size();
        if(actualIndexSize > maxNumOfIndexes) {
            String errorMessage = String.format("The number of index on a metaclass %s should NOT be more than %d! Existed index names: %s, while target index names: %s", 
            		metaClass.getName(), maxNumOfIndexes, StringUtils.join(existedIndexNames.iterator(), ", "), StringUtils.join(mergedIndexNames.iterator(), ", "));
            throw new IllegalIndexException(errorMessage);
        }
        
        
        // if this is embed class, check container index size also
        if (metaClass.isEmbed()) {
        	List<MetaRelationship> fromRefs = tempGraph.getFromReference(metaClass);
        	for(MetaRelationship fromRef : fromRefs) {
        		String containerMetaClassName = fromRef.getSourceDataType();
        		if (containerMetaClassName != null) {
	        		MetaClass containerMetaClass = tempGraph.getMetaClass(containerMetaClassName);
	        		containerMetaClass.setMetadataService(metadataService);
	        		
	        		Collection<IndexInfo> tempIndexes = tempOption.getIndexes();
	        		for (IndexInfo tempIndex : tempIndexes) {
	        			IndexInfo newIndex = new IndexInfo(tempIndex, fromRef);
	        			tempIndex.setIndexName(newIndex.getIndexName());
	        		}
	        		checkIndexSize(containerMetaClass, tempOption, tempGraph);
        		}
        	}
        } else {
        	// check descendants violated or not as indexes allow inheritance
        	List<MetaClass> descendants = tempGraph.getDescendants(metaClass);
        	for (MetaClass descendant : descendants) {
        		String metaClassName = descendant.getName();
        		if (!visited.contains(metaClassName)) {
        			visited.add(metaClassName);
        			checkIndexSize(descendant, newOption, tempGraph);
        		}
        	}
        }
    }

	@Override
    public Collection<String> getVisitFields(MetaClass metaClass) {
        return Collections.emptyList();
    }

}
