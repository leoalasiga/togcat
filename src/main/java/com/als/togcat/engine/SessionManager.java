package com.als.togcat.engine;

import com.als.togcat.utils.DateUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:08
 */
public class SessionManager implements Runnable {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final ServletContextImpl servletContext;
    final Map<String, HttpSessionImpl> sessions = new ConcurrentHashMap<>();
    final int inactiveInterval;


    public SessionManager(ServletContextImpl servletContext, int inactiveInterval) {
        this.servletContext = servletContext;
        this.inactiveInterval = inactiveInterval;
        // 在构造函数里定义了一个线程，target是这个实例自己，然后让这个守护线程后台定期执行任务
        Thread t = new Thread(this, "Session-Cleanup-Thread");
        t.setDaemon(true);
        t.start();
    }


    public HttpSession getSession(String sessionId) {
        // 获取session
        HttpSessionImpl session = sessions.get(sessionId);
        if (session == null) {
            // 没有就构造一个HttpSessionImpl实现，然后将相关的值放进去
            session = new HttpSessionImpl(this.servletContext, sessionId, inactiveInterval);
            sessions.put(sessionId, session);
            // 触发session创建监听
            this.servletContext.invokeHttpSessionCreated(session);
        } else {
            // 更新session时间
            session.lastAccessedTime = System.currentTimeMillis();
        }
        return session;
    }


    public void remove(HttpSession session) {
        // 移除失效的session
        this.sessions.remove(session.getId());
        // 触发listener里的方法
        this.servletContext.invokeHttpSessionDestroyed(session);
    }

    @Override
    public void run() {
        for (; ; ) {
            // 一直循环，守护线程定时去删除失效的session
            try {
                // 打印出来就是Session-Cleanup-Thread这个守护线程在执行
//                System.out.println(Thread.currentThread().getName());
                //60_000L表示一个long类型的整数常量。这种表现形式是Java 7及以后版本引入的一种增强，目的是为了使数字更易读
                Thread.sleep(60_000L);
            } catch (InterruptedException e) {
                break;
            }
            long now = System.currentTimeMillis();
            for (String sessionId : sessions.keySet()) {
                HttpSession session = sessions.get(sessionId);
                if (session.getLastAccessedTime() + session.getMaxInactiveInterval() * 1000L < now) {
                    logger.warn("remove expired session: {}, last access time: {}", sessionId, DateUtils.formatDateTimeGMT(session.getLastAccessedTime()));
                    // 让session失效
                    session.invalidate();
                }
            }
        }
    }
}
