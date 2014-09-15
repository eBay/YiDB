/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

/**
 * @author liasu
 * 
 */
public interface ICMSEntityVisitor {

    void processAttribute(ICMSEntity entity, String fieldName);

    void processReference(ICMSEntity entity, String fieldName);

}
