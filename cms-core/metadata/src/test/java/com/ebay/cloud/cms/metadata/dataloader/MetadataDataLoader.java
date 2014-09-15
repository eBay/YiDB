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

package com.ebay.cloud.cms.metadata.dataloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.RepositoryServiceFactory;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;
import com.ebay.cloud.cms.metadata.exception.RepositoryNotExistsException;
import com.ebay.cloud.cms.metadata.model.MetaClass;
import com.ebay.cloud.cms.metadata.model.Repository;
import com.ebay.cloud.cms.metadata.mongo.MongoRepositoryServiceImpl;
import com.ebay.cloud.cms.metadata.mongo.converter.ObjectConverter;
import com.ebay.cloud.cms.metadata.service.IRepositoryService;
import com.ebay.cloud.cms.metadata.service.MetadataContext;
import com.ebay.cloud.cms.mongo.MongoDataSource;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;

/**
 * TestDataLoader will load data into mongo.
 * 
 *  Example use:
 *    1. load data for unittest. 
 *       Three json files represent properties repositories and metaclass db/collections in classpath:/mongo/ are delivered with this jar file for unit test
 *       
 *       MongoDataSource ds = new MongoDataSource("some server");
 *       getInstance(ds).load();
 *       
 *       will load them into mongo db for unittest.
 *       
 *    2. load data to init cms mongo server.
 *       cms configurations and some predefined repository/metadata can be loaded into mongo using this class. 
 *       TBD. 
 *       
 *       TODO: add methods to load files into db.
 *    
 *       
 *       
 *       
 * 
 * @author liubo
 *
 */
public class MetadataDataLoader {
    
    private static Logger logger = LoggerFactory.getLogger(MetadataDataLoader.class);
    
    private MongoDataSource ds;
    private Mongo mongo;
    
    public void cleanUp() {
    	//jianxu1: RepoService.getRepositories won't list repository with deleting status
    	//if we failed to call deleteRepository due to exception, data loader won't be able to
    	//clean all the data.
        //IRepositoryService oldRepo = RepositoryServiceFactory.createRepositoryService(ds);
        //for (Repository r : oldRepo.getRepositories()) {
        for(String repoName: getAllRepositoryNames()){
            cleanDatabase(repoName);
        }
        
        cleanDatabase(CMSConsts.SYS_DB);
        
        MongoRepositoryServiceImpl.prepareSystemDB(mongo);

        RepositoryServiceFactory.clearRepositoryServiceCache();
    }
    
    public void cleanDatabase(String dbName) {        
        DB db = mongo.getDB(dbName);        
        Set<String> allColNames = db.getCollectionNames();
        for (String colName : allColNames) {
            if (isSystemClollection(colName)) {
                logger.info("don't drop collection {}", colName);                
            } else {
                DBCollection dbCollection = db.getCollection(colName);
                dbCollection.drop();
            }
        }
    }
    
    private boolean isSystemClollection(String colName) {
        return colName.startsWith("system.");
    }
    
    private List<String> getAllRepositoryNames(){
        DBCollection repoCollection = this.mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.REPOSITORY_COLL);
        BasicDBObject query = new BasicDBObject();
 
