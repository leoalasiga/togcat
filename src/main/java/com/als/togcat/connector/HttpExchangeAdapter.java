package com.als.togcat.connector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * HttpExchange适配器
 * Adapter模式（https://www.liaoxuefeng.com/wiki/1545956031987744/1556279623417888#:~:text=%E7%AD%94%E6%A1%88%E6%98%AF%E4%BD%BF%E7%94%A8-,Adapter%E6%A8%A1%E5%BC%8F,-%E3%80%82）
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:44
 */
public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse {
    final HttpExchange httpExchange;

    public HttpExchangeAdapter(HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    public String getRequestMethod() {
        return this.httpExchange.getRequestMethod();
    }

    @Override
    public URI getRequestURI() {
        return this.httpExchange.getRequestURI();
    }

    @Override
    public Headers getResponseHeaders() {
        return this.httpExchange.getResponseHeaders();
    }

    @Override
    public void sendResponseHeaders(int retCode, long responseLength) throws IOException {
        this.httpExchange.sendResponseHeaders(retCode, responseLength);
    }

    @Override
    public OutputStream getResponseBody() {
        return this.httpExchange.getResponseBody();
    }
}
