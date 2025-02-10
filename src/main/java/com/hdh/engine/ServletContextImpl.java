package com.hdh.engine;

import com.hdh.engine.mapping.FilterMapping;
import com.hdh.engine.mapping.ServletMapping;
import com.hdh.engine.support.Attributes;
import com.hdh.engine.utils.AnnoUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import jakarta.servlet.http.*;
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
    private Attributes attributes = new Attributes(true); // ServletContext属性

    final SessionManager sessionManager = new SessionManager(this, 600); // 会话管理器 10分钟失效

    private Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private Map<String, FilterRegistrationImpl> filterRegistrations = new HashMap<>();

    private Map<String, Servlet> nameToServlets = new HashMap<>();
    private Map<String, Filter> nameToFilters = new HashMap<>();

    private List<ServletMapping> servletMappings = new ArrayList<>();
    private List<FilterMapping> filterMappings = new ArrayList<>();

    // Listener
    private List<ServletContextListener> servletContextListeners = null; // 监听ServletContext创建和销毁
    private List<ServletContextAttributeListener> servletContextAttributeListeners = null; // 监听ServletContext属性变化
    private List<ServletRequestListener> servletRequestListeners = null; // 监听ServletRequest创建和销毁
    private List<ServletRequestAttributeListener> servletRequestAttributeListeners = null; // 监听ServletRequest属性变化
    private List<HttpSessionListener> httpSessionListeners = null; // 监听HttpSession创建和销毁
    private List<HttpSessionAttributeListener> httpSessionAttributeListeners = null; // 监听HttpSession属性变化

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
            this.invokeServletRequestInitialized(request);
            chain.doFilter(request, response);
        }catch (Exception e){
            logger.error("处理请求失败", e);
        }finally {
            this.invokeServletRequestDestroyed(request);
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

    /**
     * 获取属性值
     * @param s 属性名
     * @return 属性值
     */
    @Override
    public Object getAttribute(String s) {
        return this.attributes.getAttribute(s);
    }

    /**
     * 获取属性名列表
     * @return 属性名列表
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return this.attributes.getAttributeNames();
    }

    /**
     * 设置属性
     * @param name 属性名
     * @param value 属性值
     */
    @Override
    public void setAttribute(String name, Object value) {
        if(value == null){
            removeAttribute(name);
        }else {
            Object old = this.attributes.setAttribute(name, value);
            if(old == null) {
                // 当属性不存在时, 触发 ServletContextAttributeAdded 属性添加事件
                this.invokeServletContextAttributeAdded(name, value);
            }else {
                // 当属性存在时, 触发 ServletContextAttributeReplaced 属性替换事件
                this.invokeServletContextAttributeReplaced(name, value);
            }
        }
        this.attributes.setAttribute(name, value);
    }

    /**
     * 移除属性
     * @param s 属性名
     */
    @Override
    public void removeAttribute(String s) {
        this.attributes.removeAttribute(s);
    }

    @Override
    public String getServletContextName() {
        return null;
    }

    /**
     * 动态添加Servlet
     * @param name Servlet名称
     * @param className Servlet类名
     * @return ServletRegistration.Dynamic
     */
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

    /**
     * 添加Listener, 根据类名
     * @param className 类名
     */
    @Override
    public void addListener(String className) {
        EventListener listener = null;
        try {
            // 根据类名加载Listener类, 并创建Listener实例
            Class<EventListener> clazz = createInstance(className);
            listener = createInstance(clazz);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        addListener(listener);
    }

    /**
     * 添加Listener到对应的Listener容器中
     * @param t Listener
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        if(t instanceof ServletContextListener listener){
            if (this.servletContextListeners == null){
                this.servletContextListeners = new ArrayList<>();
            }
            this.servletContextListeners.add(listener);
        }else if(t instanceof ServletContextAttributeListener listener){
            if (this.servletContextAttributeListeners == null){
                this.servletContextAttributeListeners = new ArrayList<>();
            }
            this.servletContextAttributeListeners.add(listener);
        }else if(t instanceof ServletRequestListener listener){
            if (this.servletRequestListeners == null){
                this.servletRequestListeners = new ArrayList<>();
            }
            this.servletRequestListeners.add(listener);
        }else if(t instanceof ServletRequestAttributeListener listener){
            if (this.servletRequestAttributeListeners == null){
                this.servletRequestAttributeListeners = new ArrayList<>();
            }
            this.servletRequestAttributeListeners.add(listener);
        }else if(t instanceof HttpSessionListener listener){
            if (this.httpSessionListeners == null){
                this.httpSessionListeners = new ArrayList<>();
            }
            this.httpSessionListeners.add(listener);
        }else if(t instanceof HttpSessionAttributeListener listener){
            if (this.httpSessionAttributeListeners == null){
                this.httpSessionAttributeListeners = new ArrayList<>();
            }
            this.httpSessionAttributeListeners.add(listener);
        }
    }

    /**
     * 添加Listener, 根据类
     * @param aClass 类
     */
    @Override
    public void addListener(Class<? extends EventListener> aClass) {
        EventListener listener = null;
        try {
            listener = createInstance(aClass);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        addListener(listener);
    }

    // ================== 调用 Listener ==================

    void invokeServletContextAttributeAdded(String name, Object value) {
        logger.info("invoke ServletContextAttributeAdded: {} = {}", name, value);
        if (this.servletContextAttributeListeners != null) {
            // 创建 Event 事件
            var event = new ServletContextAttributeEvent(this, name, value);
            for (var listener : this.servletContextAttributeListeners) {
                // 调用 Listener 实现的方法
                listener.attributeAdded(event);
            }
        }
    }

    void invokeServletContextAttributeRemoved(String name, Object value) {
        logger.info("invoke ServletContextAttributeRemoved: {} = {}", name, value);
        if (this.servletContextAttributeListeners != null) {
            var event = new ServletContextAttributeEvent(this, name, value);
            for (var listener : this.servletContextAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeServletContextAttributeReplaced(String name, Object value) {
        logger.info("invoke ServletContextAttributeReplaced: {} = {}", name, value);
        if (this.servletContextAttributeListeners != null) {
            var event = new ServletContextAttributeEvent(this, name, value);
            for (var listener : this.servletContextAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeServletRequestAttributeAdded(HttpServletRequest request, String name, Object value) {
        logger.info("invoke ServletRequestAttributeAdded: {} = {}, request = {}", name, value, request);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeAdded(event);
            }
        }
    }

    void invokeServletRequestAttributeRemoved(HttpServletRequest request, String name, Object value) {
        logger.info("invoke ServletRequestAttributeRemoved: {} = {}, request = {}", name, value, request);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeServletRequestAttributeReplaced(HttpServletRequest request, String name, Object value) {
        logger.info("invoke ServletRequestAttributeReplaced: {} = {}, request = {}", name, value, request);
        if (this.servletRequestAttributeListeners != null) {
            var event = new ServletRequestAttributeEvent(this, request, name, value);
            for (var listener : this.servletRequestAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeHttpSessionAttributeAdded(HttpSession session, String name, Object value) {
        logger.info("invoke HttpSessionAttributeAdded: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeAdded(event);
            }
        }
    }

    void invokeHttpSessionAttributeRemoved(HttpSession session, String name, Object value) {
        logger.info("invoke ServletContextAttributeRemoved: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeRemoved(event);
            }
        }
    }

    void invokeHttpSessionAttributeReplaced(HttpSession session, String name, Object value) {
        logger.info("invoke ServletContextAttributeReplaced: {} = {}, session = {}", name, value, session);
        if (this.httpSessionAttributeListeners != null) {
            var event = new HttpSessionBindingEvent(session, name, value);
            for (var listener : this.httpSessionAttributeListeners) {
                listener.attributeReplaced(event);
            }
        }
    }

    void invokeServletRequestInitialized(HttpServletRequest request) {
        logger.info("invoke ServletRequestInitialized: request = {}", request);
        if (this.servletRequestListeners != null) {
            var event = new ServletRequestEvent(this, request);
            for (var listener : this.servletRequestListeners) {
                listener.requestInitialized(event);
            }
        }
    }

    void invokeServletRequestDestroyed(HttpServletRequest request) {
        logger.info("invoke ServletRequestDestroyed: request = {}", request);
        if (this.servletRequestListeners != null) {
            var event = new ServletRequestEvent(this, request);
            for (var listener : this.servletRequestListeners) {
                listener.requestDestroyed(event);
            }
        }
    }

    void invokeHttpSessionCreated(HttpSession session) {
        logger.info("invoke HttpSessionCreated: session = {}", session);
        if (this.httpSessionListeners != null) {
            var event = new HttpSessionEvent(session);
            for (var listener : this.httpSessionListeners) {
                listener.sessionCreated(event);
            }
        }
    }

    void invokeHttpSessionDestroyed(HttpSession session) {
        logger.info("invoke HttpSessionDestroyed: session = {}", session);
        if (this.httpSessionListeners != null) {
            var event = new HttpSessionEvent(session);
            for (var listener : this.httpSessionListeners) {
                listener.sessionDestroyed(event);
            }
        }
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
}
