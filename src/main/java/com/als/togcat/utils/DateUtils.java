package com.als.togcat.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TODO
 *
 * @author 刘嘉杰
 * @version 1.0.0
 * @date 2024/2/2 上午9:11
 */
public class DateUtils {
    static final ZoneId GMT = ZoneId.of("Z");

    public static long parseDateTimeGMT(String s) {
        ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
        return zdt.toInstant().toEpochMilli();
    }

    public static String formatDateTimeGMT(long ts) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), GMT);
        return zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
