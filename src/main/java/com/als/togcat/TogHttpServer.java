package com.als.togcat;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * HttpServer：通过指定IP地址和端口号，定义一个HTTP服务实例；
 * HttpHandler：处理HTTP请求的核心接口，必须实现handle(HttpExchange)方法；
 * HttpExchange：可以读取HTTP请求的输入，并将HTTP响应输出给它。
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 上午11:32
 */
public class TogHttpServer implements HttpHandler,AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final HttpServer httpServer;
    final String host;
    final int port;

    /**
     * 定义了一个httpserver，
     * @param host
     * @param port
     * @throws IOException
     */
    public TogHttpServer(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.start();
        logger.info("start togcat http server at {}:{}", host, port);
    }

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;
        try (TogHttpServer connector = new TogHttpServer(host, port)) {
            for (;;) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void close() throws Exception {
        this.httpServer.stop(3);
    }

    /**
     * 可见，HttpExchange封装了HTTP请求和响应，我们不再需要解析原始的HTTP请求，也无需构造原始的HTTP响应，而是通过HttpExchange间接操作，大大简化了HTTP请求的处理。
     * @param httpExchange
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // 获取请求方法、URI、Path、Query等:
        String method = httpExchange.getRequestMethod();
        URI requestURI = httpExchange.getRequestURI();
        String path = requestURI.getPath();
        String query = requestURI.getRawQuery();
        logger.info("{}: {}?{}", method, path, query);

        // 输出响应的Header:
        Headers responseHeaders = httpExchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/html; charset=utf-8");
        responseHeaders.set("Cache-Control", "no-cache");

        //设置200响应:
        httpExchange.sendResponseHeaders(200, 0L);

        // 输出响应的内容:
        String s = "<h1>Hello, world.</h1><p>" + LocalDateTime.now().withNano(0) + "</p>";
        try (OutputStream responseBody = httpExchange.getResponseBody()) {
            responseBody.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
