package com.als.togcat.engine.listener;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: ServletRequestListener：用于监听ServletRequest的创建和销毁事件；
 * @author: liujiajie
 * @date: 2024/2/18 15:42
 */
@WebListener
public class TogServletRequestListener implements ServletRequestListener {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        logger.info(">>> ServletRequest Destroyed", sre.getServletRequest());
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        logger.info(">>> ServletRequest Initialized", sre.getServletRequest());
    }

}
