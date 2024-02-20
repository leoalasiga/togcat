package com.als.togcat.engine;

import com.als.togcat.Config;
import com.als.togcat.connector.HttpExchangeRequest;
import com.als.togcat.engine.support.Attributes;
import com.als.togcat.engine.support.HttpHeaders;
import com.als.togcat.engine.support.Parameters;
import com.als.togcat.utils.HttpUtils;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;

import static java.util.regex.Pattern.*;

/**
 * 编写HttpServletRequestImpl，它内部持有HttpServletRequest，并实现了HttpServletRequest接口
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:48
 */
public class HttpServletRequestImpl implements HttpServletRequest {
    final Config config;
    final ServletContextImpl servletContext;
    final HttpExchangeRequest httpExchangeRequest;
    final HttpServletResponse response;
    final String method;
    final HttpHeaders headers;
    final Parameters parameters;

    String characterEncoding;
    int contentLength = 0;

    //这个版本的api没有getRequestId方法
//    String requestId = null;

    Attributes attributes = new Attributes();


    Boolean inputCalled = null;


    public HttpServletRequestImpl(Config config,ServletContextImpl servletContext, HttpExchangeRequest httpExchangeRequest, HttpServletResponse response) {
        this.config = config;
        this.servletContext = servletContext;
        this.httpExchangeRequest = httpExchangeRequest;
        this.response = response;
//        this.characterEncoding = "UTF-8";
        this.characterEncoding = config.server.requestEncoding;
        this.headers = new HttpHeaders(httpExchangeRequest.getRequestHeaders());
        this.parameters = new Parameters(httpExchangeRequest, this.characterEncoding);
        this.method = httpExchangeRequest.getRequestMethod();
        if ("POST".equals(this.method) || "PUT".equals(this.method) || "DELETE".equals(this.method) || "PATCH".equals(this.method)) {
            this.contentLength = getIntHeader("Content-Length");
        }


    }

//    public HttpServletRequestImpl(HttpExchangeRequest httpExchangeRequest) {
//        this.httpExchangeRequest = httpExchangeRequest;
//    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        String cookieValue = this.getHeader("Cookie");
        return HttpUtils.parseCookies(cookieValue);

    }

    @Override
    public long getDateHeader(String s) {
        return this.headers.getDateHeader(s);
    }

    @Override
    public String getHeader(String s) {
        return this.headers.getHeader(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        List<String> hs = this.headers.getHeaders(s);
        if (hs == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(hs);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.getHeaderNames());
    }

    @Override
    public int getIntHeader(String s) {
        return this.headers.getIntHeader(s);
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return this.servletContext.getRealPath(getRequestURI());
    }

    @Override
    public String getContextPath() {
        // root context path:
        return "";
    }

    @Override
    public String getQueryString() {
        return this.httpExchangeRequest.getRequestURI().getRawQuery();
    }

    @Override
    public String getRemoteUser() {
        // not support auth:
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        // not support auth:
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // not support auth:
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return this.httpExchangeRequest.getRequestURI().getPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer(128);
        sb.append(getScheme()).append("://").append(getServerName()).append(':').append(getServerPort()).append(getRequestURI());
        return sb;
    }

    @Override
    public String getServletPath() {
        return getRequestURI();
    }

    @Override
    public HttpSession getSession(boolean b) {
        String sessionId = null;
        Cookie[] cookies = getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
//                if ("JSESSIONID".equals(cookie.getName())) {
                if (config.server.webApp.sessionCookieName.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }
        if (sessionId == null && !b) {
            return null;
        }
        if (sessionId == null) {
            if (this.response.isCommitted()) {
                throw new IllegalStateException("Cannot create session for response is commited.");
            }
            sessionId = UUID.randomUUID().toString();
            // set cookie:
//            String cookieValue = "JSESSIONID=" + sessionId + "; Path=/; SameSite=Strict; HttpOnly";
            String cookieValue = config.server.webApp.sessionCookieName + "=" + sessionId + "; Path=/; SameSite=Strict; HttpOnly";
            this.response.addHeader("Set-Cookie", cookieValue);
        }
        return this.servletContext.sessionManager.getSession(sessionId);
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        throw new UnsupportedOperationException("changeSessionId() is not supported.");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return true;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        // not support auth:
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        // not support auth:

    }

    @Override
    public void logout() throws ServletException {
        // not support auth:

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return new ArrayList<>();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        // not support multipart:
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        // not suport websocket:
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return this.attributes.getAttribute(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes.getAttributeNames();
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.characterEncoding = env;
        this.parameters.setCharset(env);
    }

    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return this.contentLength;
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (this.inputCalled == null) {
            this.inputCalled = Boolean.TRUE;
            return new ServletInputStreamImpl(this.httpExchangeRequest.getRequestBody());
        }
        throw new IllegalStateException("Cannot reopen input stream after " + (this.inputCalled ? "getInputStream()" : "getReader()") + " was called.");
    }

    @Override
    public String getParameter(String s) {
//        String rawQuery = this.httpExchangeRequest.getRequestURI().getRawQuery();
//        if (rawQuery != null) {
//            Map<String, String> map = null;
//            try {
//                map = parseQuery(rawQuery);
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            return map.get(s);
//        }
//        return null;
        return this.parameters.getParameter(s);
    }

    private Map<String, String> parseQuery(String rawQuery) throws UnsupportedEncodingException {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return new HashMap<>(8);
        }
        String[] ss = new String[0];
        try {
            ss = compile("\\&").split(rawQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, String> map = new HashMap<>(ss.length < 8 ? 8 : ss.length);
        for (String s : ss) {
            int n = s.indexOf('=');
            if (n >= 1) {
                String key = s.substring(0, n);
                String value = s.substring(n + 1);
                map.putIfAbsent(key, URLDecoder.decode(value, StandardCharsets.UTF_8.toString()));
            }
        }
        return map;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return this.parameters.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String s) {
        return this.parameters.getParameterValues(s);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameters.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        String header = "http";
        String forwarded = config.server.forwardedHeaders.forwardedProto;
        if (!forwarded.isEmpty()) {
            String forwardedHeader = getHeader(forwarded);
            if (forwardedHeader != null) {
                header = forwardedHeader;
            }
        }
        return header;
//        return "http";
    }

    @Override
    public String getServerName() {
        String header = getHeader("Host");
        String forwarded = config.server.forwardedHeaders.forwardedHost;
        if (!forwarded.isEmpty()) {
            String forwardedHeader = getHeader(forwarded);
            if (forwardedHeader != null) {
                header = forwardedHeader;
            }
        }
        if (header == null) {
            InetSocketAddress address = this.httpExchangeRequest.getLocalAddress();
            header = address.getHostString();
        }
        return header;
//        return "localhost";
    }

    @Override
    public int getServerPort() {
        InetSocketAddress address = this.httpExchangeRequest.getLocalAddress();
        return address.getPort();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (this.inputCalled == null) {
            this.inputCalled = Boolean.FALSE;
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.httpExchangeRequest.getRequestBody()), StandardCharsets.UTF_8));
        }
        throw new IllegalStateException("Cannot reopen input stream after " + (this.inputCalled ? "getInputStream()" : "getReader()") + " was called.");
    }

    @Override
    public String getRemoteAddr() {
        String addr = null;
        String forwarded = config.server.forwardedHeaders.forwardedFor;
        if (forwarded != null && !forwarded.isEmpty()) {
            String forwardedHeader = getHeader(forwarded);
            if (forwardedHeader != null) {
                int n = forwardedHeader.indexOf(',');
                addr = n < 0 ? forwardedHeader : forwardedHeader.substring(n);
            }
        }
        if (addr == null) {
            InetSocketAddress address = this.httpExchangeRequest.getRemoteAddress();
            addr = address.getHostString();
        }
        return addr;
//        InetSocketAddress address = this.httpExchangeRequest.getRemoteAddress();
//        return address.getHostString();
    }

    @Override
    public String getRemoteHost() {
        // avoid DNS lookup by IP:
        return getRemoteAddr();
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        } else {
            Object oldValue = this.attributes.setAttribute(name, value);
            if (oldValue == null) {
                this.servletContext.invokeServletRequestAttributeAdded(this, name, value);
            } else {
                this.servletContext.invokeServletRequestAttributeReplaced(this, name, value);
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object oldValue = this.attributes.removeAttribute(name);
        this.servletContext.invokeServletRequestAttributeRemoved(this, name, oldValue);
    }

    @Override
    public Locale getLocale() {
        return Locale.CHINA;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(Arrays.asList(Locale.CHINA, Locale.US));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public int getRemotePort() {
        InetSocketAddress address = this.httpExchangeRequest.getRemoteAddress();
        return address.getPort();
    }

    @Override
    public String getLocalName() {
        return getLocalAddr();
    }

    @Override
    public String getLocalAddr() {
        InetSocketAddress address = this.httpExchangeRequest.getLocalAddress();
        return address.getHostString();
    }

    @Override
    public int getLocalPort() {
        InetSocketAddress address = this.httpExchangeRequest.getLocalAddress();
        return address.getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Async is not supported.");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new IllegalStateException("Async is not supported.");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("Async is not supported.");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String toString() {
        return String.format("HttpServletRequestImpl@%s[%s:%s]", Integer.toHexString(hashCode()), getMethod(), getRequestURI());
    }
}
