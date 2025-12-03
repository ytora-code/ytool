package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;
import xyz.ytora.ytool.str.Strs;

import java.math.BigDecimal;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class DoubleToStringConverter implements Converter<Double, String> {
    @Override
    public String convert(Double source) {
        if (source == null) return null;
        return BigDecimal.valueOf(source).toPlainString();
    }

    @Override
    public Double reverseConvert(String source) {
        if (Strs.isEmpty(source)) return null;
        return Double.valueOf(source);
    }
}
