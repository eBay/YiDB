#Introduction#

**YiDB is a metadata-driven persistence and query service for configuration data**

YiDB (Configuration Management Database) is a high performance metadata-driven persistence and query service for configuration data with supporting of RESTful API and client lib (Java, Python). YiDB is a generic system that be used for cloud configuration, as well other software needs for configuration. Moreover, YiDB supports multiple data repositories for desired data isolation.

Why the name "YiDB"? Yi in Chinese stands for "easy", also there's a famous Chinese ancient literature "Yi Jing" that tells the art of mastering forthtelling and prediction. The project was rooted and developed by a great team mainly in Shanghai China, hence the name was chosen.

**Metadata Model**

The metadata model is based on object-oriented paradigm that can support graph/tree data model. The metadata can contain two types of field: attribute field define payload of entity and relationship field define relationship between entity. The metadata is extensible by inheritance: child metadata can inherit fields from parent metadata. 

**Persistence Service**

The persistence service provides CRUD API for the runtime entity of metadata. The entity can be flat-structure or embedded-structure that conformed to the metadata definition. Besides the basic functionality, it also supports some advanced features: version based optimistic locking, audit tracing, reference integrity, security access control of data, branching and advanced data browser.

**Query Service**

The query service provides an imperative style query language that defines the traversal path of graph/tree data model. The query language supports Boolean filter, attribute selection and implicit join that will extract a tree result from graph data set. For example, *ApplicationService[@name = "comp-iq"].groups[@name = "columns"].groups[@name = "col1"].serviceInstances* will return service instances under column 1 of comp-iq application. The query language also provides SQL like utilities such as sort, limit, skip, hint, explain and etc. 

**System Management**

YiDB also comes with a system management utility to maintain system healthiness includes monitoring metrics (e.g. API latency / qps) and throttling low-priority API under overload status. The whole system is available to be deployed independently. 


#Getting Started#

##Prerequisites##

YiDB need a Mongo DB 2.0 + to run at background for data persistence. 

If using 64-bit linux, you can do folowing to install and run Mongo DB.

	# Download Mongo DB for your OS
	$ wget http://fastdl.mongodb.org/linux/mongodb-linux-x86_64-2.2.0.tgz
	
	# Install Mongo DB
	$ tar -xvzf mongodb-linux-x86_64-2.2.0.tgz
	
	# Create Directory for Database File  
	$ cd mongodb-linux-x86_64-2.2.0
	$ mkdir data
	
	# start Mongo DB server	
	$ bin/mongod --dbpath ./data


##Quick Start##

YiDB is implemented in java and managed by maven. It's easy for user to build and try on a normal computer.

But for users who just want to try without installing anything, we provid an online console to try. 

**Online Console**

After deployment, an [Online Console](http://host:8080/ui/console.html) is provided to sent queries using a web page and display the result. 


**Build**

YiDB using maven to manage project. 
you can run :

		$ git clone https://github.com/eBay/YiDB.git yidb
		$ cd yidb/cms-core
		$ mvn clean install -DskipTests
		$ cd web
		$ mvn war:war
to build the project. 

**Run**

First start mongo db on localhost:27017.

Then you can either put the builed war file into a Servlet container to run or run the following:

		$ cd yidb/bin
		$ ./build.sh
		$ ./demo.sh (or with parameter -initData to reload test data)

to run YiDB server in a embedded Jetty Server.

**Production Deployment**

Production Deployment can be done through cronus agent and cronus master, instruction to be updated


#FAQ#

**What is YiDB?**

YiDB is a key foundation component of the eBay cloud platform. It provides a high performance metadata-driven persistence and query service for eBay's data-center configuration data. Moreover, YiDB is a generic system that be used for persistence of structural data.

YiDB offers a high-performance persistence mechanism that combines the scalability of NoSQL databases and the data integrity enforcement typically only found in relational databases. We built YiDB on top of MongoDB and implemented services like metadata management, query language, referential integrity, access control and branching to address the limitations of NoSQL database. 

