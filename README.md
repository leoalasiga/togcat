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
