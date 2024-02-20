package com.als.togcat.utils;

/**
 * @description:
 * @author: liujiajie
 * @date: 2024/2/19 11:01
 */
public class HtmlUtils {
    public static String encodeHtml(String s) {
        return s.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;").replace("\"", "&quot;");
    }
}
