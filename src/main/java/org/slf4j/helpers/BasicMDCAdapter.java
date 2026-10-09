package org.slf4j.helpers;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.spi.MDCAdapter;

public class BasicMDCAdapter implements MDCAdapter {

    private final ThreadLocalMapOfStacks threadLocalMapOfDeques = new ThreadLocalMapOfStacks();

    private final InheritableThreadLocal<Map<String, String>> inheritableThreadLocalMap = new InheritableThreadLocal<Map<String, String>>() {

        protected Map<String, String> childValue(Map<String, String> parentValue) {
            return parentValue == null ? null : new HashMap<>(parentValue);
        }
    };

    @Override
    public void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        } else {
            Map<String, String> map = this.inheritableThreadLocalMap.get();
            if (map == null) {
                map = new HashMap<>();
                this.inheritableThreadLocalMap.set(map);
            }
            map.put(key, val);
        }
    }

    @Override
    public String get(String key) {
        Map<String, String> map = this.inheritableThreadLocalMap.get();
        return map != null && key != null ? map.get(key) : null;
    }

    @Override
    public void remove(String key) {
        Map<String, String> map = this.inheritableThreadLocalMap.get();
        if (map != null) {
            map.remove(key);
        }
    }

    @Override
    public void clear() {
        Map<String, String> map = this.inheritableThreadLocalMap.get();
        if (map != null) {
            map.clear();
            this.inheritableThreadLocalMap.remove();
        }
    }

    public Set<String> getKeys() {
        Map<String, String> map = this.inheritableThreadLocalMap.get();
        return map != null ? map.keySet() : null;
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        Map<String, String> oldMap = this.inheritableThreadLocalMap.get();
        return oldMap != null ? new HashMap<>(oldMap) : null;
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        Map<String, String> copy = null;
        if (contextMap != null) {
            copy = new HashMap<>(contextMap);
        }
        this.inheritableThreadLocalMap.set(copy);
    }

    @Override
    public void pushByKey(String key, String value) {
        this.threadLocalMapOfDeques.pushByKey(key, value);
    }

    @Override
    public String popByKey(String key) {
        return this.threadLocalMapOfDeques.popByKey(key);
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        return this.threadLocalMapOfDeques.getCopyOfDequeByKey(key);
    }

    @Override
    public void clearDequeByKey(String key) {
        this.threadLocalMapOfDeques.clearDequeByKey(key);
    }
}
