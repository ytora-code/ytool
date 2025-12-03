package xyz.ytora.ytool.convert;

/**
 * created by yangtong on 2025/4/4 下午4:59
 * <br/>
 * 转换服务接口
 */
public interface ConversionService {
    <T> T convert(Object source, Class<T> targetType);
}