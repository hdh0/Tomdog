package com.hdh.engine;

import com.hdh.connector.HttpExchangeRequest;
import com.hdh.engine.support.Attributes;
import com.hdh.engine.support.HttpHeaders;
import com.hdh.engine.support.Parameters;
import com.hdh.engine.utils.HttpUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.InetSocketAddress;
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

    final String method; // 请求方法
    final HttpHeaders headers; // 请求头
    final Parameters parameters; // 请求参数

    String characterEncoding = "UTF-8"; // 字符编码
    int contentLength = 0; // 请求体长度

    String requestId; // 请求ID
    Attributes attributes = new Attributes(); // 请求属性

    Boolean inputCalled = null; // 是否调用过getInputStream()方法


    public HttpServletRequestImpl(ServletContextImpl servletContext, HttpExchangeRequest exchangeRequest, HttpServletResponse response) {
        this.exchangeRequest = exchangeRequest;
        this.servletContext = servletContext;
        this.response = response;

        this.method = exchangeRequest.getRequestMethod();
        this.headers = new HttpHeaders(exchangeRequest.getRequestHeaders());
        this.parameters = new Parameters(exchangeRequest, this.characterEncoding);

        if (List.of("GET", "POST", "PUT", "DELETE").contains(this.method)) {
            this.contentLength = this.getIntHeader("Content-Length");
        }
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

    /**
     * 获取实际路径
     */
    @Override
    public String getPathTranslated() {
        return this.servletContext.getRealPath(this.getRequestURI());
    }

    @Override
    public String getContextPath() {
        return "";
    }

    /**
     * 获取查询字符串
     */
    @Override
    public String getQueryString() {
        return this.exchangeRequest.getRequestURI().getRawQuery();
    }

    @Override
    public String getRemoteUser() {
        throw new UnsupportedOperationException("不支持认证");
    }

    @Override
    public boolean isUserInRole(String s) {
        throw new UnsupportedOperationException("不支持认证");
    }

    @Override
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException("不支持认证");
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

    /**
     * 获取完整请求URL
     */
    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        url.append(this.getScheme())
                .append("://")
                .append(this.getServerName())
                .append(":")
                .append(this.getServerPort())
                .append(this.getRequestURI());
        return url;
    }

    @Override
    public String getServletPath() {
        return this.getRequestURI();
    }

    /**
     * 获取会话
     * @param create 是否创建
     */
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
        return this.getSession(true);
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
        throw new UnsupportedOperationException("不支持认证");
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        throw new UnsupportedOperationException("不支持认证");
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("不支持认证");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("不支持获取分块数据");
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        throw new UnsupportedOperationException("不支持获取分块数据");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        throw new UnsupportedOperationException("不支持升级协议(websocket)");
    }

    /**
     * 获取请求属性
     * @param name 属性名
     * @return 属性值
     */
    @Override
    public Object getAttribute(String name) {
        return this.attributes.getAttribute(name);
    }

    /**
     * 获取请求属性名称
     * @return 属性名称枚举
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes.getAttributeNames();
    }

    /**
     * 获取字符编码
     */
    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    /**
     * 设置字符编码
     */
    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        this.characterEncoding = s;
    }

    /**
     * 获取ContentLength
     */
    @Override
    public int getContentLength() {
        return this.contentLength;
    }

    /**
     * 获取ContentLength
     */
    @Override
    public long getContentLengthLong() {
        return this.contentLength;
    }

    /**
     * 获取ContentType
     */
    @Override
    public String getContentType() {
        return this.getHeader("Content-Type");
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

    /**
     * 获取协议
     */
    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    /**
     * 获取协议
     */
    @Override
    public String getScheme() {
        return "http";
    }

    /**
     * 获取服务器名称
     */
    @Override
    public String getServerName() {
        return "localhost";
    }

    /**
     * 获取服务器端口
     */
    @Override
    public int getServerPort() {
        InetSocketAddress address = this.exchangeRequest.getLocalAddress();
        return address.getPort();
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

    /**
     * 获取远程地址
     */
    @Override
    public String getRemoteAddr() {
        InetSocketAddress address = this.exchangeRequest.getRemoteAddress();
        return address.getHostString();
    }

    /**
     * 获取远程主机
     */
    @Override
    public String getRemoteHost() {
        return this.getRemoteAddr();
    }

    /**
     * 设置属性
     * @param name 属性名
     * @param value 属性值
     */
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

    /**
     * 移除属性
     * @param name 属性名
     */
    @Override
    public void removeAttribute(String name) {
        Object oldValue = this.attributes.removeAttribute(name);
        this.servletContext.invokeServletRequestAttributeRemoved(this, name, oldValue);
    }

    /**
     * 获取区域
     */
    @Override
    public Locale getLocale() {
        return Locale.CHINA;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(Locale.CHINA, Locale.US));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        throw new UnsupportedOperationException("不支持getRequestDispatcher");
    }

    /**
     * 获取远程端口
     */
    @Override
    public int getRemotePort() {
        InetSocketAddress address = this.exchangeRequest.getRemoteAddress();
        return address.getPort();
    }

    /**
     * 获取本地名称
     */
    @Override
    public String getLocalName() {
        return this.getLocalAddr();
    }

    /**
     * 获取本地地址
     */
    @Override
    public String getLocalAddr() {
        InetSocketAddress address = this.exchangeRequest.getLocalAddress();
        return address.getHostString();
    }

    /**
     * 获取本地端口
     */
    @Override
    public int getLocalPort() {
        InetSocketAddress address = this.exchangeRequest.getLocalAddress();
        return address.getPort();
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("不支持异步请求");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new IllegalStateException("不支持异步请求");
    }

    @Override
    public boolean isAsyncStarted() {
        throw new IllegalStateException("不支持异步请求");
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("不支持异步请求");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    /**
     * 获取请求ID
     */
    @Override
    public String getRequestId() {
        if (this.requestId == null) {
            this.requestId = UUID.randomUUID().toString();
        }
        return this.requestId;
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        throw new UnsupportedOperationException("不支持获取ServletConnection");
    }
}
