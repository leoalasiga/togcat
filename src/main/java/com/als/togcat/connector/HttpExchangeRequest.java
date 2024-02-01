package com.als.togcat.connector;

import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    Headers getRequestHeaders();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    byte[] getRequestBody() throws IOException;
}
