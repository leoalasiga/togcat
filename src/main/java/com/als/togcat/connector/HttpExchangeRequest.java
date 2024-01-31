package com.als.togcat.connector;

import java.net.URI;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:41
 */
public interface HttpExchangeRequest {
    String getRequestMethod();

    URI getRequestURI();
}
