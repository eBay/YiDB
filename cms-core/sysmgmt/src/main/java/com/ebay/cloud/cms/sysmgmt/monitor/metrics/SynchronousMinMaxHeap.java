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


/**
 * 
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

package com.ebay.cloud.cms.sysmgmt.monitor.metrics;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simulator heap with de-dup capability. NOTE: it's implemented by a sorted
 * list for heap character and additional map to de-dup, not a *REAL* heap. So
 * it supposed to be used only with little size of heap. Otherwise there would
 * be performance concern.
 * 
 * <p>
 * TODO: Write a heap with ability to modify a heap node's weight could help to
 * replace the performance implication implementation above.
 * 
 * TODO: Use tree set instead of LinkedList
 * 
 * @author Liangfei(Ralph) Su
 * 
 */
public class SynchronousMinMaxHeap<T extends Comparable<T>> {

    private ReentrantLock lock = new ReentrantLock();

    static final class Link<ET> {
        Link<ET> previous;
        Link<ET> next;
        ET       data;

        Link(ET t, Link<ET> p, Link<ET> n) {
            this.data = t;
            this.previous = p;
            this.next = n;
        }
    }

    private Map<String, Link<T>> itemMap;
    // head node, void->next point to the first node in the list; void->prev
    // point to the last node in the list
    private Link<T>              voidLink;
    private int                  size;
    private final int            maximumSize;

    public SynchronousMinMaxHeap(int maxSize) {
        itemMap = new HashMap<String, Link<T>>();
        voidLink = new Link<T>(null, null, null);
        voidLink.next = voidLink;
        voidLink.previous = voidLink;
        maximumSize = maxSize;
        size = 0;
    }

    public boolean offer(T o) {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Link<T> existingItem = itemMap.get(o.toString());
            if (existingItem == null) {
                if (size >= maximumSize) {
                    // if exceed maximum, may need to remove last first
                    T oldLast = peekLast();
                    if (oldLast.compareTo(o) > 0) {
                        return false;
                    }
                    removeLastImpl();
                }

                return addFromFirst(o);
            } else {
                if (existingItem.data.compareTo(o) < 0) {
                    // adjust the given item weight
                    removeItemFromList(existingItem);
                    existingItem.data = o;

                    //find the appropriate position
                    Link<T> p = voidLink.next;
                    while (p != voidLink) {
                        if (p.data.compareTo(o) >= 0) {
                            p = p.next;
                        } else {
                            p = p.previous;
                            break;
                        }
                    }

                    addIntoList(existingItem, p);
                    return true;
                }
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    private void addIntoList(Link<T> existingItem, Link<T> p) {
        // in-queue
        existingItem.next = p.next;
        p.next.previous = existingItem;

        existingItem.previous = p;
        p.next = existingItem;
    }

    private void removeItemFromList(Link<T> existingItem) {
        Link<T> p = existingItem.previous;
        Link<T> n = existingItem.next;
        p.next = n; // remove this one from list
        n.previous = p;
    }

    public T pollLast() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return removeLastImpl();
        } finally {
            lock.unlock();
        }
    }

    Map<String, Link<T>> getItemMap() {
        return itemMap;
    }
    
    Link<T> getVoidLink() {
        return voidLink;
    }

    private T removeLastImpl() {
        Link<T> last = voidLink.previous;
        if (last != voidLink) {
            Link<T> previous = last.previous;
            voidLink.previous = previous;
            previous.next = voidLink;
            size--;
            itemMap.remove(last.data.toString());
            return last.data;
        }
        throw new NoSuchElementException();
    }

    /**
     * Add given object in the correct position of the list, iteration start
     * from first
     * 
     * @param o
     * @return
     */
    private boolean addFromFirst(T o) {
        Link<T> p = voidLink.next;
        while (p != voidLink) {
            if (p.data.compareTo(o) > 0) {
                p = p.next;
            } else {
                // found the slot, o should be before start
                p = p.previous;
                break;
            }
        }

        Link<T> newNode = new Link<T>(o, p, p.next);
        p.next = newNode;
        newNode.next.previous = newNode;
        itemMap.put(o.toString(), newNode);
        size++;
        return true;
    }

    public T poll() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return removeFirstImpl();
        } finally {
            lock.unlock();
        }
    }

    private T removeFirstImpl() {
        Link<T> first = voidLink.next;
        if (first != voidLink) {
            Link<T> next = first.next;
            voidLink.next = next;
            next.previous = voidLink;
            size--;
            itemMap.remove(first.data.toString());
            return first.data;
        }
        throw new NoSuchElementException();
    }

    public T peek() {
        return peekFirst();
    }

    public boolean contains(Object object) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Link<T> link = voidLink.next;
            if (object != null) {
                while (link != voidLink) {
                    if (object.equals(link.data)) {
                        return true;
                    }
                    link = link.next;
                }
            } else {
                while (link != voidLink) {
                    if (link.data == null) {
                        return true;
                    }
                    link = link.next;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            if (size > 0) {
                size = 0;
                voidLink.next = voidLink;
                voidLink.previous = voidLink;
                itemMap.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(T[] contents) {
        T[] cont = contents;
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            int index = 0;
            if (size > contents.length) {
                Class<?> ct = contents.getClass().getComponentType();
                cont = (T[]) Array.newInstance(ct, size);
            }
            Link<T> link = voidLink.next;
            while (link != voidLink) {
                cont[index++] = (T) link.data;
                link = link.next;
            }
            if (index < cont.length) {
                cont[index] = null;
            }
            return cont;
        } finally {
            lock.unlock();
        }
    }

    public Object[] toArray() {
        int index = 0;
        Object[] contents = new Object[size];
        Link<T> link = voidLink.next;
        while (link != voidLink) {
            contents[index++] = link.data;
            link = link.next;
        }
        return contents;
    }

    public T peekFirst() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Link<T> first = voidLink.next;
            return first == voidLink ? null : first.data;
        } finally {
            lock.unlock();
        }
    }

    public T peekLast() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Link<T> last = voidLink.previous;
            return (last == voidLink) ? null : last.data;
        } finally {
            lock.unlock();
        }
    }

}
