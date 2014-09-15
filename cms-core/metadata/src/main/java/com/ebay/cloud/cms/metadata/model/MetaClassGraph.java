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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException;
import com.ebay.cloud.cms.metadata.exception.MetaDataException.MetaErrCodeEnum;

class MetaRelationshipComparator implements Comparator<MetaRelationship>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(MetaRelationship arg0, MetaRelationship arg1) {
        if (arg0 == null || arg1 == null) {
            return -1;
        }
        
        return arg0.getName().compareTo(arg1.getName());
    }
    
}

class ClassInfo {
    private List<MetaRelationship> fromReference;
    private List<String> descendants;
    private List<MetaRelationship> toReference;

    public ClassInfo() {
        super();
        
        fromReference = Collections.emptyList();
        descendants = Collections.emptyList();
        toReference = Collections.emptyList();
    }
    public List<MetaRelationship> getFromReference() {
        return fromReference;
    }
    public void setFromReference(List<MetaRelationship> fromReference) {
        this.fromReference = fromReference;
    }
    public List<String> getDescendants() {
        return descendants;
    }
    public void setDescendants(List<String> descendants) {
        this.descendants = descendants;
    }
    public List<MetaRelationship> getToReference() {
        return toReference;
    }
    public void setToReference(List<MetaRelationship> toReference) {
        this.toReference = toReference;
    }
}

public class MetaClassGraph {

    private static MetaRelationshipComparator comparator = new MetaRelationshipComparator();
        
    private Map<String, ClassInfo> map = new ConcurrentHashMap<String, ClassInfo>();
    private Map<String, MetaClass> metaClassMap = new ConcurrentHashMap<String, MetaClass>();
    
    public MetaClassGraph(Collection<MetaClass> metaClasses) {
        super();
        for (MetaClass metaClass : metaClasses) {
        	metaClassMap.put(metaClass.getName(), metaClass);
        }
    }
    
    public Collection<MetaClass> getMetaClasses() {
    	return metaClassMap.values();
    }
    
    public MetaClass getMetaClass(String metaClassName) {
    	return metaClassMap.get(metaClassName);
    }
    
    public List<MetaRelationship> getToReference(MetaClass m) {
        ClassInfo info = map.get(m.getName());
        if (info == null) {
            throw new MetaDataException(MetaErrCodeEnum.META_CLASS_NOT_FOUND_IN_GRAPH, "can not find metaclass in graph: " + m.getName());
        }
        return info.getToReference();
    }

    public List<MetaRelationship> getFromReference(MetaClass m) {
        ClassInfo info = map.get(m.getName());
        if (info == null) {
            throw new MetaDataException(MetaErrCodeEnum.META_CLASS_NOT_FOUND_IN_GRAPH, "can not find metaclass in graph: " + m.getName());
        }
        return info.getFromReference();
    }
    
    public List<MetaClass> getDescendants(MetaClass m) {
        ClassInfo info = map.get(m.getName());
        if (info == null) {
            throw new MetaDataException(MetaErrCodeEnum.META_CLASS_NOT_FOUND_IN_GRAPH, "can not find metaclass in graph: " + m.getName());
        }
        
        List<String> metas = info.getDescendants();
        if (metas.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<MetaClass> result = new ArrayList<MetaClass>(metas.size());
        for (String name : metas) {
        	result.add(metaClassMap.get(name));
        }
        
        return result;
    }
    
    public void updateMetaClass(MetaClass m) {
        List<MetaRelationship> toReferences = new ArrayList<MetaRelationship>();
        for (MetaField f : m.getFields()) {
            if (f instanceof MetaRelationship) {
                toReferences.add((MetaRelationship) f);
            }
        }
        
        if (toReferences.isEmpty()) {
            toReferences = Collections.emptyList();
        }
 
        Collections.sort(toReferences, comparator);
        
        ClassInfo info = map.get(m.getName());
        
        synchronized (this) {
        	metaClassMap.put(m.getName(), m);
            // get the info again under the protection of lock
            if (info == null) {
                info = new ClassInfo();
                info.setToReference(toReferences);
                map.put(m.getName(), info);
            } else {
                List<MetaRelationship> oldOuts = info.getToReference();
                info.setToReference(toReferences);
                for (MetaRelationship mr : oldOuts) {
                    // if references removed
                    if (m.getFieldByName(mr.getName()) == null) {
                        ClassInfo i = map.get(mr.getRefDataType());
                        if (i != null && !i.getFromReference().isEmpty()) {
                            i.getFromReference().remove(mr);
                        }
                    }
                }
            }
            
            List<String> ancestors = m.getAncestors();
            if (ancestors != null && !ancestors.isEmpty()) {
                for (String ancestor : m.getAncestors()) {
                    ClassInfo i = map.get(ancestor);
                    if (i == null) {
                        i = new ClassInfo();
                        map.put(ancestor, i);
                    }

                    if (!i.getDescendants().contains(m.getName())) {
                        List<String> old = i.getDescendants();
                        ArrayList<String> n = new ArrayList<String>(old.size() + 1);
                        n.addAll(old);
                        n.add(m.getName());
                        i.setDescendants(n);
                    }
                }
            }
            
            for (MetaRelationship to : toReferences) {
                String refName = to.getRefDataType();
                if (refName == null) {
                	throw new IllegalMetaClassException("refDataType must point to referenced metaclass name!");
                }
                ClassInfo i = map.get(refName);
                if (i == null) {
                    i = new ClassInfo();
                    map.put(refName, i);
                }

                if (!i.getFromReference().contains(to)) {
                    List<MetaRelationship> old = i.getFromReference();
                    List<MetaRelationship> n = new ArrayList<MetaRelationship>(old.size() + 1);
                    n.addAll(old);
                    n.add(to);
                    i.setFromReference(n);
                }
            }
        }
    }

    public void deleteMetaClass(MetaClass meta) {
        ClassInfo classInfo = map.remove(meta.getName());
        metaClassMap.remove(meta.getName());
        // update class info that referenced by deleted meta
        for (MetaRelationship outReference : classInfo.getToReference()) {
            ClassInfo i = map.get(outReference.getRefDataType());
            if (i != null && !i.getFromReference().isEmpty()) {
                i.getFromReference().remove(outReference);
            }
        }
        // update ancestors' descendants
        List<String> ancestors = meta.getAncestors();
        if (ancestors != null) {
            for (String ancestor : ancestors) {
                ClassInfo parentInfo = map.get(ancestor);
                if (parentInfo != null) {
                    List<String> descendants = new ArrayList<String>(parentInfo.getDescendants());
                    descendants.remove(meta.getName());
                    parentInfo.setDescendants(descendants);
                }
            }
        }
    }

//    private static boolean equalRelationships(List<MetaRelationship> l1, List<MetaRelationship> l2) {
//        if (l1.size() != l2.size()) {
//            return false;
//        }
//        
//        for (int i = 0; i < l1.size(); i++) {
//            if (!l1.get(i).getName().equals(l2.get(i).getName())) {
//                return false;
//            }
//        }
//        
//        return true;
//    }

}