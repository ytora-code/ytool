package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class DoubleToIntegerConverter implements Converter<Double, Integer> {
    @Override
    public Integer convert(Double source) {
        if (source == null) return null;
        return source.intValue();
    }

    @Override
    public Double reverseConvert(Integer source) {
        if (source == null) return null;
        return Double.valueOf(source);
    }
}
