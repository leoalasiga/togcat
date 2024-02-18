package com.als.togcat.engine;

import com.als.togcat.engine.mapping.FilterMapping;
import com.als.togcat.engine.mapping.ServletMapping;
import com.als.togcat.engine.support.Attributes;
import com.als.togcat.utils.AnnoUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/30 上午10:59
 */
public class ServletContextImpl implements ServletContext {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final SessionManager sessionManager = new SessionManager(this, 600);
    private Attributes attributes = new Attributes(true);
    private Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private Map<String, FilterRegistrationImpl> filterRegistrations = new HashMap<>();
    final Map<String, Servlet> nameToServlet = new HashMap<>();
    final Map<String, Filter> nameToFilters = new HashMap<>();
    // 添加servlet的匹配
    final List<ServletMapping> servletMappings = new ArrayList<>();
    // 添加filter的匹配
    final List<FilterMapping> filterMappings = new ArrayList<>();
    // 首先我们需要在ServletContextImpl中注册并管理所有的Listener，所以用不同的List持有注册的Listener：
    private List<ServletContextListener> servletContextListeners = null;
    private List<ServletContextAttributeListener> servletContextAttributeListeners = null;
    private List<ServletRequestListener> servletRequestListeners = null;
    private List<ServletRequestAttributeListener> servletRequestAttributeListeners = null;
    private List<HttpSessionAttributeListener> httpSessionAttributeListeners = null;
    private List<HttpSessionListener> httpSessionListeners = null;


    public void initFilters(List<Class<?>> filterClasses) {
        for (Class<?> c : filterClasses) {
            WebFilter wf = c.getAnnotation(WebFilter.class);
            if (wf != null) {
                logger.info("auto register @WebFilter: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Filter> clazz = (Class<? extends Filter>) c;
                FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, AnnoUtils.getFilterUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
            }
        }

        // init filters:
        for (String name : this.filterRegistrations.keySet()) {
            FilterRegistrationImpl registration = this.filterRegistrations.get(name);
            try {
                registration.filter.init(registration.getFilterConfig());
                this.nameToFilters.put(name, registration.filter);
                for (String urlPattern : registration.getUrlPatternMappings()) {
                    this.filterMappings.add(new FilterMapping(urlPattern, registration.filter));
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init filter failed: " + name + " / " + registration.filter.getClass().getName(), e);
            }
        }
    }

    public void initServlets(List<Class<?>> servletClasses) {
        for (Class<?> c : servletClasses) {
            WebServlet ws = c.getAnnotation(WebServlet.class);
            if (ws != null) {
                logger.info("auto register @WebServlet: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> clazz = (Class<? extends Servlet>) c;
                ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(clazz), clazz);
                registration.addMapping(AnnoUtils.getServletUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getServletInitParams(clazz));
            }
        }

        // init servlets:
        for (String name : this.servletRegistrations.keySet()) {
            ServletRegistrationImpl registration = this.servletRegistrations.get(name);

            try {
                registration.servlet.init(registration.getServletConfig());
                this.nameToServlet.put(name, registration.servlet);
                for (String urlPattern : registration.getMappings()) {
                    this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
                }
                registration.initialized = true;
            } catch (ServletException e) {
                logger.error("init servlet failed: " + name + " / " + registration.servlet.getClass().getName(), e);
            }
        }

        // important: sort mappings:
        Collections.sort(this.servletMappings);
    }


    // HTTP请求处理入口:
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 请求路径:
        String path = request.getRequestURI();
        // 搜索Servlet:
        Servlet servlet = null;
        for (ServletMapping servletMapping : this.servletMappings) {
            if (servletMapping.matches(path)) {
                servlet = servletMapping.servlet;
                break;
            }
        }

        if (servlet == null) {
            // 未匹配到任何Servlet显示404 Not Found:
            try (PrintWriter pw = response.getWriter()) {
                pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + path + "</p>");
                return;
            }

        }

        // 搜索filter:
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matches(path)) {
                enabledFilters.add(mapping.filter);
            }
        }

        Filter[] filters = enabledFilters.toArray(new Filter[0]);
        logger.atDebug().log("process {} by filter {}, servlet {}", path, Arrays.toString(filters), servlet);
        FilterChain chain = new FilterChainImpl(filters, servlet);
        try {
            this.invokeServletRequestInitialized(request);
            chain.doFilter(request, response);
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
            throw new IOException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }finally {
            this.invokeServletRequestDestroyed(request);
        }

    }


    /**
     * version 1 :servlet处理，不需要filter
     */
    // HTTP请求处理入口:
