***CMS API by category***

Written following markdown format using MarkdownPad.

Note: 

1. {} quoted means parameter, otherwise means fixed url which could be hardcoded. 

2. GET/DELETE don't need request body while POST/PUT always need 

HTTP Headers for All API:
> X-CMS-PRIORITY = [CRITICAL|IMPORTANT|NEUTRAL|NON_CRITICAL|DEBUG]

**Metadata**

1. GET  /repository
 - query all repos
2. POST /repository
 - create a repository
3. GET /repository/{reponame}
 - query given repo
4. GET /repository/{reponame}/metadata
 > Query parameter: 
 >  - mode=[uri|normal]

 - query all metadatas in given repo
5. POST /repository/{reponame}/metadata
 - create a metadata in the given repo
6. GET /repository/{reponame}/metadata/{metatype}
 > Query parameter: 
 >  - mode=[uri|normal]
 >  - fetchHistory=[true|false]

 - query given metatype on the given repo
7. POST /repository/{reponame}/metadata/{metatype}
 - Update a metadata class

8. GET /repository/{reponame}/metadata/{metatype}/indexes
 - Get all indexes on the given metaclass

9. POST /repository/{reponame}/metadata/{metatype}/indexes
 - Add an index to the given metadata

10. DELETE /repository/{reponame}/metadata/{metatype}/indexes/{indexname}
 - Delete the given indexe from given metadata

**Branch**

1. GET /repositories/{reponame}/branches
 - Query all branches
2. GET /repositories/{reponame}/branches/{branchname}
 - Query given branches
3. POST /repositories/{reponame}/branches
 - Create branch
4. POST /repositories/{reponame}/branches/{branchname}
 - Commit branch
5. DELETE /repositories/{reponame}/branches/{branchname}
 - Abort branch

**Entity**

1. GET /repository/{reponame}/branch/{branch}/metadata/{metatype}/{oid}
 > Query parameter: 
 >  - mode=[uri|normal]
 >  - fetchHistory=[true|false]

 - Query all entities of the given metadata type
2. POST /repository/{reponame}/branch/{branch}/metadata/{metatype}
 - Create an entity based on given metadata
3. POST /repository/{reponame}/branch/{branch}/metadata/{metatype}/{oid}
 - Modify a entity
4. PUT /repository/{reponame}/branch/{branch}/metadata/{metatype}/{oid}
 - Replace a entity
5. DELETE /repository/{reponame}/branch/{branch}/metadata/{metatype}/{oid}
 - Delete a entity

**Query**

1. GET /repository/{reponame}/branch/{branch}/{query}
 > Query parameter: 
 >  - mode=[uri|normal]
 >  - explain=[true|false]
 >  - sortOn=field names
 >  - sortOrder=[asc|desc]
 >  - limit= non-negative integer value
 >  - skip = non-negative integer value
 >  - hint = non-negative integer value
 >  - allowFullTableScan = [true|false]

 - query entities
 

**Monitors**

1. GET /monitors
 - query cms system metrics
2. GET /monitors/{metricname}
 - query system metric with the given name

**System Management**

1. GET /state
 - query current system state
2. PUT /state
 - set the system state
3. GET /config
 - get system level configurations
4. POST /config
 - set system level configurations

**Service**

1. GET /services/
 - Query all service status of CMS
2. GET /services/{servicename}
 - Query the status of the given service
3. PUT /services/{servicename}
 - Set the service status of CMS server