package com.hdh.connector;

import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HttpExchangeResponse 接口 用于设置响应头和响应体
 */
public interface HttpExchangeResponse{
    // 设置响应头
    Headers getResponseHeaders();
    void sendResponseHeaders(int rCode, long responseLength) throws IOException;
    OutputStream getResponseBody();
}