/**
 * 
 */
package com.ebay.cloud.cms.typsafe.entity;

import java.util.Date;

/**
 * Interface defined the field that in every CMS entity
 * 
 * @author liasu
 *
 */
public interface ICMSEntity extends IGenericEntity {

    String get_repo();
    void set_repo(String repo);

    String get_type();
    void set_type(String _type);

    public enum _StatusEnum {
        ACTIVE("active"), DELETED("deleted");

        private String value;

        _StatusEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static _StatusEnum fromString(String value) {
            if (value == null || value.length() < 1) {
                return null;
            }
            for (_StatusEnum v : _StatusEnum.values()) {
                if (v.toString().equals(value)) {
                    return v;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    _StatusEnum get_status();
    void set_status(_StatusEnum status);

    Date get_lastmodified();
    void set_lastmodified(Date date);
    
    Date get_createtime();
    void set_createtime(Date _createtime);

    String get_metaclass();
    void set_metaclass(String meta);

    String get_id();
    void set_id(String oid);

    String get_branch();
    void set_branch(String branch);

    Integer get_version();
    void set_version(Integer version);

    Integer get_pversion();
    void set_pversion(Integer pversion);
    
    String get_shardKey();
    void set_shardKey(String sh);

    String get_hostEntity();
    void set_hostEntity(String host);
    
    String get_creator();
    void set_creator(String creator);
    
    String get_modifier();
    void set_modifier(String modifier);
    
    String get_comment();
    void set_comment(String comment);

}
