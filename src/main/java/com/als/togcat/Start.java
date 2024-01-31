package com.als.togcat;

import com.als.togcat.connector.HttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午4:09
 */
public class Start {
    static Logger logger = LoggerFactory.getLogger(Start.class);
    public static void main(String[] args) throws Exception {
        try (HttpConnector connector = new HttpConnector()) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("togcat http server was shutdown.");
    }

}
