package com.hdh.engine.support;

import com.hdh.connector.HttpExchangeRequest;
import com.hdh.engine.utils.HttpUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.*;

public class Parameters {

    final HttpExchangeRequest exchangeRequest;
    Charset charset;
    Map<String, String[]> parameters;

    public Parameters(HttpExchangeRequest exchangeRequest, String charset) {
        this.exchangeRequest = exchangeRequest;
        this.charset = Charset.forName(charset);
    }

    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
    }

    public String getParameter(String name) {
        String[] values = getParameterValues(name);
        if (values == null) {
            return null;
        }
        return values[0];
    }

    /**
     * 获取所有参数名
     * @return 参数名枚举
     */
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(getParameterMap().keySet());
    }

    public String[] getParameterValues(String name) {
        return getParameterMap().get(name);
    }

    /**
     * 获取所有参数
     * @return 参数Map
     */
    public Map<String, String[]> getParameterMap() {
        if (this.parameters == null) {
            // lazy init: 第一次调用时解析参数
            this.parameters = initParameters();
        }
        return this.parameters;
    }

    /**
     * 从query和post body中解析参数
     * @return 参数Map
     */
    Map<String, String[]> initParameters() {
        Map<String, List<String>> params = new HashMap<>();
        String query = this.exchangeRequest.getRequestURI().getRawQuery();
        if (query != null) {
            params = HttpUtils.parseQuery(query, charset);
        }
        if ("POST".equals(this.exchangeRequest.getRequestMethod())) {
            String value = HttpUtils.getHeader(this.exchangeRequest.getRequestHeaders(), "Content-Type");
            // 当Content-Type为表单提交时，解析body中的参数
            if (value != null && value.startsWith("application/x-www-form-urlencoded")) {
                String requestBody;
                try {
                    requestBody = new String(this.exchangeRequest.getRequestBody(), charset);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                Map<String, List<String>> postParams = HttpUtils.parseQuery(requestBody, charset);
                // 合并query和post参数
                for (String key : postParams.keySet()) {
                    List<String> postValues = postParams.get(key);
                    List<String> queryValues = params.get(key);
                    if (queryValues == null) {
                        params.put(key, postValues);
                    } else {
                        queryValues.addAll(postValues);
                    }
                }
            }
        }
        if (params.isEmpty()) {
            return Map.of();
        }
        // List<String> -> String[]
        Map<String, String[]> paramsMap = new HashMap<>();
        for (String key : params.keySet()) {
            List<String> values = params.get(key);
            paramsMap.put(key, values.toArray(String[]::new));
        }
        return paramsMap;
    }
}
