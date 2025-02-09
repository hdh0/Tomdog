package com.hdh.engine;

import com.hdh.engine.mapping.FilterMapping;
import com.hdh.engine.mapping.ServletMapping;
import com.hdh.engine.utils.AnnoUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ServletContextImpl implements ServletContext {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final SessionManager sessionManager = new SessionManager(this, 600); // 会话管理器 10分钟失效

    private Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private Map<String, FilterRegistrationImpl> filterRegistrations = new HashMap<>();

    private Map<String, Servlet> nameToServlets = new HashMap<>();
    private Map<String, Filter> nameToFilters = new HashMap<>();

    private List<ServletMapping> servletMappings = new ArrayList<>();
    private List<FilterMapping> filterMappings = new ArrayList<>();

    /**
     * 将请求url映射到对应的Servlet进行处理
     */
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 请求路径
        String path = request.getRequestURI();
        Servlet servlet = null;
        for (ServletMapping mapping : servletMappings) {
            if(mapping.matches(path)){
                servlet = mapping.servlet;
                break;
            }
        }
        if (servlet == null){
            // 没有匹配到Servlet 404
            PrintWriter pw = response.getWriter();
            pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + path + "</p>");
            pw.close();
            return;
        }

        // 先执行Filter, 然后执行Servlet
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matches(path)){
                enabledFilters.add(mapping.filter);
            }
        }
        Filter[] filters = enabledFilters.toArray(Filter[]::new);
        FilterChain chain = new FilterChainImpl(filters, servlet);

        try {
            chain.doFilter(request, response);
        }catch (Exception e){
            logger.error("处理请求失败", e);
        }
    }

    /**
     * 初始化Servlet
     */
    public void initServlets(List<Class<?>> servletClasses){
        // 1.注册Servlet, 添加到servletRegistrations
        for (Class<?> c : servletClasses) {
            // 获取WebServlet注解
            WebServlet ws = c.getAnnotation(WebServlet.class);
            if (ws != null){
                logger.info("自动注册 Servlet: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Servlet> clazz = (Class<? extends Servlet>) c;
                // 这里Servlet进行实例化, 但没有初始化
                ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(clazz), clazz);
                registration.addMapping(AnnoUtils.getServletUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getServletInitParams(clazz));
            }
        }
        // 2.初始化Servlet, 添加到Servlet容器servletMappings
        for (String name : this.servletRegistrations.keySet()) {
            var registration = this.servletRegistrations.get(name);
            try {
                registration.servlet.init(registration.getServletConfig());
                this.nameToServlets.put(name, registration.servlet);
                for (String urlPattern : registration.getMappings()) {
                    this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
                }
                registration.initialized = true;
            }catch (ServletException e){
                logger.error("Servlet {} 初始化失败", name, e);
            }
        }
    }

    /**
     * 初始化Filter
     */
    public void initFilters(List<Class<?>> filterClasses){
        // 1.注册Filter, 添加到filterRegistrations
        for (Class<?> c : filterClasses) {
            // 获取WebServlet注解
            WebFilter wf = c.getAnnotation(WebFilter.class);
            if (wf != null){
                logger.info("自动注册 Filter: {}", c.getName());
                @SuppressWarnings("unchecked")
                Class<? extends Filter> clazz = (Class<? extends Filter>) c;
                // 这里Filter进行实例化, 但没有初始化
                FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, AnnoUtils.getFilterUrlPatterns(clazz));
                registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
            }
        }
        // 2.初始化Filter, 添加到Filter容器filterMappings
        for (String name : this.filterRegistrations.keySet()) {
            var registration = this.filterRegistrations.get(name);
            try {
                registration.filter.init(registration.getFilterConfig());
                this.nameToFilters.put(name, registration.filter);
                for (String urlPattern : registration.getUrlPatternMappings()) {
                    this.filterMappings.add(new FilterMapping(urlPattern, registration.filter));
                }
                registration.initialized = true;
            }catch (ServletException e){
                logger.error("Filter {} 初始化失败", name, e);
            }
        }
    }


    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public ServletContext getContext(String s) {
        if (s.equals("")){
            return this;
        }
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 6;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 6;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String s) {
        String defaultMime = "application/octet-stream";
        Map<String, String> mimes = Map.of(".html", "text/html", ".css", "text/css", ".js", "application/javascript", ".json", "application/json");
        int n = s.lastIndexOf(".");
        if (n == -1){
            return defaultMime;
        }
        String ext = s.substring(n);
        return mimes.getOrDefault(ext, defaultMime);
    }

    @Override
    public Set<String> getResourcePaths(String s) {
        return null;
    }

    @Override
    public URL getResource(String s) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }

    @Override
    public void log(String s) {

    }

    @Override
    public void log(String s, Throwable throwable) {

    }

    @Override
    public String getRealPath(String s) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public String getInitParameter(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public boolean setInitParameter(String s, String s1) {
        return false;
    }

    @Override
    public Object getAttribute(String s) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public void setAttribute(String s, Object o) {

    }

    @Override
    public void removeAttribute(String s) {

    }

    @Override
    public String getServletContextName() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, String className) {
        if (className == null || className.isEmpty()){
            throw new IllegalArgumentException("Servlet class is null or empty");
        }
        Servlet servlet = null;
        try {
            // 根据类名加载Servlet类
            Class<? extends Servlet> clazz = createInstance(className);
            // 创建Servlet实例
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Servlet servlet) {
        if (name == null || name.isEmpty()){
            throw new IllegalArgumentException("Servlet name is null or empty");
        }
        if (servlet == null){
            throw new IllegalArgumentException("Servlet is null");
        }
        // 注册Servlet
        var registration = new ServletRegistrationImpl(this, name, servlet);
        this.servletRegistrations.put(name, registration);
        return registration;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Class<? extends Servlet> clazz) {
        if (clazz == null){
            throw new IllegalArgumentException("Servlet class is null");
        }
        Servlet servlet = null;
        try {
            // 创建Servlet实例
            servlet = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addServlet(name, servlet);
    }

    /**
     * 创建类实例
     * @param className 类名
     */
    @SuppressWarnings("unchecked")
    private <T> T createInstance(String className) throws ServletException {
        Class<T> clazz;
        try {
            clazz = (Class<T>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found.", e);
        }
        return createInstance(clazz);
    }

    private <T> T createInstance(Class<T> clazz) throws ServletException {
        try {
            Constructor<T> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ServletException("无法实例化类: " + clazz.getName(), e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addJspFile(String s, String s1) {
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> aClass) throws ServletException {
        return createInstance(aClass);
    }

    @Override
    public ServletRegistration getServletRegistration(String name) {
        return this.servletRegistrations.get(name);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Map.copyOf(this.servletRegistrations);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, String className) {
        if(className == null || className.isEmpty()){
            throw new IllegalArgumentException("Filter 类名不合法");
        }
        Filter filter = null;
        try {
            Class<? extends Filter> clazz = createInstance(className);
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Filter filter) {
        if(name == null){
            throw new IllegalArgumentException("name 不存在");
        }
        if (filter == null){
            throw new IllegalArgumentException("filter 不存在");
        }
        // 注册Filter
        var registration = new FilterRegistrationImpl(this, name, filter);
        this.filterRegistrations.put(name, registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> clazz) {
        if(clazz == null) {
            throw new IllegalArgumentException("Filter 类不存在");
        }
        Filter filter = null;
        try {
            filter = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        return addFilter(name, filter);
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
        return createInstance(aClass);
    }

    @Override
    public FilterRegistration getFilterRegistration(String s) {
        return this.filterRegistrations.get(s);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return Map.copyOf(this.filterRegistrations);
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> set) {

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String s) {

    }

    @Override
    public <T extends EventListener> void addListener(T t) {

    }

    @Override
    public void addListener(Class<? extends EventListener> aClass) {

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void declareRoles(String... strings) {

    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return this.sessionManager.inactiveInterval;
    }

    @Override
    public void setSessionTimeout(int i) {

    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String s) {

    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String s) {

    }
}
