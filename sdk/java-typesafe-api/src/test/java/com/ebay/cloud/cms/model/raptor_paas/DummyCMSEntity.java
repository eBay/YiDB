/**
 * 
 */
package com.ebay.cloud.cms.model.raptor_paas;

import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;

/**
 * An dummy entity to test for the missing meta data cases
 * 
 * @author liasu
 * 
 */
public class DummyCMSEntity extends GenericCMSEntity {

    private String repo = "raptor-paas";

    private String metaclass = "DummyCMSEntity";
    
    public DummyCMSEntity() {
        set_type(get_metaclass());
    }

    public void set_repo(String repo) {
        this.repo = repo;
    }

    public String get_repo() {
        return repo;
    }

    public final String get_metaclass() {
        return metaclass;
    }

    public final void set_metaclass(String metaclass) {
        this.metaclass = metaclass;
    }

}
