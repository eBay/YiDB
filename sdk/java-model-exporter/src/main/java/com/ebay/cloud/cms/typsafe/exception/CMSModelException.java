/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exception;

/**
 * @author gowang
 *
 */
public class CMSModelException extends RuntimeException {

	public CMSModelException(String string, Exception e) {
		super(string, e);
	}

	private static final long serialVersionUID = 3958332533517189492L;

}
