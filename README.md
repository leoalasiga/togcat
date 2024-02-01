# 手写Tomcat（togcat）

> Tomcat是一个开源的web服务器，他的架构是基于组件设计的，可以通过将多个组件组合起来使用

![tomcat架构图](/Users/liujiajie/Desktop/my_project/togcat/img/tomcat架构图.png)

一个Tomcat Server内部可以有多个Service（服务），通常是一个Service。Service内部包含两个组件：

+ Connectors：代表一组Connector（连接器），至少定义一个Connector，也允许定义多个Connector，例如，HTTP和HTTPS两个Connector；

- Engine：代表一个引擎，所有HTTP请求经过Connector后传递给Engine。

在一个Engine内部，可以有一个或多个Host（主机），Host可以根据域名区分，在Host内部，又可以有一个或多个Context（上下文），每个Context对应一个Web App。Context是由路径前缀区分的，如`/abc`、`/xyz`、`/`分别代表3个Web App，`/`表示的Web App在Tomcat中表示根Web App。

因此，一个HTTP请求：

```
http://www.example.com/abc/hello
```

首先根据域名`www.example.com`定位到某个Host，然后，根据路径前缀`/abc`定位到某个Context，若路径前缀没有匹配到任何Context，则匹配`/`Context。在Context内部，就是开发者编写的Web App，一个Context仅包含一个Web App。

可见Tomcat Server是一个全功能的Web服务器，它支持HTTP、HTTPS和AJP等多种Connector，又能同时运行多个Host，每个Host内部，还可以挂载一个或多个Context，对应一个或多个Web App。



## servlet规范

