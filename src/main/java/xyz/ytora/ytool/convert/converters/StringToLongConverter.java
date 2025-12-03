package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;
import xyz.ytora.ytool.str.Strs;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToLongConverter implements Converter<String, Long> {
    @Override
    public Long convert(String source) {
        if (Strs.isEmpty(source)) return null;
        return Long.valueOf(source);
    }

    @Override
    public String reverseConvert(Long source) {
        if (source == null) return null;
        return String.valueOf(source);
    }
}
