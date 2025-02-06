package com.hdh.engine.mapping;

import jakarta.servlet.Servlet;

import java.util.regex.Pattern;

/**
 * ServletMapping
 * 保存servlet的映射关系
 */
public class ServletMapping extends AbstractMapping{
    public final Servlet servlet;

    public ServletMapping(String pattern, Servlet servlet) {
        super(pattern);
        this.servlet = servlet;
    }
}
