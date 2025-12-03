package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Override
    public LocalDate convert(String source) {
        if (source == null) return null;

        try {
            return LocalDate.parse(source, F);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String reverseConvert(LocalDate source) {
        return source.format(F);
    }
}
