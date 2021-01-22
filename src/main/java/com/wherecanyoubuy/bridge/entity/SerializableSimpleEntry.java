package com.wherecanyoubuy.bridge.entity;

import lombok.Getter;

import java.util.AbstractMap;

@Getter
public class SerializableSimpleEntry<K, V> extends AbstractMap.SimpleEntry<K, V> {
    public SerializableSimpleEntry(K key, V value) {
        super(key, value);
    }
}
