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

package com.ebay.cloud.cms.sysmgmt.monitor;

/**
 * @author Liangfei(Ralph) Su
 * 
 */
public final class MetricConstants {

	//TNSW - query
	public static final String TOP_QUERY_TNSW_APSW_SUCCESS = "topQuery_24h";
	public static final String TOP_QUERY_TNSW_ACSW_SUCCESS = "topQuery_1m";
	
	public static final String TOP_QUERY_TNSW_APSW_FAILURE = "fail_topQuery_24h";
	public static final String TOP_QUERY_TNSW_ACSW_FAILURE = "fail_topQuery_1m";
	
	// TNSW - write
	public static final String TOP_WRITE_TNSW_APSW_SUCCESS = "topWrite_24h";
    public static final String TOP_WRITE_TNSW_ACSW_SUCCESS = "topWrite_1m";
    
    public static final String TOP_WRITE_TNSW_APSW_FAILURE = "fail_topWrite_24h";
    public static final String TOP_WRITE_TNSW_ACSW_FAILURE = "fail_topWrite_1m";

	//ACSW:Accurate Sliding Window Metrics
	public static final String LATENCY_READ_ACSW_SUCCESS = "latencyRead_1m";
	public static final String LATENCY_WRITE_ACSW_SUCCESS = "latencyWrite_1m";
	
	public static final String LATENCY_READ_ACSW_FAILURE = "fail_latencyRead_1m";
	public static final String LATENCY_WRITE_ACSW_FAILURE = "fail_latencyWrite_1m";
	
	public static final String READ_QPS_ACSW_ALL = "readqps_1m";
	public static final String WRITE_QPS_ACSW_ALL = "writeqps_1m";
	
	public static final String READ_QPM_ACSW_SUCCESS = "readqpm";
	public static final String WRITE_QPM_ACSW_SUCCESS = "writeqpm";
	public static final String READ_QPM_ACSW_FAILURE = "fail_readqpm";
	public static final String WRITE_QPM_ACSW_FAILURE = "fail_writeqpm";
	
	public static final String READ_QPS_ACSW_SUCCESS = "readqps";
	public static final String WRITE_QPS_ACSW_SUCCESS = "writeqps";
	public static final String READ_QPS_ACSW_FAILURE = "fail_readqps";
	public static final String WRITE_QPS_ACSW_FAILURE = "fail_writeqps";
	
	public static final String LONG_QUERY_ACSW = "longquery_1m";

	// os category
	public static final String OS_CPU_SYSTEM = "cpu_sy";
	public static final String OS_CPU_USAGE = "cpu_us";
	public static final String OS_CPU_CS = "cpu_cs";
	public static final String OS_MEMORY_FREE = "mem_free";

	// jvm category
	public static final String JVM_CPU = "cpu";
	public static final String JVM_CSWCH = "cswch/s";
	public static final String JVM_MEMORY = "mem";
	public static final String JVM_ACTIVE_THREAD = "activethread";
	public static final String JVM_UPTIME = "uptime";
	public static final String JVM_GC_MJCOUNT = "gc_major_count";
	public static final String JVM_GC_MJ_LATENCY = "gc_major_latency";
	public static final String JVM_GC_MJ_INTERVAL = "gc_major_interval";
	public static final String JVM_GC_MI_COUNT = "gc_minor_count";
	public static final String JVM_GC_MI_LATENCY = "gc_minor_latency";
	public static final String JVM_GC_MI_INTERVAL = "gc_minor_interval";
	public static final String JVM_HEAP_MEMORY_USAGE = "heap_memory_usage";

	// cms category
	public static final String CMS_STATE = "state";
	public static final String CMS_ACTIVE_THREAD = "active_threads";
	public static final String CMS_MONGO_CONNECTION = "mongo_connection";
	public static final String CMS_CACHE_SIZE = "cache_size";
	public static final String CMS_CACHE_LIVENESS = "cache_liveness";
	public static final String CMS_LOCK_COUNT = "lock_count";
	public static final String CMS_LOCK_DURATION = "lock_duration";
    public static final String MONGO_METRIC = "mongoMetric";
    public static final String MONGO_DRIVER_VERSION = "mongo_driverVersion";
    public static final String REPL_STATUS = "mongo_replStatus";
    public static final String REPL_MASTER = "mongo_replMaster";
    public static final String REPL_DATABASES = "mongo_databases";
    public static final String MONGO_DB_SIZE = "mongo_db_size";
}
