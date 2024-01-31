package com.als.togcat.engine.mapping;

import java.util.regex.Pattern;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/1/29 下午5:13
 */
public class AbstractMapping implements Comparable<AbstractMapping> {

    final Pattern pattern;
    final String url;

    public AbstractMapping(String urlPattern) {
        this.url = urlPattern;
        this.pattern = buildPatter(urlPattern);
    }

    public boolean matches(String uri) {
        return pattern.matcher(uri).matches();
    }


    private Pattern buildPatter(String urlPattern) {
        StringBuilder sb = new StringBuilder(urlPattern.length() + 16);
        sb.append("^");
        for (int i = 0; i < urlPattern.length(); i++) {
            char ch = urlPattern.charAt(i);
            if (ch == '*') {
                sb.append(".*");
            } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
                sb.append(ch);
            } else {
                sb.append('\\').append(ch);
            }
        }

        sb.append("$");
        return Pattern.compile(sb.toString());
    }

    @Override
    public int compareTo(AbstractMapping o) {
        int cmp = this.priority() - o.priority();
        if (cmp == 0) {
            cmp = this.url.compareTo(o.url);
        }
        return cmp;
    }

    int priority() {
        if (this.url.equals("/")) {
            return Integer.MAX_VALUE;
        }
        if (this.url.startsWith("*")) {
            return Integer.MAX_VALUE - 1;
        }
        return 100000 - this.url.length();
    }

}
