package com.hdh.connector;

import com.hdh.engine.HttpServletRequestImpl;
import com.hdh.engine.HttpServletResponseImpl;
import com.hdh.engine.ServletContextImpl;
import com.hdh.engine.filter.LogFilter;
import com.hdh.engine.servlet.IndexServlet;
import com.hdh.engine.servlet.LoginServlet;
import com.hdh.engine.servlet.LogoutServlet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class HttpConnector implements HttpHandler, AutoCloseable{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final HttpServer httpServer;
    private final ServletContextImpl servletContext;
    private final String host;
    private final int port;

    public HttpConnector(String host, int port) throws IOException {
        // 1. 创建Servlet容器
        this.servletContext = new ServletContextImpl();
        // 2. 初始化Servlet
        this.servletContext.initServlets(List.of(IndexServlet.class, LoginServlet.class, LogoutServlet.class));
        // 3. 初始化Filter
        this.servletContext.initFilters(List.of(LogFilter.class));

        this.host = host;
        this.port = port;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.start();
        logger.info("Tomdog Server started at {}:{}", host, port);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var adapter = new HttpExchangeAdapter(exchange); // 多态写法,使用var可以转成2个接口
        HttpServletResponse response = new HttpServletResponseImpl(adapter);
        HttpServletRequest request = new HttpServletRequestImpl(this.servletContext, adapter, response);
        // 使用Servlet容器处理请求
        try {
            this.servletContext.process(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws Exception {
        httpServer.stop(3);
    }
}
