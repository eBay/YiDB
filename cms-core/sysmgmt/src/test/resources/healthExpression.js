var load = 0.2;
if ( ($latencyWrite_1m_p50 > 350) && ($writeqps_1m > 700) ) {
	load = 0.9;
}
if ( ($latencyRead_1m_p50 > 350) && ($readqps_1m > 700) ) {
	load = 0.9;
}
if ( ($latencyWrite_1m_p50 > 500) && ($writeqps_1m > 1000) ) {
    load = 0.95;
}
if ( ($latencyRead_1m_p50 > 500) && ($readqps_1m > 1000) ) {
    load = 0.95;
}
if ( $mongo_replMaster == "not found!" ) {
	load = 1.5;//1 < value < 3 means mongo issue: 2 > value > 1 - write not available
}

if ( $mongo_databases == "not found!" ) {
	load = 2.5;//1 < value < 3 means mongo issue: 3 > value > 2 - read not available
}
load;