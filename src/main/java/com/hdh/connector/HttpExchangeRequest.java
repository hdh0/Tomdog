package com.hdh.connector;

import java.net.URI;

/**
 * HttpExchangeRequest 接口 用于获取请求方法和请求URI
 */
public interface HttpExchangeRequest{
    String getRequestMethod();
    URI getRequestURI();
}