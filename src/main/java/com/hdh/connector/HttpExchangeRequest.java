package com.hdh.connector;

import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * HttpExchangeRequest 接口 用于获取请求方法和请求URI
 */
public interface HttpExchangeRequest{
    String getRequestMethod();
    URI getRequestURI();
    Headers getRequestHeaders();
    InetSocketAddress getRemoteAddress();
    InetSocketAddress getLocalAddress();
    byte[] getRequestBody() throws IOException;
}