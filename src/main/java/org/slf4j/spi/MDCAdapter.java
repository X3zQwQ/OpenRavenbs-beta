package org.slf4j.spi;

import java.util.Deque;
import java.util.Map;

public interface MDCAdapter {

    void put(String text, String text2);

    String get(String text3);

    void remove(String text4);

    void clear();

    Map<String, String> getCopyOfContextMap();

    void setContextMap(Map<String, String> contextMap);

    void pushByKey(String text5, String text6);

    String popByKey(String text7);

    Deque<String> getCopyOfDequeByKey(String text8);

    void clearDequeByKey(String text9);
}
