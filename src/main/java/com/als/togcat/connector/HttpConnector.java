package com.als.togcat.connector;

import com.als.togcat.engine.HttpServletRequestImpl;
import com.als.togcat.engine.HttpServletResponseImpl;
import com.als.togcat.engine.ServletContextImpl;
import com.als.togcat.engine.filter.HelloFilter;
import com.als.togcat.engine.filter.LogFilter;
import com.als.togcat.engine.servlet.HelloServlet;
import com.als.togcat.engine.servlet.IndexServlet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.tools.javac.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:50
 */
public class HttpConnector implements HttpHandler,AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * =================version3
     */
    final ServletContextImpl servletContext;
    final HttpServer httpServer;
    //    final Duration stopDelay = Duration.ofSeconds(5);
    public HttpConnector() throws IOException {
        this.servletContext = new ServletContextImpl();
        this.servletContext.initServlets(List.of(IndexServlet.class, HelloServlet.class));
        this.servletContext.initFilters(List.of(LogFilter.class, HelloFilter.class));


        // start http server:
        String host = "0.0.0.0";
        int port = 8080;
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.start();
        logger.info("togcat http server started at {}:{}...", host, port);
    }


    @Override
    public void close() {
//        BigDecimal bigDecimal = this.stopDelay.toSeconds();
        this.httpServer.stop(5 );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeAdapter httpExchangeAdapter = new HttpExchangeAdapter(exchange);
        HttpServletRequest request = new HttpServletRequestImpl(httpExchangeAdapter);
        HttpServletResponse response = new HttpServletResponseImpl(httpExchangeAdapter);
        // process:
        try {
            this.servletContext.process(request, response);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * =================version2
     */
//    final ServletContextImpl servletContext;
//    final HttpServer httpServer;
////    final Duration stopDelay = Duration.ofSeconds(5);
//    public HttpConnector() throws IOException {
//        this.servletContext = new ServletContextImpl();
//        this.servletContext.initialize(List.of(IndexServlet.class, HelloServlet.class));
//
//
//        // start http server:
//        String host = "0.0.0.0";
//        int port = 8080;
//        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
//        this.httpServer.createContext("/", this);
//        this.httpServer.start();
//        logger.info("togcat http server started at {}:{}...", host, port);
//    }
//
//
//    @Override
//    public void close() {
////        BigDecimal bigDecimal = this.stopDelay.toSeconds();
//        this.httpServer.stop(5 );
//    }
//
//    @Override
//    public void handle(HttpExchange exchange) throws IOException {
//        HttpExchangeAdapter httpExchangeAdapter = new HttpExchangeAdapter(exchange);
//        HttpServletRequest request = new HttpServletRequestImpl(httpExchangeAdapter);
//        HttpServletResponse response = new HttpServletResponseImpl(httpExchangeAdapter);
//        // process:
//        try {
//            this.servletContext.process(request, response);
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//        }
//    }



    /**
     * =================version1
     */
//    final HttpServer httpServer;

//    public HttpConnector() throws IOException {
//        String addr = "0.0.0.0";
//        int port = 8080;
//        this.httpServer = HttpServer.create(new InetSocketAddress(addr, port), 0);
//        this.httpServer.createContext("/", this);
//        this.httpServer.start();
//        logger.info("togcat http server started at {}:{}...", addr, port);
//    }
//
//    @Override
//    public void close() throws Exception {
//        this.httpServer.stop(3);
//    }
//
//    @Override
//    public void handle(HttpExchange httpExchange) throws IOException {
//        HttpExchangeAdapter httpExchangeAdapter = new HttpExchangeAdapter(httpExchange);
//        HttpServletRequest request = new HttpServletRequestImpl(httpExchangeAdapter);
//        HttpServletResponse response = new HttpServletResponseImpl(httpExchangeAdapter);
//        process(request, response);
//    }
//
//    private void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String name = request.getParameter("name");
//        String html = "<h1>Hello, " + (name == null ? "world" : name) + ".</h1>";
//        response.setContentType("text/html");
//        try (PrintWriter writer = response.getWriter()) {
//            writer.write(html);
//        }
//    }
}
