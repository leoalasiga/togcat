package com.als.togcat.engine;

import com.als.togcat.engine.support.Attributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionContext;

import java.util.Enumeration;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:11
 */
public class HttpSessionImpl implements HttpSession {
    final ServletContextImpl servletContext;
    String sessionId;
    int maxInactiveInterval;
    long creationTime;
    long lastAccessedTime;
    Attributes attributes;

    public HttpSessionImpl(ServletContextImpl servletContext, String sessionId, int interval) {
        this.servletContext = servletContext;
        this.sessionId = sessionId;
        this.creationTime = this.lastAccessedTime = System.currentTimeMillis();
        this.attributes = new Attributes(true);
        setMaxInactiveInterval(interval);
    }



    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return this.sessionId;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        checkValid();
        return this.attributes.getAttribute(s);    }

    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return this.attributes.getAttributeNames();    }

    @Override
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public void setAttribute(String s, Object o) {
        checkValid();
        if (s == null) {
            removeAttribute(s);
        } else {
            this.attributes.setAttribute(s, o);
        }
    }

    @Override
    public void putValue(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {
        checkValid();
        this.attributes.removeAttribute(s);
    }

    @Override
    public void removeValue(String s) {

    }

    @Override
    public void invalidate() {
        checkValid();
        this.servletContext.sessionManager.remove(this);
        this.sessionId = null;
    }

    @Override
    public boolean isNew() {
        return this.creationTime == this.lastAccessedTime;
    }


    void checkValid() {
        if (this.sessionId == null) {
            throw new IllegalStateException("Session is already invalidated.");
        }
    }
}
