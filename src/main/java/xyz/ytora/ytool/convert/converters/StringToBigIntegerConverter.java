package xyz.ytora.ytool.convert.converters;

import xyz.ytora.ytool.convert.Converter;

import java.math.BigInteger;

/**
 * created by yangtong on 2025/4/4 下午5:36
 */
public class StringToBigIntegerConverter implements Converter<String, BigInteger> {
    @Override
    public BigInteger convert(String source) {
        return BigInteger.valueOf(Long.parseLong(source));
    }

    @Override
    public String reverseConvert(BigInteger source) {
        return String.valueOf(source);
    }
}
