package com.als.togcat.connector;

import com.sun.net.httpserver.Headers;

import java.io.IOException;
import java.io.OutputStream;


/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:41
 */
public interface HttpExchangeResponse {
    Headers getResponseHeaders();

    void sendResponseHeaders(int retCode, long responseLength) throws IOException;

    OutputStream getResponseBody();
}
