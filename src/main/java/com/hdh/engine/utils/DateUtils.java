package com.hdh.engine.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class DateUtils {

    static final ZoneId GMT = ZoneId.of("Z");

    /**
     * 解析GMT时间字符串
     * @param s GMT时间字符串, 例如: "Tue, 15 Nov 1994 08:12:31 GMT"
     * @return 毫秒时间戳
     */
    public static long parseDateTimeGMT(String s) {
        ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
        return zdt.toInstant().toEpochMilli();
    }

    /**
     * 格式化时间戳为GMT时间字符串
     * @param ts 毫秒时间戳
     * @return GMT时间字符串, 例如: "Tue, 15 Nov 1994 08:12:31 GMT"
     */
    public static String formatDateTimeGMT(long ts) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), GMT);
        return zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * 解析时间字符串为 yyyy-MM-dd HH:mm:ss 格式
     * @param s 时间字符串
     * @return 毫秒时间戳
     */
    public static long parseDateTime(String s) {
        ZonedDateTime zdt = ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return zdt.toInstant().toEpochMilli();
    }

    /**
     * 格式化时间戳为 yyyy-MM-dd HH:mm:ss 格式
     * @param ts 毫秒时间戳
     * @return 格式化后的时间字符串
     */
    public static String formatDateTime(long ts) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), GMT);
        return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
