package com.als.togcat.engine.listener;

import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description:  ServletRequestAttributeListener：用于监听ServletRequest属性的添加、修改和删除事件。
 * @author: liujiajie
 * @date: 2024/2/18 15:42
 */
@WebListener
public class TogServletRequestAttributeListener implements ServletRequestAttributeListener {
    final Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public void attributeAdded(ServletRequestAttributeEvent event) {
        logger.info(">>> ServletRequest attribute added: {} = {}", event.getName(), event.getValue());
    }

    @Override
    public void attributeRemoved(ServletRequestAttributeEvent event) {
        logger.info(">>> ServletRequest attribute removed: {} = {}", event.getName(), event.getValue());
    }

    @Override
    public void attributeReplaced(ServletRequestAttributeEvent event) {
        logger.info(">>> ServletRequest attribute replaced: {} = {}", event.getName(), event.getValue());
    }
}
