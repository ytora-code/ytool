package xyz.ytora.ytool.convert;

/**
 * created by yangtong on 2025/4/4 下午4:55
 * <br/>
 * 类型转换接口
 */
public interface Converter<S, T> {
    /**
     * 从 S 转换到 T
     */
    T convert(S source);

    /**
     * 从 T 转换到 S
     */
    S reverseConvert(T source);
}
