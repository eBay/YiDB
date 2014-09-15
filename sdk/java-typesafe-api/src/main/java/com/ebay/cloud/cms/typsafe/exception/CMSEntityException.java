/**
 * 
 */
package com.ebay.cloud.cms.typsafe.exception;

/**
 *
 */
public class CMSEntityException extends RuntimeException {

    public CMSEntityException(String string) {
        super(string);
    }
    
    public CMSEntityException(String string, Exception e) {
        super(string, e);
    }

    private static final long serialVersionUID = 3958332533517189492L;

}
