package com.als.togcat.connector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    byte[] requestBodyData;

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

    @Override
    public Headers getRequestHeaders() {
        return this.httpExchange.getRequestHeaders();

    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return this.httpExchange.getRemoteAddress();

    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return this.httpExchange.getLocalAddress();
    }

    @Override
    public byte[] getRequestBody() throws IOException {
        if (this.requestBodyData == null) {
            try (InputStream input = this.httpExchange.getRequestBody()) {
                this.requestBodyData = readAllBytes(input);
            }
        }
        return this.requestBodyData;
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        return readNBytes(Integer.MAX_VALUE, input);
    }


    public byte[] readNBytes(int len, InputStream input) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, 8192)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = input.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (Integer.MAX_VALUE - 8 - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }
}
