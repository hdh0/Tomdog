package com.hdh;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class SimpleHttpServer implements HttpHandler, AutoCloseable {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private final HttpServer httpServer;
    private final String host;
    private final int port;

    public SimpleHttpServer(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.start();
        logger.info("Tomdog Server started at {}:{}", host, port);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 获取请求
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String query = uri.getQuery();
        logger.info("{}: {}?{}", method, path, query);

        // 返回响应
        Headers headers = exchange.getResponseHeaders();
        // 设置响应头
        headers.set("Content-Type", "text/html; charset=UTF-8");
        headers.set("Cache-Control", "no-cache");
        // 设置响应体
        exchange.sendResponseHeaders(200, 0);
        String s = "<h1>Hello world!</h1><p>当前时间: " + LocalDateTime.now().withNano(0) + "</p>";

        try(OutputStream out = exchange.getResponseBody()){
            out.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void close() throws Exception {
        this.httpServer.stop(3);
    }

    public static void main(String[] args){
        String host = "0.0.0.0";
        int port = 8080;
        try(SimpleHttpServer connector = new SimpleHttpServer(host, port)){
            while (true){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
