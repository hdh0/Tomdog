package com.hdh.engine;

import com.hdh.engine.support.Attributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import java.util.Enumeration;

public class HttpSessionImpl implements HttpSession{

    final ServletContextImpl servletContext;

    String sessionId;
    int maxInactiveInterval; // 会话最大存活时间
    long creationTime; // 创建时间
    long lastAccessedTime; // 最后访问时间
    Attributes attributes; // 会话属性

    public HttpSessionImpl(ServletContextImpl servletContext, String sessionId, int interval) {
        this.servletContext = servletContext;
        this.sessionId = sessionId;
        this.creationTime = this.lastAccessedTime = System.currentTimeMillis();
        this.attributes = new Attributes(true);
        setMaxInactiveInterval(interval);
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
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
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public Object getAttribute(String name) {
        checkValid();
        return this.attributes.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return this.attributes.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object value) {
        checkValid();
        if(value == null) {
            removeAttribute(name);
        } else {
            this.attributes.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        checkValid();
        this.attributes.removeAttribute(name);
    }

    /**
     * 使会话失效
     */
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

    private void checkValid() {
        if (this.sessionId == null) {
            throw new IllegalStateException("Session 已过期");
        }
    }
}
