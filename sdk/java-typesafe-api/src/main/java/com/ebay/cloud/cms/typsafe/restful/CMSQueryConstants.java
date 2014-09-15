package com.ebay.cloud.cms.typsafe.restful;


public final class CMSQueryConstants {
    public static final String QUERY_STRING = "%s[exists @%s and not @%s =& %s{@_oid}]{*}";
    
    public static final String QUERY_EMPTY_REFERENCE_STRING = "%s[ isempty @%s ]{*}";

    private CMSQueryConstants(){};
    
    
}
