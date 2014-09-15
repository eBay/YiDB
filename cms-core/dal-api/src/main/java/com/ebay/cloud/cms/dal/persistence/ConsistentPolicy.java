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

package com.ebay.cloud.cms.dal.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

public class ConsistentPolicy {

    public static final ConsistentPolicy PRIMARY_ONLY = new ConsistentPolicy("PRIMARY_ONLY", ReadPreference.primary());

    public static final ConsistentPolicy PRIMARY_PREFERRED = new ConsistentPolicy("PRIMARY_PREFERRED",
            ReadPreference.primaryPreferred());

    public static final ConsistentPolicy PRIMARY = new ConsistentPolicy("PRIMARY", ReadPreference.primary());

    public static final ConsistentPolicy SECONDARY_ONLY = new ConsistentPolicy("SECONDARY_ONLY",
            ReadPreference.secondary());

    public static final ConsistentPolicy SECONDARY_PREFERRED = new ConsistentPolicy("SECONDARY_PREFERRED",
            ReadPreference.secondaryPreferred());

    public static final ConsistentPolicy NEAREST = new ConsistentPolicy("NEAREST", ReadPreference.nearest());

    public static final ConsistentPolicy REPLICA_ACKNOWLEDGED = new ConsistentPolicy("REPLICA_ACKNOWLEDGED",
            ReadPreference.nearest(), WriteConcern.REPLICA_ACKNOWLEDGED);

    public static final ConsistentPolicy MAJORITY = new ConsistentPolicy("MAJORITY", ReadPreference.nearest(),
            WriteConcern.MAJORITY);

    private static final List<ConsistentPolicy> VALUES = new ArrayList<ConsistentPolicy>();
    static {
        VALUES.add(PRIMARY_ONLY);
        VALUES.add(PRIMARY_PREFERRED);
        VALUES.add(PRIMARY);
        VALUES.add(SECONDARY_ONLY);
        VALUES.add(SECONDARY_PREFERRED);
        VALUES.add(NEAREST);
        VALUES.add(REPLICA_ACKNOWLEDGED);
        VALUES.add(MAJORITY);
    }

    public static final Map<String, ReadPreference> READ_PREFERENCE_MAP = new HashMap<String, ReadPreference>();
    static {
        READ_PREFERENCE_MAP.put("PRIMARY_ONLY", ReadPreference.primary());
        READ_PREFERENCE_MAP.put("PRIMARY_PREFERRED", ReadPreference.primaryPreferred());
        READ_PREFERENCE_MAP.put("PRIMARY", ReadPreference.primary());
        READ_PREFERENCE_MAP.put("SECONDARY_ONLY", ReadPreference.secondary());
        READ_PREFERENCE_MAP.put("SECONDARY_PREFERRED", ReadPreference.secondaryPreferred());
        READ_PREFERENCE_MAP.put("NEAREST", ReadPreference.nearest());
    }

    public static final Map<String, WriteConcern> WRITE_CONCERN_MAP = new HashMap<String, WriteConcern>();
    static {
        WRITE_CONCERN_MAP.put("SAFE", WriteConcern.ACKNOWLEDGED);
        WRITE_CONCERN_MAP.put("MAJORITY", WriteConcern.MAJORITY);
        WRITE_CONCERN_MAP.put("ACKNOWLEDGED", WriteConcern.ACKNOWLEDGED);
        WRITE_CONCERN_MAP.put("REPLICA_ACKNOWLEDGED", WriteConcern.REPLICA_ACKNOWLEDGED);
    }
    
    private final String name;
    private final ReadPreference pref;
    private final WriteConcern concern;

    public ConsistentPolicy(String name, ReadPreference rp) {
        this.pref = rp;
        this.concern = WriteConcern.ACKNOWLEDGED;
        this.name = name;
    }

    public ConsistentPolicy(String name, WriteConcern concern) {
        this.name = name;
        this.concern = concern;
        this.pref = READ_PREFERENCE_MAP.get(name.toUpperCase());
    }
    
    public ConsistentPolicy(String name, ReadPreference rp, WriteConcern concern) {
        this.name = name;
        this.pref = rp;
        this.concern = concern;
    }

    public ReadPreference getReadPreference() {
        return pref;
    }

    public WriteConcern getWriteConcern() {
        return concern;
    }

    public String getName() {
        return name;
    }

    public static ConsistentPolicy safePolicy() {
        return ConsistentPolicy.NEAREST;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(pref.getName());
        sb.append(", Read Preference: ").append(pref.toString());
        sb.append(", Write Concern: ").append(concern.toString());
        return sb.toString();
    }

    public static ConsistentPolicy[] values() {
        return VALUES.toArray(new ConsistentPolicy[0]);
    }

    public static ConsistentPolicy parseString(String policy, String writeConcern) {
        if (policy == null) {
            return null;
        }

        String uppercase = policy.toUpperCase();
        if (policy.equals("MAJORITY")) {
            return MAJORITY;
        } else if (policy.equals("REPLICA_ACKNOWLEDGED")) {
            return REPLICA_ACKNOWLEDGED;
        }
        
        WriteConcern wc = WRITE_CONCERN_MAP.get(writeConcern);
        if (wc == null) {
            return null;
        }
        
        for (ConsistentPolicy cp : VALUES) {
            if (cp.getName().equals(uppercase)) {
                return new ConsistentPolicy(policy, cp.getReadPreference(), wc);
            }
        }
        return null;
    }
    
}