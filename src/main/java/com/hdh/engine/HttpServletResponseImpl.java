package com.hdh.engine;

import com.hdh.connector.HttpExchangeResponse;
import com.hdh.engine.support.HttpHeaders;
import com.sun.net.httpserver.Headers;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpServletResponseImpl implements HttpServletResponse {

    int status = 200;
    String contentType;

    int bufferSize = 1024;
    Boolean callOutput = null; // 是否调用了getOutputStream()方法
    ServletOutputStream output;
    PrintWriter writer;

    long contentLength = 0;
    List<Cookie> cookies = null;
    boolean committed = false;

    private final HttpExchangeResponse exchangeResponse;
    final HttpHeaders headers;

    public HttpServletResponseImpl(HttpExchangeResponse exchangeResponse) {
        this.exchangeResponse = exchangeResponse;
        this.headers = new HttpHeaders(exchangeResponse.getResponseHeaders());
        this.setContentType("text/html; charset=UTF-8");
    }

    /**
     * 设置响应头
     * @param name 响应头名称
     * @param value 响应头值
     */
    @Override
    public void setHeader(String name, String value) {
        checkNotCommitted();
        this.headers.setHeader(name, value);
    }

    /**
     * 设置响应内容类型
     * @param s 响应内容类型
     */
    @Override
    public void setContentType(String s) {
        this.contentType = s;
        this.setHeader("Content-Type", s);
    }

    /**
     * 提交响应头
     */
    void commitHeaders(long length) throws IOException {
        this.exchangeResponse.sendResponseHeaders(this.status, length);
        this.committed = true;
    }

    /**
     * 关闭 Writer 或 OutputStream
     */
    public void cleanup() throws IOException {
        if (this.callOutput != null) {
            if (this.callOutput) {
                this.output.close();
            } else {
                this.writer.close();
            }
        }
    }

    /**
     * 检查是否已提交响应
     */
    void checkNotCommitted() {
        if (this.committed) {
            throw new IllegalStateException("响应已经提交");
        }
    }

    /**
     * 获取打印流
     * @return 打印流
     * @throws IOException IO异常
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        if (callOutput == null){
            commitHeaders(0);
            this.writer = new PrintWriter(exchangeResponse.getResponseBody(), true, StandardCharsets.UTF_8);
            this.callOutput = false;
            return this.writer;
        }
        if(!callOutput){
            return this.writer;
        }
        throw new IllegalStateException("无法同时获取Writer和OutputStream");
    }

    /**
     * 添加 Cookie
     * @param cookie Cookie
     */
    @Override
    public void addCookie(Cookie cookie) {
        checkNotCommitted();
        if(this.cookies == null) {
            this.cookies = new ArrayList<>();
        } else {
            this.cookies.add(cookie);
        }
    }

    /**
     * 检查是否存在 Header
     * @param s Header 名称
     * @return 是否存在
     */
    @Override
    public boolean containsHeader(String s) {
        return this.headers.containsHeader(s);
    }

    @Override
    public String encodeURL(String s) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String s) {
        return null;
    }

    /**
     * 发送错误响应
     * @param sc 状态码
     * @param msg 错误消息
     * @throws IOException IO异常
     */
    @Override
    public void sendError(int sc, String msg) throws IOException {
        checkNotCommitted();
        this.status = sc;
        commitHeaders(-1);
    }

    /**
     * 发送错误响应
     * @param sc 状态码
     * @throws IOException IO异常
     */
    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, "Error");
    }

    /**
     * 发送重定向
     * @param url 重定向 URL
     * @throws IOException IO异常
     */
    @Override
    public void sendRedirect(String url) throws IOException {
        checkNotCommitted();
        this.status = 302;
        this.headers.setHeader("Location", url);
        commitHeaders(-1);
    }

    /**
     * 设置日期类型的响应头
     * @param name 响应头名称
     * @param date 日期
     *
     */
    @Override
    public void setDateHeader(String name, long date) {
        checkNotCommitted();
        this.headers.setDateHeader(name, date);
    }

    /**
     * 添加日期类型的响应头
     * @param name 响应头名称
     * @param date 日期
     *
     */
    @Override
    public void addDateHeader(String name, long date) {
        checkNotCommitted();
        this.headers.addDateHeader(name, date);
    }

    /**
     * 设置响应头
     * @param s 响应头名称
     * @param s1 响应头值
     */
    @Override
    public void addHeader(String s, String s1) {
        checkNotCommitted();
        this.headers.addHeader(s, s1);
    }

    /**
     * 设置整数类型的响应头
     * @param s 响应头名称
     * @param i 响应头值
     *
     */
    @Override
    public void setIntHeader(String s, int i) {
        checkNotCommitted();
        this.headers.setIntHeader(s, i);
    }

    /**
     * 添加整数类型的响应头
     * @param s 响应头名称
     * @param i 响应头值
     *
     */
    @Override
    public void addIntHeader(String s, int i) {
        checkNotCommitted();
        this.headers.addIntHeader(s, i);
    }

    /**
     * 设置响应状态码
     * @param i 状态码
     */
    @Override
    public void setStatus(int i) {
        checkNotCommitted();
        this.status = i;
    }

    /**
     * 获取响应状态码
     * @return 状态码
     */
    @Override
    public int getStatus() {
        return this.status;
    }

    /**
     * 获取响应头值
     * @param s 响应头名称
     * @return 响应头值
     */
    @Override
    public String getHeader(String s) {
        return this.headers.getHeader(s);
    }

    /**
     * 获取响应头值集合
     * @param s 响应头名称
     * @return 响应头值集合
     */
    @Override
    public Collection<String> getHeaders(String s) {
        List<String> hs = this.headers.getHeaders(s);
        if (hs == null) {
            return List.of();
        }
        return hs;
    }

    /**
     * 获取响应头名称集合
     * @return 响应头名称集合
     */
    @Override
    public Collection<String> getHeaderNames() {
        return Collections.unmodifiableCollection(this.headers.getHeaderNames());
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    /**
     * 获取内容类型
     * @return 内容类型
     */
    @Override
    public String getContentType() {
        return this.contentType;
    }

    /**
     * 获取输出流
     * @return 输出流
     * @throws IOException IO异常
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (callOutput == null){
            commitHeaders(0);
            this.output = new ServletOutputStreamImpl(this.exchangeResponse.getResponseBody());
            this.callOutput = true;
            return this.output;
        }
        if (callOutput){
            return this.output;
        }
        throw new IllegalStateException("无法同时获取Writer和OutputStream");
    }

    @Override
    public void setCharacterEncoding(String s) {

    }

    /**
     * 设置内容长度
     * @param i 内容长度
     */
    @Override
    public void setContentLength(int i) {
        this.contentLength = i;
    }

    /**
     * 设置内容长度
     */
    @Override
    public void setContentLengthLong(long l) {
        this.contentLength = l;
    }


    /**
     * 设置缓冲区大小
     * @param size 缓冲区大小
     */
    @Override
    public void setBufferSize(int size) {
        if (this.callOutput != null) {
            throw new IllegalStateException("已经获取了Writer或OutputStream");
        }
        if(size <= 0){
            throw new IllegalArgumentException("缓冲区大小必须大于0");
        }
        this.bufferSize = size;
    }

    /**
     * 获取缓冲区大小
     * @return 缓冲区大小
     */
    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    /**
     * 刷新缓冲区
     * @throws IOException IO异常
     */
    @Override
    public void flushBuffer() throws IOException {
        if(this.callOutput == null){
            throw new IllegalStateException("未获取Writer或OutputStream");
        }
        if(this.callOutput){
            this.output.flush();
        } else {
            this.writer.flush();
        }
    }

    /**
     * 清空缓冲区
     */
    @Override
    public void resetBuffer() {
        checkNotCommitted();
    }

    /**
     * 判断是否已提交响应
     */
    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    /**
     * 重置响应
     */
    @Override
    public void reset() {
        checkNotCommitted();
        this.status = 200;
        this.headers.clearHeaders();
    }

    @Override
    public void setLocale(Locale locale) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}
