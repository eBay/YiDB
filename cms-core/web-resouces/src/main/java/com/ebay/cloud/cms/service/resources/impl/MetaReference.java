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

package com.ebay.cloud.cms.service.resources.impl;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.metadata.model.MetaRelationship;

public class MetaReference {

    private List<MetaRelationship> inReference;

    private List<MetaRelationship> outReference;

    public MetaReference() {
        inReference = new ArrayList<MetaRelationship>();
        outReference = new ArrayList<MetaRelationship>();
    }

    @JsonIgnore
    public void addInReference(MetaRelationship meta) {
        inReference.add(meta);
    }

    @JsonIgnore
    public void addOutReference(MetaRelationship meta) {
        outReference.add(meta);
    }

    public List<MetaRelationship> getInReference() {
        return new ArrayList<MetaRelationship>(inReference);
    }

    public void setInReference(List<MetaRelationship> inRefs) {
        List<MetaRelationship> links = new ArrayList<MetaRelationship>(inRefs);
        inReference = links;
    }

    public List<MetaRelationship> getOutReference() {
        return new ArrayList<MetaRelationship>(outReference);
    }

    public void setOutReference(List<MetaRelationship> outRefs) {
        List<MetaRelationship> links = new ArrayList<MetaRelationship>(outRefs);
        outReference = links;
    }

}
