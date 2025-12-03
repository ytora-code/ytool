package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;
import xyz.ytora.ytool.str.Strs;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToIntegerConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        if (Strs.isEmpty(source)) return null;
        return Integer.valueOf(source);
    }

    @Override
    public String reverseConvert(Integer source) {
        if (source == null) return null;
        return String.valueOf(source);
    }
}
