package xyz.ytora.ytool.convert;

/**
 * created by yangtong on 2025/4/4 下午4:58
 * <br/>
 * 转换器注册中心接口
 */
public interface ConverterRegistry {
    <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<S, T> converter);
}