        List<String> repoNameList = new ArrayList<String>();
        DBCursor cursor = repoCollection.find(query);
        while(cursor.hasNext()) {
            DBObject bsonObject = cursor.next();
            String repoName = (String)bsonObject.get(Repository.REPOSITORY_FIELD_NAME);
            repoNameList.add(repoName);
        }
        return repoNameList;
    }
    
    private MetadataDataLoader(MongoDataSource ds) {
        this.ds = ds;
        this.mongo = ds.getMongoInstance();
    }
    
    private DBObject loadBasonFromFile(InputStream is) {
        DBObject bson;
        try {
            bson = (DBObject)JSON.parse(convertStreamToString(is));
        } catch (Exception e) {
            throw new MetaDataException(MetaErrCodeEnum.JSON_PARING_ERROR, "parse mongo data error: ", e);
        }
        
        return bson;
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is,
                        "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }
    
    public void loadPropertiesFromResource(String fileName) {
        InputStream is = null;
        try {
            is = MetadataDataLoader.class.getResourceAsStream(fileName);
        
            loadProperties(is);
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("error in closing stream: ", e);
            }
        }
    }
    
    public void loadProperties(InputStream is) {
        DBCollection propertiesCollection = mongo.getDB(CMSConsts.SYS_DB).getCollection(CMSConsts.PROPERTIES_COLLECTION);
        
        //load properties collection
        BasicDBObject cmsProperties = (BasicDBObject)loadBasonFromFile(is);
        for (String key : cmsProperties.keySet()) {
            DBObject obj = new BasicDBObject().append(key, cmsProperties.get(key));
            propertiesCollection.insert(obj);
        }
    }
    
    private void loadMetaClassesFromPath(String pathName) {
		try {

			URL url = MetadataDataLoader.class.getResource(pathName);
			URI uri = url.toURI();
			BasicDBList metas = new BasicDBList();
			
			if (uri.isOpaque()) {
				JarURLConnection connection = (JarURLConnection) url.openConnection();
				JarFile jar= connection.getJarFile();
				Enumeration<JarEntry> entries = jar.entries();

				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if (entry.getName().startsWith(pathName.substring(1))
							&& entry.getName().endsWith(".json")) {
						InputStream is = jar.getInputStream(entry);
						readMetaClass(is, metas);
					}
				}
				
			} else {
				File dir = new File(url.toURI());
				Collection<File> files = FileUtils.listFiles(dir, new String[] {"json"}, true);
				for (File f : files) {
					InputStream is = new FileInputStream(f);
					readMetaClass(is, metas);
				}
			}
			
			loadMetaClasses(metas);
			
		} catch (Exception e) {
			logger.error("error in loading metadata: ", e);
		}	
    }
    
    private void readMetaClass(InputStream is, BasicDBList metas) {
    	DBObject bson = loadBasonFromFile(is);
    	if (bson instanceof BasicDBList) {
    		metas.addAll((BasicDBList)bson);
    	} else {
    		metas.add(bson);
    	}
    }
    
    public void loadMetaClassesFromResource(String fileName) {
        InputStream is = null;
        try {
            is = MetadataDataLoader.class.getResourceAsStream(fileName);
            BasicDBList metas = new BasicDBList();
            readMetaClasses(is, metas);
            loadMetaClasses(metas);
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("error in closing stream: ", e);
            }
        }
    }
    
    private void readMetaClasses(InputStream is, BasicDBList metas) {
    	DBObject bson = loadBasonFromFile(is);
    	if (bson instanceof BasicDBList) {
    		metas.addAll((BasicDBList)bson);
    	} else {
    		metas.add(bson);
    	}
    }
    
    public void loadMetaClasses(BasicDBList metas) {
        ObjectConverter<MetaClass> converter = new ObjectConverter<MetaClass>();
        Map<String, ArrayList<MetaClass>> targets = new HashMap<String, ArrayList<MetaClass>>();
        for (Object obj : metas) {
            MetaClass m = converter.fromBson((DBObject)obj, MetaClass.class);
            String repo = m.getRepository();
            if (repo == null) {
            	repo = "cmsdb";
            	m.setRepository(repo);
            }
            if (!targets.containsKey(repo)) {
                targets.put(repo, new ArrayList<MetaClass>());
            }
            targets.get(repo).add(m);
        }

        IRepositoryService repoService = RepositoryServiceFactory.createRepositoryService(ds, "localCMSServer");

        for (Entry<String, ArrayList<MetaClass>> e : targets.entrySet()) {
            String repo = e.getKey();
            ArrayList<MetaClass> value = e.getValue();
            
            //create repo if not exist
            Repository repository;
            try {
                repository = repoService.getRepository(repo);
            }
            catch (RepositoryNotExistsException e1) {
                repoService.createRepository(new Repository(repo));
                repository = repoService.getRepository(repo);
            }
            
            MetadataContext metaContext = new MetadataContext();
            metaContext.setSourceIp("127.0.0.1");
            metaContext.setSubject("tester");
            repository.getMetadataService().batchUpsert(value, metaContext);
        }
    }
    
    public static MetadataDataLoader getInstance(MongoDataSource ds) {
        return new MetadataDataLoader(ds);
    }
    
    public void loadTestDataFromResource() {
        this.cleanUp();
        this.loadPropertiesFromResource("/mongo/properties.json");
//        this.loadRepositoriesFromResource("/mongo/repositories.json");
        this.loadMetaClassesFromResource("/mongo/metaclasses.json");
        this.loadMetaClassesFromResource("/mongo/stratus-metaclasses.json");
    }

    public void loadCMSDBMetaDataFromResource() {
        this.loadMetaClassesFromPath("/mongo/cmsdb-meta/");
    }

    public void loadMaxIndexedArraySizeMetaDataFromResource() {
        this.loadMetaClassesFromPath("/mongo/raptor-paas-maxindexedarraysize-meta");
    }
    
	public void loadMaxNumOfIndexSizeMetaDataFromResource() {
		this.loadMetaClassesFromPath("/mongo/raptor-paas-maxnumofindexsize-meta");
	}

    public static void printUsage() {
        System.out.println("Use: MetadataDataLoader mongoserver datafile.");
        System.out.println("This command will load metaclasses in the datafile into mongoserver.");
        System.out.println("Repositories will be created if not exist.");
    }
    
    public static void main(String args[]) {
        if (args.length != 2) {
            printUsage();
            System.exit(255);
        }
        
        String server = args[0];
        String fileName = args[1];
        
        MongoDataSource ds = new MongoDataSource(server);
        File dataFile = new File(fileName);
        if (!dataFile.exists()) {
            
        }
        
        getInstance(ds).loadMetaClassesFromResource(fileName);
    }

}
