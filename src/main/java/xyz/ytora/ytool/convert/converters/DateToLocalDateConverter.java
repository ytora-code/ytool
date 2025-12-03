package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * created by yangtong on 2025/8/9 22:44:24
 * <br/>
 */
public class DateToLocalDateConverter implements Converter<Date, LocalDate> {

    @Override
    public LocalDate convert(Date source) {
        if (source == null) return null;
        long millis = source.getTime();
        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @Override
    public Date reverseConvert(LocalDate source) {
        if (source == null) return null;
        Instant instant = source.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }
}
