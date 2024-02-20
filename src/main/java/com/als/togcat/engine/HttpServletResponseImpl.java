package com.als.togcat.engine;

import com.als.togcat.Config;
import com.als.togcat.connector.HttpExchangeResponse;
import com.als.togcat.engine.support.HttpHeaders;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午3:49
 */
public class HttpServletResponseImpl implements HttpServletResponse {
    final Config config;
    final HttpExchangeResponse httpExchangeResponse;
    final HttpHeaders headers;

    int status = 200;
    int bufferSize = 1024;
    Boolean callOutput = null;
    ServletOutputStream output;
    PrintWriter writer;

    String contentType;
    String characterEncoding;
    long contentLength = 0;
    Locale locale = null;
    List<Cookie> cookies = null;
    boolean committed = false;

    public HttpServletResponseImpl(Config config,HttpExchangeResponse httpExchangeResponse) {
        this.config = config;
        this.httpExchangeResponse = httpExchangeResponse;
        this.headers = new HttpHeaders(httpExchangeResponse.getResponseHeaders());
//        this.characterEncoding = "UTF-8";
        this.characterEncoding = config.server.responseEncoding;
        this.setContentType("text/html");
    }

    @Override
    public void addCookie(Cookie cookie) {
        checkNotCommitted();
        if (this.cookies == null) {
            this.cookies = new ArrayList<>();
        }
        this.cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String s) {
        return this.headers.containsHeader(s);
    }

    @Override
    public String encodeURL(String url) {
        // no need to append session id:
        return url;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return url;
    }

    @Override
    public String encodeUrl(String s) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String s) {
        return null;
    }

    @Override
    public void sendError(int sc, String s) throws IOException {
        checkNotCommitted();
        this.status = sc;
        commitHeaders(-1);
    }

    @Override
    public void sendError(int i) throws IOException {
        sendError(i, "Error");

    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkNotCommitted();
        this.status = 302;
        this.headers.setHeader("Location", location);
        commitHeaders(-1);
    }

    @Override
    public void setDateHeader(String s, long l) {
        checkNotCommitted();
        this.headers.setDateHeader(s, l);
    }

    @Override
    public void addDateHeader(String s, long l) {
        checkNotCommitted();
        this.headers.addDateHeader(s, l);
    }

    @Override
    public void setHeader(String name, String value) {
        checkNotCommitted();
        this.headers.setHeader(name, value);

//        this.httpExchangeResponse.getResponseHeaders().set(name, value);
    }

    @Override
    public void addHeader(String s, String s1) {
        checkNotCommitted();
        this.headers.addHeader(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        checkNotCommitted();
        this.headers.setIntHeader(s, i);
    }

    @Override
    public void addIntHeader(String s, int i) {
        checkNotCommitted();
        this.headers.addIntHeader(s, i);
    }

    @Override
    public void setStatus(int sc) {
        checkNotCommitted();
        this.status = sc;
    }

    @Override
    public void setStatus(int i, String s) {

    }

    @Override
    public int getStatus() {
        return this.status;
    }

    @Override
    public String getHeader(String s) {
        return this.headers.getHeader(s);
    }

    @Override
    public Collection<String> getHeaders(String s) {
        List<String> hs = this.headers.getHeaders(s);
        if (hs == null) {
            return new ArrayList<>();
        }
        return hs;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.unmodifiableSet(this.headers.getHeaderNames());
    }

    @Override
    public String getCharacterEncoding() {
        return this.characterEncoding;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (callOutput == null) {
            commitHeaders(0);
            this.output = new ServletOutputStreamImpl(this.httpExchangeResponse.getResponseBody());
            this.callOutput = Boolean.TRUE;
            return this.output;
        }
        if (callOutput.booleanValue()) {
            return this.output;
        }
        throw new IllegalStateException("Cannot open output stream when writer is opened.");
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (callOutput == null) {
            commitHeaders(0);
            this.writer = new PrintWriter(this.httpExchangeResponse.getResponseBody(), true);
            this.callOutput = Boolean.FALSE;
            return this.writer;
        }
        if (!callOutput.booleanValue()) {
            return this.writer;
        }
        throw new IllegalStateException("Cannot open writer when output stream is opened.");
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.characterEncoding = charset;
    }

    @Override
    public void setContentLength(int i) {
        this.contentLength = i;
    }

    @Override
    public void setContentLengthLong(long l) {
        this.contentLength = l;

    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;

//        setHeader("Content-Type", contentType);
        if (type.startsWith("text/")) {
            setHeader("Content-Type", contentType + "; charset=" + this.characterEncoding);
        } else {
            setHeader("Content-Type", contentType);
        }
    }


    @Override
    public void setBufferSize(int size) {
        if (this.callOutput != null) {
            throw new IllegalStateException("Output stream or writer is opened.");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
        this.bufferSize = size;
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (this.callOutput == null) {
            throw new IllegalStateException("Output stream or writer is not opened.");
        }
        if (this.callOutput.booleanValue()) {
            this.output.flush();
        } else {
            this.writer.flush();
        }
    }

    @Override
    public void resetBuffer() {
        checkNotCommitted();

    }

    @Override
    public boolean isCommitted() {
        return this.committed;

    }

    @Override
    public void reset() {
        checkNotCommitted();
        this.status = 200;
        this.headers.clearHeaders();
    }

    @Override
    public void setLocale(Locale locale) {
        checkNotCommitted();
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return this.locale == null ? Locale.getDefault() : this.locale;
    }

    void commitHeaders(long length) throws IOException {
        this.httpExchangeResponse.sendResponseHeaders(this.status, length);
        this.committed = true;
    }

    public void cleanup() throws IOException {
        if (this.callOutput != null) {
            if (this.callOutput.booleanValue()) {
                this.output.close();
            } else {
                this.writer.close();
            }
        }
    }

    // check if not committed:
    void checkNotCommitted() {
        if (this.committed) {
            throw new IllegalStateException("Response is committed.");
        }
    }
}
