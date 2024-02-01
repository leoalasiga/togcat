package com.als.togcat.engine.mapping;

import jakarta.servlet.Filter;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/1 上午9:36
 */
public class FilterMapping extends AbstractMapping {

    public final Filter filter;

    public FilterMapping(String urlPattern, Filter filter) {
        super(urlPattern);
        this.filter = filter;
    }
}
