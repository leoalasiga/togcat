package com.als.togcat.engine.support;

import com.als.togcat.connector.HttpExchangeRequest;
import com.als.togcat.utils.HttpUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/1 上午10:53
 */
public class Parameters {
    final HttpExchangeRequest exchangeRequest;
    Charset charset;
    Map<String, String[]> parameters;

    public Parameters(HttpExchangeRequest exchangeRequest, String charset) {
        this.exchangeRequest = exchangeRequest;
        this.charset = Charset.forName(charset);
    }


    public void setCharset(String charset) {
        this.charset = Charset.forName(charset);
    }


    public String getParameter(String name) throws UnsupportedEncodingException {
        String[] values = getParameterValues(name);
        if (values == null) {
            return null;
        }
        return values[0];
    }

    public Enumeration<String> getParameterNames() throws UnsupportedEncodingException {
        return Collections.enumeration(getParameterMap().keySet());
    }

    public String[] getParameterValues(String name) throws UnsupportedEncodingException {
        return getParameterMap().get(name);
    }

    public Map<String, String[]> getParameterMap() throws UnsupportedEncodingException {
        if (this.parameters == null) {
            this.parameters = initParameters();
        }
        return this.parameters;
    }

    Map<String, String[]> initParameters() throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<>();
        String query = this.exchangeRequest.getRequestURI().getRawQuery();
        if (query != null) {
            params = HttpUtils.parseQuery(query, charset);
        }
        if ("POST".equals(this.exchangeRequest.getRequestMethod())) {
            String value = HttpUtils.getHeader(this.exchangeRequest.getRequestHeaders(), "Content-Type");
            if (value != null && value.startsWith("application/x-www-form-urlencoded")) {
                String requestBody;
                try {
                    requestBody = new String(this.exchangeRequest.getRequestBody(), charset);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                Map<String, List<String>> postParams = HttpUtils.parseQuery(requestBody, charset);
                // merge:
                for (String key : postParams.keySet()) {
                    List<String> postValues = postParams.get(key);
                    List<String> queryValues = params.get(key);
                    if (queryValues == null) {
                        params.put(key, postValues);
                    } else {
                        queryValues.addAll(postValues);
                    }
                }
            }
        }
        if (params.isEmpty()) {
            return new HashMap<>();
        }
        // convert:
        Map<String, String[]> paramsMap = new HashMap<>();
        for (String key : params.keySet()) {
            List<String> values = params.get(key);
            paramsMap.put(key, values.toArray(new String[0]));
        }
        return paramsMap;
    }
}
