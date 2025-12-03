package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;
import xyz.ytora.ytool.number.Numbers;

/**
 * created by yangtong on 2025/8/13 19:19:53
 * <br/>
 */
public class NumberToBooleanConverter implements Converter<Number, Boolean> {
    @Override
    public Boolean convert(Number source) {
        if (source == null) {
            return null;
        }
        return Numbers.compare(source, 0) != 0;
    }

    @Override
    public Number reverseConvert(Boolean source) {
        if (source == null) {
            return null;
        }
        return source ? 1 : 0;
    }
}
