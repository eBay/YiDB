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

package com.ebay.cloud.cms.metadata.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.ebay.cloud.cms.consts.CMSConsts;
import com.ebay.cloud.cms.metadata.exception.IllegalIndexException;
import com.ebay.cloud.cms.utils.CheckConditions;
import com.ebay.cloud.cms.utils.EqualsUtil;
import com.ebay.cloud.cms.utils.StringUtils;

/**
 * It define the index info that will be used in query optimizer
 * 
 * @author xjiang
 * @author liasu
 * 
 */
public class IndexInfo {

	// ===== inner class =====

	public static enum IndexOptionEnum {
		unique, sparse, hashed
	}

	private String indexName;

	private Set<IndexOptionEnum> options = new HashSet<IndexInfo.IndexOptionEnum>();

	private LinkedList<String> keyList = new LinkedList<String>();

	private boolean internal;

	private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9_]*");

	public IndexInfo() {
	}

	public IndexInfo(String idxName) {
		this.indexName = idxName;
	}

	public IndexInfo(String idxName, boolean internal) {
		this.indexName = idxName;
		this.internal = internal;
	}

	/**
	 * Construct an inherited index from the given field
	 */
	public IndexInfo(IndexInfo other, MetaRelationship field) {
		String prefix = field.getName();
		this.indexName = prefix + "." + other.indexName;
		this.options = new HashSet<IndexOptionEnum>(other.options);
		// since the embed field might not have field, it must be spare for
		// unique index
		if (this.options.contains(IndexOptionEnum.unique)) {
			this.options.add(IndexOptionEnum.sparse);
		}
		this.keyList = new LinkedList<String>();
		for (String key : other.keyList) {
			keyList.add(prefix + "." + key);
		}
		this.internal = true;
	}

	public void addKeyField(String fieldName) {
		keyList.add(fieldName);
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String idxName) {
		this.indexName = idxName;
	}

	public List<String> getKeyList() {
		return keyList;
	}

	public List<IndexInfo.IndexOptionEnum> getIndexOptions() {
		return new ArrayList<IndexInfo.IndexOptionEnum>(options);
	}

	void setIndexOptions(List<IndexInfo.IndexOptionEnum> newOption) {
		this.options.clear();
		this.options.addAll(newOption);
	}

	public void addOption(IndexOptionEnum option) {
		options.add(option);
	}

	public void removeOption(IndexOptionEnum option) {
		options.remove(option);
	}

	private void checkIndexName() {
		CheckConditions.checkCondition(!StringUtils.isNullOrEmpty(indexName) && !StringUtils.isNullOrEmpty(indexName.trim()),
				new IllegalIndexException("index name can not be empty"));
		CheckConditions.checkCondition(indexName.length() < CMSConsts.MAX_LENGTH_OF_INDEX_NAME,
				new IllegalIndexException("The length of index name can not be exceed "
						+ CMSConsts.MAX_LENGTH_OF_INDEX_NAME));
		CheckConditions.checkCondition(PATTERN.matcher(getIndexName()).matches(), new IllegalIndexException(
				"index name must be characters of [A-Za-z0-9_]*!"));
	}

	public void validate() {

		checkIndexName();

		CheckConditions.checkCondition(keyList.size() < CMSConsts.MAX_FIELDS_OF_COMPOUND_INDEX,
				new IllegalIndexException("The number of fields in a compound index can not be exceed "
						+ CMSConsts.MAX_FIELDS_OF_COMPOUND_INDEX));

		for (String key : keyList) {
			if (!PATTERN.matcher(key).matches()) {
				throw new IllegalIndexException("key name must be valid attribute name characters of [A-Za-z0-9_]*!");
			}
		}

		// hashed index validation
		// "You may not create compound indexes that have hashed index fields or specify a unique constraint on a hashed index"
		// (see http://docs.mongodb.org/manual/core/indexes/#index-type-hashed)
		if (options.contains(IndexOptionEnum.hashed)) {
			if (options.contains(IndexOptionEnum.unique)) {
				throw new IllegalIndexException("Currently hashed indexes cannot guarantee uniqueness!");
			}
			if (keyList.size() > 1) {
				throw new IllegalIndexException("Currently only single field hashed index supported!");
			}
		}
	}

	@JsonIgnore
	public boolean isOverridable(IndexInfo info) {
		return EqualsUtil.equal(indexName, info.indexName) && (internal == info.internal)
				&& (!compareCollections(keyList, info.keyList) || !compareCollections(options, info.options));
	}

	private boolean compareCollections(Collection<?> coll1, Collection<?> coll2) {
		if (coll1.size() == coll2.size()) {
			for (Object o : coll2) {
				if (!coll1.contains(o)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@JsonIgnore
	public boolean isInternal() {
		return internal;
	}

	// === factory method block for internal oid index.

	public static final String OID_INDEX = "__oidIndex__";
	public static final String PK_INDEX = "__pkIndex__";
	public static final String INDEX_PREFIX = "__";
	public static final String INDEX_POSTFIX = "__Index__";
	public static final String LAST_MODIFIED_INDEX = "__lastModifiedIndex__";

	private static IndexInfo oidIndex;
	private static IndexInfo lastModifiedIndex;
	static {
        oidIndex = new IndexInfo(OID_INDEX, true);
        oidIndex.addKeyField(InternalFieldEnum.ID.getName());
        oidIndex.addOption(IndexOptionEnum.unique);

        lastModifiedIndex = new IndexInfo(LAST_MODIFIED_INDEX, true);
        lastModifiedIndex.addKeyField(InternalFieldEnum.LASTMODIFIED.getName());
    }

	public static List<IndexInfo> getInternalIndexes() {
        List<IndexInfo> list = new ArrayList<IndexInfo>();
        list.add(oidIndex);
        list.add(lastModifiedIndex);
        return list;
    }
}
