package com.als.togcat.engine;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/1 下午2:13
 */
public class FilterChainImpl implements FilterChain {
    final Filter[] filters;
    final Servlet servlet;
    final int total;
    int index = 0;

    public FilterChainImpl(Filter[] filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
        this.total = filters.length;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (index < total) {
            int current = index;
            index++;
            filters[current].doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }
}
