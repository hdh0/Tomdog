package com.hdh.engine;

import jakarta.servlet.*;

import java.io.IOException;

public class FilterChainImpl implements FilterChain {
    Filter[] filters;
    Servlet servlet;
    int total;
    int index = 0;

    public FilterChainImpl(Filter[] filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
        this.total = filters.length;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (index < total) {
            Filter filter = filters[index++];
            filter.doFilter(request, response, this);
        } else {
            // 最后一个Filter执行完毕后，执行Servlet
            servlet.service(request, response);
        }
    }
}
