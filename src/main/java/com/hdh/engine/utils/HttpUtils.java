package com.hdh.engine.utils;

import com.sun.net.httpserver.Headers;
import jakarta.servlet.http.Cookie;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class HttpUtils {

    static final Pattern QUERY_SPLIT = Pattern.compile("&");

    /**
     * 解析查询参数
     * @param query 查询参数
     * @param charset 字符集
     * @return 参数Map
     */
    public static Map<String, List<String>> parseQuery(String query, Charset charset) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        String[] ss = QUERY_SPLIT.split(query);
        Map<String, List<String>> map = new HashMap<>();
        for (String s : ss) {
            int n = s.indexOf('=');
            if (n >= 1) {
                String key = s.substring(0, n);
                String value = s.substring(n + 1);
                List<String> exist = map.computeIfAbsent(key, k -> new ArrayList<>(4));
                exist.add(URLDecoder.decode(value, charset));
            }
        }
        return map;
    }

    public static Map<String, List<String>> parseQuery(String query) {
        return parseQuery(query, StandardCharsets.UTF_8);
    }

    public static String getHeader(Headers headers, String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /**
     * 解析Cookie
     * @param cookieValue Cookie字符串
     *                    格式如：name1=value1; name2=value2
     * @return Cookie数组
     */
    public static Cookie[] parseCookies(String cookieValue) {
        if (cookieValue == null) {
            return null;
        }
        cookieValue = cookieValue.strip();
        if (cookieValue.isEmpty()) {
            return null;
        }
        String[] ss = cookieValue.split(";");
        Cookie[] cookies = new Cookie[ss.length];
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i].strip();
            int pos = s.indexOf('=');
            String name = s;
            String value = "";
            if (pos >= 0) {
                name = s.substring(0, pos);
                value = s.substring(pos + 1);
            }
            cookies[i] = new Cookie(name, value);
        }
        return cookies;
    }
}
