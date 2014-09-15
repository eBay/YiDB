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

package com.ebay.cloud.cms.dalapi.search;

import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.dal.persistence.ConsistentPolicy;
import com.ebay.cloud.cms.dal.persistence.IPersistenceService;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext;
import com.ebay.cloud.cms.dal.persistence.PersistenceContext.DBCollectionPolicy;
import com.ebay.cloud.cms.dal.search.ISearchQuery;
import com.ebay.cloud.cms.dal.search.ISearchService;
import com.ebay.cloud.cms.dal.search.ISearchStrategy;
import com.ebay.cloud.cms.dal.search.SearchOption;
import com.ebay.cloud.cms.dal.search.SearchProjection;
import com.ebay.cloud.cms.dal.search.SearchResult;
import com.ebay.cloud.cms.dal.search.impl.SearchServiceImpl;
import com.ebay.cloud.cms.dal.search.impl.field.AbstractSearchField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField;
import com.ebay.cloud.cms.dal.search.impl.field.AggregationField.AggFuncEnum;
import com.ebay.cloud.cms.dal.search.impl.field.GroupField;
import com.ebay.cloud.cms.dal.search.impl.field.ProjectionField;
import com.ebay.cloud.cms.dal.search.impl.field.SelectionField;
import com.ebay.cloud.cms.dal.search.impl.query.EmbedSearchQuery;
import com.ebay.cloud.cms.dal.search.impl.query.SearchQuery;
import com.ebay.cloud.cms.dalapi.common.DeploymentEntityGenerator;
import com.ebay.cloud.cms.dalapi.common.RaptorEntityGenerator;
import com.ebay.cloud.cms.dalapi.persistence.impl.PersistenceServiceImpl;
import com.ebay.cloud.cms.dalapi.search.utils.TestUtils;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.dataloader.MetadataDataLoader;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.MetaField;
import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.metadata.service.IMetadataService;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.ebay.cloud.cms.utils.mongo.test.CMSMongoTest;

public class SearchBaseTest extends CMSMongoTest {
    protected static ISearchService searchService = null;
    protected static IMetadataService raptorMetaService = null;
    protected static IMetadataService deployMetaService = null;
    protected static IPersistenceService persistenceService = null;
    
    protected static PersistenceContext deployContext = null;
    protected static PersistenceContext raptorContext = null;
    
    private static MetadataDataLoader metedataLoader = null;
    
    protected static final String RAPTOR_REPO = "raptor-paas";
    protected static final String DEPLOY_REPO = "software-deployment";
    
    protected static final String BRANCH_MAIN = "main";
    protected static MongoDataSource    ds;
    
    protected static final ISearchStrategy strategy = TestUtils.getDalSearchStrategy();

    @BeforeClass
    public static synchronized void setupData() {
        ds = new MongoDataSource(getConnectionString());
        metedataLoader = MetadataDataLoader.getInstance(ds);
        metedataLoader.loadTestDataFromResource();
        raptorMetaService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer")
                .getRepository(RAPTOR_REPO).getMetadataService();
        RaptorEntityGenerator.loadRaptorTopology(DBCollectionPolicy.SplitByMetadata);

        deployMetaService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer")
                .getRepository(DEPLOY_REPO).getMetadataService();
        DeploymentEntityGenerator.loadDeploymentBundle(DBCollectionPolicy.Merged);

        searchService = new SearchServiceImpl(ds);   
        
        persistenceService = new PersistenceServiceImpl(ds);
        
        deployContext = new PersistenceContext(deployMetaService, DBCollectionPolicy.Merged,
                ConsistentPolicy.safePolicy(), BRANCH_MAIN + "_base");
        deployContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
        deployContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
        raptorContext = new PersistenceContext(raptorMetaService, DBCollectionPolicy.SplitByMetadata,
                ConsistentPolicy.safePolicy(), BRANCH_MAIN + "_base");
        raptorContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
        raptorContext.setRegistration(TestUtils.getTestDalImplemantation(dataSource));
    }

    protected IEntity findManifest() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Manifest");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null, projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        MetaClass metaclass2 = metaref2.getRefMetaClass();
        SearchProjection projection2 = new SearchProjection();  
        projection2.addField(ProjectionField.STAR);            
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), null, projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        MetaRelationship metaref3 = (MetaRelationship)metaclass2.getFieldByName("approvals");
        SearchProjection projection3 = new SearchProjection();  
        projection3.addField(ProjectionField.STAR);            
        SearchQuery query3 = new SearchQuery(metaref3.getRefMetaClass(), null, projection3, strategy);
        embedFieldList.add(metaref3);
        EmbedSearchQuery embedQuery3 = new EmbedSearchQuery(query3, embedFieldList);
        embedQuery2.addChildQuery(embedQuery3);
        
        SearchOption option = new SearchOption();               
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        return result.getResultSet().get(0);
    }
    
    protected IEntity findPackage() {
        MetaClass metaclass1 = deployMetaService.getMetaClass("Package");
        SearchProjection projection1 = new SearchProjection();
        projection1.addField(ProjectionField.STAR);      
        SearchQuery query1 = new SearchQuery(metaclass1, null, projection1, strategy);
        EmbedSearchQuery embedQuery1 = new EmbedSearchQuery(query1);
        
        MetaRelationship metaref2 = (MetaRelationship)metaclass1.getFieldByName("versions");
        SearchProjection projection2 = new SearchProjection();  
        projection2.addField(ProjectionField.STAR);            
        SearchQuery query2 = new SearchQuery(metaref2.getRefMetaClass(), null, projection2, strategy);
        LinkedList<MetaRelationship> embedFieldList = new LinkedList<MetaRelationship>();
        embedFieldList.add(metaref2);
        EmbedSearchQuery embedQuery2 = new EmbedSearchQuery(query2, embedFieldList);
        embedQuery1.addChildQuery(embedQuery2);
        
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(embedQuery1, option, deployContext);
        return result.getResultSet().get(0);
    }
    
    protected List<IEntity> findServiceInstance() {
        MetaClass metadata = raptorMetaService.getMetaClass("ServiceInstance");                  
        SearchProjection projection = new SearchProjection();
        projection.addField(ProjectionField.STAR);
        ISearchQuery query = new SearchQuery(metadata, null, projection, strategy);
        SearchOption option = new SearchOption();
        SearchResult result = searchService.search(query, option, raptorContext);
        return result.getResultSet();
    }
    
    protected AbstractSearchField createSearchField(MetaClass metaclass, String fieldName) {
        MetaField metaField = metaclass.getFieldByName(fieldName);
        return new SelectionField(metaField, strategy);
    }

    protected GroupField createGroupField(MetaClass metaclass, String fieldName) {
        MetaField metaField = metaclass.getFieldByName(fieldName);
        return new GroupField(metaField, strategy);
    }
    
    protected AggregationField createAggregationField(MetaClass metaclass, AggFuncEnum func, String fieldName) {
        MetaField metaField = metaclass.getFieldByName(fieldName);
        AbstractSearchField searchField = new SelectionField(metaField, strategy);
        return new AggregationField(func, searchField);
    }
}
