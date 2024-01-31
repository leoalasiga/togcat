package com.als.togcat.connector;

import com.als.togcat.engine.HttpServletRequestImpl;
import com.als.togcat.engine.HttpServletResponseImpl;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:50
 */
public class HttpConnector implements HttpHandler,AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final HttpServer httpServer;

    public HttpConnector() throws IOException {
        String addr = "0.0.0.0";
        int port = 8080;
        this.httpServer = HttpServer.create(new InetSocketAddress(addr, port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.start();
        logger.info("togcat http server started at {}:{}...", addr, port);
    }

    @Override
    public void close() throws Exception {
        this.httpServer.stop(3);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpExchangeAdapter httpExchangeAdapter = new HttpExchangeAdapter(httpExchange);
        HttpServletRequest request = new HttpServletRequestImpl(httpExchangeAdapter);
        HttpServletResponse response = new HttpServletResponseImpl(httpExchangeAdapter);
        process(request, response);
    }

    private void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        String html = "<h1>Hello, " + (name == null ? "world" : name) + ".</h1>";
        response.setContentType("text/html");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(html);
        }
    }
}
