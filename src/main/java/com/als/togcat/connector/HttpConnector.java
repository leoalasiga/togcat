package com.als.togcat.connector;

import com.als.togcat.Config;
import com.als.togcat.engine.HttpServletRequestImpl;
import com.als.togcat.engine.HttpServletResponseImpl;
import com.als.togcat.engine.ServletContextImpl;
import com.als.togcat.engine.filter.HelloFilter;
import com.als.togcat.engine.filter.LogFilter;
import com.als.togcat.engine.listener.*;
//import com.als.togcat.engine.servlet.HelloServlet;
//import com.als.togcat.engine.servlet.IndexServlet;
//import com.als.togcat.engine.servlet.LoginServlet;
//import com.als.togcat.engine.servlet.LogoutServlet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.Executor;

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
    final Config config;
    final ClassLoader classLoader;
    final ServletContextImpl servletContext;
    final HttpServer httpServer;
    //    final Duration stopDelay = Duration.ofSeconds(5);

    public HttpConnector(Config config, String webRoot, Executor executor, ClassLoader classLoader, List<Class<?>> autoScannedClasses) throws IOException {
        logger.info("starting togcat http server at {}:{}...", config.server.host, config.server.port);
        this.config = config;
        this.classLoader = classLoader;

        // init servlet context:
        Thread.currentThread().setContextClassLoader(this.classLoader);
        ServletContextImpl ctx = new ServletContextImpl(classLoader, config, webRoot);
        ctx.initialize(autoScannedClasses);
        this.servletContext = ctx;
        Thread.currentThread().setContextClassLoader(null);

        // start http server:
        this.httpServer = HttpServer.create(new InetSocketAddress(config.server.host, config.server.port), 0);
        this.httpServer.createContext("/", this);
        this.httpServer.setExecutor(executor);
        this.httpServer.start();
        logger.info("togcat http server started at {}:{}...", config.server.host, config.server.port);
    }


    //version 1////////////////////////////////////////////////////////////////////
//    public HttpConnector() throws IOException {
//        this.servletContext = new ServletContextImpl();
//        this.servletContext.initServlets(Arrays.asList(IndexServlet.class, LoginServlet.class, LogoutServlet.class, HelloServlet.class));
//        this.servletContext.initFilters(Arrays.asList(LogFilter.class, HelloFilter.class));
//
//        //  =================version4  add listener
//        List<Class<? extends EventListener>> eventListeners = Arrays.asList(TogHttpSessionAttributeListener.class, TogHttpSessionListener.class, TogServletContextAttributeListener.class, TogServletContextListener.class
//                , TogServletRequestAttributeListener.class, TogServletRequestListener.class);
//        for (Class<? extends EventListener> eventListener : eventListeners) {
//            this.servletContext.addListener(eventListener);
//        }
//
//        // start http server:
//        String host = "0.0.0.0";
//        int port = 8080;
//        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
//        this.httpServer.createContext("/", this);
//        this.httpServer.start();
//        logger.info("togcat http server started at {}:{}...", host, port);
//    }


    @Override
    public void close() {
//        BigDecimal bigDecimal = this.stopDelay.toSeconds();
        this.servletContext.destroy();
        this.httpServer.stop(5 );
    }

    // version4///////////////////////////////////////////////////////////
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpExchangeAdapter httpExchangeAdapter = new HttpExchangeAdapter(exchange);
//        HttpServletResponse response = new HttpServletResponseImpl(httpExchangeAdapter);
        HttpServletResponse response = new HttpServletResponseImpl(config,httpExchangeAdapter);
//        HttpServletRequest request = new HttpServletRequestImpl(this.servletContext,httpExchangeAdapter,response);
        HttpServletRequest request = new HttpServletRequestImpl(config,this.servletContext,httpExchangeAdapter,response);
        // process:
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
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
