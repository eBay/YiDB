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

package com.ebay.cloud.cms.query.metadata;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.metadata.model.MetaRelationship;
import com.ebay.cloud.cms.utils.EqualsUtil;

public class ReverseMetaRelationship extends MetaRelationship {

    @JsonIgnore
	private MetaRelationship reversedReference;

    @JsonIgnore
	public MetaRelationship getReversedReference() {
		return reversedReference;
	}

	@JsonIgnore
	public void setReversedReference(MetaRelationship reversedRefernce) {
		this.reversedReference = reversedRefernce;
	}

	@Override
    public int hashCode() {
        if (getName() == null) {
            return "ReverseMetaRelationship".hashCode();
        }

        return super.hashCode();
    }

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (!super.equals(other)) {
			return false;
		}

		if (!(other instanceof ReverseMetaRelationship))
			return false;

		ReverseMetaRelationship o = (ReverseMetaRelationship) other;
		return EqualsUtil.equal(reversedReference, o.reversedReference);
	}
}
