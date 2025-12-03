package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;
import xyz.ytora.ytool.date.Dates;

import java.util.Date;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToDateConverter implements Converter<String, Date> {

    @Override
    public Date convert(String source) {
        if (source == null) return null;
        if (source.indexOf('T') >= 0) source = source.replace('T', ' ');

        try {
            source = source.trim();
            // yyyy-MM-dd HH:mm:ss; yyyy-MM-ddTHH:mm:ss
            if (source.indexOf(' ') >= 0 || source.indexOf('T') >= 0) {
                return Dates.parseDateTime(source);
            } else {
                return Dates.parseDate(source);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String reverseConvert(Date source) {
        return Dates.formatDate(source);
    }
}
