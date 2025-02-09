package com.hdh.engine;

import com.hdh.connector.HttpExchangeRequest;
import com.hdh.engine.support.HttpHeaders;
import com.hdh.engine.support.Parameters;
import com.hdh.engine.utils.HttpUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;


/**
 * HttpServletRequest 接口实现类
 */
public class HttpServletRequestImpl implements HttpServletRequest {
    final ServletContextImpl servletContext;
    final HttpExchangeRequest exchangeRequest;
    final HttpServletResponse response;
    final HttpHeaders headers; // 请求头
    final Parameters parameters; // 请求参数

    Boolean inputCalled = null; // 是否调用过getInputStream()方法


    public HttpServletRequestImpl(ServletContextImpl servletContext, HttpExchangeRequest exchangeRequest, HttpServletResponse response) {
        this.exchangeRequest = exchangeRequest;
        this.servletContext = servletContext;
        this.response = response;
        this.headers = new HttpHeaders(exchangeRequest.getRequestHeaders());
        this.parameters = new Parameters(exchangeRequest, "UTF-8");
    }

    /**
     * 获取请求参数
     * @param s 参数名
     */
    @Override
    public String getParameter(String s) {
        return this.parameters.getParameter(s);
    }

    @Override
    public String getAuthType() {
        return null;
    }

    /**
     * 获取请求头中的Cookie
     * @return Cookie数组
     */
    @Override
    public Cookie[] getCookies() {
        String cookieValue = this.getHeader("Cookie");
        return HttpUtils.parseCookies(cookieValue);
    }

    // =================== Header相关操作 ===================

    /**
     * 获取请求头, Date类型的值
     * @param s 请求头名称
     * @return long类型的值
     */
    @Override
    public long getDateHeader(String s) {
        return this.headers.getDateHeader(s);
    }

    /**
     * 获取请求头值
     * @param s 请求头名称
     * @return 请求头值
     */
    @Override
    public String getHeader(String s) {
        return this.headers.getHeader(s);
    }

    /**
     * 获取请求头值, 多个
     * @param s 请求头名称
     * @return 请求头值
     */
    @Override
    public Enumeration<String> getHeaders(String s) {
        List<String> hs = this.headers.getHeaders(s);
        if (hs != null) {
            return Collections.enumeration(hs);
        }
        return Collections.emptyEnumeration();
    }

    /**
     * 获取所有请求头名称
     * @return 请求头名称枚举
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.getHeaderNames());
    }

    /**
     * 获取请求头值, int类型
     * @param s 请求头名称
     * @return int类型的值
     */
    @Override
    public int getIntHeader(String s) {
        return this.headers.getIntHeader(s);
    }

    /**
     * 获取请求方法
     */
    @Override
    public String getMethod() {
        return this.exchangeRequest.getRequestMethod();
    }

    @Override
    public String getPathInfo() {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    /**
     * 获取请求URI
     */
    @Override
    public String getRequestURI() {
        return this.exchangeRequest.getRequestURI().getPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        // 从Cookie中获取SessionId
        String sessionId = null;
        Cookie[] cookies = this.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("JSESSIONID")) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }
        if (sessionId == null && !create) {
            return null;
        }
        if (sessionId == null) {
            if (this.response.isCommitted()){
                throw new IllegalStateException("无法创建Session, 因为响应已经提交");
            }
            sessionId = UUID.randomUUID().toString();
            // 设置SessionId到Cookie
            String cookieValue = String.format("JSESSIONID=%s; Path=/;", sessionId);
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
        throw new UnsupportedOperationException("不支持修改SessionId");
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
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

    }

    @Override
    public int getContentLength() {
        return 0;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    /**
     * 获取请求体输入流
     * @return ServletInputStream
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(this.inputCalled == null){
            this.inputCalled = true;
            return new ServletInputStreamImpl(this.exchangeRequest.getRequestBody());
        }
        throw new IllegalStateException("getInputStream()方法只能调用一次");
    }

    /**
     * 获取请求参数名称
     * @return 请求参数名称枚举
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return this.parameters.getParameterNames();
    }

    /**
     * 获取请求参数值
     * @param s 参数名
     * @return 参数值数组
     */
    @Override
    public String[] getParameterValues(String s) {
        return this.parameters.getParameterValues(s);
    }

    /**
     * 获取请求参数Map
     * @return 请求参数Map
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameters.getParameterMap();
    }

    @Override
    public String getProtocol() {
        return null;
    }

    @Override
    public String getScheme() {
        return null;
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    /**
     * 获取请求BufferedReader
     * @return BufferedReader
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (this.inputCalled == null) {
            this.inputCalled = false;
            ByteArrayInputStream bis = new ByteArrayInputStream(this.exchangeRequest.getRequestBody());
            return new BufferedReader(new InputStreamReader(bis, StandardCharsets.UTF_8));
        }
        throw new IllegalStateException("getReader()方法只能调用一次");
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
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
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
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
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    @Override
    public String getRequestId() {
        return null;
    }

    @Override
    public String getProtocolRequestId() {
        return null;
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }
}
