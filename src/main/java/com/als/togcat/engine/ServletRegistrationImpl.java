package com.als.togcat.engine;

import com.als.togcat.engine.support.InitParameters;
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
 * ServletRegistration 接口是Java Servlet规范中的一部分，用于在Servlet 3.0及以上版本中，以编程方式配置Servlet。它提供了一种方式，让开发者在代码中注册Servlet并配置相关的初始化参数、映射路径等信息。
 * 主要的实现类是ServletRegistration.Dynamic接口，它扩展自ServletRegistration。通过Dynamic接口，开发者可以动态地添加、配置和映射Servlet。
 * 通过使用ServletRegistration和ServletRegistration.Dynamic接口，开发者可以在Servlet容器启动时通过代码动态注册Servlet，并设置其属性和映射规则，而不是通过web.xml文件进行配置。
 * 这种动态注册的方式对于一些灵活性要求较高的场景非常有用。
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/30 上午11:07
 */
public class ServletRegistrationImpl implements ServletRegistration.Dynamic {

    final ServletContext servletContext;
    final String name;
    final Servlet servlet;
    final List<String> urlPatterns = new ArrayList<>(4);
    final InitParameters initParameters = new InitParameters();
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
                return ServletRegistrationImpl.this.initParameters.getInitParameter(s);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return ServletRegistrationImpl.this.initParameters.getInitParameterNames();
            }
        };
    }

    @Override
    public void setLoadOnStartup(int i) {
        checkNotInitialized("setLoadOnStartup");

    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement servletSecurityElement) {
        checkNotInitialized("setServletSecurity");
        throw new UnsupportedOperationException("Servlet security is not supported.");    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfigElement) {
        checkNotInitialized("setMultipartConfig");
        throw new UnsupportedOperationException("Multipart config is not supported.");
    }

    @Override
    public void setRunAsRole(String roleName) {
        checkNotInitialized("setRunAsRole");
        if (roleName != null) {
            throw new UnsupportedOperationException("Role is not supported.");
        }
    }

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        checkNotInitialized("setAsyncSupported");
        if (isAsyncSupported) {
            throw new UnsupportedOperationException("Async is not supported.");
        }
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        checkNotInitialized("addMapping");
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
        checkNotInitialized("setInitParameter");
        return this.initParameters.setInitParameter(s,s1);
    }

    @Override
    public String getInitParameter(String s) {
        return this.initParameters.getInitParameter(s);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> map) {
        checkNotInitialized("setInitParameter");
        return this.initParameters.setInitParameters(map);
    }

    @Override
    public Map<String, String> getInitParameters() {
        return this.initParameters.getInitParameters();
    }

    private void checkNotInitialized(String name) {
        if (this.initialized) {
            throw new IllegalStateException("Cannot call " + name + " after initialization.");
        }
    }
}