在Java Web应用中，除了Tomcat服务器外，其实还有[Jetty](https://eclipse.dev/jetty/)、[GlassFish](https://javaee.github.io/glassfish/)、[Undertow](https://undertow.io/)等多种Web服务器。

一个Java Web App通常打包为`.war`文件，并且可以部署到Tomcat、Jetty等多种Web服务器上。为什么一个Java Web App基本上可以无修改地部署到多种Web服务器上呢？原因就在于Servlet规范。

Servlet规范是Java Servlet API的规范，用于定义Web服务器如何处理HTTP请求和响应。Servlet规范有一组接口，对于Web App来说，操作的是接口，而真正对应的实现类，则由各个Web Server实现，这样一来，Java Web App实际上编译的时候仅用到了Servlet规范定义的接口，只要每个Web服务器在实现Servlet接口时严格按照规范实现，就可以保证一个Web App可以正常运行在多种Web服务器上：

```ascii
  ┌─────────────────┐
  │     Web App     │
  └─────────────────┘
           ▲
           │
           ▼
  ┌─────────────────┐
┌─┤Servlet Interface├─┐
│ └─────────────────┘ │
│          ▲          │
│          │          │
│          ▼          │
│ ┌─────────────────┐ │
│ │     Servlet     │ │
│ │ Implementation  │ │
│ └─────────────────┘ │
│       Server        │
└─────────────────────┘
```

对于Web应用程序，Servlet规范是非常重要的。Servlet规范有好几个版本，每个版本都有一些新的功能。以下是一些常见版本的新功能：

Servlet 1.0：定义了Servlet组件，一个Servlet组件运行在Servlet容器（Container）中，通过与容器交互，就可以响应一个HTTP请求；

Servlet 2.0：定义了JSP组件，一个JSP页面可以被动态编译为Servlet组件；

Servlet 2.4：定义了Filter（过滤器）组件，可以实现过滤功能；

Servlet 2.5：支持注解，提供了ServletContextListener接口，增加了一些安全性相关的特性；

Servlet 3.0：支持异步处理的Servlet，支持注解配置Servlet和过滤器，增加了SessionCookieConfig接口；

Servlet 3.1：提供了WebSocket的支持，增加了对HTTP请求和响应的流式操作的支持，增加了对HTTP协议的新特性的支持；

Servlet 4.0：支持HTTP/2的新特性，提供了HTTP/2的Server Push等特性；

Servlet 5.0：主要是把`javax.servlet`包名改成了`jakarta.servlet`；

Servlet 6.0：继续增加一些新功能，并废除一部分功能。

目前最新的Servlet版本是6.0，我们开发Jerrymouse Server也是基于最新的Servlet 6.0。

### Servlet处理流程

当Servlet容器接收到用户的HTTP请求后，由容器负责把请求转换为`HttpServletRequest`和`HttpServletResponse`对象，分别代表HTTP请求和响应，然后，经过若干个Filter组件后，到达最终的Servlet组件，由Servlet组件完成HTTP处理，将响应写入`HttpServletResponse`对象：

```ascii
 ┌────────────────────────────────┐
 │         ServletContext         │
 │                                │
 │HttpServletRequest  ┌─────────┐ │
─┼───────────────────▶│ Filter  │ │
 │HttpServletResponse └─────────┘ │
 │                         │      │
 │                         ▼      │
 │                    ┌─────────┐ │
 │                    │ Filter  │ │
 │                    └─────────┘ │
 │                         │      │
 │ ┌─────────┐             ▼      │
 │ │Listener │        ┌─────────┐ │
 │ └─────────┘        │ Filter  │ │
 │ ┌─────────┐        └─────────┘ │
 │ │Listener │             │      │
 │ └─────────┘             ▼      │
 │ ┌─────────┐        ┌─────────┐ │
 │ │Listener │        │ Servlet │ │
 │ └─────────┘        └─────────┘ │
 └────────────────────────────────┘
```

其中，`ServletContext`代表整个容器的信息，如果容器实现了`ServletContext`接口，也可以把`ServletContext`可以看作容器本身。`ServletContext`、`HttpServletRequest`和`HttpServletResponse`都是接口，具体实现由Web服务器完成。`Filter`、`Servlet`组件也是接口，但具体实现由Web App完成。此外，还有一种`Listener`接口，可以监听各种事件，但不直接参与处理HTTP请求，具体实现由Web App完成，何时调用则由容器决定。因此，针对Web App的三大组件：`Servlet`、`Filter`和`Listener`都是运行在容器中的组件，只有容器才能主动调用它们。（此处略去JSP组件，因为我们不打算支持JSP）

对于Jerrymouse服务器来说，开发服务器就必须实现Servlet容器本身，容器实现`ServletContext`接口，容器内部管理若干个`Servlet`、`Filter`和`Listener`组件。

对每个请求，需要创建`HttpServletRequest`和`HttpServletResponse`实例，查找并匹配合适的一组`Filter`和一个`Servlet`，让它们处理HTTP请求。

在处理过程中，会产生各种事件，容器负责将产生的事件发送到`Listener`组件处理。

以上就是我们编写Servlet容器按照Servlet规范所必须的全部功能。



## togcat架构

我们设计的Jerrymouse Server的架构如下：

```ascii
  ┌───────────────────────────────┐
  │       Jerrymouse Server       │
  │                 ┌───────────┐ │
  │  ┌─────────┐    │  Context  │ │
  │  │  HTTP   │    │┌─────────┐│ │
◀─┼─▶│Connector│◀──▶││ Web App ││ │
  │  └─────────┘    │└─────────┘│ │
  │                 └───────────┘ │
  └───────────────────────────────┘
```

在实现Servlet支持之前，我们先实现一个HTTP Connector。

所谓Connector，这里可以简化为一个能处理HTTP请求的服务器，HTTP/1.x协议是基于TCP连接的一个简单的请求-响应协议，首先由浏览器发送请求：

```
GET /hello HTTP/1.1
Host: www.example.com
User-Agent: curl/7.88.1
Accept: */*
```

请求头指出了请求的方法`GET`，主机`www.example.com`，路径`/hello`，接下来服务器解析请求，输出响应：

```
HTTP/1.1 200 OK
Server: Simple HttpServer/1.0
Date: Fri, 07 Jul 2023 23:15:09 GMT
Content-Type: text/html; charset=utf-8
Content-Length: 22
Connection: keep-alive

<h1>Hello, world.</h1>
```

响应返回状态码`200`，每个响应头`Header: Value`，最后是以`\r\n\r\n`分隔的响应内容。

所以我们编写HTTP Server实际上就是：

1. 监听TCP端口，等待浏览器连接；
2. 接受TCP连接后，创建一个线程处理该TCP连接：
   1. 接收浏览器发送的HTTP请求；
   2. 解析HTTP请求；
   3. 处理请求；
   4. 发送HTTP响应；
   5. 重复1～4直到TCP连接关闭。

整个流程不复杂，但是处理步骤比较繁琐，尤其是解析HTTP请求，是个体力活，不但要去读HTTP协议手册，还要做大量的兼容性测试。

所以我们选择直接使用JDK内置的`jdk.httpserver`。`jdk.httpserver`从JDK 9开始作为一个公开模块可以直接使用，它的包是`com.sun.net.httpserver`，主要提供以下几个类：

- HttpServer：通过指定IP地址和端口号，定义一个HTTP服务实例；
- HttpHandler：处理HTTP请求的核心接口，必须实现`handle(HttpExchange)`方法；
- HttpExchange：可以读取HTTP请求的输入，并将HTTP响应输出给它。

一个能处理HTTP请求的简单类实现如下：

```
class SimpleHttpHandler implements HttpHandler {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 获取请求方法、URI、Path、Query等:
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String query = uri.getRawQuery();
        logger.info("{}: {}?{}", method, path, query);
        // 输出响应的Header:
        Headers respHeaders = exchange.getResponseHeaders();
        respHeaders.set("Content-Type", "text/html; charset=utf-8");
        respHeaders.set("Cache-Control", "no-cache");
        // 设置200响应:
        exchange.sendResponseHeaders(200, 0);
        // 输出响应的内容:
        String s = "<h1>Hello, world.</h1><p>" + LocalDateTime.now().withNano(0) + "</p>";
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(s.getBytes(StandardCharsets.UTF_8));
        }
    }
}
```

可见，`HttpExchange`封装了HTTP请求和响应，我们不再需要解析原始的HTTP请求，也无需构造原始的HTTP响应，而是通过`HttpExchange`间接操作，大大简化了HTTP请求的处理。

最后写一个`SimpleHttpServer`把启动`HttpServer`、处理请求连起来：

```
public class SimpleHttpServer implements HttpHandler, AutoCloseable {
    final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 8080;
        try (SimpleHttpServer connector = new SimpleHttpServer(host, port)) {
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

    final HttpServer httpServer;
    final String host;
    final int port;

    public SimpleHttpServer(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0, "/", this);
        this.httpServer.start();
        logger.info("start jerrymouse http server at {}:{}", host, port);
    }

    @Override
    public void close() {
        this.httpServer.stop(3);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ...
    }
}
```

运行后打开浏览器，访问`http://localhost:8080`

输出Hello world



## 实现servlet服务

我们已经成功实现了一个简单的HTTP服务器，但是，好像和Servlet没啥关系，因为整个操作都是基于`HttpExchange`接口做的。

而Servlet处理HTTP的接口是基于`HttpServletRequest`和`HttpServletResponse`，前者负责读取HTTP请求，后者负责写入HTTP响应。

怎么把基于`HttpExchange`的操作转换为基于`HttpServletRequest`和`HttpServletResponse`？答案是使用[Adapter模式](https://www.liaoxuefeng.com/wiki/1252599548343744/1281319245971489)。

首先我们定义`HttpExchangeAdapter`，它持有一个`HttpExchange`实例，并实现`HttpExchangeRequest`和`HttpExchangeResponse`接口：

```
interface HttpExchangeRequest {
    String getRequestMethod();
    URI getRequestURI();
}

interface HttpExchangeResponse {
    Headers getResponseHeaders();
    void sendResponseHeaders(int rCode, long responseLength) throws IOException;
    OutputStream getResponseBody();
}

public class HttpExchangeAdapter implements HttpExchangeRequest, HttpExchangeResponse {
    final HttpExchange exchange;

    public HttpExchangeAdapter(HttpExchange exchange) {
        this.exchange = exchange;
    }

    // 实现方法
    ...
}
```

紧接着我们编写`HttpServletRequestImpl`，它内部持有`HttpServletRequest`，并实现了`HttpServletRequest`接口：

```
public class HttpServletRequestImpl implements HttpServletRequest {
    final HttpExchangeRequest exchangeRequest;

    public HttpServletRequestImpl(HttpExchangeRequest exchangeRequest) {
        this.exchangeRequest = exchangeRequest;
    }

    // 实现方法
    ...
}
```

同理，编写`HttpServletResponseImpl`如下：

```
public class HttpServletResponseImpl implements HttpServletResponse {
    final HttpExchangeResponse exchangeResponse;

    public HttpServletResponseImpl(HttpExchangeResponse exchangeResponse) {
        this.exchangeResponse = exchangeResponse;
    }

    // 实现方法
    ...
}
```

用一个图表示从`HttpExchange`转换为`HttpServletRequest`和`HttpServletResponse`如下：

```ascii
   ┌──────────────────────┐ ┌───────────────────────┐
   │  HttpServletRequest  │ │  HttpServletResponse  │
   └──────────────────────┘ └───────────────────────┘
               ▲                        ▲
               │                        │
   ┌──────────────────────┐ ┌───────────────────────┐
   │HttpServletRequestImpl│ │HttpServletResponseImpl│
┌──│- exchangeRequest     │ │- exchangeResponse ────┼──┐
│  └──────────────────────┘ └───────────────────────┘  │
│                                                      │
│  ┌──────────────────────┐ ┌───────────────────────┐  │
└─▶│ HttpExchangeRequest  │ │ HttpExchangeResponse  │◀─┘
   └──────────────────────┘ └───────────────────────┘
                      ▲         ▲
                      │         │
                      │         │
                 ┌───────────────────┐
                 │HttpExchangeAdapter│   ┌────────────┐
                 │- httpExchange ────┼──▶│HttpExchange│
                 └───────────────────┘   └────────────┘
```

接下来我们改造处理HTTP请求的`HttpHandler`接口：

```
public class HttpConnector implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var adapter = new HttpExchangeAdapter(exchange);
        var request = new HttpServletRequestImpl(adapter);
        var response = new HttpServletResponseImpl(adapter);
        process(request, response);
    }

    void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO
    }
}
```

在`handle(HttpExchange)`方法内部，我们创建的对象如下：

1. HttpExchangeAdapter实例：它内部引用了jdk.httpserver提供的HttpExchange实例；
2. HttpServletRequestImpl实例：它内部引用了HttpExchangeAdapter实例，但是转换为HttpExchangeRequest接口；
3. HttpServletResponseImpl实例：它内部引用了HttpExchangeAdapter实例，但是转换为HttpExchangeResponse接口。

所以实际上创建的实例只有3个。最后调用`process(HttpServletRequest, HttpServletResponse)`方法，这个方法内部就可以按照Servlet标准来处理HTTP请求了，因为方法参数是标准的Servlet接口：

```
void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String name = request.getParameter("name");
    String html = "<h1>Hello, " + (name == null ? "world" : name) + ".</h1>";
    response.setContentType("text/html");
    PrintWriter pw = response.getWriter();
    pw.write(html);
    pw.close();
}
```

目前，我们仅实现了代码调用时用到的`getParameter()`、`setContentType()`和`getWriter()`这几个方法。如果补全`HttpServletRequest`和`HttpServletResponse`接口所有的方法定义，我们就得到了完整的`HttpServletRequest`和`HttpServletResponse`接口实现。

运行代码，在浏览器输入`http://localhost:8080/?name=World`，结果如下：

hello world



## 实现ServletContext

在Java Web应用程序中，`ServletContext`代表应用程序的运行环境，一个Web应用程序对应一个唯一的`ServletContext`实例，`ServletContext`可以用于：

- 提供初始化和全局配置：可以从`ServletContext`获取Web App配置的初始化参数、资源路径等信息；
- 共享全局数据：`ServletContext`存储的数据可以被整个Web App的所有组件读写。

既然`ServletContext`是一个Web App的全局唯一实例，而Web App又运行在Servlet容器中，我们在实现`ServletContext`时，完全可以把它当作Servlet容器来实现，它在内部维护一组Servlet实例，并根据Servlet配置的路由信息将请求转发给对应的Servlet处理。假设我们编写了两个Servlet：

- IndexServlet：映射路径为`/`；
- HelloServlet：映射路径为`/hello`。

那么，处理HTTP请求的路径如下：

```ascii
                     ┌────────────────────┐
                     │   ServletContext   │
                     ├────────────────────┤
                     │     ┌────────────┐ │
    ┌─────────────┐  │ ┌──▶│IndexServlet│ │
───▶│HttpConnector│──┼─┤   ├────────────┤ │
    └─────────────┘  │ └──▶│HelloServlet│ │
                     │     └────────────┘ │
                     └────────────────────┘
```

下面，我们来实现`ServletContext`。首先定义`ServletMapping`，它包含一个Servlet实例，以及将映射路径编译为正则表达式：

```
public class ServletMapping {
    final Pattern pattern; // 编译后的正则表达式
    final Servlet servlet; // Servlet实例
    public ServletMapping(String urlPattern, Servlet servlet) {
        this.pattern = buildPattern(urlPattern); // 编译为正则表达式
        this.servlet = servlet;
    }
}
```

接下来实现`ServletContext`：

```
public class ServletContextImpl implements ServletContext {
    final List<ServletMapping> servletMappings = new ArrayList<>();
}
```

这个数据结构足够能让我们实现根据请求路径路由到某个特定的Servlet：

```
public class ServletContextImpl implements ServletContext {
    ...
    // HTTP请求处理入口:
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 请求路径:
        String path = request.getRequestURI();
        // 搜索Servlet:
        Servlet servlet = null;
        for (ServletMapping mapping : this.servletMappings) {
            if (mapping.matches(path)) {
                // 路径匹配:
                servlet = mapping.servlet;
                break;
            }
        }
        if (servlet == null) {
            // 未匹配到任何Servlet显示404 Not Found:
            PrintWriter pw = response.getWriter();
            pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + path + "</p>");
            pw.close();
            return;
        }
        // 由Servlet继续处理请求:
        servlet.service(request, response);
    }
}
```

这样我们就实现了`ServletContext`！

不过，细心的同学会发现，我们编写的两个Servlet：`IndexServlet`和`HelloServlet`，还没有被添加到`ServletContext`中。那么问题来了：Servlet在什么时候被初始化？

答案是在创建`ServletContext`实例后，就立刻初始化所有的Servlet。我们编写一个`initialize()`方法，用于初始化Servlet：

```
public class ServletContextImpl implements ServletContext {
    Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    Map<String, Servlet> nameToServlets = new HashMap<>();
    List<ServletMapping> servletMappings = new ArrayList<>();

    public void initialize(List<Class<?>> servletClasses) {
        // 依次添加每个Servlet:
        for (Class<?> c : servletClasses) {
            // 获取@WebServlet注解:
            WebServlet ws = c.getAnnotation(WebServlet.class);
            Class<? extends Servlet> clazz = (Class<? extends Servlet>) c;
            // 创建一个ServletRegistration.Dynamic:
            ServletRegistration.Dynamic registration = this.addServlet(AnnoUtils.getServletName(clazz), clazz);
            registration.addMapping(AnnoUtils.getServletUrlPatterns(clazz));
            registration.setInitParameters(AnnoUtils.getServletInitParams(clazz));
        }
        // 实例化Servlet:
        for (String name : this.servletRegistrations.keySet()) {
            var registration = this.servletRegistrations.get(name);
            registration.servlet.init(registration.getServletConfig());
            this.nameToServlets.put(name, registration.servlet);
            for (String urlPattern : registration.getMappings()) {
                this.servletMappings.add(new ServletMapping(urlPattern, registration.servlet));
            }
            registration.initialized = true;
        }
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String name, Servlet servlet) {
        var registration = new ServletRegistrationImpl(this, name, servlet);
        this.servletRegistrations.put(name, registration);
        return registration;
    }
}
```

从Servlet 3.0规范开始，我们必须要提供`addServlet()`动态添加一个Servlet，并且返回`ServletRegistration.Dynamic`，因此，我们在`initialize()`方法中调用`addServlet()`，完成所有Servlet的创建和初始化。

最后我们修改`HttpConnector`，实例化`ServletContextImpl`：

```
public class HttpConnector implements HttpHandler {
    // 持有ServletContext实例:
    final ServletContextImpl servletContext;
    final HttpServer httpServer;

    public HttpConnector() throws IOException {
        // 创建ServletContext:
        this.servletContext = new ServletContextImpl();
        // 初始化Servlet:
        this.servletContext.initialize(List.of(IndexServlet.class, HelloServlet.class));
        ...
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var adapter = new HttpExchangeAdapter(exchange);
        var request = new HttpServletRequestImpl(adapter);
        var response = new HttpServletResponseImpl(adapter);
        // process:
        this.servletContext.process(request, response);
    }
}
```

运行服务器，输入`http://localhost:8080/`，查看`IndexServlet`的输出：

index

输入`http://localhost:8080/hello?name=Bob`，查看`HelloServlet`的输出：

hello bob

输入错误的路径，查看404输出



#### 实现FilterChain

我们实现了`ServletContext`，并且能够管理所有的Servlet组件。本节我们继续增加对Filter组件的支持。

Filter是Servlet规范中的一个重要组件，它的作用是在HTTP请求到达Servlet之前进行预处理。它可以被一个或多个Filter按照一定的顺序组成一个处理链（FilterChain），用来处理一些公共逻辑，比如打印日志、登录检查等。

Filter还可以有针对性地拦截或者放行HTTP请求，本质上一个`FilterChain`就是一个[责任链](https://www.liaoxuefeng.com/wiki/1252599548343744/1281319474561057)模式。在Servlet容器中，处理流程如下：

```ascii
  ┌─────────────────┐
  │ ServletContext  │
  │ ┌ ─ ─ ─ ─ ─ ─ ┐ │
  │   FilterChain   │
  │ │ ┌─────────┐ │ │
──┼──▶│ Filter  │   │
  │ │ └─────────┘ │ │
  │        │        │
  │ │      ▼      │ │
  │   ┌─────────┐   │
  │ │ │ Filter  │ │ │
  │   └─────────┘   │
  │ │      │      │ │
  │        ▼        │
  │ │ ┌─────────┐ │ │
  │   │ Filter  │   │
  │ │ └─────────┘ │ │
  │  ─ ─ ─ ┬ ─ ─ ─  │
  │        ▼        │
  │   ┌─────────┐   │
  │   │ Servlet │   │
  │   └─────────┘   │
  └─────────────────┘
```

这里有几点需要注意：

1. 最终处理请求的Servlet是根据请求路径选择的；
2. Filter链上的Filter是根据请求路径匹配的，可能匹配0个或多个Filter；
3. 匹配的Filter将组成FilterChain进行调用。

下面，我们首先将`Filter`纳入`ServletContext`中管理。和`ServletMapping`类似，先定义`FilterMapping`，它包含一个`Filter`实例，以及将映射路径编译为正则表达式：

```
public class FilterMapping {
    final Pattern pattern; // 编译后的正则表达式
    final Filter filter;

    public FilterMapping(String urlPattern, Filter filter) {
        this.pattern = buildPattern(urlPattern); // 编译为正则表达式
        this.filter = filter;
    }
}
```

接着，根据Servlet规范，我们需要提供`addFilter()`动态添加一个`Filter`，并且返回`FilterRegistration.Dynamic`，所以需要在`ServletContext`中实现相关方法：

```
public class ServletContextImpl implements ServletContext {
    Map<String, FilterRegistrationImpl> filterRegistrations = new HashMap<>();
    Map<String, Filter> nameToFilters = new HashMap<>();
    List<FilterMapping> filterMappings = new ArrayList<>();

    // 根据Class Name添加Filter:
    @Override
    public FilterRegistration.Dynamic addFilter(String name, String className) {
        return addFilter(name, Class.forName(className));
    }

    // 根据Class添加Filter:
    @Override
    public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> clazz) {
        return addFilter(name, clazz.newInstance());
    }

    // 根据Filter实例添加Filter:
    @Override
    public FilterRegistration.Dynamic addFilter(String name, Filter filter) {
        var registration = new FilterRegistrationImpl(this, name, filter);
        this.filterRegistrations.put(name, registration);
        return registration;
    }
    ...
}
```

再添加一个`initFilters()`方法用于向容器添加`Filter`：

```
public class ServletContextImpl implements ServletContext {
    ...
    public void initFilters(List<Class<?>> filterClasses) {
        for (Class<?> c : filterClasses) {
            // 获取@WebFilter注解:
            WebFilter wf = c.getAnnotation(WebFilter.class);
            // 添加Filter:
            FilterRegistration.Dynamic registration = this.addFilter(AnnoUtils.getFilterName(clazz), clazz);
            // 添加URL映射:
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, AnnoUtils.getFilterUrlPatterns(clazz));
            // 设置初始化参数:
            registration.setInitParameters(AnnoUtils.getFilterInitParams(clazz));
        }
        for (String name : this.filterRegistrations.keySet()) {
            // 依次处理每个FilterRegistration.Dynamic:
            var registration = this.filterRegistrations.get(name);
            // 调用Filter.init()方法:
            registration.filter.init(registration.getFilterConfig());
            this.nameToFilters.put(name, registration.filter);
            // 将Filter定义的每个URL映射编译为正则表达式:
            for (String urlPattern : registration.getUrlPatternMappings()) {
                this.filterMappings.add(new FilterMapping(urlPattern, registration.filter));
            }
        }
    }
    ...
}
```

这样，我们就完成了对Filter组件的管理。

下一步，是改造`process()`方法，把原来直接把请求扔给`Servlet`处理，改成先匹配`Filter`，处理后再扔给最终的`Servlet`：

```
public class ServletContextImpl implements ServletContext {
    ...
    public void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // 获取请求路径:
        String path = request.getRequestURI();
        // 查找Servlet:
        Servlet servlet = null;
        for (ServletMapping mapping : this.servletMappings) {
            if (mapping.matches(path)) {
                servlet = mapping.servlet;
                break;
            }
        }
        if (servlet == null) {
            // 404错误:
            PrintWriter pw = response.getWriter();
            pw.write("<h1>404 Not Found</h1><p>No mapping for URL: " + path + "</p>");
            pw.close();
            return;
        }
        // 查找Filter:
        List<Filter> enabledFilters = new ArrayList<>();
        for (FilterMapping mapping : this.filterMappings) {
            if (mapping.matches(path)) {
                enabledFilters.add(mapping.filter);
            }
        }
        Filter[] filters = enabledFilters.toArray(Filter[]::new);
        // 构造FilterChain实例:
        FilterChain chain = new FilterChainImpl(filters, servlet);
        // 由FilterChain处理:
        chain.doFilter(request, response);
    }
    ...
}
```

注意上述`FilterChain`不仅包含一个`Filter[]`数组，还包含一个`Servlet`，这样我们调用`chain.doFilter()`时，在`FilterChain`中最后一个处理请求的就是`Servlet`，这样设计可以简化我们实现`FilterChain`的代码：

```
public class FilterChainImpl implements FilterChain {
    final Filter[] filters;
    final Servlet servlet;
    final int total; // Filter总数量
    int index = 0; // 下一个要处理的Filter[index]

    public FilterChainImpl(Filter[] filters, Servlet servlet) {
        this.filters = filters;
        this.servlet = servlet;
        this.total = filters.length;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (index < total) {
            int current = index;
            index++;
            // 调用下一个Filter处理:
            filters[current].doFilter(request, response, this);
        } else {
            // 调用Servlet处理:
            servlet.service(request, response);
        }
    }
}
```

注意`FilterChain`是一个递归调用，因为在执行`Filter.doFilter()`时，需要把`FilterChain`自身传进去，在执行`Filter.doFilter()`之前，就要把`index`调整到正确的值。

我们编写两个测试用的Filter：

- LogFilter：匹配`/*`，打印请求方法、路径等信息；
- HelloFilter：匹配`/hello`，根据请求参数决定放行还是返回403错误。

在初始化ServletContextImpl时将Filter加进去，先测试`http://localhost:8080/`：

观察后台输出，`LogFilter`应该起作用：

```
16:48:00.304 [HTTP-Dispatcher] INFO  c.i.j.engine.filter.LogFilter -- GET: /
```

再测试`http://localhost:8080/hello?name=Bob`：

观察后台输出，`HelloFilter`和`LogFilter`应该起作用：

```
16:49:31.409 [HTTP-Dispatcher] INFO  c.i.j.engine.filter.HelloFilter -- Check parameter name = Bob
16:49:31.409 [HTTP-Dispatcher] INFO  c.i.j.engine.filter.LogFilter -- GET: /hello
```

最后测试`http://localhost:8080/hello?name=Jim`：

可以看到，`HelloFilter`拦截了请求，返回403错误，最终的`HelloServlet`并没有处理该请求。

现在，我们就成功地在`ServletContext`中实现了对`Filter`的管理，以及根据每个请求，构造对应的`FilterChain`来处理请求。目前还有几个小问题：

一是和Servlet一样，Filter本身应该是Web App开发人员实现，而不是由服务器实现。我们在在服务器中写死了两个Filter，这个问题后续解决；

二是Servlet规范并没有规定多个Filter应该如何排序，我们在实现时也没有对Filter进行排序。如果要按固定顺序给Filter排序，从Servlet规范来说怎么排序都可以，通常是按`@WebFilter`定义的`filterName`进行排序，Spring Boot提供的一个`FilterRegistrationBean`允许开发人员自己定义Filter的顺序。
