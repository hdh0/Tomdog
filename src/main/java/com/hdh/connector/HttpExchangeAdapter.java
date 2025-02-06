package com.hdh.connector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;


/**
 * HttpExchangeAdapter 适配器
 */
public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse {

    private final HttpExchange exchange;
    byte[] requestBodyData;

    public HttpExchangeAdapter(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public String getRequestMethod() {
        return exchange.getRequestMethod();
    }

    @Override
    public URI getRequestURI() {
        return exchange.getRequestURI();
    }

    @Override
    public Headers getRequestHeaders() {
        return exchange.getRequestHeaders();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return exchange.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return exchange.getLocalAddress();
    }

    @Override
    public byte[] getRequestBody() throws IOException {
        if(this.requestBodyData == null) {
            try (InputStream is = this.exchange.getRequestBody()){
                this.requestBodyData = is.readAllBytes();
            }
        }
        return this.requestBodyData;
    }

    @Override
    public Headers getResponseHeaders() {
        return exchange.getResponseHeaders();
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        exchange.sendResponseHeaders(rCode, responseLength);
    }

    @Override
    public OutputStream getResponseBody() {
        return exchange.getResponseBody();
    }
}
