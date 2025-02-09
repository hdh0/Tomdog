package com.hdh.engine;

import com.hdh.engine.utils.DateUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager implements Runnable{

    final Logger logger = LoggerFactory.getLogger(getClass());
    final ServletContextImpl servletContext;
    final Map<String, HttpSessionImpl> sessions = new ConcurrentHashMap<>();
    final int inactiveInterval; // 会话失效间隔

    public SessionManager(ServletContextImpl servletContext, int interval) {
        this.servletContext = servletContext;
        this.inactiveInterval = interval;

        // 启动会话管理器线程
        Thread thread = new Thread(this, "SessionManager");
        thread.setDaemon(true);
        thread.start();
    }

    public HttpSession getSession(String sessionId) {
        HttpSessionImpl session = sessions.get(sessionId);
        if (session == null) {
            // 不存在时, 创建新的Session, 并放入sessions
            session = new HttpSessionImpl(this.servletContext, sessionId, this.inactiveInterval);
            sessions.put(sessionId, session);
        }else {
            session.lastAccessedTime = System.currentTimeMillis();
        }
        return session;
    }

    public void remove(HttpSession session){
        this.sessions.remove(session.getId());
    }

    @Override
    public void run() {
        for(;;){
            try {
                Thread.sleep(60 * 1000); // 1分钟检查一次
            } catch (InterruptedException e) {
                break;
            }
            long now = System.currentTimeMillis();
            sessions.forEach((id, session) -> {
                if (now - session.getLastAccessedTime() > session.getMaxInactiveInterval() * 1000L) {
                    logger.warn("Session {} 已过期, 最后访问时间: {}", id, DateUtils.formatDateTime(session.getLastAccessedTime()));
                    session.invalidate();
                }
            });
        }
    }
}