//    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//        // 请求路径:
//        String path = request.getRequestURI();
//        // 搜索Servlet:
//        Servlet servlet = null;
//        for (ServletMapping servletMapping : this.servletMappings) {
//            if (servletMapping.matches(path)) {
//                servlet = servletMapping.servlet;
//                break;
//            }
//        }
//
//        if (servlet == null) {
//            // 未匹配到任何Servlet显示404 Not Found:
//            try (PrintWriter pw = response.getWriter()) {
//                pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + path + "</p>");
//                return;
//            }
//
//        }
//
//        // 由Servlet继续处理请求:
//        servlet.service(request, response);
//    }
    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public ServletContext getContext(String uripath) {
        if ("".equals(uripath)) {
            return this;
        }
        // all others are not exist:
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        String defaultMime = "application/octet-stream";
        Map<String, String> mimes = new HashMap<>();
        mimes.put(".html", "text/html");
        mimes.put(".txt", "text/plain");
        mimes.put(".png", "image/png");
        mimes.put(".jpg", "image/jpeg");
        int n = file.lastIndexOf('.');
        if (n == -1) {
            return defaultMime;
        }
        String ext = file.substring(n);
        return mimes.getOrDefault(ext, defaultMime);
    }

    @Override
    public Set<String> getResourcePaths(String s) {
        return null;
    }

    @Override
    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    @Override
    public Servlet getServlet(String s) throws ServletException {
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return null;
    }

    @Override
    public Enumeration<String> getServletNames() {
        return null;
    }

    @Override
    public void log(String s) {

    }

    @Override
    public void log(Exception e, String s) {

    }

    @Override
    public void log(String s, Throwable throwable) {

    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        throw new UnsupportedOperationException("setInitParameter");
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object val) {
        if (val == null) {
            removeAttribute(name);
        } else {
            Object old = this.attributes.getAttribute(name);
            if (old == null) {
                invokeServletContextAttributeAdded(name, val);
            } else {
                invokeServletContextAttributeReplaced(name, val);
            }
        }

    }

    @Override
    public void removeAttribute(String name) {
        Object old = this.attributes.removeAttribute(name);
        this.invokeServletContextAttributeRemoved(name, old);
    }

    @Override
    public String getServletContextName() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Servlet servlet = null;
        try {
            Class<? extends Servlet> clazz = createInstance(className);
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Servlet servlet) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("servlet is null.");
        }
        ServletRegistrationImpl registration = new ServletRegistrationImpl(this, name, servlet);
        this.servletRegistrations.put(name, registration);
        return registration;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Class<? extends Servlet> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Servlet servlet = null;
        try {
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String s, String s1) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        return createInstance(clazz);
    }

    @Override
    public ServletRegistration getServletRegistration(String name) {
        return this.servletRegistrations.get(name);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return this.servletRegistrations;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, String className) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("class name is null or empty.");
        }
        Filter filter = null;
        try {
            Class<? extends Filter> clazz = createInstance(className);
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Filter filter) {
        if (name == null) {
            throw new IllegalArgumentException("name is null.");
        }
        if (filter == null) {
            throw new IllegalArgumentException("filter is null.");
        }
        FilterRegistrationImpl registration = new FilterRegistrationImpl(this, name, filter);
        this.filterRegistrations.put(name, registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("class is null.");
        }
        Filter filter = null;
        try {
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return createInstance(clazz);
    }

    @Override
    public FilterRegistration getFilterRegistration(String name) {
        return this.filterRegistrations.get(name);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return this.filterRegistrations;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> set) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String className) {
        EventListener listener = null;
        try {
            Class<EventListener> clazz = createInstance(className);
            listener = createListener(clazz);
            addListener(listener);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 实现ServletContext的addListener()接口，用于注册Listener：
     *
     * @param t
     * @param <T>
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        // 根据Listener类型放入不同的List:
        if (t instanceof ServletContextListener) {
            if (servletContextListeners == null) {
                servletContextListeners = new ArrayList<>();
            }
            servletContextListeners.add((ServletContextListener) t);
        } else if (t instanceof ServletContextAttributeListener) {
            if (servletContextAttributeListeners == null) {
                servletContextAttributeListeners = new ArrayList<>();
            }
            servletContextAttributeListeners.add((ServletContextAttributeListener) t);
        } else if (t instanceof ServletRequestListener) {
            if (servletRequestListeners == null) {
                servletRequestListeners = new ArrayList<>();
            }
            servletRequestListeners.add((ServletRequestListener) t);
        } else if (t instanceof ServletRequestAttributeListener) {
            if (servletRequestAttributeListeners == null) {
                servletRequestAttributeListeners = new ArrayList<>();
            }
            servletRequestAttributeListeners.add((ServletRequestAttributeListener) t);
        } else if (t instanceof HttpSessionAttributeListener) {
            if (httpSessionAttributeListeners == null) {
                httpSessionAttributeListeners = new ArrayList<>();
            }
            httpSessionAttributeListeners.add((HttpSessionAttributeListener) t);
        } else if (t instanceof HttpSessionListener) {
            if (httpSessionListeners == null) {
                httpSessionListeners = new ArrayList<>();
            }
            httpSessionListeners.add((HttpSessionListener) t);
        } else {
            throw new IllegalArgumentException("Unsupported listener: " + t.getClass().getName());
        }
    }

    //invoke listeners ///////////////////////////////////////////////////////
    public void invokeServletContextAttributeAdded(String name, Object val) {
        logger.info("invoke ServletContextAttributeAdded: {} = {}", name, val);
        if (this.servletContextAttributeListeners != null) {
            ServletContextAttributeEvent servletContextAttributeEvent = new ServletContextAttributeEvent(this, name, val);
            for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
                servletContextAttributeListener.attributeAdded(servletContextAttributeEvent);
            }
        }
    }

    public void invokeServletContextAttributeRemoved(String name, Object val) {
        logger.info("invoke ServletContextAttributeRemoved: {} = {}", name, val);
        if (this.servletContextAttributeListeners != null) {
            ServletContextAttributeEvent servletContextAttributeEvent = new ServletContextAttributeEvent(this, name, val);
            for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
                servletContextAttributeListener.attributeRemoved(servletContextAttributeEvent);
            }
        }
    }

    public void invokeServletContextAttributeReplaced(String name, Object val) {
        logger.info("invoke ServletContextAttributeReplaced: {} = {}", name, val);
        if (this.servletContextAttributeListeners != null) {
            ServletContextAttributeEvent servletContextAttributeEvent = new ServletContextAttributeEvent(this, name, val);
            for (ServletContextAttributeListener servletContextAttributeListener : servletContextAttributeListeners) {
                servletContextAttributeListener.attributeReplaced(servletContextAttributeEvent);
            }
        }
    }

    public void invokeServletRequestAttributeRemoved(HttpServletRequest request, String name, Object val) {
        logger.info("invoke ServletRequestAttributeRemoved: {} = {}", name, val);
        if (this.servletRequestAttributeListeners != null) {
            ServletRequestAttributeEvent servletRequestAttributeEvent = new ServletRequestAttributeEvent(this, request, name, val);
            for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
                servletRequestAttributeListener.attributeRemoved(servletRequestAttributeEvent);
            }
        }
    }

    public void invokeServletRequestAttributeReplaced(HttpServletRequest request, String name, Object val) {
        logger.info("invoke ServletRequestAttributeReplaced: {} = {}", name, val);
        if (this.servletRequestAttributeListeners != null) {
            ServletRequestAttributeEvent servletRequestAttributeEvent = new ServletRequestAttributeEvent(this, request, name, val);
            for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
                servletRequestAttributeListener.attributeReplaced(servletRequestAttributeEvent);
            }
        }
    }

    public void invokeServletRequestAttributeAdded(HttpServletRequest request, String name, Object val) {
        logger.info("invoke ServletRequestAttributeReplaced: {} = {}", name, val);
        if (this.servletRequestAttributeListeners != null) {
            ServletRequestAttributeEvent servletRequestAttributeEvent = new ServletRequestAttributeEvent(this, request, name, val);
            for (ServletRequestAttributeListener servletRequestAttributeListener : servletRequestAttributeListeners) {
                servletRequestAttributeListener.attributeAdded(servletRequestAttributeEvent);
            }
        }
    }

    public void invokeHttpSessionAttributeAdded(HttpSession session, String name, Object val) {
        logger.info("invoke HttpSessionAttributeAdded: {} = {}", name, val);
        if (this.httpSessionAttributeListeners != null) {
            HttpSessionBindingEvent httpSessionBindingEvent = new HttpSessionBindingEvent(session, name, val);
            for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
                httpSessionAttributeListener.attributeAdded(httpSessionBindingEvent);
            }
        }
    }

    public void invokeHttpSessionAttributeRemoved(HttpSession session, String name, Object val) {
        logger.info("invoke HttpSessionAttributeRemoved: {} = {}", name, val);
        if (this.httpSessionAttributeListeners != null) {
            HttpSessionBindingEvent httpSessionBindingEvent = new HttpSessionBindingEvent(session, name, val);
            for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
                httpSessionAttributeListener.attributeRemoved(httpSessionBindingEvent);
            }
        }
    }

    public void invokeHttpSessionAttributeReplaced(HttpSession session, String name, Object val) {
        logger.info("invoke HttpSessionAttributeReplaced: {} = {}", name, val);
        if (this.httpSessionAttributeListeners != null) {
            HttpSessionBindingEvent httpSessionBindingEvent = new HttpSessionBindingEvent(session, name, val);
            for (HttpSessionAttributeListener httpSessionAttributeListener : httpSessionAttributeListeners) {
                httpSessionAttributeListener.attributeReplaced(httpSessionBindingEvent);
            }
        }
    }

    public void invokeServletRequestInitialized(HttpServletRequest request) {
        logger.info("invoke ServletRequestInitialized: request = {}", request);
        if (this.servletRequestListeners != null) {
            ServletRequestEvent servletRequestEvent = new ServletRequestEvent(this, request);
            for (ServletRequestListener servletRequestListener : servletRequestListeners) {
                servletRequestListener.requestInitialized(servletRequestEvent);
            }
        }
    }

    public void invokeServletRequestDestroyed(HttpServletRequest request) {
        logger.info("invoke ServletRequestDestroyed(: request = {}", request);
        if (this.servletRequestListeners != null) {
            ServletRequestEvent servletRequestEvent = new ServletRequestEvent(this, request);
            for (ServletRequestListener servletRequestListener : servletRequestListeners) {
                servletRequestListener.requestDestroyed(servletRequestEvent);
            }
        }
    }


    public void invokeHttpSessionCreated(HttpSession httpSession) {
        logger.info("invoke HttpSessionCreated(: request = {}", httpSession);
        if (this.httpSessionListeners != null) {
            HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSession);
            for (HttpSessionListener httpSessionListener : httpSessionListeners) {
                httpSessionListener.sessionCreated(httpSessionEvent);
            }
        }
    }

    public void invokeHttpSessionDestroyed(HttpSession httpSession) {
        logger.info("invoke HttpSession Destroyed(: request = {}", httpSession);
        if (this.httpSessionListeners != null) {
            HttpSessionEvent httpSessionEvent = new HttpSessionEvent(httpSession);
            for (HttpSessionListener httpSessionListener : httpSessionListeners) {
                httpSessionListener.sessionDestroyed(httpSessionEvent);
            }
        }
    }
    // invoke listeners ///////////////////////////////////////////////////////




    @Override
    public void addListener(Class<? extends EventListener> aClass) {
        EventListener listener = null;
        try {
            listener = createInstance(aClass);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        addListener(listener);
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... strings) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return this.sessionManager.inactiveInterval;
    }

    @Override
    public void setSessionTimeout(int i) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String s) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String s) {

    }


    @SuppressWarnings("unchecked")
    private <T> T createInstance(String className) throws ServletException {
        Class<T> clazz;
        try {
            clazz = (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found.", e);
        }
        return createInstance(clazz);
    }

    private <T> T createInstance(Class<T> clazz) throws ServletException {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("Cannot instantiate class " + clazz.getName(), e);
        }
    }
}
