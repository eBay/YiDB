package com.ebay.cloud.cms.model.raptor_paas;

import java.util.List;
import java.util.ArrayList;


import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * CMS generated entity.
 * This class is generated by CMS type-safe api model exporter, DON'T modify this class.
 * 
 */
public class Manager extends GenericCMSEntity {


	public Manager() {
		set_type(get_metaclass());
//		set_repo("raptor-paas");
	}

	// the getter/setter method
	

	public String getName(){
		return (String)getFieldValue("name");
	}

	public void setName(String attrVal_name){
		setFieldValue("name", attrVal_name);
	}
		
	public List<Team> getHead(){
		return (List<Team>) getFieldValue("head");
	}

	public void setHead(List<Team> attrVals_head){
		setFieldValue("head", attrVals_head);
	}
	
	public void addHead(Team attrVal_head){
		addFieldValue("head", attrVal_head);
	}

	@JsonIgnore
	public String get_metaclass(){
		return "Manager";
	}
	
}