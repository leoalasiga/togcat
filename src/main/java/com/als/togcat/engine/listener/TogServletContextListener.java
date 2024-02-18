package com.als.togcat.engine.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description:  ServletContextListener：用于监听ServletContext的创建和销毁事件；
 * @author: liujiajie
 * @date: 2024/2/18 15:42
 */
@WebListener
public class TogServletContextListener implements ServletContextListener {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info(">>> ServletContext Initialized", sce.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info(">>> ServletContext Destroyed", sce.getServletContext());
    }


}
