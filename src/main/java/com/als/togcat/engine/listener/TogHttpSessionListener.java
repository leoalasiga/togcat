package com.als.togcat.engine.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description:  HttpSessionListener：用于监听HttpSession的创建和销毁事件；
 * @author: liujiajie
 * @date: 2024/2/18 15:42
 */
@WebListener
public class TogHttpSessionListener implements HttpSessionListener {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        logger.info(">>> HttpSession Created", se.getSession());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        logger.info(">>> HttpSession Destroyed", se.getSession());
    }

}
