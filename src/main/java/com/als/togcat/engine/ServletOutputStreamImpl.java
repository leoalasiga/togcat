package com.als.togcat.engine;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

/**
 * ServletOutputStream 是 Java Servlet 中的一个类，它提供了输出二进制数据到客户端的功能。在Servlet开发中，用于向客户端发送二进制数据（如文件、图像等）时，通常会使用 ServletOutputStream。
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:29
 */
public class ServletOutputStreamImpl extends ServletOutputStream {
    private final OutputStream output;
    private WriteListener writeListener = null;

    public ServletOutputStreamImpl(OutputStream output) {
        this.output = output;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void close() throws IOException {
        this.output.close();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        this.writeListener = writeListener;
        try {
            this.writeListener.onWritePossible();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            this.output.write(b);
        } catch (IOException e) {
            if (this.writeListener != null) {
                this.writeListener.onError(e);
            }
            throw e;
        }
    }
}
