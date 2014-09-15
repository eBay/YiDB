package com.ebay.cloud.cms.typsafe.restful;

import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;
import com.ebay.cloud.cms.typsafe.service.CMSClientConfig;

/**
 * 
 * @author liasu
 * 
 */
public class FieldCASJsonBuilder extends FieldJsonBuilder {

    private static final String CAS_FORMAT = "{ \"oldValue\" : %s, \"newValue\" : %s }";

    private ICMSEntity oldEntity;

    public FieldCASJsonBuilder(CMSClientConfig config, String fieldName, ICMSEntity oldEntity) {
        super(config, fieldName);
        this.oldEntity = oldEntity;
    }

    @Override
    public String buildJson(Object object) {
        String newValue = super.buildJson(object);
        String oldValue = super.buildJson(oldEntity);
        return String.format(CAS_FORMAT, oldValue, newValue);
    }

}
