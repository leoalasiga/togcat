package com.als.togcat.engine.mapping;

import jakarta.servlet.Servlet;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午5:13
 */
public class ServletMapping extends AbstractMapping {
    public final Servlet servlet;

    public ServletMapping(String urlPattern, Servlet servlet) {
        super(urlPattern);
        this.servlet = servlet;
    }
}
