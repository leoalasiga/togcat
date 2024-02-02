package com.als.togcat.engine.support;

import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:12
 */
public class Attributes extends LazyMap<Object>  {
    public Attributes(boolean concurrent) {
        super(concurrent);
    }

    public Attributes() {
        this(false);
    }

    public Object getAttribute(String name) {
        Objects.requireNonNull(name, "name is null.");
        return super.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return super.keyEnumeration();
    }

    public Object setAttribute(String name, Object value) {
        Objects.requireNonNull(name, "name is null.");
        return super.put(name, value);
    }

    public Object removeAttribute(String name) {
        Objects.requireNonNull(name, "name is null.");
        return super.remove(name);
    }

    public Map<String, Object> getAttributes() {
        return super.map();
    }
}
