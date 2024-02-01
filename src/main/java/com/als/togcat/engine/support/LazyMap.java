package com.als.togcat.engine.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/1 上午10:53
 */
public class LazyMap<V> {
    private Map<String, V> map = null;

    protected V get(String name) {
        if (this.map == null) {
            return null;
        }
        return this.map.get(name);
    }

    protected Set<String> keySet() {
        if (this.map == null) {
            return new HashSet<>();
        }
        return this.map.keySet();
    }

    protected Enumeration<String> keyEnumeration() {
        if (this.map == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(this.map.keySet());
    }

    protected boolean containsKey(String name) {
        if (this.map == null) {
            return false;
        }
        return this.map.containsKey(name);
    }

    protected V put(String name, V value) {
        if (this.map == null) {
            this.map = new HashMap<>();
        }
        return this.map.put(name, value);
    }

    protected V remove(String name) {
        if (this.map != null) {
            return this.map.remove(name);
        }
        return null;
    }

    protected Map<String, V> map() {
        if (this.map == null) {
            return new HashMap<>();
        }
        return Collections.unmodifiableMap(this.map);
    }
}
