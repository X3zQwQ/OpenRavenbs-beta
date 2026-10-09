package org.slf4j.helpers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalMapOfStacks {

    final ThreadLocal<Map<String, Deque<String>>> tlMapOfStacks = new ThreadLocal<>();

    public void pushByKey(String key, String value) {
        if (key != null) {
            Map<String, Deque<String>> map = this.tlMapOfStacks.get();
            if (map == null) {
                map = new HashMap<>();
                this.tlMapOfStacks.set(map);
            }
            Deque<String> deque = map.get(key);
            if (deque == null) {
                deque = new ArrayDeque<>();
            }
            deque.push(value);
            map.put(key, deque);
        }
    }

    public String popByKey(String key) {
        if (key == null) {
            return null;
        } else {
            Map<String, Deque<String>> map = this.tlMapOfStacks.get();
            if (map == null) {
                return null;
            } else {
                Deque<String> deque = map.get(key);
                return deque == null ? null : deque.pop();
            }
        }
    }

    public Deque<String> getCopyOfDequeByKey(String key) {
        if (key == null) {
            return null;
        } else {
            Map<String, Deque<String>> map = this.tlMapOfStacks.get();
            if (map == null) {
                return null;
            } else {
                Deque<String> deque = map.get(key);
                return deque == null ? null : new ArrayDeque<>(deque);
            }
        }
    }

    public void clearDequeByKey(String key) {
        if (key != null) {
            Map<String, Deque<String>> map = this.tlMapOfStacks.get();
            if (map != null) {
                Deque<String> deque = map.get(key);
                if (deque != null) {
                    deque.clear();
                }
            }
        }
    }
}
