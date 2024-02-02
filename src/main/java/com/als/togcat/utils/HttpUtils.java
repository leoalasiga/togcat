package com.als.togcat.utils;

import com.sun.net.httpserver.Headers;
import jakarta.servlet.http.Cookie;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/1 上午11:18
 */
public class HttpUtils {
    static final Pattern QUERY_SPLIT = Pattern.compile("\\&");
    /**
     * Parse query string.
     */
    public static Map<String, List<String>> parseQuery(String query, Charset charset) {
        if (query == null || query.isEmpty()) {
            return new HashMap<>();
        }
        String[] ss = QUERY_SPLIT.split(query);
        Map<String, List<String>> map = new HashMap<>();
        for (String s : ss) {
            int n = s.indexOf('=');
            if (n >= 1) {
                String key = s.substring(0, n);
                String value = s.substring(n + 1);
                List<String> exist = map.get(key);
                if (exist == null) {
                    exist = new ArrayList<>(4);
                    map.put(key, exist);
                }
                try {
                    exist.add(URLDecoder.decode(value, charset.name()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    public static Map<String, List<String>> parseQuery(String query) throws UnsupportedEncodingException {
        return parseQuery(query, StandardCharsets.UTF_8);
    }

    public static String getHeader(Headers headers, String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    public static Cookie[] parseCookies(String cookieValue) {
        if (cookieValue == null) {
            return null;
        }
        cookieValue = cookieValue.trim();
        if (cookieValue.isEmpty()) {
            return null;
        }
        String[] ss = cookieValue.split(";");
        Cookie[] cookies = new Cookie[ss.length];
        for (int i = 0; i < ss.length; i++) {
            String s = ss[i].trim();
            int pos = s.indexOf('=');
            String name = s;
            String value = "";
            if (pos >= 0) {
                name = s.substring(0, pos);
                value = s.substring(pos + 1);
            }
            cookies[i] = new Cookie(name, value);
        }
        return cookies;
    }
}
