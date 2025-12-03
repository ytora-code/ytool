package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public LocalDateTime convert(String source) {
        if (source == null) return null;
        if (source.indexOf('T') >= 0) source = source.replace('T', ' ');

        try {
            return LocalDateTime.parse(source, F);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String reverseConvert(LocalDateTime source) {
        return source.format(F);
    }
}
