package com.als.togcat.engine;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/30 上午11:07
 */
public class ServletRegistrationImpl implements ServletRegistration.Dynamic {

    final ServletContext servletContext;
    final String name;
    final Servlet servlet;
    final List<String> urlPatterns = new ArrayList<>();
    boolean initialized = false;

    public ServletRegistrationImpl(ServletContext servletContext, String name, Servlet servlet) {
        this.servletContext = servletContext;
        this.name = name;
        this.servlet = servlet;
    }

    public ServletConfig getServletConfig() {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return ServletRegistrationImpl.this.name;
            }

            @Override
            public ServletContext getServletContext() {
                return ServletRegistrationImpl.this.servletContext;
            }

            @Override
            public String getInitParameter(String s) {
                return null;
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return null;
            }
        };
    }

    @Override
    public void setLoadOnStartup(int i) {

    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement servletSecurityElement) {
        return null;
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfigElement) {

    }

    @Override
    public void setRunAsRole(String s) {

    }

    @Override
    public void setAsyncSupported(boolean b) {

    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException();
        }
        for (String urlPattern : urlPatterns) {
            this.urlPatterns.add(urlPattern);
        }
        return new HashSet<>();
    }

    @Override
    public Collection<String> getMappings() {
        return this.urlPatterns;
    }

    @Override
    public String getRunAsRole() {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String s, String s1) {
        return false;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> map) {
        return null;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return null;
    }
}
